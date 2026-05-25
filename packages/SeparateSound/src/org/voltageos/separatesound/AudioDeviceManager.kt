package org.voltageos.separatesound

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.util.Log

data class AudioDeviceEntry(
    val type: Int,
    val name: String,
    val address: String,
    val isBluetooth: Boolean,
    val isConnected: Boolean
)

object AudioDeviceManager {
    private const val TAG = "AudioDeviceManager"

    fun getAvailableDevices(context: Context): List<AudioDeviceEntry> {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        val entries = mutableListOf<AudioDeviceEntry>()

        for (device in devices) {
            when (device.type) {
                AudioDeviceInfo.TYPE_BUILTIN_SPEAKER,
                AudioDeviceInfo.TYPE_LINE_ANALOG,
                AudioDeviceInfo.TYPE_LINE_DIGITAL,
                AudioDeviceInfo.TYPE_WIRED_HEADSET,
                AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
                AudioDeviceInfo.TYPE_USB_HEADSET,
                AudioDeviceInfo.TYPE_USB_DEVICE,
                AudioDeviceInfo.TYPE_USB_ACCESSORY,
                AudioDeviceInfo.TYPE_DOCK,
                AudioDeviceInfo.TYPE_HDMI,
                AudioDeviceInfo.TYPE_HDMI_ARC,
                AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
                AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> {
                    entries.add(
                        AudioDeviceEntry(
                            type = device.type,
                            name = device.productName?.toString()
                                ?: getDefaultName(device.type),
                            address = device.address ?: "",
                            isBluetooth = device.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP
                                    || device.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
                            isConnected = device.isSink
                        )
                    )
                }
            }
        }

        return entries
    }

    fun getBluetoothA2dpDevices(context: Context): List<AudioDeviceEntry> {
        val btAdapter = BluetoothAdapter.getDefaultAdapter() ?: return emptyList()
        val a2dp = btAdapter.getProfileProxy(
            context,
            null,
            BluetoothProfile.A2DP
        ) ?: return emptyList()

        return emptyList()
    }

    fun getDeviceDisplayName(type: Int): String {
        return getDefaultName(type)
    }

    private fun getDefaultName(type: Int): String {
        return when (type) {
            AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> "Built-in Speaker"
            AudioDeviceInfo.TYPE_BUILTIN_EARPIECE -> "Earpiece"
            AudioDeviceInfo.TYPE_WIRED_HEADSET -> "Wired Headset"
            AudioDeviceInfo.TYPE_WIRED_HEADPHONES -> "Wired Headphones"
            AudioDeviceInfo.TYPE_USB_HEADSET -> "USB Headset"
            AudioDeviceInfo.TYPE_USB_DEVICE -> "USB Audio"
            AudioDeviceInfo.TYPE_USB_ACCESSORY -> "USB Accessory"
            AudioDeviceInfo.TYPE_DOCK -> "Dock"
            AudioDeviceInfo.TYPE_HDMI -> "HDMI"
            AudioDeviceInfo.TYPE_HDMI_ARC -> "HDMI ARC"
            AudioDeviceInfo.TYPE_BLUETOOTH_A2DP -> "Bluetooth A2DP"
            AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> "Bluetooth SCO"
            AudioDeviceInfo.TYPE_LINE_ANALOG -> "Line Out"
            AudioDeviceInfo.TYPE_LINE_DIGITAL -> "Digital Out"
            else -> "Unknown ($type)"
        }
    }
}
