package com.brunocsantos.agoravai.network

import com.google.gson.annotations.SerializedName

data class DirectionsResponse(
    @SerializedName("routes") val routes: List<Route>,
    @SerializedName("status") val status: String
)

data class Route(
    @SerializedName("legs") val legs: List<Leg>
)

data class Leg(
    @SerializedName("duration") val duration: Duration
)

data class Duration(
    @SerializedName("value") val valueInSeconds: Int,
    @SerializedName("text") val text: String
)