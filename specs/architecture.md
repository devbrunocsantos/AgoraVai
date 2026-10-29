# Arquitetura · Agora Vai

| Campo | Valor |
|---|---|
| Projeto | Agora Vai (`com.brunocsantos.agoravai`) |
| Plataforma | Android nativo (Kotlin + Jetpack Compose) |
| Versão do app | 1.5 (`versionCode 6`) |
| Versão do documento | 1.1 — 2026-10-26 |
| Autor | Bruno C. Santos |
| Status | *As-is*: descreve o design técnico como ele está hoje no código |

> Este documento descreve **como** o sistema é construído. **O que** o sistema
> deve fazer está nas specs vivas em [`capabilities/`](capabilities); os
> princípios estão na [`constitution.md`](constitution.md) e o fluxo de
> mudança em [`README.md`](README.md) (Spec-Driven Development + RPI).

---

## 1. Visão geral

### 1.1 Propósito
O **Agora Vai** é um assistente de pontualidade e foco pensado para pessoas que
têm dificuldade com gestão de tempo (com atenção especial a pessoas com TDAH).
Ele resolve dois problemas:

1. **Sair na hora certa para um compromisso** — calcula, a partir da rota real
   (Google Directions), quando o usuário precisa começar a se arrumar e quando
   precisa sair, e dispara lembretes progressivamente mais "insistentes".
2. **Manter o foco** — um timer Pomodoro com ciclos configuráveis, meta diária
   e automações que sugerem iniciar uma sessão por horário e/ou ao chegar num
   local (geofence).

### 1.2 Escopo
Dentro do escopo: app Android single-module, dados 100% locais (Room +
SharedPreferences), integrações com Google Maps Platform (Directions, Places,
Geofencing/Fused Location) e Google Play Billing (atualmente em modo bypass).

Fora do escopo (hoje): backend próprio, conta de usuário, sincronização entre
dispositivos, widgets, Wear OS.

### 1.3 Glossário

| Termo | Significado |
|---|---|
| Compromisso | Evento com data/hora, origem e destino |
| Etapa / Preparativo | Tarefa de preparação com duração (ex.: "Tomar banho — 15 min") |
| Item de lembrete | Objeto a não esquecer (ex.: "Chaves") |
| Tempo de segurança | Folga em minutos somada ao deslocamento |
| Horário de saída | `dataHoraEvento − (deslocamento + segurança)` |
| Tipo de notificação | Nível de insistência do alarme de saída: `PADRAO`, `ALARME`, `INSISTENTE`, `DESAFIO` |
| Automação | Regra que sugere iniciar um Pomodoro por horário e/ou local |
| Ciclo / sessão de foco | Um período de foco do Pomodoro |

---

## 2. Requisitos

Os requisitos ficam nas specs vivas, com ID, formato EARS, critérios de aceite e status:

| Spec | Prefixo | Escopo |
|---|---|---|
| [`capabilities/compromissos.md`](capabilities/compromissos.md) | `COMP` | Cadastro, rota, etapas, itens, lista |
| [`capabilities/lembretes.md`](capabilities/lembretes.md) | `LEMB` | Alarmes de etapa, saída, atraso, chegada |
| [`capabilities/pomodoro.md`](capabilities/pomodoro.md) | `POMO` | Timer, fases, conclusão, progresso |
| [`capabilities/automacoes.md`](capabilities/automacoes.md) | `AUTO` | Automações por horário/local, histórico |
| [`capabilities/sistema.md`](capabilities/sistema.md) | `SIS` | Navegação, permissões, configurações, privacidade |

---

## 3. Arquitetura

### 3.1 Estilo
**MVVM** com Jetpack Compose, sem camada de repositório e sem injeção de
dependências: os ViewModels falam direto com o `AppDao` e com singletons de
infraestrutura (`object`). Estado reativo via `StateFlow`/`SharedFlow`.

