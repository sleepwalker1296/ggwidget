package com.example.ggwidget

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.ggwidget.network.ExchangeRateApi
import com.example.ggwidget.network.GeckoApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {
    private lateinit var tokenPriceText: TextView
    private lateinit var exchangeRateText: TextView
    private lateinit var btnRefresh: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Инициализация UI элементов
        tokenPriceText = findViewById(R.id.token_price)
        exchangeRateText = findViewById(R.id.exchange_rate)
        btnRefresh = findViewById(R.id.btn_refresh)

        // Загружаем курс при запуске
        fetchCryptoPrice()
        fetchExchangeRate()

        // Обновляем данные при нажатии кнопки
        btnRefresh.setOnClickListener {
            fetchCryptoPrice()
            fetchExchangeRate()
        }
    }

    // Запрашиваем курс криптовалюты
    private fun fetchCryptoPrice() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = GeckoApi.service.getCryptoPrice()
                val newPrice = response.data?.attributes?.base_token_price_usd?.toFloatOrNull() ?: 0.00f

                Log.d("Crypto", "Цена токена: $newPrice")

                withContext(Dispatchers.Main) {
                    tokenPriceText.text = "Цена токена: $%.2f".format(newPrice)
                }
            } catch (e: Exception) {
                Log.e("Crypto", "Ошибка загрузки данных", e)
            }
        }
    }

    // Запрашиваем курс валют
    private fun fetchExchangeRate() {
        lifecycleScope.launch(Dispatchers.IO) {
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
    }
}
