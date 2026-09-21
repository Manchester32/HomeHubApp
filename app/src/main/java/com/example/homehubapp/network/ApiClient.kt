package com.example.homehubapp.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {
    // BlueStacks connects to the Windows host through this local network address.
    // If your PC/VM IP changes, update only this line.
    private const val BASE_URL = "http://10.0.0.4:5092/"

    val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val service: ApiService by lazy {
        retrofit.create(ApiService::class.java)
    }
}