```
┌──────────────────────────── UI (Compose) ─────────────────────────────┐
│ MainActivity ─ MainScreenWithBottomNav (navegação por flags booleanas)│
│  ├ CompromissosScreen / AddCompromissoScreen / AddressAutocompleteField│
│  ├ PomodoroScreen / AutomationSettingsScreen / HistoryScreen           │
│  ├ SettingsScreen / PermissionsScreen                                  │
│ ReminderActivity (tela cheia de alarme)                                │
└───────────────┬───────────────────────────────┬───────────────────────┘
                │ StateFlow                     │ StateFlow
     ┌──────────▼──────────┐         ┌──────────▼──────────┐
     │ CompromissoViewModel│         │  PomodoroViewModel  │
     └──┬────────┬─────────┘         └──┬──────┬───────┬───┘
        │        │                      │      │       │
   ┌────▼──┐ ┌───▼────────────┐   ┌─────▼─┐ ┌──▼─────┐ ┌▼──────────────┐
   │AppDao │ │Retrofit        │   │AppDao │ │Settings│ │BillingClient  │
   │(Room) │ │(Directions API)│   │       │ │Manager │ │Manager(bypass)│
   └───────┘ └────────────────┘   └───────┘ └────────┘ └───────────────┘
        │                               │
┌───────▼──── Agendamento / background ─▼────────────────────────────────┐
│ AlarmManager ─► AlarmReceiver ─► Notificação ou ReminderActivity       │
│ AlarmManager ─► NotificationActionReceiver (CHECK_LATE / CONFIRM_EXIT) │
│ Geofencing ─► GeofenceBroadcastReceiver ─► chegada / PomodoroWorker    │
│ WorkManager (periódico 24h) ─► PomodoroWorker ─► notificação "Iniciar" │
│ PomodoroService (foreground) ◄─► PomodoroTimerState (singleton)        │
└────────────────────────────────────────────────────────────────────────┘
```

### 3.2 Estrutura de pacotes

| Pacote | Responsabilidade |
|---|---|
| `data` | Entidades Room, `AppDao`, `AppDatabase` (singleton), `Converters` |
| `network` | Retrofit + modelos da Directions API |
| `notifications` | Canal de notificação, `CompromissoAlarmScheduler` (agenda e cancela todos os lembretes de um compromisso), `HorariosLembrete` (cálculo puro dos horários), `NotificationScheduler` (AlarmManager), `AlarmReceiver` e as constantes `EXTRA_*` |
| `services` | `PomodoroService`, `PomodoroScheduler` (WorkManager), estados globais (`PomodoroTimerState`, `FocusSessionState`, `PomodoroCompletionState`, `AppForegroundState`), `NotificationActionReceiver` |
| *(raiz)* | `AgoraVaiApplication` inicializa os managers de geofence para que funcionem em qualquer processo (receivers, workers); `MainActivity` |
| `services/geofence` | `GeofenceManager` (automações), `CompromissoGeofenceManager` (chegada), `GeofenceBroadcastReceiver` |
| `workers` | `PomodoroWorker` (avalia condições de uma automação) |
| `billing` | `BillingClientManager` (bypass), `SubscriptionManager` (não usado) |
| `ui/*` | Telas Compose, ViewModels, `ViewModelFactory`, tema |
| `utils` | `SettingsManager` (prefs do Pomodoro), `FraseMotivacionalManager`, `ResponsiveText` |

### 3.3 Stack

| Camada | Tecnologia |
|---|---|
| Linguagem / build | Kotlin 1.9.23, AGP 8.4.1, KSP 1.9.23-1.0.19, compilador Compose 1.5.13, Gradle 8.13, JVM target 1.8 (o `gradle/libs.versions.toml` existe mas **não é usado**) |
| UI | Compose BOM 2024.06.00, Material 3, material-icons-extended |
| Persistência | Room 2.6.1, SharedPreferences |
| Background | AlarmManager, WorkManager 2.9, Foreground Service (`dataSync`) |
| Localização | play-services-location 21.3 (Geofencing, Fused), Places 3.5, Geocoder |
| Rede | Retrofit 2.9 + Gson |
| Monetização | Play Billing 7.1 (desativado) |

---

## 4. Modelo de dados

Banco `agora_vai_database`, **versão 10**, `fallbackToDestructiveMigration()`,
`exportSchema = false`.

```
compromissos (1) ──< etapas           (FK compromissoId, ON DELETE CASCADE, índice)
             (1) ──< itens_lembrete   (FK compromissoId, ON DELETE CASCADE, índice)
automation_settings          (independente)
sessoes_pomodoro_concluidas  (independente)
```

**`compromissos`**

