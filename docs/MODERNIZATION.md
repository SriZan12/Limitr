# LIMITR modernization — 12 October 2026

## Baseline and scope

Work starts on `dev` at `9de86f1`. Existing IDE edits and the Application whitespace edit were preserved, with named Git stash backups. No AGENTS.md was found. The published `master` has application ID `com.khadka.Limitr`, namespace `com.khadka.limitr`, versionCode 9 and Room version 12; dev had a different application ID, versionCode 1 and Room version 9. Restore the published identity, including service component names, for upgrades. Keep minSdk 23. No signing credentials are changed. Published master history was merged into dev after implementing its history screen and preserving the already-discontinued referral behavior, allowing a conflict-free PR against master.

Baseline command: `gradlew.bat clean assembleDebug testDebugUnitTest lintDebug`. On dev, Gradle 7.5 fails during configuration on installed JDK 21 (`Unsupported class file major version 65`). The initial master build also failed in kapt with IllegalAccessError. Tests/lint did not complete. No production crash reports or Logcat reproductions were supplied.

## Prioritized findings (code-confirmed unless stated otherwise)

| Priority | Evidence | Impact / cause |
|---|---|---|
| P0 | dev app/build.gradle differs from published master | Different application ID cannot upgrade the published installation. |
| P0 | DatabaseModule.allowMainThreadQueries / fallbackToDestructiveMigration | Main-thread disk work; data erased on schema upgrade. Historical schema exports absent. |
| P1 | AppLaunchDetector.showOverlayScreen.observeForever | One permanent observer and unowned timer per block; retained service/views. Long-running crash link is a hypothesis without production traces. |
| P1 | NotificationListener.isAppBlocked | Continuation can resume repeatedly; observer never removed; unmatched notifications leave suspended calls; cancelAllNotifications removes unrelated notifications. |
| P1 | BlockAppActivity permission callbacks | TODO throws when reached. Null assertions on removed packages and null settings also crash. |
| P1 | DateAndTime / BlockAppActivity interval construction | End before start and tomorrow/midnight errors; timer uses mutable wall clock; expired rows cleared only from UI. |
| P1 | NotificationUtils / receivers | Unguarded exact alarms, stale alarms, unsafe extras and mutable PendingIntents. No boot recovery. |
| P1 | OverlayScreen / ActivityBlocked | Global flag and singleton window retain state across lifecycles; repeated background activity navigation; no reliable end boundary. |
| P2 | FragmentHome / VideoActivity / RecyclerView countdowns | Fragment-scope view collectors, unreleased player, unbounded per-bind countdowns. |
| P2 | Manifest/accessibility config | Broad package visibility, redundant permissions, content retrieval despite package-only detection. |
| P2 | BlockAppActivity.timerDialog | Reproduced on API 35: keyboard entry followed immediately by Start reads the previous NumberPicker value. Commit input focus before reading; covered by a device regression test. |
| P2 | MainActivity / edge-to-edge insets | Observed on API 35: white status icons on a white inset background. Give the inset area the app's dark background and set contrasting system-bar icons. |

## Decision before replacing enforcement plumbing

Preserve immediate duration timers and **one-shot** local calendar intervals. A passed start means its next occurrence; an end at/before start belongs to the following calendar day. No recurring model is introduced. Different apps overlap independently; saving for the same package replaces its session atomically. Explicit cancellation removes the current revision only.

Retain AccessibilityService package-window events. Use one TYPE_ACCESSIBILITY_OVERLAY owned by the connected service, with Home and session-details controls; no screen-tree retrieval, gestures, automatic permission changes or automatic background activity launch. An ordinary consumer app cannot suspend another package. UsageStats is optional for historical usage display, not polling-based enforcement. DevicePolicyManager suspension requires device/profile owner or delegated management, incompatible with this consumer product. Application overlays require separate special access without improving detection; accessibility overlays fit an already connected service.

Separate pure schedule evaluation, Room session persistence, service detection and window rendering. Persist duration/elapsed anchors plus boot identity and wall fallback; reconstruct from database on connection. A coroutine waits for the next boundary while connected; a non-exact alarm only requests reconciliation and optional notifications. It never starts/recreates sessions. No exact-alarm permission, permanent foreground service or wake lock. Timing may be late when Android suspends execution. Force-stop prevents recovery until the user reopens the app. A saved session and an operational service are separate UI states.

Essential interactions (LIMITR, launcher, settings, permission controllers, dialer/emergency UI, system UI, installer and keyboards) are excluded. Split screen/PiP and manufacturer restrictions remain device-test requirements. Prominent affirmative disclosure is required even on upgrade before using accessibility events; LIMITR is not an accessibility tool for disability support.

Firebase removal is explicitly requested and overrides the template's contradictory Firebase-retention paragraph. Remove SDKs, initialization and network sign-in; retain local schedules, preferences and rewards. Local profile editing replaces cloud profile editing. Do not delete remote accounts, projects or backend data. Existing local data was device-wide, not keyed by account; do not claim a new ownership boundary or import another account's cloud data.

## Official references checked

- [Play target API requirements](https://support.google.com/googleplay/android-developer/answer/11926878): general new/update requirement API 36 from August 31, 2026; extensions can run to November 1, 2026. The reported November 1 notice is owner-supplied app-specific information, not independently verified Console access. Code changes do not clear it; publication and Play confirmation are required.
- [AGP 8.11 compatibility](https://developer.android.com/build/releases/agp-8-11-0-release-notes), [Kotlin compatibility](https://kotlinlang.org/docs/gradle-configure-project.html): pin compatible stable AGP 8.11.1 / Gradle 8.13 / Kotlin 2.2.21; build JVM 17 bytecode, JDK 17+.
- [Android 14](https://developer.android.com/about/versions/14/behavior-changes-14), [Android 15](https://developer.android.com/about/versions/15/behavior-changes-15), [Android 16](https://developer.android.com/about/versions/16/behavior-changes-16): exported receivers, PendingIntent mutability, background launches, edge-to-edge, predictive back and resizing reviewed.
- [Accessibility policy](https://support.google.com/googleplay/android-developer/answer/10964491), [disclosure](https://support.google.com/googleplay/android-developer/answer/11150561), [overlay API](https://developer.android.com/reference/android/view/WindowManager.LayoutParams), [device management](https://developer.android.com/reference/android/app/admin/DevicePolicyManager).
- [Alarm guarantees](https://developer.android.com/develop/background-work/services/alarms), [16 KB support](https://developer.android.com/guide/practices/page-sizes), [Firebase automatic initialization](https://firebase.google.com/docs/reference/android/com/google/firebase/FirebaseApp).

Implementation, executed validation and release gates are recorded in TESTING.md and RELEASE.md as work completes.
