package com.brunocsantos.agoravai.services.geofence

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.brunocsantos.agoravai.data.AutomationSetting
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices

object GeofenceManager {

    private const val GEOFENCE_RADIUS_IN_METERS = 100f // Raio de 100 metros

    // PendingIntent para reutilização, aponta para o nosso BroadcastReceiver
    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(applicationContext, GeofenceBroadcastReceiver::class.java)
        PendingIntent.getBroadcast(
            applicationContext,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
    }

    private lateinit var applicationContext: Context

    fun initialize(context: Context) {
        applicationContext = context.applicationContext
    }

    fun addOrUpdateGeofence(setting: AutomationSetting) {
        if (!::applicationContext.isInitialized) {
            Log.e("AgoraVai-Geofence", "GeofenceManager não foi inicializado.")
            return
        }

        // Só adiciona se o gatilho de local estiver ativo e as coordenadas existirem
        if (setting.isEnabled && setting.isLocationEnabled && setting.latitude != null && setting.longitude != null) {
            add(setting)
        } else {
            remove(setting.id)
        }
    }

    private fun add(setting: AutomationSetting) {
        val geofence = Geofence.Builder()
            .setRequestId(setting.id.toString()) // ID único para o geofence
            .setCircularRegion(
                setting.latitude!!,
                setting.longitude!!,
                GEOFENCE_RADIUS_IN_METERS
            )
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            // AQUI ESTÁ A MUDANÇA: Ouve tanto a ENTRADA quanto a SAÍDA
            .setTransitionTypes(
                Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT
            )
            .build()

        val geofencingRequest = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER) // Verifica se já está dentro ao adicionar
            .addGeofence(geofence)
            .build()

        val geofencingClient = LocationServices.getGeofencingClient(applicationContext)

        if (ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent).run {
                addOnSuccessListener {
                    Log.d("AgoraVai-Geofence", "Geofence '${setting.title}' (ID: ${setting.id}) adicionado com sucesso.")
                }
                addOnFailureListener {
                    Log.e("AgoraVai-Geofence", "Falha ao adicionar geofence '${setting.title}'.", it)
                }
            }
        } else {
            Log.e("AgoraVai-Geofence", "Não foi possível adicionar geofence. Permissão de localização não concedida.")
        }
    }

    fun remove(settingId: Int) {
        if (!::applicationContext.isInitialized) return

        val geofencingClient = LocationServices.getGeofencingClient(applicationContext)
        geofencingClient.removeGeofences(listOf(settingId.toString())).run {
            addOnSuccessListener {
                Log.d("AgoraVai-Geofence", "Geofence (ID: $settingId) removido com sucesso.")
            }
            addOnFailureListener {
                Log.e("AgoraVai-Geofence", "Falha ao remover geofence (ID: $settingId).", it)
            }
        }
    }
}