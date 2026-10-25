package io.github.jkmungara.comatoseplus.utils

import io.github.jkmungara.comatoseplus.R
import io.github.jkmungara.comatoseplus.repositories.profiles.ProfileId

object ProfileNames {
    fun titleRes(profile: ProfileId): Int = when (profile) {
        ProfileId.DEFAULT -> R.string.pref_profile_title_default
        ProfileId.LIGHT -> R.string.pref_profile_title_light
        ProfileId.MODERATE -> R.string.pref_profile_title_moderate
        ProfileId.HIGH -> R.string.pref_profile_title_high
        ProfileId.EXTREME -> R.string.pref_profile_title_extreme
    }

    fun fromName(name: String?, fallback: ProfileId): ProfileId =
        ProfileId.values().firstOrNull { it.name == name } ?: fallback
}
