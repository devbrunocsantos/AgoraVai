package com.brunocsantos.agoravai.notifications

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneOffset

class JanelaChegadaTest {

    private fun em(dia: Int, h: Int, m: Int): Long =
        LocalDateTime.of(2026, 10, dia, h, m).toInstant(ZoneOffset.UTC).toEpochMilli()

    // Compromisso dia 27 às 10:00, saída às 9:15 e uma etapa de 45 min → preparo começa às 8:30
    private val evento = em(27, 10, 0)
    private val horarios = calcularHorarios(evento, 30, 15, listOf(45))

    @Test
    fun `LEMB-09a entrada durante o periodo conta como chegada`() {
        assertEquals(em(27, 8, 30), horarios.inicioPreparo)
        assertTrue(estaNaJanelaDeChegada(em(27, 9, 40), horarios, evento))
    }

    @Test
    fun `LEMB-09b chegar antes da saida tambem conta`() {
        assertTrue(estaNaJanelaDeChegada(em(27, 9, 10), horarios, evento))
    }

    @Test
    fun `LEMB-09c entrada no dia anterior e ignorada`() {
        assertFalse(estaNaJanelaDeChegada(em(26, 9, 40), horarios, evento))
    }

    @Test
    fun `LEMB-09 entrada antes do inicio do preparo e ignorada`() {
        assertFalse(estaNaJanelaDeChegada(em(27, 8, 29), horarios, evento))
    }

    @Test
    fun `LEMB-09 limites do periodo inicio do preparo e duas horas apos o evento`() {
        assertTrue(estaNaJanelaDeChegada(em(27, 8, 30), horarios, evento))
        assertEquals(em(27, 12, 0), fimDaJanelaDeChegada(evento))
        assertTrue(estaNaJanelaDeChegada(em(27, 12, 0), horarios, evento))
        assertFalse(estaNaJanelaDeChegada(em(27, 12, 1), horarios, evento))
    }
}
