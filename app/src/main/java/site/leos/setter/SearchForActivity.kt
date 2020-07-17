package site.leos.setter

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.searchfor_activity.*
import java.util.regex.Pattern

class SearchForActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.searchfor_activity)

        edit_query.run {
            requestFocus()
            setOnEditorActionListener { _, id, _ ->
                if (id == EditorInfo.IME_ACTION_SEARCH || id == EditorInfo.IME_NULL) searchIt(edit_query.text.toString())
                else false
            }
        }
    }

    private fun searchIt(query: String) : Boolean {
        if (query.isNotBlank()) {
            // Hide soft keyboard
            (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(edit_query.windowToken, 0)

            val mPattern = Pattern.compile("^(http://www.|https://www.|http://|https://)?[a-z0-9]+([-.][a-z0-9]+)*.[a-z]{2,5}(:[0-9]{1,5})?(/.*)?$")
            if (mPattern.matcher(query).matches()) {
                val url = if (query.startsWith("http")) query else "https://$query"
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            }
            else startActivity(Intent(this, WebSearchActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK).putExtra(Intent.EXTRA_PROCESS_TEXT, query))

            finish()
            return true
        }
        else return false
    }
}