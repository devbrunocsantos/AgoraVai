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

// LEMB-09: raio em volta do destino que conta como chegada
const val RAIO_CHEGADA_METROS = 200f

/**
 * LEMB-11: o que reagendar depois de reiniciar o aparelho ou atualizar o app.
 * Horários em epoch ms; `null` quando o lembrete não deve ser agendado.
 */
data class Reagendamento(
    val etapas: List<Pair<Int, Long>>, // (índice na ordem do usuário, horário), só as futuras
    val saida: Long?,                  // horário futuro, ou `agora` quando é uma saída perdida
    val saidaPerdida: Boolean,
    val atraso: Long?,
    val ativacaoChegada: Long?
) {
    val vazio: Boolean
        get() = etapas.isEmpty() && saida == null && atraso == null && ativacaoChegada == null
}

private val REAGENDAMENTO_VAZIO = Reagendamento(emptyList(), null, false, null, null)

/**
 * LEMB-11: decide quais lembretes de um compromisso ainda devem existir em `agora`.
 * Futuro é estritamente depois de `agora`. A saída perdida só dispara no reinício
 * (`reinicio = true`): na atualização do app o aparelho não esteve desligado.
 */
fun planoDeReagendamento(
    horarios: HorariosLembrete,
    dataHoraEvento: Long,
    agora: Long,
    ativo: Boolean,
    confirmouSaida: Boolean,
    confirmouChegada: Boolean,
    temCoordenadas: Boolean,
    reinicio: Boolean
): Reagendamento {
    // LEMB-11.c / LEMB-11.i: desativado ou já chegou, nada a reagendar
    if (!ativo || confirmouChegada) return REAGENDAMENTO_VAZIO

    // LEMB-09 / LEMB-11.h: a chegada é monitorada enquanto o período não terminou
    val ativacaoChegada =
        if (temCoordenadas && agora < fimDaJanelaDeChegada(dataHoraEvento)) maxOf(horarios.inicioPreparo, agora)
        else null

    // Quem já confirmou a saída não precisa de etapas, saída nem aviso de atraso
    if (confirmouSaida) return REAGENDAMENTO_VAZIO.copy(ativacaoChegada = ativacaoChegada)

    // LEMB-11.b: etapas passadas não disparam
    val etapas = horarios.inicioEtapas.withIndex()
        .filter { it.value > agora }
        .map { it.index to it.value }

    // LEMB-11.f / LEMB-11.g: saída perdida dispara na hora se o evento ainda não chegou
    val saidaPerdida = horarios.saida <= agora && reinicio && dataHoraEvento > agora
    val saida = when {
        horarios.saida > agora -> horarios.saida
        saidaPerdida -> agora
        else -> null
    }

    // LEMB-11.b: atraso passado não dispara; atraso futuro é mantido mesmo depois do evento
    val atraso = horarios.verificacaoAtraso.takeIf { it > agora }

    return Reagendamento(etapas, saida, saidaPerdida, atraso, ativacaoChegada)
}

enum class PosicaoChegada { DENTRO, FORA, DESCONHECIDA }

// LEMB-11.i: sem leitura, ou com leitura imprecisa demais, não dá para decidir
fun classificarPosicao(distanciaMetros: Float?, precisaoMetros: Float?): PosicaoChegada = when {
    distanciaMetros == null || precisaoMetros == null -> PosicaoChegada.DESCONHECIDA
    precisaoMetros > RAIO_CHEGADA_METROS -> PosicaoChegada.DESCONHECIDA
    distanciaMetros <= RAIO_CHEGADA_METROS -> PosicaoChegada.DENTRO
    else -> PosicaoChegada.FORA
}

// LEMB-12 (RF-001, RF-004, RF-006): o lembrete é retido só se for do compromisso em edição
// e o formulário estiver visível. Criação (idEmEdicao = null) nunca retém
fun deveReter(compromissoId: Long, idEmEdicao: Long?, formularioVisivel: Boolean): Boolean =
    idEmEdicao != null && compromissoId == idEmEdicao && formularioVisivel

// LEMB-12 (RF-003, RF-006): ao liberar, toca o "HORA DE SAIR" só se algo foi retido e a regra
// da 003 (plano com reinicio = true) considerar a saída perdida: saída passou, não confirmada,
// chegada não registrada e evento ainda não começou
fun deveTocarSaidaAoLiberar(houveRetencao: Boolean, plano: Reagendamento): Boolean =
    houveRetencao && plano.saidaPerdida

// COMP-10 (RF-007, RF-009): encerrado = sem dias de repetição e com o horário do evento alcançado
fun estaEncerrado(diasRepeticao: String, dataHoraEvento: Long, agora: Long): Boolean =
    diasRepeticao.isBlank() && agora >= dataHoraEvento

enum class DecisaoAoLigar { BLOQUEAR_PASSADO, BLOQUEAR_PERMISSAO, LIGAR }

// COMP-10 (RF-008): o bloqueio de compromisso passado vem antes da exigência de alarme exato (LEMB-10)
fun decisaoAoLigar(encerrado: Boolean, podeAgendarExato: Boolean): DecisaoAoLigar = when {
    encerrado -> DecisaoAoLigar.BLOQUEAR_PASSADO
    !podeAgendarExato -> DecisaoAoLigar.BLOQUEAR_PERMISSAO
    else -> DecisaoAoLigar.LIGAR
}
