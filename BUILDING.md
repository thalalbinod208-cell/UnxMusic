# Build UnX Music for Android

UnX Music uses the Android application ID `com.unx.muice`. The release number and version code are maintained in `gradle/libs.versions.toml`; check that file before writing release notes or naming a local APK.

## Requirements

- JDK 21
- Android SDK with the platform and build tools required by `compileSdk` in `androidApp/build.gradle.kts`
- Android SDK available to the build environment
- Internet access for Gradle dependency resolution

## Build a FOSS release APK

The repository's local `gradle.properties` defaults `isFullBuild=true`. For a build that does not require private Sentry/Last.fm build configuration, set it to `isFullBuild=false` in a local, uncommitted change. On a normal Linux development machine, run:

```sh
./gradlew :androidApp:assembleRelease
```

On Android/Termux, run Gradle through the Android development container (`adev './gradlew :androidApp:assembleRelease --console=plain'`). The APKs are written under `androidApp/build/outputs/apk/release/`. Do not upload an unsigned local build as an official signed release.

## Build a signed release

Signed builds require the original release keystore and credentials owned by the release maintainer. Keep the keystore outside version control and provide the required values through protected environment variables as documented by `./build_and_sign_apk.sh --help`. Set `KEYSTORE_PATH` to the keystore path. The helper prefixes signed APKs with `UnX-Music`; verify the APK application ID, version, signature, and checksum before publishing. Never publish credentials or keystore files. Without the original keystore, a new APK cannot update an existing installation signed by that key.

## Build and publish policy

APK builds and signing are performed locally by the maintainer. This repository intentionally has no GitHub Actions APK build or release workflow. Publish a release only after a local build succeeds and its package ID, version, signature, and checksum are verified.

## Release source of truth

- App label: `UnX Music`
- Application ID: `com.unx.muice`
- Version name and code: `gradle/libs.versions.toml`
- Public APK/source archives: [GitHub Releases](https://github.com/thalalbinod208-cell/UnxMusic/releases)
- App screenshots and visual references: [asset library](docs/ASSET_LIBRARY.md)
