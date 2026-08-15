# Bootchart 开机性能对比分析报告

## 1. 概述

对比 `cmcc-xz-bootchart-02` (v1.0.0) 和 `cmcc-xz-bootchart-03` (v1.0.1) 两份 Android 开机 bootchart 数据，分析 **cmcc-xz-bootchart-03 开机慢的主要差异**。

---

## 2. 总体开机时间对比

| 指标 | Boot02 (v1.0.0) | Boot03 (v1.0.1) | 差异 |
|---|---|---|---|
| **采样起始时间** | 6.22s | 6.70s | +0.48s |
| **采样结束时间** | 21.50s | 28.22s | +6.72s |
| **实际采样时长** | **15.28s** | **21.52s** | **+6.24s (+41%)** |
| **采样数据点数** | 74 | 104 | +30 |

> **结论**: Boot03 整体开机时长比 Boot02 慢约 **6.24秒**，慢了约 **41%**。

---

## 3. 系统环境差异

| 项目 | Boot02 | Boot03 |
|---|---|---|
| **固件版本** | `STAR-NET/TB712/TB712:14/.../1.0.0` | `STAR-NET/TB712/TB712:14/.../1.0.1` |
| **内核版本** | `5.15.170-android14-11-maybe-dirty` | `5.15.170-android14-11-maybe-dirty` |
| **CPU** | armv8l (4核) | armv8l (4核) |
| **主显示** | 1080p60hz (HDMI) | 800x1280p60hz (MIPI LCD) |
| **副显示** | 800x1280p60hz (MIPI LCD) | 576cvbs (CVBS) |
| **MAC地址** | 28:BE:F3:24:7A:A9 | 28:BE:F3:24:7A:81 |

> **注意**: 两台设备固件版本不同 (1.0.0 vs 1.0.1)，**显示配置完全不同**（HDMI+LCD vs LCD+CVBS），且为不同硬件设备（MAC地址不同）。

---

## 4. 磁盘 I/O 对比 (核心差异)

### 4.1 总体 I/O 统计

| 指标 | Boot02 | Boot03 | 差异 |
|---|---|---|---|
| 总读次数 | 22,840 | 27,015 | +18% |
| 总写次数 | 888 | 1,782 | **+101% (2倍)** |
| 读取数据量 | 1,084.7 MB | 1,067.3 MB | 基本持平 |
| 写入数据量 | 8.9 MB | **20.5 MB** | **+130% (2.3倍)** |

### 4.2 关键分区写入差异

| 分区 | Boot02 写次数 | Boot03 写次数 | 差异 |
|---|---|---|---|
| `mmcblk0` (总设备) | 481 | **6,354** | **+13.2倍** |
| `mmcblk0p31` (data分区) | 430 | **6,287** | **+14.6倍** |
| `dm-10` | 0 | 582 | 新增 |
| `dm-9` | 0 | 254 | 新增 |

> **🔴 关键发现**: Boot03 的 **写入量是 Boot02 的 2.3 倍**，尤其是 `mmcblk0p31`（data 分区）写入操作暴增 **14.6 倍**，同时新增了 `dm-10` 和 `dm-9` 两个 device-mapper 设备的写操作。这很可能是开机变慢的核心原因之一——大量的写 I/O 导致 eMMC 带宽被占满。

---

## 5. CPU 利用率对比

| 指标 | Boot02 | Boot03 |
|---|---|---|
| 起始 CPU 利用率 | 72.8% | 70.8% |
| 结束 CPU 利用率 | 71.7% | 65.8% |
| CPU 效率 (ticks/centisecond) | 2.81 | **2.53** |
| 结束 IOWait | 227 ticks | 235 ticks |

> 🔴 Boot03 的 CPU 效率较低（2.53 vs 2.81），说明 CPU 有更多时间在等待 I/O（iowait 更高），这与磁盘大量写入是一致的。

### 上下文切换对比

| 指标 | Boot02 | Boot03 |
|---|---|---|
| 起始 ctxt | 86,655 | 99,689 |
| 结束 ctxt | 331,680 | 388,864 |
| 增量 | 245,025 | 289,175 |

> Boot03 的上下文切换增量多出约 18%。

---

## 6. 进程启动时间差异分析

### 6.1 用户空间进程 - 显著延迟 (>2s)

| 进程 | Boot02 | Boot03 | 延迟 |
|---|---|---|---|
| `wpa_supplicant` | 18.59s | 24.35s | **+5.76s** |

### 6.2 用户空间进程 - 中等延迟 (0.5~1s)

