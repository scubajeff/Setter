package site.leos.setter

import android.app.SearchManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager

class WebSearchActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val query = intent.getStringExtra(Intent.EXTRA_PROCESS_TEXT) ?: ""
        if (query.isNotBlank()) {
            val searchIntent = Intent().setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)

            val searchURL = PreferenceManager.getDefaultSharedPreferences(this).getString(getString(R.string.search_engine_key),getString(R.string.url_default_search))
            if (searchURL.equals(getString(R.string.url_default_search))) {
                searchIntent.setAction(Intent.ACTION_WEB_SEARCH).putExtra(SearchManager.QUERY, query)

                // Firefox does NOT support SearchManager.QUERY extra!!!
                val defaultBrowser = packageManager.resolveActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://example.com")), PackageManager.MATCH_DEFAULT_ONLY)
                if (defaultBrowser?.activityInfo?.packageName.equals(FIREFOX))
                    searchIntent.setComponent(ComponentName(FIREFOX, "org.mozilla.gecko.BrowserApp")).setData(Uri.parse(query))
            }
            else searchIntent.setAction(Intent.ACTION_VIEW).addCategory(Intent.CATEGORY_BROWSABLE).setData(Uri.parse(searchURL + query))

            startActivity(searchIntent)
        }
        finish()
    }

    companion object {
        const val FIREFOX = "org.mozilla.firefox"
    }
}