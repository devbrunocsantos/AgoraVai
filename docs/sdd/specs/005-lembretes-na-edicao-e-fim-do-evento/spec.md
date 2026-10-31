---
id: 005
titulo: Lembretes durante a edição e compromisso encerrado
status: rascunho  # rascunho | esclarecida | pesquisada | planejada | em-implementacao | implementada | validada
criada_em: 2026-10-30
atualizada_em: 2026-10-30
responsavel: Bruno C. Santos
capacidades: LEMB, COMP
dts: —
depende_de: "001, 002"
---

# Spec 005: Lembretes durante a edição e compromisso encerrado

## 1. Contexto e problema

Dois problemas encontrados pelo Bruno nos testes em aparelho da spec 003 (2026-10-30):

1. **Lembrete toca durante a edição.** Ao reabrir um compromisso para editar, um
   lembrete dele (no horário certo) tocou enquanto o formulário estava aberto.
   A pessoa está justamente mexendo naquele compromisso; o lembrete interrompe,
   pode estar desatualizado em relação ao que ela está mudando e, nos estilos de
   tela cheia, tira a pessoa do formulário.
2. **Compromisso encerrado continua "ativo".** Depois do horário do evento, um
   compromisso sem repetição continua com o switch ligado. Não é só visual:
   desligar e religar um compromisso que já passou agenda horários no passado,
   e etapas, "HORA DE SAIR" e "Você está atrasado!" tocam na hora (pergunta em
   aberto registrada em COMP-10).

## 2. Objetivos

- Editar um compromisso sem ser interrompido pelos lembretes dele, sem que nenhum lembrete se perca em silêncio (P2).
- A lista mostra como desativados os compromissos sem repetição que já terminaram.
- Ativar um compromisso que já passou não dispara lembretes antigos.

## 3. Não-objetivos

- Repetição semanal (COMP-13 / DT-04): compromissos com dias de repetição não são desativados por esta spec.
- Mudar o bloqueio de compromisso inviável (COMP-07): editar durante o preparo continua sendo recusado com "Impossível! Atraso de N min.".
- Mudar o "Excluir após concluir" (COMP-14).
- Lembretes de outros compromissos e as automações do Pomodoro.
- Mudar o comportamento após reiniciar ou atualizar (spec 003).

## 4. Usuários e personas

- **Pessoa ajustando um compromisso perto da hora**: abre o compromisso para corrigir horário, etapas ou itens e não quer ser interrompida pelo próprio lembrete dele, mas também não pode perdê-lo.
- **Pessoa que olha a lista de compromissos**: quer ver de relance o que ainda vai lembrá-la e o que já terminou.

## 5. Histórias de usuário

### História 1 — Editar sem ser interrompido (Prioridade: P1)

Como **pessoa ajustando um compromisso**, quero **que os lembretes dele esperem enquanto eu edito** para **terminar a edição sem interrupção e sem perder o lembrete se eu desistir**.

**Por que P1:** é o problema que interrompe o uso; resolvido sozinho, já entrega valor.

**Teste independente:** abrir a edição de um compromisso pouco antes de um lembrete, esperar o horário passar no formulário e sair salvando ou sem salvar.

**Critérios de aceitação:**
- **CA-1.1 (LEMB-12.a)** — **Dado** um compromisso às 10:00 com saída às 9:15, **quando** abro a edição dele às 9:10 e continuo no formulário às 9:15, **então** "HORA DE SAIR" não toca.
- **CA-1.2 (LEMB-12.b)** — **Dado** o cenário do CA-1.1, **quando** saio do formulário sem salvar às 9:17, **então** "HORA DE SAIR" toca imediatamente e "Você está atrasado!" continua agendado para 9:25.
- **CA-1.3 (LEMB-12.c)** — **Dado** o cenário do CA-1.1, sem etapas, **quando** às 9:12 mudo o evento para 10:15 e salvo, **então** às 9:15 nada toca e "HORA DE SAIR" toca às 9:30.
- **CA-1.4 (LEMB-12.d)** — **Dado** um compromisso com etapa às 9:00 e saída às 9:15, **quando** fico no formulário das 8:55 às 9:20 e saio sem salvar, **então** toca só "HORA DE SAIR" (o lembrete mais recente que deveria ter tocado), e não a etapa.
- **CA-1.5 (LEMB-12.e)** — **Dado** os compromissos A e B, com saída de B às 9:15, **quando** às 9:15 estou editando A (ou criando um compromisso novo), **então** "HORA DE SAIR" de B toca normalmente.
- **CA-1.6 (LEMB-12.f)** — **Dado** o cenário do CA-1.1, **quando** às 9:16 tento salvar e o app recusa (ex.: "Impossível! Atraso de N min.") e então saio do formulário, **então** "HORA DE SAIR" toca imediatamente.

