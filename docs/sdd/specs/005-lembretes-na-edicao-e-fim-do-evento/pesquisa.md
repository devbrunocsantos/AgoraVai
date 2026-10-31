---
spec: docs/sdd/specs/005-lembretes-na-edicao-e-fim-do-evento/spec.md
data: 2026-10-30
commit: 0cbe75fad96d4c653d42b08af74a304ffe6b6807
branch: ccr-80e3a1a2-ocxhin
pesquisador: gerente-de-projeto + analista-de-codigo (×3), buscador-de-padroes
status: completa
---

# Pesquisa: lembretes durante a edição e compromisso encerrado

> Caminhos relativos a `app/src/main/java/com/brunocsantos/agoravai/`, salvo indicação.

## Pergunta de pesquisa

Como o app hoje (1) abre, salva e fecha o formulário de edição e sabe se uma tela está visível; (2) agenda e dispara etapas, saída e atraso, e onde um disparo poderia ser segurado; (3) representa "desligado", "passado" e "chegada", e o que depende de `isEnabled`; (4) aplica a regra de lembretes passados da 003; (5) quais padrões de código e teste seguir.

## Resumo

- O formulário não é uma rota: é uma flag `showAddCompromissoScreen` (`remember`, não `rememberSaveable`) em `MainActivity.kt:254`. Sair sem salvar = `BackHandler` (`MainActivity.kt:267-278`) ou ícone voltar (`:346-349`), ambos chamando `resetAddCompromissoState()`. Não existe noção de "tela visível"; só `AppForegroundState` (Activity `onStart`/`onStop`, `MainActivity.kt:113-116`), lido apenas pelo Pomodoro.
- **O `AlarmReceiver` não consulta estado nenhum** (`notifications/AlarmReceiver.kt:30-49`): mostra o que vier nos extras. Etapas e saída são alarmes do AlarmManager com `setExactAndAllowWhileIdle`; o atraso passa pelo `NotificationActionReceiver` (`CHECK_LATE`), que relê o banco e checa `confirmouSaida`.
- `agendar` **não filtra horários passados** (`CompromissoAlarmScheduler.kt:47-69`) — é a causa do "despejo" ao religar (`CompromissoViewModel.kt:222-234`).
- A regra da 003 já existe como função pura: `planoDeReagendamento(..., reinicio)` (`notifications/HorariosLembrete.kt:81-119`), aplicada por `CompromissoAlarmScheduler.reagendar(plano)` (`:75-89`); hoje só o `Reagendador` (boot) a usa.
- **`isEnabled = false` desliga tudo**: `agendar` sai cedo (`CompromissoAlarmScheduler.kt:48`), a geofence não registra (`CompromissoGeofenceManager.kt:40`), o receiver da geofence ignora (`GeofenceBroadcastReceiver.kt:61`) e o Reagendador filtra (`AppDao.kt:33`, `HorariosLembrete.kt:92`). O "encerrado" da spec (RF-007) mantém atraso e chegada, então não equivale a `isEnabled = false` como o código está.
- Não há ticker na lista: ela só muda quando o Room emite (`CompromissoViewModel.kt:77-78`). O esmaecimento depende só de `isEnabled` (`CompromissosScreen.kt:112-122`).
- Banco: versão 10, `fallbackToDestructiveMigration()` (`data/AppDatabase.kt:36`). Qualquer coluna nova apaga os dados (P3, DT-03).

## Descobertas detalhadas

### 1. Formulário de criação/edição

