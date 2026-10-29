---
id: 004
titulo: "Pomodoro: meta da sessão e progresso"
status: rascunho  # rascunho | esclarecida | pesquisada | planejada | em-implementacao | implementada | validada
criada_em: 2026-10-26
atualizada_em: 2026-10-29
responsavel: Bruno C. Santos
capacidades: POMO, AUTO
dts: DT-07, DT-08, DT-18
depende_de: "—"
---

# Spec 004: Pomodoro — meta da sessão e progresso

> **Ainda não aprovada.** Pesquisa e plano foram rascunhados antes da aprovação (2026-10-26) e precisam ser reconfirmados depois dela. Independente das specs 001–003.
> Migrada da convenção anterior (`specs/changes/2026-10-26-pomodoro-meta-e-progresso/`) em 2026-10-29.

## 1. Contexto e problema

- Uma sessão iniciada por automação ("3 ciclos") passa a durar a meta diária (8) se o usuário pausar e retomar (DT-18).
- Focos concluídos com o app fechado não contam no progresso do dia, justamente quando o timer está rodando em segundo plano (DT-08).
- Mexer em qualquer automação cancela todo o trabalho agendado do app, e excluir uma automação pode deixar o agendamento dela ativo (DT-07).

São correções de comportamento já especificado nas capacidades POMO e AUTO; o texto dos requisitos não muda.

## 2. Objetivos

- A meta escolhida vale do começo ao fim da sessão.
- Todo foco concluído conta no progresso do dia, com o app aberto ou fechado, sem contagem dupla.
- Cada automação controla só o próprio agendamento.

## 3. Não-objetivos

- Virar o progresso do dia à meia-noite com o app aberto (o "início do dia" é calculado uma vez) — follow-up.
- Histórico além de 24 h (decisão de produto).
- Restaurar o estado do timer quando o sistema encerra o serviço.

## 4. Usuários e personas

- **Pessoa que usa Pomodoro para focar**, muitas vezes com o app em segundo plano e sessões iniciadas por automação.

## 5. Histórias de usuário

### História 1 — A meta da sessão se mantém (Prioridade: P1)

Como **pessoa que iniciou uma sessão de 3 ciclos pela automação**, quero **que pausar e retomar não mude a meta** para **terminar quando planejei**.

**Teste independente:** automação de 2 ciclos, pausar e retomar.

**Critérios de aceitação:**
- **CA-1.1 (POMO-05.a)** — **Dada** uma sessão iniciada por "Iniciar" na automação de 3 ciclos com meta diária 8, **quando** pauso e retomo, **então** a sessão termina ao fim do 3º foco.
- **CA-1.2 (POMO-05.b)** — **Dada** uma sessão manual com meta diária 8, **quando** o 8º foco termina, **então** a sessão é concluída.
- **CA-1.3 (POMO-04.a)** — **Dado** 4 ciclos até a pausa longa, **quando** o 4º foco termina, **então** a fase vira `PAUSA_LONGA` (regressão).
- **CA-1.4 (POMO-04.b)** — **Dado** 4 ciclos até a pausa longa, **quando** o 1º, 2º e 3º focos terminam, **então** a fase seguinte é `PAUSA_CURTA`.
- **CA-1.5 (POMO-04.c)** — **Quando** uma pausa termina, **então** a fase seguinte é `FOCO`.

### História 2 — Todo foco conta (Prioridade: P1)

Como **pessoa que foca com o app fechado**, quero **que os focos concluídos apareçam no progresso do dia** para **acompanhar minha meta de verdade**.

**Critérios de aceitação:**
- **CA-2.1 (POMO-06.a)** — **Dado** o timer rodando com o app fechado (removido dos recentes), **quando** um foco termina e eu abro o app, **então** o progresso do dia aumentou 1.
- **CA-2.2 (POMO-06.b)** — **Dado** o app aberto, **quando** um foco termina, **então** o progresso aumenta 1, e não 2.

### História 3 — Cada automação cuida do próprio agendamento (Prioridade: P2)

Como **pessoa com várias automações**, quero **que desativar ou excluir uma não afete as outras** para **confiar nos gatilhos**.

**Critérios de aceitação:**
- **CA-3.1 (AUTO-09.a)** — **Dadas** as automações A (ativa) e B, **quando** desativo B, **então** o agendamento de A continua igual e o de B é cancelado.
- **CA-3.2 (AUTO-10.a)** — **Dada** uma automação com horário, **quando** a excluo, **então** nenhum agendamento dela continua existindo.

## 6. Requisitos funcionais

- **RF-001** (POMO-05): A meta da sessão DEVE ser a da automação que a iniciou ou, no início manual, a meta diária, e DEVE se manter durante toda a sessão, inclusive ao pausar e retomar.
- **RF-002** (POMO-04): As transições de fase DEVEM seguir a regra atual (pausa longa a cada N focos; pausa → foco).
- **RF-003** (POMO-06): O sistema DEVE gravar cada foco concluído exatamente uma vez, com o app aberto ou fechado.
- **RF-004** (AUTO-09): Ativar/desativar uma automação DEVE reagendar ou cancelar só os gatilhos dela.
- **RF-005** (AUTO-10): Excluir uma automação DEVE cancelar todos os gatilhos dela.

## 7. Requisitos não funcionais

- **RNF-001** (testabilidade): a regra de transição de fases e de meta DEVE ser verificável sem Android (P7).
- **RNF-002** (compatibilidade): agendamentos já existentes em aparelhos continuam funcionando após a atualização.

## 8. Entidades e conceitos-chave

- **Sessão**: sequência de focos e pausas até atingir a meta da sessão.
- **Meta diária**: número de focos por dia configurado pelo usuário.
- **Automação**: gatilho de horário e/ou local que oferece iniciar uma sessão com N ciclos.
- **Foco concluído**: registro usado no progresso do dia.

## 9. Casos de borda e erros

- Pausar/retomar pela notificação ou pela tela → mesma regra (RF-001).
- Excluir uma automação que tem um gatilho de local pendente → cancelado (RF-005).

## 10. Critérios de sucesso

- **CS-001**: zero sessões de automação que ultrapassam a meta escolhida.
- **CS-002**: o progresso do dia bate com o número de focos concluídos, com o app aberto ou fechado.

## 11. Premissas e dependências

- **Dependência**: nenhuma.
- **Premissa**: se esta spec for implementada antes de qualquer outra que adicione testes, ela inclui a dependência de teste (já presente desde a 001).

## 12. Questões em aberto

- (nenhuma — correções de comportamento já especificado; falta apenas a aprovação)

## 13. Esclarecimentos

<!-- Preenchido por /esclarecer -->

## 14. Delta nas capacidades (constituição §8)

| Capacidade | Mudança |
|---|---|
| POMO-04 | critérios novos .b e .c |
| POMO-05 | critérios .a e .b; ⚠️ → ✅ |
| POMO-06 | critérios .a e .b; ⚠️ → ✅ |
| AUTO-09 | critério .a; ⚠️ → ✅ |
| AUTO-10 | critério .a |

## Checklist de qualidade

- [x] Sem detalhes de implementação (linguagens, frameworks, APIs, tabelas)
- [x] Requisitos testáveis e sem ambiguidade
- [x] Critérios de sucesso mensuráveis
- [x] Histórias priorizadas e independentes
- [x] Casos de borda identificados
- [x] Escopo delimitado por não-objetivos
- [x] Nenhum [PRECISA ESCLARECER] restante
