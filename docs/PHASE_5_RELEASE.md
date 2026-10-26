# Phase 5: signed, optimized release builds

Version **1.2.0 / code 11**, application ID `com.khadka.Limitr`, minSdk 23, compile/target SDK 36. Confirm code 11 exceeds every Play upload before publishing; the repository cannot establish the Console's highest code.

## Changes and risks

- Release signing reads the ignored root `keystore.properties` or the four `LIMITR_*` environment variables documented in [RELEASE.md](RELEASE.md). Nonempty environment values take precedence. Missing fields or a missing key file fail release packaging; debug builds remain independent. The committed example contains placeholders only. No production key was requested or accessed.
- R8 and resource shrinking are enabled with the standard optimized Android rules and existing consumer rules. No blanket keep-all or ignore-warnings rule was needed. Retain the generated mapping with each shipped release.
- Version code increases from 10 to 11 and name from 1.1 to 1.2.0. No product UI, permission flow, Room schema or app ID changes in this phase.
- The principal risks are reflection/resource reachability after shrinking, upload-key versus Play app-signing certificate differences, and real historical upgrade compatibility. Use Play internal testing for the actual signing lineage; a disposable-key APK is only a local validation artifact.

## Executed validation, 26 October 2026

With a newly generated, two-day disposable local key supplied through environment variables:

```powershell
.\gradlew.bat -g D:\StudioProjects\Limitr\.work\gradle --max-workers=2 '-Dorg.gradle.jvmargs=-Xmx1536m -Dfile.encoding=UTF-8' '-Pkotlin.compiler.execution.strategy=in-process' assembleDebug lintDebug testDebugUnitTest lintRelease assembleRelease bundleRelease assembleDebugAndroidTest --console=plain
```

Passed in **9m 7s**. **25 unit tests passed**, zero failures/errors/skips. Debug and release lint each report **0 errors, 112 warnings**, without suppressions or a baseline. Gradle/plugin deprecation and annotation-processor warnings remain; upgrading to Gradle 9 is outside this release.

- Missing signing configuration fails with the expected field-name-only diagnostic. The ignored properties-file configuration also passes `:app:validateReleaseSigning` (22s); that temporary test properties file was then removed. The existing production signing configuration was never touched.
- `apksigner verify --verbose` passes with APK v1/v2 signatures. `jarsigner -verify` passes for the AAB; self-signed/short-expiry/no-timestamp warnings are expected for the disposable certificate. APK verification also reports JAR metadata warnings; v2 protects the APK as a whole.
- Official bundletool **1.18.3** `validate --bundle=...` passes. `aapt dump badging` confirms code 11/name 1.2.0/API 36 and the unchanged app ID.
- `python tools/check_native_alignment.py` passes for both release APK and AAB; all packaged ELF LOAD segments are 16 KB aligned. SDK `zipalign -c -P 16 4` passes for the APK. Execution on a real 16 KB device remains untested.
- R8 produces mapping/seeds/usage/configuration reports. The release APK is 18,806,609 bytes versus debug's 28,681,429; total DEX bytes fall from 25,284,180 to 3,392,020. The AAB is 19,726,406 bytes. These local sizes are not Play download sizes.
- Full API 36 instrumentation, including `-e accessibilityChecks true`, passes **7/7 tests in 17.726s** after installing the final debug build. This includes real external-app detection, a single overlay, countdown, expiry, service disable/reconnect, and exported-schema Room 13→14 migration.

The minified, disposable-key release APK was then freshly installed on the isolated API 36 emulator (the debug installation was snapshotted first). Home opened without auth. Accepted the unchanged disclosure, followed the normal Settings grant, and confirmed the service was bound with window-state events and no window-content capability. Claimed the local reward, selected Chrome, saved a one-minute timer and launched Chrome: one accessibility overlay appeared, its countdown progressed from 54 to 10 seconds, then it disappeared without any new input. History showed the saved Chrome session; back navigation and the bundled help video also worked, including playback after resource shrinking. The crash buffer remained empty. A stale Messages-app ANR dialog appeared when restoring the emulator and was dismissed before the smoke flow; no LIMITR crash/ANR was observed. This fresh-install smoke does not replace the Phase 4 debug upgrade or an actual production-signed upgrade.

## Owner manual checklist

- [ ] Configure the existing upload key locally and run the exact signed AAB/APK commands in RELEASE.md; verify certificate lineage and version code in Play Console.
- [ ] Fresh install reaches Home without auth; disclosure, Settings grant/denial/revocation and reward flow keep their existing behavior and design.
- [ ] Accessibility detects selected launches, shows one overlay, opens Details/Home, and handles timer/interval start, expiry, cancellation and reconnection.
- [ ] Upgrade a real old signed release with existing Room history/sessions and local rewards/preferences. Verify values without clearing data; use Play internal testing when the app-signing key differs from the upload key.
- [ ] Exercise the optimized build on API 23, API 36, a 16 KB device and representative OEM devices, including reboot, Doze, lock/unlock, force-stop, back navigation, large fonts and split screen.
- [ ] Review Play pre-launch reports, accessibility declaration, Data Safety/privacy updates, historic remote-account deletion process and Android vitals before staged rollout.
