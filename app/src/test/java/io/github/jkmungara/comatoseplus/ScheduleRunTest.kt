package io.github.jkmungara.comatoseplus

import io.github.jkmungara.comatoseplus.repositories.schedule.ScheduleRun
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ScheduleRunTest {
    @Test
    fun roundTrips() {
        val ok = ScheduleRun(1_700_000_000_000L, "EXTREME", true)
        assertEquals(ok, ScheduleRun.decode(ok.encode()))

        val failed = ScheduleRun(5L, "LIGHT", false)
        val decoded = ScheduleRun.decode(failed.encode())!!
        assertEquals("LIGHT", decoded.profile)
        assertFalse(decoded.ok)
    }

    @Test
    fun rejectsMalformedText() {
        assertNull(ScheduleRun.decode(null))
        assertNull(ScheduleRun.decode(""))
        assertNull(ScheduleRun.decode("not|enough"))
        assertNull(ScheduleRun.decode("abc|LIGHT|1"))
        assertTrue(ScheduleRun.decode("1|LIGHT|1")!!.ok)
    }
}
