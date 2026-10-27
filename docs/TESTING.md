> Latest QA fixes concurrent/interrupted Crypto unblocks and editor restoration. See [27 October results, limits and manual checks](QA_2026_10_27.md). The dated records below describe earlier builds.

> The preceding follow-up removed Help Instructions and closes details after successful Crypto unblock; see [validation and manual checks](FOLLOWUP_HELP_UNBLOCK.md).

> Current behavior: session controls and start/end alerts were removed after the historical runs below. Timers, intervals, history and 2-Crypto unblock remain. See [removal validation](FOLLOWUP_REMOVE_SESSIONS.md).

# Validation and manual test matrix

## Current five-phase validation, 26 October 2026

Each phase passed its build/lint/unit-test gate before the next branch began. Detailed commands, results, failures corrected before passing, and limits are in [Phase 1](PHASE_1_API_36.md), [Phase 2](PHASE_2_OFFLINE.md), [Phase 3](PHASE_3_ACCESSIBILITY.md), [Phase 4](PHASE_4_AUDIT.md) and [Phase 5](PHASE_5_RELEASE.md).

The final build passes debug/release lint (0 errors, 112 warnings), all 25 unit tests, signed R8 APK/AAB packaging and bundle/signature/alignment checks. All seven device tests pass on API 36. The optimized release passed a fresh-install Home/disclosure/Settings grant/reward/Chrome blocking/countdown/expiry/history/help-video smoke. Signing used a disposable local validation key, not a production key. A debug in-place Room 13→14 upgrade preserved existing history; actual production signing-lineage upgrades, physical OEM/16 KB devices and long-duration testing remain outstanding.

The baseline and dated records below describe the earlier modernization before `dev` commit `e2097ee`; their unsigned-release/API 35 results are historical, not the current phase results.

## Historical baseline and environment

No production crash reports, signed previous release, physical phone, manufacturer-specific device or Play Console access was available. Source findings are not production crash reproductions. No long-duration stability guarantee is made.

Baseline on unmodified dev: `gradlew.bat clean assembleDebug testDebugUnitTest lintDebug` failed before checks ran because Gradle 7.5 cannot run on installed JDK 21 (Unsupported class file major version 65). An initial master attempt also hit kapt/JDK IllegalAccessError. The baseline interval test copied its own implementation; it was replaced with tests of actual scheduling code.

Validation uses Gradle 8.13, AGP 8.11.1, Kotlin 2.2.21, SDK 36 and installed JDK 21, targeting JVM 17. Global Gradle registry permissions required `-g .work/gradle`. C: filled during Robolectric extraction; JVM test temporary files now use app/build/tmp/unit-tests. Initial environment failures and two corrected Integer/Long assertion failures are not counted as passing tests. One intermediate compilation failed while source/test signatures were edited during an active build; final validation uses stable sources.

## Automated coverage

- ScheduleEvaluatorTest: monotonic timers, exact end, reboot fallback/cap, interval endpoints, independent overlap, replacement/cancellation snapshots, malformed windows, overnight/next-day intervals, spring/autumn DST and equal local times.
- DatabaseMigrationTest: source-derived versions 3/4/6/7/8/9/10/12 and extra-column version 9; real Room 14 validation, legacy archive/history preservation, unknown-version refusal without deletion and reopen without repeated import. These are reconstructed fixtures; shipped historical schema exports are missing. HistoryMigrationTest also validates the exported 13→14 schema on device, preserving nonempty tables/null fields and checking the new index.
- SessionRepositoryTest: Room transactions, replacement/stale cancellation, duplicate labels, expiry deletion, legacy rebase and conditional option edits.
- PermissionStateTest: null settings, consent, enabled versus connected state, storage errors and revocation.
- RewardFlowTest: displaying first/daily reward offers never credits a balance; concurrent and repeated claims credit once; an existing balance is preserved and today's reward is not offered again.
- SessionInstrumentedTest: Activity recreation, expiry without a window event, scheduled-start publication, keyboard timer entry without first moving focus, 30 repeated save/replace/cancel cycles and stale reconciliation. The keyboard test uses public WindowInspector (API 29+); other tests support minSdk 23. Tests use a dedicated fixture package and clean up its sessions; run on an isolated installation.
- check_native_alignment.py: packaged ELF load segments. SDK zipalign checks packaging separately. Neither establishes actual 16 KB device behavior.

