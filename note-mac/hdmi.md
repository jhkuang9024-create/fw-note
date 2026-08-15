
amlogic android 9.0 模拟HDMI插拔命令:
模拟拔掉HDMI
echo 0 > /sys/class/amhdmitx/amhdmitx0/fake_plug
模拟插上HDMI
echo 1 > /sys/class/amhdmitx/amhdmitx0/fake_plug

