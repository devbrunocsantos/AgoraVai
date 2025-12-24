package com.brunocsantos.agoravai.services

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Singleton para comunicar a conclusão de uma sessão de Pomodoro
 * entre o Service e a UI (para exibir a tela de parabéns).
 */
object PomodoroCompletionState {

    private val _sessionCompletedEvent = MutableStateFlow<Long?>(null)
    val sessionCompletedEvent = _sessionCompletedEvent.asStateFlow()

    /**
     * Chamado pelo PomodoroService para sinalizar que uma sessão foi concluída.
     */
    fun onSessionCompleted() {
        _sessionCompletedEvent.value = System.currentTimeMillis()
    }

    /**
     * Chamado pela UI após a tela de parabéns ter sido exibida, para resetar o evento.
     */
    fun onEventConsumed() {
        _sessionCompletedEvent.value = null
    }
}