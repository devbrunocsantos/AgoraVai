package com.brunocsantos.agoravai.network

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface GoogleMapsApiService {

    @GET("maps/api/directions/json")
    suspend fun getDirections(
        @Query("origin") origin: String,
        @Query("destination") destination: String,
        @Query("key") apiKey: String
    ): Response<DirectionsResponse>
}