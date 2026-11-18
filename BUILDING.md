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

Signed builds require both the release keystore and the previous Android signing key so upgrades continue to work on older Android versions. On the maintainer's Termux setup, `build_release_termux.sh` loads credentials from `~/.config/unxmusic/release.env` (mode `0600`), builds the arm64 release, signs it with the checked-in public signing lineage, and verifies the result. Keep the keystores and credential file out of version control. Never publish passwords or private keystores. Without these keys, a new APK cannot update existing installations.

## Build and publish policy

APK builds and signing are performed locally by the maintainer. This repository intentionally has no GitHub Actions APK build or release workflow. Publish a release only after a local build succeeds and its package ID, version, signature, and checksum are verified.

## Release source of truth

- App label: `UnX Music`
- Application ID: `com.unx.muice`
- Version name and code: `gradle/libs.versions.toml`
- Public APK/source archives: [GitHub Releases](https://github.com/thalalbinod208-cell/UnxMusic/releases)
- App screenshots and visual references: [asset library](docs/ASSET_LIBRARY.md)
