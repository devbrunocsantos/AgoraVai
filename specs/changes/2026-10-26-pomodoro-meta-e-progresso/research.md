# Research — Pomodoro: meta da sessão e progresso

- **Data:** 2026-10-26
- **Autor:** Claude
- **Spec:** [`spec.md`](spec.md)
- **Requisitos do delta:** POMO-04, POMO-05, POMO-06, AUTO-09, AUTO-10

Caminhos relativos a `app/src/main/java/com/brunocsantos/agoravai/`.

## 1. Requisitos × código hoje

| Requisito | Status na spec viva | Real | Evidência |
|---|---|---|---|
| POMO-04 | ✅ | confere | `moveToNextState` (`services/PomodoroService.kt:96-164`): `cicloAtual % ciclosParaPausaLonga == 0` → pausa longa. |
| POMO-05 | ⚠️ | confere | `onStartCommand` (`PomodoroService.kt:41-58`) recalcula `metaTotalDaSessao` a **cada** comando: sem `EXTRA_CYCLES` (PAUSE, RESET, "Retomar" da notificação ou da tela) volta à meta diária. Só o `START` vindo de `NotificationActionReceiver` (`services/NotificationActionReceiver.kt:29-37`) envia `EXTRA_CYCLES`. |
| POMO-06 | ⚠️ | confere | O foco concluído só emite `FocusSessionState.onFocusSessionCompleted()` (`PomodoroService.kt:117`), um `MutableSharedFlow` sem replay (`services/FocusSessionState.kt`). Quem grava no banco é o coletor do `PomodoroViewModel` (`ui/pomodoro/PomodoroViewModel.kt:101-106`). Sem ViewModel vivo, o evento se perde. |
| AUTO-09 | ⚠️ | confere | `updateSchedules` (`PomodoroViewModel.kt:243-256`) chama `PomodoroScheduler.cancelAll` → `WorkManager.cancelAllWork()` (`services/PomodoroScheduler.kt:53-54`). |
| AUTO-10 | ✅ | **diverge** | `deleteCurrentAutomation` (`PomodoroViewModel.kt:215-224`) apaga do banco e só depois chama `updateSchedules`. Hoje o `cancelAll` acaba cancelando o trabalho da excluída por acidente. Ao trocar por cancelamento por automação, a excluída não estaria mais na lista e o trabalho dela ficaria órfão. A correção precisa cancelar explicitamente. |

## 2. Fluxo atual
- **Manual:** UI → `PomodoroViewModel.iniciarOuRetomarTimer` → `startService(START)` → `onStartCommand` → `start()`, que só zera o ciclo se `PARADO` (`:68-77`).
- **Automação:** `PomodoroWorker` → notificação "Iniciar" → `NotificationActionReceiver.START` com `EXTRA_CYCLES` → `startService`.
- **Foco concluído:** `moveToNextState` → `FocusSessionState` (evento) → VM → `dao.inserirSessaoPomodoroConcluida`.
- **Agendamento:** trabalhos com nome único e tag `"pomodoro_automation_<id>"` (`PomodoroScheduler.kt:16,19-50`); one-shot por geofence com a mesma tag (`services/geofence/GeofenceBroadcastReceiver.kt`). `cancel(context, setting)` já cancela por tag (`:59-61`), mas ninguém usa.

## 3. Contratos
- Extra `"EXTRA_CYCLES"` (literal) entre `PomodoroWorker`, `NotificationActionReceiver` e `PomodoroService`.
- Ações `PomodoroService.Actions` = `START, PAUSE, RESET`.
- Tag/nome único `"pomodoro_automation_<id>"`: o `PomodoroWorker` extrai o ID da automação dele (`workers/PomodoroWorker.kt`).
- Tabela `sessoes_pomodoro_concluidas` (sem mudança).

## 4. Restrições
- O serviço roda em `Dispatchers.Main` (`PomodoroService.kt:25`). O acesso ao Room precisa ir para `Dispatchers.IO`.
- Não há outros usos de WorkManager no app hoje, então DT-07 ainda não causa dano visível, mas causaria assim que surgisse um.

## 5. Testes existentes
Nenhum. A lógica de transição está embutida no `Service` e não dá para testá-la sem Android.

## 6. Perguntas em aberto
- Nenhuma.
