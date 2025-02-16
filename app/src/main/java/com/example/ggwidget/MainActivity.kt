package com.example.ggwidget

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.ImageButton
import android.widget.PopupMenu
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.lifecycleScope
import com.example.ggwidget.network.ExchangeRateApi
import com.example.ggwidget.network.GeckoApi
import kotlinx.coroutines.*

class MainActivity : AppCompatActivity() {
    private lateinit var tokenPriceText: TextView
    private lateinit var tokenChange1H: TextView
    private lateinit var tokenChange6H: TextView
    private lateinit var tokenChange24H: TextView
    private lateinit var exchangeRateText: TextView
    private lateinit var settingsButton: ImageButton
    private lateinit var sharedPreferences: SharedPreferences
    private var updateJob: Job? = null // Переменная для управления корутинами

    private var lastPriceChange: Float? = null // Хранит предыдущее значение %
    private val updateInterval = 30_000L // Интервал обновления (30 секунд)

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sharedPreferences = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

        // ✅ Устанавливаем сохранённую тему перед загрузкой UI
        applySavedTheme()

        setContentView(R.layout.activity_main) // Устанавливаем контент после применения темы

        window.statusBarColor = resources.getColor(R.color.backgroundDark, theme)
        window.navigationBarColor = resources.getColor(R.color.backgroundDark, theme)

        // ✅ Проверяем и запрашиваем разрешение на точные будильники (AlarmManager)
        requestExactAlarmPermission()

        // Инициализируем UI элементы
        tokenPriceText = findViewById(R.id.token_price)
        tokenChange1H = findViewById(R.id.token_change_1h)
        tokenChange6H = findViewById(R.id.token_change_6h)
        tokenChange24H = findViewById(R.id.token_change_24h)
        exchangeRateText = findViewById(R.id.exchange_rate)
        settingsButton = findViewById(R.id.settings_button)

        // Обработчик нажатия на кнопку настроек (выпадающее меню)
        settingsButton.setOnClickListener { showPopupMenu(it) }

        // Запуск автообновления
        startAutoUpdate()
    }

    override fun onDestroy() {
        super.onDestroy()
        updateJob?.cancel() // ✅ Отмена корутин при уничтожении активности
    }

    // ✅ Метод для автообновления данных каждые 30 секунд
    private fun startAutoUpdate() {
        updateJob?.cancel() // Отменяем старую корутину, если она была запущена
        updateJob = lifecycleScope.launch(Dispatchers.IO) {
            while (isActive) { // Проверяем, активна ли корутина
                fetchCryptoPrice()
                fetchExchangeRate()
                delay(updateInterval)
            }
        }
    }

    // ✅ Функция показа выпадающего меню
    private fun showPopupMenu(view: View) {
        val popupMenu = PopupMenu(this, view)
        popupMenu.menuInflater.inflate(R.menu.settings_menu, popupMenu.menu)

        // Отмечаем текущую тему
        when (getSavedTheme()) {
            AppCompatDelegate.MODE_NIGHT_YES -> popupMenu.menu.findItem(R.id.menu_dark).isChecked = true
            AppCompatDelegate.MODE_NIGHT_NO -> popupMenu.menu.findItem(R.id.menu_light).isChecked = true
        }

        popupMenu.setOnMenuItemClickListener { item: MenuItem ->
            when (item.itemId) {
                R.id.menu_dark -> {
                    saveTheme(AppCompatDelegate.MODE_NIGHT_YES)
                    true
                }
                R.id.menu_light -> {
                    saveTheme(AppCompatDelegate.MODE_NIGHT_NO)
                    true
                }
                else -> false
            }
        }

        popupMenu.show()
    }

    // ✅ Сохранение выбранной темы
    private fun saveTheme(themeMode: Int) {
        sharedPreferences.edit().putInt("theme_mode", themeMode).apply()
        AppCompatDelegate.setDefaultNightMode(themeMode)
        recreate() // Перезапуск активности для применения темы

        // ✅ Отправляем сигнал обновления виджета
        val intent = Intent(this, MyWidgetProvider::class.java).apply {
            action = "com.example.ggwidget.UPDATE_WIDGET_THEME"
        }
        sendBroadcast(intent)

        recreate() // Перезапуск активности для применения темы

    }

    // ✅ Получение сохранённой темы
    private fun getSavedTheme(): Int {
        return sharedPreferences.getInt("theme_mode", AppCompatDelegate.MODE_NIGHT_YES) // ✅ По умолчанию тёмная тема
    }

    // ✅ Применение сохранённой темы при старте
    private fun applySavedTheme() {
        AppCompatDelegate.setDefaultNightMode(getSavedTheme())
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

                val newPrice = attributes?.priceUsd ?: 0.00f
                val priceChange1H = attributes?.priceChange?.h1 ?: lastPriceChange ?: 0.00f
                val priceChange6H = attributes?.priceChange?.h6 ?: lastPriceChange ?: 0.00f
                val priceChange24H = attributes?.priceChange?.h24 ?: lastPriceChange ?: 0.00f

                lastPriceChange = priceChange24H

                withContext(Dispatchers.Main) {
                    tokenPriceText.text = "$%.2f".format(newPrice)
                    tokenChange1H.text = "%.2f%%".format(priceChange1H)
                    tokenChange6H.text = "%.2f%%".format(priceChange6H)
                    tokenChange24H.text = "%.2f%%".format(priceChange24H)
                }
            } catch (e: Exception) {
                Log.e("Crypto", "Ошибка загрузки данных", e)
            }
        }
    }

    // ✅ Обновление курса валют
    private fun fetchExchangeRate() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = ExchangeRateApi.service.getExchangeRates()
                val rubRate = response.rates["RUB"] ?: 0.00f
                val eurRate = response.rates["EUR"] ?: 0.00f

                withContext(Dispatchers.Main) {
                    exchangeRateText.text = "USD/RUB: %.2f, USD/EUR: %.2f".format(rubRate, eurRate)
                }
            } catch (e: Exception) {
                Log.e("ExchangeRate", "Ошибка загрузки данных", e)
            }
        }
    }
}
