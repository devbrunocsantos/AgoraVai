# SDD + RPI · Agora Vai

Neste projeto **a especificação vem antes do código e é a fonte da verdade**
(constituição P1). Cada funcionalidade ou correção passa pelo fluxo
Spec-Driven Development + RPI (Research → Plan → Implement):

```
/constituicao → /especificar → /esclarecer → /pesquisar → /planejar → /implementar → /validar
                    spec.md      (spec)       pesquisa.md   plano.md     código +       validacao.md
                                                            ▲ aprovação  plano marcado
                                              ▲ revisão humana
```

Contexto: `/passagem` gera um handoff para continuar em sessão nova; `/retomar` o retoma.
Painel: `/status-sdd`. O roadmap e o backlog ficam em [`roadmap.md`](roadmap.md).

## Estrutura

```
docs/sdd/
├── README.md            ← este guia
├── constituicao.md      ← princípios P1–P9, stack, comandos de verificação, definição de pronto
├── roadmap.md           ← visão, marcos, backlog, decisões e riscos
├── specs/
│   └── NNN-<slug>/
│       ├── spec.md      ← o QUÊ e o PORQUÊ (status no frontmatter)
│       ├── pesquisa.md  ← fatos do código com arquivo:linha
│       ├── plano.md     ← fases, mudanças por arquivo, critérios de sucesso, registro da implementação
│       ├── validacao.md ← evidências da validação
│       └── passagens/   ← handoffs entre sessões
├── capacidades/         ← TRANSITÓRIO (constituição §8): specs vivas COMP, LEMB, POMO, AUTO, SIS
└── arquitetura.md       ← TRANSITÓRIO: design técnico, contratos (§5.2), dívida técnica DT-xx
```

**Status da spec:** `rascunho` → `esclarecida` → `pesquisada` → `planejada` → `em-implementacao` → `implementada` → `validada`.
**Status do plano:** `rascunho` → `aprovado` → `em-implementacao` → `concluido`.

## Quando usar o fluxo completo

| Mudança | Fluxo |
|---|---|
| Typo, cor, espaçamento | Direto, sem fluxo |
| Bug que viola um requisito existente | Spec curta (referencia o requisito e o DT) → pesquisa → plano |
| Feature nova ou mudança de comportamento | **Fluxo completo** |
| Qualquer coisa em AlarmManager, Room, geofence, Service, permissões ou billing | **Fluxo completo** |

## Convenções transitórias (capacidades)

Enquanto `capacidades/` existir (até concluir as specs 001–004):

- Cada requisito vivo tem **ID estável** (`COMP-05`), frase em **EARS** e critérios `ID.letra` (`COMP-05.a`). IDs nunca são reaproveitados.

  | EARS | Forma |
  |---|---|
  | Ubíquo | O sistema DEVE … |
  | Evento | QUANDO \<gatilho\>, o sistema DEVE … |
  | Estado | ENQUANTO \<estado\>, o sistema DEVE … |
  | Indesejado | SE \<condição\>, ENTÃO o sistema DEVE … |
  | Opcional | ONDE \<configuração\>, o sistema DEVE … |

- Status: ✅ implementado · ⚠️ divergente (aponta `DT-xx`) · 🧪 aguardando verificação · ⬜ planejado · 🗑️ removido.
- Nas specs, cada `CA-x.y` cita o ID da capacidade entre parênteses — ex.: `CA-1.4 (LEMB-09.f)` — e a seção "Delta nas capacidades" lista o que muda.
- Testes citam no nome ou comentário o ID que cobrem (os existentes usam os IDs de capacidade).
- Ao concluir uma spec, a fase final incorpora o delta em `capacidades/` e atualiza `arquitetura.md` (DTs, ADRs, histórico).

## Histórico

Até 2026-10-28 o projeto usava `specs/` (`constitution.md`, `capabilities/`, `changes/AAAA-MM-DD-<slug>/` com `research.md`, `plan.md`, `implement.md`). Em 2026-10-29 tudo foi migrado para cá; o histórico dos arquivos é preservado pelo `git log --follow`.
