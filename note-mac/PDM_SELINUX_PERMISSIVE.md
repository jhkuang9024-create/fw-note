# 宽容模式（Permissive）下为什么 pdm 仍必须配置 SELinux

> 记录时间：2026-09-10
> 触发问题：设备串口 `getenforce` 返回 `Permissive`，为何 `vendor/starnet/pdm` 仍然要配一整套 sepolicy？
> 涉及模块：`vendor/starnet/pdm`（pdm-service + pdm.cli）

---

## 0. 结论先说

**`getenforce` 返回 Permissive，只关掉了内核 AVC 的"拒绝动作"，另外三件事它一件都管不了**，而 pdm 恰好三件全踩：

| 机制 | 发生在哪 | Permissive 能否绕过 |
|---|---|---|
| `neverallow` / `checkfc` 校验 | **编译期** | ❌ 否（跟设备运行时状态完全无关） |
| `selabel_lookup` 查 `*.contexts` | 运行时 **userspace** | ❌ 否，查不到直接 `return false`，不进内核 AVC |
| context 串 → SID 解析 | 内核，但**不是访问检查** | ❌ 否，类型不存在就是 `-EINVAL` |

一句话记忆：

> **`*.contexts` 标签表在 Permissive 下也必须有；`allow` 规则在 Permissive 下才可以先欠着。**
> 而标签表里引用的类型必须先在 `.te` 里 `type` 出来，否则编译期就不认。

---

## 1. Permissive 到底关掉了什么

内核里 `avc_has_perm()` 在 permissive 时直接返回 0，denial 只记一条日志：

```
avc: denied { write } for pid=... scontext=u:r:pdm_service:s0 tcontext=... permissive=1
```

所以 `vendor/starnet/pdm/sepolicy/pdm_service.te` 里那些 `allow xxx` —— **在宽容模式下确实可以暂时不写**。

但下面三样不走这条路径，必须老老实实配齐（见第 2 节实证）。

---

## 2. 本项目里的三条硬证据

### 2.1 `pdm-service.rc` 的 `seclabel` 会让 init 直接 FATAL

`vendor/starnet/pdm/pdm-service.rc` 中：

```
service pdm-service /vendor/bin/pdm-service
    class core
    user root
    group root
    seclabel u:r:pdm_service:s0
```

init 在 `exec` 前会 `setexeccon()`，失败即 FATAL：

```cpp
// system/core/init/service.cpp:313-317
if (!seclabel_.empty()) {
    if (setexeccon(seclabel_.c_str()) < 0) {
        PLOG(FATAL) << "cannot setexeccon('" << seclabel_ << "') for " << name_;
    }
}
```

若没有 `type pdm_service, domain`，`u:r:pdm_service:s0` 这个串在 `security_context_to_sid()` 阶段就解析失败。
**这是"标签解析"而不是"权限检查"，Permissive 不会把非法 context 变成合法。**
结果：子进程 abort，服务起不来（还可能进重启循环）。

> 想靠"不写 .te"省事，反而是最直接的死法。

### 2.2 `service_contexts` 查不到 = binder 服务注册不上

```c
// frameworks/native/cmds/servicemanager/service_manager.c:92-110
static bool check_mac_perms_from_lookup(pid_t spid, uid_t uid, const char *perm, const char *name)
{
    ...
    if (selabel_lookup(sehandle, &tctx, name, 0) != 0) {
        ALOGE("SELinux: No match for %s in service_contexts.\n", name);
        return false;          // ← 直接拒，根本没进内核 AVC
    }
    allowed = check_mac_perms(spid, uid, tctx, perm, name);
    ...
}
```

调用方 `svc_can_register()` / `svc_can_find()`（同文件 `:112` / `:129`）据此放行或拒绝。

因此 `vendor/starnet/pdm/sepolicy/service_contexts` 里这条：

```
vendor.starnet.pdm    u:object_r:pdm_svc:s0
```

是**运行时硬前提** —— 缺了它，Permissive 下 `addService` 一样失败，`pdm.cli` 一样找不到服务。
注意失败点位于 `selinux_check_access()` **之前**，所以宽容模式救不了。

### 2.3 `property_contexts` 没有条目 = setprop 直接被 init 拒收

`vendor/starnet/pdm/PdmService.cpp:283-305` 的 `RoWhiteListGate` 依赖：

```cpp
property_set("persist.ro.white_list.enable", "1");
```

init 侧的处理：

```cpp
// system/core/init/property_service.cpp:113-117
static bool CheckMacPerms(const std::string& name, const char* target_context,
                          const char* source_context, const ucred& cr) {
    if (!target_context || !source_context) {
        return false;      // ← target_context 来自编译进 property_contexts 的表
    }
    ...
    bool has_access = (selinux_check_access(source_context, target_context,
                                            "property_service", "set", &audit_data) == 0);
    return has_access;
}

// system/core/init/property_service.cpp:468-473
const char* target_context = nullptr;
property_info_area->GetPropertyInfo(name.c_str(), &target_context, &type);

if (!CheckMacPerms(name, target_context, source_context.c_str(), cr)) {
    *error = "SELinux permission check failed";
    return PROP_ERROR_PERMISSION_DENIED;
}
```

`target_context` 由 `GetPropertyInfo()` 从 `vendor/starnet/sepolicy/property_contexts` 查得：

