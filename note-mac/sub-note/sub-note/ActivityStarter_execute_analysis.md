# ActivityStarter.execute() 启动Activity流程分析

> 文件：`frameworks/base/services/core/java/com/android/server/wm/ActivityStarter.java`
> Android 版本：AOSP (amlogic_s905d5)

## 调用链概览

```
execute() → executeRequest() → startActivityUnchecked() → startActivityInner()
```

---

## 一、`execute()` (line 704)

这是 Activity 启动的入口方法，主要做**前期准备和后处理**：

1. **`onExecutionStarted()`** — 标记执行开始，防止同一个 `ActivityStarter` 被重复使用。

2. **检查 Intent 中的文件描述符** — 如果 Intent 携带了文件描述符，直接抛出 `IllegalArgumentException`，防止 FD 泄漏。

3. **通知 ActivityMetricsLogger** — 记录启动状态，用于性能指标统计（冷启动/热启动耗时等）。

4. **解析 Activity 信息** (`mRequest.resolveActivity()`) — 如果调用方还没解析 `activityInfo`（即还不知道要启动哪个组件），通过 PackageManager 解析 Intent 对应的 Activity。

5. **关机/重启检查点记录** — 如果 Intent 是关机或重启操作（`ACTION_REQUEST_SHUTDOWN` / `ACTION_SHUTDOWN` / `ACTION_REBOOT`），记录检查点用于事后排查。

6. **进入核心同步块**（持有 `mService.mGlobalLock`）：
   - 检查全局配置变更
   - **`resolveToHeavyWeightSwitcherIfNeeded()`** — 处理重型进程切换：如果当前已有重型进程在运行，且要启动的是另一个不同的重型进程，替换 Intent 为重型进程切换 Activity
   - **双屏显示配置覆盖** — Amlogic 定制：检查是否需要将应用启动到指定显示器
   - **`executeRequest(mRequest)`** — **核心执行逻辑**
   - 处理全局配置变更
   - 通知 `ActivityMetricsLogger` 启动完成
   - **`waitResultIfNeeded()`** — 如果需要同步等待结果，阻塞等待 Activity 可见

7. **`onExecutionComplete()` (finally)** — 回收 `ActivityStarter` 实例，放回对象池复用。

---

## 二、`executeRequest()` (line 1046)

这是预检查阶段，主要做**权限和合法性校验**：

1. **记录启动原因和时间戳**

2. **获取调用者信息** — 解析 `callerApp`、`callingPid`、`callingUid` 等身份信息

3. **处理 `resultTo`** — 如果调用方需要接收 Activity 的返回结果，建立 `sourceRecord`/`resultRecord` 关系

4. **`FLAG_ACTIVITY_FORWARD_RESULT` 处理** — 转发结果目标，用于 Intent 选择器等跳板场景

5. **基本合法性检查：**
   - Intent 的 Component 是否为空 → `START_INTENT_NOT_RESOLVED`
   - ActivityInfo 是否为空 → `START_CLASS_NOT_FOUND`

6. **Voice Session 兼容性检查** — 确保 Activity 支持语音交互类别

7. **权限和安全检查：**
   - `checkStartAnyActivityPermission()` — 启动权限检查
   - `IntentFirewall.checkStartActivity()` — Intent 防火墙过滤
   - `PermissionPolicyInternal.checkStartActivity()` — 权限策略检查

8. **后台启动检查** (`BackgroundActivityStartController`) — 从 Android 10+ 开始，后台应用不能随意启动 Activity，返回 `balCode` 表示是否被阻止

9. **拦截器处理** (`ActivityStartInterceptor`) — 可能拦截并替换启动目标（如静默模式、应用挂起等场景）

10. **权限审查** — 如果需要权限审查，替换为权限审查 Activity

11. **Instant App 处理** — 如果是免安装应用，替换为 Instant App 安装器

12. **构建 `ActivityRecord`** — 使用 `ActivityRecord.Builder` 创建代表新 Activity 的 `ActivityRecord`

13. **调用 `startActivityUnchecked()`** — 进入下一阶段

---

## 三、`startActivityUnchecked()` (line 1619)

