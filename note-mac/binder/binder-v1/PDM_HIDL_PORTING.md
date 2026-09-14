# pdm-hidl：用绑定式 HIDL 重实现 vendor/starnet/pdm

> 项目：TB612 / S905L3AP / `p291_iptv` / Android 9（PPR1，SDK 28，full-treble + VNDK current）
> 日期：2026-09-10
> 状态：**代码已完成，模块编译与策略编译均已实测通过**；整包与上板验证待执行
> 代码位置：`vendor/starnet/pdm-hidl/`（外加大目录下 3 处各 1 行的必要改动，见第 4 节）

---

## 目录

1. [背景与目标](#1-背景与目标)
2. [方案总览](#2-方案总览)
3. [HIDL 接口设计](#3-hidl-接口设计)
4. [构建集成（关键：为什么改了 3 个额外文件）](#4-构建集成关键为什么改了-3-个额外文件)
5. [SELinux 策略](#5-selinux-策略)
6. [VINTF 清单](#6-vintf-清单)
7. [源码全文](#7-源码全文)
8. [编译验证（实测记录）](#8-编译验证实测记录)
9. [上板验证与排查](#9-上板验证与排查)
10. [与旧实现的差异、限制与遗留问题](#10-与旧实现的差异限制与遗留问题)
11. [附录：涉及的既有代码依据](#11-附录涉及的既有代码依据)

---

## 1. 背景与目标

### 1.1 现状

`vendor/starnet/pdm` 是星网定制的一个 **手写私有 binder 协议** 的 vendor 服务：

| 文件 | 作用 |
|---|---|
| `IPdm.h` / `IPdm.cpp` | 自己实现 `BnPdm::onTransact` 与 `BpPdm`，手工写/读 Parcel |
| `PdmService.{h,cpp}` | 业务：读写 Amlogic unifykeys、同步系统属性、work_mode 模式 |
| `Lock.{h,cpp}` | 对 `/sys/class/unifykeys/lock` 做 `flock` 串行化 |
| `UnifyKeys.h` | unifykeys 内核节点路径常量 |
| `cli/main.cpp` | `pdm.cli`：set / get / list |
| `sepolicy/*` | 服务域、file_contexts、service_contexts |

其中 `IPdm.cpp` 里有一段相当复杂的“协议兼容”代码（`probeTokenLenOffset()` 在 0/4/8 三个候选偏移里探测 interface token，见源码注释），根因就是**没有用 AIDL/HIDL 代码生成工具链**，收发两端要自己对字节。

### 1.2 目标

用**绑定式（binderized）HIDL** 重做一遍同样功能：

- 定义 HIDL 包 `vendor.starnet.pdm@1.0`，由 `hidl-gen` 生成收发桩（不再手写 Parcel）；
- HAL 服务注册到 `hwservicemanager`，走 `/dev/hwbinder`；
- 提供等价的 C++ 命令行工具 `pdm.hidl.cli`（用法与 `pdm.cli` 一致）；
- **所有新增代码放在 `vendor/starnet/pdm-hidl/` 下**；
- 与旧服务**并存、互不影响**（可同时运行，确认无误后再停用旧的）。

### 1.3 业务功能（必须逐项保留）

1. **unifykeys 读写**：`/sys/class/unifykeys/{name,write,read,lock}`（Amlogic 内核节点，键名以板级 DTS 的 unifykey 节点为准）；
2. **键值 → 系统属性同步**：`device_id → ro.product.cmcc_deviceId` 等 20+ 条映射（`kKeyPropMap`）；
3. **读取 fallback 链**：unifykeys → 系统属性 → 默认值（首次开机 / 未烧录 unifykey 时取属性值，实测 `model` 取到 TB712）；
4. **`work_mode` 特殊处理**：真值是 `/mnt/vendor/param/usermode` **标志文件的存在性**（存在=用户模式 `"1"`，不存在=工厂模式 `"0"`），不是普通键值；
5. **属性白名单门**：写 `ro.*` 前把 `persist.ro.white_list.enable` 置 `"1"`，离开作用域置回 `"0"`（`RoWhiteListGate`），凭此让 init 放行 `whitelist_prop.txt` 内的 ro 属性运行期改写；
6. **flock 串行化**：每次读写 unifykeys 期间持有 `/sys/class/unifykeys/lock` 的文件锁；
7. **开机同步**：服务构造时做一次全量 unifykey → 属性同步。

---

## 2. 方案总览

| 项 | 旧 `vendor/starnet/pdm` | 新 `vendor/starnet/pdm-hidl` |
|---|---|---|
| IPC 机制 | 私有 binder（`/dev/binder`） | **HIDL over hwbinder**（`/dev/hwbinder`） |
| 服务名 | `vendor.starnet.pdm` | `vendor.starnet.pdm@1.0::IPdm/default` |
| 注册中心 | `servicemanager` | `hwservicemanager` |
| 收发协议 | 手写 `BnPdm::onTransact` / `BpPdm` | **hidl-gen 生成**（零手写 Parcel） |
| 服务进程 | `/vendor/bin/pdm-service` | `/vendor/bin/hw/vendor.starnet.pdm@1.0-service` |
| 服务域 | `u:r:pdm_service:s0` | `u:r:pdm_hidl_default:s0`（标准 Treble HAL 域） |
| CLI | `pdm.cli`（`/system/bin`） | `pdm.hidl.cli`（`/system/bin`，用法一致） |
| 业务逻辑 | unifykey + 属性同步 + work_mode + flock | **逐段移植，逻辑不变** |

### 2.1 文件清单

```
vendor/starnet/pdm-hidl/
├── README.md                                      # 使用/差异/上板验证(面向使用者)
├── pdm-hidl.mk                                    # PRODUCT_PACKAGES 声明(product-config 阶段)
├── interfaces/
│   ├── Android.bp                                 # hidl_package_root: vendor.starnet
│   └── pdm/1.0/
│       ├── Android.bp                             # hidl_interface "vendor.starnet.pdm@1.0"
│       ├── types.hal                              # struct Entry / enum PdmStatus
│       └── IPdm.hal                               # interface IPdm
├── service/
│   ├── Android.bp
│   ├── main.cpp                                   # configureRpcThreadpool + registerAsService
│   ├── PdmHal.{h,cpp}                             # 业务实现(移植自 PdmService.cpp)
│   ├── Lock.{h,cpp}                               # flock 串行化(与 pdm/ 同逻辑)
│   ├── UnifyKeys.h                                # 节点路径常量
│   └── vendor.starnet.pdm@1.0-service.rc
├── cli/
│   ├── Android.bp
│   └── main.cpp                                   # pdm.hidl.cli
├── sepolicy/
│   ├── pdm_hidl.te
│   ├── file_contexts
│   └── hwservice_contexts
└── manifest/
    └── vendor.starnet.pdm@1.0.xml                 # VINTF 清单片段
```

### 2.2 编译产物与落点（实测确认）

| 模块 | 安装路径 | 说明 |
|---|---|---|
| `vendor.starnet.pdm@1.0` | `vendor/lib/libvendor.starnet.pdm@1.0.so`<br>`system/lib/libvendor.starnet.pdm@1.0.so` | HIDL 接口/桩库（`hidl_interface` 自动 `vendor_available`，两侧各一份） |
| `vendor.starnet.pdm@1.0-service` | `vendor/bin/hw/vendor.starnet.pdm@1.0-service` | HAL 服务（init `class hal`、root） |
| （随服务自动安装） | `vendor/etc/init/vendor.starnet.pdm@1.0-service.rc` | init rc（`Android.bp` 的 `init_rc`） |
| `pdm.hidl.cli` | `system/bin/pdm.hidl.cli` | 命令行工具 |

---

## 3. HIDL 接口设计

### 3.1 方法映射

旧接口用 `status_t` 返回值 + 出参；HIDL 的 `Return<T>` **只表达传输层错误**（binder 是否送达），业务错误必须在方法返回值/回调里显式回传，否则客户端无法区分“键不存在”和“服务挂了”。因此定义 `PdmStatus` 枚举等价于 `status_t`：

```
setValue(string name, string value) generates (PdmStatus status, bool result);    // 旧: status_t + bool*
getValue(string name)               generates (PdmStatus status, string value);   // 旧: status_t + string*
getEntries()                        generates (PdmStatus status, vec<Entry> entries); // 旧: status_t + vector<PdmEntry>
```

| `PdmStatus` | 旧 `status_t` | 触发条件 |
|---|---|---|
| `OK = 0` | `NO_ERROR` (0) | 成功 |
| `NAME_NOT_FOUND = 1` | -2 | `name` 为空或不在 `kKeyPropMap` 中 |
| `BAD_VALUE = 2` | -22 | `work_mode` 取值不是 `"0"` / `"1"` |
| `PERMISSION_DENIED = 3` | -1 | 拿不到 `/sys/class/unifykeys/lock` |
| `UNKNOWN_ERROR = 4` | -5 | unifykeys 节点读写失败 |

> 枚举故意用小正整数而非负值（负值容易踩 hidl-gen / 序列化限制），映射关系写在注释里。

### 3.2 客户端调用形态

```cpp
#include <vendor/starnet/pdm/1.0/IPdm.h>
using namespace ::vendor::starnet::pdm::V1_0;

android::sp<IPdm> pdm = IPdm::getService();        // 取 .../default，内部会短暂重试
if (pdm == nullptr) { /* 服务未起来 */ }

PdmStatus st;
std::string model;
auto ret = pdm->getValue("model", [&](PdmStatus s, const hidl_string& v) {
    st = s;
    model.assign(v.c_str(), v.size());
});
if (!ret.isOk()) { /* 传输层失败：ret.description() */ }
```

---

## 4. 构建集成（关键：为什么改了 3 个额外文件）

### 4.1 最终方案

| # | 文件 | 改动 | 作用 |
|---|---|---|---|
| 1 | `vendor/starnet/pdm-hidl/pdm-hidl.mk` | 新增 | `PRODUCT_PACKAGES += vendor.starnet.pdm@1.0-service pdm.hidl.cli` |
| 2 | `vendor/starnet/starnet.mk` | **+1 行** `include $(LOCAL_PATH)/pdm-hidl/pdm-hidl.mk` | 把上面的声明挂进 product-config 链 |
| 3 | `device/amlogic/common/sepolicy.mk` | **+3 行** `BOARD_SEPOLICY_DIRS += vendor/starnet/pdm-hidl/sepolicy` | 让 HAL 的 SELinux 策略进镜像 |
| 4 | `device/amlogic/p291_iptv/BoardConfig.mk` | **+1 行** `DEVICE_MANIFEST_FILE += vendor/starnet/pdm-hidl/manifest/vendor.starnet.pdm@1.0.xml` | 让 VINTF 清单片段并入 device manifest（可选） |

其余全部自动：HIDL 接口库随服务/CLI 的 `shared_libs` 依赖自动进两侧；init rc 随 `init_rc` 自动装到 `vendor/etc/init/`。

### 4.2 为什么不能做到“零外部改动”（两条实测踩坑）

#### 坑 1：`Android.mk` 写 `PRODUCT_PACKAGES` 会构建失败

最初设想：把声明放在 `vendor/starnet/pdm-hidl/Android.mk`，靠 `starnet.mk` 里的
`include $(call all-subdir-makefiles)`（= `$(wildcard $(my-dir)/*/Android.mk)`）自动包含。

**实测失败**：

```
vendor/starnet/pdm-hidl/Android.mk:21: error: cannot assign to readonly variable: PRODUCT_PACKAGES
17:19:56 ckati failed with: exit status 1
```

原因：`Android.mk` 只会在 kati **主阶段**被扫到（`build/make/core/main.mk:452` 的
`subdir_makefiles := ... $(file <$(OUT_DIR)/.module_paths/Android.mk.list)`），那时
`PRODUCT_PACKAGES` 已被 `.KATI_READONLY`。声明必须写在 **product-config 阶段**
（`p291_iptv.mk → device.mk → core_amlogic.mk → starnet.mk` 这条链上）——旧
`pdm-service`/`pdm.cli` 也正是那样声明的。

#### 坑 2：`BOARD_SEPOLICY_DIRS` 写在 product makefile 里**不生效**（静默失效）

旧的 `vendor/starnet/pdm/sepolicy` 就是在 `starnet.mk` 里 `BOARD_SEPOLICY_DIRS +=` 的。
实测编译产物 `vendor/etc/selinux/vendor_sepolicy.cil` 里：

| 域 | 是否在策略里 | 加入方式 |
|---|---|---|
| `tvserver` / `system_control` | ✅ 在（118 / 237 处） | `device/amlogic/common/sepolicy.mk`（board-config 阶段） |
| `pdm_hidl_default`（新） | ❌ 不在 | 起初写在 starnet.mk（product-config） |
| `pdm_service`（旧） | ❌ **也不在** | 同样写在 starnet.mk |

即：**该变量只认 board-config 阶段的追加**；旧 pdm 的策略其实一直没进镜像（见第 10.4 节）。
修复：把 `vendor/starnet/pdm-hidl/sepolicy` 注册到
`device/amlogic/common/sepolicy.mk`（`p291_iptv/BoardConfig.mk:325` 会 include 它，
三个 amlogic 产品共用）。

### 4.3 四个改动文件的内容

**`vendor/starnet/pdm-hidl/pdm-hidl.mk`（新增）**

```make
# pdm-hidl 的构建声明:让编译产物(镜像)带上 HAL 服务与 CLI。
# 由 vendor/starnet/starnet.mk 在 product-config 阶段 include 一行引入。
#
# ⚠️ 不要改回 vendor/starnet/pdm-hidl/Android.mk 的自动包含写法:
#    Android.mk 只会在 kati 主阶段被扫到(subdir_makefiles 那一步),
#    那时 PRODUCT_PACKAGES 已被置为只读,实测报错:
#      vendor/starnet/pdm-hidl/Android.mk: cannot assign to readonly variable: PRODUCT_PACKAGES

PRODUCT_PACKAGES += \
    vendor.starnet.pdm@1.0-service \
    pdm.hidl.cli

# SELinux 策略目录不在这里声明:BOARD_SEPOLICY_DIRS 只认 board-config 阶段
# (device/amlogic/common/sepolicy.mk)里的追加——实测本文件所在的 product-config
# 阶段追加不会进策略(见该文件里的注释)。此处若再写一遍,一旦上游修好时序反而
# 会重复包含同一目录导致策略编译报重复定义。
```

**`vendor/starnet/starnet.mk`（第 16~25 行，新增末 3 行）**

```make
#for pdm (unifykey 键值管理服务及命令行工具)
PRODUCT_PACKAGES += \
	pdm-service \
	pdm.cli

#for pdm-hidl (绑定式 HIDL 版 unifykey 服务, 与上面的 pdm 并存;
# 见 vendor/starnet/pdm-hidl/README.md)
include $(LOCAL_PATH)/pdm-hidl/pdm-hidl.mk
```

**`device/amlogic/common/sepolicy.mk`（第 1~11 行）**

```make
BOARD_SEPOLICY_DIRS += \
    device/amlogic/common/sepolicy \
    device/amlogic/common/sepolicy/aml_core

# 星网 pdm-hidl(绑定式 HIDL 版 unifykey 服务, 见 vendor/starnet/pdm-hidl/README.md)
# ⚠️ 必须写在这个 board-config 阶段的文件里:实测把该目录 append 到
#    vendor/starnet/starnet.mk(旧的 vendor/starnet/pdm/sepolicy 就是那么写的)不生效,
#    编译出的 vendor/etc/selinux/vendor_sepolicy.cil 里既没有 pdm_service、
#    也没有 pdm_hidl_default(而这里的 tvserver/system_control 都在)。
BOARD_SEPOLICY_DIRS += \
    vendor/starnet/pdm-hidl/sepolicy
```

**`device/amlogic/p291_iptv/BoardConfig.mk`（第 362~372 行）**

```make
###npu board config
AUTO_PATCH_NPU := device/amlogic/common/products/mbox/manifest/npu_update.sh
ifeq ($(BOARD_NPU_SERVICE_ENABLE),true)
$(shell ($(AUTO_PATCH_NPU) $(DEVICE_MANIFEST_FILE)))
endif

# pdm-hidl(绑定式 HIDL 版 unifykey 服务)的 VINTF 清单片段。
# DEVICE_MANIFEST_FILE 支持多个文件(build/make/target/board/Android.mk:35),
# 由 assemble_vintf 与上面的板级 manifest 合并成 /vendor/etc/vintf/manifest.xml;
# 详见 vendor/starnet/pdm-hidl/README.md 第 6 节。
# ⚠️ 必须放在 AUTO_PATCH_AB / AUTO_PATCH_NPU 之后:那两个脚本会 sed -i 就地修改
#    $(DEVICE_MANIFEST_FILE) 里的每个文件,放在前面会被它们一并处理。
DEVICE_MANIFEST_FILE += vendor/starnet/pdm-hidl/manifest/vendor.starnet.pdm@1.0.xml
```

### 4.4 整包构建

```bash
# 项目既有入口脚本(与 OTA DIM/版本号逻辑无关,不用改)
./tb611_s905L3ap_p291.sh -v 1.0.1 -k 1
#   -v 1.0.1 : 只改版本串(ro.product.build.dim / BUILD_OEM_VERSION)
#   -k 1     : BUILD_XIRI_PRODUCT=2110(只影响 aisound 相关分支,与 pdm-hidl 无关)
# 脚本内部:lunch p291_iptv-userdebug → make installclean → make otapackage
# 产物:out/target/product/p291_iptv/p291_tb611.img(线刷) +
#       update_sys_s905l3ap_tb611.zip(OTA/U盘)
```

单独编译本模块（快速验证用）：

```bash
TARGET_PRODUCT=p291_iptv TARGET_BUILD_VARIANT=userdebug \
  build/soong/soong_ui.bash --make-mode \
  vendor.starnet.pdm@1.0 vendor.starnet.pdm@1.0-service pdm.hidl.cli
```

---

## 5. SELinux 策略

### 5.1 设计要点（3 处必须与旧 `pdm_service.te` 不同）

1. **不再用 `init_daemon_domain` + 手工 `service_manager_type`**，而是标准 HAL 写法：
   `hal_attribute` + `hal_server_domain` + `add_hwservice`（宏见 `system/sepolicy/public/te_macros:208/228/593`）。
2. **`.rc` 里不能写 `seclabel`**：
   `system/sepolicy/public/hal_neverallows.te:59` 有
   `neverallow * halserverdomain:process dyntransition;`
   HAL 域转换只能靠 `file_contexts` 里的 `<域>_exec` 标签 + `init_daemon_domain` 在 exec 时完成。
   （旧 `pdm-service.rc:8` 的 `seclabel u:r:pdm_service:s0` 属于另一种域模型，HAL 域下会与 neverallow 冲突。）
3. **不能照抄旧策略里的 `allow pdm_service vendor_file:file execute_no_trans`**：
   `hal_neverallows.te:50` 有
   `neverallow { halserverdomain -hal_dumpstate_server -hal_telephony_server } { file_type fs_type }:file execute_no_trans;`
   本服务不需要 exec 任何东西。

### 5.2 客户端权限

CLI 以 `shell` 域从 adb/串口执行。`shell` 已自带 `hwbinder_use(shell)` 与
`hwservicemanager list`（`system/sepolicy/public/shell.te:117-118`），因此只需补：

```te
allow shell pdm_hidl_hwservice:hwservice_manager find;   # 找到这个 hwservice
allow shell pdm_hidl_default:binder { call transfer };   # 调用 HAL 进程
allow pdm_hidl_default shell:binder transfer;            # 回包时传递引用
```

### 5.3 策略文件全文

见第 7.9 节。

---

## 6. VINTF 清单

Android 9 的 libvintf **只读单文件** `/vendor/etc/vintf/manifest.xml`
（`system/libvintf/VintfObject.cpp:165-172`），**不会**合并 `/vendor/etc/vintf/manifest/*.xml`。
因此片段必须在编译期并入 device manifest——`DEVICE_MANIFEST_FILE` 支持多文件
（`build/make/target/board/Android.mk:35` 注释明确写了 "can be a list of files"），由
`assemble_vintf` 合并。

本产品未开 `PRODUCT_ENFORCE_VINTF_MANIFEST`（=不强制），**不并入也能正常注册与调用**，
并入的价值是让 `lshal` / `checkvintf` 的清单一致（更规范、便于后续升级兼容性检查）。

---

## 7. 源码全文

### 7.1 `interfaces/Android.bp`

```blueprint
// HIDL 包根声明:包名 vendor.starnet.pdm@1.0 的 HAL 文件位于 <path>/pdm/1.0/,
// 即 vendor/starnet/pdm-hidl/interfaces/pdm/1.0/。
// 等价于 hidl-gen 的 "-r vendor.starnet:vendor/starnet/pdm-hidl/interfaces"。
// 参考树内同类写法:vendor/amlogic/common/interfaces/Android.bp。
hidl_package_root {
    name: "vendor.starnet",
    path: "vendor/starnet/pdm-hidl/interfaces",
}
```

### 7.2 `interfaces/pdm/1.0/Android.bp`

```blueprint
// HIDL 接口库(由 hidl-gen 生成收发桩代码,等价于 hidl-gen -Landroidbp 的输出)。
// 产物:libvendor.starnet.pdm@1.0.so(system 与 vendor 两侧都有,hidl_interface
// 自动带 vendor_available),头文件 <vendor/starnet/pdm/1.0/IPdm.h>。
hidl_interface {
    name: "vendor.starnet.pdm@1.0",
    root: "vendor.starnet",
    vendor: true,
    srcs: [
        "types.hal",
        "IPdm.hal",
    ],
    interfaces: [
        "android.hidl.base@1.0",
    ],
    types: [
        "Entry",
        "PdmStatus",
    ],
    // 本模块只提供服务端与 C++ 客户端;如需 App(Java)调用再置 true,
    // 并注意 app_neverallows.te 对 all_untrusted_apps 调用 halserverdomain 的 neverallow。
    gen_java: false,
}
```

### 7.3 `interfaces/pdm/1.0/types.hal`

```hal
/*
 * Copyright (C) 2026 Starnet. All rights reserved.
 *
 * 与 vendor/starnet/pdm(旧私有 binder 服务)等价的数据类型定义。
 * HIDL 不允许在 interface 内部嵌 struct,所有非接口类型必须定义在 types.hal。
 */
package vendor.starnet.pdm@1.0;

/**
 * 一个 unifykey 键值对(对应旧实现的 android::PdmEntry,PdmTypes.h)。
 */
struct Entry {
    string name;
    string value;
};

/**
 * 方法级返回码(对应旧实现回调里回传的 status_t)。
 * HIDL 的 Return<T> 只表达传输层错误(binder 是否送达),业务错误必须在方法
 * 返回值/回调里显式回传,否则客户端无法区分"键不存在"与"服务挂了"。
 */
enum PdmStatus : int32_t {
    OK = 0,                 // 旧 status_t: NO_ERROR(0)
    NAME_NOT_FOUND = 1,     // 旧 status_t: -2, name 为空或不在 kKeyPropMap 中
    BAD_VALUE = 2,          // 旧 status_t: -22, work_mode 取值非法
    PERMISSION_DENIED = 3,  // 旧 status_t: -1, 拿不到 /sys/class/unifykeys/lock
    UNKNOWN_ERROR = 4,      // 旧 status_t: -5, unifykeys 节点读写失败
};
```

### 7.4 `interfaces/pdm/1.0/IPdm.hal`

```hal
/*
 * Copyright (C) 2026 Starnet. All rights reserved.
 *
 * 与 vendor/starnet/pdm(旧服务名 vendor.starnet.pdm, 手写 Bn/Bp 私有协议)功能等价的
 * 绑定式(binderized) HIDL 接口:读写 Amlogic unifykeys 并同步到系统属性。
 *
 * 注册名: vendor.starnet.pdm@1.0::IPdm/default (hwservicemanager, 走 /dev/hwbinder)
 * 客户端: vendor/starnet/pdm-hidl/cli 以及任何链接 vendor.starnet.pdm@1.0 的 C++ 进程
 */
package vendor.starnet.pdm@1.0;

interface IPdm {
    /**
     * 烧写一个 unifykey 并同步相关系统属性(对应旧 setValue)。
     *
     * @param name  键名,必须在实现的 kKeyPropMap 中(空名或未知键返回 NAME_NOT_FOUND)
     * @param value 键值;work_mode 特殊,只接受 "0"(工厂模式)/"1"(用户模式)
     * @return status 业务返回码
     * @return result 仅在 status == OK 时为 true
     */
    setValue(string name, string value) generates (PdmStatus status, bool result);

    /**
     * 读取一个键(对应旧 getValue):先查 unifykeys 驱动,为空则回退读系统属性,
     * 再回退默认值;work_mode 直接取 /mnt/vendor/param/usermode 标志文件。
     *
     * @param name 键名,不在 kKeyPropMap 中返回 NAME_NOT_FOUND
     * @return status 业务返回码
     * @return value 键值(失败时为空串)
     */
    getValue(string name) generates (PdmStatus status, string value);

    /**
     * 列出全部已知键(对应旧 getEntries/readAllEntries)。
     *
     * @return status 业务返回码
     * @return entries 键值对列表
     */
    getEntries() generates (PdmStatus status, vec<Entry> entries);
};
```

### 7.5 `service/Android.bp`

```blueprint
// 绑定式 HIDL HAL 服务:vendor.starnet.pdm@1.0::IPdm/default
// 安装到 /vendor/bin/hw/ ,开机由 init 以 class hal 拉起(见同目录 .rc)。
cc_binary {
    name: "vendor.starnet.pdm@1.0-service",
    defaults: [
        "hidl_defaults",
    ],
    vendor: true,
    relative_install_path: "hw",
    init_rc: [
        "vendor.starnet.pdm@1.0-service.rc",
    ],
    srcs: [
        "main.cpp",
        "PdmHal.cpp",
        "Lock.cpp",
    ],
    shared_libs: [
        // hidl-gen 生成的接口库(BnHwIPdm 桩 + types)
        "vendor.starnet.pdm@1.0",
        "libbase",
        "libhidlbase",
        "libhidltransport",
        "libhwbinder",
        "libutils",
        "libcutils",  // property_get / property_set
        "liblog",
    ],
    cflags: [
        // hidl_defaults 已带 -Wall -Werror,这里只加日志 tag
        "-DLOG_TAG=\"pdm-hidl\"",
    ],
}
```

### 7.6 `service/vendor.starnet.pdm@1.0-service.rc`

```
# 开机启动 pdm-hidl 服务(绑定式 HIDL,注册到 hwservicemanager 走 /dev/hwbinder)。
# ⚠️ 执行路径必须带 hw 中间目录(Android.bp 里 relative_install_path: "hw"),
#    与 vendor/amlogic 各 HAL 服务一致;root 才能写 /sys/class/unifykeys。
# ⚠️ 不要写 seclabel:本服务域带 halserverdomain 属性,
#    system/sepolicy/public/hal_neverallows.te 有
#    neverallow * halserverdomain:process dyntransition,
#    域转换必须靠 sepolicy/file_contexts + init_daemon_domain 在 exec 时完成
#    (旧 pdm-service.rc 的 seclabel 写法在 HAL 域下会与 neverallow 冲突)。
service vendor.pdm-hidl-1-0 /vendor/bin/hw/vendor.starnet.pdm@1.0-service
    class hal
    user root
    group root
```

### 7.7 `service/main.cpp`

```cpp
// Copyright (C) 2026-2026 福建星网锐捷通讯股份有限公司. All rights reserved.

#include <vendor/starnet/pdm/1.0/IPdm.h>

#include <hidl/HidlTransportSupport.h>
#include <log/log.h>
#include <unistd.h>
#include <utils/Errors.h>  // android::OK / status_t

#include "PdmHal.h"

using ::android::hardware::configureRpcThreadpool;
using ::android::hardware::joinRpcThreadpool;
using ::vendor::starnet::pdm::V1_0::IPdm;
using ::vendor::starnet::pdm::V1_0::implementation::PdmHal;

int main() {
    ALOGI("pdm-hidl: main() pid=%d uid=%d, service starting...", getpid(), getuid());

    // 绑定式 HIDL 服务:注册到 hwservicemanager(走 /dev/hwbinder),
    // 不再是旧实现的 servicemanager(走 /dev/binder)
    configureRpcThreadpool(4, true);

    ::android::sp<IPdm> service = new PdmHal();  // 构造时同步一次属性

    ::android::status_t err = service->registerAsService();
    if (err != ::android::OK) {
        ALOGE("pdm-hidl: main() registerAsService('default') failed, err=%d", err);
        return 1;
    }
    ALOGI("pdm-hidl: service 'vendor.starnet.pdm@1.0::IPdm/default' registered successfully");

    joinRpcThreadpool();
    ALOGE("pdm-hidl: main() unexpectedly returned from joinRpcThreadpool()");
    return 0;  // 不应到达
}
```

### 7.8 `service/PdmHal.h`

```cpp
// Copyright (C) 2026-2026 福建星网锐捷通讯股份有限公司. All rights reserved.
#pragma once

#include <vendor/starnet/pdm/1.0/IPdm.h>

#include <memory>
#include <string>
#include <vector>

namespace vendor {
namespace starnet {
namespace pdm {
namespace V1_0 {
namespace implementation {

// 绑定式(binderized) HIDL 实现:与 vendor/starnet/pdm 里 PdmService 的功能等价。
// 业务逻辑(unifykeys 读写 / 属性同步 / work_mode 标志文件 / flock 串行化)逐段移植,
// 差异只在 IPC 层:收发 Parcel 由 hidl-gen 生成的 IPdm 桩负责,不再手写
// BnPdm::onTransact / BpPdm(旧实现要手工 probe interface token 偏移)。
class PdmHal : public IPdm {
  public:
    PdmHal();  // 构造时按 unifykeys 做一次开机属性同步(等价 PdmService 构造函数)
    ~PdmHal() = default;

    // Methods from ::vendor::starnet::pdm::V1_0::IPdm follow.
    ::android::hardware::Return<void> setValue(
            const ::android::hardware::hidl_string& name,
            const ::android::hardware::hidl_string& value, setValue_cb _hidl_cb) override;
    ::android::hardware::Return<void> getValue(
            const ::android::hardware::hidl_string& name, getValue_cb _hidl_cb) override;
    ::android::hardware::Return<void> getEntries(getEntries_cb _hidl_cb) override;

  private:
    // 读取全部已知 key(name->value 逐项填充)
    bool readAllEntries(std::vector<Entry>* entries);
    // key -> 系统属性(对应旧 UNIFY_KEY_MAP)
    void syncKeyToProperty(const std::string& name, const std::string& value);
    // 三个接口的业务实现(返回 PdmStatus,由上面三个 HIDL 方法转成回调回传)
    PdmStatus setValueInternal(const std::string& name, const std::string& value, bool* result);
    PdmStatus getValueInternal(const std::string& name, std::string* value);
};

}  // namespace implementation
}  // namespace V1_0
}  // namespace pdm
}  // namespace starnet
}  // namespace vendor
```

### 7.9 `service/PdmHal.cpp`

> 说明：本文件是 `vendor/starnet/pdm/PdmService.cpp` 的 HIDL 版本，业务逻辑逐段移植，
> 只把“返回值 + 出参”换成 `PdmStatus` + HIDL 回调。以下为全文（约 480 行）。

```cpp
// Copyright (C) 2026-2026 福建星网锐捷通讯股份有限公司. All rights reserved.
//
// 本文件是 vendor/starnet/pdm/PdmService.cpp 的 HIDL 版本:业务逻辑逐段移植
// (unifykeys 读写、属性映射表与 fallback 链、work_mode 标志文件、白名单门、flock),
// 只把"返回值 + 出参"换成 PdmStatus + HIDL 回调。

#include "PdmHal.h"

#include <cutils/properties.h>
#include <errno.h>
#include <fcntl.h>
#include <log/log.h>
#include <string.h>
#include <unistd.h>

#include <string>
#include <vector>

#include "Lock.h"
#include "UnifyKeys.h"

namespace vendor {
namespace starnet {
namespace pdm {
namespace V1_0 {
namespace implementation {

using ::android::hardware::hidl_string;
using ::android::hardware::hidl_vec;
using ::android::hardware::Return;
using ::android::hardware::Void;

// 每个 unifykey 对应的系统属性及默认值(原 UNIFY_KEY_MAP,按你的产品裁剪)
// ⚠️ struct 必须显式命名(避免 decltype 推导出 pointer-to-reference 非法类型)
struct KeyPropItem {
    const char* key;
    const char* prop;
    const char* def;
};
static const KeyPropItem kKeyPropMap[] = {
        // 键名以板级 DTS(keyman_gxlx.dtsi 的 unifykey 节点)为准;
        // ro.product.cmcc_* 默认值由 vendor_prop_sdmobile.mk 烘焙;property
        // service 依据 /vendor/starnet/whitelist_prop.txt 放行这几个 ro.* 的
        // 运行期改写:本服务 set/开机同步因此可成功,串口/adb setprop 亦即时
        // 生效;重启后先装载 mk 默认,再由本服务按 unifykey 值覆盖。
        // persist.sys.cmcc_* 行为 rc 复制段及上层 SDK 提供同值通道
        {"device_id", "ro.product.cmcc_deviceId", ""}, // 设备ID
        {"device_id", "ro.product.cmcc_id", ""}, // 设备ID
        // 移动SDK 鉴权凭证(和家亲 andlink):默认值与
        // device/amlogic/p291_iptv/vendor_prop_sdmobile.mk 保持一致;
        {"product_token", "ro.product.cmcc_productToken", ""},
        {"product_token", "ro.product.cmcc_token", ""},
        {"andlink_token", "ro.product.cmcc_andlinkToken", ""},
        {"andlink_token", "ro.product.cmcc_andlinktoken", ""},

        {"mac", "ro.mac", ""},
        {"mac_wifi", "ro.product.mac_wifi", ""},

        {"stbid", "ro.serialno", ""},
        {"stbid", "ro.product.stbid", ""},
        {"sn", "ro.product.cmcc_sn", ""},
        {"sn", "ro.product.serialno", ""},
        {"sn", "ro.boot.sn", ""},
        {"cmccpwd", "ro.product.cmcc_password", ""},
        {"cmei", "ro.product.cmcc_cmei", ""},
        {"work_mode", "ro.device.work.mode", ""},
        {"cmcc_date", "ro.product.cmcc_date", ""},
        // 整机产品属性(品牌/型号/平台),默认值按产品配置;
        // 需与白名单(whitelist_prop.txt)配套才能运行时改写已装载的 ro.* 属性
        {"manufacturer", "ro.product.manufacturer", ""},
        {"brand", "ro.product.brand", ""},
        {"model", "ro.product.model", ""},
        {"device", "ro.product.device", ""},
        {"cmcc_brand", "ro.product.cmcc_brand", ""},
        {"cmcc_ims_app_id", "ro.product.cmcc_imsAppId", ""},
        {"cmcc_ims_secret", "ro.product.cmcc_imsSecret", ""},
};

// 内部辅助: name -> kKeyPropMap 第一项(或 nullptr). 用于默认值 fallback / 非法 name 校验.
static const KeyPropItem* findMapItem(const std::string& name) {
    for (const auto& item : kKeyPropMap) {
        if (name == item.key) return &item;
    }
    return NULL;
}

// 内部辅助: 读 property; 读到且非空返回 true, 否则 false.
static bool getPropertyString(const char* prop, std::string* out) {
    char buf[PROPERTY_VALUE_MAX];
    int len = property_get(prop, buf, "");
    if (len > 0 && buf[0] != '\0') {
        *out = std::string(buf, (size_t)len);
        return true;
    }
    return false;
}

// 读/写 sysfs 节点(原 android::base::WriteStringToFile/ReadFileToString)
static bool writeFile(const char* path, const std::string& data) {
    ALOGD("pdm-hidl: writeFile(path=%s, data_len=%zu)", path, data.size());
    int fd = open(path, O_WRONLY);
    if (fd < 0) {
        ALOGE("pdm-hidl: writeFile open %s failed: %s", path, strerror(errno));
        return false;
    }
    size_t off = 0;
    while (off < data.size()) {
        ssize_t n = write(fd, data.data() + off, data.size() - off);
        if (n <= 0) {
            ALOGE("pdm-hidl: writeFile write %s failed at off=%zu: %s",
                  path, off, strerror(errno));
            close(fd);
            return false;
        }
        off += n;
    }
    close(fd);
    ALOGD("pdm-hidl: writeFile(%s) success, wrote %zu bytes", path, off);
    return true;
}

static bool readFile(const char* path, std::string* out) {
    ALOGD("pdm-hidl: readFile(path=%s)", path);
    int fd = open(path, O_RDONLY);
    if (fd < 0) {
        ALOGE("pdm-hidl: readFile open %s failed: %s", path, strerror(errno));
        return false;
    }
    char buf[1024];
    ssize_t n;
    out->clear();
    while ((n = read(fd, buf, sizeof(buf))) > 0) {
        out->append(buf, n);
    }
    close(fd);
    if (n != 0) {
        ALOGE("pdm-hidl: readFile read %s failed: %s (last n=%zd)",
              path, strerror(errno), n);
        return false;
    }
    ALOGD("pdm-hidl: readFile(%s) success, read %zu bytes", path, out->size());
    return true;
}

// 属性值不宜带 sysfs 尾部换行
static std::string trimTail(const std::string& s) {
    size_t end = s.size();
    while (end > 0 && (s[end - 1] == '\n' || s[end - 1] == '\r')) end--;
    return s.substr(0, end);
}

static constexpr const char* kUserModeFile = "/mnt/vendor/param/usermode";
static constexpr const char* kModeKey = "work_mode";
static constexpr const char* kModeUser = "1";    // 用户模式
static constexpr const char* kModeFactory = "0"; // 工厂模式

// 是否用户模式(标志文件存在性)
static bool isUserMode() {
    if (access(kUserModeFile, F_OK) == 0) return true;
    if (errno != ENOENT) {
        ALOGW("pdm-hidl: isUserMode: access(%s) failed: %s", kUserModeFile, strerror(errno));
    }
    return false;
}

// work_mode 取值校验: 只接受 "0"/"1"。非法值若放行,会改掉 unifykey/属性而文件不动,
// 造成"命令成功但模式没变"的假成功。
static bool isValidModeValue(const std::string& v) {
    return v == kModeUser || v == kModeFactory;
}

// 落盘模式标志文件: "1" -> 写入 "1"(用户模式); "0" -> 删除文件(工厂模式)。
// 与串口 echo 1 > /mnt/vendor/param/usermode 等效;文件内容不参与判定,仅存续性有意义。
static bool applyUserModeFile(const std::string& value) {
    if (value == kModeUser) {
        int fd = open(kUserModeFile, O_WRONLY | O_CREAT | O_TRUNC, 0644);
        if (fd < 0) {
            ALOGE("pdm-hidl: applyUserModeFile: open %s failed: %s",
                  kUserModeFile, strerror(errno));
            return false;
        }
        const char* data = "1\n";
        ssize_t n = write(fd, data, strlen(data));
        int saved_errno = errno;
        close(fd);
        if (n != (ssize_t)strlen(data)) {
            ALOGE("pdm-hidl: applyUserModeFile: write %s failed (n=%zd): %s",
                  kUserModeFile, n, strerror(saved_errno));
            return false;
        }
        ALOGI("pdm-hidl: applyUserModeFile: user work_mode ON, created %s", kUserModeFile);
        return true;
    }
    // 工厂模式: 删除标志文件(本就不存在视为已达成)
    if (unlink(kUserModeFile) != 0 && errno != ENOENT) {
        ALOGE("pdm-hidl: applyUserModeFile: unlink %s failed: %s",
              kUserModeFile, strerror(errno));
        return false;
    }
    ALOGI("pdm-hidl: applyUserModeFile: user work_mode OFF, removed %s", kUserModeFile);
    return true;
}

// init property service 的 IsWhitelistedRoProperty()(system/core/init/
// property_service.cpp)只在该属性 == "1" 时放行 whitelist_prop.txt 白名单内的
// ro.* 属性运行期改写。syncKeyToProperty 期间临时打开这个门,对象析构时(无论
// 成功失败、含提前 return)关回 "0",保证白名单 ro.* 平时只能经 pdm 通道
// (pdm.hidl.cli -> pdm-hidl 服务 -> 本函数)修改。
// 该属性已在 vendor/starnet/sepolicy/property_contexts 映射 vendor_persist_prop
// (pdm_hidl.te 已 allow set);property_set 经 property service socket 由 init
// 串行处理,先置 1 后写 ro.* 的顺序有保证。
class RoWhiteListGate {
  public:
    RoWhiteListGate() {
        int rc = property_set(kRoWhiteListEnableProp, "1");
        if (rc != 0) {
            ALOGW("pdm-hidl: RoWhiteListGate: enable %s failed rc=%d",
                  kRoWhiteListEnableProp, rc);
        }
    }
    ~RoWhiteListGate() {
        int rc = property_set(kRoWhiteListEnableProp, "0");
        if (rc != 0) {
            ALOGW("pdm-hidl: RoWhiteListGate: disable %s failed rc=%d",
                  kRoWhiteListEnableProp, rc);
        }
    }

  private:
    static constexpr const char* kRoWhiteListEnableProp = "persist.ro.white_list.enable";
};

PdmHal::PdmHal() {
    ALOGI("pdm-hidl: PdmHal() constructor start, begin boot-time sync from unifykeys");
    std::vector<Entry> entries;
    if (!readAllEntries(&entries)) {
        ALOGE("pdm-hidl: PdmHal() init sync failed, entries will be left untouched");
        return;
    }
    ALOGI("pdm-hidl: PdmHal() read %zu entries, now syncing to properties", entries.size());
    for (const auto& e : entries) {
        syncKeyToProperty(std::string(e.name.c_str(), e.name.size()),
                          std::string(e.value.c_str(), e.value.size()));
    }
    ALOGI("pdm-hidl: PdmHal() constructor done, boot sync complete");
}

bool PdmHal::readAllEntries(std::vector<Entry>* entries) {
    static const char* kKeys[] = {"mac",   "mac_wifi", "sn",      "stbid",       "cmccpwd",
                                  "cmei",  "device_id", "work_mode",   "cmcc_date",   "product_token",
                                  "andlink_token",
                                  "manufacturer", "brand", "model", "device",
                                  "cmcc_brand", "cmcc_ims_app_id", "cmcc_ims_secret"};
    const int kKeyCount = sizeof(kKeys) / sizeof(kKeys[0]);
    ALOGD("pdm-hidl: readAllEntries() start, total keys=%d", kKeyCount);
    // 整个循环持一把锁,与多客户端并发互斥
    std::unique_ptr<Lock> lock = Lock::create();
    if (!lock) {
        ALOGE("pdm-hidl: readAllEntries: failed to lock %s", kUnifyKeyLock);
        return false;
    }
    entries->clear();
    int fallbackProp = 0, fallbackDef = 0;
    for (const char* key : kKeys) {
        bool ok = true;
        if (!writeFile(kUnifyKeyName, key)) {
            ALOGW("pdm-hidl: readAllEntries: write name '%s' to %s failed, skip unifykey",
                  key, kUnifyKeyName);
            ok = false;
        }
        std::string value;
        if (ok && !readFile(kUnifyKeyRead, &value)) {
            ALOGW("pdm-hidl: readAllEntries: read value of '%s' from %s failed, skip unifykey",
                  key, kUnifyKeyRead);
            ok = false;
        }
        std::string trimmed = trimTail(value);
        // 与 getValue 保持相同 fallback 链: unifykeys 空 → property → default.
        // 避免 list 时所有值全空而 get model 却能取到 TB712 的不一致症状(0905实测)
        if (!ok || trimmed.empty()) {
            const auto* item = findMapItem(key);
            if (item != NULL) {
                std::string pv;
                if (getPropertyString(item->prop, &pv) && !pv.empty()) {
                    trimmed = pv;
                    fallbackProp++;
                } else if (item->def != NULL && item->def[0] != '\0') {
                    trimmed = item->def;
                    fallbackDef++;
                }
            }
        }
        // work_mode 特殊: 与 getValue 保持同一来源(标志文件存在性),避免 list 显示
        // unifykey/property 的旧值而 get work_mode 显示文件真值的"两处不一致"
        if (strcmp(key, kModeKey) == 0) {
            trimmed = isUserMode() ? kModeUser : kModeFactory;
        }
        Entry entry;
        entry.name = key;
        entry.value = trimmed;
        entries->push_back(std::move(entry));
    }
    lock.reset();
    ALOGI("pdm-hidl: readAllEntries() done, total %zu entries (hitUnify=%zu hitProp=%d hitDef=%d)",
          entries->size(), entries->size() - fallbackProp - fallbackDef, fallbackProp, fallbackDef);
    return true;
}

void PdmHal::syncKeyToProperty(const std::string& name, const std::string& value) {
    ALOGD("pdm-hidl: syncKeyToProperty(name='%s', value='%s') start", name.c_str(), value.c_str());
    // 打开白名单改写门,离开本函数(含失败路径)由 RoWhiteListGate 析构自动关闭
    RoWhiteListGate whiteListGate;
    int matched = 0;
    for (const auto& item : kKeyPropMap) {
        if (name != item.key) continue;
        matched++;
        const char* v = value.empty() ? item.def : value.c_str();
        ALOGD("pdm-hidl: syncKeyToProperty: mapping key='%s' -> prop='%s' value='%s'",
              name.c_str(), item.prop, v);
        if (property_set(item.prop, v) != 0) {
            ALOGW("pdm-hidl: syncKeyToProperty: setprop %s=%s failed (ro.* 已被 init 冻结时属正常)",
                  item.prop, v);
        } else {
            ALOGI("pdm-hidl: syncKeyToProperty: setprop %s=%s OK", item.prop, v);
        }
    }
    if (matched == 0) {
        ALOGW("pdm-hidl: syncKeyToProperty: key='%s' has no property mapping, skipped",
              name.c_str());
    }
}

PdmStatus PdmHal::setValueInternal(const std::string& name, const std::string& value,
                                   bool* result) {
    ALOGI("pdm-hidl: setValue(name='%s', value='%s') enter", name.c_str(), value.c_str());
    *result = false;
    // 1. 先做参数合法性校验(避免上一轮 name='' 的假 Success):
    //    name 必须非空 且 存在于 kKeyPropMap; 否则直接返回 NAME_NOT_FOUND,
    //    不会再 writeFile 空字符串到 sysfs, 也不会给上层误报 result=true.
    if (name.empty()) {
        ALOGE("pdm-hidl: setValue: empty name -> NAME_NOT_FOUND");
        return PdmStatus::NAME_NOT_FOUND;
    }
    if (findMapItem(name) == NULL) {
        ALOGE("pdm-hidl: setValue: unknown name='%s' not in kKeyPropMap -> NAME_NOT_FOUND",
              name.c_str());
        return PdmStatus::NAME_NOT_FOUND;
    }
    // work_mode 是模式开关而非普通键值: 取值必须是 "0"(工厂)/"1"(用户)。
    if (name == kModeKey && !isValidModeValue(value)) {
        ALOGE("pdm-hidl: setValue: work_mode value '%s' invalid, expect '0'(factory)/'1'(user)"
              " -> BAD_VALUE", value.c_str());
        return PdmStatus::BAD_VALUE;
    }
    std::unique_ptr<Lock> lock = Lock::create();
    if (!lock) {
        ALOGE("pdm-hidl: setValue: lock failed, return PERMISSION_DENIED");
        return PdmStatus::PERMISSION_DENIED;
    }
    // unifykeys 协议:先写 key 名,再写值
    if (!writeFile(kUnifyKeyName, name)) {
        ALOGE("pdm-hidl: setValue: write name '%s' to %s failed: %s",
              name.c_str(), kUnifyKeyName, strerror(errno));
        return PdmStatus::UNKNOWN_ERROR;
    }
    if (!writeFile(kUnifyKeyWrite, value)) {
        ALOGE("pdm-hidl: setValue: write value '%s' to %s failed: %s",
              value.c_str(), kUnifyKeyWrite, strerror(errno));
        return PdmStatus::UNKNOWN_ERROR;
    }
    lock.reset();  // 解锁后再同步属性,缩短持锁时间

    syncKeyToProperty(name, trimTail(value));

    if (name == kModeKey) {
        if (!applyUserModeFile(trimTail(value))) {
            ALOGE("pdm-hidl: setValue: apply user work_mode flag for work_mode='%s' failed"
                  " -> UNKNOWN_ERROR", value.c_str());
            return PdmStatus::UNKNOWN_ERROR;
        }
    }

    *result = true;
    ALOGI("pdm-hidl: setValue(name='%s') success, result=true", name.c_str());
    return PdmStatus::OK;
}

PdmStatus PdmHal::getValueInternal(const std::string& name, std::string* value) {
    ALOGI("pdm-hidl: getValue(name='%s') enter", name.c_str());
    *value = "";
    if (name.empty()) {
        ALOGE("pdm-hidl: getValue: empty name -> NAME_NOT_FOUND");
        return PdmStatus::NAME_NOT_FOUND;
    }
    const auto* item = findMapItem(name);
    if (item == NULL) {
        ALOGE("pdm-hidl: getValue: unknown name='%s' not in kKeyPropMap -> NAME_NOT_FOUND",
              name.c_str());
        return PdmStatus::NAME_NOT_FOUND;
    }
    // work_mode 特殊: 模式真值在标志文件里(系统其余部分也只看文件),直接据此返回,
    if (name == kModeKey) {
        *value = isUserMode() ? kModeUser : kModeFactory;
        ALOGI("pdm-hidl: getValue(name='work_mode') from user work_mode flag file '%s', value='%s'",
              kUserModeFile, value->c_str());
        return PdmStatus::OK;
    }

    std::unique_ptr<Lock> lock = Lock::create();
    if (!lock) {
        ALOGE("pdm-hidl: getValue: lock failed, return PERMISSION_DENIED");
        return PdmStatus::PERMISSION_DENIED;
    }
    // 1. 先查 unifykeys 驱动
    bool unifyOk = true;
    if (!writeFile(kUnifyKeyName, name)) {
        ALOGW("pdm-hidl: getValue: write name '%s' to %s failed, skip unifykeys",
              name.c_str(), kUnifyKeyName);
        unifyOk = false;
    }
    std::string v;
    if (unifyOk && !readFile(kUnifyKeyRead, &v)) {
        ALOGW("pdm-hidl: getValue: read '%s' from %s failed, skip unifykeys",
              name.c_str(), kUnifyKeyRead);
        unifyOk = false;
    }
    v = trimTail(v);
    lock.reset();

    // 2. Fallback 链: unifykeys 空 → 读系统 property → 用 kKeyPropMap 默认值.
    //    因为板上首次开机或 unifykeys 未烧录时, readFile 会空, 但 getprop
    //    ro.product.model 已存在(0905实测=TB712)
    if (unifyOk && !v.empty()) {
        ALOGD("pdm-hidl: getValue(name='%s') hit unifykeys len=%zu", name.c_str(), v.size());
    } else {
        // 读 key 映射的第一个 property(ro.product.model 等), 若有值直接用
        if (getPropertyString(item->prop, &v) && !v.empty()) {
            ALOGD("pdm-hidl: getValue(name='%s') fallback prop '%s'='%s'",
                  name.c_str(), item->prop, v.c_str());
        } else if (item->def != NULL && item->def[0] != '\0') {
            v = item->def;
            ALOGD("pdm-hidl: getValue(name='%s') fallback default='%s'", name.c_str(), v.c_str());
        } else {
            v.clear();
            ALOGD("pdm-hidl: getValue(name='%s') no default, return empty", name.c_str());
        }
    }
    *value = v;
    ALOGI("pdm-hidl: getValue(name='%s') done, value_len=%zu value='%s'",
          name.c_str(), value->size(), value->c_str());
    return PdmStatus::OK;
}

// ---------------- HIDL 方法:业务实现 + 回调 -------------------------------
// HIDL 的 Return<T> 只反映传输层错误,业务结果一律经 _hidl_cb 回传
// (与旧实现的 status_t 返回 + 出参一一对应)。

Return<void> PdmHal::setValue(const hidl_string& name, const hidl_string& value,
                              setValue_cb _hidl_cb) {
    bool result = false;
    PdmStatus status = setValueInternal(std::string(name.c_str(), name.size()),
                                        std::string(value.c_str(), value.size()), &result);
    ALOGD("pdm-hidl: IPdm::setValue returned status=%d result=%d",
          (int)status, result ? 1 : 0);
    _hidl_cb(status, result);
    return Void();
}

Return<void> PdmHal::getValue(const hidl_string& name, getValue_cb _hidl_cb) {
    std::string value;
    PdmStatus status = getValueInternal(std::string(name.c_str(), name.size()), &value);
    ALOGD("pdm-hidl: IPdm::getValue returned status=%d value_len=%zu",
          (int)status, value.size());
    _hidl_cb(status, value);
    return Void();
}

Return<void> PdmHal::getEntries(getEntries_cb _hidl_cb) {
    ALOGI("pdm-hidl: IPdm::getEntries() enter");
    std::vector<Entry> entries;
    if (!readAllEntries(&entries)) {
        ALOGE("pdm-hidl: IPdm::getEntries() failed");
        _hidl_cb(PdmStatus::UNKNOWN_ERROR, hidl_vec<Entry>());
        return Void();
    }
    ALOGI("pdm-hidl: IPdm::getEntries() success, %zu entries returned", entries.size());
    _hidl_cb(PdmStatus::OK, hidl_vec<Entry>(entries));
    return Void();
}

}  // namespace implementation
}  // namespace V1_0
}  // namespace pdm
}  // namespace starnet
}  // namespace vendor
```

### 7.10 `service/UnifyKeys.h`

```cpp
// Copyright (C) 2026-2026 福建星网锐捷通讯股份有限公司. All rights reserved.
#pragma once

// Amlogic unifykeys(kernel: /sys/class/unifykeys)节点。
// 与 vendor/starnet/pdm/UnifyKeys.h 保持一致(设备节点路径固定)。
constexpr const char* kUnifyKeyName  = "/sys/class/unifykeys/name";
constexpr const char* kUnifyKeyWrite = "/sys/class/unifykeys/write";
constexpr const char* kUnifyKeyRead  = "/sys/class/unifykeys/read";
constexpr const char* kUnifyKeyLock  = "/sys/class/unifykeys/lock";
```

### 7.11 `service/Lock.h`

```cpp
// Copyright (C) 2026-2026 福建星网锐捷通讯股份有限公司. All rights reserved.
#pragma once

#include <memory>

namespace vendor {
namespace starnet {
namespace pdm {
namespace V1_0 {
namespace implementation {

// 用户态互斥:对 /sys/class/unifykeys/lock 节点 fd 做 flock,
// 仅用于多客户端并发串行化,不会真正写 lock 节点(不锁存储)。
// 逻辑与 vendor/starnet/pdm/Lock.{h,cpp} 一致,仅命名空间/日志前缀不同。
class Lock {
  public:
    static std::unique_ptr<Lock> create();
    ~Lock();

  private:
    Lock();

    int fd = -1;
    bool locked = false;
};

}  // namespace implementation
}  // namespace V1_0
}  // namespace pdm
}  // namespace starnet
}  // namespace vendor
```

### 7.12 `service/Lock.cpp`

```cpp
// Copyright (C) 2026-2026 福建星网锐捷通讯股份有限公司. All rights reserved.

#include "Lock.h"

#include <errno.h>
#include <fcntl.h>
#include <log/log.h>
#include <string.h>
#include <unistd.h>

#include <memory>

#include "UnifyKeys.h"

namespace vendor {
namespace starnet {
namespace pdm {
namespace V1_0 {
namespace implementation {

std::unique_ptr<Lock> Lock::create() {
    ALOGD("pdm-hidl: Lock::create() enter, lock_path=%s", kUnifyKeyLock);
    auto lock = std::unique_ptr<Lock>(new Lock());

    lock->fd = open(kUnifyKeyLock, O_WRONLY);
    if (lock->fd < 0) {
        ALOGE("pdm-hidl: Lock::create open %s failed: %s", kUnifyKeyLock, strerror(errno));
        return nullptr;
    }

    struct flock flock;
    flock.l_whence = SEEK_SET;
    flock.l_start = 0;
    flock.l_len = 0;
    flock.l_type = F_WRLCK;
    flock.l_pid = -1;

    if (fcntl(lock->fd, F_SETLKW, &flock) < 0) {
        ALOGE("pdm-hidl: Lock::create fcntl(F_SETLKW) failed on %s: %s",
              kUnifyKeyLock, strerror(errno));
        close(lock->fd);
        lock->fd = -1;
        return nullptr;
    }

    lock->locked = true;
    ALOGD("pdm-hidl: Lock::create() success, lock acquired on %s (fd=%d)",
          kUnifyKeyLock, lock->fd);
    return lock;
}

Lock::Lock() {
    ALOGD("pdm-hidl: Lock() constructor, fd=%d locked=%d", fd, locked ? 1 : 0);
}

Lock::~Lock() {
    ALOGD("pdm-hidl: ~Lock() destructor enter, fd=%d locked=%d", fd, locked ? 1 : 0);
    if (fd < 0) {
        return;
    }

    if (locked) {
        struct flock flock;
        flock.l_whence = SEEK_SET;
        flock.l_start = 0;
        flock.l_len = 0;
        flock.l_type = F_UNLCK;
        flock.l_pid = -1;

        int ret = fcntl(fd, F_SETLK, &flock);
        if (ret < 0) {
            ALOGE("pdm-hidl: ~Lock() fcntl(F_SETLK, F_UNLCK) failed: %s", strerror(errno));
        }
        locked = false;
    }

    close(fd);
    fd = -1;
}

}  // namespace implementation
}  // namespace V1_0
}  // namespace pdm
}  // namespace starnet
}  // namespace vendor
```

### 7.13 `cli/Android.bp`

```blueprint
// 与旧 pdm.cli 一样装到 /system/bin:链接的是 HIDL 接口库的 system 变体
// (hidl_interface 生成的模块自带 vendor_available,两侧都有 .so),
// 因此从 adb/串口 shell 直接执行即可,进程仍是 shell 域。
cc_binary {
    name: "pdm.hidl.cli",
    srcs: [
        "main.cpp",
    ],
    shared_libs: [
        "vendor.starnet.pdm@1.0",
        "libbase",
        "libhidlbase",
        "libhidltransport",
        "libhwbinder",
        "libutils",
        "liblog",
    ],
    cflags: [
        "-DLOG_TAG=\"pdm-hidl-cli\"",
    ],
}
```

### 7.14 `cli/main.cpp`

```cpp
// Copyright (C) 2026-2026 福建星网锐捷通讯股份有限公司. All rights reserved.
//
// pdm.hidl.cli:旧 pdm.cli 的 HIDL 版本,命令行/输出格式保持一致:
//   pdm.hidl.cli set <name> <value>
//   pdm.hidl.cli get <name>
//   pdm.hidl.cli list
// 客户端侧不再手写 Parcel:IPdm::getService() 从 hwservicemanager 拿服务,
// 收发由 hidl-gen 生成的 BpHwIPdm 负责。

#include <vendor/starnet/pdm/1.0/IPdm.h>

#include <log/log.h>
#include <stdio.h>
#include <string.h>
#include <unistd.h>

#include <string>
#include <vector>

using ::vendor::starnet::pdm::V1_0::Entry;
using ::vendor::starnet::pdm::V1_0::IPdm;
using ::vendor::starnet::pdm::V1_0::PdmStatus;

static void printUsage(const char* prog) {
    printf("Usage:\n"
           "  %s set <name> <value>   - 设置键值对(烧写 unifykey)\n"
           "  %s get <name>           - 获取指定键的值\n"
           "  %s list                 - 获取所有条目\n",
           prog, prog, prog);
}

static const char* statusName(PdmStatus status) {
    switch (status) {
        case PdmStatus::OK:
            return "OK";
        case PdmStatus::NAME_NOT_FOUND:
            return "NAME_NOT_FOUND";
        case PdmStatus::BAD_VALUE:
            return "BAD_VALUE";
        case PdmStatus::PERMISSION_DENIED:
            return "PERMISSION_DENIED";
        case PdmStatus::UNKNOWN_ERROR:
            return "UNKNOWN_ERROR";
    }
    return "UNRECOGNIZED";
}

// 去掉尾部换行后打印,避免 sysfs 值自带 '\n' 时输出多空行
static std::string stripTail(const std::string& s) {
    size_t end = s.size();
    while (end > 0 && (s[end - 1] == '\n' || s[end - 1] == '\r')) end--;
    return s.substr(0, end);
}

int main(int argc, char* argv[]) {
    ALOGI("pdm.hidl.cli: main() argc=%d, pid=%d uid=%d", argc, getpid(), getuid());
    for (int i = 0; i < argc; i++) {
        ALOGD("pdm.hidl.cli: argv[%d]='%s'", i, argv[i]);
    }

    if (argc < 2) {
        ALOGW("pdm.hidl.cli: no command provided");
        printUsage(argv[0]);
        return 1;
    }

    // 绑定式 HAL:从 hwservicemanager 取 vendor.starnet.pdm@1.0::IPdm/default
    // (内部会短暂重试等待服务就绪)
    ::android::sp<IPdm> pdm = IPdm::getService();
    if (pdm == nullptr) {
        fprintf(stderr,
                "Error: service 'vendor.starnet.pdm@1.0::IPdm/default' not found"
                " (pdm-hidl 服务未运行或 SELinux 拦截?)\n");
        ALOGE("pdm.hidl.cli: IPdm::getService() returned NULL");
        return 1;
    }

    std::string command = argv[1];
    ALOGI("pdm.hidl.cli: executing command='%s'", command.c_str());

    if (command == "set" && argc == 4) {
        ALOGI("pdm.hidl.cli: setValue name='%s' value='%s'", argv[2], argv[3]);
        PdmStatus status = PdmStatus::UNKNOWN_ERROR;
        bool result = false;
        auto ret = pdm->setValue(argv[2], argv[3],
                                 [&](PdmStatus st, bool r) { status = st; result = r; });
        if (!ret.isOk()) {
            fprintf(stderr, "Error: setValue transact failed: %s\n", ret.description().c_str());
            ALOGE("pdm.hidl.cli: setValue transact failed: %s", ret.description().c_str());
            return 1;
        }
        if (status != PdmStatus::OK || !result) {
            fprintf(stderr, "Error: setValue(%s) failed, status=%s(%d)\n",
                    argv[2], statusName(status), (int)status);
            ALOGE("pdm.hidl.cli: setValue failed status=%d", (int)status);
            return 1;
        }
        printf("Success: set %s=%s\n", argv[2], argv[3]);
        ALOGI("pdm.hidl.cli: set %s=%s SUCCESS", argv[2], argv[3]);
    } else if (command == "get" && argc == 3) {
        ALOGI("pdm.hidl.cli: getValue name='%s'", argv[2]);
        PdmStatus status = PdmStatus::UNKNOWN_ERROR;
        std::string value;
        auto ret = pdm->getValue(argv[2], [&](PdmStatus st, const ::android::hardware::hidl_string& v) {
            status = st;
            value.assign(v.c_str(), v.size());
        });
        if (!ret.isOk()) {
            fprintf(stderr, "Error: getValue transact failed: %s\n", ret.description().c_str());
            ALOGE("pdm.hidl.cli: getValue transact failed: %s", ret.description().c_str());
            return 1;
        }
        if (status != PdmStatus::OK) {
            fprintf(stderr, "Error: getValue(%s) failed, status=%s(%d)\n",
                    argv[2], statusName(status), (int)status);
            ALOGE("pdm.hidl.cli: getValue failed status=%d", (int)status);
            return 1;
        }
        printf("%s=%s\n", argv[2], stripTail(value).c_str());
        ALOGI("pdm.hidl.cli: get %s='%s'", argv[2], stripTail(value).c_str());
    } else if (command == "list" && argc == 2) {
        ALOGI("pdm.hidl.cli: getEntries()");
        PdmStatus status = PdmStatus::UNKNOWN_ERROR;
        std::vector<Entry> entries;
        auto ret = pdm->getEntries(
                [&](PdmStatus st, const ::android::hardware::hidl_vec<Entry>& list) {
                    status = st;
                    entries.assign(list.begin(), list.end());
                });
        if (!ret.isOk()) {
            fprintf(stderr, "Error: getEntries transact failed: %s\n", ret.description().c_str());
            ALOGE("pdm.hidl.cli: getEntries transact failed: %s", ret.description().c_str());
            return 1;
        }
        if (status != PdmStatus::OK) {
            fprintf(stderr, "Error: getEntries failed, status=%s(%d)\n",
                    statusName(status), (int)status);
            ALOGE("pdm.hidl.cli: getEntries failed status=%d", (int)status);
            return 1;
        }
        for (const auto& e : entries) {
            printf("%s=%s\n", std::string(e.name.c_str(), e.name.size()).c_str(),
                   stripTail(std::string(e.value.c_str(), e.value.size())).c_str());
        }
        ALOGI("pdm.hidl.cli: listed %zu entries", entries.size());
    } else {
        ALOGW("pdm.hidl.cli: unknown/invalid command='%s' argc=%d", command.c_str(), argc);
        printUsage(argv[0]);
        return 1;
    }

    ALOGI("pdm.hidl.cli: main() done, exit 0");
    return 0;
}
```

### 7.15 `sepolicy/pdm_hidl.te`

```te
# pdm-hidl:绑定式 HIDL 版本的 unifykey 键值服务(vendor.starnet.pdm@1.0::IPdm/default)
#
# 与 vendor/starnet/pdm/sepolicy/pdm_service.te 功能等价,三处刻意不同:
#  1) 本服务是标准 Treble HAL 域(带 halserverdomain 属性,走 /dev/hwbinder),
#     故用 hal_attribute/hal_server_domain/add_hwservice 宏;
#  2) 不能照抄 pdm_service.te 的 "allow ... vendor_file:file execute_no_trans"
#     —— system/sepolicy/public/hal_neverallows.te 有
#        neverallow { halserverdomain ... } { file_type fs_type }:file execute_no_trans;
#     (本服务不 exec 任何东西,不需要该权限);
#  3) .rc 里不写 seclabel —— 同上 neverallows 有
#        neverallow * halserverdomain:process dyntransition;
#     域转换由下面 file_contexts 里的 pdm_hidl_default_exec + init_daemon_domain 在 exec 时完成。

type pdm_hidl_default, domain;
type pdm_hidl_default_exec, exec_type, vendor_file_type, file_type;
init_daemon_domain(pdm_hidl_default)

# hwservicemanager 里 "vendor.starnet.pdm::IPdm" 对应的标签(见 hwservice_contexts)
type pdm_hidl_hwservice, hwservice_manager_type;

hal_attribute(pdm_hidl)
hal_server_domain(pdm_hidl_default, hal_pdm_hidl)

# 注册 HAL(含 android.hidl.base::IBase 这一层,故宏里同时放行 hidl_base_hwservice)
add_hwservice(pdm_hidl_default, pdm_hidl_hwservice)

# --- 业务权限:照抄 pdm_service.te(除 execute_no_trans 外)--------------------
# unifykeys 内核节点 /sys/class/unifykeys/*
allow pdm_hidl_default sysfs_unifykey:dir search;
allow pdm_hidl_default sysfs_unifykey:file { open read write getattr };

# 用户模式标志文件 /mnt/vendor/param/usermode(存在=用户模式, 不存在=工厂模式):
# set/get mode 需在该目录创建/删除/读写该文件;
# 目录与文件均标 param_tv_file(device/amlogic/common/sepolicy/file_contexts)
allow pdm_hidl_default param_tv_file:dir { search read write open add_name remove_name getattr };
allow pdm_hidl_default param_tv_file:file { create open read write setattr getattr unlink };

# 属性同步(ro.product.cmcc_* / ro.mac / ro.device.work.mode 等,类型定义见
# device/amlogic/common/sepolicy/property.te;persist.ro.white_list.enable 见
# vendor/starnet/sepolicy/property_contexts)
allow pdm_hidl_default vendor_persist_prop:property_service set;
allow pdm_hidl_default vendor_platform_prop:property_service set;
allow pdm_hidl_default vendor_app_prop:property_service set;
# ro.* / persist.* 的 property_set 要经 property service socket 由 init 处理
# (参考 device/amlogic/common/sepolicy/hal_audio_default.te 同样写法)
allow pdm_hidl_default property_socket:sock_file write;
allow pdm_hidl_default init:unix_stream_socket connectto;

# --- 客户端:CLI(pdm.hidl.cli)以 shell 域从 adb/串口执行 ----------------------
# shell 已有 hwbinder_use(system/sepolicy/public/shell.te:117),故只需
# "找得到 hwservice + 能调用 HAL 服务进程"
allow shell pdm_hidl_hwservice:hwservice_manager find;
allow shell pdm_hidl_default:binder { call transfer };
allow pdm_hidl_default shell:binder transfer;
```

### 7.16 `sepolicy/file_contexts` / `sepolicy/hwservice_contexts`

```
/vendor/bin/hw/vendor.starnet.pdm@1.0-service    u:object_r:pdm_hidl_default_exec:s0
```

```
# hwservicemanager 按 <package>::<interface> 查标签(带版本号的部分会被去掉,
# 见 system/hwservicemanager/AccessControl.cpp canAdd/canGet)
vendor.starnet.pdm::IPdm    u:object_r:pdm_hidl_hwservice:s0
```

### 7.17 `manifest/vendor.starnet.pdm@1.0.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<!--
  pdm-hidl 的 VINTF 清单片段(<hal format="hidl">),对应
  vendor/starnet/pdm-hidl/interfaces/pdm/1.0/IPdm.hal。

  已并入 device manifest:device/amlogic/p291_iptv/BoardConfig.mk 里
      DEVICE_MANIFEST_FILE += vendor/starnet/pdm-hidl/manifest/vendor.starnet.pdm@1.0.xml
  (DEVICE_MANIFEST_FILE 支持多文件,build/make/target/board/Android.mk:35,
   由 assemble_vintf 与板级 manifest 合并成 /vendor/etc/vintf/manifest.xml。)

  ⚠️ 该 += 必须放在 BoardConfig 里 AUTO_PATCH_AB / AUTO_PATCH_NPU 两个 $(shell ...) 之后:
     那两个脚本(ab_update.sh / npu_update.sh)会 sed -i 就地修改 $(DEVICE_MANIFEST_FILE)
     列出的每个文件,放前面会被它们一并处理。

  注意:本产品未开 PRODUCT_ENFORCE_VINTF_MANIFEST(=不强制),不并入也能正常注册/调用,
  只影响 lshal / checkvintf 的清单一致性。
-->
<manifest version="1.0" type="device">
    <hal format="hidl">
        <name>vendor.starnet.pdm</name>
        <transport>hwbinder</transport>
        <version>1.0</version>
        <interface>
            <name>IPdm</name>
            <instance>default</instance>
        </interface>
    </hal>
</manifest>
```

---

## 8. 编译验证（实测记录）

环境：`p291_iptv` / userdebug，本机增量 `out/`（2026-09-10）。

### 8.1 模块编译

```bash
TARGET_PRODUCT=p291_iptv TARGET_BUILD_VARIANT=userdebug \
  build/soong/soong_ui.bash --make-mode \
  vendor.starnet.pdm@1.0 vendor.starnet.pdm@1.0-service pdm.hidl.cli
# → BUILD_EXIT=0
```

首次编译曾暴露一个真实错误（已修）：

```
vendor/starnet/pdm-hidl/service/main.cpp:13:37: error: no member named 'implementation' in
        namespace 'vendor::starnet::pdm::V1_0'
vendor/starnet/pdm-hidl/service/main.cpp:22:39: error: unknown type name 'PdmHal'
```

→ 原因：`main.cpp` 缺少 `#include "PdmHal.h"`。修复后 0 error
（`PdmHal.cpp`、`cli/main.cpp`、hidl-gen 生成的 `PdmAll.cpp`/`types.cpp` 首次即通过）。

### 8.2 产物落点（实测）

```
out/target/product/p291_iptv/vendor/bin/hw/vendor.starnet.pdm@1.0-service     ✓
out/target/product/p291_iptv/vendor/etc/init/vendor.starnet.pdm@1.0-service.rc ✓
out/target/product/p291_iptv/vendor/lib/libvendor.starnet.pdm@1.0.so           ✓
out/target/product/p291_iptv/system/bin/pdm.hidl.cli                           ✓
out/target/product/p291_iptv/system/lib/libvendor.starnet.pdm@1.0.so           ✓
```

（与旧的 `vendor/bin/pdm-service`、`system/bin/pdm.cli` 并存，互不覆盖。）

### 8.3 SELinux 策略编译

```bash
build/soong/soong_ui.bash --make-mode selinux_policy     # → POLICY_EXIT=0（neverallow 全量检查通过）
```

策略内容实测：

| 文件 | 结果 |
|---|---|
| `vendor/etc/selinux/vendor_sepolicy.cil` | `pdm_hidl` 相关 48 处 |
| `vendor/etc/selinux/vendor_file_contexts` | `/vendor/bin/hw/vendor.starnet.pdm@1.0-service → u:object_r:pdm_hidl_default_exec:s0` |
| `vendor/etc/selinux/vendor_hwservice_contexts` | `vendor.starnet.pdm::IPdm → u:object_r:pdm_hidl_hwservice:s0` |

关键 allow 规则（从编译出的 cil 摘录）：

```
(allow pdm_hidl_default pdm_hidl_hwservice (hwservice_manager (add find)))
(allow pdm_hidl_default hidl_base_hwservice_28_0 (hwservice_manager (add)))
(allow pdm_hidl_default sysfs_unifykey (file (read write getattr open)))
(allow pdm_hidl_default vendor_persist_prop (property_service (set)))
(allow pdm_hidl_default param_tv_file (file (read write create getattr setattr unlink open)))
(allow pdm_hidl_default property_socket_28_0 (sock_file (write)))
(allow pdm_hidl_default init_28_0 (unix_stream_socket (connectto)))
(allow shell_28_0 pdm_hidl_default (binder (call transfer)))
(allow pdm_hidl_default shell_28_0 (binder (transfer)))
```

### 8.4 VINTF 清单

```bash
build/soong/soong_ui.bash --make-mode device_manifest.xml   # → MANIFEST_EXIT=0
```

`out/target/product/p291_iptv/vendor/etc/vintf/manifest.xml` 中：

```xml
357:        <name>vendor.starnet.pdm</name>
358-        <transport>hwbinder</transport>
359-        <version>1.0</version>
360-        <interface>
361-            <name>IPdm</name>
362-            <instance>default</instance>
363-        </interface>
364-        <fqname>@1.0::IPdm/default</fqname>     ← assemble_vintf 合并时自动补全
```

### 8.5 尚未验证

- **整包 `make otapackage`**：需跑 `./tb611_s905L3ap_p291.sh -v 1.0.1 -k 1`（约 1 小时）；
  本机验证时另有一个 otapackage 构建占用 `out/.lock`，只做了模块级验证。
- **上板运行**：见第 9 节命令。

---

## 9. 上板验证与排查

```bash
# 1. 服务是否注册(应列出 vendor.starnet.pdm@1.0::IPdm/default)
adb shell lshal | grep vendor.starnet

# 2. 基本读写
adb shell pdm.hidl.cli get model                 # → TB712(或实际值)
adb shell pdm.hidl.cli set mac 00:11:22:33:44:55
adb shell pdm.hidl.cli list
adb shell getprop ro.mac                         # 验证属性同步

# 3. work_mode(标志文件语义)
adb shell 'echo 1 > /mnt/vendor/param/usermode; pdm.hidl.cli get work_mode'   # → 1
adb shell pdm.hidl.cli set work_mode 0           # 工厂模式(删除标志文件)

# 4. 日志
logcat -s pdm-hidl pdm-hidl-cli hwservicemanager
adb shell getenforce                             # 建议先确认是否 Enforcing
```

排查要点：

| 现象 | 可能原因 / 处理 |
|---|---|
| `service ... not found` | 服务没起来或 SELinux 拦截：`logcat -s hwservicemanager init pdm-hidl` 看 `add` 是否被拒；确认 init 服务名 `vendor.pdm-hidl-1-0` 及 `/vendor/bin/hw/...` 是否存在 |
| 出现 `avc: denied` | 按 denial 补 `sepolicy/pdm_hidl.te`（`dmesg | grep avc`） |
| `setprop` 失败/属性没变 | 确认 `/vendor/starnet/whitelist_prop.txt` 含该属性；`vendor/starnet/sepolicy/property_contexts` 里 `persist.ro.white_list.enable` 映射存在（旧 pdm 依赖同一机制） |
| 策略整体没生效 | 检查 `BOARD_SEPOLICY_DIRS` 是否在 board-config 阶段（见 4.2 坑 2） |

---

## 10. 与旧实现的差异、限制与遗留问题

### 10.1 不再需要的东西

- **手写 Parcel / interface token 探测**：`IPdm.cpp` 里 `probeTokenLenOffset()`、
  `roundUp4()`、手工 `writeInterfaceToken`、`readString16Inplace` 等整套兼容代码，
  在 HIDL 下由 hidl-gen 生成的 `Bp*`/`Bn*` 桩接管。
- **自己注册 `service_manager_type` + `service_contexts`**：改为 hwservicemanager 的
  `hwservice_contexts`。
- **客户端用 `servicemanager` + `interface_cast`**：改为 `IPdm::getService()`。

### 10.2 App 不能直接调用本 HAL（重要）

Treble 的 `system/sepolicy/private/app_neverallows.te:245-258` 对
`all_untrusted_apps` 调用 `halserverdomain` 有 neverallow —— 普通 App（含 Java 侧）
拿不到 `find`/`call` 权限。因此：

- 上层若是 **native/system 进程**（C++ 链接 `vendor.starnet.pdm@1.0`）：可直接调用；
- 上层若是 **App**：需要保留旧 binder 服务、或另加一个 system service/manager 转发；
  若确实要让 `system_app`/`platform_app` 走 HIDL，需在 `pdm_hidl.te` 里按类型补
  `hwservice_manager find` 与 binder `call`（并确认该类 App 不在 neverallow 覆盖面内）。

### 10.3 两个服务并存时的行为

两边都会在开机时各做一次属性同步（写入同样的值），unifykeys 侧有 flock 串行化，
不会互相破坏。若只想保留一个同步来源，停掉其中一个即可（见 10.5）。

### 10.4 遗留问题：旧 `pdm_service` 的 SELinux 策略一直没进镜像

实测编译产物 `vendor_sepolicy.cil` 里**没有** `pdm_service` 域（原因同 4.2 坑 2：
`BOARD_SEPOLICY_DIRS` 写在 `starnet.mk` 不生效）。也就是说旧 `pdm-service.rc` 里的
`seclabel u:r:pdm_service:s0` 在当前策略中**没有对应域**。

- 这条**没有在本方案里改动**（会改变旧服务的运行域，属行为变更）；
- 如果设备上旧 pdm 一直工作正常，说明设备可能运行在 permissive，或使用了别的策略来源；
- 如需一并修正：在 `device/amlogic/common/sepolicy.mk` 里追加
  `BOARD_SEPOLICY_DIRS += vendor/starnet/pdm/sepolicy` 即可（建议单独评估后再做）。

### 10.5 若要完全替换旧 pdm

1. `vendor/starnet/starnet.mk` 里注释掉 `PRODUCT_PACKAGES += pdm-service pdm.cli`；
2. 删除/停用 `vendor/starnet/pdm/pdm-service.rc`（整目录可保留做参考）；
3. 若上层 App 仍在用 `vendor.starnet.pdm` 私有协议，需先把调用方迁到 HIDL（见 10.2 限制）。

---

## 11. 附录：涉及的既有代码依据

| 结论 | 依据 |
|---|---|
| HIDL 包根/接口模块的写法 | `vendor/amlogic/common/interfaces/Android.bp`、`vendor/amlogic/common/interfaces/tvserver/1.0/Android.bp` |
| HIDL 接口库自动 `vendor_available`（两侧都有 .so） | `system/tools/hidl/build/hidl_interface.go:256` |
| 服务注册 API（`registerAsService` / `getService` / `_hidl_cb` 形态） | 本树生成物 `out/soong/.intermediates/.../ITvServer.h:52-53, 253-257` |
| HAL 服务 `.rc` 惯例（`class hal`、`/vendor/bin/hw/`、无 seclabel） | `vendor/amlogic/common/frameworks/services/subtiltleserver/subtitleserver.rc` 等 |
| `PRODUCT_PACKAGES` 在主阶段只读 | `build/make/core/main.mk:452`；实测报错见 4.2 |
| `BOARD_SEPOLICY_DIRS` 的生效时机 | 实测对比 `vendor_sepolicy.cil` 内容（见 4.2、8.3） |
| `hal_attribute` / `hal_server_domain` / `add_hwservice` 宏 | `system/sepolicy/public/te_macros:208, 228, 593` |
| 两条必须规避的 neverallow | `system/sepolicy/public/hal_neverallows.te:50, 59` |
| App 不能调用 HAL | `system/sepolicy/private/app_neverallows.te:245-258` |
| hwservice 标签查表规则（`<package>::<interface>`） | `system/hwservicemanager/AccessControl.cpp` `canAdd`/`canGet` |
| `shell` 已是 hwbinder 客户端 | `system/sepolicy/public/shell.te:117-118` |
| `DEVICE_MANIFEST_FILE` 支持多文件 | `build/make/target/board/Android.mk:35` |
| Android 9 libvintf 只读单 manifest 文件 | `system/libvintf/VintfObject.cpp:165-172` |
| 属性类型/白名单门 | `device/amlogic/common/sepolicy/property.te`、`vendor/starnet/sepolicy/property_contexts`、`vendor/starnet/whitelist_prop.txt` |
| 业务逻辑原始实现 | `vendor/starnet/pdm/PdmService.cpp`（映射表 22-59、work_mode 136-184、门 271-292、fallback 415-430） |
