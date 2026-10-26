---
description: Fase 0 (Spec) — escreve a spec delta de uma mudança (o QUÊ), sem tocar em código
argument-hint: <descrição da mudança desejada>
---

Você está na fase **Spec** do Spec-Driven Development deste projeto. Leia `specs/README.md`, `specs/constitution.md` e as specs vivas em `specs/capabilities/` relacionadas ao pedido.

Pedido: $ARGUMENTS

Regras:
- NÃO altere código nem as specs vivas. O único arquivo que você cria é o `spec.md` da mudança.
- Descreva comportamento observável, não implementação. Não cite classes nem arquivos.
- Requisitos em EARS (QUANDO / ENQUANTO / SE…ENTÃO / ONDE … o sistema DEVE …), cada um com critérios de aceite Dado/Quando/Então e sufixos `.a`, `.b`.
- Requisito novo: use o próximo ID livre do prefixo da capacidade. IDs nunca são reutilizados.
- Requisito alterado: copie o texto atual da spec viva em "Antes".
- Textos visíveis ao usuário vão entre aspas, exatos, em pt-BR, seguindo a constituição P5.
- Se o pedido for ambíguo, registre as perguntas em aberto em vez de decidir sozinho.

Passos:
1. Crie `specs/changes/<AAAA-MM-DD>-<slug-curto>/`.
2. Copie `specs/templates/spec.md` para lá e preencha.
3. Termine com o delta resumido e as perguntas em aberto, e peça aprovação antes do `/research`.
