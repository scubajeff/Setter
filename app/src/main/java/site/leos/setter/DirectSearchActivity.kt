/*
 *   Copyright 2019 Jeffrey Liu (scubajeffrey@criptext.com)
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

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

class DirectSearchActivity : AppCompatActivity() {
    private lateinit var queryEdit: TextInputEditText
    private lateinit var queryLayout: TextInputLayout
    private lateinit var metaButton: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.directsearch_activity)

        metaButton = findViewById(R.id.meta_search)
        queryLayout = findViewById(R.id.edit_layout)
        queryEdit = findViewById<TextInputEditText>(R.id.edit_query).apply {
            requestFocus()
            setOnEditorActionListener { _, id, _ ->
                if (id == EditorInfo.IME_ACTION_SEARCH || id == EditorInfo.IME_NULL) searchIt(queryEdit.text.toString(), metaButton.isChecked)
                else false
            }
        }

        findViewById<MaterialButton>(R.id.meta_search).setOnClickListener {
            if (queryEdit.text!!.isNotEmpty()) searchIt(queryEdit.text.toString(), true)
            else queryLayout.hint = getString(R.string.edit_search_hint)
        }
        findViewById<MaterialButton>(R.id.translate).setOnClickListener {
            if (queryEdit.text!!.isNotEmpty()) searchIt(queryEdit.text.toString(), false)
            else queryLayout.hint = getString(R.string.edit_translate_hint)
        }
    }

    private fun searchIt(query: String, meta: Boolean) : Boolean {
        if (query.isNotBlank()) {
            // Hide soft keyboard
            (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(queryEdit.windowToken, 0)

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