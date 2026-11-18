#!/usr/bin/env bash
set -euo pipefail

REPO_DIR="$(cd "$(dirname "$0")" && pwd)"
CREDENTIALS_FILE="${UNX_MUSIC_CREDENTIALS_FILE:-$HOME/.config/unxmusic/release.env}"
LINEAGE_FILE="$REPO_DIR/signing/unx-music-release.lineage"
VERSION_NAME="$(sed -n 's/^version-name = "\([^"]*\)"$/\1/p' "$REPO_DIR/gradle/libs.versions.toml" | head -n 1)"

if [ ! -f "$CREDENTIALS_FILE" ]; then
  echo "Missing private signing credentials: $CREDENTIALS_FILE" >&2
  exit 1
fi
if [ ! -f "$LINEAGE_FILE" ]; then
  echo "Missing signing lineage: $LINEAGE_FILE" >&2
  exit 1
fi
if ! command -v apksigner >/dev/null 2>&1; then
  echo "Install Android apksigner in Termux before building a release." >&2
  exit 1
fi

set -a
# This file is stored outside the repository with mode 0600.
. "$CREDENTIALS_FILE"
set +a

cd "$REPO_DIR"
adev './gradlew :androidApp:assembleRelease --console=plain'

SOURCE_APK="$REPO_DIR/androidApp/build/outputs/apk/release/androidApp-arm64-v8a-release-unsigned.apk"
OUTPUT_APK="${OUTPUT_APK:-$REPO_DIR/../UnX-Music-App-$VERSION_NAME-arm64.apk}"

apksigner sign \
  --alignment-preserved \
  --lineage "$LINEAGE_FILE" \
  --rotation-min-sdk-version 33 \
  --ks "$OLD_KEYSTORE_PATH" \
  --ks-key-alias "$OLD_KEY_ALIAS" \
  --ks-pass "pass:$OLD_KEYSTORE_PASSWORD" \
  --key-pass "pass:$OLD_KEY_PASSWORD" \
  --next-signer \
  --ks "$KEYSTORE_PATH" \
  --ks-key-alias "$KEY_ALIAS" \
  --ks-pass "pass:$KEYSTORE_PASSWORD" \
  --key-pass "pass:$KEY_PASSWORD" \
  --out "$OUTPUT_APK" \
  "$SOURCE_APK"

apksigner verify --verbose --print-certs "$OUTPUT_APK"
sha256sum "$OUTPUT_APK"
echo "Release APK: $OUTPUT_APK"