## Before production: manual/device matrix

| Area | Required cases |
|---|---|
| Android | APIs 23, 26, 30, 33, 34, 35, 36; physical OEM devices; fresh install and 16 KB image. |
| Upgrade | Same published ID/signing lineage; every shipped database version; schedules, history, balances, settings retained; accessibility re-consent; missing-package legacy rows reported. |
| Timer | Seconds/hours, wall-clock forward/back, lock/unlock, task removal, process recreation, expiry while overlay visible. Record observed delays. |
| Interval | Start/end while selected app remains foreground; midnight, DST, time-zone/clock changes; same start/end; multiple apps, replacement and rapid cancellation. One-shot, not recurring. |
| Detection | Rapid switching/repeated launches, window noise, package removal/rename, no duplicate windows/loops. Settings, dialer/emergency, launcher, installer, permission controller, system UI and keyboards remain available. |
| Permissions | Decline disclosure, deny/revoke/restore all optional permissions, accessibility interruption/disconnection. Saved sessions must not claim operational enforcement when unavailable. Only selected non-ongoing notifications are dismissed. |
| Recovery/power | Reboot/update/restore, stale alarms, Doze, battery saver and OEM restrictions; measure memory/CPU/wakeups. Force-stop prevents self-recovery until launch. API 23 force-stop plus reboot needs particular attention. |
| XML UI | Rotation, tablet/foldable resizing, 200% fonts, keyboard, cutouts, gesture/three-button navigation, predictive back, split-screen/PiP exposure. |
| Offline | No sign-in/network gate; balances retained; reward taps award once; owner handles old remote-account deletion. |
| Soak | Repeated multi-hour sessions across OEMs, memory/CPU sampling and redacted Logcat/Android vitals; not established by short automated tests. |

## Commands

`gradlew.bat -g .work/gradle --max-workers=2 assembleDebug testDebugUnitTest lintDebug assembleRelease bundleRelease assembleDebugAndroidTest`

With a booted isolated device: `gradlew.bat -g .work/gradle --max-workers=2 connectedDebugAndroidTest`. Run after build checks if host memory is constrained.

`python tools/check_native_alignment.py app/build/outputs/apk/release/app-release.apk` and the same command for app/build/outputs/bundle/release/app-release.aab. Release packaging now requires the local signing configuration in [RELEASE.md](RELEASE.md).

SDK tools: `zipalign -c -P 16 -v 4 <release-apk>` and `aapt dump badging <release-apk>`.

Reports are under app/build/reports/tests/testDebugUnitTest, app/build/reports/lint-results-debug.html, and app/build/reports/androidTests/connected. Local logs under .work are untracked.

## Executed device checks, 12 October 2026

An isolated API 35 Google Play x86_64 emulator was created with 2 GB RAM. No personal emulator data was changed. No API 23 or API 36 runtime, OEM phone, 16 KB runtime or production signed upgrade was tested. Host memory pressure caused slow rendering and an emulator System UI ANR during initial setup; checks resumed after stopping task-owned build/emulator processes and restarting the isolated device. This is not attributed to a LIMITR crash.

