# Android Service 与系统级核心通信架构全解 

> **导读**：本文档旨在为 Android 系统开发（尤其是车载 IVI/CarOS 领域）提供全景式、深度的 Service 原理与实战指南。涵盖从应用层级 Service 的调度机制、系统级 SystemServer 架构、Native 守护进程，到车载专属的 `CarService` 架构及 VHAL 交互。
> 掌握本文档内容，可达到 **5+ 年 Android 系统框架/车载系统开发工程师** 对 Service 机制的认知水平。

---

## 核心基石：Android 体系中的三种 "Service" 概念辨析

在系统开发语境下，“Service” 通常有三层含义，初学者极易混淆：
1. **App Service (四大组件之一)**：继承自 `android.app.Service`，运行在应用进程（或指定的 app remote 进程），由 AMS 统一调度（如前文的 `MyService`）。
2. **System Service (Java 框架服务)**：运行在 `SystemServer` 进程中的核心系统服务，如 `ActivityManagerService` (AMS)、`WindowManagerService` (WMS)、`CarServiceHelperService`。继承自 `com.android.server.SystemService`。
3. **Native Service (C/C++ 守护进程)**：运行在独立的 Native 进程中，通常由 `init` 进程解析 `.rc` 脚本拉起，如 `SurfaceFlinger`、`audioserver`、`cameraserver`，以及 HAL 层的各类硬件抽象服务。

---

## 一、 App Service 底层调度与生命周期机制 (AOSP 14)

系统层对 App Service 的调度由 `ActivityManagerService` (AMS) 和 `ActiveServices` 类主导。

### 1.1 核心数据结构与状态机
- **`ServiceRecord`**：AMS 中 Service 的“户口本”，记录了基本信息（包名、类名、运行状态、绑定的客户端列表、Crash 统计等）。
- **`ProcessRecord`**：记录运行该 Service 的进程状态。
- **`ConnectionRecord`**：记录一次 `bindService` 绑定关系。包含客户端的 `IServiceConnection` 回调对象。
- **`AppBindRecord`**：记录某个具体客户端 App 与某个 Service 之间的绑定上下文。

### 1.2 startService / bindService 核心链路
- **`startService`**：单向命令下发。触发目标进程的 `onCreate()` -> `onStartCommand()`。
- **`bindService` (三次握手)**：
  1. 客户端发起 `bindService`，将 `ServiceConnection` 封装为跨进程的 Binder Stub 传给 AMS。
  2. AMS 拉起目标进程（若不存在），触发目标进程 `onCreate()` -> `onBind()`，返回开发者定义的 `IBinder`。
  3. 目标进程通过 `publishService()` 将 `IBinder` 发送回 AMS。AMS 遍历 `ConnectionRecord`，回调客户端的 `onServiceConnected`。

### 1.3 AOSP 14 的前台与后台严苛限制 (System Dev 必看)
- **FGS (前台服务) 强制类型声明**：Android 14 强制要求 `startForeground()` 必须声明 `foregroundServiceType`（如 `location`, `mediaPlayback`, `connectedDevice`）。系统层会强校验应用是否拥有对应的权限，否则直接抛出 `SecurityException` 导致崩溃。
- **后台启动限制 (BAL/BSL)**：禁止后台应用启动 Service。在系统层，AMS 会通过 OOM Adj (Out of Memory Adjustment) 机制动态计算优先级，如果应用处于后台且试图唤醒另一个后台应用，可能直接被系统拦截。
- **短效前台服务 (Short-FGS)**：新增 `shortService` 类型，允许后台应用最多运行 3 分钟前台服务。AMS 内部启动了一个 `Handler` 定时器，超时后直接 `ProcessRecord.kill()` 杀掉进程。

### 1.4 Service 的 ANR 机制
Service 的 ANR 监控实现在 `ActiveServices` 的 Handler 中：
- **前台 Service 超时**：20 秒 (AOSP 默认宏 `SERVICE_TIMEOUT`)。
- **后台 Service 超时**：200 秒 (`SERVICE_BACKGROUND_TIMEOUT`)。
- AMS 在调度 `scheduleCreateService` 时发送延迟消息，如果目标进程在规定时间内没有回复 `serviceDoneExecuting`，AMS 将触发 ANR 收集流程（Dump 堆栈、抓取 trace）。

---

## 二、 进程优先级与 OOM_ADJ 联动 (系统性能调优核心)

在车载等内存紧张的场景，管理进程存活是系统开发者的核心日常。Service 是影响进程优先级 (`OOM_ADJ` 分数) 的关键因素。

