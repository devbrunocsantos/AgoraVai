---
spec: docs/sdd/specs/001-lembrete-de-saida/spec.md
pesquisa: docs/sdd/specs/001-lembrete-de-saida/pesquisa.md
data: 2026-10-26
commit_base: 791c539
status: concluido  # rascunho | aprovado | em-implementacao | concluido
---

# Plano de implementação: Lembrete de saída confiável

> Aprovado em 2026-10-26 por Bruno C. Santos. Código concluído em 2026-10-26 (branch `ccr-80e3a1a2-ocxhin`).
> Pendências de verificação manual: **R3/A4** (CA-2.2) e **R4** (CA-3.1, CA-3.2) — a cobrir em `/validar 001`.

## Visão geral

Fazer o lembrete de saída cumprir RF-001 a RF-006. No caminho, tirar a lógica de
alarmes do ViewModel para um agendador reutilizável (`CompromissoAlarmScheduler`),
que as specs 002 (chegada) e 003 (reiniciar) também usam, e introduzir os
primeiros testes unitários do projeto com o cálculo de horários em código puro.

## Estado atual

Ver `pesquisa.md`. Em resumo:
- `CompromissoViewModel.kt:240-316` — toda a lógica de alarmes, privada, com chaves de extras erradas (DT-02).
- `AlarmReceiver.kt:75-84` — tela cheia sem `contentIntent` (DT-19).
- `ReminderActivity.kt:74-80` / `NotificationActionReceiver.kt:50` — confirmação não fecha a notificação nem cancela o atraso (DT-16, DT-06).
- `CompromissoViewModel.kt:282,308` — `SecurityException` sem permissão de alarme exato, depois de gravar (DT-17).

## Estado desejado

Lembrete de saída com título, mensagem e "Já Saí"; tela cheia que abre pelo
toque quando bloqueada; confirmar fecha tudo e cancela o atraso; sem alarme
exato, nada é gravado e o usuário vê a mensagem com "Abrir configurações".
Desativar/excluir cancela todos os alarmes. Verificado por `HorariosLembreteTest`
e pelos roteiros R1–R5.

## Fora de escopo

- Reagendar após reiniciar/atualizar (spec 003).
- Unificar todas as chaves de extras num `object` com valores novos (quebraria alarmes já agendados).
- Permissão de tela cheia no diagnóstico (SIS-05).

## Decisões de design

| Decisão | Alternativas consideradas | Motivo da escolha |
|---|---|---|
| `object CompromissoAlarmScheduler` em `notifications/` com `agendar`, `cancelar`, `podeAgendarExato` | Só trocar as strings no VM | Corrige DT-02 mas deixa a lógica presa no VM: boot receiver e chegada não conseguiriam reagendar/cancelar |
| Manter os **valores** das chaves e os request codes atuais | `object IntentExtras` com valores novos | Mudaria os valores lidos por receptores e quebraria alarmes já agendados |
| Uma única fábrica para o `PendingIntent` de `CHECK_LATE` | Recriar o intent em cada lugar | Garante `filterEquals` entre agendar e cancelar (DT-06) |
| Roteiros manuais para notificações | Robolectric | Dependência pesada (P8) |
| Checar `podeAgendarExato` **antes** de gravar | Salvar com alarme aproximado + aviso | Decisão de produto Q1: bloquear |
| *(desvio, fase 2)* Constante `EXTRA_ACTION_NOTIFICATION_ID = "EXTRA_NOTIFICATION_ID"` (mesmo valor) | Repetir o literal | P6, sem mudar comportamento |
| *(desvio, fase 3)* Tela cheia com `setAutoCancel(false)` | Manter `true` | Com o novo `contentIntent`, o auto-cancel faria a notificação sumir no toque; se o usuário fechasse a tela sem confirmar, perderia o caminho de volta. Agora só some ao confirmar (RF-004) |

## Conformidade com a constituição

- [x] P1 A spec vem primeiro — spec aprovada em 2026-10-26
- [x] P2 Lembrete não falha em silêncio — foco da mudança
- [x] P3 Dados preservados — sem mudança de schema
- [x] P4 Local-first — sem rede nova
- [x] P5 Desenho para TDAH — mensagem curta com ação direta
- [x] P6 Contratos explícitos — parcial: o VM usa as constantes; literais de `ReminderActivity`/`NotificationActionReceiver` seguem iguais (unificação futura)
- [x] P7 Critérios verificáveis — primeiros testes do projeto + roteiros
- [x] P8 Simplicidade — só `junit:junit:4.13.2`
- [x] P9 Consistência — `object` em `notifications/`, nomes em português

