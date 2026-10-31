---
data: 2026-10-30 22:49
spec: docs/sdd/specs/005-lembretes-na-edicao-e-fim-do-evento/spec.md
plano: (ainda não existe; spec em rascunho)
branch: ccr-80e3a1a2-ocxhin
commit: 1da2e03
fase_atual: especificação da 005 (rascunho, revisão pelo revisor-de-spec não concluída) · 003 em validação (faltam M6–M10 no emulador)
---

# Passagem: spec 005 em rascunho; 003 aguardando M6–M10 no emulador

## Tarefa

Sessão que retomou a 003 (validação em aparelho), aplicou dois ajustes aprovados da revisão de código, arrumou os `.idea/*` e abriu a spec 005 a partir de dois bugs achados pelo Bruno nos testes. Fluxo da 005: `/especificar` em andamento (spec escrita; revisão pendente) → depois `/esclarecer`.

## Estado atual

- ✅ 003: M1–M5 aprovados em aparelho (2026-10-30, informado pelo Bruno); B5 da 002 repetido dado como coberto pelo M4 (decisão do Bruno). Checkboxes marcados em `docs/sdd/specs/003-reagendar-apos-reiniciar/plano.md` (`fffe0ca`).
- ✅ 003: ajustes `1da2e03` — `QUICKBOOT_POWERON` no `BootReceiver` e ativação da chegada por alarme **inexato** quando não há alarme exato e o período ainda não começou. `testDebugUnitTest assembleDebug` → 30/30. Registrado em "Decisões de design" e "Registro da implementação" do plano; caso novo no roteiro M7.
- ✅ `.idea`: `appInsightsSettings.xml` no `.gitignore` (`d4c22bf`); JDK 22 da IDE commitado (`615ccfe`, opção escolhida pelo Bruno).
- 🔄 005: `spec.md` escrita (status `rascunho`, 3 marcadores). O `revisor-de-spec` foi acionado mas a sessão encerrou **antes do resultado** — rodar a revisão de novo e corrigir os bloqueantes que não dependem do Bruno.
- 🔄 `docs/sdd/roadmap.md` modificado (005 no backlog, "Em andamento", duas decisões de 2026-10-30) e a pasta da 005 **não commitados**.
- ⏳ 003: M6, M7 (API 31/32), M8, M9, M10 no emulador → `/validar 003`. 001 (A4, R4) e 002 (R4, R5) também pendentes; dá para juntar com o M7 no emulador 12/12L.

## Mudanças feitas nesta sessão

- `app/src/main/java/com/brunocsantos/agoravai/notifications/BootReceiver.kt` — `ACTION_QUICKBOOT_POWERON` (companion) tratado como reinício.
- `app/src/main/AndroidManifest.xml` — ação `android.intent.action.QUICKBOOT_POWERON` no receiver.
- `notifications/CompromissoAlarmScheduler.kt` — `agendarAtivacaoChegadaInexata` (`setAndAllowWhileIdle`, mesmo `pendingAtivacaoChegada`, então `cancelar` continua acertando).
- `notifications/Reagendador.kt` — sem alarme exato: `ativacaoChegada <= agora` → `addGeofenceChegada`; senão alarme inexato.
- `docs/sdd/specs/005-lembretes-na-edicao-e-fim-do-evento/spec.md` — novo.

## Aprendizados importantes

