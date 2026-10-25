package io.github.jkmungara.comatoseplus.utils

import android.content.Context
import androidx.preference.PreferenceManager
import io.github.jkmungara.comatoseplus.repositories.drain.DrainMath
import io.github.jkmungara.comatoseplus.repositories.drain.DrainRecord
import io.github.jkmungara.comatoseplus.repositories.profiles.ProfileId

/**
 * Remembers how much battery was lost while each profile was active. A "session" starts when the
 * profile changes and ends when it changes again.
 */
object DrainTracker {
    private const val K_PROFILE = "drain_profile"
    private const val K_START_MS = "drain_start_ms"
    private const val K_START_LEVEL = "drain_start_level"
    private const val K_START_CHARGING = "drain_start_charging"
    private const val K_RECORDS = "drain_records"

    data class Current(val profile: String, val startMs: Long, val startLevel: Int, val nowLevel: Int?)

    fun onProfileChanged(context: Context, profile: ProfileId) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val previous = prefs.getString(K_PROFILE, null)

        /* Applying the same profile again must not cut a long session short */
        if (previous == profile.name) return

        val battery = BatteryUtils.read(context)
        val now = System.currentTimeMillis()
        val editor = prefs.edit()

        val previousStart = prefs.getLong(K_START_MS, 0L)
        if (previous != null && previousStart > 0L && battery != null) {
            val duration = now - previousStart
            val startLevel = prefs.getInt(K_START_LEVEL, -1)
            if (DrainMath.qualifies(
                    duration, startLevel, battery.level,
                    prefs.getBoolean(K_START_CHARGING, false), battery.charging
                )
            ) {
                val records = DrainMath.decode(prefs.getString(K_RECORDS, null))
                val record = DrainRecord(now, previous, duration, startLevel, battery.level)
                editor.putString(K_RECORDS, DrainMath.encode(DrainMath.append(records, record)))
            }
        }

        if (battery != null) {
            editor.putString(K_PROFILE, profile.name)
                .putLong(K_START_MS, now)
                .putInt(K_START_LEVEL, battery.level)
                .putBoolean(K_START_CHARGING, battery.charging)
        }
        editor.apply()
    }

    fun records(context: Context): List<DrainRecord> =
        DrainMath.decode(PreferenceManager.getDefaultSharedPreferences(context).getString(K_RECORDS, null))

    fun current(context: Context): Current? {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val profile = prefs.getString(K_PROFILE, null) ?: return null
        val start = prefs.getLong(K_START_MS, 0L)
        if (start <= 0L) return null
        return Current(profile, start, prefs.getInt(K_START_LEVEL, -1), BatteryUtils.read(context)?.level)
    }
}