| Coluna | Tipo | Notas |
|---|---|---|
| id | Long PK auto | |
| titulo, origem, destino | String | endereços em texto livre |
| dataHoraEvento | Long (epoch ms) | horário de chegada desejado |
| tempoDeslocamentoMinutos | Int | resultado da Directions API |
| tempoDeSegurancaMinutos | Int | |
| diasRepeticao | String | CSV de `Calendar.DAY_OF_WEEK` (1=Dom…7=Sáb) |
| isEnabled, deleteAfterComplete | Boolean | |
| latitudeDestino, longitudeDestino | Double | via Geocoder; `0.0` = desconhecido |
| confirmouSaida, confirmouChegada | Boolean | |
| notificationType | `NotificationType` (String) | fallback `PADRAO` |

**`etapas`**: `id Int PK`, `compromissoId Long FK`, `descricao`, `duracaoMinutos`.
**`itens_lembrete`**: `id Int PK`, `compromissoId Long FK`, `nomeItem`.

**`automation_settings`**: `id Int PK`, `title`, `isEnabled`, `isTimeEnabled`,
`isLocationEnabled`, `isRepeatEnabled`, `isCompleted`, `hour`, `minute`,
`repeatDays Set<DayOfWeek>` (CSV de nomes), `placeId?`, `placeAddress?`,
`latitude?`, `longitude?`, `cycles`.

**`sessoes_pomodoro_concluidas`**: `id Int PK`, `timestampConclusao Long`.
Registros com mais de 24 h são apagados quando o `PomodoroViewModel` é criado.

**SharedPreferences**

| Arquivo | Chaves |
|---|---|
| `pomodoro_settings_prefs` | `focus_duration_mins`, `short_break_duration_mins`, `long_break_duration_mins`, `cycles_before_long_break`, `daily_goal`, `safety_time_mins` |
| `geofence_status` | `at_location_<automationId>` (Boolean) |
| `motivacional_prefs` | `data_frase`, `frase_do_dia` |
| `app_settings` | `has_been_asked_battery_optimization` |
| `subscription_prefs` | `is_premium_user` (não usado) |

---

## 5. Design detalhado dos fluxos

### 5.1 Salvar compromisso (`CompromissoViewModel.salvarCompromisso`)
1. Valida título/origem/destino e etapas.
2. Geocoder no destino → `latitudeDestino/longitudeDestino` (falha é tolerada).
3. `GET maps/api/directions/json?origin&destination&key` → `routes[0].legs[0].duration.value / 60`.
4. `inicio = dataHoraEvento − (deslocamento + segurança + Σetapas)`; se `inicio < agora` → erro.
5. Insere ou atualiza o compromisso; na edição apaga etapas/itens, remove geofence e cancela alarmes antigos.
6. Insere etapas e itens, registra geofence de chegada e agenda os alarmes (5.2).

### 5.2 Linha do tempo de alarmes de um compromisso

```
 início etapa 1   início etapa N      SAÍDA                 SAÍDA+10min        CHEGADA
      │                │                │                        │                 │
  alarme etapa     alarme etapa    "HORA DE SAIR"          CHECK_LATE         geofence 200 m
  (id*100+i)       (id*100+i)      (id*100+99)          (req. code id+9999)  ("Você chegou!")
```

`saída = dataHoraEvento − deslocamento − segurança`. As etapas são agendadas
percorrendo a lista ao contrário, subtraindo a duração de cada uma.

**Convenção de IDs (request codes / notification IDs)**, implementada em `CompromissoAlarmScheduler` (`idAlarmeEtapa`, `idAlarmeSaida` e os request codes privados). Não altere sem migrar os alarmes já agendados.

| Uso | ID |
|---|---|
| Alarme da etapa `i` (índice na lista invertida) | `compromissoId*100 + i` |
| Alarme "Hora de sair" | `compromissoId*100 + 99` |
| PendingIntent da ação "Já Saí" | `compromissoId` |
| Alarme de verificação de atraso | `compromissoId + 9999` |
| Notificação de atraso | `compromissoId*100 + 98` |
| Alarme de ativação da chegada (início do preparo) | `compromissoId*100 + 97` (ação `ATIVAR_CHEGADA`) |
| Notificação de chegada | `compromissoId*100 + 999` |
| Geofence de chegada | `"compromisso_chegada_<id>"` |
| Geofence de automação | `"<automationId>"` (só dígitos) |
| Notificações do Pomodoro | 1 (foreground), 2 (conclusão), 3 (transição) |
| Notificação de automação | `automationId` |

