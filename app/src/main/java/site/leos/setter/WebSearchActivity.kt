package site.leos.setter

import android.app.SearchManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.preference.PreferenceManager
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.android.synthetic.main.activity_tabs.*

class WebSearchActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val query = intent.getStringExtra(Intent.EXTRA_PROCESS_TEXT) ?: ""
        val sp = PreferenceManager.getDefaultSharedPreferences(this)
        if (query.isNotBlank()) {
            if (sp.getBoolean(getString(R.string.meta_search_key), true)) {
                // Meta search enabled
                setContentView(R.layout.activity_tabs)

                val tabTitle = resources.getStringArray(R.array.web_search_tab_title)
                val urls = resources.getStringArray(R.array.web_search_url)

                // Get user choice of web search engine
                urls[0] = sp.getString(getString(R.string.search_engine_key), getString(R.string.url_default))
                // If user choose Mag[i] as web search engine, then remove the redundant Mag[i] tab, which is the last tab
                val count = if (urls[0] == getString(R.string.url_magi)) 1 else 0

                viewPager.adapter = ViewStateAdapter(supportFragmentManager, lifecycle, query, urls.dropLast(count).toTypedArray())
                TabLayoutMediator(tabs, viewPager) {tab, position -> tab.text = tabTitle[position] }.attach()

                viewPager.recyclerView.enforceSingleScrollDirection()
            }
            else {
                // Default search enabled
                val searchIntent = Intent().setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK).setAction(Intent.ACTION_WEB_SEARCH).putExtra(SearchManager.QUERY, query)

                // Firefox does NOT support SearchManager.QUERY extra!!!
                val defaultBrowser = packageManager.resolveActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://example.com")), PackageManager.MATCH_DEFAULT_ONLY)
                if (defaultBrowser?.activityInfo?.packageName.equals(FIREFOX))
                    searchIntent.setComponent(ComponentName(FIREFOX, "org.mozilla.gecko.BrowserApp")).setData(Uri.parse(query))

                startActivity(searchIntent)
                finish()
                return
            }
        }
        else {
            finish()
            return
        }
    }

    private class ViewStateAdapter(fragmentManager: FragmentManager, lifecycle: Lifecycle, val query: String, val url: Array<String>)
        : FragmentStateAdapter(fragmentManager, lifecycle) {

        override fun getItemCount(): Int = url.size

        override fun createFragment(position: Int): Fragment {
            return TextSearchFragment.newInstance(url[position] + query)
        }
    }

    companion object {
        const val FIREFOX = "org.mozilla.firefox"
    }
}