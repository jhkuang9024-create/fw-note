# Amlogic S905L3a（P291/tb611）音频系统架构与调试

> **培训文档** · 适用对象：音频模块新入职开发、系统集成工程师、FAE
> **配套代码树**：`tb_p-amlogic-20240429`（Android 9 / P，Amlogic 4.9 内核）
> **版本**：v1.0（2026-09-01）

---

## 目录

- [第 1 章 平台概览](#第-1-章-平台概览)
- [第 2 章 整体架构](#第-2-章-整体架构)
- [第 3 章 各层级关键源码](#第-3-章-各层级关键源码)
  - [3.1 媒体播放层](#31-媒体播放层)
  - [3.2 Java 框架层](#32-java-框架层)
  - [3.3 JNI 层](#33-jni-层)
  - [3.4 Native 客户端（libaudioclient）](#34-native-客户端libaudioclient)
  - [3.5 AudioFlinger（音频服务端）](#35-audioflinger音频服务端)
  - [3.6 AudioPolicy（音频策略层）](#36-audiopolicy音频策略层)
  - [3.7 HAL 层（audio.primary.amlogic）](#37-hal-层audioprimaryamlogic)
  - [3.8 内核层（common/）](#38-内核层common)
- [第 4 章 四大核心流程全链路](#第-4-章-四大核心流程全链路)
- [第 5 章 本平台实际修改（定制点）](#第-5-章-本平台实际修改定制点)
- [第 6 章 调试手段](#第-6-章-调试手段)
- [第 7 章 常见问题与排查思路](#第-7-章-常见问题与排查思路)
- [第 8 章 术语表](#第-8-章-术语表)
- [第 9 章 附录：关键文件索引](#第-9-章-附录关键文件索引)

---

## 第 1 章 平台概览

**本章目标**：建立平台基本信息，知道"这是什么机器、跑什么系统、音频是谁做的"。

| 项 | 内容 |
|---|---|
| 芯片 | Amlogic S905L3a（内核代号 **gxlx2**） |
| 板卡 | P291 / tb611（`tb611_s905L3ap_p291.sh`，`AML_BOARD=p291_iptv`） |
| 产品 | `p291_iptv`（中国移动 IPTV/OTT 机顶盒），`vendor/p291_tb611` 是运营商定制 App 层 |
| 系统 | Android 9 (P)，`USE_XML_AUDIO_POLICY_CONF=1`，默认策略引擎（enginedefault） |
| 内核 | **`common/` 目录即内核源码**（Linux 4.9.113 Amlogic 分支）；实际烧录为预编译 `Image.gz`，defconfig = `meson64_iptv_p291_defconfig` |
| 音频 HAL | `audio.primary.amlogic`（legacy HAL 3.0，`AUDIO_DEVICE_API_VERSION_3_0`），源码在 `hardware/amlogic/audio/audio_hal/` |
| 杜比方案 | Dolby MS12（`libms12api` prebuilt + 内核 `dolby_fw` 固件加载） |
| 声卡 | ALSA `AML-MESONAUDIO`（ASoC MESON 框架 + T9015 内置 codec），板级 DTS = `arch/arm64/boot/dts/amlogic/gxlx2_p291.dts` |
| 播放器 | 标准 NuPlayer（普通 App）+ **AmlogicPlayer/amplayer**（DTV/IPTV/本地媒体主路径） |

> 树根 `audio_output_0102.log` 是真机 logcat 样本：可见 `amplayer` 播放、HAL 的
> `adev_create_audio_patch`、`aml_audio_output_routing: switch from [0x2]HDMI to [0x0]SPEAKER`，
> 是本章及第 6 章最好的实际参照物。

---

## 第 2 章 整体架构

**本章目标**：画得出分层图，说得出每层干什么、和上下层怎么通信。

```
┌────────────────────────────────────────────────────────────────────┐
│ 应用层（Java）                                                      │
│   Launcher / OVD / CmccPlayer / DTV …                              │
│   android.media: AudioManager / AudioTrack / AudioRecord /         │
│                   AudioSystem（静态 native 方法）                   │
├────────────────────────────────────────────────────────────────────┤
│ JNI 层  frameworks/base/core/jni/                                  │
│   android_media_AudioTrack.cpp / AudioRecord.cpp / AudioSystem.cpp │
├────────────────────────────────────────────────────────────────────┤
│ Native 客户端  libaudioclient                                      │
│   AudioTrack / AudioRecord / AudioSystem（进程内单例取服务代理）      │
│   IAudioFlinger / IAudioTrack / IAudioPolicyService（binder 代理）   │
├────────────────────── Binder：media.audio_flinger ─────────────────┤
│                      Binder：media.audio_policy                     │
│ audioserver 进程（frameworks/av/media/audioserver）                 │
│   ├─ AudioFlinger（libaudioflinger）                                │
│   │    混音/路由/效果核心：MixerThread、DirectOutputThread、        │
│   │    OffloadThread、FastMixer、RecordThread、PatchPanel、Effects  │
│   └─ AudioPolicyService（libaudiopolicyservice）                    │
│        决策核心：AudioPolicyManager + Engine（enginedefault）       │
│        + AudioPolicyEffects（效果策略）                             │
├────────────────────── libaudiohal（legacy HAL 封装 + HIDL 4.0）─────┤
│ audio.primary.amlogic（hardware/amlogic/audio/audio_hal/）         │
│   audio_hw.c 主入口 / 透传、offload / MS12 / A2DP / avsync / 混音    │
├────────────────────── tinyalsa（pcm_open / pcm_write）──────────────┤
│ 内核（common/）                                                     │
│   ALSA MESON 框架（i2s/spdif/pcm DAI + T9015 codec）               │
│   amaudio / amaudio2（/dev/amaudio、/sys/class/amaudio）            │
│   audiodsp / hifi4dsp（DSP 解码）／ dolby_fw（MS12 固件）           │
│   tsync（音视频同步）/ hdmitx（HDMI 音频嵌入）                       │
└────────────────────────────────────────────────────────────────────┘
 旁路（媒体硬件解码路径，盒子视频播放的主路径）：
   AmlogicPlayer（Java/C++）→ amplayer（libamplayer）
     → libamcodec（vdec 硬解/ES 流转发）→ amadec（软解）
     → 输出：android-out（内部建 AudioTrack）或 alsa-out（SPDIF 直通）
   音视频同步：/sys/class/tsync/* + HAL 内 aml_avsync_tuning
```

**两条主播放通路（务必分清）**：

| 通路 | 谁在用 | 音频路径 | 特点 |
|---|---|---|---|
| 标准通路 | 普通 App（音乐、系统音效） | App → AudioTrack → AudioFlinger 混音 → HAL PCM → ALSA | 系统统一混音、策略管理 |
| amplayer 通路 | DTV/IPTV/本地视频（amplayer） | amplayer → amcodec/amadec 解码 → 内部 AudioTrack 或 ALSA 直通 | 解码器直达，AV 同步由 tsync 控制 |

**audioserver 进程注册的两个服务**（[main_audioserver.cpp](frameworks/av/media/audioserver/main_audioserver.cpp)）：
- `media.audio_flinger` → AudioFlinger（服务名见 [AudioFlinger.h:110](frameworks/av/services/audioflinger/AudioFlinger.h#L110)）
- `media.audio_policy` → AudioPolicyService（服务名见 [AudioPolicyService.h:54](frameworks/av/services/audiopolicy/service/AudioPolicyService.h#L54)）

客户端获取方式（[AudioSystem.cpp:48](frameworks/av/media/libaudioclient/AudioSystem.cpp#L48) `get_audio_flinger()`、[:746](frameworks/av/media/libaudioclient/AudioSystem.cpp#L746) `get_audio_policy_service()`）：`getService` → `interface_cast` → 注册死亡通知，之后全局缓存单例。

**Java ↔ native 的桥梁**：`AudioService`（system_server）通过 `AudioSystem.java` 的静态 native 方法 → JNI → libaudioclient `AudioSystem::` → binder 直达 audioserver。

---

## 第 3 章 各层级关键源码

**本章目标**：每个层级知道"关键文件在哪、核心函数是什么、看代码时先看哪"。

### 3.1 媒体播放层

> 盒子主路径，理解它才能看懂"为什么 DTV 声音不走标准路径"。

| 组件 | 位置 | 说明 |
|---|---|---|
| AmlogicPlayer | [vendor/amlogic/common/frameworks/av/MediaFactory/AmlogicPlayer.cpp](vendor/amlogic/common/frameworks/av/MediaFactory/AmlogicPlayer.cpp) | amplayer 的 Java/C++ 封装；`player_start`（:2879）、`player_start_play`（:2949）；读 `/sys/class/audiodsp/codec_mips` 查 DSP 状态（:803） |
| amplayer | [vendor/amlogic/common/LibPlayer/amplayer/](vendor/amlogic/common/LibPlayer/amplayer/) | 播放内核；`player_audio.c` `stream_audio_init` 中 `codec_alloc/codec_init` 把音频 ES 交给 amcodec；写 `/sys/class/amaudio/output_enable`（player_set_sys.c:168） |
| amcodec | [hardware/amlogic/media/amcodec/](hardware/amlogic/media/amcodec/) | 音视频解码用户态库；`codec_get_apts/vpts`（include/codec.h）、`get_audio_decoder()==AUDIO_ARM_DECODER` 决定软/硬解（codec_ctrl.c:1210） |
| amadec | [hardware/amlogic/LibAudio/amadec/](hardware/amlogic/LibAudio/amadec/) | ARM 软解音频库；输出抽象 `audio_out_operations_t`（audio-out.h）两种实现：**android-out.cpp**（内部建 `sp<AudioTrack>` 走标准通路，`android_start` :1197、`get_output_func` :1661）、**alsa-out.c**（SPDIF/IEC61937 直通，:1085） |
| 软解插件 | [hardware/amlogic/LibAudio/audio_codec/](hardware/amlogic/LibAudio/audio_codec/) | libfaad(AAC)/libmad(MPEG)/libape/libflac/libamr/libpcm/libcook/libraac…，由 amadec 按格式 dlopen（adec-ffmpeg-mgt.c:68-81） |
| 标准播放 | [NuPlayerRenderer.cpp:1854](frameworks/av/media/libmediaplayerservice/nuplayer/NuPlayerRenderer.cpp#L1854) | `onOpenAudioSink` 先试 offload（COMPRESS_OFFLOAD，:1902）失败回退 PCM；AV 同步锚点 `MediaClock::updateAnchor`（:904-906） |
| AudioOutput | [MediaPlayerService.cpp:955](frameworks/av/media/libmediaplayerservice/MediaPlayerService.cpp#L955) | NuPlayer 的 AudioSink 实现，内部创建 AudioTrack |

**AV 同步两条路线**：
1. **标准路线**：`AudioSink::getTimestamp/getPlayedOutDurationUs` → `MediaClock::updateAnchor` + `setPlaybackRate`
2. **Amlogic tunnel 路线**：HAL 内 [audio_hwsync.c](hardware/amlogic/audio/audio_hal/audio_hwsync.c) 写 `/sys/class/tsync`（`TSYNC_EVENT`、`TSYNC_PCRSCR`）下发音频暂停/恢复/PTS；[aml_avsync_tuning.c:318](hardware/amlogic/audio/audio_hal/aml_avsync_tuning.c) `aml_dev_try_avsync` 采样音视频路径延迟差，用 `avsync_adelay/avsync_drop` 微调

### 3.2 Java 框架层

| 文件 | 要点 |
|---|---|
| [AudioManager.java](frameworks/base/media/java/android/media/AudioManager.java) | App 侧 API；`getService()` 取 IAudioService（:713） |
| [AudioService.java](frameworks/base/services/core/java/com/android/server/audio/AudioService.java) | 音量/焦点/路由策略的 Java 大脑（8299 行） |
| [MediaFocusControl.java](frameworks/base/services/core/java/com/android/server/audio/MediaFocusControl.java) | 音频焦点栈 `mFocusStack`、自动 ducking |
| [AudioSystem.java](frameworks/base/media/java/android/media/AudioSystem.java) | 静态 native 方法，唯一桥 |
| [AudioTrack.java](frameworks/base/media/java/android/media/AudioTrack.java) / AudioRecord.java | App 播放/录音 API |

AudioService 关键成员：
- `mIsSingleVolume`（:212）：true 时用 **TV 音量别名表** `STREAM_VOLUME_ALIAS_TELEVISION`（:363，所有流基本都别名到 MUSIC）——本盒子走此分支
- `VolumeStreamState`（:4862）：每流×每设备的音量状态；`readSettings` 从 Settings 读（:4990），`applyDeviceVolume_syncVSS` → `AudioSystem.setStreamVolumeIndex`（:5049-5065）
- `mFixedVolumeDevices`（:590）：**HDMI_ARC / SPDIF / AUX_LINE 等固定音量设备**（TV 功放管音量）
- CEC 音量键 `sendCecVolumeKey`（:2056）

### 3.3 JNI 层

位置：**`frameworks/base/core/jni/`**（注意：不是 media/jni）：
- `android_media_AudioSystem.cpp`（如 `setStreamVolumeIndex` :516、`setDeviceConnectionState` :454、`setForceUse` :495）
- `android_media_AudioTrack.cpp`（`start` :472）、`android_media_AudioRecord.cpp`

### 3.4 Native 客户端（libaudioclient）

目录：`frameworks/av/media/libaudioclient/`（P 分支客户端从 libmedia 独立）。

**播放建链三步**（AudioTrack.cpp）：
1. `AudioTrack::set()`（:351）→ `createTrack_l()`（:1384）→ `AudioSystem::getOutputForAttr`（binder 问策略"路由到哪个 output"）→ `audioFlinger->createTrack`（:1458）
2. `track->getCblk()` 拿共享控制块（:1491）→ `cblk` 指向 `audio_track_cblk_t`（:1512）→ 数据缓冲在 `cblk+1`
3. `mProxy = new AudioTrackClientProxy(...)`（:1573）——无锁环形缓冲操作

**数据传递核心**（[AudioTrackShared.cpp:172-233](frameworks/av/media/libaudioclient/AudioTrackShared.cpp#L172-L233)）：
- 共享内存：服务端在 `Client` 的 `MemoryDealer` 堆上分配 `cblk + 环形缓冲`，经 `IMemory` binder 传给客户端
- 同步：front/rear 原子索引 + futex 等待（`ClientProxy::obtainBuffer`）；服务端 `ServerProxy` 对称消费，经 `Track::getNextBuffer` 桥接给 `AudioMixer` 的 `AudioBufferProvider`

### 3.5 AudioFlinger（音频服务端）

目录：`frameworks/av/services/audioflinger/`。**核心职责：混音 + 效果 + 输出调度**，不做路由决策（路由是 AudioPolicy 的事）。

**服务端 createTrack**（[AudioFlinger.cpp:655](frameworks/av/services/audioflinger/AudioFlinger.cpp#L655)）：
```
AudioSystem::getOutputForAttr（:697，问策略拿 output 句柄）
→ checkPlaybackThread_l（:729，找/建播放线程）
→ registerPid（:736，每进程一个 Client，带共享内存堆）
→ thread->createTrack_l（:758，创建 Track）
→ new TrackHandle(track)（:812，BnAudioTrack 返回给客户端）
```

**线程模型**（openOutput_l 决定，[AudioFlinger.cpp:2032](frameworks/av/services/audioflinger/AudioFlinger.cpp#L2032)）：

| 线程 | 触发条件 | 说明 |
|---|---|---|
| MixerThread | 普通 PCM 输出 | 默认混音线程 |
| DirectOutputThread | `AUDIO_OUTPUT_FLAG_DIRECT` 或非 PCM | 直通（透传） |
| OffloadThread | `AUDIO_OUTPUT_FLAG_COMPRESS_OFFLOAD` | offload 播放 |
| MmapPlaybackThread | `MMAP_NOIRQ` | P 新增 |
| RecordThread | 打开输入 | 录音 |

**threadLoop 主循环**（[Threads.cpp:3374-3481](frameworks/av/services/audioflinger/Threads.cpp#L3374-L3481)）——排查"音频服务卡在哪"就看这里：
```
prepareTracks_l（:3362，每 track 音量/可混性/underrun 状态机）
→ threadLoop_mix（:3378，AudioMixer::process() 混音）
→ effectChains[i]->process_l（:3427，效果链处理）
→ threadLoop_write（:3472，AudioStreamOut::write → HAL out_write）
```

**FastMixer**（低延迟）：条件 `mFrameCount < mNormalFrameCount && 非 A2DP`（[Threads.cpp:3839](frameworks/av/services/audioflinger/Threads.cpp#L3839)）；实时线程 `onWork` 直写 HAL（FastMixer.cpp:327-504）。FAST track 资格：PCM + 采样率匹配 + fast slot 空闲（Threads.cpp:1919-1980）。

**混音引擎**：[libaudioprocessing AudioMixer](frameworks/av/media/libaudioprocessing/AudioMixer.cpp)（Q4.27 定点/float、NEON、重采样、downmix）；本 HAL 还把 Android 重采样器源码直接编入 HAL（`audio_android_resample_api.c`）。

**HAL 封装**（libaudiohal）：
- `DevicesFactoryHalHybrid`（[libaudiohal/2.0/DevicesFactoryHalHybrid.cpp:34](frameworks/av/media/libaudiohal/2.0/DevicesFactoryHalHybrid.cpp#L34)）：优先 HIDL 4.0，A2DP/hearing_aid 走 legacy
- `DeviceHalLocal`：`openOutputStream`（:104）直调 `open_output_stream`；**patch API 需要 HAL 版本 >= 3.0**（:149-152，本 HAL 是 3.0 ✓）
- `AudioHwDevice::openOutputStream`（AudioHwDevice.cpp:34-94）：direct 且非 offload 编码格式失败时用 **SpdifStreamOut（IEC61937 SPDIF 包装）** 重试（:71-84）

**PatchPanel**（[PatchPanel.cpp:139](frameworks/av/services/audioflinger/PatchPanel.cpp#L139)）：patch 生命周期管理；HAL 支持 patch 时直接下发；跨模块/双 source 时在 AF 内建 `PatchTrack`/`PatchRecord` **软件桥**（录音线程读 → 播放线程混）。

**Effects**（[Effects.cpp](frameworks/av/services/audioflinger/Effects.cpp)）：`createEffect`（AudioFlinger.cpp:2952）→ `EffectChain::createEffect_l` → `EffectModule`（包装 HAL 效果，:62-109）；处理在 threadLoop 的 `process_l` → `EffectModule::process` → `mEffectInterface->process()`（Effects.cpp:422，真正的 HAL 效果处理）。

### 3.6 AudioPolicy（音频策略层）

> 决策层：**不干活，只做决定**（路由去哪、音量多少、开哪个 output），干活的是 AudioFlinger 和 HAL。

**AudioPolicyService**（[service/AudioPolicyService.cpp](frameworks/av/services/audiopolicy/service/AudioPolicyService.cpp)）：
- 创建 3 个 `AudioCommandThread`（ApmTone/ApmAudio/ApmOutput，避免 binder 重入死锁）+ `AudioPolicyManager` + `AudioPolicyEffects`
- binder 实现都在 [AudioPolicyInterfaceImpl.cpp](frameworks/av/services/audiopolicy/service/AudioPolicyInterfaceImpl.cpp)（薄封装，转发给 APM）
- **回调通道** [AudioPolicyClientImpl.cpp](frameworks/av/services/audiopolicy/service/AudioPolicyClientImpl.cpp)：APM 通过它调 AudioFlinger（`openOutput/closeOutput/createAudioPatch/…`）

**AudioPolicyManager**（[managerdefault/AudioPolicyManager.cpp](frameworks/av/services/audiopolicy/managerdefault/AudioPolicyManager.cpp)，6292 行）——本培训最重要的单个文件：
- 路由决策：`getOutputForAttr`（:839，usage→流类型→策略→设备）→ `getOutputForDevice`（:967）：
  - offload 强制 DIRECT；`HW_AV_SYNC` 也强制 DIRECT
  - 找 `getProfileForDirectOutput` → 命中开 direct output（同 session 可复用）
  - 否则 fallback 混音输出：`getOutputsForDevice` + `selectOutput`（flag 匹配度→格式接近度→primary 优先级）
- 设备管理：`setDeviceConnectionStateInt`（:134）→ `checkOutputsForDevice` → `checkOutputForAllStrategies` → 关闭多余 output
- 音量：`setStreamVolumeIndex`（:2289）→ `computeVolume`（:5611，UI index→dB）→ `checkAndSetVolume`（:5714）→ `outputDesc->setVolume` → AudioCommandThread → AF → HAL
- offload 判定：`isOffloadSupported`（:2749，属性开关 + 时长 + 非 offloadable 效果 + 有无 profile）

**策略引擎**（enginedefault，本机启用；engineconfigurable 未启用）：
- [Engine.cpp](frameworks/av/services/audiopolicy/enginedefault/src/Engine.cpp)：`getStrategyForStream`（:174）、`getDeviceForStrategyInt`（:255-748，按 mForceUse 和设备优先级选设备）
- 9 个策略（RoutingStrategy.h）：MEDIA/PHONE/SONIFICATION/SONIFICATION_RESPECTFUL/DTMF/ENFORCED_AUDIBLE/TRANSMITTED_THROUGH_SPEAKER/ACCESSIBILITY/REROUTING

**设备策略配置**（改路由行为先改这里）：
- 主配置 [device/amlogic/p291_iptv/files/audio_policy_configuration.xml](device/amlogic/p291_iptv/files/audio_policy_configuration.xml)：primary 模块 halVersion 3.0；mixPorts：`primary output`(PRIMARY)、`tunnel pcm`(DIRECT|HW_AV_SYNC)、`multichannel output`(DIRECT|HW_AV_SYNC)、`tunnel a2dp`、`primary input`
- Dolby MS12 版（`audio_policy_configuration_dolby_ms12.xml`）：多 `tunnel raw`（AC3/E_AC3/DTS/IEC61937 raw profile）、HDMI Out/Spdif/Tuner 设备
- 音量曲线：`audio_policy_volumes.xml`（每流×设备类 index→mB）
- 效果配置：[audio_effects.conf](device/amlogic/p291_iptv/files/audio_effects.conf)（标准库 + Amlogic：balance/srs/treblebass/hpeq，dap 被注释）

### 3.7 HAL 层（audio.primary.amlogic）

> Amlogic 定制的核心，40+ 源文件。**问题最终都落到这里**。

**编译产物**（[audio_hal/Android.mk](hardware/amlogic/audio/audio_hal/Android.mk)）：`audio.primary.amlogic`（主库）、`libdtvad`、`libaudiopolicymanager`（DLG，仅 USE_CUSTOM_AUDIO_POLICY）、libms12api（prebuilt）。条件编译：`TV_AUDIO_OUTPUT`（8ch）/`DOLBY_MS12_ENABLE`/`ENABLE_AEC_FUNC`。

**播放类型分发**（`adev_open_output_stream_new` :10248 → `adev_open_output_stream` :4915）：

| flag | 输出实现 | 用途 |
|---|---|---|
| PRIMARY（或 NONE） | `out_write_legacy` / `out_write_new` | 系统混音 PCM（16/32bit） |
| DIRECT | `out_write_direct` | IEC61937 透传；格式映射见 [:5001](hardware/amlogic/audio/audio_hal/audio_hw.c#L5001)（61937@192k→EAC3、32-48k→AC3/DTS、192k 6ch→DTS_HD…） |
| HW_AV_SYNC | hwsync 模式 | tunnel pcm/multichannel，开 tsync |
| DIRECT+IEC958_NONAUDIO | 裸压缩流 | 区分"裸压缩"与"61937 包裹" |

**start_output_stream**（:758）路由选择：SCO→PORT_PCM、DIRECT→PORT_SPDIF → `alsa_device_update_pcm_index` → `pcm_open`（tinyalsa）；**ARC 时额外开 `earc_pcm`**。

**out_write 主循环**（:2861）：standby 先 start → 重采样（:2945）→ 有 hwsync 混音数据先写 `aml_hal_mixer` → `pcm_write`（ARC 写 earc_pcm，:2958-2968）。

**参数接口**（改行为/调试入口）：
- `out_set_parameters`（:1576）：`routing`、`hw_av_sync`、`A2dpSuspended`…
- `adev_set_parameters`（:5659）关键 key：`connect/disconnect`（HDMI/A2DP 插拔 :5799-5860）、**`HDMI ARC Switch`**（:5865）、`hdmi_format`/`spdif_format`/`audio_type`（数字音频格式 UI）、`speaker_delay_time_ms`/`spdif_delay_time_ms`、`screen_state`、`speaker_mute`、karaoke/EQ/DRC

**Offload/编解码相关文件**：
- [audio_hw_ms12.c:83](hardware/amlogic/audio/audio_hal/audio_hw_ms12.c) `dolby_ms12_threadloop`（SCHED_FIFO 实时线程）→ prebuilt libms12api，主伴音+关联音频进 MS12 引擎
- [aml_dca_dec_api.c:592](hardware/amlogic/audio/audio_hal/aml_dca_dec_api.c) `decode_threadloop`：DTS 软解（dlopen 授权库，`AML_Match_DTS_SyncWord` :229）
- [audio_hw_dtv.c:2667](hardware/amlogic/audio/audio_hal/audio_hw_dtv.c) `audio_dtv_patch_output_threadloop`：DTV 输入→输出，Dolby/DTS/默认分派 + apts 查找 + AUDIO_LOOKUP/BREAK/DROP/LATENCY 状态机
- [amlAudioMixer.c:1351](hardware/amlogic/audio/audio_hal/amlAudioMixer.c) mixer 线程（submix 混音）、[aml_audio_mixer.c:1036](hardware/amlogic/audio/audio_hal/aml_audio_mixer.c)（TV 平台混音线程绑定 CPU2/3）
- 格式解析：[aml_ac3_parser.c](hardware/amlogic/audio/audio_hal/aml_ac3_parser.c)（IEC61937 包头）、[audio_format_parse.c](hardware/amlogic/audio/audio_hal/audio_format_parse.c)（`audio_type_parse` :299 识别 AC3/EAC3/DTS1-3/DTSHD/TRUEHD）
- 延迟：[aml_audio_delay.c:102](hardware/amlogic/audio/audio_hal/aml_audio_delay.c)（按输出口 ring buffer 插零/丢数据）
- ALSA 通路：[alsa_manager.c:81](hardware/amlogic/audio/audio_hal/alsa_manager.c) `aml_alsa_output_open`（I2S/DIGITAL/EARC 设备选择、DCV 双输出）、[alsa_config_parameters.c:130](hardware/amlogic/audio/audio_hal/alsa_config_parameters.c)（按格式生成 pcm_config，**TV 平台固定 8ch S32_LE**）

**A2DP**（Amlogic 自研通路，非 AOSP）：[a2dp_hal.c](hardware/amlogic/audio/audio_hal/a2dp_hal.c) + [a2dp_hw.c](hardware/amlogic/audio/audio_hal/a2dp_hw.c)：**unix socket（A2DP_DATA_PATH）** 与蓝牙栈通信（`a2dp_command` :317）；`a2dp_out_write`（:586）→ 8ch32→2ch16（:437）→ 状态机自动 start datapath（:369）→ 重采样（:500）→ mono 降混 → `skt_write`。

**录音/输入**（`adev_open_input_stream` :7018）：USB（audio_usb_hal.c 含 karaoke）、蓝牙远场麦（HBG/CSK）、`in_read`（:4546）多路录音（audio_multi_recorder.c）、**HDMI IN**（amadec 解析 + `parserRingBufferDataRead`，信号不稳定静音 100ms）、AEC（audio_aec_process.cpp 封 GoogleAec）、**DTV tuner**（`create_dtv_patch` + audio_dtv_ad.c）。

### 3.8 内核层（common/）

> 注意：**内核源码在 `common/` 目录**（Linux 4.9.113 Amlogic 分支），不在 `kernel/`（那个只有 configs/tests）。烧录的是预编译 Image.gz。

**驱动地图**：

```
common/
├── sound/soc/amlogic/
│   ├── meson/        机器驱动(audio_hw.c)、i2s/spdif/pcm DAI ← 本机实际使用
│   ├── auge/         AUGE 平台（g12a+ 新平台用；本板 DTS 未挂载）
│   ├── common/       audio_uevent、iec_info、spdif_info
│   └── codecs/amlogic/  T9015(S)、TXLX/TL1 ACODEC、TAS5805/5707/575X、
│                        SSM3515/3525、CS42528、PCM186X、PCM2BT…
├── drivers/amlogic/
│   ├── amaudio/      /dev/amaudio + /sys/class/amaudio（output_enable 等）
│   ├── amaudio2/     amaudio 替代实现
│   ├── audiodsp/     DSP 解码：dsp_mailbox（M2B_IRQ2_DECODE_START/STOP）、
│   │                 /sys/class/audiodsp/codec_mips（DSP 负载）
│   ├── hifi4dsp/     HiFi4 DSP：固件加载 + IPC
│   ├── dolby_fw/     dolby_fw_smc_call() 加载 Dolby 固件到 secure world
│   ├── amlkaraoke/   卡拉 OK（混音/reverb/USB 话筒）
│   ├── audioinfo/    音频信息
│   └── media/frame_sync/  ★ tsync.c / tsync_pcr.c / timestamp.c（AV 同步）
└── arch/arm64/boot/dts/amlogic/gxlx2_p291.dts   ← 板级 DTS
```

**板级声卡拓扑**（[gxlx2_p291.dts:1061](common/arch/arm64/boot/dts/amlogic/gxlx2_p291.dts#L1061)）：`aml_sound_meson` → 声卡名 **AML-MESONAUDIO**；CPU DAI = i2s/spdif/pcm(dsp_a，BT 用)；codec = **T9015 内置 codec**（reg 0xc8832000）+ spdif_codec + pcm2BT；mute_gpio = GPIOH_5。

> 结论：p291 跑 MESON ASoC 框架 + T9015；**DTS 无 earc/auge 节点**——本机 ARC 输出是 SPDIF→HDMI TX 的 ARC 通道，不是内核 earc 收端。

**内核 ↔ 用户态接口对照**（排查必备）：

| 内核模块 | 用户态接口 | 谁在用 |
|---|---|---|
| amaudio | `/dev/amaudio`、`/sys/class/amaudio/output_enable/dts_enable/dolby_enable` | amplayer（player_set_sys.c:168） |
| ALSA | `hw:0` + tinymix 控件（`SPK mute`、`Audio hdmi-out mute`、`DAC Digital Playback Volume`） | HAL 经 tinyalsa 写 |
| audiodsp | `/sys/class/audiodsp/codec_mips` | AmlogicPlayer.cpp:803 查 DSP 负载 |
| dolby_fw | ioctl → SMC | MS12 授权固件 |
| tsync | `/sys/class/tsync/`：`event`(AUDIO_PAUSE=11/AUDIO_RESUME=12)、`pts_video`、`pts_audio`、`mode`、`enable`、`pcr_recover`、`dobly_av_sync` | HAL audio_hwsync.c |
| hdmitx | hdmitx 驱动 | HDMI 音频嵌入 |

**defconfig 关键项**（[meson64_iptv_p291_defconfig](device/amlogic/p291_iptv/meson64_iptv_p291_defconfig)）：`CONFIG_AMLOGIC_AMAUDIO/AMAUDIO2=y`、`CONFIG_AMLOGIC_AUDIO_DSP=y`、`CONFIG_AMLOGIC_HIFI4DSP=y`、`CONFIG_DOLBY_FW=y`、`CONFIG_AMLOGIC_SND_SOC=y`（MESON+AUGE+COMMON）、`CONFIG_AMLOGIC_HDMITX=y`。

---

## 第 4 章 四大核心流程全链路

**本章目标**：把第 3 章各层串起来，能背诵四条链路，是排查问题的基本功。

### 4.1 播放链路（标准通路）

```
App: AudioTrack.play()                     [AudioTrack.java:2071]
 → JNI android_media_AudioTrack_start      [core/jni/android_media_AudioTrack.cpp:472]
 → AudioTrack::start()                     [libaudioclient/AudioTrack.cpp:646]
   （内部先 createTrack 已完成：AudioSystem::getOutputForAttr 问策略
     → AudioFlinger::createTrack → TrackHandle）
 → BpAudioTrack::start (binder START)
 → Track::start()                          [audioflinger/Tracks.cpp:709]
 → PlaybackThread::addTrack_l              [Threads.cpp:2307]
     └─ AudioSystem::startOutput           （通知策略流激活，触发路由生效）
 → threadLoop 每周期：
     prepareTracks_l → threadLoop_mix (AudioMixer::process)
     → effectChains process_l → threadLoop_write
 → AudioStreamOut::write                   [AudioStreamOut.cpp:205]
 → StreamOutHalLocal::write                [libaudiohal/2.0/StreamHalLocal.cpp]
 → audio_hw.c out_write                    [audio_hw.c:2861]
     └─ 重采样 → aml_hal_mixer → pcm_write (tinyalsa)
 → ALSA hw:0 (i2s_dai + T9015) → LOUTL/LOUTR → 功放 → 喇叭
```

### 4.2 音量链路

```
用户按音量键 → AudioService.adjustStreamVolume   [AudioService.java:1564]
 → getActiveStreamType 选流 → setStreamVolumeInt (:2290)
 → VolumeStreamState.setIndex → MSG_SET_DEVICE_VOLUME (:2308)
 → setDeviceVolume (:5380) → AudioSystem.setStreamVolumeIndex (JNI)
 → IAudioPolicyService → AudioPolicyManager.setStreamVolumeIndex [AudioPolicyManager.cpp:2289]
 → computeVolume (index→dB，按设备类音量曲线)     [AudioPolicyManager.cpp:5611]
 → checkAndSetVolume                            [AudioPolicyManager.cpp:5714]
     ├─ TV 定制：HDMI/ARC/耳机 0dB；扬声器 updateGain 硬件增益；开机视频 -18.37dB
     └─ outputDesc->setVolume → AudioCommandThread → AudioSystem::setStreamVolume
 → AudioFlinger::setStreamVolume → MixerThread 每 track 音量 → AudioMixer
 → HAL set_stream_volume / 混音时应用
```

### 4.3 路由链路（例：HDMI 插入）

```
HDMI 热插拔事件 → AudioService.handleDeviceConnection [AudioService.java:6268]
 → AudioSystem.setDeviceConnectionState (JNI)
 → AudioPolicyService → AudioPolicyManager.setDeviceConnectionStateInt [AudioPolicyManager.cpp:134]
     ├─ 定制①：读 /sys/class/switch/hdmi_rxsense/state，HDMI=0 强制不可用 (:148-158)
     ├─ 广播 connect 给 HAL（获取动态参数）
     ├─ checkOutputsForDevice → checkOutputForAllStrategies（重路由所有策略）
     └─ Engine.getDeviceForStrategy 决策 → createAudioPatch 下发 / routing 参数
 → HAL: aml_audio_output_routing [audio_hw.c]（日志可见 "switch from HDMI to SPEAKER"）
     └─ alsa_device_update_pcm_index → pcm_open 新端口 / 切 mixer 通路
```

### 4.4 录音链路

```
App: AudioRecord.startRecording
 → AudioSystem.getInputForAttr → AudioPolicyManager.getInputForAttr [AudioPolicyManager.cpp:1516]
   （TV 定制：语音搜索强制 REMOTE_SUBMIX，见 DLGAudioPolicyManager）
 → AudioFlinger.openInput → RecordThread
 → HAL in_read [audio_hw.c:4546]
     ├─ MIC 源：audio_multi_recorder 多路录音
     ├─ HDMI IN：amadec 解析 → parserRingBufferDataRead
     ├─ AEC：读 4ch（2ch 人声 + 2ch 参考）→ aec_spk_mic_process_int16
     └─ DTV tuner：audio_dtv_patch / audio_dtv_ad（关联音频）
 → RecordThread → RecordBufferConverter（重采样/格式转换）→ 共享缓冲 → AudioRecord
```

---

## 第 5 章 本平台实际修改（定制点）

**本章目标**：知道"哪些地方被 Amlogic/客户改过、为什么改"，改 bug 前先对照，避免改错位置。

### 5.1 AudioPolicyManager（frameworks/av）

| 位置 | 修改内容 | 原因 |
|---|---|---|
| [AudioPolicyManager.cpp:86-158](frameworks/av/services/audiopolicy/managerdefault/AudioPolicyManager.cpp#L86-L158) | `HDMI_RXSENSE_STATE` sysfs 检测：HDMI=0 时强制设备不可用 | 防路由到未插线的 HDMI 口 |
| [:228-231](frameworks/av/services/audiopolicy/managerdefault/AudioPolicyManager.cpp#L228-L231) | USB 耳机断开 → `speaker_mute=0`（ARC 激活时除外） | USB 耳机插拔自动切换喇叭 |
| [:5784-5810](frameworks/av/services/audiopolicy/managerdefault/AudioPolicyManager.cpp#L5784-L5810) | TV 音量逻辑（`ro.vendor.platform.is.stb`）：所有流用 MUSIC 曲线；ARC/耳机强制 0dB；开机视频音量 -18.37dB；扬声器 updateGain | 电视场景：音量由 TV/功放管；开机 logo 视频不爆音 |
| [:78](frameworks/av/services/audiopolicy/managerdefault/AudioPolicyManager.cpp#L78) | SURROUND_FORMATS 加 AC4 | 杜比全景声流格式支持 |

### 5.2 策略引擎（enginedefault/Engine.cpp）

| 位置 | 修改内容 |
|---|---|
| Engine.cpp:383-409 | `persist.audio.type` 0/1/2/3 强制 USB/HDMI/Speaker 出音 |
| :606-612 | `AUDIO_POLICY_FORCE_HDMI_ARC` / `FORCE_SPDIF` 强制 |
| :690-722 | MEDIA 策略下 HDMI_ARC 可与 Speaker 共存；蓝牙/USB 存在时去掉 ARC |
| :937-941 | `AUDIO_SOURCE_CMCC_KARAOK_SPEAKER → AUDIO_DEVICE_IN_BUS`（移动卡拉 OK） |

### 5.3 HAL 内策略（DLGAudioPolicyManager）

`USE_CUSTOM_AUDIO_POLICY=1` 时编译 [DLGAudioPolicyManager.cpp](hardware/amlogic/audio/audio_hal/DLGAudioPolicyManager.cpp)（libaudiopolicymanager 替换默认策略），覆写：
- `setDeviceConnectionState`（:66）：解析 `force=` 参数设置 `mForceSubmixInputSelection`
- `getDeviceForInputSource`（:95）：语音搜索（VOICE_RECOGNITION）强制走 REMOTE_SUBMIX（遥控器语音）

### 5.4 HAL（audio_hw.c 等，最大的定制区）

| 项 | 说明 |
|---|---|
| ARC 输出 | `HDMI ARC Switch` 参数 + `earc_pcm` 双路 + 策略层 0dB |
| HDMI rxsense | 与 APM 联动（5.1 第一项） |
| 透传格式 | `hdmi_format`/`spdif_format`/`audio_type` 参数选择数字输出格式（PCM/DD/AUTO） |
| MS12 | `dolby_ms12_threadloop` SCHED_FIFO + libms12api + `tunnel raw` 通路 |
| A2DP | 自研 unix socket 通路替代 AOSP |
| DTV | audio_hw_dtv.c 全链路 + libdtvad（关联音频） |
| 卡拉 OK | amlkaraoke 内核驱动 + HAL karaoke 参数 + `ro.migukaraok=true` |
| AEC | ENABLE_AEC_FUNC 下 GoogleAec 封装 |
| 混音/重采样 | amlAudioMixer/aml_audio_mixer/audio_android_resample_api（Android 重采样器编入 HAL） |

### 5.5 板级配置（device/amlogic）

| 文件 | 说明 |
|---|---|
| [p291_iptv/files/audio_policy_configuration.xml](device/amlogic/p291_iptv/files/audio_policy_configuration.xml) | 主策略（+ `_dolby_ms12.xml`/`_livetv.xml`/`audio_policy_volumes.xml`） |
| [p291_iptv/files/audio_effects.conf](device/amlogic/p291_iptv/files/audio_effects.conf) | 效果库清单（balance/srs/treblebass/hpeq + 标准） |
| [p291_iptv/files/mixer_paths.xml](device/amlogic/p291_iptv/files/mixer_paths.xml) | T9015 控件默认值（`SPK mute`、`Audio hdmi-out mute`、`AED EQ enable`） |
| [p291_iptv.mk:785-798](device/amlogic/p291_iptv/p291_iptv.mk) | `BOARD_ALSA_AUDIO=tiny`、`BUILD_WITH_USB_KARAOK=true` |
| [p291_iptv.mk:1276](device/amlogic/p291_iptv/p291_iptv.mk) | `media.arm.audio.decoder=ape,flac,dts,ac3,eac3,wma,…` 软解格式白名单 |
| [device.mk:245-272](device/amlogic/p291_iptv/device.mk) | 按运营商 variant 拷贝策略文件 |
| [meson64_iptv_p291_defconfig](device/amlogic/p291_iptv/meson64_iptv_p291_defconfig) | 内核音频 CONFIG（见 3.8） |

### 5.6 内核定制（common/）

| 项 | 说明 |
|---|---|
| amaudio / amaudio2 | `/dev/amaudio`、`/sys/class/amaudio/*`（output_enable 等） |
| audiodsp / hifi4dsp | DSP 解码 + `codec_mips` 负载显示 |
| dolby_fw | MS12 固件 SMC 加载 |
| tsync | AV 同步引擎（`/sys/class/tsync/*`） |
| codecs/amlogic | T9015 等 codec 驱动 |
| gxlx2_p291.dts | 板级声卡拓扑（AML-MESONAUDIO） |

---

## 第 6 章 调试手段

**本章目标**：遇到问题知道"先看什么日志、敲什么命令、动什么属性"。

### 6.1 抓日志

| 场景 | 命令 | 关键 tag |
|---|---|---|
| 常规 | `adb logcat` | `AudioService`、`AudioFlinger`、`APM_AudioPolicyManager`、`audio_hw_primary`、`audio_hw_ms12`、`audio_hw_dtv`、`AmlogicPlayer`、`amplayer`、`audio_dsp`、`dolbyfw` |
| 播放器 | `adb logcat -s amplayer AmlogicPlayer` | abuflevel（音频缓冲水位）、状态机 |
| 路由 | 日志搜 `aml_audio_output_routing` | `switch from [0x2]HDMI to [0x0]SPEAKER` 等 |
| 内核 | `adb shell dmesg \| grep -iE "amaudio\|tsync\|snd"` | 声卡注册、tsync 事件 |

### 6.2 dumpsys

| 命令 | 内容 |
|---|---|
| `dumpsys audio` | AudioFlinger + 策略综合（线程/输出/track/音量/效果） |
| `dumpsys media.audio_flinger` | AF 详细（每个线程、每 track 状态、混音统计） |
| `dumpsys media.audio_policy` | 策略详细（设备连接状态、策略→设备映射、音量曲线） |
| `dumpsys audio_policy` | 同上（别名） |
| `dumpsys media.audio_flinger \| grep -iE "thread|track"` | 快速看线程/track |

### 6.3 属性开关（改行为不用改代码）

| 属性 | 作用 |
|---|---|
| `audio.offload.disable` | 1 关闭 offload（APM isOffloadSupported 判定） |
| `audio.offload.video` | 允许带视频 offload |
| `audio.offload.min.duration.secs` | offload 最短时长 |
| `persist.audio.type` | 0/1/2/3 强制输出口（Engine 定制） |
| `persist.vendor.audio.output` / `vendor.audio.hdmioutput` | 输出口切换（switch_audio_output.sh） |
| `media.amplayer.truehd` | 0/1 TrueHD 直通 |
| `media.dtv.passthrough.latencyms` | DTV 直通延迟 |
| `ro.vendor.platform.is.stb` | 是否 TV 单音量模式 |
| `media.arm.audio.decoder` | 软解格式白名单 |
| `ro.migukaraok` | 卡拉 OK 开关 |

### 6.4 HAL 参数（命令行直发）

```
adb shell "cmd audio" / 或直接：
adb shell service call audio 32 ...  # 不推荐
# 推荐用 AudioSystem 工具或 app 触发；验证 HAL 参数可用：
adb shell setprop vendor.audio.hdmioutput 1   # 示例
```
（代码内可用 `AudioSystem::setParameters("hdmi_format=...")` 验证 HAL 各 key，见 3.7 参数表）

### 6.5 tinymix / tinyplay（板级直接验证，绕开 Android 栈）

```
adb shell tinymix                       # 看全部控件（T9015: SPK mute、DAC Volume…）
adb shell tinymix "SPK mute" 0          # 开喇叭
adb shell tinyplay /data/test.wav       # 直接 ALSA 播 WAV（验证内核/硬件）
adb shell tinycap /data/test.wav -D 0   # 直接录音
```

> 排查顺序铁律：**先用 tinyplay 验证内核/硬件 OK，再往上查 HAL/AudioFlinger/策略**。

### 6.6 sysfs 直接观察

```
adb shell cat /sys/class/switch/hdmi_rxsense/state   # HDMI 连接（APM 也读它）
adb shell cat /sys/class/amaudio/output_enable        # 音频通路开关（amplayer 控制）
adb shell cat /sys/class/audiodsp/codec_mips          # DSP 解码负载
adb shell cat /sys/class/tsync/enable                 # AV 同步
```

### 6.7 实操示例：验证一次完整播放

```
1. adb shell dumpsys media.audio_flinger | grep -E "Thread|output"   # 确认线程
2. adb shell dumpsys media.audio_policy | grep -A5 "MEDIA"           # 确认路由设备
3. adb logcat -c; 播放一首歌; adb logcat -s AudioFlinger APM_AudioPolicyManager audio_hw_primary
4. 搜 "start_output" / "out_write" / "aml_audio_output_routing"
```

---

## 第 7 章 常见问题与排查思路

**本章目标**：常见故障从现象→定位→修复的套路，能独立处理 80% 现场问题。

### 7.1 完全无声（应用播放）

```
1. 硬件验证：tinymix "SPK mute" 0 + tinyplay test.wav → 有声=硬件 OK
2. 路由：dumpsys media.audio_policy 看 MUSIC 策略落在哪个设备
   日志搜 aml_audio_output_routing（是否被切到 HDMI/蓝牙？）
3. 音量：dumpsys audio | grep -i volume；检查是否 0 / 是否 FixedVolume 设备
4. 焦点：另一个 app 持有焦点？dumpsys audio 看 focus stack
5. HAL 层：logcat 搜 audio_hw_primary 的 out_write/start_output_stream 报错
6. 检查 speaker_mute 参数是否被置 1（USB 耳机插拔逻辑 5.1）
```

### 7.2 开机/特定 app 无声

- 开机 logo 视频：策略里音量压到 -18.37dB（5.1 第三项），检查 `service.bootvideo.exit` 属性
- 单个 app：检查流类型（TV 所有流别名到 MUSIC，音量/静音被共享）；检查焦点、MediaSession
- 所有系统音无声但播放正常：音效配置（audio_effects.conf）是否加载失败 → dumpsys audio 看 effects

### 7.3 断音/卡顿（爆音）

```
1. 确认是否 offload：audio.offload.disable=1 对比
2. logcat 搜 underrun / AudioFlinger 警告
3. 采样率/周期：HAL alsa_config_parameters 的 period_size；
   属性 audio.usb.period_us（USB 时）
4. 内存/CPU：amplayer 缓冲水位 abuflevel 频繁见底 → 网络/解码慢
5. 蓝牙：A2DP socket 链路是否超时（a2dp_hal 状态机日志）
```

### 7.4 音画不同步

```
1. 标准通路：检查 setPlaybackRate/getTimestamp 日志（MediaClock 锚点）
2. amplayer/tunnel：/sys/class/tsync/* 各值；
   属性 media.amplayer.smooth_region（smooth 区域，日志常见 7200000000ns）
3. HAL：aml_avsync_tuning 日志（avsync_adelay/avsync_drop 微调值）
4. DTV：media.dtv.passthrough.latencyms 调整直通延迟
5. 先看视频还是音频快，再分别调 tsync mode（/sys/class/tsync/mode）
```

### 7.5 音量问题

- 音量小：T9015 `DAC Digital Playback Volume` 默认值（mixer_paths.xml）；updateGain 增益范围（devicePorts gain -8400..4000）
- 音量不可调：设备在 `mFixedVolumeDevices`（HDMI_ARC/SPDIF/HDMI）→ TV 侧管音量，属预期
- 各 app 音量不一致：TV 单音量别名（所有流= MUSIC）属预期
- 最大音量仍小：检查曲线 audio_policy_volumes.xml、`ro.audio.mapvalue` 映射

### 7.6 蓝牙（A2DP）无声/卡顿

```
1. 确认走蓝牙：日志搜 "connect" 参数、a2dp_out_open
2. 蓝牙栈侧 codec 配置：a2dp_hw.c a2dp_get_output_audio_config
3. socket 通路：A2DP_DATA_PATH 是否建立，a2dp_state_process 状态机
4. 属性/设备：dumpsys audio_policy 看 A2DP 设备是否 attached
```

### 7.7 HDMI / ARC 无声

```
1. cat /sys/class/switch/hdmi_rxsense/state（HDMI=0 → APM 强制不可用，设计如此）
2. 确认电视支持 ARC，且电视端已开
3. HAL 参数：HDMI ARC Switch（：5865）；策略：ARC 固定 0dB（预期）
4. 透传格式：hdmi_format/spdif_format/audio_type 与电视 EDID 能力匹配；
   EDID 能力在 HAL 动态 profile（get_sink_format）
5. dumpsys media.audio_policy | grep ARC 看设备连接状态
```

### 7.8 环绕声/透传失败

```
1. 内容格式：Dolby/DTS 是否进 out_write_direct（日志）
2. 配置：policy 里 tunnel raw（MS12 版）是否包含该格式；
   非 MS12 版只有 tunnel pcm → 需改配置
3. 电视端 EDID 是否声明支持（HDMI 输出能力协商）
4. media.amplayer.truehd（TrueHD 开关）
```

### 7.9 录音/语音问题

- 语音搜索无声：DLGAudioPolicyManager 的 REMOTE_SUBMIX 强制逻辑；看 `force=` 参数
- 回声/啸叫：AEC 是否编译（ENABLE_AEC_FUNC）；in_read 4ch 读入参考声道
- 卡拉 OK：`ro.migukaraok`；amlkaraoke 内核驱动；`AUDIO_SOURCE_CMCC_KARAOK_SPEAKER` 路由

### 7.10 通用定位口诀

> **先硬件后软件、先内核后上层、先路由后音量、先单通路后并发。**

---

## 第 8 章 术语表

| 术语 | 含义 |
|---|---|
| AudioFlinger | 音频服务端：混音、输出调度、效果 |
| AudioPolicy / APM | 策略层：路由、音量、设备管理决策 |
| MixerThread | 普通混音线程 |
| DirectOutputThread / OffloadThread | 直通/offload 播放线程（不混音） |
| FastMixer | 低延迟混音（实时线程） |
| cblk | 客户端-服务端共享控制块（环形缓冲+原子索引） |
| offload | 压缩格式不经过混音，直接送 HAL/解码器 |
| tunnel | 压缩流直通（Amlogic：tunnel pcm/raw，HW_AV_SYNC） |
| passthrough / 透传 | 原格式（AC3/DTS…）不经解码直接输出（SPDIF/HDMI） |
| AVSync / tsync | 音视频同步（内核 tsync + HAL avsync_tuning） |
| ARC / eARC | HDMI 音频回传通道（本机：SPDIF→HDMI TX） |
| MS12 | Dolby 多流解码引擎（libms12api） |
| T9015 | SoC 内置音频 codec（DAC/ADC） |
| amplayer | Amlogic 播放器内核（LibPlayer） |
| amadec | Amlogic ARM 软解音频库 |
| amcodec | Amlogic 编解码用户态库（vdec 等） |
| submix | 远程混音输出（WFD/录屏） |
| EDID | HDMI 显示/音频能力描述 |
| tinyalsa | 精简 ALSA 用户态库（pcm_open/pcm_write/tinymix） |

---

## 第 9 章 附录：关键文件索引

| 层级 | 文件 | 作用 |
|---|---|---|
| 策略 | [AudioPolicyManager.cpp](frameworks/av/services/audiopolicy/managerdefault/AudioPolicyManager.cpp) | 策略核心（路由/音量/设备） |
| 策略 | [Engine.cpp](frameworks/av/services/audiopolicy/enginedefault/src/Engine.cpp) | 策略引擎（定制多） |
| 策略 | [AudioPolicyService.cpp](frameworks/av/services/audiopolicy/service/AudioPolicyService.cpp) | 策略 binder 服务 |
| 策略 | [p291_iptv/files/audio_policy_configuration.xml](device/amlogic/p291_iptv/files/audio_policy_configuration.xml) | 设备策略配置 |
| 服务 | [AudioFlinger.cpp](frameworks/av/services/audioflinger/AudioFlinger.cpp) | 音频服务核心 |
| 服务 | [Threads.cpp](frameworks/av/services/audioflinger/Threads.cpp) | 线程实现（threadLoop） |
| 服务 | [Tracks.cpp](frameworks/av/services/audioflinger/Tracks.cpp) | Track 生命周期 |
| 客户端 | [libaudioclient/AudioTrack.cpp](frameworks/av/media/libaudioclient/AudioTrack.cpp) | native 播放客户端 |
| 客户端 | [AudioTrackShared.cpp](frameworks/av/media/libaudioclient/AudioTrackShared.cpp) | 共享内存环形缓冲 |
| Java | [AudioService.java](frameworks/base/services/core/java/com/android/server/audio/AudioService.java) | Java 音量/焦点 |
| HAL | [audio_hw.c](hardware/amlogic/audio/audio_hal/audio_hw.c) | HAL 主实现 |
| HAL | [audio_hw_ms12.c](hardware/amlogic/audio/audio_hal/audio_hw_ms12.c) | Dolby MS12 |
| HAL | [audio_hw_dtv.c](hardware/amlogic/audio/audio_hal/audio_hw_dtv.c) | DTV 通路 |
| HAL | [audio_hwsync.c](hardware/amlogic/audio/audio_hal/audio_hwsync.c) | tunnel AV 同步 |
| HAL | [a2dp_hal.c](hardware/amlogic/audio/audio_hal/a2dp_hal.c) | 蓝牙 A2DP |
| HAL | [DLGAudioPolicyManager.cpp](hardware/amlogic/audio/audio_hal/DLGAudioPolicyManager.cpp) | HAL 内策略 |
| 媒体 | [AmlogicPlayer.cpp](vendor/amlogic/common/frameworks/av/MediaFactory/AmlogicPlayer.cpp) | 播放器封装 |
| 媒体 | [LibPlayer/amplayer/](vendor/amlogic/common/LibPlayer/amplayer/) | 播放内核 |
| 媒体 | [amadec/](hardware/amlogic/LibAudio/amadec/) | 软解输出（android-out/alsa-out） |
| 内核 | [common/drivers/amlogic/amaudio/](common/drivers/amlogic/amaudio/) | 声卡旧接口 |
| 内核 | [common/drivers/amlogic/media/frame_sync/tsync.c](common/drivers/amlogic/media/frame_sync/tsync.c) | AV 同步引擎 |
| 内核 | [common/sound/soc/amlogic/meson/](common/sound/soc/amlogic/meson/) | 本机 ASoC 框架 |
| 内核 | [common/arch/arm64/boot/dts/amlogic/gxlx2_p291.dts](common/arch/arm64/boot/dts/amlogic/gxlx2_p291.dts) | 板级 DTS |

---

## 附录：培训建议（带人用）

1. **第一周**：跑通第 4 章四条链路（对着源码逐行走一遍），用第 6.7 节实操验证一次播放。
2. **第二周**：用第 7 章常见问题清单模拟 3-5 个故障场景（改属性/断设备），练习定位。
3. **第三周**：独立改一个定制点（如换默认输出口、加一种透传格式），对照第 5 章定制点清单。
4. **考核**：30 分钟口述"从 App 播放到喇叭出声经过哪些组件，各组件职责"；现场查一个无声音案例。
