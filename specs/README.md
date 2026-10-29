# Spec-Driven Development · Agora Vai

Neste projeto **a especificação vem antes do código e é a fonte da verdade**.
Código, testes e revisão de PR existem para cumprir as specs. Quando código e
spec divergem, uma das duas está errada e a divergência precisa ser registrada.

Cada mudança passa por quatro fases: **Spec → Research → Plan → Implement**.
A primeira define *o que* muda no comportamento. As outras três formam o
**RPI**, que decide *como* a mudança chega ao código.

```
             O QUÊ                          COMO (RPI)
        ┌────────────┐   ┌────────────┐   ┌──────────┐   ┌─────────────┐
pedido ►│   SPEC     │ ► │  RESEARCH  │ ► │   PLAN   │ ► │  IMPLEMENT  │ ► merge
        │  spec.md   │   │ research.md│   │ plan.md  │   │ código +    │   │
        │ (delta de  │   │ (fatos do  │   │(passos e │   │ testes +    │   ▼
        │  requisitos│   │  código)   │   │ decisões)│   │implement.md │  specs/capabilities
        └─────┬──────┘   └─────┬──────┘   └────┬─────┘   └──────┬──────┘  atualizadas
          aprovação        revisão          aprovação       critérios de
                                                            aceite verificados
```

## Estrutura

```
specs/
├── README.md              ← este guia
├── constitution.md        ← princípios inegociáveis; todo plano é checado contra eles
├── architecture.md        ← design técnico: componentes, dados, IDs, dívida técnica (DT-xx)
├── capabilities/          ← SPECS VIVAS: o comportamento atual do app
│   ├── compromissos.md    (COMP-xx)
│   ├── lembretes.md       (LEMB-xx)
│   ├── pomodoro.md        (POMO-xx)
│   ├── automacoes.md      (AUTO-xx)
│   └── sistema.md         (SIS-xx)
├── changes/               ← uma pasta por mudança em andamento ou concluída
│   └── AAAA-MM-DD-<slug>/
│       ├── spec.md        ← delta: requisitos adicionados/alterados/removidos
│       ├── research.md
│       ├── plan.md
│       └── implement.md
└── templates/
    ├── spec.md
    ├── research.md
    ├── plan.md
    └── implement.md
```

## Como escrever requisitos

Cada requisito tem um **ID estável**, uma frase no formato **EARS** e
**critérios de aceite** verificáveis.

| Padrão EARS | Forma | Uso |
|---|---|---|
| Ubíquo | O sistema DEVE … | Sempre verdadeiro |
| Evento | QUANDO \<gatilho\>, o sistema DEVE … | Reação a uma ação ou evento |
| Estado | ENQUANTO \<estado\>, o sistema DEVE … | Comportamento contínuo |
| Indesejado | SE \<condição de erro\>, ENTÃO o sistema DEVE … | Erros e casos de borda |
| Opcional | ONDE \<configuração\>, o sistema DEVE … | Depende de opção do usuário |

Os critérios de aceite usam **Dado / Quando / Então** e recebem sufixo
(`COMP-05.a`, `COMP-05.b`). Esse sufixo é citado no nome ou no comentário do
teste que o cobre, o que mantém a rastreabilidade spec → teste.

**Status** de cada requisito:

| Status | Significado |
|---|---|
| ✅ Implementado | O código cumpre o requisito |
| ⚠️ Divergente | O código existe mas não cumpre. Aponta para um `DT-xx` em `architecture.md` |
| 🧪 Aguardando verificação | O código foi corrigido ou implementado, mas algum critério ainda depende de verificação (ex.: roteiro manual em aparelho). Vira ✅ quando todos os critérios passam |
| ⬜ Planejado | Especificado mas não implementado |
| 🗑️ Removido | Mantido só para histórico (o ID não é reutilizado) |

Regras:
- IDs nunca são reaproveitados nem renumerados.
- Requisitos descrevem **comportamento observável** (o que o usuário vê, o que é gravado, quando algo dispara), não implementação.
- Textos visíveis ao usuário entram entre aspas e exatamente como devem aparecer.

## O ciclo de uma mudança

### Fase 0 — Spec (`/spec`)
Criar `changes/<data>-<slug>/spec.md` a partir de `templates/spec.md`. Ele contém:
- o problema e o resultado esperado para o usuário;
- o **delta**: requisitos **ADICIONADOS**, **ALTERADOS** (texto antes/depois) e **REMOVIDOS**, com IDs;
- critérios de aceite de cada requisito do delta;
- o que fica fora de escopo e as perguntas em aberto.

**Portão:** a spec é aprovada antes de qualquer pesquisa de implementação. Uma
correção de bug que só faz o código cumprir um requisito ⚠️ já existente pode
ter uma spec mínima, que referencia o requisito e o DT e não muda texto nenhum.

