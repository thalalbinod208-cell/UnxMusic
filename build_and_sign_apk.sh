#!/bin/bash

# Exit on error
set -e

# Default variables
BUILD_TYPE="release"
BUILD_VARIANT="full"
MODULE="androidApp"
NAME_SUFFIX=""
KEYSTORE_PATH="${KEYSTORE_PATH:-./unx-music-release.jks}"
SIGNING_LINEAGE_PATH="${SIGNING_LINEAGE_PATH:-./signing/unx-music-release.lineage}"
OLD_KEYSTORE_PATH="${OLD_KEYSTORE_PATH:-$HOME/.android/debug.keystore}"
OLD_KEYSTORE_PASSWORD="${OLD_KEYSTORE_PASSWORD}"
OLD_KEY_ALIAS="${OLD_KEY_ALIAS:-androiddebugkey}"
OLD_KEY_PASSWORD="${OLD_KEY_PASSWORD}"
# Read passwords from environment variables or use default (for backward compatibility)
KEYSTORE_PASSWORD="${KEYSTORE_PASSWORD}"
KEY_ALIAS="${KEY_ALIAS}"
KEY_PASSWORD="${KEY_PASSWORD}"

# Parse command line arguments
print_usage() {
  echo "Usage: $0 [options]"
  echo "Options:"
  echo "  --release          Build in release mode (default)"
  echo "  --debug            Build in debug mode"
  echo "  --full             Build full with Sentry"
  echo "  --foss             Build foss, compatibility with F-Droid, no Sentry"
  echo "  -h, --help         Show this help message"
  echo ""
  echo "Environment variables:"
  echo "  ANDROID_HOME        Android SDK directory"
  echo "  KEYSTORE_PATH       Release keystore path (default: ./unx-music-release.jks)"
  echo "  KEYSTORE_PASSWORD  Password for the keystore"
  echo "  KEY_PASSWORD       Password for the key (required, no default)"
  echo "  KEY_ALIAS          Alias for the key (required, no default)"
  echo "  OLD_KEYSTORE_PATH   Previous Android signing key used for Android 12 and earlier"
  echo "  OLD_KEYSTORE_PASSWORD / OLD_KEY_PASSWORD / OLD_KEY_ALIAS"
  exit 0
}

# Process command line arguments
while [[ "$#" -gt 0 ]]; do
  case $1 in
    --full) BUILD_VARIANT="full" ;;
    --foss) BUILD_VARIANT="foss" ;;
    --release) BUILD_TYPE="release" ;;
    --debug) BUILD_TYPE="debug" ;;
    -h|--help) print_usage ;;
    *) echo "Unknown parameter: $1"; print_usage ;;
  esac
  shift
done

# Check signing credentials after parsing so --help works without secrets.
if [ -z "$KEY_PASSWORD" ]; then
  echo "Error: KEY_PASSWORD environment variable must be set" >&2
  exit 1
fi

if [ -z "$KEYSTORE_PASSWORD" ]; then
  echo "Error: KEYSTORE_PASSWORD environment variable must be set" >&2
  exit 1
fi

if [ -z "$KEY_ALIAS" ]; then
  echo "Error: KEY_ALIAS environment variable must be set" >&2
  exit 1
fi

# The lineage preserves upgrade compatibility with releases signed by the prior
# Android Debug certificate on Android 12 and earlier.
if [ "$BUILD_TYPE" = "release" ] && [ ! -f "$SIGNING_LINEAGE_PATH" ]; then
  echo "Error: release signing lineage not found at $SIGNING_LINEAGE_PATH" >&2
  exit 1
fi
if [ "$BUILD_TYPE" = "release" ]; then
  if [ ! -f "$OLD_KEYSTORE_PATH" ]; then
    echo "Error: previous signing keystore not found at $OLD_KEYSTORE_PATH" >&2
    exit 1
  fi
  if [ -z "$OLD_KEYSTORE_PASSWORD" ] || [ -z "$OLD_KEY_PASSWORD" ]; then
    echo "Error: OLD_KEYSTORE_PASSWORD and OLD_KEY_PASSWORD must be set for release signing" >&2
    exit 1
  fi
fi

# Set derived variables based on selected options
APK_OUTPUT_DIR="./$MODULE/build/outputs/apk/$BUILD_TYPE"
SIGNED_APK_OUTPUT_DIR="./$MODULE/build/outputs/apk/$BUILD_TYPE"

