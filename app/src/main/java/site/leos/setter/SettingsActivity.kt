package site.leos.setter

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceGroup
import android.content.ComponentName
import androidx.preference.SwitchPreferenceCompat

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)
        supportFragmentManager.beginTransaction().replace(R.id.settings, SettingsFragment()).commit()
        supportActionBar?.setDisplayHomeAsUpEnabled(false)
    }

    class SettingsFragment() : PreferenceFragmentCompat() {

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)
            if (isColorDictAvailable()) {
                // Enable ColorDict setter activity
                //context!!.packageManager.setComponentEnabledSetting(
                //    ComponentName(context!!, site.leos.setter.DictActivity::class.java), PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP)

                findPreference<SwitchPreferenceCompat>(getString(R.string.colordict_fullscreen_key))?.apply {
                    isEnabled = true
                    summary = ""
                }
            } else {
                findPreference<SwitchPreferenceCompat>(getString(R.string.colordict_fullscreen_key))?.summary =getString(R.string.colordict_not_installed)
            }
        }

        private fun isColorDictAvailable(): Boolean {
            return requireActivity().packageManager?.queryIntentActivities(Intent("colordict.intent.action.SEARCH"), PackageManager.MATCH_DEFAULT_ONLY)?.size!! > 0
        }
    }
}