### 2.1 绑定标志位 (Bind Flags) 的魔法
调用 `bindService` 时传入的 `flags` 直接决定目标进程的生死存活：
- **`BIND_AUTO_CREATE`**：最常用，目标服务不存在则创建。
- **`BIND_ABOVE_CLIENT`**：**核心参数**。如果客户端是前台进程，目标 Service 进程的优先级会被提升到**比客户端还要高**。常用于 SystemUI 绑定第三方关键服务。
- **`BIND_IMPORTANT`**：目标进程会被提升到“前台服务”级别（Perceptible），即使客户端退到后台，Service 进程也不易被杀。
- **`BIND_WAIVE_PRIORITY`**：客户端不希望因为绑定关系而提升目标进程的优先级。常用于后台日志收集、非核心数据同步服务。

### 2.2 LMKD (Low Memory Killer Daemon) 联动
当内存不足时，内核会通过 cgroup 内存压力事件通知用户态的 `lmkd`。`lmkd` 根据 AMS 计算出的 `oom_score_adj` 杀进程。一个挂载了重要 Service（例如被前台 Activity 绑定）的进程，其 `oom_score_adj` 会被显著降低（越低越不容易被杀，系统进程通常为负数或0，前台 App 为 0，缓存进程为 900+）。

---

## 三、 Framework 系统服务开发 (SystemServer 架构)

车载系统开发中，经常需要添加自定义的系统服务（如 `CarCabinService` 控制空调，`CarLightingService` 控制氛围灯）。

### 3.1 SystemServer 启动流程与 BootPhase
系统启动时，Zygote fork 出的第一个 Java 进程就是 `SystemServer`。
SystemServer 中运行着 `SystemServiceManager` (SSM)，它负责按阶段 (BootPhase) 启动所有的系统服务：
1. **`PHASE_WAIT_FOR_DEFAULT_DISPLAY`**：等待显示设备。
2. **`PHASE_LOCK_SETTINGS_READY`**：设置数据准备完毕。
3. **`PHASE_SYSTEM_SERVICES_READY`**：核心服务就绪。
4. **`PHASE_BOOT_COMPLETED`**：系统启动完成，此时才会发出 `ACTION_BOOT_COMPLETED` 广播。

### 3.2 自定义系统服务规范
继承 `com.android.server.SystemService`：
```java
public class MyCustomSystemService extends SystemService {
    public MyCustomSystemService(Context context) {
        super(context);
    }

    @Override
    public void onStart() {
        // 1. 实例化真正的 Binder 服务实现
        MyCustomBinderService service = new MyCustomBinderService();
        // 2. 注册到 ServiceManager
        publishBinderService("my_custom_service", service);
    }

    @Override
    public void onBootPhase(int phase) {
        if (phase == PHASE_SYSTEM_SERVICES_READY) {
            // 在此阶段可以安全地获取其他系统服务 (如 AMS, WMS)
        }
    }
}
```

### 3.3 ServiceManager 的角色
`ServiceManager` 是 Android Binder 通信的“DNS 服务器”（Context Manager，Handle 为 0）。所有系统服务必须通过 `ServiceManager.addService()` 注册，客户端通过 `ServiceManager.getService("name")` 获取 Binder 代理。

---

## 四、 Native Service 与 HAL 服务 (底层开发)

### 4.1 init.rc 与守护进程
Native Service 是用 C/C++ 编写的守护进程，由 `init` 进程解析 `.rc` 文件启动。
例如在车载中经常会有一个 `car_watchdogd`：
```rc
service car_watchdogd /system/bin/car_watchdogd
    class core
    user system
    group system readproc
    # 宕机会自动重启
    restart_period 10
```

### 4.2 HIDL 与 AIDL 的演进 (Treble 架构)
- Android 8.0 引入 Treble，将 Framework 与 Vendor (硬件产商) 隔离。HAL 层服务使用 **HIDL** 定义，运行在独立进程中，通过 `hwservicemanager` 注册。
- Android 11+ 开始，Google 强推 **Stable AIDL** 替代 HIDL。目前主流车载平台 (如高通 8155/8295 上的 Android 12/14) 的 HAL 层通信已全面转向 C++ 版本的 AIDL，通过 `servicemanager` 统一管理。

---

## 五、 车载系统 (CarOS/IVI) 专项 Service 架构

车载系统最大的特点是引入了 `CarService` (运行在 `com.android.car` 进程中) 以及 VHAL (Vehicle HAL)。

