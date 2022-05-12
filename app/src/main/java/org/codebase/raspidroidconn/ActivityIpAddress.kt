package org.codebase.raspidroidconn

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.inputmethod.InputMethodManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.android.synthetic.main.activity_ip_address.*
import kotlinx.android.synthetic.main.activity_main.*

class ActivityIpAddress : AppCompatActivity() {

    private val appGlobals = AppGlobals()
    private var savedIP: String? = ""
    private var nextHalfIP: String? = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ip_address)

        title = "Local IP Address"

        savedIP = appGlobals.getValueString("Full_IP_Address")
        nextHalfIP = appGlobals.getValueString("Next_Half_IP")
        ipAddressEditTextId.setText(nextHalfIP)
        savedIPTextId.text = savedIP

        /*** constraint layout id***/
        ipAddressConstraintLayout.setOnClickListener {
            hideSoftKeyboard()
            ipAddressEditTextId.clearFocus()
        }

        saveIPMaterialButton.setOnClickListener {
            saveIPAddress()
            hideSoftKeyboard()
            ipAddressEditTextId.clearFocus()
        }

        setLightStateButtonId.setOnClickListener {
            navigateToLightsPage()
        }
    }

    private fun saveIPAddress() {
        val tp = textInputLayout.prefixText.toString().trim()
        Log.e("tp", tp)
        val ipAddressText = ipAddressEditTextId.text.toString().trim()

        if (ipAddressText.isEmpty()) {
            ipAddressEditTextId.error = "Please enter the IP address first"
        } else {
            appGlobals.saveString("Full_IP_Address", "$tp$ipAddressText")
            appGlobals.saveString("Next_Half_IP", ipAddressText)

            nextHalfIP = appGlobals.getValueString("Next_Half_IP")
            savedIP = appGlobals.getValueString("Full_IP_Address")
            savedIPTextId.text = savedIP
            ipAddressEditTextId.setText(nextHalfIP)
        }
    }

    private fun navigateToLightsPage() {
        if (savedIPTextId.text.isEmpty()) {
            val builder = MaterialAlertDialogBuilder(this)
            builder.setTitle("Require IP address")
            builder.setMessage("Please save the IP address first!")
            builder.setPositiveButton("Ok") {_, _ ->
                builder.create().dismiss()
            }
            builder.create().show()
        } else {
            val intent = Intent(applicationContext, MainActivity::class.java)
            startActivity(intent)
        }
    }
    // extension function to hide soft keyboard programmatically
    private fun Activity.hideSoftKeyboard() {
        (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).apply {
            hideSoftInputFromWindow(currentFocus?.windowToken, 0)
        }
    }
}