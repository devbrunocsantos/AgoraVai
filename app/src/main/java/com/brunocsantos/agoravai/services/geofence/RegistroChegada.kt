package com.brunocsantos.agoravai.services.geofence

import android.app.NotificationManager
import android.content.Context
import android.util.Log
import androidx.core.app.NotificationCompat
import com.brunocsantos.agoravai.R
import com.brunocsantos.agoravai.data.AppDatabase
import com.brunocsantos.agoravai.data.CompromissoComEtapas
import com.brunocsantos.agoravai.notifications.CHANNEL_ID
import com.brunocsantos.agoravai.notifications.CompromissoAlarmScheduler

/**
 * LEMB-09: o que acontece quando a chegada ao destino é reconhecida. Usado pela entrada
 * na geofence e pelo reagendamento após reiniciar (LEMB-11.i).
 *
 * Não refaz a checagem do período de chegada: quem chama garante que está dentro dele.
 */
object RegistroChegada {

    suspend fun registrar(context: Context, dados: CompromissoComEtapas) {
        val compromisso = dados.compromisso
        val compromissoId = compromisso.id
        val dao = AppDatabase.getDatabase(context).appDao()
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

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
            .setContentTitle("Você chegou! 🏁")
            .setContentText("Destino alcançado: ${compromisso.titulo}")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify((compromissoId * 100 + 999).toInt(), notification)

        // 4. Remove o Geofence (Limpeza)
        CompromissoGeofenceManager.removeGeofence(compromissoId)
    }
}
