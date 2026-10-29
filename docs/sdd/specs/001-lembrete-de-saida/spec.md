---
id: 001
titulo: Lembrete de saída confiável
status: implementada  # rascunho | esclarecida | pesquisada | planejada | em-implementacao | implementada | validada
criada_em: 2026-10-26
atualizada_em: 2026-10-29
responsavel: Bruno C. Santos
capacidades: LEMB, COMP
dts: DT-02, DT-06, DT-16, DT-17, DT-19
---

# Spec 001: Lembrete de saída confiável

> Aprovada em 2026-10-26 por Bruno C. Santos · implementada em 2026-10-26.
> Verificação em aparelho: A1, A2, A3, A5 ✅ (2026-10-28); faltam **A4** (CA-2.2) e **R4** (CA-3.1, CA-3.2).
> Migrada da convenção anterior (`specs/changes/2026-10-26-lembrete-de-saida/`) em 2026-10-29.

## 1. Contexto e problema

O lembrete mais importante do app, "HORA DE SAIR", chega como "Lembrete / Hora do
compromisso" e sem o botão "Já Saí". Na tela cheia, confirmar a saída não fecha
a notificação. No Android 14, se o sistema bloquear a tela cheia, a notificação
não abre nada ao ser tocada, então o usuário não consegue confirmar e recebe
"Você está atrasado!" mesmo tendo saído. Sem permissão de alarme exato, os
lembretes deixam de ser agendados sem aviso. Isso viola diretamente a
constituição P2 (um lembrete nunca falha em silêncio) e P6 (contratos explícitos).

## 2. Objetivos

- O lembrete de saída sempre mostra o título do compromisso, a mensagem certa e uma forma de confirmar a saída.
- Confirmar a saída fecha o lembrete e cancela o aviso de atraso.
- Se o app não puder garantir o horário, o usuário fica sabendo na hora de salvar ou reativar.
- Desativar ou excluir um compromisso silencia todos os lembretes dele.

## 3. Não-objetivos

- Reagendar após reiniciar ou atualizar o app (spec 003). Alarmes **já agendados** nos aparelhos continuam com o defeito até serem reagendados; a 003 faz isso automaticamente na atualização.
- Repetição semanal (COMP-13), que depende de decisão de produto.
- Mostrar a permissão de tela cheia do Android 14 no diagnóstico de permissões (SIS-05) — follow-up.

## 4. Usuários e personas

- **Pessoa que luta contra o tempo** (inclusive com TDAH): precisa de um aviso claro, na hora certa, com uma única ação óbvia ("Já Saí").

## 5. Histórias de usuário

### História 1 — Lembrete de saída com conteúdo certo (Prioridade: P1)

Como **pessoa com compromisso**, quero **que o lembrete de saída diga qual é o compromisso, o que levar e me deixe confirmar** para **sair sem esquecer nada**.

**Por que P1:** é o lembrete central do app; hoje ele chega genérico e sem ação.

**Teste independente:** criar compromisso tipo PADRAO com e sem itens e aguardar o horário de saída.

**Critérios de aceitação:**
- **CA-1.1 (LEMB-02.a)** — **Dado** o compromisso "Dentista" com os itens [Chaves], **quando** chega o horário de saída, **então** o lembrete mostra "HORA DE SAIR: Dentista" e "Saia agora! Não esqueça seus itens.".
- **CA-1.2 (LEMB-02.b)** — **Dado** um compromisso sem itens, **quando** chega o horário de saída, **então** a mensagem é "Saia agora para chegar a tempo!".
- **CA-1.3 (LEMB-03.a)** — **Dado** o tipo `PADRAO`, **quando** o lembrete de saída aparece, **então** ele tem a ação "Já Saí" e o texto expandido termina com "📌 Não esqueça: Chaves".
- **CA-1.4 (LEMB-03.b)** — **Dado** o lembrete PADRAO na bandeja, **quando** toco em "Já Saí", **então** a notificação some e o aviso de atraso não aparece.

### História 2 — Confirmar a saída pela tela cheia (Prioridade: P1)