### Fase 1 — Research (`/research`)
Documenta como o código funciona hoje na área tocada pela spec. Só fatos, com
`arquivo:linha`, sem propor solução. Também confere se os status ✅/⚠️ das
specs vivas batem com o código.
**Portão:** revisão humana.

### Fase 2 — Plan (`/plan`)
Passos pequenos e ordenados. Cada passo diz quais critérios de aceite cobre e
como será verificado. O plano inclui o **checklist da constituição** e diz que
teste ou roteiro manual cobre cada critério.
**Portão:** aprovação explícita.

### Fase 3 — Implement (`/implement`)
Implementa uma fase por vez, verificando a cada passo. Ao final:
1. Todos os critérios de aceite do delta estão verificados.
2. O delta é **incorporado** em `specs/capabilities/*.md`, com status atualizados.
3. O `architecture.md` é atualizado (DTs resolvidos, ADRs novos, histórico).

A mudança só termina quando as specs vivas descrevem o novo comportamento.

## Quando usar o ciclo completo

| Mudança | Fluxo |
|---|---|
| Typo, cor, espaçamento | Direto, sem ciclo |
| Bug que viola um requisito existente | Spec mínima → RPI curto |
| Feature nova ou mudança de comportamento | **Ciclo completo** |
| Qualquer coisa em AlarmManager, Room, geofence, Service, permissões ou billing | **Ciclo completo** |

## Usando com o Claude Code

```text
/spec quero que compromissos repetidos sejam reagendados automaticamente
   → revise e aprove specs/changes/<data>-<slug>/spec.md
/research specs/changes/<data>-<slug>/spec.md
   → revise research.md
/plan specs/changes/<data>-<slug>/research.md
   → aprove plan.md
/implement specs/changes/<data>-<slug>/plan.md
```

Boas práticas com agentes:
- Abra uma conversa limpa por fase e passe só o artefato anterior. O arquivo guarda a memória da mudança, não o chat.
- Revise a spec e o research com mais rigor que o código, porque é neles que os erros se propagam.
- Não pule os portões de aprovação.

## Backlog inicial

Requisitos hoje ⚠️ divergentes ou ⬜ planejados, em ordem de impacto. As correções de bugs já têm spec, research e plano em `changes/`:

| Mudança | Cobre | Situação |
|---|---|---|
| [`2026-10-26-lembrete-de-saida`](changes/2026-10-26-lembrete-de-saida/) | DT-02, DT-06, DT-16, DT-17, DT-19 | implementada; verificados A1–A3, A5; faltam A4 e R4 |
| [`2026-10-26-deteccao-de-chegada`](changes/2026-10-26-deteccao-de-chegada/) | DT-01, DT-20 | implementada com a Revisão 2; aguardando testes B5/B6 |
| [`2026-10-26-reagendar-apos-reiniciar`](changes/2026-10-26-reagendar-apos-reiniciar/) | DT-05 | aguardando aprovação |
| [`2026-10-26-pomodoro-meta-e-progresso`](changes/2026-10-26-pomodoro-meta-e-progresso/) | DT-07, DT-08, DT-18 | aguardando aprovação |

### Ajustes anotados em 2026-10-29 (testes em aparelho)

| Ajuste | Tipo | Requisito | Causa provável |
|---|---|---|---|
| Botões editar/excluir ficam atrás do "+" ao abrir os detalhes | Bug | COMP-12 · DT-22 | Falta espaço no fim da lista para o FAB |
| Pomodoro não deixa trocar o local depois de usar "localização atual" | Bug | AUTO-03 · DT-23 | A busca de endereço só abre uma vez por sessão (flag nunca é resetada) |
| Deixar mais bonitos o "Agora Vai..." e a frase do topo | Melhoria visual | SIS-02 | — (precisa de spec com o visual desejado) |

Backlog completo:

| Mudança sugerida | Requisitos | DT |
|---|---|---|
| Corrigir extras do lembrete de saída (título, mensagem, "Já Saí") | LEMB-02, LEMB-03 | DT-02 |
| Ativar a detecção de chegada | LEMB-09 | DT-01 |
| Fechar o alarme de tela cheia ao confirmar a saída | LEMB-07 | DT-16 |
| Tela cheia bloqueada no Android 14 sem como abrir o lembrete | LEMB-04 | DT-19 |
| Endereço inexistente aceito com percurso de 0 min | COMP-05 | DT-21 |
| Botões editar/excluir atrás do "+" | COMP-12 | DT-22 |
| Busca de local do Pomodoro só abre uma vez | AUTO-03 | DT-23 |
| Migrações reais do Room | Constituição P3 | DT-03 |
| Reagendar após reiniciar o aparelho | LEMB-11 | DT-05 |
| Reagendar compromissos recorrentes | COMP-13 | DT-04 |
| Manter a meta da automação ao pausar e retomar | POMO-05 | DT-18 |
| Contar focos concluídos com o app fechado | POMO-06 | DT-08 |
