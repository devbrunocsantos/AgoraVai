package com.brunocsantos.agoravai.billing

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object SubscriptionManager {

    private const val PREFS_NAME = "subscription_prefs"
    private const val KEY_IS_PREMIUM = "is_premium_user"

    private val _isPremium = MutableStateFlow(false)
    val isPremium = _isPremium.asStateFlow()

    /**
     * Carrega o status de assinatura salvo no dispositivo.
     * Deve ser chamada quando o aplicativo inicia.
     */
    fun initialize(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        _isPremium.value = prefs.getBoolean(KEY_IS_PREMIUM, false)
    }

    /**
     * Altera e salva o status da assinatura.
     * Por enquanto, usamos para simular uma compra.
     */
    fun setPremiumStatus(isPremium: Boolean, context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_IS_PREMIUM, isPremium).apply()
        _isPremium.value = isPremium
    }
}