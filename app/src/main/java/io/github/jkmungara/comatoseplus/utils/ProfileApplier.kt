package io.github.jkmungara.comatoseplus.utils

import android.content.Context
import android.os.Build
import androidx.preference.PreferenceManager
import io.github.jkmungara.comatoseplus.R
import io.github.jkmungara.comatoseplus.repositories.profiles.DozeDump
import io.github.jkmungara.comatoseplus.repositories.profiles.IdleMechanism
import io.github.jkmungara.comatoseplus.repositories.profiles.ProfileCatalog
import io.github.jkmungara.comatoseplus.repositories.profiles.ProfileId
import io.github.jkmungara.comatoseplus.repositories.profiles.ShellCommands
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Applies a profile the way the running Android version needs it:
 *
 * 1. Android 11 and older, and 14 and newer honor Settings.Global.device_idle_constants, so write it.
 * 2. If the system is not using the values (or this is Android 12-13, which ignore that setting),
 *    set them through DeviceConfig with root, or hand the user the exact commands to run over ADB.
 */
object ProfileApplier {
    const val PREF_APPLIED_PROFILE = "applied_profile"
    private const val PREF_USED_DEVICE_CONFIG = "used_device_config"

    sealed interface Outcome {
        /** Confirmed through dumpsys: the system is using the profile's values */
        data object Verified : Outcome

        /** Written, but there was no way to confirm (DUMP permission not granted) */
        data object AppliedUnverified : Outcome

        /** The default profile: overrides removed */
        data object DefaultApplied : Outcome

        /** Written, but dumpsys shows the system is still using other values */
        data class NotInEffect(val details: String) : Outcome

        /** Cannot be done from inside the app on this device; run [commands] in a shell */
        data class NeedsShell(val commands: List<String>, val sdk: Int) : Outcome

        data object PermissionDenied : Outcome

        data object Failed : Outcome
    }

    fun appliedProfile(context: Context): ProfileId? =
        PreferenceManager.getDefaultSharedPreferences(context)
            .getString(PREF_APPLIED_PROFILE, null)
            ?.let { name -> ProfileId.values().firstOrNull { it.name == name } }

    /**
     * [manual] is true when the user picked the profile (button or tile). Schedule and auto-revert
     * pass false, so they do not start a new auto-revert timer.
     */
    suspend fun apply(context: Context, profile: ProfileId, manual: Boolean = true): Outcome = withContext(Dispatchers.IO) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val config = ProfileCatalog.constantsOf(profile)
        val mechanism = IdleMechanism.forSdk(Build.VERSION.SDK_INT)

        fun remember() {
            prefs.edit().putString(PREF_APPLIED_PROFILE, profile.name).apply()
            DrainTracker.onProfileChanged(context, profile)
            Scheduler.onProfileApplied(context, profile, manual)
        }

        if (mechanism.honorsSettings) {
            when (DeviceIdleSettings.write(context, config)) {
                DeviceIdleSettings.WriteResult.PERMISSION_DENIED -> return@withContext Outcome.PermissionDenied
                DeviceIdleSettings.WriteResult.FAILED -> return@withContext Outcome.Failed
                DeviceIdleSettings.WriteResult.WRITTEN ->
                    /* Lets the boot receiver restore it. Removed for the default profile. */
                    prefs.edit().putString(context.getString(R.string.pref_saved_constants_key), config).apply()
            }
        }

        /* Default profile: nothing to verify. Clear DeviceConfig overrides if we ever set any. */
        if (config == null) {
            if (prefs.getBoolean(PREF_USED_DEVICE_CONFIG, false)) {
                val commands = ShellCommands.deviceConfigCommands(null)
                if (runAsRoot(commands, manual)) {
                    prefs.edit().putBoolean(PREF_USED_DEVICE_CONFIG, false).apply()
                } else {
                    return@withContext Outcome.NeedsShell(commands, Build.VERSION.SDK_INT)
                }
            }
            remember()
            return@withContext Outcome.DefaultApplied
        }

        val expected = ProfileCatalog.parse(config)
        val canVerify = DozeVerifier.hasDumpPermission(context)

        if (mechanism.honorsSettings) {
            if (!canVerify) {
                remember()
                return@withContext Outcome.AppliedUnverified
            }
            when (DozeVerifier.verify(context, expected, verifyAttempts(manual))) {
                DozeDump.Verification.Match -> {
                    remember()
                    return@withContext Outcome.Verified
                }
                DozeDump.Verification.NoData -> {
                    remember()
                    return@withContext Outcome.AppliedUnverified
                }
                is DozeDump.Verification.Mismatch -> Unit /* fall through to DeviceConfig below */
            }
        }

        /* Android 12-13, or the Settings value did not take effect: use DeviceConfig */
        val commands = ShellCommands.deviceConfigCommands(config)
        if (!runAsRoot(commands, manual))
            return@withContext Outcome.NeedsShell(commands, Build.VERSION.SDK_INT)

        prefs.edit().putBoolean(PREF_USED_DEVICE_CONFIG, true).apply()

        if (!canVerify) {
            remember()
            return@withContext Outcome.AppliedUnverified
        }

        when (val result = DozeVerifier.verify(context, expected, verifyAttempts(manual))) {
            DozeDump.Verification.Match -> {
                remember()
                Outcome.Verified
            }
            DozeDump.Verification.NoData -> {
                remember()
                Outcome.AppliedUnverified
            }
            is DozeDump.Verification.Mismatch -> Outcome.NotInEffect(result.details.first())
        }
    }

    /**
     * Automatic changes run inside a broadcast receiver, which Android gives only about ten seconds,
     * so they retry less and give up on a root prompt sooner than a change the user is watching.
     */
    private fun verifyAttempts(manual: Boolean): Int = if (manual) 4 else 2

    private fun runAsRoot(commands: List<String>, manual: Boolean): Boolean =
        ShellRunner.runAsRoot(ShellCommands.rootLine(commands), if (manual) 30L else 8L)?.success == true
}
