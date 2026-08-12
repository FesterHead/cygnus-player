package com.festerhead.cygnusplayer.ui.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

/**
 * Receiver for Cygnus Player Glance widget updates and lifecycle events.
 */
class CygnusWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = CygnusWidget()

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        requestWidgetStateUpdate(context)
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        requestWidgetStateUpdate(context)
    }

    companion object {
        /** Intent action broadcast to request active playback service widget refresh. */
        const val ACTION_REQUEST_WIDGET_UPDATE = "com.festerhead.cygnusplayer.REQUEST_WIDGET_UPDATE"

        /**
         * Broadcasts a request to update widget playback state if the service is active.
         *
         * @param context Application or receiver context.
         */
        fun requestWidgetStateUpdate(context: Context) {
            val intent = Intent(ACTION_REQUEST_WIDGET_UPDATE).apply {
                setPackage(context.packageName)
            }
            context.sendBroadcast(intent)
        }
    }
}

