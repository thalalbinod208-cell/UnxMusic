# Build UnX Music for Android

UnX Music uses the Android application ID `com.unx.muice`. The release number and version code are maintained in `gradle/libs.versions.toml`; check that file before writing release notes or naming a local APK.

## Requirements

- JDK 21
- Android SDK with the platform and build tools required by `compileSdk` in `androidApp/build.gradle.kts`
- `ANDROID_HOME` pointing to the Android SDK for the signing helper
- Internet access for Gradle dependency resolution

## Build a FOSS release APK

The repository's local `gradle.properties` defaults `isFullBuild=true`. For a build that does not require private Sentry/Last.fm build configuration, temporarily set it to `isFullBuild=false`, then run:

```sh
./gradlew :androidApp:assembleRelease
```

The APKs are written under `androidApp/build/outputs/apk/release/`. The release build creates ABI-specific APKs and a universal APK. Do not upload an unsigned local build as an official signed release.

## Build a signed release

Signed builds require a release keystore and credentials owned by the release maintainer. Keep the keystore outside version control and provide the required values through protected environment variables as documented by `./build_and_sign_apk.sh --help`. The release workflow writes its protected keystore to `unx-music-release.jks`; set `KEYSTORE_PATH` for a different local path. The helper prefixes signed APKs with `UnX-Music`; verify the APK application ID, version, signature, and checksum before publishing. Never publish credentials or keystore files.

## Build validation

The `Android build validation` workflow checks out the source, selects JDK 21, builds the FOSS release without signing secrets, and uploads the generated APKs as a short-lived workflow artifact. This validates compilation; it does not sign or publish a GitHub release.

## Release source of truth

- App label: `UnX Music`
- Application ID: `com.unx.muice`
- Version name and code: `gradle/libs.versions.toml`
- Public APK/source archives: [GitHub Releases](https://github.com/thalalbinod208-cell/UnxMusic/releases)
- App screenshots and visual references: [asset library](docs/ASSET_LIBRARY.md)
