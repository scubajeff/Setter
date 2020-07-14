package site.leos.setter

import android.app.SearchManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.*
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.preference.PreferenceManager
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.progressindicator.ProgressIndicator
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.android.synthetic.main.activity_tabs.*
import kotlinx.android.synthetic.main.fragment_webview.*

class WebSearchActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val query = intent.getStringExtra(Intent.EXTRA_PROCESS_TEXT) ?: ""
        if (query.isNotBlank()) {
            val searchURL = PreferenceManager.getDefaultSharedPreferences(this).getString(getString(R.string.search_engine_key),getString(R.string.default_search_value))
            if (searchURL.equals(getString(R.string.default_search_value))) {
                val searchIntent = Intent().setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK).setAction(Intent.ACTION_WEB_SEARCH).putExtra(SearchManager.QUERY, query)

                // Firefox does NOT support SearchManager.QUERY extra!!!
                val defaultBrowser = packageManager.resolveActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://example.com")), PackageManager.MATCH_DEFAULT_ONLY)
                if (defaultBrowser?.activityInfo?.packageName.equals(FIREFOX))
                    searchIntent.setComponent(ComponentName(FIREFOX, "org.mozilla.gecko.BrowserApp")).setData(Uri.parse(query))

                startActivity(searchIntent)
                finish()
                return
            }
            else {
                setContentView(R.layout.activity_tabs)

                val tabTitle = resources.getStringArray(R.array.search_engine_tab_title)
                viewPager.adapter = ViewStateAdapter(supportFragmentManager, lifecycle, query, resources.getStringArray(R.array.search_engine_url))
                TabLayoutMediator(tabs, viewPager) {tab, position -> tab.text = tabTitle[position] }.attach()
                viewPager.recyclerView.enforceSingleScrollDirection()
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
            return WebSearchFragment.newInstance(url[position] + query)
        }
    }

    class WebSearchFragment : Fragment() {
        lateinit var webView: WebView
        var resultLoaded:Boolean = false

        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
            return inflater.inflate(R.layout.fragment_webview, container, false)
        }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)
            webView = view.findViewById(R.id.webview)
            val progressIndicator : ProgressIndicator = view.findViewById(R.id.progress_indicator)

            // Prepare webView
            webView.settings.apply {
                //userAgentString = GoogleReverseImageSearchFragment.USER_AGENT_CHROME
                //cacheMode = WebSettings.LOAD_NO_CACHE
                javaScriptEnabled = true
                javaScriptCanOpenWindowsAutomatically = false
                loadsImagesAutomatically = true
                loadWithOverviewMode = true
                useWideViewPort = true
                setGeolocationEnabled(false)
            }

            // Load links in webview
            webView.webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean = false

                override fun onReceivedHttpError(view: WebView?, request: WebResourceRequest?, errorResponse: WebResourceResponse?) {
                    super.onReceivedHttpError(view, request, errorResponse)
                    status.text = errorResponse?.reasonPhrase
                    progressIndicator.visibility = ProgressIndicator.GONE
                    webView.visibility = WebView.GONE
                    status.visibility = TextView.VISIBLE
                }
            }

            // Display loading progress
            webView.webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView, progress: Int) {
                    if (progress < 100 && progressIndicator.visibility == ProgressIndicator.GONE) {
                        progressIndicator.visibility = ProgressIndicator.VISIBLE
                    }
                    progressIndicator.progress = progress
                    if (progress == 100) {
                        progressIndicator.visibility = ProgressBar.GONE
                        resultLoaded = true
                    }
                }

                override fun onJsAlert(view: WebView?, url: String?, message: String?, result: JsResult): Boolean {
                    result.cancel()
                    return true
                }

                override fun onJsConfirm(view: WebView?, url: String?, message: String?, result: JsResult): Boolean {
                    result.cancel()
                    return true
                }

                override fun onJsPrompt(view: WebView?, url: String?, message: String?, defaultValue: String?, result: JsPromptResult): Boolean {
                    result.cancel()
                    return true
                }
            }

            if (savedInstanceState != null) {
                resultLoaded = savedInstanceState.getBoolean(RESULT_LOADED)
                webView.restoreState(savedInstanceState)
                progressIndicator.visibility = ProgressIndicator.GONE
            }

            if (!resultLoaded) {
                status.visibility = TextView.GONE
                webView.visibility = WebView.VISIBLE
                webView.loadUrl(arguments?.getString("URL"))
            }

            webView.isFocusableInTouchMode = true
            webView.requestFocus()
            webView.setOnKeyListener(View.OnKeyListener { v, keyCode, event ->
                if (keyCode == KeyEvent.KEYCODE_BACK) {
                    if (event.action != KeyEvent.ACTION_UP) return@OnKeyListener true
                    if (webView.canGoBack()) {
                        webView.goBack()
                        return@OnKeyListener true
                    }
                    else activity?.onBackPressed()
                }
                false
            })
        }

        override fun onSaveInstanceState(outState: Bundle) {
            super.onSaveInstanceState(outState)
            outState.putBoolean(RESULT_LOADED, resultLoaded)
            webView.saveState(outState)
        }

        companion object {
            const val RESULT_LOADED = "RESULT_LOADED"

            fun newInstance(arg: String) = WebSearchFragment().apply {arguments = Bundle().apply {putString("URL", arg)}}
        }
    }

    companion object {
        const val FIREFOX = "org.mozilla.firefox"
    }
}