- Navegação por flags em `MainScreenWithBottomNav` (`MainActivity.kt:251-255`); o formulário é exibido quando `showAddCompromissoScreen` (`:336`).
- Edição: ícone Editar no card (`ui/compromissos/CompromissosScreen.kt:146`) → `onNavigateToEdit` → `iniciarEdicao(compromisso)` + flag (`MainActivity.kt:406-409`). `iniciarEdicao` copia o item da lista (sem recarregar do DAO) para `_addCompromissoUiState` (`CompromissoViewModel.kt:121-138`); `isEnabled`, coordenadas e `confirmou*` não vão para o estado. Criação × edição: `uiState.id != null` (`AddCompromissoScreen.kt:49`; `CompromissoViewModel.kt:198`).
- `CompromissoViewModel` tem escopo de Activity (`MainActivity.kt:67`) e é compartilhado por lista e formulário (`:338-340`, `:400-401`).
- **Salvar** (`CompromissoViewModel.kt:141-220`), recusas em ordem:
  1. campos em branco → `"Preencha todos os campos."` (`:143`);
  2. etapa com duração inválida → `isError` sem mensagem (`:145-149`);
  3. sem alarme exato → `pedirPermissaoAlarme` (snackbar com ação, `AddCompromissoScreen.kt:63-75`) (`:151`);
  4. (assíncrono) Geocoder (`:157-163`) e Directions; sem rota → `"Rota não encontrada."` (`:217`);
  5. `inicioPreparo < agora` → `"Impossível! Atraso de N min."` (`:172-178`);
  6. exceção → `"Erro ao salvar."` (`:218`).
- Gravação na edição, nesta ordem: `updateCompromisso` → apagar etapas e itens → `removeGeofence(id)` → `cancelar(context, id, 10)` (número de etapas fixo em 10) (`:200-207`) → inserir etapas/itens (`:209-212`) → `agendar` (`:215`). O compromisso salvo tem `isEnabled = true` e `confirmouSaida/Chegada = false` (`:180-196`). `onSuccess` só baixa a flag (`MainActivity.kt:343`).
- Sair sem salvar: não há outro callback nem limpeza além do reset; corrotinas de salvar em andamento continuam no `viewModelScope`.
- Rotação: o manifest não declara `configChanges`, e a flag usa `remember` — pelo código, girar a tela recria a Activity e volta para a lista, com o estado do formulário ainda no ViewModel (não testado em aparelho).
- Ciclo de vida: `DisposableEffect` + `LifecycleEventObserver` só em `ui/permissions/PermissionsScreen.kt:67-68`. `AppForegroundState` (`services/AppForegroundState.kt:9-16`): `StateFlow<Boolean>` escrito apenas pela `MainActivity` (`:113-116`) e lido por `PomodoroService.kt:106,127,148`.

### 2. Agendamento e disparo

- IDs/request codes (`notifications/CompromissoAlarmScheduler.kt:29-39`): etapa `id*100 + índice invertido` (`:31`, `:97`); saída `id*100+99` (`:32`); ativação da chegada `id*100+97` (`:33`); ação "Já Saí" `id` (`:34`); atraso `id+9999` (`:39`); aviso de pausados `Int.MAX_VALUE-1` (`:37`); notificação de atraso `id*100+98` (`services/NotificationActionReceiver.kt:98`); "Você chegou!" `id*100+999` (`services/geofence/RegistroChegada.kt:46`).
- `agendar` (`:47-69`): etapas, saída, atraso e, com coordenadas e janela aberta, ativação da chegada — sem filtrar passado. `cancelar` (`:103-111`) cobre etapas (+5 de margem), saída, atraso e ativação. `reagendar(plano)` (`:75-89`) cancela e agenda só o plano. `dispararSaidaAgora` (`:127-129`) faz `sendBroadcast` direto ao `AlarmReceiver`. `agendarAtivacaoChegadaInexata` (`:121-124`).
- Todos os alarmes usam `setExactAndAllowWhileIdle(RTC_WAKEUP)` (`:205`; etapas em `NotificationScheduler.kt:65/75`); sem permissão, apenas log (`:199-202`).
- Extras: etapas levam só título e mensagem (`NotificationScheduler.kt:51-54`) — sem tipo nem ID de notificação; saída leva título, mensagem, ID, ação, tipo, itens e `EXTRA_COMPROMISSO_ID` (`CompromissoAlarmScheduler.kt:158-168`). Constantes em `AlarmReceiver.kt:13-24` (`EXTRA_ACTION_NOTIFICATION_ID = "EXTRA_NOTIFICATION_ID"`).
- `AlarmReceiver.onReceive` (`AlarmReceiver.kt:30-49`): tipo 0 → notificação padrão com "Já Saí" e `autoCancel` (`:96-138`); demais → `showFullScreenAlarm` com `setFullScreenIntent` para `ReminderActivity`, `ongoing`, sem `autoCancel` (`:51-94`). Etapas sempre caem no caminho padrão; sem `EXTRA_NOTIFICATION_ID`, usam `System.currentTimeMillis()` como ID (`:34`).
- Confirmar saída (botão ou `ReminderActivity.kt:76-85`) → `CONFIRM_EXIT` → `confirmarSaida`, `cancelarVerificacaoAtraso`, `cancel(notificationId)` (`NotificationActionReceiver.kt:45-57`).
- `CHECK_LATE` (`NotificationActionReceiver.kt:78-99`) relê o banco e notifica só se `!confirmouSaida`; não checa `isEnabled`.
- Não existe hoje nenhum mecanismo de suprimir ou adiar disparos.

