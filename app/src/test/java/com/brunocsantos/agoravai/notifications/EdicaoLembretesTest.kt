package com.brunocsantos.agoravai.notifications

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneOffset

/** Spec 005, História 1 (LEMB-12): lembretes durante a edição. */
class EdicaoLembretesTest {

    private fun em(dia: Int, h: Int, m: Int): Long =
        LocalDateTime.of(2026, 10, dia, h, m).toInstant(ZoneOffset.UTC).toEpochMilli()

    // Cenário do CA-1.1: compromisso às 10:00, sem etapas, saída às 9:15 e atraso às 9:25
    private val evento = em(30, 10, 0)
    private val id = 7L

    private fun horarios(
        etapas: List<Long> = emptyList(),
        saida: Long = em(30, 9, 15),
        atraso: Long = em(30, 9, 25),
        inicioPreparo: Long = etapas.firstOrNull() ?: saida
    ) = HorariosLembrete(etapas, saida, atraso, inicioPreparo)

    // Liberar usa a regra da 003: plano com reinicio = true
    private fun planoAoLiberar(
        horarios: HorariosLembrete = horarios(),
        agora: Long,
        dataHoraEvento: Long = evento,
        confirmouSaida: Boolean = false,
        confirmouChegada: Boolean = false
    ) = planoDeReagendamento(
        horarios, dataHoraEvento, agora, ativo = true,
        confirmouSaida = confirmouSaida, confirmouChegada = confirmouChegada,
        temCoordenadas = true, reinicio = true
    )

    // CA-1.1 (LEMB-12.a): formulário do próprio compromisso aberto e visível → retém
    @Test
    fun `CA-1_1 lembrete do compromisso em edicao visivel e retido`() {
        assertTrue(deveReter(id, idEmEdicao = id, formularioVisivel = true))
    }

    // CA-1.2 (LEMB-12.b): sai sem salvar às 9:17 → "HORA DE SAIR" toca; atraso segue às 9:25
    @Test
    fun `CA-1_2 sair sem salvar depois da saida toca a saida e mantem o atraso`() {
        val p = planoAoLiberar(agora = em(30, 9, 17))

        assertTrue(p.saidaPerdida)
        assertEquals(em(30, 9, 25), p.atraso)
        assertTrue(deveTocarSaidaAoLiberar(houveRetencao = true, plano = p))
    }

    // CA-1.4 (LEMB-12.d): etapa às 9:00 e saída às 9:15; sai às 9:20 → só a saída, etapa descartada
    @Test
    fun `CA-1_4 etapa retida nao toca e a saida toca`() {
        val p = planoAoLiberar(horarios(etapas = listOf(em(30, 9, 0))), agora = em(30, 9, 20))

        assertTrue(p.etapas.isEmpty())
        assertTrue(p.saidaPerdida)
        assertTrue(deveTocarSaidaAoLiberar(true, p))
    }

    // CA-1.5 (LEMB-12.e): lembrete de outro compromisso, ou edição de um compromisso novo, não retém
    @Test
    fun `CA-1_5 outro compromisso ou criacao nao retem`() {
        assertFalse(deveReter(compromissoId = 8L, idEmEdicao = id, formularioVisivel = true))
        assertFalse(deveReter(compromissoId = 8L, idEmEdicao = null, formularioVisivel = true))
    }

    // CA-1.6 (LEMB-12.f): a recusa ao salvar não muda o plano; sai às 9:16 → a saída toca
    @Test
    fun `CA-1_6 sair depois de salvar recusado toca a saida`() {
        val p = planoAoLiberar(agora = em(30, 9, 16))

        assertTrue(deveTocarSaidaAoLiberar(true, p))
    }

    // CA-1.8 (LEMB-12.h): "Já Saí" tocado durante a edição → nada toca ao sair às 9:26
    @Test
    fun `CA-1_8 saida confirmada durante a edicao nao toca nada ao sair`() {
        val p = planoAoLiberar(agora = em(30, 9, 26), confirmouSaida = true)

        assertNull(p.saida)
        assertNull(p.atraso)
        assertFalse(deveTocarSaidaAoLiberar(true, p))
    }

    // CA-1.9 (LEMB-12.i): formulário fora da tela (tela apagada, outro app) → não retém
    @Test
    fun `CA-1_9 formulario fora da tela nao retem`() {
        assertFalse(deveReter(id, idEmEdicao = id, formularioVisivel = false))
    }

    // CA-1.10 (LEMB-12.j): atraso das 9:25 retido; sai às 9:27 → atraso descartado, saída toca de novo
    @Test
    fun `CA-1_10 atraso retido e descartado e a saida toca de novo`() {
        val p = planoAoLiberar(agora = em(30, 9, 27))

        assertNull(p.atraso)
        assertTrue(deveTocarSaidaAoLiberar(true, p))
    }

    // CA-1.11 (LEMB-12.k): chegada registrada durante a edição → ao sair nada toca
    @Test
    fun `CA-1_11 chegada durante a edicao nao toca nada ao sair`() {
        val p = planoAoLiberar(agora = em(30, 9, 22), confirmouChegada = true)

        assertTrue(p.vazio)
        assertFalse(deveTocarSaidaAoLiberar(true, p))
    }

    // CA-1.12 (LEMB-12.l): fica até 9:27 → toca "HORA DE SAIR", não o atraso das 9:25
    @Test
    fun `CA-1_12 atraso passado nao toca e a saida toca`() {
        val p = planoAoLiberar(agora = em(30, 9, 27))

        assertNull(p.atraso)
        assertTrue(p.saidaPerdida)
        assertTrue(deveTocarSaidaAoLiberar(true, p))
    }

    // CA-1.13 (LEMB-12.m): evento 10:00, saída 9:55; sai às 10:02 → saída não toca, atraso às 10:05
    @Test
    fun `CA-1_13 depois do evento a saida nao toca e o atraso futuro segue`() {
        val h = horarios(saida = em(30, 9, 55), atraso = em(30, 10, 5))
        val p = planoAoLiberar(h, agora = em(30, 10, 2))

        assertNull(p.saida)
        assertFalse(p.saidaPerdida)
        assertEquals(em(30, 10, 5), p.atraso)
        assertFalse(deveTocarSaidaAoLiberar(true, p))
    }

    // RF-003 / §9: sem retenção, liberar não repete a notificação (decisão do plano)
    @Test
    fun `RF-003 sem retencao liberar nao toca`() {
        val p = planoAoLiberar(agora = em(30, 9, 20))

        assertTrue(p.saidaPerdida)
        assertFalse(deveTocarSaidaAoLiberar(houveRetencao = false, plano = p))
    }

    // RF-003: sair exatamente no horário da saída conta como saída perdida
    @Test
    fun `RF-003 sair exatamente no horario da saida toca`() {
        val p = planoAoLiberar(agora = em(30, 9, 15))

        assertTrue(deveTocarSaidaAoLiberar(true, p))
    }
}
