# Plan — Pomodoro: meta da sessão e progresso

- **Data:** 2026-10-26
- **Spec:** [`spec.md`](spec.md) · **Research:** [`research.md`](research.md)
- **Status:** Rascunho

## 1. Objetivo
Corrigir POMO-05, POMO-06, AUTO-09 e AUTO-10, deixando a máquina de estados
testável.

## 2. Matriz de verificação

| Critério | Tipo | Verificação |
|---|---|---|
| POMO-04.a/.b/.c | Unitário | `CicloPomodoroTest`: `proximaFase` |
| POMO-05.a | Unitário + Manual | `CicloPomodoroTest`: `resolverMeta` mantém a meta ao retomar; R1 |
| POMO-05.b | Unitário | `CicloPomodoroTest`: conclusão quando o ciclo atinge a meta |
| POMO-06.a | Manual | R2: foco de 1 min, remover o app dos recentes, esperar, abrir → +1 |
| POMO-06.b | Manual | R3: com o app aberto, foco de 1 min → +1 (sem duplicar) |
| AUTO-09.a, AUTO-10.a | Manual | R4: `adb shell dumpsys jobscheduler \| grep agoravai` antes e depois de desativar ou excluir |

- **R1:** foco de 1 min, pausa de 1 min, meta diária 8; automação de 2 ciclos para daqui a 1 min; "Iniciar"; pausar e retomar; a sessão termina após o 2º foco.

## 3. Abordagem
1. **`services/CicloPomodoro.kt`** (puro): `proximaFase(faseAtual, ciclosConcluidos, ciclosAtePausaLonga, meta): ProximoPasso` (próxima fase ou sessão concluída) e `resolverMeta(iniciandoNovaSessao, ciclosDoExtra, metaDiaria, metaAtual)`.
2. **`PomodoroService`**: a meta só é definida quando uma sessão nova começa (`START` a partir de `PARADO`); `moveToNextState` usa `proximaFase`. Ao concluir um foco, grava `SessaoPomodoro` direto no Room (`Dispatchers.IO`).
3. **Remover `FocusSessionState`** e o coletor do VM, porque a gravação passa a ser do serviço (evita contagem dupla, POMO-06.b).
4. **`PomodoroScheduler`**: troca `cancelAll` por `cancelar(context, id)` (por tag). `updateSchedules` agenda as ativas com horário e cancela as demais. `deleteCurrentAutomation` cancela antes de apagar.

| Alternativa descartada | Por que não |
|---|---|
| `SharedFlow` com replay para não perder o evento | Continua dependendo da UI para gravar; o serviço é o dono do fato |
| Manter `cancelAll` e só reagendar | Mantém DT-07 |

## 4. Checklist da constituição

| Princípio | Atende? | Nota |
|---|---|---|
| P1 Spec primeiro | ☐ | aguarda aprovação |
| P2 Lembrete não falha em silêncio | ✅ | — |
| P3 Dados preservados | ✅ | sem schema |
| P4 Local-first | ✅ | — |
| P5 Desenho para TDAH | ✅ | a meta escolhida é respeitada |
| P6 Contratos explícitos | ✅ | `EXTRA_CYCLES` e tags mantidos |
| P7 Critérios verificáveis | ✅ | máquina de estados testada |
| P8 Simplicidade | ✅ | remove código (`FocusSessionState`) |
| P9 Consistência | ✅ | — |

## 5. Passos

### Fase 1 — Máquina de estados pura
- **Critérios:** POMO-04.a–c, POMO-05.a/.b (lógica)
- **Arquivos:** `services/CicloPomodoro.kt` (novo), `app/src/test/.../CicloPomodoroTest.kt` (novo). Se esta mudança for a primeira a ser implementada, inclui `testImplementation("junit:junit:4.13.2")`.
- **Verificação:** `./gradlew testDebugUnitTest`

### Fase 2 — Serviço
- **Critérios:** POMO-05, POMO-06
- **Arquivos:** `services/PomodoroService.kt`, `services/FocusSessionState.kt` (remover), `ui/pomodoro/PomodoroViewModel.kt`
- **Verificação:** build + testes + R1, R2, R3

### Fase 3 — Agendamento por automação
- **Critérios:** AUTO-09.a, AUTO-10.a
- **Arquivos:** `services/PomodoroScheduler.kt`, `ui/pomodoro/PomodoroViewModel.kt`
- **Verificação:** build + R4

## 6. Impacto
- **Room:** nenhum.
- **Manifest:** nenhum.
- **Aparelhos existentes:** os trabalhos já agendados mantêm os mesmos nomes e tags e continuam funcionando.

## 7. Riscos e rollback
| Risco | Mitigação |
|---|---|
| Mudança de comportamento na transição de fases | Testes POMO-04 escritos **antes** de trocar o código do serviço |

**Rollback:** reverter os commits.

## 8. Atualização das specs ao final
- [ ] `capabilities/pomodoro.md`: POMO-04/05/06 com critérios, status ✅
- [ ] `capabilities/automacoes.md`: AUTO-09/10 ✅
- [ ] `architecture.md`: riscar DT-07, DT-08, DT-18; §5.4 sem `FocusSessionState`; histórico
