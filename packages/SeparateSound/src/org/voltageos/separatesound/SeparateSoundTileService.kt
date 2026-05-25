package org.voltageos.separatesound

import android.content.Intent
import android.graphics.drawable.Icon
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.util.Log

class SeparateSoundTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    override fun onClick() {
        val enabled = RouteStore.isEnabled(this)
        if (enabled) {
            RouteStore.setEnabled(this, false)
            SeparateSoundService.stop(this)
        } else {
            RouteStore.setEnabled(this, true)
            SeparateSoundService.start(this)
        }
        updateTileState()
    }

    private fun updateTileState() {
        val tile = qsTile ?: return
        val enabled = RouteStore.isEnabled(this)

        tile.state = if (enabled) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = getString(R.string.tile_label)
        tile.icon = Icon.createWithResource(this, R.drawable.ic_separate_sound)
        tile.contentDescription = if (enabled) {
            val pkg = RouteStore.getSelectedPackage(this)
            val deviceName = RouteStore.getSelectedDeviceName(this)
            if (pkg != null && deviceName != null) {
                getString(R.string.status_enabled, pkg, deviceName)
            } else {
                getString(R.string.status_no_app)
            }
        } else {
            getString(R.string.status_disabled)
        }
        tile.updateTile()
    }
}
