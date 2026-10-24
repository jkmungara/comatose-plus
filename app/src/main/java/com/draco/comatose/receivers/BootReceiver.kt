package com.draco.comatose.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.preference.PreferenceManager
import com.draco.comatose.R
import com.draco.comatose.repositories.profiles.ProfileCatalog
import com.draco.comatose.utils.DeviceIdleSettings

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED)
            return

        val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context)
        val config = sharedPrefs.getString(context.getString(R.string.pref_saved_constants_key), null)
            ?: return

        /* Nothing to do if the value survived the reboot */
        if (ProfileCatalog.sameConstants(DeviceIdleSettings.read(context), config))
            return

        val result = DeviceIdleSettings.apply(context, config)
        if (result != DeviceIdleSettings.ApplyResult.Success)
            Log.w("BootReceiver", "Could not re-apply saved profile: $result")
    }
}
