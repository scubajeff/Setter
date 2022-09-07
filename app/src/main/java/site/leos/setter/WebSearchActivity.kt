/*
 *   Copyright 2019 Jeffrey Liu (scubajeffrey@criptext.com)
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package site.leos.setter

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.net.Uri
import android.os.Bundle
import android.util.TypedValue
import android.view.Menu
import android.widget.LinearLayout
import android.widget.PopupMenu
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
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

class WebSearchActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val query = intent.getStringExtra(if (intent.action == Intent.ACTION_SEND) Intent.EXTRA_TEXT else Intent.EXTRA_PROCESS_TEXT) ?: ""

        if (query.isEmpty()) {
            finish()
            return
        }

        setContentView(R.layout.activity_tabs)

        val tabTitle = resources.getStringArray(R.array.web_search_tab_title)
        val urls = resources.getStringArray(R.array.web_search_url)

        // Get user choice of web search engine
        with(PreferenceManager.getDefaultSharedPreferences(this)) {
            urls[0] = getString(getString(R.string.search_engine_key), getString(R.string.url_duck))
            urls[1] = getString(getString(R.string.second_search_engine_key), getString(R.string.url_magi))
        }

        val viewPager = findViewById<ViewPager2>(R.id.viewPager)
        val tabs = findViewById<TabLayout>(R.id.tabs)

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
                            ).apply { fillColor = ColorStateList.valueOf(
                                TypedValue().let {
                                    theme.resolveAttribute(android.R.attr.colorPrimary, it, true)
                                    ContextCompat.getColor(context, it.resourceId)
                                }
                            ) }
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

    private class ViewStateAdapter(fragmentManager: FragmentManager, lifecycle: Lifecycle, val query: String, val url: Array<String>)
        : FragmentStateAdapter(fragmentManager, lifecycle) {

        override fun getItemCount(): Int = url.size

        override fun createFragment(position: Int): Fragment {
            return TextSearchFragment.newInstance(url[position] + query)
        }
    }

    companion object {
        const val META = "META"
    }
}
