# Spec — Pomodoro: meta da sessão e progresso

- **Data:** 2026-10-26
- **Autor:** Claude (a pedido de Bruno C. Santos)
- **Status:** Rascunho
- **Capacidades afetadas:** `capabilities/pomodoro.md`, `capabilities/automacoes.md`
- **DTs relacionados:** DT-07, DT-08, DT-18
- **Depende de:** nenhuma (independente das outras três)

## 1. Problema
- Uma sessão iniciada por automação ("3 ciclos") passa a durar a meta diária (8) se o usuário pausar e retomar.
- Focos concluídos com o app fechado não contam no progresso do dia, justamente quando o timer está rodando em segundo plano.
- Mexer em qualquer automação cancela todo o trabalho agendado do app, e excluir uma automação deixa o agendamento dela ativo.

## 2. Resultado esperado
A meta escolhida vale do começo ao fim da sessão, todo foco concluído conta e
cada automação controla só o próprio agendamento.

## 3. Delta de requisitos

### ALTERADOS (só critérios e status; o texto dos requisitos não muda)

#### POMO-04 · Transições de fase
- **Critérios de aceite novos:**
  - **b.** Dado 4 ciclos até a pausa longa, quando o 1º, 2º e 3º focos terminam, então a fase seguinte é `PAUSA_CURTA`.
  - **c.** Quando uma pausa termina, então a fase seguinte é `FOCO`.

#### POMO-05 · Conclusão da sessão — ⚠️ → ✅
- **Critérios de aceite:**
  - **a.** Dada uma sessão iniciada por "Iniciar" na automação de 3 ciclos com meta diária 8, quando pauso e retomo, então a sessão termina ao fim do 3º foco.
  - **b.** Dada uma sessão manual com meta diária 8, quando o 8º foco termina, então a sessão é concluída.

#### POMO-06 · Progresso diário — ⚠️ → ✅
- **Critérios de aceite:**
  - **a.** Dado o timer rodando com o app fechado (removido dos recentes), quando um foco termina e eu abro o app, então o progresso do dia aumentou 1.
  - **b.** Dado o app aberto, quando um foco termina, então o progresso aumenta 1, e não 2.

#### AUTO-09 · Ativar e desativar — ⚠️ → ✅
- **Critérios de aceite:**
  - **a.** Dadas as automações A (ativa) e B, quando desativo B, então o agendamento de A continua igual e o de B é cancelado.

#### AUTO-10 · Excluir
- **Critérios de aceite novos:**
  - **a.** Dada uma automação com horário, quando a excluo, então nenhum trabalho agendado dela continua existindo.

## 4. Fora de escopo
- O progresso do dia não vira à meia-noite se o app ficar aberto (o "início do dia" é calculado uma vez). Fica como follow-up.
- Histórico além de 24 h (decisão de produto).
- Estado do timer quando o sistema mata o processo do serviço.

## 5. Constituição
**P7**: a máquina de estados do Pomodoro vira código puro com testes.

## 6. Perguntas em aberto
- Nenhuma: são correções de comportamento já especificado.
