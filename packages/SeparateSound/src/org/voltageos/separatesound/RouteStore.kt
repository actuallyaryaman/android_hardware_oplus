package org.voltageos.separatesound

import android.content.Context
import android.provider.Settings

object RouteStore {
    private const val KEY_ENABLED = "separate_sound_enabled"
    private const val KEY_PACKAGE = "separate_sound_pkg"
    private const val KEY_DEVICE_TYPE = "separate_sound_device_type"
    private const val KEY_DEVICE_ADDRESS = "separate_sound_device_address"
    private const val KEY_DEVICE_NAME = "separate_sound_device_name"

    fun isEnabled(context: Context): Boolean {
        return Settings.System.getInt(context.contentResolver, KEY_ENABLED, 0) == 1
    }

    fun setEnabled(context: Context, enabled: Boolean) {
        Settings.System.putInt(context.contentResolver, KEY_ENABLED, if (enabled) 1 else 0)
    }

    fun getSelectedPackage(context: Context): String? {
        val pkg = Settings.System.getString(context.contentResolver, KEY_PACKAGE)
        return if (pkg.isNullOrEmpty()) null else pkg
    }

    fun setSelectedPackage(context: Context, pkg: String?) {
        Settings.System.putString(context.contentResolver, KEY_PACKAGE, pkg ?: "")
    }

    fun getSelectedDeviceType(context: Context): Int? {
        val type = Settings.System.getInt(context.contentResolver, KEY_DEVICE_TYPE, -1)
        return if (type == -1) null else type
    }

    fun setSelectedDeviceType(context: Context, type: Int?) {
        Settings.System.putInt(context.contentResolver, KEY_DEVICE_TYPE, type ?: -1)
    }

    fun getSelectedDeviceAddress(context: Context): String? {
        val addr = Settings.System.getString(context.contentResolver, KEY_DEVICE_ADDRESS)
        return if (addr.isNullOrEmpty()) null else addr
    }

    fun setSelectedDeviceAddress(context: Context, address: String?) {
        Settings.System.putString(context.contentResolver, KEY_DEVICE_ADDRESS, address ?: "")
    }

    fun getSelectedDeviceName(context: Context): String? {
        val name = Settings.System.getString(context.contentResolver, KEY_DEVICE_NAME)
        return if (name.isNullOrEmpty()) null else name
    }

    fun setSelectedDeviceName(context: Context, name: String?) {
        Settings.System.putString(context.contentResolver, KEY_DEVICE_NAME, name ?: "")
    }

    fun clearAll(context: Context) {
        setEnabled(context, false)
        setSelectedPackage(context, null)
        setSelectedDeviceType(context, null)
        setSelectedDeviceAddress(context, null)
        setSelectedDeviceName(context, null)
    }
}
