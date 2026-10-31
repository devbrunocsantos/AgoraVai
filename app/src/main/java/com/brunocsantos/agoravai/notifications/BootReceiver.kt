package com.brunocsantos.agoravai.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * LEMB-11: o Android apaga alarmes e geofences ao reiniciar, e a atualização do app deixa
 * os lembretes agendados pela versão anterior. Este receiver os recria sem abrir o app.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // Só no reinício o aparelho esteve desligado e pode ter perdido a saída (LEMB-11.f)
        val reinicio = when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED, ACTION_QUICKBOOT_POWERON -> true
            Intent.ACTION_MY_PACKAGE_REPLACED -> false
            else -> return
        }

        // goAsync mantém o receiver vivo até o acesso ao banco terminar
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Reagendador.reagendarTudo(context, reinicio)
            } catch (e: Exception) {
                // Uma falha aqui não pode derrubar o processo durante o boot
                Log.e("AgoraVai-Boot", "Falha ao reagendar após ${intent.action}", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        // Alguns fabricantes antigos avisam o fim do boot com esta ação em vez de BOOT_COMPLETED
        const val ACTION_QUICKBOOT_POWERON = "android.intent.action.QUICKBOOT_POWERON"
    }
}
