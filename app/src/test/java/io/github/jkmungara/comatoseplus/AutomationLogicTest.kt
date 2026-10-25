package io.github.jkmungara.comatoseplus

import io.github.jkmungara.comatoseplus.repositories.drain.DrainMath
import io.github.jkmungara.comatoseplus.repositories.drain.DrainRecord
import io.github.jkmungara.comatoseplus.repositories.profiles.IdleHistory
import io.github.jkmungara.comatoseplus.repositories.schedule.ScheduleMath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.LocalTime

class AutomationLogicTest {
    private val start = LocalTime.of(23, 0)
    private val end = LocalTime.of(7, 0)

    @Test
    fun windowCrossingMidnight() {
        assertTrue(ScheduleMath.isInWindow(LocalTime.of(23, 0), start, end))
        assertTrue(ScheduleMath.isInWindow(LocalTime.of(2, 30), start, end))
        assertTrue(ScheduleMath.isInWindow(LocalTime.of(6, 59), start, end))
        assertFalse(ScheduleMath.isInWindow(LocalTime.of(7, 0), start, end))
        assertFalse(ScheduleMath.isInWindow(LocalTime.of(12, 0), start, end))
        assertFalse(ScheduleMath.isInWindow(LocalTime.of(22, 59), start, end))
    }

    @Test
    fun windowWithinOneDay() {
        val s = LocalTime.of(13, 0)
        val e = LocalTime.of(15, 0)
        assertTrue(ScheduleMath.isInWindow(LocalTime.of(14, 0), s, e))
        assertFalse(ScheduleMath.isInWindow(LocalTime.of(15, 0), s, e))
        assertFalse(ScheduleMath.isInWindow(LocalTime.of(2, 0), s, e))
    }

    @Test
    fun emptyWindowIsNotConfigured() {
        assertFalse(ScheduleMath.isConfigured(start, start))
        assertFalse(ScheduleMath.isInWindow(LocalTime.of(1, 0), start, start))
        assertNull(ScheduleMath.nextBoundary(LocalDateTime.of(2026, 10, 25, 12, 0), start, start))
    }

    @Test
    fun nextBoundary() {
        // noon: next change is the window opening tonight
        assertEquals(
            LocalDateTime.of(2026, 10, 25, 23, 0),
            ScheduleMath.nextBoundary(LocalDateTime.of(2026, 10, 25, 12, 0), start, end)
        )
        // 2 am: next change is the window closing at 7
        assertEquals(
            LocalDateTime.of(2026, 10, 25, 7, 0),
            ScheduleMath.nextBoundary(LocalDateTime.of(2026, 10, 25, 2, 0), start, end)
        )
        // exactly at a boundary: the next one, not the same instant
        assertEquals(
            LocalDateTime.of(2026, 10, 26, 7, 0),
            ScheduleMath.nextBoundary(LocalDateTime.of(2026, 10, 25, 23, 0), start, end)
        )
        // late evening after the window opened rolls to tomorrow morning
        assertEquals(
            LocalDateTime.of(2026, 10, 26, 7, 0),
            ScheduleMath.nextBoundary(LocalDateTime.of(2026, 10, 25, 23, 30), start, end)
        )
    }

    @Test
    fun minuteConversion() {
        assertEquals(LocalTime.of(23, 5), ScheduleMath.minutesToTime(23 * 60 + 5))
        assertEquals(23 * 60 + 5, ScheduleMath.timeToMinutes(LocalTime.of(23, 5)))
    }

    private val hour = 3_600_000L

    @Test
    fun drainQualification() {
        assertTrue(DrainMath.qualifies(8 * hour, 82, 80, false, false))
        assertFalse(DrainMath.qualifies(30 * 60_000L, 82, 80, false, false)) // too short
        assertFalse(DrainMath.qualifies(8 * hour, 50, 90, false, false)) // charged
        assertFalse(DrainMath.qualifies(8 * hour, 82, 80, true, false)) // charging at start
        assertFalse(DrainMath.qualifies(8 * hour, 82, 80, false, true)) // charging at end
        assertFalse(DrainMath.qualifies(8 * hour, -1, 80, false, false)) // no reading
    }

    @Test
    fun drainRecordsRoundTripAndRate() {
        val records = listOf(
            DrainRecord(1000L, "EXTREME", 8 * hour, 82, 80),
            DrainRecord(2000L, "DEFAULT", 8 * hour, 90, 82)
        )
        assertEquals(records, DrainMath.decode(DrainMath.encode(records)))
        assertEquals(0.25, records[0].ratePerHour, 1e-9)
        assertEquals(1.0, records[1].ratePerHour, 1e-9)
        assertTrue(DrainMath.decode("garbage\n1|x|2").isEmpty())
        assertTrue(DrainMath.decode(null).isEmpty())
    }

