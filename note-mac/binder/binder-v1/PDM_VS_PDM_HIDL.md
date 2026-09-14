# pdm / pdm-hidl 两种实现完整参考

> 本文档总结 star 项目中 **两套并存** 的 unifykey 键值服务实现：
>
> | | 旧实现 | 新实现 |
> |---|---|---|
> | 目录 | `vendor/starnet/pdm/` | `vendor/starnet/pdm-hidl/` |
> | IPC | 传统 libbinder（`/dev/binder`） | 绑定式 HIDL（`/dev/hwbinder`） |
> | 注册中心 | `servicemanager` | `hwservicemanager` |
> | 服务名 | `vendor.starnet.pdm` | `vendor.starnet.pdm@1.0::IPdm/default` |
> | 服务进程 | `/vendor/bin/pdm-service` | `/vendor/bin/hw/vendor.starnet.pdm@1.0-service` |
> | SELinux 域 | `pdm_service` | `pdm_hidl_default` |
> | CLI | `pdm.cli` | `pdm.hidl.cli` |
>
> 两套服务**可同时运行**，互不影响（各自开机做一次属性同步，unifykeys 侧有 flock 串行化）。
>
> 文档内容：架构对比、两种实现的**完整源码**、HIDL 中间文件的编译生成过程与产物位置、
> 全部**外部配置**（不在模块目录内但必须存在）、移植 checklist、踩坑记录。
>
> 编写日期：2026-09-12 ｜ 基线：`p291_iptv` / Android 9 (API 28) / armv7-a-neon

---

## 目录

