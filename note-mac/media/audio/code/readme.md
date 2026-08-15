# aosp14 Audio源码说明

在 AOSP 14 中，音频系统的源码结构清晰，从上层 Java 框架到下层驱动，主要分布在以下目录：

## 1. Java Framework 层 (应用 API)
路径: frameworks/base/media/java/android/media/

这是应用开发者直接接触的音频 API 层。它运行在调用 API 的 App 进程内。

核心类：AudioTrack.java (播放), AudioRecord.java (录制), AudioManager.java (管理), AudioAttributes.java, AudioFormat.java等。

Framework 服务：AudioService 运行在 SystemServer 进程，是 Java 层的音频管理入口。其源码位于 frameworks/base/services/core/java/com/android/server/audio/AudioService.java。

## 2. JNI 桥接层
路径:

frameworks/base/core/jni/

frameworks/base/media/jni/

JNI 代码将 Java 层的 android.media API 调用连接到 Native 层。

核心文件：android_media_AudioTrack.cpp, android_media_AudioRecord.cpp, android_media_AudioSystem.cpp等。

## 3. Native 框架层 (C++ 客户端)
路径: frameworks/av/media/libmedia/ 和 frameworks/av/media/libaudioclient/

这是 Native 层的客户端库，提供了 android.media 的 C++ 等价实现，负责通过 Binder IPC 与音频服务通信。

核心文件：AudioTrack.cpp, AudioRecord.cpp, AudioSystem.cpp。

Binder 代理：以字母 "I" 开头的文件，如 IAudioTrack.cpp、IAudioFlinger.cpp，位于 frameworks/av/media/libmedia/。

## 4. 音频服务层 (Media Server)
路径: frameworks/av/services/audioflinger/ 和 frameworks/av/services/audiopolicy/

这是音频系统的核心服务层，运行在独立的 audioserver 进程中。

AudioFlinger (audioflinger/)：音频系统的 “心脏”，负责音频数据的管理、混音和路由。

主入口：AudioFlinger.cpp。

核心子模块：Threads.cpp (播放/录制线程), Tracks.cpp (音频轨道), AudioMixer.cpp (混音器)。

AudioPolicyService (audiopolicy/)：音频系统的 “大脑”，负责音频策略和路由决策。

主入口：AudioPolicyService.cpp。

配置文件：策略配置文件 (audio_policy_configuration.xml) 通常位于 device/ 目录下。AAOS 的可配置音频策略引擎示例位于 frameworks/av/services/audiopolicy/engineconfigurable/config/example/。

服务入口：audioserver 的主入口在 frameworks/av/media/audioserver/main_audioserver.cpp。

## 5. Audio HAL 层
路径: hardware/interfaces/audio/

HAL 层定义了音频服务与底层硬件的标准接口。

AIDL 定义 (Android 14+)：音频 HAL 接口使用 AIDL 定义。

核心 HAL API 位于 hardware/interfaces/audio/aidl/android/hardware/audio/core/。

通用数据结构位于 hardware/interfaces/audio/aidl/android/hardware/audio/common/。

HAL 实现：AOSP 提供了一个参考实现 (/hardware/interfaces/audio/aidl/default/)。设备厂商的具体实现通常在 device/ 或 vendor/ 目录下。

## 6. Linux 内核驱动层
路径: kernel/ 和 external/tinyalsa/

这是最底层，与物理音频硬件交互。

内核驱动：通常使用 ALSA 或 OSS 架构。

用户态库：推荐使用 external/tinyalsa/ 作为 ALSA 的用户态库。

## 完整调用路径示例 (AudioTrack)
以 AudioTrack 为例，一个音频请求的完整路径如下：
```
应用层 (AudioTrack.java)
⬇ JNI
JNI 桥接层 (android_media_AudioTrack.cpp)
⬇
Native 框架层 (AudioTrack.cpp 客户端)
⬇ Binder IPC
音频服务层 (AudioFlinger 服务端)
⬇ HAL 接口调用
Audio HAL 层 (hardware/interfaces/audio/)
⬇ 内核调用
Linux 内核驱动层 (ALSA/tinyalsa)
```