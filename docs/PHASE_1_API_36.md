# Phase 1: API 36 compatibility

Base: `dev` at `e2097ee`, inspected 26 October 2026. This base already compiles and targets API 36 with AGP 8.11.1, Gradle 8.13, Kotlin 2.2.21, JVM 17 bytecode and minSdk 23. It also already opens Home without Firebase. This phase verifies that work rather than redoing the previous modernization. AGP 8.11 supports API 36 and requires Gradle 8.13/JDK 17: [official compatibility table](https://developer.android.com/build/releases/agp-8-11-0-release-notes).

## Behavior review

| Change checked | Application evidence and disposition |
|---|---|
| API 34 foreground-service types; API 35 boot/while-in-use restrictions and time limits | No foreground services or `startForegroundService` calls. Accessibility and notification listeners are system-bound services. No new type/permission or boot-started FGS is needed. |
| API 34 runtime receiver export flags | `AppLaunchDetector` uses `ContextCompat.RECEIVER_NOT_EXPORTED`. The reconciliation receiver is non-exported; its OS broadcasts and explicit in-app alarm remain supported. |
| API 34 implicit intents / mutable PendingIntent restrictions; API 35/36 background launches | Both pending intents are explicit and immutable. Notifications open Home directly. Alarm broadcasts only reconcile Room. Blocking uses an accessibility overlay; the details activity opens on a user tap, not automatically. No general BAL opt-in is added. |
| Exact alarms / background execution | No exact-alarm permission/API. `setAndAllowWhileIdle` is an inexact recovery hint. Session validity derives from timestamps; no JobScheduler/WorkManager, wake lock, or fixed-rate executor. Doze and force-stop can delay recovery. |
| Notification permission / restricted notification content | Posting checks API 33+ `POST_NOTIFICATIONS` and catches revocation. The notification listener uses package identity and the ongoing flag, not notification text. Sensitive-content redaction does not affect this filter. Permission prompts are unchanged. |
| API 35 edge-to-edge and API 36 removal of opt-out | Main, history, blocking details and video roots already apply system-bar, display-cutout and IME insets. The service overlay also applies insets. No edge-to-edge opt-out exists. |
| API 36 predictive back | No `onBackPressed`/`KEYCODE_BACK` interception. AppCompat/Navigation handle back; manifest predictive-back support is enabled. Check gesture back in the manual matrix. |
| API 36 large-screen resizing / text metrics | No fixed-orientation, aspect-ratio or resizability restrictions. Existing XML/scroll containers remain unchanged. Tablet, split-screen and large-font checks remain manual. |
| API 34 selected media / API 36 MediaStore changes | No broad media permission or MediaStore version cache. The remaining local profile picker uses a document URI; dead profile code is reviewed separately in Phase 2. |
| API 34 Java/UUID and API 35 collection overload changes | UUIDs are generated, not parsed from external strings. No affected regex group handling, `removeFirst`/`removeLast`, `getFirst`/`getLast` or fixed-rate scheduling. Java time uses core-library desugaring for API 23. |
| API 35 16 KB page sizes | AGP packages native libraries with supported alignment; DataStore contributes a native shared-counter library. Inspect APK/AAB ELF and ZIP alignment again with the final R8 release in Phase 5. Actual 16 KB runtime remains a separate device check. |
| API 36 job quotas; Bluetooth/health/network security changes | No jobs/download manager, Bluetooth, health sensors or network permission/traffic in the application. No application change required. |
| Accessibility on API 36 | Retains package-window events only, `canRetrieveWindowContent=false`, and the system binding permission. Added an opt-in real-device test that launches a separate test-package activity, checks one overlay/countdown, and waits for automatic expiry. It preserves secure settings and consent afterward. |

Official review sources: [Android 14 target changes](https://developer.android.com/about/versions/14/behavior-changes-14), [14 all apps](https://developer.android.com/about/versions/14/behavior-changes-all), [Android 15 target changes](https://developer.android.com/about/versions/15/behavior-changes-15), [15 all apps](https://developer.android.com/about/versions/15/behavior-changes-all), [Android 16 target changes](https://developer.android.com/about/versions/16/behavior-changes-16), [16 all apps](https://developer.android.com/about/versions/16/behavior-changes-all), [background activity security](https://developer.android.com/guide/components/activities/secure-bal).

## Scope and manual release checks

New product UI edits in this phase: **none**. Permission flow, dependencies, Room entities and schema are unchanged. Test fixture UI exists only in the test APK. Do not run accessibility instrumentation on a personal installation: use an isolated emulator and opt in with `-e accessibilityChecks true`.

- [ ] Grant accessibility through the existing disclosure/settings flow; deny, revoke and grant again.
- [ ] Rapidly switch selected apps, open Details/Home from the overlay, and wait for timer/interval start and expiry without a new launch.
- [ ] Exercise lock/unlock, reboot, Doze, service disconnection and OEM background restrictions.
- [ ] Test gesture/three-button back, IME, landscape, cutouts, large fonts and a tablet/foldable. Confirm existing design and permission sequence.
- [ ] Fresh install opens Home without login; existing permission/reward dialogs still behave as before.
- [ ] Upgrade a real older signed release with schedules/history/coins. Keep data and signing lineage; reconstructed migration fixtures do not replace this check.

## Executed checks (26 October 2026)

`gradlew.bat -g D:\StudioProjects\Limitr\.work\gradle --max-workers=1 "-Dorg.gradle.jvmargs=-Xmx1024m -Dfile.encoding=UTF-8" "-Pkotlin.compiler.execution.strategy=in-process" assembleDebug lintDebug testDebugUnitTest assembleDebugAndroidTest --console=plain` passed. The initial full build took 6m 5s; the test revision build took 3m 45s. All 23 unit tests passed (no skips), including existing migration fixtures. Lint has no errors; existing warnings remain visible without a baseline or disabled checks.

`aapt dump badging` confirms application ID `com.khadka.Limitr`, minSdk 23, compile/target SDK 36. `python tools/check_native_alignment.py app/build/outputs/apk/debug/app-debug.apk` and SDK `zipalign -c -P 16 4` passed for the debug APK.

The API 36 Google APIs x86_64 r7 emulator initially had system startup ANRs. Two instrumentation attempts timed out before tests began. A later run exercised all six tests, finding two test-timing races: the new overlay timer expired between observations and the existing four-second interval could expire before the assertion. Tests now leave a longer observation window and explicitly shorten the overlay session to test expiry. No application behavior was changed to accommodate these tests. Historical results in TESTING.md are not results of this phase.

Final device command: `adb -s emulator-5560 shell am instrument -w -e accessibilityChecks true com.khadka.Limitr.test/androidx.test.runner.AndroidJUnitRunner` — **all 6 tests passed in 61.914s**, including real accessibility detection, a single overlay, countdown and event-free expiry. Final lint: **0 errors, 202 warnings**. A normal launch also reached Home and the existing disclosure without login. Device resolution was reduced to 720x1280 and animations disabled to reduce emulator load; physical OEM behavior and production-signed upgrades remain untested.
