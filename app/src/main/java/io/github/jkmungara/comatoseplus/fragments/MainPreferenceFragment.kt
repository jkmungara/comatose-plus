package io.github.jkmungara.comatoseplus.fragments

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.format.DateUtils
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import io.github.jkmungara.comatoseplus.R
import io.github.jkmungara.comatoseplus.repositories.profiles.DozeDump
import io.github.jkmungara.comatoseplus.repositories.profiles.ProfileCatalog
import io.github.jkmungara.comatoseplus.repositories.profiles.ProfileId
import io.github.jkmungara.comatoseplus.repositories.profiles.ShellCommands
import io.github.jkmungara.comatoseplus.repositories.drain.DrainMath
import io.github.jkmungara.comatoseplus.repositories.profiles.IdleHistory
import io.github.jkmungara.comatoseplus.utils.DozeInsights
import io.github.jkmungara.comatoseplus.utils.DozeVerifier
import io.github.jkmungara.comatoseplus.utils.DrainTracker
import io.github.jkmungara.comatoseplus.utils.ProfileNames
import io.github.jkmungara.comatoseplus.utils.ScheduleSettings
import io.github.jkmungara.comatoseplus.views.ScheduleActivity
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import io.github.jkmungara.comatoseplus.utils.ProfileApplier
import io.github.jkmungara.comatoseplus.utils.ProfileApplier.Outcome
import io.github.jkmungara.comatoseplus.views.PermissionActivity
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
        refreshScheduleSummary()
    }

    override fun onPreferenceTreeClick(preference: Preference): Boolean {
        if (preference.key == getString(R.string.pref_status_key)) {
            /* Only selectable while the DUMP permission is missing: copy the command that grants it */
            copyToClipboard(dumpGrantCommand())
            showMessage(getString(R.string.copied))
            return true
        }

        when (preference.key) {
            getString(R.string.pref_schedule_key) -> {
                startActivity(Intent(requireContext(), ScheduleActivity::class.java))
                return true
            }
            getString(R.string.pref_history_key) -> {
                showHistory()
                return true
            }
            getString(R.string.pref_check_key) -> {
                showDozeCheck()
                return true
            }
            getString(R.string.pref_exempt_key) -> {
                showExemptApps()
                return true
            }
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


    private fun refreshScheduleSummary() {
        val ctx = requireContext()
        val time = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
        findPreference<Preference>(getString(R.string.pref_schedule_key))?.summary =
            if (!ScheduleSettings.enabled(ctx))
                getString(R.string.schedule_summary_off)
            else
                getString(
                    R.string.schedule_summary_on,
                    getString(ProfileNames.titleRes(ScheduleSettings.sleepProfile(ctx))),
                    ScheduleSettings.start(ctx).format(time),
                    ScheduleSettings.end(ctx).format(time),
                    getString(ProfileNames.titleRes(ScheduleSettings.dayProfile(ctx)))
                )
    }

    private fun profileTitle(name: String): String {
        val profile = ProfileNames.fromName(name, ProfileId.DEFAULT)
        return getString(ProfileNames.titleRes(profile))
    }

    private fun formatHours(hours: Double): String = String.format("%.1f", hours)

    private fun formatPercent(value: Double): String = String.format("%.2f", value)

    private fun formatSpan(ms: Long): String {
        val minutes = ms / 60_000
        return if (minutes >= 60)
            getString(R.string.duration_hours_minutes, minutes / 60, minutes % 60)
        else
            getString(R.string.duration_minutes, minutes)
    }

    private fun showTextDialog(titleRes: Int, text: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(titleRes)
            .setMessage(text)
            .setPositiveButton(R.string.dialog_close, null)
            .show()
    }

    private fun showDumpNeeded(titleRes: Int) {
        val command = dumpGrantCommand()
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(titleRes)
            .setMessage(getString(R.string.doze_need_dump, command))
            .setPositiveButton(R.string.dialog_copy) { _, _ ->
                copyToClipboard(command)
                showMessage(getString(R.string.copied))
            }
            .setNegativeButton(R.string.dialog_close, null)
            .show()
    }

    private fun showHistory() {
        val ctx = requireContext()
        val records = DrainTracker.records(ctx)
        val current = DrainTracker.current(ctx)
        val builder = StringBuilder()

        if (current != null) {
            val since = DateUtils.formatDateTime(
                ctx, current.startMs, DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_ABBREV_ALL
            )
            builder.append(
                getString(
                    R.string.history_current, profileTitle(current.profile), since,
                    current.startLevel, current.nowLevel?.toString() ?: "?"
                )
            )
        }

        if (records.isEmpty()) {
            builder.append(getString(R.string.history_empty))
        } else {
            builder.append(getString(R.string.history_averages_title))
            for (average in DrainMath.averages(records)) {
                builder.append(
                    getString(
                        R.string.history_average_line, profileTitle(average.profile),
                        formatPercent(average.ratePerHour), formatHours(average.totalHours), average.sessions
                    )
                )
            }

            builder.append(getString(R.string.history_recent_title))
            for (record in records.takeLast(8).reversed()) {
                val ended = DateUtils.formatDateTime(
                    ctx, record.endMs, DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_ABBREV_ALL
                )
                builder.append(
                    getString(
                        R.string.history_session_line, ended, profileTitle(record.profile),
                        formatHours(record.hours), record.startLevel, record.endLevel,
                        formatPercent(record.ratePerHour)
                    )
                )
            }
            builder.append(getString(R.string.history_note))
        }

        showTextDialog(R.string.dialog_history_title, builder.toString())
    }

    private fun showDozeCheck() {
        val appContext = requireContext().applicationContext
        if (!DozeVerifier.hasDumpPermission(appContext)) {
            showDumpNeeded(R.string.dialog_doze_title)
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            val dump = DozeInsights.readDump(appContext)
            if (!isAdded) return@launch
            if (dump == null) {
                showTextDialog(R.string.dialog_doze_title, getString(R.string.doze_failed))
                return@launch
            }

            val builder = StringBuilder()
            IdleHistory.parseState(dump)?.let {
                builder.append(getString(R.string.doze_now, it.deep, it.light))
            }

            val windowMs = 12 * 3_600_000L
            val summary = IdleHistory.summarize(IdleHistory.parseEvents(dump), windowMs)
            if (summary.coveredMs <= 0) {
                builder.append(getString(R.string.doze_no_history))
            } else {
                builder.append(
                    getString(
                        R.string.doze_summary, formatSpan(summary.coveredMs), formatSpan(summary.deepIdleMs),
                        (summary.deepShare * 100).toInt(), summary.deepIdleStretches, summary.deepMaintenanceWindows
                    )
                )
                builder.append("\n\n")
                builder.append(
                    getString(if (summary.deepShare >= 0.5) R.string.doze_good else R.string.doze_low)
                )
            }
            showTextDialog(R.string.dialog_doze_title, builder.toString())
        }
    }

    private fun appLabel(packageName: String): String = try {
        val pm = requireContext().packageManager
        pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
    } catch (_: Exception) {
        packageName
    }

    private fun showExemptApps() {
        val appContext = requireContext().applicationContext
        if (!DozeVerifier.hasDumpPermission(appContext)) {
            showDumpNeeded(R.string.dialog_exempt_title)
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            val apps = DozeInsights.readExemptApps(appContext)
            if (!isAdded) return@launch
            if (apps == null) {
                showTextDialog(R.string.dialog_exempt_title, getString(R.string.doze_failed))
                return@launch
            }

            val user = apps.filter { it.type == IdleHistory.ExemptType.USER }.sortedBy { appLabel(it.packageName).lowercase() }
            val system = apps.filter { it.type != IdleHistory.ExemptType.USER }

            val builder = MaterialAlertDialogBuilder(requireContext()).setTitle(R.string.dialog_exempt_title)
            if (user.isEmpty()) {
                builder.setMessage(getString(R.string.exempt_none))
            } else {
                /* A dialog with a message does not show its list, so the hint goes in the title */
                builder.setTitle(R.string.exempt_hint)
                    .setItems(user.map { "${appLabel(it.packageName)}\n${it.packageName}" }.toTypedArray()) { _, which ->
                        copyToClipboard("adb shell cmd deviceidle whitelist -${user[which].packageName}")
                        showMessage(getString(R.string.copied))
                    }
            }
            builder.setPositiveButton(R.string.dialog_close, null)
            if (system.isNotEmpty()) {
                builder.setNeutralButton(getString(R.string.exempt_system_button, system.size)) { _, _ ->
                    val names = system.map { appLabel(it.packageName) }.distinct().sorted().joinToString("\n")
                    showTextDialog(R.string.exempt_system_title, names)
                }
            }
            builder.show()
        }
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
