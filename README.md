# LIMITR

Android app blocker built with Kotlin, XML, Room and an opt-in accessibility service. Select a launchable app and save an immediate duration timer or a one-time calendar interval. Timers, intervals, history and rewards work offline; this version removes Firebase and Google sign-in.

- Published application ID: `com.khadka.Limitr`
- Minimum Android: API 23; compile/target SDK: API 36
- Build toolchain: AGP 8.11.1, Gradle 8.13, Kotlin 2.2.21, JDK 17+ (validated with JDK 21)

## Build and check

Install Android SDK 36 and set its path in your untracked `local.properties` or SDK environment configuration. On Windows:

```powershell
.\gradlew.bat assembleDebug testDebugUnitTest lintDebug
.\gradlew.bat connectedDebugAndroidTest # boot an isolated test device first
.\gradlew.bat assembleRelease bundleRelease
```

On macOS/Linux use `./gradlew`. See [executed validation and the device matrix](docs/TESTING.md) for this workspace's Gradle-cache override, constrained-memory options and observed results.

Release APK/AAB builds require the existing private upload key through gitignored `keystore.properties` or environment variables; missing credentials fail the release task. R8 and resource shrinking are enabled. Version 1.2.0 uses versionCode 11. See [exact signing/build commands](docs/RELEASE.md). Never replace the signing lineage or clear app data to work around an upgrade failure.

## Design and release notes

- [Prioritized audit and official compatibility/policy references](docs/MODERNIZATION.md)
- [Blocking, timing, migration and offline-data contracts](docs/ARCHITECTURE.md)
- [Build/test results, known gaps and manual test matrix](docs/TESTING.md)
- [Latest app QA and Crypto/editor fixes](docs/QA_2026_10_27.md)
- [Required owner actions before production rollout](docs/RELEASE.md)
- [Removal of session controls/alerts, with timers and intervals retained](docs/FOLLOWUP_REMOVE_SESSIONS.md)
- [Help removal and closing details after unblock](docs/FOLLOWUP_HELP_UNBLOCK.md)
- Phase records: [API 36](docs/PHASE_1_API_36.md), [offline cleanup](docs/PHASE_2_OFFLINE.md), [accessibility evaluation](docs/PHASE_3_ACCESSIBILITY.md), [audit and migrations](docs/PHASE_4_AUDIT.md), [signed R8 validation](docs/PHASE_5_RELEASE.md)

Accessibility detects package-window changes and displays a voluntary blocking overlay. It does not suspend other packages. Android can interrupt enforcement; force-stop, multi-window and manufacturer restrictions limit guarantees. The UI distinguishes saved timers and intervals from service availability. System settings, emergency interactions and service-disabling controls remain accessible.

Known historical Room schemas migrate without destructive fallback. API 36 emulator accessibility and Room migration tests pass; actual production-signed upgrades, missing historical schemas and physical OEM/long-duration validation remain release gates. Building this repository does not publish an update or clear a Play Console warning.