### 5.3 Disparo do lembrete (`AlarmReceiver`)
- `EXTRA_NOTIFICATION_TYPE == 0` (`PADRAO`) → notificação `BigTextStyle` com itens e ação opcional.
- Senão → notificação `CATEGORY_ALARM` com `fullScreenIntent` para `ReminderActivity`, que
  mostra itens e o mecanismo de dispensa do tipo e, ao confirmar, envia
  `CONFIRM_EXIT` ao `NotificationActionReceiver`.

### 5.3.1 Detecção de chegada (`GeofenceBroadcastReceiver`)
- Ativação: `CompromissoAlarmScheduler.agendar` agenda um alarme no início do preparo (`inicioMonitoramentoChegada`), se houver coordenadas e o período não tiver terminado. O alarme chega ao `NotificationActionReceiver` (`ATIVAR_CHEGADA`), que registra a geofence. Assim quem já está no destino tem a entrada inicial disparada **dentro** do período (Revisão 2).
- Registro (`CompromissoGeofenceManager.addGeofenceChegada`): raio de 200 m, só `ENTER`, `INITIAL_TRIGGER_ENTER`, expiração no fim do período de chegada (`fimDaJanelaDeChegada` = evento + 2 h). Não registra se não houver coordenadas, se o compromisso estiver desativado ou se o período já tiver terminado.
- Recebimento (`handleCompromissoChegada`, com `goAsync`): lê o compromisso com as etapas, e fora do período (`estaNaJanelaDeChegada`) ignora sem remover a geofence. Dentro do período: `confirmarSaida` + `confirmarChegada`, `CompromissoAlarmScheduler.cancelar`, fecha a notificação de saída, notifica a chegada (`id*100+999`) e remove a geofence.

### 5.4 Pomodoro (`PomodoroService`)
Máquina de estados (`PomodoroState`: `PARADO, FOCO, PAUSA_CURTA, PAUSA_LONGA`;
`TimerState`: `PARADO, RODANDO, PAUSADO`). O estado vive no singleton
`PomodoroTimerState`, compartilhado entre Service e UI. Laço de corrotina com
`delay(1000)` atualizando a notificação a cada segundo.

```
PARADO ──start──► FOCO ──fim──► (ciclo % ciclosParaPausaLonga == 0 ? PAUSA_LONGA : PAUSA_CURTA)
                   ▲                                   │
                   └──────────────── fim ──────────────┘
FOCO fim && ciclo >= meta  ──► conclusão ──► reset ──► PARADO
```
A meta vem de `EXTRA_CYCLES` (automação) ou de `daily_goal`.
`FocusSessionState` (SharedFlow) avisa o ViewModel para gravar a sessão.

### 5.5 Automações do Pomodoro
- Salvar/alternar/excluir chama `updateSchedules`: `WorkManager.cancelAllWork()` e
  reagenda todas as automações ativas (periódico de 24 h com atraso inicial até
  `hour:minute`) + registra/remove geofences (raio 100 m, ENTER|EXIT).
- `GeofenceBroadcastReceiver` grava `at_location_<id>` e, no ENTER, enfileira um
  `PomodoroWorker` one-shot.
- `PomodoroWorker` checa: habilitada, dia da semana permitido, condição de local;
  então mostra "Hora de Focar!" com ações `START`/`DISMISS`. Sem repetição →
  `markAsCompleted` e remove geofence.

### 5.6 Navegação
Não usa Navigation Compose. `MainScreenWithBottomNav` mantém flags booleanas
(`showSettingsScreen`, `showAddCompromissoScreen`, …) e um `BackHandler`
que as desliga em ordem de prioridade. Deep link simples via extra
`destination_screen = "pomodoro"`.

---

## 6. Integrações externas

