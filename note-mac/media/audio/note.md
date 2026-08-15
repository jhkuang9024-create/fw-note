# Audio

## Android 14 Audio 模块整体架构

> 五层架构：应用层 → Java Framework → Native 服务层 → HAL 层 → 内核/硬件。
> 实线（①-⑦）= 媒体数据流；虚线 = 控制流。进程 PID 均取自本设备（Amlogic AOSP14）实测日志。

![Android 14 Audio 模块整体架构图](audio_arch_android14.png)

<details>
<summary>架构图 mermaid 源码（点击展开，修改后可重新渲染）</summary>

```mermaid
flowchart TB
    subgraph L1["应用层（App 进程）"]
        direction LR
        MP["MediaPlayer<br/>MoviePlayer/MyMediaPlayer<br/>MediaPlayerExt<br/>（播放器 PID 2968）"]
        AT["AudioTrack<br/>PCM 直放接口"]
        AMG["AudioManager<br/>音量 / 焦点 API"]
        MSESS["MediaSession<br/>媒体会话"]
        SUI["MediaPlayer<br/>（systemui 按键音<br/>PID 1033）"]
    end

    subgraph L2["Java Framework（system_server，PID 632）"]
        direction LR
        ASRV["AudioService<br/>音量管理 / 焦点仲裁<br/>MediaFocusControl"]
        MSSRV["MediaSessionService<br/>会话管理 / 音量键分发"]
    end

    subgraph L3["Native 服务层（独立守护进程）"]
        direction LR
        MPS["MediaPlayerService<br/>（mediaserver PID 537）"]
        NU["NuPlayer → AmNuPlayer<br/>GenericSource│Decoder│Renderer<br/>（mediaserver PID 537）"]
        AF["AudioFlinger<br/>混音 MixerThread<br/>（audioserver PID 475）"]
        APS["AudioPolicyService<br/>设备路由策略<br/>（audioserver PID 475）"]
        EXT["MediaExtractorService<br/>AmFFmpegExtractor 解封装<br/>（media.extractor PID 535）"]
        C2S["Codec2 HAL 服务<br/>c2.amlogic.audio.decoder.ffmpeg<br/>ffmpeg 软解（PID 433）"]
        OMX["OMX HAL ✗<br/>本设备不存在"]
    end

    subgraph L4["HAL 层"]
        AHAL["音频 HAL（audio.primary，PID 423）<br/>PCM_NORMAL 用例 / PCM_SYSTEM 混音 / 48kHz<br/>ALSA latency 64ms（non-tunnel）"]
    end

    subgraph L5["内核 / 硬件"]
        KERN["ALSA 驱动 / 声卡"] --> SPK["扬声器 / HDMI"]
    end

    MP -->|"① setDataSource/prepare/start/seekTo<br/>Binder"| MPS
    SUI -->|"按键音：同样经 MediaPlayerService"| MPS
    MPS --> NU
    NU -->|"② MediaExtractor::Create 取包<br/>Binder"| EXT
    NU -->|"③ Codec2 解码请求<br/>hwBinder"| C2S
    C2S -->|"④ PCM 回传"| NU
    NU -->|"⑤ AudioTrack.write<br/>Binder"| AF
    AF -->|"⑥ 混音后 PCM<br/>AIDL Audio HAL"| AHAL
    AHAL -->|"⑦ ALSA write"| KERN
    AT -->|"PCM write（Binder）"| AF

    AMG -.->|"焦点 / 音量（Binder）"| ASRV
    MSESS -.->|"Binder"| MSSRV
    MSSRV -.->|"音量键 / 媒体键分发"| ASRV
    ASRV -.->|"音量设置（Binder）"| AF
    APS -.->|"路由策略下发"| AHAL
    NU -.->|"探测失败（VINTF 无条目）"| OMX
```

</details>

## 核心播放链路进程（6 个）

