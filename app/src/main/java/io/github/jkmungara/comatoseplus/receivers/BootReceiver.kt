package io.github.jkmungara.comatoseplus.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.preference.PreferenceManager
import io.github.jkmungara.comatoseplus.R
import io.github.jkmungara.comatoseplus.repositories.profiles.IdleMechanism
import io.github.jkmungara.comatoseplus.utils.DeviceIdleSettings
import io.github.jkmungara.comatoseplus.utils.Scheduler

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED)
            return

        /* Alarms do not survive a reboot */
        Scheduler.ensureScheduled(context)

        /* Android 12-13 ignore this setting, so there is nothing to restore */
        if (!IdleMechanism.forSdk(Build.VERSION.SDK_INT).honorsSettings)
            return

        val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context)
        val config = sharedPrefs.getString(context.getString(R.string.pref_saved_constants_key), null)
            ?: return

        /* The stored value cannot be read back by apps, so simply write it again */
        val result = DeviceIdleSettings.write(context, config)
        if (result != DeviceIdleSettings.WriteResult.WRITTEN)
            Log.w("BootReceiver", "Could not re-apply saved profile: $result")
    }
}