---

## Fase 1: Testes e cálculo puro

### Objetivo
Primeiro código testável sem Android (P7) e rede de segurança para as fases seguintes.

### Mudanças

#### 1. Dependência de teste
**Arquivo**: `app/build.gradle.kts`
**Mudança**: `testImplementation("junit:junit:4.13.2")`; remover o `ExampleUnitTest.kt` vazio.

#### 2. Cálculo puro
**Arquivo**: `notifications/HorariosLembrete.kt` (novo)
**Mudança**: `calcularHorarios(dataHoraEvento, deslocamentoMin, segurancaMin, duracoesEtapasMin): HorariosLembrete(inicioEtapas, saida, verificacaoAtraso, inicioPreparo)`. O VM passa a usá-la em `salvarCompromisso` (COMP-07).

### Critérios de sucesso

#### Verificação automatizada
- [x] `./gradlew testDebugUnitTest assembleDebug` — commit `a949f01`
- [x] `HorariosLembreteTest` cobre LEMB-01.a, LEMB-08 e COMP-07.a (regressão) — 4/4

---

## Fase 2: Agendador e correção dos extras

### Objetivo
RF-001, RF-002, RF-004 (via "Já Saí") e RF-006.

### Mudanças

#### 1. Agendador
**Arquivo**: `notifications/CompromissoAlarmScheduler.kt` (novo)
**Mudança**: conteúdo de `CompromissoViewModel.kt:240-316`, com extras via constantes `EXTRA_*` do `AlarmReceiver` (DT-02), fábrica única do `PendingIntent` de `CHECK_LATE` (DT-06) e request codes idênticos (§5.2).

#### 2. ViewModel
**Arquivo**: `ui/compromissos/CompromissoViewModel.kt`
**Mudança**: chama o agendador; as funções privadas de alarme saem.

#### 3. Confirmação
**Arquivo**: `services/NotificationActionReceiver.kt`
**Mudança**: `CONFIRM_EXIT` cancela o atraso pela fábrica do agendador, no lugar de `id*100+98`.

### Critérios de sucesso

#### Verificação automatizada
- [x] `./gradlew testDebugUnitTest assembleDebug` — commit `c32dd27`

#### Verificação manual
- [x] R1 — CA-1.1 a CA-1.4 (aparelho real, 2026-10-28: testes A1, A2)
- [x] R5 — CA-4.1, CA-4.2 (aparelho real, 2026-10-28: teste A5; `dumpsys alarm` não foi possível no celular, o critério é o comportamento observável)

---

## Fase 3: Tela cheia

### Objetivo
RF-003 e RF-004 (via tela cheia).

### Mudanças

#### 1. Notificação de tela cheia
**Arquivo**: `notifications/AlarmReceiver.kt`
**Mudança**: `setContentIntent(fullScreenPendingIntent)` (DT-19) e ID da notificação repassado à `ReminderActivity`.

#### 2. Tela do lembrete
**Arquivo**: `ui/ReminderActivity.kt`
**Mudança**: repassa `EXTRA_ACTION_NOTIFICATION_ID` no `CONFIRM_EXIT` (DT-16).

### Critérios de sucesso

#### Verificação automatizada
- [x] `./gradlew testDebugUnitTest assembleDebug` — commit `db3cc6c`

#### Verificação manual
- [x] R2 — CA-2.1, CA-2.3, CA-2.4 (aparelho real, 2026-10-28: testes A1, A3)
- [ ] R3 / A4 — CA-2.2: Android 14, `adb shell appops set com.brunocsantos.agoravai USE_FULL_SCREEN_INTENT deny`, tipo DESAFIO; tocar na notificação abre a tela do lembrete

---

## Fase 4: Permissão de alarme exato

### Objetivo
RF-005.

### Mudanças

#### 1. Checagem antes de gravar
**Arquivo**: `ui/compromissos/CompromissoViewModel.kt`
**Mudança**: `salvarCompromisso` e `toggleCompromisso` checam `podeAgendarExato` antes de qualquer gravação. Campo novo `pedirPermissaoAlarme: Boolean` em `AddCompromissoUiState`.

