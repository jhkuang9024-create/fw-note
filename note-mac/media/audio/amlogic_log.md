# Amlogic audio

## 日志

### 阶段 0：触发
```
用户点击 U 盘里的《华晨宇 - 地球之盐.mp3》，文件管理器打印路径
08-15 09:55:37.308  2573  2573 D com.starnet.filemanager.activity.FileActivity: startPlayerAction: audio path = /storage/6A6F-168B/本地音频/mp3/华晨宇 - 地球之盐.mp3

通过 ACTION_VIEW Intent 拉起 Amlogic 自带播放器 com.droidlogic.exoplayer2.demo / MoviePlayer
08-15 09:55:37.311   952  1361 E jarvis_ActivityController: activityStarting:intent = Intent { act=android.intent.action.VIEW cmp=com.droidlogic.exoplayer2.demo/com.droidlogic.videoplayer.MoviePlayer }, pkg=com.droidlogic.exoplayer2.demo

Zygote fork 新进程 pid=2968 冷启动 App
08-15 09:55:37.363   410   410 D Zygote  : Forked child process 2968
08-15 09:55:37.364   632   662 I ActivityManager: Start proc 2968:com.droidlogic.exoplayer2.demo/u0a32 for next-top-activity {com.droidlogic.exoplayer2.demo/com.droidlogic.videoplayer.MoviePlayer}

```

### 阶段 1：App 层
```
MoviePlayer 内部封装 MyMediaPlayer → android.media.MediaPlayer，并加载 Amlogic 的 MediaPlayerExt（vendor framework jar，用 invoke 扩展底层能力)
08-15 09:55:37.808  2968  2968 I MediaPlayerExt: [MediaPlayerExt]mIBinderService:android.os.BinderProxy@21a5276

判定本地文件走 file 路径
08-15 09:55:37.813  2968  2968 I MyMediaPlayer_MoviePlayer: [playFile]resume mode:true,path:/storage/6A6F-168B/本地音频/mp3/华晨宇 - 地球之盐.mp3
08-15 09:55:37.813   952  2213 E jarvis_ : lockScreenState  mode = false , state = 0
08-15 09:55:37.813   952  2213 I jarvis_LxAgentApiClient: <asyncCallAgent> method=lingxi_activity_life
08-15 09:55:37.813  2968  2968 I MyMediaPlayer_MoviePlayer: no need to show confirm dialog
08-15 09:55:37.813  2968  2968 I MyMediaPlayer_MoviePlayer: [setVideoPath, path:]/storage/6A6F-168B/本地音频/mp3/华晨宇 - 地球之盐.mp3
08-15 09:55:37.813   952  1082 I jarvis_LxAgentApiClient: <callAgent> method=lingxi_activity_life, params=Bundle[{pkg=com.droidlogic.exoplayer2.demo, life=activityStarting, class=com.droidlogic.videoplayer.MoviePlayer}]
08-15 09:55:37.813  2968  2968 I MyMediaPlayer_MoviePlayer: [setVideoURIAsync] uri:null, path:/storage/6A6F-168B/本地音频/mp3/华晨宇 - 地球之盐.mp3

申请音频焦点
08-15 09:55:38.270   632  1631 I MediaFocusControl: requestAudioFocus() from uid/pid 10032/2968 AA=USAGE_MEDIA/CONTENT_TYPE_MUSIC clientId=android.media.AudioManager@4c3fb2fcom.droidlogic.videoplayer.MyMediaPlayer$1@354753c callingPack=com.droidlogic.exoplayer2.demo req=1 flags=0x0 sdk=36
```

### 阶段 2：Binder 跨进程 → MediaPlayerService
**深度定制的AmNuPlayer替代了标准的NuPlayer**
```
创建的是 Amlogic 深度定制的 AmNuPlayer（V5.1.206，IPTV 分支版本，带 MessageMonitor/AmlPlayerTimer 等自研设施）
08-15 09:55:37.836   537   978 V AmlogicNuPlayerFactoryInit:  create AmNuPlayer
08-15 09:55:37.836   537   978 I NU-AmNuPlayerDriver:  ReCalculatePositionRatio set 0.500000
08-15 09:55:37.836   537   978 I AmlPlayerTimer: AmlPlayerTimer
08-15 09:55:37.836   537   978 I MessageMonitor: NU-AmNuPlayer MessageMonitor mDebugLevel 0, poll message us 30000000
08-15 09:55:37.836   537   978 I NU-AmNuPlayer: [#0] mPlayerInstanceId = 0
08-15 09:55:37.837   537   978 D AmlPlayerTimer: func:_timer_create  create timer.0 success.
08-15 09:55:37.837   537   978 D NU-AmNuPlayer: Enable QOS : 0

每次创建都会先探测 OMX，这台设备没有 OMX，走的是 Codec2 
08-15 09:55:37.839   224   224 I hwservicemanager: getTransport: Cannot find entry android.hardware.media.omx@1.0::IOmx/default in either framework or device VINTF manifest.
08-15 09:55:37.839   537   978 D MediaPlayerService: OMX service is not available

08-15 09:55:37.847   537   978 I NU-AmNuPlayer: [#0] setDataSourceAsync fd 9/0/5110837
```

### 阶段 3：解封装（MediaExtractor被换成ffmpeg）
```
08-15 09:55:37.886   537  3009 D NU-GenericSource: FileSource local use ffmpeg extractor
08-15 09:55:37.886   537  3009 I NU-GenericSource: FFmpegFileSource : fd=10 (/storage/6A6F-168B/本地音频/mp3/华晨宇 - 地球之盐.mp3), offset=0, length=5110837

AmFFmpegExtractor 在 mediaserver 内（pid 535）完成嗅探: 白名单+置信度判定
08-15 09:55:37.889   535   535 I AmFFmpegExtractor: all use ffmpegextractor
08-15 09:55:37.889   535   535 I AmFFmpegExtractor: is in white list : 1
08-15 09:55:37.889   535   535 I AmFFmpegExtractor: SniffAmFFmpeg  ok, confidence = 0.800000

解析出媒体信息：mp3、时长 319.4s、44.1kHz、双声道、128kbps、codec_id 86017(0x15001=MP3)、Mime:audio/ffmpeg
08-15 09:55:37.902   535   535 D AmFFmpegExtractor: fileSize:5110837, ff_ctx_bit_rate:128011, bitrateDiff:11, bitRateSum:128000, duration:(319399184,319427312)
08-15 09:55:37.902   535   535 I AmFFmpegExtractor: isMusic major_brand:null
08-15 09:55:37.902   535   535 I AmFFmpegExtractor: isMusic mp3 is music
08-15 09:55:37.902   535   535 I AmFFmpegExtractor: Stream 1(1) [audio/ffmpeg][codecid:0x15001(0x15001)] found.
08-15 09:55:37.902   535   535 I AmFFmpegExtractor:  [AmFFmpegSource 711]startTimeUs:0, seekable:1
08-15 09:55:37.902   535   535 I AmFFmpegExtractor:  [init 744](0xee0c0d90)
08-15 09:55:37.902   535   535 E AmFFmpegExtractor: mAudioMuxRead 2
08-15 09:55:37.902   535   535 I AmFFmpegExtractor: init kKeyDuration = 319399184
08-15 09:55:37.902   535   535 I AmFFmpegExtractor: audio:demux: fmt = 8
08-15 09:55:37.902   535   535 I AmFFmpegExtractor: audio:demux: channels = 2
08-15 09:55:37.902   535   535 I AmFFmpegExtractor: audio:demux: sample_rate = 44100
08-15 09:55:37.902   535   535 I AmFFmpegExtractor: audio:demux: bit_rate = 128000
08-15 09:55:37.902   535   535 I AmFFmpegExtractor: audio:demux: codec_id = 86017
08-15 09:55:37.902   535   535 I AmFFmpegExtractor: audio:demux: stream startTimeUs = 0
08-15 09:55:37.902   535   535 I AmFFmpegExtractor: audio:demux: block_align = 0
08-15 09:55:37.902   535   535 E AmFFmpegExtractor: kKeyMediaLanguage audio lang null
08-15 09:55:37.903   535   535 I AmFFmpegExtractor: Mime:audio/ffmpeg

countTracks() = 1（纯音频单轨），ffmpegHasAudio:1 / ffmpegHasVideo:0
08-15 09:55:37.904   537  3009 I NU-GenericSource: initFromDataSource, mExtractor->countTracks() = 1
08-15 09:55:37.904   537  3009 I NU-GenericSource: amlogic private metadata ffmpegHasAudio
08-15 09:55:37.904   537  3009 I NU-GenericSource: amlogic private metadata ffmpegHasVideo
08-15 09:55:37.904   537  3009 I NU-GenericSource: amlogic private metadata mSeekable:1
08-15 09:55:37.904   537  3009 I NU-GenericSource: ffmpegHasAudio : 1, ffmpegHasVideo : 0

准备好后回调：notifyPrepared → notifyPrepareCompleted → notifyListener(1,...)（MEDIA_PREPARED），App 收到 onPrepared
08-15 09:55:37.920   537  3009 I NU-GenericSource: notifyPrepared 2560
08-15 09:55:37.920   537  3003 I NU-AmNuPlayer: [#0] notifyPrepareCompleted
08-15 09:55:37.920   537  3003 D NU-AmNuPlayerDriver: [#0] notifyListener_l(0xea640230), (1, 0, 0, -1), loop setting(0, 0)
```

