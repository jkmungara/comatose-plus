package io.github.jkmungara.comatoseplus.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import io.github.jkmungara.comatoseplus.utils.Scheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Handles the schedule and auto-revert alarms, and re-arms them after time or app changes */
class ScheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val appContext = context.applicationContext
        val pending = goAsync()

        CoroutineScope(Dispatchers.Default).launch {
            try {
                when (intent.action) {
                    Scheduler.ACTION_BOUNDARY -> {
                        Scheduler.applyDesired(appContext)
                        Scheduler.ensureScheduled(appContext)
                    }
                    Scheduler.ACTION_REVERT -> {
                        Scheduler.applyRevert(appContext)
                        Scheduler.ensureScheduled(appContext)
                    }
                    /* Clock or timezone changed, or the app was updated: alarms need re-arming */
                    else -> Scheduler.ensureScheduled(appContext)
                }
            } catch (e: Exception) {
                Log.w("ScheduleReceiver", "Failed handling ${intent.action}", e)
            } finally {
                pending.finish()
            }
        }
    }
}
