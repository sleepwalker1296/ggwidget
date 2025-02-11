package com.example.ggwidget.worker

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import android.os.Build
import androidx.core.content.ContextCompat
import com.example.ggwidget.MyWidgetProvider

object WidgetUpdateScheduler {
    fun scheduleWidgetUpdate(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
        if (alarmManager == null) {
            Log.e("WidgetUpdate", "Ошибка: AlarmManager недоступен")
            return
        }

        // Проверяем, нужно ли разрешение для SCHEDULE_EXACT_ALARM
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val permissionStatus = ContextCompat.checkSelfPermission(
                context, android.Manifest.permission.SCHEDULE_EXACT_ALARM
            )
            if (permissionStatus != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                Log.e("WidgetUpdate", "Нет разрешения SCHEDULE_EXACT_ALARM")
                return
            }
        }

        val intent = Intent(context, MyWidgetProvider::class.java).apply {
            action = "com.example.ggwidget.UPDATE_WIDGET"
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val interval = 60 * 1000L // 1 минута
        val triggerTime = System.currentTimeMillis() + interval

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerTime,
            pendingIntent
        )

        Log.d("WidgetUpdate", "Запланировано обновление виджета через 1 минуту")
    }
}