### 阶段 4：解码（Codec2/OMX，日志确认是厂商 C2 组件 + ffmpeg 软解）
start流程：AmNuPlayerDriver::start (state 4) → kWhatStart → onStart → GenericSource::start ，然后 new Renderer + instantiateDecoder
```
08-15 09:55:38.273   537   978 D NU-AmNuPlayerDriver: [#0] start(0xea640230), state is 4, eos is 0
08-15 09:55:38.273  2968  2968 I MoviePlayer: onPlaybackStateChanged:4
08-15 09:55:38.274   537  3003 I NU-AmNuPlayer: [#0] kWhatStart
08-15 09:55:38.274   537  3003 I NU-AmNuPlayer: [#0] onStart: mCrypto: 0x0 (0)
08-15 09:55:38.274   537  3003 I NU-GenericSource: start

Codec2 客户端向 C2 服务申请 Amlogic 的 ffmpeg 音频解码组件
08-15 09:55:38.305   537  3014 D CCodec  : allocate(c2.amlogic.audio.decoder.ffmpeg)

C2 服务在独立进程（pid 433，通过 hwbinder 通信，日志里有 hw-BpHwBinder）：加载 libcodec2_aml_audio_decoder.so，CreateC2AudioDecoderFFMPEGFactory 创建组件
08-15 09:55:38.308   433   935 I C2VendorComponentStore: C2VendorComponentStore::createComponent name c2.amlogic.audio.decoder.ffmpeg
08-15 09:55:38.309   433   935 I C2VendorComponentStore: findComponent
08-15 09:55:38.309   433   935 I C2VendorComponentStore: C2VendorComponentStore::createComponent fetchModule
08-15 09:55:38.309   433   935 I C2VendorComponentStore: localModule libpath libcodec2_aml_audio_decoder.so 54
08-15 09:55:38.309   433   935 V C2VendorComponentStore: Loading dll
08-15 09:55:38.309   433   935 I C2VendorComponentStore: in init ===> start codec:54 secure:0 isAudio:1
08-15 09:55:38.309   433   935 V C2VendorComponentStore: init  libPath:libcodec2_aml_audio_decoder.so --> isAudio:1, createFactoryName:CreateC2AudioDecoderFFMPEGFactory
08-15 09:55:38.309   433   935 V Amlogic_C2AudioFFMPEGDecoder: create component FFMPEG 

配置协商：输入 audio/ffmpeg → 输出 audio/raw，android._config-pcm-encoding = 2（16bit PCM），采样率 44100，码率从 64000 修正为 128000，声道 1→2
08-15 09:55:38.345   537  3014 I CCodecConfig: query failed after returning 8 values (BAD_INDEX)
08-15 09:55:38.345   537  3014 D CCodecConfig: c2 config diff is Dict {
08-15 09:55:38.345   537  3014 D CCodecConfig:   c2::u32 coded.bitrate.value = 64000
08-15 09:55:38.345   537  3014 D CCodecConfig:   c2::u32 input.buffers.max-size.value = 2097152
08-15 09:55:38.345   537  3014 D CCodecConfig:   string input.media-type.value = "audio/ffmpeg"
08-15 09:55:38.345   537  3014 D CCodecConfig:   string output.media-type.value = "audio/raw"
08-15 09:55:38.345   537  3014 D CCodecConfig:   c2::u32 raw.channel-count.value = 1
08-15 09:55:38.345   537  3014 D CCodecConfig:   c2::u32 raw.channel-mask.value = 0
08-15 09:55:38.345   537  3014 D CCodecConfig:   c2::u32 raw.max-channel-count.value = 8
08-15 09:55:38.345   537  3014 D CCodecConfig:   c2::u32 raw.sample-rate.value = 44100
08-15 09:55:38.345   537  3014 D CCodecConfig: }
08-15 09:55:38.354   537  3014 D CCodec  :   int32_t android._config-pcm-encoding = 2

ffmpeg 解码器初始化：onInit → initDecoder → setUpAudioDecoder_l → AmAudioCodec / AmFFmpegDecoder::CreateCodec audio_decode_init ret:0
08-15 09:55:38.355   433  3019 I Amlogic_C2AudioFFMPEGDecoder: onInit() 453
08-15 09:55:38.355   433  3019 I Amlogic_C2AudioFFMPEGDecoder: initDecoder 511
08-15 09:55:38.355   433  3019 I Amlogic_C2AudioFFMPEGDecoder: setUp 421
08-15 09:55:38.355   433  3019 I Amlogic_C2AudioFFMPEGDecoder: vendor.media.c2.loglevels is not set used def = 1
08-15 09:55:38.355   433  3019 I Amlogic_C2AudioFFMPEGDecoder: setUpAudioDecoder_l  debug_print:0, debug_dump:0,  gloglevel:1
08-15 09:55:38.355   433  3019 I Amlogic_C2AudioFFMPEGDecoder: mAInfo codec_id:(0x15001 86017) blockalign:0 bitspersample:1 channelCount:2 SampleRate:44100 BitRate:128000
08-15 09:55:38.355   433  3019 I AmAudioCodec: AmAudioCodec 54 
08-15 09:55:38.357   433  3019 I AmFFmpegDecoder: AmFFmpegCodec::CreateCodec audio_decode_init  audio/ffmpeg
08-15 09:55:38.359  1019  1019 I MediaPlayerList: sendMediaUpdate: Creating a one item queue for a player with no queue
08-15 09:55:38.359  1019  1019 D MediaPlayerList: sendMediaUpdate state=PlaybackState {state=PLAYING(3), position=84, buffered position=0, speed=1.0, updated=586058, actions=823, custom actions=[], active item id=-1, error=null}
08-15 09:55:38.359  1019  1019 D AvrcpTargetJni: sendMediaUpdateNative
08-15 09:55:38.360  1019  1019 I bt_stack: [INFO:avrcp_service.cc(515)] virtual void bluetooth::avrcp::AvrcpService::SendMediaUpdate(bool, bool, bool) track_changed=1 :  play_state=1 :  queue=1
08-15 09:55:38.361  1019  1019 V MediaPlayerList: onActiveSessionsChanged: number of controllers: 1
08-15 09:55:38.362  2968  2994 E OpenGLRenderer: Unable to match the desired swap behavior.
08-15 09:55:38.364  1019  1019 D MediaPlayerList: onActiveSessionsChanged: controller: com.droidlogic.exoplayer2.demo
08-15 09:55:38.383     0     0 I [0 T223   ..] servicemanager: Found android.hardware.graphics.allocator.IAllocator/default in device VINTF manifest.
08-15 09:55:38.384     0     0 I [3 T223   ..] servicemanager: Found android.hardware.graphics.allocator.IAllocator/default in device VINTF manifest.
08-15 09:55:38.370  1019  1019 I MediaPlayerList: sendMediaUpdate: Creating a one item queue for a player with no queue
08-15 09:55:38.370  1019  1019 D MediaPlayerList: sendMediaUpdate state=PlaybackState {state=PLAYING(3), position=96, buffered position=0, speed=1.0, updated=586070, actions=823, custom actions=[], active item id=-1, error=null}
08-15 09:55:38.370  1019  1019 W MediaControlGattService: Feature PLAYER_NAME(BIT 1) support: true
08-15 09:55:38.370  1019  1019 W MediaControlGattService: Feature PLAYER_NAME(BIT 1) support: true
08-15 09:55:38.370  1019  1019 W MediaControlGattService: Feature MEDIA_STATE(BIT 17) support: true
08-15 09:55:38.371  1019  1019 V MediaPlayerList: onActiveSessionsChanged: number of controllers: 1
08-15 09:55:38.371  1019  1019 D MediaPlayerList: onActiveSessionsChanged: controller: com.droidlogic.exoplayer2.demo
08-15 09:55:38.374  1019  1019 I MediaPlayerList: sendMediaUpdate: Creating a one item queue for a player with no queue
08-15 09:55:38.374  1019  1019 D MediaPlayerList: sendMediaUpdate state=PlaybackState {state=PLAYING(3), position=100, buffered position=0, speed=1.0, updated=586074, actions=823, custom actions=[], active item id=-1, error=null}
08-15 09:55:38.374  1019  1019 D AvrcpTargetJni: sendMediaUpdateNative
08-15 09:55:38.374  1019  1019 I bt_stack: [INFO:avrcp_service.cc(515)] virtual void bluetooth::avrcp::AvrcpService::SendMediaUpdate(bool, bool, bool) track_changed=0 :  play_state=0 :  queue=0
08-15 09:55:38.377  2968  2968 D MoviePlayer: sendDolbyVersionLogoMsg
08-15 09:55:38.377   632  1391 W ServiceManagerCppClient: Permission failure: android.permission.ROTATE_SURFACE_FLINGER from uid=10032 pid=0
08-15 09:55:38.377   632  1391 D PermissionCache: checking android.permission.ROTATE_SURFACE_FLINGER for uid=10032 => denied (204 us)
08-15 09:55:38.377   632  1391 W ServiceManagerCppClient: Permission failure: android.permission.INTERNAL_SYSTEM_WINDOW from uid=10032 pid=0
08-15 09:55:38.377   632  1391 D PermissionCache: checking android.permission.INTERNAL_SYSTEM_WINDOW for uid=10032 => denied (56 us)
08-15 09:55:38.377   632  1391 W ServiceManagerCppClient: Permission failure: android.permission.ACCESS_SURFACE_FLINGER from uid=10032 pid=0
08-15 09:55:38.378   632  1391 D PermissionCache: checking android.permission.ACCESS_SURFACE_FLINGER for uid=10032 => denied (49 us)
08-15 09:55:38.379   433  3019 I AmAudioCodec: ffmpeg decoder NeedCheckFrame 0  id =0x15001
08-15 09:55:38.379   433  3019 I AmFFmpegDecoder: AmFFmpegCodec::CreateCodec audio_decode_init  audio/ffmpeg ret:0
08-15 09:55:38.379   433  3019 I Amlogic_C2AudioFFMPEGDecoder: ffmpeg audio_decode_init return 0
08-15 09:55:38.379   433  3019 I Amlogic_C2AudioFFMPEGDecoder: C2AudioFFMPEGDecoder setUp done

缓冲池：输入/输出 block pool 用 DMABUF heap（system）建立
08-15 09:55:38.380   537  3014 D C2Store : debug.c2.use_dmabufheaps set, forcing DMABUF Heaps
08-15 09:55:38.380   537  3014 D C2Store : Using DMABUF Heaps
08-15 09:55:38.383   537  3014 D CCodecBufferChannel: [c2.amlogic.audio.decoder.ffmpeg#916] Created input block pool with allocatorID 16 => poolID 17 - OK (0)
08-15 09:55:38.384   433   898 D C2Store : debug.c2.use_dmabufheaps set, forcing DMABUF Heaps
08-15 09:55:38.384   433   898 D C2Store : Using DMABUF Heaps
08-15 09:55:38.384   433   898 I DMABUFHEAPS: Using : Non-legacy ION heaps
08-15 09:55:38.385   537  3014 I CCodecBufferChannel: [c2.amlogic.audio.decoder.ffmpeg#916] Created output block pool with allocatorID 16 => poolID 17 - OK

数据开始流动：First CB_INPUT_AVAILABLE → First Input Buffer Queued → 输出格式变更(audio/raw, channel-mask=0xC, 44.1kHz) → First CB_OUTPUT_AVAILABLE → First Out Buffer Queued to Render, timeUs:0
08-15 09:55:38.393   537  3013 I NU-AmNuPlayerDecoder: [#0] [c2.amlogic.audio.decoder.ffmpeg] First CB_INPUT_AVAILABLE received.
08-15 09:55:38.395   537  3015 I NU-AmNuPlayerDecoder: [#0] [c2.amlogic.audio.decoder.ffmpeg] First Input Buffer Queued to Codec
08-15 09:55:38.395   433  3019 I Amlogic_C2AudioFFMPEGDecoder: process Invalid data frame
08-15 09:55:38.401   632   645 W ActivityTaskManager: Request to remove task ignored for non-existent task 15
08-15 09:55:38.409   632   645 D CompatibilityChangeReporter: Compat change id reported: 214016041; UID 10032; state: ENABLED
08-15 09:55:38.409   632   645 I ImeTracker: com.droidlogic.exoplayer2.demo:56096b7c: onRequestHide at ORIGIN_SERVER_HIDE_INPUT reason HIDE_UNSPECIFIED_WINDOW
08-15 09:55:38.409   632   645 I ImeTracker: com.droidlogic.exoplayer2.demo:56096b7c: onCancelled at PHASE_SERVER_SHOULD_HIDE
08-15 09:55:38.410   632   652 I ActivityTaskManager: Displayed com.droidlogic.exoplayer2.demo/com.droidlogic.videoplayer.MoviePlayer for user 0: +1s76ms
08-15 09:55:38.413   433  3019 I Amlogic_C2AudioFFMPEGDecoder: Reconfiguring decoder: 0->44100 Hz, 0->2 channels
08-15 09:55:38.414   433  3019 I C2VendorComponentStore: setDmaBufUsage 5
08-15 09:55:38.414   433  3019 I DMABUFHEAPS: Using DMA-BUF heap named: system
08-15 09:55:38.418   537  3014 D CCodecConfig: c2 config diff is   c2::u32 raw.channel-mask.value = 12
08-15 09:55:38.419   537  3014 D CCodecBuffers: [c2.amlogic.audio.decoder.ffmpeg#916:Output[N]] popFromStashAndRegister: at 0us, output format changed to AMessage(what = 0x00000000) = {
08-15 09:55:38.419   537  3014 D CCodecBuffers:   int32_t aac-max-output-channel_count = 8
08-15 09:55:38.419   537  3014 D CCodecBuffers:   int32_t channel-count = 2
08-15 09:55:38.419   537  3014 D CCodecBuffers:   int32_t channel-mask = 12
08-15 09:55:38.419   537  3014 D CCodecBuffers:   int32_t max-output-channel-count = 8
08-15 09:55:38.419   537  3014 D CCodecBuffers:   string mime = "audio/raw"
08-15 09:55:38.419   537  3014 D CCodecBuffers:   int32_t sample-rate = 44100
08-15 09:55:38.419   537  3014 D CCodecBuffers:   int32_t android._config-pcm-encoding = 2
08-15 09:55:38.419   537  3014 D CCodecBuffers: }
08-15 09:55:38.420   433   935 I C2VendorComponentStore: setDmaBufUsage 5
08-15 09:55:38.421   537  3013 I NU-AmNuPlayerDecoder: [#0] handleOutputFormatChange   format: AMessage(what = 0x00000000) = {
08-15 09:55:38.421   537  3013 I NU-AmNuPlayerDecoder:   int32_t aac-max-output-channel_count = 8
08-15 09:55:38.421   537  3013 I NU-AmNuPlayerDecoder:   int32_t channel-count = 2
08-15 09:55:38.421   537  3013 I NU-AmNuPlayerDecoder:   int32_t channel-mask = 12
08-15 09:55:38.421   537  3013 I NU-AmNuPlayerDecoder:   int32_t max-output-channel-count = 8
08-15 09:55:38.421   537  3013 I NU-AmNuPlayerDecoder:   string mime = "audio/raw"
08-15 09:55:38.421   537  3013 I NU-AmNuPlayerDecoder:   int32_t sample-rate = 44100
08-15 09:55:38.421   537  3013 I NU-AmNuPlayerDecoder:   int32_t android._config-pcm-encoding = 2
08-15 09:55:38.421   537  3013 I NU-AmNuPlayerDecoder: }
08-15 09:55:38.421   537  3013 I NU-AmNuPlayerDecoder: [#0] [c2.amlogic.audio.decoder.ffmpeg] First CB_OUTPUT_AVAILABLE received. input buffer queued: 4
08-15 09:55:38.421   537  3013 I NU-AmNuPlayerDecoder: [#0] [c2.amlogic.audio.decoder.ffmpeg] First Out Buffer Queued to Render. timeUs:0
```

