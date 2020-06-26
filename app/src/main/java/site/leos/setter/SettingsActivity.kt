package site.leos.setter

import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceFragmentCompat
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
        }

        override fun onResume() {
            super.onResume()
            if (isColorDictAvailable()) {
                // Enable ColorDict setter activity
                //context!!.packageManager.setComponentEnabledSetting(
                //    ComponentName(context!!, site.leos.setter.DictActivity::class.java), PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP)

                findPreference<SwitchPreferenceCompat>(getString(R.string.colordict_fullscreen_key))?.apply {
                    isEnabled = true
                    summary = ""
                }
            } else {
                findPreference<SwitchPreferenceCompat>(getString(R.string.colordict_fullscreen_key))?.let {
                    it.summary = getString(R.string.colordict_not_installed)
                    it.setOnPreferenceClickListener {
                        try {startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://$COLORDICT_PACKAGE_NAME")))
                        } catch (e: ActivityNotFoundException) {
                            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/$COLORDICT_PACKAGE_NAME")))
                        }
                        true
                    }
                }
            }
        }

        private fun isColorDictAvailable(): Boolean {
            return requireActivity().packageManager?.queryIntentActivities(Intent("colordict.intent.action.SEARCH"), PackageManager.MATCH_DEFAULT_ONLY)?.size!! > 0
        }

        companion object {
            const val COLORDICT_PACKAGE_NAME = "details?id=com.socialnmobile.colordict"
        }
    }
}