Como **pessoa que usa os tipos insistentes**, quero **que a tela do lembrete apareça mesmo com o celular bloqueado e que confirmar feche tudo** para **não ser cobrado por atraso depois de ter saído**.

**Por que P1:** sem isso o usuário recebe "Você está atrasado!" mesmo tendo saído.

**Teste independente:** tipo ALARME com a tela bloqueada; e Android 14 com a tela cheia revogada.

**Critérios de aceitação:**
- **CA-2.1 (LEMB-04.a)** — **Dado** o tipo `ALARME` com a tela bloqueada, **quando** chega o horário de saída, **então** a tela acende e mostra "JÁ ESTOU SAINDO".
- **CA-2.2 (LEMB-04.b)** — **Dado** que o sistema exibiu só a notificação (sem tela cheia), **quando** toco nela, **então** a tela do lembrete abre.
- **CA-2.3 (LEMB-07.b)** — **Dada** a tela cheia aberta, **quando** confirmo a saída, **então** a tela fecha e a notificação do lembrete some da bandeja.
- **CA-2.4 (LEMB-07.c)** — **Dada** a saída confirmada, **quando** se passam 10 min do horário de saída, **então** "Você está atrasado!" não aparece.

### História 3 — Aviso quando o app não pode garantir o horário (Prioridade: P1)

Como **pessoa que depende do lembrete**, quero **ser avisada na hora de salvar se o app não conseguir avisar no horário exato** para **não confiar num lembrete que não vai tocar**.

**Por que P1:** hoje a falha é silenciosa (P2).

**Teste independente:** Android 12/12L com "Alarmes e lembretes" desligado.

**Critérios de aceitação:**
- **CA-3.1 (LEMB-10.a)** — **Dada** a permissão de alarmes exatos negada, **quando** salvo um compromisso válido, **então** vejo a mensagem com "Abrir configurações" e nada é gravado.
- **CA-3.2 (LEMB-10.b)** — **Dada** a permissão negada, **quando** religo um compromisso desativado, **então** vejo a mensagem e o switch continua desligado.

### História 4 — Desativar ou excluir silencia o compromisso (Prioridade: P2)

Como **pessoa que cancelou um compromisso**, quero **que nenhum lembrete dele toque** para **não receber avisos sem sentido**.

**Teste independente:** desativar um compromisso e excluir outro antes da saída.

**Critérios de aceitação:**
- **CA-4.1 (COMP-10.a)** — **Dado** um compromisso desativado antes do horário de saída, **quando** passa o horário de saída + 10 min, **então** nenhum lembrete de saída nem aviso de atraso aparece.
- **CA-4.2 (COMP-11.b)** — **Dado** um compromisso excluído, **quando** passa o horário de saída + 10 min, **então** nenhum aviso de atraso aparece.

## 6. Requisitos funcionais

- **RF-001** (LEMB-02): No horário de saída, o sistema DEVE disparar o lembrete com título "HORA DE SAIR: <título>" e mensagem "Saia agora! Não esqueça seus itens." (com itens) ou "Saia agora para chegar a tempo!" (sem itens).
- **RF-002** (LEMB-03): ONDE o tipo é `PADRAO`, o lembrete DEVE ter a ação "Já Saí" e o texto expandido DEVE terminar com "📌 Não esqueça: <itens>".
- **RF-003** (LEMB-04): ONDE o tipo é `ALARME`, `INSISTENTE` ou `DESAFIO`, o sistema DEVE abrir uma tela cheia que acende a tela, aparece sobre o bloqueio, lista os itens e mostra o mecanismo de confirmação do tipo. SE o sistema não permitir abrir a tela cheia sozinho, ENTÃO tocar na notificação DEVE abrir essa mesma tela.
- **RF-004** (LEMB-07): QUANDO o usuário confirmar a saída (ação ou tela cheia), o sistema DEVE fechar o lembrete e cancelar o aviso de atraso.
- **RF-005** (LEMB-10): Os alarmes DEVEM ser exatos e disparar em Doze. SE o app não puder agendar alarmes exatos, ENTÃO, ao salvar ou reativar, o sistema DEVE exibir "Permita “Alarmes e lembretes” para o Agora Vai avisar na hora certa." e NÃO DEVE salvar nem reativar. Ao salvar, a mensagem DEVE oferecer a ação "Abrir configurações", que leva à tela da permissão.
- **RF-006** (COMP-10, COMP-11): Desativar ou excluir um compromisso DEVE cancelar todos os seus lembretes pendentes, inclusive o aviso de atraso.