### 阶段 5：渲染输出（对应AudioFlinger，日志能看到更下层）

```
openAudioSink: offloadOnly(0) offloadingAudio(0) → NON-offload 模式（PCM 回灌路径，不是硬件 offload），channelMask 0x3
08-15 09:55:38.422   537  3012 V NU-AmNuPlayerRenderer: [#0] openAudioSink: offloadOnly(0) offloadingAudio(0)
08-15 09:55:38.422   537  3012 V NU-AmNuPlayerRenderer: [#0] openAudioSink channelMask:0x3
08-15 09:55:38.422   537  3012 V NU-AmNuPlayerRenderer: [#0] openAudioSink: open AudioSink in NON-offload mode

AudioTrack 在 audioserver（pid 475）创建：AudioFlinger: createTrack_l(): mismatch between requested flags (00000008) and output flags (00000002)（请求 DEEP_BUFFER，设备只有 PRIMARY 输出，降级处理）；notificationFrames 9405 for frameCount 18810（约 0.42s 的播放缓冲）
08-15 09:55:38.429   475   475 W AudioFlinger: createTrack_l(): mismatch between requested flags (00000008) and output flags (00000002)
08-15 09:55:38.430   423  1169 I audio_hw_hal_primary: [audiohal_get_latency:1344] io 13: out:0xe7943990 frames:3072 rate:48000 whole_latency:64 alsa_latency:64
08-15 09:55:38.430   475   475 D AudioFlinger: Client defaulted notificationFrames to 9405 for frameCount 18810

首笔 PCM 写入：First audio pcm date write to audiosink → media rendering started → 回调 (6,...) = MEDIA_STARTED
08-15 09:55:38.444   537  3012 I NU-AmNuPlayerRenderer: [#0] First audio pcm date write to audiosink.
08-15 09:55:38.444   537  3012 I NU-AmNuPlayerRenderer: [#0] setStartingTimeMedia 26122, mAudioPlayedOutDur 0
08-15 09:55:38.444   537  3003 I NU-AmNuPlayer: [#0] media rendering started
08-15 09:55:38.444   537  3003 I NU-AmNuPlayer: [#0] finishResume  mResumePending:0  
08-15 09:55:38.444   537  3003 D NU-AmNuPlayerDriver: [#0] notifyListener_l(0xea640230), (6, 0, 0, -1), loop setting(0, 0)

缓冲填满：Audio PCM buffer First Full, frameCount:18810, frameSize:4（18810 帧 × 22.676µs/帧 ≈ 0.43s）
08-15 09:55:38.482   537  3012 I NU-AmNuPlayerRenderer: [#0] Audio PCM buffer First Full. time pay:37748, frameCount:18810, mAudioSink->frameSize():4
08-15 09:55:38.482   537  3012 W NU-AmNuPlayerRenderer: [#0] postDrainAudioQueue long delay: 26658 (18810*0.022676), numFramesPendingPlayout:(18810-0)

Amlogic 音频 HAL（pid 423，audio.primary）内部：PCM_NORMAL usecase、PCM_SYSTEM input port、48kHz 混音（setPortConfig fmt 0x1 samplerate 48000，即 HAL 内部统一 48k）、whole_latency:64ms alsa_latency:64（3072 帧），stream 为 pcm / non tunnel 模式
08-15 09:55:37.313   423  1193 I audio_hw_hal_submixing: [usecase_change_validate_l_sm:1507] ++++ out:0xe7943990 continuous:0 dev masks:0, out masks:0, out usecase PCM_NORMAL
08-15 09:55:37.313   423  1193 I audio_hw_hal_submixing: [usecase_change_validate_l_sm:1514] io 13: out:0xe7943990 add usecase PCM_NORMAL, cnt 1
08-15 09:55:37.313   423  1193 D audio_hw_hal_submixing: [usecase_change_validate_l_sm:1527] io 13: out:0xe7943990 cur dev masks:0, add out usecase:PCM_NORMAL
08-15 09:55:37.313   423  1193 I audio_hw_hal_submixing: [usecase_change_validate_l_sm:1556] ---- out:0xe7943990 continuous:0 dev masks:0x1, out masks:0x1, out usecase PCM_NORMAL
08-15 09:55:37.313   423  1193 D audio_hw_hal_port: [setPortConfig:302] +++ch mask = 0x3, fmt 0x1, samplerate 48000
08-15 09:55:37.313   423  1193 D audio_hw_hal_port: [new_input_port:462] buf_frames:384,frame_size:4 ==> thunk_size:1536
08-15 09:55:37.313   423  1193 I audio_hw_hal_port: [get_input_port_type:122] samplerate:48000, flags:0x2, channel_cnt:2
08-15 09:55:37.313   423  1193 D audio_hw_hal_port: [new_input_port:499] inport:PCM_SYSTEM, buf:6144, direct:0, format:0x1, rate:48000, ch:2
08-15 09:55:37.313   423  1193 D audio_hw_hal_submixing: [mixer_get_available_inport_index:141] +inportsAvailMasks: 0xfe
08-15 09:55:37.313   423  1193 D audio_hw_hal_submixing: [mixer_get_available_inport_index:144] -inportsAvailMasks:0xfc, index 1
08-15 09:55:37.313   423  1193 I audio_hw_hal_submixing: [init_mixer_input_port:211] input port:PCM_SYSTEM, size 384 frames, frame_write_sum:890880
08-15 09:55:37.313   423  1193 I audio_hw_hal_port: [get_input_port_type:122] samplerate:48000, flags:0x2, channel_cnt:2
08-15 09:55:37.313   423  1193 I audio_hw_hal_submixing: [mixer_aux_buffer_write_sm:1351] io 13: out:0xe7943990 usecase:PCM_NORMAL standby to unstandby, input port:PCM_SYSTEM
08-15 09:55:37.313   423  1193 I audio_hw_hal_submixing: [mixer_aux_buffer_write_sm:1362] padding_bytes 12288
08-15 09:55:37.313   423  1193 I audio_hw_hal_submixing: [mixer_write_inport:340] input port:PCM_SYSTEM is active now
08-15 09:55:37.313   423  1193 I audio_hw_hal_submixing: [mixer_aux_buffer_write_sm:1362] padding_bytes 10752
08-15 09:55:37.396   423  1193 I audio_hw_hal_stream: aml_stream_out_info_print: out:0xe7943990, stream_type:pcm, sync_mode:non tunnel, input_size:3571712 bytes
08-15 09:55:37.396   423  1193 I audio_hw_hal_stream: aml_stream_out_info_print: last_time:559523 ms (sec:559, nsec:523199641), last_position:18509 ms (888448), cur_time:585091 ms (sec:585, nsec:91141903), cur_position:18560 ms (890880)
08-15 09:55:37.396   423  1193 I audio_hw_hal_stream: aml_stream_out_info_print: time_gap:25568 ms (thr:100 ms), position_gap:50 ms, delay:42 ms, jitter: Position gap is behind system time gap by 25518 ms (thr:100 ms)
08-15 09:55:37.476   423  1193 I audio_hw_hal_primary: [audiohal_get_latency:1344] io 13: out:0xe7943990 frames:3072 rate:48000 whole_latency:64 alsa_latency:64

运行期：每秒一次 getCurrentPosition（1001→2002→3002…）；HAL 侧周期打印 position/jitter 检查，Position gap is ahead of system time gap by 0 ms 说明播放时钟健康
08-15 09:55:39.554   537   978 I NU-AmNuPlayerDriver: [#0] [getCurrentPosition][752] position : 1001 msec
08-15 09:55:40.308  2968  2968 I MoviePlayer: [displayModeImpl]handleMessage
08-15 09:55:40.310  2968  2968 D MoviePlayer: isInSplitScreen:false
08-15 09:55:40.310  2968  2968 I MoviePlayer: [displayModeImpl]dispWidth:1280,dispHeight:800,mode:800x1280p60hz
08-15 09:55:40.310  2968  2968 I MoviePlayer: [displayModeImpl]videoNum <=0, return
08-15 09:55:40.365   632  1393 E TaskPersister: File error accessing recents directory (directory doesn't exist?).
08-15 09:55:40.555   537   978 I NU-AmNuPlayerDriver: [#0] [getCurrentPosition][752] position : 2002 msec
08-15 09:55:41.277  2968  2968 D UEventObserver: startObserving...
08-15 09:55:41.278  2968  2968 D UEventObserver: startObserving...
08-15 09:55:41.555   537   978 I NU-AmNuPlayerDriver: [#0] [getCurrentPosition][752] position : 3002 msec
08-15 09:55:42.412   423  1193 I audio_hw_hal_stream: aml_stream_out_info_print: out:0xe7943990, stream_type:pcm, sync_mode:non tunnel, input_size:4530176 bytes
08-15 09:55:42.412   423  1193 I audio_hw_hal_stream: aml_stream_out_info_print: last_time:590059 ms (sec:590, nsec:59143864), last_position:23477 ms (1126912), cur_time:590107 ms (sec:590, nsec:107139364), cur_position:23525 ms (1129216)
08-15 09:55:42.412   423  1193 I audio_hw_hal_stream: aml_stream_out_info_print: time_gap:48 ms (thr:100 ms), position_gap:48 ms, delay:69 ms, jitter: Position gap is ahead of system time gap by 0 ms (thr:100 ms)
08-15 09:55:42.816   537   978 I NU-AmNuPlayerDriver: [#0] [getCurrentPosition][752] position : 4263 msec
08-15 09:55:43.631   537  3014 D BufferPoolAccessor2.0: bufferpool2 0xe8602788 : 8(16777216 size) total buffers - 5(10485760 size) used buffers - 210/218 (recycle/alloc) - 8/432 (fetch/transfer)
08-15 09:55:43.634   433  3019 D BufferPoolAccessor2.0: bufferpool2 0xed943988 : 4(18432 size) total buffers - 1(4608 size) used buffers - 213/217 (recycle/alloc) - 5/216 (fetch/transfer)
08-15 09:55:43.817   537   978 I NU-AmNuPlayerDriver: [#0] [getCurrentPosition][752] position : 5264 msec
08-15 09:55:45.077   537   978 I NU-AmNuPlayerDriver: [#0] [getCurrentPosition][752] position : 6524 msec
08-15 09:55:46.079   537   978 I NU-AmNuPlayerDriver: [#0] [getCurrentPosition][752] position : 7526 msec
08-15 09:55:47.339   537   978 I NU-AmNuPlayerDriver: [#0] [getCurrentPosition][752] position : 8786 msec
08-15 09:55:42.412   423  1193 I audio_hw_hal_stream: aml_stream_out_info_print: out:0xe7943990, stream_type:pcm, sync_mode:non tunnel, input_size:4530176 bytes
08-15 09:55:42.412   423  1193 I audio_hw_hal_stream: aml_stream_out_info_print: last_time:590059 ms (sec:590, nsec:59143864), last_position:23477 ms (1126912), cur_time:590107 ms (sec:590, nsec:107139364), cur_position:23525 ms (1129216)
08-15 09:55:42.412   423  1193 I audio_hw_hal_stream: aml_stream_out_info_print: time_gap:48 ms (thr:100 ms), position_gap:48 ms, delay:69 ms, jitter: Position gap is ahead of system time gap by 0 ms (thr:100 ms)

A/V 同步：纯音频场景 AV sync info: Audio on Header，以音频 PTS 为锚
08-15 09:55:48.453   537  3012 I NU-AmNuPlayerRenderer: PTS: AV sync info:Audio on Header 
08-15 09:55:48.453   537  3012 I NU-AmNuPlayerRenderer: PTS:   SystemTimeStamp:9899651
08-15 09:55:48.453   537  3012 I NU-AmNuPlayerRenderer: PTS:   Last AudioTimeStamp:10396734
08-15 09:55:48.453   537  3012 I NU-AmNuPlayerRenderer: PTS:   Last VideoTimeStamp:-1
08-15 09:55:48.453   537  3012 I NU-AmNuPlayerRenderer: PTS:   AV diff:9899652 ,Ajump:0,Vjump:0
08-15 09:55:48.453   537  3012 I NU-AmNuPlayerRenderer: PTS:   mAjumpedNum:0, mAudioJumped till now=596153379
08-15 09:55:48.453   537  3012 I NU-AmNuPlayerRenderer: PTS:   mVjumpedNum:0, mVideoJumped till now=596153379
08-15 09:55:48.453   537  3012 I NU-AmNuPlayerRenderer: PTS:   mTotalAudioJumpedTimeUs:0, mTotalAudioJumpedTimeUs:0
08-15 09:55:48.453   537  3012 I NU-AmNuPlayerRenderer: PTS:   mLastVideoUs:-1
08-15 09:55:48.453   537  3012 I NU-AmNuPlayerRenderer: PTS:   mLastAudioUs:10422857
08-15 09:55:48.453   537  3012 I NU-AmNuPlayerRenderer: PTS:   mAudioDelay 0 mFormat 0x1 mMs12Enabled 0

```

