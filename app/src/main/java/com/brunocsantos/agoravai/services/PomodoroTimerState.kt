package com.brunocsantos.agoravai.services

import android.content.Context
import com.brunocsantos.agoravai.ui.pomodoro.PomodoroState
import com.brunocsantos.agoravai.ui.pomodoro.TimerState
import com.brunocsantos.agoravai.utils.SettingsManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object PomodoroTimerState {

    // CORREÇÃO: Usando a constante DEFAULT_FOCUS_MINS e convertendo para Long
    private val _tempoRestante = MutableStateFlow(SettingsManager.DEFAULT_FOCUS_MINS * 60 * 1000L)
    val tempoRestante = _tempoRestante.asStateFlow()

    private val _estadoPomodoro = MutableStateFlow(PomodoroState.PARADO)
    val estadoPomodoro = _estadoPomodoro.asStateFlow()

    private val _estadoTimer = MutableStateFlow(TimerState.PARADO)
    val estadoTimer = _estadoTimer.asStateFlow()

    fun setTempo(tempo: Long) { _tempoRestante.value = tempo }
    fun setEstadoPomodoro(estado: PomodoroState) { _estadoPomodoro.value = estado }
    fun setEstadoTimer(estado: TimerState) { _estadoTimer.value = estado }

    /**
     * NOVA FUNÇÃO: Reseta o timer para o valor de foco salvo nas configurações.
     * Usada para atualizar a UI imediatamente após salvar as configurações.
     */
    fun resetToDefaults(context: Context) {
        if (_estadoTimer.value == TimerState.PARADO) {
            // CORREÇÃO: Usando a nova função getFocusDuration e convertendo para Long
            val duracaoFocoSalva = SettingsManager.getFocusDuration(context) * 60 * 1000L
            _tempoRestante.value = duracaoFocoSalva
        }
    }
}