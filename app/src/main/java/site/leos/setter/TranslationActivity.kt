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
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.widget.PopupMenu
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import java.util.*


class TranslationActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tabs)

        // Prepare query url string
        val defaultLocale = Locale.getDefault()
        val query = intent.getStringExtra(KEY_QUERY)
        var deepLURL = getString(R.string.url_deepl)
        var googleURL = getString(R.string.url_google_translation)
        val papagoURL = getString(R.string.url_papago_translation) + query
        val udURL = getString(R.string.url_urban_dictionary) + query
        val jikiURL = getString(R.string.url_jikipedia) + query

        deepLURL = "$deepLURL${defaultLocale.language}/$query"
        googleURL = if (defaultLocale.language.equals("zh")) googleURL + defaultLocale.language + "-" + defaultLocale.country + "&text=" else googleURL + defaultLocale.language + "&text="
        googleURL += query

        val viewPager = findViewById<ViewPager2>(R.id.viewPager)
        val tabs = findViewById<TabLayout>(R.id.tabs)

        viewPager.adapter = ViewStateAdapter(supportFragmentManager, lifecycle, deepLURL, googleURL, papagoURL, udURL, jikiURL)
        TabLayoutMediator(tabs, viewPager) {tab, position ->
            when (position) {
                0 -> {tab.text = "DeepL"}
                1 -> {tab.text = "Google"}
                2 -> {tab.text = "Papago"}
                3 -> {tab.text = "Urban Dictionary"}
                4 -> {tab.text = getString(R.string.jikiName)}
            }
        }.attach()
        viewPager.recyclerView.enforceSingleScrollDirection()

        tabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener{
            override fun onTabSelected(tab: TabLayout.Tab) {
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

            override fun onTabUnselected(tab: TabLayout.Tab?) {}

            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        // Use reflection to reduce Viewpager2 slide sensitivity, so that PhotoView inside can zoom presently
        val recyclerView = (ViewPager2::class.java.getDeclaredField("mRecyclerView").apply{ isAccessible = true }).get(viewPager) as RecyclerView
        (RecyclerView::class.java.getDeclaredField("mTouchSlop")).apply {
            isAccessible = true
            set(recyclerView, (get(recyclerView) as Int) * 7)
        }
    }

    private class ViewStateAdapter(fragmentManager: FragmentManager, lifecycle: Lifecycle, val url0:String, val url1:String, val url2:String, val url3:String, val url4:String)
        : FragmentStateAdapter(fragmentManager, lifecycle) {
        override fun getItemCount(): Int = 5

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> TextSearchFragment.newInstance(url0)
                1 -> TextSearchFragment.newInstance(url1)
                2 -> TextSearchFragment.newInstance(url2)
                3 -> TextSearchFragment.newInstance(url3)
                else -> TextSearchFragment.newInstance(url4)
            }
        }
    }

    companion object {
        const val KEY_QUERY = "KEY_QUERY"
    }
}