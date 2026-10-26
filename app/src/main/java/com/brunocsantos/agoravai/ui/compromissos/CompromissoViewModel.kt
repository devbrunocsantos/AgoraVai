package com.brunocsantos.agoravai.ui.compromissos

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.brunocsantos.agoravai.data.AppDao
import com.brunocsantos.agoravai.data.Compromisso
import com.brunocsantos.agoravai.data.CompromissoComEtapas
import com.brunocsantos.agoravai.data.Etapa
import com.brunocsantos.agoravai.data.ItemLembrete
import com.brunocsantos.agoravai.data.NotificationType
import com.brunocsantos.agoravai.network.RetrofitInstance
import com.brunocsantos.agoravai.notifications.EXTRA_NOTIFICATION_TYPE
import com.brunocsantos.agoravai.notifications.NotificationScheduler
import com.brunocsantos.agoravai.notifications.calcularHorarios
import com.brunocsantos.agoravai.services.NotificationActionReceiver
import com.brunocsantos.agoravai.services.geofence.CompromissoGeofenceManager
import com.brunocsantos.agoravai.utils.SettingsManager
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import java.util.Collections
import java.util.Locale
import java.util.TimeZone
import kotlin.random.Random

enum class AddressField { ORIGIN, DESTINATION }

data class EtapaState(
    val id: Int = System.currentTimeMillis().toInt() + Random.nextInt(),
    var descricao: String = "",
    var duracaoMinutos: String = "",
    val isError: Boolean = false
)

data class ItemLembreteState(
    val id: Int = System.currentTimeMillis().toInt() + Random.nextInt(),
    var nome: String = ""
)

data class AddCompromissoUiState(
    val id: Long? = null,
    val titulo: String = "",
    val origem: String = "",
    val destino: String = "",
    val etapas: List<EtapaState> = listOf(EtapaState()),
    val itensLembrete: List<ItemLembreteState> = emptyList(),
    val calendar: Calendar = Calendar.getInstance(),
    val showDatePicker: Boolean = false,
    val showTimePicker: Boolean = false,
    val diasRepeticao: List<Int> = emptyList(),
    val tempoDeSeguranca: String = "",
    val deleteAfterComplete: Boolean = false,
    val notificationType: NotificationType = NotificationType.PADRAO,
    val saveError: String? = null
)

class CompromissoViewModel(private val dao: AppDao, private val applicationContext: Context) : ViewModel() {

