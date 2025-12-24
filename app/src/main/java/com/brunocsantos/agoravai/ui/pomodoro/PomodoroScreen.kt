package com.brunocsantos.agoravai.ui.pomodoro

import android.app.Activity
import android.content.res.Configuration
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.brunocsantos.agoravai.data.AutomationSetting
import com.brunocsantos.agoravai.utils.ResponsiveText // <-- Importante: Certifique-se de ter criado o arquivo em utils
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PomodoroScreen(
    viewModel: PomodoroViewModel,
    onNavigateToAutomationSettings: () -> Unit,
    onNavigateToHistory: () -> Unit
) {
    val isPremiumUser by viewModel.isPremiumUser.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var showPremiumDialog by remember { mutableStateOf(false) }

    val sessionCompletedEvent by viewModel.sessionCompletedEvent.collectAsStateWithLifecycle()
    var showCompletionDialog by remember { mutableStateOf(false) }

    LaunchedEffect(sessionCompletedEvent) {
        if (sessionCompletedEvent != null) {
            showCompletionDialog = true
            viewModel.onCompletionEventConsumed()
        }
    }

    if (showCompletionDialog) {
        CompletionDialog(onDismiss = { showCompletionDialog = false })
    }

    if (showPremiumDialog) {
        PremiumDialog(
            onDismiss = { showPremiumDialog = false },
            onSubscribeClick = {
                (context as? Activity)?.let { viewModel.startPurchaseFlow(it) }
                showPremiumDialog = false
            }
        )
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = {
                if (isPremiumUser) {
                    viewModel.onAddNewAutomation()
                    onNavigateToAutomationSettings()
                } else {
                    showPremiumDialog = true
                }
            }) {
                Icon(Icons.Default.Add, contentDescription = "Adicionar Pomodoro")
            }
        }
    ) { paddingValues ->
        // --- LÓGICA DE LAYOUT ADAPTATIVO ---
        val configuration = LocalConfiguration.current
        val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

        if (isLandscape) {
            PomodoroLandscapeLayout(
                modifier = Modifier.padding(paddingValues),
                viewModel = viewModel,
                onNavigateToAutomationSettings = onNavigateToAutomationSettings,
                onNavigateToHistory = onNavigateToHistory
            )
        } else {
            PomodoroPortraitLayout(
                modifier = Modifier.padding(paddingValues),
                viewModel = viewModel,
                onNavigateToAutomationSettings = onNavigateToAutomationSettings,
                onNavigateToHistory = onNavigateToHistory
            )
        }
    }
}

@Composable
fun PomodoroPortraitLayout(
    modifier: Modifier = Modifier,
    viewModel: PomodoroViewModel,
    onNavigateToAutomationSettings: () -> Unit,
    onNavigateToHistory: () -> Unit
) {
    val automationSettings by viewModel.automationSettings.collectAsStateWithLifecycle()
    val isPremiumUser by viewModel.isPremiumUser.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        item { ManualTimerSection(viewModel = viewModel) }
        item { HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp)) }

        if (isPremiumUser) {
            item {
                // --- CORREÇÃO DE LAYOUT ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // ResponsiveText com weight(1f) ocupa o espaço mas respeita o botão
                    ResponsiveText(
                        text = "Pomodoros Agendados",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f).padding(end = 8.dp)
                    )
                    IconButton(onClick = onNavigateToHistory) {
                        Icon(Icons.Default.History, contentDescription = "Abrir Histórico")
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
            if (automationSettings.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillParentMaxHeight(0.5f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Toque em + para agendar um Pomodoro.")
                    }
                }
            } else {
                items(automationSettings) { setting ->
                    AutomationItem(
                        setting = setting,
                        onEditClick = {
                            viewModel.onEditAutomation(setting)
                            onNavigateToAutomationSettings()
                        },
                        onToggle = { viewModel.toggleAutomation(context, setting) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        } else {
            item {
                var showPremiumDialog by remember { mutableStateOf(false) }
                PremiumPlaceholder(onSubscribeClick = { showPremiumDialog = true })
            }
        }
    }
}

@Composable
fun PomodoroLandscapeLayout(
    modifier: Modifier = Modifier,
    viewModel: PomodoroViewModel,
    onNavigateToAutomationSettings: () -> Unit,
    onNavigateToHistory: () -> Unit
) {
    val automationSettings by viewModel.automationSettings.collectAsStateWithLifecycle()
    val isPremiumUser by viewModel.isPremiumUser.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Row(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Painel Esquerdo: Timer
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            ManualTimerSection(viewModel = viewModel)
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Painel Direito: Agendamentos
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            if (isPremiumUser) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ResponsiveText(
                        text = "Agendados",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f).padding(end = 8.dp)
                    )
                    IconButton(onClick = onNavigateToHistory) {
                        Icon(Icons.Default.History, contentDescription = "Abrir Histórico")
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                if (automationSettings.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Nenhum agendamento.")
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        items(automationSettings) { setting ->
                            AutomationItem(
                                setting = setting,
                                onEditClick = {
                                    viewModel.onEditAutomation(setting)
                                    onNavigateToAutomationSettings()
                                },
                                onToggle = { viewModel.toggleAutomation(context, setting) }
                            )
                        }
                    }
                }
            } else {
                var showPremiumDialog by remember { mutableStateOf(false) }
                PremiumPlaceholder(onSubscribeClick = { showPremiumDialog = true })
            }
        }
    }
}


