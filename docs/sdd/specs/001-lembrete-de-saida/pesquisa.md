---
spec: docs/sdd/specs/001-lembrete-de-saida/spec.md
data: 2026-10-26
commit: 791c539
branch: ccr-80e3a1a2-ocxhin
pesquisador: Claude
status: completa  # completa | parcial
---

# Pesquisa: Lembrete de saída confiável

Caminhos relativos a `app/src/main/java/com/brunocsantos/agoravai/`. Linhas referem-se ao código **antes** da implementação.

## Pergunta de pesquisa

Por que o lembrete de saída chega sem título, mensagem e ação; por que confirmar não fecha a notificação nem cancela o atraso; e o que acontece sem permissão de alarme exato?

## Resumo

- Existem dois "dialetos" de chaves de extras: o ViewModel grava `"EXTRA_TITLE"` etc. e o `AlarmReceiver` lê `"extra_title"` → caem nos padrões genéricos e `hasAction` é sempre `false` (DT-02).
- A notificação de tela cheia não tem `contentIntent`; no Android 14 sem `USE_FULL_SCREEN_INTENT` ela não abre nada (novo DT-19; LEMB-04 passa de ✅ para ⚠️).
- `ReminderActivity` não recebe nem repassa o ID da notificação; como ela é `ongoing`, fica presa na bandeja (DT-16).
- O cancelamento do aviso de atraso cria o `PendingIntent` sem `action` e nunca casa com o original (DT-06).
- Sem alarme exato, as etapas só registram log e a saída lança `SecurityException` depois de o compromisso já ter sido gravado (DT-17).
- Toda a lógica de alarmes é privada no `CompromissoViewModel` — nada fora da UI consegue reagendar ou cancelar.
- Não há nenhum teste no projeto.

## Descobertas detalhadas

### Requisitos × código

| Requisito | Status na capacidade | Real | Evidência |
|---|---|---|---|
| LEMB-02 | ⚠️ | confere (diverge) | VM grava `"EXTRA_TITLE"`/`"EXTRA_MESSAGE"` (`ui/compromissos/CompromissoViewModel.kt:296-297`); receiver lê `"extra_title"`/`"extra_message"` (`notifications/AlarmReceiver.kt:14-15,29-30`) e cai em "Lembrete" / "Hora do compromisso". |
| LEMB-03 | ⚠️ | confere | `"EXTRA_HAS_ACTION"`, `"EXTRA_ACTION_INTENT"`, `"EXTRA_ACTION_TITLE"` (`CompromissoViewModel.kt:299-301`) contra constantes minúsculas (`AlarmReceiver.kt:17-19,114-123`): `hasAction` sempre `false`. `"EXTRA_NOTIFICATION_ID"` (`:298`) contra `"extra_notification_id"` (`AlarmReceiver.kt:16,32`): o ID vira `System.currentTimeMillis().toInt()`. Itens funcionam porque `EXTRA_ITEMS = "EXTRA_ITEMS"` (`:21`). |
| LEMB-04 | ✅ | **diverge** | Sem `setContentIntent` na tela cheia (`AlarmReceiver.kt:75-84`). Novo **DT-19**. |
| LEMB-07 | ⚠️ | confere | `ReminderActivity.confirmarSaida` envia `CONFIRM_EXIT` só com `"EXTRA_COMPROMISSO_ID"` (`ui/ReminderActivity.kt:74-80`); o receiver só cancela se `"EXTRA_NOTIFICATION_ID" != 0` (`services/NotificationActionReceiver.kt:21,53`); notificação `setOngoing(true)` (`AlarmReceiver.kt:84`). |
| LEMB-08 | ✅ | confere | `CHECK_LATE`, request code `id + 9999` (`CompromissoViewModel.kt:278-282`). |
| LEMB-10 | ⚠️ | confere | `NotificationScheduler.scheduleAlarm` só loga sem permissão (`notifications/NotificationScheduler.kt:491-501`); helpers do VM chamam `setExact…` no `else` (`CompromissoViewModel.kt:282,308`) → `SecurityException` → "Erro ao salvar." (`:218`) depois das gravações (`:197-212`). |
| COMP-10/11 | ⚠️ (11.a) | confere | `cancelarTodosAlarmes` cria o `PendingIntent` de atraso sem `action` (`CompromissoViewModel.kt:311-315`); `Intent.filterEquals` não casa. `CONFIRM_EXIT` cancela `id*100+98` (`NotificationActionReceiver.kt:50`), nunca agendado. |

### Componentes

