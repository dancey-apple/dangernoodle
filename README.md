# dangernoodle
Snake Game Clone

A Nokia-style Snake game for Android, controlled with swipes.

## Install (sideload)

1. Download [`dist/dangernoodle.apk`](dist/dangernoodle.apk) to your phone.
2. Open it, and allow "Install unknown apps" for your browser or file manager when prompted.
3. Requires Android 8.0 (API 26) or newer.

## How to play

- **Swipe** in any direction to steer. You can swipe anywhere on the screen, and quick
  back-to-back swipes are buffered so tight turns work.
- **Tap** to pause, tap again (or swipe) to resume. **Back** while paused returns to the menu.
- Eat the diamond pellets to grow and score (points per pellet = level).
- Every 5 pellets a bonus critter appears for a limited number of moves (countdown at the
  top right). Eat it fast for more points.
- **Walls ON**: hitting the border ends the game. **Walls OFF** (dashed border): edges wrap around.
- Levels 1–9 set the speed.
- **Themes** (tap the left/right side of the theme button on the menu): Classic, Synthwave,
  Vaporwave, Candy, Spooky (a skelly snake that eats ghosts) and Sci-Fi.

The top 10 scores are saved on the device. A qualifying score lets you enter three initials
(swipe up/down to change a letter, left/right to move between letters).

## Building

### Android Studio / Gradle

Open the project in Android Studio, or run `./gradlew assembleDebug`. Output goes to
`app/build/outputs/apk/`.

### Without the Android SDK

`scripts/build-apk.sh` builds the APK with just a JDK and the Debian/Ubuntu packaged tools:

```sh
sudo apt-get install aapt dalvik-exchange zipalign apksigner
./scripts/build-apk.sh        # writes dist/dangernoodle.apk
```

It uses `$ANDROID_HOME/platforms/android-34/android.jar` if available, otherwise downloads
Robolectric's API 34 `android-all` jar from Maven Central.

### Signing

Both builds sign with `keystore/sideload.jks` (password `dangernoodle`), so newer builds
install over older ones. This key is only for sideloading; don't use it for a Play Store
release.
