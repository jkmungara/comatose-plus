## Comatose+ 4.1.0

Automation and insights: schedule your profiles, switch from Quick Settings, and see whether the changes actually help.

### New
- **Sleep schedule.** Pick a sleep window and a profile for inside and outside it, for example Extreme from 23:00 to 07:00 and Light the rest of the day. Switching happens at the exact time (exact alarms are granted automatically on Android 13 and newer) and the alarms are restored after a reboot, a clock or timezone change, and an app update.
- **Auto-revert.** Profiles you pick by hand can return to the schedule (or Default) after 2, 4, 8 or 12 hours.
- **Quick Settings tile.** Add the Comatose+ tile and tap it to cycle Default, Light, Moderate, High and Extreme.
- **Battery history.** Tracks battery lost per hour on each profile, using sessions of at least an hour while not charging, with a per-profile average and a list of recent sessions. Use a profile overnight, then compare it with Default.
- **Doze check.** Reads the system's Doze history and shows the current idle state, how long the phone spent in deep idle over the recent history, and how many stretches and maintenance windows there were.
- **Doze-exempt apps.** Lists apps that are exempt from Doze. Tap one to copy the command that removes its exemption (changing the list needs a shell or root).

### Notes
- Doze check, Doze-exempt apps and verification need the optional `DUMP` permission:

      adb shell pm grant io.github.jkmungara.comatoseplus android.permission.DUMP

- New permissions: exact alarms (schedule and auto-revert), and `QUERY_ALL_PACKAGES` so the exempt apps list can show app names.
- Battery history includes anything you did with the phone during a session, so compare overnight sessions. A session that included charging is discarded when the level went up.
- Force-stopping the app clears its alarms until you open it again.
- No "switch while charging" rule. Android does not deliver plug and unplug events to apps in the background, so it would need a permanent foreground service.
- A profile picked by hand stays until the next scheduled change or the auto-revert time.

### Known limitations
- Confirmed working on a Poco X8 Pro. The Android 12 to 13 path follows the AOSP source. If a profile does not take effect on your device, please open an issue with the output of `adb shell dumpsys deviceidle`.
- On Android 12 to 13, and wherever the shell fallback is used, values are set with `device_config`. The system or Google Play services may reset them later.
- The Shizuku option is not implemented. Use root or the copied ADB command.

### Credits
Fork of [tytydraco/Comatose](https://github.com/tytydraco/Comatose) by Tyler Nijmeh, released under the 2-Clause BSD license.