# Use an apksigner executable supported by the current host. Android Gradle's
# packaged release APKs are already aligned; apksigner will preserve alignment.
if [ -z "${APKSIGNER:-}" ]; then
  APKSIGNER="$(command -v apksigner || true)"
fi
if [ -z "$APKSIGNER" ] && [ -n "${ANDROID_HOME:-}" ]; then
  BUILD_TOOLS_PATH="$ANDROID_HOME/build-tools/$(ls "$ANDROID_HOME/build-tools" | sort | tail -n 1)"
  APKSIGNER="$BUILD_TOOLS_PATH/apksigner"
fi
if [ -z "$APKSIGNER" ] || [ ! -x "$APKSIGNER" ]; then
  echo "Error: apksigner not found or not executable on this host." >&2
  exit 1
fi

# Create output directory for signed APKs
mkdir -p "$SIGNED_APK_OUTPUT_DIR"

# Log the start of the process
echo "===================="
echo "Building APK Process"
echo "===================="
echo "Build Type: $BUILD_TYPE"
echo "===================="

# Step 1: Clean the project
if [ "${SKIP_CLEAN:-false}" != "true" ]; then
  echo "[Step 1] Cleaning the project..."
  ./gradlew clean --no-configuration-cache
  echo "Project cleaned successfully."
else
  echo "[Step 1] Skipping clean; using the verified incremental release build."
fi

# Step 2: Build the APK
echo "[Step 2] Building APK..."
./gradlew "$MODULE":assemble"$BUILD_TYPE"
echo "APK built successfully."

# Step 3: Locate the built APKs
APK_PATHS=$(find "$APK_OUTPUT_DIR" -name "*.apk")
if [ -z "$APK_PATHS" ]; then
  echo "Error: APKs not found in $APK_OUTPUT_DIR"
  exit 1
fi
echo "Built APKs located: $APK_PATHS"

# Step 4: Align and sign each APK
for APK_PATH in $APK_PATHS; do
  RELEASE_NAME=$(basename "${APK_PATH/-unsigned/}")
  RELEASE_NAME="${RELEASE_NAME/app-/}"
  RELEASE_NAME="${RELEASE_NAME/$MODULE-/}"
  SIGNED_APK_PATH="$SIGNED_APK_OUTPUT_DIR/UnX-Music-$BUILD_VARIANT$NAME_SUFFIX-$(basename "$RELEASE_NAME")"

  echo "[Step 4] Signing the aligned APK: $APK_PATH..."
  if [ "$BUILD_TYPE" = "release" ]; then
    "$APKSIGNER" sign \
      --alignment-preserved \
      --lineage "$SIGNING_LINEAGE_PATH" \
      --rotation-min-sdk-version 33 \
      --ks "$OLD_KEYSTORE_PATH" \
      --ks-key-alias "$OLD_KEY_ALIAS" \
      --ks-pass pass:"$OLD_KEYSTORE_PASSWORD" \
      --key-pass pass:"$OLD_KEY_PASSWORD" \
      --next-signer \
      --ks "$KEYSTORE_PATH" \
      --ks-key-alias "$KEY_ALIAS" \
      --ks-pass pass:"$KEYSTORE_PASSWORD" \
      --key-pass pass:"$KEY_PASSWORD" \
      --out "$SIGNED_APK_PATH" \
      "$APK_PATH"
  else
    "$APKSIGNER" sign \
      --alignment-preserved \
      --ks "$KEYSTORE_PATH" \
      --ks-key-alias "$KEY_ALIAS" \
      --ks-pass pass:"$KEYSTORE_PASSWORD" \
      --key-pass pass:"$KEY_PASSWORD" \
      --out "$SIGNED_APK_PATH" \
      "$APK_PATH"
  fi
  echo "APK signed successfully: $SIGNED_APK_PATH"

  echo "[Step 6] Verifying the signed APK: $SIGNED_APK_PATH..."
  "$APKSIGNER" verify --verbose "$SIGNED_APK_PATH"
  echo "Signed APK verified successfully: $SIGNED_APK_PATH"
done

echo "[Step 7] Cleaning up temporary files..."
cd "$SIGNED_APK_OUTPUT_DIR"
rm -f *.idsig
rm -f *unsigned*

# Completion message
echo "===================="
echo "Process Completed Successfully!"
echo "Signed APKs Path: $SIGNED_APK_OUTPUT_DIR"
echo "===================="
