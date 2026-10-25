# RPI — Research → Plan → Implement

Fluxo de trabalho para toda mudança não trivial no **Agora Vai**. Ele separa
*entender*, *decidir* e *executar* em três fases, cada uma com um artefato
revisável. Funciona tanto para trabalho manual quanto com um agente de IA
(Claude Code): os comandos `/research`, `/plan` e `/implement` em
[`.claude/commands/`](../../.claude/commands) seguem exatamente este documento.

O RPI é a camada de execução do **SDD** ([`docs/SDD.md`](../SDD.md)): o SDD diz
*o que o sistema é e deve fazer*; o RPI diz *como uma mudança chega lá*.

```
            ┌───────────┐     ┌──────────┐     ┌─────────────┐
 pedido ──► │ RESEARCH  │ ──► │   PLAN   │ ──► │  IMPLEMENT  │ ──► PR + SDD atualizado
            │ research.md│    │ plan.md  │     │ código +    │
            │ (fatos)    │    │(decisões)│     │ implement.md│
            └─────┬──────┘    └────┬─────┘     └──────┬──────┘
                  │ revisão humana │ aprovação humana │ verificação
```

## Quando usar

| Tamanho da mudança | Fluxo |
|---|---|
| Typo, texto, ajuste visual de 1 arquivo | Direto, sem RPI |
| Bug com causa conhecida em 1–2 arquivos | RPI curto (research e plan podem ser poucas linhas) |
| Feature nova, mudança de schema, alarmes, geofence, billing, permissões | **RPI completo** |

Regra prática: se a mudança toca **AlarmManager, Room, geofence, Service ou
permissões**, faça RPI completo — são as áreas onde bugs silenciosos acontecem
(ver SDD §9).

## Onde ficam os artefatos

```
docs/rpi/
├── README.md              ← este guia
├── templates/
│   ├── research.md
│   ├── plan.md
│   └── implement.md
└── tasks/
    └── AAAA-MM-DD-<slug>/
        ├── research.md
        ├── plan.md
        └── implement.md
```

Exemplo: `docs/rpi/tasks/2026-10-25-corrigir-geofence-chegada/`.

---

## Fase 1 — Research (entender, sem mudar nada)

**Objetivo:** documentar *como o sistema funciona hoje* na área da mudança.
Apenas fatos, com referências `arquivo:linha`. Nenhuma proposta de solução.

Checklist:
- [ ] Reescrever o pedido com suas palavras e listar os IDs do SDD envolvidos (`RF-xx`, `DT-xx`).
- [ ] Mapear arquivos, classes e funções relevantes (com `caminho:linha`).
- [ ] Descrever o fluxo atual ponta a ponta (UI → ViewModel → DAO/Scheduler → Receiver).
- [ ] Listar contratos que não podem quebrar: chaves de `Intent` extra, request codes de `PendingIntent`, IDs de notificação, nomes de tabelas/colunas, chaves de SharedPreferences.
- [ ] Registrar restrições de plataforma (versão do Android, permissões, Doze, fabricantes).
- [ ] Listar perguntas em aberto.

**Saída:** `research.md` (template em [`templates/research.md`](templates/research.md)).
**Portão:** alguém leu o research e concorda que ele descreve a realidade.

> Dica: o research é a fase que mais economiza tempo. Um plano baseado num
> entendimento errado gera código errado com muita confiança.

## Fase 2 — Plan (decidir, ainda sem mudar código)

**Objetivo:** transformar o research em passos pequenos, ordenados e verificáveis.

Checklist:
- [ ] Objetivo e critérios de aceite testáveis.
- [ ] O que **não** será feito (fora de escopo).
- [ ] Abordagem escolhida e alternativas descartadas (com o porquê).
- [ ] Passos em fases; cada passo lista arquivos e a verificação (build, teste, roteiro manual).
- [ ] Impacto em dados: precisa de migração Room? Bump de versão? (ver DT-03 — nunca dependa de migração destrutiva com dados reais).
- [ ] Impacto em permissões/manifest.
- [ ] Riscos e plano de rollback.
- [ ] Seções do SDD que serão atualizadas.

**Saída:** `plan.md` (template em [`templates/plan.md`](templates/plan.md)).
**Portão:** aprovação explícita do plano antes de escrever código.

## Fase 3 — Implement (executar o plano, fase por fase)

**Objetivo:** implementar exatamente o plano aprovado, verificando a cada fase.

Regras:
1. Uma fase do plano por vez; rode a verificação da fase antes de seguir.
2. Se a realidade divergir do plano, **pare**, registre em `implement.md` e ajuste o plano (com nova aprovação se mudar o escopo).
3. Siga o estilo do código existente (nomes em português no domínio, `StateFlow` nos ViewModels, `object` para singletons de infraestrutura).
4. Commits pequenos e descritivos, um por fase quando possível.
5. Ao final: atualize o `docs/SDD.md` (seções afetadas + histórico de versões) e marque os `DT-xx` resolvidos.

Verificações mínimas por fase:
- `./gradlew assembleDebug`
- `./gradlew test` (e `connectedAndroidTest` se houver teste instrumentado)
- Roteiro manual descrito no plano (ex.: criar compromisso para daqui a 3 min, bloquear a tela, conferir alarme).

**Saída:** código + `implement.md` (template em [`templates/implement.md`](templates/implement.md)).

---

## Usando com o Claude Code

```text
/research corrigir detecção de chegada no destino (DT-01)
  → revise docs/rpi/tasks/<data>-<slug>/research.md

/plan docs/rpi/tasks/<data>-<slug>/research.md
  → revise e aprove o plan.md

/implement docs/rpi/tasks/<data>-<slug>/plan.md
```

Boas práticas com agentes:
- Comece cada fase numa conversa limpa, passando só o artefato da fase anterior — o arquivo é a memória, não o histórico do chat.
- Revise o research com mais rigor que o código: é onde erros se propagam.
- Não pule a aprovação do plano.

## Backlog sugerido para os primeiros ciclos RPI

Vindo do SDD §9, em ordem de impacto:

1. **DT-02** — unificar as chaves de extras do alarme de saída (título, mensagem e botão "Já Saí").
2. **DT-01** — inicializar `CompromissoGeofenceManager` (detecção de chegada).
3. **DT-03** — habilitar `exportSchema` e escrever migrações reais do Room.
4. **DT-05** — receiver de `BOOT_COMPLETED` para reagendar alarmes e geofences.
5. **DT-04** — reagendar compromissos recorrentes.
6. **DT-14** — extrair o cálculo de horários para uma classe pura e cobrir com testes unitários.
