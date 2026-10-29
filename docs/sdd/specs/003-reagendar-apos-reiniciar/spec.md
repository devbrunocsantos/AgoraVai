---
id: 003
titulo: Reagendar após reiniciar ou atualizar
status: pesquisada  # rascunho | esclarecida | pesquisada | planejada | em-implementacao | implementada | validada
criada_em: 2026-10-26
atualizada_em: 2026-10-29
responsavel: Bruno C. Santos
capacidades: LEMB, AUTO
dts: DT-05
depende_de: "001, 002"
---

# Spec 003: Reagendar após reiniciar ou atualizar

> **Esclarecida em 2026-10-29** (3 perguntas). Pesquisa (parcial) e plano foram rascunhados em 2026-10-26, antes da aprovação, e precisam ser refeitos/reconfirmados.
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
- Avisar quando a permissão de localização em segundo plano estiver revogada após o reinício: a chegada não é detectada nesse caso; o diagnóstico de permissões (SIS-05) já mostra a falta — avaliar aviso ativo em spec própria.

## 4. Usuários e personas

- **Pessoa que reinicia o celular ou o deixa descarregar**: não sabe que isso apaga os lembretes e não vai abrir o app para "consertar".

## 5. Histórias de usuário

### História 1 — Lembretes voltam após reiniciar (Prioridade: P1)

Como **pessoa com compromissos cadastrados**, quero **que os lembretes continuem valendo depois de reiniciar o celular** para **não perder um compromisso sem saber**.

**Por que P1:** hoje todos os lembretes somem em silêncio.

**Teste independente:** criar compromisso, reiniciar, não abrir o app e aguardar.

**Critérios de aceitação:**
- **CA-1.1 (LEMB-11.a)** — **Dado** um compromisso amanhã às 10:00 com saída às 9:15, **quando** reinicio o aparelho hoje e não abro o app, **então** às 9:15 o lembrete de saída toca.
- **CA-1.2 (LEMB-11.b)** — **Dado** um compromisso com etapas às 8:00 e 8:20 e saída às 9:15, **quando** o aparelho fica desligado das 7:55 às 8:30, **então** nenhuma das duas etapas toca, e a saída e o atraso continuam agendados.
- **CA-1.3 (LEMB-11.c)** — **Dado** um compromisso desativado, **quando** o aparelho reinicia, **então** nada é agendado para ele.
- **CA-1.4 (LEMB-11.f)** — **Dado** um compromisso às 10:00 com saída às 9:15, saída não confirmada e o usuário fora do destino, **quando** o aparelho fica desligado das 9:00 às 9:20, **então** "HORA DE SAIR" toca em até 1 min após o primeiro desbloqueio depois do reinício, e o aviso de atraso continua agendado para 9:25.
- **CA-1.5 (LEMB-11.g)** — **Dado** um compromisso às 10:00 com saída às 9:15, **quando** o aparelho fica desligado das 9:00 às 10:05, **então** nem as etapas, nem "HORA DE SAIR", nem "Você está atrasado!" tocam.
- **CA-1.6 (LEMB-11.h)** — **Dado** um compromisso às 10:00 com preparo às 8:30 e destino localizado, **quando** reinicio o aparelho às 8:00, não abro o app e entro no raio do destino às 9:40, **então** recebo "Você chegou! 🏁" e os lembretes pendentes não tocam.
- **CA-1.7 (LEMB-11.i)** — **Dado** um compromisso às 10:00 com saída às 9:15 e que estou no destino, **quando** o aparelho fica desligado das 9:00 às 9:20 e eu não saio do lugar, **então** recebo "Você chegou! 🏁" em até 1 min após o primeiro desbloqueio depois do reinício, e "HORA DE SAIR" e "Você está atrasado!" não tocam.

### História 2 — Atualizar o app corrige os lembretes antigos (Prioridade: P1)

Como **pessoa que atualiza o app**, quero **que os lembretes já agendados passem a funcionar do jeito novo** para **receber as correções da spec 001 sem recriar compromissos**.

