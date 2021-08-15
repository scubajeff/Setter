package site.leos.setter

import android.annotation.SuppressLint
import android.content.*
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.webkit.*
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import com.google.android.material.progressindicator.LinearProgressIndicator
import kotlinx.android.synthetic.main.fragment_webview.*

class TextSearchFragment : Fragment(){
    private lateinit var webView: WebView
    private var resultLoaded = false
    private lateinit var urlString: String
    private var popupRemoved = false
    private lateinit var packageManager: PackageManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        urlString = arguments?.getString(KEY_URL)!!
        packageManager = requireActivity().packageManager
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_webview, container, false)
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        webView = view.findViewById(R.id.webview)
        val progressIndicator : LinearProgressIndicator = view.findViewById(R.id.progress_indicator)

        // Prepare webView
        webView.settings.apply {
            //userAgentString = GoogleReverseImageSearchFragment.USER_AGENT_CHROME
            //cacheMode = WebSettings.LOAD_NO_CACHE
            javaScriptEnabled = true
            domStorageEnabled = true
            javaScriptCanOpenWindowsAutomatically = true
            loadsImagesAutomatically = true
            loadWithOverviewMode = true
            useWideViewPort = true
            builtInZoomControls = true
            displayZoomControls = false
            setSupportZoom(true)
            setGeolocationEnabled(false)

            if (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES && WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
                WebSettingsCompat.setForceDark(this, WebSettingsCompat.FORCE_DARK_ON)
            }
        }
        webView.scrollBarStyle = WebView.SCROLLBARS_OUTSIDE_OVERLAY
        webView.isScrollbarFadingEnabled = true

        registerForContextMenu(webView)

        // Load links in webview
        webView.webViewClient = object : WebViewClient() {
            private val defaultBrowserName = packageManager.resolveActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://example.com")), PackageManager.MATCH_DEFAULT_ONLY)?.activityInfo?.packageName ?: ""

            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean =
                request?.let {
                    when(it.url.scheme) {
                        in setOf("http", "https")-> {
                            if (defaultBrowserName != (packageManager.resolveActivity(Intent(Intent.ACTION_VIEW, it.url), PackageManager.MATCH_DEFAULT_ONLY)?.activityInfo?.packageName ?: "NO_MATCH")
                                && !(it.url.toString().startsWith(urlString.substringBefore('?')))
                            ) {
                                try {
                                    startActivity(Intent(Intent.ACTION_VIEW, it.url).apply {
                                        addCategory(Intent.CATEGORY_BROWSABLE)
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) flags =
                                            flags or Intent.FLAG_ACTIVITY_REQUIRE_NON_BROWSER
                                    })
                                } catch (e: ActivityNotFoundException) {
                                    e.printStackTrace()
                                    return false
                                }
                                webView.stopLoading()
                                true
                            } else false
                        }
                        else-> {
                            try {
                                startActivity(Intent().setAction(Intent.ACTION_VIEW).setData(it.url))
                            } catch (e: ActivityNotFoundException) {
                                e.printStackTrace()
                            }
                            webView.stopLoading()
                            true
                        }
                    }
                } ?: true

            /*
            override fun onReceivedHttpError(view: WebView?, request: WebResourceRequest?, errorResponse: WebResourceResponse?) {
                super.onReceivedHttpError(view, request, errorResponse)
                status.text = errorResponse?.reasonPhrase
                progressIndicator.visibility = ProgressIndicator.GONE
                webView.visibility = WebView.GONE
                status.visibility = TextView.VISIBLE
            }
             */

            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                super.onReceivedError(view, request, error)
                // Reload the page when ERROR_TIMEOUT or ERROR_HOST_LOOKUP happens on base url
                if (error?.errorCode == ERROR_TIMEOUT || error?.errorCode == ERROR_HOST_LOOKUP) {
                    if (urlString.contains(request?.url?.host.toString())) view?.reload()
                }
                //Log.e("===================================", "${error?.errorCode} ${error?.description} ${request?.url}")
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                url?.let {
                    when {
                        it.contains("deepl.com")-> {
                            view?.evaluateJavascript("(function() { document.getElementById('dl_cookieBanner').style.display = 'none'; document.getElementById('footer').style.display = 'none'; })();") {}
                        }
                        it.contains("twitter.com")-> {
                            view?.postDelayed( Runnable { view.evaluateJavascript("(function() { document.getElementById('layers').style.display = 'none'; })();") {} }, 2000)
                        }
                        it.contains("reddit.com")-> {
                            view?.postDelayed( Runnable { view.evaluateJavascript("(function() { document.getElementsByClassName('XPromoPill__container')[0].style.display = 'none'; })();") {} }, 2000)
                        }
                        it.contains("weibo.cn")-> {
                            view?.postDelayed(Runnable { view.evaluateJavascript("(function() { document.getElementsByClassName('card card11')[0].style.display = 'none'; document.getElementsByClassName('m-tab-bar m-bar-panel m-container-max')[0].style.display = 'none'; })();") {} }, 2000)
                        }
                        (it.contains("stackoverflow.com") || it.contains("stackexchange.com"))-> {
                            //view?.postDelayed( Runnable { view.evaluateJavascript("(function() { document.getElementsByClassName('js-consent-banner')[0].style.display = 'none'; })();") {} }, 1000)
                            view?.evaluateJavascript("(function() { document.getElementsByClassName('js-consent-banner')[0].style.display = 'none'; })();") {}
                        }
                        else-> {}
                    }
                }
            }
        }

        // Display loading progress
        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView, progress: Int) {
                if (progress < 100 && progressIndicator.visibility == LinearProgressIndicator.GONE) {
                    progressIndicator.visibility = LinearProgressIndicator.VISIBLE
                }
                progressIndicator.progress = progress
                if (progress == 100) {
                    progressIndicator.visibility = ProgressBar.GONE
                    resultLoaded = true
                } else resultLoaded = false
            }
            override fun onJsAlert(view: WebView?, url: String?, message: String?, result: JsResult): Boolean {
                result.cancel()
                return true
            }

            override fun onJsBeforeUnload(view: WebView?, url: String?, message: String?, result: JsResult): Boolean {
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

        /*
        // catch link clicked event
        webView.setOnTouchListener { v, event ->
            webView.hitTestResult.apply {
                Log.e(">>>>", "${this.type} ${this.extra}")
            }
            false
        }
         */

        if (savedInstanceState != null) {
            resultLoaded = savedInstanceState.getBoolean(RESULT_LOADED)
            webView.restoreState(savedInstanceState)
            progressIndicator.visibility = LinearProgressIndicator.GONE
        }

        if (!resultLoaded) {
            status.visibility = TextView.GONE
            webView.visibility = WebView.VISIBLE
            webView.loadUrl(urlString)
        }
    }

    override fun onResume() {
        super.onResume()

        popupRemoved = false

        webView.onResume()
        webView.isFocusableInTouchMode = true
        webView.requestFocus()
        webView.setOnKeyListener(View.OnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                if (event.action != KeyEvent.ACTION_UP) return@OnKeyListener true
                if (resultLoaded) {
                    if (webView.canGoBack()) {
                        webView.goBack()

                        return@OnKeyListener true
                    }
                    else activity?.onBackPressed()
                } else {
                    webView.stopLoading()
                    return@OnKeyListener true
                }
            }
            false
        })
    }

    override fun onPause() {
        webView.onPause()
        super.onPause()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(RESULT_LOADED, resultLoaded)
        webView.saveState(outState)
    }

    override fun onDestroy() {
        if (PreferenceManager.getDefaultSharedPreferences(requireContext()).getBoolean(getString(R.string.remove_cookie_key), true)) {
            CookieManager.getInstance().removeAllCookies(null)
        }
        super.onDestroy()
    }

    override fun onCreateContextMenu(menu: ContextMenu, v: View, menuInfo: ContextMenu.ContextMenuInfo?) {
        when (webView.hitTestResult.type) {
/*
            WebView.HitTestResult.UNKNOWN_TYPE-> {
                menu.add(0, MENU_ITEM_UNKNOWN, 0, R.string.menuitem_browser)
                menu.add(0, MENU_ITEM_SHARE_HYPERLINK, 1, R.string.menuitem_share_hyperlink)
                menu.add(0, MENU_ITEM_COPY_HYPERLINK, 2, R.string.menuitem_copy_hyperlink)
                menu.setHeaderTitle(webView.url)
            }
*/
            WebView.HitTestResult.SRC_ANCHOR_TYPE, WebView.HitTestResult.EMAIL_TYPE, WebView.HitTestResult.GEO_TYPE, WebView.HitTestResult.PHONE_TYPE-> {
                menu.add(0, MENU_ITEM_VIEW_HYPERLINK, 0, R.string.menuitem_view_hyperlink)
                menu.add(0, MENU_ITEM_SHARE_HYPERLINK, 1, R.string.menuitem_share_hyperlink)
                menu.add(0, MENU_ITEM_COPY_HYPERLINK, 2, R.string.menuitem_copy_hyperlink)
                menu.setHeaderTitle(webView.hitTestResult.extra.toString())
            }
            WebView.HitTestResult.IMAGE_TYPE, WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE-> {
                menu.add(0, MENU_ITEM_SEARCH_IMAGE, 0, R.string.menuitem_search_image)
                //menu.add(0, MENU_ITEM_DOWNLOAD_IMAGE, 1, R.string.menuitem_download_image)
            }
            else-> super.onCreateContextMenu(menu, v, menuInfo)
        }
    }

    override fun onContextItemSelected(item: MenuItem): Boolean =
        webView.hitTestResult.extra?.let {
            when(item.itemId) {
                MENU_ITEM_VIEW_HYPERLINK->{
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it)))
                    true
                }
                MENU_ITEM_SHARE_HYPERLINK->{
                    startActivity(Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, it)
                        type = "text/plain"
                    })
                    true
                }
                MENU_ITEM_COPY_HYPERLINK->{
                    (requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("", it))
                    true
                }
                MENU_ITEM_DOWNLOAD_IMAGE->{
                    true
                }
                MENU_ITEM_SEARCH_IMAGE->{
                    startActivity(Intent().apply {
                        action = ReverseImageSearchActivity.REVERSE_SEARCH_LINK
                        putExtra(Intent.EXTRA_TEXT, it)
                        putExtra(SHARE_FROM_ME, true)
                        type = "text/plain"
                    })
                    true
                }
                else-> false
            }
        } ?: false
