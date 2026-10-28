package com.brunocsantos.agoravai.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.brunocsantos.agoravai.data.Compromisso
import com.brunocsantos.agoravai.data.Etapa
import com.brunocsantos.agoravai.services.NotificationActionReceiver

/**
 * Agenda e cancela todos os lembretes de um compromisso (etapas, saída e atraso).
 * Os request codes seguem a convenção de architecture.md §5.2 e não podem mudar,
 * senão os cancelamentos deixam de acertar alarmes já agendados.
 */
object CompromissoAlarmScheduler {

    private const val TAG = "AgoraVai-Debug"

    // Margem de alarmes de etapa cancelados além do número atual de etapas (edições podem ter removido etapas)
    private const val MARGEM_CANCELAMENTO_ETAPAS = 5

    fun idAlarmeEtapa(compromissoId: Long, indiceInvertido: Int) = (compromissoId.toInt() * 100) + indiceInvertido
    fun idAlarmeSaida(compromissoId: Long) = (compromissoId.toInt() * 100) + 99
    fun idAlarmeAtivacaoChegada(compromissoId: Long) = (compromissoId.toInt() * 100) + 97
    private fun requestCodeConfirmarSaida(compromissoId: Long) = compromissoId.toInt()
    private fun requestCodeVerificacaoAtraso(compromissoId: Long) = compromissoId.toInt() + 9999

    fun podeAgendarExato(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        return alarmManager.canScheduleExactAlarms()
    }

    fun agendar(context: Context, compromisso: Compromisso, etapas: List<Etapa>, itens: List<String>) {
        if (!compromisso.isEnabled) return
        val horarios = calcularHorarios(
            compromisso.dataHoraEvento,
            compromisso.tempoDeslocamentoMinutos,
            compromisso.tempoDeSegurancaMinutos,
            etapas.map { it.duracaoMinutos }
        )

        // O índice do alarme de etapa é contado de trás para frente, como sempre foi
        etapas.forEachIndexed { index, etapa ->
            NotificationScheduler.scheduleAlarm(
                context,
                horarios.inicioEtapas[index],
                idAlarmeEtapa(compromisso.id, etapas.size - 1 - index),
                "Próximo Preparativo: ${etapa.descricao}",
                "Faltam ${etapa.duracaoMinutos} min."
            )
        }

        agendarSaida(context, compromisso, horarios.saida, itens)
        agendarExato(context, horarios.verificacaoAtraso, pendingVerificacaoAtraso(context, compromisso.id))

        // LEMB-09: a geofence de chegada só é registrada no início do período. Se esse horário
        // já passou (ex.: religar no meio do período), o AlarmManager dispara na hora
        val temCoordenadas = compromisso.latitudeDestino != 0.0 && compromisso.longitudeDestino != 0.0
        if (temCoordenadas && fimDaJanelaDeChegada(compromisso.dataHoraEvento) > System.currentTimeMillis()) {
            agendarExato(context, inicioMonitoramentoChegada(horarios), pendingAtivacaoChegada(context, compromisso.id))
        }
    }

    fun cancelar(context: Context, compromissoId: Long, numEtapas: Int) {
        for (i in 0 until (numEtapas + MARGEM_CANCELAMENTO_ETAPAS)) {
            NotificationScheduler.cancelAlarm(context, idAlarmeEtapa(compromissoId, i))
        }
        NotificationScheduler.cancelAlarm(context, idAlarmeSaida(compromissoId))
        cancelarVerificacaoAtraso(context, compromissoId)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingAtivacaoChegada(context, compromissoId))
    }

    fun cancelarVerificacaoAtraso(context: Context, compromissoId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingVerificacaoAtraso(context, compromissoId))
    }

    private fun agendarSaida(context: Context, compromisso: Compromisso, horario: Long, itens: List<String>) {
        val idSaida = idAlarmeSaida(compromisso.id)

        val confirmIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.Actions.CONFIRM_EXIT.toString()
            putExtra(EXTRA_COMPROMISSO_ID, compromisso.id)
            putExtra(EXTRA_ACTION_NOTIFICATION_ID, idSaida)
        }
        val confirmPending = PendingIntent.getBroadcast(
            context,
            requestCodeConfirmarSaida(compromisso.id),
            confirmIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val mensagem = if (itens.isNotEmpty()) "Saia agora! Não esqueça seus itens." else "Saia agora para chegar a tempo!"

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(EXTRA_TITLE, "HORA DE SAIR: ${compromisso.titulo}")
            putExtra(EXTRA_MESSAGE, mensagem)
            putExtra(EXTRA_NOTIFICATION_ID, idSaida)
            putExtra(EXTRA_HAS_ACTION, true)
            putExtra(EXTRA_ACTION_INTENT, confirmPending)
            putExtra(EXTRA_ACTION_TITLE, "Já Saí")
            putExtra(EXTRA_NOTIFICATION_TYPE, compromisso.notificationType.ordinal)
            putStringArrayListExtra(EXTRA_ITEMS, ArrayList(itens))
            putExtra(EXTRA_COMPROMISSO_ID, compromisso.id)
        }
        val pending = PendingIntent.getBroadcast(
            context,
            idSaida,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        agendarExato(context, horario, pending)
    }

    // Mesmo Intent (componente + ação) e request code para agendar e cancelar, senão o cancelamento não casa
    private fun pendingVerificacaoAtraso(context: Context, compromissoId: Long): PendingIntent {
        val intent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.Actions.CHECK_LATE.toString()
            putExtra(EXTRA_COMPROMISSO_ID, compromissoId)
        }
        return PendingIntent.getBroadcast(
            context,
            requestCodeVerificacaoAtraso(compromissoId),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun pendingAtivacaoChegada(context: Context, compromissoId: Long): PendingIntent {
        val intent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.Actions.ATIVAR_CHEGADA.toString()
            putExtra(EXTRA_COMPROMISSO_ID, compromissoId)
        }
        return PendingIntent.getBroadcast(
            context,
            idAlarmeAtivacaoChegada(compromissoId),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun agendarExato(context: Context, triggerAtMillis: Long, pendingIntent: PendingIntent) {
        if (!podeAgendarExato(context)) {
            // Quem chama deve checar podeAgendarExato antes e avisar o usuário (LEMB-10)
            Log.e(TAG, "ERRO: Permissão para agendar alarmes exatos não foi concedida.")
            return
        }
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
    }
}
