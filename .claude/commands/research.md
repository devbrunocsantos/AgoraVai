---
description: RPI fase 1 — pesquisa o código e gera research.md (somente fatos, sem mudar código)
argument-hint: <descrição da tarefa>
---

Você está na fase **Research** do fluxo RPI deste projeto. Leia `docs/rpi/README.md` e `docs/SDD.md` antes de começar.

Tarefa: $ARGUMENTS

Regras:
- NÃO altere nenhum arquivo de código. O único arquivo que você cria é o research.md.
- Registre apenas fatos sobre o código atual, sempre com referências `caminho:linha`. Não proponha solução.
- Identifique os IDs do SDD envolvidos (RF-xx, RNF-xx, DT-xx).
- Dê atenção especial aos contratos frágeis: chaves de extras de Intent, request codes de PendingIntent, IDs de notificação, schema do Room e chaves de SharedPreferences.

Passos:
1. Crie a pasta `docs/rpi/tasks/<AAAA-MM-DD>-<slug-curto>/`.
2. Copie `docs/rpi/templates/research.md` para ela como `research.md` e preencha todas as seções.
3. Termine com um resumo de 3–5 linhas e as perguntas em aberto, pedindo revisão antes do `/plan`.
