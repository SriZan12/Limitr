# Phase 4: confirmed fixes and remaining suggestions

## Fixed

- **History collision:** the old name-only primary key replaced another package's history when display names matched. Room 14 uses `(appName, appPackage)`; the counter query uses both fields in the existing save transaction. The 13→14 migration copies all six historical values, including blank package identities and null timestamps/counters, before swapping tables. Existing rows cannot collide under the expanded key. It cannot recover history overwritten by an older release.
- **History sort work:** the history query used a temporary sort tree. An index on `blockedDate` lets SQLite scan the index for the existing descending query. Query-plan checks verify the index; no screen ordering or design changes.
- **Lifecycle cancellation shown as storage failure:** history collection and SessionViewModel now propagate cancellation before handling genuine storage exceptions, following Phase 3's coordinator fix. A ViewModel regression verifies cancellation does not publish an error or leave the busy flag set.
- **Unbounded broadcast work:** boot/time/update reconciliation uses a worker dispatcher and an eight-second cancellable deadline, with `PendingResult.finish()` in `finally`. It is a recovery hint; stored timestamps and the next app/service refresh remain authoritative if the deadline is reached.
- **Nullable usage statistics:** a null platform result now uses the existing unavailable-usage path instead of dereferencing it.
- **Lint cleanup:** removed 39 strings that lint marked unused and that had no remaining Kotlin/XML references. No active text, layout, permission flow or visual design changed; checks are not suppressed.

## Reviewed and retained

Room writes/migrations are transactional, suspending DAO calls run off the main thread, and destructive fallback/main-thread queries are absent. The coordinator intentionally lives for the Application lifetime and holds an Application context. Accessibility/notification service scopes are cancelled on destruction; windows/receivers are released. Fragment collectors use view lifecycle owners, repeating UI work stops with STARTED state, RecyclerView references are cleared on view destruction, and the video player releases in onStop. This is a source/lifecycle audit, not proof of leak-free multi-hour operation.

Legacy Room versions 3/4/6/7/8/9/10/12 still migrate through 13 to 14. Source-derived fixtures preserve their archived rows; unsupported versions fail without deletion. The exported 13 schema remains unchanged, and the generated 14 schema accompanies this change. A device migration test uses the real exported 13 schema, validates Room 14, checks history/session/legacy values and blank/null fields, inserts a second same-name package, verifies index use, and reopens with Room.

## Suggested but not done

- Physical OEM/multi-hour leak, power and launch-latency profiling; no supplied production crash traces to reproduce beyond code-confirmed defects.
- Broad dependency, kapt→KSP, Gradle DSL and architecture upgrades. Current pins support API 36; avoid migration churn in the deadline release.
- Visual/content-description/typography/launcher-icon lint changes and active copy edits. These require a separate UI scope under the current constraint. Unused drawable/font/layout cleanup and adapter diffing can be reviewed separately.
- A single storage transaction spanning Crypto spending and Room cancellation. The current optional paid-unblock path spans DataStore and Room; cancellation/write failure or concurrent activities can leave a cancelled session without a coin charge. Free Stop session already exists. A unified transactional store or explicit compensation design needs a separate data-contract review; this phase does not change the economy or permission/UI flow.
- Actual published signing-lineage upgrades and unknown historical database shapes. Reconstructed fixtures and a debug 13→14 upgrade cannot establish all production upgrade paths.

## Manual checklist

- [ ] Upgrade an old signed release with schedules/history/coins, including duplicate labels and missing packages; verify data, counts and ordering.
- [ ] Two different packages with the same display name keep independent history counts.
- [ ] Rotate/background history and session screens during disk work; no false storage error.
- [ ] Accessibility enable/revoke/reconnect, repeated launches and timer/interval expiry.
- [ ] Home opens without auth; permission/onboarding/reward flow and screen designs unchanged.
- [ ] Boot/time-zone/update reconciliation under Doze and OEM restrictions; expired sessions stay expired.

Primary risks are the history migration and cancellation paths. Execution results are recorded below after checks.

Executed 26 October 2026: `gradlew.bat assembleDebug lintDebug testDebugUnitTest assembleDebugAndroidTest` with the Phase 3 cache/two-worker/JVM options passed (final run **47s**). **25 unit tests passed**, including all established legacy fixtures through Room 14, independent same-name history and both cancellation regressions. **Lint: 0 errors, 137 warnings**; no baseline/disabled checks. An initial test-only compilation error using `use` on RoomDatabase was corrected with explicit close before this passing build.

API 36 full instrumentation with `-e accessibilityChecks true`: **7/7 passed in 14.138s**, including the exported-schema migration and actual accessibility reconnect/expiry. Separately, installed the new debug APK with `adb install -r` over Phase 3's Room 13 installation, launched Home, and compared copied SQLite/WAL snapshots: schema became 14 and all three existing history rows/values were identical (session and legacy tables were empty in that installation; the migration test covers nonempty rows there). No uninstall/clear-data step was used for this upgrade. This debug-key upgrade is not a production signing-lineage test.