### 5.1 CarService 的启动链路
1. `SystemServer` 中启动了 `CarServiceHelperService` (系统服务)。
2. `CarServiceHelperService` 会去 `bindService` 拉起 `com.android.car` 进程中的 `CarService`。
3. `CarService` 初始化时，会启动数十个子服务（如 `CarAudioService`, `CarAppFocusService`, `CarPropertyService`）。

### 5.2 核心枢纽：CarPropertyService 与 VHAL
车载中最核心的 Service 是 **`CarPropertyService`**。

packages/services/Car/service/src/com/android/car/CarPropertyService.java

packages/services/Car/car-lib/src/android/car/hardware/property/CarPropertyManager.java

- **职责**：管理车内所有硬件属性（空调温度、车门状态、车速、挡位等）。
- **通信路径**：App (如 HVAC 空调应用) -> `CarPropertyManager` -> Binder IPC -> `CarService` (`CarPropertyService`) -> Binder IPC/hwbinder -> **Vehicle HAL (VHAL)** -> CAN/LIN 模块 -> 真实硬件。
- 开发者必须熟练掌握 `hardware/interfaces/automotive/vehicle/` 目录下的 VHAL 属性定义（如 `VehicleProperty.HVAC_TEMPERATURE_SET`）。

### 5.3 多用户架构 (MUMD / MUDS) 对 Service 的挑战
车载系统通常是多用户系统（主驾 User 10，副驾 User 11，后台系统 User 0）。
- `SystemServer` 和 `CarService` 始终运行在 **User 0** (System User)。
- 仪表盘 (Cluster)、桌面 (Launcher)、空调 (HVAC) 等应用通常运行在当前用户（如 User 10）。
- **跨用户绑定 (Cross-User Bind)**：当 App 绑定系统级 App Service 时，必须使用 `Context.bindServiceAsUser()`，否则默认只在当前用户空间内寻找 Service，导致 `ServiceNotFoundException`。需要在 AndroidManifest 中声明 `android:singleUser="true"` 并申请 `INTERACT_ACROSS_USERS` 权限。

---

## 六、 Binder IPC 与进阶高级特性 (Service 的灵魂)

作为 5+ 年资深开发，必须深入理解 Service 背后的 Binder 机制。

### 6.1 AIDL 高级语法与内存拷贝
- **`oneway` 关键字**：异步调用。客户端发起调用后不等待服务端返回，直接执行下一行代码。极大地防止了 SystemServer 被阻塞（避免 Watchdog 触发重启）。
- **`in`、`out`、`inout` 定向标签**：
  - `in`：数据只能由客户端传向服务端（底层发生一次拷贝）。
  - `out`：服务端收到的是一个空对象，服务端填充数据后传回客户端。
  - `inout`：双向传递，性能开销最大。合理使用定向标签可以显著减少 Binder 缓冲区的内存拷贝。

### 6.2 死亡监听 (DeathRecipient) 保证健壮性
当客户端绑定了一个 Service（特别是 SystemServer 绑定 App Service 时），如果 App 崩溃，系统必须能够感知并清理资源，否则会造成内存泄漏或状态死锁。
```java
// 客户端/系统侧监听目标 Binder 的死亡
IBinder targetBinder = ...;
targetBinder.linkToDeath(new IBinder.DeathRecipient() {
    @Override
    public void binderDied() {
        Log.e(TAG, "目标 Service 进程已死亡，执行资源回收或重新拉起策略！");
        targetBinder.unlinkToDeath(this, 0);
    }
}, 0);
```

### 6.3 Binder 线程池耗尽陷阱
- 每个进程的 Binder 线程池默认大小为 **15** (`ProcessState::makeBinderThreadName`)。
- 如果某个 Service 的 AIDL 方法执行耗时操作（如 IO、锁等待），并发调用一旦超过 15 个，后续的 Binder 请求会被放入驱动的等待队列。
- 此时如果客户端也是同步调用，将导致系统级的大面积卡顿甚至 ANR。
- **解决策略**：服务端耗时操作抛到子线程池执行；客户端使用 `oneway` 或者异步回调机制。

---

## 结语

从应用层的 `bindService`，到 `OOM_ADJ` 的进程保活机制；从 `SystemServer` 的启动树，到车载专属的 `CarPropertyService` 乃至最底层的 Binder 线程池管理。掌握这些内容，即打通了 Android 系统通信的任督二脉，能够从容应对复杂的车载跨进程架构设计与系统级性能/稳定性攻坚。