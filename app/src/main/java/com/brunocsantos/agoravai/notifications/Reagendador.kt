package com.brunocsantos.agoravai.notifications

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.brunocsantos.agoravai.data.AppDatabase
import com.brunocsantos.agoravai.data.CompromissoComEtapas
import com.brunocsantos.agoravai.services.geofence.RegistroChegada
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

/**
 * LEMB-11: depois de reiniciar o aparelho ou atualizar o app, recria os lembretes que
 * ainda fazem sentido, sem precisar que o app seja aberto.
 */
object Reagendador {

    private const val TAG = "AgoraVai-Boot"

    // LEMB-11.i: tempo máximo para ler a localização dentro do receiver (~10 s no total)
    private const val TIMEOUT_LOCALIZACAO_MS = 4_000L

    // LEMB-11.i: sem localização, a saída perdida espera este tempo para dar chance de a
    // chegada ser detectada pela geofence antes (que então cancela a saída)
    private const val ATRASO_SAIDA_SEM_LOCALIZACAO_MS = 45_000L

    suspend fun reagendarTudo(context: Context, reinicio: Boolean) {
        val inicio = System.currentTimeMillis()
        Log.d(TAG, "inicio reinicio=$reinicio")

        val dao = AppDatabase.getDatabase(context).appDao()
        val agora = System.currentTimeMillis()
        val compromissos = dao.getCompromissosAtivosComEtapas()

        // A localização é lida no máximo uma vez, e só se algum compromisso precisar
        var localizacaoLida = false
        var localizacao: Location? = null

        compromissos.forEach { c ->
            val plano = planoDe(c, agora, reinicio)
            val itens = c.itensLembrete.map { it.nomeItem }

            // LEMB-11.i: saída perdida com destino no período → a chegada tem precedência
            if (plano.saidaPerdida && plano.ativacaoChegada != null) {
                if (!localizacaoLida) {
                    localizacao = lerLocalizacao(context)
                    localizacaoLida = true
                }
                when (posicao(c, localizacao)) {
                    PosicaoChegada.DENTRO -> {
                        RegistroChegada.registrar(context, c)
                        return@forEach
                    }
                    PosicaoChegada.FORA ->
                        CompromissoAlarmScheduler.reagendar(context, c.compromisso, c.etapas, itens, plano)
                    PosicaoChegada.DESCONHECIDA ->
                        CompromissoAlarmScheduler.reagendar(
                            context, c.compromisso, c.etapas, itens,
                            plano.copy(saida = agora + ATRASO_SAIDA_SEM_LOCALIZACAO_MS)
                        )
                }
            } else {
                CompromissoAlarmScheduler.reagendar(context, c.compromisso, c.etapas, itens, plano)
            }
        }

        // RNF-002: tempo total do reagendamento
        Log.d(TAG, "fim n=${compromissos.size} em ${System.currentTimeMillis() - inicio}ms")
    }

    private fun planoDe(c: CompromissoComEtapas, agora: Long, reinicio: Boolean): Reagendamento {
        val compromisso = c.compromisso
        val horarios = calcularHorarios(
            compromisso.dataHoraEvento,
            compromisso.tempoDeslocamentoMinutos,
            compromisso.tempoDeSegurancaMinutos,
            c.etapas.map { it.duracaoMinutos }
        )
        return planoDeReagendamento(
            horarios,
            compromisso.dataHoraEvento,
            agora,
            ativo = compromisso.isEnabled,
            confirmouSaida = compromisso.confirmouSaida,
            confirmouChegada = compromisso.confirmouChegada,
            temCoordenadas = compromisso.latitudeDestino != 0.0 && compromisso.longitudeDestino != 0.0,
            reinicio = reinicio
        )
    }

    private fun posicao(c: CompromissoComEtapas, local: Location?): PosicaoChegada {
        if (local == null) return PosicaoChegada.DESCONHECIDA
        val destino = Location("destino").apply {
            latitude = c.compromisso.latitudeDestino
            longitude = c.compromisso.longitudeDestino
        }
        val precisao = if (local.hasAccuracy()) local.accuracy else null
        return classificarPosicao(local.distanceTo(destino), precisao)
    }

    private suspend fun lerLocalizacao(context: Context): Location? {
        val fina = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        // Abaixo do Android 10 não existe a permissão de localização em segundo plano
        val segundoPlano = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        if (!fina || !segundoPlano) return null

        val cts = CancellationTokenSource()
        return try {
            val local = withTimeoutOrNull(TIMEOUT_LOCALIZACAO_MS) {
                LocationServices.getFusedLocationProviderClient(context)
                    .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
                    .await()
            }
            if (local == null) cts.cancel()
            local
        } catch (e: SecurityException) {
            Log.e(TAG, "Sem permissão para ler a localização", e)
            null
        }
    }
}
