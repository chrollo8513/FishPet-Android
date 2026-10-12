# FishPet 3D Pet Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make FishPet a polished 3D-looking virtual fish whose widget runs short interaction scenes and whose companion app swims continuously.

**Architecture:** Persist event-scene state in the existing pure model, derive short poses from timestamps, and render a shared Canvas aquarium as RemoteViews snapshots or an app view. Widget work is limited to immediate and short one-shot refreshes.

**Tech Stack:** Kotlin, Android App Widgets/RemoteViews, Canvas, SharedPreferences, JUnit 4.

**Spec:** `docs/superpowers/specs/2026-10-12-fishpet-3d-pet-design.md`

## Global Constraints

- Retain API 26, package `com.example.fishpet`, existing Gradle setup, and GitHub Actions workflow.
- Persist pet state locally and retain current hunger, health, growth, death, cooldown, reset, and sound behaviour.
- Do not use a foreground service or continuous widget animation.
- Do not commit or push.

## Review Focus

- A dead fish must never start a feed or tap reaction.
- A cooldown tap must retain its previous event scene and food count.
- Delayed/missed widget alarms must expire scenes safely based on timestamps.
- All reaction types must complete in a bounded time without an additional alarm loop.
- The app must stop its redraw loop when it is not visible.

---

### Task 1: Persisted interaction scene model

**Files:**
- Modify: `app/src/main/java/com/example/fishpet/FishSimulation.kt`
- Modify: `app/src/main/java/com/example/fishpet/FishStore.kt`
- Modify: `app/src/test/java/com/example/fishpet/FishSimulationTest.kt`

**Interfaces:**
- Produces: `FishReaction`, reaction fields on `FishState`, and timestamp-driven `FishSceneTiming.nextRefreshAtSeconds` for the widget.

- [ ] Add failing JVM tests for reaction selection, dead-fish rejection, and bounded frame timing.
- [ ] Run the focused tests and observe the missing reaction API failure.
- [ ] Add the persisted reaction scene model and JSON serialization.
- [ ] Run the focused tests and the full unit suite.

### Task 2: Condition-aware pose and reactions

**Files:**
- Modify: `app/src/main/java/com/example/fishpet/FishMotion.kt`
- Modify: `app/src/test/java/com/example/fishpet/FishMotionTest.kt`

**Interfaces:**
- Consumes: `FishState` reaction scene fields.
- Produces: reaction-aware `SwimmingPose` metadata for `FishArt`.

- [ ] Add failing tests for feed bite mouth openness and distinct reaction poses.
- [ ] Run the focused tests and observe the missing pose metadata failure.
- [ ] Implement timestamp-derived feeding and reaction poses without mutable animation state.
- [ ] Run the focused tests and the full unit suite.

### Task 3: Glass tank rendering and widget scene updates

**Files:**
- Modify: `app/src/main/java/com/example/fishpet/FishArt.kt`
- Modify: `app/src/main/java/com/example/fishpet/FishWidget.kt`
- Modify: `app/src/main/res/layout/fish_widget.xml`
- Modify: `app/src/main/res/drawable/widget_bg.xml`

**Interfaces:**
- Consumes: reaction-aware `SwimmingPose` and `FishSceneTiming`.

- [ ] Render the rounded glass tank, depth layers, fish shading, food, bite, and reaction effects from a single Canvas path.
- [ ] Schedule only the next eligible widget frame after living-fish interactions.
- [ ] Update widget styling and accessibility copy.
- [ ] Compile/debug-build when the local Gradle environment supports it.

### Task 4: Companion experience and documentation

**Files:**
- Modify: `app/src/main/java/com/example/fishpet/MainActivity.kt`
- Modify: `README.md`

**Interfaces:**
- Consumes: the same persisted state and shared app renderer.

- [ ] Improve companion app hierarchy and care feedback while preserving its visible-only redraw behaviour.
- [ ] Document widget capabilities, Android limitations, and APK instructions.
- [ ] Run available test/build verification and inspect all modified files.
