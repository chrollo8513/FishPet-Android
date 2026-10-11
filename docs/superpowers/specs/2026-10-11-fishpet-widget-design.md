# FishPet Widget Redesign

## Intent

FishPet remains an offline, widget-first Android virtual pet. The home-screen widget is the primary aquarium; the activity is a compact companion for inspecting and resetting the pet. The design must remain compatible with API 26+, avoid services and third-party libraries, and preserve the existing GitHub Actions APK build.

## Architecture

`FishSimulation` will be a pure Kotlin simulation layer. It owns bounded elapsed-time advancement, hunger, health, growth stages, death, feeding cooldowns, and the temporary feeding scene. A small `FishStore` adapter will atomically load/save the model in `SharedPreferences` and expose refresh, feed, and reset actions.

`FishArt` will render a self-contained illustrated aquarium bitmap using Canvas: gradients, scenery, bubbles, water highlights, and a state-sensitive goldfish. The widget will use supported RemoteViews controls (ImageView, TextView, ProgressBar, Button) and a single one-shot alarm after a successful feed to clear the event frame. It will not use a continuous animation loop, service, or frequent updates.

## Behaviour

- Growth states are BABY, JUVENILE, and ADULT, calculated from configurable thresholds and healthy care time.
- Hunger and health advance deterministically from timestamps; a maximum single clock jump is simulated to protect against implausible clock changes while ordinary multi-day absence remains supported.
- Healthy care grows the fish; starvation reduces health, suppresses growth, and eventually persists death. A dead fish cannot be fed.
- Feeding uses a configurable cooldown, restores hunger/health, increments a local feed count, and opens a short persisted feeding scene.
- The widget shows stage, hunger state, health, compact progress bars, and a feed affordance. A companion app shows age, growth, feed count, status, reset, and widget instructions.

## Testing

Local JVM tests will cover healthy growth, starvation/death, cooldown, recovery, and rollback-safe timestamps. The Android build will be run if local Gradle is available; otherwise the exact limitation will be reported.
