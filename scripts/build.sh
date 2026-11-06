#!/usr/bin/env bash
set -euo pipefail

ROOT="$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"
BUILD="$ROOT/build"
DEPS="$ROOT/.deps"
OUT_APK="$BUILD/MHOutlanders-Spoof.apk"
ANDROID_JAR="${ANDROID_JAR:-$DEPS/android.jar}"
R8_JAR="${R8_JAR:-$DEPS/r8.jar}"

KEYSTORE="${KEYSTORE:-$ROOT/keystore/lab.keystore}"
KEY_ALIAS="${KEY_ALIAS:-lab}"
KEY_PASS="${KEY_PASS:-labpass}"
STORE_PASS="${STORE_PASS:-labpass}"

die() { printf 'error: %s\n' "$*" >&2; exit 1; }
need() { command -v "$1" >/dev/null 2>&1 || die "missing command: $1"; }

need java
need javac
need jar
need keytool
need aapt2
need apksigner
need zip
need curl
need unzip

mkdir -p "$BUILD/classes" "$BUILD/stubs" "$BUILD/dex" "$DEPS"

if [ ! -f "$ANDROID_JAR" ]; then
  echo "[*] downloading Android platform 35 android.jar"
  curl -fL --retry 2 -o "$DEPS/platform-35.zip" \
    https://dl.google.com/android/repository/platform-35_r02.zip
  unzip -p "$DEPS/platform-35.zip" android-35/android.jar > "$ANDROID_JAR"
fi

if [ ! -f "$R8_JAR" ]; then
  echo "[*] downloading R8/D8"
  curl -fL --retry 2 -o "$R8_JAR" \
    https://maven.google.com/com/android/tools/r8/8.5.35/r8-8.5.35.jar
fi

echo "[*] compiling Xposed API stubs"
find "$ROOT/stubs" -name '*.java' > "$BUILD/stubs-sources.txt"
javac -source 8 -target 8 -d "$BUILD/stubs" @"$BUILD/stubs-sources.txt"
jar cf "$BUILD/stubs.jar" -C "$BUILD/stubs" .

echo "[*] compiling module"
find "$ROOT/src" -name '*.java' > "$BUILD/app-sources.txt"
javac -source 8 -target 8 \
  -cp "$ANDROID_JAR:$BUILD/stubs.jar" \
  -d "$BUILD/classes" @"$BUILD/app-sources.txt"

echo "[*] dexing"
rm -f "$BUILD/dex/classes.dex"
# shellcheck disable=SC2046
java -cp "$R8_JAR" com.android.tools.r8.D8 \
  --release --min-api 26 \
  --lib "$ANDROID_JAR" --lib "$BUILD/stubs.jar" \
  --output "$BUILD/dex" \
  $(find "$BUILD/classes" -name '*.class')

echo "[*] linking APK"
rm -f "$BUILD/unsigned.apk" "$OUT_APK" "$OUT_APK.idsig"
aapt2 link \
  -o "$BUILD/unsigned.apk" \
  -I "$ANDROID_JAR" \
  --manifest "$ROOT/AndroidManifest.xml" \
  -A "$ROOT/assets" \
  --min-sdk-version 26 \
  --target-sdk-version 35

( cd "$BUILD" && zip -j unsigned.apk dex/classes.dex >/dev/null )

if [ ! -f "$KEYSTORE" ]; then
  echo "[*] creating local signing key (do not commit this file)"
  mkdir -p "$(dirname "$KEYSTORE")"
  keytool -genkeypair \
    -keystore "$KEYSTORE" \
    -alias "$KEY_ALIAS" \
    -keyalg RSA -keysize 2048 -validity 10000 \
    -storepass "$STORE_PASS" -keypass "$KEY_PASS" \
    -dname "CN=MHOutlanders Spoof"
fi

echo "[*] signing"
apksigner sign \
  --ks "$KEYSTORE" \
  --ks-key-alias "$KEY_ALIAS" \
  --ks-pass "pass:$STORE_PASS" \
  --key-pass "pass:$KEY_PASS" \
  --out "$OUT_APK" \
  "$BUILD/unsigned.apk"

apksigner verify --verbose "$OUT_APK" | sed -n '1,12p'
sha256sum "$OUT_APK"
echo "[+] done: $OUT_APK"
