package com.draco.comatose.views

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.draco.comatose.R
import com.draco.comatose.viewmodels.PermissionActivityViewModel
import com.google.android.material.snackbar.Snackbar

class PermissionActivity : AppCompatActivity() {
    private val viewModel: PermissionActivityViewModel by viewModels()

    private lateinit var command: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_permission)

        applyNavigationBarInsets(findViewById(R.id.root))

        command = findViewById(R.id.command)

        /* Copy ADB command to clipboard */
        command.setOnClickListener {
            val clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("ADB Command", command.text.toString())
            clipboardManager.setPrimaryClip(clip)

            Snackbar.make(command, R.string.copied, Snackbar.LENGTH_SHORT).show()
        }

        /* Once permission is granted, return */
        viewModel.permissionGranted.observe(this) {
            if (it == true)
                finish()
        }

        /* Without the permission the app is unusable, so leaving this screen leaves the app */
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finishAffinity()
            }
        })
    }

    override fun onResume() {
        super.onResume()
        viewModel.refresh()
    }

    private fun applyNavigationBarInsets(root: android.view.View) {
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.navigationBars() or WindowInsetsCompat.Type.displayCutout()
            )
            view.updatePadding(left = bars.left, right = bars.right, bottom = bars.bottom)
            insets
        }
    }
}