- Fresh installation opens directly into local operation without authentication or usage-access permission. App discovery, session details and accessibility disclosure/settings grant were exercised. Android reported TYPE_WINDOW_STATE_CHANGED only and zero screen-content capabilities for the service.
- Accessibility settings being enabled while the service is disconnected is displayed separately. UIAutomator inspection temporarily suppresses other accessibility services; enforcement checks therefore used screenshots and window dumps, not UIAutomator.
- Reproduced keyboard timer input remaining uncommitted on Start. After the fix, typing 30 seconds then Start and launching Chrome displayed exactly one accessibility overlay with a decreasing countdown. Chrome stayed foregrounded throughout; the overlay disappeared without interaction after expiry. Window/screenshot samples completed 8.89 s and 16.20 s after the Start command with one window, and at 36.63 s with none. These sparse samples include ADB and host delays and establish neither exact latency nor a worst-case bound.
- The home screen's dark system-bar inset background and readable light icons were visually confirmed after the contrast fix.
- Crash-buffer Logcat contained no entries during these short checks. This is not a long-duration stability result.

An initial `connectedDebugAndroidTest` attempt had no connected device after the emulator was stopped for host-memory recovery. A later direct `adb shell am instrument -w com.khadka.Limitr.test/androidx.test.runner.AndroidJUnitRunner` passed the original four tests in 17.685 s. An intermediate test setup failure (`@After` returning nullable Unit instead of JVM void) was fixed before this passing run.

## Historical executed build results, 12 October 2026

The following completed successfully in **7m 34s** on the final application/test sources, including the keyboard and status-bar fixes:

```powershell
.\gradlew.bat -g .work/gradle --max-workers=1 `
  '-Dorg.gradle.jvmargs=-Xmx1024m -Dfile.encoding=UTF-8' `
  '-Pkotlin.compiler.execution.strategy=in-process' `
  assembleDebug testDebugUnitTest lintDebug assembleRelease bundleRelease connectedDebugAndroidTest --console=plain
