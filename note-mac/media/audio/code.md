# Audio

操作：播放、音量加、音量减、快进、快进

## 常见概念

AudioFormat类定义了代表不同编码格式的常量

MediaCodec：Android 提供的底层编解码 API，在底层通过 MediaCodec API 创建一个对应的解码器（Decoder），将压缩数据解码成 AudioTrack 可以处理的 PCM 数据

MediaExtractor：解封装器

MediaMuxer：封装器

通用容器：如 MPEG-4（.mp4, .m4a）、Matroska（.mkv）、Ogg（.ogg）、3GPP（.3gp）

纯音频/流式容器：如 ADTS（.aac）、FLAC（.flac）、MP3（.mp3）、WAV（.wav）

## 播放本地音频文件

### 第一步：app调用上层方法

```java
mediaPlayer = MediaPlayer.create(this, R.raw.test);
mediaPlayer.start();
```

java层：frameworks/base/media/java/android/media/MediaPlayer.java
```java
// 此方法仅通过EventHandler系统将事件回传至主应用线程。我们采用弱引用指向原始MediaPlayer对象，以确保原生代码不会因对象意外消失而崩溃。（此即传递给native_setup()的cookie。）
private native void native_setup(Object mediaplayerThis, @NonNull Parcel attributionSource, int audioSessionId);
// 仅当MediaPlayer构造时出现问题时，音频会话ID才会为0。
public native int getAudioSessionId();
private static native final void native_init();
private native void _start() throws IllegalStateException;

public static MediaPlayer create(Context context, int resid) {
    int s = AudioSystem.newAudioSessionId();
    return create(context, resid, null, s > 0 ? s : 0);
}
public static MediaPlayer create(Context context, int resid, AudioAttributes audioAttributes, int audioSessionId) {
    try {
        AssetFileDescriptor afd = context.getResources().openRawResourceFd(resid);
        if (afd == null) return null;

        MediaPlayer mp = new MediaPlayer(context, audioSessionId);

        final AudioAttributes aa = audioAttributes != null ? audioAttributes : new AudioAttributes.Builder().build();
        mp.setAudioAttributes(aa);
        mp.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
        afd.close();
        mp.prepare();
        return mp;
    } catch (IOException ex) {
        Log.d(TAG, "create failed:", ex);
        // fall through
    } catch (IllegalArgumentException ex) {
        Log.d(TAG, "create failed:", ex);
        // fall through
    } catch (SecurityException ex) {
        Log.d(TAG, "create failed:", ex);
        // fall through
    }
    return null;
}
private MediaPlayer(Context context, int sessionId) {
    super(new AudioAttributes.Builder().build(), AudioPlaybackConfiguration.PLAYER_TYPE_JAM_MEDIAPLAYER);

    Looper looper;
    if ((looper = Looper.myLooper()) != null) {
        mEventHandler = new EventHandler(this, looper);
    } else if ((looper = Looper.getMainLooper()) != null) {
        mEventHandler = new EventHandler(this, looper);
    } else {
        mEventHandler = null;
    }

    mTimeProvider = new TimeProvider(this);
    mOpenSubtitleSources = new Vector<InputStream>();

    AttributionSource attributionSource = context == null ? AttributionSource.myAttributionSource() : context.getAttributionSource();
    // set the package name to empty if it was null
    if (attributionSource.getPackageName() == null) {
        attributionSource = attributionSource.withPackageName("");
    }

    /* Native setup requires a weak reference to our object.
        * It's easier to create it here than in C++.
        */
    try (ScopedParcelState attributionSourceState = attributionSource.asScopedParcelState()) {
        native_setup(new WeakReference<>(this), attributionSourceState.getParcel(), resolvePlaybackSessionId(context, sessionId));
    }
    baseRegisterPlayer(getAudioSessionId());
}
public void start() throws IllegalStateException {
    //FIXME use lambda to pass startImpl to superclass
    final int delay = getStartDelayMs();
    if (delay == 0) {
        startImpl();
    } else {
        new Thread() {
            public void run() {
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                baseSetStartDelayMs(0);
                try {
                    startImpl();
                } catch (IllegalStateException e) {
                    // fail silently for a state exception when it is happening after
                    // a delayed start, as the player state could have changed between the
                    // call to start() and the execution of startImpl()
                }
            }
        }.start();
    }
}
private void startImpl() {
    baseStart(0); // unknown device at this point
    stayAwake(true);
    tryToEnableNativeRoutingCallback();
    _start();
}
```

