# Follow-up: Home before blocking and full-display overlay

Requested behavior: opening a selected app during an active session immediately returns to Home, then keeps the blocking notice above the launcher. Exit dismisses the notice; Details opens the existing session screen. Home backgrounds the selected app, rather than terminating its process or deleting its recent task.

## Changes

- `AppLaunchDetector` requests `GLOBAL_ACTION_HOME` when it first detects an active blocked launch. A separate in-memory blocked package retains the notice across the resulting launcher event; otherwise treating Home as an essential app would immediately remove the notice. Countdown ticks and Room publications do not repeatedly send Home. A new app/system screen, Exit, expiry, cancellation, screen-off or service disconnect clears that transient state. A failed system Home request is logged, while the overlay still protects the selected app.
- Duplicate suppression compares package, window ID and class. A Chrome first-run check reproduced a second Activity starting just after Home; package-only suppression ignored its event and left it foreground. A new window from the same selected package now triggers Home again. Device coverage launches two different Activities from the same test package; Chrome's asynchronous first-run path is also checked manually.
- Home retention uses the resolved default launcher, refreshed on each blocked launch. Enumerating every Home handler also includes Android Settings' boot fallback; retaining over that package would incorrectly cover Settings. A device regression verifies that opening Settings dismisses the notice.
- `OverlayScreen` lays out its window across the full display. API 30+ explicitly disables automatic window inset fitting and allows layout into cutout edges; API 28–29 uses short-edge cutout support. System-bar/cutout insets remain applied to content, preserving reachable controls and the existing scroll layout.
- The opt-in accessibility device regression checks the real resumed launcher (not just overlay presence), full-display window bounds, an opaque background at the top of the screen, countdown, Exit, reopening, service reconnection and expiry.

UI edits are limited to the requested overlay window bounds: the background covers the status-bar area while existing controls retain safe insets. No layout asset, typography, permission flow, Room schema, dependency or version change.

References: Android's [Home/global action API](https://developer.android.com/reference/android/accessibilityservice/AccessibilityService#performGlobalAction(int)) and [window inset fitting](https://developer.android.com/reference/android/view/WindowManager.LayoutParams#setFitInsetsTypes(int)).

## Executed validation, 26 October 2026

`gradlew.bat -g D:\StudioProjects\Limitr\.work\gradle --max-workers=2 "-Dorg.gradle.jvmargs=-Xmx1536m -Dfile.encoding=UTF-8" "-Pkotlin.compiler.execution.strategy=in-process" assembleDebug lintDebug testDebugUnitTest assembleDebugAndroidTest --console=plain` passed on the final sources in **2m 40s**. All **25 unit tests pass**; lint reports **0 errors, 112 existing warnings**, without a baseline or suppressed checks.

`adb -s emulator-5560 shell am instrument -w -e accessibilityChecks true com.khadka.Limitr.test/androidx.test.runner.AndroidJUnitRunner` passes **7/7 tests in 21.484s** on API 36. The accessibility regression verifies the actual resumed launcher, a single full-display overlay, its top-edge background, countdown, two Activities from the same blocked package, Settings dismissal, Exit, reopening, service disconnect/reconnect and expiry leaving Home visible. The full run also includes Room migration and existing session regressions. Crash-buffer Logcat is empty.

Manual checks on the isolated emulator verified repeated Chrome launches returning Home, Details opening its existing screen, full overlay background, landscape scrolling to both buttons, and 200% text in portrait with reachable controls. Android's letterbox education window appeared during forced rotation; essential system windows intentionally dismiss the notice. The original package-only event filter reproduced Chrome's second-Activity race before the fix. An intermediate onPause-triggered test fixture failed to reliably launch its second Activity; it was replaced with an explicit second-Activity launch before the final passing run. The emulator confirmed that Home candidates include `com.android.settings/.FallbackHome`, while the resolved default is NexusLauncher; the final Settings regression passes with that distinction.

No physical OEM/API 23/16 KB runtime or production-signed upgrade was repeated for this follow-up. Signed R8 packaging was validated in Phase 5; this follow-up's executed build gate is the debug/test configuration above. No production signing material was used.

## Manual checklist

- [ ] With a timer active, open the selected app: launcher becomes foreground and one notice remains. Check that video/game foreground activity stops; background services/audio may continue according to that app's behavior.
- [ ] Exit leaves Home visible. Reopening the selected app returns Home again. Details opens the existing session screen.
- [ ] Expiry/cancellation removes the notice without reopening the selected app. Disabling accessibility, screen-off and opening Settings clear the notice.
- [ ] Portrait/landscape, large text, display cutouts and gesture/three-button navigation: no status-bar strip shows through, and both buttons remain reachable by scrolling when needed.
- [ ] Fresh Home/onboarding without auth and existing Room data/rewards retain their behavior. No migration is introduced by this follow-up.
- [ ] Repeat on representative OEM devices and the minimum supported API; the system may reject/delay Home, and force-stop/multi-window/background services remain platform limitations.
