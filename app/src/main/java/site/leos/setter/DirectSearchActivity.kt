package site.leos.setter

import android.app.SearchManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import com.google.android.material.textfield.TextInputEditText
import kotlinx.android.synthetic.main.directsearch_activity.*

class DirectSearchActivity : AppCompatActivity() {
    private lateinit var query: TextInputEditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.directsearch_activity)

        query = findViewById(R.id.edit_query)
        query_type_rg.apply { check(if (PreferenceManager.getDefaultSharedPreferences(context).getBoolean(getString(R.string.meta_search_key), true)) R.id.meta_query else R.id.browser_query) }
        meta_query.setOnClickListener { if (query.text!!.isNotEmpty()) searchIt(query.text.toString(), true) }
        browser_query.setOnClickListener { if (query.text!!.isNotEmpty()) searchIt(query.text.toString(), false) }

        query.run {
            requestFocus()
            setOnEditorActionListener { _, id, _ ->
                if (id == EditorInfo.IME_ACTION_SEARCH || id == EditorInfo.IME_NULL)
                    searchIt(query.text.toString(), query_type_rg.checkedRadioButtonId==R.id.meta_query)
                else false
            }
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
                // Default search enabled
                val searchIntent = Intent().setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK).setAction(Intent.ACTION_WEB_SEARCH).putExtra(SearchManager.QUERY, query)

                // Firefox does NOT support SearchManager.QUERY extra!!!
                val defaultBrowser = packageManager.resolveActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://example.com")), PackageManager.MATCH_DEFAULT_ONLY)
                if (defaultBrowser?.activityInfo?.packageName.equals(WebSearchActivity.FIREFOX))
                    searchIntent.setComponent(ComponentName(WebSearchActivity.FIREFOX, "org.mozilla.gecko.BrowserApp")).setData(Uri.parse(query))

                startActivity(searchIntent)
            }

            finish()
            return true
        }
        else return false
    }
}
