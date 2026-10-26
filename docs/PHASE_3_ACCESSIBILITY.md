# Phase 3: launch detection decision and hardening

Recommendation given before implementation on 26 October 2026: **retain AccessibilityService**. No detector switch, new permission or permission-flow change is justified by this review.

| Consideration | Current accessibility events | UsageStatsManager polling |
|---|---|---|
| Reliability | System sends package-window events while the enabled service is connected. API 36 launch/overlay/expiry was exercised in Phase 1. Events are not a guarantee of exclusive foreground ownership in split screen/PiP; OEM disconnection still matters. | Queries historical usage events. An ordinary app has no public continuous launch callback here; it would poll and reconcile event history. Events are unavailable before user unlock, and polling stops when execution is suspended. |
| Battery | Event driven; no launch polling/wake lock/FGS. Only an active overlay ticks once per second. Duplicate-event suppression avoids redundant power/keyguard queries. | Faster detection requires more frequent queries/wakeups. A long-running polling design would also need a supported background-execution mechanism. Relative battery cost is an engineering assessment, not a measured benchmark. |
| Play review | Higher policy burden: declaration, clear core-purpose explanation, separate prominent disclosure and affirmative consent. `isAccessibilityTool=false` is appropriate. Package-only events, no screen-content retrieval, and user-controlled blocking remain important. Approval cannot be inferred from a successful build. | Avoids AccessibilityService-specific declaration if accessibility is removed, but still accesses sensitive app-use data. Policy approval is not automatic. |
| UX / blocking | Existing user-granted service supplies both detection and the accessibility overlay. Current disclosure/settings flow is retained. | Usage access becomes required instead of optional; an alternative blocker window/background execution may need additional access and ongoing notification UX. This conflicts with preserving the current permission flow. |

Sources: [UsageStatsManager](https://developer.android.com/reference/android/app/usage/UsageStatsManager), [AccessibilityService lifecycle](https://developer.android.com/reference/android/accessibilityservice/AccessibilityService), [Play accessibility policy](https://support.google.com/googleplay/android-developer/answer/10964491). The battery/latency/UX comparison is an application-specific engineering inference from these APIs, not a comparative device measurement.

Device-owner package suspension is an enterprise-management alternative, not a replacement for a normally installed consumer app. Usage statistics remain useful for the existing optional historical usage display. A switch would require a separately approved prototype and measured OEM/latency/power evidence.

## Implemented

- Refresh persisted sessions on system reconnection before collecting state, so delayed alarms/process suspension do not leave the service using a stale snapshot.
- Ignore duplicate package-window events even when no overlay is active. Session changes still arrive through the state collector, and failed window attachments remain retryable. This avoids redundant binder work without delaying genuine package changes.
- Propagate coroutine cancellation before the broad `IllegalStateException` handler. `CancellationException` inherits from it; disconnecting a collector while Room is pending previously could incorrectly publish a storage failure and cancel alarms. A regression uses a queued Room executor to exercise cancellation while the transaction is pending.
- Extend the real-service instrumentation test through disable/reconnect, window cleanup and enforcement of the still-persisted session, followed by expiry without another window event.

The service already owns/cancels its scope and window, unregisters the receiver, excludes essential apps, debounces platform events by 100 ms, and does not retain event objects or Activity contexts. No perpetual polling, self-enable, forced foreground restart, battery-exemption prompt or OEM settings screen is added. Android controls binding; after force-stop the user must reopen the app. OEM battery behavior still needs physical-device tests.

## Manual checks and risks

- [ ] Accessibility disable/re-enable, interrupt, process death, reboot/update and OEM battery restriction.
- [ ] Rapid repeated app launches; a single overlay; current-app timer/interval activation and expiry.
- [ ] Essential system apps, Home and Details remain available; test lock/unlock and split screen/PiP.
- [ ] Home opens without auth and the existing disclosure/permission/reward flow is unchanged.
- [ ] Upgrade an old signed version with existing Room sessions/history/coins; verify preservation.
- [ ] Measure latency, CPU/wakeups and memory during a multi-hour OEM soak. No measured battery or leak-free claim is made here.

Room schema/entities and product UI are unchanged. Cancellation and event filtering are the primary regression risks; automated results follow below.

Executed 26 October 2026: `gradlew.bat assembleDebug lintDebug testDebugUnitTest assembleDebugAndroidTest` (same local cache; two workers, 1536 MB heap, in-process Kotlin) passed in **2m 14s**. **24 unit tests passed; lint: 0 errors, 176 warnings**. On the isolated API 36 emulator, `adb -s emulator-5560 shell am instrument -w -e accessibilityChecks true com.khadka.Limitr.test/androidx.test.runner.AndroidJUnitRunner` passed **all 6 tests in 26.294s**, including disable/reconnect/window cleanup, persisted enforcement and expiry. This is short functional coverage, not an OEM battery/soak result.
