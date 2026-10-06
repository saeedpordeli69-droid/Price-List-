package com.priceyar.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.RemoteViews
import androidx.core.content.FileProvider
import java.io.File

class PriceWidgetProvider : AppWidgetProvider() {

    companion object {

        private const val DEFAULT_TEXT = "جستجوی کالا"

        private const val PREFS_NAME = "priceyar"
        private const val INFO_MODE_KEY = "widgetInfoMode"

        private const val ACTION_TOGGLE_INFO =
            "com.priceyar.widget.TOGGLE_INFO"

        private const val ACTION_SHARE_LIST =
            "com.priceyar.widget.SHARE_LIST"

        const val ACTION_ADD_PRODUCT =
            "com.priceyar.widget.ADD_PRODUCT"

        const val ACTION_CHOOSE_SHARE_MODE =
            "com.priceyar.widget.CHOOSE_SHARE_MODE"

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

            views.setOnClickPendingIntent(
                R.id.widgetSearchArea,
                createSearchPendingIntent(
                    context,
                    appWidgetId
                )
            )

            views.setOnClickPendingIntent(
                R.id.widgetAddButton,
                createAddProductPendingIntent(
                    context,
                    appWidgetId
                )
            )

            views.setOnClickPendingIntent(
                R.id.widgetHelpButton,
                createInfoTogglePendingIntent(
                    context,
                    appWidgetId
                )
            )

            views.setOnClickPendingIntent(
                R.id.widgetShareButton,
                createShareListPendingIntent(
                    context,
                    appWidgetId
                )
            )

            views.setOnClickPendingIntent(
                R.id.widgetLedgerButton,
                createLedgerPendingIntent(
                    context,
                    appWidgetId
                )
            )

            val infoEnabled =
                isInfoModeEnabled(context)

            if (infoEnabled) {

                views.setInt(
                    R.id.widgetHelpButton,
                    "setColorFilter",
                    android.graphics.Color.rgb(
                        100,
                        180,
                        255
                    )
                )

            } else {

                views.setInt(
                    R.id.widgetHelpButton,
                    "setColorFilter",
                    android.graphics.Color.rgb(
                        232,
                        234,
                        237
                    )
                )
            }

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
                    Intent.FLAG_ACTIVITY_CLEAR_TASK

            return PendingIntent.getActivity(
                context,
                appWidgetId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or
                    PendingIntent.FLAG_IMMUTABLE
            )
        }

        private fun createAddProductPendingIntent(
            context: Context,
            appWidgetId: Int
        ): PendingIntent {

            val intent =
                Intent(
                    context,
                    MainActivity::class.java
                )

            intent.action =
                ACTION_ADD_PRODUCT

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

        private fun createInfoTogglePendingIntent(
            context: Context,
            appWidgetId: Int
        ): PendingIntent {

            val intent =
                Intent(
                    context,
                    PriceWidgetProvider::class.java
                )

            intent.action =
                ACTION_TOGGLE_INFO

            intent.putExtra(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                appWidgetId
            )

            return PendingIntent.getBroadcast(
                context,
                appWidgetId + 20000,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or
                    PendingIntent.FLAG_IMMUTABLE
            )
        }

        private fun createShareListPendingIntent(
            context: Context,
            appWidgetId: Int
        ): PendingIntent {

            val intent =
                Intent(
                    context,
                    MainActivity::class.java
                )

            intent.action =
                ACTION_CHOOSE_SHARE_MODE

            intent.flags =
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP

            return PendingIntent.getActivity(
                context,
                appWidgetId + 30000,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or
                    PendingIntent.FLAG_IMMUTABLE
            )
        }

        private fun createLedgerPendingIntent(
            context: Context,
            appWidgetId: Int
        ): PendingIntent {

            val intent =
                Intent(
                    context,
                    MainActivity::class.java
                )

            intent.putExtra(
                "openLedger",
                true
            )

            intent.flags =
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP

            return PendingIntent.getActivity(
                context,
                appWidgetId + 40000,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or
                    PendingIntent.FLAG_IMMUTABLE
            )
        }

        private fun toggleInfoMode(
            context: Context
        ) {

            val prefs =
                context.getSharedPreferences(
                    PREFS_NAME,
                    Context.MODE_PRIVATE
                )

            val current =
                prefs.getBoolean(
                    INFO_MODE_KEY,
                    false
                )

            prefs.edit()
                .putBoolean(
                    INFO_MODE_KEY,
                    !current
                )
                .apply()

            updateWidget(context)
        }

        fun isInfoModeEnabled(
            context: Context
        ): Boolean {

            return context
                .getSharedPreferences(
                    PREFS_NAME,
                    Context.MODE_PRIVATE
                )
                .getBoolean(
                    INFO_MODE_KEY,
                    false
                )
        }

        private fun sharePriceList(
            context: Context
        ) {

            val prefs =
                context.getSharedPreferences(
                    PREFS_NAME,
                    Context.MODE_PRIVATE
                )

            val json =
                prefs.getString(
                    "data",
                    ""
                )

            if (json.isNullOrBlank()) {
                return
            }

            try {

                val sharedDirectory =
                    File(
                        context.cacheDir,
                        "shared"
                    )

                if (!sharedDirectory.exists()) {
                    sharedDirectory.mkdirs()
                }

                val file =
                    File(
                        sharedDirectory,
                        "PriceYar-PriceList.json"
                    )

                file.writeText(
                    json,
                    Charsets.UTF_8
                )

                val uri =
                    FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        file
                    )

                val sendIntent =
                    Intent(Intent.ACTION_SEND).apply {

                        type = "application/json"

                        putExtra(
                            Intent.EXTRA_STREAM,
                            uri
                        )

                        addFlags(
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )

                        clipData =
                            android.content.ClipData
                                .newRawUri(
                                    "PriceYar",
                                    uri
                                )
                    }

                val chooser =
                    Intent.createChooser(
                        sendIntent,
                        "ارسال لیست قیمت با"
                    )

                chooser.addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK
                )

                context.startActivity(
                    chooser
                )

            } catch (_: Exception) {
            }
        }
    }

    override fun onReceive(
        context: Context,
        intent: Intent
    ) {

        if (
            ACTION_TOGGLE_INFO ==
            intent.action
        ) {

            toggleInfoMode(context)
            return
        }

        if (
            ACTION_SHARE_LIST ==
            intent.action
        ) {

            sharePriceList(context)
            return
        }

        super.onReceive(
            context,
            intent
        )
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
