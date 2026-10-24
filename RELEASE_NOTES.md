## Comatose+ 3.2.0

Fixes the "The system did not keep the new value" message that showed for every profile in 3.1.x, and makes profiles work on the Android versions that need a different mechanism.

### What was wrong
`device_idle_constants` is a hidden setting, and since Android 12 apps cannot read it back (they get a SecurityException). The 3.1.x apps tried to read it after writing, saw nothing, and reported a failure even when the write had worked. 3.2.0 no longer reads the setting. It confirms the result a different way instead (see below).

### What's new
- **Version-aware apply.** Checked against the Android source: Android 11 and older read only the setting; Android 12 to 13 read only DeviceConfig (`device_idle`) and ignore the setting; Android 14 to 16 read both, with the setting overriding. The app now picks the right path for your version.
- **Real verification.** With the optional DUMP permission the app reads `dumpsys deviceidle` and compares the values Doze is actually using with the profile, and tells you if they match.
- **Root and ADB fallback.** When the setting alone is not enough (Android 12 to 13, or the system is not using it), the app sets the values with `device_config` if root is available. Otherwise it shows the exact `adb shell device_config put device_idle ...` command with a copy button.
- **Honest status line.** It shows the last applied profile, with "(verified)" when confirmed, and never claims a state it cannot check.
- The "Current state" row now has an icon so it lines up with the profile rows.

### Changes
- New optional `DUMP` permission. The permission screen now shows one command that grants both permissions. Existing installs keep working without DUMP; they just cannot verify.
- `light_pre_idle_to` is ignored on Android 14 and newer (removed upstream). The other profile keys still exist in Android 16.
- Unit tests for the dump parser, version selection and command generation.

### Install
Same signing key as 3.1.x, so it installs over those versions. To enable verification run once:

    adb shell pm grant com.draco.comatose android.permission.DUMP

### Known limitations
- Not tested across many devices. Please report what you see in `adb shell dumpsys deviceidle` if a profile does not take effect.
- On Android 12 to 13, and wherever the shell fallback is used, values are set with `device_config`. The system or Google Play services may reset them later.
- The Shizuku option is not implemented. Use root or the copied ADB command.

### Credits
Fork of [tytydraco/Comatose](https://github.com/tytydraco/Comatose) by Tyler Nijmeh, released under the 2-Clause BSD license.