### 3. Regra de lembretes passados (003)

- `planoDeReagendamento(horarios, dataHoraEvento, agora, ativo, confirmouSaida, confirmouChegada, temCoordenadas, reinicio)` → `Reagendamento(etapas, saida, saidaPerdida, atraso, ativacaoChegada)` (`notifications/HorariosLembrete.kt:63-119`):
  - desativado ou chegada confirmada → vazio (`:92`);
  - ativação da chegada = `max(inicioPreparo, agora)` se há coordenadas e `agora < evento+2h` (`:95-97`);
  - saída confirmada → só a chegada (`:100`);
  - etapas e atraso passados descartados (`:103-105`, `:116`);
  - saída perdida = `saida <= agora && reinicio && evento > agora`, reagendada para `agora` (`:108-113`).
- Efeito em `Reagendador.reagendarTudo` (`notifications/Reagendador.kt:45-109`): antes da saída perdida, lê a localização uma vez (timeout 4 s); DENTRO → `RegistroChegada.registrar`; DESCONHECIDA com alarme exato → saída adiada 45 s (`:66-81`); sem alarme exato → `dispararSaidaAgora` (`:96-98`). Entrada: `BootReceiver` (`notifications/BootReceiver.kt:19-36`).

### 4. Modelo, "desligado", "passado" e chegada

- `Compromisso` (`data/Compromisso.kt:8-31`): `dataHoraEvento` (epoch absoluto), `diasRepeticao` (CSV, `:16`), `isEnabled` (`:21`), `deleteAfterComplete` (`:22`), coordenadas (0.0 = sem), `confirmouSaida`/`confirmouChegada` (`:27-28`), `notificationType`. Não há campo de "encerrado".
- `AppDao` (`data/AppDao.kt`): `getCompromissosAtivosComEtapas` (`isEnabled = 1`, `:33`), `getAllCompromissosComEtapas` (Flow por `dataHoraEvento`, `:36-38`), `deleteExpiredCompromissos` (`deleteAfterComplete = 1 AND dataHoraEvento < :agora`, `:47-48`; chamado só no `init` do VM, `CompromissoViewModel.kt:86`; não cancela alarmes nem geofence).
- "Passado" não é um conceito único: comparações pontuais com `dataHoraEvento` em `AppDao.kt:47`, `HorariosLembrete.kt:46-50,108`, `CompromissoGeofenceManager.kt:49`, `CompromissoAlarmScheduler.kt:66`, `CompromissoViewModel.kt:172-178`.
- Repetição: nenhum código calcula a próxima ocorrência; `diasRepeticao` só é gravado, lido no VM (`:119,133,185`) e exibido (`CompromissosScreen.kt:125`).
- Desligar (`toggleCompromisso`, `CompromissoViewModel.kt:222-234`): grava `isEnabled`, chama `cancelar(id, etapas.size)` e `removeGeofence`; notificações já exibidas ficam. Religar: exige alarme exato (Toast `MENSAGEM_PERMISSAO_ALARME`, `:224-227`) e chama `agendar` com o evento original, **sem zerar `confirmouSaida/Chegada`**.
- Chegada: janela `inicioPreparo` → `evento + 2h` (`HorariosLembrete.kt:44-50`); geofence registrada pelo alarme `ATIVAR_CHEGADA` (`NotificationActionReceiver.kt:60-75` → `CompromissoGeofenceManager.kt:38-84`, expira no fim da janela, `:62`); recepção em `GeofenceBroadcastReceiver.kt:50-80`; `RegistroChegada.registrar` (`:21-50`) grava saída+chegada, `cancelar` os alarmes, fecha a notificação `id*100+99`, mostra "Você chegou!" e remove a geofence.

