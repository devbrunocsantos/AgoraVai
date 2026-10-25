---
description: RPI fase 3 — executa um plan.md aprovado, fase por fase, com verificação
argument-hint: <caminho do plan.md>
---

Você está na fase **Implement** do fluxo RPI deste projeto. Leia `docs/rpi/README.md`, `docs/SDD.md` e o plano: $ARGUMENTS

Antes de começar, confirme que o plano está com status "Aprovado". Se não estiver, pare e peça aprovação.

Regras:
- Implemente uma fase do plano por vez e rode a verificação da fase antes de seguir.
- Se a realidade divergir do plano, pare, registre o desvio no `implement.md` e pergunte antes de mudar o escopo.
- Siga o estilo do código existente (nomes de domínio em português, `StateFlow` nos ViewModels, `object` para singletons).
- Mantenha `implement.md` (template `docs/rpi/templates/implement.md`, mesma pasta do plano) atualizado a cada fase.
- Faça um commit por fase com mensagem descritiva.

Ao terminar:
1. Atualize `docs/SDD.md` nas seções listadas no plano, marque os DT-xx resolvidos e adicione uma linha no histórico de versões.
2. Marque os critérios de aceite no `implement.md`.
3. Resuma o que foi feito, o resultado das verificações e as pendências.
