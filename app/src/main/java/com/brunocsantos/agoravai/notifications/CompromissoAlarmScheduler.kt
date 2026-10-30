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

// LEMB-10 / LEMB-11.e: mostrado quando o app não pode agendar alarmes exatos
const val MENSAGEM_PERMISSAO_ALARME = "Permita “Alarmes e lembretes” para o Agora Vai avisar na hora certa."

/**
 * Agenda e cancela todos os lembretes de um compromisso (etapas, saída e atraso).
 * Os request codes seguem a convenção de arquitetura.md §5.2 e não podem mudar,
 * senão os cancelamentos deixam de acertar alarmes já agendados.
 *
 * `agendar` (salvar e religar) agenda todos os horários, sem filtrar o passado.
 * `reagendar` (reiniciar e atualizar) segue o plano de `planoDeReagendamento` (LEMB-11).
 */
object CompromissoAlarmScheduler {

    private const val TAG = "AgoraVai-Debug"

    // Margem de alarmes de etapa cancelados além do número atual de etapas (edições podem ter removido etapas)
    private const val MARGEM_CANCELAMENTO_ETAPAS = 5

    fun idAlarmeEtapa(compromissoId: Long, indiceInvertido: Int) = (compromissoId.toInt() * 100) + indiceInvertido
    fun idAlarmeSaida(compromissoId: Long) = (compromissoId.toInt() * 100) + 99
    fun idAlarmeAtivacaoChegada(compromissoId: Long) = (compromissoId.toInt() * 100) + 97
    private fun requestCodeConfirmarSaida(compromissoId: Long) = compromissoId.toInt()

    // LEMB-11.e: notificação global (não pertence a um compromisso), fora da faixa id*100+x
    const val ID_NOTIFICACAO_LEMBRETES_PAUSADOS = Int.MAX_VALUE - 1

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

        etapas.indices.forEach { index ->
            agendarEtapa(context, compromisso.id, etapas, index, horarios.inicioEtapas[index])
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

    /**
     * LEMB-11: cancela o que existir e agenda só o que o plano manda. Com plano vazio,
     * apenas limpa sobras (mesmos request codes, RNF-001).
     */
    fun reagendar(
        context: Context,
        compromisso: Compromisso,
        etapas: List<Etapa>,
        itens: List<String>,
        plano: Reagendamento
    ) {
        cancelar(context, compromisso.id, etapas.size)
        plano.etapas.forEach { (index, horario) ->
            agendarEtapa(context, compromisso.id, etapas, index, horario)
        }
        plano.saida?.let { agendarSaida(context, compromisso, it, itens) }
        plano.atraso?.let { agendarExato(context, it, pendingVerificacaoAtraso(context, compromisso.id)) }
        plano.ativacaoChegada?.let { agendarExato(context, it, pendingAtivacaoChegada(context, compromisso.id)) }
    }

    // O índice do alarme de etapa é contado de trás para frente, como sempre foi
    private fun agendarEtapa(context: Context, compromissoId: Long, etapas: List<Etapa>, index: Int, horario: Long) {
        val etapa = etapas[index]
        NotificationScheduler.scheduleAlarm(
            context,
            horario,
            idAlarmeEtapa(compromissoId, etapas.size - 1 - index),
            "Próximo Preparativo: ${etapa.descricao}",
            "Faltam ${etapa.duracaoMinutos} min."
        )
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

    // LEMB-11.e: sem alarme exato, a saída perdida é entregue na hora, sem passar pelo AlarmManager
    fun dispararSaidaAgora(context: Context, compromisso: Compromisso, itens: List<String>) {
        context.sendBroadcast(intentSaida(context, compromisso, itens))
    }

    private fun agendarSaida(context: Context, compromisso: Compromisso, horario: Long, itens: List<String>) {
        val pending = PendingIntent.getBroadcast(
            context,
            idAlarmeSaida(compromisso.id),
            intentSaida(context, compromisso, itens),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        agendarExato(context, horario, pending)
    }

    private fun intentSaida(context: Context, compromisso: Compromisso, itens: List<String>): Intent {
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

        return Intent(context, AlarmReceiver::class.java).apply {
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
