package com.brunocsantos.agoravai.ui.compromissos

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.RemoveCircle
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.brunocsantos.agoravai.data.NotificationType
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import com.brunocsantos.agoravai.utils.ResponsiveText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCompromissoScreen(
    uiState: AddCompromissoUiState,
    viewModel: CompromissoViewModel,
    onSaveClick: () -> Unit,
    onBackClick: () -> Unit
) {
    val dateFormatter = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    val timeFormatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val isEditing = uiState.id != null

    val snackbarHostState = remember { SnackbarHostState() }
    val scrollState = rememberScrollState() // Estado de rolagem para a Column

    LaunchedEffect(uiState.saveError) {
        uiState.saveError?.let { error ->
            snackbarHostState.showSnackbar(message = error, duration = SnackbarDuration.Short)
            viewModel.dismissError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    ResponsiveText(
                        text = if (isEditing) "Editar Compromisso" else "Novo Compromisso",
                        style = MaterialTheme.typography.titleLarge
                    ) },
                navigationIcon = { IconButton(onClick = { onBackClick() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar") } },
                actions = { Button(onClick = { onSaveClick() }) { Text("Salvar") } }
            )
        }
    ) { paddingValues ->
        // ALTERAÇÃO 1: Mudança de LazyColumn para Column com verticalScroll para evitar conflitos de lista e travamentos
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(scrollState), // Habilita rolagem suave
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp)) // Espaço inicial

            // TÍTULO
            OutlinedTextField(
                value = uiState.titulo,
                onValueChange = { viewModel.onTituloChanged(it) },
                label = { Text("Título do Compromisso") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, keyboardType = KeyboardType.Text),
                trailingIcon = { InfoIconWithDialog(title = "Título", description = "Dê um nome curto para identificar seu compromisso (ex: Consulta Médica, Reunião).") }
            )

            // ENDEREÇOS
            AddressAutocompleteField(
                label = "Endereço de Partida",
                initialValue = uiState.origem,
                infoDescription = "Local de saída. O app usará isso para calcular o tempo de trânsito.",
                onAddressSelected = { viewModel.onOrigemChanged(it) },
                onCurrentLocationClick = { viewModel.fetchCurrentLocationFor(AddressField.ORIGIN) }
            )

            AddressAutocompleteField(
                label = "Endereço de Destino",
                initialValue = uiState.destino,
                infoDescription = "Local do evento. Usado para monitorar sua chegada.",
                onAddressSelected = { viewModel.onDestinoChanged(it) },
                onCurrentLocationClick = { viewModel.fetchCurrentLocationFor(AddressField.DESTINATION) }
            )

            // --- ALTERAÇÃO 2: CORREÇÃO DO CLIQUE DATA/HORA ---
            // Usamos um Box para capturar o clique e desabilitamos o TextField visualmente (enabled=false)
            // mas restauramos as cores originais para parecer habilitado.
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Campo DATA
                Box(modifier = Modifier.weight(1f).clickable { viewModel.openDatePicker() }) {
                    OutlinedTextField(
                        value = dateFormatter.format(uiState.calendar.time),
                        onValueChange = {},
                        label = { Text("Data") },
                        enabled = false, // Desabilita interação interna do campo
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = MaterialTheme.colorScheme.onSurface,
                            disabledBorderColor = MaterialTheme.colorScheme.outline,
                            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        trailingIcon = {
                            // Ícone precisa ser clicável independentemente do campo desabilitado
                            IconButton(onClick = { /* Ação do ícone */ }) {
                                InfoIconWithDialog(title = "Data do Evento", description = "Se a repetição for selecionada, esta data serve como início para a repetição deste evento.")
                            }
                        }
                    )
                }

                // Campo HORA
                Box(modifier = Modifier.weight(1f).clickable { viewModel.openTimePicker() }) {
                    OutlinedTextField(
                        value = timeFormatter.format(uiState.calendar.time),
                        onValueChange = {},
                        label = { Text("Hora") },
                        enabled = false,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = MaterialTheme.colorScheme.onSurface,
                            disabledBorderColor = MaterialTheme.colorScheme.outline,
                            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        trailingIcon = {
                            IconButton(onClick = { /* Ação do ícone */ }) {
                                InfoIconWithDialog(title = "Hora do Evento", description = "Horário de início do compromisso.")
                            }
                        }
                    )
                }
            }

            // TEMPO SEGURANÇA
            OutlinedTextField(
                value = uiState.tempoDeSeguranca,
                onValueChange = { viewModel.onSafetyTimeChanged(it) },
                label = { Text("Tempo de Segurança (min)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                trailingIcon = { InfoIconWithDialog(title = "Tempo de Segurança", description = "Margem extra de tempo para evitar atrasos imprevistos. Soma-se ao trânsito.") }
            )

            // SWITCH EXCLUSÃO AUTOMÁTICA
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Excluir após concluir", style = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.width(8.dp))
                    InfoIconWithDialog(title = "Limpeza Automática", description = "Se ativado, o compromisso será apagado automaticamente após a data do evento passar.")
                }
                Switch(checked = uiState.deleteAfterComplete, onCheckedChange = { viewModel.onDeleteAfterCompleteChanged(it) })
            }

            // REPETIÇÃO
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Repetir nos dias:", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.width(8.dp))
                    InfoIconWithDialog(title = "Repetição", description = "Selecione os dias da semana para compromissos recorrentes.")
                }
                Spacer(Modifier.height(8.dp))
                DiasSemanaSelector(diasSelecionados = uiState.diasRepeticao, onDiaClick = { viewModel.onDiaSemanaChanged(it) })
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Estilo do Lembrete (TDAH)", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.width(8.dp))
                InfoIconWithDialog(title = "Insistência", description = "Escolha como o app deve chamar sua atenção na hora de sair.")
            }
            Spacer(modifier = Modifier.height(8.dp))

            NotificationTypeSelector(
                selected = uiState.notificationType,
                onSelect = {
                    // O log abaixo ajuda a debugar se o clique está funcionando
                    android.util.Log.d("AgoraVai-Debug", "Tipo selecionado na UI: $it")
                    viewModel.onNotificationTypeChanged(it)
                }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // PREPARATIVOS
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Meus Preparativos", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.width(8.dp))
                InfoIconWithDialog(title = "Preparativos", description = "Liste o que fazer antes de sair (ex: Banho - 15min). O app avisará o início de cada etapa.")
            }

            // Loop simples (Substituindo itemsIndexed do LazyColumn)
            uiState.etapas.forEachIndexed { index, etapaState ->
                EtapaInputItem(
                    etapaState = etapaState,
                    onDescriptionChange = { newDesc -> viewModel.onEtapaDescriptionChanged(etapaState, newDesc) },
                    onDurationChange = { newDuration -> viewModel.onEtapaDurationChanged(etapaState, newDuration) },
                    onRemoveClick = { viewModel.removeEtapa(etapaState) },
                    onMoveUp = { viewModel.moveEtapa(index, -1) },
                    onMoveDown = { viewModel.moveEtapa(index, 1) },
                    showUp = index > 0,
                    showDown = index < uiState.etapas.size - 1
                )
            }

            TextButton(onClick = { viewModel.addEtapa() }) {
                Icon(Icons.Default.AddCircle, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Adicionar Preparativo")
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // ITENS DE LEMBRETE
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Não esquecer de levar", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.width(8.dp))
                InfoIconWithDialog(title = "Checklist de Saída", description = "Itens essenciais para levar com você. Eles aparecerão escritos na notificação de 'Hora de Sair'.")
            }

            uiState.itensLembrete.forEach { itemState ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    OutlinedTextField(
                        value = itemState.nome,
                        onValueChange = { viewModel.onItemNomeChanged(itemState, it) },
                        placeholder = { Text("Ex: Chaves") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, keyboardType = KeyboardType.Text)
                    )
                    IconButton(onClick = { viewModel.removeItemLembrete(itemState) }) {
                        Icon(Icons.Default.Clear, contentDescription = "Remover item", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }

            TextButton(onClick = { viewModel.addItemLembrete() }) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Adicionar Item")
            }

            Spacer(modifier = Modifier.height(32.dp)) // Espaço final
        }

        // DIALOGS (Mantidos iguais)
        if (uiState.showDatePicker) {
            val datePickerState = rememberDatePickerState(initialSelectedDateMillis = uiState.calendar.timeInMillis)
            DatePickerDialog(
                onDismissRequest = { viewModel.closeDatePicker() },
                confirmButton = { Button(onClick = { datePickerState.selectedDateMillis?.let { dataSelecionada -> viewModel.onDateSelected(dataSelecionada) }; viewModel.closeDatePicker() }) { Text("OK") } },
                dismissButton = { Button(onClick = { viewModel.closeDatePicker() }) { Text("Cancelar") } }
            ) { DatePicker(state = datePickerState) }
        }

        if (uiState.showTimePicker) {
            val timePickerState = rememberTimePickerState(initialHour = uiState.calendar.get(java.util.Calendar.HOUR_OF_DAY), initialMinute = uiState.calendar.get(java.util.Calendar.MINUTE), is24Hour = true)
            TimePickerDialog(
                onDismissRequest = { viewModel.closeTimePicker() },
                onConfirm = { viewModel.onTimeSelected(timePickerState.hour, timePickerState.minute); viewModel.closeTimePicker() }
            ) { TimePicker(state = timePickerState) }
        }
    }
}