@Composable
fun ManualTimerSection(viewModel: PomodoroViewModel) {
    val tempoRestante by viewModel.tempoRestante.collectAsStateWithLifecycle()
    val estadoPomodoro by viewModel.estadoPomodoro.collectAsStateWithLifecycle()
    val estadoTimer by viewModel.estadoTimer.collectAsStateWithLifecycle()
    val focusDuration by viewModel.focusDuration.collectAsStateWithLifecycle()
    val shortBreakDuration by viewModel.shortBreakDuration.collectAsStateWithLifecycle()
    val longBreakDuration by viewModel.longBreakDuration.collectAsStateWithLifecycle()

    val dailyProgress by viewModel.dailyProgress.collectAsStateWithLifecycle()
    val dailyGoal by viewModel.dailyGoal.collectAsStateWithLifecycle()

    val context = LocalContext.current

    val duracaoTotal = when (estadoPomodoro) {
        PomodoroState.FOCO -> focusDuration * 60 * 1000L
        PomodoroState.PAUSA_CURTA -> shortBreakDuration * 60 * 1000L
        PomodoroState.PAUSA_LONGA -> longBreakDuration * 60 * 1000L
        PomodoroState.PARADO -> focusDuration * 60 * 1000L
    }

    val progresso = if (duracaoTotal > 0) tempoRestante.toFloat() / duracaoTotal.toFloat() else 1f

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.padding(top = 16.dp)
    ) {
        CircularTimer(
            progress = progresso,
            pomodoroState = estadoPomodoro
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Sessões concluídas: $dailyProgress / $dailyGoal",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.secondary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(
                onClick = { viewModel.iniciarOuRetomarTimer(context) },
                enabled = estadoTimer != TimerState.RODANDO
            ) {
                Text(if (estadoTimer == TimerState.PAUSADO) "Retomar" else "Iniciar")
            }
            Button(
                onClick = { viewModel.pausarTimer(context) },
                enabled = estadoTimer == TimerState.RODANDO
            ) {
                Text("Pausar")
            }
            Button(
                onClick = { viewModel.resetarTimer(context) },
                enabled = estadoTimer != TimerState.PARADO
            ) {
                Text("Resetar")
            }
        }
    }
}

@Composable
fun CircularTimer(
    progress: Float,
    pomodoroState: PomodoroState
) {
    val animatedProgress by animateFloatAsState(targetValue = progress, label = "Timer Progress Animation")

    val progressColor = when (pomodoroState) {
        PomodoroState.FOCO -> if (MaterialTheme.colorScheme.background.luminance() > 0.5) MaterialTheme.colorScheme.primary else Color.White
        PomodoroState.PAUSA_CURTA -> Color(0xFF66FFC2)
        PomodoroState.PAUSA_LONGA -> Color(0xFF00D4A1)
        PomodoroState.PARADO -> MaterialTheme.colorScheme.outline
    }

    val trackColor = MaterialTheme.colorScheme.surfaceVariant

    val textToShow = when(pomodoroState) {
        PomodoroState.FOCO -> "Hora de Focar! 💪"
        PomodoroState.PAUSA_CURTA -> "Pausa Curta ☕"
        PomodoroState.PAUSA_LONGA -> "Pausa Longa ✨"
        PomodoroState.PARADO -> "Pronto?"
    }

    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(220.dp)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
            )
            drawArc(
                color = progressColor,
                startAngle = -90f,
                sweepAngle = 360f * animatedProgress,
                useCenter = false,
                style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
            )
        }
        Text(
            text = textToShow,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = progressColor,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun CompletionDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Done, contentDescription = null, modifier = Modifier.size(48.dp)) },
        title = { Text("Sessão Concluída!") },
        text = { Text("Parabéns por manter o foco! Você completou sua sessão. Um bom descanso é a melhor recompensa.") },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("OK")
            }
        }
    )
}

@Composable
fun PremiumPlaceholder(onSubscribeClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.WorkspacePremium,
            contentDescription = "Recurso Premium",
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Recurso Premium",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Agende Pomodoros por horário e localização para automatizar sua rotina de foco. Assine agora para desbloquear esta e outras funcionalidades!",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onSubscribeClick) {
            Text("Assinar Agora")
        }
    }
}

@Composable
fun PremiumDialog(onDismiss: () -> Unit, onSubscribeClick: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.WorkspacePremium, contentDescription = null) },
        title = { Text("Desbloqueie o Agendamento") },
        text = { Text("Automatize seus ciclos de foco com o agendamento por horário e local. Torne-se um assinante Premium para ter acesso total.") },
        confirmButton = {
            Button(onClick = onSubscribeClick) {
                Text("Assinar Agora")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Depois")
            }
        }
    )
}

@Composable
fun AutomationItem(
    setting: AutomationSetting,
    onEditClick: () -> Unit,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEditClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (setting.isEnabled) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(setting.title, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "${setting.cycles} ciclos de foco",
                    style = MaterialTheme.typography.bodyMedium
                )
                if (setting.isTimeEnabled) {
                    Text(
                        text = "Às ${String.format(Locale.getDefault(), "%02d:%02d", setting.hour, setting.minute)}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                if (setting.isLocationEnabled) {
                    Text(
                        text = "Em: ${setting.placeAddress ?: "N/A"}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                if (setting.isRepeatEnabled) {
                    val days = if (setting.repeatDays.isEmpty()) { "Apenas uma vez"
                    } else if (setting.repeatDays.size == 7) { "Diariamente"
                    } else {
                        setting.repeatDays.sorted().joinToString(", ") {
                            it.getDisplayName(java.time.format.TextStyle.SHORT, Locale("pt", "BR"))
                        }
                    }
                    Text(text = "Repete: $days", style = MaterialTheme.typography.bodyMedium)
                }
            }
            Switch(
                checked = setting.isEnabled, onCheckedChange = { onToggle() },
                modifier = Modifier.padding(start = 16.dp)
            )
        }
    }
}