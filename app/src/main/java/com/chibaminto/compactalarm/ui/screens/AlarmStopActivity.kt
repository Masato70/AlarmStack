package com.chibaminto.compactalarm.ui.screens

import android.app.KeyguardManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.chibaminto.compactalarm.R
import com.chibaminto.compactalarm.receiver.AlarmReceiver
import com.chibaminto.compactalarm.ui.theme.CompactAlarmTheme
import com.chibaminto.compactalarm.utils.AlarmScheduler
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@AndroidEntryPoint
class AlarmStopActivity : ComponentActivity() {

    private var alarmId: String? = null
    private var notificationId: Int = -1
    private var label: String = ""
    private var autoCloseJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setupWindowFlags()
        setupSystemBars()

        alarmId = intent.getStringExtra(AlarmScheduler.EXTRA_ALARM_ID)
        notificationId = intent.getIntExtra(AlarmReceiver.EXTRA_NOTIFICATION_ID, -1)
        label = intent.getStringExtra(AlarmReceiver.EXTRA_ALARM_LABEL) ?: ""

        // 3分後に自動で閉じる（自動停止と連動）
        autoCloseJob = lifecycleScope.launch {
            delay(3 * 60 * 1000L)
            finish()
        }

        setContent {
            CompactAlarmTheme(darkTheme = true) {
                AlarmStopScreen(
                    currentTime = LocalTime.now(),
                    label = label,
                    snoozeMinutes = AlarmReceiver.SNOOZE_MINUTES,
                    onStopClick = { stopAlarmAndFinish() },
                    onSnoozeClick = { snoozeAndFinish() }
                )
            }
        }
    }

    private fun setupWindowFlags() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
        }

        val keyguardManager = getSystemService(KEYGUARD_SERVICE) as KeyguardManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            keyguardManager.requestDismissKeyguard(this, null)
        }
    }

    /**
     * ステータスバー・ナビゲーションバーを黒に設定
     */
    private fun setupSystemBars() {
        window.statusBarColor = android.graphics.Color.BLACK
        window.navigationBarColor = android.graphics.Color.BLACK

        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }
    }

    private fun stopAlarmAndFinish() {
        autoCloseJob?.cancel()
        val stopIntent = Intent(this, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_STOP_ALARM
            putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId)
            putExtra(AlarmReceiver.EXTRA_NOTIFICATION_ID, notificationId)
        }
        sendBroadcast(stopIntent)
        finish()
    }

    private fun snoozeAndFinish() {
        autoCloseJob?.cancel()
        val snoozeIntent = Intent(this, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_SNOOZE_ALARM
            putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId)
            putExtra(AlarmReceiver.EXTRA_NOTIFICATION_ID, notificationId)
        }
        sendBroadcast(snoozeIntent)
        finish()
    }

    override fun onDestroy() {
        autoCloseJob?.cancel()
        super.onDestroy()
    }
}

@Composable
private fun AlarmStopScreen(
    currentTime: LocalTime,
    label: String,
    snoozeMinutes: Int,
    onStopClick: () -> Unit,
    onSnoozeClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            // 現在時刻
            Text(
                text = currentTime.format(DateTimeFormatter.ofPattern("HH:mm")),
                style = MaterialTheme.typography.displayLarge.copy(
                    fontWeight = FontWeight.Light,
                    fontSize = 80.sp
                ),
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(12.dp))

            // ラベル or "アラーム"
            if (label.isNotEmpty()) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )
            } else {
                Text(
                    text = stringResource(R.string.alarm),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(64.dp))

            // 停止ボタン
            Button(
                onClick = onStopClick,
                modifier = Modifier
                    .size(140.dp)
                    .scale(scale),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 8.dp,
                    pressedElevation = 12.dp
                )
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.stop),
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onError
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.stop),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onError
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // スヌーズボタン
            OutlinedButton(
                onClick = onSnoozeClick,
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.height(56.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.snooze_after_minutes, snoozeMinutes),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    }
}