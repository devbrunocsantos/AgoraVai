---
spec: docs/sdd/specs/002-deteccao-de-chegada/spec.md
pesquisa: docs/sdd/specs/002-deteccao-de-chegada/pesquisa.md
data: 2026-10-26
commit_base: 13c687c
status: concluido  # rascunho | aprovado | em-implementacao | concluido
---

# Plano de implementação: Detecção de chegada

> Aprovado em 2026-10-27 por Bruno C. Santos (com a Fase 4, DT-20, acrescentada na aprovação). Fase 5 (Revisão 2) aprovada em 2026-10-28.
> Pré-requisito: spec 001 implementada (`CompromissoAlarmScheduler`, `calcularHorarios`).
> Pendências de verificação manual: **B5**, **B6**, R4, R5 — a cobrir em `/validar 002`.

## Visão geral

Fazer LEMB-09 funcionar com o app aberto ou fechado e só no período do
compromisso: inicializar os managers num `Application`, criar a regra pura do
período, filtrar no receiver e — na Revisão 2 — começar o monitoramento só no
início do período, por um alarme de ativação.

## Estado atual

Ver `pesquisa.md`. Em resumo:
- `MainActivity.kt:122` — só o `GeofenceManager` é inicializado, e só com a Activity viva.
- `CompromissoGeofenceManager.kt:37` — `addGeofenceChegada` retorna cedo; `NEVER_EXPIRE` (`:51`).
- `GeofenceBroadcastReceiver.kt:52-83` — sem `goAsync()` e sem verificação de horário.
- `CompromissoViewModel` — `tempLatDestino/tempLngDestino` são campos que vazam entre salvamentos (DT-20).

## Estado desejado

Chegada detectada dentro de [início do preparo, evento + 2 h], com app aberto ou
fechado, inclusive para quem já está no destino quando o período começa; lembretes
pendentes cancelados; nada fora do período. Verificado por `JanelaChegadaTest`
e pelos roteiros B5, B6, R4, R5.

## Fora de escopo

- Re-registrar a geofence depois de reiniciar (spec 003 — herda pela reutilização de `CompromissoAlarmScheduler.agendar`).
- Mover `createNotificationChannel` para o `Application` (follow-up).

## Decisões de design

| Decisão | Alternativas consideradas | Motivo da escolha |
|---|---|---|
| `AgoraVaiApplication` inicializa os dois managers | Só chamar `initialize` na `MainActivity` | Receivers/workers em segundo plano continuariam sem conseguir remover geofences |
| Regra pura `janelaDeChegada` / `estaNaJanelaDeChegada` em `HorariosLembrete.kt` | Lógica no receiver | P7: testável sem Android |
| Filtro no receiver + `setExpirationDuration(fimDaJanela − agora)` | Registrar só no início do preparo (via alarme) | Original: menos peças. **Substituída na Revisão 2** |
| *(Revisão 2)* Alarme de ativação no início do preparo registra a geofence com `INITIAL_TRIGGER_ENTER` | Geofence `DWELL` | `DWELL` atrasa a detecção e não resolve o período; o alarme faz a entrada inicial cair **dentro** do período e evita geofence antes da hora |
| *(desvio, fase 3)* A chegada também fecha a notificação "HORA DE SAIR" se já estiver na bandeja | Só cancelar alarmes pendentes | A chegada grava `confirmouSaida`; LEMB-07 diz que confirmar fecha o lembrete; a tela cheia ficaria presa |
| *(desvio, fase 3)* `addGeofenceChegada` não registra para compromisso desativado | — | Proteção; os chamadores já não faziam isso |

## Conformidade com a constituição

- [x] P1 A spec vem primeiro — aprovada em 2026-10-27; Revisão 2 em 2026-10-28
- [x] P2 Lembrete não falha em silêncio — chegada em segundo plano; sem "Hora de sair" para quem chegou
- [x] P3 Dados preservados — só uma consulta nova, sem schema
- [x] P4 Local-first — localização só no aparelho
- [x] P5 Desenho para TDAH — celebra a chegada, evita ruído
- [x] P6 Contratos explícitos — `requestId` e IDs mantidos; request code novo `id*100+97` documentado em §5.2
- [x] P7 Critérios verificáveis — regra pura com teste + roteiros
- [x] P8 Simplicidade — `Application` é o ponto padrão; sem dependências
- [x] P9 Consistência

---

## Fase 1: Application

