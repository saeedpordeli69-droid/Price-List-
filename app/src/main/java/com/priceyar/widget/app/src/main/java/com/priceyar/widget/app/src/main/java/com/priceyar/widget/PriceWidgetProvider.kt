package com.priceyar.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import org.json.JSONObject

class PriceWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        manager: AppWidgetManager,
        ids: IntArray
    ) {
        for (id in ids) {
            updateWidget(context, manager, id)
        }
    }

    companion object {

        fun updateWidget(
            context: Context,
            manager: AppWidgetManager,
            appWidgetId: Int
        ) {

            val views = RemoteViews(
                context.packageName,
                R.layout.price_widget
            )

            val prefs =
                context.getSharedPreferences(
                    "priceyar",
                    Context.MODE_PRIVATE
                )

            val json =
                prefs.getString("data", "") ?: ""

            var text = "جستجوی کالا"

            if (json.isNotEmpty()) {
                try {
                    val data = JSONObject(json)

                    val active =
                        data.optInt("active", 0)

                    val stores =
                        data.optJSONArray("stores")

                    if (stores != null &&
                        active >= 0 &&
                        active < stores.length()
                    ) {
                        val store =
                            stores.getJSONObject(active)

                        text =
                            store.optString(
                                "name",
                                "جستجوی کالا"
                            )
                    }

                } catch (_: Exception) {
                }
            }

            views.setTextViewText(
                R.id.widgetSearchText,
                text
            )

            val intent =
                Intent(
                    context,
                    SearchActivity::class.java
                )

            val pending =
                PendingIntent.getActivity(
                    context,
                    appWidgetId,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or
                    PendingIntent.FLAG_IMMUTABLE
                )

            views.setOnClickPendingIntent(
                R.id.widgetSearchArea,
                pending
            )

            manager.updateAppWidget(
                appWidgetId,
                views
            )
        }

        fun showResult(
            context: Context,
            manager: AppWidgetManager,
            appWidgetId: Int,
            name: String,
            price: String
        ) {

            val views = RemoteViews(
                context.packageName,
                R.layout.price_widget
            )

            views.setTextViewText(
                R.id.widgetSearchText,
                "$name  •  $price"
            )

            val intent =
                Intent(
                    context,
                    SearchActivity::class.java
                )

            val pending =
                PendingIntent.getActivity(
                    context,
                    appWidgetId,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or
                    PendingIntent.FLAG_IMMUTABLE
                )

            views.setOnClickPendingIntent(
                R.id.widgetSearchArea,
                pending
            )

            manager.updateAppWidget(
                appWidgetId,
                views
            )
        }
    }
}
