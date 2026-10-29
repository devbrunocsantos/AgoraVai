---
spec: docs/sdd/specs/003-reagendar-apos-reiniciar/spec.md
data: 2026-10-29
commit: 9e47d7421b50b61d20328b72002c89622be1cc7a
branch: ccr-80e3a1a2-ocxhin
pesquisador: Claude (gerente-de-projeto) + analista-de-codigo ×3, buscador-de-padroes
status: completa  # completa | parcial
---

# Pesquisa: Reagendar após reiniciar ou atualizar

Caminhos relativos a `app/src/main/java/com/brunocsantos/agoravai/` (testes em `app/src/test/java/com/brunocsantos/agoravai/`).
Substitui a pesquisa parcial de 2026-10-26, feita antes das specs 001 e 002.

## Pergunta de pesquisa

O que acontece hoje com alarmes de compromisso, ativação de chegada e geofences de automação ao reiniciar ou atualizar o app; quais peças existem para recriá-los sem abrir o app; e como o código trata horários no passado, a ordem entre saída e chegada e a falta de permissão.

## Resumo

- **Não existe** receiver de `BOOT_COMPLETED`/`MY_PACKAGE_REPLACED` nem a permissão `RECEIVE_BOOT_COMPLETED` (`AndroidManifest.xml:5-21,49-59`). Nenhum receiver tem `intent-filter`.
- `CompromissoAlarmScheduler.agendar(context, compromisso, etapas, itens)` é o ponto único que agenda etapas, saída, atraso e a ativação da chegada (`notifications/CompromissoAlarmScheduler.kt:37-66`). Ele **não filtra horários no passado** de etapas, saída e atraso: com `setExactAndAllowWhileIdle`, um horário passado dispara imediatamente. A ativação da chegada só é filtrada pelo **fim** do período (`:62-65`).
- `AlarmReceiver` mostra o lembrete **sem consultar o banco** (nem `confirmouSaida`, nem `confirmouChegada`) (`notifications/AlarmReceiver.kt:30-49`). `CHECK_LATE` checa só `confirmouSaida` (`services/NotificationActionReceiver.kt:85`).
- Não há ordenação entre o alarme de saída e o de ativação da chegada. A ativação só **registra** a geofence; a entrada inicial chega depois, de forma assíncrona, pelo Play Services (`services/geofence/CompromissoGeofenceManager.kt:66,73`).
- Não há consulta `suspend` que liste compromissos (só `Flow`, `data/AppDao.kt:31-33`). Para automações existe `getActiveAutomationSettings()` (`isEnabled = 1`), sem uso hoje (`AppDao.kt:85`).
- O canal de notificação só é criado em `MainActivity.onCreate` (`MainActivity.kt:120`), não no `AgoraVaiApplication`.
- Trabalhos periódicos do WorkManager (gatilho de horário das automações) já sobrevivem ao reboot pela própria biblioteca (`services/PomodoroScheduler.kt:19-50`).

## Descobertas detalhadas

### Agendamento de lembretes (`notifications/CompromissoAlarmScheduler.kt`)

- IDs e request codes (contratos §5.2): `idAlarmeEtapa = id*100 + índiceInvertido` (`:25`), `idAlarmeSaida = id*100+99` (`:26`), `idAlarmeAtivacaoChegada = id*100+97` (`:27`), confirmar saída `id` (`:28`), atraso `id+9999` (`:29`).
- `podeAgendarExato(context)` — `true` abaixo do Android 12; senão `canScheduleExactAlarms()` (`:31-35`).
- `agendar` (`:37-66`): sai se `!isEnabled` (`:38`); `calcularHorarios` (`:39-44`); etapas via `NotificationScheduler.scheduleAlarm` (`:47-55`, `NotificationScheduler.kt:43-81`); saída via `agendarSaida` (`:57`, `:83-118`), alvo `AlarmReceiver` com extras de título, mensagem, `EXTRA_NOTIFICATION_ID`, ação "Já Saí", tipo, itens e `compromissoId` (`:100-110`); atraso `CHECK_LATE` (`:58`, `:121-132`); ativação `ATIVAR_CHEGADA` se houver coordenadas e `fimDaJanelaDeChegada > agora` (`:62-65`, `:134-145`). O comentário em `:60-61` registra que um início de período já passado "dispara na hora".
- `agendarExato` (`:147-155`): sem permissão, só `Log.e` e retorna; senão `setExactAndAllowWhileIdle(RTC_WAKEUP)`. As etapas repetem a checagem em `NotificationScheduler.kt:63-73`.
- `cancelar(context, id, numEtapas)` (`:68-76`): etapas `0 until numEtapas+5`, saída, atraso e ativação. `cancelarVerificacaoAtraso` (`:78-81`).
- Todos os `PendingIntent` usam `FLAG_IMMUTABLE or FLAG_UPDATE_CURRENT`; mesmo request code + mesmo Intent → substitui o alarme existente.