**Critérios de aceitação:**
- **CA-2.1 (LEMB-11.d)** — **Dada** a versão anterior instalada com um compromisso tipo PADRAO com lembretes agendados, **quando** atualizo o app, **então** os lembretes passam a aparecer com título, mensagem e "Já Saí".

### História 3 — Aviso quando não dá para reagendar (Prioridade: P1)

Como **pessoa que depende do lembrete**, quero **ser avisada se os lembretes não puderem ser reagendados** para **corrigir a permissão a tempo**.

**Critérios de aceitação:**
- **CA-3.1 (LEMB-11.e)** — **Dada** a permissão de alarmes exatos negada e um compromisso ativo com lembrete futuro, **quando** o aparelho reinicia, **então** recebo "Seus lembretes estão pausados" com "Permita “Alarmes e lembretes” para o Agora Vai avisar na hora certa.", e tocar na notificação abre a tela da permissão "Alarmes e lembretes". *(Só reproduzível no Android 12/12L, como LEMB-10.)*
- **CA-3.2 (LEMB-11.j)** — **Dado** o mesmo cenário, **quando** o app é atualizado, **então** recebo o mesmo aviso.

### História 4 — Automação por local após reiniciar (Prioridade: P2)

Como **pessoa que usa automação de foco por local**, quero **que ela continue funcionando após reiniciar** para **não ter que reeditar a automação**.

**Critérios de aceitação:**
- **CA-4.1 (AUTO-05.a)** — **Dada** uma automação com local ativa, **quando** reinicio o aparelho e entro no raio sem abrir o app, **então** recebo "Hora de Focar!".
- **CA-4.2 (AUTO-05.b)** — **Dada** uma automação com local ativa, **quando** atualizo o app e entro no raio sem abri-lo, **então** recebo "Hora de Focar!".

## 6. Requisitos funcionais

- **RF-001** (LEMB-11): QUANDO o aparelho terminar de reiniciar ou o app for atualizado, o sistema DEVE, sem precisar que o app seja aberto, reagendar os lembretes (etapas, saída e atraso) que ainda estão no futuro, para todos os compromissos ativos.
- **RF-002** (LEMB-11): Lembretes de etapa e de atraso cujo horário passou enquanto o aparelho estava desligado NÃO DEVEM disparar. Lembretes de atraso ainda no futuro seguem o RF-001, mesmo que caiam depois do horário do evento.
- **RF-002a** (LEMB-11): SE o horário de saída passou enquanto o aparelho estava desligado, a saída não foi confirmada, a chegada não foi considerada no reinício (RF-003) e o horário do evento ainda não chegou, ENTÃO o sistema DEVE disparar o lembrete de saída imediatamente após o reinício. Se o horário do evento já passou, as etapas e a saída perdidas NÃO DEVEM disparar; a detecção de chegada segue o RF-003.
- **RF-003** (LEMB-11, LEMB-09): No mesmo momento, o sistema DEVE reativar a detecção de chegada dos compromissos ativos cujo período de chegada não terminou. SE o usuário já estiver dentro do raio quando a detecção for ativada ou reativada durante o período, ENTÃO a chegada DEVE ser considerada nesse momento, antes de qualquer lembrete de saída perdido (RF-002a).
- **RF-004** (LEMB-11): QUANDO o reagendamento do RF-001 ocorrer e houver ao menos um compromisso ativo com lembrete futuro, SE o app não puder agendar alarmes exatos, ENTÃO o sistema DEVE notificar "Seus lembretes estão pausados" com "Permita “Alarmes e lembretes” para o Agora Vai avisar na hora certa.", e tocar na notificação DEVE abrir a tela da permissão "Alarmes e lembretes" do Agora Vai.
- **RF-005** (AUTO-05): ONDE o gatilho de local de uma automação está ligado, o monitoramento DEVE continuar após reiniciar o aparelho ou atualizar o app, sem precisar abrir o app.

## 7. Requisitos não funcionais

