---
spec: docs/sdd/specs/003-reagendar-apos-reiniciar/spec.md
data: 2026-10-26
commit: 791c539
branch: ccr-80e3a1a2-ocxhin
pesquisador: Claude
status: parcial  # completa | parcial
---

# Pesquisa: Reagendar após reiniciar ou atualizar

Caminhos relativos a `app/src/main/java/com/brunocsantos/agoravai/`.

> **Parcial:** feita antes da implementação das specs 001 e 002. Os fatos sobre `CompromissoAlarmScheduler`, `AgoraVaiApplication` e o alarme de ativação da chegada (Revisão 2 da 002) eram previsões e devem ser **reconferidos no código atual** (HEAD `066e026`) antes do planejamento definitivo.

## Pergunta de pesquisa

O que acontece com alarmes, geofences e trabalhos agendados quando o aparelho reinicia ou o app é atualizado, e quais peças existem para reagendá-los sem abrir o app?

## Resumo

- Não existe receiver de `BOOT_COMPLETED`/`MY_PACKAGE_REPLACED` nem a permissão `RECEIVE_BOOT_COMPLETED` no manifest.
- Geofences de automações só são registradas quando o usuário salva/alterna uma automação → somem após reiniciar.
- Trabalhos periódicos do WorkManager se reagendam sozinhos (AUTO-04 ok).
- Request codes iguais + `FLAG_UPDATE_CURRENT` tornam o reagendamento idempotente.
- Os dados para reagendar existem, mas só como `Flow`; é preciso consulta `suspend` (ou `.first()`).

## Descobertas detalhadas

### Requisitos × código

| Requisito | Status na capacidade | Real | Evidência |
|---|---|---|---|
| LEMB-11 | ⬜ | confere | `AndroidManifest.xml` sem `RECEIVE_BOOT_COMPLETED` nem receiver de boot/atualização. |
| AUTO-05 | ✅ | **diverge após reboot** | Geofences de automação só em `PomodoroViewModel.updateSchedules` (`ui/pomodoro/PomodoroViewModel.kt:243-256`). |
| AUTO-04 | ✅ | confere | `PeriodicWorkRequest` (`services/PomodoroScheduler.kt:19-50`) persistido pelo WorkManager. |

### Dados e agendamento

- `AppDao.getAllCompromissosComEtapas()` só existe como `Flow` (`data/AppDao.kt`). Automações têm `getAllAutomationSettingsOnce()`.
- Alarmes RTC usam horário absoluto (epoch); `dataHoraEvento` também (`data/Compromisso.kt`) → mudança de fuso/relógio não pede ação.
- Após a spec 001: `CompromissoAlarmScheduler.agendar/cancelar/podeAgendarExato`. Após a 002: `AgoraVaiApplication` inicializa os managers; `agendar` também agenda o alarme de ativação da chegada (request code `id*100+97`).

## Fluxo de dados relevante

```
BOOT_COMPLETED / MY_PACKAGE_REPLACED → (inexistente)
hoje: alarmes e geofences só são (re)criados por salvarCompromisso / toggleCompromisso / updateSchedules
```

## Padrões e convenções a seguir

- Receivers com Room usam `goAsync()` + corrotina em IO (padrão adotado em `GeofenceBroadcastReceiver` e `NotificationActionReceiver.ATIVAR_CHEGADA` na spec 002).
- Regras puras em `notifications/HorariosLembrete.kt`, com testes JUnit.

## Pontos de integração prováveis

- `AndroidManifest.xml` — permissão e receiver.
- `notifications/CompromissoAlarmScheduler.kt` — reutilizável para reagendar.
- `services/geofence/GeofenceManager.kt` — `addOrUpdateGeofence` das automações.
- `data/AppDao.kt` — consulta `suspend` de compromissos ativos com etapas e itens.

## Restrições e riscos observados

- `BOOT_COMPLETED` exige `RECEIVE_BOOT_COMPLETED` e receiver `exported="true"` (broadcast protegido, só o sistema envia).
- `MY_PACKAGE_REPLACED` só é entregue ao próprio app e não precisa de permissão.
- Receiver tem ~10 s; Room exige `goAsync()`.
- Direct Boot: `BOOT_COMPLETED` chega após o desbloqueio (o app não é `directBootAware`).
- Alguns fabricantes só entregam o boot com "início automático" liberado (orientação em `PermissionsScreen`, SIS-05).
- Contratos: IDs e request codes de `arquitetura.md` §5.2 devem ser os mesmos do agendamento original.

## Contexto histórico

- `docs/sdd/specs/002-deteccao-de-chegada/plano.md` (Revisão 2) — o monitoramento da chegada passou a ser ativado por alarme; reagendar via `CompromissoAlarmScheduler.agendar` já cobre a chegada. O plano desta spec precisa refletir isso.

## Referências de código

- Verificação possível com `adb reboot`, `adb shell dumpsys alarm | grep agoravai` e, para atualização, `adb install -r`.

## Questões em aberto

- Reconferir os pontos marcados como previsão (ver aviso no topo).
