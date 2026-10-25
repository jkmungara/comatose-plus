# Comatose+

Put your Android device into a deeper sleep by tuning Doze, the system's idle battery saver.
Pick a profile, and the system enters idle sooner, stays there longer, and wakes less often.

Comatose+ is a maintained fork of [tytydraco/Comatose](https://github.com/tytydraco/Comatose),
which had not been updated since 2022 and no longer worked reliably on recent Android versions.

## What's different from the original

- **Works on modern Android.** Doze reads its settings from different places depending on the Android version.
  Comatose+ picks the right one for Android 11 through 16 (see the table below).
- **Verifies the result.** It reads `dumpsys deviceidle` and tells you whether the system is really using the profile,
  instead of assuming the write worked.
- **Root and ADB fallback.** Where an app cannot set the values itself, it uses root if available, or shows the exact
  `adb shell device_config ...` command with a copy button.
- **Shows the active profile** with a check mark and a status line.
- **Handles failures.** No crash when the permission is revoked, a bounded root prompt, and a permission screen you can leave.
- **Modern build.** Android Gradle Plugin 8.7, Gradle 8.10, Kotlin 2.0, target SDK 35, signed releases built by GitHub Actions.
- **Simplified UI.** The About section and the Google Play services licenses dependency are removed.
- **Its own app.** Separate package name and app name ("Comatose+"), so it can be installed alongside the original.
- Minimum Android version is 8.0 (API 26).

Extra permissions used by the automation features: exact alarms (the schedule, granted automatically on Android 13+) and
`QUERY_ALL_PACKAGES` (to show app names in the exempt apps list).

## Features

- **Profiles** from Default to Extreme, applied the way your Android version needs it.
- **Sleep schedule.** For example Extreme from 23:00 to 07:00 and Light the rest of the day. The app switches at the exact time and back again.
- **Auto-revert.** Profiles you pick by hand can return to the schedule (or Default) after 2, 4, 8 or 12 hours.
- **Quick Settings tile.** Edit your Quick Settings panel to add the Comatose+ tile, then tap it to cycle through the profiles.
- **Battery history.** Records how much battery was lost per hour on each profile (sessions of at least an hour, not charging),
  so you can compare Extreme with Default.
- **Doze check.** Reads the system's Doze history and shows how much of the last hours the phone really spent in deep idle.
- **Doze-exempt apps.** Lists apps that ignore Doze and copies the command that removes an exemption.

Doze check, Doze-exempt apps and verification need the optional `DUMP` permission (see Install). There is no "switch while
charging" rule on purpose: Doze does not run while the phone is charging, so a profile has no effect on the charger anyway.

## Profiles

| Profile | Effect |
|---|---|
| Default | Remove all overrides and use the device's own idle settings |
| Light | Does not affect day-to-day usage |
| Moderate | Additional savings with slightly less timely notifications |
| High | Deep sleep where background processes and checks are delayed |
| Extreme | Maximum savings with large sacrifices to background work |

## Install

1. Download the APK from the [latest release](https://github.com/jkmungara/comatose-plus/releases/latest).
   Comatose+ uses its own package name (`io.github.jkmungara.comatoseplus`), so it installs next to the original
   Comatose and does not replace it.
2. Grant the permissions. On a rooted device the app does this for you. Otherwise run, on a computer with ADB:

       adb shell "pm grant io.github.jkmungara.comatoseplus android.permission.WRITE_SECURE_SETTINGS; pm grant io.github.jkmungara.comatoseplus android.permission.DUMP"

   `WRITE_SECURE_SETTINGS` is required. `DUMP` is optional and enables verification.
3. Open the app and tap a profile.

## How it works

Checked against the AOSP `DeviceIdleController` source:

| Android | API | Doze reads | What Comatose+ does |
|---|---|---|---|
| 11 and older | 30- | `Settings.Global.device_idle_constants` | writes the setting |
| 12, 12L, 13 | 31-33 | DeviceConfig namespace `device_idle` only (the setting is ignored) | needs root, or you run the shown `device_config put device_idle ...` commands |
| 14, 15, 16 | 34+ | both; the setting overrides the matching DeviceConfig keys | writes the setting, and falls back to DeviceConfig if the system is not using it |

`light_pre_idle_to` was removed in Android 14 and is ignored there. The other profile keys still exist in Android 16.

Since Android 12 apps cannot read `device_idle_constants` back (it is a hidden setting). That is why verification uses
`dumpsys deviceidle`, which shows the values Doze is actually using. Without the `DUMP` permission the app can only show
which profile it last applied.

## Troubleshooting

    adb shell dumpsys deviceidle          # the "Settings:" section shows the values Doze is using
    adb shell device_config list device_idle

If a profile does not take effect, please open an issue and include the `dumpsys deviceidle` output and your device and
Android version. Google Play services and some vendor ROMs have been reported to reset these values.

## Build

Requires JDK 17.

    ./gradlew testDebugUnitTest assembleDebug

Tagging `v*` runs the release workflow, which builds a signed APK and publishes a GitHub release using `RELEASE_NOTES.md`.

## License and credits

2-Clause BSD, see [LICENSE](LICENSE). Original app by Tyler Nijmeh ([tytydraco](https://github.com/tytydraco)).
