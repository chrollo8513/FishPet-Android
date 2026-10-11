package com.example.fishpet

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.SystemClock
import android.widget.RemoteViews
import kotlin.math.max

class FishWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) = refresh(context)

    override fun onAppWidgetOptionsChanged(context: Context, manager: AppWidgetManager, appWidgetId: Int, newOptions: android.os.Bundle) {
        refresh(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_FEED -> {
                feedAndRefresh(context)
            }
            ACTION_PET -> petAndRefresh(context)
            ACTION_CLEAR_FEEDING_SCENE -> refresh(context)
            else -> super.onReceive(context, intent)
        }
    }

    override fun onDisabled(context: Context) {
        val alarm = context.getSystemService(AlarmManager::class.java)
        alarm.cancel(clearSceneIntent(context))
        super.onDisabled(context)
    }

    companion object {
        private const val ACTION_FEED = "com.example.fishpet.FEED"
        private const val ACTION_PET = "com.example.fishpet.PET"
        private const val ACTION_CLEAR_FEEDING_SCENE = "com.example.fishpet.CLEAR_FEEDING_SCENE"
        private const val FEED_REQUEST_CODE = 1001
        private const val PET_REQUEST_CODE = 1003
        private const val CLEAR_REQUEST_CODE = 1002

        fun refresh(context: Context, latestFeedResult: FeedResult = FeedResult.NONE) {
            val now = System.currentTimeMillis() / 1000L
            val state = FishStore.refresh(context, now)
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, FishWidget::class.java))
            ids.forEach { id -> manager.updateAppWidget(id, buildRemoteViews(context, state, now, latestFeedResult)) }
        }

        fun feedAndRefresh(context: Context): FishUpdate {
            val update = FishStore.feed(context)
            if (update.result == FeedResult.FED) {
                FishSounds.play(context, FishSoundEffect.FEED)
                FishSounds.playDelayed(context, FishSoundEffect.EAT, 300L)
                scheduleSceneClear(context, update.state)
            }
            refresh(context, update.result)
            return update
        }

        fun petAndRefresh(context: Context): FishState {
            val state = FishStore.pet(context)
            if (!state.dead) {
                val sound = if (state.swimmingState == SwimmingState.STARVING || state.swimmingState == SwimmingState.CRITICAL) {
                    FishSoundEffect.STARVING
                } else {
                    FishSoundEffect.HAPPY
                }
                FishSounds.play(context, sound)
                scheduleSceneClear(context, state)
            }
            refresh(context)
            return state
        }

        private fun buildRemoteViews(context: Context, state: FishState, now: Long, result: FeedResult): RemoteViews {
            return RemoteViews(context.packageName, R.layout.fish_widget).apply {
                setImageViewBitmap(R.id.aquarium, FishArt.render(state, now))
                setTextViewText(R.id.fish_title, "${state.name} · ${state.stage().label}")
                setTextViewText(R.id.mood, if (state.dead) "✕ New fish needed" else "${state.mood.label} · ${ageText(state.ageSeconds(now))}")
                setTextViewText(R.id.hunger_label, "Hunger  ${state.hungerStatus.label} ${state.hunger.toInt()}%")
                setTextViewText(R.id.health_label, "Health  ${state.health.toInt()}%")
                setProgressBar(R.id.hunger_progress, 100, state.hunger.toInt().coerceIn(0, 100), false)
                setProgressBar(R.id.health_progress, 100, state.health.toInt().coerceIn(0, 100), false)
                setTextViewText(R.id.feed_feedback, feedbackText(state, now, result))
                setTextViewText(R.id.feed, feedLabel(state, now))
                setBoolean(R.id.feed, "setEnabled", !state.dead)
                setOnClickPendingIntent(R.id.feed, feedIntent(context))
                setOnClickPendingIntent(R.id.aquarium, petIntent(context))
            }
        }

        private fun feedbackText(state: FishState, now: Long, result: FeedResult): String = when {
            result == FeedResult.FED || state.isFeeding(now) -> "Yum! Pellets are drifting down."
            result == FeedResult.DEAD || state.dead -> "This fish has passed on. Reset in the app."
            result == FeedResult.COOLDOWN -> "Tiny tummy is resting — try again soon."
            else -> "Tap Feed to care for ${state.name}."
        }

        private fun feedLabel(state: FishState, now: Long): String = when {
            state.dead -> "Resting"
            state.lastFedAtSeconds > 0L && now - state.lastFedAtSeconds < FishBalance().feedingCooldownSeconds -> {
                val minutes = max(1L, (FishBalance().feedingCooldownSeconds - (now - state.lastFedAtSeconds) + 59L) / 60L)
                "Feed · ${minutes}m"
            }
            else -> "Feed"
        }

        private fun ageText(seconds: Long): String = when {
            seconds >= 86_400L -> "${seconds / 86_400L}d old"
            seconds >= 3_600L -> "${seconds / 3_600L}h old"
            else -> "new arrival"
        }

        private fun feedIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
            context,
            FEED_REQUEST_CODE,
            Intent(context, FishWidget::class.java).setAction(ACTION_FEED).setData(Uri.parse("fishpet://feed")),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        private fun clearSceneIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
            context,
            CLEAR_REQUEST_CODE,
            Intent(context, FishWidget::class.java).setAction(ACTION_CLEAR_FEEDING_SCENE).setData(Uri.parse("fishpet://clear-feeding")),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        private fun petIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
            context,
            PET_REQUEST_CODE,
            Intent(context, FishWidget::class.java).setAction(ACTION_PET).setData(Uri.parse("fishpet://pet")),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        private fun scheduleSceneClear(context: Context, state: FishState) {
            val nowSeconds = System.currentTimeMillis() / 1000L
            val delaySeconds = FishSceneTiming.secondsUntilRefresh(state, nowSeconds) ?: return
            context.getSystemService(AlarmManager::class.java).set(
                AlarmManager.ELAPSED_REALTIME,
                SystemClock.elapsedRealtime() + delaySeconds * 1000L,
                clearSceneIntent(context),
            )
        }
    }
}
