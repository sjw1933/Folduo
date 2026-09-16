# Galaxy Z Fold8 SM-F9710 notes

The `fold8-support` branch lets Folduo run on the Galaxy Z Fold8 SM-F9710 (China model). Everything below was tested on one phone on 2026-09-16.

## Tested device

| Item | Value |
| --- | --- |
| Model / device | SM-F9710 / h8q |
| OS | Android 17 (SDK 37), One UI 9.0 |
| Build | `F9710ZSS2AZH7` |
| Shizuku | 13.6.0.r1086.2650830c, started over ADB |
| Folduo | 0.1.21 plus this branch, built locally and signed with a debug key |

## What changed compared with upstream

- `DeviceSupport` lists the supported models, SM-F966Z and SM-F9710. The three hard-coded SM-F966Z checks now use it, and the cover wallpaper helper has the same list.
- `AngleLog` matches the wallpaper angle log in both the Fold7 bracket format and the Fold8 equals format. This is the Fold8 format:
  `SprWallpaper|FoldInteractive: onCommand: action=jp.bunkaich.sukashimotion.READ_ANGLE, mCurrentAngle=180.0, isVisible=true`
- The stock cover image is a webp resource on Fold8, so its URI has no `.png` suffix. The helper accepts both forms of the URI.
- `tools/fold8-probe.sh` is a read-only ADB probe for the device states, displays, hinge sensors and wallpaper that Folduo needs.

## What was measured on the phone

- `DeviceStateManager` has `CONCURRENT_INNER_DEFAULT` (4) and `CONCURRENT_OUTER_DEFAULT` (5), and both pass Folduo's property checks.
- Display 0 is the cover screen (1248×1972) and display 1 is the inner screen (2448×1848). This is the layout Folduo expects.
- The public hinge sensor (type 36) reports at 90° resolution. Samsung's Folding Angle sensor (type 65686) reports at 0.01°, but it needs `com.samsung.permission.SSENSOR`, which the shell does not have. Fine angles therefore come only from the FoldInteractive wallpaper, at 1° steps and about 60 Hz.
- After both screens used the angle-aware wallpaper, the phone completed a close handoff and an open handoff with no new errors or recoveries.

## Required setup

Upstream describes the cover wallpaper step as optional. On Fold8 it is required.

1. Set the inner home wallpaper to the stock FoldInteractive video `video_002.mp4`.
2. Run `python3 tools/cover-wallpaper.py apply` so the cover home screen uses the same wallpaper. Without this step, closing the phone hides the inner wallpaper and all angle lines say `isVisible=false`. Folduo then never receives a closed angle of 3° or less, and setup stays at "Close the phone fully once to finish setup."
3. Start Shizuku. Over USB, run the starter included in the Shizuku APK:
   `adb shell <shizuku apk dir>/lib/arm64/libshizuku.so`
   Run it again after every reboot.
4. Make the Folduo home the default HOME (tap **Use Folduo as the home app**). This is required on Fold8, see "Inner HOME while Folduo is active" below.

To restore the stock cover image, run `python3 tools/cover-wallpaper.py restore-stock`.

## Auto Blocker turns USB debugging off

Samsung's Auto Blocker (internal name `rampart`) turns itself back on after it has been switched off, about 30 minutes later by default (`rampart_auto_enabled_switch_enabled=1`). When it turns on, it sets `adb_enabled=0`. That stops adbd, which kills a Shizuku server started over ADB, and Folduo stops. The log shows `AutoTurnOnReceiver` from `com.samsung.android.rampart`, followed by `adbd_auth domain socket unavailable`.

To avoid this, turn off Auto Blocker's automatic turn-on option in Settings. Otherwise, start Shizuku again after each time it turns on.

## HOME screen handoff

On Fold8 each display runs its own launcher activity: the cover uses `com.sec.android.app.launcher/.Launcher` and the inner screen uses `.activities.LauncherActivity`. Each activity sits under its own HOME root task. Upstream's HOME handoff reparents the source launcher task into the destination HOME root with `moveTaskToRootTask`. On Fold8 this is rejected with `IllegalArgumentException: moveTaskToRootTask: Attempt to move task … to rootTask …`. The launcher tasks were left nested, the inner screen went black and the icons were laid out wrongly. With `DeviceSupport.separateHomes()`, Fold8 now shows the destination display's own HOME instead of moving the launcher task.

## Inner HOME while Folduo is active

When Folduo holds `CONCURRENT_OUTER_DEFAULT`, the cover is logical display 0 and the inner screen is logical display 1 (2448×1848, landscape, rotation 0, `FLAG_PRESENTATION`). Apps drawn on the inner screen, and the frosted transition over them, look correct in that state. The HOME screen on the inner display does not:

- The background is black. Samsung's wallpaper service attaches a wallpaper engine only to display 0 (`dumpsys wallpaper` lists connections for `displayId=0` only), so display 1 has no wallpaper.
- The Samsung launcher uses a different workspace layout on display 1 from the normal inner HOME.
- Display 1 keeps Samsung's own launcher task, so the Samsung launcher's HOME always looks like this on the inner screen.

The Folduo home works on Fold8 after the fix below, and on Fold8 it is required: with Samsung's launcher as HOME, the inner screen is black and laid out wrongly after unfolding. Set it as the default HOME: `adb shell cmd role add-role-holder android.app.role.HOME jp.bunkaich.sukashimotion`. To go back to the stock launcher, run the same command with `com.sec.android.app.launcher`.

### Moving the Folduo home

Upstream moves the Folduo home task into the destination HOME root with `moveTaskToRootTask`, then launches it from recents on that display. On Fold8, Android rejects `moveTaskToRootTask` whenever the target root is a HOME root. The first call fails, so the Folduo home stayed on the cover and the inner screen showed Samsung's black HOME. On models with `separateHomes()`, the Folduo home now skips that call and uses `startActivityFromRecents` with the destination display as the launch display. That also reparents the task. If it fails, Folduo shows the destination's own HOME instead. Samsung's launcher task is never moved.

Checked on the phone: closing and opening with the Folduo home moves it to the inner screen and back, with no rejected moves. While the task moves, `InputDispatcher` sometimes logs `Found window … HomeActivity in display 0, but it should belong to display 1`. No input problem has been seen.

## Not verified

- Long-term stability, battery drain, and use without USB (Shizuku over wireless debugging).
- Whether upstream's Fold7-tuned `GlassProjection` looks right on the Fold8 panel sizes. The effect shows, but it has not been tuned.
- Other Fold8 models and other One UI builds.
