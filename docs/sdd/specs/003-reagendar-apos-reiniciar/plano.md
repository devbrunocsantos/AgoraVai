---
spec: docs/sdd/specs/003-reagendar-apos-reiniciar/spec.md
pesquisa: docs/sdd/specs/003-reagendar-apos-reiniciar/pesquisa.md
data: 2026-10-26
commit_base: <preencher ao aprovar>
status: rascunho  # rascunho | aprovado | em-implementacao | concluido
---

# Plano de implementação: Reagendar após reiniciar ou atualizar

> **Rascunho anterior à aprovação da spec.** Depois de `/esclarecer 003` e da reconferência da pesquisa, revisar com `/planejar 003` (o `revisor-de-plano` deve conferir as referências contra o código atual).
> Pré-requisitos: specs 001 e 002 implementadas ✅.

## Visão geral

Um `BootReceiver` para `BOOT_COMPLETED` e `MY_PACKAGE_REPLACED` chama um
`Reagendador` que, para cada compromisso ativo, reagenda só os horários futuros
via `CompromissoAlarmScheduler.agendar` (que, desde a Revisão 2 da spec 002,
também agenda a ativação da chegada) e re-registra as geofences das automações
com local. Sem alarme exato, notifica o usuário.

## Estado atual

- `AndroidManifest.xml` — sem receiver de boot/atualização nem `RECEIVE_BOOT_COMPLETED`.
- `ui/pomodoro/PomodoroViewModel.kt:243-256` — único ponto que registra geofences de automação.
- `data/AppDao.kt` — compromissos com etapas só como `Flow`.

## Estado desejado

Após `adb reboot` ou `adb install -r`, sem abrir o app, `dumpsys alarm` lista os
alarmes futuros dos compromissos ativos e nenhum do passado; automações por
local e chegada voltam a funcionar; sem permissão, aparece "Seus lembretes estão pausados".

## Fora de escopo

- Gatilho de horário das automações (WorkManager já se reagenda).
- Repetição semanal (COMP-13).

## Decisões de design

| Decisão | Alternativas consideradas | Motivo da escolha |
|---|---|---|
| Receiver de `BOOT_COMPLETED` + `MY_PACKAGE_REPLACED` com `goAsync()` | Reagendar ao abrir o app | Não cumpre "sem abrir o app"; o usuário pode nunca abrir |
| Manter `AlarmManager` exato | Migrar lembretes para o WorkManager | WorkManager não é exato; violaria LEMB-10 |
| `agendar(..., apenasFuturos = true)` só no reagendador | Filtrar sempre | Salvar e religar mantêm o comportamento atual até a decisão de produto sobre COMP-10 |
| Chegada reagendada pelo próprio `agendar` (alarme de ativação) | Registrar geofence de chegada no reagendador | Reaproveita a Revisão 2 da 002; um caminho só |

## Conformidade com a constituição

- [ ] P1 A spec vem primeiro — aguarda aprovação
- [x] P2 Lembrete não falha em silêncio — foco da mudança, inclusive o aviso sem permissão
- [x] P3 Dados preservados — só leitura
- [x] P4 Local-first
- [x] P5 Desenho para TDAH — aviso curto e acionável
- [x] P6 Contratos explícitos — reutiliza o agendador (mesmos códigos)
- [x] P7 Critérios verificáveis — ver estratégia de testes
- [x] P8 Simplicidade — receiver padrão, sem dependências
- [x] P9 Consistência

---

## Fase 1: Regra e reagendador

### Mudanças

#### 1. Regra pura
**Arquivo**: `notifications/HorariosLembrete.kt`
**Mudança**: `filtrarFuturos(horarios, agora)` — quais alarmes ainda devem ser agendados.

#### 2. Agendador
**Arquivo**: `notifications/CompromissoAlarmScheduler.kt`
**Mudança**: parâmetro `apenasFuturos` em `agendar`.

#### 3. Reagendador
**Arquivo**: `notifications/Reagendador.kt` (novo)
**Mudança**: `reagendarTudo(context)`: compromissos ativos → `agendar(apenasFuturos = true)`; automações ativas com local → `GeofenceManager.addOrUpdateGeofence`.

