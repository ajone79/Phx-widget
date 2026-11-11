package com.phoenixeu168.widget.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import com.phoenixeu168.widget.work.Scheduler

class PhxWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = PhxWidget()

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        Scheduler.start(context)
        Scheduler.refreshNow(context, fetch = true)
    }

    /** After a reboot or app update, make sure the background redraw is running again. */
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        Scheduler.start(context)
        Scheduler.refreshNow(context, fetch = false)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        Scheduler.cancelAll(context)
    }
}
