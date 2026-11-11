package com.phoenixeu168.widget.work

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.util.concurrent.TimeUnit

/**
 * A 15-minute periodic worker is the safety net; while a widget exists a self-rescheduling chain redraws it
 * every 1, 5 or 15 minutes (cheap: no network unless the saved data is older than 25 minutes).
 */
object Scheduler {
    private const val PERIODIC = "phx_periodic"
    private const val NOW = "phx_now"
    private fun chainName(slot: Int) = "phx_chain_$slot"

    const val KEY_SLOT = "slot"
    const val KEY_FETCH = "fetch"

    fun start(context: Context) {
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC,
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<RefreshWorker>(15, TimeUnit.MINUTES).build(),
        )
    }

    fun cancelAll(context: Context) {
        val wm = WorkManager.getInstance(context)
        wm.cancelUniqueWork(PERIODIC)
        wm.cancelUniqueWork(chainName(0))
        wm.cancelUniqueWork(chainName(1))
    }

    /** Redraw now; [fetch] also downloads fresh data first. Restarts the redraw chain with the current interval. */
    fun refreshNow(context: Context, fetch: Boolean) {
        WorkManager.getInstance(context).enqueueUniqueWork(
            NOW,
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<RefreshWorker>()
                .setInputData(workDataOf(KEY_FETCH to fetch))
                .build(),
        )
    }

    /** Queue the next redraw. Two alternating names mean a running worker never cancels itself. */
    fun scheduleChain(context: Context, minutes: Int, currentSlot: Int) {
        val wm = WorkManager.getInstance(context)
        if (currentSlot < 0) {
            wm.cancelUniqueWork(chainName(0))
            wm.cancelUniqueWork(chainName(1))
        }
        val next = if (currentSlot == 0) 1 else 0
        wm.enqueueUniqueWork(
            chainName(next),
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<RefreshWorker>()
                .setInitialDelay(minutes.toLong(), TimeUnit.MINUTES)
                .setInputData(workDataOf(KEY_SLOT to next))
                .build(),
        )
    }
}
