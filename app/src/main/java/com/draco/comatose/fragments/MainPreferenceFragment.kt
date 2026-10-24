package com.draco.comatose.fragments

import android.content.Intent
import android.database.ContentObserver
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import com.draco.comatose.R
import com.draco.comatose.repositories.profiles.ProfileCatalog
import com.draco.comatose.repositories.profiles.ProfileId
import com.draco.comatose.utils.DeviceIdleSettings
import com.draco.comatose.utils.DeviceIdleSettings.ApplyResult
import com.draco.comatose.views.PermissionActivity
import com.google.android.material.snackbar.Snackbar

class MainPreferenceFragment : PreferenceFragmentCompat() {
    /**
     * Refresh the active-profile indicator whenever the stored value changes, so it also
     * reflects changes made outside this app (adb, the system, other apps)
     */
    private val settingsObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            refreshActiveProfile()
        }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.main, rootKey)
    }

    override fun onStart() {
        super.onStart()
        requireContext().contentResolver.registerContentObserver(
            DeviceIdleSettings.uri,
            false,
            settingsObserver
        )
        refreshActiveProfile()
    }

    override fun onStop() {
        requireContext().contentResolver.unregisterContentObserver(settingsObserver)
        super.onStop()
    }

    override fun onPreferenceTreeClick(preference: Preference): Boolean {
        val profile = ProfileId.values().firstOrNull { getString(keyRes(it)) == preference.key }
            ?: return super.onPreferenceTreeClick(preference)

        applyProfile(profile)
        return true
    }

    private fun applyProfile(profile: ProfileId) {
        val config = ProfileCatalog.constantsOf(profile)

        when (val result = DeviceIdleSettings.apply(requireContext(), config)) {
            ApplyResult.Success -> {
                /* Remember it so the boot receiver can restore it */
                PreferenceManager.getDefaultSharedPreferences(requireContext()).edit()
                    .putString(getString(R.string.pref_saved_constants_key), config)
                    .apply()
                showMessage(R.string.snackbar_applied)
            }

            ApplyResult.PermissionDenied -> {
                showMessage(R.string.snackbar_permission_lost)
                startActivity(Intent(requireContext(), PermissionActivity::class.java))
            }

            is ApplyResult.Mismatch -> showMessage(R.string.snackbar_not_applied)

            is ApplyResult.Failure -> showMessage(R.string.snackbar_failed)
        }

        refreshActiveProfile()
    }

    private fun refreshActiveProfile() {
        if (!isAdded) return

        val active = ProfileCatalog.identify(DeviceIdleSettings.read(requireContext()))

        ProfileId.values().forEach { profile ->
            findPreference<Preference>(getString(keyRes(profile)))?.setIcon(
                if (profile == active)
                    R.drawable.ic_baseline_check_circle_24
                else
                    R.drawable.ic_baseline_bedtime_24
            )
        }

        findPreference<Preference>(getString(R.string.pref_status_key))?.summary =
            if (active != null)
                getString(R.string.status_active, getString(titleRes(active)))
            else
                getString(R.string.status_custom)
    }

    private fun showMessage(messageRes: Int) {
        view?.let { Snackbar.make(it, messageRes, Snackbar.LENGTH_SHORT).show() }
    }

    private fun keyRes(profile: ProfileId): Int = when (profile) {
        ProfileId.DEFAULT -> R.string.pref_profile_key_default
        ProfileId.LIGHT -> R.string.pref_profile_key_light
        ProfileId.MODERATE -> R.string.pref_profile_key_moderate
        ProfileId.HIGH -> R.string.pref_profile_key_high
        ProfileId.EXTREME -> R.string.pref_profile_key_extreme
    }

    private fun titleRes(profile: ProfileId): Int = when (profile) {
        ProfileId.DEFAULT -> R.string.pref_profile_title_default
        ProfileId.LIGHT -> R.string.pref_profile_title_light
        ProfileId.MODERATE -> R.string.pref_profile_title_moderate
        ProfileId.HIGH -> R.string.pref_profile_title_high
        ProfileId.EXTREME -> R.string.pref_profile_title_extreme
    }
}
