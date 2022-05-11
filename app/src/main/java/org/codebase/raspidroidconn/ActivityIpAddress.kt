package org.codebase.raspidroidconn

import android.app.Activity
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.inputmethod.InputMethodManager
import kotlinx.android.synthetic.main.activity_ip_address.*
import kotlinx.android.synthetic.main.activity_main.*

class ActivityIpAddress : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ip_address)

        /*** constraint layout id***/
        ipAddressConstraintLayout.setOnClickListener {
            hideSoftKeyboard()
            ipAddressEditTextId.clearFocus()
        }
        ipAddressEditTextId.setOnFocusChangeListener { view, b ->

        }
    }

    // extension function to hide soft keyboard programmatically
    private fun Activity.hideSoftKeyboard() {
        (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).apply {
            hideSoftInputFromWindow(currentFocus?.windowToken, 0)
        }
    }
}