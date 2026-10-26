# Implement — Lembrete de saída confiável

- **Spec:** [`spec.md`](spec.md) · **Plano:** [`plan.md`](plan.md)
- **Branch:** `ccr-80e3a1a2-ocxhin`
- **Início / fim:** 2026-10-26 / —

## Progresso por fase

| Fase | Status | Commit | Verificação |
|---|---|---|---|
| 1 — Testes e cálculo puro | ✅ feito | ver `git log` ("fase 1") | build ✅ · testes 4/4 ✅ |
| 2 — Agendador e correção dos extras | ✅ feito | ver `git log` ("fase 2") | build ✅ · testes 4/4 ✅ · manual R1, R5 ⏳ (aparelho) |
| 3 — Tela cheia | ⬜ pendente | | |
| 4 — Permissão de alarme exato | ⬜ pendente | | |

## Desvios do plano
- **Fase 2:** criada a constante `EXTRA_ACTION_NOTIFICATION_ID = "EXTRA_NOTIFICATION_ID"` (mesmo valor de antes) para o agendador e o `NotificationActionReceiver` não repetirem o literal (P6). Não muda o comportamento.

## Critérios de aceite

| Critério | Resultado | Evidência |
|---|---|---|
| LEMB-01.a (regressão) | ✅ | `HorariosLembreteTest` |
| LEMB-08 (+10 min) | ✅ | `HorariosLembreteTest` |
| COMP-07.a (regressão do cálculo) | ✅ | `HorariosLembreteTest` |
| LEMB-02.a / .b | ⏳ código pronto, falta roteiro R1 | `CompromissoAlarmScheduler.agendarSaida` usa as constantes `EXTRA_TITLE`/`EXTRA_MESSAGE` |
| LEMB-03.a / .b | ⏳ código pronto, falta roteiro R1 | `EXTRA_HAS_ACTION`/`EXTRA_ACTION_INTENT`/`EXTRA_ACTION_TITLE` corretos; `EXTRA_NOTIFICATION_ID` = `id*100+99`, o mesmo que a ação "Já Saí" fecha |
| COMP-10.a, COMP-11.b | ⏳ código pronto, falta roteiro R5 | `pendingVerificacaoAtraso` é a mesma fábrica para agendar e cancelar (com a ação `CHECK_LATE`) |

## Saída das verificações
```
$ ./gradlew testDebugUnitTest assembleDebug
BUILD SUCCESSFUL
HorariosLembreteTest: tests=4 failures=0 errors=0
```

## Incorporação nas specs vivas
- [ ] Delta incorporado em `specs/capabilities/lembretes.md` e `compromissos.md`
- [ ] `specs/architecture.md` atualizado
- [ ] `spec.md` marcada como **Concluída**

## Pendências / follow-ups
- `PomodoroWorker` ainda usa o literal `"EXTRA_NOTIFICATION_ID"` (mesmo valor da constante; funciona). Trocar numa mudança do Pomodoro.
- Achado novo: `CompromissoViewModel.tempLatDestino/tempLngDestino` são campos do ViewModel que não são zerados entre salvamentos. Se o Geocoder falhar ao salvar um compromisso, ele herda as coordenadas do anterior. Afeta LEMB-09 → registrar como DT-20 ao concluir.