### Mudanças
- **Arquivo**: `AgoraVaiApplication.kt` (novo) — inicializa `GeofenceManager` e `CompromissoGeofenceManager`.
- **Arquivo**: `AndroidManifest.xml` — `android:name=".AgoraVaiApplication"`.
- **Arquivo**: `MainActivity.kt` — inicializações saem daqui.

### Critérios de sucesso
#### Verificação automatizada
- [x] `./gradlew testDebugUnitTest assembleDebug` — commit `5d19f22`
- [x] `AgoraVaiApplication` presente no manifest mesclado
#### Verificação manual
- [ ] Criar um compromisso e conferir `adb shell dumpsys location | grep compromisso_chegada` (após a Revisão 2: conferir depois do início do preparo)

---

## Fase 2: Regra do período

### Mudanças
- **Arquivo**: `notifications/HorariosLembrete.kt` — `janelaDeChegada(horarios, dataHoraEvento)` / `estaNaJanelaDeChegada(agora, …)`, período = [início do preparo, evento + 2 h].
- **Arquivo**: `app/src/test/.../notifications/JanelaChegadaTest.kt` (novo).

### Critérios de sucesso
#### Verificação automatizada
- [x] `./gradlew testDebugUnitTest` — commit `cd3bfd2` (9/9)
- [x] `JanelaChegadaTest` cobre CA-1.1 e CA-2.1 (lógica): dentro, antes do preparo, dia anterior, limites 8:30 e 12:00/12:01

---

## Fase 3: Registro e receiver

### Mudanças
- **Arquivo**: `services/geofence/CompromissoGeofenceManager.kt` — recebe as etapas; expiração no fim do período; não registra se `fimDaJanela ≤ agora` nem para desativado.
- **Arquivo**: `GeofenceBroadcastReceiver.kt` — `goAsync()`; fora do período ignora sem remover; dentro: `confirmarSaida` + `confirmarChegada`, `CompromissoAlarmScheduler.cancelar`, fecha "HORA DE SAIR", notifica e remove a geofence.
- **Arquivo**: `data/AppDao.kt` — `getCompromissoComEtapasById` (`@Transaction`, sem schema).
- **Arquivo**: `CompromissoViewModel.kt` — passa as etapas.

### Critérios de sucesso
#### Verificação automatizada
- [x] `./gradlew testDebugUnitTest assembleDebug` — commit `ffda78d`
#### Verificação manual
- [x] B1 — CA-2.1 (parcial): entrada antes do período ignorada ✅ (aparelho real, 2026-10-28); a detecção posterior fica com B5
- [x] B2/B3 — executados já dentro do raio: ❌ "HORA DE SAIR" tocou e "Você chegou!" não apareceu. Causa: a entrada inicial caía antes do período e era ignorada; sem sair do raio, não há nova entrada → **Revisão 2** (Fase 5)
- [ ] R4 — CA-3.1: destino que o mapa não localiza; salvar funciona e não há geofence

---

## Fase 4: Coordenadas não vazam entre salvamentos (DT-20)

### Mudanças
- **Arquivo**: `ui/compromissos/CompromissoViewModel.kt` — `tempLatDestino`/`tempLngDestino` viram variáveis locais de cada salvamento, iniciando em `0.0`.

### Critérios de sucesso
#### Verificação automatizada
- [x] `./gradlew testDebugUnitTest assembleDebug` — commit `e80e99a`
#### Verificação manual
- [ ] R5 — CA-3.2: salvar A (endereço comum) e depois B (não localizável); `dumpsys location` sem geofence para B. **B4 inconclusivo**: a Directions API aceitou o endereço inexistente com 0 min (**DT-21**) — precisa de outro endereço não localizável pelo Geocoder

---

## Fase 5: O monitoramento começa no início do período (Revisão 2)

### Objetivo
RF-002 (início do monitoramento) e RF-003, mantendo CA-1.1 a CA-3.2.

### Mudanças
- **Arquivo**: `notifications/CompromissoAlarmScheduler.kt` — `agendar` também agenda um **alarme de ativação** no início do preparo; `cancelar` o cancela.
- **Arquivo**: `services/NotificationActionReceiver.kt` — ação nova `ATIVAR_CHEGADA` (com `goAsync`) registra a geofence com `INITIAL_TRIGGER_ENTER`. Se o horário já passou, o `AlarmManager` dispara na hora — um caminho só.
- **Arquivo**: `ui/compromissos/CompromissoViewModel.kt` — deixa de chamar `addGeofenceChegada` direto.
- **Arquivo**: `notifications/HorariosLembrete.kt` + teste.
- **Contrato novo (§5.2):** request code `compromissoId*100 + 97` (mesma limitação de ≥ 97 etapas do DT-12).

