# Research — Lembrete de saída confiável

- **Data:** 2026-10-26
- **Autor:** Claude
- **Spec:** [`spec.md`](spec.md)
- **Requisitos do delta:** LEMB-02, LEMB-03, LEMB-04, LEMB-07, LEMB-10, COMP-10, COMP-11

Caminhos relativos a `app/src/main/java/com/brunocsantos/agoravai/`.

## 1. Requisitos × código hoje

| Requisito | Status na spec viva | Real | Evidência |
|---|---|---|---|
| LEMB-02 | ⚠️ | confere (diverge) | O VM grava `"EXTRA_TITLE"`/`"EXTRA_MESSAGE"` (`ui/compromissos/CompromissoViewModel.kt:296-297`), mas o receiver lê `EXTRA_TITLE = "extra_title"` e `EXTRA_MESSAGE = "extra_message"` (`notifications/AlarmReceiver.kt:14-15,29-30`) e cai nos padrões "Lembrete" / "Hora do compromisso". |
| LEMB-03 | ⚠️ | confere | `"EXTRA_HAS_ACTION"`, `"EXTRA_ACTION_INTENT"` e `"EXTRA_ACTION_TITLE"` (`CompromissoViewModel.kt:299-301`) contra as constantes minúsculas (`AlarmReceiver.kt:17-19,114-123`): `hasAction` é sempre `false`. `"EXTRA_NOTIFICATION_ID"` (`:298`) contra `"extra_notification_id"` (`AlarmReceiver.kt:16,32`): o ID vira `System.currentTimeMillis().toInt()`. Os itens funcionam porque `EXTRA_ITEMS = "EXTRA_ITEMS"` (`:21`). |
| LEMB-04 | ✅ | **diverge** | A notificação de tela cheia não tem `setContentIntent` (`AlarmReceiver.kt:75-84`). No Android 14+, sem a permissão `USE_FULL_SCREEN_INTENT`, o sistema mostra só a notificação, que não abre nada. **Novo DT-19. A spec viva precisa ir para ⚠️.** |
| LEMB-07 | ⚠️ | confere | `ReminderActivity.confirmarSaida` envia `CONFIRM_EXIT` só com `"EXTRA_COMPROMISSO_ID"` (`ui/ReminderActivity.kt:74-80`); o receiver só cancela a notificação se `"EXTRA_NOTIFICATION_ID" != 0` (`services/NotificationActionReceiver.kt:21,53`). A `ReminderActivity` também não recebe o ID da notificação (`AlarmReceiver.kt:59-66`). Como a notificação é `setOngoing(true)` (`:84`), ela fica na bandeja. |
| LEMB-08 | ✅ | confere | Agendado em `CompromissoViewModel.kt:278-282` para `NotificationActionReceiver`, com ação `CHECK_LATE` e request code `id + 9999`. |
| LEMB-10 | ⚠️ | confere | `NotificationScheduler.scheduleAlarm` (etapas) não agenda quando não há permissão e só registra no log (`notifications/NotificationScheduler.kt:491-501`). Os helpers do VM usam `if (… canScheduleExactAlarms()) setExact… else setExact…` (`CompromissoViewModel.kt:282,308`): o `else` lança `SecurityException`, que o `catch` de `salvarCompromisso` transforma em "Erro ao salvar." (`:218`), mas só depois de o compromisso, as etapas e os itens já terem sido gravados (`:197-212`). |
| COMP-10/11 | ⚠️ (11.a) | confere | `cancelarTodosAlarmes` cria o `PendingIntent` de atraso **sem `action`** (`CompromissoViewModel.kt:311-315`). Como `Intent.filterEquals` compara a ação, ele não casa com o original e o alarme de atraso nunca é cancelado. `CONFIRM_EXIT` cancela `id*100+98` via `AlarmReceiver` (`NotificationActionReceiver.kt:50`), um ID que nunca foi agendado. |

## 2. Arquivos e componentes relevantes

