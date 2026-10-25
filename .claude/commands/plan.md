---
description: RPI fase 2 — transforma um research.md aprovado em plan.md (sem mudar código)
argument-hint: <caminho do research.md>
---

Você está na fase **Plan** do fluxo RPI deste projeto. Leia `docs/rpi/README.md`, `docs/SDD.md` e o research indicado: $ARGUMENTS

Regras:
- NÃO altere código. O único arquivo que você cria é `plan.md`, na mesma pasta do research.
- Baseie-se no research; se ele estiver incompleto ou errado, aponte o problema em vez de adivinhar.
- Use o template `docs/rpi/templates/plan.md`.
- Cada fase deve ser pequena e ter verificação concreta (`./gradlew assembleDebug`, `./gradlew test`, roteiro manual).
- Se houver mudança de schema do Room, planeje uma `Migration` explícita — não conte com `fallbackToDestructiveMigration` (SDD DT-03).
- Liste as seções do SDD que precisarão ser atualizadas.

Termine apresentando o plano resumido e peça aprovação explícita antes do `/implement`. Deixe o status do plano como "Rascunho".
