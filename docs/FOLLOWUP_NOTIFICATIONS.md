# Blocking alerts and per-app notification hiding

## Diagnosis

Start/end alerts were deliberately removed with the earlier session-feature cleanup: there was no alert producer or POST_NOTIFICATIONS declaration/request, and startup removed the old `sessions` channel. This follow-up restores the two requested blocking alerts without restoring session controls or the bell.

The existing `NotificationListener` could already cancel eligible notifications. Its switch was disabled without a current blocking row, saved only to that row (lost at expiry), reset itself when access was missing, and did not apply the intended selection after returning from Settings. The click handler also read `isChecked` inside an asynchronous operation, allowing intervening renders to replace the user's choice. The corrected handler captures the Boolean immediately.

## Fixed / implemented

- `BlockingAlerts` creates a new **Blocking alerts** channel and posts one current alert per package. An active timer/interval posts a start alert; natural expiry replaces it with an ended alert. Future intervals wait for their start. Early Crypto unblock/replacement does not generate a false expiry alert. Saved start-revision markers prevent repeated starts on refresh/relaunch, including after dismissing an alert. The old-channel cleanup leaves the new channel alone. Start alerts explain when accessibility enforcement is unavailable.
- Room reconciliation returns the rows it actually expired in the same transaction that removes them. The coordinator publishes state, schedules the next existing inexact alarm and delivers those alerts. Publication/delivery completes even when the coordinator's `collectLatest` boundary job cancels itself after publishing new state. The notification work runs on IO and posting failures do not mark the blocking database as corrupt.
- Restored Android 13+ POST_NOTIFICATIONS. The first successful timer/interval save requests it if needed; denial leaves blocking functional. A visible start/end-alert status opens a permission request or system channel/app settings for later enablement. This permission is independent of access to other apps' notifications.
- `NotificationPreferences` stores explicit package-keyed choices in the existing DataStore. They work before blocking and survive expiry, reopening and app restarts. Opening an existing block imports its legacy option only when there is no explicit per-app choice. The existing Room `blockNotifications` column remains as a compatibility fallback; no entity/schema migration is needed.
- The listener combines current blocking state with preference updates, checks the current deadline for each notification, and rescans already-posted notifications on connection/state/option changes. Only selected, currently blocked packages qualify. LIMITR's own alerts, essential apps and ongoing notifications remain available. It cancels individual keys and never reads/stores/transmits message contents.
- Connection state is distinct from access being enabled. Disconnect/revoke clears service state and cancels collection; destruction cancels the service scope. Listener callbacks are confined to Main because API 23 may deliver them on binder threads. The details screen requests a rebind when appropriate on API 24+, and explains the settings recovery path. No manually started background service, polling loop or foreground service is introduced.

## UI / permission changes

1. Move the existing switch beside a visible **Hide this app's notifications** label below the app name.
2. Add status text for off, ready for a future block, currently hiding, missing access and disconnected listener; show the ongoing/essential notification exceptions. The switch is available before a block exists and can always be turned off when access is revoked.
3. Explain notification access before opening Android Settings. Returning after a grant completes the pending enable action; declining leaves the option off. The pending intent to enable survives activity recreation while Settings is open.
4. Add a separate start/end-alert status/settings action and the contextual POST_NOTIFICATIONS prompt. After permanent denial, the action opens app-level notification settings; when global permission is available it opens the Blocking alerts channel. Timer/interval cards, history, Crypto flow, accessibility and usage-access onboarding remain unchanged.

## Limits / suggested but not done

- A notification listener dismisses notifications after Android posts them; it cannot guarantee preventing the initial sound/banner. Android/OEM restrictions can also limit cancellation. No blanket cancellation, DND control or sensitive-message access is added.
- Existing inexact alarms and coordinator deadlines deliver expiry while the process runs and recover after background reclamation. Doze/OEM restrictions can delay the end alert. Force-stop prevents recovery until Android allows the app to run again. Exact-alarm access and a permanent foreground service were not added.
- Permission/channel denial suppresses alerts. Granting permission during an active block can publish its start; expiries while alerts are disabled are not replayed later.
- Room changes and NotificationManager delivery are not one atomic transaction. Abrupt process death between expiry commit and posting can lose an alert; death between posting a start and saving its revision marker can repeat it. A durable Room outbox would require a separately tested migration. Blocking correctness does not depend on notification delivery.
- Physical OEM, long-duration Doze and actual production-signing-lineage upgrade checks remain owner tests.

## Manual checklist

