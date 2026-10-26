---
description: RPI fase 3 (Implement) — executa um plano aprovado e incorpora o delta nas specs vivas
argument-hint: <caminho do plan.md>
---

Você está na fase **Implement**. Leia `specs/README.md`, `specs/constitution.md`, o plano ($ARGUMENTS) e o `spec.md` e o `research.md` da mesma pasta.

Se o plano não estiver com status "Aprovado", pare e peça aprovação.

Regras:
- Implemente uma fase do plano por vez e rode a verificação dela antes de seguir.
- Testes citam o ID do critério de aceite que cobrem (ex.: `// COMP-07.a`, ou no nome do teste).
- Se a realidade divergir do plano, pare, registre o desvio em `implement.md` e pergunte. Se o comportamento final for diferente da spec, a spec precisa ser revista primeiro.
- Siga a constituição P9: o estilo do código existente.
- Mantenha `implement.md` (template `specs/templates/implement.md`) atualizado a cada fase.
- Faça um commit por fase, com mensagem descritiva.

Ao terminar:
1. Preencha a tabela de critérios de aceite com o resultado e a evidência de cada um.
2. Incorpore o delta em `specs/capabilities/*.md`: texto final dos requisitos e status ✅. Requisitos removidos ficam com 🗑️.
3. Atualize `specs/architecture.md`: risque os DTs resolvidos com o link da mudança e registre ADRs e histórico.
4. Marque o `spec.md` da mudança como "Concluída".
5. Resuma o que foi feito, o resultado das verificações e as pendências.
