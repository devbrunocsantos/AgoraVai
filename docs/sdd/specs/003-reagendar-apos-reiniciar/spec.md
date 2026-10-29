---
id: 003
titulo: Reagendar após reiniciar ou atualizar
status: rascunho  # rascunho | esclarecida | pesquisada | planejada | em-implementacao | implementada | validada
criada_em: 2026-10-26
atualizada_em: 2026-10-29
responsavel: Bruno C. Santos
capacidades: LEMB, AUTO
dts: DT-05
depende_de: "001, 002"
---

# Spec 003: Reagendar após reiniciar ou atualizar

> **Ainda não aprovada.** Pesquisa e plano foram rascunhados antes da aprovação (2026-10-26) e precisam ser reconfirmados depois dela.
> Migrada da convenção anterior (`specs/changes/2026-10-26-reagendar-apos-reiniciar/`) em 2026-10-29.

## 1. Contexto e problema

Quando o celular reinicia (inclusive por falta de bateria), o Android apaga
todos os alarmes e áreas monitoradas do app. O usuário não recebe mais nenhum
lembrete dos compromissos já cadastrados e não tem como saber disso (DT-05,
viola P2). Além disso, quem atualizar o app continuaria com os lembretes
defeituosos agendados pela versão antiga (spec 001).

## 2. Objetivos

- Depois de reiniciar ou atualizar, sem o usuário abrir o app, todos os lembretes futuros voltam a estar agendados.
- A detecção de chegada (spec 002) e as automações por local voltam a funcionar.
- Se não for possível reagendar, o usuário é avisado.

## 3. Não-objetivos

- O gatilho de horário das automações: já sobrevive à reinicialização sozinho; nada muda.
- Repetição semanal (COMP-13).
- Reagir a mudança de hora/fuso do sistema: os horários são instantes absolutos e o agendamento já acompanha a mudança.

## 4. Usuários e personas

- **Pessoa que reinicia o celular ou o deixa descarregar**: não sabe que isso apaga os lembretes e não vai abrir o app para "consertar".

## 5. Histórias de usuário

### História 1 — Lembretes voltam após reiniciar (Prioridade: P1)

Como **pessoa com compromissos cadastrados**, quero **que os lembretes continuem valendo depois de reiniciar o celular** para **não perder um compromisso sem saber**.

**Por que P1:** hoje todos os lembretes somem em silêncio.

**Teste independente:** criar compromisso, reiniciar, não abrir o app e aguardar.

**Critérios de aceitação:**
- **CA-1.1 (LEMB-11.a)** — **Dado** um compromisso amanhã às 10:00 com saída às 9:15, **quando** reinicio o aparelho hoje e não abro o app, **então** às 9:15 o lembrete de saída toca.
- **CA-1.2 (LEMB-11.b)** — **Dado** um compromisso cujo alarme da primeira etapa era às 8:00, **quando** o aparelho reinicia às 8:10, **então** essa etapa não toca, mas as seguintes, a saída e o atraso continuam agendados.
- **CA-1.3 (LEMB-11.c)** — **Dado** um compromisso desativado, **quando** o aparelho reinicia, **então** nada é agendado para ele.

### História 2 — Atualizar o app corrige os lembretes antigos (Prioridade: P1)

Como **pessoa que atualiza o app**, quero **que os lembretes já agendados passem a funcionar do jeito novo** para **receber as correções da spec 001 sem recriar compromissos**.

**Critérios de aceitação:**
- **CA-2.1 (LEMB-11.d)** — **Dada** a versão anterior instalada com lembretes agendados, **quando** atualizo o app, **então** os lembretes passam a aparecer com título, mensagem e "Já Saí".

### História 3 — Aviso quando não dá para reagendar (Prioridade: P1)

Como **pessoa que depende do lembrete**, quero **ser avisada se os lembretes não puderem ser reagendados** para **corrigir a permissão a tempo**.

**Critérios de aceitação:**
- **CA-3.1 (LEMB-11.e)** — **Dada** a permissão de alarmes exatos negada, **quando** o aparelho reinicia, **então** recebo "Seus lembretes estão pausados", e tocar na notificação abre o app.

### História 4 — Automação por local após reiniciar (Prioridade: P2)

