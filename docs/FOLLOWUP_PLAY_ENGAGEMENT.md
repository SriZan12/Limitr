# Google Play updates and reviews

Scope: Play-managed updates and contextual reviews on `dev-improvements`. Referrals were explicitly deferred by the owner. No referral/backend/authentication code, Room entity/schema change, reward change or permission request is added.

## Implementation decisions

- Official libraries: `app-update-ktx:2.1.0`, `review-ktx:2.0.2`, and `kotlinx-coroutines-play-services:1.10.2` for cancellable Task awaiting. No monolithic Play Core or Firebase dependency.
- `SelectUpdateFlow` and `ReviewEligibility` are pure policies. `PlayPromptRepository` owns atomic DataStore reservations/history; `RequestReviewIfEligible` and `RecordBlockingSuccess` are application use cases. SDK repositories use application-context managers supplied by Hilt. `PlayEngagementViewModel` exposes StateFlow and owns checks/listeners without retaining an Activity. The Activity-scoped host owns launchers, Play UI and the restart snackbar.
- Priority **4–5** selects immediate when allowed; **0–3** selects flexible. High priority can fall back to flexible; minor releases are not escalated when flexible is disallowed. Fresh prompts wait for unobstructed Home after startup dialogs. The same version/priority is offered at most once per **24 hours**, including failures and cancellations. The app remains usable after rejection.
- Foreground checks run on Home, details (including notification/overlay entry) and History. Details/History only recover an existing update or offer installation of an already-downloaded update. Splash and the legacy blocked-activity trampoline redirect into these hosts. An interrupted immediate update bypasses cooldown; a declined recovery is not relaunched repeatedly in the same Activity instance.
- A new query supplies each single-use `AppUpdateInfo`; consuming the ViewModel command prevents replay on recreation/recollection. Persist the chosen flow with the offer: an in-progress flexible download must not be escalated into an immediate update after returning or recreating the process. Downloaded/installing states take precedence over recovery prompts. The install listener survives rotation and unregisters on ViewModel clear. Foreground checks also restore downloads completed while the listener was absent. Flexible installation calls `completeUpdate()` only after a tap, with retry on failure. Immediate progress/install UI belongs to Play.
- A positive action is successfully saving a timer or interval. A review becomes eligible after **seven days**, saves on **three distinct UTC dates**, and a save within the preceding **ten minutes**. The next unobstructed Home visit may request it. There is no review button, sentiment gate or incentive; claiming Crypto does not trigger it.
- A review attempt is reserved atomically before contacting Play, with a **90-day local cooldown** even if the card never appears. This is LIMITR's conservative policy, not Google's undisclosed quota. Only three dates/timestamps and prompt markers are kept locally. Eligibility begins when this version first runs; historic Room data is not mined for it. Existing Android backup rules preserve these preferences/cooldowns.
- Acquire short-lived `ReviewInfo` only at launch time and recheck visibility before presenting. Drop an unlaunched review when leaving the eligible screen. Completion releases the host without marking the user as reviewed, awarding Crypto or displaying success/error. Offline/unsupported-store failures leave blocking usable.
- Dismiss the restart snackbar before Play UI and leave Play's card and font unchanged. Defer Play UI while Play Store has any timer/interval, including a future interval that could start during the card. This avoids covering Play UI with the accessibility overlay without bypassing the user's block; readiness updates when the schedule clears.

## UI edits

1. Play's standard update consent/progress and review screens at eligible moments.
2. An Acme **Restart to install** snackbar after flexible download, with installation-failure retry. Swiping it away defers it until the next foreground visit.
3. Privacy copy explains Play-managed updates/reviews and local prompt history. Existing onboarding, notification permissions and layouts are retained.

## Play Console prerequisites

Neither feature requires a custom backend. Configure `inAppUpdatePriority` in the Google Play Developer API release entry at rollout time; priority cannot be changed afterward and can reflect intermediate versions across tracks. Internal app sharing does not support this field, so test priority through an internal track or the SDK fake.

Actual updates require an eligible Play-owned installation, matching application ID/signing certificate and a higher available version code. Review display depends on Play eligibility and quota. This change does not publish a release, configure Console, fix an upload-key mismatch or verify the earlier key reset. Keep the signing lineage. SDK fakes do not demonstrate actual Play UI or submission.

## Validation

