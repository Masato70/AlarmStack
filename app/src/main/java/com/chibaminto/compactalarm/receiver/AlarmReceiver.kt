package com.chibaminto.compactalarm.receiver

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import com.chibaminto.compactalarm.R
import com.chibaminto.compactalarm.data.AlarmDataStore
import com.chibaminto.compactalarm.ui.screens.AlarmStopActivity
import com.chibaminto.compactalarm.utils.AlarmScheduler
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class AlarmReceiver : BroadcastReceiver() {

    @Inject
    lateinit var dataStore: AlarmDataStore

    @Inject
    lateinit var alarmScheduler: AlarmScheduler

    companion object {
        private const val TAG = "AlarmReceiver"
        const val ACTION_ALARM_TRIGGERED = "com.chibaminto.compactalarm.ALARM_TRIGGERED"
        const val ACTION_STOP_ALARM = "com.chibaminto.compactalarm.STOP_ALARM"
        const val ACTION_SNOOZE_ALARM = "com.chibaminto.compactalarm.SNOOZE_ALARM"
        const val EXTRA_NOTIFICATION_ID = "notification_id"
        const val EXTRA_ALARM_LABEL = "alarm_label"
        private const val CHANNEL_ID = "alarm_channel"
        private const val CHANNEL_NAME = "Alarm Notifications"

        const val SNOOZE_MINUTES = 5
        private const val AUTO_STOP_DELAY_MS = 3 * 60 * 1000L // 3分
        private const val AUTO_STOP_REQUEST_CODE = Int.MAX_VALUE - 1

        private var mediaPlayer: MediaPlayer? = null
        private var vibrator: Vibrator? = null
        private var fadeInJob: kotlinx.coroutines.Job? = null
        // 現在鳴動中の通知IDを追跡
        private var currentNotificationId: Int? = null
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "onReceive: ${intent.action}")

        when (intent.action) {
            ACTION_ALARM_TRIGGERED -> {
                val alarmId = intent.getStringExtra(AlarmScheduler.EXTRA_ALARM_ID)
                val hour = intent.getIntExtra(AlarmScheduler.EXTRA_ALARM_HOUR, -1)
                val minute = intent.getIntExtra(AlarmScheduler.EXTRA_ALARM_MINUTE, -1)
                if (alarmId != null) {
                    val notificationId = if (hour >= 0 && minute >= 0) {
                        hour * 60 + minute
                    } else {
                        alarmId.hashCode()
                    }
                    triggerAlarm(context, alarmId, notificationId)
                }
            }
            ACTION_STOP_ALARM -> {
                val alarmId = intent.getStringExtra(AlarmScheduler.EXTRA_ALARM_ID)
                val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)
                    .takeIf { it >= 0 }
                stopAlarm(context, alarmId, notificationId)
            }
            ACTION_SNOOZE_ALARM -> {
                val alarmId = intent.getStringExtra(AlarmScheduler.EXTRA_ALARM_ID)
                val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)
                    .takeIf { it >= 0 }
                if (alarmId != null) {
                    snoozeAlarm(context, alarmId, notificationId)
                }
            }
        }
    }

    /**
     * アラームを発火
     */
    private fun triggerAlarm(context: Context, alarmId: String, notificationId: Int) {
        Log.d(TAG, "Triggering alarm: $alarmId (notificationId=$notificationId)")

        createNotificationChannel(context)

        // 既に鳴動中のアラームがあれば先に停止（通知もキャンセル）
        currentNotificationId?.let { oldId ->
            if (oldId != notificationId) {
                val notificationManager = context.getSystemService<NotificationManager>()
                notificationManager?.cancel(oldId)
            }
        }
        stopAlarmSound()
        stopVibration()

        // バイブレーションは即時開始（常に必要）
        startVibration(context)
        currentNotificationId = notificationId

        // カードデータを読み込んでから音・通知を処理
        scope.launch {
            try {
                val cards = dataStore.cardsFlow.first()
                val card = cards.find { it.id == alarmId }
                val isVibrationOnly = card?.isVibrationOnly ?: false
                val label = card?.label ?: ""

                if (!isVibrationOnly) {
                    startAlarmSound(context)
                }

                showAlarmNotification(context, alarmId, notificationId, label)
                scheduleAutoStop(context, alarmId, notificationId)

                // 曜日リピートの再スケジュールまたは無効化
                if (card != null && card.selectedWeekdays.isNotEmpty()) {
                    alarmScheduler.scheduleAlarm(card.id, card.alarmTime, card.selectedWeekdays)
                    Log.d(TAG, "Repeating alarm rescheduled: $alarmId")
                } else {
                    dataStore.updateCardEnabled(alarmId, false)
                    Log.d(TAG, "One-shot alarm disabled: $alarmId")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to handle trigger logic, fallback", e)
                startAlarmSound(context)
                showAlarmNotification(context, alarmId, notificationId, "")
                scheduleAutoStop(context, alarmId, notificationId)
                dataStore.updateCardEnabled(alarmId, false)
            }
        }
    }

    /**
     * アラームを停止
     */
    private fun stopAlarm(context: Context, alarmId: String?, notificationId: Int?) {
        Log.d(TAG, "Stopping alarm: $alarmId (notificationId=$notificationId)")

        stopAlarmSound()
        stopVibration()
        cancelAutoStop(context)
        currentNotificationId = null

        val idToCancel = notificationId
            ?: currentNotificationId
            ?: alarmId?.hashCode()
            ?: return

        val notificationManager = context.getSystemService<NotificationManager>()
        notificationManager?.cancel(idToCancel)
    }

    /**
     * スヌーズ
     */
    private fun snoozeAlarm(context: Context, alarmId: String, notificationId: Int?) {
        Log.d(TAG, "Snoozing alarm: $alarmId for $SNOOZE_MINUTES minutes")

        stopAlarmSound()
        stopVibration()
        cancelAutoStop(context)
        currentNotificationId = null

        // 通知をキャンセル
        if (notificationId != null && notificationId >= 0) {
            val notificationManager = context.getSystemService<NotificationManager>()
            notificationManager?.cancel(notificationId)
        }

        // スヌーズをスケジュール
        alarmScheduler.scheduleSnooze(alarmId, SNOOZE_MINUTES)
    }

    /**
     * 自動停止をスケジュール（3分後）
     */
    private fun scheduleAutoStop(context: Context, alarmId: String, notificationId: Int) {
        val stopIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_STOP_ALARM
            putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId)
            putExtra(EXTRA_NOTIFICATION_ID, notificationId)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            AUTO_STOP_REQUEST_CODE,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val am = context.getSystemService<AlarmManager>()
        am?.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            System.currentTimeMillis() + AUTO_STOP_DELAY_MS,
            pendingIntent
        )
        Log.d(TAG, "Auto-stop scheduled in ${AUTO_STOP_DELAY_MS / 1000}s")
    }

    /**
     * 自動停止をキャンセル
     */
    private fun cancelAutoStop(context: Context) {
        val stopIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_STOP_ALARM
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            AUTO_STOP_REQUEST_CODE,
            stopIntent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            val am = context.getSystemService<AlarmManager>()
            am?.cancel(pendingIntent)
            Log.d(TAG, "Auto-stop cancelled")
        }
    }

    private fun startAlarmSound(context: Context) {
        stopAlarmSound()
        try {
            val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

            mediaPlayer = MediaPlayer().apply {
                setDataSource(context, alarmUri)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                isLooping = true
                setVolume(0f, 0f) // 音量0で開始
                prepare()
                start()
            }

            // 30秒かけて音量を0→1にフェードイン
            fadeInJob?.cancel()
            fadeInJob = scope.launch {
                val fadeDurationMs = 30_000L
                val steps = 60
                val stepDelay = fadeDurationMs / steps
                for (i in 1..steps) {
                    val volume = i.toFloat() / steps
                    try {
                        mediaPlayer?.setVolume(volume, volume)
                    } catch (_: Exception) {
                        break // MediaPlayerが解放済みなら中断
                    }
                    delay(stepDelay)
                }
            }

            Log.d(TAG, "Alarm sound started with fade-in")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start alarm sound", e)
        }
    }

    private fun stopAlarmSound() {
        fadeInJob?.cancel()
        fadeInJob = null
        mediaPlayer?.let {
            try {
                if (it.isPlaying) it.stop()
                it.reset()
                it.release()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to stop alarm sound", e)
            }
        }
        mediaPlayer = null
    }

    private fun startVibration(context: Context) {
        stopVibration()
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService<VibratorManager>()?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }

        val pattern = longArrayOf(0, 1000, 500)
        vibrator?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                it.vibrate(
                    VibrationEffect.createWaveform(pattern, 0),
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .build()
                )
            } else {
                @Suppress("DEPRECATION")
                it.vibrate(pattern, 0)
            }
        }
    }

    private fun stopVibration() {
        vibrator?.cancel()
        vibrator = null
    }

    private fun createNotificationChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Alarm notifications"
            setBypassDnd(true)
            enableVibration(true)
            setSound(null, null)
        }
        context.getSystemService<NotificationManager>()?.createNotificationChannel(channel)
    }

    private fun showAlarmNotification(
        context: Context,
        alarmId: String,
        notificationId: Int,
        label: String
    ) {
        val notificationManager = context.getSystemService<NotificationManager>() ?: return

        // フルスクリーンインテント
        val fullScreenIntent = Intent(context, AlarmStopActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId)
            putExtra(EXTRA_NOTIFICATION_ID, notificationId)
            putExtra(EXTRA_ALARM_LABEL, label)
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 停止アクション
        val stopIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_STOP_ALARM
            putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId)
            putExtra(EXTRA_NOTIFICATION_ID, notificationId)
        }
        val stopPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId + 10000,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // スヌーズアクション
        val snoozeIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_SNOOZE_ALARM
            putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId)
            putExtra(EXTRA_NOTIFICATION_ID, notificationId)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId + 20000,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (label.isNotEmpty()) label else context.getString(R.string.alarm)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_alarm)
            .setContentTitle(title)
            .setContentText(context.getString(R.string.tap_to_stop_alarm))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(false)
            .setOngoing(true)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setContentIntent(fullScreenPendingIntent)
            .setDeleteIntent(stopPendingIntent)
            .addAction(
                R.drawable.ic_stop,
                context.getString(R.string.stop),
                stopPendingIntent
            )
            .addAction(
                R.drawable.ic_alarm,
                context.getString(R.string.snooze_after_minutes, SNOOZE_MINUTES),
                snoozePendingIntent
            )
            .build()

        notificationManager.notify(notificationId, notification)
    }
}