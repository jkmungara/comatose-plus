package io.github.jkmungara.comatoseplus.views

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import io.github.jkmungara.comatoseplus.R
import io.github.jkmungara.comatoseplus.fragments.MainPreferenceFragment
import io.github.jkmungara.comatoseplus.utils.PermissionUtils
import io.github.jkmungara.comatoseplus.utils.Scheduler

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        /* Only add the fragment once; it restores itself after rotation */
        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.preferences, MainPreferenceFragment())
                .commit()
        }
    }

    override fun onResume() {
        super.onResume()

        /* Alarms are cleared when the app is force-stopped, so re-arm them whenever it is opened */
        Scheduler.ensureScheduled(this)

        /* If we are missing the permission (never granted, or revoked later), go get it */
        if (!PermissionUtils.isPermissionsGranted(this, android.Manifest.permission.WRITE_SECURE_SETTINGS))
            startActivity(Intent(this, PermissionActivity::class.java))
    }
}
