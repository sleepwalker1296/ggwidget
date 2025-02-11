package com.example.ggwidget

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import android.widget.Button
import android.widget.RadioGroup

class SettingsActivity : AppCompatActivity() {

    private lateinit var themeRadioGroup: RadioGroup
    private lateinit var saveButton: Button
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        sharedPreferences = getSharedPreferences("AppSettings", MODE_PRIVATE)

        themeRadioGroup = findViewById(R.id.themeRadioGroup)
        saveButton = findViewById(R.id.saveButton)

        // Загружаем текущую тему
        when (sharedPreferences.getString("theme", "system")) {
            "light" -> themeRadioGroup.check(R.id.radioLight)
            "dark" -> themeRadioGroup.check(R.id.radioDark)
            else -> themeRadioGroup.check(R.id.radioSystem)
        }

        // Сохраняем выбранную тему
        saveButton.setOnClickListener {
            val editor = sharedPreferences.edit()
            val selectedTheme = when (themeRadioGroup.checkedRadioButtonId) {
                R.id.radioLight -> "light"
                R.id.radioDark -> "dark"
                else -> "system"
            }
            editor.putString("theme", selectedTheme)
            editor.apply()

            applyTheme(selectedTheme)
        }
    }

    private fun applyTheme(theme: String) {
        when (theme) {
            "light" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            "dark" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }

        // Перезапускаем активность, чтобы применить тему
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
