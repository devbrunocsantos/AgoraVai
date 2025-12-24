package com.brunocsantos.agoravai.services

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Singleton para comunicar a conclusão de um ciclo de foco
 * do Service para o ViewModel, de forma reativa.
 */
object FocusSessionState {

    // Usamos um SharedFlow para emitir "eventos" sem um estado inicial.
    private val _focusSessionCompleted = MutableSharedFlow<Unit>()
    val focusSessionCompleted = _focusSessionCompleted.asSharedFlow()

    /**
     * Chamado pelo PomodoroService para sinalizar que um ciclo de foco foi concluído.
     */
    suspend fun onFocusSessionCompleted() {
        // Emitimos um evento 'Unit', que é como dizer "aconteceu!".
        _focusSessionCompleted.emit(Unit)
    }
}