package com.example.fishpet

import android.content.Context
import org.json.JSONObject

object FishStore {
    private const val PREFERENCES = "fishpet"
    private const val KEY_STATE = "fish"

    @Synchronized
    fun refresh(context: Context, nowSeconds: Long = nowSeconds()): FishState {
        val preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
        val state = FishSimulator.advance(read(preferences.getString(KEY_STATE, null), nowSeconds), nowSeconds)
        persist(preferences.edit(), state)
        return state
    }

    @Synchronized
    fun feed(context: Context, nowSeconds: Long = nowSeconds()): FishUpdate {
        val preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
        val update = FishSimulator.feed(read(preferences.getString(KEY_STATE, null), nowSeconds), nowSeconds)
        persist(preferences.edit(), update.state)
        return update
    }

    @Synchronized
    fun pet(context: Context, nowSeconds: Long = nowSeconds()): FishState {
        val preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
        val state = FishSimulator.pet(read(preferences.getString(KEY_STATE, null), nowSeconds), nowSeconds)
        persist(preferences.edit(), state)
        return state
    }

    @Synchronized
    fun rename(context: Context, proposedName: String): FishState {
        val preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
        val state = FishSimulator.rename(read(preferences.getString(KEY_STATE, null), nowSeconds()), proposedName)
        persist(preferences.edit(), state)
        return state
    }

    @Synchronized
    fun reset(context: Context, nowSeconds: Long = nowSeconds()): FishState {
        val preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
        val fresh = FishSimulator.newBaby(nowSeconds, FishColor.random())
        persist(preferences.edit(), fresh)
        return fresh
    }

    private fun read(raw: String?, fallbackNowSeconds: Long): FishState {
        val objectValue = try {
            JSONObject(raw ?: "{}")
        } catch (_: Exception) {
            JSONObject()
        }
        val updated = objectValue.optLong("updatedAtSeconds", objectValue.optLong("updated", fallbackNowSeconds))
        return FishState(
            name = objectValue.optString("name", "Bubbles").ifBlank { "Bubbles" },
            color = FishColor.fromStorage(objectValue.optString("color", FishColor.ORANGE.name)),
            hunger = objectValue.optDouble("hunger", 15.0),
            health = objectValue.optDouble("health", 100.0),
            growth = objectValue.optDouble("growth", 0.0),
            updatedAtSeconds = updated,
            lastFedAtSeconds = objectValue.optLong("lastFedAtSeconds", objectValue.optLong("lastFed", 0L)),
            bornAtSeconds = objectValue.optLong("bornAtSeconds", updated),
            feedingSceneUntilSeconds = objectValue.optLong("feedingSceneUntilSeconds", 0L),
            playfulSceneUntilSeconds = objectValue.optLong("playfulSceneUntilSeconds", 0L),
            reactionSceneUntilSeconds = objectValue.optLong("reactionSceneUntilSeconds", 0L),
            reaction = FishReaction.fromStorage(objectValue.optString("reaction", FishReaction.NONE.name)),
            foodEaten = objectValue.optInt("foodEaten", 0),
            dead = objectValue.optBoolean("dead", false),
        )
    }

    private fun persist(editor: android.content.SharedPreferences.Editor, state: FishState) {
        val value = JSONObject()
            .put("name", state.name)
            .put("color", state.color.name)
            .put("hunger", state.hunger)
            .put("health", state.health)
            .put("growth", state.growth)
            .put("updatedAtSeconds", state.updatedAtSeconds)
            .put("lastFedAtSeconds", state.lastFedAtSeconds)
            .put("bornAtSeconds", state.bornAtSeconds)
            .put("feedingSceneUntilSeconds", state.feedingSceneUntilSeconds)
            .put("playfulSceneUntilSeconds", state.playfulSceneUntilSeconds)
            .put("reactionSceneUntilSeconds", state.reactionSceneUntilSeconds)
            .put("reaction", state.reaction.name)
            .put("foodEaten", state.foodEaten)
            .put("dead", state.dead)
            .toString()
        editor.putString(KEY_STATE, value).commit()
    }

    private fun nowSeconds(): Long = System.currentTimeMillis() / 1000L
}
