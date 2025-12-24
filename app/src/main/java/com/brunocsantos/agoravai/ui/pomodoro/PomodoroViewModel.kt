package com.brunocsantos.agoravai.ui.pomodoro

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.brunocsantos.agoravai.billing.BillingClientManager
import com.brunocsantos.agoravai.data.AppDao
import com.brunocsantos.agoravai.data.AutomationSetting
import com.brunocsantos.agoravai.data.SessaoPomodoro
import com.brunocsantos.agoravai.services.FocusSessionState
import com.brunocsantos.agoravai.services.PomodoroCompletionState
import com.brunocsantos.agoravai.services.PomodoroScheduler
import com.brunocsantos.agoravai.services.PomodoroService
import com.brunocsantos.agoravai.services.PomodoroTimerState
import com.brunocsantos.agoravai.services.geofence.GeofenceManager
import com.brunocsantos.agoravai.utils.SettingsManager
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.libraries.places.api.model.Place
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.DayOfWeek
import java.util.Calendar
import java.util.Locale

data class EditAutomationUiState(
    val id: Int = 0,
    val title: String = "",
    val isTimeEnabled: Boolean = false,
    val isLocationEnabled: Boolean = false,
    val isRepeatEnabled: Boolean = false,
    val isCompleted: Boolean = false,
    val hour: Int = 9,
    val minute: Int = 0,
    val repeatDays: Set<DayOfWeek> = emptySet(),
    val showTimePicker: Boolean = false,
    val placeId: String? = null,
    val placeAddress: String? = "Toque para selecionar",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val cycles: String = SettingsManager.DEFAULT_CYCLES.toString()
)

