# FishPet 3D Pet Redesign

## Intent

FishPet remains an offline, widget-first Android pet. The widget is a mostly still glass-aquarium snapshot that plays only short, event-driven sequences after Feed or fish taps. The app is the richer, continuously animated companion view. The implementation keeps the existing package, API 26 floor, local persistence, and GitHub Actions build workflow.

## Architecture

The pure Kotlin model remains the source of truth. `FishState` gains a persisted `reaction` and `reactionSceneUntilSeconds`; `FishSimulator.pet` chooses a bounded reaction and `FishSceneTiming` selects only the next frame required by a feed or reaction. State remains in `SharedPreferences`, so food, care effects, and the selected interaction survive widget refreshes.

`FishMotion` maps the state and timestamp to a pose. It exposes feeding phases (drop, approach, bite, celebrate), mouth openness, eye expression, and reaction effect metadata. `FishArt` uses that pose to draw a 3D-looking goldfish and a rounded glass tank with layered water, reflections, gravel, plants, bubbles, food, and transient effects. It renders a bitmap for `RemoteViews` and directly draws the same scene in the app preview.

## Behaviour

- A successful feed immediately stores care effects, then shows food drop, approach, bite/swallow, and satisfied frames before returning to idle. Cooldown and death feedback do not schedule a success animation.
- Tapping the fish selects one persisted living-fish reaction: wiggle, blink, ripple, heart, or bubble burst. It schedules a handful of best-effort, non-wakeup widget snapshots and then expires safely.
- The app continues to redraw only while visible. Healthy fish swim smoothly, hungry fish slow down, starving fish move weakly, and dead fish float without fin/tail movement.
- The widget has no service, continuous loop, or persistent alarm. Its `AlarmManager` work is limited to the next timestamp in a short interaction sequence.

## Validation

Pure JVM tests cover reaction persistence timing, living/dead interaction rules, feed event frames, and condition-sensitive motion. The Android build is run when a Gradle executable or wrapper is available; otherwise that environment limitation is reported.
