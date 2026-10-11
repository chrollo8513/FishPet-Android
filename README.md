# MheyFish Android Widget

MheyFish is an offline Android home-screen virtual aquarium. The widget is the primary experience: it renders a cute 2D goldfish with an illustrated aquarium, hunger and health indicators, growth stage, and an in-widget Feed action. The lightweight companion app shows the same pet, care stats, growth progress, feeding count, reset, and widget setup instructions.

## Features

- Canvas-rendered aquarium with blue water gradients, plants, rocks, bubbles, ripples, and a polished goldfish.
- A state-sensitive swimming pose engine: baby fish are lively, adult fish are graceful, hungry fish slow down, critical fish barely move, and dead fish remain still.
- A smooth, frame-by-frame animated companion-app preview. The home-screen widget uses battery-friendly pose snapshots rather than unsupported continuous animation.
- Visible BABY, JUVENILE, and ADULT growth stages; healthy elapsed care advances growth.
- Deterministic timestamp simulation for hunger, health, starvation, recovery, death, and safe clock rollback handling.
- Persistent local state in `SharedPreferences`, including death and feeding history.
- A cooldown-protected Feed action. Successful feeds play a generated plop/munch sound (when enabled), immediately show food pellets and an eating pose, and use one best-effort scheduled refresh to clear the event frame.
- Tap the aquarium in the widget for a short happy or tired reaction. Sound effects are generated locally, respect silent/media-volume settings, and can be disabled in the companion app.
- Compact widget progress bars for hunger and health; no continuous service or animation loop.

## Build the debug APK

1. Install Android Studio with Android SDK 35 and JDK 17.
2. Open this project and allow Gradle sync to download dependencies.
3. Run **Build > Build APK(s)**, or use `gradle assembleDebug --stacktrace` when Gradle is installed globally.
4. Install `app/build/outputs/apk/debug/app-debug.apk`, then long-press the home screen and add **MheyFish** from the widget picker.

The GitHub Actions workflow also builds and uploads a debug APK on pushes to `main` and manual dispatch.

## Widget animation limits

Android App Widgets are RemoteViews snapshots. The widget cannot continuously animate Canvas content reliably, so it displays a current swimming pose whenever Android updates it and immediately changes pose after Feed or aquarium taps. Feeding uses one immediate illustrated frame plus one non-wakeup, best-effort refresh after the latest transient event window. The companion app is the only smooth animated preview. There is no permanently running animation loop, foreground service, or aggressive update schedule, so the cleanup refresh can be delayed by launcher or power-management policy.

## Testing

`FishSimulationTest.kt` covers care/persistence behaviour, while `FishMotionTest.kt` covers motion energy, growth-stage tempo, eating poses, and dead-fish stillness. Run them with `gradle test` in an Android/Gradle environment.
