package io.github.jkmungara.comatoseplus.utils

import android.content.Context
import androidx.preference.PreferenceManager
import io.github.jkmungara.comatoseplus.repositories.profiles.ProfileId
import io.github.jkmungara.comatoseplus.repositories.schedule.ScheduleMath
import java.time.LocalTime

/** Stored sleep schedule and auto-revert settings */
object ScheduleSettings {
    private const val K_ENABLED = "schedule_enabled"
    private const val K_START = "schedule_start_minutes"
    private const val K_END = "schedule_end_minutes"
    private const val K_SLEEP_PROFILE = "schedule_sleep_profile"
    private const val K_DAY_PROFILE = "schedule_day_profile"
    private const val K_REVERT_HOURS = "revert_hours"
    private const val K_REVERT_AT = "revert_at_ms"

    val revertChoicesHours = listOf(0, 2, 4, 8, 12)

    private fun prefs(context: Context) = PreferenceManager.getDefaultSharedPreferences(context)

    fun enabled(context: Context): Boolean = prefs(context).getBoolean(K_ENABLED, false)
    fun setEnabled(context: Context, value: Boolean) = prefs(context).edit().putBoolean(K_ENABLED, value).apply()

    fun start(context: Context): LocalTime = ScheduleMath.minutesToTime(prefs(context).getInt(K_START, 23 * 60))
    fun end(context: Context): LocalTime = ScheduleMath.minutesToTime(prefs(context).getInt(K_END, 7 * 60))
    fun setStart(context: Context, time: LocalTime) =
        prefs(context).edit().putInt(K_START, ScheduleMath.timeToMinutes(time)).apply()
    fun setEnd(context: Context, time: LocalTime) =
        prefs(context).edit().putInt(K_END, ScheduleMath.timeToMinutes(time)).apply()

    fun sleepProfile(context: Context): ProfileId =
        ProfileNames.fromName(prefs(context).getString(K_SLEEP_PROFILE, null), ProfileId.EXTREME)
    fun dayProfile(context: Context): ProfileId =
        ProfileNames.fromName(prefs(context).getString(K_DAY_PROFILE, null), ProfileId.LIGHT)
    fun setSleepProfile(context: Context, profile: ProfileId) =
        prefs(context).edit().putString(K_SLEEP_PROFILE, profile.name).apply()
    fun setDayProfile(context: Context, profile: ProfileId) =
        prefs(context).edit().putString(K_DAY_PROFILE, profile.name).apply()

    fun revertHours(context: Context): Int = prefs(context).getInt(K_REVERT_HOURS, 0)
    fun setRevertHours(context: Context, hours: Int) = prefs(context).edit().putInt(K_REVERT_HOURS, hours).apply()

    /** When a pending auto-revert should happen (epoch millis), or 0 if none */
    fun revertAt(context: Context): Long = prefs(context).getLong(K_REVERT_AT, 0L)
    fun setRevertAt(context: Context, at: Long) = prefs(context).edit().putLong(K_REVERT_AT, at).apply()
}
