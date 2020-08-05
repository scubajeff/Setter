package site.leos.setter

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

class SearchWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        // There may be multiple widgets active, so update all of them
        appWidgetIds.forEach {
            val intent: PendingIntent = Intent(context, DirectSearchActivity::class.java).let { i ->
                PendingIntent.getActivity(context, 0, i, 0)
            }
            val views = RemoteViews(context.packageName, R.layout.search_widget).apply {
                setOnClickPendingIntent(R.id.searchwidget_text, intent)
                setOnClickPendingIntent(R.id.searchwidget_logo, intent)
            }

            // Tell the AppWidgetManager to perform an update on the current app widget
            appWidgetManager.updateAppWidget(it, views)
        }
    }
}