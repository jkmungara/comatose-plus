package io.github.jkmungara.comatoseplus.utils

import android.content.Context
import android.content.pm.PackageManager
import io.github.jkmungara.comatoseplus.repositories.profiles.DozeDump
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * Checks what Doze is really using by reading `dumpsys deviceidle`. Needs the optional DUMP
 * permission, which can be granted over ADB or root just like WRITE_SECURE_SETTINGS.
 */
object DozeVerifier {
    fun hasDumpPermission(context: Context): Boolean =
        context.checkSelfPermission(android.Manifest.permission.DUMP) == PackageManager.PERMISSION_GRANTED

    private suspend fun readDump(): String? = withContext(Dispatchers.IO) {
        val result = ShellRunner.run(listOf("dumpsys", "deviceidle"))
        result?.takeIf { it.success }?.output
    }

    /**
     * Verify that the system is using [expected]. The system picks changes up asynchronously, so
     * retry a few times before reporting a mismatch.
     */
    suspend fun verify(
        context: Context,
        expected: Map<String, String>,
        attempts: Int = 4
    ): DozeDump.Verification {
        if (!hasDumpPermission(context)) return DozeDump.Verification.NoData

        var last: DozeDump.Verification = DozeDump.Verification.NoData
        repeat(attempts) { attempt ->
            val dump = readDump()
            if (dump != null) {
                last = DozeDump.verify(expected, dump)
                if (last is DozeDump.Verification.Match) return last
            }
            if (attempt < attempts - 1) delay(600)
        }
        return last
    }
}
