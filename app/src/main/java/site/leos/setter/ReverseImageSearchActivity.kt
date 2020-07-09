package site.leos.setter

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.android.synthetic.main.activity_tabs.*

class ReverseImageSearchActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tabs)

        viewPager.adapter = ViewStateAdapter(supportFragmentManager, lifecycle)
        TabLayoutMediator(tabs, viewPager) {tab, position ->
            when (position) {
                ReverseImageSearchFragment.SERVICE_GOOGLE -> {tab.text = "Google"}
                ReverseImageSearchFragment.SERVICE_SOGOU -> {tab.text = getString(R.string.sogou)}
                ReverseImageSearchFragment.SERVICE_BING -> {tab.text = "Bing"}
                ReverseImageSearchFragment.SERVICE_YANDEX -> {tab.text = "Yandex"}
                ReverseImageSearchFragment.SERVICE_TINEYE -> {tab.text = "TinEye"}
                //ReverseImageSearchFragment.SERVICE_PAILITAO -> {tab.text = getString(R.string.pailitao)}
            }
        }.attach()

        viewPager.recyclerView.enforceSingleScrollDirection()
    }

    private class ViewStateAdapter(fragmentManager: FragmentManager, lifecycle: Lifecycle) : FragmentStateAdapter(fragmentManager, lifecycle) {
        override fun getItemCount(): Int = ReverseImageSearchFragment.SERVICES_TOTAL

        override fun createFragment(position: Int): Fragment {
            return ReverseImageSearchFragment.newInstance(position)
        }
    }
}