### Regras puras (`notifications/HorariosLembrete.kt`)

- `HorariosLembrete(inicioEtapas: List<Long>, saida, verificacaoAtraso, inicioPreparo)` (`:12-17`); horários em `Long` epoch ms.
- `calcularHorarios(...)` (`:19-41`): `saida = evento − (deslocamento+segurança)`; etapas encadeadas de trás para frente; `verificacaoAtraso = saida + 10 min` (`MINUTOS_VERIFICACAO_ATRASO`, `:6`); `inicioPreparo` = início da 1ª etapa ou a saída, se não houver etapas.
- `fimDaJanelaDeChegada(evento) = evento + 2 h` (`:44-47`); `estaNaJanelaDeChegada(agora, horarios, evento)` (`:49-50`); `inicioMonitoramentoChegada(horarios) = inicioPreparo` (`:54`).

### Exibição e checagens no disparo

- `AlarmReceiver.onReceive` (`AlarmReceiver.kt:30-49`): lê só extras; `notificationId` padrão `currentTimeMillis`. Tipo `PADRAO` → notificação com "Já Saí" (`:96-138`); demais → full-screen para `ReminderActivity`, `ongoing`, `autoCancel=false` (`:51-94`). Sem acesso ao banco.
- `CHECK_LATE` (`NotificationActionReceiver.kt:78-101`): `getCompromissoById`; notifica se existir e `!confirmouSaida` (`:85`); não checa `isEnabled` nem `confirmouChegada`; sem `goAsync`; notificação `id*100+98` (`:98`).
- `ATIVAR_CHEGADA` (`NotificationActionReceiver.kt:60-75`): `goAsync` + IO; `getCompromissoComEtapasById`; se `!confirmouChegada` chama `addGeofenceChegada` (`:66-68`). Não checa ativo, período nem coordenadas aqui.

### Detecção de chegada

- `CompromissoGeofenceManager.addGeofenceChegada(compromisso, etapas)` (`CompromissoGeofenceManager.kt:37`): retorna se não inicializado (`:38`), `!isEnabled` (`:39`) ou lat/lng = 0.0 (`:42-45`); expiração `fimDaJanela − agora`, retorna se ≤ 0 (`:48-52`); raio 200 m (`:20`), requestId `compromisso_chegada_<id>` (`:55`), só ENTER, `INITIAL_TRIGGER_ENTER` (`:62,66`); checa só `ACCESS_FINE_LOCATION`, falhas só em log (`:72-82`). O parâmetro `etapas` não é usado. Reregistrar com o mesmo requestId não remove antes; a substituição depende do Play Services.
- `GeofenceBroadcastReceiver.handleCompromissoChegada` (`GeofenceBroadcastReceiver.kt:55-108`): `goAsync`+IO; sai se `!isEnabled` ou `confirmouChegada` (`:67`); fora do período ignora e mantém a geofence (`:75-79`); dentro: `confirmarSaida` + `confirmarChegada` (`:84-85`), `CompromissoAlarmScheduler.cancelar` (`:88`), `cancel(idSaida)` fecha "HORA DE SAIR" (`:89`), notifica "Você chegou!" `id*100+999` (`:92-100`), remove a geofence (`:103`).
- **Ordem saída × chegada:** sem etapas, `inicioPreparo == saida`, logo ativação e saída têm o mesmo instante; a saída é agendada antes (`CompromissoAlarmScheduler.kt:57` antes de `:64`). Com horários passados, ambos disparam imediatamente e a saída aparece antes de a entrada no raio ser reportada. O que existe depois da chegada é fechar a notificação de saída (`GeofenceBroadcastReceiver.kt:89`) e o `CHECK_LATE` respeitar `confirmouSaida`.

