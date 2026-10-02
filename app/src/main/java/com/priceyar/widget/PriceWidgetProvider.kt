package com.priceyar.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.RemoteViews

class PriceWidgetProvider : AppWidgetProvider() {

    companion object {

        private const val DEFAULT_TEXT = "جستجوی کالا"

        fun updateWidget(context: Context) {

            val manager =
                AppWidgetManager.getInstance(context)

            val component =
                ComponentName(
                    context,
                    PriceWidgetProvider::class.java
                )

            val ids =
                manager.getAppWidgetIds(component)

            for (id in ids) {

                updateSingleWidget(
                    context,
                    manager,
                    id
                )
            }
        }

        fun showResult(
            context: Context,
            manager: AppWidgetManager,
            appWidgetId: Int,
            name: String,
            price: String
        ) {

            val views =
                createViews(
                    context,
                    appWidgetId
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

            manager.updateAppWidget(
                appWidgetId,
                views
            )
        }

        private fun updateSingleWidget(
            context: Context,
            manager: AppWidgetManager,
            appWidgetId: Int
        ) {

            val views =
                createViews(
                    context,
                    appWidgetId
                )

            views.setTextViewText(
                R.id.widgetSearchText,
                DEFAULT_TEXT
            )

            manager.updateAppWidget(
                appWidgetId,
                views
            )
        }

        private fun createViews(
            context: Context,
            appWidgetId: Int
        ): RemoteViews {

            val manager =
                AppWidgetManager.getInstance(context)

            val options =
                manager.getAppWidgetOptions(
                    appWidgetId
                )

            val minWidth =
                options.getInt(
                    AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,
                    250
                )

            val minHeight =
                options.getInt(
                    AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT,
                    48
                )

            val layout =
                if (
                    minHeight >= 90 ||
                    minWidth >= 400
                ) {
                    R.layout.price_widget_large
                } else {
                    R.layout.price_widget
                }

            val views =
                RemoteViews(
                    context.packageName,
                    layout
                )

            // جستجوی معمولی
            views.setOnClickPendingIntent(
                R.id.widgetSearchArea,
                createSearchPendingIntent(
                    context,
                    appWidgetId
                )
            )

            // افزودن کالا
            views.setOnClickPendingIntent(
                R.id.widgetAddButton,
                createMainPendingIntent(
                    context,
                    appWidgetId
                )
            )

            // جستجو همراه با توضیحات
            views.setOnClickPendingIntent(
                R.id.widgetHelpButton,
                createInfoPendingIntent(
                    context,
                    appWidgetId
                )
            )

            return views
        }

        private fun createSearchPendingIntent(
            context: Context,
            appWidgetId: Int
        ): PendingIntent {

            val intent =
                Intent(
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

        private fun createMainPendingIntent(
            context: Context,
            appWidgetId: Int
        ): PendingIntent {

            val intent =
                Intent(
                    context,
                    MainActivity::class.java
                )

            intent.flags =
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP

            return PendingIntent.getActivity(
                context,
                appWidgetId + 10000,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or
                    PendingIntent.FLAG_IMMUTABLE
            )
        }

        private fun createInfoPendingIntent(
            context: Context,
            appWidgetId: Int
        ): PendingIntent {

            val intent =
                Intent(
                    context,
                    SearchActivity::class.java
                )

            intent.putExtra(
                "showDescription",
                true
            )

            intent.flags =
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP

            return PendingIntent.getActivity(
                context,
                appWidgetId + 20000,
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

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {

        updateSingleWidget(
            context,
            appWidgetManager,
            appWidgetId
        )

        super.onAppWidgetOptionsChanged(
            context,
            appWidgetManager,
            appWidgetId,
            newOptions
        )
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
