package com.brunocsantos.agoravai.utils

import android.content.Context
import java.time.LocalDate
import kotlin.random.Random

object FraseMotivacionalManager {

    private val frases = listOf(
        "...ser diferente.",
        "...dar certo.",
        "...ser a minha vez.",
        "...acontecer de verdade.",
        "...ser a hora da virada.",
        "...fluir com mais leveza.",
        "...ser um capítulo de sucesso.",
        "...valer a pena todo o esforço.",
        "...ser o início de algo incrível.",
        "...ser a minha melhor versão.",
        "...ser a hora de colher os frutos.",
        "...ser o momento de brilhar.",
        "...funcionar, porque eu farei funcionar.",
        "...ser a minha prioridade.",
        "...ter o meu foco total.",
        "...ser um caminho de realizações.",
        "...ser a construção de um sonho.",
        "...ser o fim da procrastinação.",
        "...transformar o plano em realidade.",
        "...ser a prova do meu potencial.",
        "...ser leve, focado e constante.",
        "...deslanchar de vez.",
        "...ser o movimento que gera a mudança.",
        "...ser a escalada até o topo.",
        "...ser o meu tempo de florescer.",
        "...ser a mudança que eu quero ver.",
        "...ter consistência e disciplina.",
        "...ser o resultado de cada pequeno passo.",
        "...silenciar o medo com a ação.",
        "...construir o futuro que eu desenhei.",
        "...ser a minha vez de vencer.",
        "...ser a prova de que eu sou capaz.",
        "...ter mais ação e menos distração.",
        "...transformar cada obstáculo em degrau.",
        "...deixar de ser promessa e virar realidade.",
        "...ser o fim da espera e o começo da ação.",
        "...trazer o resultado que eu mereço.",
        "...fazer acontecer, e não apenas esperar.",
        "...ser o meu momento de tomar o controle.",
        "...ser o fim do quase e o início do feito.",
        "...ser a minha vitória diária.",
        "...ser o meu auge de produtividade."
    )

    fun getFraseDoDia(context: Context): String {
        val prefs = context.getSharedPreferences("motivacional_prefs", Context.MODE_PRIVATE)
        val hoje = LocalDate.now().toString()

        val dataSalva = prefs.getString("data_frase", null)
        val fraseSalva = prefs.getString("frase_do_dia", null)

        // Se a data salva for a mesma de hoje e já tivermos uma frase, retorne-a.
        if (hoje == dataSalva && fraseSalva != null) {
            return fraseSalva
        }

        // Se for um novo dia ou a primeira execução, escolha uma nova frase.
        // Garante que a nova frase seja diferente da última que foi salva.
        val novaFrase = frases.filter { it != fraseSalva }.random()

        // Salva a nova frase e a data de hoje.
        prefs.edit()
            .putString("data_frase", hoje)
            .putString("frase_do_dia", novaFrase)
            .apply()

        return novaFrase
    }
}