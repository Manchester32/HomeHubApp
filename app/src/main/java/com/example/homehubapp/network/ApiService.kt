package com.example.homehubapp.network

import com.example.homehubapp.model.*
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface ApiService {

    @POST("api/Auth/register")
    fun register(@Body request: RegisterRequest): Call<ApiResponse>

    @POST("api/Auth/login")
    fun login(@Body request: LoginRequest): Call<LoginResponse>

    @GET("api/Services")
    fun getServices(): Call<List<ServiceDto>>

    @POST("api/Requests")
    fun createServiceRequest(@Body request: CreateServiceRequest): Call<RequestCreatedResponse>

    @GET("api/Requests/user/{userId}")
    fun getUserRequests(@Path("userId") userId: Int): Call<List<ServiceRequestDto>>

    @PUT("api/Requests/{id}/status")
    fun updateRequestStatus(
        @Path("id") id: Int,
        @Body request: UpdateStatusRequest
    ): Call<ApiResponse>

    @POST("api/Payments")
    fun createPayment(@Body request: CreatePaymentRequest): Call<PaymentResponse>

    @GET("api/Users/{id}")
    fun getUser(@Path("id") id: Int): Call<UserProfile>

    @PUT("api/Users/{id}")
    fun updateUser(
        @Path("id") id: Int,
        @Body request: UpdateProfileRequest
    ): Call<UserProfile>
}
