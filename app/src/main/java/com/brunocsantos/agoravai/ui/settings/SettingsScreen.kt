package com.brunocsantos.agoravai.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.brunocsantos.agoravai.ui.pomodoro.PomodoroViewModel
import com.brunocsantos.agoravai.utils.SettingsManager
import com.brunocsantos.agoravai.utils.ResponsiveText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: PomodoroViewModel,
    onBackClick: () -> Unit,
    onNavigateToPermissions: () -> Unit
) {
    val context = LocalContext.current

    val focusDuration by viewModel.focusDuration.collectAsStateWithLifecycle()
    val shortBreakDuration by viewModel.shortBreakDuration.collectAsStateWithLifecycle()
    val longBreakDuration by viewModel.longBreakDuration.collectAsStateWithLifecycle()
    val cyclesBeforeLongBreak by viewModel.cyclesBeforeLongBreak.collectAsStateWithLifecycle()
    val dailyGoal by viewModel.dailyGoal.collectAsStateWithLifecycle()
    val safetyTime by viewModel.safetyTime.collectAsStateWithLifecycle() // <-- NOVO

    var tempFocus by remember(focusDuration) { mutableStateOf(focusDuration.toString()) }
    var tempShortBreak by remember(shortBreakDuration) { mutableStateOf(shortBreakDuration.toString()) }
    var tempLongBreak by remember(longBreakDuration) { mutableStateOf(longBreakDuration.toString()) }
    var tempCycles by remember(cyclesBeforeLongBreak) { mutableStateOf(cyclesBeforeLongBreak.toString()) }
    var tempDailyGoal by remember(dailyGoal) { mutableStateOf(dailyGoal.toString()) }
    var tempSafetyTime by remember(safetyTime) { mutableStateOf(safetyTime.toString()) } // <-- NOVO

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    ResponsiveText(
                        text = "Configurações",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    Button(onClick = {
                        viewModel.savePomodoroSettings(
                            context = context,
                            focus = tempFocus.toIntOrNull() ?: SettingsManager.DEFAULT_FOCUS_MINS,
                            shortBreak = tempShortBreak.toIntOrNull() ?: SettingsManager.DEFAULT_SHORT_BREAK_MINS,
                            longBreak = tempLongBreak.toIntOrNull() ?: SettingsManager.DEFAULT_LONG_BREAK_MINS,
                            cycles = tempCycles.toIntOrNull() ?: SettingsManager.DEFAULT_CYCLES,
                            dailyGoal = tempDailyGoal.toIntOrNull() ?: SettingsManager.DEFAULT_DAILY_GOAL,
                            safetyTime = tempSafetyTime.toIntOrNull() ?: SettingsManager.DEFAULT_SAFETY_TIME_MINS
                        )
                        onBackClick()
                    }) {
                        Text("Salvar")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Pomodoro", style = MaterialTheme.typography.titleLarge)

            OutlinedTextField(
                value = tempFocus,
                onValueChange = { tempFocus = it.filter { char -> char.isDigit() } },
                label = { Text("Tempo de Foco (minutos)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = tempShortBreak,
                onValueChange = { tempShortBreak = it.filter { char -> char.isDigit() } },
                label = { Text("Pausa Curta (minutos)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = tempLongBreak,
                onValueChange = { tempLongBreak = it.filter { char -> char.isDigit() } },
                label = { Text("Pausa Longa (minutos)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = tempCycles,
                onValueChange = { tempCycles = it.filter { char -> char.isDigit() } },
                label = { Text("Ciclos antes da Pausa Longa") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = tempDailyGoal,
                onValueChange = { tempDailyGoal = it.filter { char -> char.isDigit() } },
                label = { Text("Meta diária de sessões de foco") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            Text("Compromissos", style = MaterialTheme.typography.titleLarge)

            OutlinedTextField(
                value = tempSafetyTime,
                onValueChange = { tempSafetyTime = it.filter { char -> char.isDigit() } },
                label = { Text("Tempo de Segurança Padrão (minutos)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            Text("Assinatura", style = MaterialTheme.typography.titleLarge)

            Text(
                text = "Se você trocou de celular ou reinstalou o app, use esta opção para reativar sua assinatura Premium.",
                style = MaterialTheme.typography.bodySmall
            )

            Button(
                onClick = {
                    viewModel.billingClientManager.queryPurchases()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Restaurar Compras")
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // --- NOVO BOTÃO DE DIAGNÓSTICO ---
            Text("Sistema", style = MaterialTheme.typography.titleLarge)
            OutlinedButton(
                onClick = onNavigateToPermissions,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Settings, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Verificar Permissões & Problemas")
            }
        }
    }
}