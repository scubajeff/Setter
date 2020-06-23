package site.leos.setter

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.os.SystemClock.sleep
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

class ReverseImageSearchActivity : AppCompatActivity() {
    @SuppressLint("LongLogTag")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        var wait = 1
        while (wait < 5) {
            if (isNetworkActive()) {
                if ((intent?.action == Intent.ACTION_SEND) && (intent.type?.startsWith("image/") == true)) {
                    // Is a single image share intent
                    (intent.getParcelableExtra<Parcelable>(Intent.EXTRA_STREAM) as? Uri)?.let {
                        //lifecycleScope.launch {
                        //GlobalScope.launch(Dispatchers.Main) {
                        CoroutineScope(Dispatchers.IO).launch(Dispatchers.IO) {
                            val option = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                            BitmapFactory.decodeStream(contentResolver.openInputStream(it),null,option)
                            val iis = contentResolver.openInputStream(it)
                            if (iis != null) {
                                val fingerprint = uploadImage(iis,getSampleSize(option.outWidth, MAX_IMAGE_WIDTH))
                                iis.close()
                                if (fingerprint != "") startActivity(Intent(Intent.ACTION_VIEW,Uri.parse(fingerprint)))
                            }
                        }
                    }
                }
                break
            } else {
                sleep((wait * 100).toLong())
                wait *= 2
                if (wait > 4) Log.w(TAG,"No active Network")
            }
        }

        finish()
        return
    }

    private fun isNetworkActive(): Boolean {
        return (getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager).isDefaultNetworkActive
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private suspend fun uploadImage(imageStream: InputStream, sampleSize: Int): String {

        return withContext(Dispatchers.IO) {
            var line: String? = null
            try {
                val crlf = "\r\n"
                val boundary = "====" + System.currentTimeMillis()

                val conn = URL("https://www.google.com/searchbyimage/upload").openConnection() as HttpURLConnection
                //val conn = URL("https://httpbin.org/post").openConnection() as HttpURLConnection
                //val conn = URL("https://tineye.com/search").openConnection() as HttpURLConnection

                conn.useCaches = false
                conn.doOutput = true
                conn.requestMethod = "POST"
                conn.instanceFollowRedirects = false
                conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; U; Android-4.0.3; en-us; Galaxy Nexus Build/IML74K) AppleWebKit/535.7 (KHTML, like Gecko) CrMo/16.0.912.75 Mobile Safari/535.7")
                conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")

                val outputStream = conn.outputStream
                val writer = outputStream.writer()
                writer.append("--$boundary$crlf")
                        .append("Content-Disposition: form-data; name=\"encoded_image\"$crlf")
//                        .append("Content-Disposition: form-data; name=\"image\"$crlf")
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
                val newOption = BitmapFactory.Options().apply {
                    inJustDecodeBounds = false
                    inSampleSize = sampleSize
                }
                BitmapFactory.decodeStream(imageStream, null, newOption)?.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
                //inputStream.close()
                writer.append("$crlf--$boundary--$crlf").flush()
                writer.close()
                outputStream.close()

                if (conn.responseCode == HttpURLConnection.HTTP_MOVED_TEMP) {
                    val reader = conn.inputStream.bufferedReader()!!

                    while (true){
                        line = reader.readLine()
                        if (line == null) break
                        if (line.indexOf("HREF=") > 0) break
                    }
                    reader.close()
                }
                conn.disconnect()
            } catch (e: Exception) {
                e.printStackTrace()
            }

            line?.substringAfter('"')?.substringBefore('"') ?: ""
        }
    }

    private fun getSampleSize(currentWidth: Int, requiredWidth: Int): Int {
        var inSampleSize = 1

        if (currentWidth > requiredWidth) {
            val halfWidth = currentWidth / 2
            while (halfWidth / inSampleSize >= requiredWidth) inSampleSize *= 2
        }

        return inSampleSize
    }

    companion object {
        const val TAG = "ReverseImageSearchActivity"
        const val MAX_IMAGE_WIDTH:Int = 256
    }
}