### 阶段 6：音量加/减
音量加（keycode 115，KEYCODE_VOLUME_UP）：
```
内核上报按键 → InputReader → MediaSessionService: dispatchVolumeKeyEvent 把键事件优先派给正在播放的会话 → AudioService: adjustSuggestedStreamVolume() stream=3（MUSIC 流）→ adjustStreamVolume dir=1 → updateVolumeGroupIndex ... group music Index=51（音量组 51）
08-15 09:55:47.749     0     0 I         : [0 T2865  ..] input input1: key 115 down
08-15 09:55:47.731   632   734 D InputReader: getDisplayIdi -1
08-15 09:55:47.731   568   568 D XIRI_driver: send to xiri: {"cmd":"key","event":1,"keycode":115,"pid":1,"vid":1,"devnum":0}
08-15 09:55:47.730   568   568 I xiriservice_All: type=1400 audit(0.0:1506): avc:  denied  { ioctl } for  path="/dev/input/event1" dev="tmpfs" ino=426 ioctlcmd=0x4502 scontext=u:r:xiriservice_All:s0 tcontext=u:object_r:input_device:s0 tclass=chr_file permissive=1
08-15 09:55:47.738   632   653 D CompatibilityChangeReporter: Compat change id reported: 173031413; UID 1000; state: DISABLED
08-15 09:55:47.738   632   653 D CompatibilityChangeReporter: Compat change id reported: 173031413; UID 1000; state: ENABLED
08-15 09:55:47.738  1033  1033 I ndroid.systemui: type=1400 audit(0.0:1507): avc:  denied  { call } for  scontext=u:r:priv_app:s0:c512,c768 tcontext=u:r:zygote:s0 tclass=binder permissive=1 app=com.android.systemui
08-15 09:55:47.741   952   966 I jarvis_ : System call method methodId = increase_volume
08-15 09:55:47.741   952   966 I jarvis_binded: mmkv binded
08-15 09:55:47.769   952   966 E jarvis_ : [, , 0]:TipsRingDialogUtil hash82184497
08-15 09:55:47.771   632   645 D AudioService: adjustStreamVolume() stream=3, dir=0, flags=1, caller=com.android.systemui
08-15 09:55:47.773   632   645 D AudioService: forceUse=0 full:false
08-15 09:55:47.773   632   645 D AudioService: adjustSreamVolume playbackDeviceConditions=false tvConditions=false
08-15 09:55:47.774   423  1169 D AudioHalHardware: +getParameters(), key = hal_param_hal_control_vol_en
08-15 09:55:47.774   423  1169 D AudioHalHardware: -getParameters(), return ""
08-15 09:55:47.781   632   645 W WindowManager: com.android.systemui skip checkAddPermission for Window 2020
08-15 09:55:47.781   632   645 W WindowManager: [addWindow] title:
08-15 09:55:47.784   632   645 D CompatibilityChangeReporter: Compat change id reported: 168419799; UID 10010; state: DISABLED
08-15 09:55:47.784   632   645 D CompatibilityChangeReporter: Compat change id reported: 273564678; UID 10010; state: DISABLED
08-15 09:55:47.789   632  2007 D AudioService: Volume controller visible: true
08-15 09:55:47.798   632  2007 D CoreBackPreview: Window{f7e9b71 u0 com.android.systemui}: Setting back callback OnBackInvokedCallbackInfo{mCallback=android.window.IOnBackInvokedCallback$Stub$Proxy@7cdd1d7, mPriority=0, mIsAnimationCallback=false}
08-15 09:55:47.801  1033  1224 E OpenGLRenderer: Unable to match the desired swap behavior.
08-15 09:55:47.868   568   568 D XIRI_driver: send to xiri: {"cmd":"key","event":0,"keycode":115,"pid":1,"vid":1,"devnum":0}
08-15 09:55:47.868   632   734 D InputReader: getDisplayIdi -1
08-15 09:55:47.870   632   733 D WindowManager: handleVolumeKeyInArc KeyEvent { action=ACTION_DOWN, keyCode=KEYCODE_VOLUME_UP, scanCode=115, metaState=0, flags=0x8, repeatCount=0, eventTime=595431875000, downTime=595431875000, deviceId=2, source=0x101, displayId=-1 }
08-15 09:55:47.872   632   733 D WindowManager: handleVolumeKeyInArc KeyEvent { action=ACTION_UP, keyCode=KEYCODE_VOLUME_UP, scanCode=115, metaState=0, flags=0x8, repeatCount=0, eventTime=595568710000, downTime=595431875000, deviceId=2, source=0x101, displayId=-1 }
08-15 09:55:47.886     0     0 I         : [0 T2865  ..] input input1: key 115 up
08-15 09:55:47.873  1258  1258 D RemoteIME: keycode: 24, realAction: false
08-15 09:55:47.882   632  2007 D MediaSessionService: dispatchVolumeKeyEvent, pkg=com.droidlogic.exoplayer2.demo, opPkg=com.droidlogic.exoplayer2.demo, pid=2968, uid=10032, asSystem=true, event=KeyEvent { action=ACTION_DOWN, keyCode=KEYCODE_VOLUME_UP, scanCode=115, metaState=0, flags=0x8, repeatCount=0, eventTime=595431875000, downTime=595431875000, deviceId=2, source=0x101, displayId=-1 }, stream=-2147483648, musicOnly=false
08-15 09:55:47.883   632  2007 D MediaSessionService: Adjusting com.droidlogic.exoplayer2.demo/MoviePlayer (userId=0) by 1. flags=4113, suggestedStream=-2147483648, preferSuggestedStream=false
08-15 09:55:47.886  1258  1258 D RemoteIME: keycode: 24, realAction: true
08-15 09:55:47.887  2968  2968 D MoviePlayer: keycode:24 down is 0, action:1
08-15 09:55:47.893   632   811 D AudioService: adjustSuggestedStreamVolume() stream=3, flags=4113, caller=android, volControlStream=-1, userSelect=false
08-15 09:55:47.893   632   645 D MediaSessionService: dispatchVolumeKeyEvent, pkg=com.droidlogic.exoplayer2.demo, opPkg=com.droidlogic.exoplayer2.demo, pid=2968, uid=10032, asSystem=true, event=KeyEvent { action=ACTION_UP, keyCode=KEYCODE_VOLUME_UP, scanCode=115, metaState=0, flags=0x8, repeatCount=0, eventTime=595568710000, downTime=595431875000, deviceId=2, source=0x101, displayId=-1 }, stream=-2147483648, musicOnly=false
08-15 09:55:47.894   632   645 D MediaSessionService: Adjusting com.droidlogic.exoplayer2.demo/MoviePlayer (userId=0) by 0. flags=4116, suggestedStream=-2147483648, preferSuggestedStream=false
08-15 09:55:47.912   632   811 V AudioService: getActiveStreamType: Returning suggested type 3
08-15 09:55:47.912   632   811 D AudioService: adjustStreamVolume() stream=3, dir=1, flags=4113, caller=android
08-15 09:55:47.914   632   811 D AudioService: updateVolumeGroupIndex for stream 11, muted=false, device=2, index=510, group system Muted=false, Index=51, forceMuteState=false
08-15 09:55:47.914   632   811 W BroadcastLoopers: Found previously unknown looper Thread[SessionRecordThread,5,main]
08-15 09:55:47.917  1019  1019 D HeadsetClientService: Volume changed for stream: 11
08-15 09:55:47.919  1033  1033 E QuickSettings: action:android.media.VOLUME_CHANGED_ACTION
08-15 09:55:47.922   952   952 E jarvis_action===: android.media.VOLUME_CHANGED_ACTION
08-15 09:55:47.922   952   952 E jarvis_ : jarvis_:onVolumeChange== 11 
08-15 09:55:47.922  1570  1570 E action===: [, , 0]:android.media.VOLUME_CHANGED_ACTION
08-15 09:55:47.922  1570  1570 E action===: android.media.VOLUME_CHANGED_ACTION
08-15 09:55:47.923  1570  1570 E lxAgent : lxAgent:onVolumeChange== 11 
08-15 09:55:47.924   632   811 D AudioService: updateVolumeGroupIndex for stream 10, muted=false, device=2, index=510, group accessibility Muted=false, Index=51, forceMuteState=false
08-15 09:55:47.925  2968  2978 W System  : A resource failed to call close. 
08-15 09:55:47.926   632   811 D AudioService: updateVolumeGroupIndex for stream 9, muted=false, device=2, index=510, group tts Muted=false, Index=51, forceMuteState=false
08-15 09:55:47.927  1570  1570 E action===: [, , 0]:android.media.VOLUME_CHANGED_ACTION
08-15 09:55:47.927   952   952 E jarvis_action===: android.media.VOLUME_CHANGED_ACTION
08-15 09:55:47.927  1570  1570 E action===: android.media.VOLUME_CHANGED_ACTION
08-15 09:55:47.927   952   952 E jarvis_ : jarvis_:onVolumeChange== 10 
08-15 09:55:47.927  1570  1570 E lxAgent : lxAgent:onVolumeChange== 10 
08-15 09:55:47.927  1019  1019 D HeadsetClientService: Volume changed for stream: 10
08-15 09:55:47.928  1019  1019 D HeadsetClientService: Volume changed for stream: 9
08-15 09:55:47.928   632   811 D AudioService: updateVolumeGroupIndex for stream 8, muted=false, device=2, index=510, group dtmf Muted=false, Index=51, forceMuteState=false
08-15 09:55:47.929   952   952 E jarvis_action===: android.media.VOLUME_CHANGED_ACTION
08-15 09:55:47.929  1570  1570 E action===: [, , 0]:android.media.VOLUME_CHANGED_ACTION
08-15 09:55:47.929   952   952 E jarvis_ : jarvis_:onVolumeChange== 9 
08-15 09:55:47.929  1570  1570 E action===: android.media.VOLUME_CHANGED_ACTION
08-15 09:55:47.929  1570  1570 E lxAgent : lxAgent:onVolumeChange== 9 
08-15 09:55:47.931   632   811 D AudioService: updateVolumeGroupIndex for stream 3, muted=false, device=2, index=510, group music Muted=false, Index=51, forceMuteState=false
08-15 09:55:47.931  1019  1019 D HeadsetClientService: Volume changed for stream: 8
08-15 09:55:47.932  1033  1033 E QuickSettings: action:android.media.VOLUME_CHANGED_ACTION
08-15 09:55:47.932   632   811 D AudioService: forceUse=0 full:false
08-15 09:55:47.932   632   811 D AudioService: adjustSreamVolume playbackDeviceConditions=false tvConditions=false
08-15 09:55:47.933   423  1169 D AudioHalHardware: +getParameters(), key = hal_param_hal_control_vol_en
08-15 09:55:47.933   423  1169 D AudioHalHardware: -getParameters(), return ""
08-15 09:55:47.936   952   952 E jarvis_action===: android.media.VOLUME_CHANGED_ACTION
08-15 09:55:47.936  1570  1570 E action===: [, , 0]:android.media.VOLUME_CHANGED_ACTION
08-15 09:55:47.936  1570  1570 E action===: android.media.VOLUME_CHANGED_ACTION
08-15 09:55:47.937  1570  1570 E lxAgent : lxAgent:onVolumeChange== 8 
08-15 09:55:47.937   952   952 E jarvis_ : jarvis_:onVolumeChange== 8 
08-15 09:55:47.937   952   952 E jarvis_action===: android.media.VOLUME_CHANGED_ACTION
08-15 09:55:47.938   952   952 E jarvis_ : jarvis_:onVolumeChange== 3 
08-15 09:55:47.938  1019  1019 D HeadsetClientService: Volume changed for stream: 3
08-15 09:55:47.939  1570  1570 E action===: android.media.VOLUME_CHANGED_ACTION
08-15 09:55:47.939  1570  1570 E lxAgent : lxAgent:onVolumeChange== 3 
08-15 09:55:47.940   632   811 D AudioService: adjustSuggestedStreamVolume() stream=3, flags=4112, caller=android, volControlStream=-1, userSelect=false
08-15 09:55:47.940   632  2007 D AudioService: Volume controller visible: true
08-15 09:55:47.948   952   952 I jarvis_ : eventbus post:VolumeEvent
08-15 09:55:47.949   952   952 I jarvis_ : local volume=51,remoteVolume=51

同时 systemui（pid 1033）每次按键都会新建一个 MediaPlayer 播放按键提示音：日志里出现了第二个 AmNuPlayer [#1]，setDataSourceAsync fd 19/.../13518（13518 字节、时长仅 477ms 的小 mp3），走完全相同的 ffmpeg 解封装 + C2 解码 + NON-offload 输出链路，播完 AVERROR_EOF → queue EOS → saw output EOS → onQueueEOS → audio sink stop 后 reset 析构
08-15 09:55:47.974   537   978 V AmlogicNuPlayerFactoryInit:  create AmNuPlayer
08-15 09:55:47.974   537   978 I NU-AmNuPlayerDriver:  ReCalculatePositionRatio set 0.500000
08-15 09:55:47.975   537   978 I AmlPlayerTimer: AmlPlayerTimer
08-15 09:55:47.975   537   978 I MessageMonitor: NU-AmNuPlayer MessageMonitor mDebugLevel 0, poll message us 30000000
08-15 09:55:47.975   537   978 I NU-AmNuPlayer: [#1] mPlayerInstanceId = 1
08-15 09:55:47.975   537   978 D AmlPlayerTimer: func:_timer_create  create timer.1 success.
08-15 09:55:47.975   537   978 D NU-AmNuPlayer: Enable QOS : 0
08-15 09:55:47.975   537   978 D NU-AmNuPlayerDriver: [#1] AmNuPlayerDriver(0xea644300) created, clientPid(1033), mPlayerInstanceId(1)
08-15 09:55:47.975   537   978 I NU-AmNuPlayerDriver: [#1] 
08-15 09:55:47.975   537   978 I NU-AmNuPlayerDriver: --------------------------------
08-15 09:55:47.975   537   978 I NU-AmNuPlayerDriver: ARCH = arm
08-15 09:55:47.975   537   978 I NU-AmNuPlayerDriver: branch name:   origin/project/iptv/r-iptv-dev-v3.0]
08-15 09:55:47.975   537   978 I NU-AmNuPlayerDriver: git version:   6813fe2217fe2ed7a1a950f4d2ab93222cb65337 
08-15 09:55:47.975   537   978 I NU-AmNuPlayerDriver: change id:      Change-Id: I992c058f8140cc983aaa0a39666b80b8ca3b1c17 
08-15 09:55:47.975   537   978 I NU-AmNuPlayerDriver: ID       :      PD#IPTV-39521 
08-15 09:55:47.975   537   978 I NU-AmNuPlayerDriver: last changed:  Date: Thu Mar 19 02:26:18 2026 +0000
08-15 09:55:47.975   537   978 I NU-AmNuPlayerDriver: build-time:     Wed Mar 25 02:38:52 UTC 2026
08-15 09:55:47.975   537   978 I NU-AmNuPlayerDriver: build-name:     wang.ren
08-15 09:55:47.975   537   978 I NU-AmNuPlayerDriver: uncommitted-file-num:0
08-15 09:55:47.975   537   978 I NU-AmNuPlayerDriver: BaseCommitMsg: commit 6813fe2217fe2ed7a1a950f4d2ab93222cb65337 PD#IPTV-39521 Change-Id: I992c058f8140cc983aaa0a39666b80b8ca3b1c17
08-15 09:55:47.975   537   978 I NU-AmNuPlayerDriver: PathesOnBase:  
08-15 09:55:47.975   537   978 I NU-AmNuPlayerDriver: 
08-15 09:55:47.975   537   978 I NU-AmNuPlayerDriver: MM-module-name:AmNuPlayer,Version:V5.1.206-g6813fe2
08-15 09:55:47.975   537   978 I NU-AmNuPlayerDriver: 
08-15 09:55:47.975   537   978 I NU-AmNuPlayerDriver: --------------------------------
08-15 09:55:47.975   537   978 I NU-AmNuPlayerDriver: [#1] Supported functions : 
08-15 09:55:47.975   537   978 I NU-AmNuPlayerDriver: {"MM-module-name":"AmNuPlayer"{"Features":["Switchaudio/subtitletracks","HTTPlivestreamingformpegtsandmp3formats","Localplaybackforvariousformats","UDPunicastandmulticastformpegtsformat","RTP/RTSPplaybackformpegtsformat","HLSlive/vodformpegtsformat","Androidaudiooffloadmode","SupportBuilt-insubtitles","AVS3videosupportforprotocolHTTPandLocal","AC4audiosupportforprotocolHTTP,Local,HLSandUDP","DVBTelextsupport","AudioVIVIDsuportforLocal/HTTP/UDP/HLS","AVS3videosupportvforprotocolHLSandUDP","ISOplaybackforlocal""libcurldownloadforprotocolHLS""dnscacheforprotocolHLS""ipv6andipv4firstplaybackforprotocolHLS""IPTVMulticasttoUnicastforprotocolRTPandUDP""IPTVcmccsoftprobeforprotocolUDPRTPHTTPHLS"],"SupportedFormats":[{"Protocol":"UDP","FileFormats":["MPEGTS"],},{"Protocol":"HLS","FileFormats":["MPEGTS","MP4"],},{"Protocol":"HTTPLIVE","FileFormats":["MPEGTS","MP3"],},{"Protocol":"LOCAL/HTTPVOD","FileFormats":["MP4","ASF","AVI","MPEGTS","MPEGPS","FLV","LIVE_FLV","WAV","PMP","H264","IVF","OG
08-15 09:55:47.976   224   224 I hwservicemanager: getTransport: Cannot find entry android.hardware.media.omx@1.0::IOmx/default in either framework or device VINTF manifest.
08-15 09:55:47.977   537   978 D MediaPlayerService: OMX service is not available
08-15 09:55:47.979   537   978 I NU-AmNuPlayer: [#1] setDataSourceAsync fd 19/3999504/13518
08-15 09:55:47.979   537  3045 I NU-AmNuPlayer: [#1] kWhatSetAudioSink
08-15 09:55:47.996   423  1193 I audio_hw_hal_primary: [audiohal_get_latency:1344] io 13: out:0xe7943990 frames:3072 rate:48000 whole_latency:64 alsa_latency:64
08-15 09:55:48.006   537   978 D NU-AmNuPlayer: part of md5:240a77755895f0bbdb127ec41ced09fc
08-15 09:55:48.006   537   978 I NU-AmNuPlayer: [#1] file size too small, check M3U8
08-15 09:55:48.107   535   535 E exFFmpeg: [mp3 @ 0xf0f06d30] EOF Found
08-15 09:55:48.107   535   535 I AmFFmpegExtractor: No more packets from ffmpeg. because of AVERROR_EOF
08-15 09:55:48.107   535   535 I AmFFmpegExtractor: Read return error, and update status:-1011
08-15 09:55:48.107   535   535 I AmFFmpegExtractor: audio/ffmpeg_reading, feedMore err: -1011
08-15 09:55:48.121   537  3052 E NU-GenericSource: hasBufferAvailable return -1011
08-15 09:55:48.121   537  3052 I NU-AmNuPlayerDecoder: [#1] queue EOS buffer to MediaCodec. Audio
08-15 09:55:48.123   537  3049 I NU-AmNuPlayerRenderer: [#1] Audio PCM buffer First Full. time pay:23995, frameCount:18810, mAudioSink->frameSize():4
08-15 09:55:48.124   537  3049 W NU-AmNuPlayerRenderer: [#1] postDrainAudioQueue long delay: 26658 (18810*0.022676), numFramesPendingPlayout:(18810-0)
08-15 09:55:48.124   423  1193 I audio_hw_hal_primary: [audiohal_get_latency:1344] io 13: out:0xe7943990 frames:3072 rate:48000 whole_latency:64 alsa_latency:64
08-15 09:55:48.125   423   626 I audio_hw_hal_primary: out_update_source_metadata_v7() line 2024 usage:1 content_type:0
08-15 09:55:48.126   433  3053 I Amlogic_C2AudioFFMPEGDecoder: process Invalid data frame
08-15 09:55:48.126   433  3053 I Amlogic_C2AudioFFMPEGDecoder: drainEos 736
08-15 09:55:48.126   537  3050 I NU-AmNuPlayerDecoder: [#1] [audio] saw output EOS
08-15 09:55:48.127   537  3049 I NU-AmNuPlayerRenderer: [#1] audio have queued eos, no need to check buffer discontinue
08-15 09:55:48.127   537  3049 I NU-AmNuPlayerRenderer: [#1] onQueueEOS audio 
08-15 09:55:48.143   952   952 E jarvis_AppUtils: getPkgNameByPidFromFile failed, pid===1033, ex=java.io.FileNotFoundException: /proc/1033/cmdline: open failed: ENOENT (No such file or directory)
08-15 09:55:48.143   952   952 E jarvis_AppUtils: read /proc/pid/cmdline failed, pname===null, fallback to AMS...
08-15 09:55:48.150   537  3049 D AudioTrack: getTimestamp_l(26): device stall time corrected using current time 595851170283
08-15 09:55:48.151   537  3049 W NU-AmNuPlayerRenderer: [#1] postDrainAudioQueue long delay: 26658 (18810*0.022676), numFramesPendingPlayout:(20691-1881)
08-15 09:55:48.178   537  3049 D AudioTrack: stop(26): called with 20736 frames delivered
08-15 09:55:48.178   537  3050 I NU-AmNuPlayerRenderer: [#1] setDiscontinuityFromM3u8 audio 0
08-15 09:55:48.182   537  3049 D NU-AmNuPlayerRenderer: [#1] audio sink stop from [onDrainAudioQueue,2537]
08-15 09:55:59.155   537   978 D NU-AmNuPlayerDriver: [#1] reset(0xea644300) at state 6
08-15 09:55:59.155   632   811 D AudioService: adjustStreamVolume() stream=3, dir=0, flags=4112, caller=android
08-15 09:55:59.155   537   978 D NU-AmNuPlayerDriver: [#1] notifyListener_l(0xea644300), (8, 0, 0, -1), loop setting(0, 0)
08-15 09:55:59.155   537   978 I NU-GenericSource: --->disconnect<---
08-15 09:55:59.156   632   811 D AudioService: forceUse=0 full:false
08-15 09:55:59.156   632   811 D AudioService: adjustSreamVolume playbackDeviceConditions=false tvConditions=false
08-15 09:55:59.156   537   978 I NU-GenericSource: --->disconnect OK 
08-15 09:55:59.156   537  3050 I NU-AmNuPlayerDecoder: [#1] [doFlush:1547] audio flushing 
08-15 09:55:59.156   537  3050 I NU-AmNuPlayerDecoder: [#1] [doFlush:1554] audio flushing 
08-15 09:55:59.156   537  3049 I NU-AmNuPlayerRenderer: [#1] clearAnchorTime
08-15 09:55:59.156   537  3049 I NU-AmNuPlayerRenderer: [#1] flushing audio
08-15 09:55:59.156   423   626 D AudioHalHardware: +getParameters(), key = hal_param_hal_control_vol_en
08-15 09:55:59.156   423   626 D AudioHalHardware: -getParameters(), return ""
08-15 09:55:59.156   537  3049 D NU-AmNuPlayerRenderer: [#1] audio sink pause-flush-stop from [onFlush,4923]
08-15 09:55:59.157   537  3050 I NU-AmNuPlayerDecoder: [#1] [doFlush:1560] audio flushing 
08-15 09:55:59.157   537  3045 I NU-AmNuPlayer: [#1] decoder audio flush completed
08-15 09:55:59.157   433  3053 I Amlogic_C2AudioFFMPEGDecoder: onRelease 492
08-15 09:55:59.157   433  3053 I Amlogic_C2AudioFFMPEGDecoder: onStop() 464
08-15 09:55:59.157   433  3053 I Amlogic_C2AudioFFMPEGDecoder: tearDown 439
08-15 09:55:59.157   433  3053 I Amlogic_C2AudioFFMPEGDecoder: tearDownAudioDecoder_l 308
08-15 09:55:59.158   433  3053 I Amlogic_C2AudioFFMPEGDecoder: unload_ffmpeg_decoder_lib 325
08-15 09:55:59.158   433  3053 I AmFFmpegDecoder: ffmpeg_decoder_close 160 
08-15 09:55:59.158   433  3053 I AmAudioCodec: decode_close 283 
08-15 09:55:59.158   433  3053 I AmAudioCodec: ~AmAudioCodec 66 
08-15 09:55:59.158   433  3053 I AmAudioCodec: decode_close 283 
08-15 09:55:59.158   433  3053 W RefBase : RefBase: Explicit destruction, weak count = 0 (in 0xf1853590). Use sp<> to manage this object.
08-15 09:55:59.158   433  3053 W RefBase : CallStack::getCurrentInternal not linked, returning null
08-15 09:55:59.158   433  3053 W RefBase : CallStack::logStackInternal not linked
08-15 09:55:59.158   537  3051 I hw-BpHwBinder: onLastStrongRef automatically unlinking death recipients
08-15 09:55:59.159   537  3051 D BufferPoolAccessor2.0: bufferpool2 0xe8603988 : 0(0 size) total buffers - 0(0 size) used buffers - 12/20 (recycle/alloc) - 8/36 (fetch/transfer)
08-15 09:55:59.159   433   433 I hw-BpHwBinder: onLastStrongRef automatically unlinking death recipients
08-15 09:55:59.159   433   433 I Amlogic_C2AudioFFMPEGDecoder: ~C2AudioFFMPEGDecoder() 280
08-15 09:55:59.159   433   433 I Amlogic_C2AudioFFMPEGDecoder: onRelease 492
08-15 09:55:59.159   433   433 I Amlogic_C2AudioFFMPEGDecoder: onStop() 464
08-15 09:55:59.159   433   433 I Amlogic_C2AudioFFMPEGDecoder: tearDown 439
08-15 09:55:59.159   433   433 I Amlogic_C2AudioFFMPEGDecoder: ~C2AudioFFMPEGDecoder() 287  exit
08-15 09:55:59.160   537  3050 D MediaCodec: flushMediametrics
08-15 09:55:59.160   433   898 D BufferPoolAccessor2.0: bufferpool2 0xed941348 : 0(0 size) total buffers - 0(0 size) used buffers - 15/18 (recycle/alloc) - 3/18 (fetch/transfer)
08-15 09:55:59.160   537  3045 I NU-AmNuPlayer: [#1] audio shutdown completed
08-15 09:55:59.160   537  3045 I NU-AmNuPlayer: [#1] audio setHasNoMedia
08-15 09:55:59.160   537  3050 I NuPlayerDecoderBase: ReadDataHandler release.
08-15 09:55:59.160   537  3050 I MessageMonitor: ReadDataHandler MessageMonitor exit
08-15 09:55:59.160   537  3050 I MessageMonitor: NU-AmNuPlayerDecoder-audN MessageMonitor exit
08-15 09:55:59.161   537  3045 D NU-AmNuPlayerDriver: [#1] notifyResetComplete(0xea644300)
08-15 09:55:59.162   537  3045 I NU-AmNuPlayer: mSeekByUserCompleted_5904: 1
08-15 09:55:59.162   537   978 D NU-AmNuPlayerDriver: [#1] reset(0xea644300) at state 0
08-15 09:55:59.162   537   978 I NU-AmNuPlayerDriver: ~AmNuPlayerDriver(0xea644300)

音量减（keycode 114）：链路相同，dir=-1，group music Index 51→50，systemui 又建了 AmNuPlayer [#2] 放同一个按键音
08-15 09:55:59.003   568   568 D XIRI_driver: send to xiri: {"cmd":"key","event":1,"keycode":114,"pid":1,"vid":1,"devnum":0}
08-15 09:55:59.004   632   734 D InputReader: getDisplayIdi -1
08-15 09:55:59.022     0     0 I         : [0 T2865  ..] input input1: key 114 down
08-15 09:55:59.007   952   966 I jarvis_ : System call method methodId = reduce_volume
08-15 09:55:59.007   952   966 I jarvis_binded: mmkv binded
08-15 09:55:59.009   632   645 D AudioService: adjustStreamVolume() stream=3, dir=0, flags=1, caller=com.android.systemui
08-15 09:55:59.010   632   645 D AudioService: forceUse=0 full:false
08-15 09:55:59.010   632   645 D AudioService: adjustSreamVolume playbackDeviceConditions=false tvConditions=false
08-15 09:55:59.011   423   626 D AudioHalHardware: +getParameters(), key = hal_param_hal_control_vol_en
08-15 09:55:59.011   423   626 D AudioHalHardware: -getParameters(), return ""
08-15 09:55:59.018   632   645 W WindowManager: com.android.systemui skip checkAddPermission for Window 2020
08-15 09:55:59.018   632   645 W WindowManager: [addWindow] title:
08-15 09:55:59.024   632  1631 D AudioService: Volume controller visible: true
08-15 09:55:59.027   632  1631 D CoreBackPreview: Window{9cc0042 u0 com.android.systemui}: Setting back callback OnBackInvokedCallbackInfo{mCallback=android.window.IOnBackInvokedCallback$Stub$Proxy@e73e590, mPriority=0, mIsAnimationCallback=false}
08-15 09:55:59.035  1033  1224 E OpenGLRenderer: Unable to match the desired swap behavior.
08-15 09:55:59.133     0     0 I         : [0 T2865  ..] input input1: key 114 up
08-15 09:55:59.115   568   568 D XIRI_driver: send to xiri: {"cmd":"key","event":0,"keycode":114,"pid":1,"vid":1,"devnum":0}
08-15 09:55:59.115   632   734 D InputReader: getDisplayIdi -1
08-15 09:55:59.116   632   733 D WindowManager: handleVolumeKeyInArc KeyEvent { action=ACTION_DOWN, keyCode=KEYCODE_VOLUME_DOWN, scanCode=114, metaState=0, flags=0x8, repeatCount=0, eventTime=606704082000, downTime=606704082000, deviceId=2, source=0x101, displayId=-1 }
08-15 09:55:59.117  1258  1258 D RemoteIME: keycode: 25, realAction: false
08-15 09:55:59.117   632   733 D WindowManager: handleVolumeKeyInArc KeyEvent { action=ACTION_UP, keyCode=KEYCODE_VOLUME_DOWN, scanCode=114, metaState=0, flags=0x8, repeatCount=0, eventTime=606815808000, downTime=606704082000, deviceId=2, source=0x101, displayId=-1 }
08-15 09:55:59.119   632  2007 D MediaSessionService: dispatchVolumeKeyEvent, pkg=com.droidlogic.exoplayer2.demo, opPkg=com.droidlogic.exoplayer2.demo, pid=2968, uid=10032, asSystem=true, event=KeyEvent { action=ACTION_DOWN, keyCode=KEYCODE_VOLUME_DOWN, scanCode=114, metaState=0, flags=0x8, repeatCount=0, eventTime=606704082000, downTime=606704082000, deviceId=2, source=0x101, displayId=-1 }, stream=-2147483648, musicOnly=false
08-15 09:55:59.119   632  2007 D MediaSessionService: Adjusting com.droidlogic.exoplayer2.demo/MoviePlayer (userId=0) by -1. flags=4113, suggestedStream=-2147483648, preferSuggestedStream=false
08-15 09:55:59.119   632   811 D AudioService: adjustSuggestedStreamVolume() stream=3, flags=4113, caller=android, volControlStream=-1, userSelect=false
08-15 09:55:59.120  1258  1258 D RemoteIME: keycode: 25, realAction: true
08-15 09:55:59.120  2968  2968 D MoviePlayer: keycode:25 down is 0, action:1
08-15 09:55:59.121   632   811 V AudioService: getActiveStreamType: Returning suggested type 3
08-15 09:55:59.121   632  2007 D MediaSessionService: dispatchVolumeKeyEvent, pkg=com.droidlogic.exoplayer2.demo, opPkg=com.droidlogic.exoplayer2.demo, pid=2968, uid=10032, asSystem=true, event=KeyEvent { action=ACTION_UP, keyCode=KEYCODE_VOLUME_DOWN, scanCode=114, metaState=0, flags=0x8, repeatCount=0, eventTime=606815808000, downTime=606704082000, deviceId=2, source=0x101, displayId=-1 }, stream=-2147483648, musicOnly=false
08-15 09:55:59.121   632  2007 D MediaSessionService: Adjusting com.droidlogic.exoplayer2.demo/MoviePlayer (userId=0) by 0. flags=4116, suggestedStream=-2147483648, preferSuggestedStream=false
08-15 09:55:59.121   632   811 D AudioService: adjustStreamVolume() stream=3, dir=-1, flags=4113, caller=android
08-15 09:55:59.123   632   811 D AudioService: updateVolumeGroupIndex for stream 11, muted=false, device=2, index=500, group system Muted=false, Index=50, forceMuteState=false
08-15 09:55:59.125   632   811 D AudioService: updateVolumeGroupIndex for stream 10, muted=false, device=2, index=500, group accessibility Muted=false, Index=50, forceMuteState=false
08-15 09:55:59.125  1019  1019 D HeadsetClientService: Volume changed for stream: 11
08-15 09:55:59.127  1033  1033 E QuickSettings: action:android.media.VOLUME_CHANGED_ACTION
08-15 09:55:59.127   952   952 E jarvis_action===: android.media.VOLUME_CHANGED_ACTION
08-15 09:55:59.127   952   952 E jarvis_ : jarvis_:onVolumeChange== 11 
08-15 09:55:59.128  1570  1570 E action===: [, , 0]:android.media.VOLUME_CHANGED_ACTION
08-15 09:55:59.128  1570  1570 E action===: android.media.VOLUME_CHANGED_ACTION
08-15 09:55:59.128  1570  1570 E lxAgent : lxAgent:onVolumeChange== 11 
08-15 09:55:59.129   632   811 D AudioService: updateVolumeGroupIndex for stream 9, muted=false, device=2, index=500, group tts Muted=false, Index=50, forceMuteState=false
08-15 09:55:59.129  1019  1019 D HeadsetClientService: Volume changed for stream: 10
08-15 09:55:59.131   632   811 D AudioService: updateVolumeGroupIndex for stream 8, muted=false, device=2, index=500, group dtmf Muted=false, Index=50, forceMuteState=false
08-15 09:55:59.132   952   952 E jarvis_action===: android.media.VOLUME_CHANGED_ACTION
08-15 09:55:59.132  1570  1570 E action===: [, , 0]:android.media.VOLUME_CHANGED_ACTION
08-15 09:55:59.132   952   952 E jarvis_ : jarvis_:onVolumeChange== 10 
08-15 09:55:59.132  1570  1570 E action===: android.media.VOLUME_CHANGED_ACTION
08-15 09:55:59.133  1570  1570 E lxAgent : lxAgent:onVolumeChange== 10 
08-15 09:55:59.136  1019  1019 D HeadsetClientService: Volume changed for stream: 9
08-15 09:55:59.136   632   811 D AudioService: updateVolumeGroupIndex for stream 3, muted=false, device=2, index=500, group music Muted=false, Index=50, forceMuteState=false
08-15 09:55:59.137  1570  1570 E action===: [, , 0]:android.media.VOLUME_CHANGED_ACTION
08-15 09:55:59.137  1570  1570 E action===: android.media.VOLUME_CHANGED_ACTION
08-15 09:55:59.137  1570  1570 E lxAgent : lxAgent:onVolumeChange== 9 
08-15 09:55:59.137  1033  1033 E QuickSettings: action:android.media.VOLUME_CHANGED_ACTION
08-15 09:55:59.138  1033  1033 E QuickSettings: action:android.media.VOLUME_CHANGED_ACTION
08-15 09:55:59.140  1570  1570 E action===: [, , 0]:android.media.VOLUME_CHANGED_ACTION
08-15 09:55:59.140   952   952 E jarvis_action===: android.media.VOLUME_CHANGED_ACTION
08-15 09:55:59.140   952   952 E jarvis_ : jarvis_:onVolumeChange== 9 
08-15 09:55:59.140  1570  1570 E action===: android.media.VOLUME_CHANGED_ACTION
08-15 09:55:59.140   952   952 E jarvis_action===: android.media.VOLUME_CHANGED_ACTION
08-15 09:55:59.141   952   952 E jarvis_ : jarvis_:onVolumeChange== 8 
08-15 09:55:59.141  1570  1570 E lxAgent : lxAgent:onVolumeChange== 8 
08-15 09:55:59.141  1033  1033 E QuickSettings: action:android.media.VOLUME_CHANGED_ACTION
08-15 09:55:59.142  1019  1019 D HeadsetClientService: Volume changed for stream: 8
08-15 09:55:59.142  1019  1019 D HeadsetClientService: Volume changed for stream: 3
08-15 09:55:59.142   952   952 E jarvis_action===: android.media.VOLUME_CHANGED_ACTION
08-15 09:55:59.143   952   952 E jarvis_ : jarvis_:onVolumeChange== 3 
08-15 09:55:59.144  1570  1570 E action===: [, , 0]:android.media.VOLUME_CHANGED_ACTION
08-15 09:55:59.144  1570  1570 E action===: android.media.VOLUME_CHANGED_ACTION
08-15 09:55:59.144  1570  1570 E lxAgent : lxAgent:onVolumeChange== 3 
08-15 09:55:59.144  1033  1033 E QuickSettings: action:android.media.VOLUME_CHANGED_ACTION
08-15 09:55:59.147   952   952 I jarvis_ : eventbus post:VolumeEvent
08-15 09:55:59.147   952   952 I jarvis_ : local volume=50,remoteVolume=50
08-15 09:55:59.151   632   811 D AudioService: forceUse=0 full:false
08-15 09:55:59.151   632   811 D AudioService: adjustSreamVolume playbackDeviceConditions=false tvConditions=false
08-15 09:55:59.152   423   626 D AudioHalHardware: +getParameters(), key = hal_param_hal_control_vol_en
08-15 09:55:59.152   423   626 D AudioHalHardware: -getParameters(), return ""
08-15 09:55:59.153   632  1631 D AudioService: Volume controller visible: true
08-15 09:55:59.153   632   811 D AudioService: adjustSuggestedStreamVolume() stream=3, flags=4112, caller=android, volControlStream=-1, userSelect=false
08-15 09:55:59.155   632   811 V AudioService: getActiveStreamType: Returning suggested type 3
08-15 09:55:59.173   537   978 V AmlogicNuPlayerFactoryInit:  create AmNuPlayer
08-15 09:55:59.173   537   978 I NU-AmNuPlayerDriver:  ReCalculatePositionRatio set 0.500000
08-15 09:55:59.173   537   978 I AmlPlayerTimer: AmlPlayerTimer
08-15 09:55:59.174   537   978 I MessageMonitor: NU-AmNuPlayer MessageMonitor mDebugLevel 0, poll message us 30000000
08-15 09:55:59.174   537   978 I NU-AmNuPlayer: [#2] mPlayerInstanceId = 2
08-15 09:55:59.174   537   978 D AmlPlayerTimer: func:_timer_create  create timer.1 success.
08-15 09:55:59.174   537   978 D NU-AmNuPlayer: Enable QOS : 0
08-15 09:55:59.174   537   978 D NU-AmNuPlayerDriver: [#2] AmNuPlayerDriver(0xea644300) created, clientPid(1033), mPlayerInstanceId(2)
08-15 09:55:59.174   537   978 I NU-AmNuPlayerDriver: [#2] 
08-15 09:55:59.174   537   978 I NU-AmNuPlayerDriver: --------------------------------
08-15 09:55:59.174   537   978 I NU-AmNuPlayerDriver: ARCH = arm
08-15 09:55:59.174   537   978 I NU-AmNuPlayerDriver: branch name:   origin/project/iptv/r-iptv-dev-v3.0]
08-15 09:55:59.174   537   978 I NU-AmNuPlayerDriver: git version:   6813fe2217fe2ed7a1a950f4d2ab93222cb65337 
08-15 09:55:59.174   537   978 I NU-AmNuPlayerDriver: change id:      Change-Id: I992c058f8140cc983aaa0a39666b80b8ca3b1c17 
08-15 09:55:59.174   537   978 I NU-AmNuPlayerDriver: ID       :      PD#IPTV-39521 
08-15 09:55:59.174   537   978 I NU-AmNuPlayerDriver: last changed:  Date: Thu Mar 19 02:26:18 2026 +0000
08-15 09:55:59.174   537   978 I NU-AmNuPlayerDriver: build-time:     Wed Mar 25 02:38:52 UTC 2026
08-15 09:55:59.174   537   978 I NU-AmNuPlayerDriver: build-name:     wang.ren
08-15 09:55:59.174   537   978 I NU-AmNuPlayerDriver: uncommitted-file-num:0
08-15 09:55:59.174   537   978 I NU-AmNuPlayerDriver: BaseCommitMsg: commit 6813fe2217fe2ed7a1a950f4d2ab93222cb65337 PD#IPTV-39521 Change-Id: I992c058f8140cc983aaa0a39666b80b8ca3b1c17
08-15 09:55:59.174   537   978 I NU-AmNuPlayerDriver: PathesOnBase:  
08-15 09:55:59.174   537   978 I NU-AmNuPlayerDriver: 
08-15 09:55:59.174   537   978 I NU-AmNuPlayerDriver: MM-module-name:AmNuPlayer,Version:V5.1.206-g6813fe2
08-15 09:55:59.174   537   978 I NU-AmNuPlayerDriver: 
08-15 09:55:59.174   537   978 I NU-AmNuPlayerDriver: --------------------------------
08-15 09:55:59.174   537   978 I NU-AmNuPlayerDriver: [#2] Supported functions : 
08-15 09:55:59.174   537   978 I NU-AmNuPlayerDriver: {"MM-module-name":"AmNuPlayer"{"Features":["Switchaudio/subtitletracks","HTTPlivestreamingformpegtsandmp3formats","Localplaybackforvariousformats","UDPunicastandmulticastformpegtsformat","RTP/RTSPplaybackformpegtsformat","HLSlive/vodformpegtsformat","Androidaudiooffloadmode","SupportBuilt-insubtitles","AVS3videosupportforprotocolHTTPandLocal","AC4audiosupportforprotocolHTTP,Local,HLSandUDP","DVBTelextsupport","AudioVIVIDsuportforLocal/HTTP/UDP/HLS","AVS3videosupportvforprotocolHLSandUDP","ISOplaybackforlocal""libcurldownloadforprotocolHLS""dnscacheforprotocolHLS""ipv6andipv4firstplaybackforprotocolHLS""IPTVMulticasttoUnicastforprotocolRTPandUDP""IPTVcmccsoftprobeforprotocolUDPRTPHTTPHLS"],"SupportedFormats":[{"Protocol":"UDP","FileFormats":["MPEGTS"],},{"Protocol":"HLS","FileFormats":["MPEGTS","MP4"],},{"Protocol":"HTTPLIVE","FileFormats":["MPEGTS","MP3"],},{"Protocol":"LOCAL/HTTPVOD","FileFormats":["MP4","ASF","AVI","MPEGTS","MPEGPS","FLV","LIVE_FLV","WAV","PMP","H264","IVF","OG
08-15 09:55:59.175   224   224 I hwservicemanager: getTransport: Cannot find entry android.hardware.media.omx@1.0::IOmx/default in either framework or device VINTF manifest.
08-15 09:55:59.176   537   978 D MediaPlayerService: OMX service is not available
08-15 09:55:59.178   537   978 I NU-AmNuPlayer: [#2] setDataSourceAsync fd 21/3999504/13518
```