| 进程 | Boot02 | Boot03 | 延迟 |
|---|---|---|---|
| `tee_attest` | 8.56s | 9.35s | +0.79s |
| `/system/bin/derive_sdk` | 未出现 | 6.91s | 新增 |
| `netd/logd/servicemanager/surfaceflinger` 等一组 | 8.56s | 9.09s | +0.53s |
| `hwservicemanager` 及相关 HAL 服务组 | 8.00s | 8.52s | +0.52s |

### 6.3 Boot03 独有的新增进程

| 进程 | 首次出现时间 | 说明 |
|---|---|---|
| `/vendor/bin/tee_hdcp` | 9.35s | HDCP 可信执行环境服务 |
| `/system/bin/gsid` | 7.77s | GSI (Generic System Image) 守护进程 |
| `/system/bin/btfloader` | 8.00s | 蓝牙固件加载器 |
| `/system/bin/clean_scratch_files` | 7.56s | 临时文件清理 |
| `/apex/com.android.sdkext/bin/derive_sdk` | 6.91s | SDK 扩展派生 |

### 6.4 Boot02 独有但 Boot03 缺失的进程

| 进程 | 说明 |
|---|---|
| `/system/bin/vdc` | volume daemon client |
| `/system/bin/odsign` | on-device signing |
| `/system/bin/flags_health_check` | 特性标志健康检查 |
| `/system/bin/mediametrics` | 媒体指标服务 |

> 🔴 `tee_hdcp` 是 Boot03 新增的进程（与 HDCP 内容保护相关），这可能是由于显示配置变化（从 HDMI 变为 MIPI LCD 主显）带来的额外安全初始化开销。

---

## 7. 内核工作线程延迟

Boot03 中存在大量内核工作线程（kworker）的显著延迟或新增，特别是与以下相关的：

- **`mmc_complete`** / **`kblockd`**: 与 eMMC I/O 完成相关，说明存储 I/O 压力大
- **`dm-` (device mapper) / `kdmflush`**: 与 device-mapper 写回相关，Boot03 新增了大量这类线程
- **`sock_diag_events`**: 网络 socket 诊断相关
- **`fsverity_read_queue`**: 文件系统完整性校验（fs-verity），Boot03 新增

---

## 8. 根因分析总结

### 🔴 主要原因 (按影响程度排序)

1. **eMMC 写入量暴增 (最关键)**
   - `mmcblk0p31` (data分区) 写入量增加 14.6 倍
   - 新增 `dm-10`/`dm-9` 设备写入
   - 写入数据量从 8.9MB → 20.5MB (2.3倍)
   - **影响**: eMMC 带宽被大量写入占满，导致所有读 I/O 变慢，进而拖慢所有进程启动

2. **固件版本差异 (v1.0.0 → v1.0.1)**
   - 新增进程: `gsid`, `btfloader`, `tee_hdcp`, `clean_scratch_files`, `derive_sdk`
   - 新功能/安全机制引入了额外的初始化开销

3. **wpa_supplicant 启动严重延迟 (+5.76s)**
   - WiFi 认证守护进程从 18.59s 延迟到 24.35s
   - 可能与 WiFi 固件/驱动配置变更或 I/O 争抢有关

4. **显示配置变更**
   - 主显从 HDMI 1080p → MIPI LCD 800x1280
   - 副显从 MIPI LCD → CVBS 576
   - 可能触发了不同的显示 HAL 初始化路径和 HDCP 相关服务

### 🟡 次要因素

5. **fs-verity 文件完整性校验** (Boot03 新增相关 kworker)
6. **更多的上下文切换** (+18%)，说明系统调度压力更大

---

## 9. 优化建议

| 优先级 | 建议 | 预期收益 |
|---|---|---|
| **P0** | 排查 `mmcblk0p31` 写入量暴增的原因——是否是 data 分区挂载参数变化、是否有额外的初始化脚本在大量写文件 | 高 |
| **P0** | 对比 v1.0.0 和 v1.0.1 的 init.rc / fstab 差异，定位新增的启动服务 | 高 |
| **P1** | 排查 wpa_supplicant 启动延迟的原因，对比 WiFi 驱动/固件配置 | 中 |
| **P1** | 评估 `tee_hdcp` 等新增进程是否可以延迟启动(lazy init) | 中 |
| **P2** | 检查显示配置变更是否引入了不必要的初始化等待 | 低 |

---

## 10. 数据来源

- **Boot02**: `cmcc-xz-bootchart-02/` (2026-07-30 11:50:05, v1.0.0, HDMI+LCD)
- **Boot03**: `cmcc-xz-bootchart-03/` (2026-07-30 15:54:46, v1.0.1, LCD+CVBS)

分析工具: 基于 `/proc/stat`, `/proc/diskstats`, `/proc/*/stat` 的原始 bootchart 数据。
