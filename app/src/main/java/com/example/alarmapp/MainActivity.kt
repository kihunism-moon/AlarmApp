package com.example.alarmapp

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.TimePicker
import org.w3c.dom.Text
import java.util.*

class MainActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
//      step0 : 뷰 초기화 하기 -> 완료
        initOnOffButton()
        initChangeAlarmTimeButton()
        
        val model = fetchDataFromSharedPreferences()
        renderView(model)

//      step1 : 데이터 가져오기


//      step2 : 뷰에 데이터를 그려주기


    }

    private fun initOnOffButton() {
        val onOffButton = findViewById<Button>(R.id.onOffButton)
        onOffButton.setOnClickListener {

            val model = it.tag as? AlarmDisplayModel ?: return@setOnClickListener
            val newModel = saveAlarmModel(model.hour, model.minute, model.onOff.not())
            renderView(newModel)

            if(newModel.onOff) {
//                켜진 경우 -> 알람을 등록
                val calendar = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, newModel.hour)
                    set(Calendar.MINUTE, newModel.minute)

                    if(before(Calendar.getInstance())) {
                        add(Calendar.DATE, 1)
                    }
                }
                val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
                val intent = Intent(this, AlarmReceiver::class.java)
                val pendingIntent = PendingIntent.getBroadcast(this, REQUEST_CODE,
                        intent, PendingIntent.FLAG_UPDATE_CURRENT)



                alarmManager.setInexactRepeating(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    AlarmManager.INTERVAL_DAY,
                    pendingIntent
                )
            }else {
//                꺼진 경우 -> 알람을 제거
                cancelAlarm()
            }
        }
    }

    private fun initChangeAlarmTimeButton() {
        val changeAlarmButton = findViewById<Button>(R.id.changeAlarmTimeButton)
        changeAlarmButton.setOnClickListener {

            val calendar = Calendar.getInstance()
            TimePickerDialog(
                this, { picker, hour, minute ->
                    val model = saveAlarmModel(hour, minute, false)
                    renderView(model)
                    cancelAlarm()
                      }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE),
                false
            )
                .show()

        }

    }

    private fun saveAlarmModel(
        hour: Int,
        minute: Int,
        onOff: Boolean
    ): AlarmDisplayModel {

        val model = AlarmDisplayModel(
            hour = hour,
            minute = minute,
            onOff = onOff
        )

        val sharedPreferences = getSharedPreferences("time", Context.MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            putString("alarm", model.makeDataForDB())
            putBoolean("onOff", model.onOff)
            commit()
        }
        return model
    }
    
    private fun fetchDataFromSharedPreferences(): AlarmDisplayModel {
        val sharedPreferences = getSharedPreferences("time", Context.MODE_PRIVATE)
        val timeDBValue = sharedPreferences.getString(ALARM_KEY, "9:30") ?: "9:30"
        val onOffDBValue = sharedPreferences.getBoolean(ONOFF_KEY, false)
        val alarmData = timeDBValue.split(":") // split을 쓰면 구분자를 기준으로 List형으로 생성 된다.

        val alarmModel = AlarmDisplayModel(
            hour = alarmData[0].toInt(),
            minute = alarmData[1].toInt(),
            onOff = onOffDBValue
        )

//      보정 예외처리

        val pendingIntent = PendingIntent.getBroadcast(this, REQUEST_CODE,
            Intent(this, AlarmReceiver::class.java),
            PendingIntent.FLAG_NO_CREATE)

        if((pendingIntent == null) and alarmModel.onOff) {
//          알람은 꺼져 있는데, 데이터는 켜져 있는 경우
            alarmModel.onOff = false

        }else if((pendingIntent != null) and alarmModel.onOff.not()) {
//          알람은 켜져 있는데, 데이터는 꺼져 있는 경우
//          알람을 취소함
            pendingIntent.cancel()
        }
        return alarmModel

    }

    private fun renderView(model: AlarmDisplayModel) {
        findViewById<TextView>(R.id.ampmTextView).apply {
            text = model.ampmText
        }

        findViewById<TextView>(R.id.timeTextView).apply {
            text = model.timeText
        }

        findViewById<Button>(R.id.onOffButton).apply {
            text = model.onOffText
            tag = model // Object 속성 -> 최상위 속성 (temp 랑 비슷)

        }
    }

     private fun cancelAlarm() {
         val pendingIntent = PendingIntent.getBroadcast(this, REQUEST_CODE,
             Intent(this, AlarmReceiver::class.java),
             PendingIntent.FLAG_NO_CREATE)
         pendingIntent?.cancel()
     }

    companion object{
        private const val REQUEST_CODE = 1000
        private const val ALARM_KEY = "alarm"
        private const val ONOFF_KEY = "onOff"
        private const val SHARED_PREFERENCES_NAME = "time"
    }

}