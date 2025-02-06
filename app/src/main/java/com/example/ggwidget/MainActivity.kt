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
import android.view.animation.AnimationUtils
private lateinit var tokenPriceText: TextView
private lateinit var tokenChangeText: TextView


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
        fetchCryptoPrice(updatePercent = true)
        fetchExchangeRate()

        // Обновляем данные при нажатии кнопки
        btnRefresh.setOnClickListener {
            fetchCryptoPrice(updatePercent = true)
            fetchExchangeRate()
        }
    }

    // Запрашиваем курс криптовалюты
    private fun fetchCryptoPrice(updatePercent: Boolean) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = GeckoApi.service.getCryptoPrice()
                val newPrice = response.data?.attributes?.base_token_price_usd?.toFloatOrNull() ?: 0.00f
                val priceChange = response.data?.attributes?.price_change_percentage_h24?.toFloatOrNull() ?: 0.00f // Берём данные за 24 часа

                Log.d("Crypto", "Цена токена: $newPrice, Изменение: $priceChange%")

                withContext(Dispatchers.Main) {
                    updatePriceDisplay(newPrice, priceChange)
                }
            } catch (e: Exception) {
                Log.e("Crypto", "Ошибка загрузки данных", e)
            }
        }
    }

    private fun updatePriceDisplay(price: Float, change: Float) {
        val formattedPrice = "$%.2f".format(price)
        val formattedChange = "%.2f%%".format(change)

        tokenPriceText.text = formattedPrice
        tokenChangeText.text = if (change >= 0) "+$formattedChange" else formattedChange
        tokenChangeText.setTextColor(if (change >= 0) android.graphics.Color.GREEN else android.graphics.Color.RED)
    }

    private fun animatePriceChange() {
        val fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in)
        tokenPriceText.startAnimation(fadeIn)
        tokenChangeText.startAnimation(fadeIn)
    }

    private fun startLoadingAnimation() {
        val rotate = AnimationUtils.loadAnimation(this, R.anim.rotate)
        btnRefresh.startAnimation(rotate)
    }

    // Останавливаем анимацию загрузки
    private fun stopLoadingAnimation() {
        btnRefresh.clearAnimation()
    }

    // Анимация вспышки при изменении цены
    private fun animatePriceFlash(change: Float) {
        val flashAnimation = if (change >= 0) {
            AnimationUtils.loadAnimation(this, R.anim.flash_green)
        } else {
            AnimationUtils.loadAnimation(this, R.anim.flash_red)
        }
        tokenPriceText.startAnimation(flashAnimation)
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
