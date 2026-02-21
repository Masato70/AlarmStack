package com.chibaminto.compactalarm.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.chibaminto.compactalarm.data.AlarmDataStore
import com.chibaminto.compactalarm.utils.AlarmScheduler
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject
    lateinit var dataStore: AlarmDataStore

    @Inject
    lateinit var alarmScheduler: AlarmScheduler

    companion object {
        private const val TAG = "BootReceiver"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "onReceive: ${intent.action}")
        
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_LOCKED_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                rescheduleAlarms()
            }
        }
    }

    /**
     * 全ての有効なアラームを再スケジュール
     */
    private fun rescheduleAlarms() {
        scope.launch {
            try {
                val cards = dataStore.cardsFlow.first()
                val enabledCards = cards.filter { it.isEnabled }
                
                Log.d(TAG, "Rescheduling ${enabledCards.size} alarms")
                
                enabledCards.forEach { card ->
                    alarmScheduler.scheduleAlarm(
                        card.id,
                        card.alarmTime,
                        card.selectedWeekdays
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to reschedule alarms", e)
            }
        }
    }
}
