package com.phoenixeu168.widget.work

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.updateAll
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.phoenixeu168.widget.data.Repo
import com.phoenixeu168.widget.data.Store
import com.phoenixeu168.widget.widget.PhxWidget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RefreshWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val ctx = applicationContext
        // No widget on the home screen any more: stop the chain instead of redrawing nothing.
        if (GlanceAppWidgetManager(ctx).getGlanceIds(PhxWidget::class.java).isEmpty()) {
            Scheduler.cancelAll(ctx)
            return@withContext Result.success()
        }
        val repo = Repo(ctx)
        if (inputData.getBoolean(Scheduler.KEY_FETCH, false) || repo.isStale()) repo.refresh()
        PhxWidget().updateAll(ctx)
        Scheduler.scheduleChain(ctx, Store(ctx).prefs().refreshMinutes, inputData.getInt(Scheduler.KEY_SLOT, -1))
        Result.success()
    }
}
