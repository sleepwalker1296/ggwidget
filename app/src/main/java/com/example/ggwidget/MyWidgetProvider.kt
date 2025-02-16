package com.example.ggwidget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.util.Log
import android.widget.RemoteViews
import androidx.appcompat.app.AppCompatDelegate
import com.example.ggwidget.network.GeckoApi
import kotlinx.coroutines.*

class MyWidgetProvider : AppWidgetProvider() {

    private val widgetScope = CoroutineScope(SupervisorJob() + Dispatchers.IO) // ✅ НЕ будет отменяться при обновлении

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        Log.d("CryptoWidget", "onUpdate: Виджет обновляется...")

        for (widgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, widgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent?) {
        super.onReceive(context, intent)

        Log.d("CryptoWidget", "onReceive вызван с action: ${intent?.action}")

        if (intent?.action == "com.example.ggwidget.UPDATE_WIDGET" ||
            intent?.action == "com.example.ggwidget.UPDATE_WIDGET_THEME") {

            Log.d("CryptoWidget", "Получен сигнал обновления виджета")

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

        val views = RemoteViews(context.packageName, R.layout.widget_layout)

        // ✅ Читаем текущую тему
        val sharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val isDarkTheme = sharedPreferences.getInt("theme_mode", AppCompatDelegate.MODE_NIGHT_YES) == AppCompatDelegate.MODE_NIGHT_YES

        // ✅ Меняем фон виджета
        val backgroundResource = if (isDarkTheme) R.drawable.widget_background else R.drawable.widget_background_light
        views.setInt(R.id.widget_container, "setBackgroundResource", backgroundResource)

        // ✅ Меняем цвет текста
        val textColor = if (isDarkTheme) Color.WHITE else Color.BLACK
        views.setTextColor(R.id.widget_token_title, textColor)
        views.setTextColor(R.id.widget_token_price, textColor)
        views.setTextColor(R.id.widget_token_change, textColor)

        // ✅ Используем `widgetScope.launch` вместо `CoroutineScope.launch`
        widgetScope.launch {
            try {
                val response = GeckoApi.service.getCryptoPrice()
                val attributes = response.data?.attributes
                val newPrice = attributes?.priceUsd ?: 0.00f
                val priceChange = attributes?.priceChange?.h24 ?: 0.00f

                Log.d("CryptoWidget", "Цена: $newPrice, Изменение: $priceChange%")

                withContext(Dispatchers.Main) {
                    views.setTextViewText(R.id.widget_token_price, "$%.2f".format(newPrice))
                    views.setTextViewText(R.id.widget_token_change, "%.2f%%".format(priceChange))

                    val priceColor = if (priceChange >= 0) Color.GREEN else Color.RED
                    views.setTextColor(R.id.widget_token_change, priceColor)

                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }
            } catch (e: Exception) {
                Log.e("CryptoWidget", "Ошибка загрузки данных: ${e.message}")
            }
        }

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
}
