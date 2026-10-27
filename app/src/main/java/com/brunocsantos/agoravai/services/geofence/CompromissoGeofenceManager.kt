package com.brunocsantos.agoravai.services.geofence

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.brunocsantos.agoravai.data.Compromisso
import com.brunocsantos.agoravai.data.Etapa
import com.brunocsantos.agoravai.notifications.fimDaJanelaDeChegada
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices

object CompromissoGeofenceManager {

    // Raio de 200 metros para considerar que "Chegou"
    private const val GEOFENCE_RADIUS_IN_METERS = 200f
    private lateinit var applicationContext: Context

    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(applicationContext, GeofenceBroadcastReceiver::class.java)
        PendingIntent.getBroadcast(
            applicationContext,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
    }

    fun initialize(context: Context) {
        applicationContext = context.applicationContext
    }

    fun addGeofenceChegada(compromisso: Compromisso, etapas: List<Etapa>) {
        if (!::applicationContext.isInitialized) return
        if (!compromisso.isEnabled) return

        // Verifica se temos coordenadas válidas
        if (compromisso.latitudeDestino == 0.0 || compromisso.longitudeDestino == 0.0) {
            Log.e("CompromissoGeofence", "Coordenadas inválidas para compromisso ${compromisso.id}")
            return
        }

        // LEMB-09: o monitoramento termina sozinho no fim do período de chegada
        val duracaoMonitoramento = fimDaJanelaDeChegada(compromisso.dataHoraEvento) - System.currentTimeMillis()
        if (duracaoMonitoramento <= 0) {
            Log.d("CompromissoGeofence", "Período de chegada já terminou para compromisso ${compromisso.id}")
            return
        }

        val geofence = Geofence.Builder()
            .setRequestId("compromisso_chegada_${compromisso.id}") // ID único com prefixo
            .setCircularRegion(
                compromisso.latitudeDestino,
                compromisso.longitudeDestino,
                GEOFENCE_RADIUS_IN_METERS
            )
            .setExpirationDuration(duracaoMonitoramento)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER) // Só queremos saber quando ENTRA
            .build()

        val request = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofence(geofence)
            .build()

        val client = LocationServices.getGeofencingClient(applicationContext)

        if (ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            client.addGeofences(request, geofencePendingIntent)
                .addOnSuccessListener {
                    Log.d("CompromissoGeofence", "Geofence de chegada adicionado: ${compromisso.titulo}")
                }
                .addOnFailureListener {
                    Log.e("CompromissoGeofence", "Falha ao adicionar geofence", it)
                }
        } else {
            Log.e("CompromissoGeofence", "Sem permissão de localização para adicionar Geofence.")
        }
    }

    fun removeGeofence(compromissoId: Long) {
        if (!::applicationContext.isInitialized) return
        val client = LocationServices.getGeofencingClient(applicationContext)
        client.removeGeofences(listOf("compromisso_chegada_$compromissoId"))
    }
}