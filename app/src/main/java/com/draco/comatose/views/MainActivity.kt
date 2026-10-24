package com.draco.comatose.views

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.draco.comatose.R
import com.draco.comatose.fragments.MainPreferenceFragment
import com.draco.comatose.utils.PermissionUtils

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

        /* If we are missing the permission (never granted, or revoked later), go get it */
        if (!PermissionUtils.isPermissionsGranted(this, android.Manifest.permission.WRITE_SECURE_SETTINGS))
            startActivity(Intent(this, PermissionActivity::class.java))
    }
}
