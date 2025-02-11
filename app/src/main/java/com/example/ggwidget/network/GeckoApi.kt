package com.example.ggwidget.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET

// Модель данных (добавляем обработку `null`)
data class GeckoResponse(
    val data: GeckoData? = null
)

data class GeckoData(
    val attributes: GeckoAttributes? = null
)

data class GeckoAttributes(
    val base_token_price_usd: Float? = 0.00f, // Дефолтное значение, если null
    val price_change_percentage_24h: Float? = 0.00f, // Дефолтное значение, если null
    val price_change_percentage_h1: Float? = 0.00f, // Добавлено
    val price_change_percentage_h6: Float? = 0.00f, // Добавлено
)

// Интерфейс API
interface GeckoApiService {
    @GET("api/v2/networks/ton/pools/EQAoJ9eh8MoKzErNE86N1uHzp4Eskth5Od5tDEYgS5mVU_Fj")
    suspend fun getCryptoPrice(): GeckoResponse
}

// Retrofit-клиент
object GeckoApi {
    private val retrofit = Retrofit.Builder()
        .baseUrl("https://api.geckoterminal.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val service: GeckoApiService = retrofit.create(GeckoApiService::class.java)
}
