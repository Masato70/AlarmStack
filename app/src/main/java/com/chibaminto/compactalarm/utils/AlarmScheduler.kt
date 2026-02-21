package com.chibaminto.compactalarm.utils

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.content.getSystemService
import com.chibaminto.compactalarm.receiver.AlarmReceiver
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlarmScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val alarmManager: AlarmManager? = context.getSystemService()

    companion object {
        private const val TAG = "AlarmScheduler"
        const val EXTRA_ALARM_ID = "alarm_id"
        const val EXTRA_ALARM_HOUR = "alarm_hour"
        const val EXTRA_ALARM_MINUTE = "alarm_minute"
        const val EXTRA_IS_SNOOZE = "is_snooze"
        private const val SNOOZE_REQUEST_CODE_OFFSET = 30000
    }

    @SuppressLint("ScheduleExactAlarm")
    fun scheduleAlarm(
        alarmId: String,
        alarmTime: LocalTime,
        weekdays: List<DayOfWeek> = emptyList()
    ) {
        val alarmManager = alarmManager ?: run {
            Log.e(TAG, "AlarmManager is null")
            return
        }

        val triggerTimeMillis = calculateNextTriggerTime(alarmTime, weekdays)

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_ALARM_TRIGGERED
            putExtra(EXTRA_ALARM_ID, alarmId)
            putExtra(EXTRA_ALARM_HOUR, alarmTime.hour)
            putExtra(EXTRA_ALARM_MINUTE, alarmTime.minute)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarmId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setAlarmClock(
                        AlarmManager.AlarmClockInfo(triggerTimeMillis, pendingIntent),
                        pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTimeMillis,
                        pendingIntent
                    )
                    Log.w(TAG, "Exact alarm permission not granted, using inexact alarm")
                }
            } else {
                alarmManager.setAlarmClock(
                    AlarmManager.AlarmClockInfo(triggerTimeMillis, pendingIntent),
                    pendingIntent
                )
            }
            Log.d(TAG, "Alarm scheduled: $alarmId at ${java.util.Date(triggerTimeMillis)}")
        } catch (e: SecurityException) {
            Log.e(TAG, "Failed to schedule alarm: ${e.message}")
        }
    }

    /**
     * スヌーズをスケジュール
     */
    @SuppressLint("ScheduleExactAlarm")
    fun scheduleSnooze(alarmId: String, snoozeMinutes: Int) {
        val alarmManager = alarmManager ?: run {
            Log.e(TAG, "AlarmManager is null")
            return
        }

        val triggerTimeMillis = System.currentTimeMillis() + snoozeMinutes * 60 * 1000L
        val snoozeTime = LocalTime.now().plusMinutes(snoozeMinutes.toLong())

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_ALARM_TRIGGERED
            putExtra(EXTRA_ALARM_ID, alarmId)
            putExtra(EXTRA_ALARM_HOUR, snoozeTime.hour)
            putExtra(EXTRA_ALARM_MINUTE, snoozeTime.minute)
            putExtra(EXTRA_IS_SNOOZE, true)
        }

        // スヌーズ専用のリクエストコード（通常アラームと別管理）
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarmId.hashCode() + SNOOZE_REQUEST_CODE_OFFSET,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setAlarmClock(
                        AlarmManager.AlarmClockInfo(triggerTimeMillis, pendingIntent),
                        pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTimeMillis,
                        pendingIntent
                    )
                }
            } else {
                alarmManager.setAlarmClock(
                    AlarmManager.AlarmClockInfo(triggerTimeMillis, pendingIntent),
                    pendingIntent
                )
            }
            Log.d(TAG, "Snooze scheduled: $alarmId for $snoozeMinutes min later")
        } catch (e: SecurityException) {
            Log.e(TAG, "Failed to schedule snooze: ${e.message}")
        }
    }

    fun cancelAlarm(alarmId: String) {
        // scheduleAlarm で登録したものと同じ action を指定しないとキャンセルできない
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_ALARM_TRIGGERED
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarmId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager?.cancel(pendingIntent)

        // スヌーズもキャンセル（同じ action で登録されている）
        val snoozeIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_ALARM_TRIGGERED
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            alarmId.hashCode() + SNOOZE_REQUEST_CODE_OFFSET,
            snoozeIntent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (snoozePendingIntent != null) {
            alarmManager?.cancel(snoozePendingIntent)
        }

        Log.d(TAG, "Alarm cancelled: $alarmId")
    }

    private fun calculateNextTriggerTime(
        alarmTime: LocalTime,
        weekdays: List<DayOfWeek>
    ): Long {
        val now = Calendar.getInstance()
        val alarmCalendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, alarmTime.hour)
            set(Calendar.MINUTE, alarmTime.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (weekdays.isNotEmpty()) {
            val today = LocalDate.now()
            var nextAlarmDate = today
            var daysToAdd = 0

            while (daysToAdd <= 7) {
                val checkDate = today.plusDays(daysToAdd.toLong())
                if (weekdays.contains(checkDate.dayOfWeek)) {
                    if (daysToAdd == 0) {
                        val todayAlarmTime = alarmCalendar.timeInMillis
                        if (todayAlarmTime <= now.timeInMillis) {
                            daysToAdd++
                            continue
                        }
                    }
                    nextAlarmDate = checkDate
                    break
                }
                daysToAdd++
            }

            alarmCalendar.set(Calendar.YEAR, nextAlarmDate.year)
            alarmCalendar.set(Calendar.MONTH, nextAlarmDate.monthValue - 1)
            alarmCalendar.set(Calendar.DAY_OF_MONTH, nextAlarmDate.dayOfMonth)
        } else {
            if (alarmCalendar.timeInMillis <= now.timeInMillis) {
                alarmCalendar.add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        return alarmCalendar.timeInMillis
    }

    fun canScheduleExactAlarms(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager?.canScheduleExactAlarms() == true
        } else {
            true
        }
    }
}