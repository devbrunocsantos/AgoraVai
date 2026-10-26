# Research — Detecção de chegada

- **Data:** 2026-10-26
- **Autor:** Claude
- **Spec:** [`spec.md`](spec.md)
- **Requisitos do delta:** LEMB-09 (vizinhos: COMP-08, LEMB-02, LEMB-08, AUTO-05, AUTO-08)

Caminhos relativos a `app/src/main/java/com/brunocsantos/agoravai/`.

## 1. Requisitos × código hoje

| Requisito | Status na spec viva | Real | Evidência |
|---|---|---|---|
| LEMB-09 | ⚠️ | confere | `CompromissoGeofenceManager.initialize` nunca é chamado (só `GeofenceManager.initialize(this)` em `MainActivity.kt:122`). `addGeofenceChegada` retorna na primeira linha (`services/geofence/CompromissoGeofenceManager.kt:37`). |
| AUTO-05 / AUTO-08 | ✅ | **diverge parcialmente** | `GeofenceManager` só é inicializado pela `MainActivity`. Quando o `PomodoroWorker` roda num processo sem Activity e chama `GeofenceManager.remove` (`workers/PomodoroWorker.kt:72`), a chamada não faz nada (`GeofenceManager.kt:remove`, guarda `isInitialized`). Automações de uso único arquivadas em segundo plano ficam com a geofence ativa. Incluo este caso no DT-01. |

## 2. Componentes e fluxo atual
1. Ao salvar ou reativar: `CompromissoGeofenceManager.addGeofenceChegada(compromisso)` (`CompromissoViewModel.kt:214,227`). O método exige `lat/lng != 0.0` (`CompromissoGeofenceManager.kt:40`) e registra raio de 200 m, `NEVER_EXPIRE` (`:51`), só `ENTER`, com `INITIAL_TRIGGER_ENTER` (`:56`).
2. O `PendingIntent` usa request code 0 para `GeofenceBroadcastReceiver`, `FLAG_MUTABLE` (`:22-30`), idêntico ao do `GeofenceManager`. É o mesmo `PendingIntent` compartilhado, e o receiver distingue os casos pelo `requestId`.
3. `GeofenceBroadcastReceiver.onReceive`: um `requestId` com prefixo `compromisso_chegada_` leva a `handleCompromissoChegada` (`GeofenceBroadcastReceiver.kt:52-83`).
4. `handleCompromissoChegada` lança `CoroutineScope(Dispatchers.IO)` **sem `goAsync()`** (`:58`), então o processo pode morrer antes de terminar. Ele checa `!confirmouChegada` e chama `dao.confirmarChegada`, notifica (ID `id*100+999`) e chama `removeGeofence`, que **não faz nada** se o processo foi criado só para o receiver (manager não inicializado).
5. Nenhuma verificação de horário: qualquer entrada, em qualquer data, conta como chegada.

## 3. Contratos que não podem quebrar
- `requestId` `"compromisso_chegada_<id>"` (automações usam só dígitos; o receiver diferencia por isso).
- Notificação de chegada: ID `id*100+999`.
- `PendingIntent` de geofence compartilhado (request code 0) entre os dois managers.
- DAO: `confirmarSaida(id)`, `confirmarChegada(id)`. Para calcular o início do preparo é preciso ler as etapas, e não existe consulta "compromisso com etapas por ID". Uma `@Transaction @Query` nova não muda o schema.

## 4. Restrições de plataforma
- Geofence em segundo plano exige `ACCESS_BACKGROUND_LOCATION` (pedida em `MainActivity.kt`, SIS-03).
- O Android avisa entradas com atraso de até alguns minutos e é menos preciso em Doze.
- Geofences são apagadas ao reiniciar o aparelho (tratado em outra mudança).
- O receiver pode rodar num processo novo, sem `MainActivity`. Por isso a inicialização precisa estar no `Application`. Hoje não existe classe `Application` (o `AndroidManifest.xml` não tem `android:name`).

## 5. Testes existentes
Nenhum para esta área. Teste manual possível com localização simulada (`adb emu geo fix`, ou um app de mock location em aparelho real).

## 6. Outros achados
- `CompromissoGeofenceManager.addGeofenceChegada` também registra a geofence de compromissos desativados reativados no passado, que nunca expira.

## 7. Perguntas em aberto
- Ver Q3 e Q4 na spec.