## Fluxo de dados relevante

```
Editar (CompromissosScreen:146) → iniciarEdicao (VM:121) → flag showAddCompromissoScreen (MainActivity:406)
  ├─ Voltar (BackHandler MainActivity:267 / ícone :346) → resetAddCompromissoState   [alarmes antigos seguem agendados]
  └─ Salvar (VM:141) → validações → Directions → update + removeGeofence + cancelar(10) → agendar (VM:215)

AlarmManager ─▶ AlarmReceiver (sem consulta) ─▶ notificação padrão | ReminderActivity (tela cheia)
AlarmManager ─▶ NotificationActionReceiver CHECK_LATE ─▶ lê banco ─▶ "Você está atrasado!" se !confirmouSaida
Boot ─▶ BootReceiver ─▶ Reagendador ─▶ planoDeReagendamento (puro) ─▶ CompromissoAlarmScheduler.reagendar
Switch ─▶ toggleCompromisso ─▶ updateCompromisso ─▶ agendar (sem filtro) | cancelar + removeGeofence
```

## Padrões e convenções a seguir

- **Regra pura × efeito**: funções top-level sem Android em `notifications/HorariosLembrete.kt` (KDoc `:8-10`), com `agora` como parâmetro e `null` = "não agendar"; efeito num `object` (`Reagendador.kt`) com try/catch por compromisso que relança `CancellationException` (`:104-108`).
- **Testes**: só JUnit 4.13.2 (`app/build.gradle.kts:104`), sem Robolectric/mockk. Nomes entre crases começando pelo ID sem ponto (`` `LEMB-11f ...` ``, `app/src/test/.../ReagendamentoTest.kt:88`) e comentário com o ID com ponto acima (`:86`); helper de data em UTC `em(dia, h, m)` (`:13-14`); fixtures com parâmetros padrão (`:19-37`).
- **Estado global**: `object` com `MutableStateFlow` privado + `asStateFlow()` + setter (`services/AppForegroundState.kt:9-16`, `PomodoroTimerState.kt:10-24`); evento consumível em `PomodoroCompletionState.kt:12-27`.
- **Mensagens**: textos em Kotlin; texto compartilhado vira `const val` top-level (`MENSAGEM_PERMISSAO_ALARME`, `CompromissoAlarmScheduler.kt:14`). Na lista, Toast no VM e `return` antes de gravar (`CompromissoViewModel.kt:222-227`); no formulário, `saveError` no UiState + snackbar (`AddCompromissoScreen.kt:55-60`).
- **Esmaecido**: `surfaceVariant.copy(alpha = 0.6f)`, elevação 1dp, título `alpha 0.5f` (`CompromissosScreen.kt:112-122`).
- **Nomes e comentários**: domínio em português; comentários com o ID da regra no ponto exato (`// LEMB-10:`, `// RF-003:`).

## Pontos de integração prováveis

- `MainActivity.kt:254, 267-278, 336-349, 406-409` — onde o formulário abre, salva e fecha.
- `MainActivity.kt:113-116` / `services/AppForegroundState.kt` — único sinal de visibilidade existente.
- `notifications/AlarmReceiver.kt:30-49` — ponto único em que etapas e saída viram notificação.
- `services/NotificationActionReceiver.kt:78-99` — disparo do atraso.
- `notifications/HorariosLembrete.kt:81-119` + `CompromissoAlarmScheduler.reagendar` (`:75-89`) — regra da 003 já pronta.
- `CompromissoViewModel.kt:141-220` (salvar), `:222-234` (toggle), `:86` (expiração).
- `ui/compromissos/CompromissosScreen.kt:112-127` — switch e esmaecimento.
- `data/Compromisso.kt`, `data/AppDao.kt:33,47`, `data/AppDatabase.kt:36` — se o "encerrado" precisar ser persistido.
- `services/geofence/RegistroChegada.kt:34-35` — exemplo de cancelar alarmes e fechar a notificação de saída por ID.

## Restrições e riscos observados

