package site.leos.setter

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.android.synthetic.main.activity_tabs.*
import java.util.*


class TranslationActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tabs)

        // Prepare query url string
        val defaultLocale = Locale.getDefault()
        var deepLURL = getString(R.string.url_deepl)
        var googleURL = getString(R.string.url_google_tranlation)
        val query = intent.getStringExtra("QUERY")
        deepLURL = "$deepLURL${defaultLocale.language}/$query"
        if (defaultLocale.language.equals("zh")) googleURL = googleURL + defaultLocale.language + "-" + defaultLocale.country + "&text="
        else googleURL = googleURL + defaultLocale.language + "&text="
        googleURL += query

        viewPager.adapter = ViewStateAdapter(supportFragmentManager, lifecycle, deepLURL, googleURL)
        TabLayoutMediator(tabs, viewPager) {tab, position ->
            when (position) {
                0 -> {tab.text = "DeepL"}
                1 -> {tab.text = "Google"}
            }
        }.attach()
        viewPager.recyclerView.enforceSingleScrollDirection()
    }

    private class ViewStateAdapter(fragmentManager: FragmentManager, lifecycle: Lifecycle, val url0:String, val url1:String)
        : FragmentStateAdapter(fragmentManager, lifecycle) {
        override fun getItemCount(): Int = 2

        override fun createFragment(position: Int): Fragment {
            when (position) {
                0 -> return TextSearchFragment.newInstance(url0)
                else -> return TextSearchFragment.newInstance(url1)
            }
        }
    }
}

