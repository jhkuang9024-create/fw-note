使用命令设置音量：
https://chat.deepseek.com/a/chat/s/ada1184a-6ff1-4e75-9668-2577ab988bd5

media volume 命令 (最推荐)
media volume --stream <stream_type> --set <volume_value>
<stream_type>：要控制的音频流类型，用数字表示。最常用的是 3（媒体音量）。
3：媒体音量 (STREAM_MUSIC)
0：通话音量 (STREAM_VOICE_CALL)
1：系统音量 (STREAM_SYSTEM)
2：铃声音量 (STREAM_RING)
4：闹钟音量 (STREAM_ALARM)
5：通知音量 (STREAM_NOTIFICATION)
<volume_value>：音量值，范围通常是 0 到 15。系统修改后也可能是0～255。
将媒体音量设置为5:
media volume --stream 3 --set 5
[v] will control stream=3 (STREAM_MUSIC)
[v] will set volume to index=10
[v] Connecting to AudioService

service call audio 命令 (更底层)
设置音量 (setStreamVolume)
adb shell service call audio 10 i32 <stream_type> i32 <volume_value> i32 1
audio 10：表示调用 setStreamVolume() 方法。
<stream_type>：同上，例如 3 代表媒体音量。
<volume_value>：音量值。
末尾的 i32 1：是固定参数。
示例：将媒体音量设置为10
adb shell service call audio 10 i32 3 i32 10 i32 1
2. 调整音量 (adjustStreamVolume)
用于相对地增加或减少音量。

adb shell service call audio 9 i32 <stream_type> i32 <adjust_type> i32 1
audio 9：表示调用 adjustStreamVolume() 方法。

<adjust_type>：调整方式。
1：增加音量
-1：降低音量
-100：静音
100：取消静音
示例：将媒体音量调高一级
adb shell service call audio 9 i32 3 i32 1 i32 1