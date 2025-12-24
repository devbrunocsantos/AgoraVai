package com.brunocsantos.agoravai.services.geofence

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.brunocsantos.agoravai.R
import com.brunocsantos.agoravai.data.AppDatabase
import com.brunocsantos.agoravai.notifications.CHANNEL_ID
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class GeofenceBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val geofencingEvent = GeofencingEvent.fromIntent(intent)

        if (geofencingEvent?.hasError() == true) {
            Log.e("AgoraVai-Geofence", "Erro no Geofence: ${geofencingEvent.errorCode}")
            return
        }

        val triggerList = geofencingEvent?.triggeringGeofences ?: return

        for (geofence in triggerList) {
            val requestId = geofence.requestId

            // --- TIPO 1: GEOFENCE DE POMODORO (IDs numéricos) ---
            if (requestId.all { it.isDigit() }) {
                handlePomodoroGeofence(context, requestId, geofencingEvent.geofenceTransition)
            }
            // --- TIPO 2: GEOFENCE DE COMPROMISSO (Prefixo específico) ---
            else if (requestId.startsWith("compromisso_chegada_")) {
                // Só nos importamos com a entrada (Chegada)
                if (geofencingEvent.geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
                    val compromissoId = requestId.removePrefix("compromisso_chegada_").toLongOrNull()
                    if (compromissoId != null) {
                        handleCompromissoChegada(context, compromissoId)
                    }
                }
            }
        }
    }

    private fun handleCompromissoChegada(context: Context, compromissoId: Long) {
        val dao = AppDatabase.getDatabase(context).appDao()
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Usamos GlobalScope ou um Scope manual pois o Receiver morre rápido.
        // GoAsync() é outra opção, mas aqui o processamento é rápido.
        CoroutineScope(Dispatchers.IO).launch {
            val compromisso = dao.getCompromissoById(compromissoId)

            // Só processa se o compromisso existe e ainda não foi confirmado
            if (compromisso != null && !compromisso.confirmouChegada) {
                Log.d("AgoraVai-Geofence", "Chegada detectada para: ${compromisso.titulo}")

                // 1. Atualiza DB
                dao.confirmarChegada(compromissoId)

                // 2. Mostra Notificação de Sucesso
                val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_launcher_foreground) // Certifique-se de ter este ícone ou use outro
                    .setContentTitle("Você chegou! \uD83C\uDFC1")
                    .setContentText("Destino alcançado: ${compromisso.titulo}")
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true)
                    .build()

                notificationManager.notify((compromissoId * 100 + 999).toInt(), notification)

                // 3. Remove o Geofence (Limpeza)
                CompromissoGeofenceManager.removeGeofence(compromissoId)
            }
        }
    }

    private fun handlePomodoroGeofence(context: Context, geofenceId: String, transition: Int) {
        // Esta lógica permanece a mesma do seu código anterior
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val prefs = context.getSharedPreferences("geofence_status", Context.MODE_PRIVATE)
                val key = "at_location_$geofenceId"

                if (transition == Geofence.GEOFENCE_TRANSITION_ENTER) {
                    Log.d("AgoraVai-Geofence", "Pomodoro: DENTRO do local ($geofenceId)")
                    prefs.edit().putBoolean(key, true).apply()

                    val dao = AppDatabase.getDatabase(context).appDao()
                    val setting = dao.getAutomationSettingById(geofenceId.toInt())

                    if (setting != null && setting.isEnabled) {
                        val workRequest = OneTimeWorkRequestBuilder<com.brunocsantos.agoravai.workers.PomodoroWorker>()
                            .addTag("pomodoro_automation_$geofenceId")
                            .build()
                        WorkManager.getInstance(context).enqueue(workRequest)
                    }
                } else if (transition == Geofence.GEOFENCE_TRANSITION_EXIT) {
                    Log.d("AgoraVai-Geofence", "Pomodoro: FORA do local ($geofenceId)")
                    prefs.edit().putBoolean(key, false).apply()
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}