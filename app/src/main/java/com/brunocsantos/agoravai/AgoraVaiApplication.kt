package com.brunocsantos.agoravai

import android.app.Application
import com.brunocsantos.agoravai.services.geofence.CompromissoGeofenceManager
import com.brunocsantos.agoravai.services.geofence.GeofenceManager

class AgoraVaiApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // Inicializados aqui, e não na MainActivity, para funcionarem também em receivers
        // e workers que rodam com o app fechado (LEMB-09, AUTO-08)
        GeofenceManager.initialize(this)
        CompromissoGeofenceManager.initialize(this)
    }
}
