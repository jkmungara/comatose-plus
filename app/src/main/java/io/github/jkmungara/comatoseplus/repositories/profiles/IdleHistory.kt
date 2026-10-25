package io.github.jkmungara.comatoseplus.repositories.profiles

/**
 * Reads the "Idling history" section, the current state, and the exempt app list from
 * `dumpsys deviceidle` output. Pure Kotlin so it can be unit tested.
 */
object IdleHistory {
    enum class Kind { NORMAL, LIGHT_IDLE, LIGHT_MAINTENANCE, DEEP_IDLE, DEEP_MAINTENANCE, UNKNOWN }

    /** An event that happened [ageMs] milliseconds ago */
    data class Event(val ageMs: Long, val kind: Kind)

    data class Summary(
        val coveredMs: Long,
        val deepIdleMs: Long,
        val deepMaintenanceMs: Long,
        val lightIdleMs: Long,
        val deepIdleStretches: Int,
        val deepMaintenanceWindows: Int
    ) {
        val deepShare: Double get() = if (coveredMs <= 0) 0.0 else deepIdleMs.toDouble() / coveredMs
    }

    data class State(val deep: String, val light: String)

    enum class ExemptType { USER, SYSTEM, SYSTEM_EXCEPT_IDLE }

    data class ExemptApp(val type: ExemptType, val packageName: String, val appId: Int)

    private fun kindOf(label: String): Kind = when (label) {
        "normal" -> Kind.NORMAL
        "light-idle" -> Kind.LIGHT_IDLE
        "light-maint" -> Kind.LIGHT_MAINTENANCE
        "deep-idle" -> Kind.DEEP_IDLE
        "deep-maint" -> Kind.DEEP_MAINTENANCE
        else -> Kind.UNKNOWN
    }

    /** Events from oldest to newest */
    fun parseEvents(dump: String): List<Event> {
        val events = mutableListOf<Event>()
        var inSection = false
        for (line in dump.lineSequence()) {
            if (!inSection) {
                if (line.trim() == "Idling history:") inSection = true
                continue
            }
            if (!line.startsWith("    ")) break

            val trimmed = line.trim()
            val colon = trimmed.indexOf(':')
            if (colon <= 0) continue
            val label = trimmed.substring(0, colon).trim()
            val rest = trimmed.substring(colon + 1).trim()
            val timeText = rest.substringBefore(' ')
            val relative = DozeDump.parseDuration(timeText) ?: continue
            events += Event(ageMs = -relative, kind = kindOf(label))
        }
        return events.sortedByDescending { it.ageMs }
    }

    /**
     * How the last [windowMs] were spent. Each event's state lasts until the next event, and the
     * newest one lasts until now.
     */
    fun summarize(events: List<Event>, windowMs: Long): Summary {
        if (events.isEmpty()) return Summary(0, 0, 0, 0, 0, 0)

        var deep = 0L
        var maintenance = 0L
        var light = 0L
        var stretches = 0
        var windows = 0
        var covered = 0L

        for ((index, event) in events.withIndex()) {
            val startAge = event.ageMs
            val endAge = if (index + 1 < events.size) events[index + 1].ageMs else 0L
            val clippedStart = minOf(startAge, windowMs)
            val clippedEnd = minOf(endAge, windowMs)
            val length = (clippedStart - clippedEnd).coerceAtLeast(0)
            covered += length

            when (event.kind) {
                Kind.DEEP_IDLE -> {
                    deep += length
                    if (startAge <= windowMs) stretches++
                }
                Kind.DEEP_MAINTENANCE -> {
                    maintenance += length
                    if (startAge <= windowMs) windows++
                }
                Kind.LIGHT_IDLE, Kind.LIGHT_MAINTENANCE -> light += length
                else -> Unit
            }
        }
        return Summary(covered, deep, maintenance, light, stretches, windows)
    }

    /** The current mState and mLightState, for example IDLE and ACTIVE */
    fun parseState(dump: String): State? {
        val regex = Regex("""mState=(\S+)\s+mLightState=(\S+)""")
        val match = dump.lineSequence().firstNotNullOfOrNull { regex.find(it) } ?: return null
        return State(match.groupValues[1], match.groupValues[2])
    }

    /** Output of `dumpsys deviceidle whitelist`: lines like `user,com.example.app,10123` */
    fun parseExemptApps(output: String): List<ExemptApp> =
        output.lineSequence().mapNotNull { line ->
            val parts = line.trim().split(',')
            if (parts.size != 3) return@mapNotNull null
            val type = when (parts[0]) {
                "user" -> ExemptType.USER
                "system" -> ExemptType.SYSTEM
                "system-excidle" -> ExemptType.SYSTEM_EXCEPT_IDLE
                else -> return@mapNotNull null
            }
            val appId = parts[2].toIntOrNull() ?: return@mapNotNull null
            ExemptApp(type, parts[1], appId)
        }.toList()
}
