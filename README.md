# MheyFish Android Widget

MheyFish is an offline Android home-screen virtual aquarium. The widget is the primary experience: it renders a cute 3D-looking goldfish in a glass aquarium, with hunger and health indicators, growth stage, Feed action, and short tap reactions. The lightweight companion app shows the same pet in a continuously swimming live aquarium, plus care stats, reset, sound, and widget setup instructions.

## Features

- Rounded glass-tank aquarium with a visible water line, depth shading, front-glass reflections, gravel, rocks, plants, bubbles, ripples, and a polished 3D-looking goldfish.
- A state-sensitive swimming pose engine: baby fish are lively, adult fish are graceful, hungry fish slow down, critical fish barely move, and dead fish remain still.
- A smooth, frame-by-frame animated companion-app preview. The home-screen widget uses battery-friendly pose snapshots rather than unsupported continuous animation.
- Visible BABY, JUVENILE, and ADULT growth stages; healthy elapsed care advances growth.
- Deterministic timestamp simulation for hunger, health, starvation, recovery, death, and safe clock rollback handling.
- Persistent local state in `SharedPreferences`, including death, feeding history, a custom fish name, and a randomly selected baby-fish color.
- A cooldown-protected Feed action. Successful feeds play generated PCM plop/munch effects (when enabled), show pellets falling from the surface, then approach, bite/swallow, and satisfied poses before returning to idle.
- Tap the aquarium in the widget for a short persisted wiggle, blink, ripple, heart, or bubble-burst reaction. Sound effects request transient audio focus, respect silent/media-volume settings, and can be disabled in the companion app.
- Health and hunger badges are drawn above the fish inside the aquarium image; no continuous service or animation loop.

## Build the debug APK

1. Install Android Studio with Android SDK 35 and JDK 17.
2. Open this project and allow Gradle sync to download dependencies.
3. Run **Build > Build APK(s)**, or use `gradle assembleDebug --stacktrace` when Gradle is installed globally.
4. Install `app/build/outputs/apk/debug/app-debug.apk`, then long-press the home screen and add **MheyFish** from the widget picker.

The GitHub Actions workflow also builds and uploads a debug APK on pushes to `main` and manual dispatch.

## Widget animation limits

Android App Widgets are RemoteViews snapshots. The widget cannot continuously animate Canvas content reliably, so it displays a posed aquarium while idle and swaps only a few bitmap frames after Feed or aquarium taps. Feeding uses a pellet-drop, approach, bite/swallow, and celebration sequence. Tap reactions use a similarly short, best-effort sequence. The companion app is the only smooth animated preview. There is no permanently running animation loop, foreground service, or aggressive update schedule, so event frames can be delayed by launcher or power-management policy.

## Testing

`FishSimulationTest.kt` covers care/persistence behaviour, while `FishMotionTest.kt` covers motion energy, growth-stage tempo, eating poses, and dead-fish stillness. Run them with `gradle test` in an Android/Gradle environment.