| Serviço | Uso | Observações |
|---|---|---|
| Google Directions API | Tempo de deslocamento | Chamada direta do cliente com a chave no query string; sem `departure_time` (não considera trânsito) e sem `mode` (padrão: carro). |
| Google Places SDK | Autocomplete de endereço (compromissos) e de local (automações, restrito a BR) | Inicializado em `MainActivity` se houver chave. |
| Geocoder (Android) | Endereço → coordenadas e reverso | API deprecated em < 33 usada com `@Suppress`. |
| Geofencing / Fused Location | Chegada e automações por local | Exige `ACCESS_BACKGROUND_LOCATION`. |
| Play Billing | Assinatura premium | **Desativado** (`isPremium = true` forçado). |

---

## 7. Permissões

`POST_NOTIFICATIONS`, `SCHEDULE_EXACT_ALARM`, `USE_EXACT_ALARM`, `INTERNET`,
`ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`, `ACCESS_BACKGROUND_LOCATION`,
`FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_DATA_SYNC`,
`REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`, `USE_FULL_SCREEN_INTENT`, `WAKE_LOCK`,
`SYSTEM_ALERT_WINDOW`.

---

## 8. Build e configuração

1. Criar `local.properties` com `MAPS_API_KEY="<chave>"`, **com aspas**, porque o valor é inserido cru no `BuildConfig` (Directions + Places habilitadas). Fora do Android Studio, inclua também `sdk.dir=<caminho do SDK>`.
2. `./gradlew assembleDebug` / `./gradlew bundleRelease`.
3. Testes: `./gradlew test` e `./gradlew connectedAndroidTest` (hoje só existem os exemplos do template).

---

## 9. Dívida técnica e riscos conhecidos

Itens encontrados na leitura do código. Cada DT está ligado a um requisito ⚠️/⬜ nas specs ou a um princípio da constituição.

