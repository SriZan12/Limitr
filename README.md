# LIMITR

Android app blocker built with Kotlin, XML, Room and an opt-in accessibility service. Select a launchable app and save an immediate duration timer or a one-time calendar interval. Local sessions, history and rewards work offline; this version removes Firebase and Google sign-in.

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

Release APK/AAB outputs are unsigned until configured with the existing private upload key. Never replace the signing lineage or clear app data to work around an upgrade failure.

## Design and release notes

- [Prioritized audit and official compatibility/policy references](docs/MODERNIZATION.md)
- [Blocking, timing, migration and offline-data contracts](docs/ARCHITECTURE.md)
- [Build/test results, known gaps and manual test matrix](docs/TESTING.md)
- [Required owner actions before production rollout](docs/RELEASE.md)

Accessibility detects package-window changes and displays a voluntary blocking overlay. It does not suspend other packages. Android can interrupt enforcement; force-stop, multi-window and manufacturer restrictions limit guarantees. The UI distinguishes saved sessions from service availability. System settings, emergency interactions and service-disabling controls remain accessible.

Known historical Room schemas migrate without destructive fallback. Actual signed release upgrades, missing historical schemas, API 36 runtime and OEM/long-duration validation remain release gates. Building this repository does not publish an update or clear a Play Console warning.