| 进程                                                       | PID                      | 角色                             | **关键组件（日志佐证）**                                     |
| ---------------------------------------------------------- | ------------------------ | -------------------------------- | ------------------------------------------------------------ |
| **文件管理器** com.starnet.filemanager                     | 2573                     | 触发源                           | FileActivity：识别点击的音频，发 `ACTION_VIEW` 拉起播放器    |
| **播放器 App** com.droidlogic.exoplayer2.demo（AM Player） | 2968（由 Zygote 410 fork | 用户态客户端：UI + Binder 客户端 | MoviePlayer（Activity/UI/进度条）、MyMediaPlayer（封装 android.media.MediaPlayer）、**MediaPlayerExt**（Amlogic vendor 扩展，[:325](vscode-webview://1k209ksk0vj2fgsb54fucalmm2brcf034ud394plalqve86cu2t9/note-mac/media/audio/play_local_audio_file.log#L325)）、MediaSessionCompat、SubtitleManager、PreparedTimeoutTracker |
| **mediaserver**                                            | 537                      | 播放引擎主体（Binder 服务端）    | **MediaPlayerService**（每个播放器一个 Client，[:369](vscode-webview://1k209ksk0vj2fgsb54fucalmm2brcf034ud394plalqve86cu2t9/note-mac/media/audio/play_local_audio_file.log#L369)）、**AmlogicNuPlayerFactoryInit → AmNuPlayerDriver/AmNuPlayer**（替代标准 NuPlayer，V5.1.206，[:341-348](vscode-webview://1k209ksk0vj2fgsb54fucalmm2brcf034ud394plalqve86cu2t9/note-mac/media/audio/play_local_audio_file.log#L341-L348)）、**NU-GenericSource**（FileSource 读 fd、AmAnotherPacketSource 包队列、[:645](vscode-webview://1k209ksk0vj2fgsb54fucalmm2brcf034ud394plalqve86cu2t9/note-mac/media/audio/play_local_audio_file.log#L645)）、**NU-AmNuPlayerDecoder**、**NU-AmNuPlayerRenderer**（AudioSink 打开/排空、AV 同步、PTS 锚点）、**CCodec/CCodecBufferChannel**（Codec2 客户端，[:805](vscode-webview://1k209ksk0vj2fgsb54fucalmm2brcf034ud394plalqve86cu2t9/note-mac/media/audio/play_local_audio_file.log#L805)）、MediaCodec（异步模式）、AmlPlayerTimer/MessageMonitor（Amlogic 自研打点设施）、BufferPoolAccessor（客户端缓冲池） |
| **media.extractor**                                        | 535                      | 解封装服务                       | **AmFFmpegExtractor**（MediaExtractor::Create 返回 `extractor name:AmFFmpegExtractor`，[:456](vscode-webview://1k209ksk0vj2fgsb54fucalmm2brcf034ud394plalqve86cu2t9/note-mac/media/audio/play_local_audio_file.log#L456)；嗅探/解封装/字节级 seek，[:403-436](vscode-webview://1k209ksk0vj2fgsb54fucalmm2brcf034ud394plalqve86cu2t9/note-mac/media/audio/play_local_audio_file.log#L403-L436), [:2553](vscode-webview://1k209ksk0vj2fgsb54fucalmm2brcf034ud394plalqve86cu2t9/note-mac/media/audio/play_local_audio_file.log#L2553)）、AmFFmpegByteIOAdapter、AmFFmpegSource、StreamFormatter |
| **Codec2 HAL 进程**                                        | 433                      | 解码服务（hwBinder 服务端）      | **C2VendorComponentStore**（加载 `libcodec2_aml_audio_decoder.so`，[:827-830](vscode-webview://1k209ksk0vj2fgsb54fucalmm2brcf034ud394plalqve86cu2t9/note-mac/media/audio/play_local_audio_file.log#L827-L830)）、**Amlogic_C2AudioFFMPEGDecoder**（组件 `c2.amlogic.audio.decoder.ffmpeg`，[:805](vscode-webview://1k209ksk0vj2fgsb54fucalmm2brcf034ud394plalqve86cu2t9/note-mac/media/audio/play_local_audio_file.log#L805)）、AmAudioCodec/**AmFFmpegDecoder**（ffmpeg 软解 mp3float，[:971](vscode-webview://1k209ksk0vj2fgsb54fucalmm2brcf034ud394plalqve86cu2t9/note-mac/media/audio/play_local_audio_file.log#L971)）、BufferPoolAccessor（服务端 DMABUF 缓冲池） |
| **audioserver**                                            | 475                      | 音频混音                         | **AudioFlinger**：`createTrack_l()` 创建音轨（[:1061](vscode-webview://1k209ksk0vj2fgsb54fucalmm2brcf034ud394plalqve86cu2t9/note-mac/media/audio/play_local_audio_file.log#L1061)）、notificationFrames/frameCount 管理（[:1063](vscode-webview://1k209ksk0vj2fgsb54fucalmm2brcf034ud394plalqve86cu2t9/note-mac/media/audio/play_local_audio_file.log#L1063)） |
| **音频 HAL 进程**（Amlogic audio.primary）                 | 423                      | 硬件抽象/最终输出                | **audio_hw_hal_primary**（latency 64ms、source metadata，[:286](vscode-webview://1k209ksk0vj2fgsb54fucalmm2brcf034ud394plalqve86cu2t9/note-mac/media/audio/play_local_audio_file.log#L286)）、**audio_hw_hal_submixing**（PCM_NORMAL usecase、PCM_SYSTEM input port、48kHz 混音器，[:153-168](vscode-webview://1k209ksk0vj2fgsb54fucalmm2brcf034ud394plalqve86cu2t9/note-mac/media/audio/play_local_audio_file.log#L153-L168)）、audio_hw_hal_port/stream（端口配置、`non tunnel` PCM 流、position/jitter 监测，[:210-212](vscode-webview://1k209ksk0vj2fgsb54fucalmm2brcf034ud394plalqve86cu2t9/note-mac/media/audio/play_local_audio_file.log#L210-L212), [:1143](vscode-webview://1k209ksk0vj2fgsb54fucalmm2brcf034ud394plalqve86cu2t9/note-mac/media/audio/play_local_audio_file.log#L1143)）、audio_hw_hal_resourcemgr、AudioHalHardware（get/setParameters）→ 之下是 ALSA/内核 → 扬声器 |

## 总结

**一次音频播放至少要过 6 个进程**：App → mediaserver → media.extractor → Codec2 服务 → audioserver → 音频 HAL。这是 Android 14 把解封装（media.extractor 独立服务）和解码（Codec2 HAL 独立进程）都外置到独立进程的结果

###### **每个解封装/解码环节都多一次跨进程**：mediaserver 的 NU-GenericSource 用 Binder 调 media.extractor 取包，NU-AmNuPlayerDecoder 用 hwBinder 调 C2 服务解码

## 播放 U 盘音频文件完整时序图（基于 play_local_audio_file.log 实测）

> 说明：participant 标签 = 进程名 + PID；箭头上的时间戳取自日志原文；`Note` 标注对应组件的关键处理逻辑。
> 测试文件：《华晨宇 - 地球之盐.mp3》（U 盘 `/storage/6A6F-168B`，44.1kHz / 双声道 / 128kbps，时长 319.4s），操作序列：播放 → 音量加 → 音量减 → 快进 ×3。

```mermaid
sequenceDiagram
    participant USER as 用户
    participant FM as 文件管理器<br/>com.starnet.filemanager<br/>PID 2573
    participant SYS as system_server<br/>ATM/InputReader<br/>MediaSessionService<br/>AudioService/MediaFocusControl<br/>PID 632
    participant APP as AM Player<br/>com.droidlogic.exoplayer2.demo<br/>MoviePlayer/MyMediaPlayer<br/>MediaPlayerExt<br/>PID 2968
    participant MS as mediaserver<br/>MediaPlayerService<br/>AmNuPlayer/GenericSource<br/>Decoder/Renderer/CCodec<br/>PID 537
    participant EXT as media.extractor<br/>AmFFmpegExtractor<br/>PID 535
    participant C2 as Codec2 HAL 进程<br/>C2VendorComponentStore<br/>c2.amlogic.audio.decoder.ffmpeg<br/>PID 433
    participant AF as audioserver<br/>AudioFlinger<br/>PID 475
    participant HAL as 音频 HAL 进程<br/>Amlogic audio.primary<br/>submixing/48kHz/ALSA 64ms<br/>PID 423
    participant SUI as systemui<br/>按键音 MediaPlayer<br/>PID 1033

    rect rgb(225, 238, 255)
    Note over USER, APP: 【阶段1 触发与进程启动】09:55:37.2 ~ 37.8
    USER->>FM: 09:55:37.243 点击 U 盘《华晨宇 - 地球之盐.mp3》
    FM->>FM: startPlayerAction: audio path = /storage/6A6F-168B/本地音频/mp3/华晨宇 - 地球之盐.mp3
    FM->>SYS: 09:55:37.311 发 ACTION_VIEW Intent 拉起播放器
    SYS->>SYS: 09:55:37.338 ATM START Activity
    SYS->>APP: 09:55:37.363 AMS 经 Zygote(410) fork 新进程 2968
    APP->>APP: 09:55:37.607 MoviePlayer onCreate，构造 MyMediaPlayer + MediaPlayerExt（Binder 代理）
    end

    rect rgb(230, 255, 235)
    Note over APP, EXT: 【阶段2 数据源设置与解封装】09:55:37.8 ~ 37.9
    APP->>APP: playFile(resume:true) → setVideoPath → 判定本地文件走 file 路径
    APP->>MS: Binder: MediaPlayerService 创建播放器实例
    MS->>MS: 09:55:37.836 AmlogicNuPlayerFactoryInit: create AmNuPlayer（V5.1.206，替代标准 NuPlayer）
    MS->>MS: 09:55:37.839 探测 OMX 失败（VINTF 无条目）→ 确认解码走 Codec2
    APP->>MS: 09:55:37.847 Binder: setDataSourceAsync fd 9/0/5110837
    MS->>EXT: 09:55:37.886 MediaExtractor::Create（跨进程 Binder）
    EXT->>EXT: AmFFmpegExtractor 白名单+嗅探 mp3（confidence=0.8）
    EXT->>EXT: 解析：1 条音频轨 / 44.1kHz / 2ch / 128kbps / codec 86017(0x15001) / 时长 319.4s
    EXT-->>MS: 09:55:37.903 countTracks=1（find stream info 耗时 16.7ms）
    MS->>MS: NU-GenericSource 持有 FileSource(fd=10)，建 AmAnotherPacketSource 包队列（本地上限 500 包）
    MS-->>APP: 09:55:37.920 notifyPrepareCompleted → 回传 MEDIA_PREPARED
    end

    rect rgb(255, 243, 225)
    Note over APP, C2: 【阶段3 start 与解码器初始化】09:55:38.2 ~ 38.4
    APP->>SYS: 09:55:38.264 start() 前先 requestAudioFocus
    SYS-->>APP: 09:55:38.270 MediaFocusControl 授予焦点（USAGE_MEDIA/CONTENT_TYPE_MUSIC）
    APP->>MS: 09:55:38.273 Binder: start
    MS->>MS: AmNuPlayerDriver::start(state4) → kWhatStart → onStart → GenericSource:start
    MS->>MS: 09:55:38.282 Renderer 查询输出设备：streamType=3, devices=0x2（扬声器）
    MS->>MS: 09:55:38.292 instantiateDecoder（mime=audio/ffmpeg, codec-id=86017）
    MS->>C2: 09:55:38.305 CCodec allocate 组件 c2.amlogic.audio.decoder.ffmpeg（hwBinder）
    C2->>C2: C2VendorComponentStore 动态加载 libcodec2_aml_audio_decoder.so
    C2->>C2: Amlogic_C2AudioFFMPEGDecoder: onInit → initDecoder → AmFFmpegDecoder audio_decode_init
    MS->>C2: 配置协商：输入 audio/ffmpeg → 输出 audio/raw（16bit PCM）
    C2-->>MS: 09:55:38.386 输入/输出 DMABUF block pool 就绪（codec 配置共 99.8ms）
    MS->>C2: 09:55:38.395 First Input Buffer Queued（压缩 mp3 帧开始入队）
    C2-->>MS: 09:55:38.419 输出格式变更：audio/raw / 44.1kHz / 2ch / channel-mask 0xC
    C2-->>MS: 09:55:38.421 首帧 PCM 输出（timeUs:0）→ 进入 Renderer 队列
    end

    rect rgb(240, 228, 255)
    Note over MS, HAL: 【阶段4 AudioSink 打开与 PCM 数据流】09:55:38.4 起
    MS->>MS: openAudioSink：NON-offload 模式（offloadOnly=0），channelMask 0x3
    MS->>AF: 09:55:38.429 Binder: AudioTrack createTrack_l
    AF->>AF: 请求 flags 0x8(DEEP_BUFFER) 与输出 0x2(PRIMARY) 不匹配 → 自动降级
    AF->>AF: frameCount=18810（约 0.43s 深缓冲），notificationFrames=9405
    AF->>HAL: 打开 PCM_NORMAL 输出用例
    HAL->>HAL: 37.313 起 PCM_SYSTEM input port 已 unstandby 并 padding 填充
    HAL->>HAL: 内部 48kHz 混音（源 44.1k 重采样），ALSA latency 64ms（3072 帧），non-tunnel 模式
    MS->>MS: 09:55:38.444 First PCM write to AudioSink → media rendering started
    MS-->>APP: 09:55:38.444 MEDIA_STARTED → onPlaybackStateChanged(PLAYING)
    MS->>AF: PCM 持续写入（frameSize=4，18810 帧/周期）
    AF->>HAL: 混音后送 HAL（out_update_source_metadata usage:1 content_type:0）
    MS->>MS: 09:55:38.482 Audio PCM buffer First Full（耗时 37.7ms）
    HAL->>HAL: 每 5s jitter 检查：位置差≈时间差（偏差 0ms，播放时钟健康）
    APP->>MS: 每秒 getCurrentPosition（1001 → 2002 → 3002 ... ms）
    MS-->>APP: 返回当前位置（驱动侧按包 PTS 计算）
    end

    rect rgb(255, 238, 240)
    Note over USER, SUI: 【阶段5 音量加/减与系统按键音】09:55:47 / 09:55:59
    USER->>SYS: 09:55:47.749 遥控器 VOLUME_UP（key 115）
    SYS->>SYS: InputReader → MediaSessionService dispatchVolumeKeyEvent（优先当前播放会话）
    SYS->>SYS: AudioService adjustStreamVolume(stream=3, dir=+1) → 音量组 music Index 51
    SUI->>MS: 09:55:47.974 systemui 新建 MediaPlayer → 独立 AmNuPlayer #1（clientPid 1033）
    MS->>EXT: 按键音 mp3（13518 字节/477ms）走同一 ffmpeg 解封装链路
    MS->>C2: 走同一 C2 ffmpeg 解码链路
    MS->>AF: 走同一 AudioSink 输出，与主播放混音
    EXT-->>MS: 48.107 AVERROR_EOF → queue EOS → drainEos → saw output EOS
    MS->>MS: audio sink stop（48.182），#1 挂起待用
    USER->>SYS: 09:55:59.003 VOLUME_DOWN（key 114）→ dir=-1 → 音量组 music Index 50
    SYS->>SUI: 系统音量 UI 更新（volume=51 → 50）
    SUI->>MS: 59.155 reset #1（释放 ffmpeg 解码器、AmFFmpegExtractor、Renderer）
    SUI->>MS: 59.173 systemui 再建 AmNuPlayer #2 播放同一按键音（EOS 后同样 reset）
    end

    rect rgb(235, 245, 238)
    Note over USER, EXT: 【阶段6 快进（Seek）】09:56:18 / 09:56:19 / 09:56:31
    USER->>APP: 09:56:18.977 拖动进度条 → MoviePlayer.seekToTimeBarPosition
    APP->>MS: seekTo(42704ms) → Binder: AmNuPlayerDriver::seekTo(mode=3 SEEK_CLOSEST)
    MS->>MS: kWhatSeek → Decoder doFlush（三段式 flush 在途缓冲）
    MS->>MS: Renderer clearAnchorTime + flushing audio + audio sink pause-flush-stop
    MS->>EXT: 19.030 ffmpeg Seek：字节偏移 602638848（seekFlag:8 按帧对齐）
    EXT-->>MS: 新位置数据包 + queueDiscontinuity（时间轴不连续标记）
    MS->>MS: doSeek mAudioLastDequeueTimeUs=42704000 → performResumeDecoders
    MS->>C2: 19.033 重新喂流 → C2 组件 onFlush_sm 后继续解码
    MS-->>APP: 19.032 MEDIA_SEEK_COMPLETE → onSeekComplete
    APP->>MS: 19.034 App 自动 start() 恢复播放
    C2-->>MS: 19.044 首帧 timeUs:42710204（帧对齐，比目标晚 6.2ms）
    MS->>MS: First PCM write → setStartingTimeMedia 42736326 重锚时间轴
    MS->>MS: 19.045 media rendering started（续播完成，全程约 68ms）
    Note over USER, MS: 19.393 第二次快进 seekTo(50537ms)：同链路（ffmpeg 偏移 713178144，帧偏差 +9.9ms）
    Note over USER, MS: 31.626 第三次快进 seekTo(111941ms)：同链路（ffmpeg 偏移 1579711392，帧偏差 +19.8ms）
    end
```

### 各阶段关键处理逻辑解读

1. **触发层（2573→632→2968）**：FileActivity 只负责定位文件并转发 `ACTION_VIEW` Intent，真正播放由 AM Player 完成，进程冷启动由 AMS 经 Zygote fork（[play_local_audio_file.log:204](play_local_audio_file.log#L204)）。
2. **App 层（2968）**：`MediaPlayerExt` 通过 vendor framework 扩展标准 MediaPlayer（getMediaInfo 经 invoke 下发到 native，[log:325](play_local_audio_file.log#L325)）；`start()` 前先 `requestAudioFocus`，由 system_server 的 MediaFocusControl 仲裁（[log:639](play_local_audio_file.log#L639)）。
3. **mediaserver（537）**：Amlogic 用 `AmlogicNuPlayerFactoryInit` 工厂把标准 NuPlayer 整体替换为 **AmNuPlayer**（[log:341](play_local_audio_file.log#L341)）；GenericSource 在本地用 FileSource(fd=10) 读文件，包缓存到 AmAnotherPacketSource（本地播放上限 500 包/2MB）；每次创建先探测 OMX，失败后走 Codec2（[log:368-369](play_local_audio_file.log#L368-L369)）。
4. **media.extractor（535）**：`AmFFmpegExtractor` 白名单+置信度 0.8 嗅探识别 mp3（[log:403-405](play_local_audio_file.log#L403-L405)），输出 codec-id 86017(0x15001)；seek 按时间换算字节偏移 + `seekFlag:8`（AVSEEK_FLAG_FRAME）按帧对齐（[log:2553](play_local_audio_file.log#L2553)）。
5. **Codec2 HAL（433）**：C2 组件 `c2.amlogic.audio.decoder.ffmpeg` 由 C2VendorComponentStore 动态加载 `libcodec2_aml_audio_decoder.so`（[log:827-830](play_local_audio_file.log#L827-L830)），内部是 ffmpeg 软解（mp3float）；输出 `audio/raw` 16bit PCM；缓冲池走 DMABUF heap（[log:1004-1010](play_local_audio_file.log#L1004-L1010)）；MediaCodec 异步回调模式。
6. **Renderer（537 内）**：`openAudioSink` 为 **NON-offload** 模式（PCM 回灌路径，[log:1054-1056](play_local_audio_file.log#L1054-L1056)）；用 `setStartingTimeMedia` 锚定 PTS 起点，纯音频场景以音频为主时钟（AV sync info: Audio on Header）；seek 时负责 clearAnchorTime + audio sink pause-flush-stop（[log:2526-2550](play_local_audio_file.log#L2526-L2550)）。
7. **AudioFlinger（475）**：`createTrack_l` 收到 flags 0x8(DEEP_BUFFER) 与 PRIMARY 输出 0x2 不匹配的警告后自动降级（[log:1061](play_local_audio_file.log#L1061)），创建 18810 帧深缓冲音轨。
8. **音频 HAL（423）**：`PCM_NORMAL` usecase + `PCM_SYSTEM` input port，内部统一 48kHz 混音（源 44.1k 由 HAL 重采样），ALSA latency 64ms（3072 帧），`non tunnel` PCM 模式；运行期每 5s 打印 position/jitter 健康检查（[log:153-168](play_local_audio_file.log#L153-L168), [log:1143-1145](play_local_audio_file.log#L1143-L1145)）。
9. **音量链路**：音量键由 MediaSessionService 优先派发给活跃播放会话，AudioService 调整 stream=3（MUSIC）音量组索引（51↔50）；systemui 的按键音每次独立新建 MediaPlayer，走**与主播放完全相同的** AmNuPlayer → AmFFmpegExtractor → C2 → AudioFlinger 链路，播完 EOS 后 reset 释放（[log:1256](play_local_audio_file.log#L1256), [log:1820-1866](play_local_audio_file.log#L1820-L1866)）。
10. **Seek 链路**：`mode=3`（SEEK_CLOSEST）→ Decoder/Renderer 双 flush + audio sink 先停后启 → ffmpeg 字节级 seek → discontinuity 标记让渲染器重锚时间轴 → seek complete 后 App 自动 start 续播；三次快进的首帧时间偏差 6.2 / 9.9 / 19.8ms，均为 mp3 帧对齐粒度。

### 关键耗时（日志自带 KPI 打点）

| 环节 | 耗时 | 日志位置 |
| --- | --- | --- |
| 点击 → 首次出声 | ≈1.2s（其中 App 冷启动约 1s） | 37.243 → 38.444 |
| AmNuPlayer 创建 → 出声（引擎侧） | ≈608ms | 37.836 → 38.444 |
| ffmpeg 流信息探测 | 16.7ms | [log:457](play_local_audio_file.log#L457) |
| Codec2 组件配置（含缓冲池） | 99.8ms | [log:1020](play_local_audio_file.log#L1020) |
| 首缓冲填满（18810 帧） | 37.7ms | [log:1098](play_local_audio_file.log#L1098) |
| 单次快进（端到端） | 58 ~ 68ms | 三次 seek |

## 时序图（图片版，可缩放查看）

> 与上方 mermaid 代码内容一致，已渲染为高清图片（7152×8127），点击可放大缩放查看。

![播放 U 盘音频文件完整时序图](audio_play_sequence.png)
