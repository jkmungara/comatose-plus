package com.draco.comatose.repositories.profiles

/**
 * Where Android reads the Doze tunables from, per the AOSP DeviceIdleController source:
 *
 * - Android 11 and older (API 30-): only Settings.Global.device_idle_constants
 * - Android 12 to 13 (API 31-33):   only DeviceConfig namespace "device_idle"
 * - Android 14 and newer (API 34+): both, and the Settings.Global string overrides the
 *                                   matching individual DeviceConfig keys
 */
enum class IdleMechanism {
    SETTINGS_ONLY,
    DEVICE_CONFIG_ONLY,
    SETTINGS_OVERRIDES_DEVICE_CONFIG;

    /** True if writing Settings.Global.device_idle_constants has any effect on this version */
    val honorsSettings: Boolean get() = this != DEVICE_CONFIG_ONLY

    companion object {
        fun forSdk(sdk: Int): IdleMechanism = when {
            sdk <= 30 -> SETTINGS_ONLY
            sdk <= 33 -> DEVICE_CONFIG_ONLY
            else -> SETTINGS_OVERRIDES_DEVICE_CONFIG
        }
    }
}
