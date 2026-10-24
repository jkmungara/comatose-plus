package com.draco.comatose.fragments

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.draco.comatose.R
import com.draco.comatose.repositories.profiles.DozeDump
import com.draco.comatose.repositories.profiles.ProfileCatalog
import com.draco.comatose.repositories.profiles.ProfileId
import com.draco.comatose.repositories.profiles.ShellCommands
import com.draco.comatose.utils.DozeVerifier
import com.draco.comatose.utils.ProfileApplier
import com.draco.comatose.utils.ProfileApplier.Outcome
import com.draco.comatose.views.PermissionActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class MainPreferenceFragment : PreferenceFragmentCompat() {
    private var busy = false
    private var statusJob: Job? = null

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.main, rootKey)
    }

    override fun onStart() {
        super.onStart()
        refreshStatus()
    }

    override fun onPreferenceTreeClick(preference: Preference): Boolean {
        if (preference.key == getString(R.string.pref_status_key)) {
            /* Only selectable while the DUMP permission is missing: copy the command that grants it */
            copyToClipboard(dumpGrantCommand())
            showMessage(getString(R.string.copied))
            return true
        }

        val profile = ProfileId.values().firstOrNull { getString(keyRes(it)) == preference.key }
            ?: return super.onPreferenceTreeClick(preference)

        applyProfile(profile)
        return true
    }

    private fun applyProfile(profile: ProfileId) {
        if (busy) return
        busy = true
        setStatus(getString(R.string.status_applying))

        val appContext = requireContext().applicationContext
        viewLifecycleOwner.lifecycleScope.launch {
            val outcome = ProfileApplier.apply(appContext, profile)
            busy = false
            if (!isAdded) return@launch

            when (outcome) {
                Outcome.Verified -> showMessage(getString(R.string.snackbar_verified))
                Outcome.AppliedUnverified -> showMessage(getString(R.string.snackbar_applied_unverified))
                Outcome.DefaultApplied -> showMessage(getString(R.string.snackbar_default_applied))
                is Outcome.NotInEffect -> showMessage(getString(R.string.snackbar_not_in_effect, outcome.details), long = true)
                is Outcome.NeedsShell -> showShellDialog(outcome)
                Outcome.PermissionDenied -> {
                    showMessage(getString(R.string.snackbar_permission_lost))
                    startActivity(Intent(requireContext(), PermissionActivity::class.java))
                }
                Outcome.Failed -> showMessage(getString(R.string.snackbar_failed))
            }

            refreshStatus()
        }
    }

    private fun showShellDialog(outcome: Outcome.NeedsShell) {
        val line = ShellCommands.adbLine(outcome.commands)
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.dialog_shell_title)
            .setMessage(getString(R.string.dialog_shell_message, outcome.sdk, line))
            .setPositiveButton(R.string.dialog_copy) { _, _ ->
                copyToClipboard(line)
                showMessage(getString(R.string.copied))
            }
            .setNegativeButton(R.string.dialog_close, null)
            .show()
    }

    /**
     * Show which profile was last applied. The stored setting cannot be read back by apps, so
     * confirm it against `dumpsys deviceidle` when the DUMP permission is granted.
     */
    private fun refreshStatus() {
        if (!isAdded) return
        statusJob?.cancel()

        val appContext = requireContext().applicationContext
        val applied = ProfileApplier.appliedProfile(appContext)
        val canVerify = DozeVerifier.hasDumpPermission(appContext)

        findPreference<Preference>(getString(R.string.pref_status_key))?.isSelectable = !canVerify

        if (applied == null) {
            markActive(null)
            setStatus(getString(R.string.status_none))
            return
        }

        markActive(applied)
        val title = getString(titleRes(applied))

        if (applied == ProfileId.DEFAULT) {
            setStatus(getString(R.string.status_active, title))
        } else if (!canVerify) {
            setStatus(getString(R.string.status_unverified, title))
        } else {
            setStatus(getString(R.string.status_checking, title))
            val expected = ProfileCatalog.parse(ProfileCatalog.constantsOf(applied))
            statusJob = viewLifecycleOwner.lifecycleScope.launch {
                when (DozeVerifier.verify(appContext, expected, attempts = 1)) {
                    DozeDump.Verification.Match -> setStatus(getString(R.string.status_verified, title))
                    DozeDump.Verification.NoData -> setStatus(getString(R.string.status_active, title))
                    is DozeDump.Verification.Mismatch -> {
                        markActive(null)
                        setStatus(getString(R.string.status_not_in_effect, title))
                    }
                }
            }
        }
    }

    private fun markActive(active: ProfileId?) {
        ProfileId.values().forEach { profile ->
            findPreference<Preference>(getString(keyRes(profile)))?.setIcon(
                if (profile == active)
                    R.drawable.ic_baseline_check_circle_24
                else
                    R.drawable.ic_baseline_bedtime_24
            )
        }
    }

    private fun setStatus(text: String) {
        if (!isAdded) return
        findPreference<Preference>(getString(R.string.pref_status_key))?.summary = text
    }

    private fun showMessage(message: String, long: Boolean = false) {
        view?.let {
            Snackbar.make(it, message, if (long) Snackbar.LENGTH_LONG else Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun copyToClipboard(text: String) {
        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Comatose", text))
    }

    private fun dumpGrantCommand(): String =
        "adb shell pm grant ${requireContext().packageName} android.permission.DUMP"

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