Jni层：frameworks/base/media/jni/android_media_MediaPlayer.cpp
```c++
static void android_media_MediaPlayer_native_init(JNIEnv *env)
{
    jclass clazz;

    clazz = env->FindClass("android/media/MediaPlayer");
    if (clazz == NULL) {
        return;
    }

    fields.context = env->GetFieldID(clazz, "mNativeContext", "J");
    if (fields.context == NULL) {
        return;
    }

    fields.post_event = env->GetStaticMethodID(clazz, "postEventFromNative",
                                               "(Ljava/lang/Object;IIILjava/lang/Object;)V");
    if (fields.post_event == NULL) {
        return;
    }

    fields.surface_texture = env->GetFieldID(clazz, "mNativeSurfaceTexture", "J");
    if (fields.surface_texture == NULL) {
        return;
    }

    env->DeleteLocalRef(clazz);

    clazz = env->FindClass("android/net/ProxyInfo");
    if (clazz == NULL) {
        return;
    }

    fields.proxyConfigGetHost =
        env->GetMethodID(clazz, "getHost", "()Ljava/lang/String;");

    fields.proxyConfigGetPort =
        env->GetMethodID(clazz, "getPort", "()I");

    fields.proxyConfigGetExclusionList =
        env->GetMethodID(clazz, "getExclusionListAsString", "()Ljava/lang/String;");

    env->DeleteLocalRef(clazz);

    // Modular DRM
    FIND_CLASS(clazz, "android/media/MediaDrm$MediaDrmStateException");
    if (clazz) {
        GET_METHOD_ID(gStateExceptionFields.init, clazz, "<init>", "(ILjava/lang/String;)V");
        gStateExceptionFields.classId = static_cast<jclass>(env->NewGlobalRef(clazz));

        env->DeleteLocalRef(clazz);
    } else {
        ALOGE("JNI android_media_MediaPlayer_native_init couldn't "
              "get clazz android/media/MediaDrm$MediaDrmStateException");
    }

    gPlaybackParamsFields.init(env);
    gSyncParamsFields.init(env);
    gVolumeShaperFields.init(env);
}

static void android_media_MediaPlayer_native_setup(JNIEnv *env, jobject thiz, jobject weak_this,
                                       jobject jAttributionSource,
                                       jint jAudioSessionId)
{
    ALOGV("native_setup");

    Parcel* parcel = parcelForJavaObject(env, jAttributionSource);
    android::content::AttributionSourceState attributionSource;
    attributionSource.readFromParcel(parcel);
    sp<MediaPlayer> mp = sp<MediaPlayer>::make(
        attributionSource, static_cast<audio_session_t>(jAudioSessionId));
    if (mp == NULL) {
        jniThrowException(env, "java/lang/RuntimeException", "Out of memory");
        return;
    }

    // create new listener and give it to MediaPlayer
    sp<JNIMediaPlayerListener> listener = new JNIMediaPlayerListener(env, thiz, weak_this);
    mp->setListener(listener);

    // Stow our new C++ MediaPlayer in an opaque field in the Java object.
    setMediaPlayer(env, thiz, mp);
}
static jint android_media_MediaPlayer_get_audio_session_id(JNIEnv *env,  jobject thiz) {
    ALOGV("get_session_id()");
    sp<MediaPlayer> mp = getMediaPlayer(env, thiz);
    if (mp == NULL ) {
        jniThrowException(env, "java/lang/IllegalStateException", NULL);
        return 0;
    }

    return (jint) mp->getAudioSessionId();
}
static void android_media_MediaPlayer_start(JNIEnv *env, jobject thiz){
    // 这里 getMediaPlayer 做了一件关键的事：它从 Java 层 MediaPlayer 对象的 mNativeContext 字段（一个 long 型变量）中，读取并强转为 MediaPlayer* 指针。
    // 这个指针是在之前的 native_setup 中通过 setMediaPlayer 存入的
    ALOGV("start");
    sp<MediaPlayer> mp = getMediaPlayer(env, thiz); // 关键：从 Java 对象中取出 Native 指针
    if (mp == NULL ) {
        jniThrowException(env, "java/lang/IllegalStateException", NULL);
        return;
    }
    // 调用 mp->start()，并传入 process_media_player_call 处理返回状态
    // mp 的实际类型是 android::MediaPlayer，代码位于 frameworks/av/media/libmedia/MediaPlayer.cpp。这个类并不做重体力活，它是一个 Binder 客户端（Client）。
    process_media_player_call( env, thiz, mp->start(), NULL, NULL );
}
static sp<MediaPlayer> getMediaPlayer(JNIEnv* env, jobject thiz)
{
    Mutex::Autolock l(sLock);
    MediaPlayer* const p = (MediaPlayer*)env->GetLongField(thiz, fields.context);
    return sp<MediaPlayer>(p);
}
static void process_media_player_call(JNIEnv *env, jobject thiz, status_t opStatus, const char* exception, const char *message)
{
    if (exception == NULL) {  // Don't throw exception. Instead, send an event.
        if (opStatus != (status_t) OK) {
            sp<MediaPlayer> mp = getMediaPlayer(env, thiz);
            if (mp != 0) mp->notify(MEDIA_ERROR, opStatus, 0);
        }
    } else {  // Throw exception!
        if ( opStatus == (status_t) INVALID_OPERATION ) {
            jniThrowException(env, "java/lang/IllegalStateException", NULL);
        } else if ( opStatus == (status_t) BAD_VALUE ) {
            jniThrowException(env, "java/lang/IllegalArgumentException", NULL);
        } else if ( opStatus == (status_t) PERMISSION_DENIED ) {
            jniThrowException(env, "java/lang/SecurityException", NULL);
        } else if ( opStatus != (status_t) OK ) {
            if (strlen(message) > 230) {
               // if the message is too long, don't bother displaying the status code
               jniThrowException( env, exception, message);
            } else {
               char msg[256];
                // append the status code to the message
               sprintf(msg, "%s: status=0x%X", message, opStatus);
               jniThrowException( env, exception, msg);
            }
        }
    }
}
```
### 第二步：mp->start() 进入到 libmedia 客户端