```

| Check | Observed result |
|---|---|
| Debug / release APK / release AAB | All built successfully. Release outputs remain unsigned. |
| Unit tests | 21 passed; zero failures, errors or skips. Migration tests iterate the source-derived historical fixture shapes. |
| Connected instrumentation | 5 passed on API 35; zero failures or skips, including the keyboard-entry regression. |
| Lint | Zero errors, 232 warnings. Existing unused resources, typography, dependency-update and style warnings remain visible; no lint baseline was added. |
| Effective APK identity | `aapt dump badging`: `com.khadka.Limitr`, versionCode 10, versionName 1.1, minSdk 23, targetSdk 36, compileSdk 36. |
| APK packaging | `zipalign -c -P 16 -v 4`: Verification successful. |
| Native ELF alignment | Script passed for APK and AAB: DataStore's shared-counter library has 16 KB PT_LOAD alignment on all four packaged ABIs. No other native libraries were present. |

Gradle 9 deprecation warnings and Kotlin annotation-target warnings remain; the pinned Gradle 8.13 build passes. The release manifest contains no Firebase provider, Google sign-in component, Internet permission, broad package-visibility permission, exact-alarm permission or application-overlay permission. Production signing, real release-data upgrades and the untested device scenarios above remain mandatory before rollout.

## UI restoration checks, 13 October 2026

The original home/overflow menu and permission/Crypto popup flow were restored without changing the blocking engine or database schema. Validation used the same isolated API 35 emulator; a connected personal phone was not targeted.

- `assembleDebug testDebugUnitTest lintDebug assembleDebugAndroidTest` passed in 6m 22s with the one-worker, 1 GB heap and in-process Kotlin flags above. **23 unit tests passed** with zero failures, errors or skips, including two new reward regressions.
- After adding immediate status refresh to the dialog-result handlers, `assembleDebug lintDebug assembleRelease bundleRelease assembleDebugAndroidTest` passed in **5m 34s**. Lint reports **zero errors and 218 warnings**. Release APK and AAB remain unsigned.
- Installed the final debug/test APKs using `adb -s emulator-5554 install -r` and ran `adb -s emulator-5554 shell am instrument -w com.khadka.Limitr.test/androidx.test.runner.AndroidJUnitRunner`: **5 tests passed in 15.663s**. An explicit device serial kept the connected personal phone out of the run.
- Final release APK metadata still reports the published application ID, minSdk 23 and target/compile SDK 36. APK zipalign and APK/AAB native ELF alignment checks passed again.
- Manual UI checks: fresh-launch disclosure; checkbox retained through rotation; explicit consent followed by the permission popup; Android settings grant followed by the updated Granted button; Continue followed by the 15-Crypto first reward; Claim updating the home balance; no profile icon or horizontal action row; overflow menu opens. With the service connected, the availability message is hidden. After force-stop/relaunch, the balance stays at 15, permission setup reflects unavailable access, and continuing does not offer the already-claimed reward again.
- At 200% font scale the restored header fitted the emulator screen. Existing app-list usage captions clip at that scale; this check does not establish full-screen large-font support. System UI/Messages ANRs occurred during emulator startup under host build load; they were not attributed to LIMITR. No new API 36, OEM, long-duration or signed-upgrade testing was performed.

## Blocking screen and full UI review, 13 October 2026

Compared active XML screens against published commit `fe69574`; restoration details and original spacing values are in [UI_REVIEW.md](UI_REVIEW.md). This pass restores the two timer/interval cards, notification/history controls, unblock popup, blocking overlay, history rows, list padding and help layout. Scheduling, Room and reward transaction logic are unchanged.

Initial `assembleDebug testDebugUnitTest lintDebug assembleDebugAndroidTest` passed in **5m 5s** using the same cache/worker/JVM options above. All **23 unit tests passed**; lint reported zero errors and 200 warnings. Subsequent visual review corrected image scaling for the bell/history controls and the history back-arrow contrast; the help layout was also restored before final packaging.

Executed on the isolated API 35 emulator only:

- Opened Chrome's configuration from All Apps; checked original card assets, fonts, divider, margins, button placement and normal system-bar insets.
- Used the original three-wheel picker to start a one-minute timer. The Duration card counted down, Chrome showed the restored icon-card/Remaining Time overlay, and Details opened the configuration screen. The unblock popup showed the original coin/cost/button. After expiry, the configuration returned to Set Timer/Set Interval with no stale countdown.
- Saved a one-shot interval; the cards showed Starts in and the concrete date/time range. The blocked-app row and original styled history list showed the saved session. Stop session confirmation cleared the interval and reset both cards.
- Checked configuration and app rows at 200% font scale. Checked landscape configuration scrolling and the timer dialog's scroll-to-Start behavior at that scale. The app-list caption clipping observed in the preceding review is fixed by content-height rows and concise unavailable-usage text.
- The emulator displayed a System UI ANR during cold startup; it recovered after closing that system dialog. LIMITR's crash-buffer check was empty during these UI checks. This does not establish long-running or manufacturer-specific behavior.

Physical devices, API 23/API 36 runtime, tablets/foldables, every locale/font combination and signed production upgrades remain outside this executed UI review.

Final `assembleDebug lintDebug assembleRelease bundleRelease assembleDebugAndroidTest` passed in **5m 16s**. Final lint: **zero errors, 200 warnings**. Installed the final APKs with `adb -s emulator-5554 install -r`; `adb -s emulator-5554 shell am instrument -w com.khadka.Limitr.test/androidx.test.runner.AndroidJUnitRunner` passed **all 5 tests in 15.454s**. The final bell/history image scaling and Help popup were visually checked. Cancellation also remained effective after an explicit app force-stop and relaunch. Release outputs remain unsigned; APK metadata still reports the published ID, minSdk 23 and target/compile SDK 36, and 16 KB zipalign verification passed.

One test interval reappeared after terminating/restarting the virtual device with `emu kill`. Repeated cancellation survived both app force-stop/relaunch and a clean `adb reboot`; a copied test database/WAL contained zero saved sessions after that clean reboot. Abrupt virtual-device shutdown durability is not established by this review; this observation is not attributed to a confirmed application defect.
