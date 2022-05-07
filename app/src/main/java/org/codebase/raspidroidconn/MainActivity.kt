package org.codebase.raspidroidconn

import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.MaterialTimePicker.INPUT_MODE_KEYBOARD
import com.google.android.material.timepicker.TimeFormat
import io.crossbar.autobahn.wamp.Client
import io.crossbar.autobahn.wamp.Session
import io.crossbar.autobahn.wamp.types.*
import kotlinx.android.synthetic.main.activity_main.*
import java.lang.String
import java.time.LocalDateTime
import java.util.*
import java.util.concurrent.CompletableFuture


@RequiresApi(Build.VERSION_CODES.N)

class MainActivity : AppCompatActivity() {

    private lateinit var radioGroup: RadioGroup
    private lateinit var radioButtons: RadioButton
    private var wampSession = Session()
    private val appGlobals = AppGlobals()
    private val SWITCH_STATUS = "switch_status"
    private val LIGHT_STATUS = "light_on"

    private var switchStatus:Boolean? = null
    private var lightStatus:Boolean? = null
    private var selectedHour: Int? = null
    private var selectedMinute: Int? = null

    private var buttonText = ""
    private var hourAsText = ""
    private var minuteAsText = ""
    private var radioButtonText = ""
    private var savedOnTime = ""
    private var savedOffTime = ""

    @RequiresApi(Build.VERSION_CODES.O)
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

        lightOn_time_button.setOnClickListener {
            showTimePicker()
        }

        lightOff_time_button.setOnClickListener {
            showOffTimePicker()
        }

        getRadioButtonText()

        savedOnTime = appGlobals.getValueString("LightOnTime").toString()

