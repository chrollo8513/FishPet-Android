package com.example.fishpet

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

class FishWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) { refresh(context) }
    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context,intent)
        if (intent.action == FEED) { FishStore.update(context,feed=true); refresh(context) }
    }
    companion object {
        private const val FEED = "com.example.fishpet.FEED"
        fun refresh(ctx: Context) {
            val state = FishStore.update(ctx)
            val mgr = AppWidgetManager.getInstance(ctx)
            val component = ComponentName(ctx,FishWidget::class.java)
            val ids = mgr.getAppWidgetIds(component)
            for (id in ids) {
                val view = RemoteViews(ctx.packageName,R.layout.fish_widget)
                view.setImageViewBitmap(R.id.aquarium,FishArt.render(state))
                view.setTextViewText(R.id.status,"${state.stage} • ${state.mood}\nHunger ${state.hunger.toInt()}%  HP ${state.health.toInt()}%")
                val intent = Intent(ctx,FishWidget::class.java).setAction(FEED)
                val pending = PendingIntent.getBroadcast(ctx,0,intent,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                view.setOnClickPendingIntent(R.id.feed,pending)
                mgr.updateAppWidget(id,view)
            }
        }
    }
}