class PomodoroViewModel(
    private val dao: AppDao,
    val billingClientManager: BillingClientManager,
    private val applicationContext: Context
) : ViewModel() {

    val sessionCompletedEvent: StateFlow<Long?> = PomodoroCompletionState.sessionCompletedEvent

    val tempoRestante: StateFlow<Long> = PomodoroTimerState.tempoRestante
    val estadoPomodoro: StateFlow<PomodoroState> = PomodoroTimerState.estadoPomodoro
    val estadoTimer: StateFlow<TimerState> = PomodoroTimerState.estadoTimer

    val automationSettings: StateFlow<List<AutomationSetting>> = dao.getAllAutomationSettings()
        .stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000L), initialValue = emptyList())
    val completedAutomationSettings: StateFlow<List<AutomationSetting>> = dao.getCompletedAutomationSettings()
        .stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000L), initialValue = emptyList())

    private val _editAutomationUiState = MutableStateFlow(EditAutomationUiState())
    val editAutomationUiState: StateFlow<EditAutomationUiState> = _editAutomationUiState.asStateFlow()
    private val _launchAutocomplete = MutableStateFlow(false)
    val launchAutocomplete = _launchAutocomplete.asStateFlow()
    val isPremiumUser: StateFlow<Boolean> = billingClientManager.isPremium

    val focusDuration = SettingsManager.focusDuration
    val shortBreakDuration = SettingsManager.shortBreakDuration
    val longBreakDuration = SettingsManager.longBreakDuration
    val cyclesBeforeLongBreak = SettingsManager.cyclesBeforeLongBreak
    val dailyGoal = SettingsManager.dailyGoal
    val safetyTime = SettingsManager.safetyTime // <-- NOVO STATEFLOW

    val dailyProgress: StateFlow<Int> = dao.getSessoesConcluidasHoje(getStartOfDayInMillis())
        .stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000L), initialValue = 0)


    val isSaveEnabled: StateFlow<Boolean> = _editAutomationUiState
        .map { state ->
            val isTitleValid = state.title.isNotBlank()
            val areRepeatDaysValid = !state.isRepeatEnabled || state.repeatDays.isNotEmpty()
            val isLocationValid = !state.isLocationEnabled || !state.placeId.isNullOrEmpty()
            val isAnyTriggerEnabled = state.isTimeEnabled || state.isLocationEnabled
            val areCyclesValid = state.cycles.toIntOrNull() != null && state.cycles.toInt() > 0
            isTitleValid && areRepeatDaysValid && isLocationValid && isAnyTriggerEnabled && areCyclesValid
        }
        .stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = false)

    init {
        viewModelScope.launch {
            FocusSessionState.focusSessionCompleted.collect {
                Log.d("PomodoroViewModel", "Evento de foco concluído recebido. Inserindo no DB.")
                dao.inserirSessaoPomodoroConcluida(SessaoPomodoro())
            }
        }

        viewModelScope.launch {
            val twentyFourHoursAgo = System.currentTimeMillis() - 24 * 60 * 60 * 1000
            dao.limparSessoesAntigas(twentyFourHoursAgo)
        }
    }

    private fun getStartOfDayInMillis(): Long {
        return Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    fun onCompletionEventConsumed() {
        PomodoroCompletionState.onEventConsumed()
    }

    fun startPurchaseFlow(activity: Activity) {
        billingClientManager.launchPurchaseFlow(activity)
    }

    // FUNÇÃO CORRIGIDA COM O NOVO PARÂMETRO
    fun savePomodoroSettings(context: Context, focus: Int, shortBreak: Int, longBreak: Int, cycles: Int, dailyGoal: Int, safetyTime: Int) {
        SettingsManager.saveSettings(context, focus, shortBreak, longBreak, cycles, dailyGoal, safetyTime)
        PomodoroTimerState.resetToDefaults(context)
    }

    fun onAddNewAutomation() {
        _editAutomationUiState.value = EditAutomationUiState(
            title = "Novo Pomodoro",
            cycles = cyclesBeforeLongBreak.value.toString()
        )
    }

    fun onEditAutomation(setting: AutomationSetting) {
        _editAutomationUiState.value = EditAutomationUiState(
            id = setting.id, title = setting.title, isTimeEnabled = setting.isTimeEnabled,
            isLocationEnabled = setting.isLocationEnabled, isRepeatEnabled = setting.isRepeatEnabled,
            isCompleted = setting.isCompleted, hour = setting.hour, minute = setting.minute,
            repeatDays = setting.repeatDays, placeId = setting.placeId,
            placeAddress = setting.placeAddress ?: "Toque para selecionar",
            latitude = setting.latitude, longitude = setting.longitude,
            cycles = setting.cycles.toString()
        )
    }

    fun onTitleChanged(title: String) { _editAutomationUiState.update { it.copy(title = title) } }
    fun onTimeSelected(hour: Int, minute: Int) { _editAutomationUiState.update { it.copy(hour = hour, minute = minute) } }
    fun onDaySelected(day: DayOfWeek) {
        _editAutomationUiState.update { currentState ->
            val newDays = currentState.repeatDays.toMutableSet()
            if (newDays.contains(day)) newDays.remove(day) else newDays.add(day)
            currentState.copy(repeatDays = newDays)
        }
    }

    fun openTimePicker() { _editAutomationUiState.update { it.copy(showTimePicker = true) } }
    fun closeTimePicker() { _editAutomationUiState.update { it.copy(showTimePicker = false) } }
    fun onTimeToggle(isEnabled: Boolean) { _editAutomationUiState.update { it.copy(isTimeEnabled = isEnabled) } }
    fun onLocationToggle(isEnabled: Boolean) { _editAutomationUiState.update { it.copy(isLocationEnabled = isEnabled) } }
    fun onRepeatToggle(isEnabled: Boolean) { _editAutomationUiState.update { it.copy(isRepeatEnabled = isEnabled) } }
    fun onLocationClick() { _launchAutocomplete.value = true }

    fun onLocationSelected(place: Place) {
        _editAutomationUiState.update {
            it.copy(
                placeId = place.id, placeAddress = place.address,
                latitude = place.latLng?.latitude, longitude = place.latLng?.longitude
            )
        }
    }

    fun onCyclesChanged(cycles: String) {
        if (cycles.all { it.isDigit() }) {
            _editAutomationUiState.update { it.copy(cycles = cycles) }
        }
    }

    fun saveAutomationSetting(context: Context) {
        val currentState = _editAutomationUiState.value
        val existingSetting = automationSettings.value.find { it.id == currentState.id }
        val isEnabled = existingSetting?.isEnabled ?: true
        val setting = AutomationSetting(
            id = currentState.id, title = currentState.title, isEnabled = isEnabled,
            isTimeEnabled = currentState.isTimeEnabled, isLocationEnabled = currentState.isLocationEnabled,
            isRepeatEnabled = currentState.isRepeatEnabled, isCompleted = currentState.isCompleted,
            hour = currentState.hour, minute = currentState.minute, repeatDays = currentState.repeatDays,
            placeId = currentState.placeId, placeAddress = currentState.placeAddress,
            latitude = currentState.latitude, longitude = currentState.longitude,
            cycles = currentState.cycles.toIntOrNull() ?: SettingsManager.DEFAULT_CYCLES
        )
        viewModelScope.launch {
            dao.insertOrUpdateAutomationSetting(setting)
            updateSchedules(context)
        }
    }

    fun toggleAutomation(context: Context, setting: AutomationSetting) {
        viewModelScope.launch {
            val updatedSetting = setting.copy(isEnabled = !setting.isEnabled)
            dao.insertOrUpdateAutomationSetting(updatedSetting)
            updateSchedules(context)
        }
    }

    fun deleteCurrentAutomation(context: Context) {
        val settingIdToDelete = _editAutomationUiState.value.id
        if (settingIdToDelete != 0) {
            viewModelScope.launch {
                dao.deleteAutomationSettingById(settingIdToDelete)
                GeofenceManager.remove(settingIdToDelete)
                updateSchedules(context)
            }
        }
    }

    fun reactivateAutomation(setting: AutomationSetting, context: Context) {
        viewModelScope.launch {
            dao.reactivateAutomation(setting.id)
            updateSchedules(context)
        }
    }

    fun deleteAutomationPermanently(setting: AutomationSetting) {
        viewModelScope.launch { dao.deleteAutomationSettingById(setting.id) }
    }

    fun clearHistory() {
        viewModelScope.launch {
            dao.deleteCompletedAutomations()
        }
    }

    private fun updateSchedules(context: Context) {
        viewModelScope.launch {
            PomodoroScheduler.cancelAll(context)
            val allSettings = dao.getAllAutomationSettingsOnce()
            allSettings.forEach { setting ->
                if (setting.isEnabled) {
                    if (setting.isTimeEnabled) { PomodoroScheduler.schedule(context, setting) }
                    GeofenceManager.addOrUpdateGeofence(setting)
                } else {
                    GeofenceManager.remove(setting.id)
                }
            }
        }
    }

    fun iniciarOuRetomarTimer(context: Context) {
        val intent = Intent(context, PomodoroService::class.java).apply { action = PomodoroService.Actions.START.toString() }
        context.startService(intent)
    }

    fun pausarTimer(context: Context) {
        val intent = Intent(context, PomodoroService::class.java).apply { action = PomodoroService.Actions.PAUSE.toString() }
        context.startService(intent)
    }

    fun resetarTimer(context: Context) {
        val intent = Intent(context, PomodoroService::class.java).apply { action = PomodoroService.Actions.RESET.toString() }
        context.startService(intent)
    }

    @SuppressLint("MissingPermission")
    fun fetchCurrentLocation() {
        if (ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.w("PomodoroViewModel", "fetchCurrentLocation chamada sem permissão de localização.")
            return
        }

        viewModelScope.launch {
            try {
                val fusedLocationClient = LocationServices.getFusedLocationProviderClient(applicationContext)
                val location = fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    CancellationTokenSource().token
                ).await()

                if (location != null) {
                    val geocoder = Geocoder(applicationContext, Locale.getDefault())
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        geocoder.getFromLocation(location.latitude, location.longitude, 1) { addressList ->
                            if (addressList.isNotEmpty()) {
                                val address = addressList[0]
                                val addressText = address.getAddressLine(0) ?: "Endereço desconhecido"
                                _editAutomationUiState.update {
                                    it.copy(
                                        placeAddress = addressText,
                                        latitude = location.latitude,
                                        longitude = location.longitude,
                                        placeId = "current_location_${System.currentTimeMillis()}"
                                    )
                                }
                            }
                        }
                    } else {
                        @Suppress("DEPRECATION")
                        val legacyAddresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                        if (legacyAddresses != null && legacyAddresses.isNotEmpty()) {
                            val address = legacyAddresses[0]
                            val addressText = address.getAddressLine(0) ?: "Endereço desconhecido"
                            _editAutomationUiState.update {
                                it.copy(
                                    placeAddress = addressText,
                                    latitude = location.latitude,
                                    longitude = location.longitude,
                                    placeId = "current_location_${System.currentTimeMillis()}"
                                )
                            }
                        }
                    }
                } else {
                    Log.e("PomodoroViewModel", "Não foi possível obter a localização.")
                }
            } catch (e: Exception) {
                Log.e("PomodoroViewModel", "Exceção ao buscar localização: ${e.message}", e)
            }
        }
    }
}