    @Test
    fun drainAveragesAreWeightedAndSorted() {
        val records = listOf(
            DrainRecord(1L, "EXTREME", 8 * hour, 82, 80),
            DrainRecord(2L, "EXTREME", 8 * hour, 80, 76),
            DrainRecord(3L, "DEFAULT", 8 * hour, 90, 82)
        )
        val averages = DrainMath.averages(records)
        assertEquals("EXTREME", averages[0].profile) // lowest drain first
        assertEquals(2, averages[0].sessions)
        assertEquals(6.0 / 16.0, averages[0].ratePerHour, 1e-9)
        assertEquals(1.0, averages[1].ratePerHour, 1e-9)
    }

    @Test
    fun drainLogIsCapped() {
        var list = emptyList<DrainRecord>()
        repeat(DrainMath.MAX_RECORDS + 10) { list = DrainMath.append(list, DrainRecord(it.toLong(), "X", hour, 50, 49)) }
        assertEquals(DrainMath.MAX_RECORDS, list.size)
        assertEquals(10L, list.first().endMs)
    }

    private val sampleDump = listOf(
        "  Flags:",
        "    some_flag=true",
        "",
        "  Settings:",
        "    light_after_inactive_to=+2m0s0ms",
        "",
        "  Idling history:",
        "         normal: -9h0m0s0ms (screen)",
        "     light-idle: -8h30m0s0ms",
        "      deep-idle: -8h0m0s0ms",
        "     deep-maint: -5h0m0s0ms",
        "      deep-idle: -4h45m0s0ms",
        "     deep-maint: -1h0m0s0ms",
        "         normal: -30m0s0ms (screen)",
        "  Whitelist system apps:",
        "    com.example",
        "  mState=IDLE mLightState=ACTIVE"
    ).joinToString("\n")

    @Test
    fun parsesIdlingHistory() {
        val events = IdleHistory.parseEvents(sampleDump)
        assertEquals(7, events.size)
        assertEquals(IdleHistory.Kind.NORMAL, events.first().kind)
        assertEquals(9 * hour, events.first().ageMs)
        assertEquals(IdleHistory.Kind.NORMAL, events.last().kind)
        assertEquals(30 * 60_000L, events.last().ageMs)
        assertEquals(IdleHistory.Kind.DEEP_MAINTENANCE, events[3].kind)
    }

    @Test
    fun summarizesIdleTime() {
        val events = IdleHistory.parseEvents(sampleDump)
        val summary = IdleHistory.summarize(events, 12 * hour)
        // deep idle: 8h to 5h ago (3h) plus 4h45m to 1h ago (3h45m)
        assertEquals(3 * hour + 3 * hour + 45 * 60_000L, summary.deepIdleMs)
        assertEquals(2, summary.deepIdleStretches)
        assertEquals(2, summary.deepMaintenanceWindows)
        assertEquals(30 * 60_000L, summary.lightIdleMs)
        assertEquals(9 * hour, summary.coveredMs)
        assertTrue(summary.deepShare > 0.7)
    }

    @Test
    fun summaryClipsToWindow() {
        val events = IdleHistory.parseEvents(sampleDump)
        val summary = IdleHistory.summarize(events, 6 * hour)
        // only the last 6 hours: deep idle from 6h to 5h ago (1h) plus 3h45m
        assertEquals(1 * hour + 3 * hour + 45 * 60_000L, summary.deepIdleMs)
        assertEquals(6 * hour, summary.coveredMs)
        assertEquals(1, summary.deepIdleStretches) // the first stretch began before the window
    }

    @Test
    fun parsesStateAndExemptApps() {
        val state = IdleHistory.parseState(sampleDump)
        assertNotNull(state)
        assertEquals("IDLE", state!!.deep)
        assertEquals("ACTIVE", state.light)
        assertNull(IdleHistory.parseState("nothing here"))

        val apps = IdleHistory.parseExemptApps(
            "system-excidle,com.a,1000\nsystem,com.b,1001\nuser,com.c,10123\nbad line\nuser,com.d,x\n"
        )
        assertEquals(3, apps.size)
        assertEquals(IdleHistory.ExemptType.USER, apps[2].type)
        assertEquals("com.c", apps[2].packageName)
        assertEquals(10123, apps[2].appId)
        assertTrue(IdleHistory.parseEvents("garbage").isEmpty())
        assertEquals(0L, IdleHistory.summarize(emptyList(), hour).coveredMs)
    }
}
