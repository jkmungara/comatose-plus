package com.draco.comatose.repositories.profiles

import kotlin.math.abs

/**
 * Reads the "Settings:" section of `dumpsys deviceidle`, which shows the values Doze is actually
 * using no matter whether they came from Settings.Global or DeviceConfig. Pure Kotlin.
 */
object DozeDump {
    sealed interface Verification {
        /** Every comparable key matches */
        data object Match : Verification

        /** At least one key differs from what we asked for */
        data class Mismatch(val details: List<String>) : Verification

        /** The dump had nothing we could compare (unexpected format, or no overlapping keys) */
        data object NoData : Verification
    }

    private val durationRegex = Regex("""^([+-])?(?:(\d+)d)?(?:(\d+)h)?(?:(\d+)m)?(?:(\d+)s)?(?:(\d+)ms)?$""")

    /**
     * Parse android.util.TimeUtils.formatDuration output such as "+5m0s0ms", "+30s0ms", "+1h0m0s0ms"
     * or "0" into milliseconds. Returns null if the text is not in that format.
     */
    fun parseDuration(text: String): Long? {
        val trimmed = text.trim()
        if (trimmed == "0") return 0L
        val match = durationRegex.matchEntire(trimmed) ?: return null
        val groups = match.groupValues.drop(2).take(5)
        if (groups.all { it.isEmpty() }) return null

        val (d, h, m, s, ms) = groups.map { it.toLongOrNull() ?: 0L }
        val total = (((d * 24 + h) * 60 + m) * 60 + s) * 1000L + ms
        return if (match.groupValues[1] == "-") -total else total
    }

    /** Numeric value of a dumped setting: a duration, a plain number, or a number with an "m" suffix */
    fun numericValue(raw: String): Double? =
        parseDuration(raw)?.toDouble()
            ?: raw.trim().toDoubleOrNull()
            ?: raw.trim().removeSuffix("m").toDoubleOrNull()

    /** key to raw value for the lines under "Settings:" */
    fun settingsSection(dump: String): Map<String, String> {
        val result = LinkedHashMap<String, String>()
        var inSection = false
        for (line in dump.lineSequence()) {
            if (!inSection) {
                if (line.trim() == "Settings:") inSection = true
                continue
            }
            val trimmed = line.trim()
            val index = trimmed.indexOf('=')
            if (!line.startsWith("    ") || index <= 0) break
            result[trimmed.substring(0, index)] = trimmed.substring(index + 1)
        }
        return result
    }

    /** Compare the keys we asked for against what the system reports it is using */
    fun verify(expected: Map<String, String>, dump: String): Verification {
        val actual = settingsSection(dump)
        if (actual.isEmpty()) return Verification.NoData

        var compared = 0
        val problems = mutableListOf<String>()
        for ((key, wantedText) in expected) {
            val raw = actual[key] ?: continue /* key does not exist on this Android version */
            val wanted = wantedText.toDoubleOrNull() ?: continue
            val actualValue = numericValue(raw) ?: continue
            compared++
            if (abs(wanted - actualValue) > 1e-6)
                problems += "$key: wanted $wantedText but the system uses $raw"
        }

        return when {
            compared == 0 -> Verification.NoData
            problems.isEmpty() -> Verification.Match
            else -> Verification.Mismatch(problems)
        }
    }
}
