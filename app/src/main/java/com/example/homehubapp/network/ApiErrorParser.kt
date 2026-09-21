package com.example.homehubapp.network

import org.json.JSONObject
import retrofit2.Response

object ApiErrorParser {
    fun message(response: Response<*>, fallback: String): String {
        return try {
            val raw = response.errorBody()?.string().orEmpty()
            if (raw.isBlank()) fallback
            else JSONObject(raw).optString("message", raw.trim('"'))
        } catch (_: Exception) {
            fallback
        }
    }
}