## 7. Requisitos não funcionais

- **RNF-001** (confiabilidade): os lembretes DEVEM disparar com o app fechado e em Doze (P2).
- **RNF-002** (compatibilidade): alarmes já agendados por versões anteriores NÃO DEVEM ser duplicados ao reagendar (mesmos identificadores de agendamento).
- **RNF-003** (usabilidade): textos curtos e diretos, em pt-BR (P5).

## 8. Entidades e conceitos-chave

- **Compromisso**: evento com destino, horário, tipo de lembrete (`PADRAO`, `ALARME`, `INSISTENTE`, `DESAFIO`), etapas de preparo e itens a levar.
- **Lembrete de saída**: aviso no horário de saída; confirmar grava `confirmouSaida`.
- **Aviso de atraso**: "Você está atrasado!" 10 min após a saída, se a saída não foi confirmada (LEMB-08).

## 9. Casos de borda e erros

- Sistema bloqueia a tela cheia (Android 14 sem `USE_FULL_SCREEN_INTENT`) → CA-2.2.
- Usuário fecha a tela cheia sem confirmar → a notificação continua na bandeja como caminho de volta.
- Permissão de alarme exato revogada (Android 12/12L) → CA-3.1/3.2.

## 10. Critérios de sucesso

- **CS-001**: 100% dos lembretes de saída mostram título e mensagem corretos (zero ocorrências de "Lembrete / Hora do compromisso").
- **CS-002**: nenhum aviso de atraso após a saída confirmada.
- **CS-003**: nenhum compromisso salvo sem que o app consiga lembrá-lo no horário.

## 11. Premissas e dependências

- **Premissa**: com `USE_EXACT_ALARM` (API 33+) a permissão é automática; a revogação só ocorre no Android 12/12L.
- **Dependência**: nenhuma. As specs 002 e 003 dependem desta.

## 12. Questões em aberto

- (nenhuma)

## 13. Esclarecimentos

### Sessão 2026-10-26
- P: Sem alarme exato, bloquear ou salvar mesmo assim? → R: **bloquear** — o app nunca aceita um compromisso que não consegue lembrar na hora certa (impacto: RF-005, CA-3.1/3.2).
- P: O texto "Permita “Alarmes e lembretes” para o Agora Vai avisar na hora certa." está bom? → R: **aprovado** (impacto: RF-005).

## 14. Delta nas capacidades (constituição §8)

| Capacidade | Mudança | Status |
|---|---|---|
| LEMB-02, LEMB-03, LEMB-07 | texto sem mudança; critérios novos | ⚠️ → 🧪 (✅ após verificação) |
| LEMB-04 | **Antes:** ONDE o tipo é `ALARME`, abrir tela cheia … com o botão único "JÁ ESTOU SAINDO". **Depois:** texto de RF-003 | ✅ → ⚠️ (DT-19) → 🧪 |
| LEMB-10 | **Antes:** … avisar e oferecer o atalho para a permissão. **Depois:** texto de RF-005 | ⚠️ → 🧪 |
| COMP-10, COMP-11 | texto sem mudança; critérios novos; COMP-11.a ⚠️ → ✅ | ✅ |

## Checklist de qualidade

- [x] Sem detalhes de implementação (linguagens, frameworks, APIs, tabelas)
- [x] Requisitos testáveis e sem ambiguidade
- [x] Critérios de sucesso mensuráveis
- [x] Histórias priorizadas e independentes
- [x] Casos de borda identificados
- [x] Escopo delimitado por não-objetivos
- [x] Nenhum [PRECISA ESCLARECER] restante
