package com.tutozz.blespam

import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.Toast

abstract class BaseSpammerTileService : TileService() {

    abstract val spammerTypes: List<String>
    abstract val spammerLabel: String

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    override fun onClick() {
        super.onClick()

        // If ANY are running, we consider the tile active, so clicking it stops all of them.
        val isRunning = spammerTypes.any { SpamService.isSpammerRunning(it) }

        if (isRunning) {
            spammerTypes.forEach { SpamService.stopSpammer(this, it) }
        } else {
            val bluetoothAvailability = BluetoothHelper.availability(this)
            if (bluetoothAvailability != BluetoothHelper.BluetoothAvailability.READY) {
                if (bluetoothAvailability == BluetoothHelper.BluetoothAvailability.OFF) {
                    BluetoothHelper.showBluetoothOffNotification(this)
                } else {
                    BluetoothHelper.toStatusMessageRes(bluetoothAvailability)?.let { resId ->
                        Toast.makeText(this, getString(resId), Toast.LENGTH_SHORT).show()
                    }
                }
                updateTileState()
                return
            }
            spammerTypes.forEach { SpamService.startSpammer(this, it) }
        }

        updateTileState()
    }

    protected fun updateTileState() {
        val tile = qsTile ?: return

        val isRunning = spammerTypes.any { SpamService.isSpammerRunning(it) }
        val bluetoothAvailability = BluetoothHelper.availability(this)
        val isBluetoothReady = bluetoothAvailability == BluetoothHelper.BluetoothAvailability.READY

        if (isRunning) {
            tile.state = Tile.STATE_ACTIVE
            tile.label = "Spamming..."
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                tile.subtitle = spammerLabel
            }
        } else if (!isBluetoothReady) {
            tile.state = Tile.STATE_UNAVAILABLE
            tile.label = spammerLabel
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                tile.subtitle = BluetoothHelper.toStatusMessageRes(bluetoothAvailability)
                    ?.let { getString(it) }
                    ?: "Unavailable"
            }
        } else {
            tile.state = Tile.STATE_INACTIVE
            tile.label = spammerLabel
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                tile.subtitle = "Off"
            }
        }

        tile.updateTile()
    }
}
