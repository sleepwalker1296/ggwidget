package com.example.ggwidget

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.ggwidget.network.ExchangeRateApi
import com.example.ggwidget.network.GeckoApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {
    private lateinit var tokenPriceText: TextView
    private lateinit var tokenChange1H: TextView
    private lateinit var tokenChange6H: TextView
    private lateinit var tokenChange24H: TextView
    private lateinit var exchangeRateText: TextView
    private lateinit var settingsButton: ImageButton

    private var lastPriceChange: Float? = null // Хранит предыдущее значение %
    private val updateInterval = 30_000L // Интервал обновления (30 секунд)

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        window.statusBarColor = resources.getColor(R.color.backgroundDark, theme)
        window.navigationBarColor = resources.getColor(R.color.backgroundDark, theme)

        // Отключаем фиолетовый оверлей на статус-баре
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        }

        // ✅ Проверяем и запрашиваем разрешение на точные будильники (AlarmManager)
        requestExactAlarmPermission()

        // Инициализируем UI элементы
        tokenPriceText = findViewById(R.id.token_price)
        tokenChange1H = findViewById(R.id.token_change_1h)
        tokenChange6H = findViewById(R.id.token_change_6h)
        tokenChange24H = findViewById(R.id.token_change_24h)
        exchangeRateText = findViewById(R.id.exchange_rate)
        settingsButton = findViewById(R.id.settings_button)

        // Обработчик нажатия на кнопку настроек
        settingsButton.setOnClickListener {
            Log.d("Settings", "Кнопка настроек нажата!")
        }

        // Запуск автообновления
        startAutoUpdate()
    }

    // ✅ Метод для автообновления данных каждые 30 секунд
    private fun startAutoUpdate() {
        lifecycleScope.launch(Dispatchers.IO) {
            while (true) {
                fetchCryptoPrice()
                fetchExchangeRate()
                delay(updateInterval)
            }
        }
    }

    // ✅ Запрос разрешения на точные будильники (AlarmManager)
    private fun requestExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                Log.e("Permission", "Запрос разрешения на точные будильники")
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                startActivity(intent)
            }
        }
    }

    // ✅ Запрашиваем курс криптовалюты
    private fun fetchCryptoPrice() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = GeckoApi.service.getCryptoPrice()
                val attributes = response.data?.attributes

                val newPrice = attributes?.base_token_price_usd ?: 0.00f
                val priceChange1H = attributes?.price_change_percentage_h1 ?: lastPriceChange ?: 0.00f
                val priceChange6H = attributes?.price_change_percentage_h6 ?: lastPriceChange ?: 0.00f
                val priceChange24H = attributes?.price_change_percentage_24h ?: lastPriceChange ?: 0.00f

                lastPriceChange = priceChange24H  // Сохраняем последнее значение

                Log.d("CryptoUpdate", "Цена: $newPrice, 1H: $priceChange1H%, 6H: $priceChange6H%, 24H: $priceChange24H%")

                withContext(Dispatchers.Main) {
                    updatePriceDisplay(newPrice, priceChange1H, priceChange6H, priceChange24H)
                }
            } catch (e: Exception) {
                Log.e("Crypto", "Ошибка загрузки данных", e)
            }
        }
    }

    // ✅ Обновление курса валют
    private suspend fun fetchExchangeRate() {
        try {
            val response = ExchangeRateApi.service.getExchangeRates()
            val rubRate = response.rates["RUB"] ?: 0.00f
            val eurRate = response.rates["EUR"] ?: 0.00f

            Log.d("ExchangeRate", "Курс USD/RUB: $rubRate, USD/EUR: $eurRate")

            withContext(Dispatchers.Main) {
                exchangeRateText.text = "USD/RUB: %.2f, USD/EUR: %.2f".format(rubRate, eurRate)
            }
        } catch (e: Exception) {
            Log.e("ExchangeRate", "Ошибка загрузки данных", e)
        }
    }

    // ✅ Обновление UI
    private fun updatePriceDisplay(price: Float, change1H: Float, change6H: Float, change24H: Float) {
        val formattedPrice = "$%.2f".format(price)
        val formattedChange1H = formatPercentage(change1H)
        val formattedChange6H = formatPercentage(change6H)
        val formattedChange24H = formatPercentage(change24H)

        // ✅ Обновляем цену токена
        if (tokenPriceText.text.toString() != formattedPrice) {
            tokenPriceText.text = formattedPrice
        }

        // ✅ Обновляем 1H изменение
        tokenChange1H.apply {
            text = formattedChange1H
            setTextColor(getColorForPercentage(change1H))
        }

        // ✅ Обновляем 6H изменение
        tokenChange6H.apply {
            text = formattedChange6H
            setTextColor(getColorForPercentage(change6H))
        }

        // ✅ Обновляем 24H изменение
        tokenChange24H.apply {
            text = formattedChange24H
            setTextColor(getColorForPercentage(change24H))
        }
    }

    // ✅ Форматирование процента (чтобы всегда был + или -)
    private fun formatPercentage(value: Float): String {
        return if (value >= 0) "+%.2f%%".format(value) else "%.2f%%".format(value)
    }

    // ✅ Получение цвета для процента
    private fun getColorForPercentage(value: Float): Int {
        return if (value >= 0) getColor(R.color.green) else getColor(R.color.red)
    }
}
