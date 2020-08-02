package site.leos.setter

import android.app.SearchManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.SparseArray
import android.view.Menu
import android.widget.LinearLayout
import android.widget.PopupMenu
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
                urls[0] = sp.getString(getString(R.string.search_engine_key), getString(R.string.url_duck))
                urls[1] = sp.getString(getString(R.string.second_search_engine_key), getString(R.string.url_magi))

                viewPager.adapter = ViewStateAdapter(supportFragmentManager, lifecycle, query, urls)
                TabLayoutMediator(tabs, viewPager) {tab, position -> tab.text = tabTitle[position] }.attach()

                viewPager.recyclerView.enforceSingleScrollDirection()

                // Long click on tab 0 or 1 to temporarily change it's search engine
                val tabStrip: LinearLayout = tabs.getChildAt(0) as LinearLayout
                tabStrip.getChildAt(0).setOnLongClickListener {
                    // Show popup when the tab is displayed
                    if (viewPager.currentItem != 0) false
                    else {
                        val menuEntries = resources.getStringArray(R.array.web_search_engine_entries)
                        val menuValues = resources.getStringArray(R.array.web_search_engine_values)
                        val f = (viewPager.adapter as ViewStateAdapter).getFragmentAt(0)
                        val popupMenu = PopupMenu(baseContext, it).run {
                            for (i in menuEntries.indices) menu.add(Menu.NONE, i, i, menuEntries[i])
                            show()
                            setOnMenuItemClickListener {
                                (f as TextSearchFragment).reload(menuValues[it.itemId] + query)
                                true
                            }
                        }

                        true
                    }
                }
                tabStrip.getChildAt(1).setOnLongClickListener {
                    // Show popup when the tab is displayed
                    if (viewPager.currentItem != 1) false
                    else {
                        val menuEntries = resources.getStringArray(R.array.second_search_engine_entries)
                        val menuValues = resources.getStringArray(R.array.second_search_engine_values)
                        val f = (viewPager.adapter as ViewStateAdapter).getFragmentAt(1)
                        val popupMenu = PopupMenu(baseContext, it).run {
                            for (i in menuEntries.indices) menu.add(Menu.NONE, i, i, menuEntries[i])
                            show()
                            setOnMenuItemClickListener {
                                (f as TextSearchFragment).reload(menuValues[it.itemId] + query)
                                true
                            }
                        }

                        true
                    }
                }
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
        val registeredFragments: SparseArray<Fragment> = SparseArray()

        override fun getItemCount(): Int = url.size

        override fun createFragment(position: Int): Fragment {
            val f :Fragment = TextSearchFragment.newInstance(url[position] + query)
            registeredFragments.put(position, f)

            return f
        }

        fun getFragmentAt(position: Int) :Fragment { return registeredFragments.get(position) }
    }

    companion object {
        const val FIREFOX = "org.mozilla.firefox"
    }
}