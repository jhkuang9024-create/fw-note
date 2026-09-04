# Function

## apk安装限制

位置一：在文件frameworks/base/services/core/java/com/android/server/pm/PackageManagerShellCommand.java的onCommand(String cmd)中的runInstall()等三个入口前加判断限制。

```java
private static boolean isAdbInstallDisabled() {
    if (new File("/mnt/vendor/param/usermode").exists()) {
        // sys.disable_adb_pm_install = "true" (default) ADB install disable
        // sys.disable_adb_pm_install = "false" ADB install enable
        return "true".equals(SystemProperties.get("sys.disable_adb_pm_install", "true"));
    }
    return false;
}
// 检测到禁止安装后在串口等打印信息
getErrPrintWriter().println("Failed: adb/pm install is disabled");
```
影响位置：runInstall()、runInstallCreate()、runInstallExisting()

位置二：在frameworks/base/services/core/java/com/android/server/pm/PackageManagerService.java中的installPackageLI方法中做限制

## PhoneWindowManager.java中做按键事件拦截和功能定制

