package com.brunocsantos.agoravai.services

import com.brunocsantos.agoravai.notifications.deveReter

/**
 * LEMB-12: compromisso em edição e se o formulário está visível. Consultado pelos receivers
 * antes de mostrar etapas, saída e atraso.
 *
 * Só em memória: se o processo morrer, tudo volta a tocar normalmente (P2).
 * Receivers e UI rodam na main thread; os métodos são sincronizados para que cada
 * leitura + mudança seja atômica.
 */
object EdicaoEmAndamento {

    private var idEmEdicao: Long? = null
    private var visivel = false
    private var houveRetencao = false

    @Synchronized
    fun iniciar(compromissoId: Long) {
        idEmEdicao = compromissoId
        visivel = true
        houveRetencao = false
    }

    // RF-006: ao voltar a ficar visível, a retenção recomeça para os lembretes seguintes
    @Synchronized
    fun tornarVisivel() {
        if (idEmEdicao != null) visivel = true
    }

    /** RF-001: devolve true e registra a retenção se o lembrete deve esperar. */
    @Synchronized
    fun reter(compromissoId: Long): Boolean =
        deveReter(compromissoId, idEmEdicao, visivel).also { if (it) houveRetencao = true }

    /** RF-006: formulário deixou de estar visível. Para de reter e devolve (id, houve retenção). */
    @Synchronized
    fun suspender(): Pair<Long?, Boolean> {
        visivel = false
        return (idEmEdicao to houveRetencao).also { houveRetencao = false }
    }

    /** RF-002 / RF-003: fim da edição (salvou ou saiu). Limpa tudo e devolve (id, houve retenção). */
    @Synchronized
    fun encerrar(): Pair<Long?, Boolean> = suspender().also { idEmEdicao = null }
}
