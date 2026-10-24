## Comatose+ 3.1.0

A maintenance release of [Comatose](https://github.com/tytydraco/Comatose) that brings the project up to date, makes failures visible instead of silent, and trims the main screen. The Doze profiles themselves are unchanged.

### What's new
- **Active profile indicator.** The profile currently in effect gets a check mark, and a "Current state" line names it. If the stored value matches no profile (changed by another app, the system, or ADB) it says so. The indicator updates live while the app is open.
- **Honest apply results.** After writing a profile, the app reads the value back. You now get a clear message if the system did not keep it, if the write failed, or if the permission was revoked, instead of an unconditional "Successfully applied".

### Fixes
- No more crash when `WRITE_SECURE_SETTINGS` has been revoked. The app now takes you back to the permission screen. The boot receiver also handles this safely and skips the write if your profile survived the reboot.
- The permission screen no longer polls every 100 ms forever. Root grant runs once with a 30 second timeout, and the permission is re-checked when you return to the screen (for example after running the ADB command).
- Back on the permission screen now leaves the app instead of trapping you.
- Rotating the device no longer re-launches the permission screen or recreates the settings fragment.
- Content is padded for the navigation bar on Android 15 edge-to-edge.

### Changes
- **Removed the About section** (developer, source code, contact, licenses) from the main screen, along with the Google Play services licenses dependency.
- **Minimum Android version is now 8.0 (API 26)**, up from 6.0.
- App backup is disabled, and the boot receiver is no longer exported.
- Modernized build: Android Gradle Plugin 8.7.3, Gradle 8.10.2, Kotlin 2.0.21, compile and target SDK 35, Java 17, version catalog, GitHub Actions CI.
- Added unit tests for profile matching. The Play Store listing icon was dropped from the source tree.

### Install
1. Install the APK. It is signed with a new key, so it cannot update the original Comatose (same package name, different signature). Uninstall the original first.
2. Grant the permission, either automatically on a rooted device or over ADB:

       adb shell pm grant com.draco.comatose android.permission.WRITE_SECURE_SETTINGS

3. Open the app and pick a profile. Verify the SHA-256 in the attached `.sha256` file if you like.

### Known limitations
- This release has not been tested on a physical device across Android versions. Treat it as a preview.
- Android versions differ in where Doze reads its settings, and some devices may ignore or override `device_idle_constants` (Google Play services has been reported to reset it). The app tells you when the value does not stick, but it cannot tell whether Doze actually behaves differently. To check:

      adb shell dumpsys deviceidle
      adb shell device_config list device_idle

### Credits
Fork of [tytydraco/Comatose](https://github.com/tytydraco/Comatose) by Tyler Nijmeh, released under the 2-Clause BSD license.
