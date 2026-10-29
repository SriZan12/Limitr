# Acme typography, notification controls and All Apps loading

## Changes

- Bundle the original Acme font and its SIL Open Font License. Remove unused downloadable font declarations, preload resources and provider certificates. Font rendering no longer requires Google Play services or a font download.
- Apply Acme in day/night themes and AppCompat's inflater, including dialog/menu text and API 23 text widgets. The inflater also supplies the number picker's input before its wheel paint copies the font, preserving the platform input's commit-on-Done behavior without private-field reflection. Explicitly apply Acme to accessibility-service overlay text because that window does not use AppCompat. Retain existing bold/italic emphasis. The inflater is retained in R8 builds.
- Use the platform time picker's spinner mode: its clock-face renderer hardcodes sans-serif and offers no public font setter. Start/end selection, 12/24-hour mode and saved editor restoration remain. In-app transient feedback uses Acme snackbars, anchored to the current dialog when appropriate.
- Put notification hiding in one rounded card below the timer/interval controls: labelled switch, short scope description, separated status, optional exceptions and an explicit setup/reconnect button. The status itself is no longer an undisclosed tap target. Existing access disclosure and return-from-Settings behavior are retained.
- Remove the enabled start/end-alert status entirely. When posting is disabled, retain a concise **Enable blocking alerts** recovery action. Start/end notification delivery and its contextual permission request remain.
- Preload launchable apps from the launcher activity before Home appears, with a fallback when MainActivity opens directly. A process-local repository retains the last complete snapshot across tabs, details and activity recreation. Concurrent preload/resume reads share one job. PackageManager/icon/usage reads run on IO without retaining an Activity.
- Refresh installed apps and rolling 24-hour usage on resume while the previous rows stay visible. Diff list updates retain the adapter and scroll position. Revoked Usage Access clears cached usage immediately, with another check before publishing an in-flight result. Read failures retain existing rows and expose retry.
- For a genuinely cold load, show a lightweight shimmer matching item icons, titles, durations, usage bars and percentages. It respects RTL and disabled system animations, and stops when hidden, paused or detached. Empty/error results replace the skeleton rather than animating indefinitely. No third-party shimmer dependency or polling service is added.

Room entities/schema, timers, history and Crypto persistence are unchanged. Accessibility and Usage Access onboarding are unchanged. The separate local version/signing, SessionClock, Home XML and IDE edits are outside this commit.

## Scope and limits

- A fresh process must read Android's installed-package and usage services; zero cold-start waiting cannot be guaranteed. Cached returns are immediate and cold loading uses the requested skeleton. The cache is memory-only and refreshes when All Apps resumes, not continuously while it remains open.
- Acme applies to app-owned text. Android owns permission dialogs, Settings, keyboards and notification-shade typography. Glyphs absent from Acme use Android's font fallback so non-Latin installed-app names remain readable.
- Shimmer is decorative and announces a single loading description to accessibility services. No simulated delay is added to production loading.
- Existing notification-listener and inexact-alarm limits still apply; see [notification follow-up](FOLLOWUP_NOTIFICATIONS.md).