### Automações com local

- `GeofenceManager.addOrUpdateGeofence(setting)` (`services/geofence/GeofenceManager.kt:36-47`): registra se `isEnabled && isLocationEnabled && lat/lng != null`; senão `remove`. Retorna com log se não inicializado (`:37-40`). Raio 100 m (`:17`), `NEVER_EXPIRE` (`:58`), ENTER+EXIT (`:61`), `INITIAL_TRIGGER_ENTER` (`:66`), requestId = `setting.id` (`:52`); checa só `ACCESS_FINE_LOCATION` (`:72`).
- Registro hoje só em `PomodoroViewModel.updateSchedules` (`ui/pomodoro/PomodoroViewModel.kt:243-256`), chamado por salvar, toggle, excluir e reativar; usa `getAllAutomationSettingsOnce()` (todas, inclusive arquivadas, que têm `isEnabled = 0`, `AppDao.kt:88,90`). `PomodoroWorker.kt:68-73` remove a geofence de automações de uso único arquivadas.
- Entrada: `GeofenceBroadcastReceiver.handlePomodoroGeofence` grava `at_location_<id>` em `geofence_status` (`:115-120`) e enfileira `OneTimeWorkRequest<PomodoroWorker>` com tag `pomodoro_automation_<id>` (`:122-130`); "Hora de Focar!" vem de `PomodoroWorker.showStartNotification` (`workers/PomodoroWorker.kt:108`).
- Entidade `AutomationSetting` (`data/AutomationSetting.kt:7-33`): `isEnabled`, `isTimeEnabled`, `isLocationEnabled`, `isRepeatEnabled`, `isCompleted`, `latitude`/`longitude` (anuláveis), `cycles`.

### Dados e infraestrutura

- `AppDao.kt`: `getCompromissoById` (`:25`), `getCompromissoComEtapasById` (`@Transaction`, `:29`), `getAllCompromissosComEtapas(): Flow` (`:31-33`, a única listagem), `confirmarSaida`/`confirmarChegada` (`:37,40`), `deleteExpiredCompromissos` (`:43`); `getActiveAutomationSettings` (`:85`), `getAllAutomationSettingsOnce` (`:88`).
- `Compromisso` (`data/Compromisso.kt:9-31`): `isEnabled`, `dataHoraEvento`, `tempoDeslocamentoMinutos`, `tempoDeSegurancaMinutos`, `diasRepeticao`, `deleteAfterComplete`, `latitudeDestino`/`longitudeDestino` (padrão 0.0), `confirmouSaida`, `confirmouChegada`, `notificationType`. `CompromissoComEtapas` traz `etapas` e `itensLembrete` (`:33-47`).
- Manifest: `android:name=".AgoraVaiApplication"` (`:24`); permissões incluem `POST_NOTIFICATIONS`, `SCHEDULE_EXACT_ALARM`, `USE_EXACT_ALARM`, `ACCESS_BACKGROUND_LOCATION` (`:5-21`); receivers `AlarmReceiver` (`exported=false`), `GeofenceBroadcastReceiver` (`exported=true`), `NotificationActionReceiver` (`exported=false`) (`:49-59`).
- `AgoraVaiApplication.onCreate` inicializa só os dois managers de geofence (`AgoraVaiApplication.kt:13-14`). Canal `agora_vai_channel`, `IMPORTANCE_HIGH`, criado só em `MainActivity.kt:120` (`NotificationScheduler.kt:14-26`).

## Fluxo de dados relevante

