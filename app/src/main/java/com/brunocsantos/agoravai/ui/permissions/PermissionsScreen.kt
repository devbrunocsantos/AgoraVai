package com.brunocsantos.agoravai.ui.permissions

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.brunocsantos.agoravai.utils.ResponsiveText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionsScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Estados das permissões (atualizam ao retomar o app)
    var hasNotificationPermission by remember { mutableStateOf(false) }
    var hasLocationPermission by remember { mutableStateOf(false) }
    var hasExactAlarmPermission by remember { mutableStateOf(false) }
    var hasOverlayPermission by remember { mutableStateOf(false) }

    // Função para verificar o status atual
    fun checkPermissions() {
        hasNotificationPermission = if (Build.VERSION.SDK_INT >= 33) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else true

        hasLocationPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

        hasExactAlarmPermission = if (Build.VERSION.SDK_INT >= 31) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.canScheduleExactAlarms()
        } else true

        // Verifica se pode desenhar sobre outros apps (indicativo para telas cheias em alguns Androids)
        hasOverlayPermission = Settings.canDrawOverlays(context)
    }

    // Observa o ciclo de vida para atualizar os checks quando o usuário voltar das configurações
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                checkPermissions()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Launchers para pedir permissão nativa (quando possível)
    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { checkPermissions() }
    val locationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { checkPermissions() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { ResponsiveText("Permissões", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
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
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, null, modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        "Para funcionar como seu assistente pessoal, o app precisa de acesso a alguns recursos. Toque para ativar.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            HorizontalDivider()

            // 1. Notificações
            PermissionItemRow(
                title = "Notificações",
                description = "Essencial para os lembretes de 'Hora de Sair' e avisos dos preparativos.",
                icon = Icons.Default.NotificationsActive,
                isGranted = hasNotificationPermission,
                onClick = {
                    if (Build.VERSION.SDK_INT >= 33) notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    else openAppSettings(context)
                }
            )

            // 2. Localização
            PermissionItemRow(
                title = "Localização (GPS)",
                description = "Necessário para calcular o trânsito real e saber quando você chegou ao destino.",
                icon = Icons.Default.LocationOn,
                isGranted = hasLocationPermission,
                onClick = { locationLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION) }
            )

            // 3. Alarmes Exatos
            PermissionItemRow(
                title = "Alarmes Exatos",
                description = "Garante que o lembrete toque na hora certa, contornando a economia de bateria.",
                icon = Icons.Default.Alarm,
                isGranted = hasExactAlarmPermission,
                onClick = {
                    if (Build.VERSION.SDK_INT >= 31) {
                        val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}"))
                        try { context.startActivity(intent) } catch (e: Exception) { openAppSettings(context) }
                    }
                }
            )

            // 4. Sobreposição (Para o modo Insistente)
            PermissionItemRow(
                title = "Tela Cheia / Sobreposição",
                description = "CRUCIAL para o modo 'Insistente'. Permite que o alarme apareça sobre outros apps.",
                icon = Icons.Default.Layers,
                isGranted = hasOverlayPermission,
                onClick = {
                    val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
                    try { context.startActivity(intent) } catch (e: Exception) { openAppSettings(context) }
                }
            )

            HorizontalDivider()

            // 5. Card Específico para Xiaomi/MIUI (Sempre visível como dica extra)
            XiaomiPermissionCard(context)

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun PermissionItemRow(
    title: String,
    description: String,
    icon: ImageVector,
    isGranted: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isGranted) { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isGranted) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        border = if (isGranted) null else null // Sem borda se ok
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isGranted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(description, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(modifier = Modifier.width(8.dp))

            if (isGranted) {
                Icon(Icons.Default.CheckCircle, "Concedido", tint = MaterialTheme.colorScheme.primary)
            } else {
                Button(onClick = onClick, contentPadding = PaddingValues(horizontal = 12.dp)) {
                    Text("Ativar")
                }
            }
        }
    }
}

@Composable
fun XiaomiPermissionCard(context: Context) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)) // Laranja suave
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.WarningAmber, null, tint = Color(0xFFE65100))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Usuários Xiaomi / Samsung", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFFE65100))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Se o alarme não aparece em tela cheia, você precisa ativar manualmente 'Iniciar em Segundo Plano' e 'Mostrar na Tela de Bloqueio' nas configurações do App.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFBF360C)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = { openAppSettings(context) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100))
            ) {
                Text("Abrir Configurações do Sistema")
            }
        }
    }
}

private fun openAppSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
    intent.data = Uri.parse("package:${context.packageName}")
    context.startActivity(intent)
}