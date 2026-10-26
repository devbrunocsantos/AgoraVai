package com.brunocsantos.agoravai.ui

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.brunocsantos.agoravai.data.NotificationType
import com.brunocsantos.agoravai.notifications.EXTRA_ACTION_NOTIFICATION_ID
import com.brunocsantos.agoravai.services.NotificationActionReceiver
import com.brunocsantos.agoravai.ui.theme.AgoraVaiTheme
import kotlinx.coroutines.delay

class ReminderActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        turnScreenOnAndKeyguard()

        // Recebendo os dados do Alarme
        val title = intent.getStringExtra("EXTRA_TITLE") ?: "Lembrete"
        val message = intent.getStringExtra("EXTRA_MESSAGE") ?: "Hora do compromisso!"

        // Lista de itens (Ex: Chaves, Carteira...)
        val items = intent.getStringArrayListExtra("EXTRA_ITEMS") ?: arrayListOf()

        // Tipo de insistência
        val typeOrdinal = intent.getIntExtra("EXTRA_TYPE", 0)
        val type = NotificationType.values().getOrElse(typeOrdinal) { NotificationType.PADRAO }

        // ID para confirmar a saída
        val compromissoId = intent.getLongExtra("EXTRA_COMPROMISSO_ID", -1L)
        val notificationId = intent.getIntExtra(EXTRA_ACTION_NOTIFICATION_ID, 0)

        setContent {
            AgoraVaiTheme {
                ReminderScreen(
                    title = title,
                    message = message,
                    items = items,
                    type = type,
                    onDismiss = {
                        confirmarSaida(compromissoId, notificationId)
                        finish()
                    }
                )
            }
        }
    }

    private fun confirmarSaida(compromissoId: Long, notificationId: Int) {
        if (compromissoId != -1L) {
            val intent = Intent(this, NotificationActionReceiver::class.java).apply {
                action = NotificationActionReceiver.Actions.CONFIRM_EXIT.toString()
                putExtra("EXTRA_COMPROMISSO_ID", compromissoId)
                putExtra(EXTRA_ACTION_NOTIFICATION_ID, notificationId) // Fecha a notificação do alarme
            }
            sendBroadcast(intent)
        }
    }

    private fun turnScreenOnAndKeyguard() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            keyguardManager.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }
    }
}

@Composable
fun ReminderScreen(
    title: String,
    message: String,
    items: List<String>,
    type: NotificationType,
    onDismiss: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Ícone de Alerta
            Icon(
                imageVector = Icons.Default.NotificationsActive,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = message, // Mensagem principal (ex: "Saia agora!")
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // --- CHECKLIST DE ITENS (SE HOUVER) ---
            if (items.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false), // Ocupa espaço flexível, mas não empurra tudo
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Não esqueça de levar:",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(items) { item ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        null,
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.secondary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(item, style = MaterialTheme.typography.bodyLarge)
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            } else {
                Spacer(modifier = Modifier.weight(1f)) // Empurra botões para baixo se não tiver lista
            }

            // --- BOTÕES DE AÇÃO (BASEADOS NO TIPO) ---
            when (type) {
                NotificationType.ALARME -> SimpleDismissButton(onDismiss)
                NotificationType.INSISTENTE -> HoldToDismissButton(onDismiss)
                NotificationType.DESAFIO -> ChallengeDismissButton(onDismiss)
                else -> SimpleDismissButton(onDismiss)
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun SimpleDismissButton(onDismiss: () -> Unit) {
    Button(
        onClick = onDismiss,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text("JÁ ESTOU SAINDO", fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun HoldToDismissButton(onDismiss: () -> Unit) {
    var progress by remember { mutableStateOf(0f) }
    val animatedProgress by animateFloatAsState(targetValue = progress, label = "progress")

    LaunchedEffect(progress) {
        if (progress >= 1f) onDismiss()
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier.size(100.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeWidth = 8.dp
            )

            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                val startTime = System.currentTimeMillis()
                                try {
                                    while (true) {
                                        val elapsedTime = System.currentTimeMillis() - startTime
                                        progress = (elapsedTime / 3000f).coerceAtMost(1f)
                                        if (progress >= 1f) return@detectTapGestures
                                        delay(16)
                                    }
                                } finally {
                                    if (progress < 1f) progress = 0f
                                }
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Text("SEGURE", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }
        Spacer(Modifier.height(16.dp))
        Text("Segure para confirmar saída", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun ChallengeDismissButton(onDismiss: () -> Unit) {
    var text by remember { mutableStateOf("") }
    val challengeWord = "JÁ FIZ"
    val isMatch = text.trim().equals(challengeWord, ignoreCase = true)

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Digite '$challengeWord' para liberar:")
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = text,
            onValueChange = { text = it.uppercase() },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline
            )
        )
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = onDismiss,
            enabled = isMatch,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isMatch) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Text("CONFIRMAR SAÍDA")
        }
    }
}