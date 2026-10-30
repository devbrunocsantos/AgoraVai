package com.brunocsantos.agoravai.notifications

import android.content.Context
import android.util.Log
import com.brunocsantos.agoravai.data.AppDatabase

/**
 * LEMB-11: depois de reiniciar o aparelho ou atualizar o app, recria os lembretes que
 * ainda fazem sentido, sem precisar que o app seja aberto.
 */
object Reagendador {

    private const val TAG = "AgoraVai-Boot"

    suspend fun reagendarTudo(context: Context, reinicio: Boolean) {
        val inicio = System.currentTimeMillis()
        Log.d(TAG, "inicio reinicio=$reinicio")

        val dao = AppDatabase.getDatabase(context).appDao()
        val agora = System.currentTimeMillis()
        val compromissos = dao.getCompromissosAtivosComEtapas()

        compromissos.forEach { c ->
            val compromisso = c.compromisso
            val horarios = calcularHorarios(
                compromisso.dataHoraEvento,
                compromisso.tempoDeslocamentoMinutos,
                compromisso.tempoDeSegurancaMinutos,
                c.etapas.map { it.duracaoMinutos }
            )
            val plano = planoDeReagendamento(
                horarios,
                compromisso.dataHoraEvento,
                agora,
                ativo = compromisso.isEnabled,
                confirmouSaida = compromisso.confirmouSaida,
                confirmouChegada = compromisso.confirmouChegada,
                temCoordenadas = compromisso.latitudeDestino != 0.0 && compromisso.longitudeDestino != 0.0,
                reinicio = reinicio
            )
            CompromissoAlarmScheduler.reagendar(
                context, compromisso, c.etapas, c.itensLembrete.map { it.nomeItem }, plano
            )
        }

        // RNF-002: tempo total do reagendamento
        Log.d(TAG, "fim n=${compromissos.size} em ${System.currentTimeMillis() - inicio}ms")
    }
}