- [第一部分：总览与对比](#第一部分总览与对比)
- [第二部分：旧实现 vendor/starnet/pdm](#第二部分旧实现-vendorstarnetpdm)
- [第三部分：新实现 vendor/starnet/pdm-hidl](#第三部分新实现-vendorstarnetpdm-hidl)
- [第四部分：HIDL 中间文件如何编译生成 & 产物位置](#第四部分hidl-中间文件如何编译生成--产物位置)
- [第五部分：外部依赖与配置清单](#第五部分外部依赖与配置清单)
- [第六部分：移植 Checklist](#第六部分移植-checklist)
- [第七部分：踩坑记录](#第七部分踩坑记录)

---

# 第一部分：总览与对比

## 1.1 业务功能（两套实现完全等价）

对外提供 3 个操作，读写 Amlogic unifykeys 并同步到系统属性：

| 操作 | 语义 |
|---|---|
| `set <name> <value>` | 烧写一个 unifykey + 同步相关系统属性 |
| `get <name>` | 读取一个键（unifykeys → property → 默认值 三级 fallback） |
| `list` | 列出全部已知键 |

内部业务由 6 块组成，两套实现逐段一致：

1. **`kKeyPropMap` 映射表**：key → 系统 property（一个 key 可映射多个 property）
2. **sysfs 读写 helper**：`writeFile` / `readFile` / `trimTail`
3. **fallback 链**：unifykeys → property → 默认值（`getValue` 和 `readAllEntries` 必须用同一条链）
4. **`work_mode` 双通道**：真值来源是标志文件 `/mnt/vendor/param/usermode` 的存在性
5. **`RoWhiteListGate`**：写 `ro.*` 属性前临时打开白名单门 `persist.ro.white_list.enable`
6. **`Lock` flock**：对 `/sys/class/unifykeys/lock` 加 advisory 锁做多客户端串行化

## 1.2 分层对比

```
┌──────────────────────────┬──────────────────────────────────────┐
│        旧 pdm            │           pdm-hidl                   │
├──────────────────────────┼──────────────────────────────────────┤
│ cli/main.cpp             │ cli/main.cpp                         │
│   defaultServiceManager()│   IPdm::getService()                 │
│   interface_cast<IPdm>   │   (内部走 hwservicemanager)          │
├──────────────────────────┤                                      │
│ IPdm.cpp  ← 手写协议     │   ★ 整块删除 ★                       │
│   BpPdm: writeInterface  │   hidl-gen 生成 PdmAll.cpp：         │
│     Token + writeString16│     BpHwPdm / BnHwPdm / BsPdm        │
│   BnPdm::onTransact:     │   收发 Parcel 全自动                  │
│     手工 probe token 偏移│                                      │
├──────────────────────────┼──────────────────────────────────────┤
│ PdmService.cpp           │ PdmHal.cpp                           │
│   status_t + 出参        │   PdmStatus + 出参 + HIDL 薄壳        │
│   （业务本体）           │   （业务本体逐段移植，逻辑相同）       │
├──────────────────────────┼──────────────────────────────────────┤
│ Lock / UnifyKeys         │ Lock / UnifyKeys（仅换命名空间）      │
├──────────────────────────┼──────────────────────────────────────┤
│ main.cpp                 │ main.cpp                             │
│   addService(重试 10 次) │   registerAsService(失败即退出)       │
│   joinThreadPool         │   joinRpcThreadpool                  │
├──────────────────────────┼──────────────────────────────────────┤
│ sepolicy:                │ sepolicy:                            │
│   pdm_service (自有域)   │   pdm_hidl_default (标准 HAL 域)      │
│   service_manager_type   │   hwservice_manager_type              │
│   service_contexts       │   hwservice_contexts                  │
└──────────────────────────┴──────────────────────────────────────┘
```

## 1.3 关键差异速查

| 维度 | 旧 pdm | pdm-hidl | 移植注意 |
|---|---|---|---|
| 收发协议 | 手写 `BnPdm::onTransact` / `BpPdm`，还要手工 probe interface token 偏移 | hidl-gen 生成，无需关心 | 这个模块最大价值就是**删掉这 258 行手写协议** |
| 调用方 | `servicemanager` + `interface_cast<IPdm>` | `IPdm::getService()` | **不能**用 `servicemanager` 查 HIDL 服务 |
| 服务注册名 | `vendor.starnet.pdm` | `vendor.starnet.pdm@1.0::IPdm/default` | instance 名 `default` 由 `registerAsService()` 无参调用隐含 |
| 错误码 | `status_t`（负数 errno 风格） | `PdmStatus` 枚举 | HIDL 的 `Return<T>` 只表达传输层错误 |
| 字符串 | `std::string` / `String16` | `hidl_string` | 转 std::string 必须 `std::string(s.c_str(), s.size())` |
| 依赖库 | `libbinder`, `libutils`, `libcutils`, `liblog` | 加 `libhidlbase`, `libhidltransport`, `libhwbinder` + 接口库 | Android 11+ `libhidltransport` 已并入 `libhidlbase` |
| App 能否调用 | 能（`system_app` 已放行） | **不能**（`app_neverallows.te` 禁止 App 调 HAL） | 移植前必须确认有无 App 侧调用 |
| 接口库产物 | 无（协议在进程内） | `vendor.starnet.pdm@1.0.so`，vendor + system 各一份 | `hidl_interface` 自动带 `vendor_available` |

---

# 第二部分：旧实现 vendor/starnet/pdm

## 2.1 目录清单

```
vendor/starnet/pdm/
├── Android.bp              # cc_binary × 2 (pdm-service / pdm.cli)
├── main.cpp                # 服务进程入口
├── PdmService.h/.cpp       # 业务实现（449 行）
├── IPdm.h                  # 接口声明 + BnPdm / BpPdm 类声明
├── IPdm.cpp                # ★ 手写 binder 协议（258 行）
├── PdmTypes.h              # struct PdmEntry
├── UnifyKeys.h             # 4 个设备节点路径常量
├── Lock.h/.cpp             # flock 串行化
├── pdm-service.rc          # init 拉起
├── cli/main.cpp            # 命令行工具
└── sepolicy/
    ├── pdm_service.te
    ├── file_contexts
    └── service_contexts
```

## 2.2 数据流

```
pdm.cli (system/bin, shell 域)
  │ ProcessState::self()->startThreadPool()
  │ defaultServiceManager()
  │   → sm->getService("vendor.starnet.pdm")
  │   → interface_cast<IPdm>(binder)  →  BpPdm
  │
  │ BpPdm::setValue()
  │   data.writeInterfaceToken("vendor.starnet.pdm.IPdm")
  │   data.writeString16(name); data.writeString16(value)
  │   remote()->transact(TRANSACTION_setValue, data, &reply)
  ▼
/dev/binder  ───────────────────────────────────────────────►  pdm-service (vendor/bin, pdm_service 域)
                                                                  │
                                                    BnPdm::onTransact(code, data, reply, flags)
                                                      ├─ probeTokenLenOffset(data, len=23) 在 0/4/8 找 token
                                                      ├─ readString16Inplace() 读并比对 descriptor
                                                      ├─ setDataPosition(payloadPos)
                                                      └─ switch(code) → PdmService::setValue()
                                                                        │
                                                                        ├─ Lock::create()  flock
                                                                        ├─ writeFile(/sys/class/unifykeys/name)
                                                                        ├─ writeFile(/sys/class/unifykeys/write)
                                                                        ├─ syncKeyToProperty()  (RoWhiteListGate)
                                                                        └─ applyUserModeFile()  (仅 work_mode)
                                                                        │
                                                      reply->writeInt32(err); writeInt32(result)
                                                                  ▼
/dev/binder  ──────────────────────────────────────────────►  BpPdm::setValue 读 reply，printf
```

## 2.3 源码全文

### 2.3.1 `vendor/starnet/pdm/Android.bp`

```python
// Android 9 版本:传统 libbinder,无需 AIDL 生成工具链和 VINTF
// ⚠️ 注意:pdm-service 必须放 vendor 分区(vendor: true),原因:
//    1. 开机时需写 ro.product.cmcc_* 等 vendor 侧属性;
//    2. Android 9 property.te:315-411 neverallow 禁止 coredomain(除白名单外)写非 core property_type 的属性;
//       改为 vendor 分区后,pdm_service domain 变为 non-coredomain,写 vendor_*_prop 合规,不触发 neverallow。
//    3. 对照 device/amlogic/common/sepolicy/system_control.te 也是 vendor 分区 non-coredomain,大量 set_prop(vendor_persist_prop)已过编译。
cc_binary {
    name: "pdm-service",
    vendor: true,
    srcs: [
        "main.cpp",
        "IPdm.cpp",
        "PdmService.cpp",
        "Lock.cpp",
    ],
    shared_libs: [
        "libbinder",
        "libutils",
        "libcutils",   // property_set
        "liblog",
    ],
    init_rc: ["pdm-service.rc"],
}

cc_binary {
    name: "pdm.cli",
    srcs: [
        "cli/main.cpp",
        "IPdm.cpp",
    ],
    shared_libs: [
        "libbinder",
        "libutils",
        "liblog",
    ],
}
```

> 注意 `pdm.cli` **不写 `vendor: true`** → 装到 `/system/bin`；且 `srcs` 里也带了 `IPdm.cpp`（客户端需要 `BpPdm` 的打包代码）。

### 2.3.2 `vendor/starnet/pdm/main.cpp`

```cpp
#include <binder/IPCThreadState.h>
#include <binder/IServiceManager.h>
#include <binder/ProcessState.h>
#include <log/log.h>
#include <unistd.h>

#include "PdmService.h"

using namespace android;

int main() {
    ALOGI("pdm: main() pid=%d uid=%d, service starting...", getpid(), getuid());

    ALOGD("pdm: main() getting defaultServiceManager");
    sp<IServiceManager> sm = defaultServiceManager();
    if (sm == NULL) {
        ALOGE("pdm: main() defaultServiceManager returned NULL, aborting");
        return 1;
    }
    ALOGI("pdm: main() got ServiceManager, creating PdmService instance");

    sp<PdmService> service = new PdmService();  // 构造时同步一次属性
    ALOGI("pdm: main() PdmService instance created");

    // servicemanager 可能还没起来,重试几次(它和本服务同属 core class)
    const char* kServiceName = "vendor.starnet.pdm";
    status_t err = NO_ERROR;
    ALOGI("pdm: main() registering service name='%s' (up to 10 retries)", kServiceName);
    for (int i = 0; i < 10; i++) {
        err = sm->addService(String16(kServiceName), service);
        if (err == NO_ERROR) break;
        ALOGW("pdm: addService(%s) failed(%d), retry %d after 200ms...",
              kServiceName, err, i + 1);
        usleep(200 * 1000);
    }
    if (err != NO_ERROR) {
        ALOGE("pdm: addService(%s) exhausted all retries, final err=%d", kServiceName, err);
        return 1;
    }
    ALOGI("pdm: service '%s' registered successfully, entering thread pool", kServiceName);

    ALOGD("pdm: main() starting binder thread pool");
    ProcessState::self()->startThreadPool();
    ALOGI("pdm: main() joining binder thread pool, serving requests...");
    IPCThreadState::self()->joinThreadPool();
    ALOGE("pdm: main() unexpectedly returned from joinThreadPool()");
    return 0;  // 不应到达
}
```

### 2.3.3 `vendor/starnet/pdm/PdmTypes.h`

```cpp
#pragma once

#include <string>

namespace android {

struct PdmEntry {
    std::string name;
    std::string value;
    PdmEntry() {}
    PdmEntry(const std::string& n, const std::string& v) : name(n), value(v) {}
};

}  // namespace android
```

### 2.3.4 `vendor/starnet/pdm/IPdm.h`

```cpp
#pragma once

#include <binder/IInterface.h>
#include <binder/IBinder.h>

#include <string>
#include <vector>

#include "PdmTypes.h"

namespace android {

// 对应原 AIDL 文件里 IPdm 的 setValue/getValue/getEntries
// (getSchemas 在原服务里是空实现,这里不再保留)
class IPdm : public IInterface {
public:
    DECLARE_META_INTERFACE(Pdm)

    enum {
        TRANSACTION_setValue   = IBinder::FIRST_CALL_TRANSACTION,
        TRANSACTION_getValue   = IBinder::FIRST_CALL_TRANSACTION + 1,
        TRANSACTION_getEntries = IBinder::FIRST_CALL_TRANSACTION + 2,
    };

    virtual status_t setValue(const std::string& name, const std::string& value,
                              bool* result) = 0;
    virtual status_t getValue(const std::string& name, std::string* value) = 0;
    virtual status_t getEntries(std::vector<PdmEntry>* entries) = 0;
};

class BnPdm : public BnInterface<IPdm> {
public:
    status_t onTransact(uint32_t code, const Parcel& data, Parcel* reply,
                        uint32_t flags = 0) override;
};

class BpPdm : public BpInterface<IPdm> {
public:
    explicit BpPdm(const sp<IBinder>& impl) : BpInterface<IPdm>(impl) {}

    status_t setValue(const std::string& name, const std::string& value,
                      bool* result) override;
    status_t getValue(const std::string& name, std::string* value) override;
    status_t getEntries(std::vector<PdmEntry>* entries) override;
};

}  // namespace android
```

### 2.3.5 `vendor/starnet/pdm/IPdm.cpp` ★ 手写协议（HIDL 版本的价值就在这里）

```cpp
#include "IPdm.h"

#include <binder/Parcel.h>
#include <log/log.h>
#include <utils/String8.h>

namespace android {

// 第二个参数是全局唯一的 descriptor,服务名与之解耦
IMPLEMENT_META_INTERFACE(Pdm, "vendor.starnet.pdm.IPdm")

static String16 toString16(const std::string& s) { return String16(s.c_str()); }

static std::string toString(const String16& s16) {
    String8 s8(s16);
    return std::string(s8.string(), s8.size());
}

// ---------- 服务端:解析请求、调用本地实现、回填 reply ----------
// ⚠️ 协议兼容核心: 不用 libbinder 的 Parcel::enforceInterface().
//
// 根因(pdm_test_0906.log L546-L554): libbinder enforceInterface() 内部 *硬编码*:
//   readInt32(strict_mask 4B) → readInt32(work_src 4B) → readString16(token)
// 而我们动态探测 Format A/B 时, Parcel 头里前置 0/4/8B 额外字节的组合在
// Android 9 不同 caller 进程里其实不止 2 种;一旦我们 probe 出来的 tokenOffset 跟
// enforceInterface() 预期对不上, 它就会: ① 读错 token len → 内部报 "Reading NULL string"
// ② 返回值却可能是 NO_ERROR(因为它跳过了一些 0) → ③ 后面 payload(name/value) 位置
// 错位 → 读到空串, 触发 getValue: empty name → NAME_NOT_FOUND (-2).
//
// 解决方案: 手工实现 token 校验 + payload seek 三步:
//   1) probeTokenLenOffset(): 在 0/4/8 三个候选位置找 expectedLen(23), 找到即 token
//      的 String16 头 int32 所在字节偏移.
//   2) 从该 offset 调 readString16Inplace(): 它内部自行 readInt32 读 len 并读内容,
//      ⚠️ 绝不能在其前再手动 readInt32(len), 否则位置落在 token 字符上必返回 NULL;
//      读到的 token compare(server_desc);
//   3) 算 String16 结尾的 4B 对齐偏移, setDataPosition() 到那里 → 之后直接
//      readString16(name/value) 就是业务 payload, 永不错位.
static inline int probeTokenLenOffset(const Parcel& data, size_t expectedLen, size_t size) {
    const int candidates[] = {0, 4, 8};
    for (int off : candidates) {
        if (size < (size_t)(off + 4)) continue;
        status_t save = data.dataPosition();
        const_cast<Parcel&>(data).setDataPosition(off);
        int32_t v = const_cast<Parcel&>(data).readInt32();
        const_cast<Parcel&>(data).setDataPosition(save);
        if ((size_t)v == expectedLen) return off;
    }
    return 0;
}
// String16 4B 对齐用: n*2 char bytes + 4B head → roundUp4(4 + 2*n)
static inline size_t roundUp4(size_t x) { return (x + 3) & ~size_t(3); }

status_t BnPdm::onTransact(uint32_t code, const Parcel& data, Parcel* reply,
                           uint32_t /*flags*/) {
    String16 svr_desc = IPdm::getInterfaceDescriptor();
    String8 svr_desc8(svr_desc);
    const size_t expectedLen = svr_desc.size();
    const size_t size = data.dataSize();
    // 1. 找 token String16 的 len int32 所在字节偏移 (tokenLenOffset)
    int tokenLenOffset = probeTokenLenOffset(data, expectedLen, size);
    ALOGD("pdm: BnPdm::onTransact(code=%u) enter, detected tokenLenOffset=%d, "
          "size=%ld cap=%ld, svr_desc.len=%zu",
          code, tokenLenOffset, (long)size, (long)data.dataCapacity(), expectedLen);
    ALOGD("pdm: BnPdm::onTransact server-side descriptor='%s' (len=%zu)",
          svr_desc8.string(), svr_desc8.size());

    // 2. 从 tokenLenOffset 开始, 读 + 比对 descriptor, 算出 payload 起始
    status_t tokenErr = NO_ERROR;
    size_t payloadPos = 0;
    {
        status_t save = data.dataPosition();
        Parcel& d = const_cast<Parcel&>(data);
        // ⚠️ readString16Inplace() 内部会先 readInt32() 读 String16 的 len
        // (frameworks/native/libs/binder/Parcel.cpp readString16Inplace), 这里
        // 不能再先手动 readInt32(len) —— 否则位置会落到 token 字符内容上, 二次读
        // 出的"长度"是字符数据 → 必返回 NULL → BAD_VALUE(-22), 即 0907 日志
        // "BnPdm::onTransact readString16Inplace returned NULL"、pdm.cli 全挂的根因。
        d.setDataPosition(tokenLenOffset);
        size_t actual_len = 0;
        const char16_t* ptr = d.readString16Inplace(&actual_len);
        String16 client_token;
        if (ptr == NULL) {
            ALOGE("pdm: BnPdm::onTransact(code=%u) readString16Inplace returned NULL", code);
            tokenErr = BAD_VALUE;
        } else if (actual_len != expectedLen) {
            String8 client8;
            client8 = String8(String16(ptr, actual_len));
            ALOGE("pdm: BnPdm::onTransact(code=%u) token actual_len=%zu != expected=%zu, client='%s'",
                  code, actual_len, expectedLen, client8.string());
            tokenErr = BAD_VALUE;
        } else {
            client_token = String16(ptr, actual_len);
            if (client_token != svr_desc) {
                String8 client8(client_token);
                ALOGE("pdm: BnPdm::onTransact(code=%u) token mismatch: client='%s' expected='%s'",
                      code, client8.string(), svr_desc8.string());
                tokenErr = BAD_TYPE;
            } else {
                payloadPos = (size_t)d.dataPosition();
                ALOGD("pdm: BnPdm::onTransact(code=%u) token OK, payloadPos=%zu",
                      code, payloadPos);
            }
        }
        d.setDataPosition(save);
    }

    if (tokenErr != NO_ERROR) {
        if (reply != NULL) reply->writeInt32(tokenErr);
        return tokenErr;
    }

    switch (code) {
        case IPdm::TRANSACTION_setValue: {
            ALOGD("pdm: BnPdm::onTransact -> TRANSACTION_setValue");
            Parcel& d = const_cast<Parcel&>(data);
            d.setDataPosition(payloadPos);
            std::string name = toString(d.readString16());
            std::string value = toString(d.readString16());
            ALOGD("pdm: BnPdm::onTransact(setValue) name='%s' value_len=%zu",
                  name.c_str(), value.size());
            bool result = false;
            status_t err = setValue(name, value, &result);
            ALOGD("pdm: BnPdm::onTransact(setValue) service returned err=%d result=%d",
                  err, result ? 1 : 0);
            if (reply != NULL) {
                reply->writeInt32(err);
                if (err == NO_ERROR) reply->writeInt32(result ? 1 : 0);
            }
            return NO_ERROR;
        }
        case IPdm::TRANSACTION_getValue: {
            ALOGD("pdm: BnPdm::onTransact -> TRANSACTION_getValue");
            Parcel& d = const_cast<Parcel&>(data);
            d.setDataPosition(payloadPos);
            std::string name = toString(d.readString16());
            ALOGD("pdm: BnPdm::onTransact(getValue) name='%s'", name.c_str());
            std::string value;
            status_t err = getValue(name, &value);
            ALOGD("pdm: BnPdm::onTransact(getValue) service returned err=%d value_len=%zu",
                  err, value.size());
            if (reply != NULL) {
                reply->writeInt32(err);
                if (err == NO_ERROR) reply->writeString16(toString16(value));
            }
            return NO_ERROR;
        }
        case IPdm::TRANSACTION_getEntries: {
            ALOGD("pdm: BnPdm::onTransact -> TRANSACTION_getEntries");
            (void)payloadPos; // getEntries 无入参,不需要读
            std::vector<PdmEntry> entries;
            status_t err = getEntries(&entries);
            ALOGD("pdm: BnPdm::onTransact(getEntries) service returned err=%d entries=%zu",
                  err, entries.size());
            if (reply != NULL) {
                reply->writeInt32(err);
                if (err == NO_ERROR) {
                    reply->writeInt32(entries.size());
                    for (const auto& e : entries) {
                        reply->writeString16(toString16(e.name));
                        reply->writeString16(toString16(e.value));
                    }
                }
            }
            return NO_ERROR;
        }
        default:
            // BBinder 自己处理 FIRST_CALL_TRANSACTION / PING 等,要从 pos 0 开始读 token,
            // 先 rollback 到 0.
            const_cast<Parcel&>(data).setDataPosition(0);
            ALOGD("pdm: BnPdm::onTransact(code=%u) -> fallthrough to BBinder (rollback pos 0)", code);
            return BBinder::onTransact(code, data, reply, 0);
    }
}

// ---------- 客户端:打包请求、解析 reply ----------
status_t BpPdm::setValue(const std::string& name, const std::string& value, bool* result) {
    ALOGD("pdm: BpPdm::setValue(name='%s', value='%s') enter", name.c_str(), value.c_str());
    // 调试: 打印客户端写入的 descriptor(与服务端对照)
    String16 cli_desc = IPdm::getInterfaceDescriptor();
    String8 cli_desc8(cli_desc);
    ALOGD("pdm: BpPdm::setValue client-side writeInterfaceToken descriptor='%s' (len=%zu)",
          cli_desc8.string(), cli_desc8.size());
    Parcel data, reply;
    data.writeInterfaceToken(IPdm::getInterfaceDescriptor());
    data.writeString16(toString16(name));
    data.writeString16(toString16(value));
    // ⚠️ 协议修复: transact 返回的是 binder 层传输错误,不是业务错误!
    //   transact=0 只代表"包送到了并收到回复",业务错误在 reply.readInt32() 里
    status_t transact_err = remote()->transact(IPdm::TRANSACTION_setValue, data, &reply);
    if (transact_err != NO_ERROR) {
        ALOGE("pdm: BpPdm::setValue transact failed: %d", transact_err);
        return transact_err;
    }
    status_t err = reply.readInt32();
    if (err == NO_ERROR && result != NULL) {
        *result = reply.readInt32() != 0;
        ALOGD("pdm: BpPdm::setValue done, err=%d result=%d", err, *result ? 1 : 0);
    } else {
        ALOGD("pdm: BpPdm::setValue done, err=%d", err);
    }
    return err;
}

status_t BpPdm::getValue(const std::string& name, std::string* value) {
    ALOGD("pdm: BpPdm::getValue(name='%s') enter", name.c_str());
    String16 cli_desc = IPdm::getInterfaceDescriptor();
    String8 cli_desc8(cli_desc);
    ALOGD("pdm: BpPdm::getValue client-side writeInterfaceToken descriptor='%s' (len=%zu)",
          cli_desc8.string(), cli_desc8.size());
    Parcel data, reply;
    data.writeInterfaceToken(IPdm::getInterfaceDescriptor());
    data.writeString16(toString16(name));
    status_t transact_err = remote()->transact(IPdm::TRANSACTION_getValue, data, &reply);
    if (transact_err != NO_ERROR) {
        ALOGE("pdm: BpPdm::getValue transact failed: %d", transact_err);
        return transact_err;
    }
    status_t err = reply.readInt32();
    if (err == NO_ERROR && value != NULL) {
        *value = toString(reply.readString16());
        ALOGD("pdm: BpPdm::getValue done, err=%d value_len=%zu", err, value->size());
    } else {
        ALOGD("pdm: BpPdm::getValue done, err=%d", err);
    }
    return err;
}

status_t BpPdm::getEntries(std::vector<PdmEntry>* entries) {
    ALOGD("pdm: BpPdm::getEntries() enter");
    String16 cli_desc = IPdm::getInterfaceDescriptor();
    String8 cli_desc8(cli_desc);
    ALOGD("pdm: BpPdm::getEntries client-side writeInterfaceToken descriptor='%s' (len=%zu)",
          cli_desc8.string(), cli_desc8.size());
    Parcel data, reply;
    data.writeInterfaceToken(IPdm::getInterfaceDescriptor());
    status_t transact_err = remote()->transact(IPdm::TRANSACTION_getEntries, data, &reply);
    if (transact_err != NO_ERROR) {
        ALOGE("pdm: BpPdm::getEntries transact failed: %d", transact_err);
        return transact_err;
    }
    status_t err = reply.readInt32();
    if (err == NO_ERROR && entries != NULL) {
        entries->clear();
        int32_t n = reply.readInt32();
        entries->reserve(n);
        ALOGD("pdm: BpPdm::getEntries reading %d entries", n);
        for (int32_t i = 0; i < n; i++) {
            entries->push_back(
                    PdmEntry(toString(reply.readString16()), toString(reply.readString16())));
        }
        ALOGD("pdm: BpPdm::getEntries done, err=%d entries=%zu", err, entries->size());
    } else {
        ALOGD("pdm: BpPdm::getEntries done, err=%d", err);
    }
    return err;
}

}  // namespace android
```

### 2.3.6 `vendor/starnet/pdm/PdmService.h`

```cpp
#pragma once

#include "IPdm.h"

namespace android {

class PdmService : public BnPdm {
public:
    PdmService();

    status_t setValue(const std::string& name, const std::string& value, bool* result) override;
    status_t getValue(const std::string& name, std::string* value) override;
    status_t getEntries(std::vector<PdmEntry>* entries) override;

private:
    // 读取全部已知 key(name->value 逐项填充)
    bool readAllEntries(std::vector<PdmEntry>* entries);
    // key -> 系统属性(对应原 UNIFY_KEY_MAP)
    void syncKeyToProperty(const std::string& name, const std::string& value);
};

}  // namespace android
```

### 2.3.7 `vendor/starnet/pdm/PdmService.cpp`（业务本体，449 行）

```cpp
#include "PdmService.h"

#include <cutils/properties.h>
#include <errno.h>
#include <fcntl.h>
#include <log/log.h>
#include <string.h>
#include <unistd.h>

#include "Lock.h"
#include "UnifyKeys.h"

namespace android {

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

// 全部写完后关闭 fd 的 sysfs 读写(原 android::base::WriteStringToFile/ReadFileToString)
static bool writeFile(const char* path, const std::string& data) {
    ALOGD("pdm: writeFile(path=%s, data_len=%zu)", path, data.size());
    int fd = open(path, O_WRONLY);
    if (fd < 0) {
        ALOGE("pdm: writeFile open %s failed: %s", path, strerror(errno));
        return false;
    }
    size_t off = 0;
    while (off < data.size()) {
        ssize_t n = write(fd, data.data() + off, data.size() - off);
        if (n <= 0) {
            ALOGE("pdm: writeFile write %s failed at off=%zu: %s", path, off, strerror(errno));
            close(fd);
            return false;
        }
        off += n;
    }
    close(fd);
    ALOGD("pdm: writeFile(%s) success, wrote %zu bytes", path, off);
    return true;
}

static bool readFile(const char* path, std::string* out) {
    ALOGD("pdm: readFile(path=%s)", path);
    int fd = open(path, O_RDONLY);
    if (fd < 0) {
        ALOGE("pdm: readFile open %s failed: %s", path, strerror(errno));
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
        ALOGE("pdm: readFile read %s failed: %s (last n=%zd)", path, strerror(errno), n);
        return false;
    }
    ALOGD("pdm: readFile(%s) success, read %zu bytes", path, out->size());
    return true;
}

// 属性值不宜带 sysfs 尾部换行
static std::string trimTail(const std::string& s) {
    size_t end = s.size();
    while (end > 0 && (s[end - 1] == '\n' || s[end - 1] == '\r')) end--;
    if (end != s.size()) {
        ALOGD("pdm: trimTail: trimmed %zu trailing whitespace chars", s.size() - end);
    }
    return s.substr(0, end);
}

static constexpr const char* kUserModeFile = "/mnt/vendor/param/usermode";
static constexpr const char* kModeKey = "work_mode";
static constexpr const char* kModeUser = "1";   // 用户模式
static constexpr const char* kModeFactory = "0"; // 工厂模式

// 是否用户模式(标志文件存在性)
static bool isUserMode() {
    if (access(kUserModeFile, F_OK) == 0) return true;
    if (errno != ENOENT) {
        ALOGW("pdm: isUserMode: access(%s) failed: %s", kUserModeFile, strerror(errno));
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
            ALOGE("pdm: applyUserModeFile: open %s failed: %s", kUserModeFile, strerror(errno));
            return false;
        }
        const char* data = "1\n";
        ssize_t n = write(fd, data, strlen(data));
        int saved_errno = errno;
        close(fd);
        if (n != (ssize_t)strlen(data)) {
            ALOGE("pdm: applyUserModeFile: write %s failed (n=%zd): %s",
                  kUserModeFile, n, strerror(saved_errno));
            return false;
        }
        ALOGI("pdm: applyUserModeFile: user work_mode ON, created %s", kUserModeFile);
        return true;
    }
    // 工厂模式: 删除标志文件(本就不存在视为已达成)
    if (unlink(kUserModeFile) != 0 && errno != ENOENT) {
        ALOGE("pdm: applyUserModeFile: unlink %s failed: %s", kUserModeFile, strerror(errno));
        return false;
    }
    ALOGI("pdm: applyUserModeFile: user work_mode OFF, removed %s", kUserModeFile);
    return true;
}

PdmService::PdmService() {
    ALOGI("pdm: PdmService() constructor start, begin boot-time sync from unifykeys");
    std::vector<PdmEntry> entries;
    if (!readAllEntries(&entries)) {
        ALOGE("pdm: PdmService() init sync failed, entries will be left untouched");
        return;
    }
    ALOGI("pdm: PdmService() read %zu entries, now syncing to properties", entries.size());
    for (const auto& e : entries) {
        syncKeyToProperty(e.name, e.value);
    }
    ALOGI("pdm: PdmService() constructor done, boot sync complete");
}

bool PdmService::readAllEntries(std::vector<PdmEntry>* entries) {
    static const char* kKeys[] = {"mac",   "mac_wifi", "sn",      "stbid",       "cmccpwd",
                                  "cmei",  "device_id", "work_mode",   "cmcc_date",   "product_token",
                                  "andlink_token",
                                  "manufacturer", "brand", "model", "device",
                                  "cmcc_brand", "cmcc_ims_app_id", "cmcc_ims_secret"};
    const int kKeyCount = sizeof(kKeys) / sizeof(kKeys[0]);
    ALOGD("pdm: readAllEntries() start, total keys=%d", kKeyCount);
    // 整个循环持一把锁,与多客户端并发互斥
    std::unique_ptr<Lock> lock = Lock::create();
    if (!lock) {
        ALOGE("pdm: readAllEntries: failed to lock %s", kUnifyKeyLock);
        return false;
    }
    ALOGD("pdm: readAllEntries: acquired lock %s", kUnifyKeyLock);
    entries->clear();
    int fallbackProp = 0, fallbackDef = 0;
    for (const char* key : kKeys) {
        bool ok = true;
        if (!writeFile(kUnifyKeyName, key)) {
            ALOGW("pdm: readAllEntries: write name '%s' to %s failed, skip unifykey", key, kUnifyKeyName);
            ok = false;
        }
        std::string value;
        if (ok && !readFile(kUnifyKeyRead, &value)) {
            ALOGW("pdm: readAllEntries: read value of '%s' from %s failed, skip unifykey", key, kUnifyKeyRead);
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
                    ALOGD("pdm: readAllEntries: key='%s' fallback prop '%s'='%s'",
                          key, item->prop, trimmed.c_str());
                } else if (item->def != NULL && item->def[0] != '\0') {
                    trimmed = item->def;
                    fallbackDef++;
                    ALOGD("pdm: readAllEntries: key='%s' fallback default='%s'", key, trimmed.c_str());
                }
            }
        }
        // work_mode 特殊: 与 getValue 保持同一来源(标志文件存在性),避免 list 显示
        // unifykey/property 的旧值而 get work_mode 显示文件真值的"两处不一致"
        if (strcmp(key, kModeKey) == 0) {
            trimmed = isUserMode() ? kModeUser : kModeFactory;
            ALOGD("pdm: readAllEntries: key='work_mode' from user work_mode flag file, value='%s'",
                  trimmed.c_str());
        }
        entries->push_back(PdmEntry(key, trimmed));
    }
    lock.reset();
    ALOGI("pdm: readAllEntries() done, total %zu entries (hitUnify=%zu hitProp=%d hitDef=%d)",
          entries->size(), entries->size() - fallbackProp - fallbackDef, fallbackProp, fallbackDef);
    return true;
}

// init property service 的 IsWhitelistedRoProperty()(system/core/init/
// property_service.cpp)只在该属性 == "1" 时放行 whitelist_prop.txt 白名单内的
// ro.* 属性运行期改写。syncKeyToProperty 期间临时打开这个门,对象析构时(无论
// 成功失败、含提前 return)关回 "0",保证白名单 ro.* 平时只能经 pdm 通道
// (pdm.cli -> pdm-service -> 本函数)修改,串口/adb 等其他调用方 setprop 一律
// 被 init 拒收。
// 该属性已在 vendor/starnet/sepolicy/property_contexts 映射 vendor_persist_prop
// (pdm_service.te 已 allow set);property_set 经 property service socket 由 init
// 串行处理,先置 1 后写 ro.* 的顺序有保证。
class RoWhiteListGate {
public:
    RoWhiteListGate() {
        int rc = property_set(kRoWhiteListEnableProp, "1");
        if (rc != 0) {
            ALOGW("pdm: RoWhiteListGate: enable %s failed rc=%d", kRoWhiteListEnableProp, rc);
        } else {
            ALOGD("pdm: RoWhiteListGate: %s enabled", kRoWhiteListEnableProp);
        }
    }
    ~RoWhiteListGate() {
        int rc = property_set(kRoWhiteListEnableProp, "0");
        if (rc != 0) {
            ALOGW("pdm: RoWhiteListGate: disable %s failed rc=%d", kRoWhiteListEnableProp, rc);
        } else {
            ALOGD("pdm: RoWhiteListGate: %s disabled", kRoWhiteListEnableProp);
        }
    }

private:
    static constexpr const char* kRoWhiteListEnableProp = "persist.ro.white_list.enable";
};

void PdmService::syncKeyToProperty(const std::string& name, const std::string& value) {
    ALOGD("pdm: syncKeyToProperty(name='%s', value='%s') start", name.c_str(), value.c_str());
    // 打开白名单改写门,离开本函数(含失败路径)由 RoWhiteListGate 析构自动关闭
    RoWhiteListGate whiteListGate;
    int matched = 0;
    for (const auto& item : kKeyPropMap) {
        if (name != item.key) continue;
        matched++;
        const char* v = value.empty() ? item.def : value.c_str();
        ALOGD("pdm: syncKeyToProperty: mapping key='%s' -> prop='%s' value='%s'",
              name.c_str(), item.prop, v);
        if (property_set(item.prop, v) != 0) {
            ALOGW("pdm: syncKeyToProperty: setprop %s=%s failed (ro.* 已被 init 冻结时属正常)",
                  item.prop, v);
        } else {
            ALOGI("pdm: syncKeyToProperty: setprop %s=%s OK", item.prop, v);
        }
    }
    if (matched == 0) {
        ALOGW("pdm: syncKeyToProperty: key='%s' has no property mapping, skipped", name.c_str());
    } else {
        ALOGD("pdm: syncKeyToProperty(name='%s') done, matched %d property entries", name.c_str(), matched);
    }
}

status_t PdmService::setValue(const std::string& name, const std::string& value,
                              bool* result) {
    ALOGI("pdm: setValue(name='%s', value='%s') enter", name.c_str(), value.c_str());
    *result = false;
    // 1. 先做参数合法性校验(避免上一轮 name='' 的假 Success):
    //    name 必须非空 且 存在于 kKeyPropMap; 否则直接返回 NAME_NOT_FOUND,
    //    不会再 writeFile 空字符串到 sysfs, 也不会给上层误报 result=true.
    if (name.empty()) {
        ALOGE("pdm: setValue: empty name -> NAME_NOT_FOUND");
        return NAME_NOT_FOUND;
    }
    if (findMapItem(name) == NULL) {
        ALOGE("pdm: setValue: unknown name='%s' not in kKeyPropMap -> NAME_NOT_FOUND", name.c_str());
        return NAME_NOT_FOUND;
    }
    // work_mode 是模式开关而非普通键值: 取值必须是 "0"(工厂)/"1"(用户)。
    if (name == kModeKey && !isValidModeValue(value)) {
        ALOGE("pdm: setValue: work_mode value '%s' invalid, expect '0'(factory)/'1'(user) -> BAD_VALUE",
              value.c_str());
        return BAD_VALUE;
    }
    std::unique_ptr<Lock> lock = Lock::create();
    if (!lock) {
        ALOGE("pdm: setValue: lock failed, return PERMISSION_DENIED");
        return PERMISSION_DENIED;
    }
    ALOGD("pdm: setValue: acquired lock");
    // unifykeys 协议:先写 key 名,再写值
    if (!writeFile(kUnifyKeyName, name)) {
        ALOGE("pdm: setValue: write name '%s' to %s failed: %s",
              name.c_str(), kUnifyKeyName, strerror(errno));
        return UNKNOWN_ERROR;
    }
    if (!writeFile(kUnifyKeyWrite, value)) {
        ALOGE("pdm: setValue: write value '%s' to %s failed: %s",
              value.c_str(), kUnifyKeyWrite, strerror(errno));
        return UNKNOWN_ERROR;
    }
    ALOGD("pdm: setValue: unifykey write done, releasing lock");
    lock.reset();  // 解锁后再同步属性,缩短持锁时间

    syncKeyToProperty(name, trimTail(value));

    if (name == kModeKey) {
        if (!applyUserModeFile(trimTail(value))) {
            ALOGE("pdm: setValue: apply user work_mode flag for work_mode='%s' failed -> UNKNOWN_ERROR",
                  value.c_str());
            return UNKNOWN_ERROR;
        }
    }

    *result = true;
    ALOGI("pdm: setValue(name='%s') success, result=true", name.c_str());
    return NO_ERROR;
}

status_t PdmService::getValue(const std::string& name, std::string* value) {
    ALOGI("pdm: getValue(name='%s') enter", name.c_str());
    *value = "";
    if (name.empty()) {
        ALOGE("pdm: getValue: empty name -> NAME_NOT_FOUND");
        return NAME_NOT_FOUND;
    }
    const auto* item = findMapItem(name);
    if (item == NULL) {
        ALOGE("pdm: getValue: unknown name='%s' not in kKeyPropMap -> NAME_NOT_FOUND", name.c_str());
        return NAME_NOT_FOUND;
    }
    // work_mode 特殊: 模式真值在标志文件里(系统其余部分也只看文件),直接据此返回,
    if (name == kModeKey) {
        *value = isUserMode() ? kModeUser : kModeFactory;
        ALOGI("pdm: getValue(name='work_mode') from user work_mode flag file '%s', value='%s'",
              kUserModeFile, value->c_str());
        return NO_ERROR;
    }

    std::unique_ptr<Lock> lock = Lock::create();
    if (!lock) {
        ALOGE("pdm: getValue: lock failed, return PERMISSION_DENIED");
        return PERMISSION_DENIED;
    }
    ALOGD("pdm: getValue: acquired lock");
    // 1. 先查 unifykeys 驱动
    bool unifyOk = true;
    if (!writeFile(kUnifyKeyName, name)) {
        ALOGW("pdm: getValue: write name '%s' to %s failed, skip unifykeys", name.c_str(), kUnifyKeyName);
        unifyOk = false;
    }
    std::string v;
    if (unifyOk && !readFile(kUnifyKeyRead, &v)) {
        ALOGW("pdm: getValue: read '%s' from %s failed, skip unifykeys", name.c_str(), kUnifyKeyRead);
        unifyOk = false;
    }
    v = trimTail(v);
    lock.reset();

    // 2. Fallback 链: unifykeys 空 → 读系统 property → 用 kKeyPropMap 默认值.
    //    因为板上首次开机或 unifykeys 未烧录时, readFile 会空, 但 getprop ro.product.model 已存在(0905实测=TB712)
    if (unifyOk && !v.empty()) {
        ALOGD("pdm: getValue(name='%s')  hit unifykeys len=%zu", name.c_str(), v.size());
    } else {
        // 读 key 映射的第一个 property(ro.product.model 等), 若有值直接用
        if (getPropertyString(item->prop, &v) && !v.empty()) {
            ALOGD("pdm: getValue(name='%s') fallback prop '%s'='%s'", name.c_str(), item->prop, v.c_str());
        } else if (item->def != NULL && item->def[0] != '\0') {
            v = item->def;
            ALOGD("pdm: getValue(name='%s') fallback default='%s'", name.c_str(), v.c_str());
        } else {
            v.clear();
            ALOGD("pdm: getValue(name='%s') no default, return empty", name.c_str());
        }
    }
    *value = v;
    ALOGI("pdm: getValue(name='%s') done, value_len=%zu value='%s'",
          name.c_str(), value->size(), value->c_str());
    return NO_ERROR;
}

status_t PdmService::getEntries(std::vector<PdmEntry>* entries) {
    ALOGI("pdm: getEntries() enter");
    bool ok = readAllEntries(entries);
    if (ok) {
        ALOGI("pdm: getEntries() success, %zu entries returned", entries->size());
    } else {
        ALOGE("pdm: getEntries() failed");
    }
    return ok ? NO_ERROR : UNKNOWN_ERROR;
}

}  // namespace android
```

### 2.3.8 `vendor/starnet/pdm/UnifyKeys.h`

```cpp
#pragma once

// Amlogic unifykeys(kernel: /sys/class/unifykeys)节点 —— 需确认目标 A9 内核已带该驱动
constexpr const char* kUnifyKeyName   = "/sys/class/unifykeys/name";
constexpr const char* kUnifyKeyWrite  = "/sys/class/unifykeys/write";
constexpr const char* kUnifyKeyRead   = "/sys/class/unifykeys/read";
constexpr const char* kUnifyKeyLock   = "/sys/class/unifykeys/lock";
```

### 2.3.9 `vendor/starnet/pdm/Lock.h` / `Lock.cpp`

```cpp
// Lock.h
// Copyright (C) 2026-2026 福建星网锐捷通讯股份有限公司. All rights reserved.
#pragma once

#include <memory>

// 与 PdmService/IPdm 同命名空间(本模块均为传统 libbinder,android 命名空间)
namespace android {

// 用户态互斥:对 /sys/class/unifykeys/lock 节点 fd 做 flock,
// 仅用于多客户端并发串行化,不会真正写 lock 节点(不锁存储)
class Lock {
  public:
    static std::unique_ptr<Lock> create();
    ~Lock();

  private:
    Lock();

    int fd = -1;
    bool locked = false;
};

}  // namespace android
```

```cpp
// Lock.cpp
// Copyright (C) 2026-2026 福建星网锐捷通讯股份有限公司. All rights reserved.

#include "Lock.h"

#include <errno.h>
#include <fcntl.h>
#include <log/log.h>
#include <string.h>
#include <unistd.h>

#include <memory>

#include "UnifyKeys.h"

namespace android {

std::unique_ptr<Lock> Lock::create() {
    ALOGD("pdm: Lock::create() enter, lock_path=%s", kUnifyKeyLock);
    auto lock = std::unique_ptr<Lock>(new Lock());

    lock->fd = open(kUnifyKeyLock, O_WRONLY);
    if (lock->fd < 0) {
        ALOGE("pdm: Lock::create open %s failed: %s", kUnifyKeyLock, strerror(errno));
        return nullptr;
    }
    ALOGD("pdm: Lock::create open %s success, fd=%d", kUnifyKeyLock, lock->fd);

    struct flock flock;
    flock.l_whence = SEEK_SET;
    flock.l_start = 0;
    flock.l_len = 0;
    flock.l_type = F_WRLCK;
    flock.l_pid = -1;

    ALOGD("pdm: Lock::create acquiring F_WRLCK (blocking) on fd=%d", lock->fd);
    if (fcntl(lock->fd, F_SETLKW, &flock) < 0) {
        ALOGE("pdm: Lock::create fcntl(F_SETLKW) failed on %s: %s",
              kUnifyKeyLock, strerror(errno));
        close(lock->fd);
        lock->fd = -1;
        return nullptr;
    }

    lock->locked = true;
    ALOGD("pdm: Lock::create() success, lock acquired on %s (fd=%d)", kUnifyKeyLock, lock->fd);
    return lock;
}

Lock::Lock() {
    ALOGD("pdm: Lock() constructor, fd=%d locked=%d", fd, locked ? 1 : 0);
}

Lock::~Lock() {
    ALOGD("pdm: ~Lock() destructor enter, fd=%d locked=%d", fd, locked ? 1 : 0);
    if (fd < 0) {
        ALOGD("pdm: ~Lock() fd invalid, nothing to do");
        return;
    }

    if (locked) {
        struct flock flock;
        flock.l_whence = SEEK_SET;
        flock.l_start = 0;
        flock.l_len = 0;
        flock.l_type = F_UNLCK;
        flock.l_pid = -1;

        ALOGD("pdm: ~Lock() releasing F_UNLCK on fd=%d", fd);
        int ret = fcntl(fd, F_SETLK, &flock);
        if (ret < 0) {
            ALOGE("pdm: ~Lock() fcntl(F_SETLK, F_UNLCK) failed: %s", strerror(errno));
        }
        locked = false;
    }

    close(fd);
    ALOGD("pdm: ~Lock() closed fd=%d, done", fd);
    fd = -1;
}

}  // namespace android
```

### 2.3.10 `vendor/starnet/pdm/cli/main.cpp`

```cpp
#include <binder/IServiceManager.h>
#include <binder/ProcessState.h>
#include <log/log.h>
#include <stdio.h>
#include <string.h>
#include <unistd.h>

#include <string>
#include <vector>

#include "../IPdm.h"

using namespace android;

static void printUsage(const char* prog) {
    printf("Usage:\n"
           "  %s set <name> <value>   - 设置键值对(烧写 unifykey)\n"
           "  %s get <name>           - 获取指定键的值\n"
           "  %s list                 - 获取所有条目\n",
           prog, prog, prog);
}

// 去掉尾部换行后打印,避免 sysfs 值自带 '\n' 时输出多空行
static std::string stripTail(const std::string& s) {
    size_t end = s.size();
    while (end > 0 && (s[end - 1] == '\n' || s[end - 1] == '\r')) end--;
    if (end != s.size()) {
        ALOGD("pdm-cli: stripTail trimmed %zu chars", s.size() - end);
    }
    return s.substr(0, end);
}

int main(int argc, char* argv[]) {
    ALOGI("pdm-cli: main() argc=%d, pid=%d uid=%d", argc, getpid(), getuid());
    for (int i = 0; i < argc; i++) {
        ALOGD("pdm-cli: argv[%d]='%s'", i, argv[i]);
    }

    if (argc < 2) {
        ALOGW("pdm-cli: no command provided");
        printUsage(argv[0]);
        return 1;
    }

    ALOGD("pdm-cli: starting binder thread pool");
    // 客户端也需要初始化 binder 上下文
    ProcessState::self()->startThreadPool();

    ALOGD("pdm-cli: getting ServiceManager");
    sp<IServiceManager> sm = defaultServiceManager();
    if (sm == NULL) {
        fprintf(stderr, "Error: failed to get ServiceManager\n");
        ALOGE("pdm-cli: defaultServiceManager() returned NULL");
        return 1;
    }

    ALOGI("pdm-cli: looking up service 'vendor.starnet.pdm'");
    sp<IBinder> binder = sm->getService(String16("vendor.starnet.pdm"));  // 内部会短暂重试
    if (binder == NULL) {
        fprintf(stderr, "Error: service 'vendor.starnet.pdm' not found"
                        " (pdm-service 未运行或 SELinux 拦截?)\n");
        ALOGE("pdm-cli: service 'vendor.starnet.pdm' not found");
        return 1;
    }
    ALOGI("pdm-cli: found service binder=%p, casting to IPdm", binder.get());
    sp<IPdm> pdm = interface_cast<IPdm>(binder);
    if (pdm == NULL) {
        ALOGE("pdm-cli: interface_cast<IPdm> failed");
        fprintf(stderr, "Error: interface cast failed\n");
        return 1;
    }
    ALOGD("pdm-cli: interface cast OK, pdm=%p", pdm.get());

    std::string command = argv[1];
    ALOGI("pdm-cli: executing command='%s'", command.c_str());

    if (command == "set" && argc == 4) {
        ALOGI("pdm-cli: setValue name='%s' value='%s'", argv[2], argv[3]);
        bool ok = false;
        status_t st = pdm->setValue(argv[2], argv[3], &ok);
        ALOGI("pdm-cli: setValue returned st=%d ok=%d", st, ok ? 1 : 0);
        if (st != NO_ERROR || !ok) {
            fprintf(stderr, "Error: setValue(%s) failed, st=%d\n", argv[2], st);
            ALOGE("pdm-cli: setValue failed st=%d ok=%d", st, ok ? 1 : 0);
            return 1;
        }
        printf("Success: set %s=%s\n", argv[2], argv[3]);
        ALOGI("pdm-cli: set %s=%s SUCCESS", argv[2], argv[3]);
    } else if (command == "get" && argc == 3) {
        ALOGI("pdm-cli: getValue name='%s'", argv[2]);
        std::string value;
        status_t st = pdm->getValue(argv[2], &value);
        ALOGI("pdm-cli: getValue returned st=%d value_len=%zu", st, value.size());
        if (st != NO_ERROR) {
            fprintf(stderr, "Error: getValue(%s) failed, st=%d\n", argv[2], st);
            ALOGE("pdm-cli: getValue failed st=%d", st);
            return 1;
        }
        std::string trimmed = stripTail(value);
        printf("%s=%s\n", argv[2], trimmed.c_str());
        ALOGI("pdm-cli: get %s='%s'", argv[2], trimmed.c_str());
    } else if (command == "list" && argc == 2) {
        ALOGI("pdm-cli: getEntries()");
        std::vector<PdmEntry> entries;
        status_t st = pdm->getEntries(&entries);
        ALOGI("pdm-cli: getEntries returned st=%d count=%zu", st, entries.size());
        if (st != NO_ERROR) {
            fprintf(stderr, "Error: getEntries failed, st=%d\n", st);
            ALOGE("pdm-cli: getEntries failed st=%d", st);
            return 1;
        }
        for (const auto& e : entries) {
            std::string trimmed = stripTail(e.value);
            printf("%s=%s\n", e.name.c_str(), trimmed.c_str());
            ALOGD("pdm-cli:   %s='%s'", e.name.c_str(), trimmed.c_str());
        }
        ALOGI("pdm-cli: listed %zu entries", entries.size());
    } else {
        ALOGW("pdm-cli: unknown/invalid command='%s' argc=%d", command.c_str(), argc);
        printUsage(argv[0]);
        return 1;
    }
    ALOGI("pdm-cli: main() done, exit 0");
    return 0;
}
```

## 2.4 init rc

`vendor/starnet/pdm/pdm-service.rc`：

```
# 开机启动 pdm 服务(root,才能写 /sys/class/unifykeys)
# ⚠️ 执行路径改为 /vendor/bin/pdm-service(配合 Android.bp vendor:true)
#    原因: pdm-service 需写 vendor_* 属性; 为 non-coredomain(参考 system_control.te)避开 neverallow
service pdm-service /vendor/bin/pdm-service
    class core
    user root
    group root
    seclabel u:r:pdm_service:s0
```

> 与 HIDL 版本的关键差别：这里**可以**写 `seclabel`（`pdm_service` 不是 halserverdomain），
> 且 `class core`。

## 2.5 SELinux 策略

### `vendor/starnet/pdm/sepolicy/pdm_service.te`

```selinux

type pdm_service, domain, binder_in_vendor_violators;
type pdm_service_exec, exec_type, vendor_file_type, file_type;
init_daemon_domain(pdm_service)


allow pdm_service vendor_file:file execute_no_trans;


type pdm_svc, service_manager_type;


allow pdm_service pdm_svc:service_manager { add find };


allow { shell system_app } pdm_svc:service_manager find;
allow { shell system_app } pdm_service:binder { call transfer };
allow pdm_service servicemanager:binder { call transfer };


allow pdm_service sysfs_unifykey:dir search;
allow pdm_service sysfs_unifykey:file { open read write getattr };

# 用户模式标志文件 /mnt/vendor/param/usermode(存在=用户模式, 不存在=工厂模式):
# set/get mode 需在该目录创建/删除/读写该文件;
# 目录与文件均标 param_tv_file(device/amlogic/common/sepolicy/file_contexts)
allow pdm_service param_tv_file:dir { search read write open add_name remove_name getattr };
allow pdm_service param_tv_file:file { create open read write setattr getattr unlink };


allow pdm_service vendor_persist_prop:property_service set;
allow pdm_service vendor_platform_prop:property_service set;
allow pdm_service vendor_app_prop:property_service set;
```

### `vendor/starnet/pdm/sepolicy/file_contexts`

```
/vendor/bin/pdm-service    u:object_r:pdm_service_exec:s0
```

### `vendor/starnet/pdm/sepolicy/service_contexts`

```
vendor.starnet.pdm    u:object_r:pdm_svc:s0
```

## 2.6 外部配置（旧实现相关）

`vendor/starnet/starnet.mk` 中：

```make
#for pdm (unifykey 键值管理服务及命令行工具)
PRODUCT_PACKAGES += \
	pdm-service \
	pdm.cli
```

旧的 sepolicy 目录注册也写在同一个文件里：

```make
BOARD_SEPOLICY_DIRS += \
    vendor/starnet/sepolicy \
    vendor/starnet/pdm/sepolicy
```

### 关于「写在 starnet.mk 里的 BOARD_SEPOLICY_DIRS 到底生不生效」

这是一个**容易踩坑、且文档与实际不一致**的点，这里给出实测结论（2026-09-12 校验当前构建产物）：

| 检查项 | 实测结果 | 结论 |
|---|---|---|
| `vendor_sepolicy.cil` 里有 `(type pdm_service)` | ✅ 有 | `.te` **生效** |
| `vendor_sepolicy.cil` 里有 `pdm_svc` | ✅ 有 | `.te` 里的类型/allow **生效** |
| `vendor_file_contexts` 里有 `/vendor/bin/pdm-service → pdm_service_exec` | ✅ 有 | `file_contexts` **生效** |
| `service_contexts` 的 `vendor.starnet.pdm → pdm_svc` | ❌ 未出现在任何已安装的上下文文件里（本产品输出目录下**根本没有** `vendor_service_contexts`） | `service_contexts` **未生效** |

即：**写在 `starnet.mk`（product-config 阶段）的 `BOARD_SEPOLICY_DIRS` 是生效的**——
`.te` 和 `file_contexts` 都进了最终策略。`pdm-hidl/README.md` 第 3 节里"该目录不生效"
的说法与当前构建产物不符，应以本表的实测结果为准。

**移植建议**（保持现状，不要"统一"）：

- 新模块（pdm-hidl）的 sepolicy 目录**继续放在 board-config 阶段**
  （`device/amlogic/common/sepolicy.mk`）——这是已验证可用的位置；
- **不要在两处都写**：同一个 sepolicy 目录被重复 append 会导致策略编译报重复定义
  （见 `vendor/starnet/pdm-hidl/pdm-hidl.mk` 里的注释）；
- 旧的 `service_contexts` 未被安装的原因属于本仓库遗留问题（Android 9 的 vendor
  `service_contexts` 归属需要单独确认），移植时若发现旧服务查不到标签，
  改用 HIDL 版本即可绕开整个 `service_manager` 标签体系。

> 对照：新实现用的是 `hwservice_contexts`，实测**正常生效**
> （`vendor/etc/selinux/vendor_hwservice_contexts` 里有 `vendor.starnet.pdm::IPdm`）。
> 这是 HIDL 相对旧 binder 的一个隐性好处：标签体系更简单、链路更短。

---

# 第三部分：新实现 vendor/starnet/pdm-hidl

## 3.1 目录清单

```
vendor/starnet/pdm-hidl/
├── README.md                                    # 使用/验证说明
├── pdm-hidl.mk                                  # PRODUCT_PACKAGES 声明
├── interfaces/
│   ├── Android.bp                               # hidl_package_root ← 包名→目录映射
│   └── pdm/1.0/
│       ├── types.hal                            # struct Entry / enum PdmStatus
│       ├── IPdm.hal                             # interface IPdm
│       └── Android.bp                           # hidl_interface
├── service/
│   ├── Android.bp                               # cc_binary (vendor, /vendor/bin/hw)
│   ├── main.cpp                                 # 36 行骨架
│   ├── PdmHal.h / PdmHal.cpp                    # 业务实现（497 行）
│   ├── UnifyKeys.h                              # 4 个设备节点路径常量
│   ├── Lock.h / Lock.cpp                        # flock 串行化
│   └── vendor.starnet.pdm@1.0-service.rc
├── cli/
│   ├── Android.bp                               # cc_binary (system, /system/bin)
│   └── main.cpp                                 # 命令行工具
├── sepolicy/
│   ├── pdm_hidl.te
│   ├── file_contexts
│   └── hwservice_contexts
└── manifest/
    └── vendor.starnet.pdm@1.0.xml               # VINTF 片段（可选）
```

## 3.2 数据流

```
pdm.hidl.cli (system/bin, shell 域)
  │ IPdm::getService()
  │   → hwservicemanager getTransport("vendor.starnet.pdm@1.0::IPdm","default")
  │   → sm->get(...) → cast → BpHwPdm
  │
  │ BpHwPdm::setValue(name, value, cb)
  │   （hidl-gen 生成的 Parcel 打包，使用方无需关心）
  ▼
/dev/hwbinder ──────────────────────────────────────►  vendor.starnet.pdm@1.0-service
                                                        (vendor/bin/hw, pdm_hidl_default 域)
                                                        │
                                              BnHwPdm::onTransact（生成）
                                                        │
                                              PdmHal::setValue(hidl_string, hidl_string, _hidl_cb)
                                                        ├─ setValueInternal()  ← 业务本体
                                                        │    ├─ 参数校验（空名/未知 key/BAD_VALUE）
                                                        │    ├─ Lock::create()  flock
                                                        │    ├─ writeFile(.../unifykeys/name)
                                                        │    ├─ writeFile(.../unifykeys/write)
                                                        │    ├─ lock.reset()
                                                        │    ├─ syncKeyToProperty() (RoWhiteListGate)
                                                        │    └─ applyUserModeFile() (仅 work_mode)
                                                        └─ _hidl_cb(status, result)
                                                        ▼
/dev/hwbinder ──────────────────────────────────────►  CLI 回调 lambda 收值，printf
```

**与旧实现的分界**：`BnPdm::onTransact` / `BpPdm` 那 258 行手写协议 → 被 hild-gen 生成的
`BsPdm.h` / `BpHwPdm.h` / `PdmAll.cpp` 完全替代。业务代码 `PdmHal.cpp` 与 `PdmService.cpp`
逐段对应。

## 3.3 接口层

### 3.3.1 `interfaces/pdm/1.0/types.hal`

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

### 3.3.2 `interfaces/pdm/1.0/IPdm.hal`

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

### 3.3.3 接口设计的 4 条硬约束（移植到新项目同样适用）

| # | 约束 | 原因 |
|---|---|---|
| 1 | **业务错误码必须自定义 enum** | `Return<T>` 只表达传输层错误；不定义则客户端无法区分"键不存在"和"服务挂了" |
| 2 | **struct 不能写在 interface 里** | HIDL 语法禁止；必须放 `types.hal`，且每个类型都要在 `Android.bp` 的 `types:` 列出，否则 `types.cpp` 不生成序列化、链接报错 |
| 3 | **多返回值用 `generates (...)`** | C++ 侧变成 `Return<void>` + `_hidl_cb` 回调 |
| 4 | **`work_mode` 不是普通键** | 有独立的真值来源（标志文件），接口注释必须写明 |

## 3.4 构建声明

### 3.4.1 `interfaces/Android.bp` —— 包根映射

```python
// HIDL 包根声明:包名 vendor.starnet.pdm@1.0 的 HAL 文件位于 <path>/pdm/1.0/,
// 即 vendor/starnet/pdm-hidl/interfaces/pdm/1.0/。
// 等价于 hidl-gen 的 "-r vendor.starnet:vendor/starnet/pdm-hidl/interfaces"。
// 参考树内同类写法:vendor/amlogic/common/interfaces/Android.bp。
hidl_package_root {
    name: "vendor.starnet",
    path: "vendor/starnet/pdm-hidl/interfaces",
}
```

> **这是整个 HIDL 能编起来的前提**。没有它，hidl-gen 不知道 `vendor.starnet.pdm@1.0`
> 去磁盘上哪个目录找 `.hal`。等价于命令行 `-rvendor.starnet:vendor/starnet/pdm-hidl/interfaces`。

### 3.4.2 `interfaces/pdm/1.0/Android.bp` —— 接口库

```python
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

字段含义（移植时逐项对照）：

| 字段 | 作用 / 移植注意 |
|---|---|
| `name` | 产出的 `.so` 名，也是别人 `shared_libs` 里写的名字 |
| `root` | 决定生成的头文件路径 → `#include <vendor/starnet/pdm/1.0/IPdm.h>` |
| `vendor: true` | 接口库放 vendor 侧；**自动带 `vendor_available`**，一份声明产出 vendor + system 两个变体 |
| `srcs` | 两个 `.hal` 都要写 |
| `interfaces` | `android.hidl.base@1.0` —— 每个 HIDL 接口都隐含继承 `IBase`，必写 |
| `types` | **必须与 types.hal 同步维护** |
| `gen_java` | Treble 的 `app_neverallows.te` 禁止普通 App 调 HAL，Java 侧生成了也用不了 |

## 3.5 服务端

### 3.5.1 `service/main.cpp` —— 36 行固定骨架

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

四个动作，顺序不能变：

1. `configureRpcThreadpool(4, true)` —— 起 4 个 hwbinder 线程，`true` = 稍后会 join
2. `new PdmHal()` —— **构造时做一次开机属性同步**（等价旧 `PdmService` 构造函数）
3. `registerAsService()` —— **不传参 → instance 名就是 `default`**
4. `joinRpcThreadpool()` —— 阻塞；返回即出错

> ⚠️ **第 3 步的容错方向**：`hwservicemanager` 的 `ServiceManager::add()` 在 `canAdd` 被拒时
> 返回 `bool false`（不是 error），生成的 `PdmAll.cpp` 把它转成 `UNKNOWN_ERROR`。
> 所以**必须检查返回值 + `return 1`**，否则会出现"进程活着但服务没注册"的幽灵状态。

### 3.5.2 `service/PdmHal.h`

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

**「薄壳模式」是移植的核心手法**：

```
setValueInternal(name, value, bool* result) -> PdmStatus   ← 业务逻辑，从旧 PdmService 逐段搬
getValueInternal(name, string* value)       -> PdmStatus
readAllEntries(vector<Entry>*)              -> bool
syncKeyToProperty(name, value)              -> void
────────────────────── 以上与 HIDL 无关，可原样复用 ──────────────────────
setValue(hidl_string, hidl_string, cb) -> Return<void>     ← 薄壳：转类型 → 调 internal → _hidl_cb
getValue(hidl_string, cb)              -> Return<void>
getEntries(cb)                         -> Return<void>
```

这样业务代码保持"返回值 + 出参"的 C++ 风格，**改动量从"重写"降到"改类型"**：

| 项 | 旧 pdm | 新 pdm-hidl |
|---|---|---|
| 返回类型 | `status_t`（`NO_ERROR`/`NAME_NOT_FOUND`/…） | `PdmStatus` 枚举 |
| 字符串 | `std::string` ↔ `String16` | `std::string` ↔ `hidl_string` |
| 日志 | `ALOG*("pdm: ...")` | `ALOG*("pdm-hidl: ...")` |

> ⚠️ `hidl_string` → `std::string` 必须写 `std::string(s.c_str(), s.size())`，
> **不能只用 `s.c_str()`**——hidl_string 不保证 NUL 结尾。反向可直接赋值（有隐式构造）。

### 3.5.3 `service/PdmHal.cpp`（业务本体，497 行）

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

### 3.5.4 业务逻辑的 6 块（移植时必须逐块保留）

| # | 模块 | 位置 | 移植注意 |
|---|---|---|---|
| 1 | **`kKeyPropMap` 映射表** | `PdmHal.cpp:35-77` | **必须按新产品 DTS/白名单重写**；一个 key 可映射多个 property（同名 key 多行实现）；`struct` 必须显式命名 |
| 2 | **sysfs 读写 helper** | `PdmHal.cpp:99-150` | 没用 `android::base::WriteStringToFile`（vendor 侧不一定链得上），手写 `writeFile`/`readFile`/`trimTail` |
| 3 | **fallback 链** | `getValueInternal` | `unifykeys → property → 默认值`；**`readAllEntries`（list）必须用同一条链**，否则"`list` 全空但 `get model` 有值" |
| 4 | **`work_mode` 双通道** | `getValueInternal` / `setValueInternal` | 真值是**标志文件存在性**；`get` 直接看文件不读 unifykeys；`set` 额外调 `applyUserModeFile`（`"1"` 写文件 / `"0"` 删文件）；取值只接受 `"0"`/`"1"` |
| 5 | **`RoWhiteListGate`** | `PdmHal.cpp:212-231` | RAII：构造置 `"1"`、析构置 `"0"`，保证**任何 return 路径都关回去**。这是安全边界，不能省 |
| 6 | **flock 粒度** | `Lock` | `setValue` 先 `lock.reset()` 再同步属性（缩短持锁）；`readAllEntries` **整个循环持一把锁**。粒度不同是刻意的 |

### 3.5.5 `service/UnifyKeys.h`

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

### 3.5.6 `service/Lock.h` / `Lock.cpp`

```cpp
// Lock.h
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

```cpp
// Lock.cpp
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

### 3.5.7 `service/Android.bp`

```python
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

**5 个要点**：

| 项 | 说明 |
|---|---|
| `defaults: ["hidl_defaults"]` | 已带 `-Wall -Werror`，所以自己只加 `-DLOG_TAG` |
| `vendor: true` | **必须**。理由同旧实现：要写 vendor 侧 property，且避开 coredomain 的 neverallow |
| `relative_install_path: "hw"` | 装到 `/vendor/bin/hw/`，与 vendor 各 HAL 服务一致 |
| `init_rc: [...]` | Soong 自动把 `.rc` 装到 `/vendor/etc/init/` |
| `libcutils` | **给 `property_get`/`property_set` 用的，别漏** |

## 3.6 客户端 CLI

### 3.6.1 `cli/Android.bp`

```python
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

> 与 `service/Android.bp` 的差别：**不写 `vendor: true`** → 装到 `/system/bin`；
> 少 `libcutils`（CLI 不直接读写 property）；少 `init_rc`。

### 3.6.2 `cli/main.cpp`

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

**两个移植要点**：

1. **取服务方式**：`IPdm::getService()`（从 hwservicemanager）。
   **不能用 `servicemanager` + `interface_cast`**——那是旧实现的路径，两套服务在两个不同的注册中心。
2. **两层错误检查，缺一不可**：
   ```cpp
   auto ret = pdm->setValue(...);
   if (!ret.isOk())         { /* 第一层：传输层，IPC 失败/服务挂了 */ }
   if (status != PdmStatus::OK) { /* 第二层：业务层，键不存在等 */ }
   ```

输出格式（`name=value`、`Success: set x=y`）与旧 `pdm.cli` **逐字一致**，保证上层脚本不用改。

## 3.7 init rc（`service/vendor.starnet.pdm@1.0-service.rc`）

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

三条硬约束：

| 约束 | 原因 |
|---|---|
| 路径必须带 `/hw/` | 对应 `Android.bp` 的 `relative_install_path: "hw"` |
| `class hal`（不是旧实现的 `class core`） | 与 vendor 各 HAL 服务一致 |
| **不能写 `seclabel`** | `hal_neverallows.te` 有 `neverallow * halserverdomain:process dyntransition`；HAL 域转换必须靠 `file_contexts` + `init_daemon_domain` 在 exec 时完成 |

`user root` 是必须的（写 `/sys/class/unifykeys`）。

## 3.8 SELinux 策略

### 3.8.1 `sepolicy/pdm_hidl.te`

```selinux
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

**与旧 `pdm_service.te` 的三处刻意差异（照抄旧文件会编译不过）**：

| # | 差异 | 原因 |
|---|---|---|
| 1 | 用 `hal_attribute`/`hal_server_domain`/`add_hwservice` 宏 | HAL 域标准写法；`hal_server_domain` 会给域打上 `halserverdomain` 属性 |
| 2 | **删掉 `allow <域> vendor_file:file execute_no_trans`** | `hal_neverallows.te` 明确 neverallow；本服务也不 exec 任何东西 |
| 3 | **`.rc` 里不写 `seclabel`** | 同上，`neverallow * halserverdomain:process dyntransition` |

> ⚠️ 本树 `add_hwservice` 宏（`system/sepolicy/public/te_macros:593`）**只加 allow、不声明类型**：
> ```
> define(`add_hwservice', `
>   allow $1 $2:hwservice_manager { add find };
>   allow $1 hidl_base_hwservice:hwservice_manager add;
>   neverallow { domain -$1 } $2:hwservice_manager add;
> ')
> ```
> 所以 `type pdm_hidl_hwservice, hwservice_manager_type;` **必须自己写**，否则类型未定义。
> 移植到别的 AOSP 版本时要先确认该宏的定义，有的版本会自己声明类型（那样重复声明会报错）。

### 3.8.2 `sepolicy/file_contexts`

```
/vendor/bin/hw/vendor.starnet.pdm@1.0-service    u:object_r:pdm_hidl_default_exec:s0
```

这一行和 `init_daemon_domain` 是配对的，是**域转换的唯一途径**。

### 3.8.3 `sepolicy/hwservice_contexts`

```
# hwservicemanager 按 <package>::<interface> 查标签(带版本号的部分会被去掉,
# 见 system/hwservicemanager/AccessControl.cpp canAdd/canGet)
vendor.starnet.pdm::IPdm    u:object_r:pdm_hidl_hwservice:s0
```

> ⚠️ **key 不带 `@1.0`**。`AccessControl.cpp:46` 用 `fqIface.package() + "::" + fqIface.name()`
> 去查表，写带版本号的形式会导致 `No match for interface ... in hwservice_contexts`
> 而静默拒绝（既拒绝 `add` 也拒绝 `find`）。

## 3.9 VINTF 清单（可选）

`manifest/vendor.starnet.pdm@1.0.xml`：

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

**为什么本产品不并入也能用**：`ServiceManagement.cpp:689-704` 里，只有定义了
`ENFORCE_VINTF_MANIFEST` 才走 manifest 检查分支；未定义时 `vintfLegacy = (transport == EMPTY)`，
直接走注册表查询。

**移植到开了 `PRODUCT_ENFORCE_VINTF_MANIFEST` 的项目时**，manifest 片段**必须**并入，
否则 `getService()` 会失败。

## 3.10 构建集成（4 处外部改动，全部必须）

| # | 文件 | 改动内容 | 为什么必须在这 |
|---|---|---|---|
| 1 | `vendor/starnet/starnet.mk` | `include vendor/starnet/pdm-hidl/pdm-hidl.mk` | product-config 阶段；`PRODUCT_PACKAGES` 在这声明才被镜像打包看见 |
| 2 | `vendor/starnet/pdm-hidl/pdm-hidl.mk` | `PRODUCT_PACKAGES += vendor.starnet.pdm@1.0-service pdm.hidl.cli` | 接口 `.so` **不用列**，是 `shared_libs` 依赖，Soong 自动带两侧变体 |
| 3 | `device/amlogic/common/sepolicy.mk` | `BOARD_SEPOLICY_DIRS += vendor/starnet/pdm-hidl/sepolicy` | **board-config 阶段**（已验证可用） |
| 4 | `device/amlogic/p291_iptv/BoardConfig.mk:372` | `DEVICE_MANIFEST_FILE += vendor/starnet/pdm-hidl/manifest/vendor.starnet.pdm@1.0.xml` | 必须放在 `AUTO_PATCH_AB`/`AUTO_PATCH_NPU` 两个 `$(shell ...)` **之后** |

### 3.10.1 `vendor/starnet/pdm-hidl/pdm-hidl.mk`

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

### 3.10.2 改动 1：`vendor/starnet/starnet.mk`

```make
#for pdm (unifykey 键值管理服务及命令行工具)
PRODUCT_PACKAGES += \
	pdm-service \
	pdm.cli

#for pdm-hidl (绑定式 HIDL 版 unifykey 服务, 与上面的 pdm 并存;
# 见 vendor/starnet/pdm-hidl/README.md)
include vendor/starnet/pdm-hidl/pdm-hidl.mk
```

### 3.10.3 改动 3：`device/amlogic/common/sepolicy.mk`

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
ifneq ($(TARGET_BUILD_GOOGLE_ATV), true)
BOARD_SEPOLICY_DIRS += \
    device/google/atv/sepolicy
endif
```

> 注：上面注释里的"starnet.mk 里追加不生效"与本文档 2.6 节的实测结果**不一致**——
> 当前构建产物显示两处都生效。为稳妥起见**保持现状**（新目录放 board-config），
> 但不要因为这条注释去改动已工作的旧实现。详见 [2.6 节](#26-外部配置旧实现相关)。

### 3.10.4 改动 4：`device/amlogic/p291_iptv/BoardConfig.mk`

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

> ⚠️ **不要改回 `pdm-hidl/Android.mk` 自动包含的写法**。同时**本目录下不要放 `Android.bp`**
> （Soong 会忽略同目录的 `Android.mk`），所有 Soong 模块都放 `interfaces/`、`service/`、
> `cli/` 子目录。

# 第四部分：HIDL 中间文件如何编译生成 & 产物位置

这一部分专门回答三个问题：**hidl-gen 什么时候被调用、生成的文件放在哪、最终装到哪里**。

## 4.1 构建链路总览

```
Android.bp (hidl_interface)
      │  Soong 解析
      ▼
out/soong/build.ninja          ← 生成规则（genrule）
      │  ninja 执行
      ▼
sbox 沙箱 (out/soong/.temp)
      │  --sandbox-path out/soong/.temp --output-root <gen 目录>
      ▼
out/soong/host/linux-x86/bin/hidl-gen   ← 宿主机 x86_64 工具
      │  -Lc++-sources / -Lc++-headers
      ▼
out/soong/.intermediates/vendor/starnet/pdm-hidl/interfaces/pdm/1.0/
   ├── vendor.starnet.pdm@1.0_genc++/gen/...          ← .cpp
   └── vendor.starnet.pdm@1.0_genc++_headers/gen/...  ← .h
      │  clang++ 编译（每个变体各编一份 .o）
      ▼
.../vendor.starnet.pdm@1.0/<变体>/{obj/…/*.o, *.a, *.so}
      │  Soong install
      ▼
out/target/product/p291_iptv/{vendor/lib, system/lib, vendor/bin/hw, system/bin}/...
```

## 4.2 hidl-gen 工具本体

| 项 | 路径 |
|---|---|
| 可执行文件 | `out/soong/host/linux-x86/bin/hidl-gen`（约 3.7 MB，宿主机 x86_64 版） |
| 源码 | `system/tools/hidl/` |
| 工具自身的编译中间件 | `out/soong/.intermediates/system/tools/hidl/`（含 `libhidl-gen`、`libhidl-gen-ast`、`libhidl-gen-hash`、`utils/libhidl-gen-utils`、`hidl-gen`） |

## 4.3 ninja 里的两条生成规则

在 `out/soong/build.ninja` 里搜 `vendor.starnet.pdm@1.0_genc++` 可以找到。

### 4.3.1 C++ 实现（`-Lc++-sources`）—— `build.ninja:1586730`

```ninja
rule m.vendor.starnet.pdm_1.0_genc___.generator
    command = ${g.genrule.sboxCmd} --sandbox-path out/soong/.temp \
      --output-root out/soong/.intermediates/vendor/starnet/pdm-hidl/interfaces/pdm/1.0/vendor.starnet.pdm@1.0_genc++/gen \
      -c 'out/soong/host/linux-x86/bin/hidl-gen -d __SBOX_DEPFILE__ -o __SBOX_OUT_DIR__ \
          -Lc++-sources \
          -randroid.hidl:system/libhidl/transport \
          -rvendor.starnet:vendor/starnet/pdm-hidl/interfaces \
          vendor.starnet.pdm@1.0' ${depfileArgs} ${allouts}
    deps = gcc

build out/soong/.intermediates/.../vendor.starnet.pdm@1.0_genc++/gen/vendor/starnet/pdm/1.0/PdmAll.cpp \
    | out/soong/.intermediates/.../vendor.starnet.pdm@1.0_genc++/gen/vendor/starnet/pdm/1.0/types.cpp \
    : m.vendor.starnet.pdm_1.0_genc___.generator \
      vendor/starnet/pdm-hidl/interfaces/pdm/1.0/types.hal \
      vendor/starnet/pdm-hidl/interfaces/pdm/1.0/IPdm.hal \
    | ${g.genrule.sboxCmd} out/soong/host/linux-x86/bin/hidl-gen
    depfile = out/soong/.intermediates/.../vendor.starnet.pdm@1.0_genc++/gen/vendor/starnet/pdm/1.0/PdmAll.cpp.d
    allouts = __SBOX_OUT_DIR__/vendor/starnet/pdm/1.0/PdmAll.cpp \
              __SBOX_OUT_DIR__/vendor/starnet/pdm/1.0/types.cpp
```

### 4.3.2 头文件（`-Lc++-headers`）—— 紧随其后

同一目录结构，只把 `-Lc++-sources` 换成 `-Lc++-headers`，
`--output-root` 改成 `..._genc++_headers/gen`。

### 4.3.3 命令行参数逐项解释（手动重现时照抄）

| 参数 | 含义 |
|---|---|
| `-o __SBOX_OUT_DIR__` | 输出根目录（sandbox 替换为真实的 `--output-root`） |
| `-Lc++-sources` / `-Lc++-headers` | 生成语言后端 |
| `-d __SBOX_DEPFILE__` | 生成 depfile（`.hal` 依赖，用于增量） |
| `-randroid.hidl:system/libhidl/transport` | import 路径映射：`android.hidl.base@1.0` 在哪个目录 |
| `-rvendor.starnet:vendor/starnet/pdm-hidl/interfaces` | **包根映射**，对应 `interfaces/Android.bp` 的 `hidl_package_root` |
| `vendor.starnet.pdm@1.0` | 要生成的接口 FQName |

> `-rvendor.starnet:...` 这条**如果包根写错了，hidl-gen 会报 "Could not find package"**。
> 这就是 `interfaces/Android.bp` 里 `hidl_package_root` 必须存在的原因。

## 4.4 生成的文件清单（真实路径）

接口包根目录：`out/soong/.intermediates/vendor/starnet/pdm-hidl/interfaces/pdm/1.0/`

```
interfaces/pdm/1.0/
├── vendor.starnet.pdm@1.0_genc++/gen/vendor/starnet/pdm/1.0/
│   ├── PdmAll.cpp          37156 B   ← 收发桩：BpHwPdm 实现 + BnHwPdm::onTransact
│   │                                 + IPdm::getService/registerAsService + HIDL_FETCH_IPdm
│   ├── types.cpp            2516 B   ← Entry / PdmStatus 的序列化
│   └── PdmAll.cpp.d          245 B   ← depfile，列出依赖的 .hal
│
├── vendor.starnet.pdm@1.0_genc++_headers/gen/vendor/starnet/pdm/1.0/
│   ├── IPdm.h              19531 B   ← ★ 服务端要实现的接口（纯虚函数签名在这）
│   ├── BsPdm.h              6426 B   ← 服务端基类（passthrough 用）+ HIDL_FETCH_IPdm 宏
│   ├── BpHwPdm.h            3273 B   ← 客户端代理（CLI 用）
│   ├── BnHwPdm.h            2353 B   ← 服务端 binder 节点
│   ├── IHwPdm.h              593 B   ← hw 版接口（passthrough 的中间层）
│   ├── types.h              7458 B   ← Entry / PdmStatus 的 C++ 形态
│   ├── hwtypes.h             880 B
│   └── IPdm.h.d              243 B
│
├── vendor.starnet.pdm@1.0/
│   ├── android_arm_armv7-a-neon_cortex-a9_core_shared/
│   │   ├── vendor.starnet.pdm@1.0.so        ← 给 /system 侧用
│   │   ├── vendor.starnet.pdm@1.0.so.toc
│   │   └── vendor.starnet.pdm@1.0.so.toc.d
│   ├── android_arm_armv7-a-neon_cortex-a9_vendor_shared/
│   │   ├── vendor.starnet.pdm@1.0.so        ← 给 /vendor 侧用（72 KB）
│   │   └── ...
│   ├── android_arm_armv7-a-neon_cortex-a9_core_static/
│   │   └── obj/.intermediates/vendor/starnet/pdm-hidl/interfaces/pdm/1.0/
│   │       └── vendor.starnet.pdm@1.0_genc++/gen/vendor/starnet/pdm/1.0/
│   │           ├── PdmAll.o / PdmAll.o.d
│   │           └── types.o  / types.o.d
│   └── android_arm_armv7-a-neon_cortex-a9_vendor_static/
│       └── （同上，另一份 .o）
│
├── vendor.starnet.pdm@1.0_genc++/            （只有 gen/，见上）
└── vendor.starnet.pdm@1.0_genc++_headers/    （只有 gen/，见上）
```

### 关键理解：为什么有 4 个变体目录

`hidl_interface` 的 `vendor: true` 让 Soong **自动带 `vendor_available`**，于是接口库被编出 4 份：

| 变体 | 给谁用 | 说明 |
|---|---|---|
| `core_shared` | system 侧进程 | CLI 链的就是它 → 落 `/system/lib/` |
| `vendor_shared` | vendor 侧进程 | 服务端链的就是它 → 落 `/vendor/lib/` |
| `core_static` | system 静态链接场景 | 本模块没用到，但 Soong 仍会编 |
| `vendor_static` | vendor 静态链接场景 | 同上 |

每个变体各自编译一份 `.o`（**4 份 `PdmAll.o` + 4 份 `types.o`**），
放在 `.../<变体>/obj/.intermediates/<源码相对路径>/` 下——这是 Soong 的惯例：
`obj/` 目录按源码相对路径镜像。

## 4.5 服务端 / CLI 的编译中间件

```
out/soong/.intermediates/vendor/starnet/pdm-hidl/

├── service/vendor.starnet.pdm@1.0-service/
│   └── android_arm_armv7-a-neon_cortex-a9_vendor/
│       ├── vendor.starnet.pdm@1.0-service        ← 链接产物
│       └── obj/vendor/starnet/pdm-hidl/service/
│           ├── main.o     main.o.d
│           ├── PdmHal.o   PdmHal.o.d
│           └── Lock.o     Lock.o.d
│
└── cli/pdm.hidl.cli/
    └── android_arm_armv7-a-neon_cortex-a9_core/
        ├── pdm.hidl.cli                          ← 链接产物
        └── obj/vendor/starnet/pdm-hidl/cli/main.o
```

## 4.6 编译时的 include 路径（`build.ninja:1586130`）

编译 `PdmAll.cpp` 时，Soong 显式把生成目录加进 `-I`：

```
cFlags = -Ivendor/starnet/pdm-hidl/interfaces/pdm/1.0
         ...
         -Iout/soong/.intermediates/system/libhidl/transport/manager/1.0/android.hidl.manager@1.0_genc++_headers/gen
         -Iout/soong/.intermediates/system/libhidl/transport/manager/1.1/android.hidl.manager@1.1_genc++_headers/gen
         -Iout/soong/.intermediates/system/libhidl/transport/base/1.0/android.hidl.base@1.0_genc++_headers/gen
         ...
         -Iout/soong/.intermediates/vendor/starnet/pdm-hidl/interfaces/pdm/1.0/vendor.starnet.pdm@1.0_genc++/gen
         -Iout/soong/.intermediates/vendor/starnet/pdm-hidl/interfaces/pdm/1.0/vendor.starnet.pdm@1.0_genc++_headers/gen
         ...
```

> 注意：你自己的代码写 `#include <vendor/starnet/pdm/1.0/IPdm.h>`，实际解析到的是
> `..._genc++_headers/gen/vendor/starnet/pdm/1.0/IPdm.h`。**不要在自己目录里再放一份同名头文件。**

## 4.7 最终落地产物（`out/target/product/p291_iptv/`）

| 文件 | 大小 | 来源模块 |
|---|---|---|
| `vendor/lib/vendor.starnet.pdm@1.0.so` | 71272 B | `vendor.starnet.pdm@1.0`（vendor_shared） |
| `system/lib/vendor.starnet.pdm@1.0.so` | 80396 B | `vendor.starnet.pdm@1.0`（core_shared） |
| `vendor/bin/hw/vendor.starnet.pdm@1.0-service` | 28100 B | `vendor.starnet.pdm@1.0-service` |
| `vendor/etc/init/vendor.starnet.pdm@1.0-service.rc` | — | `init_rc` 自动安装 |
| `system/bin/pdm.hidl.cli` | — | `pdm.hidl.cli` |
| `vendor/etc/selinux/vendor_sepolicy.cil` | — | 48 处 `pdm_hidl` |
| `vendor/etc/selinux/vendor_hwservice_contexts` | — | `vendor.starnet.pdm::IPdm` 一行 |
| `vendor/etc/selinux/vendor_file_contexts` | — | `/vendor/bin/hw/vendor.starnet.pdm@1.0-service` 一行 |
| `symbols/system/bin/pdm.hidl.cli` | — | 符号（debug） |

### 4.7.1 另有一套 Make/kati 风格的镜像目录

这些**不是 hidl-gen 的直接产出**，而是 kati 阶段的 install 中间件，排查时容易混淆：

```
out/target/product/p291_iptv/obj/
├── SHARED_LIBRARIES/
│   ├── vendor.starnet.pdm@1.0_intermediates/          ← system 侧 .so + 调试信息
│   │   ├── vendor.starnet.pdm@1.0.so
│   │   ├── vendor.starnet.pdm@1.0.so.debug
│   │   ├── vendor.starnet.pdm@1.0.so.dynsyms / .funcsyms / .keep_symbols
│   │   ├── vendor.starnet.pdm@1.0.so.mini_debuginfo.xz
│   │   ├── export_includes / link_type
│   │   └── {LINKED,PACKED}/
│   └── vendor.starnet.pdm@1.0.vendor_intermediates/   ← vendor 侧同名一套
├── STATIC_LIBRARIES/vendor.starnet.pdm@1.0_intermediates/
├── EXECUTABLES/
│   ├── vendor.starnet.pdm@1.0-service_intermediates/{LINKED,PACKED}/
│   ├── pdm.hidl.cli_intermediates/{LINKED,PACKED}/
│   ├── pdm-service_intermediates/                     ← 旧实现
│   └── pdm.cli_intermediates/                         ← 旧实现
└── PACKAGING/target_files_intermediates/p291_iptv-target_files-eng.root/…
```

## 4.8 其它有用的构建索引文件

| 文件 | 用途 |
|---|---|
| `out/soong/build.ninja` | 全部 Soong 生成的 ninja 规则（419 MB，grep 时用 `-m` 限量） |
| `out/.module_paths/Android.bp.list` | 全树 Android.bp 文件索引 |
| `out/.module_paths/Android.mk.list` | 全树 Android.mk 文件索引 |
| `out/soong/host/linux-x86/bin/hidl-gen` | hidl-gen 本体 |
| `out/target/product/p291_iptv/vendor/etc/selinux/` | 所有已安装的 SELinux 上下文文件 |

## 4.9 手动重现 / 自检

### 4.9.1 不重新编整个树，单独看生成代码

```bash
out/soong/host/linux-x86/bin/hidl-gen -o /tmp/pdmgen -Lc++-sources \
  -randroid.hidl:system/libhidl/transport \
  -rvendor.starnet:vendor/starnet/pdm-hidl/interfaces \
  vendor.starnet.pdm@1.0

# 头文件用另一个后端
out/soong/host/linux-x86/bin/hidl-gen -o /tmp/pdmhdr -Lc++-headers \
  -randroid.hidl:system/libhidl/transport \
  -rvendor.starnet:vendor/starnet/pdm-hidl/interfaces \
  vendor.starnet.pdm@1.0
```

### 4.9.2 只清本模块的中间件，强制重新生成

```bash
rm -rf out/soong/.intermediates/vendor/starnet/pdm-hidl
m vendor.starnet.pdm@1.0-service pdm.hidl.cli
```

### 4.9.3 编译顺序建议（不要一上来就整包）

```bash
m vendor.starnet.pdm@1.0              # 1. 先确认接口库能生成
m vendor.starnet.pdm@1.0-service      # 2. 再服务端（会暴露桩签名不匹配）
m pdm.hidl.cli                        # 3. 再客户端
m selinux_policy                      # 4. 策略（会做 neverallow 全量检查）
m otapackage                          # 5. 最后整包
```

### 4.9.4 快速核对策略是否进镜像

```bash
grep -c pdm_hidl out/target/product/p291_iptv/vendor/etc/selinux/vendor_sepolicy.cil
grep pdm_hidl     out/target/product/p291_iptv/vendor/etc/selinux/vendor_hwservice_contexts
grep pdm          out/target/product/p291_iptv/vendor/etc/selinux/vendor_file_contexts
```

预期输出：`48` / `vendor.starnet.pdm::IPdm  u:object_r:pdm_hidl_hwservice:s0` /
`/vendor/bin/hw/vendor.starnet.pdm@1.0-service  u:object_r:pdm_hidl_default_exec:s0`

## 4.10 增量重建的触发条件

| 改动 | 触发重跑 hidl-gen？ | 触发重编 .o？ |
|---|---|---|
| 改 `.hal` 内容 | ✅（depfile 里的 `.hal`） | ✅ |
| 改 `Android.bp` 的 `types:` | ✅ | ✅ |
| 改 `Android.bp` 的 `name`/`root` | ✅（规则本身变了） | ✅ |
| 改 `PdmHal.cpp` | ❌ | ✅（只重编服务端） |
| 改 `pdm_hidl.te` | ❌ | ❌（但 `m selinux_policy` 会重跑） |
| 改 `hidl_package_root` 的 path | ✅ | ✅ |

> **接口 hash**：hidl-gen 会为 `.hal` 算一个接口 hash 写进生成的代码。改 `.hal` 后
> 如果只重刷了服务端镜像、客户端还是旧的，第一次调用会报 `BAD_TYPE`。
> 全量 `m otapackage` 不会遇到这个问题。

# 第五部分：外部依赖与配置清单

**这一部分是最容易漏的**：以下依赖 **不在** `pdm-hidl/` 目录内，但缺一不可。
移植到新项目时必须逐项确认目标项目里也存在。

## 5.1 SELinux 类型依赖

| 类型 | 定义位置 | 缺失后果 | 两套实现是否都需要 |
|---|---|---|---|
| `sysfs_unifykey` | `device/amlogic/common/sepolicy/file.te:76` | sepolicy 编译失败 | ✅ 旧 + 新 |
| `param_tv_file` | `device/amlogic/common/sepolicy/file.te:21` | sepolicy 编译失败 | ✅ 旧 + 新 |
| `vendor_persist_prop` | `device/amlogic/common/sepolicy/property.te:9-11` | sepolicy 编译失败 | ✅ 旧 + 新 |
| `vendor_platform_prop` | 同上 | 同上 | ✅ 旧 + 新 |
| `vendor_app_prop` | 同上 | 同上 | ✅ 旧 + 新 |
| `hidl_base_hwservice` | `system/sepolicy/public/`（平台自带） | sepolicy 编译失败 | 仅新 |
| `pdm_hidl_hwservice` | **本模块自己声明**（`pdm_hidl.te`） | — | 仅新 |
| `pdm_svc` | 旧模块自己声明（`pdm_service.te`） | — | 仅旧 |

## 5.2 文件系统 / 内核依赖

| 依赖 | 本仓库位置 | 缺失后果 |
|---|---|---|
| Amlogic unifykeys 驱动（`/sys/class/unifykeys/{name,read,write,lock}`） | 内核 DTS（键名以板级 `keyman_gxlx.dtsi` 的 unifykey 节点为准） | 整个业务失效 |
| `/mnt/vendor/param` 挂载 | `device/amlogic/p291_iptv/fstab.*.amlogic:14`（`/dev/block/param` ext4） | `work_mode` 标志文件写不了 |
| `/mnt/vendor/param` 的文件标签 | `device/amlogic/common/sepolicy/file_contexts:264`（`param_tv_file`） | `work_mode` 被 SELinux 拦 |
| `/vendor/starnet/whitelist_prop.txt` | `vendor/starnet/whitelist_prop.txt` | `ro.*` 运行期改写被 init 拒收，`set` 命令"成功但属性没变" |
| `persist.ro.white_list.enable` 的 property_contexts 映射 | `vendor/starnet/sepolicy/property_contexts:7`（→ `vendor_persist_prop`） | 白名单门打不开 |

## 5.3 `whitelist_prop.txt` 的内容（与 `kKeyPropMap` 必须配套）

```
ro.product.cmcc_deviceId
ro.product.cmcc_productToken
ro.product.cmcc_andlinkToken
ro.product.cmcc_andlinktoken
ro.product.cmcc_id
ro.product.cmcc_token
ro.product.brand
ro.product.device
ro.product.manufacturer
ro.product.model
ro.product.cmcc_brand
ro.product.cmcc_imsAppId
ro.product.cmcc_imsSecret
ro.serialno
ro.product.stbid
ro.product.cmcc_sn
ro.product.serialno
ro.mac
ro.product.mac_wifi
ro.product.cmcc_password
ro.product.cmcc_cmei
ro.device.work.mode
ro.product.cmcc_date
ro.boot.sn
```

> ⚠️ **`kKeyPropMap` 里出现的每一个 `prop`，都必须在 `whitelist_prop.txt` 里有对应行**，
> 否则 `syncKeyToProperty` 的 `property_set` 会被 init 静默拒绝（日志里只是一条
> `setprop xxx failed (ro.* 已被 init 冻结时属正常)` 的 ALOGW，很容易忽略）。

## 5.4 SELinux 策略目录注册（两套实现的现状）

| 注册位置 | 内容 | 实测状态 |
|---|---|---|
| `device/amlogic/common/sepolicy.mk`（**board-config**） | `vendor/starnet/pdm-hidl/sepolicy` | ✅ 生效 |
| `vendor/starnet/starnet.mk`（product-config） | `vendor/starnet/sepolicy`、`vendor/starnet/pdm/sepolicy` | ✅ `.te`/`file_contexts` 生效；`service_contexts` 未安装 |

详见 [2.6 节](#26-外部配置旧实现相关)。

## 5.5 构建系统依赖

| 依赖 | 说明 |
|---|---|
| `hidl_package_root` / `hidl_interface` Soong 模块类型 | 需要 Android 8.0+ 的 build/soong |
| `hidl_defaults` | `build/soong` 内置的 cc defaults |
| `libhidlbase` / `libhidltransport` / `libhwbinder` | Android 9 是分开的三个库；**Android 11+ `libhidltransport` 已并入 `libhidlbase`**，移植时要从 `shared_libs` 去掉 |
| `libbinder`（仅旧实现） | Android 9 的传统 binder |

## 5.6 上板运行依赖

| 依赖 | 说明 |
|---|---|
| `hwservicemanager` 进程 | 新实现必需；`ps -A \| grep hwservicemanager` 确认 |
| `servicemanager` 进程 | 旧实现必需 |
| `init` 对 `class hal` 的处理 | 标准行为，无需额外配置 |
| `PRODUCT_ENFORCE_VINTF_MANIFEST` | 本产品**未开**；新项目若开了，manifest 片段必须并入 |

---

# 第六部分：移植 Checklist

## 6.1 拷贝与改名

```
[ ] 1. 拷贝 vendor/starnet/pdm-hidl/ 到新项目，重命名（如 vendor/<厂商>/pdm-hidl/）
[ ] 2. 全局替换包名与命名空间：
        vendor.starnet.pdm@1.0            →  <厂商>.<模块>@<版本>
        vendor::starnet::pdm::V1_0        →  <新命名空间>
        vendor/starnet/pdm/1.0/           →  <新包路径>/<版本>/
        pdm_hidl_default                  →  <新前缀>_default
        pdm_hidl_hwservice                →  <新前缀>_hwservice
        pdm.hidl.cli                      →  <新 CLI 名>
        vendor.pdm-hidl-1-0               →  <新 init 服务名>
[ ] 3. 改 interfaces/Android.bp 的 hidl_package_root（name + path）
[ ] 4. 改 interfaces/<pkg>/1.0/Android.bp 的 name / root / types
[ ] 5. 改 service/Android.bp 的 name / init_rc 文件名
[ ] 6. 改 cli/Android.bp 的 name
```

## 6.2 先编接口，再写实现

```
[ ] 7.  m <新接口库名>          ← 先只编接口，确认 hidl-gen 能生成
[ ] 8.  打开 out/soong/.intermediates/<包路径>/<包名>_genc++_headers/gen/.../<Iface>.h
         核对 PdmHal.h 的虚函数签名（不用猜 hidl_string / Return<void>）
```

## 6.3 业务适配

```
[ ] 9.  按新产品重写 kKeyPropMap：
          - 键名以板级 DTS 的 unifykey 节点为准
          - property 名与 whitelist_prop.txt 对齐
          - 填上产品默认值（本实现全是 ""，正式项目通常需要真实默认值）
[ ] 10. 若新产品 work_mode 语义相反，改 kModeUser / kModeFactory 两个常量即可
[ ] 11. 确认 unifykeys 节点路径（若内核驱动换了路径，改 UnifyKeys.h）
[ ] 12. 确认 work_mode 标志文件路径（kUserModeFile），以及所在分区已挂载且标签正确
```

## 6.4 SELinux 适配

```
[ ] 13. 确认 4 个外部类型在新项目 sepolicy 里存在：
          sysfs_unifykey / param_tv_file / vendor_persist_prop / vendor_platform_prop
[ ] 14. 改 pdm_hidl.te 的类型名与路径
[ ] 15. 改 file_contexts 的服务路径
[ ] 16. 改 hwservice_contexts 的 key（★ 不带 @版本号）
[ ] 17. 确认 add_hwservice 宏在新 AOSP 版本里的定义（有的版本会自己声明类型）
[ ] 18. 若客户端不在 shell 域，按实际域名补 find + binder call/transfer
```

## 6.5 构建集成

```
[ ] 19. 新建 <模块>.mk（PRODUCT_PACKAGES），由 product-config 阶段的 makefile include
[ ] 20. BOARD_SEPOLICY_DIRS 追加到 board-config 阶段的文件（不要两处都写）
[ ] 21. 若开了 PRODUCT_ENFORCE_VINTF_MANIFEST：DEVICE_MANIFEST_FILE 追加 manifest 片段，
          位置避开所有会 sed -i 改 manifest 的 $(shell ...)
[ ] 22. 本模块目录下不要放 Android.bp，也不要用 Android.mk 自动包含
```

## 6.6 编译与验证

```
[ ] 23. m <服务名> <CLI 名>
[ ] 24. m selinux_policy          ← 会做 neverallow 全量检查
[ ] 25. 上板：
          adb shell getenforce                    # 先确认是否 Enforcing
          adb shell lshal | grep <包名>           # 服务应列出
          adb shell <CLI> list
          adb shell <CLI> get model
          adb shell <CLI> set model TEST
          adb shell getprop ro.product.model      # 验证属性同步
          adb shell <CLI> set work_mode 1         # 用户模式
          adb shell <CLI> set work_mode 0         # 工厂模式
[ ] 26. logcat -s pdm-hidl pdm-hidl-cli hwservicemanager
[ ] 27. dmesg | grep avc          # 按需补策略
```

## 6.7 旧服务去留（方案阶段就要定）

```
[ ] 28. 若确定只用 HIDL：从 PRODUCT_PACKAGES 摘掉 pdm-service / pdm.cli
[ ] 29. 若 App 仍在调旧服务：★ 保留旧服务 ★
          Treble 的 app_neverallows.te 对 all_untrusted_apps → halserverdomain
          有 neverallow，普通 App 拿不到 HIDL HAL 的 find/call 权限。
          需要 App 通道时方案只有两条：
            (a) 保留旧 binder 服务
            (b) 另加一个 system service / manager 做转发
[ ] 30. 两服务并存时都会各做一次开机属性同步（写同样的值，unifykeys 侧有 flock
          串行化），不会互相破坏；只想留一个同步来源就停掉其中一个
```

---

# 第七部分：踩坑记录

## 7.1 构建系统

| # | 现象 | 根因 / 结论 |
|---|---|---|
| 1 | `vendor/starnet/pdm-hidl/Android.mk: cannot assign to readonly variable: PRODUCT_PACKAGES` | `Android.mk` 只会在 kati 主阶段被扫到，那时 `PRODUCT_PACKAGES` 已只读。**用 `<模块>.mk` + include 的写法** |
| 2 | 同目录下 `Android.bp` 被忽略 | Soong 见到同目录 `Android.mk` 就跳过 `Android.bp`。Soong 模块都放子目录 |
| 3 | 改了 `.hal` 但生成的 `types.cpp` 缺项 → 链接报错 | `Android.bp` 的 `types:` 列表必须与 `types.hal` 同步维护 |
| 4 | hidl-gen 报 `Could not find package` | `interfaces/Android.bp` 的 `hidl_package_root` 的 `path` 写错，或没写 |
| 5 | 策略目录 append 了但没进镜像 | 见 [2.6 节](#26-外部配置旧实现相关)——**新目录放 board-config 阶段**（已验证） |
| 6 | `DEVICE_MANIFEST_FILE` 追加的 manifest 被 sed 改坏 | 必须放在 `AUTO_PATCH_AB`/`AUTO_PATCH_NPU` 两个 `$(shell ...)` **之后** |

## 7.2 SELinux

| # | 现象 | 根因 / 结论 |
|---|---|---|
| 7 | 加 `seclabel` 后 neverallow 编译失败 | `neverallow * halserverdomain:process dyntransition`。HAL 域转换靠 `file_contexts` + `init_daemon_domain` |
| 8 | 照抄 `pdm_service.te` 的 `execute_no_trans` | `neverallow { halserverdomain ... } { file_type fs_type }:file execute_no_trans`。HAL 不 exec 任何东西 |
| 9 | `type pdm_hidl_hwservice` 重复声明报错（换 AOSP 版本时） | 本树 `add_hwservice` 宏不声明类型；有的版本声明。移植前先看 `te_macros` |
| 10 | hwservicemanager 日志 `No match for interface ... in hwservice_contexts` | `hwservice_contexts` 的 key 写成了 `vendor.starnet.pdm@1.0::IPdm`。**必须不带版本号** |
| 11 | CLI 报 `service not found`，但 `ps` 能看到服务进程 | 见 [7.4 排错速查](#74-运行时排错速查) |

## 7.3 业务逻辑（两套实现共通）

| # | 现象 | 根因 / 结论 |
|---|---|---|
| 12 | `list` 所有值全空，但 `get model` 能取到值 | `readAllEntries` 没走 fallback 链。**两处必须用同一条链**（0905 实测） |
| 13 | `list` 显示 unifykey 旧值，`get work_mode` 显示文件真值 | `work_mode` 在两个函数里来源不一致。**都以标志文件存在性为准** |
| 14 | `set xxx` 返回成功但 `getprop` 没变 | `/vendor/starnet/whitelist_prop.txt` 缺该属性行，或 `persist.ro.white_list.enable` 的 property_contexts 映射缺失 |
| 15 | `set work_mode 2` 命令成功但模式没变 | 取值校验缺失。`work_mode` 只接受 `"0"`/`"1"`，否则返回 `BAD_VALUE` |
| 16 | `set ""` 假成功 | 空 name 必须返回 `NAME_NOT_FOUND`，不能 writeFile 空串到 sysfs |
| 17 | 白名单门常开（安全问题） | `RoWhiteListGate` 必须是 RAII，任何 return 路径都关回去 |
| 18 | 属性值带尾部换行 | sysfs 读出来的值要用 `trimTail` 去掉 `\n`/`\r` |

## 7.4 运行时排错速查

### 7.4.1 新实现（HIDL）

**最关键的推理链**（这是最容易误判的地方）：

```
服务进程活着（ps 可见，停在 binder_thread_read）
   ⟹ registerAsService() 返回了 OK
   ⟹ hwservicemanager 的 ServiceManager::add() 返回了 true
   ⟹ 设备上的 vendor_hwservice_contexts 有标签，且 pdm_hidl_default 有 add 权限
   ⟹ 服务确实在注册表里

那 getService() 为什么返回 nullptr？
   ⟹ getRawServiceInternal 里 getTransport() 拿到了 EMPTY
   ⟹ EMPTY 只有两个来源：
       (a) 客户端对 pdm_hidl_hwservice 没有 find 权限
       (b) 注册表里已经没有这条了（hwservicemanager 崩过/重启过）
```

依据：生成的 `PdmAll.cpp:888` 是
`return ret.isOk() && ret ? ::android::OK : ::android::UNKNOWN_ERROR;`，
而 `ServiceManager::add()` 在 `canAdd` 被拒时返回的是 `bool false`（不是 error）。
`main.cpp` 检查了返回值并 `return 1`，所以**进程活着 = 注册成功**。

**诊断命令**：

```bash
# ① hwservicemanager 活着吗；注册表整体状态
ps -A | grep hwservicemanager
lshal                              # 条目很少 → 走了 (b)

# ② 服务进程 / 当前 shell 分别是什么域
cat /proc/<服务pid>/attr/current   # 期望 u:r:pdm_hidl_default:s0
cat /proc/self/attr/current        # 决定了 .te 里 allow shell ... 生不生效

# ③ 设备上的策略是不是这份构建产物
grep pdm_hidl /vendor/etc/selinux/vendor_hwservice_contexts
grep -c pdm_hidl /vendor/etc/selinux/vendor_sepolicy.cil

# ④ avc（最直接）
dmesg | grep -i avc | tail -20
logcat -d -s hwservicemanager | tail -40
```

> **为什么 `lshal` 是好判据**：`ServiceManager::list()`（`ServiceManager.cpp:310-328`）
> 只检查 `canList`，**不检查 `canGet`**。所以服务名出现在 `lshal` 里就说明它确实注册成功了
> ——即使 `find` 被拒也会照样列出来。

**按结果分支**：

| 观察 | 结论 | 处置 |
|---|---|---|
| `/proc/<pid>/attr/current` 不是 `pdm_hidl_default` | 服务跑在别的域（多半是旧 binary 或 file_contexts 没生效） | 比对 md5 与 `out/target/product/.../vendor/bin/hw/...`；重刷 `vendor.img` |
| `cat /proc/self/attr/current` 不是 `shell` | console 的域不是 shell → `allow shell ...` 不适用 | 用 `adb shell` 复测；若 adb 能过，按实际域名补规则 |
| `/vendor/etc/selinux/` 里没有 `pdm_hidl` | 设备 vendor 分区不是这份产物 | 重刷 `vendor.img`/整包 |
| `lshal` 整体条目很少 | hwservicemanager 崩过重启，注册表丢了 | `setprop ctl.restart vendor.pdm-hidl-1-0`；根因查 hwservicemanager 日志 |
| `dmesg` 里有 `avc: denied { find }` | 客户端缺 find 权限 | 按 avc 里的 `scontext=` 补规则 |

**本产品的一个特殊点**：console 被改写过（`device/amlogic/common/shadow/console.rc`）：

```
service console /system/bin/login -p
    override
    user shell
    seclabel u:r:shell:s0
```

`login` 本身不做域转换（toybox 的 login 里没有 `setexeccon`），但如果在 console 里
`su` 过，`su.te` 有 `domain_auto_trans(shell, su_exec, su)`，进程会变成 `u:r:su:s0`。
虽然 `su` 域在 userdebug 下是 `permissive su;`（permissive 域上 `selinux_check_access`
会放行），但仍然建议用 `cat /proc/self/attr/current` 确认，别靠猜。

### 7.4.2 旧实现（binder）

```bash
ps -A | grep pdm-service
cat /proc/<pid>/attr/current       # 期望 u:r:pdm_service:s0
lshal 不适用（HIDL 专用）→ 用 cat /sys/kernel/debug/binder/proc/<pid> 或 dumpsys
dmesg | grep -i avc | tail -20
logcat -s pdm pdm-cli
```

旧实现的历史问题（已在代码里修掉，仅作背景）：

| 现象 | 根因 |
|---|---|
| `getValue: empty name → NAME_NOT_FOUND (-2)`，`pdm.cli` 全挂 | `BnPdm::onTransact` 里用了 `Parcel::enforceInterface()`，与手工 probe 的 `tokenOffset` 对不上 → payload 错位。**改成手工 probe token + `readString16Inplace()` 三步**（见 `IPdm.cpp` 顶部注释） |
| `readString16Inplace returned NULL` → `BAD_VALUE` | 在 `readString16Inplace()` 之前又手动 `readInt32(len)`，位置落到 token 字符上 |
| 业务错误被当成传输错误 | `remote()->transact()` 的返回值只是传输层结果，业务错误在 `reply.readInt32()` 里 |

## 7.5 从旧实现查 bug 时的一个反复出现的教训

> **"服务进程活着" ≠ "服务注册成功"。**

两套实现的注册路径不同，容错方向也不同：

| | 旧实现 `addService()` | 新实现 `registerAsService()` |
|---|---|---|
| 失败时的返回值 | 明确的 `status_t` 错误码 | **`bool false`**（`canAdd` 被拒时） |
| 客户端是否感知 | 是（err != NO_ERROR） | 是（生成代码转成 `UNKNOWN_ERROR`） |
| 但若调用方**不检查返回值** | 进程继续跑，服务没注册 | 进程继续跑，服务没注册 → **幽灵状态** |

所以两边的 `main.cpp` 都**必须**检查返回值并退出。排查"`ps` 能看到进程但客户端报
not found"这类问题时，第一步永远是：**先确认服务到底在不在注册表里**（
新实现用 `lshal`，旧实现用 `dumpsys` / 直接调用试）。

---

*文档结束。源码以仓库内实际文件为准；本文档中所有路径、行数、命令输出均基于
2026-09-12 的 `p291_iptv` 构建产物核对。*




