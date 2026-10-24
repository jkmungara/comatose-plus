# Comatose
Put your Android device into a deep sleep by tuning Doze.

The app writes a profile to `Settings.Global.device_idle_constants` and re-applies it after boot.
It needs the `WRITE_SECURE_SETTINGS` permission, which it requests from root automatically or you
can grant over ADB:

    adb shell pm grant com.draco.comatose android.permission.WRITE_SECURE_SETTINGS

## Does a profile actually take effect?
Android versions differ in where Doze reads its tunables, so check on your device:

    adb shell settings get global device_idle_constants
    adb shell dumpsys deviceidle          # look at the "Settings:" section
    adb shell device_config list device_idle

The app shows the active profile and tells you if the system did not keep the value.
Known caveat: other components (for example Google Play services) have been reported to reset this
setting, and recent Android versions may take Doze values from DeviceConfig (`device_idle`) instead.

## Build
Requires JDK 17.

    ./gradlew testDebugUnitTest assembleDebug