/*
        run {
            if (webView.hitTestResult.type == WebView.HitTestResult.UNKNOWN_TYPE) {
                when(item.itemId) {
                    MENU_ITEM_UNKNOWN -> {
                        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(webView.url)))
                        true
                    }
                    MENU_ITEM_SHARE_HYPERLINK -> {
                        startActivity(Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, webView.url)
                            type = "text/plain"
                        })
                        true
                    }
                    MENU_ITEM_COPY_HYPERLINK -> {
                        (requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(
                            ClipData.newPlainText("", webView.url)
                        )
                        true
                    }
                    else -> false
                }
            }
            else false
        }
*/

    fun reload(newUrl: String) {
        urlString = newUrl
        webView.loadUrl(urlString)
    }

    fun getCurrentUrl(): String? {
        return webView.url
    }

    companion object {
        private const val RESULT_LOADED = "RESULT_LOADED"
        private const val KEY_URL = "KEY_URL"
        private const val SHARE_FROM_ME = "SHARE_FROM_ME"
        private const val MENU_ITEM_VIEW_HYPERLINK = 0
        private const val MENU_ITEM_SHARE_HYPERLINK = 1
        private const val MENU_ITEM_COPY_HYPERLINK = 2
        private const val MENU_ITEM_DOWNLOAD_IMAGE = 3
        private const val MENU_ITEM_SEARCH_IMAGE = 4
        private const val MENU_ITEM_UNKNOWN = 5

        fun newInstance(url: String) = TextSearchFragment().apply {arguments = Bundle().apply {putString(KEY_URL, url)}}
    }
}