| # | Severidade | Problema | Onde |
|---|---|---|---|
| ~~DT-01~~ | **Alta** | ✅ *Resolvido em [`deteccao-de-chegada`](changes/2026-10-26-deteccao-de-chegada/), verificado em aparelho em 2026-10-29 (B5, B6).* `CompromissoGeofenceManager.initialize()` nunca é chamado, então `addGeofenceChegada` retorna cedo e **a detecção de chegada (LEMB-09) nunca funciona**. Além disso, os dois managers de geofence só são inicializados pela `MainActivity`, então receivers e workers em segundo plano não conseguem remover geofences (AUTO-08). | `MainActivity.onCreate`, managers de geofence |
| ~~DT-02~~ | **Alta** | ✅ *Resolvido em [`lembrete-de-saida`](changes/2026-10-26-lembrete-de-saida/), verificado em aparelho em 2026-10-28.* Chaves de extra divergentes: o ViewModel grava `"EXTRA_TITLE"`, `"EXTRA_MESSAGE"`, `"EXTRA_NOTIFICATION_ID"`, `"EXTRA_HAS_ACTION"`, `"EXTRA_ACTION_INTENT"`, `"EXTRA_ACTION_TITLE"`, mas o `AlarmReceiver` lê as constantes `"extra_title"`, `"extra_message"`… Resultado: o lembrete de saída sai com título/mensagem padrão, ID aleatório e **sem o botão "Já Saí"**. | `CompromissoViewModel.agendarAlarmeSaidaComAction`, `AlarmReceiver` |
| DT-03 | **Alta** | `fallbackToDestructiveMigration` + `exportSchema=false`: qualquer mudança de schema **apaga todos os dados do usuário**. Os schemas em `app/schemas` (v3, v4) estão desatualizados (DB está na v10). | `AppDatabase` |
| DT-04 | Alta | Repetição de compromissos (COMP-13) é só armazenada: os alarmes são únicos e não são reagendados para a próxima ocorrência. | `CompromissoViewModel.agendarAlarmes` |
| DT-05 | Alta | Não existe receiver de `BOOT_COMPLETED`/`TIME_SET`: alarmes e geofences somem após reiniciar o aparelho. | `AndroidManifest.xml` |
| ~~DT-06~~ | Média | ✅ *Resolvido em [`lembrete-de-saida`](changes/2026-10-26-lembrete-de-saida/), verificado em aparelho em 2026-10-28.* Cancelamento do alarme de atraso usa um `Intent` sem `action`, que não casa com o `PendingIntent` original — o alarme CHECK_LATE não é cancelado ao excluir/desativar (mitigado porque ele checa `confirmouSaida`, mas dispara para compromissos excluídos se o ID for reaproveitado). `CONFIRM_EXIT` tenta cancelar o ID `*100+98`, que nunca foi agendado. | `cancelarTodosAlarmes`, `NotificationActionReceiver` |
| DT-07 | Média | `PomodoroScheduler.cancelAll` chama `WorkManager.cancelAllWork()`, cancelando **qualquer** trabalho do app, não só automações. | `PomodoroScheduler` |
| DT-08 | Média | Gravação de sessões de foco depende do `PomodoroViewModel` estar vivo para coletar o `SharedFlow`; com o app fechado, focos concluídos não são contados. | `FocusSessionState`, `PomodoroViewModel` |
| DT-09 | Média | Billing em modo bypass (premium sempre ativo) e `SubscriptionManager` sem uso — precisa de decisão antes de publicar. | `billing/` |
| DT-10 | Média | Chave da Directions API exposta no cliente; ideal restringir por pacote/SHA-1 ou usar proxy. Directions sem `departure_time=now` ignora trânsito. | `network/`, `build.gradle.kts` |
| DT-11 | Baixa | `GeofenceBroadcastReceiver` é `exported=true` sem necessidade. | `AndroidManifest.xml` |
| DT-12 | Baixa | IDs `compromissoId*100 + i` estouram `Int` para IDs grandes e colidem se houver ≥ 98 etapas. | `CompromissoViewModel` |
| DT-13 | Baixa | `app/release/app-release.aab` está versionado (`.gitignore` tem `/buildapp-release.aab`, typo). `libs.versions.toml` existe mas quase não é usado; `lifecycle-runtime-compose` declarado duas vezes; `minifyEnabled=false` no release. | build |
| DT-14 | Baixa | Sem testes automatizados; lógica de negócio (cálculo de horários, máquina de estados) acoplada a Android, difícil de testar. | geral |
| DT-15 | Baixa | `TimePickerDialog` duplicado (`MainActivity.kt` e `AddCompromissoScreen.kt`); arquivos com várias declarações numa linha só. | UI |
| ~~DT-16~~ | Média | ✅ *Resolvido em [`lembrete-de-saida`](changes/2026-10-26-lembrete-de-saida/), verificado em aparelho em 2026-10-28.* Ao confirmar a saída na `ReminderActivity`, o broadcast `CONFIRM_EXIT` vai sem `EXTRA_NOTIFICATION_ID` e a notificação de tela cheia (`ongoing`) continua na bandeja (LEMB-07). | `ReminderActivity.confirmarSaida`, `AlarmReceiver` |
| DT-17 | Média | 🧪 *Corrigido em [`lembrete-de-saida`](changes/2026-10-26-lembrete-de-saida/), aguardando verificação em aparelho.* Sem permissão de alarme exato (Android 12+), `NotificationScheduler.scheduleAlarm` só registra no log, enquanto os helpers do ViewModel chamam `setExactAndAllowWhileIdle` no `else` e lançam `SecurityException`, que vira "Erro ao salvar." com o compromisso já gravado (LEMB-10). | `NotificationScheduler`, `CompromissoViewModel` |
| DT-22 | Média | A lista de compromissos usa `contentPadding` vertical de 8 dp. O FAB "+" fica por cima do fim da lista, e no último card expandido cobre os botões editar e excluir (alinhados à direita) (COMP-12). | `CompromissosScreen` (`LazyColumn`) |
| DT-23 | Média | `PomodoroViewModel.onLocationClick` põe `_launchAutocomplete = true` e ninguém volta para `false`. O `StateFlow` não reemite valor igual, então a busca de endereço (Places) só abre no primeiro toque da sessão (AUTO-03). | `PomodoroViewModel`, `MainActivity` (coletor de `launchAutocomplete`) |
| DT-21 | **Alta** | A Directions API faz correspondência aproximada de endereços: um destino inexistente ou ambíguo pode virar uma rota válida para outro lugar, e o app aceita sem avisar (`geocoded_waypoints[].partial_match` é ignorado). Além disso, `duration.value / 60` arredonda para baixo: percursos com menos de 1 min aparecem como 0. Resultado visto em aparelho: endereço inexistente salvo com "0 min" de percurso, e o lembrete de saída tocaria tarde demais (P2). | `CompromissoViewModel.salvarCompromisso`, `network/DirectionsResponse.kt` |
| DT-20 | Média | 🧪 *Corrigido em [`deteccao-de-chegada`](changes/2026-10-26-deteccao-de-chegada/), aguardando verificação em aparelho.* `CompromissoViewModel.tempLatDestino/tempLngDestino` são campos do ViewModel que não são zerados entre salvamentos. Se o Geocoder falhar, o compromisso herda as coordenadas do anterior e a detecção de chegada (LEMB-09) usaria o destino errado. | `CompromissoViewModel.salvarCompromisso` |
| DT-19 | Alta | 🧪 *Corrigido em [`lembrete-de-saida`](changes/2026-10-26-lembrete-de-saida/), aguardando verificação em aparelho.* A notificação de tela cheia não tem `contentIntent`. No Android 14+, sem `USE_FULL_SCREEN_INTENT`, o lembrete vira uma notificação que não abre nada, e o usuário não consegue confirmar a saída (LEMB-04). | `AlarmReceiver.showFullScreenAlarm` |
| DT-18 | Média | `PomodoroService.onStartCommand` recalcula a meta a cada comando: PAUSE/START sem `EXTRA_CYCLES` trocam a meta da automação pela meta diária (POMO-05). | `PomodoroService` |