| Arquivo:linha | Componente | Papel |
|---|---|---|
| `ui/compromissos/CompromissoViewModel.kt:144-219` | `salvarCompromisso` | Valida, chama a Directions, grava e agenda |
| `ui/compromissos/CompromissoViewModel.kt:222-238` | `toggleCompromisso`, `excluirCompromisso` | Reagendam ou cancelam |
| `ui/compromissos/CompromissoViewModel.kt:240-316` | `agendarAlarmes`, `agendarAlarmeSaidaComAction`, `cancelarTodosAlarmes` | Lógica de alarmes, privada no VM |
| `notifications/NotificationScheduler.kt` | `scheduleAlarm`, `cancelAlarm` | Alarmes das etapas |
| `notifications/AlarmReceiver.kt:13-22` | Constantes `EXTRA_*` | Única definição de chaves (parcial) |
| `notifications/AlarmReceiver.kt:49-88` | `showFullScreenAlarm` | Notificação + `ReminderActivity` |
| `ui/ReminderActivity.kt:40-81` | Tela cheia | Lê extras literais |
| `services/NotificationActionReceiver.kt:41-56` | `CONFIRM_EXIT` | Grava a saída, fecha a notificação |
| `ui/compromissos/AddCompromissoScreen.kt:48-53` | Snackbar de `saveError` | Única forma de erro do formulário, sem ação |

## Fluxo de dados relevante

```
"Salvar" → salvarCompromisso → Directions → dao.inserir/updateCompromisso → etapas/itens
        → addGeofenceChegada → agendarAlarmes
agendarAlarmes: etapas (invertidas) NotificationScheduler.scheduleAlarm(id*100+i)
             → saída agendarAlarmeSaidaComAction(id*100+99) → CHECK_LATE (id+9999)
Na hora: AlarmReceiver → PADRAO: notificação | outros: fullScreenIntent → ReminderActivity
Confirmação: "Já Saí" / tela cheia → NotificationActionReceiver.CONFIRM_EXIT → dao.confirmarSaida
CHECK_LATE → se !confirmouSaida → notifica atraso (id*100+98)
```

## Padrões e convenções a seguir

- **Singletons de infraestrutura** como `object` em `notifications/` (P9).
- **Contratos** (request codes, IDs, extras) documentados em `arquitetura.md` §5.2.
- **Testes**: não há; `ExampleUnitTest.kt` só tem `package` e não há `testImplementation`. `junit:junit:4.13.2` já está previsto em `libs.versions.toml`.

## Pontos de integração prováveis

- `CompromissoViewModel.kt:240-316` — lógica a extrair.
- `AlarmReceiver.kt:49-88` — notificação de tela cheia.
- `NotificationActionReceiver.kt:41-56` — confirmação de saída.
- `AddCompromissoScreen.kt:48-53` — único canal de erro do formulário.

## Restrições e riscos observados

- **Contratos que não podem quebrar** (§5.2): etapas `id*100+i`, saída `id*100+99`, ação "Já Saí" `id`, atraso `id+9999`, notificação de atraso `id*100+98`.
- **Chaves de extras**: os valores lidos por cada receptor não podem mudar sem mudar quem envia (`NotificationActionReceiver`, `PomodoroWorker`, `ReminderActivity` usam literais maiúsculos).
- Ações de `NotificationActionReceiver.Actions`: `START, DISMISS, CONFIRM_EXIT, CHECK_LATE`.
- `canScheduleExactAlarms()` existe a partir da API 31; manifest declara `SCHEDULE_EXACT_ALARM` e `USE_EXACT_ALARM`. Tela de permissão: `Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM`.
- Android 14: `USE_FULL_SCREEN_INTENT` só é automática para apps de chamada/alarme.
- Notificação `ongoing` só sai com `cancel(id)` explícito.
- Room: nenhuma mudança necessária.

## Contexto histórico

- Primeira mudança no fluxo SDD do projeto. Linha de base `./gradlew assembleDebug testDebugUnitTest` passando em 2026-10-26.

## Referências de código

- `app/build.gradle.kts` — `MAPS_API_KEY` inserido cru em `buildConfigField`: `local.properties` precisa do valor **com aspas**.
- `arquitetura.md` §3.3 listava versões do `libs.versions.toml`, que **não é usado**; as reais (raiz `build.gradle.kts`) eram AGP 8.4.1, Kotlin 1.9.23, KSP 1.9.23-1.0.19, compilador Compose 1.5.13.

## Questões em aberto

- Resolvidas na spec (Sessão 2026-10-26).
