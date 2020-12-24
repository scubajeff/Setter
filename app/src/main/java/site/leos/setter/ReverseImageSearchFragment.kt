package site.leos.setter

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.*
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.android.material.progressindicator.LinearProgressIndicator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.lang.Integer.max
import java.net.HttpURLConnection
import java.net.URL
import java.util.*
import javax.net.ssl.HttpsURLConnection

class ReverseImageSearchFragment : Fragment() {
    lateinit var webView:WebView
    lateinit var status:TextView
    var resultLoaded:Boolean = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_webview, container, false)
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        var result:String? = null
        val type = arguments?.getInt(SERVICE_KEY)!!

        super.onViewCreated(view, savedInstanceState)
        webView = view.findViewById(R.id.webview)
        status = view.findViewById(R.id.status)
        val progressIndicator : LinearProgressIndicator = view.findViewById(R.id.progress_indicator)

        // Prepare webView
        webView.settings.apply {
            userAgentString = USER_AGENT_CHROME
            //cacheMode = WebSettings.LOAD_NO_CACHE
            javaScriptEnabled = true
            javaScriptCanOpenWindowsAutomatically = false
            loadsImagesAutomatically = true
            layoutAlgorithm = WebSettings.LayoutAlgorithm.TEXT_AUTOSIZING
            loadWithOverviewMode = true
            useWideViewPort = true
            builtInZoomControls = true
            displayZoomControls = false
            setSupportZoom(true)
            setGeolocationEnabled(false)
        }
        webView.scrollBarStyle = WebView.SCROLLBARS_OUTSIDE_OVERLAY
        webView.isScrollbarFadingEnabled = true

        // Load links in webview
        webView.webViewClient = object : WebViewClient() {
            // Load pages in this webview
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean = false

            // Force https to http
            override fun onLoadResource(view: WebView?, url: String?) {
                val mUrl = url?.replace("http://", "https://")
                super.onLoadResource(view, mUrl)
            }

            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                super.onReceivedError(view, request, error)
                // Reload the page when ERROR_TIMEOUT or ERROR_HOST_LOOKUP happens on result's base url
                if (error?.errorCode == WebViewClient.ERROR_TIMEOUT || error?.errorCode == WebViewClient.ERROR_HOST_LOOKUP) {
                    if (result!!.contains(request?.url?.host.toString())) view?.reload()
                }
                //Log.e("===================================", "${error?.errorCode} ${error?.description} ${request?.url}")
            }

            /*
            override fun onReceivedHttpError(view: WebView?, request: WebResourceRequest?, errorResponse: WebResourceResponse?) {
                super.onReceivedHttpError(view, request, errorResponse)
                status.text = errorResponse?.reasonPhrase
                progressIndicator.visibility = LinearProgressIndicator.GONE
                webView.visibility = WebView.GONE
                status.visibility = TextView.VISIBLE
            }
             */
        }

        webView.setOnLongClickListener { v ->
            val hitTestResult = (v as WebView).hitTestResult
            when (hitTestResult.type) {
                WebView.HitTestResult.SRC_ANCHOR_TYPE -> {
                    startActivity(Intent().apply {
                        action = Intent.ACTION_VIEW
                        data = Uri.parse(hitTestResult.extra)
                    })
                    true
                }
                else -> {
                    false
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

        if (savedInstanceState != null) {
            resultLoaded = savedInstanceState.getBoolean(RESULT_LOADED)
            webView.restoreState(savedInstanceState)
            progressIndicator.visibility = LinearProgressIndicator.GONE
        }

        if (!resultLoaded) {
            activity?.intent.let {
                if ((it?.action == Intent.ACTION_SEND) && (it.type?.startsWith("image/") == true)) {
                    // It's image file share
                    (it.getParcelableExtra<Parcelable>(Intent.EXTRA_STREAM) as? Uri)?.let {
                        // Prepare progressIndicator
                        progressIndicator.apply {
                            isIndeterminate = true
                            visibility = LinearProgressIndicator.VISIBLE
                        }

                        // Launch coroutine to upload image
                        //lifecycleScope.launch {
                        CoroutineScope(Dispatchers.IO).launch(Dispatchers.IO) {
                            if (result == null) {
                                // Get image dimension
                                val option = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                                BitmapFactory.decodeStream(activity?.contentResolver?.openInputStream(it), null, option)

                                // Upload the image
                                result = uploadImage(it, getSampleSize(max(option.outWidth, option.outHeight), MAX_SIDE_LENGTH), type)
                            }

                            // Load result page in webView
                            withContext(Dispatchers.Main) {
                                if (result!!.startsWith("Error")) {
                                    progressIndicator.visibility = LinearProgressIndicator.GONE
                                    webView.visibility = WebView.GONE
                                    status.text = result
                                    status.visibility = TextView.VISIBLE
                                }
                                else {
                                    // Make progress indicator determinated by page loading progress
                                    progressIndicator.apply {
                                        isIndeterminate = false
                                        max = 100
                                    }

                                    status.visibility = TextView.GONE
                                    webView.visibility = WebView.VISIBLE
                                    webView.loadUrl(result!!)
                                }
                            }
                        }
                    }
                }
                else {
                    // It's a image link share, link already validate in ReverseImageSearchActivity
                    val imageURL = it?.getStringExtra(Intent.EXTRA_TEXT)
                    result = when(type) {
                        SERVICE_GOOGLE -> "https://images.google.com/searchbyimage?image_url=$imageURL&encoded_image=&image_content=&filename=&hl=en"
                        SERVICE_SOGOU -> "https://pic.sogou.com/ris?flag=1&drag=0&query=$imageURL"
                        SERVICE_BING -> "https://cn.bing.com/images/search?view=detailv2&iss=sbi&FORM=SBIVSP&sbisrc=UrlPaste&q=imgurl:$imageURL&idpbck=1"
                        // Yandex format parameter in JSON
                        SERVICE_YANDEX -> ""
                        else -> ""
                    }
                    status.visibility = TextView.GONE
                    webView.visibility = WebView.VISIBLE
                    webView.loadUrl(result!!)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()

        webView.isFocusableInTouchMode = true
        webView.requestFocus()
        webView.setOnKeyListener(View.OnKeyListener { v, keyCode, event ->
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

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(RESULT_LOADED, resultLoaded)
        webView.saveState(outState)
    }

    private suspend fun uploadImage(imageUri: Uri, sampleSize: Int, serviceType: Int): String {

        return withContext(Dispatchers.IO) {
            var line:String? = null
            var conn:HttpsURLConnection? = null
            var imageStream:InputStream? = null

            try {
                val crlf = "\r\n"
                val boundary = "====" + System.currentTimeMillis()

                val formDataName = FORM_DATA_NAME[serviceType]
                conn = URL(BASE_URL[serviceType]).openConnection() as HttpsURLConnection
                //conn = URL("https://httpbin.org/post").openConnection() as HttpURLConnection
                //formDataName = "image"

                // Http POST header
                conn.useCaches = false
                conn.doOutput = true
                conn.requestMethod = "POST"
                conn.instanceFollowRedirects = false
                conn.setRequestProperty("User-Agent", USER_AGENT_GOOGLE_NEXUS)
                conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")

                // Http request body
                val outputStream = conn.outputStream
                val writer = outputStream.writer()

                // Sogou needs another parameter: flag=1
                if (serviceType == SERVICE_SOGOU) {
                    writer.append("--$boundary$crlf")
                        .append("Content-Disposition: form-data; name=\"flag\"$crlf$crlf")
                        .append("1$crlf")
                }

                // Bing needs these two extra parameters: imgurl=, cbir=sbi
                if (serviceType == SERVICE_BING) {
                    writer.append("--$boundary$crlf")
                        .append("Content-Disposition: form-data; name=\"imgurl\"$crlf$crlf")
                        .append("$crlf")
                    writer.append("--$boundary$crlf")
                        .append("Content-Disposition: form-data; name=\"cbir\"$crlf$crlf")
                        .append("sbi$crlf")
                }

                // The actual image file
                val newOption = BitmapFactory.Options().apply {
                    inJustDecodeBounds = false
                    inSampleSize = sampleSize
                }
                imageStream = activity?.contentResolver?.openInputStream(imageUri)

                if (serviceType == SERVICE_BING) {
                    // Bing use BASE64 encoding
                    writer.append("--$boundary$crlf")
                        .append("Content-Disposition: form-data; name=\"$formDataName\"$crlf")
                        .append(crlf).flush()

                    BitmapFactory.decodeStream(imageStream, null, newOption)
                        ?.compress(Bitmap.CompressFormat.JPEG, 70, Base64.getEncoder().wrap(conn.outputStream))
                }
                else {
                    writer.append("--$boundary$crlf")
                        .append("Content-Disposition: form-data; name=\"$formDataName\"; filename=\"r.jpg\"$crlf")
                        .append("Content-Type: image/jpeg$crlf")
                        .append(crlf).flush()

                    //imageInputStream.copyTo(outputStream, 4096)
                    //val inputStream = contentResolver.openInputStream(imageUri)!!
                    /*
                    val buffer = ByteArray(4096)
                    var byteRead = imageStream.read(buffer)
                    while (byteRead != -1) {
                        outputStream.write(buffer, 0, byteRead)
                        byteRead = imageStream.read(buffer)
                    }
                    */
                    BitmapFactory.decodeStream(imageStream, null, newOption)?.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
                }
                imageStream?.close()
                writer.append("$crlf--$boundary--$crlf").flush()
                writer.close()
                outputStream.close()

                // Processing response
                val reader = conn.inputStream.bufferedReader()
                when(serviceType) {
                    SERVICE_GOOGLE, SERVICE_SOGOU, SERVICE_BING -> {
                        // Google, Sogou, Bing all response with 302 redirect
                        if (conn.responseCode == HttpURLConnection.HTTP_MOVED_TEMP) {
                            line = conn.headerFields["Location"]?.get(0)
                        }
                    }
                    SERVICE_YANDEX -> {
                        if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                            while(true) {
                                line = reader.readLine()
                                if (line == null) break
                                if (line.indexOf("cbir_id", 0, true) > 0) break
                            }
                        }
                    }
                    /*
                    SERVICE_TINEYE -> {
                        if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                            while(true) {
                                line = reader.readLine()
                                if (line == null) break
                                if (line.indexOf("base_url", 0, true) > 0) break
                            }
                        }
                    }
                    SERVICE_PAILITAO -> {
                        Log.w("************************", conn.responseCode.toString())
                        // Pailitao response with a JSON object, like this
                        // {"status":1,"name":"O1CN01qm2A9q1Hyo9hvTIYB_!!0-imgsearch.jpg","url":"//g-search3.alicdn.com/img/bao/uploaded/i4/O1CN01qm2A9q1Hyo9hvTIYB_!!0-imgsearch.jpg","error":false}
                        if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                            while(true) {
                                line = reader.readLine()
                                Log.w("******************************", line)
                                if (line == null) break
                                if (line.length > 1) {
                                    val json = JSONObject(line)
                                    line = json.getString("name")
                                    break
                                }
                            }
                        }
                        else {
                            while(true) {
                                val l = reader.readLine()
                                if (l == null) break
                                else Log.w("********************************", l)
                            }
                        }
                    }
                     */
                }
                reader.close()
            } catch (e: Exception) {
                e.printStackTrace()
                line = "Error: $e"
            }
            finally {
                imageStream?.close()
                conn?.disconnect()
            }

            if (line != null) {
                if (line.startsWith("Error")) line
                else {
                    // Besides Yandex, all other services return quoted URL
                    if (serviceType != SERVICE_YANDEX) line = line.substringAfter('"').substringBefore('"')

                    when (serviceType) {
                        SERVICE_GOOGLE -> line

                        // Sogou redirect to http rather than https
                        SERVICE_SOGOU -> line.replace("http://", "https://")

                        // Bing returned URL doesn't have base
                        SERVICE_BING -> "https://cn.bing.com" + line

                        // Yandex return a JSON object, the following is a dirty hack to strip out what we need: cbir_id
                        SERVICE_YANDEX -> "https://yandex.com/images/search?family=yes&rpt=imageview&" + line.subSequence(line.indexOf("cbir_id", 0, true), line.indexOf('&'))

                        // TinEye return a url like "/result/xxxxxxxxxxxxxxxxxxxxxx"
                        //SERVICE_TINEYE -> BASE_URL[SERVICE_TINEYE] + line.substringAfterLast('/')

                        //SERVICE_PAILITAO -> "https://www.pailitao.com/search?q=+&imgfile=&tfsid=" + Uri.parse(line) + "&app=imgsearch"
                        else -> line
                    }
                }
            }
            else ""
        }
    }

    // Calculate inSampleSize for image decoding, the longest side length is around MAX_SIDE_LENGTH, so that the decoded size is relative small, but
    // enough for image recognition
    private fun getSampleSize(currentWidth: Int, requiredWidth: Int): Int {
        var inSampleSize = 1

        if (currentWidth > requiredWidth) {
            val halfWidth = currentWidth / 2
            while (halfWidth / inSampleSize >= requiredWidth) inSampleSize *= 2
        }
        return inSampleSize
    }

    companion object {
        const val SERVICE_KEY = "SERVICE_KEY"
        const val RESULT_LOADED = "RESULT_LOADED"
        const val MAX_SIDE_LENGTH:Int = 256
        const val USER_AGENT_CHROME = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.36"
        const val USER_AGENT_GOOGLE_NEXUS = "Mozilla/5.0 (Linux; U; Android-4.0.3; en-us; Galaxy Nexus Build/IML74K) AppleWebKit/535.7 (KHTML, like Gecko) CrMo/16.0.912.75 Mobile Safari/535.7"

        const val SERVICES_TOTAL = 5
        const val SERVICE_GOOGLE = 0
        const val SERVICE_SOGOU = 1
        const val SERVICE_BING = 2
        const val SERVICE_YANDEX = 3
        //const val SERVICE_TINEYE = 4
        //const val SERVICE_PAILITAO = 5
        val BASE_URL = arrayOf("https://www.google.com/searchbyimage/upload",
                                "https://pic.sogou.com/ris_upload",
                                "https://cn.bing.com/images/search?view=detailv2&iss=sbiupload&FORM=SBIVSP",
                                "https://yandex.com/images/search?family=yes&rpt=imageview&format=json&request=%7B%22blocks%22%3A%5B%7B%22block%22%3A%22b-page_type_search-by-image__link%22%7D%5D%7D",
                                "https://tineye.com/search/",
                                "https://www.pailitao.com/image")
        val FORM_DATA_NAME = arrayOf("encoded_image", "pic_path", "imageBin", "upfile", "image", "imgfile")

        fun newInstance(arg: Int) = ReverseImageSearchFragment().apply {arguments = Bundle().apply {putInt(SERVICE_KEY, arg)}}
    }
}