模块层级	核心代码路径 (基于AOSP根目录)	主要职责
应用层	packages/apps/Camera2/	AOSP自带的系统相机应用，提供用户界面和交互逻辑。
框架层 (Java API)	frameworks/base/core/java/android/hardware/camera2/	应用开发者使用的CameraManager等Java API。
框架层 (Native)	frameworks/av/camera/	相机框架的Native层核心，提供libcamera_client等库，是Java API与底层服务的桥梁。
框架层 (服务)	frameworks/av/services/camera/libcameraservice/	核心系统服务CameraService，管理所有相机请求、权限和硬件连接。
服务进程	frameworks/av/camera/cameraserver/	cameraserver进程的启动入口(main_cameraserver.cpp)，一个独立的Native进程。
HAL层 (接口定义)	hardware/interfaces/camera/	定义相机HAL的标准HIDL接口，如ICameraProvider。
HAL层 (厂商实现)	vendor/[芯片厂商]/ (例如 vendor/qcom/)	芯片厂商（如高通、MTK）对HAL接口的具体实现，包含核心算法。
内核驱动层	kernel/ 或厂商驱动目录	摄像头传感器、执行器等最底层的驱动程序，通常基于V4L2框架。
设备配置层	device/[厂商]/[设备型号]/	特定设备的配置文件（如device.mk），决定编译时包含哪些HAL模块和特性