### História 2 — Compromisso encerrado aparece desativado (Prioridade: P2)

Como **pessoa que olha a lista**, quero **que compromissos que já terminaram apareçam desativados** para **saber o que ainda vai me lembrar e não disparar lembretes antigos ao mexer no switch**.

**Por que P2:** o estado errado confunde e o religar dispara lembretes antigos, mas nenhum lembrete se perde.

**Teste independente:** criar um compromisso sem repetição para daqui a poucos minutos, esperar terminar e abrir a lista.

**Critérios de aceitação:**
- **CA-2.1 (COMP-10.b)** — **Dado** um compromisso sem repetição às 10:00, **quando** ele termina (RF-007) e abro a lista, **então** o switch aparece desligado e o card esmaecido.
- **CA-2.2 (COMP-10.c)** — **Dado** um compromisso sem repetição cujo evento já passou, **quando** ligo o switch, **então** vejo "Esse compromisso já passou. Edite a data para ativá-lo de novo.", o switch continua desligado e nenhum lembrete toca.
- **CA-2.3 (COMP-10.d)** — **Dado** um compromisso encerrado, **quando** edito a data para amanhã e salvo, **então** ele volta ativo e os lembretes são agendados para amanhã.
- **CA-2.4 (COMP-10.e)** — **Dado** um compromisso com dias de repetição cujo evento já passou, **quando** abro a lista, **então** ele continua como estava (nada muda até a spec de repetição).
- **CA-2.5 (COMP-14.a)** — **Dado** um compromisso encerrado com "Excluir após concluir" marcado, **quando** abro o app, **então** ele é apagado, como hoje.

## 6. Requisitos funcionais

- **RF-001** (LEMB-12): ENQUANTO o formulário de edição de um compromisso estiver aberto, os lembretes desse compromisso (etapas, "HORA DE SAIR" e "Você está atrasado!", em qualquer estilo) NÃO DEVEM tocar.
- **RF-002** (LEMB-12): QUANDO o usuário salvar a edição, os lembretes retidos durante a edição DEVEM ser descartados e valem só os lembretes do novo agendamento (COMP-09).
- **RF-003** (LEMB-12): QUANDO o usuário sair do formulário sem salvar (inclusive depois de uma tentativa recusada), SE algum lembrete desse compromisso deveria ter tocado durante a edição, ENTÃO o sistema DEVE tocar imediatamente o mais recente deles; os lembretes ainda futuros DEVEM continuar agendados.
- **RF-004** (LEMB-12): Lembretes de outros compromissos e o formulário de novo compromisso NÃO DEVEM ser afetados por RF-001 a RF-003.
- **RF-005** (LEMB-12, LEMB-09): A detecção de chegada do compromisso em edição [PRECISA ESCLARECER: durante a edição, a detecção de chegada continua funcionando (e "Você chegou! 🏁" aparece normalmente) ou também fica retida até sair do formulário?]
- **RF-006** (LEMB-12): SE o usuário sair do app com o formulário aberto (tela inicial, outro app, app encerrado), ENTÃO [PRECISA ESCLARECER: os lembretes retidos tocam na hora, como ao sair sem salvar, ou continuam retidos até a pessoa voltar ao formulário?]
- **RF-007** (COMP-10): QUANDO um compromisso sem dias de repetição terminar, o sistema DEVE desativá-lo (switch desligado, card esmaecido), sem cancelar o que ainda faltar dos lembretes e da detecção de chegada até esse momento. [PRECISA ESCLARECER: o compromisso termina no horário do evento ou ao fim do período de chegada (2 h depois do evento)?]
- **RF-008** (COMP-10): SE o usuário tentar ativar um compromisso sem dias de repetição cujo evento já passou, ENTÃO o sistema DEVE exibir "Esse compromisso já passou. Edite a data para ativá-lo de novo.", manter o switch desligado e não agendar lembretes.
- **RF-009** (COMP-13, COMP-14): Compromissos com dias de repetição NÃO DEVEM ser desativados por RF-007 nem bloqueados por RF-008; "Excluir após concluir" mantém o comportamento atual.

