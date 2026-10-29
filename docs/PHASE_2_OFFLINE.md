# Phase 2: finish Firebase and profile cleanup

Base: Phase 1 (`dev-improvements`). Firebase Auth, Analytics, Crashlytics, Google sign-in, Google Services plugins/configuration and network permissions were already absent from `dev`. Splash already routes directly to Home; no authentication gate is reintroduced.

Removed the unreachable profile destination, profile fragment/helper, its sole Glide dependency, abandoned signup/profile/network-error layouts, associated images/drawables, twelve unused account/network strings and the unused remote-result wrapper. The old `ActivityBlocked` compatibility class stays for existing intents; its unused profile-bearing layout is removed. Active screens are not redesigned. Existing first/daily reward wording remains because it is part of the preserved UI and does not perform authentication.

No Room entity/schema, DataStore balance, preference key, permission flow, or active reward behavior changed. Old local photo/name preferences and remote Firebase accounts are not deleted. The privacy explanation about historical remote accounts remains useful even though no Firebase SDK is present.

## Validation and risks

The new debug APK's DEX was scanned: Firebase, Google Auth and Glide class prefixes are absent. The merged debug manifest has no Firebase components or Internet permission. Source/config/ProGuard scans found only the intentional historical-account privacy notice. The primary risk is an overlooked resource reference; compilation/lint validate resource references. Production-signed upgrades still need real historical data.

- [ ] Fresh launch and upgrade go straight to Home without login or an Internet prompt.
- [ ] Existing permission/disclosure and reward popup sequence works; balance is retained and a claim credits once.
- [ ] Accessibility still detects selected app launches and removes overlays at expiry.
- [ ] Upgrade the old signed app with Room schedules/history; verify no data deletion.
- [ ] History/help/video, timer/interval dialogs and notification controls keep their existing appearance.

Executed 26 October 2026: `gradlew.bat assembleDebug lintDebug testDebugUnitTest` with the Phase 1 cache/one-worker/JVM options passed in **7m 49s**. **23 unit tests passed; lint: 0 errors, 176 warnings** (down from 202). No lint checks were disabled. Room migration fixtures and reward idempotence/balance preservation tests remain green. Device UI validation for these deletions is deferred to the final assembled release; Phase 1's API 36 results are not reported as a run of Phase 2.
