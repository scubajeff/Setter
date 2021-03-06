package site.leos.setter

import android.app.SearchManager
import android.content.*
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.widget.LinearLayout
import android.widget.PopupMenu
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.doOnLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.shape.CutCornerTreatment
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.shape.ShapeAppearanceModel
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.android.synthetic.main.activity_tabs.*


class WebSearchActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val query = intent.getStringExtra(Intent.EXTRA_PROCESS_TEXT) ?: ""
        val sp = PreferenceManager.getDefaultSharedPreferences(this)
        if (query.isNotBlank()) {
            //if (sp.getBoolean(getString(R.string.meta_search_key), true)) {
            if (intent.getBooleanExtra(META, true)) {
                // Meta search enabled
                setContentView(R.layout.activity_tabs)

                val tabTitle = resources.getStringArray(R.array.web_search_tab_title)
                val urls = resources.getStringArray(R.array.web_search_url)

                // Get user choice of web search engine
                urls[0] = sp.getString(getString(R.string.search_engine_key), getString(R.string.url_duck))
                urls[1] = sp.getString(getString(R.string.second_search_engine_key), getString(R.string.url_magi))

                tabs.addOnTabSelectedListener(object :TabLayout.OnTabSelectedListener{
                    override fun onTabReselected(tab: TabLayout.Tab?) {
                    }

                    override fun onTabUnselected(tab: TabLayout.Tab) {
                        // Restore default background
                        if (tab.position == 0 || tab.position == 1) tab.view.background = tabs.background
                        tab.view.setOnLongClickListener(null)
                    }

                    override fun onTabSelected(tab: TabLayout.Tab) {
                        // Draw a popup menu indicator for tab 0 and 1
                        if (tab.position == 0 || tab.position == 1) {
                            tab.view.apply {
                                doOnLayout {
                                    background = MaterialShapeDrawable(
                                        ShapeAppearanceModel.builder()
                                            .setTopLeftCornerSize((width + height - (8 * resources.displayMetrics.densityDpi / 160)).toFloat())
                                            .setTopLeftCorner(CutCornerTreatment())
                                            .build()
                                    ).apply { fillColor = ColorStateList.valueOf(getColor(R.color.color_primary)) }
                                }
                            }
                        }

                        tab.view.setOnLongClickListener { v->
                            PopupMenu(baseContext, v).run {
                                menu.add(Menu.NONE, 0, 0, R.string.menuitem_browser)
                                menu.add(Menu.NONE, 1, 1, R.string.menuitem_share_hyperlink)
                                menu.add(Menu.NONE, 2, 2, R.string.menuitem_copy_hyperlink)
                                show()
                                setOnMenuItemClickListener { menuItem->
                                    (supportFragmentManager.findFragmentByTag("f${tabs.selectedTabPosition}") as TextSearchFragment).getCurrentUrl()?.let { url->
                                        when(menuItem.itemId) {
                                            0-> startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                            1-> startActivity(Intent().apply {
                                                action = Intent.ACTION_SEND
                                                putExtra(Intent.EXTRA_TEXT, url)
                                                type = "text/plain"
                                            })
                                            2-> (getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("", url))
                                        }
                                    }
                                    true
                                }
                            }
                            true
                        }
                    }
                })

                viewPager.adapter = ViewStateAdapter(supportFragmentManager, lifecycle, query, urls)
                TabLayoutMediator(tabs, viewPager) {tab, position -> tab.text = tabTitle[position] }.attach()

                viewPager.recyclerView.enforceSingleScrollDirection()

                // Use reflection to reduce Viewpager2 slide sensitivity, so that PhotoView inside can zoom presently
                val recyclerView = (ViewPager2::class.java.getDeclaredField("mRecyclerView").apply{ isAccessible = true }).get(viewPager) as RecyclerView
                (RecyclerView::class.java.getDeclaredField("mTouchSlop")).apply {
                    isAccessible = true
                    set(recyclerView, (get(recyclerView) as Int) * 6)
                }

                // Click on tab 0 or 1 to temporarily change it's search engine
                val tabStrip: LinearLayout = tabs.getChildAt(0) as LinearLayout
                tabStrip.getChildAt(0).setOnClickListener {
                    // Show popup when the tab is displayed
                    if (viewPager.currentItem == 0) {
                        val menuEntries = resources.getStringArray(R.array.web_search_engine_entries)
                        val menuValues = resources.getStringArray(R.array.web_search_engine_values)
                        //val f = (viewPager.adapter as ViewStateAdapter).getFragmentAt(0)
                        PopupMenu(baseContext, it).run {
                            for (i in menuEntries.indices) menu.add(Menu.NONE, i, i, menuEntries[i])
                            show()
                            setOnMenuItemClickListener { menuItem->
                                urls[0] = menuValues[menuItem.itemId]
                                (supportFragmentManager.findFragmentByTag("f0") as TextSearchFragment).reload(menuValues[menuItem.itemId] + query)
                                true
                            }
                        }
                    }
                }
                tabStrip.getChildAt(1).setOnClickListener {
                    // Show popup when the tab is displayed
                    if (viewPager.currentItem == 1) {
                        val menuEntries = resources.getStringArray(R.array.second_search_engine_entries)
                        val menuValues = resources.getStringArray(R.array.second_search_engine_values)
                        PopupMenu(baseContext, it).run {
                            for (i in menuEntries.indices) menu.add(Menu.NONE, i, i, menuEntries[i])
                            show()
                            setOnMenuItemClickListener { menuItem->
                                urls[1] = menuValues[menuItem.itemId]
                                (supportFragmentManager.findFragmentByTag("f1") as TextSearchFragment).reload(menuValues[menuItem.itemId] + query)
                                true
                            }
                        }
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

        override fun getItemCount(): Int = url.size

        override fun createFragment(position: Int): Fragment {
            return TextSearchFragment.newInstance(url[position] + query)
        }
    }

    companion object {
        const val FIREFOX = "org.mozilla.firefox"
        const val META = "META"
    }
}
