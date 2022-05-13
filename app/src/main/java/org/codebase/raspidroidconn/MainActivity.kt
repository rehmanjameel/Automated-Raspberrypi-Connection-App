package org.codebase.raspidroidconn

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.github.florent37.singledateandtimepicker.dialog.SingleDateAndTimePickerDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.crossbar.autobahn.wamp.Client
import io.crossbar.autobahn.wamp.Session
import io.crossbar.autobahn.wamp.types.CallResult
import kotlinx.android.synthetic.main.activity_main.*
import java.text.SimpleDateFormat
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
    private var radioButtonText = ""
    private var savedOnTime: String? = ""
    private var savedOffTime: String? = ""

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        title = "Light ON/OFF"
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

        /*** Switch button checked change listener to check the state of button
         * and publishing button current state text to subscribed topic ***/
        lightSwitchOnOffId.setOnClickListener {
            if (lightSwitchOnOffId.isChecked) {
                buttonText = "ON"
                lightOnOffImageId.setImageResource(R.drawable.light_on)
                appGlobals.saveBoolean(SWITCH_STATUS, true)
                appGlobals.saveBoolean(LIGHT_STATUS, true)
                directOnOff()
            } else {
                buttonText = "OFF"
                lightOnOffImageId.setImageResource(R.drawable.light_off)
                appGlobals.saveBoolean(SWITCH_STATUS, false)
                appGlobals.saveBoolean(LIGHT_STATUS, false)
                directOnOff()
            }
        }

        savedOnTime = appGlobals.getValueString("LightOnTime")

        savedOffTime = appGlobals.getValueString("LightOffTime")
        if (lightOn_time_text.text.isEmpty() && lightOff_time_text.text.isEmpty()) {
            Log.e("here", "is")
            lightOn_time_text.text = "__"
            lightOff_time_text.text = "__"
        } else {
            Log.e("Here", "not")
            lightOn_time_text.text = savedOnTime
            lightOff_time_text.text = savedOffTime
        }

        lightOnTimeButtonId.setOnClickListener {
            singleLightOnDateTimePicker()
        }

        lightOn_time_button.setOnClickListener {
            saveLightOnOffTime()
        }

        lightOffTimeButtonId.setOnClickListener {
            singleLightOffDateTimePicker()
        }

        lightOff_time_button.setOnClickListener {
            saveLightOnOffTime()
        }

    }

    private fun callProcedure() {
        // val args: MutableList<Any> = ArrayList()

        Log.e("time", lightOn_time_text.text.toString())

        if (appGlobals.getValueInt("sp") == 1) {
            if (wampSession.isConnected) {
                val callProc: CompletableFuture<CallResult> = wampSession.call("pk.codebase.sys.automatically_on_off", true)
                callProc.thenAccept { callResult ->
                    println(String.format("Call result: %s", callResult.results))
                }
                callProc.exceptionally { throwable ->
                    Log.e("thr", throwable.message.toString())
                    errorDialogBox(throwable.message.toString())
                    throwable.printStackTrace()
                    null
                }
            } else {
                errorDialogBox("Connection not created to server! or invalid Ip address")
            }
        }
    }

    /*** private function using for creating the client connection to publish the events
     * over networks with wamp connection ***/
    private fun publishSession() {
        wampSession = Session()
        val ipAddress = appGlobals.getValueString("Full_IP_Address")
        val client = Client(wampSession, "ws://$ipAddress:8080/ws", "realm1")
        client.connect().whenComplete { exitInfo, throwable ->
            Log.e("info", exitInfo.toString())
            if (throwable == null) {
                Log.e("Client", "Connection Created")
            }
        }
    }

    private fun directOnOff() {
        if (wampSession.isConnected) {
            Log.e("is", "here")
            if (buttonText.lowercase() == "on") {
                val callProc: CompletableFuture<CallResult> = wampSession.call("pk.codebase.sys.light_on")
                callProc.thenAccept { callResult ->
                    println(String.format("Call result: %s", callResult.results))
                    wampSession.call("pk.codebase.sys.automatically_on_off", false)
                }
                callProc.exceptionally { throwable ->
                    Log.e("thr", throwable.message.toString())
                    lightSwitchOnOffId.isChecked = false
                    lightOnOffImageId.setImageResource(R.drawable.light_off)
                    errorDialogBox(throwable.message.toString())
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
                    lightSwitchOnOffId.isChecked = false
                    lightOnOffImageId.setImageResource(R.drawable.light_off)
                    errorDialogBox(throwable.message.toString())
                    throwable.printStackTrace()
                    null
                }
            }
        } else {
            errorDialogBox("Connection not created to server! or invalid Ip address")
        }
    }

    private fun saveLightOnOffTime() {
        val savedOnTimeSeconds: Long = appGlobals.getValueLong("timeSeconds")
        val savedOffTimeSeconds: Long = appGlobals.getValueLong("offTimeSeconds")

        if (wampSession.isConnected) {
            if (lightOn_time_button.isPressed) {
                Log.e("on", "is here")
                val callProc: CompletableFuture<CallResult> = wampSession.call("pk.codebase.sys.set_on_at", savedOnTimeSeconds)
                wampSession.call("pk.codebase.sys.automatically_on_off", false)
                callProc.thenAccept { callResult ->
                    Log.e("in accept", "accepted")
                    println(String.format("Call result: %s", callResult.results))
                }
                callProc.exceptionally { throwable ->
                    Log.e("thr", throwable.message.toString())
                    errorDialogBox(throwable.message.toString())
                    throwable.printStackTrace()
                    null
                }
            } else if (lightOff_time_button.isPressed) {
                Log.e("off", "here")
                val callProc: CompletableFuture<CallResult> = wampSession.call("pk.codebase.sys.set_off_at", savedOffTimeSeconds)
                callProc.thenAccept { callResult ->
                    println(String.format("Call result: %s", callResult.results))
                    wampSession.call("pk.codebase.sys.automatically_on_off", false)
                }
                callProc.exceptionally { throwable ->
                    Log.e("thr", throwable.message.toString())
                    errorDialogBox(throwable.message.toString())
                    throwable.printStackTrace()
                    null
                }
            }
        } else {
            errorDialogBox("Connection not created to server! or invalid Ip address")
        }
    }

    private fun checkedRadioButtonId() {
        val sp : Int = appGlobals.getValueInt("sp")
        Log.e("sp", sp.toString())
        when (sp) {
            1 -> {
                callProcedure()
                sunTimeId.isChecked = true
                lightSwitchOnOffId.isChecked = false
                lightSwitchOnOffId.isEnabled = false
                lightOnOffImageId.setImageResource(R.drawable.light_off)
                disableTimeButtons()
            }
            2 -> {
                manualTimeId.isChecked = true
                lightSwitchOnOffId.isChecked = false
                lightSwitchOnOffId.isEnabled = false
                lightOnOffImageId.setImageResource(R.drawable.light_off)
                enableTimeButtons()
            }
            3 -> {
                directOnOffId.isChecked = true
                lightSwitchOnOffId.isEnabled = true
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
                    lightSwitchOnOffId.isEnabled = true
                }
            }
        }
    }

    private fun disableTimeButtons() {
        lightOnTimeButtonId.isEnabled = false
        lightOffTimeButtonId.isEnabled = false
        lightOn_time_button.backgroundTintList = getColorStateList(R.color.off_white)
        lightOn_time_button.isEnabled = false
        lightOff_time_button.backgroundTintList = getColorStateList(R.color.off_white)
        lightOff_time_text.visibility = View.INVISIBLE
        lightOn_time_text.visibility = View.INVISIBLE
        lightOff_time_button.isEnabled = false
    }

    private fun enableTimeButtons() {
        lightOnTimeButtonId.isEnabled = true
        lightOffTimeButtonId.isEnabled = true

        lightOff_time_text.visibility = View.VISIBLE
        lightOn_time_text.visibility = View.VISIBLE
    }

    private fun singleLightOnDateTimePicker() {
        SingleDateAndTimePickerDialog.Builder(this)
//            .bottomSheet()

            .displayListener {picker ->
                // Retrieve the SingleDateAndTimePicker
            }
            .title("Set Light on time")
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

                    val formatter = SimpleDateFormat("dd-MM-yyyy HH:mm")

                    val formatted = formatter.format(date.time)
                    appGlobals.saveString("LightOnTime", formatted)
                    savedOnTime = appGlobals.getValueString("LightOnTime").toString()
                    println("Current Date and Time is: $formatted")
                    lightOn_time_text.text = formatted.toString()
                    val dateInSeconds = (date.time) / 1000
                    appGlobals.saveLong("timeSeconds", dateInSeconds)

                    lightOn_time_button.backgroundTintList = getColorStateList(R.color.purple_700)
                    lightOn_time_button.isEnabled = true
                }
            }
            .display()
    }

    private fun singleLightOffDateTimePicker() {
        SingleDateAndTimePickerDialog.Builder(this)

            .displayListener {picker ->
                // Retrieve the SingleDateAndTimePicker
            }
            .title("Set Light off time")
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

                    val formatter = SimpleDateFormat("dd-MM-yyyy HH:mm")

                    val formatted = formatter.format(date.time)
                    appGlobals.saveString("LightOffTime", formatted)
                    savedOffTime = appGlobals.getValueString("LightOffTime").toString()
                    println("Current Date and Time is: $formatted")
                    lightOff_time_text.text = formatted.toString()

                    val dateInSeconds = (date.time) / 1000
                    Log.e("date in sec", dateInSeconds.toString())
                    appGlobals.saveLong("offTimeSeconds", dateInSeconds)
                    lightOff_time_button.backgroundTintList = getColorStateList(R.color.purple_700)
                    lightOff_time_button.isEnabled = true
                }
            }
            .display()
    }

    private fun errorDialogBox(message: String) {
        val builder = MaterialAlertDialogBuilder(this@MainActivity)
        builder.setTitle("Server Connection Error!")
        builder.setMessage(message)
        builder.setPositiveButton("Ok") {_, _ ->
            builder.create().dismiss()
        }
        builder.create().show()


    }
}