---
id: 002
titulo: Detecção de chegada
status: implementada  # rascunho | esclarecida | pesquisada | planejada | em-implementacao | implementada | validada
criada_em: 2026-10-26
atualizada_em: 2026-10-29
responsavel: Bruno C. Santos
capacidades: LEMB, COMP
dts: DT-01, DT-20
depende_de: "001"
---

# Spec 002: Detecção de chegada

> Aprovada em 2026-10-27 por Bruno C. Santos · implementada em 2026-10-27 · **Revisão 2** aprovada e implementada em 2026-10-28.
> Verificação em aparelho: B1 ✅; aguardando **B5** (CA-1.4) e **B6** (CA-1.3). CA-1.1, CA-1.2 e CA-3.1/3.2 ainda sem roteiro concluído.
> Migrada da convenção anterior (`specs/changes/2026-10-26-deteccao-de-chegada/`) em 2026-10-29.

## 1. Contexto e problema

O app promete reconhecer a chegada ao destino, mas a detecção nunca é ativada
(DT-01). Ativá-la como estava criaria problemas novos: o monitoramento valeria
para sempre e dispararia a qualquer hora. Se o destino for um lugar que o
usuário frequenta (o trabalho, por exemplo), ele receberia "Você chegou!" dias
antes do compromisso, e a chegada real não seria detectada. Além disso, chegar
cedo não impediria o "HORA DE SAIR" de tocar depois.

No teste em aparelho (2026-10-28) apareceu um caso real: o compromisso criado
com o usuário **já no destino** (reunião no prédio do trabalho) nunca teve a
chegada detectada, e "HORA DE SAIR" tocou.

## 2. Objetivos

- Quando o usuário chega ao destino no período do compromisso, o app reconhece a chegada, comemora e silencia os lembretes que perderam o sentido.
- Fora desse período, estar no local não conta.
- Quem já está no destino quando o período começa tem a chegada reconhecida.

## 3. Não-objetivos

- Voltar a monitorar o destino depois de reiniciar o aparelho (spec 003).
- Mudar o raio de detecção (200 m).

## 4. Usuários e personas

- **Pessoa que chega cedo ou já está no local**: não quer ouvir "Hora de sair" nem "Você está atrasado!" quando já chegou.

## 5. Histórias de usuário

### História 1 — Chegada reconhecida no período (Prioridade: P1)

Como **pessoa a caminho de um compromisso**, quero **que o app perceba que cheguei** para **não receber lembretes sem sentido e ter a chegada registrada**.

**Por que P1:** é a funcionalidade prometida e hoje inexistente.

**Teste independente:** compromisso com preparo começando em poucos minutos, estando no destino.

**Critérios de aceitação:**
- **CA-1.1 (LEMB-09.a)** — **Dado** um compromisso às 10:00 com preparo começando às 8:30, **quando** entro no raio às 9:40, **então** recebo "Você chegou! 🏁" e o compromisso fica com chegada confirmada.
- **CA-1.2 (LEMB-09.b)** — **Dado** que cheguei às 9:10 e a saída era às 9:15, **quando** dá 9:15, **então** "HORA DE SAIR" não toca, e às 9:25 "Você está atrasado!" também não.
- **CA-1.3 (LEMB-09.d)** — **Dado** o app fechado (processo encerrado), **quando** entro no raio dentro do período, **então** a chegada é detectada da mesma forma.
- **CA-1.4 (LEMB-09.f)** — **Dado** que estou no destino quando salvo o compromisso e continuo lá, **quando** o preparo começa, **então** recebo "Você chegou! 🏁", e "HORA DE SAIR" e "Você está atrasado!" não tocam.

### História 2 — Estar no local fora do período não conta (Prioridade: P1)

Como **pessoa cujo compromisso é num lugar que frequento**, quero **que só a chegada no dia e horário certos conte** para **não ter a detecção "gasta" antes da hora**.

**Teste independente:** compromisso amanhã; estar no destino hoje.

**Critérios de aceitação:**
- **CA-2.1 (LEMB-09.c)** — **Dado** um compromisso amanhã, **quando** entro no raio hoje, **então** nada acontece e a chegada no dia certo continua sendo detectada.

### História 3 — Destino sem coordenadas (Prioridade: P2)

Como **pessoa que digitou um endereço que o mapa não localiza**, quero **que o compromisso seja salvo normalmente** para **não ser bloqueada por um recurso extra**.

