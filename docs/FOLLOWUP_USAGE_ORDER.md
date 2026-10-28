# All Apps: most-used first over the last 24 hours

The previous All Apps implementation sorted by app name, regardless of usage. It also used `queryAndAggregateUsageStats` for a rolling 24-hour request, although Android can expand those aggregates to calendar interval boundaries. The user confirmed that the intended window is the last 24 hours.

## Behavior and implementation

- Read Android's `UsageStatsManager.queryEvents` on an IO dispatcher only while loading the visible list. A 24-hour lookback before the requested window initializes foreground state across the cutoff; only overlaps with the requested 24 hours count. No polling service, accessibility changes or saved usage-event history is added.
- Track resumed/paused activities and union overlapping activities within each package, including duplicate events. Older Android versions use single-foreground package behavior; Android 10+ allows multiple resumed packages. Screen-off/lock and shutdown events close active spans; startup discards unmatched spans rather than charging powered-off time. Background/foreground-service time does not count as interactive app use.
- Sort by raw foreground milliseconds descending, then case-insensitive name and package for stable ties. Zero-use apps appear last. Usage Access denial, revocation or a null result produces alphabetical ordering and the existing unavailable marker; granted access with no observed use shows zero.
- The existing duration, percentage and bar use one snapshot. Percentages represent each app's share of foreground time among the listed, non-essential launchable apps. Brief positive use shows `<1 m` / `<1%`; finer progress units reduce rounding loss for small values. Existing normal/warning/danger colors and their 50%/80% thresholds remain.
- Refresh when All Apps resumes, including after returning from usage settings or another app. Cancel loading when it pauses or its view is destroyed.

Only list ordering, existing indicator values and their accessibility descriptions change. No XML layout/design, permission sequence, Room entity/schema, signing configuration or accessibility blocking mechanism changes.

## Why this API

Usage Access is the existing optional, user-granted permission intended for usage statistics. It avoids creating a permanent collector and does not rely on LIMITR's accessibility service staying alive. Foreground events fit a rolling 24-hour window; aggregate buckets do not promise exact boundaries. Visible time can include apps that are not actively foreground, and foreground-service time can include background audio/work, so neither is used for this ranking.

Official references: [UsageStatsManager queries and interval boundaries](https://developer.android.com/reference/android/app/usage/UsageStatsManager), [foreground versus visible/service time](https://developer.android.com/reference/android/app/usage/UsageStats), [activity and device events](https://developer.android.com/reference/android/app/usage/UsageEvents.Event).

## Limits / suggested but not done

These are estimates from the events Android retains, not a guaranteed Digital Wellbeing match. Missing OEM events, history truncation, manual clock changes and same-class multi-instance activities can reduce accuracy. If an activity's opening event predates the extra lookback and has been discarded, the app does not invent a duration. Multiple resumed packages can contribute overlapping time. No broad architecture rewrite, continuous tracking, extra permissions or database storage is introduced.

## Manual checklist

- [ ] Without Usage Access: alphabetical list, unavailable markers, empty bars; app details and blocking still work.
- [ ] Grant Usage Access through the existing permission dialog and return: used apps move above unused apps, greatest duration first.
- [ ] Use one app briefly and another longer; return to LIMITR and verify the order, durations, percentages and bar lengths agree.
- [ ] Verify ties remain stable, sub-minute usage is not labelled zero, and excluded system apps do not dilute the displayed percentages.
- [ ] Tap rows after reordering and confirm the correct package opens in details.
- [ ] Revoke Usage Access and return: previously shown values clear; restore access and verify recalculation.
- [ ] Switch tabs, rotate and leave during loading; no stale results, duplicate list updates or crashes.
- [ ] Check a midnight/24-hour boundary, screen lock/unlock, reboot and split-screen behavior on physical OEM devices.

## Executed validation, 28 October 2026

```powershell
.\gradlew.bat -g .work/gradle --max-workers=2 `
  '-Dorg.gradle.jvmargs=-Xmx1536m -Dfile.encoding=UTF-8' `
  '-Pkotlin.compiler.execution.strategy=in-process' `
  assembleDebug lintDebug lintRelease testDebugUnitTest assembleDebugAndroidTest --console=plain

# Isolated API 36 device; repeat with emulator-5562 for API 23.
adb -s emulator-5560 shell am instrument -w `
  -e accessibilityChecks true -e notificationChecks true -e usageChecks true `
  com.khadka.Limitr.test/androidx.test.runner.AndroidJUnitRunner
```

- Final debug/test packaging and debug/release lint gate: **PASS, 2m 22s**. **49 unit tests pass**, zero failures/errors/skips. Debug and release lint each report **0 errors, 108 existing warnings**.
- Added 16 unit tests for rolling-window clipping, carry-in/open activities, handoffs/duplicates, multi-resume and older single-foreground behavior, screen-off/shutdown/startup, incomplete events, descending raw-duration ordering, stable ties, visible-app percentages, tiny positive values, permission denial/revocation and the actual Android reader via Robolectric.
- **API 36: 13/13 device tests pass, 44.192s. API 23: 7/7 supported tests pass, 22.705s.** Six newer-API tests are excluded on API 23, not counted as passes. The new opt-in usage test temporarily grants/revokes the usage AppOp, measures a real foreground fixture, and restores the original mode. Existing accessibility/overlay, timing, migration, Crypto and editor regressions also pass where supported.
- API 36 manual UI: granted Usage Access through the existing disclosure/settings flow; observed Chrome above Calendar/unused apps; verified `<1 m` / `<1%` and the small bar for brief Calendar use; tapped the first reordered row and confirmed Chrome details. Revoked/restored the AppOp while backgrounded: old values cleared to unavailable/alphabetical, then recalculated. Tab switching and portrait/landscape recreation preserved the correct list. No XML layout changes.
- Updated isolated debug installations in place without clearing data. This follow-up changes no Room schema; the existing migration test still passes. This is not a production-signed old-version upgrade test.
- API 36 and API 23 crash buffers were empty after the short checks. Physical OEM behavior, long-duration/midnight/reboot usage accuracy and actual release-signing-lineage upgrades remain manual gates.
- `assembleRelease bundleRelease` with the same cache/worker/JVM flags: **PASS, 3m 8s**, including R8 and resource shrinking. APK signature verification, AAB JAR signature verification, bundletool validation, APK zipalign and APK/AAB native ELF alignment checks pass. Outputs are under `app/build/outputs`; the user's separate `app/release/app-release.aab` was not replaced. This run validates release packaging, not a new release runtime smoke.
- Release packaging used the existing private local signing configuration. The certificate matches the user's replacement upload key (`C0:32:7A:8C:EA:EB:52:DA:24:00:BE:66:F4:86:2C:8B:E0:91:84:EC`). Play Console acceptance/upload-key-reset activation was not verified and no bundle was uploaded.
- Local evidence: `.work/usage-order-final-build.log`, `.work/usage-api36-tests.log`, `.work/usage-api23-tests.log`, `.work/usage-api36-{two-apps,revoked,restored}.png` and the two device crash logs. These are untracked test artifacts.

The user's pre-existing IDE, version/signing-file and SessionClock edits are outside this change and remain uncommitted. Validation used that existing local version configuration (12 / 1.3.0); this follow-up does not bump the committed version again.
