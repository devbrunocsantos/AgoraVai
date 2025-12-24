package com.brunocsantos.agoravai.ui.pomodoro

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.brunocsantos.agoravai.data.AutomationSetting
import com.brunocsantos.agoravai.utils.ResponsiveText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    completedItems: List<AutomationSetting>,
    onBackClick: () -> Unit,
    onReactivateClick: (AutomationSetting) -> Unit,
    onDeleteClick: (AutomationSetting) -> Unit,
    onClearHistoryClick: () -> Unit
) {
    var showClearHistoryDialog by remember { mutableStateOf(false) }

    if (showClearHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showClearHistoryDialog = false },
            title = { Text("Limpar Histórico?") },
            text = { Text("Tem certeza que deseja excluir permanentemente todos os itens do histórico? Esta ação não pode ser desfeita.") },
            confirmButton = {
                Button(
                    onClick = {
                        onClearHistoryClick()
                        showClearHistoryDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Excluir Tudo")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearHistoryDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    ResponsiveText(
                        text = "Histórico de Pomodoros",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    // Só mostra o botão de limpar se a lista não estiver vazia
                    if (completedItems.isNotEmpty()) {
                        IconButton(onClick = { showClearHistoryDialog = true }) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = "Limpar Histórico")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        if (completedItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Seu histórico está vazio.")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(completedItems) { setting ->
                    // Aqui você pode criar um Composable de item de histórico
                    // ou reutilizar o AutomationItem com algumas modificações.
                    // Por simplicidade, vamos usar um Card simples.
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Text(setting.title, style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(onClick = { onDeleteClick(setting) }) {
                                    Text("Excluir")
                                }
                                Spacer(Modifier.width(8.dp))
                                Button(onClick = { onReactivateClick(setting) }) {
                                    Text("Reativar")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}