        savedOffTime = appGlobals.getValueString("LightOffTime").toString()
        if (savedOnTime.isBlank() && savedOffTime.isBlank()) {
            Log.e("here", "is")
            lightOn_time_text.text = "__"
            lightOff_time_text.text = "__"
        } else {
            Log.e("Here", "not")
            lightOn_time_text.text = savedOnTime
            lightOff_time_text.text = savedOffTime
        }
    }

    /*** publishing button text to the subscribed topic org.codebase
     * on raspberrypi python code ***/
    private fun demonstratePublish(){
//        val args = listOf<Any>(details.realm, details.authmethod, buttonText)
        Log.e("butText", buttonText)
        if (wampSession.isConnected) {
            val pubFuture = wampSession.publish("org.codebase", buttonText.lowercase())

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

    private fun callProcedure() {
        if (wampSession.isConnected) {
            val args: MutableList<Any> = ArrayList()
//            args.add(var1)
//            args.add(var2)
//            args.add(var3)
            val callProc: CompletableFuture<CallResult> = wampSession.call("org.codebase.sys.light_on_off",
                args, "", true)
            callProc.thenAccept { callResult ->
                println(String.format("Call result: %s", callResult.results[0]))
            }
            callProc.exceptionally { throwable ->
                Log.e("thr", throwable.message.toString())
                throwable.printStackTrace()
                null
            }
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

        val client = Client(wampSession, "ws://192.168.100.163:8080/ws", "realm1")
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

    @RequiresApi(Build.VERSION_CODES.O)
    private fun showTimePicker(){
        val hour = selectedHour ?: LocalDateTime.now().hour
        /*** '?:' is an Elvis Operator
         * If first operand isn't null, then it will be returned.
         * If it is null, then the second operand will be returned.
         * it ensure that expression won't return any null value ***/
        val minutes = selectedMinute ?: LocalDateTime.now().minute

        MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_24H)
            .setHour(hour)
            .setMinute(minutes)
            .setInputMode(INPUT_MODE_KEYBOARD)
            .build()
            .apply { addOnPositiveButtonClickListener {
                onTimeSelected(this.hour, this.minute)
            }
            }.show(supportFragmentManager, MaterialTimePicker::class.java.canonicalName)
    }

    private fun onTimeSelected(hour: Int, minute: Int) {
        selectedHour = hour
        selectedMinute = minute
        hourAsText = if (hour < 10) "0$hour" else hour.toString()
        minuteAsText = if (minute < 10) "0$minute" else minute.toString()
        val onTime = "$hourAsText:$minuteAsText"

        appGlobals.saveString("LightOnTime", onTime)
        savedOnTime = appGlobals.getValueString("LightOnTime").toString()
        lightOn_time_text.text = savedOnTime
        lightOff_time_button.backgroundTintList = getColorStateList(R.color.purple_700)
        lightOff_time_button.isClickable = true

//        "$hourAsText:$minuteAsText".also {
//            output.text = it}

        /*Button bt = (Button)sender
        val view = View(this@MainActivity)
        Log.e("buton", (view.id == R.id.lightOff_time_button).toString())
        val button = view as Button
        Log.e("Here", "out of when")

        when (view.id) {
            R.id.lightOn_time_button -> {
                Log.e("Here", "Here in when")
                "$hourAsText:$minuteAsText".also {
                    output.text = it }
            }
            R.id.lightOff_time_button -> {
                "$hourAsText:$minuteAsText".also {
                    lightOff_time_text.text = it }
            }
        }*/

    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun showOffTimePicker(){
        val hour = selectedHour ?: LocalDateTime.now().hour
        val minutes = selectedMinute ?: LocalDateTime.now().minute

        MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_24H)
            .setHour(hour)
            .setMinute(minutes)
            .setInputMode(INPUT_MODE_KEYBOARD)
            .build()
            .apply { addOnPositiveButtonClickListener { offTimeSelected(this.hour, this.minute)
            }
            }.show(supportFragmentManager, MaterialTimePicker::class.java.canonicalName)
    }

    private fun offTimeSelected(hour: Int, minute: Int) {
        selectedHour = hour
        selectedMinute = minute
        hourAsText = if (hour < 10) "0$hour" else hour.toString()
        minuteAsText = if (minute < 10) "0$minute" else minute.toString()
        val offTime = "$hourAsText:$minuteAsText"
        appGlobals.saveString("LightOffTime", offTime)

        savedOffTime = appGlobals.getValueString("LightOffTime").toString()
        lightOff_time_text.text = savedOffTime
        saveTimeButtonId.visibility = View.VISIBLE

//        "$hourAsText:$minuteAsText".also {
//            lightOff_time_text.text = it }
    }

    private fun getRadioButtonText() {
        //Getting radiobutton text on the basis of radio button id
        val sp : Int = appGlobals.getValueInt("sp")
        when (sp) {
            1 -> {
                sunTimeId.isChecked = true
                disableTimeButtons()
            }
            2 -> {
                manualTimeId.isChecked = true
                enableTimeButtons()
            }
            3 -> {
                directOnOffId.isChecked = true
                disableTimeButtons()
            }
        }
        radioGroupId.setOnCheckedChangeListener { radioGroup, id ->
            val radioButtonId: Int = radioGroup.checkedRadioButtonId
            if (radioButtonId != -1) {
                radioButtons = findViewById(radioButtonId)
                println(radioButtons)
            }
            radioButtonText = radioButtons.text.toString()

            when (id) {
                R.id.sunTimeId -> {
                    appGlobals.saveInt("sp", 1)
                    lightSwitchOnOffId.isClickable = false
                    disableTimeButtons()
                    buttonText = listOf<Any>(hourAsText, minuteAsText).toString()
                    println("manual $buttonText")
                }
                R.id.manualTimeId -> {
                    appGlobals.saveInt("sp", 2)

                    lightSwitchOnOffId.isClickable = false
                    enableTimeButtons()
                    println("sun $buttonText")
                }
                R.id.directOnOffId -> {
                    appGlobals.saveInt("sp", 3)
                    disableTimeButtons()
                    lightSwitchOnOffId.isClickable = true
                    println("direct $buttonText")
                }
            }
        }
    }

    private fun disableTimeButtons() {
        lightOn_time_button.backgroundTintList = getColorStateList(R.color.off_white)
        lightOn_time_button.isClickable = false
        lightOff_time_button.backgroundTintList = getColorStateList(R.color.off_white)
        lightOff_time_button.isClickable = false
        saveTimeButtonId.visibility = View.INVISIBLE
    }

    private fun enableTimeButtons() {
        lightOn_time_button.backgroundTintList = getColorStateList(R.color.purple_700)
        lightOn_time_button.isClickable = true
        lightOff_time_button.backgroundTintList = getColorStateList(R.color.off_white)
        lightOff_time_button.isClickable = false
        saveTimeButtonId.visibility = View.INVISIBLE
    }

//    private fun pickDateTime() {
//        val currentDateTime = Calendar.getInstance()
//        val startYear = currentDateTime.get(Calendar.YEAR)
//        val startMonth = currentDateTime.get(Calendar.MONTH)
//        val startDay = currentDateTime.get(Calendar.DAY_OF_MONTH)
//        val startHour = currentDateTime.get(Calendar.HOUR_OF_DAY)
//        val startMinute = currentDateTime.get(Calendar.MINUTE)
//
//        DatePickerDialog(this, DatePickerDialog.OnDateSetListener { _, year, month, day ->
//            TimePickerDialog(this, TimePickerDialog.OnTimeSetListener { _, hour, minute ->
//                val pickedDateTime = Calendar.getInstance()
//                pickedDateTime.set(year, month, day, hour, minute)
//                doSomethingWith(pickedDateTime)
//            }, startHour, startMinute, false).show()
//        }, startYear, startMonth, startDay).show()
//    }

    /*private fun demonstrateSubscribe(session: Session, details: SessionDetails?) {
        // Subscribe to topic to receive its events.
        val subFuture: CompletableFuture<Subscription> = session.subscribe("org.codebase",
            TriConsumer { args: List<Any>, kwargs: Map<String, Any>, details: EventDetails ->
                onEvent(args, kwargs, details)
            })
        subFuture.whenComplete(BiConsumer<Subscription, Throwable> { subscription: Subscription, throwable: Throwable? ->
            if (throwable == null) {
                // We have successfully subscribed.
                println("Subscribed to topic " + subscription.topic)
            } else {
                // Something went bad.
                throwable.printStackTrace()
            }
        })
    }

    private fun onEvent(args: List<Any>, kwargs: Map<String, Any>, details: EventDetails) {
        println(String.format("Got event: %s", args[0]))
    }*/
}