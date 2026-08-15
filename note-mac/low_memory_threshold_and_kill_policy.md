# Android（本仓库）低内存阈值与杀进程策略：位置与源码脉络

本文整理“低内存阈值”和“低内存杀进程策略”在本仓库中的定义位置、数据流与关键方法，覆盖 framework（AMS）→ lmkd →（可选）内核接口三层。

## 1. 总体结论（先给结论再看细节）

- “低内存阈值”并非单一常量，而是由 framework 侧计算出一组 **6 桶 minfree**（与 **6 桶 oom_score_adj** 对应），再通过 socket 下发给 lmkd。
- “杀进程策略”主要由 lmkd 实现：它基于 PSI/vmpressure/minfree 触发，在满足最小可杀 `oom_score_adj` 的候选集中按策略选一个或多个进程杀掉（可配置“杀最重任务”等）。
- 设备侧可以通过两类入口影响行为：
  - 属性：`ro.lmk.*`、`persist.device_config.lmkd_native.*`、`ro.config.low_ram` 等
  - 资源 overlay：覆盖 `config_lowMemoryKillerMinFreeKbytes*` / `config_extraFreeKbytes*`

## 2. 数据流（framework → lmkd）

1) framework（ActivityManager/ProcessList/OomAdjuster）根据设备内存与屏幕等因素计算 6 桶 `minfree`（kB）与 6 桶 `oom_adj`（即 `oom_score_adj`）。

2) framework 通过 `LmkdConnection` 连接 lmkd socket，下发：
- `LMK_TARGET`：6 对 `<minfree_pages, oom_score_adj>`（阈值与桶映射）
- `LMK_PROCPRIO`：每个进程的 `<pid, uid, oom_score_adj>`（进程优先级）

3) lmkd 接收并缓存阈值（并设置 `sys.lmk.minfree_levels`），同时监控 PSI/vmpressure；压力达到阈值时，按策略选择目标并 kill。

## 3. framework 层：阈值、优先级、对外“low memory”判断

### 3.1 6 桶 minfree 阈值的计算与下发