| Arquivo:linha | Componente | Papel |
|---|---|---|
| `ui/compromissos/CompromissoViewModel.kt:144-219` | `salvarCompromisso` | Valida, chama a Directions, grava e agenda |
| `ui/compromissos/CompromissoViewModel.kt:222-238` | `toggleCompromisso`, `excluirCompromisso` | Reagendam ou cancelam |
| `ui/compromissos/CompromissoViewModel.kt:240-316` | `agendarAlarmes`, `agendarAlarmeSaidaComAction`, `cancelarTodosAlarmes` | Toda a lógica de alarmes, privada no VM |
| `notifications/NotificationScheduler.kt` | `scheduleAlarm`, `cancelAlarm` | Alarmes das etapas |
| `notifications/AlarmReceiver.kt:13-22` | Constantes `EXTRA_*` | Única definição de chaves (parcial) |
| `notifications/AlarmReceiver.kt:49-88` | `showFullScreenAlarm` | Notificação + `ReminderActivity` |
| `ui/ReminderActivity.kt:40-81` | Tela cheia | Lê extras literais `"EXTRA_TITLE"` etc. |
| `services/NotificationActionReceiver.kt:41-56` | `CONFIRM_EXIT` | Grava a saída, fecha a notificação |
| `ui/compromissos/AddCompromissoScreen.kt:48-53` | Snackbar de `saveError` | Única forma de erro do formulário, sem ação |

## 3. Fluxo atual
1. "Salvar" → `salvarCompromisso` → Directions → `dao.inserirCompromisso`/`updateCompromisso` → etapas e itens → `addGeofenceChegada` → `agendarAlarmes`.
2. `agendarAlarmes`: para cada etapa (lista invertida), `NotificationScheduler.scheduleAlarm(id*100+i)`; depois `agendarAlarmeSaidaComAction(id*100+99)`; depois o alarme `CHECK_LATE` (`id+9999`).
3. Na hora: `AlarmReceiver` → `PADRAO`: notificação; outros tipos: notificação com `fullScreenIntent` → `ReminderActivity`.
4. Confirmação: ação "Já Saí" ou botão da tela cheia → `NotificationActionReceiver.CONFIRM_EXIT` → `dao.confirmarSaida`.
5. `CHECK_LATE` → se `!confirmouSaida`, notifica o atraso (ID `id*100+98`).

## 4. Contratos que não podem quebrar
- **Request codes e IDs** (`architecture.md` §5.2): etapas `id*100+i`, saída `id*100+99`, ação "Já Saí" `id`, atraso `id+9999`, notificação de atraso `id*100+98`. Precisam continuar iguais para que cancelamentos e reagendamentos acertem alarmes já agendados.
- **Chaves de extras:** existem dois "dialetos". As constantes do `AlarmReceiver` têm valores minúsculos (`"extra_title"`…), exceto `EXTRA_ITEMS` e `EXTRA_COMPROMISSO_ID`. `NotificationActionReceiver`, `PomodoroWorker` e `ReminderActivity` usam literais em maiúsculas (`"EXTRA_NOTIFICATION_ID"`, `"EXTRA_CYCLES"`, `"EXTRA_TITLE"`…). Os **valores** lidos por cada receptor não podem mudar sem mudar quem envia.
- **Ações:** `NotificationActionReceiver.Actions` = `START, DISMISS, CONFIRM_EXIT, CHECK_LATE`.
- **Room:** nenhuma mudança necessária.

## 5. Restrições de plataforma
- `canScheduleExactAlarms()` existe a partir da API 31. O manifest declara `SCHEDULE_EXACT_ALARM` e `USE_EXACT_ALARM`. Com `USE_EXACT_ALARM` (API 33+), a permissão é concedida automaticamente. No Android 12/12L o usuário pode revogá-la.
- A tela de permissão é `Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM` (API 31+).
- Android 14 (API 34): `USE_FULL_SCREEN_INTENT` só é concedida automaticamente a apps de chamada ou alarme. Sem ela, o `fullScreenIntent` vira uma notificação comum.
- Notificação `ongoing` só sai com `cancel(id)` explícito.

## 6. Testes e verificação existentes
Nenhum. `app/src/test/.../ExampleUnitTest.kt` só tem a linha `package`, e não há `testImplementation` no `app/build.gradle.kts`. `./gradlew assembleDebug testDebugUnitTest` passa (linha de base verificada em 2026-10-26).

## 7. Outros achados
- `local.properties` precisa de `MAPS_API_KEY="…"` **com aspas**: o valor é inserido cru em `buildConfigField` (`app/build.gradle.kts`), e sem aspas o `BuildConfig.java` não compila.
- `architecture.md` §3.3 lista as versões do `libs.versions.toml`, que **não é usado**. As versões reais (`build.gradle.kts` da raiz) são AGP 8.4.1, Kotlin 1.9.23, KSP 1.9.23-1.0.19 e compilador Compose 1.5.13.

## 8. Perguntas em aberto
- Ver Q1 e Q2 na spec.
