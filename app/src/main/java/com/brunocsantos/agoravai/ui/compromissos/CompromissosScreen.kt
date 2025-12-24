package com.brunocsantos.agoravai.ui.compromissos

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.brunocsantos.agoravai.data.Compromisso
import com.brunocsantos.agoravai.data.CompromissoComEtapas
import com.brunocsantos.agoravai.data.Etapa
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun CompromissosScreen(
    viewModel: CompromissoViewModel,
    onNavigateToAdd: () -> Unit,
    onNavigateToEdit: (CompromissoComEtapas) -> Unit
) {
    val compromissos by viewModel.compromissos.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var compromissoParaExcluir by remember { mutableStateOf<CompromissoComEtapas?>(null) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onNavigateToAdd) {
                Icon(Icons.Default.Add, contentDescription = "Adicionar Compromisso")
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            if (compromissos.isEmpty()) {
                EmptyStateMessage()
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(compromissos, key = { it.compromisso.id }) { compromissoComEtapas ->
                        CompromissoItem(
                            compromissoComEtapas = compromissoComEtapas,
                            onDeleteClick = { compromissoParaExcluir = it },
                            onEditClick = { onNavigateToEdit(it) },
                            onToggle = { isEnabled -> viewModel.toggleCompromisso(compromissoComEtapas, isEnabled) },
                            context = context
                        )
                    }
                }
            }
        }
    }

    compromissoParaExcluir?.let { compromisso ->
        AlertDialog(
            onDismissRequest = { compromissoParaExcluir = null },
            title = { Text("Confirmar Exclusão") },
            text = { Text("Tem certeza de que deseja excluir o compromisso \"${compromisso.compromisso.titulo}\"? Todas as notificações agendadas serão canceladas.") },
            confirmButton = {
                Button(
                    onClick = { viewModel.excluirCompromisso(context, compromisso); compromissoParaExcluir = null },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Excluir") }
            },
            dismissButton = { Button(onClick = { compromissoParaExcluir = null }) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun EmptyStateMessage() {
    Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Icon(Icons.Default.CheckCircleOutline, null, Modifier.size(80.dp), MaterialTheme.colorScheme.primary.copy(alpha = 0.7f))
            Text("Tudo organizado por aqui! ✨", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
            Text("Toque no botão + para planejar\nseu próximo compromisso.", style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun CompromissoItem(
    compromissoComEtapas: CompromissoComEtapas,
    onDeleteClick: (CompromissoComEtapas) -> Unit,
    onEditClick: (CompromissoComEtapas) -> Unit,
    onToggle: (Boolean) -> Unit,
    context: Context
) {
    val dateFormatter = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    val timeFormatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    var isExpanded by remember { mutableStateOf(false) }
    val containerColor = if (compromissoComEtapas.compromisso.isEnabled) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)

    Card(
        modifier = Modifier.fillMaxWidth().animateContentSize(spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium)),
        elevation = CardDefaults.cardElevation(defaultElevation = if (compromissoComEtapas.compromisso.isEnabled) 4.dp else 1.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(modifier = Modifier.clickable { isExpanded = !isExpanded }.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = compromissoComEtapas.compromisso.titulo, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = if (compromissoComEtapas.compromisso.isEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(4.dp))
                    InfoRow(icon = Icons.Default.CalendarMonth, text = "${dateFormatter.format(Date(compromissoComEtapas.compromisso.dataHoraEvento))} às ${timeFormatter.format(Date(compromissoComEtapas.compromisso.dataHoraEvento))}")
                    if (compromissoComEtapas.compromisso.diasRepeticao.isNotBlank()) { Spacer(modifier = Modifier.height(4.dp)); InfoRow(icon = Icons.Default.Repeat, text = formatarDiasRepeticao(compromissoComEtapas.compromisso.diasRepeticao)) }
                }
                Switch(checked = compromissoComEtapas.compromisso.isEnabled, onCheckedChange = { onToggle(it) }, modifier = Modifier.padding(end = 8.dp))
                Icon(imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = if (isExpanded) "Recolher" else "Expandir", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            AnimatedVisibility(visible = isExpanded) {
                Column {
                    Divider(modifier = Modifier.padding(vertical = 12.dp))
                    Text("Detalhes do Planejamento", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
                    val tempoTotalPreparo = compromissoComEtapas.etapas.sumOf { it.duracaoMinutos }
                    InfoRow(icon = Icons.Default.AccessTime, text = "Tempo total de preparo: $tempoTotalPreparo minutos")
                    Spacer(modifier = Modifier.height(4.dp))
                    InfoRow(icon = Icons.Default.DirectionsCar, text = "Tempo de viagem estimado: ${compromissoComEtapas.compromisso.tempoDeslocamentoMinutos} minutos")

                    if (compromissoComEtapas.etapas.isNotEmpty()) { Spacer(modifier = Modifier.height(16.dp)); Text("Preparativos:", style = MaterialTheme.typography.titleSmall); Spacer(modifier = Modifier.height(8.dp)); compromissoComEtapas.etapas.forEach { etapa -> EtapaDetailItem(etapa = etapa) } }
                    if (compromissoComEtapas.itensLembrete.isNotEmpty()) { Spacer(modifier = Modifier.height(16.dp)); Text("Não esquecer:", style = MaterialTheme.typography.titleSmall); Spacer(modifier = Modifier.height(4.dp)); Text(text = compromissoComEtapas.itensLembrete.joinToString(", ") { it.nomeItem }, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }

                    Row(modifier = Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.End) {
                        IconButton(onClick = { abrirRotaNoMaps(context, compromissoComEtapas.compromisso.origem, compromissoComEtapas.compromisso.destino) }) { Icon(imageVector = Icons.Default.Map, contentDescription = "Ver rota no Mapa", tint = MaterialTheme.colorScheme.primary) }
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(onClick = { onEditClick(compromissoComEtapas) }) { Icon(imageVector = Icons.Default.Edit, contentDescription = "Editar Compromisso", tint = MaterialTheme.colorScheme.secondary) }
                        IconButton(onClick = { onDeleteClick(compromissoComEtapas) }) { Icon(imageVector = Icons.Default.Delete, contentDescription = "Excluir Compromisso", tint = MaterialTheme.colorScheme.error) }
                    }
                }
            }
        }
    }
}

private fun abrirRotaNoMaps(context: Context, origem: String, destino: String) {
    try {
        val uriString = "http://maps.google.com/maps?daddr=${Uri.encode(destino)}" // Simplificado para destino direto
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uriString))
        context.startActivity(intent)
    } catch (e: Exception) { Toast.makeText(context, "Não foi possível abrir o mapa.", Toast.LENGTH_SHORT).show() }
}

@Composable fun InfoRow(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) { Row(verticalAlignment = Alignment.CenterVertically) { Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary); Spacer(modifier = Modifier.width(8.dp)); Text(text = text, style = MaterialTheme.typography.bodyMedium) } }
@Composable fun EtapaDetailItem(etapa: Etapa) { Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.CheckCircleOutline, null, Modifier.size(20.dp), MaterialTheme.colorScheme.secondary); Spacer(Modifier.width(12.dp)); Text(etapa.descricao, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium); Text("${etapa.duracaoMinutos} min", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } }
private fun formatarDiasRepeticao(dias: String): String { if (dias.isBlank()) return "Não se repete"; val mapaDias = mapOf(Calendar.SUNDAY to "Dom", Calendar.MONDAY to "Seg", Calendar.TUESDAY to "Ter", Calendar.WEDNESDAY to "Qua", Calendar.THURSDAY to "Qui", Calendar.FRIDAY to "Sex", Calendar.SATURDAY to "Sáb"); return dias.split(',').mapNotNull { it.toIntOrNull() }.sorted().mapNotNull { mapaDias[it] }.joinToString(", ") }