### Critérios de sucesso
#### Verificação automatizada
- [x] `./gradlew testDebugUnitTest assembleDebug` — commit `355933f` (10/10)
- [x] `JanelaChegadaTest`: horário de ativação = início do preparo (CA-1.4, regra)
#### Verificação manual
- [ ] **B5** — CA-1.4 (e CA-1.1, CA-1.2): no destino, criar o compromisso com o preparo começando em ~5 min e não sair do lugar → "Você chegou! 🏁" no início do preparo; "HORA DE SAIR" e atraso não tocam
- [ ] **B6** — CA-1.3: igual ao B5, fechando o app à força (`adb shell am kill com.brunocsantos.agoravai` ou remover dos recentes) antes do preparo começar

---

## Estratégia de testes

| Critério (spec) | Tipo | Teste | Fase |
|---|---|---|---|
| CA-1.1, CA-2.1 (regra) | unitário | `JanelaChegadaTest` | 2 |
| CA-1.4 (regra) | unitário | `JanelaChegadaTest` (ativação) | 5 |
| CA-1.1, CA-1.2, CA-1.4 | manual | B5 | 5 |
| CA-1.3 | manual | B6 | 5 |
| CA-2.1 | manual | B1 ✅ | 3 |
| CA-3.1 | manual | R4 | 3 |
| CA-3.2 | manual | R5 | 4 |

## Rastreabilidade

| Requisito | Fase(s) |
|---|---|
| RF-001 | 3 |
| RF-002 | 2, 3, 5 |
| RF-003 | 5 |
| RF-004 | 1, 3 |
| RF-005 | 4 |

## Riscos e mitigação

| Risco | Impacto | Mitigação |
|---|---|---|
| Inicializar no `Application` aumenta a abertura | Baixo | Só atribuições de contexto |
| Imprecisão do GPS gera falso negativo | Médio | Raio de 200 m; período de 2 h |
| Alarme de ativação é mais um ponto de falha | Médio | Reutiliza o mesmo agendador exato dos lembretes; cancelado junto |

## Migração e rollback

- Room: só consulta nova, sem versão. Manifest: `android:name=".AgoraVaiApplication"`.
- Geofences já registradas em aparelhos: nenhuma (nunca foram registradas).
- **Rollback:** reverter os commits das fases.

## Registro da implementação

- **Código:** 2026-10-27 (fases 1–4) e 2026-10-28 (fase 5).
- **Saída das verificações:** BUILD SUCCESSFUL; `HorariosLembreteTest: tests=4 failures=0`; `JanelaChegadaTest: tests=6 failures=0`.
- **Teste em aparelho (2026-10-28):** sem como simular localização, os compromissos foram criados com destino no local onde o usuário estava. Resultados nas fases 3 e 4 acima.
- **Incorporação nas capacidades** (commits `d3b5fe2`, `066e026`):
  - [x] Delta em `capacidades/lembretes.md` (LEMB-09), `compromissos.md` (COMP-08) e `automacoes.md` (AUTO-08), com status 🧪
  - [x] `arquitetura.md` (DT-01 e DT-20 🧪, §3.2, §5.3.1, histórico 1.4)
  - [ ] Após B5, B6, R4, R5: trocar 🧪 por ✅ em LEMB-09/COMP-08 e riscar DT-01, DT-20
- **Follow-ups:**
  - Geofences de chegada não sobrevivem a reiniciar → spec 003 (basta reagendar via `CompromissoAlarmScheduler.agendar`; o plano da 003 precisa refletir isso).
  - `NotificationScheduler.createNotificationChannel` só na `MainActivity`: se um receiver notificar antes de o app ser aberto uma vez após a instalação, o canal não existe. Mover para o `AgoraVaiApplication`.
  - **DT-21** (endereço inexistente aceito com 0 min) — novo item de backlog.

## Referências

- Spec: `docs/sdd/specs/002-deteccao-de-chegada/spec.md`
- Pesquisa: `docs/sdd/specs/002-deteccao-de-chegada/pesquisa.md`
- Contratos: `docs/sdd/arquitetura.md` §5.2, fluxo §5.3.1
