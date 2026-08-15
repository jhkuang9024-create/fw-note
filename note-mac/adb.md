jhkuang@jhkuangdeiMac 移动高清 % adb devices

List of devices attached

192.168.11.185:5555	offline

192.168.11.232:5555	device

电脑连接了多个设备时使用adb

adb -s ip:port command

例: adb -s 192.168.11.185:5555 pull /data/test1.log

清除所有连接的设备：

adb kill-server