这是**过渡与窗口管理阶段**：

1. **创建 Transition** — 如果 Shell 端过渡动画已启用，创建 `TRANSIT_OPEN` 过渡并开始收集变化

2. **`deferWindowLayout()` / `continueWindowLayout()`** — 延迟窗口布局，在整个启动流程结束后统一执行，避免中间状态的布局闪烁

3. **调用 `startActivityInner()`** — 核心逻辑

4. **`handleStartResult()`** — 处理后启动结果：
   - **启动失败**：清理残留的 Activity/Task，中止 Transition
   - **启动成功**：配置 `setAlwaysOnTop`、更新 display 配置、处理通知栏收起、请求 Transition 播放

5. **`postStartActivityProcessing()`** — 通知任务变更监听器，处理 `START_TASK_TO_FRONT` / `START_DELIVERED_TO_TOP` 等结果

---

## 四、`startActivityInner()` (line 1785)

这是**真正执行 Activity 启动的核心逻辑**：

1. **`setInitialState()`** — 初始化启动状态，设置启动参数

2. **`computeLaunchingTaskFlags()`** — 计算启动标志位（如 `FLAG_ACTIVITY_NEW_TASK` 等）

3. **`getReusableTask()`** — 查找可复用的已有 Task（如 `singleTask`/`singleInstance` 模式复用）

4. **`computeTargetTask()`** — 确定目标 Task，决定是新建还是加入已有

5. **`computeLaunchParams()`** — 计算启动参数（窗口模式、display 区域等）

6. **`isAllowedToStart()`** — 最终检查是否允许启动

7. **`recycleTask()`** — 如果复用已有 Task，清理 Task 上已有的 Activity（如 `clearTop` 逻辑）

8. **`deliverToCurrentTopIfNeeded()`** — 如果栈顶已经是目标 Activity，直接走 `onNewIntent()` 而不创建新的

9. **`getOrCreateRootTask()` / `setNewTask()` / `addOrReparentStartingActivity()`** — 创建或复用 Task，将 Activity 放入对应 Task

10. **`moveToFront()`** — 将目标 Task 移到最前面

11. **URI 权限授予** — 处理 Intent 中的 URI 读写权限

12. **`mTargetRootTask.startActivityLocked()`** — **真正将 Activity 添加到 Task 栈**

13. **`resumeFocusedTasksTopActivities()`** — 如果 `mDoResume` 为 true，恢复顶层 Activity 的焦点，触发 Activity 的生命周期回调（`onResume` 等）

14. **更新最近任务列表** — `mSupervisor.mRecentTasks.add(startedTask)`

15. **PiP 处理** — 如果是启动到画中画模式，移动 Activity 到 Pinned Task

---

## 整体流程图

```
execute()
 ├── 解析 Activity 信息
 ├── Heavy-weight process 处理
 ├── executeRequest()
 │    ├── 调用者身份识别
 │    ├── 权限/安全/防火墙检查
 │    ├── 后台启动检查
 │    ├── 拦截器 & 权限审查 & Instant App
 │    ├── 构建 ActivityRecord
 │    └── startActivityUnchecked()
 │         ├── 创建 Transition / 延迟布局
 │         ├── startActivityInner()
 │         │    ├── 计算启动标志 & Task 选择
 │         │    ├── 复用已有 Task 或创建新 Task
 │         │    ├── 将 Activity 加入 Task 栈
 │         │    └── resumeFocusedTasksTopActivities()
 │         ├── handleStartResult()
 │         └── postStartActivityProcessing()
 ├── 通知 ActivityMetricsLogger
 └── waitResultIfNeeded()
```

---

## 总结

这是一个典型的从 Intent 解析到最终 Activity 被创建/恢复的完整链路：

- **`execute`** 负责前期准备和事后清理，是整体流程的编排者
- **`executeRequest`** 侧重安全校验，做权限、后台启动、防火墙等检查，最后构建 `ActivityRecord`
- **`startActivityUnchecked`** 负责任务栈管理和窗口过渡动画
- **`startActivityInner`** 是实际执行放置 Activity 和驱动生命周期的地方
