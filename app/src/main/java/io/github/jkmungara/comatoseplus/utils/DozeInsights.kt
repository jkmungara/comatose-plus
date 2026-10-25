package io.github.jkmungara.comatoseplus.utils

import android.content.Context
import io.github.jkmungara.comatoseplus.repositories.profiles.IdleHistory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Reads Doze information from `dumpsys deviceidle`. Needs the DUMP permission. */
object DozeInsights {
    /** Full `dumpsys deviceidle` output, or null without the DUMP permission or on failure */
    suspend fun readDump(context: Context): String? = withContext(Dispatchers.IO) {
        if (!DozeVerifier.hasDumpPermission(context)) return@withContext null
        ShellRunner.run(listOf("dumpsys", "deviceidle"))?.takeIf { it.success }?.output
    }

    /** Apps exempt from Doze, or null without the DUMP permission or on failure */
    suspend fun readExemptApps(context: Context): List<IdleHistory.ExemptApp>? = withContext(Dispatchers.IO) {
        if (!DozeVerifier.hasDumpPermission(context)) return@withContext null
        ShellRunner.run(listOf("dumpsys", "deviceidle", "whitelist"))
            ?.takeIf { it.success }
            ?.let { IdleHistory.parseExemptApps(it.output) }
    }
}
