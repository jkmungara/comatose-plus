package io.github.jkmungara.comatoseplus.utils

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager

object BatteryUtils {
    data class Reading(val level: Int, val charging: Boolean)

    /** Current battery level in percent and whether a charger is plugged in, or null if unavailable */
    fun read(context: Context): Reading? {
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)) ?: return null
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        if (level < 0 || scale <= 0) return null
        val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0
        return Reading(level * 100 / scale, plugged)
    }
}
