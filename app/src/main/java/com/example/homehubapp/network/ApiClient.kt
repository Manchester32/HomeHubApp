package com.example.homehubapp.network

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
    // Final PoE: the app runs on a real phone, so the API must be reachable online.
    // Use your hosted address (https://yourapp.azurewebsites.net/) or your PC's LAN IP
    // (e.g. http://192.168.1.20:5092/) when the phone is on the same Wi-Fi.
    // If the address changes, update only this line.
    private const val BASE_URL = "https://homehub20261105134433-f6dzbebrh6frcjet.canadacentral-01.azurewebsites.net/"

    // Short timeouts so a missing connection fails quickly instead of hanging
    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val service: ApiService by lazy {
        retrofit.create(ApiService::class.java)
    }
}