## 7. Requisitos não funcionais

- **RNF-001** (tempo de resposta): o lembrete do RF-003 DEVE aparecer em até 5 s depois de sair do formulário.
- **RNF-002** (consistência): ao abrir a lista, nenhum compromisso sem repetição encerrado aparece ativo.

## 8. Entidades e conceitos-chave

- **Formulário de edição**: a tela que abre ao tocar em editar um compromisso existente, até a pessoa salvar com sucesso ou sair dela.
- **Lembrete retido**: lembrete do compromisso em edição cujo horário chegou com o formulário aberto.
- **Compromisso encerrado**: compromisso sem dias de repetição que já terminou (RF-007).
- **Período de chegada**: definido na spec 002 (do início do preparo até 2 h depois do evento).

## 9. Casos de borda e erros

- Vários lembretes retidos → ao sair sem salvar, toca só o mais recente (CA-1.4), para não despejar notificações de uma vez (P5).
- "Você está atrasado!" retido → é um lembrete como os outros: retido e, se for o mais recente, tocado ao sair; não toca se a saída já tiver sido confirmada (LEMB-08).
- Editar durante o preparo → o salvar é recusado (COMP-07, sem mudança); ao sair, o lembrete retido toca (CA-1.6).
- Lembrete já aberto quando a edição começa (ex.: "HORA DE SAIR" exibido às 9:15 e edição aberta às 9:16) → continua aberto; a retenção vale só para o que tocaria depois.
- Compromisso desligado manualmente antes do evento → depois do evento continua desligado e religá-lo é bloqueado (RF-008).
- Evento editado para o passado → já recusado ao salvar (COMP-07).

## 10. Critérios de sucesso

- **CS-001**: zero lembretes do próprio compromisso tocando enquanto o formulário de edição dele está aberto.
- **CS-002**: zero lembretes perdidos por causa da edição: todo lembrete retido é substituído pelo novo agendamento (salvou) ou tocado ao sair (não salvou).
- **CS-003**: zero compromissos sem repetição encerrados aparecendo ativos, e zero lembretes antigos disparados ao mexer no switch.

## 11. Premissas e dependências

- **Premissa**: ao sair sem salvar, tocar só o lembrete mais recente basta; os anteriores perderam o sentido (mesma lógica da 003 para etapas passadas).
- **Premissa**: a desativação automática só precisa estar visível quando a lista é exibida; não precisa de aviso ao usuário.
- **Premissa**: o texto "Esse compromisso já passou. Edite a data para ativá-lo de novo." segue P5 e precisa de aprovação.
- **Dependência**: spec 001 (lembretes e estilos), spec 002 (período de chegada) e COMP-07/09/10/14.

## 12. Questões em aberto

- RF-005: detecção de chegada durante a edição — continua ou fica retida?
- RF-006: sair do app com o formulário aberto — toca na hora ou continua retido?
- RF-007: o compromisso termina no horário do evento ou ao fim do período de chegada?

## 13. Esclarecimentos

<!-- Preenchido por /esclarecer -->

## 14. Delta nas capacidades (constituição §8)

| Capacidade | Mudança |
|---|---|
| LEMB-12 (nova) | Lembretes durante a edição: RF-001 a RF-006; critérios `.a`–`.f` |
| COMP-09 | Acrescenta a retenção dos lembretes enquanto o formulário está aberto (remete a LEMB-12) |
| COMP-10 | Resolve a observação sobre religar compromisso passado: RF-007 a RF-009; critérios `.b`–`.e` |
| COMP-14 | Critério `.a` (sem mudança de comportamento) |

## Checklist de qualidade

- [x] Sem detalhes de implementação (linguagens, frameworks, APIs, tabelas)
- [x] Requisitos testáveis e sem ambiguidade
- [x] Critérios de sucesso mensuráveis
- [x] Histórias priorizadas e independentes
- [x] Casos de borda identificados
- [x] Escopo delimitado por não-objetivos
- [ ] Nenhum [PRECISA ESCLARECER] restante