```
persist.ro.white_list.enable   u:object_r:vendor_persist_prop:s0
```

**条目缺失 → `target_context == nullptr` → 硬拒**，此时连 `selinux_check_access()` 都还没被调用。
而宽容模式能放行的，恰恰只有后面那一步。

> 后果：白名单门开不了 → `syncKeyToProperty()` 里所有 `ro.*` 改写全部失败。
> 这条在 Permissive 下同样成立，可以直接在串口验证。

---

## 3. 编译期这一关（与 `getenforce` 完全无关）

`BOARD_SEPOLICY_DIRS`（`vendor/starnet/starnet.mk:69-70`）加进去的 `.te` 参与 `checkpolicy` 编译，**`neverallow` 是 build 时检查**：

- 违反 `neverallow` → 编译直接失败，报 `neverallow check failed`；
- `file_contexts` / `service_contexts` 引用了 policy 中不存在的类型 → `checkfc` 报 invalid context，编译失败。

`vendor/starnet/pdm/Android.bp` 开头那段注释自己就记录了这个教训：为了绕开 `property.te` 的 neverallow（coredomain 写非 core property），才把 `pdm-service` 从 system 挪到 **vendor 分区**，使其成为 non-coredomain。

这类约束在 PC 上编译时就生效，跟板子跑在什么模式下、串口 `getenforce` 显示什么，没有任何关系。

---

## 4. 工程现实原因

1. **Permissive 是调试态，不是出厂态。**
   `setenforce 0` 重启即失效；出厂 user 版、`androidboot.selinux=enforcing`、CTS/VTS/GMS、运营商入库都要求 `getenforce` = `Enforcing`。

2. **现在不写，等于把问题攒到切 Enforcing 那天集中爆发**，且那时没有 denial 日志可参考，定位成本最高。

3. **Permissive 期恰恰是写 policy 的最佳窗口**：`allow` 写得对不对，看 `dmesg | grep avc` 里有没有 `permissive=1` 就知道 —— 日志照出，功能不挡。

   ```sh
   # 串口上自检
   dmesg | grep -i avc | grep pdm_service
   ```

4. 目前这套 policy 覆盖已经比较完整：`sysfs_unifykey`、`param_tv_file`、三个 vendor prop、binder 调用方（`shell` / `system_app`）都写了。

---

## 5. 如果确实想先放掉访问检查

本仓库 `device/amlogic/common/sepolicy/` 里已有一批**按域灰度**的先例，例如：

- `device/amlogic/common/sepolicy/stbdetector.te`
- `device/amlogic/common/sepolicy/motion_detect.te`
- `device/amlogic/common/sepolicy/preinstall.te`

做法是在 `.te` 里加一行：

```te
permissive pdm_service;
```

但必须清楚它的边界：**只放掉访问检查**。第 2 节的 (1)(2)(3) 以及第 3 节的编译期约束，一样救不了 —— `type pdm_service`、`service_contexts`、`property_contexts` 这三张表还是得补齐。

也就是说，无论走哪条路，下面这 3 个小文件都省不掉。

---

## 6. 附：pdm 实际需要的 SELinux 配置清单

| 文件 | 作用 | 能否省略 |
|---|---|---|
| `vendor/starnet/pdm/sepolicy/pdm_service.te` | 定义 `pdm_service` / `pdm_service_exec` / `pdm_svc` 类型 + allow 规则 | ❌ 类型定义必须有（否则 init FATAL + 编译失败） |
| `vendor/starnet/pdm/sepolicy/file_contexts` | `/vendor/bin/pdm-service` → `pdm_service_exec` | ❌ 编译期 checkfc + 出厂 enforcing 需要 |
| `vendor/starnet/pdm/sepolicy/service_contexts` | `vendor.starnet.pdm` → `pdm_svc` | ❌ 缺了注册不上 binder 服务（Permissive 也一样） |
| `vendor/starnet/sepolicy/property_contexts` | `persist.ro.white_list.enable` → `vendor_persist_prop` | ❌ 缺了 setprop 被 init 硬拒（Permissive 也一样） |
| `vendor/starnet/pdm/pdm-service.rc` 的 `seclabel` | 域切换入口 | ❌ init 硬要求，context 非法即 FATAL |
| `vendor/starnet/starnet.mk:69-70` | 把 `vendor/starnet/pdm/sepolicy` 挂进 `BOARD_SEPOLICY_DIRS` | ❌ 不挂等于没写 |
| `pdm_service.te` 中的各条 `allow` | 具体访问权限 | ✅ 仅 Permissive 下可暂缺（Enforcing 必挂） |

### 切换 Enforcing 前的自检清单

```sh
# 1. 当前模式
getenforce

# 2. 本域是否还有 denial（换 Enforcing 后会变成真失败）
dmesg | grep -i avc | grep -E "pdm_service|pdm_svc"

# 3. 服务是否真的起来并完成域切换
ps -Z | grep pdm-service          # 期望 u:r:pdm_service:s0
getprop | grep init.svc.pdm-service

# 4. binder 服务注册是否正常（看 servicemanager 有无 No match 报错）
logcat -b all | grep -E "No match for vendor.starnet.pdm|service_manager"

# 5. 白名单门开关属性是否可写
setprop persist.ro.white_list.enable 1 && getprop persist.ro.white_list.enable
```
