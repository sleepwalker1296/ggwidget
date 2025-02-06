package com.example.ggwidget.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET

// Модель данных для API обмена валют
data class ExchangeRateResponse(
    val rates: Map<String, Float>
)

// Интерфейс API
interface ExchangeRateApiService {
    @GET("v4/latest/USD")
    suspend fun getExchangeRates(): ExchangeRateResponse
}

// Retrofit-клиент
object ExchangeRateApi {
    private val retrofit = Retrofit.Builder()
        .baseUrl("https://api.exchangerate-api.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val service: ExchangeRateApiService = retrofit.create(ExchangeRateApiService::class.java)
}