**Critérios de aceitação:**
- **CA-3.1 (LEMB-09.e)** — **Dado** um destino sem coordenadas, **quando** salvo, **então** o compromisso é salvo normalmente, sem detecção de chegada.
- **CA-3.2 (COMP-08.a)** — **Dado** que salvei o compromisso A, com destino localizado, **quando** salvo o compromisso B, cujo destino não é localizado, **então** B fica sem coordenadas e sem detecção de chegada (e não herda as coordenadas de A).

## 6. Requisitos funcionais

- **RF-001** (LEMB-09): ONDE o destino tem coordenadas e a permissão de localização em segundo plano foi concedida, QUANDO o usuário entrar num raio de 200 m do destino **entre o início do preparo e 2 horas depois do horário do evento**, o sistema DEVE: (1) gravar chegada e saída confirmadas; (2) cancelar os lembretes desse compromisso que ainda não dispararam (etapas, saída e atraso); (3) notificar "Você chegou! 🏁" com "Destino alcançado: <título>"; (4) parar de monitorar o destino.
- **RF-002** (LEMB-09): Entradas no raio fora desse período DEVEM ser ignoradas. O monitoramento DEVE começar no início do período, e não antes, e DEVE terminar sozinho 2 horas depois do horário do evento.
- **RF-003** (LEMB-09, Revisão 2): SE o usuário já estiver dentro do raio quando o período de chegada começar, ENTÃO o sistema DEVE considerar a chegada nesse momento.
- **RF-004** (LEMB-09): A detecção DEVE funcionar com o app fechado.
- **RF-005** (COMP-08): SE o destino não puder ser localizado, ENTÃO o compromisso DEVE ser salvo sem coordenadas e sem detecção de chegada; NÃO DEVE herdar coordenadas de outro salvamento.

## 7. Requisitos não funcionais

- **RNF-001** (bateria): não monitorar o destino fora do período de chegada.
- **RNF-002** (privacidade): localização usada só no aparelho (P4).

## 8. Entidades e conceitos-chave

- **Período de chegada**: [início do preparo, horário do evento + 2 h].
- **Início do preparo**: horário de início da primeira etapa (ou da saída, se não houver etapas).
- **Chegada confirmada**: marca do compromisso; implica saída confirmada.

## 9. Casos de borda e erros

- Já dentro do raio quando o período começa → RF-003 / CA-1.4.
- Compromisso religado no meio do período → o monitoramento começa imediatamente.
- Imprecisão do GPS → raio de 200 m e período de 2 h dão folga.
- O sistema avisa entradas com atraso de alguns minutos e com menor precisão em economia de energia.

## 10. Critérios de sucesso

- **CS-001**: nenhum "HORA DE SAIR" ou "Você está atrasado!" para quem já chegou no período.
- **CS-002**: nenhum "Você chegou!" fora do período do compromisso.

## 11. Premissas e dependências

- **Dependência**: spec 001 (agendador usado para cancelar os lembretes).
- **Premissa**: o usuário concedeu localização em segundo plano (SIS-03).

## 12. Questões em aberto

- (nenhuma)

## 13. Esclarecimentos

### Sessão 2026-10-27
- P: O período "do início do preparo até 2 h depois do evento" está bom? (2 h cobre quem chega atrasado.) → R: **aprovado** (impacto: RF-001, RF-002).
- P: Chegar deve cancelar os lembretes pendentes e marcar a saída? → R: **sim** (impacto: RF-001).

### Sessão 2026-10-28 — Revisão 2
- P: Aprovar a Revisão 2 (considerar a chegada de quem já está no raio quando o período começa; monitorar só a partir do início do período)? → R: **aprovada** (impacto: RF-002, RF-003, CA-1.4; o caso de borda "já dentro do raio" deixa de ser limitação). Ela também permite testar a chegada **sem simular localização**.

## 14. Delta nas capacidades (constituição §8)

| Capacidade | Mudança | Status |
|---|---|---|
| LEMB-09 | **Antes:** … QUANDO entrar num raio de 200 m do destino, gravar `confirmouChegada`, notificar e remover a geofence. **Depois:** RF-001 a RF-004 | ⚠️ → 🧪 |
| COMP-08 | texto sem mudança; critério novo COMP-08.a (DT-20) | ⚠️ → 🧪 |
| AUTO-08 | remover geofence de automação em segundo plano passa a funcionar (efeito colateral da correção do DT-01) | 🧪 |

## Checklist de qualidade

- [x] Sem detalhes de implementação (linguagens, frameworks, APIs, tabelas)
- [x] Requisitos testáveis e sem ambiguidade
- [x] Critérios de sucesso mensuráveis
- [x] Histórias priorizadas e independentes
- [x] Casos de borda identificados
- [x] Escopo delimitado por não-objetivos
- [x] Nenhum [PRECISA ESCLARECER] restante
