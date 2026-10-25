package io.github.jkmungara.comatoseplus.repositories.drain

/** One finished stretch of time spent on a single profile */
data class DrainRecord(
    val endMs: Long,
    val profile: String,
    val durationMs: Long,
    val startLevel: Int,
    val endLevel: Int
) {
    val hours: Double get() = durationMs / 3_600_000.0
    val percentLost: Int get() = startLevel - endLevel
    val ratePerHour: Double get() = if (hours <= 0.0) 0.0 else percentLost / hours
}

data class ProfileAverage(
    val profile: String,
    val sessions: Int,
    val totalHours: Double,
    val ratePerHour: Double
)

/** Battery drain bookkeeping. Pure Kotlin so it can be unit tested. */
object DrainMath {
    const val MIN_SESSION_MS = 60 * 60 * 1000L
    const val MAX_RECORDS = 60

    /**
     * A session is only meaningful if it lasted at least an hour, the phone was not charging at
     * either end, and the level actually went down.
     */
    fun qualifies(
        durationMs: Long,
        startLevel: Int,
        endLevel: Int,
        startCharging: Boolean,
        endCharging: Boolean
    ): Boolean =
        durationMs >= MIN_SESSION_MS &&
            startLevel in 0..100 && endLevel in 0..100 &&
            endLevel <= startLevel &&
            !startCharging && !endCharging

    fun encode(records: List<DrainRecord>): String =
        records.joinToString("\n") { "${it.endMs}|${it.profile}|${it.durationMs}|${it.startLevel}|${it.endLevel}" }

    fun decode(text: String?): List<DrainRecord> {
        if (text.isNullOrBlank()) return emptyList()
        return text.lines().mapNotNull { line ->
            val parts = line.split('|')
            if (parts.size != 5) return@mapNotNull null
            val end = parts[0].toLongOrNull() ?: return@mapNotNull null
            val duration = parts[2].toLongOrNull() ?: return@mapNotNull null
            val start = parts[3].toIntOrNull() ?: return@mapNotNull null
            val endLevel = parts[4].toIntOrNull() ?: return@mapNotNull null
            DrainRecord(end, parts[1], duration, start, endLevel)
        }
    }

    fun append(existing: List<DrainRecord>, record: DrainRecord): List<DrainRecord> =
        (existing + record).takeLast(MAX_RECORDS)

    /** Percent lost per hour for each profile, weighted by how long each session lasted */
    fun averages(records: List<DrainRecord>): List<ProfileAverage> =
        records.groupBy { it.profile }.map { (profile, list) ->
            val hours = list.sumOf { it.hours }
            val lost = list.sumOf { it.percentLost }
            ProfileAverage(profile, list.size, hours, if (hours <= 0.0) 0.0 else lost / hours)
        }.sortedBy { it.ratePerHour }
}