关键方法（建议从这里开始读）：
- [ProcessList.updateOomLevels()](file:///Volumes/disk2/sdk/amlogic_s905d5/frameworks/base/services/core/java/com/android/server/am/ProcessList.java#L984-L1075)
  - 根据 `mTotalMemMb` 与 `displayWidth*displayHeight` 计算 `scale`，在 `mOomMinFreeLow` 与 `mOomMinFreeHigh` 之间插值得到 `mOomMinFree[]`
  - 支持用资源强制/调整：
    - `config_lowMemoryKillerMinFreeKbytesAbsolute`
    - `config_lowMemoryKillerMinFreeKbytesAdjust`
    - `config_extraFreeKbytesAbsolute`
    - `config_extraFreeKbytesAdjust`
  - 在 `write==true` 时，通过 `LMK_TARGET` 把 6 桶阈值写给 lmkd，并设置 `sys.sysctl.extra_free_kbytes`

桶的基础值（kB）：
- [ProcessList.mOomMinFreeLow / mOomMinFreeHigh](file:///Volumes/disk2/sdk/amlogic_s905d5/frameworks/base/services/core/java/com/android/server/am/ProcessList.java#L384-L405)

阈值相关的系统资源定义（可被产品 overlay 覆盖）：
- [frameworks/base/core/res/res/values/config.xml](file:///Volumes/disk2/sdk/amlogic_s905d5/frameworks/base/core/res/res/values/config.xml#L208-L243)

### 3.2 进程“谁更容易被杀”：oom_score_adj 的设置

framework 侧会给每个进程设置 `oom_score_adj`（范围大致 -1000..+1000），并通过 lmkd socket 下发。

关键方法：
- [ProcessList.setOomAdj(pid, uid, amt)](file:///Volumes/disk2/sdk/amlogic_s905d5/frameworks/base/services/core/java/com/android/server/am/ProcessList.java#L1474-L1504)
  - 通过 `LMK_PROCPRIO` 把 `<pid, uid, oom_score_adj>` 发送给 lmkd
- [ProcessList.remove(pid)](file:///Volumes/disk2/sdk/amlogic_s905d5/frameworks/base/services/core/java/com/android/server/am/ProcessList.java#L1506-L1518)
  - 通过 `LMK_PROCREMOVE` 从 lmkd 侧移除进程记录
- [ProcessList.writeLmkd(buf, repl)](file:///Volumes/disk2/sdk/amlogic_s905d5/frameworks/base/services/core/java/com/android/server/am/ProcessList.java#L1571-L1584)
  - 统一的 socket 发送入口（依赖 `LmkdConnection` 的连接与重连）
- [OomAdjuster.java](file:///Volumes/disk2/sdk/amlogic_s905d5/frameworks/base/services/core/java/com/android/server/am/OomAdjuster.java)
  - 负责根据进程状态/组件重要性计算“应该给它什么 adj”，最终会走到 `ProcessList.setOomAdj()`

### 3.3 framework 里的“lowMemory”字段怎么算

这和 lmkd 触发 kill 不是同一条路径，但经常被拿来做 UI 或策略判断。

关键方法：
- [ProcessList.getMemLevel(adjustment)](file:///Volumes/disk2/sdk/amlogic_s905d5/frameworks/base/services/core/java/com/android/server/am/ProcessList.java#L1457-L1464)
  - 把某个 `adj` 映射回对应桶的 minfree（字节）
- [ProcessList.getMemoryInfo(outInfo)](file:///Volumes/disk2/sdk/amlogic_s905d5/frameworks/base/services/core/java/com/android/server/am/ProcessList.java#L1618-L1630)
  - `outInfo.lowMemory = availMem < (homeAppMem + (cachedAppMem-homeAppMem)/2)`

## 4. lmkd 层：触发阈值、决策策略、选谁杀

lmkd 代码位置：
- [system/memory/lmkd/lmkd.cpp](file:///Volumes/disk2/sdk/amlogic_s905d5/system/memory/lmkd/lmkd.cpp)
- lmkd 配置说明（属性含义与默认）：[system/memory/lmkd/README.md](file:///Volumes/disk2/sdk/amlogic_s905d5/system/memory/lmkd/README.md#L23-L100)

### 4.1 lmkd 的配置入口（阈值/策略开关）

核心点：lmkd 读取属性时使用 “实验属性覆盖 ro 属性” 的逻辑：
- `persist.device_config.lmkd_native.<name>` 优先于 `ro.lmk.<name>`

关键实现：
- [GET_LMK_PROPERTY 宏与说明](file:///Volumes/disk2/sdk/amlogic_s905d5/system/memory/lmkd/lmkd.cpp#L120-L129)
- [update_props()](file:///Volumes/disk2/sdk/amlogic_s905d5/system/memory/lmkd/lmkd.cpp#L3745-L3797)
  - 读取并生效：`kill_heaviest_task`、`kill_timeout_ms`、`use_minfree_levels`、`psi_partial_stall_ms`、`psi_complete_stall_ms`、`thrashing_limit` 等
  - `ro.config.low_ram` 会影响默认阈值（例如 PSI stall 默认值）

启动与“实验属性变更后 reinit”的触发点：
- [lmkd.rc](file:///Volumes/disk2/sdk/amlogic_s905d5/system/memory/lmkd/lmkd.rc#L1-L51)

### 4.2 framework 下发的 6 桶 minfree 在 lmkd 侧怎么接收

关键方法：
- [cmd_target()](file:///Volumes/disk2/sdk/amlogic_s905d5/system/memory/lmkd/lmkd.cpp#L1384-L1458)
  - 接收 `LMK_TARGET` 的 `<minfree, oom_adj_score>` 对
  - 设置 `sys.lmk.minfree_levels`（形如 `minfree:adj,minfree:adj,...`）
  - 如检测到旧 in-kernel LMK 模块，也会尝试写 `/sys/module/lowmemorykiller/parameters/minfree` 与 `/sys/module/lowmemorykiller/parameters/adj`

### 4.3 lmkd 何时认为“内存压力到了”：PSI / vmpressure / minfree 模式

PSI 相关关键方法：
- [init_psi_monitors()](file:///Volumes/disk2/sdk/amlogic_s905d5/system/memory/lmkd/lmkd.cpp#L3231-L3263)
  - 根据 `psi_partial_stall_ms / psi_complete_stall_ms` 初始化不同压力级别的监控
- [init_mp_psi(level, use_new_strategy)](file:///Volumes/disk2/sdk/amlogic_s905d5/system/memory/lmkd/lmkd.cpp#L3163-L3184)
- [mp_event_psi(...)](file:///Volumes/disk2/sdk/amlogic_s905d5/system/memory/lmkd/lmkd.cpp#L2584-L2923)
  - PSI 事件触发后会解析 PSI/meminfo 并进入“选择并 kill”流程

“minfree 兼容模式”开关：
- `ro.lmk.use_minfree_levels`（见 README 与 [update_props()](file:///Volumes/disk2/sdk/amlogic_s905d5/system/memory/lmkd/lmkd.cpp#L3765-L3770)）
  - 打开时更接近老内核 lowmemorykiller 的 “free/filecache 阈值” 行为

### 4.4 lmkd 的“选谁杀、怎么杀”的核心策略

核心方法链（强烈建议按这个链路阅读）：
- [find_and_kill_process(min_score_adj, ...)](file:///Volumes/disk2/sdk/amlogic_s905d5/system/memory/lmkd/lmkd.cpp#L2418-L2467)
  - 从 `OOM_SCORE_ADJ_MAX` 往下扫到 `min_score_adj`
  - 根据 `kill_heaviest_task` 选择 “杀最重任务” 或 “按队尾取一个”
  - 如果要杀到 perceptible（`oom_score_adj <= 200`）附近，会强制改用“杀最重任务”以减少 victim 数
- [kill_one_process(procp, min_oom_score, ...)](file:///Volumes/disk2/sdk/amlogic_s905d5/system/memory/lmkd/lmkd.cpp#L2304-L2416)
  - 读取 `/proc/<pid>/status` 校验 tgid、防 pid reuse
  - 使用 `reaper.kill(...)` 真正发起 kill
  - 记录 kill 统计、上报事件（包括 `ctrl_data_write_lmk_kill_occurred`）

策略相关关键开关（属性）：
- `ro.lmk.kill_heaviest_task`：倾向选择最重的 eligible 进程
- `ro.lmk.kill_timeout_ms`：两次 kill 之间的节流间隔
- `ro.config.low_ram`：影响默认阈值与部分策略默认值

## 5. 设备/产品层：你们的板级/产品默认值通常在哪里改

### 5.1 通过 product/vendor 属性影响 lmkd

本仓库里 Amlogic 产品已有对 lmkd 属性的设置示例：
- [device/amlogic/common/product_property.mk](file:///Volumes/disk2/sdk/amlogic_s905d5/device/amlogic/common/product_property.mk#L64-L71)
  - `ro.lmk.kill_timeout_ms=100`
  - 非 low-ram 时 `ro.lmk.kill_heaviest_task=true`
- [device/amlogic/common/core_amlogic.mk](file:///Volumes/disk2/sdk/amlogic_s905d5/device/amlogic/common/core_amlogic.mk#L937-L942)
  - `ro.lmk.use_new_strategy=true`
- `ro.config.low_ram=true` 的示例（某些产品开启）：[gxl/vendor_prop.mk](file:///Volumes/disk2/sdk/amlogic_s905d5/device/amlogic/common/products/mbox/gxl/vendor_prop.mk#L76-L80)

### 5.2 通过 overlay 资源影响 framework 侧阈值

overlay 一般在 `device/<vendor>/<product>/overlay/.../frameworks/base/core/res/.../config.xml`。

本树里可参考 overlay 路径样式（示例之一）：
- `device/amlogic/raman/overlay/frameworks/base/core/res/res/values/config.xml`

要覆盖的资源名在 framework 默认值文件中：
- [config_lowMemoryKillerMinFreeKbytesAbsolute / Adjust](file:///Volumes/disk2/sdk/amlogic_s905d5/frameworks/base/core/res/res/values/config.xml#L208-L224)
- [config_extraFreeKbytesAbsolute / Adjust](file:///Volumes/disk2/sdk/amlogic_s905d5/frameworks/base/core/res/res/values/config.xml#L226-L242)

## 6. 读代码建议（快速定位）

- 先看 framework 如何算出 6 桶：从 [ProcessList.updateOomLevels()](file:///Volumes/disk2/sdk/amlogic_s905d5/frameworks/base/services/core/java/com/android/server/am/ProcessList.java#L984-L1075) 开始。
- 再看 lmkd 如何接收与触发：从 [cmd_target()](file:///Volumes/disk2/sdk/amlogic_s905d5/system/memory/lmkd/lmkd.cpp#L1384-L1458) → [init_psi_monitors()](file:///Volumes/disk2/sdk/amlogic_s905d5/system/memory/lmkd/lmkd.cpp#L3231-L3263) → [mp_event_psi()](file:///Volumes/disk2/sdk/amlogic_s905d5/system/memory/lmkd/lmkd.cpp#L2584-L2923) → [find_and_kill_process()](file:///Volumes/disk2/sdk/amlogic_s905d5/system/memory/lmkd/lmkd.cpp#L2418-L2467)。
- 最后回到产品默认配置：查 `device/amlogic/**/vendor_prop.mk`、`product_property.mk`、`core_amlogic.mk` 里是否设置了 `ro.config.low_ram` / `ro.lmk.*`。