// --- NOVO COMPONENTE DE SELEÇÃO ---
@Composable
fun NotificationTypeSelector(
    selected: NotificationType,
    onSelect: (NotificationType) -> Unit
) {
    Column {
        NotificationType.values().forEach { type ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(type) }
                    .padding(vertical = 4.dp)
            ) {
                RadioButton(selected = (type == selected), onClick = { onSelect(type) })
                Column {
                    Text(text = when(type) {
                        NotificationType.PADRAO -> "Padrão"
                        NotificationType.ALARME -> "Alarme Tela Cheia"
                        NotificationType.INSISTENTE -> "Insistente (Segurar)"
                        NotificationType.DESAFIO -> "Confirmação (Digitar)"
                    }, fontWeight = FontWeight.Bold)
                    Text(text = when(type) {
                        NotificationType.PADRAO -> "Notificação simples no topo da tela."
                        NotificationType.ALARME -> "Acorda a tela como um despertador."
                        NotificationType.INSISTENTE -> "Exige segurar o botão por 3s para parar."
                        NotificationType.DESAFIO -> "Exige digitar 'JÁ FIZ' para parar."
                    }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
@Composable fun InfoIconWithDialog(title: String, description: String, modifier: Modifier = Modifier) { var showDialog by remember { mutableStateOf(false) }; IconButton(onClick = { showDialog = true }, modifier = modifier) { Icon(imageVector = Icons.Outlined.Info, contentDescription = "Info", tint = MaterialTheme.colorScheme.primary) }; if (showDialog) { AlertDialog(onDismissRequest = { showDialog = false }, icon = { Icon(Icons.Default.Info, contentDescription = null) }, title = { Text(text = title) }, text = { Text(text = description) }, confirmButton = { TextButton(onClick = { showDialog = false }) { Text("Entendi") } }) } }
@Composable fun DiasSemanaSelector(diasSelecionados: List<Int>, onDiaClick: (Int) -> Unit) { val dias = listOf("D" to Calendar.SUNDAY, "S" to Calendar.MONDAY, "T" to Calendar.TUESDAY, "Q" to Calendar.WEDNESDAY, "Q" to Calendar.THURSDAY, "S" to Calendar.FRIDAY, "S" to Calendar.SATURDAY); Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { dias.forEach { (letra, dia) -> val isSelected = diasSelecionados.contains(dia); Button(onClick = { onDiaClick(dia) }, shape = CircleShape, modifier = Modifier.size(40.dp), contentPadding = PaddingValues(0.dp), colors = ButtonDefaults.buttonColors(containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant, contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)) { Text(text = letra) } } } }
@Composable fun TimePickerDialog(title: String = "Selecione a Hora", onDismissRequest: () -> Unit, onConfirm: () -> Unit, content: @Composable () -> Unit) { AlertDialog(onDismissRequest = onDismissRequest, title = { Text(title) }, text = content, confirmButton = { Button(onClick = onConfirm) { Text("OK") } }, dismissButton = { TextButton(onClick = onDismissRequest) { Text("Cancelar") } }) }
@Composable fun EtapaInputItem(etapaState: EtapaState, onDescriptionChange: (String) -> Unit, onDurationChange: (String) -> Unit, onRemoveClick: () -> Unit, onMoveUp: () -> Unit, onMoveDown: () -> Unit, showUp: Boolean, showDown: Boolean) { Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) { Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(24.dp)) { if (showUp) IconButton(onClick = onMoveUp, modifier = Modifier.size(20.dp)) { Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Subir", tint = MaterialTheme.colorScheme.primary) } else Spacer(modifier = Modifier.size(20.dp)); Spacer(modifier = Modifier.height(2.dp)); if (showDown) IconButton(onClick = onMoveDown, modifier = Modifier.size(20.dp)) { Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Descer", tint = MaterialTheme.colorScheme.primary) } else Spacer(modifier = Modifier.size(20.dp)) }
    OutlinedTextField(value = etapaState.descricao, onValueChange = onDescriptionChange, label = { Text("Descrição") }, modifier = Modifier.weight(0.6f), keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, keyboardType = KeyboardType.Text))
    OutlinedTextField(value = etapaState.duracaoMinutos, onValueChange = onDurationChange, label = { Text("Min") }, modifier = Modifier.weight(0.25f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), isError = etapaState.isError)
    IconButton(onClick = onRemoveClick, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.RemoveCircle, contentDescription = "Remover", tint = MaterialTheme.colorScheme.error) } } }