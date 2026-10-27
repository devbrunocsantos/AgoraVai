# Implement — Detecção de chegada

- **Spec:** [`spec.md`](spec.md) · **Plano:** [`plan.md`](plan.md)
- **Branch:** `ccr-80e3a1a2-ocxhin`
- **Início / fim:** 2026-10-27 / —

## Progresso por fase

| Fase | Status | Commit | Verificação |
|---|---|---|---|
| 1 — Application | ✅ feito | ver `git log` ("chegada fase 1") | build ✅ · testes ✅ · `AgoraVaiApplication` presente no manifest mesclado ✅ · `dumpsys location` ⏳ (aparelho) |
| 2 — Regra do período | ⬜ pendente | | |
| 3 — Registro e receiver | ⬜ pendente | | |
| 4 — Coordenadas não vazam (DT-20) | ⬜ pendente | | |

## Desvios do plano
- **Aprovação:** a Fase 4 (DT-20, critério COMP-08.a) foi acrescentada ao plano na aprovação, conforme proposto ao fim da mudança `lembrete-de-saida`.

## Critérios de aceite

| Critério | Resultado | Evidência |
|---|---|---|

## Saída das verificações
```
$ ./gradlew testDebugUnitTest assembleDebug
BUILD SUCCESSFUL
```

## Incorporação nas specs vivas
- [ ] Delta incorporado em `specs/capabilities/lembretes.md` e `compromissos.md`
- [ ] `specs/architecture.md` atualizado
- [ ] `spec.md` marcada como **Concluída**

## Pendências / follow-ups
- …
