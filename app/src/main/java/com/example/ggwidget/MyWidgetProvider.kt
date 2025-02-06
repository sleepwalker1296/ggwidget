package com.example.ggwidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import com.example.ggwidget.worker.WidgetUpdateScheduler
import android.content.Intent
import android.util.Log
import android.widget.RemoteViews
import androidx.lifecycle.lifecycleScope
import com.example.ggwidget.network.GeckoApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MyWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)

        // Запускаем обновление виджета через WorkManager
        WidgetUpdateScheduler.scheduleWidgetUpdate(context)
    }
}

    private fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        val views = RemoteViews(context.packageName, R.layout.widget_layout)

        // Запускаем обновление курса через API
        GlobalScope.launch(Dispatchers.IO) {
            try {
                val response = GeckoApi.service.getCryptoPrice()
                val newPrice = response.data?.attributes?.base_token_price_usd?.toFloatOrNull() ?: 0.00f
                val priceChange = response.data?.attributes?.price_change_percentage_h1?.toFloatOrNull() ?: 0.00f

                Log.d("CryptoWidget", "Цена токена: $newPrice, Изменение: $priceChange%")

                withContext(Dispatchers.Main) {
                    views.setTextViewText(R.id.widget_token_price, "$%.2f".format(newPrice))
                    views.setTextViewText(R.id.widget_token_change, "%.2f%%".format(priceChange))

                    // Меняем цвет текста в зависимости от роста или падения цены
                    val color = if (priceChange >= 0) android.graphics.Color.GREEN else android.graphics.Color.RED
                    views.setTextColor(R.id.widget_token_change, color)

                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }
            } catch (e: Exception) {
                Log.e("CryptoWidget", "Ошибка загрузки данных", e)
            }
        }

        // Настраиваем обновление при нажатии на виджет
        val intent = Intent(context, MyWidgetProvider::class.java)
        val pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        views.setOnClickPendingIntent(R.id.widget_token_price, pendingIntent)

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

