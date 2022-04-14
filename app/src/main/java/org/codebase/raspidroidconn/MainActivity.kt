package org.codebase.raspidroidconn

import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import io.crossbar.autobahn.wamp.Client
import io.crossbar.autobahn.wamp.Session
import io.crossbar.autobahn.wamp.types.*
import kotlinx.android.synthetic.main.activity_main.*


@RequiresApi(Build.VERSION_CODES.N)

class MainActivity : AppCompatActivity() {

    private val appGlobals = AppGlobals()
    private var buttonText = ""
    private var switchStatus:Boolean? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Log.e("text", lightOnButtonId.text.toString())
        lightSwitchOnOffId.setOnCheckedChangeListener { compoundButton, checked ->
            publishSession()
            if (compoundButton.isChecked) {
                compoundButton.text = "On"
                buttonText = compoundButton.text.toString()
            } else {
                compoundButton.text = "Off"
                buttonText = compoundButton.text.toString()
            }
        }
    }

    private fun demonstratePublish(session: Session, details: SessionDetails?){
        Log.e("connec", details!!.realm)

//        Log.e("connec", details.realm)

        val args = listOf<Any>(details.realm, details.authmethod, buttonText)
        val pubFuture = session.publish("org.codebase", args)

        pubFuture.thenAccept { publication: Publication? ->
            println("Published successfully $publication")
        }
        pubFuture.exceptionally { throwable ->
            Log.e("thr", throwable.message.toString())
            throwable.printStackTrace()
            null
        }
    }

    private fun publishSession() {
        val wampSession = Session()
//        wampSession.addOnJoinListener(this::demonstrateSubscribe)
        wampSession.addOnJoinListener { session, details ->
            demonstratePublish(session, details)
        }

        val client = Client(wampSession, "ws://192.168.100.127:8080/ws", "realm1")
        Log.e("connec", "1")
        val test = client.connect().whenComplete { exitInfo, throwable ->
            if (throwable != null) {
                Log.e("Exit", "$exitInfo")
            }
        }

        if (test.isDone) {
            Log.e("heellooo", "2")
        }
        Log.e("connec", "2")
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