package io.github.jkmungara.comatoseplus.repositories.schedule

import java.time.LocalDateTime
import java.time.LocalTime

/** Sleep window arithmetic. Pure Kotlin so it can be unit tested. */
object ScheduleMath {
    /** A window where start equals end is treated as "not configured" */
    fun isConfigured(start: LocalTime, end: LocalTime): Boolean = start != end

    /** True if [now] is inside the window. Windows may cross midnight, for example 23:00 to 07:00. */
    fun isInWindow(now: LocalTime, start: LocalTime, end: LocalTime): Boolean {
        if (start == end) return false
        return if (start < end)
            now >= start && now < end
        else
            now >= start || now < end
    }

    /** The next moment the window opens or closes after [now], or null if it is not configured */
    fun nextBoundary(now: LocalDateTime, start: LocalTime, end: LocalTime): LocalDateTime? {
        if (start == end) return null

        fun nextOccurrence(time: LocalTime): LocalDateTime {
            var candidate = now.toLocalDate().atTime(time)
            if (!candidate.isAfter(now)) candidate = candidate.plusDays(1)
            return candidate
        }

        val a = nextOccurrence(start)
        val b = nextOccurrence(end)
        return if (a.isBefore(b)) a else b
    }

    fun minutesToTime(minutes: Int): LocalTime = LocalTime.of((minutes / 60) % 24, minutes % 60)

    fun timeToMinutes(time: LocalTime): Int = time.hour * 60 + time.minute
}
