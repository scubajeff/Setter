package site.leos.setter

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Point
import android.os.Bundle
import android.view.Gravity
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager

class DictActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val query = intent.getStringExtra(if (intent.action == Intent.ACTION_SEND) Intent.EXTRA_TEXT else Intent.EXTRA_PROCESS_TEXT) ?: ""

        if (query.isEmpty()) {
            finish()
            return
        }

        if (isColorDictAvailable()) {
            // We can do dictionary lookup only if ColorDict is installed
            run cjk@{
                if (!query.contains(' ')) {
                    // If no space detected in query string, that must be a word or in some language with no delimiter for words, like CJK language

                    // If CJK character detected, that could well means it's in CJK language, go to translation instead of word look up
                    val qLength = query.length
                    var detectedCJKChar = false
                    var i = 0
                    var cp: Int
                    while (i < qLength) {
                        cp = query.codePointAt(i)
                        i += Character.charCount(cp)
                        if (Character.UnicodeScript.of(cp) == Character.UnicodeScript.HAN) {
                            detectedCJKChar = true
                            break
                        }
                    }
                    if (detectedCJKChar && qLength > 4) return@cjk

                    // Not CJK, no space, that must be a word
                    val colorDictIntent = Intent("colordict.intent.action.SEARCH")

                    if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean(getString(R.string.colordict_fullscreen_key), false)) {
                        colorDictIntent.putExtra("EXTRA_FULLSCREEN", true)
                    } else {
                        val size = Point()
                        @Suppress("DEPRECATION")
                        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.R) windowManager.defaultDisplay.getSize(size)
                        else display?.getRealSize(size)
                        colorDictIntent.putExtra("EXTRA_FULLSCREEN", false)
                                        .putExtra("EXTRA_HEIGHT", size.y / 2)
                                        .putExtra("EXTRA_MARGIN_LEFT", size.x / 20)
                                        .putExtra("EXTRA_MARGIN_RIGHT", size.x / 20)
                                        .putExtra("EXTRA_GRAVITY", Gravity.BOTTOM)
                    }

                    colorDictIntent.putExtra("EXTRA_QUERY", query)
                    startActivity(colorDictIntent)

                    finish()
                    return
                }
            }
        }

        startActivity(Intent(this, TranslationActivity::class.java).putExtra(TranslationActivity.KEY_QUERY, query))

        finish()
    }

    private fun isColorDictAvailable() : Boolean {
        return baseContext.packageManager.resolveActivity(Intent("colordict.intent.action.SEARCH"), PackageManager.MATCH_DEFAULT_ONLY) != null
    }
}
