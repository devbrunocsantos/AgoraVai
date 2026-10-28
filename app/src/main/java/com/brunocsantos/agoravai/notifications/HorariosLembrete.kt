package com.brunocsantos.agoravai.notifications

private const val MINUTO_MS = 60_000L

// Minutos após a saída em que o aviso de atraso é verificado (LEMB-08)
const val MINUTOS_VERIFICACAO_ATRASO = 10

/**
 * Horários (epoch ms) de todos os lembretes de um compromisso.
 * Cálculo puro, sem dependência de Android, para poder ser testado.
 */
data class HorariosLembrete(
    val inicioEtapas: List<Long>, // Na mesma ordem das etapas definida pelo usuário
    val saida: Long,
    val verificacaoAtraso: Long,
    val inicioPreparo: Long // Início da primeira etapa, ou a própria saída se não houver etapas
)

fun calcularHorarios(
    dataHoraEvento: Long,
    deslocamentoMinutos: Int,
    segurancaMinutos: Int,
    duracoesEtapasMinutos: List<Int>
): HorariosLembrete {
    val saida = dataHoraEvento - (deslocamentoMinutos + segurancaMinutos) * MINUTO_MS

    // As etapas são encadeadas de trás para frente a partir do horário de saída
    val inicioEtapas = MutableList(duracoesEtapasMinutos.size) { 0L }
    var proximoHorario = saida
    for (i in duracoesEtapasMinutos.indices.reversed()) {
        proximoHorario -= duracoesEtapasMinutos[i] * MINUTO_MS
        inicioEtapas[i] = proximoHorario
    }

    return HorariosLembrete(
        inicioEtapas = inicioEtapas,
        saida = saida,
        verificacaoAtraso = saida + MINUTOS_VERIFICACAO_ATRASO * MINUTO_MS,
        inicioPreparo = proximoHorario
    )
}

// LEMB-09: a chegada só conta do início do preparo até este tempo depois do evento
const val HORAS_APOS_EVENTO_JANELA_CHEGADA = 2

fun fimDaJanelaDeChegada(dataHoraEvento: Long): Long =
    dataHoraEvento + HORAS_APOS_EVENTO_JANELA_CHEGADA * 60 * MINUTO_MS

fun estaNaJanelaDeChegada(agora: Long, horarios: HorariosLembrete, dataHoraEvento: Long): Boolean =
    agora >= horarios.inicioPreparo && agora <= fimDaJanelaDeChegada(dataHoraEvento)

// LEMB-09: o monitoramento da chegada começa no início do período. Se o usuário já
// estiver no destino nesse momento, a entrada inicial já conta como chegada
fun inicioMonitoramentoChegada(horarios: HorariosLembrete): Long = horarios.inicioPreparo