### 阶段 7：快进/Seek
日志里拖动进度条触发了 3 次 seek（42.7s、50.5s、111.9s，前两次相隔 400ms）。以第一次 42704ms 为例，完整链路：
```
App：seekToTimeBarPosition → MyMediaPlayer.seekTo(42704)，同时把 MediaSession 状态置为 BUFFERING(6)/PAUSED
08-15 09:56:18.977  2968  2968 I MoviePlayerControView_MoviePlayer: seekToTimeBarPosition
08-15 09:56:18.977  2968  2968 D MyMediaPlayer_MoviePlayer: [seekTo]msec:42704,mState:4
08-15 09:56:18.977  2968  2968 I MyMediaPlayer_MoviePlayer: [seekTo]:42704
08-15 09:56:18.977  2968  2968 I MoviePlayer: onPlaybackStateChanged:7
08-15 09:56:18.978  2968  2968 I MoviePlayer: [updatePlaybackState] state=6, pos=40424

Binder 到服务端：AmNuPlayerDriver::seekTo (42704 ms, 3) at state 5（mode 3 = SEEK_CLOSEST，就近同步点）→ kWhatSeek seekTimeUs=42704000
08-15 09:56:18.989   537   596 D NU-AmNuPlayerDriver: [#0] seekTo(0xea640230) (42704 ms, 3) at state 5
08-15 09:56:18.989   537   596 D NU-AmNuPlayerDriver: [#0] notifyListener_l(0xea640230), (7, 0, 0, -1), loop setting(0, 0)
08-15 09:56:18.989   537   596 I NU-AmNuPlayer: [#0] seekToAsync  NowTimeMs:  626689  mLastSeekTimeMs:  585537, Seek diffTime:41152, quickExit:0
08-15 09:56:18.989   537   596 D NU-AmNuPlayerDriver: [#0] seekTo_END(0xea640230) (42704 ms, 3) at state 5
08-15 09:56:18.989   537  3003 I NU-AmNuPlayer: [#0] kWhatSeek
08-15 09:56:18.989   537  3003 I NU-AmNuPlayer: [#0] kWhatSeek seekTimeUs=42704000 us, mode=3, needNotify=1

解码器 flush：Decoder doFlush 三段式 flushing，C2 组件 onFlush_sm；渲染器 flush：clearAnchorTime + flushing audio + audio sink pause-flush-stop（先停 AudioTrack 排出队列）
08-15 09:56:18.996   537  3013 I NU-AmNuPlayerDecoder: [#0] [doFlush:1547] audio flushing 
08-15 09:56:18.996   537  3013 I NU-AmNuPlayerDecoder: [#0] [doFlush:1554] audio flushing 
08-15 09:56:18.996   537  3003 D NU-AmNuPlayerDriver: [#0] notifyListener_l(0xea640230), (211, 0, 0, 20), loop setting(0, 0)
08-15 09:56:18.997   537  3012 I NU-AmNuPlayerRenderer: [#0] clearAnchorTime
08-15 09:56:18.997   537  3012 I NU-AmNuPlayerRenderer: [#0] flushing audio
08-15 09:56:18.998   537  3014 D CCodecBuffers: [c2.amlogic.audio.decoder.ffmpeg#916:1D-Output.Impl[N]] Client returned a buffer it does not own according to our record: 0
08-15 09:56:18.998   537  3014 D CCodecBuffers: [c2.amlogic.audio.decoder.ffmpeg#916:1D-Output.Impl[N]] Client returned a buffer it does not own according to our record: 1
08-15 09:56:18.998   537  3013 I NU-AmNuPlayerDecoder: [#0] [doFlush:1560] audio flushing 
08-15 09:56:19.028   537  3012 D NU-AmNuPlayerRenderer: [#0] audio sink pause-flush-stop from [onFlush,4923]

解封装器 seek：AmFFmpegExtractor: Seek stream_index:0, To seekTimeUs:42704000, seekPosition:602638848, seekFlag:8（按字节位置换算，seekFlag 8 = AVSEEK_FLAG_FRAME，mp3 按帧对齐）
08-15 09:56:19.030   535   588 I AmFFmpegExtractor: Seek stream_index:0, To seekTimeUs:42704000, seekPosition:602638848, seekFlag:8
08-15 09:56:19.030   535   588 I AmFFmpegExtractor: Seeking to 602638848 end. seekflag 8 

插入不连续标记：queueDiscontinuity for trackType:2 → doSeek mAudioLastDequeueTimeUs=42704000 → performResumeDecoders → 回调 (4,...) = MEDIA_SEEK_COMPLETE，App 收到 onSeekComplete
08-15 09:56:19.031   537  3009 I NU-GenericSource: queueDiscontinuityIfNeeded, queueDiscontinuity for trackType:2, seeking:1, formatChange:0 From[readBuffer:4873]
08-15 09:56:19.032   537  3009 I NU-GenericSource: [doSeek:3934] mAudioLastDequeueTimeUs:42704000
08-15 09:56:19.032   537  3003 I NU-AmNuPlayer: [#0] performResumeDecoders  needNotify:1
08-15 09:56:19.032   537  3003 I NU-AmNuPlayer: [#0] finishResume  mResumePending:1  
08-15 09:56:19.032   537  3003 D NU-AmNuPlayerDriver: [#0] notifyListener_l(0xea640230), (4, 0, 0, -1), loop setting(0, 0)
08-15 09:56:19.032   537  3013 D MediaCodec: keep callback message for reclaim
08-15 09:56:19.032  2968  2968 I MyMediaPlayer_MoviePlayer: [onSeekComplete] progressBarSeekFlag:,mStateBac:4

App 自动重新 start() → 解码器重新喂数据（First Input Buffer Queued）→ 首个输出帧 timeUs:42710204（比目标晚 6.2ms，即 mp3 帧边界对齐结果）→ First audio pcm write to audiosink → setStartingTimeMedia 42736326（重新锚定时间轴）→ media rendering started
08-15 09:56:19.034  2968  2968 I MyMediaPlayer_MoviePlayer: [start]mMediaPlayer:
08-15 09:56:19.034  2968  2968 I MoviePlayer: onPlaybackStateChanged:4
08-15 09:56:19.035  2968  2968 I MoviePlayer: [updatePlaybackState] state=3, pos=42704
08-15 09:56:19.035   433  3019 I Amlogic_C2AudioFFMPEGDecoder: onFlush_sm 768
08-15 09:56:19.035   433   935 I C2VendorComponentStore: setDmaBufUsage 5
08-15 09:56:19.036   433  3019 I Amlogic_C2AudioFFMPEGDecoder: process Invalid data frame
08-15 09:56:19.037  1570  1570 E myNoti  : [, , 0]:onPlaybackStateChanged
08-15 09:56:19.037   952   952 D myNoti  : onPlaybackStateChanged
08-15 09:56:19.037   537  3013 I NU-AmNuPlayerDecoder: [#0] [c2.amlogic.audio.decoder.ffmpeg] First CB_INPUT_AVAILABLE received.
08-15 09:56:19.037   952   952 I jarvis_ : <onMediaStart>
08-15 09:56:19.037   952   952 I jarvis_ : overlock,start true,0,0,,
08-15 09:56:19.037   537  3015 I NU-AmNuPlayerDecoder: [#0] [c2.amlogic.audio.decoder.ffmpeg] First Input Buffer Queued to Codec
08-15 09:56:19.040   952   952 E jarvis_AppUtils: getPkgNameByPidFromFile failed, pid===1570, ex=java.io.FileNotFoundException: /proc/1570/cmdline: open failed: ENOENT (No such file or directory)
08-15 09:56:19.041   952   952 E jarvis_AppUtils: read /proc/pid/cmdline failed, pname===null, fallback to AMS...
08-15 09:56:19.041   433  3019 I exFFmpeg: [mp3float @ 0xee640410] overread, skip -6 enddists: -2 -2
08-15 09:56:19.043  1019  1019 V AudioMediaPlayerWrapper: onPlaybackStateChanged(): com.droidlogic.exoplayer2.demo : PlaybackState {state=PLAYING(3), position=42704, buffered position=0, speed=1.0, updated=626735, actions=823, custom actions=[], active item id=-1, error=null}
08-15 09:56:19.044   537  3013 I NU-AmNuPlayerDecoder: [#0] [c2.amlogic.audio.decoder.ffmpeg] First CB_OUTPUT_AVAILABLE received. input buffer queued: 7
08-15 09:56:19.044   537  3013 I NU-AmNuPlayerDecoder: [#0] [c2.amlogic.audio.decoder.ffmpeg] First Out Buffer Queued to Render. timeUs:42710204
08-15 09:56:19.045   537  3012 I NU-AmNuPlayerRenderer: [#0] First audio pcm date write to audiosink.
08-15 09:56:19.045   537  3012 I NU-AmNuPlayerRenderer: [#0] setStartingTimeMedia 42736326, mAudioPlayedOutDur 0
08-15 09:56:19.045   537  3012 W NU-AmNuPlayerRenderer: [#0] postDrainAudioQueue long delay: 2665 (1881*0.022676), numFramesPendingPlayout:(1881-0)
08-15 09:56:19.045   537  3003 I NU-AmNuPlayer: [#0] media rendering started
08-15 09:56:19.045   537  3003 I NU-AmNuPlayer: [#0] finishResume  mResumePending:0  
08-15 09:56:19.045   537  3003 D NU-AmNuPlayerDriver: [#0] notifyListener_l(0xea640230), (6, 0, 0, -1), loop setting(0, 0)

```