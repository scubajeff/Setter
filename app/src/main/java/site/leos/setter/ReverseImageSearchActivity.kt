package site.leos.setter

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.android.synthetic.main.activity_reverse_image_search.*

class ReverseImageSearchActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reverse_image_search)

        viewPager.adapter = ViewStateAdapter(supportFragmentManager, lifecycle)
        TabLayoutMediator(tabs, viewPager) {tab, position ->  
            when (position) {
                0 -> {tab.text = "Google"}
            }
        }.attach()
    }

    private class ViewStateAdapter(fragmentManager: FragmentManager, lifecycle: Lifecycle) : FragmentStateAdapter(fragmentManager, lifecycle) {
        override fun getItemCount(): Int = 1

        override fun createFragment(position: Int): Fragment {
            return GoogleReverseImageSearchFragment()
        }
    }
}