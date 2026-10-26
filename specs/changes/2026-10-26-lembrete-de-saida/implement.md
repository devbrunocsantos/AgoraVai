# Implement — Lembrete de saída confiável

- **Spec:** [`spec.md`](spec.md) · **Plano:** [`plan.md`](plan.md)
- **Branch:** `ccr-80e3a1a2-ocxhin`
- **Início / fim:** 2026-10-26 / —

## Progresso por fase

| Fase | Status | Commit | Verificação |
|---|---|---|---|
| 1 — Testes e cálculo puro | ✅ feito | ver `git log` ("fase 1") | build ✅ · testes 4/4 ✅ |
| 2 — Agendador e correção dos extras | ⬜ pendente | | |
| 3 — Tela cheia | ⬜ pendente | | |
| 4 — Permissão de alarme exato | ⬜ pendente | | |

## Desvios do plano
- Nenhum até aqui.

## Critérios de aceite

| Critério | Resultado | Evidência |
|---|---|---|
| LEMB-01.a (regressão) | ✅ | `HorariosLembreteTest` |
| LEMB-08 (+10 min) | ✅ | `HorariosLembreteTest` |
| COMP-07.a (regressão do cálculo) | ✅ | `HorariosLembreteTest` |

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
- …
