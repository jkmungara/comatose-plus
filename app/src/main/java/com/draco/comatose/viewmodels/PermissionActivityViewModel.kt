package com.draco.comatose.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.draco.comatose.BuildConfig
import com.draco.comatose.utils.PermissionUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class PermissionActivityViewModel(application: Application) : AndroidViewModel(application) {
    private val _permissionGranted = MutableLiveData(isWriteSecureSettingsPermissionGranted())
    val permissionGranted: LiveData<Boolean> = _permissionGranted

    private var rootAttempted = false

    private fun isWriteSecureSettingsPermissionGranted(): Boolean {
        val context = getApplication<Application>().applicationContext
        return PermissionUtils.isPermissionsGranted(context, android.Manifest.permission.WRITE_SECURE_SETTINGS)
    }

    /**
     * Re-check the permission. Call this when the screen comes back to the foreground, for
     * example after the user ran the ADB command.
     */
    fun refresh() {
        _permissionGranted.value = isWriteSecureSettingsPermissionGranted()
    }

    /**
     * If the device is rooted, grant the permission ourselves. Runs once, waits for the command
     * to finish (the root prompt may need a tap) and gives up after a timeout.
     */
    private fun tryRootGrant() {
        if (rootAttempted) return
        rootAttempted = true

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val process = ProcessBuilder(
                    "su",
                    "-c",
                    "pm grant ${BuildConfig.APPLICATION_ID} android.permission.WRITE_SECURE_SETTINGS"
                ).redirectErrorStream(true).start()

                try {
                    process.waitFor(ROOT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                } finally {
                    process.destroy()
                }
            } catch (_: Exception) {
                /* No su binary, or the user denied it. The ADB route still works. */
            }

            _permissionGranted.postValue(isWriteSecureSettingsPermissionGranted())
        }
    }

    init {
        if (_permissionGranted.value != true)
            tryRootGrant()
    }

    private companion object {
        const val ROOT_TIMEOUT_SECONDS = 30L
    }
}
