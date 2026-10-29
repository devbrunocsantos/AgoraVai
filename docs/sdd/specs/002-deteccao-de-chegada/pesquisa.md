---
spec: docs/sdd/specs/002-deteccao-de-chegada/spec.md
data: 2026-10-26
commit: 791c539
branch: ccr-80e3a1a2-ocxhin
pesquisador: Claude
status: completa  # completa | parcial
---

# Pesquisa: Detecção de chegada

Caminhos relativos a `app/src/main/java/com/brunocsantos/agoravai/`. Linhas referem-se ao código **antes** da implementação.

## Pergunta de pesquisa

Por que a detecção de chegada (LEMB-09) nunca funciona, e o que existe hoje para restringi-la ao período do compromisso e fazê-la funcionar com o app fechado?

## Resumo

- `CompromissoGeofenceManager.initialize` nunca é chamado; `addGeofenceChegada` retorna na primeira linha (DT-01).
- Os dois managers de geofence só são inicializados pela `MainActivity`; em processos sem Activity (receivers, workers), `remove` não faz nada — afeta também AUTO-08.
- A geofence é registrada com `NEVER_EXPIRE` e `INITIAL_TRIGGER_ENTER`, sem nenhuma verificação de horário no receiver.
- O receiver lança corrotina sem `goAsync()`; o processo pode morrer antes de gravar.
- Não existe classe `Application` nem consulta "compromisso com etapas por ID".

## Descobertas detalhadas

### Requisitos × código

| Requisito | Status na capacidade | Real | Evidência |
|---|---|---|---|
| LEMB-09 | ⚠️ | confere | Só `GeofenceManager.initialize(this)` em `MainActivity.kt:122`. `addGeofenceChegada` retorna na primeira linha (`services/geofence/CompromissoGeofenceManager.kt:37`). |
| AUTO-05 / AUTO-08 | ✅ | **diverge parcialmente** | `PomodoroWorker` chama `GeofenceManager.remove` (`workers/PomodoroWorker.kt:72`) num processo sem Activity; a guarda `isInitialized` faz a chamada não fazer nada. Incluído no DT-01. |

### Registro da geofence

- Ao salvar ou reativar: `addGeofenceChegada(compromisso)` (`CompromissoViewModel.kt:214,227`); exige `lat/lng != 0.0` (`CompromissoGeofenceManager.kt:40`); raio 200 m, `NEVER_EXPIRE` (`:51`), só `ENTER`, `INITIAL_TRIGGER_ENTER` (`:56`).
- `PendingIntent` com request code 0 para `GeofenceBroadcastReceiver`, `FLAG_MUTABLE` (`:22-30`), idêntico ao do `GeofenceManager` — compartilhado; o receiver distingue pelo `requestId`.
- Também registra geofence de compromissos desativados reativados no passado, que nunca expira.

### Receiver

- `requestId` com prefixo `compromisso_chegada_` → `handleCompromissoChegada` (`GeofenceBroadcastReceiver.kt:52-83`).
- `CoroutineScope(Dispatchers.IO)` **sem `goAsync()`** (`:58`); checa `!confirmouChegada`, chama `dao.confirmarChegada`, notifica (ID `id*100+999`) e `removeGeofence` (que não faz nada sem manager inicializado).
- Nenhuma verificação de horário.

## Fluxo de dados relevante

```
salvar/reativar → addGeofenceChegada (retorna cedo: não inicializado)
[se registrada] entrada no raio → GeofenceBroadcastReceiver → handleCompromissoChegada
  → dao.confirmarChegada → notificação id*100+999 → removeGeofence
```

## Padrões e convenções a seguir

- Managers de geofence como `object` com `initialize(context)`.
- Regras de cálculo em `notifications/HorariosLembrete.kt` (criado na spec 001), testadas sem Android.
- Agendamento e cancelamento de lembretes via `CompromissoAlarmScheduler` (spec 001).

## Pontos de integração prováveis

- `MainActivity.kt:122` — inicialização atual dos managers.
- `AndroidManifest.xml` — sem `android:name` na `<application>`.
- `CompromissoGeofenceManager.kt:37-56` — registro.
- `GeofenceBroadcastReceiver.kt:52-83` — tratamento da chegada.
- `data/AppDao.kt` — `confirmarSaida(id)`, `confirmarChegada(id)`; falta consulta de compromisso com etapas por ID (`@Transaction @Query` não muda o schema).

## Restrições e riscos observados

- **Contratos**: `requestId` `"compromisso_chegada_<id>"` (automações usam só dígitos); notificação de chegada `id*100+999`; `PendingIntent` de geofence compartilhado (request code 0).
- Geofence em segundo plano exige `ACCESS_BACKGROUND_LOCATION` (pedida em `MainActivity.kt`, SIS-03).
- O Android avisa entradas com atraso e só avisa **entradas** (não permanência) — origem da Revisão 2.
- Geofences somem ao reiniciar (spec 003).
- O receiver pode rodar num processo novo → inicialização precisa estar no `Application`.

## Contexto histórico

- `docs/sdd/specs/001-lembrete-de-saida/plano.md` — criou `CompromissoAlarmScheduler` e `HorariosLembrete`, reutilizados aqui; registrou o DT-20.

## Referências de código

- Teste manual possível com localização simulada (`adb emu geo fix` ou app de mock location) — ou, após a Revisão 2, criando o compromisso estando no destino.

## Questões em aberto

- Resolvidas na spec (Sessões 2026-10-27 e 2026-10-28).
