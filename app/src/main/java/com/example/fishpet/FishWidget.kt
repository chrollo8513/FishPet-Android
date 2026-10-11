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
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        refresh(context)
    }

    override fun onAppWidgetOptionsChanged(context: Context, manager: AppWidgetManager, appWidgetId: Int, newOptions: android.os.Bundle) {
        refresh(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_FEED -> feedAndRefresh(context)
            ACTION_PET -> petAndRefresh(context)
            ACTION_ADVANCE_SCENE -> scheduleSceneRefresh(context, refresh(context))
            else -> super.onReceive(context, intent)
        }
    }

    override fun onDisabled(context: Context) {
        context.getSystemService(AlarmManager::class.java).cancel(sceneIntent(context))
        super.onDisabled(context)
    }

    companion object {
        private const val ACTION_FEED = "com.example.fishpet.FEED"
        private const val ACTION_PET = "com.example.fishpet.PET"
        private const val ACTION_ADVANCE_SCENE = "com.example.fishpet.ADVANCE_SCENE"
        private const val FEED_REQUEST_CODE = 1001
        private const val PET_REQUEST_CODE = 1003
        private const val SCENE_REQUEST_CODE = 1002

        fun refresh(context: Context, latestFeedResult: FeedResult = FeedResult.NONE): FishState {
            val now = System.currentTimeMillis() / 1000L
            val state = FishStore.refresh(context, now)
            val manager = AppWidgetManager.getInstance(context)
            manager.getAppWidgetIds(ComponentName(context, FishWidget::class.java)).forEach { id ->
                manager.updateAppWidget(id, buildRemoteViews(context, state, now, latestFeedResult))
            }
            return state
        }

        fun feedAndRefresh(context: Context): FishUpdate {
            val update = FishStore.feed(context)
            if (update.result == FeedResult.FED) {
                FishSounds.play(context, FishSoundEffect.FEED)
                FishSounds.playDelayed(context, FishSoundEffect.EAT, 300L)
                scheduleSceneRefresh(context, update.state)
            }
            refresh(context, update.result)
            return update
        }

        fun petAndRefresh(context: Context): FishState {
            val state = FishStore.pet(context)
            if (!state.dead) {
                FishSounds.play(
                    context,
                    if (state.swimmingState == SwimmingState.STARVING || state.swimmingState == SwimmingState.CRITICAL) {
                        FishSoundEffect.STARVING
                    } else {
                        FishSoundEffect.HAPPY
                    },
                )
                scheduleSceneRefresh(context, state)
            }
            refresh(context)
            return state
        }

        private fun buildRemoteViews(context: Context, state: FishState, now: Long, result: FeedResult): RemoteViews =
            RemoteViews(context.packageName, R.layout.fish_widget).apply {
                setImageViewBitmap(R.id.aquarium, FishArt.render(state, now))
                setTextViewText(R.id.feed_feedback, feedbackText(state, now, result))
                setTextViewText(R.id.feed, feedLabel(state, now))
                setBoolean(R.id.feed, "setEnabled", !state.dead)
                setOnClickPendingIntent(R.id.feed, feedIntent(context))
                setOnClickPendingIntent(R.id.aquarium, petIntent(context))
            }

        private fun feedbackText(state: FishState, now: Long, result: FeedResult): String = when {
            result == FeedResult.FED || state.isFeeding(now) -> "${state.name} is eating!"
            result == FeedResult.DEAD || state.dead -> "This fish has passed on. Reset in the app."
            result == FeedResult.COOLDOWN -> "Tiny tummy is resting - try again soon."
            else -> "${state.name} - ${state.mood.label}"
        }

        private fun feedLabel(state: FishState, now: Long): String = when {
            state.dead -> "Resting"
            state.lastFedAtSeconds > 0L && now - state.lastFedAtSeconds < FishBalance().feedingCooldownSeconds -> {
                val minutes = max(1L, (FishBalance().feedingCooldownSeconds - (now - state.lastFedAtSeconds) + 59L) / 60L)
                "Feed - ${minutes}m"
            }
            else -> "Feed"
        }

        private fun feedIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
            context,
            FEED_REQUEST_CODE,
            Intent(context, FishWidget::class.java).setAction(ACTION_FEED).setData(Uri.parse("fishpet://feed")),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        private fun sceneIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
            context,
            SCENE_REQUEST_CODE,
            Intent(context, FishWidget::class.java).setAction(ACTION_ADVANCE_SCENE).setData(Uri.parse("fishpet://advance-scene")),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        private fun petIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
            context,
            PET_REQUEST_CODE,
            Intent(context, FishWidget::class.java).setAction(ACTION_PET).setData(Uri.parse("fishpet://pet")),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        private fun scheduleSceneRefresh(context: Context, state: FishState) {
            val nowSeconds = System.currentTimeMillis() / 1000L
            val triggerAtSeconds = FishSceneTiming.nextRefreshAtSeconds(state, nowSeconds) ?: return
            context.getSystemService(AlarmManager::class.java).set(
                AlarmManager.ELAPSED_REALTIME,
                SystemClock.elapsedRealtime() + (triggerAtSeconds - nowSeconds) * 1_000L,
                sceneIntent(context),
            )
        }
    }
}
