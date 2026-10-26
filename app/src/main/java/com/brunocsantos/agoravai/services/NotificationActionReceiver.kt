package com.brunocsantos.agoravai.services

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.brunocsantos.agoravai.R
import com.brunocsantos.agoravai.data.AppDatabase
import com.brunocsantos.agoravai.notifications.CHANNEL_ID
import com.brunocsantos.agoravai.notifications.CompromissoAlarmScheduler
import com.brunocsantos.agoravai.notifications.EXTRA_ACTION_NOTIFICATION_ID
import com.brunocsantos.agoravai.utils.SettingsManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationId = intent.getIntExtra(EXTRA_ACTION_NOTIFICATION_ID, 0)

        // ID do compromisso (usado para as novas ações)
        val compromissoId = intent.getLongExtra("EXTRA_COMPROMISSO_ID", -1L)

        when (intent.action) {
            // --- Lógica do Pomodoro (Mantida) ---
            Actions.START.toString() -> {
                val cycles = intent.getIntExtra("EXTRA_CYCLES", SettingsManager.DEFAULT_CYCLES)
                val serviceIntent = Intent(context, PomodoroService::class.java).apply {
                    action = PomodoroService.Actions.START.toString()
                    putExtra("EXTRA_CYCLES", cycles)
                }
                context.startService(serviceIntent)
                if (notificationId != 0) notificationManager.cancel(notificationId)
            }

            Actions.DISMISS.toString() -> {
                if (notificationId != 0) notificationManager.cancel(notificationId)
            }

            // --- NOVA AÇÃO: Confirmar que já saiu ---
            Actions.CONFIRM_EXIT.toString() -> {
                if (compromissoId != -1L) {
                    CoroutineScope(Dispatchers.IO).launch {
                        val dao = AppDatabase.getDatabase(context).appDao()
                        dao.confirmarSaida(compromissoId)

                        // Cancela o alarme de verificação de atraso (LEMB-07)
                        CompromissoAlarmScheduler.cancelarVerificacaoAtraso(context, compromissoId)
                    }
                    // Fecha a notificação de "Hora de Sair"
                    if (notificationId != 0) notificationManager.cancel(notificationId)
                }
            }

            // --- NOVA AÇÃO: Checar se está atrasado ---
            Actions.CHECK_LATE.toString() -> {
                if (compromissoId != -1L) {
                    CoroutineScope(Dispatchers.IO).launch {
                        val dao = AppDatabase.getDatabase(context).appDao()
                        val compromisso = dao.getCompromissoById(compromissoId)

                        // Se o compromisso existe E o usuário AINDA NÃO confirmou a saída
                        if (compromisso != null && !compromisso.confirmouSaida) {

                            // Mostra notificação de atraso em VERMELHO
                            val notif = NotificationCompat.Builder(context, CHANNEL_ID)
                                .setSmallIcon(R.drawable.ic_launcher_foreground) // Use um ícone de alerta se tiver
                                .setContentTitle("Você está atrasado! \uD83C\uDFC3")
                                .setContentText("Ainda dá tempo! Saia agora para: ${compromisso.titulo}")
                                .setPriority(NotificationCompat.PRIORITY_HIGH)
                                .setColor(0xFFFF0000.toInt()) // Vermelho
                                .setAutoCancel(true)
                                .build()

                            // ID para o atraso: ID*100 + 98
                            notificationManager.notify((compromissoId * 100 + 98).toInt(), notif)
                        }
                    }
                }
            }
        }
    }

    enum class Actions {
        START, DISMISS, CONFIRM_EXIT, CHECK_LATE
    }
}