- **"Encerrado" × `isEnabled`**: gravar `isEnabled = false` no horário do evento faria geofence, receiver de chegada e Reagendador ignorarem o compromisso (contraria RF-007/CA-2.7/CA-2.8). Um estado persistido novo exigiria mudança de schema com `fallbackToDestructiveMigration` (P3/DT-03).
- **Nada acorda no horário do evento** hoje: não há alarme nem ticker para "encerrar" (RNF-002 com a lista aberta).
- **Retenção**: o `AlarmReceiver` não tem acesso a estado e roda em `BroadcastReceiver`; estado só em memória (`object`) se perde se o processo morrer — caso em que o formulário também não está mais visível.
- **Rotação** hoje volta para a lista (flag em `remember`); a spec diz que girar a tela não conta como sair (RF-006, CA-1.15).
- **Fechar o lembrete antigo ao salvar** (RF-002): a notificação de saída tem ID fixo (`id*100+99`) e pode ser cancelada; a tela cheia (`ReminderActivity`, `singleTask`) não é fechada por `cancel` de notificação. Notificações de etapa têm ID baseado no relógio e não são localizáveis.
- `cancelar(context, id, 10)` no salvar e `cancelar(id, etapas.size)` no toggle usam contagens diferentes de etapas (margem +5).
- Religar não zera `confirmouSaida/Chegada` — um compromisso com saída confirmada religado não gera atraso (`CHECK_LATE`).
- `deleteExpiredCompromissos` não cancela alarmes nem geofence dos apagados.
- Sem Robolectric: só lógica pura é testável por unidade; UI, receivers e AlarmManager ficam para roteiro manual.

## Contexto histórico

- `docs/sdd/specs/003-reagendar-apos-reiniciar/spec.md` (RF-002/RF-002a/RF-003) — regra de lembretes passados que a 005 adota (P4 revista).
- `docs/sdd/specs/003-reagendar-apos-reiniciar/plano.md` — `planoDeReagendamento` e `reagendar(plano)`; alarme inexato da chegada sem alarme exato (`1da2e03`).
- `docs/sdd/specs/001-lembrete-de-saida/` — bloqueio sem alarme exato ao salvar/religar (LEMB-10, decisão 2026-10-26).
- `docs/sdd/specs/002-deteccao-de-chegada/` — período de chegada a partir do início do preparo (Revisão 2).
- `docs/sdd/capacidades/compromissos.md:87` — observação em COMP-10 sobre religar compromisso passado.

## Referências de código

- `MainActivity.kt:67, 113-116, 251-278, 336-349, 400-409` — VM, foreground, flags, BackHandler, formulário.
- `ui/compromissos/CompromissoViewModel.kt:77-86, 121-138, 141-220, 222-234` — lista, expiração, edição, salvar, toggle.
- `ui/compromissos/CompromissosScreen.kt:112-127, 146` — card e switch.
- `notifications/CompromissoAlarmScheduler.kt:14, 29-129, 158-205` — IDs, agendar, cancelar, reagendar, disparo imediato.
- `notifications/AlarmReceiver.kt:13-138` — extras e notificações.
- `notifications/HorariosLembrete.kt:25-57, 63-124` — horários, janela, plano da 003.
- `notifications/Reagendador.kt:45-179` — efeito do plano no boot.
- `services/NotificationActionReceiver.kt:45-99` — "Já Saí", ativar chegada, atraso.
- `services/geofence/CompromissoGeofenceManager.kt:38-84`, `GeofenceBroadcastReceiver.kt:50-80`, `RegistroChegada.kt:21-50`.
- `data/Compromisso.kt:8-31`, `data/AppDao.kt:18-48`, `data/AppDatabase.kt:9-36`.
- `app/src/test/java/com/brunocsantos/agoravai/notifications/ReagendamentoTest.kt:13-37, 86-104`.

## Questões em aberto

- Como representar "encerrado" sem mudar o schema (derivado de `dataHoraEvento` e `diasRepeticao` na exibição e no toggle?) ou com migração real — decisão de plano, com impacto em P3/DT-03.
- Se a retenção deve morar no disparo (receiver consulta um estado de "em edição") ou no agendamento (cancelar ao abrir o formulário e reagendar ao sair) — decisão de plano; a segunda reaproveita `planoDeReagendamento`.
- O comportamento real de rotação e de `onStop` com a tela apagada precisa ser confirmado em aparelho.
