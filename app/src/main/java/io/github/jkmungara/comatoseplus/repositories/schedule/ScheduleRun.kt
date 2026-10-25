package io.github.jkmungara.comatoseplus.repositories.schedule

/** The outcome of the most recent automatic change (schedule or auto-revert) */
data class ScheduleRun(val atMillis: Long, val profile: String, val ok: Boolean) {
    fun encode(): String = "$atMillis|$profile|${if (ok) 1 else 0}"

    companion object {
        fun decode(text: String?): ScheduleRun? {
            val parts = text?.split('|') ?: return null
            if (parts.size != 3) return null
            val at = parts[0].toLongOrNull() ?: return null
            return ScheduleRun(at, parts[1], parts[2] == "1")
        }
    }
}
