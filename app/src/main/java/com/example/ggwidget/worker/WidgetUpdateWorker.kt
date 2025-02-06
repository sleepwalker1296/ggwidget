package com.example.ggwidget.worker

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.util.Log
import android.widget.RemoteViews
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.ggwidget.MyWidgetProvider
import com.example.ggwidget.R
import com.example.ggwidget.network.GeckoApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

class WidgetUpdateWorker(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {

    override fun doWork(): Result {
        val context = applicationContext

        return try {
            runBlocking {
                val response = GeckoApi.service.getCryptoPrice()
                val newPrice = response.data?.attributes?.base_token_price_usd?.toFloatOrNull() ?: 0.00f
                val priceChange = response.data?.attributes?.price_change_percentage_h1?.toFloatOrNull() ?: 0.00f

                updateWidget(context, newPrice, priceChange)
            }
            Log.d("WidgetWorker", "Виджет обновлён")
            Result.success()
        } catch (e: Exception) {
            Log.e("WidgetWorker", "Ошибка обновления виджета", e)
            Result.retry()
        }
    }

    private suspend fun updateWidget(context: Context, price: Float, change: Float) {
        withContext(Dispatchers.Main) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, MyWidgetProvider::class.java)
            val widgetIds = appWidgetManager.getAppWidgetIds(componentName)

            for (widgetId in widgetIds) {
                val views = RemoteViews(context.packageName, R.layout.widget_layout)
                views.setTextViewText(R.id.widget_token_price, "$%.2f".format(price))
                views.setTextViewText(R.id.widget_token_change, "%.2f%%".format(change))

                val color = if (change >= 0) android.graphics.Color.GREEN else android.graphics.Color.RED
                views.setTextColor(R.id.widget_token_change, color)

                appWidgetManager.updateAppWidget(widgetId, views)
            }
        }
    }
}
