# Plan — Detecção de chegada

- **Data:** 2026-10-26
- **Spec:** [`spec.md`](spec.md) · **Research:** [`research.md`](research.md)
- **Status:** Rascunho
- **Pré-requisito:** `2026-10-26-lembrete-de-saida` implementada (`CompromissoAlarmScheduler`, `calcularHorarios`)

## 1. Objetivo
Fazer LEMB-09 funcionar com o app aberto ou fechado e só no período do
compromisso.

## 2. Matriz de verificação

| Critério | Tipo | Verificação |
|---|---|---|
| LEMB-09.a / .c (regra do período) | Unitário | `JanelaChegadaTest`: dentro, antes, depois, limites exatos |
| LEMB-09.a | Manual | R1: compromisso em ~40 min, mock de localização no destino após o início do preparo |
| LEMB-09.b | Manual | R1 continuação: nem "HORA DE SAIR" nem atraso tocam; `dumpsys alarm` sem alarmes do compromisso |
| LEMB-09.c | Manual | R2: compromisso amanhã; mock no destino hoje → nada; `dumpsys location` ainda lista a geofence |
| LEMB-09.d | Manual | R3: `adb shell am kill com.brunocsantos.agoravai` e repetir R1 |
| LEMB-09.e | Manual | R4: destino que o Geocoder não resolve; salvar funciona e não há geofence |

## 3. Abordagem
1. **`AgoraVaiApplication`** (novo) inicializa `GeofenceManager` e `CompromissoGeofenceManager`, que passam a funcionar em qualquer processo (receivers, workers). Essas inicializações saem da `MainActivity`. Isso também corrige o `GeofenceManager.remove` no `PomodoroWorker`.
2. **Regra pura** `janelaDeChegada(horarios, dataHoraEvento)` / `estaNaJanelaDeChegada(agora, …)` em `notifications/HorariosLembrete.kt`, com período = [início do preparo, evento + 2 h].
3. **Registro:** `addGeofenceChegada` recebe as etapas e usa `setExpirationDuration(fimDaJanela − agora)`. Se `fimDaJanela ≤ agora`, não registra.
4. **Receiver:** `handleCompromissoChegada` com `goAsync()`. Nova consulta `getCompromissoComEtapasById` (`@Transaction`, sem mudança de schema). Fora do período → ignora sem remover a geofence. Dentro → `confirmarSaida` + `confirmarChegada`, `CompromissoAlarmScheduler.cancelar`, notifica e remove a geofence.

| Alternativa descartada | Por que não |
|---|---|
| Só chamar `initialize` na `MainActivity` | Funciona com o app aberto, mas o receiver em segundo plano continua sem conseguir remover a geofence; e o problema do período continua |
| Registrar a geofence só no início do preparo (via alarme) | Mais um alarme e mais um ponto de falha; o filtro no receiver + expiração resolve com menos peças |
| Geofence `DWELL` | Atrasa a detecção e não resolve o problema do período; fica para o caso de borda futuro |

## 4. Checklist da constituição

| Princípio | Atende? | Nota |
|---|---|---|
| P1 Spec primeiro | ☐ | aguarda aprovação |
| P2 Lembrete não falha em silêncio | ✅ | chegada detectada também em segundo plano |
| P3 Dados preservados | ✅ | só uma consulta nova, sem schema |
| P4 Local-first | ✅ | localização só no aparelho |
| P5 Desenho para TDAH | ✅ | não toca "Hora de sair" para quem já chegou |
| P6 Contratos explícitos | ✅ | `requestId` e IDs mantidos |
| P7 Critérios verificáveis | ✅ | regra pura com teste + roteiros |
| P8 Simplicidade | ✅ | `Application` é o ponto padrão do Android para inicialização; sem dependências |
| P9 Consistência | ✅ | — |

## 5. Passos

### Fase 1 — Application
- **Arquivos:** `AgoraVaiApplication.kt` (novo), `AndroidManifest.xml` (`android:name`), `MainActivity.kt`
- **Verificação:** build + abrir o app, criar um compromisso e conferir a geofence com `adb shell dumpsys location | grep compromisso_chegada`

### Fase 2 — Regra do período
- **Critérios cobertos:** LEMB-09.a/.c (lógica)
- **Arquivos:** `notifications/HorariosLembrete.kt`, `app/src/test/.../JanelaChegadaTest.kt`
- **Verificação:** `./gradlew testDebugUnitTest`

### Fase 3 — Registro e receiver
- **Critérios cobertos:** LEMB-09.a–e
- **Arquivos:** `services/geofence/CompromissoGeofenceManager.kt`, `GeofenceBroadcastReceiver.kt`, `data/AppDao.kt`, `CompromissoViewModel.kt` (passar as etapas)
- **Verificação:** build + R1–R4

## 6. Impacto
- **Room:** só uma consulta nova (`@Transaction`), sem mudança de versão.
- **Manifest:** `android:name=".AgoraVaiApplication"`.
- **Geofences já registradas em aparelhos:** hoje não existe nenhuma (nunca foram registradas), então não há o que migrar.
- **Textos visíveis:** sem textos novos.

## 7. Riscos e rollback
| Risco | Mitigação |
|---|---|
| Inicializar no `Application` aumenta o tempo de abertura | São só atribuições de contexto (lazy), custo desprezível |
| Imprecisão do GPS gera falso negativo | Raio de 200 m mantido; período de 2 h dá folga |

**Rollback:** reverter os commits.

## 8. Atualização das specs ao final
- [ ] `capabilities/lembretes.md`: LEMB-09 com o texto novo, status ✅
- [ ] `architecture.md`: riscar DT-01; §3.2 `AgoraVaiApplication`; §5 fluxo de chegada; histórico
