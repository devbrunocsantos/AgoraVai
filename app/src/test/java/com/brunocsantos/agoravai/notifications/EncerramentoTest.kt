package com.brunocsantos.agoravai.notifications

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneOffset

/** Spec 005, História 2 (COMP-10): compromisso encerrado e religar. */
class EncerramentoTest {

    private fun em(dia: Int, h: Int, m: Int): Long =
        LocalDateTime.of(2026, 10, dia, h, m).toInstant(ZoneOffset.UTC).toEpochMilli()

    private val evento = em(30, 10, 0)

    private fun horarios(
        etapas: List<Long> = emptyList(),
        saida: Long = em(30, 9, 15),
        atraso: Long = em(30, 9, 25),
        inicioPreparo: Long = etapas.firstOrNull() ?: saida
    ) = HorariosLembrete(etapas, saida, atraso, inicioPreparo)

    private fun plano(
        horarios: HorariosLembrete,
        agora: Long,
        confirmouSaida: Boolean = false,
        reinicio: Boolean = true
    ) = planoDeReagendamento(
        horarios, evento, agora, ativo = true,
        confirmouSaida = confirmouSaida, confirmouChegada = false,
        temCoordenadas = true, reinicio = reinicio
    )

    // CA-2.1 (COMP-10.b): sem repetição, encerra exatamente no horário do evento
    @Test
    fun `CA-2_1 sem repeticao encerra no horario do evento`() {
        assertFalse(estaEncerrado("", evento, agora = evento - 1))
        assertTrue(estaEncerrado("", evento, agora = evento))
        assertTrue(estaEncerrado("", evento, agora = em(30, 10, 1)))
    }

    // CA-2.1 (COMP-10.b): repetição em branco conta como sem repetição
    @Test
    fun `CA-2_1 repeticao em branco conta como sem repeticao`() {
        assertTrue(estaEncerrado(" ", evento, agora = em(30, 10, 1)))
    }

    // CA-2.2 (COMP-10.c): ligar um encerrado é bloqueado
    @Test
    fun `CA-2_2 ligar compromisso encerrado e bloqueado`() {
        assertEquals(DecisaoAoLigar.BLOQUEAR_PASSADO, decisaoAoLigar(encerrado = true, podeAgendarExato = true))
    }

    // CA-2.3 (COMP-10.d): editado para amanhã, deixa de estar encerrado
    @Test
    fun `CA-2_3 evento amanha nao esta encerrado`() {
        assertFalse(estaEncerrado("", em(31, 10, 0), agora = em(30, 10, 5)))
    }

    // CA-2.4 (COMP-10.e): com dias de repetição, nunca encerra e religar não é bloqueado
    @Test
    fun `CA-2_4 com repeticao nao encerra e ligar e permitido`() {
        val encerrado = estaEncerrado("2,4", evento, agora = em(30, 11, 0))

        assertFalse(encerrado)
        assertEquals(DecisaoAoLigar.LIGAR, decisaoAoLigar(encerrado, podeAgendarExato = true))
    }

    // CA-2.7 (COMP-10.f): encerrado continua ativo; às 10:02 o atraso das 10:05 e a chegada seguem
    @Test
    fun `CA-2_7 encerrado mantem atraso futuro e chegada`() {
        val h = horarios(saida = em(30, 9, 55), atraso = em(30, 10, 5))
        val p = plano(h, agora = em(30, 10, 2))

        assertEquals(em(30, 10, 5), p.atraso)
        assertFalse(p.saidaPerdida)
        assertEquals(em(30, 10, 2), p.ativacaoChegada)
    }

    // CA-2.8 (COMP-10.g): chegada monitorada até 2 h depois do evento
    @Test
    fun `CA-2_8 chegada monitorada ate duas horas depois do evento`() {
        val h = horarios(saida = em(30, 9, 55), atraso = em(30, 10, 5))

        assertNotNull(plano(h, agora = em(30, 10, 30)).ativacaoChegada)
        assertNull(plano(h, agora = em(30, 12, 0)).ativacaoChegada)
    }

    // CA-2.9 (COMP-10.h): etapa 9:00, saída 9:15; religa às 9:20 → só a saída, atraso às 9:25
    @Test
    fun `CA-2_9 religar depois da saida toca so a saida`() {
        val p = plano(horarios(etapas = listOf(em(30, 9, 0))), agora = em(30, 9, 20))

        assertTrue(p.etapas.isEmpty())
        assertTrue(p.saidaPerdida)
        assertEquals(em(30, 9, 20), p.saida)
        assertEquals(em(30, 9, 25), p.atraso)
    }

    // CA-2.10 (COMP-10.i): encerrado e sem alarme exato → a mensagem é a de passado, não a de permissão
    @Test
    fun `CA-2_10 passado vem antes da permissao`() {
        assertEquals(DecisaoAoLigar.BLOQUEAR_PASSADO, decisaoAoLigar(encerrado = true, podeAgendarExato = false))
    }

    // CA-2.11 (COMP-10.j): etapa 9:00, saída 9:15; religa às 9:05 → nada na hora, saída às 9:15
    @Test
    fun `CA-2_11 religar entre a etapa e a saida nao toca nada na hora`() {
        val p = plano(horarios(etapas = listOf(em(30, 9, 0))), agora = em(30, 9, 5))

        assertTrue(p.etapas.isEmpty())
        assertFalse(p.saidaPerdida)
        assertEquals(em(30, 9, 15), p.saida)
    }

    // LEMB-10 (regressão): evento futuro sem alarme exato continua bloqueado pela permissão
    @Test
    fun `LEMB-10 evento futuro sem permissao continua bloqueado`() {
        assertEquals(DecisaoAoLigar.BLOQUEAR_PERMISSAO, decisaoAoLigar(encerrado = false, podeAgendarExato = false))
    }

    // COMP-10 (D3): religar com a saída já confirmada só monitora a chegada
    @Test
    fun `COMP-10 religar com saida confirmada so monitora a chegada`() {
        val p = plano(horarios(), agora = em(30, 9, 20), confirmouSaida = true)

        assertNull(p.saida)
        assertNull(p.atraso)
        assertNotNull(p.ativacaoChegada)
    }
}
