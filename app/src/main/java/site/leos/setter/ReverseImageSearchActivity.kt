package site.leos.setter

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
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
                ReverseImageSearchFragment.SERVICE_TINEYE -> {tab.text = "TinEye"}
            }
        }.attach()
        viewPager.recyclerView.enforceSingleScrollDirection()
    }

    private class ViewStateAdapter(fragmentManager: FragmentManager, lifecycle: Lifecycle) : FragmentStateAdapter(fragmentManager, lifecycle) {
        override fun getItemCount(): Int = 3

        override fun createFragment(position: Int): Fragment {
            return ReverseImageSearchFragment.newInstance(position)
        }
    }
}