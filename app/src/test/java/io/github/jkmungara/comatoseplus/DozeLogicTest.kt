package io.github.jkmungara.comatoseplus

import io.github.jkmungara.comatoseplus.repositories.profiles.DozeDump
import io.github.jkmungara.comatoseplus.repositories.profiles.IdleMechanism
import io.github.jkmungara.comatoseplus.repositories.profiles.ProfileCatalog
import io.github.jkmungara.comatoseplus.repositories.profiles.ProfileId
import io.github.jkmungara.comatoseplus.repositories.profiles.ShellCommands
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DozeLogicTest {
    @Test
    fun parsesTimeUtilsDurations() {
        assertEquals(300_000L, DozeDump.parseDuration("+5m0s0ms"))
        assertEquals(30_000L, DozeDump.parseDuration("+30s0ms"))
        assertEquals(500L, DozeDump.parseDuration("+500ms"))
        assertEquals(3_600_000L, DozeDump.parseDuration("+1h0m0s0ms"))
        assertEquals(172_800_000L, DozeDump.parseDuration("+2d0h0m0s0ms"))
        assertEquals(0L, DozeDump.parseDuration("0"))
        assertNull(DozeDump.parseDuration("50.0m"))
        assertEquals(50.0, DozeDump.numericValue("50.0m")!!, 0.0)
    }

    @Test
    fun verifiesAgainstDump() {
        val expected = ProfileCatalog.parse(ProfileCatalog.constantsOf(ProfileId.LIGHT))
        val good = "  Settings:\n    light_after_inactive_to=+2m0s0ms\n    inactive_to=+10m0s0ms\n    location_accuracy=50.0m\n"
        assertEquals(DozeDump.Verification.Match, DozeDump.verify(expected, good))

        val stale = "  Settings:\n    light_after_inactive_to=+5m0s0ms\n    inactive_to=+10m0s0ms\n"
        val result = DozeDump.verify(expected, stale)
        assertTrue(result is DozeDump.Verification.Mismatch)

        assertEquals(DozeDump.Verification.NoData, DozeDump.verify(expected, "garbage"))
    }

    @Test
    fun picksMechanismPerAndroidVersion() {
        assertEquals(IdleMechanism.SETTINGS_ONLY, IdleMechanism.forSdk(30))
        assertEquals(IdleMechanism.DEVICE_CONFIG_ONLY, IdleMechanism.forSdk(31))
        assertEquals(IdleMechanism.DEVICE_CONFIG_ONLY, IdleMechanism.forSdk(33))
        assertEquals(IdleMechanism.SETTINGS_OVERRIDES_DEVICE_CONFIG, IdleMechanism.forSdk(34))
        assertEquals(IdleMechanism.SETTINGS_OVERRIDES_DEVICE_CONFIG, IdleMechanism.forSdk(36))
        assertFalse(IdleMechanism.DEVICE_CONFIG_ONLY.honorsSettings)
        assertTrue(IdleMechanism.SETTINGS_ONLY.honorsSettings)
    }

    @Test
    fun buildsShellCommands() {
        val commands = ShellCommands.deviceConfigCommands("light_idle_to=120000,location_accuracy=50")
        assertEquals(
            listOf(
                "device_config put device_idle light_idle_to 120000",
                "device_config put device_idle location_accuracy 50"
            ),
            commands
        )
        assertEquals(listOf("device_config reset trusted_defaults device_idle"), ShellCommands.deviceConfigCommands(null))
        assertEquals(
            "adb shell \"device_config put device_idle light_idle_to 120000; device_config put device_idle location_accuracy 50\"",
            ShellCommands.adbLine(commands)
        )
    }
}