```
Hoje (reagendamento só pela UI):
salvarCompromisso / toggleCompromisso (CompromissoViewModel.kt:143-236)
  → CompromissoAlarmScheduler.agendar → AlarmManager (etapas, saída, atraso, ativação)
ativação (inicioPreparo) → NotificationActionReceiver.ATIVAR_CHEGADA → addGeofenceChegada
  → [Play Services, assíncrono] ENTER → GeofenceBroadcastReceiver.handleCompromissoChegada
saída → AlarmReceiver (só extras) → notificação / ReminderActivity
atraso → CHECK_LATE → se !confirmouSaida → "Você está atrasado!"
updateSchedules (PomodoroViewModel.kt:243-256) → GeofenceManager.addOrUpdateGeofence

Reboot/atualização: nenhum ponto de entrada; alarmes e geofences somem.
```

## Padrões e convenções a seguir

- **Receiver com Room**: `val pendingResult = goAsync(); CoroutineScope(Dispatchers.IO).launch { try { AppDatabase.getDatabase(context).appDao()… } finally { pendingResult.finish() } }` — `NotificationActionReceiver.kt:60-75`, `GeofenceBroadcastReceiver.kt:55-108`. Sem `catch`; logs `Log.d/e` com tag fixa (`"AgoraVai-Debug"`, `"AgoraVai-Geofence"`).
- **Chamar o agendador a partir de `CompromissoComEtapas`**: `agendar(ctx, c.compromisso, c.etapas, c.itensLembrete.map { it.nomeItem })` — `CompromissoViewModel.kt:233`.
- **Regras puras**: funções top-level, `Long` epoch ms, `agora` como parâmetro; constantes `const val` com comentário citando o ID — `HorariosLembrete.kt:6,19-54`.
- **Testes**: JUnit 4, mesmo pacote; helper `private fun em(dia, h, m)` em UTC; nome entre crases começando pelo ID sem ponto (`` `LEMB-09c entrada no dia anterior e ignorada` ``) — `HorariosLembreteTest.kt:8-54`, `JanelaChegadaTest.kt:10-57`.
- **Abrir a permissão de alarme exato**: `Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}"))` com guarda `SDK_INT >= S` — `ui/compromissos/AddCompromissoScreen.kt:70-71`; variação com fallback para as configurações do app em `ui/permissions/PermissionsScreen.kt:144-145`. Texto reutilizável `MENSAGEM_PERMISSAO_ALARME` em `CompromissoViewModel.kt:45`.
- **Notificações**: `setSmallIcon(R.drawable.ic_launcher_foreground)`, `PRIORITY_HIGH`, `setAutoCancel(true)` (`GeofenceBroadcastReceiver.kt:92-100`); `contentIntent` com `PendingIntent.getActivity(..., FLAG_UPDATE_CURRENT or FLAG_IMMUTABLE)` (`AlarmReceiver.kt:71-86`).
- **Contratos**: `EXTRA_*` top-level em `AlarmReceiver.kt:13-24`; ações em `enum class Actions` dentro do receiver (`NotificationActionReceiver.kt:106-108`); IDs/request codes como funções em `CompromissoAlarmScheduler.kt:25-29`.

## Pontos de integração prováveis

- `AndroidManifest.xml:5-21,49-59` — permissões e receivers.
- `CompromissoAlarmScheduler.agendar` (`:37-66`) — único caminho de agendamento; hoje sem filtro de passado.
- `AlarmReceiver.kt:30-49` — exibição da saída, sem consulta ao banco.
- `NotificationActionReceiver.kt:60-101` — `ATIVAR_CHEGADA` e `CHECK_LATE`.
- `GeofenceManager.addOrUpdateGeofence` (`:36`) e `AppDao.getActiveAutomationSettings` (`:85`).
- `AppDao.kt:31-33` — só `Flow` para listar compromissos.
- `AgoraVaiApplication.kt:13-14` e `NotificationScheduler.createNotificationChannel` (`:14-26`).

## Restrições e riscos observados

