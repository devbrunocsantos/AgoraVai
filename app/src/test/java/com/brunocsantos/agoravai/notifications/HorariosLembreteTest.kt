package com.brunocsantos.agoravai.notifications

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneOffset

class HorariosLembreteTest {

    private fun hora(h: Int, m: Int): Long =
        LocalDateTime.of(2026, 10, 26, h, m).toInstant(ZoneOffset.UTC).toEpochMilli()

    // LEMB-01.a: saída às 9:00, etapas [Banho 15, Café 10] → Banho às 8:35, Café às 8:50
    @Test
    fun `LEMB-01a etapas encadeadas de tras para frente a partir da saida`() {
        val horarios = calcularHorarios(
            dataHoraEvento = hora(10, 0),
            deslocamentoMinutos = 45,
            segurancaMinutos = 15,
            duracoesEtapasMinutos = listOf(15, 10)
        )

        assertEquals(hora(9, 0), horarios.saida)
        assertEquals(listOf(hora(8, 35), hora(8, 50)), horarios.inicioEtapas)
        assertEquals(hora(8, 35), horarios.inicioPreparo)
    }

    // LEMB-08: aviso de atraso 10 min após o horário de saída
    @Test
    fun `LEMB-08 verificacao de atraso dez minutos apos a saida`() {
        val horarios = calcularHorarios(hora(10, 0), 30, 15, emptyList())

        assertEquals(hora(9, 15), horarios.saida)
        assertEquals(hora(9, 25), horarios.verificacaoAtraso)
    }

    // COMP-07.a: evento 10:00, 30 de deslocamento, 15 de segurança, 20 de etapas → preparo às 8:55
    @Test
    fun `COMP-07a inicio do preparo soma deslocamento seguranca e etapas`() {
        val horarios = calcularHorarios(hora(10, 0), 30, 15, listOf(20))

        assertEquals(hora(8, 55), horarios.inicioPreparo)
        val atrasoAs9 = (hora(9, 0) - horarios.inicioPreparo) / 60_000
        assertEquals(5L, atrasoAs9)
    }

    @Test
    fun `sem etapas o inicio do preparo e a propria saida`() {
        val horarios = calcularHorarios(hora(10, 0), 30, 15, emptyList())

        assertEquals(emptyList<Long>(), horarios.inicioEtapas)
        assertEquals(horarios.saida, horarios.inicioPreparo)
    }
}
