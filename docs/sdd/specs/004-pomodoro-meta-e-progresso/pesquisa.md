---
spec: docs/sdd/specs/004-pomodoro-meta-e-progresso/spec.md
data: 2026-10-26
commit: 791c539
branch: ccr-80e3a1a2-ocxhin
pesquisador: Claude
status: completa  # completa | parcial
---

# Pesquisa: Pomodoro — meta da sessão e progresso

Caminhos relativos a `app/src/main/java/com/brunocsantos/agoravai/`. As specs 001–002 não tocaram nesta área, mas convém reconferir as linhas antes de implementar.

## Pergunta de pesquisa

Por que a meta da sessão muda ao pausar/retomar, por que focos com o app fechado não contam, e como o agendamento das automações é cancelado?

## Resumo

- `PomodoroService.onStartCommand` recalcula a meta a **cada** comando; só o `START` vindo da automação envia `EXTRA_CYCLES` (DT-18).
- O foco concluído é só um evento num `SharedFlow` sem replay; quem grava é o `PomodoroViewModel` — sem UI viva, o evento se perde (DT-08).
- `updateSchedules` chama `WorkManager.cancelAllWork()` (DT-07). `cancel(context, setting)` por tag já existe, mas ninguém usa.
- Excluir hoje "funciona" por acidente (o `cancelAll`); ao trocar por cancelamento por automação, a excluída ficaria órfã se não for cancelada explicitamente.
- A máquina de estados está embutida no `Service`, sem testes.

## Descobertas detalhadas

### Requisitos × código

| Requisito | Status na capacidade | Real | Evidência |
|---|---|---|---|
| POMO-04 | ✅ | confere | `moveToNextState` (`services/PomodoroService.kt:96-164`): `cicloAtual % ciclosParaPausaLonga == 0` → pausa longa. |
| POMO-05 | ⚠️ | confere | `onStartCommand` (`PomodoroService.kt:41-58`) recalcula `metaTotalDaSessao` a cada comando; sem `EXTRA_CYCLES` volta à meta diária. Só o `START` de `NotificationActionReceiver` (`services/NotificationActionReceiver.kt:29-37`) envia `EXTRA_CYCLES`. |
| POMO-06 | ⚠️ | confere | `FocusSessionState.onFocusSessionCompleted()` (`PomodoroService.kt:117`), `MutableSharedFlow` sem replay (`services/FocusSessionState.kt`); coletor no `PomodoroViewModel` (`ui/pomodoro/PomodoroViewModel.kt:101-106`). |
| AUTO-09 | ⚠️ | confere | `updateSchedules` (`PomodoroViewModel.kt:243-256`) → `PomodoroScheduler.cancelAll` → `WorkManager.cancelAllWork()` (`services/PomodoroScheduler.kt:53-54`). |
| AUTO-10 | ✅ | **diverge** | `deleteCurrentAutomation` (`PomodoroViewModel.kt:215-224`) apaga e só depois chama `updateSchedules`. |

## Fluxo de dados relevante

```
Manual: UI → PomodoroViewModel.iniciarOuRetomarTimer → startService(START) → onStartCommand → start() (zera ciclo só se PARADO, :68-77)
Automação: PomodoroWorker → notificação "Iniciar" → NotificationActionReceiver.START (+EXTRA_CYCLES) → startService
Foco concluído: moveToNextState → FocusSessionState (evento) → VM → dao.inserirSessaoPomodoroConcluida
Agendamento: trabalhos com nome único e tag "pomodoro_automation_<id>" (PomodoroScheduler.kt:16,19-50); one-shot por geofence com a mesma tag (GeofenceBroadcastReceiver)
```

## Padrões e convenções a seguir

- Regras puras + JUnit (`notifications/HorariosLembrete.kt` e `HorariosLembreteTest`, spec 001).
- `object` para singletons de infraestrutura (`PomodoroScheduler`).

## Pontos de integração prováveis

- `services/PomodoroService.kt:41-58, 96-164` — meta e transições.
- `services/FocusSessionState.kt` e `PomodoroViewModel.kt:101-106` — gravação dos focos.
- `services/PomodoroScheduler.kt:53-61` — `cancelAll` / `cancel`.
- `PomodoroViewModel.kt:215-256` — excluir e reagendar.

## Restrições e riscos observados

- **Contratos**: extra `"EXTRA_CYCLES"` (literal) entre `PomodoroWorker`, `NotificationActionReceiver` e `PomodoroService`; ações `START, PAUSE, RESET`; tag/nome `"pomodoro_automation_<id>"` (o `PomodoroWorker` extrai o ID dela); tabela `sessoes_pomodoro_concluidas` sem mudança.
- O serviço roda em `Dispatchers.Main` (`PomodoroService.kt:25`); Room precisa de `Dispatchers.IO`.
- Não há outros usos de WorkManager hoje — DT-07 ainda não causa dano visível, mas causaria.

## Contexto histórico

- `PomodoroWorker` ainda usa o literal `"EXTRA_NOTIFICATION_ID"` (follow-up da spec 001).

## Referências de código

- Verificação de agendamentos: `adb shell dumpsys jobscheduler | grep agoravai`.

## Questões em aberto

- (nenhuma)
