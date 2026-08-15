# SurfaceFlinger 模块教案级讲义（基于本仓库源码）
适用范围：`frameworks/native/services/surfaceflinger`（本仓库路径）

目标：读完并动手验证后，你应能像一个成熟的 Android 图形栈/系统开发者一样，读懂并定位 SurfaceFlinger 的关键路径：事务（Transaction）→ 提交（commit）→ 合成（composite）→ 呈现（present），并能对刷新率/VSYNC/合成策略/HWC 交互、关键性能指标与调试方法形成系统化的心智模型。

本讲义的写作风格偏“教案”：每个主题给出学习目标、主线图、关键数据结构/函数入口、典型问题与练习。讲义内容尽可能以源码为准，并给出可点击的代码定位链接（`file:///...`）。

---

## 目录
- 1. 模块定位与边界：SurfaceFlinger 在 Android 图形栈中的角色
- 2. 目录结构速查：本模块有哪些子系统
- 3. 进程入口与启动：main → init → Scheduler::run
- 4. 核心对象与线程模型：谁在什么线程做什么
- 5. 显示系统抽象：DisplayDevice / CompositionDisplay / Output
- 6. 图层系统抽象：Layer（Legacy）与 FrontEnd（新前端）并存
- 7. 事务系统（Transaction）：从客户端到 SF 的状态变更如何落地
- 8. 帧循环主线：configure / commit / composite / postComposition
- 9. 合成路径与 HWC：validate/present、Client Target、fence 语义
- 10. 刷新率与 VSYNC：Scheduler、VsyncSchedule、RefreshRateSelector
- 11. 性能与功耗：TimeStats、FrameTimeline、PowerAdvisor
- 12. 捕获与可观测性：ScreenCapture、LayerTracing、TransactionTracing、FrameTracer
- 13. 常见问题定位清单：黑屏/卡顿/掉帧/撕裂/刷新率异常/色彩异常
- 14. 训练题：把知识变成“能独立排障与改代码”的能力

---

## 1. 模块定位与边界：SurfaceFlinger 在 Android 图形栈中的角色
### 1.1 你要掌握的“主线”
SurfaceFlinger（下文 SF）是 Android 图形系统的合成服务，核心职责是：
- 接收来自系统/应用的绘制结果（BufferQueue/GraphicBuffer）、层级关系与属性变更（Transaction）。
- 在每个 VSYNC 周期内决定“这帧显示什么、以什么刷新率显示、哪些交给 HWC 合成、哪些交给 GPU 合成”。
- 与 Composer HAL（HWC/HWC3）交互：validate/commit/present，得到 present fence、layer release fence。
- 维护“窗口/输入”所需的几何与可见性快照，并向上游（WM、Input、Stats）发回回调与指标。

你可以用下图快速建立全局心智模型（省略大量细节）：

```text
App / SystemUI / SystemServer
  |  (SurfaceControl, Transaction, BufferQueue)
  v
Binder( ISurfaceComposer / AIDL )  +  BufferQueue producer
  |                                 |
  v                                 v
SurfaceFlinger main thread  <---  Buffer latch (acquire fence)
  |
  | commit(): 事务合并/过滤/应用、Layer/Display 状态生成
  |
  | composite(): 形成 CompositionRefreshArgs
  |     -> CompositionEngine
  |         -> (GPU) RenderEngine + (HWC) Composer HAL
  |
  | postComposition(): release fence 分发、TimeStats/FrameTimeline、回调
  v
Display (Physical/Virutal)
```

### 1.2 SF 的“输入/输出”是什么
以“接口抽象”的语言描述：
- 输入
  - “层与显示的状态描述”：来自 Transaction（位置、透明度、裁剪、z-order、颜色空间/亮度策略、帧率投票等）
  - “像素内容”：GraphicBuffer + acquire fence（来自 BufferQueue）
  - “VSYNC 节拍”：硬件 VSYNC 或合成 VSYNC（由 Scheduler 管理）
- 输出
  - 对每个 display 的 present：HWC present 或 GPU 渲染到 framebuffer/virtual display buffer
  - fence：present fence（显示扫描出/提交完成）、release fence（layer/buffer 可复用）
  - 回调：TransactionCallback、FrameTimeline、WindowInfos（输入系统）、统计指标

### 1.3 与其他模块的边界
本目录只包含 SF 服务与其子系统，不包含：
- RenderEngine 的完整实现（通常在 `frameworks/native/libs/renderengine` 之类目录）
- UI 渲染（Skia、HWUI）与应用绘制
- BufferQueue/GraphicBuffer/Gralloc 的完整实现
- 具体厂商 HWC HAL 的实现（通常在 vendor）

本讲义会在“需要理解交互协议”的程度上覆盖这些边界模块。

---

## 2. 目录结构速查：本模块有哪些子系统
以“你打开目录第一眼要看懂什么”为目标，给出结构化的速查表。以下路径均相对 `frameworks/native/services/surfaceflinger/`。

