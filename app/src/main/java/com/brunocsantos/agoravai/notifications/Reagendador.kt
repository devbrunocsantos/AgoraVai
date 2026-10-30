package com.brunocsantos.agoravai.notifications

import android.Manifest
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.brunocsantos.agoravai.R
import com.brunocsantos.agoravai.data.AppDatabase
import com.brunocsantos.agoravai.data.CompromissoComEtapas
import com.brunocsantos.agoravai.services.geofence.CompromissoGeofenceManager
import com.brunocsantos.agoravai.services.geofence.GeofenceManager
import com.brunocsantos.agoravai.services.geofence.RegistroChegada
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.CancellationException
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
        val agora = System.currentTimeMillis()
        Log.d(TAG, "inicio reinicio=$reinicio")

        val dao = AppDatabase.getDatabase(context).appDao()
        val compromissos = dao.getCompromissosAtivosComEtapas()

        // LEMB-11.e: sem alarme exato, só o que não depende de alarme é feito
        val podeAgendarExato = CompromissoAlarmScheduler.podeAgendarExato(context)
        var lembretesPausados = false

        // A localização é lida no máximo uma vez, e só se algum compromisso precisar
        var localizacaoLida = false
        var localizacao: Location? = null

        compromissos.forEach { c ->
            // Uma falha num compromisso não pode impedir o reagendamento dos outros (P2)
            try {
                var plano = planoDe(c, agora, reinicio)
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
                        PosicaoChegada.FORA -> Unit
                        PosicaoChegada.DESCONHECIDA ->
                            // Sem alarme exato não dá para esperar: a saída é entregue na hora
                            if (podeAgendarExato) plano = plano.copy(saida = agora + ATRASO_SAIDA_SEM_LOCALIZACAO_MS)
                    }
                }

                if (podeAgendarExato) {
                    CompromissoAlarmScheduler.reagendar(context, c.compromisso, c.etapas, itens, plano)
                } else {
                    CompromissoAlarmScheduler.cancelar(context, c.compromisso.id, c.etapas.size)
                    // A detecção de chegada não depende de alarme: é ativada direto (LEMB-11, RF-003)
                    if (plano.ativacaoChegada != null) {
                        CompromissoGeofenceManager.addGeofenceChegada(c.compromisso, c.etapas)
                    }
                    if (plano.saidaPerdida) {
                        CompromissoAlarmScheduler.dispararSaidaAgora(context, c.compromisso, itens)
                    }
                    // Lembretes futuros que ficaram sem agendar geram o aviso
                    if (plano.etapas.isNotEmpty() || plano.atraso != null || (plano.saida != null && !plano.saidaPerdida)) {
                        lembretesPausados = true
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Falha ao reagendar o compromisso ${c.compromisso.id}", e)
            }
        }

        if (lembretesPausados) mostrarAvisoLembretesPausados(context)

        // AUTO-05: as geofences das automações ativas também somem ao reiniciar
        try {
            dao.getActiveAutomationSettings().forEach { GeofenceManager.addOrUpdateGeofence(it) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Falha ao reativar as geofences das automações", e)
        }

        // RNF-002: tempo total do reagendamento
        Log.d(TAG, "fim n=${compromissos.size} em ${System.currentTimeMillis() - agora}ms")
    }

    // LEMB-11.e: só ocorre no Android 12+, pois abaixo disso podeAgendarExato é sempre true
    private fun mostrarAvisoLembretesPausados(context: Context) {
        val intent = Intent(
            Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
            Uri.parse("package:${context.packageName}")
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val pending = PendingIntent.getActivity(
            context,
            CompromissoAlarmScheduler.ID_NOTIFICACAO_LEMBRETES_PAUSADOS,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Seus lembretes estão pausados")
            .setContentText(MENSAGEM_PERMISSAO_ALARME)
            .setStyle(NotificationCompat.BigTextStyle().bigText(MENSAGEM_PERMISSAO_ALARME))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(CompromissoAlarmScheduler.ID_NOTIFICACAO_LEMBRETES_PAUSADOS, notification)
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
            withTimeoutOrNull(TIMEOUT_LOCALIZACAO_MS) {
                LocationServices.getFusedLocationProviderClient(context)
                    .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
                    .await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Sem permissão, Play Services indisponível logo após o boot etc.: decide como DESCONHECIDA
            Log.e(TAG, "Não foi possível ler a localização", e)
            null
        } finally {
            cts.cancel()
        }
    }
}
