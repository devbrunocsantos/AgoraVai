---
description: RPI fase 2 (Plan) — transforma spec + research em plano verificável (sem mudar código)
argument-hint: <caminho do research.md>
---

Você está na fase **Plan**. Leia `specs/README.md`, `specs/constitution.md`, `specs/architecture.md`, o research ($ARGUMENTS) e o `spec.md` da mesma pasta.

Regras:
- NÃO altere código nem specs vivas. O único arquivo que você cria é `plan.md`, na mesma pasta.
- Use `specs/templates/plan.md`.
- Preencha a matriz de verificação: **todo** critério de aceite do delta tem um teste (que citará o ID do critério) ou um roteiro manual concreto.
- Preencha o checklist da constituição. Toda exceção precisa de justificativa.
- Fases pequenas, cada uma com os critérios que cobre e uma verificação concreta (`./gradlew assembleDebug`, `./gradlew test`, roteiro).
- Mudança de schema do Room exige `Migration` explícita e schema exportado (P3).
- Se o research estiver incompleto ou contradisser a spec, aponte o problema em vez de adivinhar.

Deixe o status como "Rascunho". Termine com o plano resumido e peça aprovação explícita antes do `/implement`.
