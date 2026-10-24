# Comatose+
Put your Android device into a deeper sleep by tuning Doze. A maintained fork of [tytydraco/Comatose](https://github.com/tytydraco/Comatose).

## How it works
Doze reads its timing values from different places depending on the Android version
(checked against the AOSP `DeviceIdleController` source):

| Android | API | Doze reads | What Comatose+ does |
|---|---|---|---|
| 11 and older | 30- | `Settings.Global.device_idle_constants` | writes the setting |
| 12, 12L, 13 | 31-33 | DeviceConfig namespace `device_idle` only (the setting is ignored) | needs root, or you run the shown `adb shell device_config put device_idle ...` commands |
| 14, 15, 16 | 34+ | both; the setting overrides the matching DeviceConfig keys | writes the setting, and falls back to DeviceConfig if the system is not using it |

`light_pre_idle_to` was removed in Android 14, so it is ignored there. The other keys still exist in Android 16.

## Permissions
Required, so the app can write the setting:

    adb shell pm grant com.draco.comatose android.permission.WRITE_SECURE_SETTINGS

Optional, so the app can confirm a profile is really in effect by reading `dumpsys deviceidle`:

    adb shell pm grant com.draco.comatose android.permission.DUMP

On a rooted device the app grants both itself. Since Android 12, apps cannot read `device_idle_constants` back
(it is a hidden setting), so without DUMP the app can only show which profile it last applied.

## Checking by hand
    adb shell dumpsys deviceidle          # "Settings:" shows the values Doze is using
    adb shell device_config list device_idle

## Build
Requires JDK 17.

    ./gradlew testDebugUnitTest assembleDebug

Released under the 2-Clause BSD license. Original app by Tyler Nijmeh.