    val compromissos: StateFlow<List<CompromissoComEtapas>> = dao.getAllCompromissosComEtapas()
        .stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000L), initialValue = emptyList())

    private val _addCompromissoUiState = MutableStateFlow(AddCompromissoUiState())
    val addCompromissoUiState: StateFlow<AddCompromissoUiState> = _addCompromissoUiState.asStateFlow()

    private val defaultSafetyTime = SettingsManager.safetyTime
    private var tempLatDestino: Double = 0.0
    private var tempLngDestino: Double = 0.0

    init {
        viewModelScope.launch { try { dao.deleteExpiredCompromissos(System.currentTimeMillis()) } catch (e: Exception) { Log.e("CompromissoVM", "Erro clean", e) } }
    }

    // --- Métodos de UI ---
    fun onTituloChanged(t: String) { _addCompromissoUiState.update { it.copy(titulo = t, saveError = null) } }
    fun onOrigemChanged(o: String) { _addCompromissoUiState.update { it.copy(origem = o, saveError = null) } }
    fun onDestinoChanged(d: String) { _addCompromissoUiState.update { it.copy(destino = d, saveError = null) } }
    fun onDeleteAfterCompleteChanged(v: Boolean) { _addCompromissoUiState.update { it.copy(deleteAfterComplete = v) } }
    fun onNotificationTypeChanged(type: NotificationType) { _addCompromissoUiState.update { it.copy(notificationType = type) } }

    fun onSafetyTimeChanged(safetyTime: String) { if (safetyTime.all { it.isDigit() }) _addCompromissoUiState.update { it.copy(tempoDeSeguranca = safetyTime) } }
    fun openDatePicker() { _addCompromissoUiState.update { it.copy(showDatePicker = true) } }
    fun closeDatePicker() { _addCompromissoUiState.update { it.copy(showDatePicker = false) } }
    fun openTimePicker() { _addCompromissoUiState.update { it.copy(showTimePicker = true) } }
    fun closeTimePicker() { _addCompromissoUiState.update { it.copy(showTimePicker = false) } }

    fun resetAddCompromissoState() {
        val nextHour = Calendar.getInstance().apply { add(Calendar.HOUR_OF_DAY, 1); set(Calendar.MINUTE, 0) }
        val defaultItens = listOf(ItemLembreteState(nome = "Chaves \uD83D\uDD11"), ItemLembreteState(nome = "Carteira \uD83D\uDCB3"), ItemLembreteState(nome = "Documentos \uD83D\uDCC4"))
        _addCompromissoUiState.value = AddCompromissoUiState(calendar = nextHour, tempoDeSeguranca = defaultSafetyTime.value.toString(), itensLembrete = defaultItens)
    }

    // --- Etapas e Itens ---
    fun addEtapa() { _addCompromissoUiState.update { it.copy(etapas = it.etapas + EtapaState()) } }
    fun removeEtapa(etapa: EtapaState) { _addCompromissoUiState.update { s -> if(s.etapas.size > 1) s.copy(etapas = s.etapas.filterNot { it.id == etapa.id }) else s } }
    fun onEtapaDescriptionChanged(etapa: EtapaState, desc: String) { _addCompromissoUiState.update { s -> s.copy(etapas = s.etapas.map { if (it.id == etapa.id) it.copy(descricao = desc) else it }) } }
    fun onEtapaDurationChanged(etapa: EtapaState, dur: String) { _addCompromissoUiState.update { s -> s.copy(etapas = s.etapas.map { if (it.id == etapa.id) it.copy(duracaoMinutos = dur, isError = false) else it }) } }
    fun moveEtapa(fromIndex: Int, direction: Int) { _addCompromissoUiState.update { s -> val to = fromIndex + direction; if (to in s.etapas.indices) { val l = s.etapas.toMutableList(); Collections.swap(l, fromIndex, to); s.copy(etapas = l) } else s } }

    fun addItemLembrete() { _addCompromissoUiState.update { it.copy(itensLembrete = it.itensLembrete + ItemLembreteState()) } }
    fun removeItemLembrete(item: ItemLembreteState) { _addCompromissoUiState.update { s -> s.copy(itensLembrete = s.itensLembrete.filterNot { it.id == item.id }) } }
    fun onItemNomeChanged(item: ItemLembreteState, nome: String) { _addCompromissoUiState.update { s -> s.copy(itensLembrete = s.itensLembrete.map { if (it.id == item.id) it.copy(nome = nome) else it }) } }

    fun onDiaSemanaChanged(dia: Int) { _addCompromissoUiState.update { s -> val dias = s.diasRepeticao.toMutableList(); if (dias.contains(dia)) dias.remove(dia) else dias.add(dia); s.copy(diasRepeticao = dias) } }

    fun iniciarEdicao(compromisso: CompromissoComEtapas) {
        val cal = Calendar.getInstance().apply { timeInMillis = compromisso.compromisso.dataHoraEvento }
        val etapasState = if (compromisso.etapas.isNotEmpty()) compromisso.etapas.map { EtapaState(id = it.id, descricao = it.descricao, duracaoMinutos = it.duracaoMinutos.toString()) } else listOf(EtapaState())
        val itensState = if (compromisso.itensLembrete.isNotEmpty()) compromisso.itensLembrete.map { ItemLembreteState(id = it.id, nome = it.nomeItem) } else emptyList()
        _addCompromissoUiState.value = AddCompromissoUiState(
            id = compromisso.compromisso.id,
            titulo = compromisso.compromisso.titulo,
            origem = compromisso.compromisso.origem,
            destino = compromisso.compromisso.destino,
            etapas = etapasState,
            itensLembrete = itensState,
            calendar = cal,
            diasRepeticao = compromisso.compromisso.diasRepeticao.split(',').filter { it.isNotBlank() }.mapNotNull { it.toIntOrNull() },
            tempoDeSeguranca = compromisso.compromisso.tempoDeSegurancaMinutos.toString(),
            deleteAfterComplete = compromisso.compromisso.deleteAfterComplete,
            notificationType = compromisso.compromisso.notificationType
        )
    }

    // --- Salvar ---
    fun salvarCompromisso(context: Context, apiKey: String, onSuccess: () -> Unit) {
        val currentState = _addCompromissoUiState.value
        if (currentState.titulo.isBlank() || currentState.origem.isBlank() || currentState.destino.isBlank()) { _addCompromissoUiState.update { it.copy(saveError = "Preencha todos os campos.") }; return }

        val validatedEtapas = currentState.etapas.map { etapa ->
            val dur = etapa.duracaoMinutos.toIntOrNull() ?: 0
            if (etapa.descricao.isNotBlank() && dur <= 0) etapa.copy(isError = true) else etapa.copy(isError = false)
        }
        if (validatedEtapas.any { it.isError }) { _addCompromissoUiState.update { it.copy(etapas = validatedEtapas) }; return }
        val validatedItens = currentState.itensLembrete.filter { it.nome.isNotBlank() }

        viewModelScope.launch {
            try {
                val geocoder = Geocoder(context, Locale.getDefault())
                try {
                    @Suppress("DEPRECATION") val list = geocoder.getFromLocationName(currentState.destino, 1)
                    if (!list.isNullOrEmpty()) { tempLatDestino = list[0].latitude; tempLngDestino = list[0].longitude }
                } catch (e: Exception) { Log.w("CompromissoVM", "Geocoder falhou.") }

                val response = RetrofitInstance.api.getDirections(currentState.origem, currentState.destino, apiKey)
                if (response.isSuccessful && response.body()?.routes?.isNotEmpty() == true) {
                    val leg = response.body()!!.routes[0].legs[0]
                    val duracaoViagemMinutos = leg.duration.valueInSeconds / 60
                    val tempoSeguranca = currentState.tempoDeSeguranca.toIntOrNull() ?: defaultSafetyTime.value

                    val duracoesEtapas = validatedEtapas.filter { it.descricao.isNotBlank() }.map { it.duracaoMinutos.toIntOrNull() ?: 0 }
                    val horaLimiteInicio = calcularHorarios(currentState.calendar.timeInMillis, duracaoViagemMinutos, tempoSeguranca, duracoesEtapas).inicioPreparo

                    if (horaLimiteInicio < System.currentTimeMillis()) {
                        val atraso = (System.currentTimeMillis() - horaLimiteInicio) / (1000 * 60)
                        _addCompromissoUiState.update { it.copy(saveError = "Impossível! Atraso de $atraso min.") }
                        return@launch
                    }

                    val compromissoParaSalvar = Compromisso(
                        id = currentState.id ?: 0,
                        titulo = currentState.titulo,
                        dataHoraEvento = currentState.calendar.timeInMillis,
                        tempoDeslocamentoMinutos = duracaoViagemMinutos,
                        diasRepeticao = currentState.diasRepeticao.joinToString(","),
                        origem = currentState.origem,
                        destino = currentState.destino,
                        tempoDeSegurancaMinutos = tempoSeguranca,
                        isEnabled = true,
                        deleteAfterComplete = currentState.deleteAfterComplete,
                        latitudeDestino = tempLatDestino,
                        longitudeDestino = tempLngDestino,
                        notificationType = currentState.notificationType,
                        confirmouSaida = false,
                        confirmouChegada = false
                    )

                    val compromissoId = if (currentState.id == null) {
                        dao.inserirCompromisso(compromissoParaSalvar)
                    } else {
                        dao.updateCompromisso(compromissoParaSalvar)
                        dao.excluirEtapasPorCompromissoId(currentState.id)
                        dao.excluirItensPorCompromissoId(currentState.id)
                        CompromissoGeofenceManager.removeGeofence(currentState.id)
                        cancelarTodosAlarmes(context, currentState.id.toInt(), 10)
                        currentState.id
                    }

                    val etapasParaSalvar = validatedEtapas.filter { it.descricao.isNotBlank() }.map { Etapa(compromissoId = compromissoId, descricao = it.descricao, duracaoMinutos = it.duracaoMinutos.toInt()) }
                    dao.inserirEtapas(etapasParaSalvar)
                    val itensParaSalvar = validatedItens.map { ItemLembrete(compromissoId = compromissoId, nomeItem = it.nome) }
                    dao.inserirItensLembrete(itensParaSalvar)

                    val finalCompromisso = compromissoParaSalvar.copy(id = compromissoId)
                    CompromissoGeofenceManager.addGeofenceChegada(finalCompromisso)
                    agendarAlarmes(context, finalCompromisso, etapasParaSalvar, validatedItens)
                    onSuccess()
                } else { _addCompromissoUiState.update { it.copy(saveError = "Rota não encontrada.") } }
            } catch (e: Exception) { _addCompromissoUiState.update { it.copy(saveError = "Erro ao salvar.") } }
        }
    }

    fun toggleCompromisso(c: CompromissoComEtapas, isEnabled: Boolean) {
        viewModelScope.launch {
            val updated = c.compromisso.copy(isEnabled = isEnabled)
            dao.updateCompromisso(updated)
            val itensState = c.itensLembrete.map { ItemLembreteState(id = it.id, nome = it.nomeItem) }
            if (isEnabled) { agendarAlarmes(applicationContext, updated, c.etapas, itensState); CompromissoGeofenceManager.addGeofenceChegada(updated) }
            else { cancelarTodosAlarmes(applicationContext, updated.id.toInt(), c.etapas.size); CompromissoGeofenceManager.removeGeofence(updated.id) }
        }
    }

    fun excluirCompromisso(context: Context, c: CompromissoComEtapas) {
        viewModelScope.launch {
            dao.excluirCompromisso(c.compromisso)
            cancelarTodosAlarmes(context, c.compromisso.id.toInt(), c.etapas.size)
            CompromissoGeofenceManager.removeGeofence(c.compromisso.id)
        }
    }

    private fun agendarAlarmes(context: Context, compromisso: Compromisso, etapas: List<Etapa>, itens: List<ItemLembreteState>) {
        if (!compromisso.isEnabled) return
        val id = compromisso.id.toInt()
        val calendar = Calendar.getInstance().apply { timeInMillis = compromisso.dataHoraEvento }
        val tempoTotal = compromisso.tempoDeslocamentoMinutos + compromisso.tempoDeSegurancaMinutos
        val horarioSaida = (calendar.clone() as Calendar).apply { add(Calendar.MINUTE, -tempoTotal) }
        var proximoHorario = horarioSaida.clone() as Calendar

        etapas.reversed().forEachIndexed { index, etapa ->
            proximoHorario.add(Calendar.MINUTE, -etapa.duracaoMinutos)
            NotificationScheduler.scheduleAlarm(context, proximoHorario.timeInMillis, (id * 100) + index, "Próximo Preparativo: ${etapa.descricao}", "Faltam ${etapa.duracaoMinutos} min.")
        }

        // 1. Converte lista de itens para ArrayList<String> para passar na Intent
        val itemsList = ArrayList(itens.map { it.nome })

        val msgFinal = if (itemsList.isNotEmpty()) "Saia agora! Não esqueça seus itens." else "Saia agora para chegar a tempo!"

        val confirmIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.Actions.CONFIRM_EXIT.toString()
            putExtra("EXTRA_COMPROMISSO_ID", compromisso.id)
            putExtra("EXTRA_NOTIFICATION_ID", (id * 100) + 99)
        }
        val confirmPending = PendingIntent.getBroadcast(context, id, confirmIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        // 2. Passa a lista e o tipo para o helper
        agendarAlarmeSaidaComAction(
            context = context,
            time = horarioSaida.timeInMillis,
            id = (id * 100) + 99,
            title = "HORA DE SAIR: ${compromisso.titulo}",
            msg = msgFinal,
            actionIntent = confirmPending,
            type = compromisso.notificationType,
            items = itemsList // <-- Passando a lista aqui
        )

        // Alarme de Verificação de Atraso (10 min depois - Backup)
        val checkIntent = Intent(context, NotificationActionReceiver::class.java).apply { action = NotificationActionReceiver.Actions.CHECK_LATE.toString(); putExtra("EXTRA_COMPROMISSO_ID", compromisso.id) }
        val checkPending = PendingIntent.getBroadcast(context, id + 9999, checkIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val timeCheck = (horarioSaida.clone() as Calendar).apply { add(Calendar.MINUTE, 10) }.timeInMillis
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && am.canScheduleExactAlarms()) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeCheck, checkPending) else am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeCheck, checkPending)
    }

    private fun agendarAlarmeSaidaComAction(
        context: Context,
        time: Long,
        id: Int,
        title: String,
        msg: String,
        actionIntent: PendingIntent,
        type: NotificationType,
        items: ArrayList<String> = arrayListOf() // Novo parâmetro
    ) {
        val intent = Intent(context, com.brunocsantos.agoravai.notifications.AlarmReceiver::class.java).apply {
            putExtra("EXTRA_TITLE", title)
            putExtra("EXTRA_MESSAGE", msg)
            putExtra("EXTRA_NOTIFICATION_ID", id)
            putExtra("EXTRA_HAS_ACTION", true)
            putExtra("EXTRA_ACTION_INTENT", actionIntent)
            putExtra("EXTRA_ACTION_TITLE", "Já Saí")
            putExtra(EXTRA_NOTIFICATION_TYPE, type.ordinal)
            putStringArrayListExtra("EXTRA_ITEMS", items) // <-- Anexando a lista
            putExtra("EXTRA_COMPROMISSO_ID", (id / 100).toLong()) // Passa ID do compromisso para a Activity confirmar
        }
        val pending = PendingIntent.getBroadcast(context, id, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && am.canScheduleExactAlarms()) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, pending) else am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, pending)
    }

    private fun cancelarTodosAlarmes(context: Context, id: Int, numEtapas: Int) {
        for (i in 0 until (numEtapas + 5)) NotificationScheduler.cancelAlarm(context, (id * 100) + i)
        NotificationScheduler.cancelAlarm(context, (id * 100) + 99)
        val checkIntent = Intent(context, NotificationActionReceiver::class.java); val p = PendingIntent.getBroadcast(context, id + 9999, checkIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT); (context.getSystemService(Context.ALARM_SERVICE) as AlarmManager).cancel(p)
    }

    @SuppressLint("MissingPermission")
    fun fetchCurrentLocationFor(field: AddressField) {
        if (ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) { _addCompromissoUiState.update { it.copy(saveError = "Permissão negada.") }; return }
        viewModelScope.launch { try { val loc = LocationServices.getFusedLocationProviderClient(applicationContext).getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, CancellationTokenSource().token).await() ?: LocationServices.getFusedLocationProviderClient(applicationContext).lastLocation.await()
            if (loc != null) { val geo = Geocoder(applicationContext, Locale.getDefault()); val fb = "${loc.latitude}, ${loc.longitude}"; if (Build.VERSION.SDK_INT >= 33) geo.getFromLocation(loc.latitude, loc.longitude, 1) { l -> updateAddressField(field, if(l.isNotEmpty()) l[0].getAddressLine(0)?:fb else fb) } else { val l = geo.getFromLocation(loc.latitude, loc.longitude, 1); updateAddressField(field, if(!l.isNullOrEmpty()) l[0].getAddressLine(0)?:fb else fb) }
            } else _addCompromissoUiState.update { it.copy(saveError = "Localização não encontrada.") }
        } catch (e: Exception) { _addCompromissoUiState.update { it.copy(saveError = "Erro local.") } }
        }
    }
    private fun updateAddressField(f: AddressField, a: String) { if(a.isBlank()) return; _addCompromissoUiState.update { s -> when (f) { AddressField.ORIGIN -> s.copy(origem = a); AddressField.DESTINATION -> s.copy(destino = a) } } }
    fun onDateSelected(dateMillis: Long) { val u = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = dateMillis }; val l = _addCompromissoUiState.value.calendar.clone() as Calendar; l.set(u.get(Calendar.YEAR), u.get(Calendar.MONTH), u.get(Calendar.DAY_OF_MONTH)); _addCompromissoUiState.update { it.copy(calendar = l) } }
    fun onTimeSelected(h: Int, m: Int) { val c = _addCompromissoUiState.value.calendar.clone() as Calendar; c.set(Calendar.HOUR_OF_DAY, h); c.set(Calendar.MINUTE, m); _addCompromissoUiState.update { it.copy(calendar = c) } }
    fun dismissError() { _addCompromissoUiState.update { it.copy(saveError = null) } }
}