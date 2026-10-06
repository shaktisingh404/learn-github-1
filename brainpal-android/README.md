# BrainPal Counter (Android)

Counts Instagram Reels and YouTube Shorts as you scroll and shows live counts
around the camera notch (▶ Reels · ▷ Shorts). Data stays on-device.

## Build
Open `brainpal-android/` in Android Studio, or run `./gradlew assembleDebug`
(needs the Android SDK). CI also builds the APK: Actions → "Android APK".

## Use
1. Install the APK and open the app.
2. Tap **Enable counting** → Accessibility → BrainPal Counter → turn on.
3. Open Reels/Shorts. The pill appears at the notch and updates per swipe.

## Notes
- Uses an AccessibilityService (`TYPE_ACCESSIBILITY_OVERLAY`), so no "draw over apps" permission is needed.
- Detection relies on Instagram/YouTube view ids (`ReelCounterService.kt`); apps change these, so expect to adjust them.
- Play Store requires an Accessibility declaration and in-app disclosure for this kind of service.
