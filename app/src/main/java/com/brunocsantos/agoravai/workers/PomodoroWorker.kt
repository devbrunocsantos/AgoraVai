package com.brunocsantos.agoravai.workers

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.brunocsantos.agoravai.R
import com.brunocsantos.agoravai.data.AppDatabase
import com.brunocsantos.agoravai.notifications.CHANNEL_ID
import com.brunocsantos.agoravai.services.NotificationActionReceiver
import com.brunocsantos.agoravai.services.geofence.GeofenceManager
import java.time.LocalDate

class PomodoroWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        Log.d("AgoraVai-Debug", "PomodoroWorker: Iniciando verificação de condições...")

        val automationId = tags.firstOrNull { it.startsWith("pomodoro_automation_") }
            ?.substringAfter("pomodoro_automation_")
            ?.toIntOrNull()

        if (automationId == null) {
            Log.e("AgoraVai-Debug", "Worker não conseguiu extrair o ID da automação da tag.")
            return Result.failure()
        }

        val dao = AppDatabase.getDatabase(context).appDao()
        val setting = dao.getAutomationSettingById(automationId)

        if (setting == null) {
            Log.e("AgoraVai-Debug", "Worker não encontrou a automação com ID: $automationId.")
            return Result.success()
        }

        if (!setting.isEnabled) {
            Log.d("AgoraVai-Debug", "Automação ${setting.id} está desativada. Saindo.")
            return Result.success()
        }

        val repetitionAllowed = !setting.isRepeatEnabled || setting.repeatDays.contains(LocalDate.now().dayOfWeek)
        if (!repetitionAllowed) {
            Log.d("AgoraVai-Debug", "Automação ${setting.id} não deve repetir hoje. Saindo.")
            return Result.success()
        }

        val prefs = context.getSharedPreferences("geofence_status", Context.MODE_PRIVATE)
        val isAtLocation = prefs.getBoolean("at_location_${setting.id}", false)
        val locationConditionMet = !setting.isLocationEnabled || isAtLocation

        if (!locationConditionMet) {
            Log.d("AgoraVai-Debug", "Automação ${setting.id} requer estar no local, mas o status é 'fora'. Saindo.")
            return Result.success()
        }

        // --- MUDANÇA PRINCIPAL: MOSTRAR NOTIFICAÇÃO EM VEZ DE INICIAR O SERVIÇO ---
        Log.d("AgoraVai-Debug", "CONDIÇÕES CUMPRIDAS para a automação ${setting.id}. Mostrando notificação de início.")
        showStartNotification(setting.title, automationId, setting.cycles)

        // A lógica de arquivamento permanece a mesma
        if (!setting.isRepeatEnabled) {
            Log.d("AgoraVai-Debug", "Automação ${setting.id} é do tipo 'uma vez'. Auto-arquivando...")
            dao.markAsCompleted(setting.id)
            if (setting.isLocationEnabled) {
                GeofenceManager.remove(setting.id)
            }
            Log.d("AgoraVai-Debug", "Automação ${setting.id} arquivada com sucesso.")
        }

        return Result.success()
    }

    private fun showStartNotification(title: String, notificationId: Int, cycles: Int) {
        // Intent para o botão "Iniciar"
        val startIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.Actions.START.toString()
            putExtra("EXTRA_NOTIFICATION_ID", notificationId)
            putExtra("EXTRA_CYCLES", cycles)
        }
        val startPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId, // Usa o ID da automação como request code para ser único
            startIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Intent para o botão "Ignorar" (ou ao fechar a notificação)
        val dismissIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.Actions.DISMISS.toString()
            putExtra("EXTRA_NOTIFICATION_ID", notificationId)
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId + 1, // request code diferente para evitar conflito
            dismissIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Use um ícone adequado
            .setContentTitle("Hora de Focar!")
            .setContentText("Pronto para iniciar sua sessão de '${title}'?")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setDeleteIntent(dismissPendingIntent) // Disparado quando a notificação é fechada
            .addAction(R.drawable.ic_launcher_foreground, "Iniciar", startPendingIntent)
            .addAction(R.drawable.ic_launcher_foreground, "Ignorar", dismissPendingIntent)


        try {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
        } catch (e: SecurityException) {
            // Pode acontecer em versões mais novas do Android se a permissão não for concedida
            Log.e("PomodoroWorker", "Falha ao mostrar notificação por falta de permissão.", e)
        }
    }
}