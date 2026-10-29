---
spec: docs/sdd/specs/004-pomodoro-meta-e-progresso/spec.md
pesquisa: docs/sdd/specs/004-pomodoro-meta-e-progresso/pesquisa.md
data: 2026-10-26
commit_base: <preencher ao aprovar>
status: rascunho  # rascunho | aprovado | em-implementacao | concluido
---

# Plano de implementação: Pomodoro — meta da sessão e progresso

> **Rascunho anterior à aprovação da spec.** Após aprovar a spec, revisar com `/planejar 004`.

## Visão geral

Extrair a máquina de estados do Pomodoro para código puro e testado
(`CicloPomodoro`), fazer o serviço definir a meta só no início de uma sessão
nova e gravar os focos concluídos diretamente no Room, e trocar o
`cancelAllWork()` por cancelamento por automação.

## Estado atual

- `PomodoroService.kt:41-58` — meta recalculada a cada comando.
- `FocusSessionState.kt` + `PomodoroViewModel.kt:101-106` — gravação dependente da UI.
- `PomodoroScheduler.kt:53-54` — `cancelAllWork()`.
- `PomodoroViewModel.kt:215-224` — excluir não cancela explicitamente.

## Estado desejado

Meta estável durante a sessão; cada foco gravado uma vez pelo serviço; cada
automação cancela/reagenda só a si mesma. Verificado por `CicloPomodoroTest` e
pelos roteiros R1–R4.

## Fora de escopo

- Virada do dia com o app aberto; histórico > 24 h; restaurar o timer após o serviço ser encerrado.

## Decisões de design

| Decisão | Alternativas consideradas | Motivo da escolha |
|---|---|---|
| Serviço grava `SessaoPomodoro` direto no Room (IO) e `FocusSessionState` é removido | `SharedFlow` com replay | Continuaria dependendo da UI para gravar; o serviço é o dono do fato; remove a contagem dupla |
| `PomodoroScheduler.cancelar(context, id)` por tag | Manter `cancelAll` e só reagendar | Mantém DT-07 |
| Meta definida só em `START` a partir de `PARADO` | Enviar `EXTRA_CYCLES` em todo comando | Mais pontos de envio, mais chance de erro |

## Conformidade com a constituição

- [ ] P1 A spec vem primeiro — aguarda aprovação
- [x] P2 Lembrete não falha em silêncio — n/a
- [x] P3 Dados preservados — sem schema
- [x] P4 Local-first
- [x] P5 Desenho para TDAH — a meta escolhida é respeitada
- [x] P6 Contratos explícitos — `EXTRA_CYCLES` e tags mantidos
- [x] P7 Critérios verificáveis — máquina de estados testada
- [x] P8 Simplicidade — remove código (`FocusSessionState`)
- [x] P9 Consistência

---

## Fase 1: Máquina de estados pura

### Mudanças
- **Arquivo**: `services/CicloPomodoro.kt` (novo) — `proximaFase(faseAtual, ciclosConcluidos, ciclosAtePausaLonga, meta): ProximoPasso` (próxima fase ou sessão concluída) e `resolverMeta(iniciandoNovaSessao, ciclosDoExtra, metaDiaria, metaAtual)`.
- **Arquivo**: `app/src/test/.../services/CicloPomodoroTest.kt` (novo).

### Critérios de sucesso
#### Verificação automatizada
- [ ] `./gradlew testDebugUnitTest`
- [ ] `CicloPomodoroTest` cobre CA-1.3, CA-1.4, CA-1.5 (`proximaFase`), CA-1.1 (`resolverMeta` mantém a meta ao retomar) e CA-1.2 (conclusão na meta) — escritos **antes** de trocar o serviço

---

## Fase 2: Serviço

### Mudanças
- **Arquivo**: `services/PomodoroService.kt` — meta só em sessão nova; `moveToNextState` usa `proximaFase`; ao concluir foco grava no Room (IO).
- **Arquivo**: `services/FocusSessionState.kt` — remover.
- **Arquivo**: `ui/pomodoro/PomodoroViewModel.kt` — remover o coletor.

### Critérios de sucesso
#### Verificação automatizada
- [ ] `./gradlew testDebugUnitTest assembleDebug`
#### Verificação manual
- [ ] R1 — CA-1.1: foco 1 min, pausa 1 min, meta diária 8; automação de 2 ciclos daqui a 1 min; "Iniciar"; pausar e retomar → termina após o 2º foco
- [ ] R2 — CA-2.1: foco de 1 min, remover o app dos recentes, esperar, abrir → +1
- [ ] R3 — CA-2.2: app aberto, foco de 1 min → +1 (sem duplicar)

---

## Fase 3: Agendamento por automação

### Mudanças
- **Arquivo**: `services/PomodoroScheduler.kt` — `cancelar(context, id)` por tag no lugar de `cancelAll`.
- **Arquivo**: `ui/pomodoro/PomodoroViewModel.kt` — `updateSchedules` agenda as ativas com horário e cancela as demais; `deleteCurrentAutomation` cancela antes de apagar.

### Critérios de sucesso
#### Verificação automatizada
- [ ] `./gradlew testDebugUnitTest assembleDebug`
#### Verificação manual
- [ ] R4 — CA-3.1, CA-3.2: `adb shell dumpsys jobscheduler | grep agoravai` antes e depois de desativar B e de excluir uma automação

---

## Estratégia de testes

| Critério (spec) | Tipo | Teste | Fase |
|---|---|---|---|
| CA-1.1 a CA-1.5 | unitário | `CicloPomodoroTest` | 1 |
| CA-1.1 | manual | R1 | 2 |
| CA-2.1 | manual | R2 | 2 |
| CA-2.2 | manual | R3 | 2 |
| CA-3.1, CA-3.2 | manual | R4 | 3 |

## Rastreabilidade

| Requisito | Fase(s) |
|---|---|
| RF-001, RF-002 | 1, 2 |
| RF-003 | 2 |
| RF-004, RF-005 | 3 |
| RNF-001 | 1 |
| RNF-002 | 3 (nomes e tags mantidos) |

## Riscos e mitigação

| Risco | Impacto | Mitigação |
|---|---|---|
| Mudança de comportamento na transição de fases | Médio | Testes de POMO-04 escritos antes de trocar o serviço |

## Migração e rollback

- Room e manifest: nenhum impacto. Trabalhos já agendados mantêm nomes e tags.
- **Rollback:** reverter os commits.

## Atualização das capacidades ao final

- [ ] `capacidades/pomodoro.md`: POMO-04/05/06 com critérios, status ✅
- [ ] `capacidades/automacoes.md`: AUTO-09/10 ✅
- [ ] `arquitetura.md`: riscar DT-07, DT-08, DT-18; §5.4 sem `FocusSessionState`; histórico

## Referências

- Spec: `docs/sdd/specs/004-pomodoro-meta-e-progresso/spec.md`
- Pesquisa: `docs/sdd/specs/004-pomodoro-meta-e-progresso/pesquisa.md`
- Padrão de regra pura + teste: `notifications/HorariosLembrete.kt`, `HorariosLembreteTest`
