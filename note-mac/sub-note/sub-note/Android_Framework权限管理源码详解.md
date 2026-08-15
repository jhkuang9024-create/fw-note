# Android Framework 权限管理源码详解

> 基于 amlogic_s905d5 项目源码分析，结合实际场景深入讲解 Android 权限管理的设计、实现与调用链路。

---

## 目录

1. [架构总览](#一架构总览)
2. [权限的定义与声明](#二权限的定义与声明)
3. [权限状态数据模型](#三权限状态数据模型)
4. [权限检查调用链路](#四权限检查调用链路)
5. [权限授予与撤销流程](#五权限授予与撤销流程)
6. [AppOps 双层权限模型](#六appops-双层权限模型)
7. [系统权限配置与特殊权限](#七系统权限配置与特殊权限)
8. [权限升级与拆分机制](#八权限升级与拆分机制)
9. [实际场景演练](#九实际场景演练)
10. [Framework 开发实践要点](#十framework-开发实践要点)

---

## 一、架构总览

### 1.1 三层架构图

Android Framework 权限管理采用**服务化分层架构**，整体分为三层：

```
┌──────────────────────────────────────────────────────────────────────────┐
│                         应用层 (App / API Provider)                        │
│                                                                          │
│  ┌──────────────────────────────┐  ┌─────────────────────────────────┐   │
│  │ Context.checkPermission()    │  │ PermissionChecker.xxx()         │   │
│  │ (仅检查 Runtime Permission)  │  │ (联合检查 Permission + AppOps)  │   │
│  └──────────────────────────────┘  └─────────────────────────────────┘   │
├──────────────────────────────────────────────────────────────────────────┤
│                     Framework Binder 服务层 (System Server)               │
│                                                                          │
│  ┌─────────────────────────────┐  ┌────────────────────────────────────┐ │
│  │ PermissionManagerService    │  │ AppOpsService                      │ │
│  │ ├─ IPermissionManager.Stub  │  │ ├─ IAppOpsService.Stub             │ │
│  │ ├─ PermissionManagerService │  │ ├─ UidState 管理                   │ │
│  │ │  Impl (核心实现, 249KB)   │  │ ├─ noteOp / startOp / finishOp     │ │
│  │ ├─ PermissionRegistry      │  │ └─ MODE_ALLOWED / IGNORED / ERRORD │ │
│  │ ├─ DevicePermissionState   │  │                                    │ │
│  │ └─ CheckPermissionDelegate │  │                                    │ │
│  └─────────────────────────────┘  └────────────────────────────────────┘ │
│                                                                          │
│  ┌──────────────────────────────────────────────────────────────────────┐│
│  │ ActivityManagerService (组件级权限强制检查)                            ││
│  │ ├─ checkComponentPermission(perm, pid, uid, owningUid, exported)     ││
│  │ ├─ enforceCallingPermission(perm, func) → throw SecurityException    ││
│  │ └─ sActiveProcessInfoSelfLocked (PID级拒绝缓存)                      ││
│  └──────────────────────────────────────────────────────────────────────┘│
├──────────────────────────────────────────────────────────────────────────┤
│                         持久化数据层                                      │
│                                                                          │
│  ┌────────────────────────────────┐  ┌──────────────────────────────┐    │
│  │ /data/system/users/0/         │  │ /data/system/appops.xml      │    │
│  │   runtime-permissions.xml     │  │ (AppOps 状态持久化)           │    │
│  │   (Runtime权限授予记录)        │  │                              │    │
│  └────────────────────────────────┘  └──────────────────────────────┘    │
│                                                                          │
│  ┌──────────────────────────────────────────────────────────────────────┐│
│  │ /etc/permissions/platform.xml (系统UID权限映射)                       ││
│  └──────────────────────────────────────────────────────────────────────┘│
└──────────────────────────────────────────────────────────────────────────┘
```

### 1.2 权限管理的两个独立子系统

Android 权限由 **两个独立但协作的子系统** 组成：

| 子系统 | Binder 服务名 | 核心类 | 职责 |
|--------|--------------|--------|------|
| **PermissionManager** | `"permissionmgr"` | `PermissionManagerService` + `PermissionManagerServiceImpl` | 管理权限的**授予/撤销状态** |
| **AppOpsManager** | `"appops"` | `AppOpsManager` + `AppOpsService` | 控制 API 访问的**行为模式**（允许/静默/抛异常） |

**关键理解**：每个 `dangerous` 级别的 Runtime Permission 都对应一个 AppOp code：

```
Permission                    ↔  AppOp Code
─────────────────────────────────────────────
android.permission.CAMERA     ↔  OP_CAMERA (26)
android.permission.RECORD_AUDIO ↔ OP_RECORD_AUDIO (27)
android.permission.ACCESS_FINE_LOCATION ↔ OP_FINE_LOCATION (1)
android.permission.ACCESS_COARSE_LOCATION ↔ OP_COARSE_LOCATION (0)
android.permission.READ_CONTACTS ↔ OP_READ_CONTACTS (4)
android.permission.READ_SMS   ↔  OP_READ_SMS (14)
...
```

---

## 二、权限的定义与声明

### 2.1 平台权限在 AndroidManifest.xml 中声明

**文件**：`frameworks/base/core/res/AndroidManifest.xml`

```xml
<!-- CAMERA 权限定义 -->
<permission android:name="android.permission.CAMERA"
    android:permissionGroup="android.permission-group.UNDEFINED"
    android:label="@string/permlab_camera"
    android:description="@string/permdesc_camera"
    android:backgroundPermission="android.permission.BACKGROUND_CAMERA"
    android:protectionLevel="dangerous|instant" />
```

### 2.2 保护级别 (protectionLevel) 详解

| 级别 | 含义 | 授予方式 | 典型权限 |
|------|------|----------|----------|
| `normal` | 普通权限 | 安装时自动授予，用户不可撤销 | `INTERNET`, `ACCESS_NETWORK_STATE` |
| `dangerous` | 危险权限 | 运行时动态申请，需用户确认 | `CAMERA`, `RECORD_AUDIO`, `ACCESS_FINE_LOCATION` |
| `signature` | 签名权限 | 仅同签名应用可获取 | `BIND_CELL_BROADCAST_SERVICE`, `HARDWARE_TEST` |
| `signature\|privileged` | 特权签名权限 | 仅 system/priv-app 目录下的系统应用 | `WRITE_OBB`, `BLUETOOTH_PRIVILEGED` |
| `instant` | 即时应用可用 | 即时应用可申请 | 与 `dangerous` 组合使用 |
| `appop` | AppOp 权限 | 由 AppOps 系统控制 | 与 `dangerous` 组合 |
| `role` | 角色权限 | 由角色管理器授予 | `SYSTEM_WELLBEING`, `COMPANION_DEVICE_MANAGER` |

### 2.3 权限在注册表中的管理

**文件**：`frameworks/base/services/core/java/com/android/server/pm/permission/PermissionRegistry.java`

```java
public class PermissionRegistry {
    // 权限名 -> Permission 对象
    private final ArrayMap<String, Permission> mPermissions = new ArrayMap<>();

    // 权限树 (动态权限)
    private final ArrayMap<String, Permission> mPermissionTrees = new ArrayMap<>();

    // 权限组
    private final ArrayMap<String, ParsedPermissionGroup> mPermissionGroups = new ArrayMap<>();

    // AppOp 权限包映射
    private final ArrayMap<String, ArraySet<String>> mAppOpPermissionPackages = new ArrayMap<>();
}
```

---

## 三、权限状态数据模型

### 3.1 三层状态结构

权限状态采用**设备 → 用户 → UID** 的三层嵌套结构：

```
DevicePermissionState (设备级, 单例)
  └── UserPermissionState (用户级, 按 userId 索引, SparseArray)
        └── UidPermissionState (UID级, 按 appId 索引, SparseArray)
              └── PermissionState (单个权限的状态)
```

**文件**：`DevicePermissionState.java`

```java
public final class DevicePermissionState {
    private final SparseArray<UserPermissionState> mUserStates = new SparseArray<>();

    public UserPermissionState getUserState(@UserIdInt int userId) {
        return mUserStates.get(userId);
    }
}
```

在 `PermissionManagerServiceImpl` 中的初始化：

```java
// 第266行
private final DevicePermissionState mState = new DevicePermissionState();
```

### 3.2 UidPermissionState 详解

**文件**：`frameworks/base/services/core/java/com/android/server/pm/permission/UidPermissionState.java`

这是权限管理的核心数据结构，以 **UID** 为粒度存储权限状态：

```java
public final class UidPermissionState {
    private boolean mMissing;  // 权限状态是否丢失（如回滚场景）
    @Nullable
    private ArrayMap<String, PermissionState> mPermissions;  // 权限名 -> 权限状态

    // === 核心方法 ===

    // 检查某个权限是否已授予
    public boolean isPermissionGranted(@NonNull String name) {
        final PermissionState permissionState = getPermissionState(name);
        return permissionState != null && permissionState.isGranted();
    }

    // 授予一个权限
    public boolean grantPermission(@NonNull Permission permission) {
        final PermissionState permissionState = getOrCreatePermissionState(permission);
        return permissionState.grant();  // 返回是否状态发生了变化
    }

    // 撤销一个权限
    public boolean revokePermission(@NonNull Permission permission) {
        final String name = permission.getName();
        final PermissionState permissionState = getPermissionState(name);
        if (permissionState == null) {
            return false;
        }
        final boolean changed = permissionState.revoke();
        // 如果权限状态回到了默认值（未授予+无标志），则从 map 中移除
        if (changed && permissionState.isDefault()) {
            removePermissionState(name);
        }
        return changed;
    }

    // 获取或创建 PermissionState（懒加载）
    private PermissionState getOrCreatePermissionState(@NonNull Permission permission) {
        if (mPermissions == null) {
            mPermissions = new ArrayMap<>();
        }
        final String name = permission.getName();
        PermissionState permissionState = mPermissions.get(name);
        if (permissionState == null) {
            permissionState = new PermissionState(permission);
            mPermissions.put(name, permissionState);
        }
        return permissionState;
    }

    // 计算 Linux GID
    public int[] computeGids(@NonNull int[] globalGids, @UserIdInt int userId) {
        IntArray gids = IntArray.wrap(globalGids);
        if (mPermissions == null) return gids.toArray();
        for (int i = 0; i < mPermissions.size(); i++) {
            PermissionState permissionState = mPermissions.valueAt(i);
            if (!permissionState.isGranted()) continue;
            final int[] permissionGids = permissionState.computeGids(userId);
            if (permissionGids.length != 0) {
                gids.addAll(permissionGids);
            }
        }
        return gids.toArray();
    }
}
```

### 3.3 权限标志位 (Permission Flags)

每个权限除了授予/未授予状态外，还附带一组标志位：

| 标志位 | 含义 |
|--------|------|
| `FLAG_PERMISSION_USER_SET` | 用户主动设置过（授予或拒绝） |
| `FLAG_PERMISSION_USER_FIXED` | 用户勾选了"不再询问" |
| `FLAG_PERMISSION_SYSTEM_FIXED` | 系统固定，不可更改 |
| `FLAG_PERMISSION_POLICY_FIXED` | 设备策略固定，不可更改 |
| `FLAG_PERMISSION_REVIEW_REQUIRED` | 需要重新审核 |
| `FLAG_PERMISSION_ONE_TIME` | 一次性权限 |
| `FLAG_PERMISSION_RESTRICTION_UPGRADE_EXEMPT` | 免除权限升级限制 |

---

## 四、权限检查调用链路

### 4.1 整体调用链

以**相机权限检查**为例，完整的 Binder IPC 调用链路：

```
1. App 调用 Camera.open()
        ↓
2. CameraService::connectDevice()
        ↓
3. PermissionChecker.checkPermissionForDataDelivery(
        context, "android.permission.CAMERA", ...)
        ↓
4. PermissionCheckerManager.checkPermission()
        ↓ (判断是否关联 AppOp)
        ↓
5. [快速路径] 无 AppOp 关联 → Context.checkPermission() 本地检查
   [慢速路径] 有 AppOp 关联 → Binder IPC
        ↓
6. IPermissionChecker.Stub (Binder 调用)
        ↓
7. PermissionManagerService.checkPermission()
        ↓ 委托给
8. PermissionManagerServiceImpl.checkPermissionInternal()
        ├── 检查 UidPermissionState 中 CAMERA 是否授予
        ├── 检查 FULLER_PERMISSION_MAP 是否有更全权限
        └── 检查 AppOpsService 中 OP_CAMERA 的 mode
              ├── MODE_ALLOWED → PERMISSION_GRANTED
              ├── MODE_IGNORED → PERMISSION_SOFT_DENIED (静默失败)
              └── MODE_ERRORED → PERMISSION_HARD_DENIED (抛异常)
```

### 4.2 场景一：组件启动时的权限检查 (ActivityManagerService)

**文件**：`frameworks/base/services/core/java/com/android/server/am/ActivityManagerService.java`

当一个 Activity/Service/BroadcastReceiver 被启动时，AMS 会进行组件级权限检查：

```java
/**
 * 组件权限检查 — 所有组件 IPC 调用的第一道防线
 */
public static int checkComponentPermission(
        @PermissionName String permission,
        int pid, int uid,
        int owningUid, boolean exported) {

    // 第1步：自身进程直接放行（同一个 system_server 内的调用）
    if (pid == MY_PID) {
        return PackageManager.PERMISSION_GRANTED;
    }

    // 第2步：检查进程级拒绝缓存（避免重复 IPC 查询）
    if (permission != null) {
        synchronized (sActiveProcessInfoSelfLocked) {
            ProcessInfo procInfo = sActiveProcessInfoSelfLocked.get(pid);
            if (procInfo != null && procInfo.deniedPermissions != null
                    && procInfo.deniedPermissions.contains(permission)) {
                return PackageManager.PERMISSION_DENIED;
            }
        }
    }

    // 第3步：委托给 ActivityManager 的静态方法
    return ActivityManager.checkComponentPermission(
            permission, uid, owningUid, exported);
}
```

**`ActivityManager.checkComponentPermission`** 是底层判断逻辑：

```java
public static int checkComponentPermission(String permission, int uid,
        int owningUid, boolean exported) {

    // 第1步：Root(uid=0) 和 System(uid=1000) 直接放行
    final int appId = UserHandle.getAppId(uid);
    if (canAccessUnexportedComponents(uid)) {
        return PackageManager.PERMISSION_GRANTED;
    }

    // 第2步：隔离进程直接拒绝
    if (UserHandle.isIsolated(uid)) {
        return PackageManager.PERMISSION_DENIED;
    }

    // 第3步：组件所有者直接放行（App访问自己的组件）
    if (owningUid >= 0 && UserHandle.isSameApp(uid, owningUid)) {
        return PackageManager.PERMISSION_GRANTED;
    }

    // 第4步：非导出组件拒绝外部访问
    if (!exported) {
        return PackageManager.PERMISSION_DENIED;
    }

    // 第5步：无权限要求则放行
    if (permission == null) {
        return PackageManager.PERMISSION_GRANTED;
    }

    // 第6步：最终通过 PackageManager 查询权限状态
    try {
        return AppGlobals.getPackageManager().checkUidPermission(permission, uid);
    } catch (RemoteException e) {
        throw e.rethrowFromSystemServer();
    }
}
```

**决策树总结**：

```
checkComponentPermission(perm, uid, owningUid, exported)
│
├── pid == MY_PID ? → GRANTED (自身进程)
├── deniedPermissions.contains(perm) ? → DENIED (缓存拒绝)
├── appId == ROOT/SYSTEM ? → GRANTED (特权进程)
├── Isolated ? → DENIED (隔离进程)
├── sameApp(uid, owningUid) ? → GRANTED (组件所有者)
├── !exported ? → DENIED (非导出组件)
├── perm == null ? → GRANTED (无权限要求)
└── else → checkUidPermission(perm, uid) (最终查询)
```

### 4.3 场景二：系统服务中的权限强制执行 (enforce)

**文件**：`ActivityManagerService.java` (行 6055-6101)

```java
// 检查调用者权限，不满足则抛 SecurityException
void enforceCallingPermission(String permission, String func) {
    if (checkCallingPermission(permission) == PackageManager.PERMISSION_GRANTED) {
        return;
    }
    String msg = "Permission Denial: " + func + " from pid="
            + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid()
            + " requires " + permission;
    Slog.w(TAG, msg);
    throw new SecurityException(msg);
}

// 检查指定 pid/uid 的权限
void enforcePermission(String permission, int pid, int uid, String func) {
    if (checkPermission(permission, pid, uid) == PackageManager.PERMISSION_GRANTED) {
        return;
    }
    String msg = "Permission Denial: " + func + " from pid=" + pid
            + ", uid=" + uid + " requires " + permission;
    throw new SecurityException(msg);
}

// 需要至少一个权限满足
private void enforceCallingHasAtLeastOnePermission(String func, String... permissions) {
    for (String permission : permissions) {
        if (checkCallingPermission(permission) == PackageManager.PERMISSION_GRANTED) {
            return;
        }
    }
    String msg = "Permission Denial: " + func + " from pid="
            + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid()
            + " requires one of " + Arrays.toString(permissions);
    throw new SecurityException(msg);
}
```

### 4.4 场景三：PermissionManagerServiceImpl 核心检查

**文件**：`PermissionManagerServiceImpl.java` (行 942-1054)

```java
@Override
public int checkPermission(String pkgName, String permName, int userId) {
    // 1. 用户不存在 → 拒绝
    if (!mUserManagerInt.exists(userId)) {
        return PackageManager.PERMISSION_DENIED;
    }
    // 2. 包不存在 → 拒绝
    final AndroidPackage pkg = mPackageManagerInt.getPackage(pkgName);
    if (pkg == null) {
        return PackageManager.PERMISSION_DENIED;
    }
    return checkPermissionInternal(pkg, true, permName, userId);
}

private int checkPermissionInternal(AndroidPackage pkg, boolean isPackageExplicit,
        String permissionName, int userId) {
    final int callingUid = Binder.getCallingUid();

    // 3. 应用可见性过滤 (Android 11+)
    if (isPackageExplicit || pkg.getSharedUserId() == null) {
        if (mPackageManagerInt.filterAppAccess(
                pkg.getPackageName(), callingUid, userId, false)) {
            return PackageManager.PERMISSION_DENIED;
        }
    }

    final int uid = UserHandle.getUid(userId, pkg.getUid());
    final boolean isInstantApp = mPackageManagerInt.getInstantAppPackageName(uid) != null;

    synchronized (mLock) {
        final UidPermissionState uidState = getUidStateLocked(pkg, userId);
        if (uidState == null) {
            return PackageManager.PERMISSION_DENIED;
        }

        // 4. 检查权限是否授予
        if (checkSinglePermissionInternalLocked(uidState, permissionName, isInstantApp)) {
            return PackageManager.PERMISSION_GRANTED;
        }

        // 5. FULLER_PERMISSION_MAP: 检查是否有更全的权限
        // 例如: 持有 ACCESS_FINE_LOCATION 自动拥有 ACCESS_COARSE_LOCATION
        final String fullerPermissionName = FULLER_PERMISSION_MAP.get(permissionName);
        if (fullerPermissionName != null
                && checkSinglePermissionInternalLocked(uidState, fullerPermissionName, isInstantApp)) {
            return PackageManager.PERMISSION_GRANTED;
        }
    }
    return PackageManager.PERMISSION_DENIED;
}

// UID 级权限检查：先查包权限，再查系统级固定权限
private int checkUidPermissionInternal(AndroidPackage pkg, int uid, String permissionName) {
    if (pkg != null) {
        return checkPermissionInternal(pkg, false, permissionName, userId);
    }
    // 没有包信息 → 查询 mSystemPermissions
    synchronized (mLock) {
        if (checkSingleUidPermissionInternalLocked(uid, permissionName)) {
            return PackageManager.PERMISSION_GRANTED;
        }
        final String fullerPermissionName = FULLER_PERMISSION_MAP.get(permissionName);
        if (fullerPermissionName != null
                && checkSingleUidPermissionInternalLocked(uid, fullerPermissionName)) {
            return PackageManager.PERMISSION_GRANTED;
        }
    }
    return PackageManager.PERMISSION_DENIED;
}

// 系统固定权限检查（来自 /etc/permissions/platform.xml）
private boolean checkSingleUidPermissionInternalLocked(int uid, String permissionName) {
    ArraySet<String> permissions = mSystemPermissions.get(uid);
    return permissions != null && permissions.contains(permissionName);
}
```

### 4.5 场景四：getUidStateLocked — 状态查询路径

```java
// 按 appId + userId 查找 UidPermissionState
@GuardedBy("mLock")
private UidPermissionState getUidStateLocked(@AppIdInt int appId, @UserIdInt int userId) {
    final UserPermissionState userState = mState.getUserState(userId);
    if (userState == null) {
        return null;
    }
    return userState.getUidState(appId);
}
```

**数据流**：`mState` (DevicePermissionState) → `getUserState(userId)` → `getUidState(appId)` → `UidPermissionState`

---

## 五、权限授予与撤销流程

### 5.1 运行时权限授予

**文件**：`PermissionManagerServiceImpl.java` (行 1330-1480)

```java
// 公开入口
public void grantRuntimePermission(String packageName, String permName, final int userId) {
    final int callingUid = Binder.getCallingUid();
    final boolean overridePolicy =
            checkUidPermission(callingUid, ADJUST_RUNTIME_PERMISSIONS_POLICY)
                    == PackageManager.PERMISSION_GRANTED;
    grantRuntimePermissionInternal(packageName, permName, overridePolicy,
            callingUid, userId, mDefaultPermissionCallback);
}

// 内部实现
private void grantRuntimePermissionInternal(String packageName, String permName,
        boolean overridePolicy, int callingUid, final int userId,
        PermissionCallback callback) {

    // 步骤1: 调用者必须持有 GRANT_RUNTIME_PERMISSIONS 权限
    mContext.enforceCallingOrSelfPermission(
            android.Manifest.permission.GRANT_RUNTIME_PERMISSIONS,
            "grantRuntimePermission");

    // 步骤2: 跨用户权限检查
    enforceCrossUserPermission(callingUid, userId, true, true, "grantRuntimePermission");

    // 步骤3: 查找目标包
    final AndroidPackage pkg = mPackageManagerInt.getPackage(packageName);
    if (pkg == null) {
        Log.e(TAG, "Unknown package: " + packageName);
        return;
    }

    // 步骤4: 包可见性过滤
    if (mPackageManagerInt.filterAppAccess(packageName, callingUid, userId, false)) {
        throw new IllegalArgumentException("Unknown package: " + packageName);
    }

    // 步骤5: 查找权限定义
    final Permission permission;
    synchronized (mLock) {
        permission = mRegistry.getPermission(permName);
        if (permission == null) {
            throw new IllegalArgumentException("Unknown permission: " + permName);
        }
    }

    // 步骤6: 检查是否被 SYSTEM_FIXED / POLICY_FIXED 锁定
    // 步骤7: 检查硬限制 (hardRestricted)
    // 步骤8: 检查软限制 (softRestricted)

    // 步骤9: 实际授予
    synchronized (mLock) {
        final UidPermissionState uidState = getUidStateLocked(pkg, userId);
        final boolean wasGranted = uidState.isPermissionGranted(permName);
        uidState.grantPermission(bp);  // 调用 UidPermissionState.grantPermission()
        // ...
    }

    // 步骤10: 回调通知 → 可能杀死应用进程
    callback.onPermissionGranted(packageName, userId);
    notifyRuntimePermissionStateChanged(packageName, userId);
}
```

### 5.2 运行时权限撤销

```java
// 公开入口
public void revokeRuntimePermission(String packageName, String permName,
        int userId, String reason) {
    final int callingUid = Binder.getCallingUid();
    final boolean overridePolicy = checkUidPermission(callingUid,
            ADJUST_RUNTIME_PERMISSIONS_POLICY) == PackageManager.PERMISSION_GRANTED;
    revokeRuntimePermissionInternal(packageName, permName, overridePolicy,
            callingUid, userId, reason, mDefaultPermissionCallback);
}

// 内部实现的核心撤销逻辑
private void revokeRuntimePermissionInternal(...) {
    // 步骤1: 调用者必须持有 REVOKE_RUNTIME_PERMISSIONS 权限
    mContext.enforceCallingOrSelfPermission(
            android.Manifest.permission.REVOKE_RUNTIME_PERMISSIONS,
            "revokeRuntimePermission");

    // 步骤2: 跨用户权限检查
    // 步骤3: 查找目标包和权限定义
    // 步骤4: 检查 SYSTEM_FIXED / POLICY_FIXED 标志

    // 步骤5: 实际撤销
    synchronized (mLock) {
        uidState.revokePermission(bp);  // 调用 UidPermissionState.revokePermission()
    }

    // 步骤6: 回调通知 → 杀死应用进程
    callback.onPermissionRevoked(packageName, userId);
}
```

### 5.3 权限变更后杀死应用进程

```java
// PermissionManagerService.java 第199行
public static void killUid(int appId, int userId, String reason) {
    final long identity = Binder.clearCallingIdentity();
    try {
        IActivityManager am = ActivityManager.getService();
        if (am != null) {
            am.killUidForPermissionChange(appId, userId, reason);
        }
    } finally {
        Binder.restoreCallingIdentity(identity);
    }
}
```

### 5.4 安装时权限恢复 (restorePermissionState)

**文件**：`PermissionManagerServiceImpl.java` (行 2563-2639)

这是应用安装/更新时最核心的权限恢复逻辑：

```java
private void restorePermissionState(@NonNull AndroidPackage pkg, boolean replace,
        @Nullable String changingPackageName, @Nullable PermissionCallback callback,
        @UserIdInt int filterUserId) {

    // 重要区分：install 权限 vs runtime 权限
    // - install 权限：安装时授予所有用户和未来用户 (normal, signature)
    // - runtime 权限：运行时显式授予特定用户 (dangerous, targetSdk >= 23)
    // - 特殊处理：targetSdk <= 22 的 app 的 dangerous 权限也作为 install 权限处理

    // 步骤1: 遍历所有请求的权限，分类处理
    final List<String> requestedPermissions = pkg.getRequestedPermissions();
    for (int i = 0; i < requestedPermissions.size(); i++) {
        final String permissionName = requestedPermissions.get(i);
        final Permission permission = mRegistry.getPermission(permissionName);

        // 特权权限白名单检查
        if (permission.isPrivileged() && checkPrivilegedPermissionAllowlist(pkg, ps, permission)) {
            isPrivilegedPermissionAllowlisted.add(permissionName);
        }

        // 签名权限：检查签名匹配
        if (permission.isSignature() && shouldGrantPermissionBySignature(pkg, permission)) {
            shouldGrantSignaturePermission.add(permissionName);
        }

        // 内部权限：检查保护级别匹配
        if (permission.isInternal() && shouldGrantPermissionByProtectionFlags(pkg, ps, permission)) {
            shouldGrantInternalPermission.add(permissionName);
        }
    }

    // 步骤2: 根据分类结果，按用户执行授予/撤销操作
    // ...
}
```

---

## 六、AppOps 双层权限模型

### 6.1 AppOps 的设计目的

**文件**：`frameworks/base/core/java/android/app/AppOpsManager.java`

```java
/**
 * App-ops are used for two purposes: Access control and tracking.
 *
 * Access control:
 * App-ops can either be controlled for each uid or for each package.
 * To control access the app-op can be set to:
 *   MODE_DEFAULT - Default behavior
 *   MODE_ALLOWED - Allow the access
 *   MODE_IGNORED - Don't allow, return placeholder data
 *   MODE_ERRORED - Throw a SecurityException
 *
 * Runtime permissions and app-ops:
 * Each platform defined runtime permission has an associated app op.
 * If the runtime permission is denied → SecurityException
 * If the runtime permission is granted but app-op is MODE_IGNORED → silent failure
 */
```

### 6.2 四种模式详解

| 模式 | 值 | 行为 | 使用场景 |
|------|-----|------|----------|
| `MODE_ALLOWED` | 0 | 正常访问 | 用户同意权限后 |
| `MODE_IGNORED` | 1 | 静默忽略（返回空数据/不执行操作） | 老应用(targetSdk<23)被用户关闭权限时 |
| `MODE_ERRORED` | 2 | 抛出 SecurityException | 强制拒绝访问 |
| `MODE_DEFAULT` | 3 | 由系统根据上下文决定 | 默认状态 |

### 6.3 AppOpsService 核心检查逻辑

**文件**：`frameworks/base/services/core/java/com/android/server/appop/AppOpsService.java` (行 2413-2447)

```java
private @Mode int checkOperationUnchecked(int code, int uid,
        @NonNull String packageName, @Nullable String attributionTag, boolean raw) {

    // 1. 验证包名与 UID 对应关系
    PackageVerificationResult pvr = verifyAndGetBypass(uid, packageName, null);

    // 2. 检查包是否被 suspend（暂停）
    if (isOpRestrictedDueToSuspend(code, packageName, uid)) {
        return AppOpsManager.MODE_IGNORED;
    }

    synchronized (this) {
        // 3. 检查受限操作
        if (isOpRestrictedLocked(uid, code, packageName, attributionTag,
                pvr.bypass, true)) {
            return AppOpsManager.MODE_IGNORED;
        }

        // 4. Op 转换 (switch op)
        code = AppOpsManager.opToSwitch(code);

        // 5. 优先检查 UID 级别模式
        UidState uidState = getUidStateLocked(uid, false);
        if (uidState != null
                && uidState.getUidMode(code) != AppOpsManager.opToDefaultMode(code)) {
            final int rawMode = uidState.getUidMode(code);
            return raw ? rawMode : uidState.evalMode(code, rawMode);
        }

        // 6. 检查 Package 级别模式
        Op op = getOpLocked(code, uid, packageName, null, false, pvr.bypass, false);
        if (op == null) {
            return AppOpsManager.opToDefaultMode(code);
        }
        return raw ? op.getMode() : op.uidState.evalMode(op.op, op.getMode());
    }
}
```

### 6.4 noteOperation — 记录数据访问

```java
private SyncNotedAppOp noteOperationUnchecked(int code, int uid,
        @NonNull String packageName, @Nullable String attributionTag,
        int proxyUid, String proxyPackageName, @Nullable String proxyAttributionTag,
        @OpFlags int flags, boolean shouldCollectAsyncNotedOp,
        @Nullable String message, boolean shouldCollectMessage) {

    // 验证包名和 UID
    PackageVerificationResult pvr = verifyAndGetBypass(uid, packageName, ...);

    synchronized (this) {
        // 获取或创建 Ops 对象
        final Ops ops = getOpsLocked(uid, packageName, attributionTag,
                pvr.isAttributionTagValid, pvr.bypass, true);

        final Op op = getOpLocked(ops, code, uid, true);
        final AttributedOp attributedOp = op.getOrCreateAttribution(op, attributionTag);

        // 获取 UID 状态
        final UidState uidState = ops.uidState;

        // 受限检查
        if (isOpRestrictedLocked(uid, code, packageName, attributionTag, pvr.bypass, false)) {
            attributedOp.rejected(uidState.getState(), flags);
            return new SyncNotedAppOp(AppOpsManager.MODE_IGNORED, ...);
        }

        // UID 模式检查
        final int switchCode = AppOpsManager.opToSwitch(code);
        final int uidMode = uidState.getUidMode(switchCode);
        if (uidMode != AppOpsManager.opToDefaultMode(switchCode)) {
            final int result = uidState.evalMode(switchCode, uidMode);
            if (result != AppOpsManager.MODE_ALLOWED) {
                attributedOp.rejected(uidState.getState(), flags);
                return new SyncNotedAppOp(result, ...);
            }
        }

        // Package 模式检查
        if (switchOp.uidState.evalMode(switchOp.op, switchOp.getMode()) != MODE_ALLOWED) {
            attributedOp.rejected(uidState.getState(), flags);
            return new SyncNotedAppOp(result, ...);
        }

        // 记录访问
        attributedOp.accessed(uidState.getState(), flags);
        return new SyncNotedAppOp(AppOpsManager.MODE_ALLOWED, ...);
    }
}
```

### 6.5 PermissionChecker — API Provider 的标准入口

**文件**：`frameworks/base/core/java/android/content/PermissionChecker.java`

这是给**系统 API Provider**（如 CameraService、LocationManagerService）使用的标准工具类：

```java
// === 三种检查结果 ===
public static final int PERMISSION_GRANTED      = 0;  // Runtime权限已授予 + AppOp允许
public static final int PERMISSION_SOFT_DENIED  = 1;  // Runtime权限已授予 + AppOp被拒绝
public static final int PERMISSION_HARD_DENIED  = 2;  // Runtime权限被拒绝

// === 两种检查模式 ===

// Preflight 模式：注册回调/监听器时使用，不留下数据访问记录
public static int checkPermissionForPreflight(Context context,
        String permission, int pid, int uid, String packageName) {
    return context.getSystemService(PermissionCheckerManager.class)
            .checkPermission(permission, attributionSource.asState(), null,
            false /*forDataDelivery*/, false, false, OP_NONE);
}

// DataDelivery 模式：实际传输隐私数据前使用，记录访问
public static int checkPermissionForDataDelivery(Context context,
        String permission, int pid, int uid, String packageName,
        String attributionTag, String message, boolean startDataDelivery) {
    return context.getSystemService(PermissionCheckerManager.class)
            .checkPermission(permission, attributionSource.asState(), message,
            true /*forDataDelivery*/, startDataDelivery, false, OP_NONE);
}
```

### 6.6 PermissionCheckerManager 快速路径优化

**文件**：`frameworks/base/core/java/android/permission/PermissionCheckerManager.java`

```java
public int checkPermission(String permission,
        AttributionSourceState attributionSource, String message,
        boolean forDataDelivery, boolean startDataDelivery,
        boolean fromDatasource, int attributedOp) {

    // 快速路径：非 Runtime、非 AppOp 权限 → 本地直接检查（不走 Binder IPC）
    if (AppOpsManager.permissionToOpCode(permission) == AppOpsManager.OP_NONE) {
        if (fromDatasource) {
            // 数据源跳过自己，检查 Attribution Chain 中的下一个
            if (attributionSource.next != null && attributionSource.next.length > 0) {
                return mContext.checkPermission(permission,
                        attributionSource.next[0].pid,
                        attributionSource.next[0].uid) == PackageManager.PERMISSION_GRANTED
                        ? PERMISSION_GRANTED : PERMISSION_HARD_DENIED;
            }
        } else {
            return (mContext.checkPermission(permission,
                    attributionSource.pid, attributionSource.uid)
                    == PackageManager.PERMISSION_GRANTED)
                ? PERMISSION_GRANTED : PERMISSION_HARD_DENIED;
        }
    }

    // 慢速路径：需要同时检查 Runtime Permission + AppOps → 走 Binder IPC
    return mService.checkPermission(permission, attributionSource, message,
            forDataDelivery, startDataDelivery, fromDatasource, attributedOp);
}
```

---

## 七、系统权限配置与特殊权限

### 7.1 /etc/permissions/platform.xml

此文件定义系统级 UID 到权限的映射。例如：

```xml
<permissions>
    <!-- system_server (uid=1000) 拥有的权限 -->
    <assign-permission name="android.permission.INTERNET" uid="1000" />
    <assign-permission name="android.permission.CAMERA" uid="1000" />
    <assign-permission name="android.permission.RECORD_AUDIO" uid="1000" />
    ...
</permissions>
```

这些权限通过 `SystemConfig` 加载到 `mSystemPermissions`：

```java
// PermissionManagerServiceImpl 构造函数
SystemConfig systemConfig = SystemConfig.getInstance();
mSystemPermissions = systemConfig.getSystemPermissions();
mGlobalGids = systemConfig.getGlobalGids();
```

### 7.2 保护级别为 signature 的权限示例

来自 `frameworks/base/core/res/AndroidManifest.xml`：

| 权限名 | 保护级别 | 用途 |
|--------|----------|------|
| `android.permission.HARDWARE_TEST` | `signature` | 硬件测试 |
| `android.permission.MANAGE_DYNAMIC_SYSTEM` | `signature` | 动态系统管理 |
| `android.permission.CAMERA_OPEN_CLOSE_LISTENER` | `signature` | 相机开关监听 |
| `android.permission.ACCESS_MOCK_LOCATION` | `signature` | 模拟位置 |
| `android.permission.BIND_CELL_BROADCAST_SERVICE` | `signature` | 小区广播服务 |
| `android.permission.NETWORK_STACK` | `signature` | 网络栈控制 |
| `android.permission.NETWORK_SETTINGS` | `signature` | 网络设置 |
| `android.permission.VIRTUAL_INPUT_DEVICE` | `signature` | 虚拟输入设备 |
| `android.permission.ACCOUNT_MANAGER` | `signature` | 账户管理 |
| `android.permission.VIBRATE_ALWAYS_ON` | `signature` | 持续震动 |

### 7.3 保护级别为 signature|privileged 的权限示例

| 权限名 | 保护级别 | 用途 |
|--------|----------|------|
| `android.permission.WRITE_OBB` | `signature\|privileged` | OBB 文件写入 |
| `android.permission.LOCATION_BYPASS` | `signature\|privileged` | 绕过位置限制 |
| `android.permission.INSTALL_LOCATION_PROVIDER` | `signature\|privileged` | 安装位置提供者 |
| `android.permission.SCORE_NETWORKS` | `signature\|privileged` | 网络评分 |
| `android.permission.BLUETOOTH_PRIVILEGED` | `signature\|privileged` | 蓝牙特权操作 |
| `android.permission.SEND_SMS_NO_CONFIRMATION` | `signature\|privileged` | 无确认发短信 |
| `android.permission.TETHER_PRIVILEGED` | `signature\|privileged` | 热点特权 |
| `android.permission.READ_WIFI_CREDENTIAL` | `signature\|privileged` | 读取 WiFi 凭据 |

---

## 八、权限升级与拆分机制

### 8.1 FULLER_PERMISSION_MAP — 权限替代关系

**文件**：`PermissionManagerServiceImpl.java` (行 229-236)

```java
/** 如果 value 权限已授予，则 key 权限也自动视为已授予 */
private static final Map<String, String> FULLER_PERMISSION_MAP = new HashMap<>();

static {
    // 精细位置 → 粗略位置
    FULLER_PERMISSION_MAP.put(Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION);
    // 跨用户 → 完全跨用户
    FULLER_PERMISSION_MAP.put(Manifest.permission.INTERACT_ACROSS_USERS,
            Manifest.permission.INTERACT_ACROSS_USERS_FULL);
}
```

### 8.2 SplitPermission — 权限拆分

Android 版本升级时常会将一个旧权限拆分为多个更细粒度的新权限。例如 Android 10 将 `READ_EXTERNAL_STORAGE` 拆分为 `READ_MEDIA_AUDIO`、`READ_MEDIA_VIDEO`、`READ_MEDIA_IMAGES`。

```java
// 获取拆分权限信息
private List<PermissionManager.SplitPermissionInfo> getSplitPermissionInfos() {
    return SystemConfig.getInstance().getSplitPermissions();
}

// 判断权限是否从非运行时权限拆分而来
private boolean isPermissionSplitFromNonRuntime(String permName, int targetSdk) {
    final List<PermissionManager.SplitPermissionInfo> splitPerms = getSplitPermissionInfos();
    for (int i = 0; i < size; i++) {
        final PermissionManager.SplitPermissionInfo splitPerm = splitPerms.get(i);
        if (targetSdk < splitPerm.getTargetSdk()
                && splitPerm.getNewPermissions().contains(permName)) {
            // 如果源权限不是运行时权限，则该拆分权限应从非运行时继承
            final Permission perm = mRegistry.getPermission(splitPerm.getSplitPermission());
            return perm != null && !perm.isRuntime();
        }
    }
    return false;
}
```

### 8.3 隐式权限继承

当应用升级到新 SDK 版本时，系统会自动将旧权限的授予状态继承给新拆分的权限：

```java
// 建立新权限到源权限的映射
// 如果任何源权限已授予，则新权限也授予
// 合并所有源权限的标志位

private void inheritPermissionStateToNewImplicitPermissionLocked(
        UidPermissionState uidState, String newPermName,
        List<String> sourcePermNames) {
    // 1. 检查所有源权限的授予状态
    boolean anyGranted = false;
    int mergedFlags = 0;
    for (String sourcePerm : sourcePermNames) {
        if (uidState.isPermissionGranted(sourcePerm)) {
            anyGranted = true;
        }
        mergedFlags |= uidState.getPermissionFlags(sourcePerm);
    }

    // 2. 继承状态到新权限
    if (anyGranted) {
        uidState.grantPermission(newPermission);
    }
    uidState.updatePermissionFlags(newPermission, mergedFlags, mergedFlags);
}
```

---

## 九、实际场景演练

### 场景 9.1：第三方应用请求相机权限的完整流程

```
步骤1: App 在 AndroidManifest.xml 声明
─────────────────────────────────────────
<uses-permission android:name="android.permission.CAMERA" />

步骤2: App 运行时请求权限
─────────────────────────────────────────
ActivityCompat.requestPermissions(this,
    new String[]{Manifest.permission.CAMERA}, REQUEST_CODE);
        ↓
系统弹出权限对话框，用户点击"允许"

步骤3: PermissionController 调用 grantRuntimePermission
─────────────────────────────────────────
PermissionManager.grantRuntimePermission(
    "com.example.app", "android.permission.CAMERA", userId);
        ↓
PermissionManagerServiceImpl.grantRuntimePermissionInternal()
  ├── enforceCallingOrSelfPermission(GRANT_RUNTIME_PERMISSIONS)
  ├── 查找包和权限定义
  ├── uidState.grantPermission(CAMERA)
  │     └── PermissionState.grant()  // 设置 granted=true
  ├── 写入 runtime-permissions.xml
  └── callback.onPermissionGranted() → 通知 App

步骤4: App 打开相机
─────────────────────────────────────────
CameraManager.openCamera()
        ↓
CameraService::connectDevice()
        ↓
PermissionChecker.checkPermissionForDataDelivery(
    context, "android.permission.CAMERA", ...)
        ↓
PermissionCheckerManager.checkPermission()
  ├── CAMERA 有关联 AppOp (OP_CAMERA=26) → 慢速路径
  │     ↓ Binder IPC
  ├── PermissionManagerServiceImpl.checkPermission()
  │     └── uidState.isPermissionGranted("CAMERA") → true ✓
  └── AppOpsService.checkOperation(OP_CAMERA)
        └── uidState.evalMode(OP_CAMERA, MODE_ALLOWED) → MODE_ALLOWED ✓
        ↓
返回 PERMISSION_GRANTED → 相机正常打开
```

### 场景 9.2：老应用(targetSdk<23)的兼容处理

```
步骤1: 老应用(targetSdk=22)在 Android 6.0+ 设备上运行
─────────────────────────────────────────────────────────
用户在设置中关闭了该应用的相机权限

步骤2: 系统处理
─────────────────────────────────────────────────────────
- Runtime Permission: CAMERA → 仍保持 GRANTED (兼容性)
- AppOps OP_CAMERA mode → 设为 MODE_IGNORED

步骤3: 老应用尝试打开相机
─────────────────────────────────────────────────────────
CameraService 调用 PermissionChecker.checkPermissionForDataDelivery()
        ↓
PermissionManagerServiceImpl.checkPermission()
  └── uidState.isPermissionGranted("CAMERA") → true (兼容性保留)
        ↓
AppOpsService.checkOperation(OP_CAMERA)
  └── uidState.evalMode(OP_CAMERA, MODE_IGNORED) → MODE_IGNORED
        ↓
返回 PERMISSION_SOFT_DENIED
        ↓
CameraService 不打开相机，但也不抛异常 → 静默失败
```

### 场景 9.3：系统服务启动时的权限检查

```
步骤1: SystemUI (uid=1000) 启动
─────────────────────────────────────────
ActivityManagerService 检查组件权限：
checkComponentPermission("android.permission.STATUS_BAR", pid, 1000, -1, true)
        ↓
ActivityManager.checkComponentPermission()
  ├── appId=1000 == SYSTEM_UID → GRANTED ✓ (直接放行)

步骤2: 普通 App 尝试绑定系统服务
─────────────────────────────────────────
bindService(intent, conn, BIND_AUTO_CREATE)
        ↓
AMS.checkComponentPermission("android.permission.BIND_WALLPAPER",
    callingPid, callingUid, wallpaperServiceUid, true)
        ↓
ActivityManager.checkComponentPermission()
  ├── appId != ROOT/SYSTEM
  ├── !sameApp(callingUid, wallpaperServiceUid)
  ├── exported=true
  ├── perm="BIND_WALLPAPER" != null
  └── AppGlobals.getPackageManager().checkUidPermission(
          "BIND_WALLPAPER", callingUid)
        ↓
  如果 App 没有在 Manifest 中声明该权限 → PERMISSION_DENIED
  AMS 抛出 SecurityException
```

---

## 十、Framework 开发实践要点

### 10.1 新增危险权限的步骤

1. **在 `AndroidManifest.xml` 中声明**：
```xml
<permission android:name="android.permission.MY_NEW_PERMISSION"
    android:permissionGroup="android.permission-group.MY_GROUP"
    android:protectionLevel="dangerous" />
```

2. **在 `AppOpsManager` 中定义对应的 Op Code**：
```java
public static final int OP_MY_NEW = 120;  // 选择一个未使用的编号
```

3. **在 `AppOpsManager.permissionToOpCode()` 中建立映射**。

4. **在 `PermissionManagerServiceImpl` 中注册到 `FULLER_PERMISSION_MAP`**（如有替代关系）。

5. **在 PermissionController App 中添加权限处理逻辑**（UI 展示）。

### 10.2 API Provider 权限检查最佳实践

```java
// ❌ 错误做法：只用 Context.checkPermission()
if (context.checkPermission(Manifest.permission.CAMERA, pid, uid)
        == PackageManager.PERMISSION_GRANTED) {
    // 这只检查了 Runtime Permission，没检查 AppOps！
    deliverCameraData();
}

// ✅ 正确做法：使用 PermissionChecker
// 注册回调时用 Preflight 模式
int result = PermissionChecker.checkPermissionForPreflight(
        context, Manifest.permission.CAMERA, pid, uid, packageName);
if (result == PermissionChecker.PERMISSION_GRANTED) {
    registerListener();
}

// 实际传输数据时用 DataDelivery 模式
int result = PermissionChecker.checkPermissionForDataDelivery(
        context, Manifest.permission.CAMERA, pid, uid,
        packageName, attributionTag, "camera frame", false);
if (result == PermissionChecker.PERMISSION_GRANTED) {
    deliverCameraFrame();
} else if (result == PermissionChecker.PERMISSION_SOFT_DENIED) {
    // 权限已授予但 AppOp 被忽略 → 返回空数据，不崩溃
    deliverEmptyFrame();
} else {
    // PERMISSION_HARD_DENIED → 权限未授予，抛 SecurityException
    throw new SecurityException("Camera permission denied");
}
```

### 10.3 系统服务内部权限检查模式

```java
// 模式1: check + enforce（推荐用于 Binder 接口）
public void someSystemApi() {
    enforceCallingPermission(Manifest.permission.SOME_PERMISSION,
            "someSystemApi");
    // ... 执行业务逻辑
}

// 模式2: check + 自定义处理
public boolean someQuery() {
    if (checkCallingPermission(Manifest.permission.SOME_PERMISSION)
            != PackageManager.PERMISSION_GRANTED) {
        return getLimitedResult();  // 返回降级结果
    }
    return getFullResult();
}

// 模式3: 多权限检查
public void complexApi() {
    enforceCallingHasAtLeastOnePermission("complexApi",
            Manifest.permission.PERM_A,
            Manifest.permission.PERM_B);
    // ...
}
```

### 10.4 Binder 调用身份注意事项

```java
// ✅ 正确：使用 Binder.getCallingUid() 获取调用者身份
int callingUid = Binder.getCallingUid();

// ❌ 错误：使用 Process.myUid() 获取的是当前进程 UID
int myUid = Process.myUid();  // 在 system_server 中永远是 1000

// 如果需要以系统身份执行操作
long identity = Binder.clearCallingIdentity();
try {
    // 以系统身份执行操作
} finally {
    Binder.restoreCallingIdentity(identity);
}
```

### 10.5 关键文件索引

| 层次 | 文件路径 | 核心职责 |
|------|----------|----------|
| **权限定义** | `frameworks/base/core/res/AndroidManifest.xml` | 所有平台权限声明 |
| **数据模型** | `.../pm/permission/UidPermissionState.java` | UID 级权限状态 |
| **数据模型** | `.../pm/permission/DevicePermissionState.java` | 设备级权限状态 |
| **数据模型** | `.../pm/permission/PermissionRegistry.java` | 权限注册表 |
| **服务入口** | `.../pm/permission/PermissionManagerService.java` | Binder 服务入口 |
| **核心实现** | `.../pm/permission/PermissionManagerServiceImpl.java` | 权限管理核心 (249KB) |
| **API层** | `core/java/android/content/PermissionChecker.java` | 双层联合检查 API |
| **API层** | `core/java/android/permission/PermissionCheckerManager.java` | 带快速路径的权限检查 |
| **API层** | `core/java/android/app/AppOpsManager.java` | AppOps 操作控制 |
| **AppOps服务** | `.../appop/AppOpsService.java` | AppOps 核心服务 |
| **组件检查** | `core/java/android/app/ActivityManager.java` | checkComponentPermission 底层 |
| **AMS检查** | `.../am/ActivityManagerService.java` | enforceCallingPermission 等 |
| **系统配置** | `.../pm/SystemConfig.java` | 系统权限配置加载 |
| **系统配置** | `/etc/permissions/platform.xml` | 系统 UID 权限映射 |

---

> **总结**：Android Framework 的权限管理是一个精心设计的多层系统，通过 **PermissionManager + AppOpsManager** 双层结构实现了灵活的权限控制。理解这套源码架构，对于开发系统级功能、调试权限问题、以及理解 Android 安全模型至关重要。
