package com.priceyar.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

class PriceWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
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
        const val ACTION_ADD_PRODUCT = "com.priceyar.widget.ACTION_ADD_PRODUCT"
        const val ACTION_CHOOSE_SHARE_MODE = "com.priceyar.widget.ACTION_CHOOSE_SHARE_MODE"

        fun updateWidget(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, PriceWidgetProvider::class.java))
            ids.forEach { update(context, manager, it) }
        }

        fun refresh(context: Context) = updateWidget(context)

        private fun update(context: Context, manager: AppWidgetManager, id: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_price)

            val searchIntent = Intent(context, SearchActivity::class.java)
            val searchPending = PendingIntent.getActivity(
                context, id + 10, searchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.search_button, searchPending)

            val addIntent = Intent(context, MainActivity::class.java).apply {
                action = ACTION_ADD_PRODUCT
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
            val addPending = PendingIntent.getActivity(
                context, id + 20, addIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.add_button, addPending)

            val ledgerIntent = Intent(context, MainActivity::class.java).apply {
                putExtra("openLedger", true)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
            val ledgerPending = PendingIntent.getActivity(
                context, id + 30, ledgerIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.ledger_button, ledgerPending)

            val shareIntent = Intent(context, MainActivity::class.java).apply {
                action = ACTION_CHOOSE_SHARE_MODE
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
            val sharePending = PendingIntent.getActivity(
                context, id + 40, shareIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.share_button, sharePending)

            manager.updateAppWidget(id, views)
        }
    }
}
