package com.example.fishpet

import android.content.Context
import org.json.JSONObject
import kotlin.math.max
import kotlin.math.min

data class FishState(var hunger: Double = 15.0, var health: Double = 100.0, var growth: Double = 0.0, var updated: Long = System.currentTimeMillis() / 1000, var lastFed: Long = 0, var dead: Boolean = false) {
    val stage get() = if (growth < 30) "Baby" else if (growth < 70) "Juvenile" else "Adult"
    val mood get() = if (dead) "Dead" else if (health < 40) "Sick" else if (hunger > 70) "Hungry" else "Happy"
    fun advance(now: Long) {
        if (dead || now <= updated) return
        val hours = (now - updated) / 3600.0
        val safe = if (hunger < 70) min(hours, (70 - hunger) / 3.0) else 0.0
        growth = min(100.0, growth + safe * 0.5)
        health = max(0.0, health - (hours - safe) * 2)
        hunger = min(100.0, hunger + hours * 3)
        dead = health <= 0
        updated = now
    }
    fun feed(now: Long): Boolean {
        advance(now)
        if (dead || now - lastFed < 1800) return false
        hunger = max(0.0, hunger - 35)
        lastFed = now
        return true
    }
}

object FishStore {
    private const val KEY = "fish"
    @Synchronized fun update(ctx: Context, reset: Boolean = false, feed: Boolean = false): FishState {
        val prefs = ctx.getSharedPreferences("fishpet", Context.MODE_PRIVATE)
        val now = System.currentTimeMillis() / 1000
        val o = try { JSONObject(prefs.getString(KEY, "{}") ?: "{}") } catch (_: Exception) { JSONObject() }
        val state = if (reset) FishState(updated = now) else FishState(o.optDouble("hunger",15.0),o.optDouble("health",100.0),o.optDouble("growth",0.0),o.optLong("updated",now),o.optLong("lastFed",0),o.optBoolean("dead",false))
        state.advance(now)
        if (feed) state.feed(now)
        prefs.edit().putString(KEY,JSONObject().put("hunger",state.hunger).put("health",state.health).put("growth",state.growth).put("updated",state.updated).put("lastFed",state.lastFed).put("dead",state.dead).toString()).commit()
        return state
    }
}
