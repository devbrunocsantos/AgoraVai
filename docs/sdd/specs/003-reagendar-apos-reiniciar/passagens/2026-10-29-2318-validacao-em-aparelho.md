---
data: 2026-10-29 23:18
spec: docs/sdd/specs/003-reagendar-apos-reiniciar/spec.md
plano: docs/sdd/specs/003-reagendar-apos-reiniciar/plano.md
branch: ccr-80e3a1a2-ocxhin
commit: 3d62620
fase_atual: validação — aguardando roteiros M1–M10 em aparelho (todas as 5 fases do plano implementadas)
---

# Passagem: 003 implementada; 001, 002 e 003 aguardando testes em aparelho

## Tarefa

Sessão longa (gerente-de-projeto) que: migrou a convenção `specs/` → `docs/sdd/`; registrou B5/B6 da 002; conduziu a 003 por esclarecer → pesquisar → planejar → implementar. Estado: **implementação concluída**, falta a rodada única de testes em aparelho e então `/validar` das três specs.

## Estado atual

- ✅ Migração para `docs/sdd/` (`e58a74b`): constituição v1.0.0 com §8 (capacidades e `arquitetura.md` **transitórias** até concluir 001–004); specs 001–004 reescritas no template novo, CAs citam o ID da capacidade (`CA-1.4 (LEMB-09.f)`); `implement.md` mesclado no `plano.md` ("Registro da implementação"); comandos antigos `/spec /research /plan /implement` removidos; `roadmap.md` criado.
- ✅ AGP 8.4.1 → 8.13.2 verificado (`clean testDebugUnitTest assembleDebug`) e commitado à parte (`f4be55c`).
- ✅ 002: B5 e B6 aprovados (informado pelo Bruno; `c5a1042`).
- ✅ 003: spec `implementada`, plano `concluido`. Commits: fase 1 `ac66a8c`, 2 `430acb1`, 3 `d82636a`, 4 `c19bb79`, correção da revisão `d61e8c3`, fase 5 (docs) `d14bf78`, roadmap `3d62620`. `./gradlew testDebugUnitTest assembleDebug` → 30/30 (`ReagendamentoTest` 20/20).
- ⏳ Testes em aparelho:
  - **003:** M1–M10 + B5 da 002 repetido (roteiros em `plano.md` § "Estratégia de testes").
  - **001:** A4 (Android 14, CA-2.2) e R4 (Android 12/12L, CA-3.1/3.2).
  - **002:** R4 (CA-3.1) e R5 (CA-3.2; difícil por causa do DT-21 — precisa de destino que o Geocoder não resolva).
- ⏳ Depois: `/validar 001`, `/validar 002`, `/validar 003`.

## Mudanças feitas nesta sessão (código, spec 003)

- `notifications/HorariosLembrete.kt` — `RAIO_CHEGADA_METROS`, `Reagendamento`, `planoDeReagendamento`, `PosicaoChegada`, `classificarPosicao`.
- `notifications/CompromissoAlarmScheduler.kt` — `MENSAGEM_PERMISSAO_ALARME` (movida do VM), `ID_NOTIFICACAO_LEMBRETES_PAUSADOS = Int.MAX_VALUE - 1`, `reagendar`, `agendarEtapa`, `intentSaida`, `dispararSaidaAgora`.
- `notifications/Reagendador.kt` (novo) e `notifications/BootReceiver.kt` (novo); `AndroidManifest.xml` com `RECEIVE_BOOT_COMPLETED` + receiver (`BOOT_COMPLETED`, `MY_PACKAGE_REPLACED`).
- `services/geofence/RegistroChegada.kt` (novo, extraído do `GeofenceBroadcastReceiver`).
- `data/AppDao.kt` — `getCompromissosAtivosComEtapas()` (sem schema).
- `AgoraVaiApplication.kt` cria o canal; removido de `MainActivity.kt`.
- Teste: `app/src/test/.../notifications/ReagendamentoTest.kt`.

## Aprendizados importantes

- `CompromissoAlarmScheduler.agendar` **não** filtra horários passados (disparam na hora) — salvar/religar continuam assim de propósito; só `reagendar` segue o plano.
- `AlarmReceiver` não consulta o banco; a precedência da chegada sobre a saída perdida é resolvida no `Reagendador` (leitura única de localização, até 4 s; fallback 45 s).
- Saída perdida só vale no `BOOT_COMPLETED` (`reinicio = true`); na atualização não repete.
- Sem alarme exato: aviso só se sobrar lembrete futuro; chegada ativada direto (`addGeofenceChegada`); saída perdida via `sendBroadcast` ao `AlarmReceiver`.
- No Windows, heredoc Python com aspas triplas dentro de `bash -c` falhou (`unexpected EOF`); usar script em arquivo no scratchpad e `python -I <arquivo>`.
- `stash@{0}` (GitHub Desktop, 2026-10-26, sobre `main`) tem uma implementação antiga paralela (BootReceiver, recorrência `proximaOcorrencia` — DT-04). **Não aplicar**; pode servir de referência para DT-04.

## Decisões tomadas

- Migrar para `docs/sdd/`; reescrever as 4 specs; mesclar `implement.md`; remover comandos antigos — registradas no roadmap.
- 003: saída perdida toca ao religar se o evento não passou (RF-002a); ambos os cenários de chegada como CA; aviso abre direto a permissão — registradas na spec §13.
- 003 plano: regra pura + `reagendar` separado; localização + fallback 45 s; tocar de novo a saída não confirmada; RNF-002 medido com ~10 e extrapolado — registradas no plano ("Decisões de design").
- Preferência do Bruno: **só recomendar nova sessão quando o contexto não for mais suficiente**.
- Fases 2–4 em sequência e testes em aparelho numa rodada única (opção A).

## Tentativas que não funcionaram

- Nenhuma de implementação. (Ver a armadilha do heredoc acima.)

## Artefatos relevantes

- `docs/sdd/specs/003-reagendar-apos-reiniciar/plano.md` — roteiros M1–M10 e "Registro da implementação" (pendências da revisão).
- `docs/sdd/specs/001-lembrete-de-saida/plano.md`, `docs/sdd/specs/002-deteccao-de-chegada/plano.md` — roteiros pendentes.
- `docs/sdd/roadmap.md` — backlog, decisões, riscos.

## Próximos passos

1. Obter do Bruno a decisão sobre as 2 pendências da revisão da 003 (ver Notas). Se aprovar alguma, implementar como ajuste pequeno (registrar em "Decisões de design" do plano).
2. Receber os resultados dos testes em aparelho; marcar os checkboxes manuais nos planos.
3. `/validar 003` (e 001/002 conforme os resultados).
4. Depois do M1: especificar DT-21 (endereço inexistente com 0 min) e planejar DT-03 (migrações do Room) antes de qualquer mudança de schema.

## Notas

- **Pendências para decisão do Bruno (revisão de código da 003):**
  1. Sem alarme exato, a ativação direta da chegada antes do período não detecta quem já está no destino quando o preparo começa. Alternativa: ativação com alarme inexato (`setAndAllowWhileIdle`, sem permissão).
  2. Incluir `android.intent.action.QUICKBOOT_POWERON` no `BootReceiver` (fabricantes antigos).
- `.idea/*` modificados (JDK 22 local) e `.idea/appInsightsSettings.xml` não versionado: configurações locais, fora dos commits por decisão; perguntar se vão para `.gitignore`.
- Spec 004 (Pomodoro) segue em `rascunho`, aguardando aprovação.
- Nenhum push foi feito; todos os commits estão só na branch local `ccr-80e3a1a2-ocxhin`.
