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
    val base_token_price_usd: String? = "0.00", // Дефолтное значение, если null
    val price_change_percentage_h1: String? = "0.00" // Дефолтное значение, если null
)

// Интерфейс API
interface GeckoApiService {
    @GET("api/v2/networks/ton/pools/EQAf2LUJZMdxSAGhlp-A60AN9bqZeVM994vCOXH05JFo-7dc")
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
