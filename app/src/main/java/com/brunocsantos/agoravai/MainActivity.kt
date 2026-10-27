package com.brunocsantos.agoravai

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.brunocsantos.agoravai.billing.BillingClientManager
import com.brunocsantos.agoravai.data.AppDatabase
import com.brunocsantos.agoravai.notifications.NotificationScheduler
import com.brunocsantos.agoravai.services.AppForegroundState
import com.brunocsantos.agoravai.services.PomodoroTimerState
import com.brunocsantos.agoravai.ui.ViewModelFactory
import com.brunocsantos.agoravai.ui.compromissos.AddCompromissoScreen
import com.brunocsantos.agoravai.ui.compromissos.CompromissosScreen
import com.brunocsantos.agoravai.ui.compromissos.CompromissoViewModel
import com.brunocsantos.agoravai.ui.permissions.PermissionsScreen // <-- IMPORTANTE
import com.brunocsantos.agoravai.ui.pomodoro.*
import com.brunocsantos.agoravai.ui.settings.SettingsScreen
import com.brunocsantos.agoravai.ui.theme.AgoraVaiTheme
import com.brunocsantos.agoravai.utils.FraseMotivacionalManager
import com.brunocsantos.agoravai.utils.SettingsManager
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var billingClientManager: BillingClientManager
    private val database by lazy { AppDatabase.getDatabase(this) }
    private val viewModelFactory by lazy { ViewModelFactory(database.appDao(), billingClientManager, applicationContext) }

    private val compromissoViewModel: CompromissoViewModel by viewModels { viewModelFactory }
    private val pomodoroViewModel: PomodoroViewModel by viewModels { viewModelFactory }

    private var initialScreen: String? = null

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (!isGranted) {
            Toast.makeText(this, "Permissão para notificações é recomendada.", Toast.LENGTH_SHORT).show()
        }
    }

    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        requestBackgroundLocationPermission()
                    }
                }
            }
            else -> {
                Toast.makeText(this, "Permissão de localização é necessária para as automações por local.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private val placesAutocompleteLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val place = result.data?.let { Autocomplete.getPlaceFromIntent(it) }
            if (place != null) {
                pomodoroViewModel.onLocationSelected(place)
            }
        } else {
            Log.e("AgoraVai-Places", "Erro ou cancelamento no Autocomplete.")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initialScreen = intent.getStringExtra("destination_screen")
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) { AppForegroundState.setAppInForeground(true) }
            override fun onStop(owner: LifecycleOwner) { AppForegroundState.setAppInForeground(false) }
        })
        billingClientManager = BillingClientManager(this)
        initializePlacesApi()
        NotificationScheduler.createNotificationChannel(this)
        SettingsManager.initialize(this)
        PomodoroTimerState.resetToDefaults(this)
        askNotificationPermission()
        askLocationPermissions()
        checkAndRequestBatteryOptimizations()
        lifecycleScope.launch {
            pomodoroViewModel.launchAutocomplete.collectLatest { shouldLaunch ->
                if (shouldLaunch) { launchPlacesAutocomplete() }
            }
        }

        setContent {
            AgoraVaiTheme {
                MainScreenWithBottomNav(
                    compromissoViewModel = compromissoViewModel,
                    pomodoroViewModel = pomodoroViewModel,
                    initialScreen = if (initialScreen == "pomodoro") 1 else 0
                )
            }
        }
    }

    @SuppressLint("MissingSuperCall")
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val destination = intent.getStringExtra("destination_screen")
        if (destination == "pomodoro") {
            initialScreen = destination
            setContent {
                AgoraVaiTheme {
                    MainScreenWithBottomNav(
                        compromissoViewModel = compromissoViewModel,
                        pomodoroViewModel = pomodoroViewModel,
                        initialScreen = 1
                    )
                }
            }
        }
    }

    private fun initializePlacesApi() {
        try {
            val apiKey = BuildConfig.MAPS_API_KEY
            if (apiKey.isNotBlank() && !Places.isInitialized()) {
                Places.initialize(applicationContext, apiKey)
            }
        } catch (e: Exception) {
            Log.e("AgoraVai-Debug", "Falha grave ao inicializar o Google Places", e)
        }
    }

    private fun launchPlacesAutocomplete() {
        val fields = listOf(Place.Field.ID, Place.Field.NAME, Place.Field.ADDRESS, Place.Field.LAT_LNG)
        val intent = Autocomplete.IntentBuilder(AutocompleteActivityMode.OVERLAY, fields)
            .setCountry("BR")
            .build(this)
        placesAutocompleteLauncher.launch(intent)
    }

    private fun requestIgnoreBatteryOptimizations(context: Context) {
        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:" + context.packageName)
        }
        context.startActivity(intent)
    }

    private fun checkAndRequestBatteryOptimizations() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        val isIgnoringOptimizations = powerManager.isIgnoringBatteryOptimizations(packageName)
        if (!isIgnoringOptimizations) {
            val prefs = getSharedPreferences("app_settings", Context.MODE_PRIVATE)
            val hasBeenAsked = prefs.getBoolean("has_been_asked_battery_optimization", false)
            if (!hasBeenAsked) {
                AlertDialog.Builder(this)
                    .setTitle("Melhore a Confiabilidade")
                    .setMessage("Para garantir que os timers e automações funcionem corretamente mesmo com o app fechado, recomendamos desativar a otimização de bateria para este aplicativo.\n\nDeseja configurar isso agora?")
                    .setPositiveButton("Configurar") { _, _ ->
                        requestIgnoreBatteryOptimizations(this)
                        prefs.edit().putBoolean("has_been_asked_battery_optimization", true).apply()
                    }
                    .setNegativeButton("Agora Não") { dialog, _ ->
                        dialog.dismiss()
                        prefs.edit().putBoolean("has_been_asked_battery_optimization", true).apply()
                    }
                    .setCancelable(false)
                    .show()
            }
        }
    }

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun askLocationPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            locationPermissionRequest.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION))
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    requestBackgroundLocationPermission()
                }
            }
        }
    }

    private fun requestBackgroundLocationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            AlertDialog.Builder(this)
                .setTitle("Permissão Adicional Necessária")
                .setMessage("Para que as automações por local funcionem mesmo com o aplicativo fechado, por favor, escolha a opção 'Permitir o tempo todo' na próxima tela.")
                .setPositiveButton("Entendi") { _, _ ->
                    locationPermissionRequest.launch(arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION))
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreenWithBottomNav(
    compromissoViewModel: CompromissoViewModel,
    pomodoroViewModel: PomodoroViewModel,
    initialScreen: Int
) {
    var selectedScreen by rememberSaveable { mutableStateOf(initialScreen) }
    var showSettingsScreen by remember { mutableStateOf(false) }
    var showAutomationSettingsScreen by remember { mutableStateOf(false) }
    var showHistoryScreen by remember { mutableStateOf(false) }
    var showAddCompromissoScreen by remember { mutableStateOf(false) }
    var showPermissionsScreen by remember { mutableStateOf(false) } // <-- NOVO ESTADO

    val pomodoroAutomationState by pomodoroViewModel.editAutomationUiState.collectAsStateWithLifecycle()
    val isPomodoroSaveEnabled by pomodoroViewModel.isSaveEnabled.collectAsStateWithLifecycle()
    val pomodoroCompletedItems by pomodoroViewModel.completedAutomationSettings.collectAsStateWithLifecycle()

    val compromissoState by compromissoViewModel.addCompromissoUiState.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val fraseDoDia = FraseMotivacionalManager.getFraseDoDia(context)

    // Atualizado para incluir showPermissionsScreen
    BackHandler(enabled = showSettingsScreen || showAutomationSettingsScreen || showHistoryScreen || showAddCompromissoScreen || showPermissionsScreen) {
        when {
            showPermissionsScreen -> showPermissionsScreen = false // <-- Volta da tela de permissões
            showAutomationSettingsScreen -> showAutomationSettingsScreen = false
            showHistoryScreen -> showHistoryScreen = false
            showSettingsScreen -> showSettingsScreen = false
            showAddCompromissoScreen -> {
                showAddCompromissoScreen = false
                compromissoViewModel.resetAddCompromissoState()
            }
        }
    }

    if (pomodoroAutomationState.showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = pomodoroAutomationState.hour,
            initialMinute = pomodoroAutomationState.minute, is24Hour = true
        )
        TimePickerDialog(
            title = "Selecione o Horário",
            onDismissRequest = { pomodoroViewModel.closeTimePicker() },
            onConfirm = {
                pomodoroViewModel.onTimeSelected(timePickerState.hour, timePickerState.minute)
                pomodoroViewModel.closeTimePicker()
            },
        ) {
            TimePicker(state = timePickerState)
        }
    }

    when {
        showPermissionsScreen -> { // <-- RENDERIZA A TELA DE PERMISSÕES
            PermissionsScreen(
                onBackClick = { showPermissionsScreen = false }
            )
        }
        showAutomationSettingsScreen -> {
            AutomationSettingsScreen(
                automationState = pomodoroAutomationState,
                isSaveEnabled = isPomodoroSaveEnabled,
                onTitleChange = pomodoroViewModel::onTitleChanged,
                onTimeToggle = pomodoroViewModel::onTimeToggle,
                onLocationToggle = pomodoroViewModel::onLocationToggle,
                onRepeatToggle = pomodoroViewModel::onRepeatToggle,
                onLocationClick = pomodoroViewModel::onLocationClick,
                onTimeClick = pomodoroViewModel::openTimePicker,
                onDayClick = pomodoroViewModel::onDaySelected,
                onBackClick = { showAutomationSettingsScreen = false },
                onSaveClick = {
                    pomodoroViewModel.saveAutomationSetting(context)
                    showAutomationSettingsScreen = false
                },
                onDeleteClick = {
                    pomodoroViewModel.deleteCurrentAutomation(context)
                    showAutomationSettingsScreen = false
                },
                onCyclesChange = pomodoroViewModel::onCyclesChanged,
                onFetchCurrentLocationClick = pomodoroViewModel::fetchCurrentLocation
            )
        }
        showHistoryScreen -> {
            HistoryScreen(
                completedItems = pomodoroCompletedItems,
                onBackClick = { showHistoryScreen = false },
                onReactivateClick = { setting -> pomodoroViewModel.reactivateAutomation(setting, context) },
                onDeleteClick = { setting -> pomodoroViewModel.deleteAutomationPermanently(setting) },
                onClearHistoryClick = pomodoroViewModel::clearHistory
            )
        }
        showAddCompromissoScreen -> {
            val apiKey = BuildConfig.MAPS_API_KEY
            AddCompromissoScreen(
                uiState = compromissoState,
                viewModel = compromissoViewModel,
                onSaveClick = {
                    compromissoViewModel.salvarCompromisso(context, apiKey) {
                        showAddCompromissoScreen = false
                    }
                },
                onBackClick = {
                    showAddCompromissoScreen = false
                    compromissoViewModel.resetAddCompromissoState()
                }
            )
        }
        showSettingsScreen -> {
            SettingsScreen(
                viewModel = pomodoroViewModel,
                onBackClick = { showSettingsScreen = false },
                onNavigateToPermissions = { showPermissionsScreen = true } // <-- CONECTA O BOTÃO
            )
        }
        else -> {
            Scaffold(
                topBar = {
                    CenterAlignedTopAppBar(
                        title = {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Agora Vai...", color = MaterialTheme.colorScheme.primary, fontSize = 22.sp)
                                Text(fraseDoDia, style = MaterialTheme.typography.bodyMedium, fontSize = 14.sp)
                            }
                        },
                        actions = {
                            IconButton(onClick = { showSettingsScreen = true }) {
                                Icon(Icons.Default.Settings, contentDescription = "Configurações")
                            }
                        },
                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                },
                bottomBar = {
                    NavigationBar {
                        NavigationBarItem(
                            icon = { Icon(Icons.Default.Checklist, contentDescription = "Compromissos") },
                            label = { Text("Compromissos") },
                            selected = selectedScreen == 0,
                            onClick = { selectedScreen = 0 }
                        )
                        NavigationBarItem(
                            icon = { Icon(Icons.Default.Timer, contentDescription = "Pomodoro") },
                            label = { Text("Pomodoro") },
                            selected = selectedScreen == 1,
                            onClick = { selectedScreen = 1 }
                        )
                    }
                }
            ) { innerPadding ->
                Box(modifier = Modifier.padding(innerPadding)) {
                    if (selectedScreen == 0) {
                        CompromissosScreen(
                            viewModel = compromissoViewModel,
                            onNavigateToAdd = {
                                compromissoViewModel.resetAddCompromissoState()
                                showAddCompromissoScreen = true
                            },
                            onNavigateToEdit = { compromisso ->
                                compromissoViewModel.iniciarEdicao(compromisso)
                                showAddCompromissoScreen = true
                            }
                        )
                    } else {
                        PomodoroScreen(
                            viewModel = pomodoroViewModel,
                            onNavigateToAutomationSettings = {
                                pomodoroViewModel.onAddNewAutomation()
                                showAutomationSettingsScreen = true
                            },
                            onNavigateToHistory = { showHistoryScreen = true }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TimePickerDialog(
    title: String,
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
    content: @Composable () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(title) },
        text = content,
        confirmButton = { Button(onClick = onConfirm) { Text("OK") } },
        dismissButton = { TextButton(onClick = onDismissRequest) { Text("Cancelar") } }
    )
}