#### 4. Consulta
**Arquivo**: `data/AppDao.kt`
**Mudança**: consulta `suspend` de compromissos ativos com etapas e itens (sem schema).

### Critérios de sucesso
#### Verificação automatizada
- [ ] `./gradlew testDebugUnitTest assembleDebug`
- [ ] `ReagendamentoTest` cobre CA-1.2 (passado descartado, futuro mantido) e CA-1.3 (desativado não gera alarmes)

---

## Fase 2: Receiver

### Mudanças
- **Arquivo**: `notifications/BootReceiver.kt` (novo) — `exported=true`, `BOOT_COMPLETED` e `MY_PACKAGE_REPLACED`, `goAsync()` → `Reagendador.reagendarTudo`.
- **Arquivo**: `AndroidManifest.xml` — `RECEIVE_BOOT_COMPLETED` + `<receiver>`.

### Critérios de sucesso
#### Verificação automatizada
- [ ] `./gradlew testDebugUnitTest assembleDebug`
#### Verificação manual
- [ ] R1 — CA-1.1: criar compromisso, `adb reboot`, não abrir o app, `dumpsys alarm` lista os alarmes; esperar o lembrete
- [ ] R2 — CA-2.1: instalar a versão 1.5, criar compromisso, `adb install -r` a nova, conferir título e "Já Saí"
- [ ] R4 — CA-4.1: automação por local, `adb reboot`, entrar no local → "Hora de Focar!"

---

## Fase 3: Aviso sem permissão

### Mudanças
- **Arquivo**: `notifications/Reagendador.kt` — sem alarme exato → notificação de RF-004 com `contentIntent` para a `MainActivity`.

### Critérios de sucesso
#### Verificação automatizada
- [ ] `./gradlew testDebugUnitTest assembleDebug`
#### Verificação manual
- [ ] R3 — CA-3.1: Android 12, revogar alarmes exatos, `adb reboot` → "Seus lembretes estão pausados"; tocar abre o app

---

## Estratégia de testes

| Critério (spec) | Tipo | Teste | Fase |
|---|---|---|---|
| CA-1.2, CA-1.3 | unitário | `ReagendamentoTest` | 1 |
| CA-1.1 | manual | R1 | 2 |
| CA-2.1 | manual | R2 | 2 |
| CA-4.1 | manual | R4 | 2 |
| CA-3.1 | manual | R3 | 3 |
| CA-1.4 (proposto) | manual | a definir após `/esclarecer` | 2 |

## Rastreabilidade

| Requisito | Fase(s) |
|---|---|
| RF-001, RF-002 | 1, 2 |
| RF-003 | 1 (via `agendar`), 2 |
| RF-004 | 3 |
| RF-005 | 1, 2 |
| RNF-001 | 1 (request codes + `FLAG_UPDATE_CURRENT`) |

## Riscos e mitigação

| Risco | Impacto | Mitigação |
|---|---|---|
| Reagendar dispara lembretes antigos | Alto | `apenasFuturos = true` + `ReagendamentoTest` |
| Receiver estoura o tempo com muitos compromissos | Médio | `goAsync()`; operações locais e rápidas |

## Migração e rollback

- Room: uma consulta nova, sem schema. Manifest: permissão + receiver (a Play Store não exige justificativa).
- Na atualização, todos os lembretes futuros são reagendados com os extras corrigidos.
- **Rollback:** reverter os commits e remover o receiver do manifest.

## Atualização das capacidades ao final

- [ ] `capacidades/lembretes.md`: LEMB-11 com o texto novo, status ✅
- [ ] `capacidades/automacoes.md`: AUTO-05 com o texto novo
- [ ] `arquitetura.md`: riscar DT-05; §3.1 diagrama com o `BootReceiver`; §7 permissão; histórico

## Referências

- Spec: `docs/sdd/specs/003-reagendar-apos-reiniciar/spec.md`
- Pesquisa: `docs/sdd/specs/003-reagendar-apos-reiniciar/pesquisa.md`
- Padrão de receiver com `goAsync()`: `NotificationActionReceiver` (`ATIVAR_CHEGADA`, spec 002)
