package com.draco.comatose.repositories.profiles

/**
 * Shell commands that set a profile through DeviceConfig. Run as root, or from a computer with ADB
 * (the shell user may write DeviceConfig; ordinary apps cannot).
 */
object ShellCommands {
    const val NAMESPACE = "device_idle"

    /** One `device_config put` per key, or a reset for the default profile (null/blank config) */
    fun deviceConfigCommands(config: String?): List<String> =
        if (config.isNullOrBlank())
            listOf("device_config reset trusted_defaults $NAMESPACE")
        else
            ProfileCatalog.parse(config).map { (key, value) -> "device_config put $NAMESPACE $key $value" }

    /** All commands in a single line that can be pasted into a terminal */
    fun adbLine(commands: List<String>): String =
        "adb shell \"" + commands.joinToString("; ") + "\""

    /** A single string to hand to `su -c` */
    fun rootLine(commands: List<String>): String = commands.joinToString("; ")
}
