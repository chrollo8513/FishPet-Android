# FishPet Widget Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Turn the minimal FishPet widget into a polished, persistent 2D aquarium pet without continuous background work.

**Architecture:** A pure Kotlin simulation model provides testable care and growth rules; SharedPreferences persists it atomically. Canvas draws the aquarium bitmap while RemoteViews provides compact status and action controls.

**Tech Stack:** Kotlin, Android App Widgets/RemoteViews, Canvas, SharedPreferences, JUnit 4.

**Spec:** `docs/superpowers/specs/2026-10-11-fishpet-widget-design.md`

## Global Constraints

- Retain API 26 minimum and no third-party runtime dependencies.
- No continuous animation loop, foreground service, or aggressive widget schedule.
- Preserve the existing GitHub Actions APK workflow and do not commit or push.
- Use timestamp-based local persistence and supported RemoteViews controls.

## Review Focus

- Device clock moves backwards: no hunger, health, or growth regression.
- Very large forward clock movement: avoid implausible multi-year simulation in one refresh.
- Feed taps repeated inside the cooldown: never add food or reset cooldown.
- Death state survives refresh and reset is the only way to create a live fish.
- Widget event alarm is absent/delayed: persisted feeding frame expires safely on the next refresh.

---

### Task 1: Deterministic pet simulation

**Files:**
- Create: `app/src/main/java/com/example/fishpet/FishSimulation.kt`
- Create: `app/src/test/java/com/example/fishpet/FishSimulationTest.kt`
- Modify: `app/build.gradle.kts`

**Produces:** `FishState`, `FishBalance`, `FishSimulator`, `FeedResult`, and growth/hunger enums consumed by persistence and rendering.

- [ ] Write failing JVM tests for growth, starvation/death, feed cooldown, health recovery, and clock rollback.
- [ ] Run the focused tests and confirm they fail because the simulation API is absent.
- [ ] Implement the immutable timestamp simulation and configurable balance rules.
- [ ] Run the focused tests and all local JVM tests.

### Task 2: Persistence and widget actions

**Files:**
- Modify: `app/src/main/java/com/example/fishpet/FishStore.kt`
- Modify: `app/src/main/java/com/example/fishpet/FishWidget.kt`
- Modify: `app/src/main/AndroidManifest.xml`

**Consumes:** `FishSimulator.advance`, `FishSimulator.feed`, `FishState`.

- [ ] Replace the mutable JSON model with synchronized persisted simulation actions.
- [ ] Add a one-shot post-feed widget refresh alarm and event-expiry-safe rendering refresh.
- [ ] Run the focused simulation test suite.

### Task 3: Illustrated widget and companion screen

**Files:**
- Modify: `app/src/main/java/com/example/fishpet/FishArt.kt`
- Modify: `app/src/main/java/com/example/fishpet/MainActivity.kt`
- Modify: `app/src/main/res/layout/fish_widget.xml`
- Modify: `app/src/main/res/drawable/widget_bg.xml`
- Create: progress and button drawables under `app/src/main/res/drawable/`
- Modify: `README.md`

**Consumes:** persisted `FishState` and its derived appearance/status values.

- [ ] Render the stage, health, hunger, direction, and feeding frame in a layered Canvas aquarium.
- [ ] Add compact RemoteViews status bars and an accessible feed action.
- [ ] Make the companion screen show current care, growth, age, reset, and setup help.
- [ ] Run available build/test verification and inspect changed files.
