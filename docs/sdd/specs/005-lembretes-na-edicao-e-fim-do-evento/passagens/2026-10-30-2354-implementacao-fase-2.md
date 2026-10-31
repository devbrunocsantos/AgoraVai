---
data: 2026-10-30 23:54
spec: docs/sdd/specs/005-lembretes-na-edicao-e-fim-do-evento/spec.md
plano: docs/sdd/specs/005-lembretes-na-edicao-e-fim-do-evento/plano.md
branch: ccr-80e3a1a2-ocxhin
commit: a9099fa
fase_atual: 2 — Retenção durante a edição (automatizada ✅; aguardando roteiros manuais do Bruno) → depois Fase 3
---

# Passagem: 005 implementada até a Fase 2, aguardando verificação manual

## Tarefa

Spec 005 (lembretes durante a edição e compromisso encerrado), fase de **implementação** (`/implementar 005`). Nesta sessão: retomada da especificação → 2 revisões da spec → `/esclarecer` (5 perguntas + P4 revista) → `/pesquisar` → `/planejar` (aprovado) → Fases 1 e 2 implementadas e commitadas.

## Estado atual

- ✅ Spec `em-implementacao`, plano `em-implementacao` (`plano.md` com "Registro da implementação" das Fases 1–2).
- ✅ Fase 1 (`3a42e6d`): regras puras + `EdicaoLembretesTest` (13) e `EncerramentoTest` (12). 55/55.
- ✅ Fase 2 (`a9099fa`): retenção no disparo; automatizada marcada no plano. 55/55, `assembleDebug` ok.
- 🔄 Fase 2 — **verificação manual pendente** (A1–A6, B1, C1, C1b, C2, C3, C4 no plano). Não marcar sem o Bruno confirmar.
- ⏳ Fase 3 (fechar notificação de saída antiga ao salvar — 1 mudança em `CompromissoViewModel.kt`, ramo de edição do salvar), Fase 4 (encerrado/religar), Fase 5 (capacidades + `arquitetura.md` §5.2).
- ⏳ Paralelo e fora da 005: 003 M6–M10 no emulador → `/validar 003`; 001 (A4, R4) e 002 (R4, R5 bloqueado por DT-21).

## Mudanças feitas nesta sessão (código)

- `notifications/HorariosLembrete.kt` (fim do arquivo) — `deveReter`, `deveTocarSaidaAoLiberar`, `estaEncerrado`, `DecisaoAoLigar`, `decisaoAoLigar`.
- `notifications/Reagendador.kt:151-152` — `planoDe` público (usado pelo VM).
- `services/EdicaoEmAndamento.kt` (novo) — `iniciar`, `tornarVisivel`, `reter`, `suspender(): Pair<Long?,Boolean>`, `encerrar(): Pair<Long?,Boolean>`.
- `notifications/AlarmReceiver.kt` — const `EXTRA_SEM_RETENCAO`; no `onReceive`, retorno antecipado se `EdicaoEmAndamento.reter(id)`.
- `notifications/NotificationScheduler.kt` — `scheduleAlarm(..., compromissoId = -1L)` põe `EXTRA_COMPROMISSO_ID` na etapa.
- `notifications/CompromissoAlarmScheduler.kt` — `agendarEtapa` passa o id; `dispararSaidaAgora(..., semRetencao = false)`.
- `services/NotificationActionReceiver.kt` — `CHECK_LATE` retém via `EdicaoEmAndamento.reter`.
- `ui/compromissos/CompromissoViewModel.kt` — `escopoLiberacao` top-level; `formularioAberto`; `sessaoFormulario`; `abrirCriacao`, `abrirEdicao`, `fecharSemSalvar`, `formularioSaiuDaTela`, `formularioVoltouATela`, `liberar`, `onCleared`; salvar confere a sessão antes de recusar/gravar/fechar/mostrar erro; `CancellationException` relançada.
- `MainActivity.kt` — flag do formulário vem do VM; `onStart` → `formularioVoltouATela`; `onStop` (se `!isChangingConfigurations`) → `formularioSaiuDaTela`; voltar/ícone → `fecharSemSalvar`; FAB/editar → `abrirCriacao`/`abrirEdicao`.

## Aprendizados importantes

