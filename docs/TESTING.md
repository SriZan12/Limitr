# Validation and manual test matrix

No production crash reports, signed previous release, physical phone, manufacturer-specific device or Play Console access was available. Source findings are not production crash reproductions. No long-duration stability guarantee is made.

Baseline on unmodified dev: `gradlew.bat clean assembleDebug testDebugUnitTest lintDebug` failed before checks ran because Gradle 7.5 cannot run on installed JDK 21 (Unsupported class file major version 65). An initial master attempt also hit kapt/JDK IllegalAccessError. The baseline interval test copied its own implementation; it was replaced with tests of actual scheduling code.

Validation uses Gradle 8.13, AGP 8.11.1, Kotlin 2.2.21, SDK 36 and installed JDK 21, targeting JVM 17. Global Gradle registry permissions required `-g .work/gradle`. C: filled during Robolectric extraction; JVM test temporary files now use app/build/tmp/unit-tests. Initial environment failures and two corrected Integer/Long assertion failures are not counted as passing tests. One intermediate compilation failed while source/test signatures were edited during an active build; final validation uses stable sources.

## Automated coverage

- ScheduleEvaluatorTest: monotonic timers, exact end, reboot fallback/cap, interval endpoints, independent overlap, replacement/cancellation snapshots, malformed windows, overnight/next-day intervals, spring/autumn DST and equal local times.
- DatabaseMigrationTest: source-derived versions 3/4/6/7/8/9/10/12 and extra-column version 9; real Room 13 validation, legacy archive/history preservation, unknown-version refusal without deletion and reopen without repeated import. These are reconstructed fixtures; shipped historical schema exports are missing.
- SessionRepositoryTest: Room transactions, replacement/stale cancellation, duplicate labels, expiry deletion, legacy rebase and conditional option edits.
- PermissionStateTest: null settings, consent, enabled versus connected state, storage errors and revocation.
- SessionInstrumentedTest: Activity recreation, expiry without a window event, scheduled-start publication, 30 repeated save/replace/cancel cycles and stale reconciliation. Tests use a dedicated fixture package and clean up its sessions; run on an isolated installation.
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
| Offline/profile | No sign-in/network gate; balances retained; reward taps award once; local photo access revocation; owner handles old remote-account deletion. |
| Soak | Repeated multi-hour sessions across OEMs, memory/CPU sampling and redacted Logcat/Android vitals; not established by short automated tests. |

## Commands

`gradlew.bat -g .work/gradle --max-workers=2 assembleDebug testDebugUnitTest lintDebug assembleRelease bundleRelease assembleDebugAndroidTest`

With a booted isolated device: `gradlew.bat -g .work/gradle --max-workers=2 connectedDebugAndroidTest`. Run after build checks if host memory is constrained.

`python tools/check_native_alignment.py app/build/outputs/apk/release/app-release-unsigned.apk` and the same command for app/build/outputs/bundle/release/app-release.aab.

SDK tools: `zipalign -c -P 16 -v 4 <release-apk>` and `aapt dump badging <release-apk>`.

Reports are under app/build/reports/tests/testDebugUnitTest, app/build/reports/lint-results-debug.html, and app/build/reports/androidTests/connected. Local logs under .work are untracked. Final executed results follow after validation completes.