Final gate on 28 October 2026: **PASS, 9m 6s**. Exact command (JDK 21, the existing workspace SDK/cache):

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-21'
.\gradlew.bat -g .work/gradle --max-workers=2 `
  '-Dorg.gradle.jvmargs=-Xmx1536m -Dfile.encoding=UTF-8' `
  '-Pkotlin.compiler.execution.strategy=in-process' `
  testDebugUnitTest assembleDebug lintDebug lintRelease assembleRelease bundleRelease assembleDebugAndroidTest --console=plain

# Isolated emulators only; repeat with emulator-5562 for API 23.
adb -s emulator-5560 install -r app/build/outputs/apk/debug/app-debug.apk
adb -s emulator-5560 install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb -s emulator-5560 shell am instrument -w -r `
  -e accessibilityChecks true -e notificationChecks true -e usageChecks true `
  com.khadka.Limitr.test/androidx.test.runner.AndroidJUnitRunner
```

- **78 unit tests pass**, zero failures/errors/skips. The 19 new cases cover priority/allowed types, update/review cooldowns and clock rollback, concurrent review reservation, experience/positive-action eligibility, startup gating, single-use consumption, cancellation/process restoration, flexible download recovery/listener cleanup, explicit restart UI, installation retry, review completion/failure and unavailable Play. Tests caught and fixed flexible recovery being incorrectly treated as immediate; cancellation now also works when a restored result arrives before the first query.
- Debug and release lint each report **0 errors, 64 existing warnings**. Initial build-only issues (stale generated injection output during iteration and the test launcher's Kotlin property override) were corrected before the final passing gate.
- **API 36: 18/18 pass, 84.368s. API 23: 12/12 supported tests pass, 43.832s.** Six newer-API tests are excluded on API 23. These are real-device regressions for accessibility/Home/overlay, notifications, Room migration, Crypto/editors, usage and typography; the new SDK flows are tested with SDK fakes on the JVM. Both emulator crash buffers are empty. Devices were updated in place without clearing app data; the connected personal device was not targeted. Isolated snapshots were saved and both emulators stopped.
- Signed R8/resource-shrunk APK and AAB builds, APK/JAR signatures, bundletool validation, 16 KB zip alignment and native ELF alignment pass. This is packaging validation, not a new release runtime smoke or Play upload. The existing private signing configuration was used; upload-key-reset acceptance was not verified. Local builds use the user's version 12 / 1.3.0; their version/signing-file, IDE, Home XML and SessionClock edits remain separate.
- Google's `FakeAppUpdateManager` 2.1.0 omits launch PendingIntents once in progress. Tests verify the immediate recovery request and graceful failed-launch handling; successful real resumption remains a Play test-track check. `FakeReviewManager` provides completion without rendering a card; no test assumes a review was submitted. No production quota or real Play availability is claimed.
- Local evidence: `.work/play-validation-final.log`, `.work/play-api36-tests.log`, `.work/play-api23-tests.log`, `.work/play-*-crashes.log`, `.work/play-apk-signature.log`, `.work/play-aab-signature.log` and `.work/play-bundle-validation.log` (untracked).

## Manual checklist

- [ ] Install a lower-version internal-track release containing this feature; publish a higher priority 0–3 release. Consent is flexible and blocking remains usable during download.
- [ ] Cancel consent; rotate, return from details and relaunch within 24 hours. The same offer should not repeatedly prompt.
- [ ] Finish downloading while backgrounded/terminated. Return through launcher and a blocking notification: restart action appears without duplicate consent. Tap restart; verify retained timers, history, Crypto and no login.
- [ ] Test priority 4–5 through an internal track. Immediate progress resumes after interruption/process recreation. Rejection allows continued use without a loop. Exercise network/storage failure and retry.
- [ ] Onboarding/reward/editor/access dialogs stay unobstructed. Block or schedule Play Store; prompts wait until its block clears.
- [ ] After seven days and saves on three dates, save and return Home within ten minutes. With an eligible Play account, review flow can appear. Dismiss/finish: no success claim, balance change, navigation failure or repeated request. Internal-track quotas differ from production.
- [ ] Without Play Store/network, Home and blocking remain usable. Rotate/background during requests: no stale-window crash or delayed review on another screen.
- [ ] Regression: accessibility launch detection/Home/overlay, start/end alerts, notification hiding, no-auth onboarding, and upgrade from the old production-signed version with existing Room data. Physical OEM and real Play delivery checks remain owner work.

## Official references

- [Android: in-app updates integration](https://developer.android.com/guide/playcore/in-app-updates/kotlin-java)
- [Android: test updates](https://developer.android.com/guide/playcore/in-app-updates/test)
- [Android: review timing, card design and quotas](https://developer.android.com/guide/playcore/in-app-review)
- [Android: review integration/completion semantics](https://developer.android.com/guide/playcore/in-app-review/kotlin-java)
- [Android: test reviews and FakeReviewManager limitations](https://developer.android.com/guide/playcore/in-app-review/test)

Checked against the official documentation on 28 October 2026.
