package io.github.jkmungara.comatoseplus.tiles

import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.Toast
import io.github.jkmungara.comatoseplus.R
import io.github.jkmungara.comatoseplus.repositories.profiles.ProfileId
import io.github.jkmungara.comatoseplus.utils.ProfileApplier
import io.github.jkmungara.comatoseplus.utils.ProfileApplier.Outcome
import io.github.jkmungara.comatoseplus.utils.ProfileNames
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/** Quick Settings tile: tap to cycle Default, Light, Moderate, High, Extreme */
class ProfileTileService : TileService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onStartListening() {
        super.onStartListening()
        refreshTile()
    }

    override fun onClick() {
        super.onClick()

        val profiles = ProfileId.values()
        val current = ProfileApplier.appliedProfile(applicationContext) ?: ProfileId.DEFAULT
        val next = profiles[(current.ordinal + 1) % profiles.size]

        qsTile?.apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                subtitle = getString(R.string.status_applying)
            updateTile()
        }

        scope.launch {
            when (ProfileApplier.apply(applicationContext, next)) {
                is Outcome.NeedsShell, Outcome.PermissionDenied, Outcome.Failed, is Outcome.NotInEffect ->
                    Toast.makeText(applicationContext, R.string.tile_needs_app, Toast.LENGTH_LONG).show()
                else -> Unit
            }
            refreshTile()
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun refreshTile() {
        val tile = qsTile ?: return
        val applied = ProfileApplier.appliedProfile(applicationContext)

        tile.label = getString(R.string.app_name)
        tile.icon = Icon.createWithResource(this, R.drawable.ic_tile)
        tile.state = if (applied == null || applied == ProfileId.DEFAULT) Tile.STATE_INACTIVE else Tile.STATE_ACTIVE
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            tile.subtitle = getString(ProfileNames.titleRes(applied ?: ProfileId.DEFAULT))
        tile.updateTile()
    }
}
