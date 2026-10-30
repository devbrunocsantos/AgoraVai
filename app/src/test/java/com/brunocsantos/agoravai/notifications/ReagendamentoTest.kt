package com.brunocsantos.agoravai.notifications

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneOffset

class ReagendamentoTest {

    private fun em(dia: Int, h: Int, m: Int): Long =
        LocalDateTime.of(2026, 10, dia, h, m).toInstant(ZoneOffset.UTC).toEpochMilli()

    // Compromisso dia 27 às 10:00, saída às 9:15 e atraso às 9:25 (números dos CAs da spec 003)
    private val evento = em(27, 10, 0)

    private fun horarios(
        etapas: List<Long> = emptyList(),
        saida: Long = em(27, 9, 15),
        atraso: Long = em(27, 9, 25),
        inicioPreparo: Long = etapas.firstOrNull() ?: saida
    ) = HorariosLembrete(etapas, saida, atraso, inicioPreparo)

    private fun plano(
        horarios: HorariosLembrete,
        agora: Long,
        dataHoraEvento: Long = evento,
        ativo: Boolean = true,
        confirmouSaida: Boolean = false,
        confirmouChegada: Boolean = false,
        temCoordenadas: Boolean = true,
        reinicio: Boolean = true
    ) = planoDeReagendamento(
        horarios, dataHoraEvento, agora, ativo, confirmouSaida, confirmouChegada, temCoordenadas, reinicio
    )

    // LEMB-11.a: reiniciar na véspera mantém todos os lembretes nos horários originais
    @Test
    fun `LEMB-11a reinicio na vespera reagenda etapas saida atraso e ativacao`() {
        val h = horarios(etapas = listOf(em(27, 8, 40), em(27, 9, 0)))
        val p = plano(h, agora = em(26, 20, 0))

        assertEquals(listOf(0 to em(27, 8, 40), 1 to em(27, 9, 0)), p.etapas)
        assertEquals(em(27, 9, 15), p.saida)
        assertFalse(p.saidaPerdida)
        assertEquals(em(27, 9, 25), p.atraso)
        assertEquals(em(27, 8, 40), p.ativacaoChegada)
    }

    // LEMB-11.b: etapas às 8:00 e 8:20, aparelho volta às 8:30
    @Test
    fun `LEMB-11b etapas passadas descartadas saida e atraso mantidos`() {
        val h = horarios(etapas = listOf(em(27, 8, 0), em(27, 8, 20)))
        val p = plano(h, agora = em(27, 8, 30))

        assertTrue(p.etapas.isEmpty())
        assertEquals(em(27, 9, 15), p.saida)
        assertEquals(em(27, 9, 25), p.atraso)
    }

    @Test
    fun `LEMB-11b etapa exatamente em agora e descartada`() {
        val h = horarios(etapas = listOf(em(27, 8, 0), em(27, 8, 20)))
        val p = plano(h, agora = em(27, 8, 20))

        assertTrue(p.etapas.isEmpty())
    }

    @Test
    fun `LEMB-11b atraso exatamente em agora e descartado`() {
        val p = plano(horarios(), agora = em(27, 9, 25))

        assertNull(p.atraso)
    }

    // LEMB-11.c
    @Test
    fun `LEMB-11c compromisso desativado nada e agendado`() {
        val p = plano(horarios(), agora = em(26, 20, 0), ativo = false)

        assertTrue(p.vazio)
    }

    // LEMB-11.f: desligado das 9:00 às 9:20, saída 9:15 → dispara agora; atraso segue às 9:25
    @Test
    fun `LEMB-11f saida perdida antes do evento dispara agora e atraso segue em 925`() {
        val agora = em(27, 9, 20)
        val p = plano(horarios(), agora = agora)

        assertEquals(agora, p.saida)
        assertTrue(p.saidaPerdida)
        assertEquals(em(27, 9, 25), p.atraso)
    }

    @Test
    fun `LEMB-11f saida exatamente em agora dispara imediata`() {
        val agora = em(27, 9, 15)
        val p = plano(horarios(), agora = agora)

        assertEquals(agora, p.saida)
        assertTrue(p.saidaPerdida)
    }

    @Test
    fun `LEMB-11f sem etapas saida perdida dispara saida e ativacao em agora`() {
        val agora = em(27, 9, 20)
        val p = plano(horarios(), agora = agora)

        assertEquals(agora, p.saida)
        assertEquals(agora, p.ativacaoChegada)
    }