### 2.1 顶层关键文件
- 入口： [main_surfaceflinger.cpp](file:///Volumes/disk2/sdk/amlogic_s905d5/frameworks/native/services/surfaceflinger/main_surfaceflinger.cpp)
- 主类： [SurfaceFlinger.h](file:///Volumes/disk2/sdk/amlogic_s905d5/frameworks/native/services/surfaceflinger/SurfaceFlinger.h)、[SurfaceFlinger.cpp](file:///Volumes/disk2/sdk/amlogic_s905d5/frameworks/native/services/surfaceflinger/SurfaceFlinger.cpp)
- Layer（Legacy）：[Layer.h](file:///Volumes/disk2/sdk/amlogic_s905d5/frameworks/native/services/surfaceflinger/Layer.h)、[Layer.cpp](file:///Volumes/disk2/sdk/amlogic_s905d5/frameworks/native/services/surfaceflinger/Layer.cpp)
- DisplayDevice： [DisplayDevice.h](file:///Volumes/disk2/sdk/amlogic_s905d5/frameworks/native/services/surfaceflinger/DisplayDevice.h)、[DisplayDevice.cpp](file:///Volumes/disk2/sdk/amlogic_s905d5/frameworks/native/services/surfaceflinger/DisplayDevice.cpp)

### 2.2 Scheduler（VSYNC、刷新率、EventThread）
- 目录：`Scheduler/`
- 关键： [Scheduler.h](file:///Volumes/disk2/sdk/amlogic_s905d5/frameworks/native/services/surfaceflinger/Scheduler/Scheduler.h)
- 关键接口（commit/composite 的抽象边界）：[ICompositor.h](file:///Volumes/disk2/sdk/amlogic_s905d5/frameworks/native/services/surfaceflinger/Scheduler/include/scheduler/interface/ICompositor.h)

### 2.3 CompositionEngine（合成框架）
- 目录：`CompositionEngine/`
- 总入口接口： [CompositionEngine.h](file:///Volumes/disk2/sdk/amlogic_s905d5/frameworks/native/services/surfaceflinger/CompositionEngine/include/compositionengine/CompositionEngine.h)
- 子系统：Output/Display/OutputLayer、planner（策略预测/缓存）

### 2.4 DisplayHardware（HWC、Composer HAL 适配）
- 目录：`DisplayHardware/`
- 关键抽象： [HWComposer.h](file:///Volumes/disk2/sdk/amlogic_s905d5/frameworks/native/services/surfaceflinger/DisplayHardware/HWComposer.h)
- HWC2/HIDL/AIDL 适配：`HWC2.*`, `AidlComposerHal.*`, `HidlComposerHal.*`

### 2.5 FrontEnd（新前端：事务与快照生成）
- 目录：`FrontEnd/`
- 综述： [FrontEnd/readme.md](file:///Volumes/disk2/sdk/amlogic_s905d5/frameworks/native/services/surfaceflinger/FrontEnd/readme.md)
- 关键：TransactionHandler、LayerLifecycleManager、LayerHierarchy、LayerSnapshotBuilder

### 2.6 Tracing / TimeStats / FrameTimeline
- Tracing：`Tracing/`（LayerTracing、TransactionTracing 等）
- TimeStats：`TimeStats/`
- FrameTimeline：`FrameTimeline/`、FrameTracer：`FrameTracer/`

这一节的目标是让你知道“该去哪里找什么”，后续每个主题会回到这些目录。

---

## 3. 进程入口与启动：main → init → Scheduler::run
### 3.1 入口 main 做了什么（为什么这些步骤重要）
入口在 [main_surfaceflinger.cpp](file:///Volumes/disk2/sdk/amlogic_s905d5/frameworks/native/services/surfaceflinger/main_surfaceflinger.cpp#L79-L166)。你应该逐行理解它的工程意义，而不是“看到就略过”。

关键动作拆解：
- HIDL/AIDL 线程池与 allocator service
  - 配置 RPC 线程池、根据 sysprop 启动 Graphics Allocator passthrough service（对早期启动阶段很关键）。
- Binder 线程池与调度策略
  - 限制 binder 线程数：`ProcessState::self()->setThreadPoolMaxThreadCount(4)`。
  - 把 binder 线程池设置为 RT（SCHED_FIFO，最低 RT 优先级），避免 binder 回调造成延迟抖动。
- 创建 SF 实例并初始化
  - `sp<SurfaceFlinger> flinger = surfaceflinger::createSurfaceFlinger();`
  - `flinger->init();` 在对外注册服务前完成关键初始化（RenderEngine、HWC、Display 配置、Scheduler 等）。
- 注册服务
  - 注册传统的 `SurfaceFlinger` 服务名，以及 AIDL 的 `SurfaceFlingerAIDL`（见 [main_surfaceflinger.cpp](file:///Volumes/disk2/sdk/amlogic_s905d5/frameworks/native/services/surfaceflinger/main_surfaceflinger.cpp#L145-L155)）。
- 进入主循环
  - `flinger->run();` 实际上是 `Scheduler::run()`（见 [SurfaceFlinger::run](file:///Volumes/disk2/sdk/amlogic_s905d5/frameworks/native/services/surfaceflinger/SurfaceFlinger.cpp#L532-L534)）。

### 3.2 init 里最关键的“依赖顺序”
`SurfaceFlinger::init()` 在 [SurfaceFlinger.cpp](file:///Volumes/disk2/sdk/amlogic_s905d5/frameworks/native/services/surfaceflinger/SurfaceFlinger.cpp#L830-L978)。重点理解“先后顺序”：
- RenderEngine 先创建（需要 EGL/VK 上下文、线程策略等）
- CompositionEngine 注入 RenderEngine、TimeStats、HWComposer，并设置回调
- `configureLocked()` 处理 boot 时的 display hotplug/模式配置
- commit primary display（后续 Scheduler 初始化依赖 DisplayDevice 的部分对象）
- `initScheduler(display)` 创建 Scheduler、EventThread（Render/LastComposite）、initVsync
- 启动其它线程：RegionSamplingThread、StartPropertySetThread、PowerAdvisor 等

对照代码位置：
- RenderEngine 创建： [SurfaceFlinger::init](file:///Volumes/disk2/sdk/amlogic_s905d5/frameworks/native/services/surfaceflinger/SurfaceFlinger.cpp#L836-L857)
- configure + primary display commit + initScheduler： [SurfaceFlinger::init](file:///Volumes/disk2/sdk/amlogic_s905d5/frameworks/native/services/surfaceflinger/SurfaceFlinger.cpp#L892-L919)
- initScheduler 细节： [SurfaceFlinger::initScheduler](file:///Volumes/disk2/sdk/amlogic_s905d5/frameworks/native/services/surfaceflinger/SurfaceFlinger.cpp#L3987-L4049)

### 3.3 Scheduler::run 意味着什么
`SurfaceFlinger::run()` 只是转调： [SurfaceFlinger::run](file:///Volumes/disk2/sdk/amlogic_s905d5/frameworks/native/services/surfaceflinger/SurfaceFlinger.cpp#L532-L534)。

从架构上，这代表：
- SF 主线程是 Scheduler 的消息循环线程（MessageQueue/Looper 抽象）。
- 所有“必须串行化、与显示帧节拍相关”的工作，都应该在这个线程执行（或通过 `mScheduler->schedule(...)` 安排到这里）。

练习（建议真做）：
- 在 `SurfaceFlinger::getNewTexture()` 中观察跨线程 schedule 的使用： [SurfaceFlinger::getNewTexture](file:///Volumes/disk2/sdk/amlogic_s905d5/frameworks/native/services/surfaceflinger/SurfaceFlinger.cpp#L769-L795)。
- 思考：为什么 genTextures 要尽量在主线程做？什么时候必须跨线程 schedule？

---

## 4. 核心对象与线程模型：谁在什么线程做什么
### 4.1 线程清单（必须背下来并能关联职责）
建议把这些线程当作“系统中的角色”去记：
- SF 主线程（Scheduler::run 所在线程）
  - 事务 flush/commit、选择刷新率、组织合成参数、驱动 CompositionEngine present
- Binder 线程池
  - 承接来自客户端/系统服务的 IPC 调用（创建 Layer、提交 Transaction、查询状态等）
  - 关键原则：避免在 binder 线程做重活；应把重活 schedule 到主线程
- EventThread（至少两条逻辑周期）
  - Render cycle 与 LastComposite cycle（见 [initScheduler](file:///Volumes/disk2/sdk/amlogic_s905d5/frameworks/native/services/surfaceflinger/SurfaceFlinger.cpp#L4031-L4044)）
  - 负责向 app/SF 内部提供 VSYNC 事件与时间预算信息
- HWC 回调线程（由 HAL 侧触发）
  - hotplug/vsync/refresh 等回调进入 SF（通常会抓锁或 schedule 合成）
  - 示例：`onComposerHalRefresh()` 里直接触发 `scheduleComposite`： [SurfaceFlinger::onComposerHalRefresh](file:///Volumes/disk2/sdk/amlogic_s905d5/frameworks/native/services/surfaceflinger/SurfaceFlinger.cpp#L2173-L2176)
- RenderEngine 相关线程（取决于 RenderEngine 类型：GLES/Threaded/SkiaGL/SkiaVK 等）
  - SF init 里可以通过 sysprop 选择类型： [chooseRenderEngineTypeViaSysProp](file:///Volumes/disk2/sdk/amlogic_s905d5/frameworks/native/services/surfaceflinger/SurfaceFlinger.cpp#L805-L826)
- RegionSamplingThread
  - 用于亮度/内容采样等（由 `ICompositor::sample()` 驱动）
- StartPropertySetThread（启动阶段属性写入/依赖通知）
  - 避免在主线程 property_set 导致死锁/阻塞（见 init 末尾的注释与创建）

### 4.2 关键锁与并发原则
最重要的锁是 `mStateLock`（保护 Layer/Display 的状态树），你需要掌握以下原则：
- 状态拆分：`mCurrentState`（事务提交中）、`mDrawingState`（当前用于绘制的快照）
- 主线程串行化：commit/composite/postComposition 等核心路径多数在主线程执行，但仍会与 binder/HWC 回调并发，因此锁与 schedule 两者缺一不可
- “跨线程访问 HWC/RenderEngine”的规则在各自类注释中通常有说明，务必遵守（避免死锁/卡顿）

练习：
- 在 [Scheduler::schedule](file:///Volumes/disk2/sdk/amlogic_s905d5/frameworks/native/services/surfaceflinger/Scheduler/Scheduler.h#L131-L144) 看看任务是如何被 post 到消息队列的。

---

## 5. 显示系统抽象：DisplayDevice / CompositionDisplay / Output
你要把“显示”当作三层抽象来理解：
1) 逻辑显示设备（DisplayDevice）：SF 侧对外管理、参与事务、持有显示属性与策略
2) 合成显示（CompositionDisplay）：CompositionEngine 的 display 表示，用于组织 OutputLayer、渲染目标与策略
3) HAL display（HWC display / HalDisplayId）：真正输出到硬件或虚拟 buffer 的对象

### 5.1 物理显示与虚拟显示
SF 同时管理 physical display 与 virtual display：
- 物理显示的热插拔由 HWC 回调驱动，SF 会在 configure/commit 中处理
- 虚拟显示既可能走 HWC（HAL virtual display），也可能走 GPU（fallback）
  - 相关逻辑： [SurfaceFlinger::acquireVirtualDisplay](file:///Volumes/disk2/sdk/amlogic_s905d5/frameworks/native/services/surfaceflinger/SurfaceFlinger.cpp#L614-L633)

### 5.2 display 模式与刷新率
display mode 变更不是“立即生效”的简单 set：
- mode set 可能有 timeline（约束/无缝切换），需要在 commit 中 finalize
  - 代码片段：commit 开头检查 `isModeSetPending()` 并可能 `finalizeDisplayModeChange`： [SurfaceFlinger::commit](file:///Volumes/disk2/sdk/amlogic_s905d5/frameworks/native/services/surfaceflinger/SurfaceFlinger.cpp#L2393-L2423)
- Scheduler 与 RefreshRateSelector 会基于 layer history 与策略选择 preferred mode（后文详解）

---

## 6. 图层系统抽象：Layer（Legacy）与 FrontEnd（新前端）并存
这是近年 SF 最大的架构变化之一：为了把“事务处理/层级生成/快照生成”做得更可预测、更高性能，引入了 FrontEnd pipeline，但在工程上必须与 Legacy Layer 并存一段时间。

### 6.1 什么时候走 Legacy，什么时候走新前端
SF 在启动时根据属性决定：
- `mLayerLifecycleManagerEnabled`：是否启用 LayerLifecycleManager
- `mLegacyFrontEndEnabled`：是否保留 legacy 生成路径
  - 初始化逻辑在 SF 构造/属性读取附近（可从 [SurfaceFlinger.cpp](file:///Volumes/disk2/sdk/amlogic_s905d5/frameworks/native/services/surfaceflinger/SurfaceFlinger.cpp#L494-L498) 看到组合判断）

commit 阶段的分岔点（非常重要）：
- legacy path： [updateLayerSnapshotsLegacy](file:///Volumes/disk2/sdk/amlogic_s905d5/frameworks/native/services/surfaceflinger/SurfaceFlinger.cpp#L2210-L2229)
- new FrontEnd path： [updateLayerSnapshots](file:///Volumes/disk2/sdk/amlogic_s905d5/frameworks/native/services/surfaceflinger/SurfaceFlinger.cpp#L2263-L2375)
- 二者都可能运行（为了过渡与对比），但最终会归一

### 6.2 FrontEnd 五阶段流水线（建议背下来）
FrontEnd 自带的综述非常精炼，强烈建议先读一遍： [FrontEnd/readme.md](file:///Volumes/disk2/sdk/amlogic_s905d5/frameworks/native/services/surfaceflinger/FrontEnd/readme.md#L57-L70)

五阶段（对应 readme 中的 bullet）：
1) 事务队列与 ready 过滤（TransactionHandler）
2) Layer 生命周期与 server-side RequestedLayerState 维护（LayerLifecycleManager）
3) 树结构生成（LayerHierarchyBuilder）
4) Flatten + Z-order 快照生成（LayerSnapshotBuilder）
5) 回调发射（callbacks）

关键的工程目标：
- 热路径少锁/少拷贝/少上下文切换
- buffer update 快路径必须稳定且快
- snapshot 与 composition 解耦（可 clone、可异步消费）

---

## 7. 事务系统（Transaction）：从客户端到 SF 的状态变更如何落地
### 7.1 事务的本质：原子性、顺序性、可合并但不可交换
从使用者角度，Transaction 是一组对 Layer/Display 的状态变更；从 SF 角度，它是一组“待合并的状态 delta”。

FrontEnd 文档明确指出 merge 的代数性质（必须理解，否则会犯很隐蔽的 bug）： [FrontEnd/readme.md](file:///Volumes/disk2/sdk/amlogic_s905d5/frameworks/native/services/surfaceflinger/FrontEnd/readme.md#L31-L54)
- 合并是 associative（结合律成立）
- 但不是 commutative（交换律不成立）
- 顺序由 applyToken 局部保证（默认按进程/producer 隔离）

### 7.2 事务何时“真正生效”：Flush/Commit/Composite 的分层
很多初学者会把“收到 transaction”误解成“立即改变屏幕”。实际上在 SF 里至少分三层：
- apply/merge：把 delta 合并进 server-side state（可能在 binder 线程，也可能延后）
- commit：决定这一帧要使用哪些 state，更新 drawing state、计算几何/可见区域、决定是否需要合成
- composite：真正把当前帧渲染/提交到显示

在本仓库里，`ICompositor` 接口把 commit/composite 分得非常清晰： [ICompositor.h](file:///Volumes/disk2/sdk/amlogic_s905d5/frameworks/native/services/surfaceflinger/Scheduler/include/scheduler/interface/ICompositor.h#L37-L55)

### 7.3 Transaction Flush：为什么要有“ready filters”
SF 不能无脑把收到的事务全部应用：
- 有些事务带 desiredPresentTime，希望与某个 VSYNC 对齐
- 有些事务需要等待 buffer ready 或 fence 条件
- 有些事务可能因为 backpressure 被延后

你需要在 `SurfaceFlinger::commit()` 中抓住这一段主线：
- `flushTransactions = clearTransactionFlags(eTransactionFlushNeeded)`
- `updates = flushLifecycleUpdates()`
- `updateLayerSnapshotsLegacy/updateLayerSnapshots(...)` 依据 flush 的 updates 做状态落地
见： [SurfaceFlinger::commit](file:///Volumes/disk2/sdk/amlogic_s905d5/frameworks/native/services/surfaceflinger/SurfaceFlinger.cpp#L2465-L2507)

### 7.4 TransactionState：你需要真正读懂的“事务载体”
在 SF 内部，事务最终会落到 `TransactionState` 结构上（它是从 client 侧 LayerState/DisplayState 扩展而来，并补齐了 server 侧需要的字段）。

建议直接从定义入手通读： [TransactionState.h](file:///Volumes/disk2/sdk/amlogic_s905d5/frameworks/native/services/surfaceflinger/TransactionState.h#L37-L148)

需要重点掌握的字段与语义：
- `applyToken`：事务排序/隔离的关键。同一 applyToken 内顺序保证，不同 applyToken 之间只保证“最终一致”，不保证全局严格顺序。
- `desiredPresentTime` 与 `isAutoTimestamp`：表达“希望对齐哪个时间点呈现”，是 ready 过滤的重要输入。
- `states`（`std::vector<ResolvedComposerState>`）：每个元素对应一个 layer 的变更，包含几何、可见性、buffer 变更、帧率投票等。
- `displays`：display 级别变更集合。
- `listenerCallbacks`：事务回调载体（OnCommit/OnComplete/Present 等），与 postComposition 的 fence 分发强相关。
- `uncacheBufferIds`：提示释放/回收缓存资源（常见于 ClientCache 与 buffer cache 变化）。

### 7.5 ResolvedComposerState：buffer 变更如何被解析为“可合成对象”
`ResolvedComposerState` 继承自 `ComposerState`，并补充了 `externalTexture` 等字段： [TransactionState.h](file:///Volumes/disk2/sdk/amlogic_s905d5/frameworks/native/services/surfaceflinger/TransactionState.h#L37-L47)

要点：
- client 提交的“buffer”最终需要被解析为 RenderEngine/CompositionEngine 可消费的 `renderengine::ExternalTexture`
- 只有同时满足“buffer changes + externalTexture 已解析 + 目标 surface 存在”的状态，才会被当作有效的 buffer 更新处理

定位 buffer 类问题时很有用的辅助遍历：
- `traverseStatesWithBuffers(...)`：只访问包含 buffer 变更且已解析的 layer state
- `traverseStatesWithBuffersWhileTrue(...)`：允许在遍历中删除 state（用于过滤无效 buffer 或异常路径）
见： [TransactionState.h](file:///Volumes/disk2/sdk/amlogic_s905d5/frameworks/native/services/surfaceflinger/TransactionState.h#L78-L112)

---

## 8. 帧循环主线：configure / commit / composite / postComposition
这一章是整份讲义的“主干”。建议你把以下四个函数当作“SF 的帧循环四步法”，逐行读懂，并能在脑中把它们串起来：
- `configure()`：处理 display hotplug/模式等配置变化
- `commit()`：应用事务、latch buffer、选择刷新率、更新输入信息，返回“是否需要合成”
- `composite()`：构造 CompositionRefreshArgs 并驱动 CompositionEngine present
- `postComposition()`：收集 present/release fence、回调、统计、资源回收、下一帧调度

对应源码：
- configure： [SurfaceFlinger::configure](file:///Volumes/disk2/sdk/amlogic_s905d5/frameworks/native/services/surfaceflinger/SurfaceFlinger.cpp#L2203-L2208)
- commit： [SurfaceFlinger::commit](file:///Volumes/disk2/sdk/amlogic_s905d5/frameworks/native/services/surfaceflinger/SurfaceFlinger.cpp#L2377-L2531)
- composite： [SurfaceFlinger::composite](file:///Volumes/disk2/sdk/amlogic_s905d5/frameworks/native/services/surfaceflinger/SurfaceFlinger.cpp#L2533-L2772)
- postComposition： [SurfaceFlinger::postComposition](file:///Volumes/disk2/sdk/amlogic_s905d5/frameworks/native/services/surfaceflinger/SurfaceFlinger.cpp#L2856-L3087)

### 8.1 configure：为什么看起来这么短
configure 表面上只是调用 `configureLocked()` 并设置 transaction flag。原因是：
- display 配置变化最终要在 commit/composite 周期内“收敛到一帧可呈现的状态”
- 任何涉及 Layer/Display 状态树的变化，都需要在统一的锁与线程上下文下完成

### 8.2 commit：这帧“要不要合成”的判定在哪里
commit 的返回值就是 “needs composite”。你需要能回答：它为什么会返回 false？
常见原因：
- mode set pending 需要等待 fence，commit 直接 scheduleFrame 等下一次： [SurfaceFlinger::commit](file:///Volumes/disk2/sdk/amlogic_s905d5/frameworks/native/services/surfaceflinger/SurfaceFlinger.cpp#L2393-L2407)
- backpressure 条件触发，先 commit 不 composite： [SurfaceFlinger::commit](file:///Volumes/disk2/sdk/amlogic_s905d5/frameworks/native/services/surfaceflinger/SurfaceFlinger.cpp#L2425-L2431)
- boot 阶段还在 BOOTLOADER（为了避免无意义的渲染）： [SurfaceFlinger::commit 返回处](file:///Volumes/disk2/sdk/amlogic_s905d5/frameworks/native/services/surfaceflinger/SurfaceFlinger.cpp#L2528-L2531)

同时，commit 内部做了许多“下一帧必须的信息更新”：
- 选择刷新率：`mScheduler->chooseRefreshRateForContent()`： [SurfaceFlinger::commit](file:///Volumes/disk2/sdk/amlogic_s905d5/frameworks/native/services/surfaceflinger/SurfaceFlinger.cpp#L2514-L2517)
- updateCursorAsync、updateInputFlinger（输入系统依赖窗口信息的更新）

### 8.3 composite：把 LayerSnapshot/LayerFE 塞进 CompositionEngine
composite 的核心是构造 `compositionengine::CompositionRefreshArgs refreshArgs`：
- 填 outputs（物理 + 虚拟显示）与颜色/变换/调试选项
- 填 layersWithQueuedFrames（用于策略/追踪等）
- 计算 earliestPresentTime / expectedPresentTime 等与 HWC 交互的时间预算
- 把 snapshot 移交给 CompositionEngine（moveSnapshotsToCompositionArgs / moveSnapshotsFromCompositionArgs）

关键调用点： [SurfaceFlinger::composite present](file:///Volumes/disk2/sdk/amlogic_s905d5/frameworks/native/services/surfaceflinger/SurfaceFlinger.cpp#L2644-L2668)

这段代码背后的设计意图：
- SF 负责“生成这一帧的合成输入”
- CompositionEngine 负责“如何合成并呈现”（GPU/HWC 细节在那边）

### 8.4 postComposition：fence、回调与资源回收的总汇
postComposition 很长，但你应该能分块理解：
- present fences 与 GPU composition fences 采集并写入 FrameTargeter/FrameTimeline： [postComposition 开头](file:///Volumes/disk2/sdk/amlogic_s905d5/frameworks/native/services/surfaceflinger/SurfaceFlinger.cpp#L2862-L2906)
- Layer 的 onPostComposition / releasePendingBuffer： [postComposition layer 处理](file:///Volumes/disk2/sdk/amlogic_s905d5/frameworks/native/services/surfaceflinger/SurfaceFlinger.cpp#L2935-L2955)
- TimeStats 更新、present fence 注入 Scheduler： [postComposition 统计/注入](file:///Volumes/disk2/sdk/amlogic_s905d5/frameworks/native/services/surfaceflinger/SurfaceFlinger.cpp#L3019-L3030)
- RenderEngine cleanup、纹理池维护： [postComposition cleanup/texture pool](file:///Volumes/disk2/sdk/amlogic_s905d5/frameworks/native/services/surfaceflinger/SurfaceFlinger.cpp#L3045-L3068)

练习：
- 用“输入/输出”语言复述 postComposition：输入是什么？输出是什么？

---

## 9. 合成路径与 HWC：validate/present、Client Target、fence 语义
这一章目标：你能在遇到“黑屏/花屏/撕裂/延迟/卡顿/掉帧”时，把问题快速归类到：
- buffer 没来（producer/BufferQueue）
- latch 等待（acquire fence）
- HWC validate/present 问题（composer HAL）
- GPU 合成慢（RenderEngine）
- 同步链路异常（present fence/release fence）

### 9.1 HWC 抽象：HWComposer
SF 通过 `HWComposer` 与 HAL 交互。它既封装了“能力查询”，也封装了“每帧交互协议”的关键方法：
- 设备请求的 composition change：`getDeviceCompositionChanges(...)`
- 设置 client target：`setClientTarget(...)`
- present 并取 release fences：`presentAndGetReleaseFences(...)`
见： [HWComposer.h](file:///Volumes/disk2/sdk/amlogic_s905d5/frameworks/native/services/surfaceflinger/DisplayHardware/HWComposer.h#L137-L156)

理解这些方法需要你同时掌握两套合成模式：
- Device composition（HWC 合成）：layer 直接给 HWC，由硬件合成输出
- Client composition（GPU 合成）：SF/RenderEngine 把多个 layer 画到一个 client target buffer，再交给 HWC 扫出

### 9.2 fence 语义：acquire / release / present
这是系统级排障必须掌握的概念：
- acquire fence：producer 写完 buffer 的信号；SF latch 前要等待它
- release fence：consumer（通常是 HWC）用完 buffer 的信号；producer 复用 buffer 前要等待它
- present fence：display/hwc 提交/扫描出完成的信号；用于帧时序统计与同步推导

在 SF 的帧循环中：
- latchBuffers 读取/等待 acquire fence（Layer::latchBuffer 系列）
- postComposition 分发 release/present fence（Layer::onLayerDisplayed、releasePendingBuffer）

可以从 `latchBuffers()` 的注释看出“为什么 latch 的顺序与集合必须稳定”： [SurfaceFlinger::latchBuffers](file:///Volumes/disk2/sdk/amlogic_s905d5/frameworks/native/services/surfaceflinger/SurfaceFlinger.cpp#L4139-L4157)

### 9.3 HWC 的“每帧握手”你应该如何理解（简化版）
不同 Android 版本、不同 HWC 代际实现细节会不同，但你可以用一个稳定的抽象理解每帧流程：
- SF/CompositionEngine 把本帧每个 layer 的 composition state 填给 HWC（buffer/transform/alpha/crop/hdr metadata 等）
- 调用 HWC 的 validate/getDeviceCompositionChanges
  - HWC 可能要求一些 layer 从 client composition 切到 device composition 或反之
  - HWC 可能提出 displayRequests/layerRequests（例如 client target property/overlay 要求等）
  - 对应抽象结构：`HWComposer::DeviceRequestedChanges`： [HWComposer.h](file:///Volumes/disk2/sdk/amlogic_s905d5/frameworks/native/services/surfaceflinger/DisplayHardware/HWComposer.h#L82-L95)
- 如果有 client composition：
  - SF/RenderEngine 先把 client target 画好
  - 再 `setClientTarget(...)` 交给 HWC
- 调用 present 并取回 present fence 与 layer release fences

排障建议：
- 只有部分 layer 异常：优先怀疑 layerRequests/changedTypes 或该 layer buffer/fence
- 全屏异常但截图正常：优先怀疑 present/扫描出链路（HWC/display）

### 9.4 CompositionEngine 的职责边界
SF 在 composite 中只做“参数准备 + 调用 present”，核心调用是：
- `mCompositionEngine->present(refreshArgs);`： [SurfaceFlinger::composite](file:///Volumes/disk2/sdk/amlogic_s905d5/frameworks/native/services/surfaceflinger/SurfaceFlinger.cpp#L2663-L2667)

CompositionEngine 的接口定义强调了它的边界： [CompositionEngine.h](file:///Volumes/disk2/sdk/amlogic_s905d5/frameworks/native/services/surfaceflinger/CompositionEngine/include/compositionengine/CompositionEngine.h#L42-L80)
- preComposition / present / updateCursorAsync
- setHwComposer / setRenderEngine / setTimeStats
- needsAnotherUpdate（合成后可能要求再来一帧）

---

## 10. 刷新率与 VSYNC：Scheduler、VsyncSchedule、RefreshRateSelector
这一章目标：你能解释“为什么有时 60Hz、有时 90/120Hz、有时又被限制”，并能在代码里找到决策点。

### 10.1 Scheduler：把 SF 的帧循环变成“可控的时序系统”
Scheduler 不是简单的 VSYNC 分发器，它把以下概念统一起来：
- 目标呈现时间（expectedPresentTime）
- 工作预算（workDuration/readyDuration）
- 多显示器的 pacesetter（节拍基准）
- resync 与 vsync period 变化的处理
- content detection 与刷新率选择（LayerHistory + RefreshRateSelector）

Scheduler 的接口你至少要熟悉这些：
- `run()`：主循环
- `schedule(...)`：把任务投递到 SF 主线程
- `createEventThread(...)`：创建 VSYNC 事件源（Render/LastComposite 两种周期）
- `chooseRefreshRateForContent()`：基于层历史选择刷新率
见： [Scheduler.h](file:///Volumes/disk2/sdk/amlogic_s905d5/frameworks/native/services/surfaceflinger/Scheduler/Scheduler.h#L121-L238)

### 10.2 initScheduler：features 与 vsync config 的组合
`SurfaceFlinger::initScheduler` 把多个开关组合为 features，并创建 Scheduler： [initScheduler](file:///Volumes/disk2/sdk/amlogic_s905d5/frameworks/native/services/surfaceflinger/SurfaceFlinger.cpp#L3987-L4049)

你应该注意：
- present fence 可靠性会影响 feature（`Capability::PRESENT_FENCE_IS_NOT_RELIABLE`）
- Kernel idle timer、GPU backpressure 等也作为 feature 影响调度策略
- 创建两条 EventThread 的 work/ready duration 参数不同（对应 late config 与 refresh period）

### 10.3 FrameTarget/FrameTargeter：把时间从“测量”变成“可用的决策依据”
在 commit/composite 中你会看到 `frameTargets` / `frameTargeters`，它们把“当前帧是否 pending、是否 miss、expectedPresentTime 等”串起来：
- commit 根据 FrameTarget 判断是否等待 mode set、是否 backpressure
- composite 根据 FrameTarget 判断是否需要 earliestPresentTime
- postComposition 把 present fence 写回 targeter，供下一帧调度推导

入口就在 `ICompositor::commit/composite` 的参数： [ICompositor.h](file:///Volumes/disk2/sdk/amlogic_s905d5/frameworks/native/services/surfaceflinger/Scheduler/include/scheduler/interface/ICompositor.h#L37-L49)

### 10.4 RefreshRateSelector：从 layer “投票”到 display “选择”
要理解刷新率选择，你必须理解两类输入：
- 显式输入：系统策略（policy）、用户设置、应用 setFrameRate、WindowManager 约束
- 隐式输入：内容检测（LayerHistory 统计 buffer 更新频率、可见区域等）

SF 在 commit 中调用： [chooseRefreshRateForContent](file:///Volumes/disk2/sdk/amlogic_s905d5/frameworks/native/services/surfaceflinger/SurfaceFlinger.cpp#L2514-L2517)

当你遇到“刷新率不对”的 bug，第一步应先回答：
- 是投票不对（layer frame rate vote/priority）？
- 是 policy 限制（low power/peak refresh rate）？
- 是 content detection 误判（例如小面积视频、小 dirty region）？
- 是 mode set 被延后（pending timeline/fence）？

---

## 11. 性能与功耗：TimeStats、FrameTimeline、PowerAdvisor
这一章目标：你能把“用户感知的卡顿/掉帧/延迟”映射到 SF 的统计与时序链路上。

### 11.1 TimeStats：SF 的帧级指标与累计统计
在 composite/postComposition 中多处更新 TimeStats，例如：
- miss frame 计数： [commit missed frame](file:///Volumes/disk2/sdk/amlogic_s905d5/frameworks/native/services/surfaceflinger/SurfaceFlinger.cpp#L2384-L2387)
- frame duration 记录： [composite recordFrameDuration](file:///Volumes/disk2/sdk/amlogic_s905d5/frameworks/native/services/surfaceflinger/SurfaceFlinger.cpp#L2682-L2683)
- total frames 与 present fence： [postComposition TimeStats](file:///Volumes/disk2/sdk/amlogic_s905d5/frameworks/native/services/surfaceflinger/SurfaceFlinger.cpp#L3019-L3021)

你应该能回答：
- missed frame 是如何定义的？在什么条件下递增？
- frame duration 的起点/终点分别是什么？
- present fence 不可靠会如何影响统计与调度（feature flags）？

### 11.2 FrameTimeline：把“这一帧”的关键时间点串起来
FrameTimeline 相关调用在 commit/postComposition：
- wake up 时间： [commit setSfWakeUp](file:///Volumes/disk2/sdk/amlogic_s905d5/frameworks/native/services/surfaceflinger/SurfaceFlinger.cpp#L2468-L2471)
- present 时间与 fence： [postComposition setSfPresent](file:///Volumes/disk2/sdk/amlogic_s905d5/frameworks/native/services/surfaceflinger/SurfaceFlinger.cpp#L2901-L2906)

理解 FrameTimeline 的意义：
- 它是连接应用侧 frame timeline（Choreographer/SurfaceFlinger）与系统统计（jank）的一条重要链路
- 对很多“看似随机的卡顿”定位非常关键

### 11.3 PowerAdvisor：工作预算与 hint session
commit/composite 中对 PowerAdvisor 的调用体现了“把时序决策反馈给功耗系统”的设计：
- commit start、expectedPresentTime、frameDelay、targetWorkDuration： [commit power hint](file:///Volumes/disk2/sdk/amlogic_s905d5/frameworks/native/services/surfaceflinger/SurfaceFlinger.cpp#L2438-L2456)
- composite end 与 present timing： [composite/postComposition power hint](file:///Volumes/disk2/sdk/amlogic_s905d5/frameworks/native/services/surfaceflinger/SurfaceFlinger.cpp#L2684-L2694)

当你在厂商平台做性能功耗调优，这部分经常要联动修改（但必须极其谨慎，容易引入抖动）。

---

## 12. 捕获与可观测性：ScreenCapture、LayerTracing、TransactionTracing、FrameTracer
这一章目标：你能选择合适的“可观测性工具”来验证你的猜想，而不是盲改。

### 12.1 ScreenCapture：为什么它不是简单的“截个图”
SF 内部的屏幕捕获通常需要：
- 决定 RenderArea（显示/某个 layer stack/某个 layer subtree）
- 决定是否允许 protected content
- 决定颜色空间/变换/裁剪
- 与 RenderEngine/CompositionEngine 协同渲染到目标 buffer

入口在 `captureScreenCommon/renderScreenImpl` 等（声明在头文件中）： [SurfaceFlinger.h 截图相关](file:///Volumes/disk2/sdk/amlogic_s905d5/frameworks/native/services/surfaceflinger/SurfaceFlinger.h#L852-L864)

常见排障使用场景：
- “屏幕显示异常但截图正常”：倾向 HWC/硬件链路
- “屏幕与截图都异常”：倾向 layer 内容/RenderEngine/合成输入

### 12.2 LayerTracing：抓取层级与合成快照
LayerTracing 可在 commit 或 composite 阶段记录（取决于 flag）：
- commit 阶段： [commit addToLayerTracing 条件](file:///Volumes/disk2/sdk/amlogic_s905d5/frameworks/native/services/surfaceflinger/SurfaceFlinger.cpp#L2522-L2525)
- composite 阶段： [composite addToLayerTracing](file:///Volumes/disk2/sdk/amlogic_s905d5/frameworks/native/services/surfaceflinger/SurfaceFlinger.cpp#L2746-L2749)

你需要理解：
- tracing 可能阻塞（代码中明确提示），只能用于调试
- 记录点不同会影响你看到的“状态到底是哪一阶段的”

### 12.3 TransactionTracing：把事务按 VSYNC 序列化落盘
init 中设置 writer function： [TransactionTraceWriter setup](file:///Volumes/disk2/sdk/amlogic_s905d5/frameworks/native/services/surfaceflinger/SurfaceFlinger.cpp#L957-L976)

commit 中把已 flush 的 updates 记录： [commit addCommittedTransactions](file:///Volumes/disk2/sdk/amlogic_s905d5/frameworks/native/services/surfaceflinger/SurfaceFlinger.cpp#L2474-L2483)

非常适合定位：
- 某个窗口属性（alpha/transform/z）“哪一次事务改坏了”
- desiredPresentTime 导致的事务延迟/乱序（跨 applyToken）

### 12.4 FrameTracer / FrameTimeline：从 SF 侧看每帧的关键点
bootFinished 中初始化 FrameTracer/FrameTimeline： [bootFinished initialize](file:///Volumes/disk2/sdk/amlogic_s905d5/frameworks/native/services/surfaceflinger/SurfaceFlinger.cpp#L712-L714)

这套工具对“性能抖动、偶现掉帧”尤其有用。

---

## 13. 常见问题定位清单：黑屏/卡顿/掉帧/撕裂/刷新率异常/色彩异常
这一章给你一个“遇到问题先沿着什么路径排”的清单。它不会替代具体分析，但能显著减少瞎猜成本。

### 13.1 黑屏/无画面
优先检查链路分层：
- layer 是否存在、是否可见、是否在正确的 layer stack
- buffer 是否被 latch（看 acquire fence、hasReadyFrame）
- 合成是否发生（commit 是否返回 mustComposite；是否被 boot stage 拦截）
- HWC 是否连接/是否拒绝 present（查看 HWC logs、capability、mode pending）

源码提示点：
- boot 阶段不合成： [commit 返回条件](file:///Volumes/disk2/sdk/amlogic_s905d5/frameworks/native/services/surfaceflinger/SurfaceFlinger.cpp#L2528-L2531)
- display 是否 connected：init 中对 primary display 有 fatal 检查： [init primary display checks](file:///Volumes/disk2/sdk/amlogic_s905d5/frameworks/native/services/surfaceflinger/SurfaceFlinger.cpp#L910-L913)

### 13.2 掉帧/卡顿
把掉帧拆成三段看：
- commit 慢（事务多、锁竞争、layer snapshot 构建慢）
- composite 慢（GPU 画不完、HWC validate/present 慢）
- 等 fence（acquire/present fence 等待导致时间预算被吃掉）

直接可用的信号：
- `didMissFrame()`： [commit missed frame](file:///Volumes/disk2/sdk/amlogic_s905d5/frameworks/native/services/surfaceflinger/SurfaceFlinger.cpp#L2384-L2387)
- present latency tracker： [postComposition presentLatency](file:///Volumes/disk2/sdk/amlogic_s905d5/frameworks/native/services/surfaceflinger/SurfaceFlinger.cpp#L2910-L2916)

### 13.3 撕裂/帧不同步
常见原因：
- VSYNC 失配（硬件 vsync 关闭/启用时机问题）
- present fence 不可靠导致调度推导错误
- 某些 vendor 平台的 HWC 实现不满足预期（尤其是 expectedPresentTime 支持与否）

可以关注：
- 是否支持 ExpectedPresentTime： [composite optional feature check](file:///Volumes/disk2/sdk/amlogic_s905d5/frameworks/native/services/surfaceflinger/SurfaceFlinger.cpp#L2644-L2652)
- 是否启用硬件 vsync： [postComposition enableHardwareVsync](file:///Volumes/disk2/sdk/amlogic_s905d5/frameworks/native/services/surfaceflinger/SurfaceFlinger.cpp#L3035-L3039)

### 13.4 刷新率异常（锁死 60、频繁跳变、视频不跟随）
按决策链分层排：
- layer 的 frame rate vote 是否正确上报
- RefreshRateSelector policy 是否限制（peak/min、content type）
- content detection 是否误判（小面积视频、dirty region）
- mode set 是否一直 pending（timeline fence 不触发）

切入点：
- commit 里选择刷新率： [chooseRefreshRateForContent](file:///Volumes/disk2/sdk/amlogic_s905d5/frameworks/native/services/surfaceflinger/SurfaceFlinger.cpp#L2514-L2517)
- initScheduler 里 feature flags（content detection 等）： [initScheduler features](file:///Volumes/disk2/sdk/amlogic_s905d5/frameworks/native/services/surfaceflinger/SurfaceFlinger.cpp#L4001-L4019)

### 13.5 色彩/HDR/亮度异常
关注点：
- outputColorSetting（是否启用 color management）： [composite outputColorSetting](file:///Volumes/disk2/sdk/amlogic_s905d5/frameworks/native/services/surfaceflinger/SurfaceFlinger.cpp#L2620-L2623)
- HDR layer 判定逻辑： [isHdrLayer](file:///Volumes/disk2/sdk/amlogic_s905d5/frameworks/native/services/surfaceflinger/SurfaceFlinger.cpp#L2789-L2817)
- HWC HDR capabilities/metadata 支持（在 HWComposer 接口中）

---

## 14. 训练题：把知识变成“能独立排障与改代码”的能力
建议按顺序做，每题都要求你给出：
1) 你认为的根因假设
2) 你要用什么证据验证（trace/log/dumpsys/代码断点）
3) 如果验证失败，你的下一步分支是什么

### 14.1 阅读题：把帧循环画成你的版本
任务：
- 用你自己的语言复述一帧：从 `Scheduler` 触发到 `postComposition` 结束发生了什么。
- 要求你标注出“锁的边界”和“跨线程 schedule 的位置”。

参考函数：
- [SurfaceFlinger::commit](file:///Volumes/disk2/sdk/amlogic_s905d5/frameworks/native/services/surfaceflinger/SurfaceFlinger.cpp#L2377-L2531)
- [SurfaceFlinger::composite](file:///Volumes/disk2/sdk/amlogic_s905d5/frameworks/native/services/surfaceflinger/SurfaceFlinger.cpp#L2533-L2772)
- [SurfaceFlinger::postComposition](file:///Volumes/disk2/sdk/amlogic_s905d5/frameworks/native/services/surfaceflinger/SurfaceFlinger.cpp#L2856-L3087)

### 14.2 定位题：事务为什么“延后生效”
现象：
- 某个窗口 setPosition/setAlpha 后，偶现要到下一帧或更久才生效。
要求：
- 从 applyToken、desiredPresentTime、transaction ready filters 的角度解释可能原因。
- 指出你会从哪几个函数入手打点/抓 trace。

### 14.3 性能题：commit 慢还是 composite 慢
现象：
- 高频小窗口动画掉帧。
要求：
- 给出你判断 commit vs composite 的证据链（TimeStats/FrameTimeline/trace 点位）。
- 给出你认为最可能的 3 类根因，并说明对应验证手段。

### 14.4 工程题：增加一个“轻量统计点”（不改行为）
任务（只做思考，不建议直接在量产产品里乱加）：
- 选择一个你认为对调试最关键的位置（例如 commit 开始/结束，composite 开始/结束，present fence 信号时间）。
- 设计一个不会引入明显开销的统计方案（例如把计数/直方图汇总到 dumpsys 中）。
- 说明如何避免 binder 线程影响主线程（schedule/锁的策略）。

---

## 附：建议的源码阅读路线（按收益排序）
1) 入口与初始化： [main_surfaceflinger.cpp](file:///Volumes/disk2/sdk/amlogic_s905d5/frameworks/native/services/surfaceflinger/main_surfaceflinger.cpp) → [SurfaceFlinger::init](file:///Volumes/disk2/sdk/amlogic_s905d5/frameworks/native/services/surfaceflinger/SurfaceFlinger.cpp#L830-L978)
2) 帧循环主线：configure/commit/composite/postComposition（第 8 章四个函数链接）
3) FrontEnd：先读 [FrontEnd/readme.md](file:///Volumes/disk2/sdk/amlogic_s905d5/frameworks/native/services/surfaceflinger/FrontEnd/readme.md) 再看 `updateLayerSnapshots`
4) Scheduler：从 [Scheduler.h](file:///Volumes/disk2/sdk/amlogic_s905d5/frameworks/native/services/surfaceflinger/Scheduler/Scheduler.h) 入手，沿着 `commit/composite` 的参数链往下追
5) HWC：从 [HWComposer.h](file:///Volumes/disk2/sdk/amlogic_s905d5/frameworks/native/services/surfaceflinger/DisplayHardware/HWComposer.h) 开始，结合你的平台 HAL 实现对照

---

## 附2：实战调试命令与开关清单
本节不依赖特定平台实现，但命令是否可用会随 Android 版本/编译选项而变化。遇到命令不可用时，以 `dumpsys SurfaceFlinger --help`（或 `-h`）输出为准。

### A2.1 dumpsys：第一时间获取“现场快照”
常用命令（按排障收益排序）：

```bash
adb shell dumpsys SurfaceFlinger
adb shell dumpsys SurfaceFlinger --proto > sf.pb
adb shell dumpsys SurfaceFlinger --list
adb shell dumpsys SurfaceFlinger --displays
adb shell dumpsys SurfaceFlinger --layers
adb shell dumpsys SurfaceFlinger --latency
```

建议的使用策略：
- 先 `--displays/--layers` 证伪“layer/display 根本不存在或不可见”
- 再结合 `--latency`/proto 输出定位“慢在 commit 还是 composite/postComposition”

### A2.2 属性（sysprop/property）：验证假设的快速手段
不同版本可用属性不同，以下以“思路”为主：

```bash
adb shell getprop debug.sf.*
adb shell getprop persist.debug.sf.*
adb shell getprop sf.debug.*
```

与本仓库代码直接相关的一些开关线索：
- 事务 tracing：`debug.sf.enable_transaction_tracing`（非 user build 更常用，见 SF 构造逻辑）
- 刷新率 overlay：`sf.debug.show_refresh_rate_overlay`（bootFinished 中读取并启用）
- RenderEngine backend：`debug.renderengine.backend`（见 chooseRenderEngineTypeViaSysProp）
- HAL 虚拟显示：`debug.sf.enable_hwc_vds`

### A2.3 trace/atrace：定位“慢在谁身上”

```bash
adb shell atrace --list_categories
adb shell atrace gfx view sched freq idle am wm -b 16384 -t 10 > trace.html
```

建议的读 trace 方法：
- 先把每帧分成 commit 与 composite 两段，看哪段长
- 再下钻到 HWC present、GPU queue、binder call、锁竞争（mutex contention）