- **RNF-001** (idempotência): reagendar várias vezes NÃO DEVE duplicar lembretes.
- **RNF-002** (desempenho): com 50 compromissos ativos, o reagendamento DEVE terminar em até 10 s após o primeiro desbloqueio.

## 8. Entidades e conceitos-chave

- **Compromisso ativo**: compromisso com o switch ligado.
- **Período de chegada**: definido na spec 002.
- **Automação com local**: automação de Pomodoro com gatilho de local ligado.

## 9. Casos de borda e erros

- O reagendamento só acontece após o primeiro desbloqueio depois do reinício — aceitável para lembretes de compromisso; os prazos dos CAs contam a partir dele.
- Fabricantes que exigem "início automático" liberado (ex.: Xiaomi) — orientação já existe na tela de permissões (SIS-05).
- Reiniciar no meio do período de chegada → a detecção recomeça na hora (RF-003).
- Saída perdida com o aparelho desligado → lembrete de saída imediato, se o evento ainda não começou e o usuário não está no destino (RF-002a, RF-003).
- Aviso de atraso que cai depois do evento (trajeto < 10 min) → dispara normalmente, como sem reinício (RF-002).

## 10. Critérios de sucesso

- **CS-001**: 100% dos lembretes futuros de compromissos ativos estão agendados após reiniciar, sem abrir o app.
- **CS-002**: zero lembretes de etapa ou atraso "antigos" disparando após reiniciar, e zero saídas perdidas sem aviso antes do evento.

## 11. Premissas e dependências

- **Dependência**: spec 001 (lembretes de saída e permissão de alarme exato) e spec 002 (detecção de chegada e seu período).

## 12. Questões em aberto

- (nenhuma)

## 13. Esclarecimentos

### Sessão 2026-10-29
- P: Ao religar, o que fazer com o lembrete de saída que deveria ter tocado com o aparelho desligado? → R: **tocar já, se o evento não passou** e a saída não foi confirmada; etapas e atraso passados continuam descartados (impacto: RF-002, RF-002a, CA-1.4, CA-1.5, §9, CS-002).
- P: Quais cenários de chegada após reiniciar viram critério? → R: **os dois** — reinício antes do preparo e reinício durante o período já no destino (impacto: RF-003, CA-1.6, CA-1.7).
- P (Q5): Texto e destino do aviso de lembretes pausados? → R: **texto proposto aprovado; o toque abre direto a tela da permissão** (impacto: RF-004, CA-3.1).
- Revisão (`revisor-de-spec`, mesma data): resolvidos conflitos entre RF-002a e RF-003 (a chegada tem precedência), o escopo de "depois do evento" (só etapas e saída perdidas), prazos medidos a partir do primeiro desbloqueio, CAs renumerados e com horários concretos, CAs novos CA-3.2 e CA-4.2, RNF-002 mensurável. Localização em segundo plano revogada ficou fora de escopo (§3).

## 14. Delta nas capacidades (constituição §8)

| Capacidade | Mudança |
|---|---|
| LEMB-11 | **Antes:** QUANDO o aparelho reiniciar ou a hora do sistema mudar, reagendar alarmes e geofences de todos os compromissos ativos e futuros. **Depois:** RF-001 a RF-004, incluindo RF-002a (mudança de hora sai do requisito). ⬜ → ✅ |
| LEMB-09 | Generaliza a regra da Revisão 2 da 002: considerar a chegada de quem já está no raio também quando a detecção é **reativada** durante o período (reinício, religar o switch) — o comportamento implementado da 002 já faz isso ao religar |
| AUTO-05 | Acrescenta RF-005 ao texto atual; critérios .a e .b |

## Checklist de qualidade

- [x] Sem detalhes de implementação (linguagens, frameworks, APIs, tabelas)
- [x] Requisitos testáveis e sem ambiguidade
- [x] Critérios de sucesso mensuráveis
- [x] Histórias priorizadas e independentes
- [x] Casos de borda identificados
- [x] Escopo delimitado por não-objetivos
- [x] Nenhum [PRECISA ESCLARECER] restante