---

## 10. Decisões de arquitetura (ADRs resumidos)

| ADR | Decisão | Motivo | Consequência |
|---|---|---|---|
| ADR-01 | Dados 100% locais (Room) | Simplicidade, privacidade | Sem sync/backup |
| ADR-02 | `AlarmManager` exato para lembretes | Precisão ao minuto mesmo em Doze | Exige permissão de alarme exato (Android 12+) |
| ADR-03 | Foreground Service para o timer | Timer sobrevive com app em background | Notificação persistente obrigatória |
| ADR-04 | Estado global em `object`s com `StateFlow` | Compartilhar estado Service ↔ UI sem binding | Estado se perde se o processo morrer |
| ADR-05 | Navegação por flags no Compose | Rapidez de implementação | Sem back stack real nem deep links robustos |
| ADR-06 | Automação Pomodoro sugere (notificação) em vez de iniciar sozinha | Evitar iniciar foco sem o usuário querer | Exige interação |

Novas decisões são registradas aqui a partir da fase **Plan** (constituição P8).

---

## 11. Manutenção deste documento

- É atualizado na fase **Implement** de cada mudança que altere componentes, dados, IDs ou decisões.
- Ao corrigir um DT no código, marque-o com 🧪 e o link da mudança enquanto algum critério aguarda verificação. Quando todos os critérios passarem, risque-o (~~DT-xx~~) e atualize o status do requisito na spec viva para ✅.

| Versão | Data | Mudança |
|---|---|---|
| 1.0 | 2026-10-25 | Versão inicial, engenharia reversa do código (app 1.5). |
| 1.1 | 2026-10-26 | Requisitos movidos para `specs/capabilities/` (Spec-Driven Development); DT-16 a DT-18 adicionados. |
| 1.2 | 2026-10-26 | DT-19 adicionado, DT-01 ampliado, versões reais do build corrigidas (§3.3, §8). |
| 1.3 | 2026-10-26 | Mudança `lembrete-de-saida`: `CompromissoAlarmScheduler` e `HorariosLembrete` (§3.2, §5.2); DT-02, DT-06, DT-16, DT-17 e DT-19 corrigidos (🧪); DT-20 adicionado. |
| 1.4 | 2026-10-27 | Mudança `deteccao-de-chegada`: `AgoraVaiApplication`, fluxo de chegada (§5.3.1); DT-01 e DT-20 corrigidos (🧪). |
| 1.5 | 2026-10-28 | Resultados dos testes em aparelho: DT-02, DT-06 e DT-16 resolvidos; DT-21 adicionado. |
| 1.6 | 2026-10-28 | Revisão 2 da chegada: alarme de ativação no início do preparo (§5.2, §5.3.1). |
| 1.7 | 2026-10-29 | DT-22 e DT-23 adicionados (ajustes anotados nos testes em aparelho). |
| 1.8 | 2026-10-29 | DT-01 resolvido (chegada verificada em aparelho). |
