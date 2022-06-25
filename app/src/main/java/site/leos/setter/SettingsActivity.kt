package site.leos.setter

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat


class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)
        supportFragmentManager.beginTransaction().replace(R.id.settings, SettingsFragment()).commit()
        supportActionBar?.setDisplayHomeAsUpEnabled(false)

        // Disable 'Look up' context action menu when 'Translation' button is enabled in API 29+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            packageManager.setComponentEnabledSetting(ComponentName(BuildConfig.APPLICATION_ID, "${BuildConfig.APPLICATION_ID}.DictActivity28"), PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP)
    }

    class SettingsFragment: PreferenceFragmentCompat() {

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)
        }

        override fun onResume() {
            super.onResume()

            val colorDictAvailable = isColorDictAvailable()
            findPreference<SwitchPreferenceCompat>(getString(R.string.colordict_fullscreen_key))?.isVisible = colorDictAvailable
            findPreference<Preference>(getString(R.string.install_colordict_key))?.isVisible = !colorDictAvailable
        }

        override fun onPreferenceTreeClick(preference: Preference): Boolean =
            if (preference.key == getString(R.string.add_widget_key)) {
                requireContext().getSystemService(AppWidgetManager::class.java).requestPinAppWidget(ComponentName(requireContext(), SearchWidget::class.java), null, null)
                true
            }
            else super.onPreferenceTreeClick(preference)

        private fun isColorDictAvailable(): Boolean {
            return requireContext().packageManager.resolveActivity(Intent("colordict.intent.action.SEARCH"), PackageManager.MATCH_DEFAULT_ONLY) != null
        }
    }
}