package com.draco.comatose.repositories.profiles

/**
 * The profiles the app can apply. Pure Kotlin (no Android dependencies) so it can be unit tested.
 */
enum class ProfileId { DEFAULT, LIGHT, MODERATE, HIGH, EXTREME }

object ProfileCatalog {
    /**
     * The raw `device_idle_constants` string for a profile (null means "use system defaults")
     */
    fun constantsOf(id: ProfileId): String? = when (id) {
        ProfileId.DEFAULT -> DeviceIdleConstantsProfiles.DEFAULT
        ProfileId.LIGHT -> DeviceIdleConstantsProfiles.LIGHT
        ProfileId.MODERATE -> DeviceIdleConstantsProfiles.MODERATE
        ProfileId.HIGH -> DeviceIdleConstantsProfiles.HIGH
        ProfileId.EXTREME -> DeviceIdleConstantsProfiles.EXTREME
    }

    /**
     * Parse "key=value,key=value" into a map. Blank or null input yields an empty map.
     */
    fun parse(raw: String?): Map<String, String> {
        if (raw.isNullOrBlank()) return emptyMap()
        return raw.split(',')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .associate {
                val index = it.indexOf('=')
                if (index < 0) it to "" else it.substring(0, index).trim() to it.substring(index + 1).trim()
            }
    }

    /**
     * True if both strings describe the same set of constants, regardless of ordering or whitespace
     */
    fun sameConstants(a: String?, b: String?): Boolean = parse(a) == parse(b)

    /**
     * Which profile is currently active, or null if the value matches none of them
     * (for example, another app or the system changed it)
     */
    fun identify(currentValue: String?): ProfileId? =
        ProfileId.values().firstOrNull { sameConstants(constantsOf(it), currentValue) }
}
