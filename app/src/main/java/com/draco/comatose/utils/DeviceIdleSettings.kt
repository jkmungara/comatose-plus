package com.draco.comatose.utils

import android.content.Context
import android.provider.Settings
import android.util.Log
import com.draco.comatose.repositories.constants.SettingsConstants

/**
 * Writes Settings.Global.device_idle_constants.
 *
 * Note this setting is hidden and not annotated @Readable, so since Android 12 apps get a
 * SecurityException when reading it back. Whether a write took effect is checked through
 * `dumpsys deviceidle` instead (see DozeVerifier).
 */
object DeviceIdleSettings {
    private const val TAG = "DeviceIdleSettings"

    enum class WriteResult { WRITTEN, PERMISSION_DENIED, FAILED }

    fun write(context: Context, config: String?): WriteResult {
        return try {
            val ok = Settings.Global.putString(
                context.contentResolver,
                SettingsConstants.DEVICE_IDLE_CONSTANTS,
                config
            )
            if (ok) WriteResult.WRITTEN else WriteResult.FAILED
        } catch (e: SecurityException) {
            Log.w(TAG, "WRITE_SECURE_SETTINGS not granted", e)
            WriteResult.PERMISSION_DENIED
        } catch (e: Exception) {
            Log.w(TAG, "Failed to write ${SettingsConstants.DEVICE_IDLE_CONSTANTS}", e)
            WriteResult.FAILED
        }
    }
}