- [ ] Fresh install opens Home without auth; accessibility and usage onboarding still work.
- [ ] Before creating a timer, turn hiding on. Decline setup, then retry and grant notification access in Settings; returning completes the toggle. Rotate while Settings is open.
- [ ] Select hiding for app A only. While A is blocked, its normal notifications disappear; app B, ongoing notifications and LIMITR start/end alerts remain available. Turning the switch off allows new notifications immediately.
- [ ] Revoke listener access while hiding is selected: show missing-access feedback, permit turning off, and recover after granting/reconnecting.
- [ ] Allow LIMITR posting permission; start a timer and receive a start alert. Leave details/the app; at natural expiry receive an end alert. Tap both alerts and verify the correct app details.
- [ ] Deny posting permission or mute the Blocking alerts channel: timers and interception still work; status offers a path to enable alerts.
- [ ] Future intervals notify at their start/end. Replacement, early Crypto unblock, repeated refresh, dismissed alerts and process recreation do not generate misleading duplicate/expiry alerts.
- [ ] Reopen/restart after expiry: the per-app switch choice remains. Upgrade with existing Room data and check timers, intervals, history and Crypto without uninstalling.
- [ ] Large fonts, portrait/landscape and keyboard: all controls remain reachable in the existing scroll container.

Official references: [notification permission](https://developer.android.com/develop/ui/views/notifications/notification-permission), [notification listener lifecycle/cancellation](https://developer.android.com/reference/android/service/notification/NotificationListenerService), [inexact alarms and background limits](https://developer.android.com/develop/background-work/services/alarms).

## Executed validation, 28 October 2026

```powershell
.\gradlew.bat -g .work/gradle --max-workers=2 `
  '-Dorg.gradle.jvmargs=-Xmx1536m -Dfile.encoding=UTF-8' `
  '-Pkotlin.compiler.execution.strategy=in-process' `
  assembleDebug lintDebug lintRelease testDebugUnitTest assembleRelease bundleRelease assembleDebugAndroidTest --console=plain

# Isolated emulator only; API 23 used emulator-5562.
adb -s emulator-5560 shell am instrument -w -r `
  -e accessibilityChecks true -e notificationChecks true -e usageChecks true `
  com.khadka.Limitr.test/androidx.test.runner.AndroidJUnitRunner
```

- Full application/build gate: **PASS, 7m 52s**. **56 unit tests pass**, zero failures/errors/skips. Debug and release lint: **0 errors, 108 existing warnings**. Seven new unit cases cover alert lifecycle/deduplication/cancellation, scheduled intervals/replacement, late recovery, disabled alerts, legacy channel cleanup and independent per-package choices.
- A later test-only synchronization correction passed `assembleDebugAndroidTest lintDebug` with one worker and a 1 GB heap in **1m 15s**. Application/release sources did not change after the full gate.
- **API 36: 16/16 tests pass, 46.949s. API 23: 10/10 supported tests pass, 34.810s.** Six newer-API tests are excluded on API 23, not counted as passes. New device tests exercise real automatic start/end alerts without keeping details open, actual DataStore close/reopen, and switch-driven interception before/after blocking, switching off/on, expiry and access revocation. Existing accessibility/Home/overlay, notification filtering, Room migration, Crypto, editor and usage regressions pass where supported.
- API 36 manual checks: decline notification-access disclosure, retry/grant via Android Settings, rotate while Settings is open, return with the pending switch enabled; deny posting permission, recover through the corrected app-level settings page and enable alerts; visually confirm Chrome start and end notifications, including the accessibility-unavailable explanation. After force-stop/relaunch the saved switch is still on and the listener reconnects. New switch/status and alert controls remain reachable at 200% font size in landscape; font/rotation settings were restored.
- Both device crash buffers were empty after these checks. Debug updates were installed in place without clearing data. No Room entity/schema change is included; production-signed upgrades remain a separate owner check.
- R8/resource-shrunk APK and AAB packaging, signatures, bundletool validation, APK zipalign and APK/AAB native ELF alignment pass. Release packaging uses the existing private local replacement upload key; nothing was uploaded and Play key-reset activation was not checked. This follow-up validates release packaging, not a new release runtime smoke. The user's separate `app/release/app-release.aab` was not replaced.
- Initial harness issues were corrected before the passing runs: a short wall-clock interval test raced initialization; host Windows DataStore file replacement was unsuitable for the persistence test, which now runs on Android; a Kotlin test inferred a non-void return; the switch test needed to wait for saved state and rendered/enabled controls. The saved choice/UI were correct when the earlier wait timed out. An API 36 snapshot came up with ADB offline; cold boot recovered it.
- Local evidence is in `.work/notification-fix-release-build.log`, `.work/notification-fix-test-final-build.log`, `.work/notifications-api36-verified-tests.log`, `.work/notifications-api23-verified-tests.log`, the signature/alignment logs and `notifications-api36-*.png`. These artifacts are untracked.

The user's existing IDE, version/signing-file and SessionClock edits remain outside this commit. Local builds used their version 12 / 1.3.0; no additional version bump or signing configuration change is included here.
