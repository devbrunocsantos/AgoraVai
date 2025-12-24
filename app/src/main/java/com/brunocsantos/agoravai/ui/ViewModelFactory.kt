package com.brunocsantos.agoravai.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.brunocsantos.agoravai.billing.BillingClientManager
import com.brunocsantos.agoravai.data.AppDao
import com.brunocsantos.agoravai.ui.compromissos.CompromissoViewModel
import com.brunocsantos.agoravai.ui.pomodoro.PomodoroViewModel

class ViewModelFactory(
    private val dao: AppDao,
    private val billingClientManager: BillingClientManager,
    private val context: Context
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CompromissoViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            // A CORREÇÃO É AQUI: passamos o 'context' para o CompromissoViewModel
            return CompromissoViewModel(dao, context) as T
        }
        if (modelClass.isAssignableFrom(PomodoroViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PomodoroViewModel(dao, billingClientManager, context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}