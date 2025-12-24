package com.brunocsantos.agoravai.ui.pomodoro

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale
import com.brunocsantos.agoravai.utils.ResponsiveText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutomationSettingsScreen(
    automationState: EditAutomationUiState,
    isSaveEnabled: Boolean,
    onTitleChange: (String) -> Unit,
    onTimeToggle: (Boolean) -> Unit,
    onLocationToggle: (Boolean) -> Unit,
    onRepeatToggle: (Boolean) -> Unit,
    onLocationClick: () -> Unit,
    onTimeClick: () -> Unit,
    onDayClick: (DayOfWeek) -> Unit,
    onBackClick: () -> Unit,
    onSaveClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onCyclesChange: (String) -> Unit,
    onFetchCurrentLocationClick: () -> Unit // NOVO CALLBACK
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    ResponsiveText(
                        text = if (automationState.id == 0) "Novo Pomodoro" else "Editar Pomodoro",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    Button(
                        onClick = onSaveClick,
                        enabled = isSaveEnabled
                    ) {
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
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = automationState.title,
                onValueChange = onTitleChange,
                label = { Text("Título do Pomodoro") },
                modifier = Modifier.fillMaxWidth(),
                isError = automationState.title.isBlank()
            )

            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = automationState.cycles,
                onValueChange = onCyclesChange,
                label = { Text("Meta de sessões de foco") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                isError = automationState.cycles.toIntOrNull() == null || automationState.cycles.toInt() <= 0
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // --- Gatilho de Tempo ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Iniciar às...", style = MaterialTheme.typography.titleMedium)
                Switch(checked = automationState.isTimeEnabled, onCheckedChange = onTimeToggle)
            }
            AnimatedVisibility(visible = automationState.isTimeEnabled) {
                Column(modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onTimeClick),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Horário", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            text = String.format(Locale.getDefault(), "%02d:%02d", automationState.hour, automationState.minute),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // --- Gatilho de Localização ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Iniciar em...", style = MaterialTheme.typography.titleMedium)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AnimatedVisibility(visible = automationState.isLocationEnabled) {
                        IconButton(onClick = onFetchCurrentLocationClick) {
                            Icon(Icons.Default.GpsFixed, contentDescription = "Usar localização atual")
                        }
                    }
                    Switch(checked = automationState.isLocationEnabled, onCheckedChange = onLocationToggle)
                }
            }
            AnimatedVisibility(visible = automationState.isLocationEnabled) {
                Column {
                    val hasError = automationState.placeId == null
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp, bottom = 4.dp)
                            .border(
                                width = if (hasError) 1.dp else 0.dp,
                                color = if (hasError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.background,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable(onClick = onLocationClick)
                            .padding(horizontal = 12.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Local", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            text = automationState.placeAddress ?: "Nenhum local selecionado",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (automationState.placeAddress == "Toque para selecionar") MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    if (hasError) {
                        Text(
                            text = "É necessário selecionar um local.",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // --- Gatilho de Repetição ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Repetir em...", style = MaterialTheme.typography.titleMedium)
                Switch(checked = automationState.isRepeatEnabled, onCheckedChange = onRepeatToggle)
            }

            AnimatedVisibility(visible = automationState.isRepeatEnabled) {
                val hasError = automationState.repeatDays.isEmpty()
                Column(modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)) {
                    WeekDaySelector(
                        selectedDays = automationState.repeatDays,
                        onDayClick = onDayClick,
                        isEnabled = true,
                        modifier = if (hasError) Modifier.border(1.dp, MaterialTheme.colorScheme.error, RoundedCornerShape(50)) else Modifier
                    )
                    if (hasError) {
                        Text(
                            text = "Selecione pelo menos um dia da semana.",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                        )
                    }
                }
            }

            AnimatedVisibility(visible = !automationState.isRepeatEnabled) {
                Text(
                    text = "Quando desativado, este Pomodoro será executado apenas uma vez e depois será arquivado.",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 4.dp, top = 8.dp)
                )
            }

            if (automationState.id != 0) {
                Spacer(modifier = Modifier.height(32.dp))
                Button(
                    onClick = onDeleteClick,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Excluir Pomodoro")
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun WeekDaySelector(
    selectedDays: Set<DayOfWeek>,
    onDayClick: (DayOfWeek) -> Unit,
    isEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(2.dp),
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        DayOfWeek.values().forEach { day ->
            val isSelected = selectedDays.contains(day)
            val backgroundColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
            val contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
            val borderModifier = if (isSelected) Modifier else Modifier.border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(backgroundColor)
                    .then(borderModifier)
                    .clickable(enabled = isEnabled) { onDayClick(day) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = day.getDisplayName(TextStyle.SHORT, Locale("pt", "BR")).first().toString(),
                    color = contentColor,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}