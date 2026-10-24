package com.draco.comatose.utils

import java.util.concurrent.TimeUnit

/** Runs commands in a child process with a timeout. Blocking: call from a background thread. */
object ShellRunner {
    data class Result(val exitCode: Int, val output: String) {
        val success: Boolean get() = exitCode == 0
    }

    /** Returns null if the process could not be started at all (for example, no `su` binary) */
    fun run(command: List<String>, timeoutSeconds: Long = 15): Result? {
        return try {
            val process = ProcessBuilder(command).redirectErrorStream(true).start()

            /* Kill the process if it hangs, for example while waiting on a root prompt */
            val watchdog = Thread {
                try {
                    if (!process.waitFor(timeoutSeconds, TimeUnit.SECONDS))
                        process.destroyForcibly()
                } catch (_: InterruptedException) {
                }
            }.apply {
                isDaemon = true
                start()
            }

            val output = process.inputStream.bufferedReader().use { it.readText() }
            val exitCode = process.waitFor()
            watchdog.interrupt()
            Result(exitCode, output)
        } catch (_: Exception) {
            null
        }
    }

    fun runAsRoot(command: String, timeoutSeconds: Long = 30): Result? =
        run(listOf("su", "-c", command), timeoutSeconds)
}
