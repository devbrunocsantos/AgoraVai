---
description: RPI fase 1 (Research) — mapeia o código ligado a uma spec aprovada (somente fatos)
argument-hint: <caminho do spec.md>
---

Você está na fase **Research**. Leia `specs/README.md`, `specs/architecture.md`, a spec da mudança ($ARGUMENTS) e as specs vivas que ela afeta.

Se a spec não estiver com status "Aprovada", pare e peça aprovação.

Regras:
- NÃO altere código nem specs. O único arquivo que você cria é `research.md`, na mesma pasta da spec.
- Registre só fatos sobre o código atual, sempre com `caminho:linha`. Não proponha solução.
- Para cada requisito do delta e cada requisito vizinho afetado, confira se o status na spec viva (✅/⚠️/⬜) bate com o código. Divergências são achados importantes.
- Dê atenção especial aos contratos da constituição P6: extras de Intent, request codes, IDs de notificação e geofence, schema do Room, SharedPreferences.

Use `specs/templates/research.md`. Termine com um resumo de 3 a 5 linhas e as perguntas em aberto, e peça revisão antes do `/plan`.
