package org.voltageos.separatesound

import android.app.AlertDialog
import android.content.Context
import android.media.AudioDeviceInfo
import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreference

class SeparateSoundSettingsFragment : PreferenceFragmentCompat() {

    private lateinit var enablePref: SwitchPreference
    private lateinit var appPref: Preference
    private lateinit var devicePref: Preference
    private lateinit var statusPref: Preference

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.separate_sound_settings)

        enablePref = findPreference("enable_separate_sound")!!
        appPref = findPreference("selected_app")!!
        devicePref = findPreference("selected_device")!!
        statusPref = findPreference("status")!!

        val ctx = requireContext()

        enablePref.isChecked = RouteStore.isEnabled(ctx)
        enablePref.setOnPreferenceChangeListener { _, newValue ->
            val enabled = newValue as Boolean
            RouteStore.setEnabled(ctx, enabled)
            if (enabled) {
                SeparateSoundService.start(ctx)
            } else {
                SeparateSoundService.stop(ctx)
            }
            refreshUi()
            true
        }

        appPref.setOnPreferenceClickListener {
            showAppPickerDialog()
            true
        }

        devicePref.setOnPreferenceClickListener {
            showDevicePickerDialog()
            true
        }

        refreshUi()
    }

    override fun onResume() {
        super.onResume()
        refreshUi()
    }

    private fun refreshUi() {
        val ctx = requireContext()
        val enabled = RouteStore.isEnabled(ctx)
        val pkg = RouteStore.getSelectedPackage(ctx)
        val deviceName = RouteStore.getSelectedDeviceName(ctx)

        enablePref.isChecked = enabled
        appPref.isEnabled = enabled
        devicePref.isEnabled = enabled && pkg != null

        appPref.summary = if (pkg != null) {
            getAppLabel(ctx, pkg) ?: pkg
        } else {
            getString(R.string.no_app_selected)
        }

        devicePref.summary = deviceName ?: getString(R.string.device_default)

        statusPref.summary = buildStatusText(ctx, enabled, pkg, deviceName)
    }

    private fun buildStatusText(
        ctx: Context,
        enabled: Boolean,
        pkg: String?,
        deviceName: String?
    ): String {
        if (!enabled) return getString(R.string.status_disabled)
        if (pkg == null || deviceName == null) return getString(R.string.status_no_app)
        val label = getAppLabel(ctx, pkg) ?: pkg

        val audioManager = ctx.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
        val configs = audioManager.getActivePlaybackConfigurations()
        val uid = getUidForPackage(ctx, pkg)
        val isPlaying = uid != null && configs.any { it.clientUid == uid }

        return if (isPlaying) {
            getString(R.string.status_enabled, label, deviceName)
        } else {
            getString(R.string.status_app_not_running, label)
        }
    }

    private fun showAppPickerDialog() {
        val ctx = requireContext()
        val pm = ctx.packageManager
        val intent = android.content.Intent(android.content.Intent.ACTION_MAIN)
        intent.addCategory(android.content.Intent.CATEGORY_LAUNCHER)
        val resolveInfos = pm.queryIntentActivities(intent, 0)

        val apps = resolveInfos
            .filter { it.activityInfo.packageName != ctx.packageName }
            .sortedBy { it.loadLabel(pm).toString() }
            .toTypedArray()

        val labels = apps.map { it.loadLabel(pm).toString() }.toTypedArray()
        val packages = apps.map { it.activityInfo.packageName }.toTypedArray()

        AlertDialog.Builder(ctx)
            .setTitle(R.string.app_picker_title)
            .setItems(labels) { _, which ->
                val pkg = packages[which]
                RouteStore.setSelectedPackage(ctx, pkg)
                refreshUi()
                SeparateSoundService.update(ctx)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun showDevicePickerDialog() {
        val ctx = requireContext()
        val devices = AudioDeviceManager.getAvailableDevices(ctx)
            .filter { it.type != AudioDeviceInfo.TYPE_BUILTIN_EARPIECE }

        if (devices.isEmpty()) {
            android.widget.Toast.makeText(ctx, "No audio output devices found", android.widget.Toast.LENGTH_SHORT).show()
            return
        }

        val names = devices.map { it.name }.toTypedArray()
        val currentType = RouteStore.getSelectedDeviceType(ctx)
        val currentAddress = RouteStore.getSelectedDeviceAddress(ctx)

        var checkedItem = -1
        for (i in devices.indices) {
            val d = devices[i]
            if (d.type == currentType && d.address == currentAddress) {
                checkedItem = i
                break
            }
        }

        AlertDialog.Builder(ctx)
            .setTitle(R.string.device_picker_title)
            .setSingleChoiceItems(names, checkedItem) { dialog, which ->
                val device = devices[which]
                RouteStore.setSelectedDeviceType(ctx, device.type)
                RouteStore.setSelectedDeviceAddress(ctx, device.address)
                RouteStore.setSelectedDeviceName(ctx, device.name)
                refreshUi()
                SeparateSoundService.update(ctx)
                dialog.dismiss()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun getAppLabel(ctx: Context, pkg: String): String? {
        return try {
            val pm = ctx.packageManager
            val ai = pm.getApplicationInfo(pkg, 0)
            pm.getApplicationLabel(ai)?.toString()
        } catch (e: Exception) {
            null
        }
    }

    private fun getUidForPackage(ctx: Context, pkg: String): Int? {
        return try {
            ctx.packageManager.getApplicationInfo(pkg, 0).uid
        } catch (e: Exception) {
            null
        }
    }
}
