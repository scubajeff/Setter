package site.leos.setter

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.preference.PreferenceManager
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.android.synthetic.main.activity_tabs.*
import java.util.regex.Pattern

class ReverseImageSearchActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (intent.action == Intent.ACTION_SEND) {
            // If text/* is shared to us and the text is not a image link then call WebSearchActivity
            if (intent.type?.startsWith("text/")!!) {
                intent.getStringExtra(Intent.EXTRA_TEXT)?.let {
                    val imgPattern = Pattern.compile("^https://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|](\\.(?i)(jpe?g|png|gif|bmp))\$")
                    if (!imgPattern.matcher(it).matches()) {
                        // If text sent is not a image link, then call WebSearchActivity
                        startActivity(Intent(this, WebSearchActivity::class.java)
                            .putExtra(Intent.EXTRA_PROCESS_TEXT, it)
                            .putExtra(WebSearchActivity.META, PreferenceManager.getDefaultSharedPreferences(this).getBoolean(getString(R.string.meta_search_key), true)))
                        finish()
                        return
                    }
                }
            }

            // Otherwise do image reverse search
            setContentView(R.layout.activity_tabs)

            viewPager.adapter = ViewStateAdapter(supportFragmentManager, lifecycle)
            TabLayoutMediator(tabs, viewPager) {tab, position ->
                when (position) {
                    ReverseImageSearchFragment.SERVICE_GOOGLE -> {tab.text = "Google"}
                    ReverseImageSearchFragment.SERVICE_SOGOU -> {tab.text = getString(R.string.sogou)}
                    ReverseImageSearchFragment.SERVICE_BING -> {tab.text = "Bing"}
                    ReverseImageSearchFragment.SERVICE_YANDEX -> {tab.text = "Yandex"}
                    //ReverseImageSearchFragment.SERVICE_TINEYE -> {tab.text = "TinEye"}
                    //ReverseImageSearchFragment.SERVICE_PAILITAO -> {tab.text = getString(R.string.pailitao)}
                }
            }.attach()

            viewPager.recyclerView.enforceSingleScrollDirection()
        }
        else finish()
    }

    private class ViewStateAdapter(fragmentManager: FragmentManager, lifecycle: Lifecycle) : FragmentStateAdapter(fragmentManager, lifecycle) {
        override fun getItemCount(): Int = ReverseImageSearchFragment.SERVICES_TOTAL

        override fun createFragment(position: Int): Fragment {
            return ReverseImageSearchFragment.newInstance(position)
        }
    }
}