Font source and license: [Google Fonts Acme](https://github.com/google/fonts/tree/main/ofl/acme). Android references: [bundled fonts and backward compatibility](https://developer.android.com/develop/ui/views/text-and-emoji/fonts-in-xml), [platform clock renderer](https://github.com/aosp-mirror/platform_frameworks_base/blob/master/core/java/android/widget/RadialTimePickerView.java).

## Manual checklist

- [ ] Fresh/offline install: Home, tabs, app rows, menus, onboarding, rewards, history, blocker, overlay, errors and picker text use Acme. Check night mode and large text.
- [ ] Notification card: off/ready/active/missing-access/disconnected states are clear. Grant/revoke access, return from Settings and turn the saved choice off/on. Start/end alerts still arrive; enabled status text is absent; denial recovery works.
- [ ] Cold Home load shows item-shaped skeletons if needed, then real rows. Warm tab switching, details return and rotation retain the list without a loading flash.
- [ ] Use two apps, return and verify most-used-first ordering and consistent usage indicators. Revoke/regrant Usage Access; install/remove an app and return; check refresh and correct row tap targets.
- [ ] Rotate with an open timer or interval picker; values survive. Check 12/24-hour modes, keyboard input, 200% fonts and landscape.
- [ ] Existing signed upgrade retains Room timers, intervals, history, Crypto and per-app notification choices. Accessibility launch detection/Home/overlay and natural expiry still work.

## Executed validation

```powershell
.\gradlew.bat -g .work/gradle --max-workers=2 `
  '-Dorg.gradle.jvmargs=-Xmx1536m -Dfile.encoding=UTF-8' `
  '-Pkotlin.compiler.execution.strategy=in-process' `
  assembleDebug lintDebug lintRelease testDebugUnitTest assembleRelease bundleRelease assembleDebugAndroidTest --console=plain

# Final picker correction: debug/test/lint gate, with one worker and a 1 GB heap.
.\gradlew.bat -g .work/gradle --max-workers=1 `
  '-Dorg.gradle.jvmargs=-Xmx1024m -Dfile.encoding=UTF-8' `
  '-Pkotlin.compiler.execution.strategy=in-process' `
  assembleDebug assembleDebugAndroidTest lintDebug testDebugUnitTest --console=plain

# Isolated emulator only; API 23 used emulator-5562.
adb -s emulator-5560 shell am instrument -w -r `
  -e accessibilityChecks true -e notificationChecks true -e usageChecks true `
  com.khadka.Limitr.test/androidx.test.runner.AndroidJUnitRunner
```

- Initial full debug/release gate: **PASS, 10m 2s**. Final picker correction's debug/test gate: **PASS, 6m 58s**. **59 unit tests pass**, zero failures/errors/skips. Three new cache regressions cover overlapping reads, retained rows, permission revocation during a read, failures and retry. Debug/release lint report **0 errors, 64 existing warnings**, down from 108; no new warnings.
- Final API 36 run: **18/18 pass, 69.017s**. API 23: **12/12 supported tests pass, 60.082s**; six newer-API tests are excluded, not counted as passes. New device tests inspect actual typefaces on Home/tabs/rows/details/timer/interval pickers and recreated activities, and verify cached rows survive recreation without skeletons reappearing. Existing notification, accessibility, Room migration, Crypto, editor and usage checks pass where supported.
- API 36 manual checks: offline/local Acme rendering in permission setup and Home; most-used-first rows; notification card and retained hiding preference; no enabled-alert status; a real cold-start capture shows item-shaped skeletons, followed by real content. The card and spinner interval dialog remain readable with reachable controls at **200% text size in landscape**. Font/rotation/accessibility settings were restored.
- Both final emulator crash logs are empty. Debug updates were installed in place without clearing data; no Room migration or data change is part of this follow-up. The connected personal device was not targeted.
- The initial cold API 36 run coincided with a System UI ANR and failed launch/usage waits; those cases passed on the stable emulator. The new typography test also caught the platform number-picker input bypassing the normal inflater; that confirmed font exception was fixed before the final passing runs. Early build-only issues (an unsupported optional RecyclerView API and a lint constant) were corrected without changing dependencies.
- Evidence: `.work/ui-polish-full-build.log`, `.work/ui-polish-picker-build.log`, `.work/ui-polish-api36-final-tests.log`, `.work/ui-polish-api23-tests.log`, `.work/ui-polish-cold-7.png`, `.work/ui-polish-notification-card-full.png`, `.work/ui-polish-large-landscape-card.png` and `.work/ui-polish-large-landscape-picker.png`. These local artifacts are untracked.

- Final picker correction's `assembleRelease bundleRelease lintRelease` gate: **PASS, 6m 5s**, using the same cache/in-process compiler settings with one worker and a 1536 MB heap. APK/AAB signatures, bundletool validation, zipalign and native ELF alignment pass. Both artifacts contain the original Acme bytes and OFL license; the font inflater's name is retained in the R8 mapping. No dependencies were changed.
- Release validation is packaging validation, not a new release runtime smoke. The existing private replacement upload key was used; nothing was uploaded and Play key-reset acceptance was not checked. Local builds use the user's separate version 12 / 1.3.0; version/signing changes and their `app/release/app-release.aab` remain untouched. Final release evidence is in `.work/ui-polish-final-release.log` and `.work/ui-polish-*-signature.log` / bundle-validation / alignment logs.
