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
import com.github.florent37.singledateandtimepicker.dialog.SingleDateAndTimePickerDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.crossbar.autobahn.wamp.Client
import io.crossbar.autobahn.wamp.Session
import io.crossbar.autobahn.wamp.types.CallResult
import io.crossbar.autobahn.wamp.types.Publication
import kotlinx.android.synthetic.main.activity_main.*
import java.lang.String
import java.text.ParseException
import java.text.SimpleDateFormat
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

        val date: LocalDateTime = LocalDateTime.now()
        val seconds: Int = date.toLocalTime().toSecondOfDay()
        Log.e("seconds", seconds.toString())
        /*** creating connection on app start ***/
        publishSession()
        checkedRadioButtonId()
        getRadioButtonText()

        switchStatus = appGlobals.getValueBoolean(SWITCH_STATUS)
        lightStatus = appGlobals.getValueBoolean(LIGHT_STATUS)
        lightSwitchOnOffId.isChecked = switchStatus as Boolean
        if (lightStatus as Boolean) {
            lightOnOffImageId.setImageResource(R.drawable.light_on)
        } else {
            lightOnOffImageId.setImageResource(R.drawable.light_off)
        }

        Log.e("current", System.currentTimeMillis().toString())
        /*** Switch button checked change listener to check the state of button
         * and publishing button current state text to subscribed topic ***/
        lightSwitchOnOffId.setOnCheckedChangeListener { compoundButton, checked ->
            if (compoundButton.isChecked) {
                buttonText = "On"
                lightOnOffImageId.setImageResource(R.drawable.light_on)
                appGlobals.saveBoolean(SWITCH_STATUS, true)
                appGlobals.saveBoolean(LIGHT_STATUS, true)
                callProcedure()
//                demonstratePublish()
            } else {
                buttonText = "Off"
                lightOnOffImageId.setImageResource(R.drawable.light_off)
                appGlobals.saveBoolean(SWITCH_STATUS, false)
                appGlobals.saveBoolean(LIGHT_STATUS, false)
                callProcedure()
//                demonstratePublish()
            }
        }

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

        /*** constraint layout id***/
        constraintLayoutId.setOnClickListener {
            hideSoftKeyboard()
        }

        lightOn_time_button.setOnClickListener {
            singleLightOnDateTimePicker()
        }

        lightOff_time_button.setOnClickListener {
            singleLightOffDateTimePicker()
        }

        saveTimeButtonId.setOnClickListener {
            callProcedure()
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
                lightOnOffImageId.setImageResource(R.drawable.light_off)
            } else if (lightSwitchOnOffId.textOff == "Off") {
                lightSwitchOnOffId.isChecked = true
                lightOnOffImageId.setImageResource(R.drawable.light_on)
            }

            Log.e("Error", "Session not connected or invalid Ip address")
        }
    }

    private fun callProcedure() {
        if (wampSession.isConnected) {
            val args: MutableList<Any> = ArrayList()
            val savedOnTimeSeconds :Long = appGlobals.getValueLong("timeSeconds")
            val savedOffTimeSeconds: Long  = appGlobals.getValueLong("offTimeSeconds")
            Log.e("time", lightOn_time_text.text.toString())
            Log.e("time", savedOnTimeSeconds.toString())
            Log.e("time", savedOffTimeSeconds.toString())
//            args.add(savedOnTimeSeconds.toString())
//            args.add(savedOffTimeSeconds.toString())
//            args.add(true)
            if (buttonText.lowercase() == "on") {
                val callProc: CompletableFuture<CallResult> = wampSession.call("pk.codebase.sys.light_on")
                callProc.thenAccept { callResult ->
                    println(String.format("Call result: %s", callResult.results))
                }
                callProc.exceptionally { throwable ->
                    Log.e("thr", throwable.message.toString())
                    throwable.printStackTrace()
                    null
                }
            } else if (buttonText.lowercase() == "off") {
                val callProc: CompletableFuture<CallResult> = wampSession.call("pk.codebase.sys.light_off")
                callProc.thenAccept { callResult ->
                    println(String.format("Call result: %s", callResult.results))
                }
                callProc.exceptionally { throwable ->
                    Log.e("thr", throwable.message.toString())
                    throwable.printStackTrace()
                    null
                }
            }

        }
    }
    /*** private function using for creating the client connection to publish the events
     * over networks with wamp connection ***/
    private fun publishSession() {
        wampSession = Session()

        val client = Client(wampSession, "ws://192.168.100.218:8080/ws", "realm1")
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

    private fun checkedRadioButtonId() {
        val sp : Int = appGlobals.getValueInt("sp")
        when (sp) {
            1 -> {
                sunTimeId.isChecked = true
                lightSwitchOnOffId.isChecked = false
                lightSwitchOnOffId.isClickable = false
                lightOnOffImageId.setImageResource(R.drawable.light_off)
                disableTimeButtons()
            }
            2 -> {
                manualTimeId.isChecked = true
                lightSwitchOnOffId.isChecked = false
                lightSwitchOnOffId.isClickable = false
                lightOnOffImageId.setImageResource(R.drawable.light_off)
                enableTimeButtons()
            }
            3 -> {
                directOnOffId.isChecked = true
                disableTimeButtons()
            }
        }
    }

    private fun getRadioButtonText() {
        //Getting radiobutton text on the basis of radio button id

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
                    checkedRadioButtonId()
                }
                R.id.manualTimeId -> {
                    appGlobals.saveInt("sp", 2)
                    checkedRadioButtonId()
                }
                R.id.directOnOffId -> {
                    appGlobals.saveInt("sp", 3)
                    checkedRadioButtonId()
                    lightSwitchOnOffId.isClickable = true
                }
            }
        }
    }

    private fun disableTimeButtons() {
        lightOn_time_button.backgroundTintList = getColorStateList(R.color.off_white)
        lightOn_time_button.isClickable = false
        lightOff_time_button.backgroundTintList = getColorStateList(R.color.off_white)
        lightOff_time_text.visibility = View.INVISIBLE
        lightOn_time_text.visibility = View.INVISIBLE
        lightOff_time_button.isClickable = false
        saveTimeButtonId.visibility = View.INVISIBLE
    }

    private fun enableTimeButtons() {
        lightOn_time_button.backgroundTintList = getColorStateList(R.color.purple_700)
        lightOn_time_button.isClickable = true
        lightOff_time_button.backgroundTintList = getColorStateList(R.color.purple_700)
        lightOff_time_button.isClickable = true
        lightOff_time_text.visibility = View.VISIBLE
        lightOn_time_text.visibility = View.VISIBLE
        saveTimeButtonId.visibility = View.INVISIBLE
    }

    private fun singleLightOnDateTimePicker() {
        SingleDateAndTimePickerDialog.Builder(this) //.bottomSheet()

            .displayListener {picker ->
                // Retrieve the SingleDateAndTimePicker
            }
            .title("Select Date time")
            .curved()
            .displayMinutes(true)
            .displayHours(true)
            .displayDays(false)
            .displayMonth(true)
            .displayYears(true)
            .displayDaysOfMonth(true)
            .minutesStep(1)
            .displayMonthNumbers(true)
            .listener { date ->
                if (date != null) {
                    Log.e("date", date.time.toString())

                    Log.e("form", savedOnTime)
                    val formatter = SimpleDateFormat("dd-MM-yyyy HH:mm")

                    val formatted = formatter.format(date.time)
                    appGlobals.saveString("LightOnTime", formatted)
                    savedOnTime = appGlobals.getValueString("LightOnTime").toString()
                    println("Current Date and Time is: $formatted")
                    lightOn_time_text.text = formatted.toString()
                    val dateInSeconds = (date.time) / 1000
                    appGlobals.saveLong("timeSeconds", dateInSeconds)
                }
            }
            .display()
    }

    private fun singleLightOffDateTimePicker() {
        SingleDateAndTimePickerDialog.Builder(this) //.bottomSheet()

            .displayListener {picker ->
                // Retrieve the SingleDateAndTimePicker
            }
            .title("Select Date time")
            .curved()
            .displayMinutes(true)
            .displayHours(true)
            .displayDays(false)
            .displayMonth(true)
            .displayYears(true)
            .displayDaysOfMonth(true)
            .minutesStep(1)
            .displayMonthNumbers(true)
            .listener { date ->
                if (date != null) {
                    Log.e("date", date.time.toString())

                    Log.e("form", savedOnTime)
                    val formatter = SimpleDateFormat("dd-MM-yyyy HH:mm")

                    val formatted = formatter.format(date.time)
                    appGlobals.saveString("LightOffTime", formatted)
                    savedOnTime = appGlobals.getValueString("LightOffTime").toString()
                    println("Current Date and Time is: $formatted")
                    lightOff_time_text.text = formatted.toString()
                    saveTimeButtonId.visibility = View.VISIBLE

//                    val givenDateString = "Tue Apr 23 16:08:28 GMT+05:30 2013"
//                    val sdf = SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy")
                    val dateInSeconds = (date.time) / 1000
                    Log.e("date in sec", dateInSeconds.toString())
                    appGlobals.saveLong("offTimeSeconds", dateInSeconds)
                }
            }
            .display()
    }
}