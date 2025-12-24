package com.brunocsantos.agoravai.services

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Singleton para monitorar se o aplicativo está em primeiro plano (visível para o usuário).
 */
object AppForegroundState {
    private val _isAppInForeground = MutableStateFlow(false)
    val isAppInForeground = _isAppInForeground.asStateFlow()

    fun setAppInForeground(isInForeground: Boolean) {
        _isAppInForeground.value = isInForeground
    }
}