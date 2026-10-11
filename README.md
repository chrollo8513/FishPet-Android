# FishPet Android Widget (MVP v0.1)

This is an Android Studio Gradle project, **not a compiled APK**. It contains a widget-first offline fish pet with 2D programmatically drawn artwork, Feed button, hunger, health, growth, permanent death and a minimal companion activity. The widget appears in the Android widget picker after app installation; users must add it manually.

## Build an APK on Windows

1. Install Android Studio, Android SDK 35, and JDK 17 (Android Studio usually bundles a JDK).
2. Open this folder (`FishPet-Android`) in Android Studio. Allow Gradle sync and install prompted SDK components. An internet connection is needed for initial Gradle dependencies.
3. Select **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
4. Find `app/build/outputs/apk/debug/app-debug.apk` and send it to an Android phone.
5. Install the APK (allow installation from the source if prompted). Long-press home screen > Widgets > FishPet > Add.
6. Use **Feed** on the widget. The pet persists between app launches and widget updates.

## Limitations

- Requires Android 8.0+ (API 26); different launchers may handle widget sizing differently.
- Home-screen fish artwork is a static snapshot, not continuous animation.
- Android schedules widget refreshes approximately every 30 minutes; actual timing may vary. Feeding refreshes immediately.
- Feeding is rate-limited to once per 30 minutes. Hunger rises 3/hour; health decreases 2/hour after hunger exceeds 70; growth increases while adequately fed.
- The game is offline, local to each phone, and does not sync between devices.
- This code has not been compiled/tested in an Android SDK environment here. Debug builds are suitable for personal testing, not public release. For distribution, create a signed release build.
