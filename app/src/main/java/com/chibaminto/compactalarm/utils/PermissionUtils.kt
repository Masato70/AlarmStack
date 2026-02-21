package com.chibaminto.compactalarm.utils

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService

/**
 * 権限関連のユーティリティ
 */
object PermissionUtils {

    /**
     * 通知権限が許可されているか確認
     */
    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    /**
     * 正確なアラーム権限が許可されているか確認
     */
    fun hasExactAlarmPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService<AlarmManager>()
            alarmManager?.canScheduleExactAlarms() == true
        } else {
            true
        }
    }

    /**
     * 正確なアラーム権限の設定画面を開くIntent
     */
    fun getExactAlarmSettingsIntent(): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
        } else {
            Intent(Settings.ACTION_SETTINGS)
        }
    }

    /**
     * 通知権限が必要かどうか
     */
    fun requiresNotificationPermission(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    }
}
