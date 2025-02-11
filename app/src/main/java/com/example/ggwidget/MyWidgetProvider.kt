package com.example.ggwidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.RemoteViews
import com.example.ggwidget.worker.WidgetUpdateScheduler
import com.example.ggwidget.network.GeckoApi
import kotlinx.coroutines.*

class MyWidgetProvider : AppWidgetProvider() {

    private var lastPrice: Float = 0.00f  // Хранит последнюю цену токена
    private var lastPriceChange: Float = 0.00f  // Хранит последнее изменение %

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)

        Log.d("CryptoWidget", "onUpdate: Обновление виджета...")

        // Запускаем обновление виджета через AlarmManager
        WidgetUpdateScheduler.scheduleWidgetUpdate(context)

        for (widgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, widgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent?) {
        super.onReceive(context, intent)

        Log.d("CryptoWidget", "onReceive() вызван с action: ${intent?.action}")

        if (intent?.action == "com.example.ggwidget.UPDATE_WIDGET") {
            Log.d("CryptoWidget", "onReceive: Получен сигнал обновления виджета")

            val appWidgetManager = AppWidgetManager.getInstance(context)
            val ids = appWidgetManager.getAppWidgetIds(
                ComponentName(context, MyWidgetProvider::class.java)
            )

            for (widgetId in ids) {
                updateWidget(context, appWidgetManager, widgetId)
            }
        }
    }

    private fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        Log.d("CryptoWidget", "Обновление виджета ID: $appWidgetId")

        // Запускаем обновление курса через API
        val views = RemoteViews(context.packageName, R.layout.widget_layout)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = GeckoApi.service.getCryptoPrice()
                val attributes = response.data?.attributes

                val newPrice = attributes?.priceUsd ?: 0.00f
                val priceChange = attributes?.priceChange?.h24 ?: lastPriceChange

                // Сохраняем данные
                lastPrice = newPrice
                lastPriceChange = priceChange

                Log.d("CryptoWidget", "Цена: $newPrice, Изменение: $priceChange%")
                Log.d("CryptoAPI", "API ответ: ${response.data?.attributes}")

                withContext(Dispatchers.Main) {
                    views.setTextViewText(R.id.widget_token_price, "$%.2f".format(newPrice))
                    views.setTextViewText(R.id.widget_token_change, "%.2f%%".format(priceChange))

                    val color = if (priceChange >= 0) android.graphics.Color.GREEN else android.graphics.Color.RED
                    views.setTextColor(R.id.widget_token_change, color)

                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }
            } catch (e: Exception) {
                Log.e("CryptoWidget", "Ошибка загрузки данных", e)
            }
        }

        // Настраиваем обновление при нажатии на виджет
        val intent = Intent(context, MyWidgetProvider::class.java).apply {
            action = "com.example.ggwidget.UPDATE_WIDGET"
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_refresh_button, pendingIntent)

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    override fun onEnabled(context: Context?) {
        super.onEnabled(context)
        Log.d("CryptoWidget", "onEnabled: Виджет добавлен на экран")
    }

    override fun onDisabled(context: Context?) {
        super.onDisabled(context)
        Log.d("CryptoWidget", "onDisabled: Последний виджет удалён")
    }
}