Como **pessoa que usa automação de foco por local**, quero **que ela continue funcionando após reiniciar** para **não ter que reeditar a automação**.

**Critérios de aceitação:**
- **CA-4.1 (AUTO-05.a)** — **Dada** uma automação com local ativa, **quando** reinicio o aparelho e entro no raio sem abrir o app, **então** recebo "Hora de Focar!".

## 6. Requisitos funcionais

- **RF-001** (LEMB-11): QUANDO o aparelho terminar de reiniciar ou o app for atualizado, o sistema DEVE, sem precisar que o app seja aberto, reagendar os lembretes (etapas, saída e atraso) que ainda estão no futuro, para todos os compromissos ativos.
- **RF-002** (LEMB-11): Lembretes cujo horário já passou NÃO DEVEM disparar.
- **RF-003** (LEMB-11): No mesmo momento, o sistema DEVE reativar a detecção de chegada (spec 002) dos compromissos ativos cujo período de chegada não terminou.
- **RF-004** (LEMB-11): SE o app não puder agendar alarmes exatos, ENTÃO o sistema DEVE notificar "Seus lembretes estão pausados" com "Permita “Alarmes e lembretes” para o Agora Vai avisar na hora certa.", e tocar na notificação DEVE abrir o app. [PRECISA ESCLARECER: os textos "Seus lembretes estão pausados" / "Permita “Alarmes e lembretes”…" estão aprovados?]
- **RF-005** (AUTO-05): ONDE o gatilho de local de uma automação está ligado, o monitoramento DEVE continuar após reiniciar o aparelho ou atualizar o app, sem precisar abrir o app.

## 7. Requisitos não funcionais

- **RNF-001** (idempotência): reagendar várias vezes NÃO DEVE duplicar lembretes.
- **RNF-002** (desempenho): o reagendamento termina dentro do tempo que o sistema concede após o boot, mesmo com dezenas de compromissos.

## 8. Entidades e conceitos-chave

- **Compromisso ativo**: compromisso com o switch ligado.
- **Período de chegada**: definido na spec 002.
- **Automação com local**: automação de Pomodoro com gatilho de local ligado.

## 9. Casos de borda e erros

- Celulares com criptografia de inicialização: o aviso de boot só chega após o primeiro desbloqueio — aceitável para lembretes de compromisso.
- Fabricantes que exigem "início automático" liberado (ex.: Xiaomi) — orientação já existe na tela de permissões (SIS-05).
- Reiniciar no meio do período de chegada → a detecção recomeça na hora (RF-003).

## 10. Critérios de sucesso

- **CS-001**: 100% dos lembretes futuros de compromissos ativos estão agendados após reiniciar, sem abrir o app.
- **CS-002**: zero lembretes "antigos" disparando após reiniciar.

## 11. Premissas e dependências

- **Dependência**: spec 001 (agendador reutilizável) e spec 002 (alarme de ativação da chegada — com a Revisão 2, reagendar os lembretes já reagenda a detecção de chegada).

## 12. Questões em aberto

- Q5 — textos da notificação de RF-004 (ver marcador).
- Não há critério de aceitação para RF-003 (chegada após reiniciar). Proposta: **CA-1.4** — *Dado um compromisso cujo preparo começa depois do reinício, quando reinicio e não abro o app, então a chegada é detectada no período.* Confirmar em `/esclarecer`.

## 13. Esclarecimentos

<!-- Preenchido por /esclarecer -->

## 14. Delta nas capacidades (constituição §8)

| Capacidade | Mudança |
|---|---|
| LEMB-11 | **Antes:** QUANDO o aparelho reiniciar ou a hora do sistema mudar, reagendar alarmes e geofences de todos os compromissos ativos e futuros. **Depois:** RF-001 a RF-004 (mudança de hora sai do requisito). ⬜ → ✅ |
| AUTO-05 | Acrescenta RF-005 ao texto atual |

## Checklist de qualidade

- [x] Sem detalhes de implementação (linguagens, frameworks, APIs, tabelas)
- [x] Requisitos testáveis e sem ambiguidade
- [x] Critérios de sucesso mensuráveis
- [x] Histórias priorizadas e independentes
- [x] Casos de borda identificados
- [x] Escopo delimitado por não-objetivos
- [ ] Nenhum [PRECISA ESCLARECER] restante
