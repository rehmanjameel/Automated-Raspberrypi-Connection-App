package org.codebase.raspidroidconn

import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.inputmethod.InputMethodManager
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.crossbar.autobahn.wamp.Client
import io.crossbar.autobahn.wamp.Session
import io.crossbar.autobahn.wamp.types.*
import kotlinx.android.synthetic.main.activity_main.*


@RequiresApi(Build.VERSION_CODES.N)

class MainActivity : AppCompatActivity() {

    private var wampSession = Session()
    private val appGlobals = AppGlobals()
    private var buttonText = ""
    private val SWITCH_STATUS = "switch_status"
    private val LIGHT_STATUS = "light_on"
    private var switchStatus:Boolean? = null
    private var lightStatus:Boolean? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        /*** creating connection on app start ***/
        publishSession()

        switchStatus = appGlobals.getValueBoolean(SWITCH_STATUS)
        lightStatus = appGlobals.getValueBoolean(LIGHT_STATUS)
        lightSwitchOnOffId.isChecked = switchStatus as Boolean

        if (lightStatus as Boolean) {
            lightOnOffImageId.setImageResource(R.drawable.light_on)
        } else {
            lightOnOffImageId.setImageResource(R.drawable.light_off)
        }
        /*** Switch button checked change listener to check the state of button
         * and publishing button current state text to subscribed topic ***/
        lightSwitchOnOffId.setOnCheckedChangeListener { compoundButton, checked ->
            if (compoundButton.isChecked) {
                buttonText = "On"
                lightOnOffImageId.setImageResource(R.drawable.light_on)
                appGlobals.saveBoolean(SWITCH_STATUS, true)
                appGlobals.saveBoolean(LIGHT_STATUS, true)
                demonstratePublish()
            } else {
                buttonText = "Off"
                lightOnOffImageId.setImageResource(R.drawable.light_off)
                appGlobals.saveBoolean(SWITCH_STATUS, false)
                appGlobals.saveBoolean(LIGHT_STATUS, false)
                demonstratePublish()
            }
        }

        /*** constraint layout id***/
        constraintLayoutId.setOnClickListener {
            hideSoftKeyboard()
        }
    }

    /*** publishing button text to the subscribed topic org.codebase
     * on raspberrypi python code ***/
    private fun demonstratePublish(){
//        val args = listOf<Any>(details.realm, details.authmethod, buttonText)
        Log.e("butText", buttonText)
        if (wampSession.isConnected) {
            val pubFuture = wampSession.publish("org.codebase", buttonText)

            pubFuture.thenAccept { publication: Publication? ->
                println("Published successfully $publication")
            }
            pubFuture.exceptionally { throwable ->
                Log.e("thr", throwable.message.toString())
                throwable.printStackTrace()
                null
            }
        } else {
            val builder = MaterialAlertDialogBuilder(this@MainActivity)
            builder.setTitle("Wamp Error!")
            builder.setMessage("Session not connected or invalid Ip address")
            builder.setPositiveButton("Ok") {_, _ ->
                builder.create().dismiss()
            }
            builder.create().show()

            if (lightSwitchOnOffId.textOn == "On") {
                lightSwitchOnOffId.isChecked = false
            } else if (lightSwitchOnOffId.textOff == "Off") {
                lightSwitchOnOffId.isChecked = true
            }

            Log.e("Error", "Session not connected or invalid Ip address")
        }
    }

    /*** private function using for creating the client connection to publish the events
     * over networks with wamp connection ***/
    private fun publishSession() {
        wampSession = Session()
//        wampSession.addOnJoinListener(this::demonstrateSubscribe)
//        wampSession.addOnJoinListener { session, details ->
//            demonstratePublish(session, details)
//        }

        val client = Client(wampSession, "ws://192.168.100.210:8080/ws", "realm1")
        client.connect().whenComplete { exitInfo, throwable ->
            if (throwable == null) {
                Log.e("Client", "Connection Created")
            }
        }
    }

    // extension function to hide soft keyboard programmatically
    private fun Activity.hideSoftKeyboard() {
        (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).apply {
            hideSoftInputFromWindow(currentFocus?.windowToken, 0)
        }
    }

//    private fun demonstrateSubscribe(session: Session, details: SessionDetails?) {
//        // Subscribe to topic to receive its events.
//        val subFuture: CompletableFuture<Subscription> = session.subscribe("org.codebase",
//            TriConsumer { args: List<Any>, kwargs: Map<String, Any>, details: EventDetails ->
//                onEvent(args, kwargs, details)
//            })
//        subFuture.whenComplete(BiConsumer<Subscription, Throwable> { subscription: Subscription, throwable: Throwable? ->
//            if (throwable == null) {
//                // We have successfully subscribed.
//                println("Subscribed to topic " + subscription.topic)
//            } else {
//                // Something went bad.
//                throwable.printStackTrace()
//            }
//        })
//    }
//
//    private fun onEvent(args: List<Any>, kwargs: Map<String, Any>, details: EventDetails) {
//        println(String.format("Got event: %s", args[0]))
//    }
}