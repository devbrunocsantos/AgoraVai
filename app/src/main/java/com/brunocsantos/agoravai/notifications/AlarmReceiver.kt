package com.brunocsantos.agoravai.notifications

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import com.brunocsantos.agoravai.R
import com.brunocsantos.agoravai.ui.ReminderActivity

const val CHANNEL_ID = "agora_vai_channel"
const val EXTRA_TITLE = "extra_title"
const val EXTRA_MESSAGE = "extra_message"
const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
const val EXTRA_HAS_ACTION = "extra_has_action"
const val EXTRA_ACTION_INTENT = "extra_action_intent"
const val EXTRA_ACTION_TITLE = "extra_action_title"
const val EXTRA_NOTIFICATION_TYPE = "extra_notification_type"
const val EXTRA_ITEMS = "EXTRA_ITEMS" // Novo: Lista de strings
const val EXTRA_COMPROMISSO_ID = "EXTRA_COMPROMISSO_ID" // Novo: ID para confirmação
// ID da notificação a fechar, lido pelo NotificationActionReceiver (valor mantido por compatibilidade)
const val EXTRA_ACTION_NOTIFICATION_ID = "EXTRA_NOTIFICATION_ID"

private const val TAG = "AgoraVai-Debug"

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra(EXTRA_TITLE) ?: "Lembrete"
        val message = intent.getStringExtra(EXTRA_MESSAGE) ?: "Hora do compromisso"

        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, System.currentTimeMillis().toInt())
        val typeOrdinal = intent.getIntExtra(EXTRA_NOTIFICATION_TYPE, 0)

        // Novos dados recebidos do ViewModel
        val items = intent.getStringArrayListExtra(EXTRA_ITEMS)
        val compromissoId = intent.getLongExtra(EXTRA_COMPROMISSO_ID, -1L)

        Log.d(TAG, "Alarme disparado. Tipo: $typeOrdinal, ID: $notificationId, Itens: ${items?.size}")

        // Se for PADRAO (0), mostra notificação normal. Se for outro, abre Activity Tela Cheia.
        if (typeOrdinal == 0) {
            showStandardNotification(context, title, message, notificationId, intent, items)
        } else {
            showFullScreenAlarm(context, title, message, typeOrdinal, notificationId, items, compromissoId)
        }
    }

    private fun showFullScreenAlarm(
        context: Context,
        title: String,
        message: String,
        typeOrdinal: Int,
        notificationId: Int,
        items: ArrayList<String>?,
        compromissoId: Long
    ) {
        // Intent para abrir a Activity de Lembrete com todos os dados
        val fullScreenIntent = Intent(context, ReminderActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION
            putExtra("EXTRA_TITLE", title)
            putExtra("EXTRA_MESSAGE", message)
            putExtra("EXTRA_TYPE", typeOrdinal)
            putStringArrayListExtra("EXTRA_ITEMS", items) // Passa a lista de itens
            putExtra("EXTRA_COMPROMISSO_ID", compromissoId) // Passa o ID
        }

        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(fullScreenPendingIntent, true) // Acorda a tela
            .setAutoCancel(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(notificationId, builder.build())
    }

    private fun showStandardNotification(
        context: Context,
        title: String,
        message: String,
        notificationId: Int,
        intent: Intent,
        items: ArrayList<String>?
    ) {
        // Se houver itens, adicionamos ao corpo da notificação padrão
        val finalMessage = if (!items.isNullOrEmpty()) {
            // Ex: "Saia agora!\n\n📌 Não esqueça: Chaves, Carteira"
            "$message\n\n\uD83D\uDCCC Não esqueça: ${items.joinToString(", ")}"
        } else {
            message
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message) // Texto curto (preview)
            .setStyle(NotificationCompat.BigTextStyle().bigText(finalMessage)) // Texto longo (expandido)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        val hasAction = intent.getBooleanExtra(EXTRA_HAS_ACTION, false)
        if (hasAction) {
            val actionIntent = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(EXTRA_ACTION_INTENT, PendingIntent::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(EXTRA_ACTION_INTENT)
            }

            val actionTitle = intent.getStringExtra(EXTRA_ACTION_TITLE) ?: "Confirmar"

            if (actionIntent != null) {
                builder.addAction(R.drawable.ic_launcher_foreground, actionTitle, actionIntent)
            }
        }

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(notificationId, builder.build())
    }
}