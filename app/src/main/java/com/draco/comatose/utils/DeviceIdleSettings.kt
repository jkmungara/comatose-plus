package com.draco.comatose.utils

import android.content.Context
import android.net.Uri
import android.provider.Settings
import android.util.Log
import com.draco.comatose.repositories.constants.SettingsConstants
import com.draco.comatose.repositories.profiles.ProfileCatalog

/**
 * Reads and writes Settings.Global.device_idle_constants, reporting what actually happened
 * instead of assuming the write worked.
 */
object DeviceIdleSettings {
    private const val TAG = "DeviceIdleSettings"

    sealed interface ApplyResult {
        /** The value was written and reads back as expected */
        data object Success : ApplyResult

        /** WRITE_SECURE_SETTINGS is missing (never granted, or revoked) */
        data object PermissionDenied : ApplyResult

        /** The write went through but the stored value differs from what we asked for */
        data class Mismatch(val actual: String?) : ApplyResult

        /** Anything else (provider rejected the write, unexpected exception) */
        data class Failure(val error: Throwable?) : ApplyResult
    }

    val uri: Uri get() = Settings.Global.getUriFor(SettingsConstants.DEVICE_IDLE_CONSTANTS)

    /** Current stored value. Reading needs no special permission. */
    fun read(context: Context): String? = try {
        Settings.Global.getString(context.contentResolver, SettingsConstants.DEVICE_IDLE_CONSTANTS)
    } catch (e: Exception) {
        Log.w(TAG, "Could not read ${SettingsConstants.DEVICE_IDLE_CONSTANTS}", e)
        null
    }

    fun apply(context: Context, config: String?): ApplyResult {
        return try {
            val written = Settings.Global.putString(
                context.contentResolver,
                SettingsConstants.DEVICE_IDLE_CONSTANTS,
                config
            )
            if (!written)
                return ApplyResult.Failure(null)

            val actual = read(context)
            if (ProfileCatalog.sameConstants(actual, config))
                ApplyResult.Success
            else
                ApplyResult.Mismatch(actual)
        } catch (e: SecurityException) {
            Log.w(TAG, "WRITE_SECURE_SETTINGS not granted", e)
            ApplyResult.PermissionDenied
        } catch (e: Exception) {
            Log.w(TAG, "Failed to write ${SettingsConstants.DEVICE_IDLE_CONSTANTS}", e)
            ApplyResult.Failure(e)
        }
    }
}
