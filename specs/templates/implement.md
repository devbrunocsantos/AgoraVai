# Implement — <título da mudança>

- **Spec:** [`spec.md`](spec.md) · **Plano:** [`plan.md`](plan.md)
- **Branch:** `<branch>`
- **Início / fim:** AAAA-MM-DD / AAAA-MM-DD

## Progresso por fase

| Fase | Status | Commit | Verificação |
|---|---|---|---|
| 1 — <nome> | ⬜ pendente / 🟨 em andamento / ✅ feito | `abc1234` | build ✅ · testes ✅ · manual ✅ |
| 2 — <nome> | ⬜ | | |

## Desvios do plano
<O que foi diferente e por quê. Se o comportamento mudou em relação à spec, a spec precisa ser revista e reaprovada.>

## Critérios de aceite

| Critério | Resultado | Evidência |
|---|---|---|
| PREFIXO-NN.a | ✅ / ❌ | teste `…` / roteiro manual executado em <aparelho, Android X> |

## Saída das verificações
```
$ ./gradlew assembleDebug
…
$ ./gradlew test
…
```

## Incorporação nas specs vivas
- [ ] Delta incorporado em `specs/capabilities/<arquivo>.md` (texto final + status ✅)
- [ ] `specs/architecture.md` atualizado (DTs riscados, ADR, histórico)
- [ ] `spec.md` desta mudança marcada como **Concluída**

## Pendências / follow-ups
- …
