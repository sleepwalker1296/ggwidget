package com.example.ggwidget.network

import com.google.gson.annotations.SerializedName
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET

// Модель данных с исправленными названиями полей
data class GeckoResponse(
    val data: GeckoData? = null
)

data class GeckoData(
    val attributes: GeckoAttributes? = null
)

data class GeckoAttributes(
    @SerializedName("base_token_price_usd") val priceUsd: Float? = 0.00f,
    @SerializedName("price_change_percentage") val priceChange: PriceChange? = null
)

// Новый класс для вложенного JSON
data class PriceChange(
    @SerializedName("h1") val h1: Float? = 0.00f,
    @SerializedName("h6") val h6: Float? = 0.00f,
    @SerializedName("h24") val h24: Float? = 0.00f
)

// Интерфейс API
interface GeckoApiService {
    @GET("api/v2/networks/ton/pools/EQAoJ9eh8MoKzErNE86N1uHzp4Eskth5Od5tDEYgS5mVU_Fj")
    suspend fun getCryptoPrice(): GeckoResponse
}

// Retrofit-клиент с логированием
object GeckoApi {
    private val retrofit = Retrofit.Builder()
        .baseUrl("https://api.geckoterminal.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val service: GeckoApiService = retrofit.create(GeckoApiService::class.java)
}