- **Bug 2 não é só visual:** nada desliga `isEnabled` depois do evento; religar um compromisso passado chama `agendar` (`ui/compromissos/CompromissoViewModel.kt:222-233`), que não filtra horários passados → tudo toca na hora (observação já existente em COMP-10, `docs/sdd/capacidades/compromissos.md:87`). Só `deleteExpiredCompromissos` (`data/AppDao.kt:47`, COMP-14) age após o evento.
- **Bug 1:** salvar recusa preparo no passado (COMP-07, `CompromissoViewModel.kt:172-176`), então o lembrete que tocou durante a edição estava no horário certo; se tocou, o preparo já começou e o salvar seria recusado — por isso o CA-1.6 (sair após salvar recusado).
- `nova-spec.sh` da skill `/especificar` falha neste Windows (chama um `python` que cai no alias da Microsoft Store); criar a pasta à mão. `python -I <arquivo>` direto funciona.
- Armadilha: `cat > arquivo` sem heredoc/stdin trava o Bash (aconteceu; tarefa interrompida). E arquivos do projeto misturam LF/CRLF (`Reagendador.kt`, `CompromissoAlarmScheduler.kt`, manifest em CRLF; `BootReceiver.kt` em LF) — usar Edit ou scripts com `newline=''`.
- Ao exibir duas saídas de comando sem separador, confundi a listagem `git ls-files .idea` com o fim do `.gitignore` e afirmei errado que os `.idea/*` estavam ignorados; corrigido com o Bruno. Os `.idea/*` versionados (compiler, gradle, misc, vcs…) são intencionais desde o commit inicial.
- Se um aparelho enviar `QUICKBOOT_POWERON` **e** `BOOT_COMPLETED`, o reagendamento roda duas vezes; idempotente exceto a saída perdida, que pode tocar de novo (mesmo ID de notificação, substitui).

## Decisões tomadas

- 003: incluir `QUICKBOOT_POWERON` e alarme inexato para a ativação da chegada sem alarme exato — aprovado pelo Bruno — registrado no plano e no roadmap.
- 003: B5 da 002 coberto pelo M4 — Bruno — registrado no plano.
- `.idea`: commitar JDK 22 (opção 2) e ignorar `appInsightsSettings.xml` — Bruno — commits próprios.
- 005, bug 1: lembretes do compromisso em edição **esperam** enquanto o formulário está aberto; salvar → vale o novo agendamento; sair sem salvar → toca na hora o que deveria ter tocado — Bruno — na spec.
- 005, bug 2: compromisso sem repetição **desliga automaticamente** após terminar; religar evento passado é bloqueado com mensagem — Bruno (opção recomendada) — na spec.
- 005 numa spec só, com duas histórias (P1 edição, P2 encerrado) — proposta aceita pelo Bruno.

## Tentativas que não funcionaram

- `bash nova-spec.sh` (ver acima).
- Script de edição com `cat > $TMP/p.py` perdido no início do comando: travou esperando stdin.

## Artefatos relevantes

- `docs/sdd/specs/005-lembretes-na-edicao-e-fim-do-evento/spec.md`
- `docs/sdd/specs/003-reagendar-apos-reiniciar/plano.md` (roteiros M6–M10, "Registro da implementação")
- `docs/sdd/roadmap.md`
- `docs/sdd/capacidades/compromissos.md` (COMP-07, 09, 10, 14) e `docs/sdd/capacidades/lembretes.md` (LEMB-08, 09; LEMB-12 será nova)

## Próximos passos

1. ~~Revisão da spec~~ — feita após esta passagem: os bloqueantes independentes do Bruno foram corrigidos (CA-1.3 reescrito para testar a retenção, RF-010 para reativar ao salvar, CS-003 restrito a "sem repetição, depois do evento", RF-006 com tela apagada/bloqueada, CAs com "em até 5 s"). As decisões que dependem dele estão em §12 da spec, abaixo das 3 questões marcadas.
2. ~~Commit~~ — feito (`930dda6` + commit das correções da revisão).
3. `/esclarecer` da 005 com as 3 questões marcadas e as 5 da revisão em §12: RF-005 (chegada durante a edição: continua ou retida?), RF-006 (sair do app com o formulário aberto: toca na hora ou segue retido? — P2 sugere tocar), RF-007 (termina no horário do evento ou ao fim do período de chegada, +2 h?). Validar também o texto proposto "Esse compromisso já passou. Edite a data para ativá-lo de novo." e a premissa de tocar só o lembrete mais recente.
4. Em paralelo: resultados de M6–M10 no emulador → marcar no plano da 003 → `/validar 003`.

## Notas

- Preferência do Bruno: só recomendar nova sessão quando o contexto não for mais suficiente.
- Nenhum push feito; tudo na branch local `ccr-80e3a1a2-ocxhin`.
- `stash@{0}` (implementação antiga paralela) continua sem aplicar.
- Spec 004 segue em rascunho.
