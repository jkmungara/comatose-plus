package io.github.jkmungara.comatoseplus.utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import io.github.jkmungara.comatoseplus.receivers.ScheduleReceiver
import io.github.jkmungara.comatoseplus.repositories.profiles.ProfileId
import io.github.jkmungara.comatoseplus.repositories.schedule.ScheduleMath
import io.github.jkmungara.comatoseplus.repositories.schedule.ScheduleRun
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/**
 * Sleep schedule and auto-revert. Two alarms are used: one for the next time the sleep window
 * opens or closes, and one for a pending auto-revert.
 */
object Scheduler {
    const val ACTION_BOUNDARY = "io.github.jkmungara.comatoseplus.action.SCHEDULE_BOUNDARY"
    const val ACTION_REVERT = "io.github.jkmungara.comatoseplus.action.REVERT"
    private const val REQUEST_BOUNDARY = 1
    private const val REQUEST_REVERT = 2

    /** The profile the schedule wants right now, or null if the schedule is off or not configured */
    fun desiredProfile(context: Context, now: LocalTime = LocalTime.now()): ProfileId? {
        if (!ScheduleSettings.enabled(context)) return null
        val start = ScheduleSettings.start(context)
        val end = ScheduleSettings.end(context)
        if (!ScheduleMath.isConfigured(start, end)) return null
        return if (ScheduleMath.isInWindow(now, start, end))
            ScheduleSettings.sleepProfile(context)
        else
            ScheduleSettings.dayProfile(context)
    }

    suspend fun applyDesired(context: Context): ProfileApplier.Outcome? {
        val profile = desiredProfile(context) ?: return null
        return applyAutomatically(context, profile)
    }

    private suspend fun applyAutomatically(context: Context, profile: ProfileId): ProfileApplier.Outcome {
        val outcome = ProfileApplier.apply(context, profile, manual = false)
        val ok = outcome is ProfileApplier.Outcome.Verified ||
            outcome is ProfileApplier.Outcome.AppliedUnverified ||
            outcome is ProfileApplier.Outcome.DefaultApplied
        ScheduleSettings.setLastRun(context, ScheduleRun(System.currentTimeMillis(), profile.name, ok))
        return outcome
    }

    /** Auto-revert fired: go back to what the schedule wants, or to the default profile */
    suspend fun applyRevert(context: Context) {
        ScheduleSettings.setRevertAt(context, 0L)
        val target = desiredProfile(context) ?: ProfileId.DEFAULT
        applyAutomatically(context, target)
    }

    /** Called whenever a profile has been applied */
    fun onProfileApplied(context: Context, profile: ProfileId, manual: Boolean) {
        val hours = ScheduleSettings.revertHours(context)
        if (manual && profile != ProfileId.DEFAULT && hours > 0) {
            val at = System.currentTimeMillis() + hours * 3_600_000L
            ScheduleSettings.setRevertAt(context, at)
            setAlarm(context, REQUEST_REVERT, ACTION_REVERT, at)
        } else {
            ScheduleSettings.setRevertAt(context, 0L)
            cancel(context, REQUEST_REVERT, ACTION_REVERT)
        }
    }

    /** Make the alarms match the stored settings. Safe to call any time. */
    fun ensureScheduled(context: Context) {
        val next = nextBoundaryMillis(context)
        if (next != null)
            setAlarm(context, REQUEST_BOUNDARY, ACTION_BOUNDARY, next)
        else
            cancel(context, REQUEST_BOUNDARY, ACTION_BOUNDARY)

        val revertAt = ScheduleSettings.revertAt(context)
        if (revertAt > 0L)
            setAlarm(context, REQUEST_REVERT, ACTION_REVERT, revertAt)
        else
            cancel(context, REQUEST_REVERT, ACTION_REVERT)
    }

    /** The next change and the profile that will be applied then, or null if the schedule is off */
    fun nextChange(context: Context): Pair<ProfileId, LocalDateTime>? {
        if (!ScheduleSettings.enabled(context)) return null
        val start = ScheduleSettings.start(context)
        val end = ScheduleSettings.end(context)
        val now = LocalDateTime.now()
        val boundary = ScheduleMath.nextBoundary(now, start, end) ?: return null
        val inWindow = ScheduleMath.isInWindow(now.toLocalTime(), start, end)
        val profile = if (inWindow) ScheduleSettings.dayProfile(context) else ScheduleSettings.sleepProfile(context)
        return profile to boundary
    }

    private fun nextBoundaryMillis(context: Context): Long? {
        if (!ScheduleSettings.enabled(context)) return null
        val boundary = ScheduleMath.nextBoundary(
            LocalDateTime.now(), ScheduleSettings.start(context), ScheduleSettings.end(context)
        ) ?: return null
        return boundary.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    private fun pendingIntent(context: Context, request: Int, action: String): PendingIntent {
        val intent = Intent(context, ScheduleReceiver::class.java).setAction(action)
        return PendingIntent.getBroadcast(
            context, request, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun setAlarm(context: Context, request: Int, action: String, atMillis: Long) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val pending = pendingIntent(context, request, action)
        val exactAllowed = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()
        try {
            if (exactAllowed)
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, pending)
            else
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, pending)
        } catch (_: SecurityException) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, pending)
        }
    }

    private fun cancel(context: Context, request: Int, action: String) {
        context.getSystemService(AlarmManager::class.java).cancel(pendingIntent(context, request, action))
    }
}
