package site.leos.setter

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager

class WebSearchActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val query = intent.getStringExtra(Intent.EXTRA_PROCESS_TEXT) ?: ""
        if (query.isNotBlank()) {
            val searchURL = PreferenceManager.getDefaultSharedPreferences(this).getString(getString(R.string.search_engine_key),getString(R.string.url_sp))
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(searchURL + query)))
        }
        finish()
    }
}