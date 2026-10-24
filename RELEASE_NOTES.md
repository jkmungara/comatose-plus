## Comatose+ 4.0.0

Comatose+ is now its own app. It has a new package name and app name, so it installs next to the original Comatose instead of replacing it.

### Changed
- **New package name:** `io.github.jkmungara.comatoseplus` (was `com.draco.comatose`).
- **New app name:** "Comatose+".
- The source tree, permission commands, README and docs all use the new name.
- No behavior changes. The Doze profiles, version-aware apply and verification are the same as 3.2.0.

### Upgrading from 3.x (important)
Because the package name changed, Android treats this as a different app. It does not update 3.x in place.

1. Install 4.0.0. You can keep 3.x installed while you try it, and uninstall it when you are happy.
2. Grant the permissions to the **new** package (the old grants do not carry over). On a rooted device the app does it for you. Otherwise:

       adb shell "pm grant io.github.jkmungara.comatoseplus android.permission.WRITE_SECURE_SETTINGS; pm grant io.github.jkmungara.comatoseplus android.permission.DUMP"

3. Pick a profile again. The last applied profile is not migrated.

If you had a profile applied in 3.x and uninstall it, tap **Default** in the old app first if you want the system settings reset, or the values stay until the next change.

### Known limitations
- Confirmed working on a Poco X8 Pro. The Android 12 to 13 path follows the AOSP source. If a profile does not take effect on your device, please open an issue with the output of `adb shell dumpsys deviceidle`.
- On Android 12 to 13, and wherever the shell fallback is used, values are set with `device_config`. The system or Google Play services may reset them later.
- The Shizuku option is not implemented. Use root or the copied ADB command.

### Credits
Fork of [tytydraco/Comatose](https://github.com/tytydraco/Comatose) by Tyler Nijmeh, released under the 2-Clause BSD license.
