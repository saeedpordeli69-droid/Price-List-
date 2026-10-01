package com.priceyar.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

class PriceWidgetProvider : AppWidgetProvider() {

    companion object {

        private const val DEFAULT_TEXT = "جستجوی کالا"

        /**
         * به‌روزرسانی همه ویجت‌های PriceYar
         */
        fun updateWidget(context: Context) {

            val manager = AppWidgetManager.getInstance(context)

            val component = ComponentName(
                context,
                PriceWidgetProvider::class.java
            )

            val ids = manager.getAppWidgetIds(component)

            for (id in ids) {
                updateSingleWidget(
                    context,
                    manager,
                    id
                )
            }
        }

        /**
         * نمایش نتیجه جست‌وجو روی یک ویجت
         */
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

            val resultText =
                if (price.isNotBlank()) {
                    "$name  •  $price تومان"
                } else {
                    name
                }

            views.setTextViewText(
                R.id.widgetSearchText,
                resultText
            )

            views.setOnClickPendingIntent(
                R.id.widgetSearchArea,
                createSearchPendingIntent(
                    context,
                    appWidgetId
                )
            )

            manager.updateAppWidget(
                appWidgetId,
                views
            )
        }

        /**
         * به‌روزرسانی یک ویجت
         */
        private fun updateSingleWidget(
            context: Context,
            manager: AppWidgetManager,
            appWidgetId: Int
        ) {

            val views = RemoteViews(
                context.packageName,
                R.layout.price_widget
            )

            views.setTextViewText(
                R.id.widgetSearchText,
                DEFAULT_TEXT
            )

            views.setOnClickPendingIntent(
                R.id.widgetSearchArea,
                createSearchPendingIntent(
                    context,
                    appWidgetId
                )
            )

            manager.updateAppWidget(
                appWidgetId,
                views
            )
        }

        /**
         * ساخت Intent برای باز کردن صفحه جست‌وجو
         */
        private fun createSearchPendingIntent(
            context: Context,
            appWidgetId: Int
        ): PendingIntent {

            val intent = Intent(
                context,
                SearchActivity::class.java
            )

            intent.flags =
                Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP

            return PendingIntent.getActivity(
                context,
                appWidgetId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or
                    PendingIntent.FLAG_IMMUTABLE
            )
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {

        for (appWidgetId in appWidgetIds) {

            updateSingleWidget(
                context,
                appWidgetManager,
                appWidgetId
            )
        }
    }

    override fun onDeleted(
        context: Context,
        appWidgetIds: IntArray
    ) {
        super.onDeleted(
            context,
            appWidgetIds
        )
    }

    override fun onEnabled(
        context: Context
    ) {
        super.onEnabled(context)

        updateWidget(context)
    }

    override fun onDisabled(
        context: Context
    ) {
        super.onDisabled(context)
    }
}
