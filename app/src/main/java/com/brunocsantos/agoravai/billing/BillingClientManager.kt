package com.brunocsantos.agoravai.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * VERSÃO "BYPASS" PARA TESTES
 * O Billing real foi desativado. O app agirá como se o usuário fosse Premium.
 */
class BillingClientManager(private val context: Context) : PurchasesUpdatedListener {

    // Define como TRUE para liberar as funcionalidades de automação nos testes
    private val _isPremium = MutableStateFlow(true)
    val isPremium = _isPremium.asStateFlow()

    private val _products = MutableStateFlow<List<ProductDetails>>(emptyList())
    val products = _products.asStateFlow()

    init {
        // startConnection() -> COMENTADO PARA NÃO CONECTAR
        Log.w("BillingManager", "⚠️ MODO DE TESTE: Billing Desativado. Premium Forçado = TRUE.")
    }

    // Métodos mantidos vazios para compatibilidade, apenas logando a chamada

    fun queryProducts() {
        Log.d("BillingManager", "queryProducts chamado (Ignorado no modo teste)")
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: MutableList<Purchase>?) {
        // Callback ignorado
    }

    fun launchPurchaseFlow(activity: Activity) {
        Log.d("BillingManager", "launchPurchaseFlow chamado. Compra simulada (Nada acontece visualmente).")
        // Opcional: Você pode forçar true aqui também caso queira simular o sucesso de um clique
        _isPremium.value = true
    }

    fun queryPurchases() {
        Log.d("BillingManager", "queryPurchases chamado. Reforçando Premium = true.")
        _isPremium.value = true
    }
}