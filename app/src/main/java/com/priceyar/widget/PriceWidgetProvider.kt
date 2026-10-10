package com.priceyar.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

class PriceWidgetProvider : AppWidgetProvider() {

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        if (intent.action == ACTION_TOGGLE_INFO) {
            val prefs = context.getSharedPreferences(
                "priceyar",
                Context.MODE_PRIVATE
            )

            val enabled = prefs.getBoolean("infoModeEnabled", false)

            prefs.edit()
                .putBoolean("infoModeEnabled", !enabled)
                .apply()

            updateWidget(context)
        }
    }

    override fun onUpdate(
        context: Context,
        manager: AppWidgetManager,
        ids: IntArray
    ) {
        ids.forEach { update(context, manager, it) }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        manager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: android.os.Bundle
    ) {
        update(context, manager, appWidgetId)
    }

    companion object {

        const val ACTION_ADD_PRODUCT =
            "com.priceyar.widget.ACTION_ADD_PRODUCT"

        const val ACTION_CHOOSE_SHARE_MODE =
            "com.priceyar.widget.ACTION_CHOOSE_SHARE_MODE"

        const val ACTION_TOGGLE_INFO =
            "com.priceyar.widget.ACTION_TOGGLE_INFO"

        fun isInfoModeEnabled(context: Context): Boolean {
            return context.getSharedPreferences(
                "priceyar",
                Context.MODE_PRIVATE
            ).getBoolean("infoModeEnabled", false)
        }

        fun updateWidget(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(
                ComponentName(context, PriceWidgetProvider::class.java)
            )

            ids.forEach { update(context, manager, it) }
        }

        fun refresh(context: Context) {
            updateWidget(context)
        }

        fun showResult(
            context: Context,
            manager: AppWidgetManager,
            id: Int,
            name: String,
            price: String
        ) {
            context.getSharedPreferences(
                "priceyar",
                Context.MODE_PRIVATE
            ).edit()
                .putString("widget_result_name", name)
                .putString("widget_result_price", price)
                .apply()

            update(context, manager, id)
        }

        private fun update(
            context: Context,
            manager: AppWidgetManager,
            id: Int
        ) {
            val options = manager.getAppWidgetOptions(id)

            val minWidth = options.getInt(
                AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0
            )
            val maxWidth = options.getInt(
                AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, minWidth
            )
            val minHeight = options.getInt(
                AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0
            )
            val maxHeight = options.getInt(
                AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, minHeight
            )

            val width = maxOf(minWidth, maxWidth)
            val height = maxOf(minHeight, maxHeight)

            val layout = if (width >= 280 && height >= 150) {
                R.layout.price_widget_large
            } else {
                R.layout.price_widget
            }

            val views = RemoteViews(context.packageName, layout)

            val prefs = context.getSharedPreferences(
                "priceyar",
                Context.MODE_PRIVATE
            )

            views.setTextViewText(
                R.id.widget_result_name,
                prefs.getString("widget_result_name", "PriceYar")
                    ?: "PriceYar"
            )

            val price = prefs.getString("widget_result_price", "") ?: ""

            views.setTextViewText(
                R.id.widget_result_price,
                if (price.isBlank()) "" else "قیمت فروش: $price تومان"
            )

            val searchIntent = Intent(
                context, SearchActivity::class.java
            ).apply {
                putExtra("opened_from_widget", true)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val searchPending = PendingIntent.getActivity(
                context, id + 10, searchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or
                    PendingIntent.FLAG_IMMUTABLE
            )

            views.setOnClickPendingIntent(
                R.id.search_button, searchPending
            )

            val addIntent = Intent(
                context, MainActivity::class.java
            ).apply {
                action = ACTION_ADD_PRODUCT
                addFlags(
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
                )
            }

            val addPending = PendingIntent.getActivity(
                context, id + 20, addIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or
                    PendingIntent.FLAG_IMMUTABLE
            )

            views.setOnClickPendingIntent(
                R.id.add_button, addPending
            )

            val ledgerIntent = Intent(
                context, MainActivity::class.java
            ).apply {
                putExtra("openLedger", true)
                addFlags(
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
                )
            }

            val ledgerPending = PendingIntent.getActivity(
                context, id + 30, ledgerIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or
                    PendingIntent.FLAG_IMMUTABLE
            )

            views.setOnClickPendingIntent(
                R.id.ledger_button, ledgerPending
            )

            val shareIntent = Intent(
                context, MainActivity::class.java
            ).apply {
                action = ACTION_CHOOSE_SHARE_MODE
                addFlags(
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
                )
            }

            val sharePending = PendingIntent.getActivity(
                context, id + 40, shareIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or
                    PendingIntent.FLAG_IMMUTABLE
            )

            views.setOnClickPendingIntent(
                R.id.share_button, sharePending
            )

            val infoIntent = Intent(
                context, PriceWidgetProvider::class.java
            ).apply {
                action = ACTION_TOGGLE_INFO
            }

            val infoPending = PendingIntent.getBroadcast(
                context, id + 50, infoIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or
                    PendingIntent.FLAG_IMMUTABLE
            )

            views.setOnClickPendingIntent(
                R.id.info_button, infoPending
            )

            manager.updateAppWidget(id, views)
        }
    }
}