#### 2. UI
**Arquivos**: `AddCompromissoScreen.kt`, `CompromissosScreen.kt`
**Mudança**: Snackbar com ação "Abrir configurações" (`ACTION_REQUEST_SCHEDULE_EXACT_ALARM`); no toggle, `Toast` e o compromisso não é gravado como ativo.

### Critérios de sucesso

#### Verificação automatizada
- [x] `./gradlew testDebugUnitTest assembleDebug` — commit `f43269f`

#### Verificação manual
- [ ] R4 — CA-3.1, CA-3.2: Android 12/12L, desligar "Alarmes e lembretes"; salvar mostra a mensagem com "Abrir configurações" e a lista não muda; religar um desativado mostra a mensagem e o switch fica desligado

---

## Estratégia de testes

**Roteiro base:** origem "Av. Paulista, 1000, São Paulo", destino "Av. Paulista, 1500, São Paulo", segurança 1 min, etapa "Teste" de 1 min, evento daqui a ~8 min.

| Critério (spec) | Tipo | Teste | Fase |
|---|---|---|---|
| LEMB-01.a, LEMB-08, COMP-07.a (regressão) | unitário | `HorariosLembreteTest` | 1 |
| CA-1.1, CA-1.2 | manual | R1: tipo PADRAO com itens; repetir sem itens | 2 |
| CA-1.3, CA-1.4 | manual | R1: "Já Saí", esperar 10 min, sem atraso | 2 |
| CA-4.1, CA-4.2 | manual | R5: desativar um, excluir outro; `dumpsys alarm \| grep agoravai` | 2 |
| CA-2.1, CA-2.3, CA-2.4 | manual | R2: tipo ALARME, tela bloqueada, confirmar | 3 |
| CA-2.2 | manual | R3 (A4): Android 14 sem tela cheia | 3 |
| CA-3.1, CA-3.2 | manual | R4: Android 12 sem alarme exato | 4 |

## Rastreabilidade

| Requisito | Fase(s) |
|---|---|
| RF-001, RF-002 | 2 |
| RF-003 | 3 |
| RF-004 | 2, 3 |
| RF-005 | 4 |
| RF-006 | 2 |
| RNF-002 | 2 (request codes copiados de §5.2) |

## Riscos e mitigação

| Risco | Impacto | Mitigação |
|---|---|---|
| Erro de request code quebra o cancelamento | Alto | Códigos copiados de §5.2 + R5 |
| `SecurityException` em caminho não coberto | Alto | `podeAgendarExato` antes de gravar; o agendador também protege a chamada |

## Migração e rollback

- Room: nenhum impacto. Manifest: nenhum.
- Alarmes já agendados seguem com os extras antigos até serem reagendados (a spec 003 resolve na atualização).
- Textos novos: "Permita “Alarmes e lembretes” para o Agora Vai avisar na hora certa.", "Abrir configurações".
- **Rollback:** reverter os commits das fases (sem schema, sem migração).

## Registro da implementação

- **Início / fim do código:** 2026-10-26 / 2026-10-26.
- **Saída das verificações:** `./gradlew testDebugUnitTest assembleDebug` → BUILD SUCCESSFUL; `HorariosLembreteTest: tests=4 failures=0`.
- **Incorporação nas capacidades** (commit `13c687c`):
  - [x] Delta em `capacidades/lembretes.md` e `compromissos.md`, com status 🧪 até os roteiros passarem
  - [x] `arquitetura.md` atualizado (DTs 🧪, §3.2, §5.2, DT-20, histórico 1.3)
  - [ ] Após A4 e R4: trocar 🧪 por ✅ em LEMB-04/LEMB-10 e riscar DT-02, DT-06, DT-16, DT-17, DT-19
- **Follow-ups:**
  - `PomodoroWorker` ainda usa o literal `"EXTRA_NOTIFICATION_ID"` (mesmo valor; funciona) — trocar numa mudança do Pomodoro.
  - Achado **DT-20** (`tempLatDestino/tempLngDestino` não zerados) — corrigido na spec 002.
  - Diagnóstico de permissões (SIS-05) não mostra a permissão de tela cheia do Android 14.

## Referências

- Spec: `docs/sdd/specs/001-lembrete-de-saida/spec.md`
- Pesquisa: `docs/sdd/specs/001-lembrete-de-saida/pesquisa.md`
- Contratos: `docs/sdd/arquitetura.md` §5.2