再往下到 Native C++ 客户端（libmedia.so 中的 android::MediaPlayer）。

源码目录：frameworks/av/media/libmedia

源码位置：frameworks/av/media/libmedia/mediaplayer.cpp
```c++
status_t MediaPlayer::start() {
    // 加锁，检查状态
    // 调用 Binder 远程接口，向 MediaPlayerService 发送 START 命令
    return mPlayer->start(); 
    // 这里的 mPlayer 是一个 sp<IMediaPlayer>，它是与服务端通信的 Binder 代理
}
```
mPlayer 是通过 Binder 机制与 MediaPlayerService 通信的代理对象。此时，调用已经从 JNI 所在的 App 进程，通过 Binder IPC 跨进程传递到了 mediaserver 进程（或 audioserver 进程，取决于 Android 版本）中的 MediaPlayerService。

### 第三步：服务端 MediaPlayerService 接收请求

源码目录：frameworks/av/media/libmediaplayerservice

源码位置：frameworks/av/media/libmediaplayerservice/MediaPlayerService.cpp

MediaPlayerService 接收到 start() 的 Binder 事务后，会找到对应的 Client 对象（每个播放器实例在服务端对应一个 Client）。

Client::start() 会调用其内部持有的播放器引擎实例，通常是 NuPlayerDriver。

```c++
sp<MediaPlayerBase>     getPlayer() const { Mutex::Autolock lock(mLock); return mPlayer; }
status_t MediaPlayerService::Client::start()
{
    ALOGV("[%d] start", mConnId);
    // mPlayer 是一个 sp<MediaPlayerBase>，实际指向 NuPlayerDriver
    sp<MediaPlayerBase> p = getPlayer();
    if (p == 0) return UNKNOWN_ERROR;
    p->setLooping(mLoop);
    return p->start();
}
```

### 第四步：进入播放引擎 NuPlayerDriver / NuPlayer

源码目录：frameworks/av/media/libmediaplayerservice/nuplayer

源码位置：frameworks/av/media/libmediaplayerservice/nuplayer/NuPlayerDriver.cpp
```c++
status_t NuPlayerDriver::start() {
    ALOGV("start(%p), state is %d, eos is %d", this, mState, mAtEOS);
    Mutex::Autolock autoLock(mLock);
    return start_l();
}
status_t NuPlayerDriver::start_l() {
    // 调用NuPlayer的start方法
    mPlayer->start();
}
// 在NuPlayerDriver的构造函数中对mPlayer进行赋值的
mPlayer(new NuPlayer(pid, mMediaClock)),
```
frameworks/av/media/libmediaplayerservice/nuplayer/NuPlayer.cpp
```c++
void NuPlayer::start() {
    (new AMessage(kWhatStart, this))->post();
}
```
从这一步开始，真正的媒体处理逻辑启动了。代码位于 frameworks/av/media/libstagefright/。

NuPlayerDriver 是播放器的驱动封装，它的 start() 方法会调用 NuPlayer 的 start()。

NuPlayer 内部维护着状态机。当调用 start() 时：

1. 如果数据源尚未解析，它会触发 MediaExtractor 来解析本地文件（R.raw.test 会被转换为 AssetFileDescriptor，最终由 FileSource 读取）。
2. 根据音视频流类型，创建 NuPlayer::Decoder（解码器）和 NuPlayer::Renderer（渲染器）。
3. 解码器通过 Codec2（Android 14 默认）或 OMX（旧版）与硬件/软件解码器通信。
4. 音频数据解码后，通过 AudioSink（实际为 AudioTrack）将 PCM 数据写入 AudioFlinger 服务进行混音和输出。

完整流程：
```
App Java层 (mediaPlayer.start())
⬇ JNI
JNI Bridge (android_media_MediaPlayer_start 获取 sp<MediaPlayer>)
⬇ 同进程 Native 调用
libmedia客户端 (MediaPlayer::start() 发起 Binder 调用)
⬇ Binder IPC（跨进程）
MediaPlayerService服务端 (Client::start() 调用 NuPlayerDriver)
⬇
NuPlayerDriver (start() 触发状态机)
⬇
NuPlayer (创建提取器、解码器、渲染器)
⬇
Codec2 / OMX (硬件/软件解码)
⬇
AudioFlinger / SurfaceFlinger (音频输出/视频渲染)
```