- **Horários no passado disparam na hora**: qualquer reagendamento via `agendar` hoje faria etapas e atrasos antigos tocarem imediatamente (contrário a RF-002), e a saída perdida tocaria mesmo depois do evento (contrário a RF-002a).
- **Corrida saída × chegada (CA-1.7)**: se a saída já passou e o usuário está no destino, `agendar` dispararia "HORA DE SAIR" na hora, enquanto a chegada depende de um ENTER assíncrono do Play Services. `AlarmReceiver` não consulta `confirmouChegada`. Nas condições do B5 (aprovado), a ativação ocorria minutos antes da saída.
- **Canal de notificação**: após reiniciar, os canais persistem; numa instalação nova sem o app ter sido aberto, o canal não existe (só criado em `MainActivity`). Relevante para o aviso "Seus lembretes estão pausados" vindo de receiver.
- **Permissão**: `agendarExato` sem permissão só loga (`:147-152`); as etapas também (`NotificationScheduler.kt:63-73`). `GeofenceManager` e `CompromissoGeofenceManager` checam só `ACCESS_FINE_LOCATION`, falhas só em log.
- **`BOOT_COMPLETED`** exige `RECEIVE_BOOT_COMPLETED` e receiver `exported="true"` (broadcast protegido); `MY_PACKAGE_REPLACED` é entregue só ao próprio app. Receiver tem ~10 s; o app não é `directBootAware`, então o boot chega após o primeiro desbloqueio. Alguns fabricantes exigem "início automático" (orientação em `PermissionsScreen`, SIS-05).
- `toggleCompromisso` não reseta `confirmouSaida`/`confirmouChegada` ao religar (`CompromissoViewModel.kt:232`).
- `GeofenceBroadcastReceiver` é `exported=true` (DT-11).

## Contexto histórico

- `docs/sdd/specs/001-lembrete-de-saida/plano.md` — criou `CompromissoAlarmScheduler` e manteve os request codes de §5.2 para que reagendar substitua alarmes existentes.
- `docs/sdd/specs/002-deteccao-de-chegada/plano.md` (Fase 5, Revisão 2) — a chegada passou a ser ativada por alarme no início do preparo; o comentário em `CompromissoAlarmScheduler.kt:60-61` registra que religar no meio do período dispara a ativação na hora.
- **Stash `stash@{0}`** (2026-10-26, GitHub Desktop, sobre `main` `4f34bff`): implementação anterior e paralela, fora do fluxo SDD, com `BootReceiver` (`BOOT_COMPLETED` + `MY_PACKAGE_REPLACED`, `goAsync`, `getCompromissosAtivosComEtapas()`, `getActiveAutomationSettings()`), `AgoraVaiApp`, `CompromissoAlarmScheduler.reprogramar` (descarta compromisso cujo atraso já passou), `proximaOcorrencia` (recorrência, DT-04) e um `agendarExato` que ignora horários passados. Não aplicado; anterior às specs 001/002.

## Referências de código

- `notifications/CompromissoAlarmScheduler.kt:25-155` — agendador.
- `notifications/HorariosLembrete.kt:6-54` — regras puras.
- `notifications/AlarmReceiver.kt:13-138` — extras e exibição.
- `notifications/NotificationScheduler.kt:14-81` — canal e alarmes de etapa.
- `services/NotificationActionReceiver.kt:26-108` — ações.
- `services/geofence/CompromissoGeofenceManager.kt:20-89`, `GeofenceManager.kt:17-90`, `GeofenceBroadcastReceiver.kt:28-133`.
- `ui/compromissos/CompromissoViewModel.kt:45,143-241`, `ui/pomodoro/PomodoroViewModel.kt:201-256`, `workers/PomodoroWorker.kt:48-108`.
- `data/AppDao.kt:25-94`, `data/Compromisso.kt:9-47`, `data/AutomationSetting.kt:7-33`.
- `AndroidManifest.xml:5-68`, `AgoraVaiApplication.kt:13-14`, `MainActivity.kt:120`.
- Verificação manual: `adb reboot`, `adb shell dumpsys alarm | grep agoravai`, `adb install -r`.

## Questões em aberto

- Se reregistrar uma geofence com o mesmo requestId substitui a anterior é comportamento do Play Services, não verificado no código (afeta idempotência, RNF-001).
- Tempo real do reagendamento com 50 compromissos (RNF-002) só será medível com o código.
