#!/usr/bin/env bash
# Builds a signed, sideloadable APK without Gradle or the Android SDK manager.
#
# Needs: a JDK (javac), aapt, dx (or dalvik-exchange), zipalign and apksigner.
# On Debian/Ubuntu: sudo apt-get install aapt dalvik-exchange zipalign apksigner
#
# The framework android.jar is taken from $ANDROID_JAR, else from
# $ANDROID_HOME/platforms/android-34, else Robolectric's android-all jar
# (API 34) is downloaded once from Maven Central into build/cache.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SRC="$ROOT/app/src/main"
BUILD="$ROOT/build/apk"
OUT="${OUT:-$ROOT/dist/dangernoodle.apk}"
KEYSTORE="${KEYSTORE:-$ROOT/keystore/sideload.jks}"
KS_PASS="${KS_PASS:-dangernoodle}"
KEY_ALIAS="${KEY_ALIAS:-sideload}"

DX="$(command -v dx || command -v dalvik-exchange || true)"
for tool in javac aapt zipalign apksigner; do
    command -v "$tool" >/dev/null || { echo "missing tool: $tool" >&2; exit 1; }
done
[ -n "$DX" ] || { echo "missing tool: dx / dalvik-exchange" >&2; exit 1; }

if [ -z "${ANDROID_JAR:-}" ]; then
    if [ -n "${ANDROID_HOME:-}" ] && [ -f "$ANDROID_HOME/platforms/android-34/android.jar" ]; then
        ANDROID_JAR="$ANDROID_HOME/platforms/android-34/android.jar"
    else
        ANDROID_JAR="$ROOT/build/cache/android-all-14.jar"
        if [ ! -f "$ANDROID_JAR" ]; then
            mkdir -p "$(dirname "$ANDROID_JAR")"
            echo "Downloading android-all (API 34) from Maven Central..."
            curl -fsSL -o "$ANDROID_JAR.tmp" \
                https://repo1.maven.org/maven2/org/robolectric/android-all/14-robolectric-10818077/android-all-14-robolectric-10818077.jar
            mv "$ANDROID_JAR.tmp" "$ANDROID_JAR"
        fi
    fi
fi

rm -rf "$BUILD"
mkdir -p "$BUILD/gen" "$BUILD/classes" "$(dirname "$OUT")"

echo "Packaging resources..."
aapt package -f -m \
    -J "$BUILD/gen" \
    -M "$SRC/AndroidManifest.xml" \
    -S "$SRC/res" \
    -I "$ANDROID_JAR" \
    -F "$BUILD/unsigned.apk"

echo "Compiling Java..."
find "$SRC/java" "$BUILD/gen" -name '*.java' > "$BUILD/sources.txt"
# java.* comes from the JDK's Java 8 API; android.* from the framework jar.
javac -nowarn -Xlint:-options -encoding UTF-8 --release 8 \
    -classpath "$ANDROID_JAR" \
    -d "$BUILD/classes" @"$BUILD/sources.txt"

echo "Dexing..."
"$DX" --dex --min-sdk-version=26 --output="$BUILD/classes.dex" "$BUILD/classes"
(cd "$BUILD" && aapt add -f unsigned.apk classes.dex >/dev/null)

echo "Aligning and signing..."
zipalign -f -p 4 "$BUILD/unsigned.apk" "$BUILD/aligned.apk"
apksigner sign --ks "$KEYSTORE" --ks-pass "pass:$KS_PASS" --ks-key-alias "$KEY_ALIAS" \
    --key-pass "pass:$KS_PASS" --out "$OUT" "$BUILD/aligned.apk"
apksigner verify "$OUT"
rm -f "$OUT.idsig"

echo "Built $OUT ($(du -h "$OUT" | cut -f1))"