    @Test
    fun `LEMB-11f saida confirmada nao agenda etapas saida nem atraso`() {
        val h = horarios(etapas = listOf(em(27, 9, 0)))
        val p = plano(h, agora = em(27, 8, 30), confirmouSaida = true)

        assertTrue(p.etapas.isEmpty())
        assertNull(p.saida)
        assertNull(p.atraso)
        assertEquals(em(27, 9, 0), p.ativacaoChegada)
    }

    @Test
    fun `LEMB-11f na atualizacao saida passada nao dispara`() {
        val p = plano(horarios(), agora = em(27, 9, 20), reinicio = false)

        assertNull(p.saida)
        assertFalse(p.saidaPerdida)
        assertEquals(em(27, 9, 25), p.atraso)
    }

    // LEMB-11.g: aparelho volta às 10:05, depois do evento
    @Test
    fun `LEMB-11g evento ja passou nao dispara etapas saida nem atraso`() {
        val agora = em(27, 10, 5)
        val h = horarios(etapas = listOf(em(27, 8, 40)))
        val p = plano(h, agora = agora)

        assertTrue(p.etapas.isEmpty())
        assertNull(p.saida)
        assertFalse(p.saidaPerdida)
        assertNull(p.atraso)
        assertEquals(agora, p.ativacaoChegada)
        assertFalse(p.vazio)
    }

    @Test
    fun `LEMB-11g evento exatamente agora nao dispara saida perdida`() {
        val p = plano(horarios(), agora = evento)

        assertNull(p.saida)
        assertFalse(p.saidaPerdida)
    }

    // LEMB-11.h: reinício às 8:00, preparo às 8:30
    @Test
    fun `LEMB-11h reinicio antes do preparo agenda ativacao no inicio do preparo`() {
        val h = horarios(etapas = listOf(em(27, 8, 30)))
        val p = plano(h, agora = em(27, 8, 0))

        assertEquals(em(27, 8, 30), p.ativacaoChegada)
    }

    // LEMB-11.i (regra): quem já chegou não recebe nada
    @Test
    fun `LEMB-11i chegada confirmada nada e agendado`() {
        val p = plano(horarios(), agora = em(27, 9, 20), confirmouChegada = true)

        assertTrue(p.vazio)
    }

    @Test
    fun `LEMB-11 sem coordenadas nao agenda ativacao mas agenda saida`() {
        val p = plano(horarios(), agora = em(26, 20, 0), temCoordenadas = false)

        assertNull(p.ativacaoChegada)
        assertEquals(em(27, 9, 15), p.saida)
    }

    @Test
    fun `LEMB-11 apos fim do periodo de chegada nao ativa chegada`() {
        val p = plano(horarios(), agora = em(27, 12, 1))

        assertNull(p.ativacaoChegada)
        assertTrue(p.vazio)
    }

    @Test
    fun `LEMB-11 exatamente no fim do periodo nao ativa chegada`() {
        val p = plano(horarios(), agora = em(27, 12, 0))

        assertNull(p.ativacaoChegada)
    }

    // Trajeto de 5 min e segurança 0: saída 9:55, atraso 10:05 (depois do evento)
    @Test
    fun `LEMB-11 atraso depois do evento ainda futuro e mantido`() {
        val h = horarios(saida = em(27, 9, 55), atraso = em(27, 10, 5))
        val p = plano(h, agora = em(27, 10, 1))

        assertEquals(em(27, 10, 5), p.atraso)
        assertNull(p.saida)
    }

    @Test
    fun `RNF-001 regra e deterministica para a mesma entrada`() {
        val h = horarios(etapas = listOf(em(27, 8, 40), em(27, 9, 0)))

        assertEquals(plano(h, agora = em(27, 8, 50)), plano(h, agora = em(27, 8, 50)))
    }

    // LEMB-11.i: a leitura de localização decide se a chegada vem antes da saída perdida
    @Test
    fun `LEMB-11i classificar posicao`() {
        assertEquals(PosicaoChegada.DENTRO, classificarPosicao(150f, 20f))
        assertEquals(PosicaoChegada.DENTRO, classificarPosicao(200f, 20f))
        assertEquals(PosicaoChegada.FORA, classificarPosicao(500f, 20f))
        assertEquals(PosicaoChegada.DESCONHECIDA, classificarPosicao(null, null))
        assertEquals(PosicaoChegada.DESCONHECIDA, classificarPosicao(150f, 300f))
    }
}
