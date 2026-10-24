## Comatose+ 3.1.1

A fix release for layout problems found on Android 15 and 16 devices (reported on a Poco X8 Pro). Everything in [3.1.0](https://github.com/jkmungara/comatose-plus/releases/tag/v3.1.0) still applies.

### Fixes
- **Layout under the status and title bars.** On Android 15 and newer, apps targeting SDK 35 are forced edge-to-edge, which pushed the profile list under the status bar and title bar and hid the "Current state" row. The app now opts out of the forced edge-to-edge layout and goes back to the original layout and bar colors. The manual inset handling added in 3.1.0 is removed.
- **Message bar behind the navigation buttons.** Messages are now shown above the system navigation bar.

### Improved
- When the system does not keep a profile, the message now shows the value it actually stores (or "(empty)"), which helps diagnose devices that ignore or reset the setting.

### Install
Same signing key as 3.1.0, so it installs over 3.1.0. If you still have the original Comatose, uninstall it first (same package name, different signature). The permission is granted automatically on rooted devices, or with:

    adb shell pm grant com.draco.comatose android.permission.WRITE_SECURE_SETTINGS

### Known limitations
- Not tested across a wide range of devices. Some Android versions and OEM builds ignore or reset `device_idle_constants`. If a profile does not stick, the app now says what is stored, and these commands show what the system is using:

      adb shell settings get global device_idle_constants
      adb shell dumpsys deviceidle

### Credits
Fork of [tytydraco/Comatose](https://github.com/tytydraco/Comatose) by Tyler Nijmeh, released under the 2-Clause BSD license.