- **`ReminderActivity` abre com `NEW_TASK | CLEAR_TASK` na task do app** (`AlarmReceiver.kt:~61`, sem `taskAffinity` em `AndroidManifest.xml:83-90`): destrói a `MainActivity`. Por isso a liberação roda fora do `viewModelScope`, e fechar a tela cheia ao salvar é inalcançável (Fase 3 só cancela a notificação `idAlarmeSaida`).
- `isEnabled = false` desliga geofence, receiver de chegada e Reagendador → "encerrado" é derivado (`estaEncerrado`), nunca gravado.
- A "regra da 003" = `planoDeReagendamento(reinicio = true)`: só a saída perdida toca (se saída passou, não confirmada, evento não começou); etapas e atraso passados descartados. Eu errei ao apresentar a P4 como "mesma regra da 003" — corrigido com o Bruno (P4 revista na §13 da spec).
- Arquivos com CRLF (`CompromissoViewModel.kt`, plano, roadmap): scripts Python de substituição precisam normalizar `\n` → newline do arquivo. Heredoc com `\r\n` dentro de script Python quebra o Bash: escrever o script com a ferramenta Write no scratchpad.
- `cancelAlarm` em `NotificationScheduler` cria `PendingIntent` com `FLAG_UPDATE_CURRENT` e intent vazio — cancela mesmo assim (extras não entram em `filterEquals`).
- Testes só com JUnit puro (sem Robolectric, constituição); `./gradlew testDebugUnitTest assembleDebug -q` + contagem nos XML de `app/build/test-results/testDebugUnitTest/`.

## Decisões tomadas (todas registradas na spec §13 e/ou no plano "Decisões de design")

- Spec: encerra no horário do evento sem cortar atraso/chegada; formulário fora da tela libera; chegada segue na edição; ao liberar/religar vale a regra da 003; religar antes do evento entra (RF-011); 3 aprovações (texto RF-008, "Atraso de N min" mantido, fechar o "HORA DE SAIR" antigo ao salvar).
- Plano: D1 reter no disparo; D2 encerrado derivado; D3 religar via plano respeitando confirmações e sem ler localização; liberar só se houve retenção; formulário no VM; liberação em escopo de processo; RF-002 só notificação.
- Implementação: contador `sessaoFormulario` em vez de cancelar o `Job` do salvar (cancelar no meio poderia deixar alarmes cancelados sem o novo agendamento).

## Tentativas que não funcionaram

- Scripts Python via heredoc com `'\r\n'` literal — o Bash quebrou/escreveu o arquivo errado; usar Write + `python -I <arquivo>`.

## Artefatos relevantes

- `docs/sdd/specs/005-lembretes-na-edicao-e-fim-do-evento/plano.md` (fases, roteiros, decisões, registro)
- `docs/sdd/specs/005-lembretes-na-edicao-e-fim-do-evento/spec.md` (§6 RFs, §13 esclarecimentos)
- `docs/sdd/specs/005-lembretes-na-edicao-e-fim-do-evento/pesquisa.md`
- `docs/sdd/roadmap.md`

## Próximos passos

1. Receber do Bruno os resultados dos roteiros da Fase 2; marcar no plano (ou corrigir falhas).
2. Fase 3: em `CompromissoViewModel.kt`, ramo `else` do salvar (após `CompromissoAlarmScheduler.cancelar(context, currentState.id, 10)`), cancelar a notificação `CompromissoAlarmScheduler.idAlarmeSaida(currentState.id)` via `NotificationManager` (modelo: `services/geofence/RegistroChegada.kt:35`). Verificação + roteiros C5–C7. Commit.
3. Fase 4 conforme o plano (constante `MENSAGEM_COMPROMISSO_PASSADO`, `agora` de 30 s no VM, card com `ligado`, novo `toggleCompromisso`). Roteiros D1–D9. Commit.
4. Fase 5: capacidades (LEMB-12 nova, LEMB-09/11, COMP-09/10/14) e `arquitetura.md` §5.2 (extras novos), spec `implementada`, roadmap. Commit. Depois `/validar 005`.

## Notas

- O Bruno prefere continuar na mesma sessão enquanto houver contexto e deu autonomia "até achar algo que precise de aprovação"; commits por fase são esperados (constituição §6).
- Nenhum push; branch local `ccr-80e3a1a2-ocxhin`. `stash@{0}` antigo continua sem aplicar.
