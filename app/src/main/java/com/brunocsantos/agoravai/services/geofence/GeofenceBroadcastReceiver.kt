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
import com.brunocsantos.agoravai.notifications.CompromissoAlarmScheduler
import com.brunocsantos.agoravai.notifications.calcularHorarios
import com.brunocsantos.agoravai.notifications.estaNaJanelaDeChegada
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

        // goAsync mantém o receiver vivo até o acesso ao banco terminar (LEMB-09.d)
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dados = dao.getCompromissoComEtapasById(compromissoId) ?: return@launch
                val compromisso = dados.compromisso

                // Só processa se o compromisso está ativo e a chegada ainda não foi confirmada
                if (!compromisso.isEnabled || compromisso.confirmouChegada) return@launch

                val horarios = calcularHorarios(
                    compromisso.dataHoraEvento,
                    compromisso.tempoDeslocamentoMinutos,
                    compromisso.tempoDeSegurancaMinutos,
                    dados.etapas.map { it.duracaoMinutos }
                )
                if (!estaNaJanelaDeChegada(System.currentTimeMillis(), horarios, compromisso.dataHoraEvento)) {
                    // Fora do período: ignora e continua monitorando para a chegada de verdade (LEMB-09.c)
                    Log.d("AgoraVai-Geofence", "Entrada fora do período de chegada: ${compromisso.titulo}")
                    return@launch
                }

                Log.d("AgoraVai-Geofence", "Chegada detectada para: ${compromisso.titulo}")

                // 1. Quem chegou também saiu: grava as duas confirmações
                dao.confirmarSaida(compromissoId)
                dao.confirmarChegada(compromissoId)

                // 2. Cancela etapas, saída e atraso que ainda não dispararam, e fecha o lembrete de saída se estiver aberto (LEMB-09.b)
                CompromissoAlarmScheduler.cancelar(context, compromissoId, dados.etapas.size)
                notificationManager.cancel(CompromissoAlarmScheduler.idAlarmeSaida(compromissoId))

                // 3. Mostra Notificação de Sucesso
                val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_launcher_foreground)
                    .setContentTitle("Você chegou! \uD83C\uDFC1")
                    .setContentText("Destino alcançado: ${compromisso.titulo}")
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true)
                    .build()

                notificationManager.notify((compromissoId * 100 + 999).toInt(), notification)

                // 4. Remove o Geofence (Limpeza)
                CompromissoGeofenceManager.removeGeofence(compromissoId)
            } finally {
                pendingResult.finish()
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