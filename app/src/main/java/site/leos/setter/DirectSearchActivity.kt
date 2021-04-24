package site.leos.setter

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.android.synthetic.main.directsearch_activity.*

class DirectSearchActivity : AppCompatActivity() {
    private lateinit var query: TextInputEditText
    private lateinit var queryLayout: TextInputLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.directsearch_activity)

        queryLayout = findViewById(R.id.edit_layout)
        query = findViewById<TextInputEditText>(R.id.edit_query).apply {
            requestFocus()
            setOnEditorActionListener { _, id, _ ->
                if (id == EditorInfo.IME_ACTION_SEARCH || id == EditorInfo.IME_NULL) searchIt(query.text.toString(), true)
                else false
            }
        }

        findViewById<MaterialButton>(R.id.meta_search).setOnClickListener {
            if (query.text!!.isNotEmpty()) searchIt(query.text.toString(), true)
            else queryLayout.hint = getString(R.string.edit_search_hint)
        }
        findViewById<MaterialButton>(R.id.translate).setOnClickListener {
            if (query.text!!.isNotEmpty()) searchIt(query.text.toString(), false)
            else queryLayout.hint = getString(R.string.edit_translate_hint)
        }
    }

    private fun searchIt(query: String, meta: Boolean) : Boolean {
        if (query.isNotBlank()) {
            // Hide soft keyboard
            (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(edit_query.windowToken, 0)

            if (meta) {
                if (android.util.Patterns.WEB_URL.matcher(query).matches()) {
                    val url = if (query.startsWith("http")) query else "https://$query"
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                }
                else startActivity(Intent(this, WebSearchActivity::class.java)
                        .putExtra(Intent.EXTRA_PROCESS_TEXT, query).putExtra(WebSearchActivity.META, true)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                )
            } else {
                /*
                // Default search enabled
                val searchIntent = Intent().setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK).setAction(Intent.ACTION_WEB_SEARCH).putExtra(SearchManager.QUERY, query)

                // Firefox does NOT support SearchManager.QUERY extra!!!
                val defaultBrowser = packageManager.resolveActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://example.com")), PackageManager.MATCH_DEFAULT_ONLY)
                if (defaultBrowser?.activityInfo?.packageName.equals(WebSearchActivity.FIREFOX))
                    searchIntent.setComponent(ComponentName(WebSearchActivity.FIREFOX, "org.mozilla.gecko.BrowserApp")).setData(Uri.parse(query))

                startActivity(searchIntent)
                */
                startActivity(Intent(this, TranslationActivity::class.java).putExtra(TranslationActivity.KEY_QUERY, query).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK))
            }

            finish()
            return true
        }
        else return false
    }
}
