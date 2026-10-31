---
id: 005
titulo: Lembretes durante a edição e compromisso encerrado
status: pesquisada  # rascunho | esclarecida | pesquisada | planejada | em-implementacao | implementada | validada
criada_em: 2026-10-30
atualizada_em: 2026-10-30
responsavel: Bruno C. Santos
capacidades: LEMB, COMP
dts: —
depende_de: "001, 002, 003"
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
- Ativar um compromisso sem repetição cujo evento já passou não dispara lembretes antigos.
- Liberar a retenção ou religar um compromisso antes do evento dispara no máximo um lembrete passado: o "HORA DE SAIR".

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
- **CA-1.1 (LEMB-12.a)** — **Dado** um compromisso às 10:00, sem etapas, com saída às 9:15, **quando** abro a edição dele às 9:10 e continuo no formulário às 9:15, com a tela ligada e o app em primeiro plano durante todo o intervalo, **então** "HORA DE SAIR" não toca.
- **CA-1.2 (LEMB-12.b)** — **Dado** o cenário do CA-1.1, **quando** saio do formulário sem salvar às 9:17, **então** "HORA DE SAIR" toca em até 5 s (RNF-001) e, às 9:25, sem saída confirmada, "Você está atrasado!" toca.
- **CA-1.3 (LEMB-12.c)** — **Dado** o cenário do CA-1.1, **quando** às 9:17 (com "HORA DE SAIR" retido) mudo o evento para 10:15 e salvo, **então** nada toca no momento do salvamento e "HORA DE SAIR" toca às 9:30.
- **CA-1.4 (LEMB-12.d)** — **Dado** um compromisso com etapa às 9:00 e saída às 9:15, **quando** fico no formulário das 8:55 às 9:20, com a tela ligada e o app em primeiro plano durante todo o intervalo, e saio sem salvar, **então** toca só "HORA DE SAIR", e não a etapa.
- **CA-1.5 (LEMB-12.e)** — **Dado** os compromissos A e B, com saída de B às 9:15, **quando** às 9:15 estou editando A (ou criando um compromisso novo), **então** "HORA DE SAIR" de B toca normalmente.
- **CA-1.6 (LEMB-12.f)** — **Dado** o cenário do CA-1.1, **quando** às 9:16 tento salvar sem mudar o horário, vejo "Impossível! Atraso de 1 min." e então saio do formulário, **então** "HORA DE SAIR" toca em até 5 s (RNF-001).
- **CA-1.7 (LEMB-12.g)** — **Dado** o tipo ALARME e o cenário do CA-1.1, **quando** chega 9:15 com o formulário aberto, **então** a tela cheia não abre; **e quando** saio sem salvar às 9:17, **então** ela abre em até 5 s (RNF-001).
- **CA-1.8 (LEMB-12.h)** — **Dado** o tipo PADRAO e "HORA DE SAIR" exibido na bandeja às 9:15 e a edição aberta às 9:16, **quando** toco "Já Saí" na notificação e saio sem salvar às 9:26, **então** nada toca (a saída confirmada cancela o atraso, LEMB-08).
- **CA-1.9 (LEMB-12.i)** — **Dado** o cenário do CA-1.1, com "HORA DE SAIR" retido, **quando** às 9:17 a tela apaga (ou vou para a tela inicial) com o formulário aberto, **então** "HORA DE SAIR" toca em até 5 s e, às 9:25, sem saída confirmada, "Você está atrasado!" toca normalmente.
- **CA-1.10 (LEMB-12.j)** — **Dado** o cenário do CA-1.9, **quando** às 9:18 volto ao formulário e fico nele até 9:26, **então** "Você está atrasado!" das 9:25 fica retido e, ao sair sem salvar às 9:27, ele não toca, mas "HORA DE SAIR" toca de novo em até 5 s (RF-003).
- **CA-1.11 (LEMB-12.k)** — **Dado** o cenário do CA-1.1, com "HORA DE SAIR" retido, **quando** às 9:20 entro no raio do destino com o formulário aberto, **então** "Você chegou! 🏁" aparece e, ao sair sem salvar às 9:22, nada toca (nem às 9:25).
- **CA-1.12 (LEMB-12.l)** — **Dado** o cenário do CA-1.1, **quando** fico no formulário até 9:27 e saio sem salvar, **então** toca "HORA DE SAIR" em até 5 s e o "Você está atrasado!" das 9:25, que já passou, não toca.
- **CA-1.13 (LEMB-12.m)** — **Dado** um compromisso às 10:00 com saída às 9:55, **quando** fico no formulário das 9:50 às 10:02 e saio sem salvar, **então** "HORA DE SAIR" não toca (o evento já começou) e "Você está atrasado!" toca às 10:05.
- **CA-1.14 (LEMB-12.n)** — **Dado** o cenário do CA-1.8, sem tocar "Já Saí", **quando** às 9:17 mudo o evento para 10:15 e salvo, **então** a notificação "HORA DE SAIR" das 9:15 some (RF-002).
- **CA-1.15 (LEMB-12.o)** — **Dado** o cenário do CA-1.1, **quando** giro a tela às 9:16, **então** nada toca; **e quando** o aparelho reinicia às 9:17 com o formulário aberto, **então** "HORA DE SAIR" toca logo após o reinício e "Você está atrasado!" às 9:25 (RF-006).

### História 2 — Compromisso encerrado aparece desativado (Prioridade: P2)

Como **pessoa que olha a lista**, quero **que compromissos que já terminaram apareçam desativados** para **saber o que ainda vai me lembrar e não disparar lembretes antigos ao mexer no switch**.

**Por que P2:** o estado errado confunde e o religar dispara lembretes antigos, mas nenhum lembrete se perde.

**Teste independente:** criar um compromisso sem repetição para daqui a poucos minutos, esperar terminar e abrir a lista.

**Critérios de aceitação:**
- **CA-2.1 (COMP-10.b)** — **Dado** um compromisso sem repetição às 10:00, **quando** abro a lista às 10:01 (ou, com a lista aberta desde 9:59, até 10:01), **então** o switch aparece desligado e o card esmaecido.
- **CA-2.2 (COMP-10.c)** — **Dado** um compromisso sem repetição às 10:00, com saída às 9:55 sem confirmação, **quando** ligo o switch às 10:02, **então** vejo "Esse compromisso já passou. Edite a data para ativá-lo de novo.", o switch continua desligado, nenhum lembrete novo é agendado e o atraso das 10:05 e a detecção de chegada continuam como estavam (RF-007).
- **CA-2.3 (COMP-10.d)** — **Dado** um compromisso encerrado, **quando** edito a data para amanhã e salvo, **então** ele volta ativo e os lembretes são agendados para amanhã.
- **CA-2.4 (COMP-10.e)** — **Dado** um compromisso com dias de repetição cujo evento já passou, **quando** abro a lista, **então** o switch continua ligado e desligá-lo e religá-lo não é bloqueado (nada muda até a spec de repetição).
- **CA-2.5 (COMP-14.a)** — **Dado** um compromisso encerrado com "Excluir após concluir" marcado, **quando** abro o app, **então** ele é apagado, como hoje.
- **CA-2.6 (COMP-09.a)** — **Dado** um compromisso desligado à mão com evento amanhã, **quando** edito e salvo, **então** ele fica ativo e os lembretes são agendados (RF-010).
- **CA-2.7 (COMP-10.f)** — **Dado** um compromisso sem repetição com saída às 9:55 e evento às 10:00, sem saída confirmada, **quando** ele é encerrado às 10:00, **então** "Você está atrasado!" ainda toca às 10:05 — também se o aparelho reiniciar às 10:02 (LEMB-11).
- **CA-2.8 (COMP-10.g)** — **Dado** o cenário do CA-2.7, **quando** entro no raio do destino às 10:30, **então** recebo "Você chegou! 🏁" (período de chegada, LEMB-09).
- **CA-2.9 (COMP-10.h)** — **Dado** um compromisso às 10:00 com etapa às 9:00 e saída às 9:15, desligado à mão às 8:50, **quando** religo às 9:20, **então** só "HORA DE SAIR" toca, em até 5 s, a etapa não toca e "Você está atrasado!" continua agendado para 9:25.
- **CA-2.10 (COMP-10.i)** — **Dado** um compromisso sem repetição cujo evento já passou e o app sem permissão de alarme exato, **quando** ligo o switch, **então** vejo "Esse compromisso já passou. Edite a data para ativá-lo de novo." e não a mensagem de permissão (RF-008 antes de LEMB-10).
- **CA-2.11 (COMP-10.j)** — **Dado** um compromisso às 10:00 com etapa às 9:00 e saída às 9:15, desligado à mão às 8:50, **quando** religo às 9:05, **então** nada toca na hora e "HORA DE SAIR" toca às 9:15.

## 6. Requisitos funcionais

- **RF-001** (LEMB-12): ENQUANTO o formulário de edição de um compromisso estiver aberto e visível, os lembretes desse compromisso (etapas, "HORA DE SAIR" e "Você está atrasado!", em qualquer estilo) NÃO DEVEM tocar.
- **RF-002** (LEMB-12): QUANDO o usuário salvar a edição, os lembretes retidos durante a edição DEVEM ser descartados e valem só os lembretes do novo agendamento (COMP-09); o lembrete de saída do agendamento antigo que ainda estiver aberto na bandeja ou em tela cheia DEVE ser fechado.
- **RF-003** (LEMB-12): QUANDO o usuário sair do formulário sem salvar (inclusive depois de uma tentativa recusada), o sistema DEVE tocar imediatamente "HORA DE SAIR" no estilo escolhido, SE o horário de saída já passou, a saída não foi confirmada e o horário do evento ainda não chegou. Etapas e avisos de atraso cujo horário passou durante a edição NÃO DEVEM tocar; os lembretes ainda futuros (inclusive o atraso depois do evento) DEVEM continuar agendados. É a mesma regra do reinício (003, RF-002/RF-002a).
- **RF-004** (LEMB-12): Lembretes de outros compromissos e o formulário de novo compromisso NÃO DEVEM ser afetados por RF-001 a RF-003.
- **RF-005** (LEMB-12, LEMB-09): A detecção de chegada do compromisso em edição NÃO DEVE ser afetada pela retenção: QUANDO a chegada for detectada com o formulário aberto, "Você chegou! 🏁" DEVE aparecer normalmente e os lembretes pendentes e retidos DEVEM ser cancelados (LEMB-09); ao sair do formulário, nenhum lembrete retido toca.
- **RF-006** (LEMB-12): SE o formulário deixar de estar visível sem a pessoa sair dele (tela apagada ou bloqueada, tela inicial, outro app, app encerrado pelo sistema), ENTÃO o sistema DEVE suspender a retenção: o sistema DEVE aplicar imediatamente a regra do RF-003, e os lembretes seguintes DEVEM tocar no horário normal enquanto o formulário não estiver visível. QUANDO o formulário voltar a ficar visível, a retenção DEVE recomeçar só para os lembretes que vierem depois. Reiniciar o aparelho com o formulário aberto conta como sair sem salvar (RF-003, depois LEMB-11). Girar a tela não conta como deixar de estar visível.
- **RF-007** (COMP-10, LEMB-11): QUANDO chegar o horário do evento de um compromisso sem dias de repetição que esteja ativo, o sistema DEVE marcá-lo como encerrado (switch desligado, card esmaecido), inclusive na lista que já estiver aberta. O encerramento NÃO DEVE cancelar o que ainda estiver no prazo: o aviso de atraso ainda futuro e a detecção de chegada até o fim do período de chegada continuam valendo, inclusive depois de reiniciar ou atualizar (LEMB-11).
- **RF-008** (COMP-10): SE o usuário tentar ativar um compromisso sem dias de repetição cujo evento já passou, ENTÃO o sistema DEVE exibir "Esse compromisso já passou. Edite a data para ativá-lo de novo.", manter o switch desligado e não agendar lembretes; a tentativa não altera o que o RF-007 mantém no prazo. Essa verificação vem antes da exigência de alarme exato (LEMB-10).
- **RF-009** (COMP-13, COMP-14): Compromissos com dias de repetição NÃO DEVEM ser desativados por RF-007 nem bloqueados por RF-008; "Excluir após concluir" mantém o comportamento atual.
- **RF-010** (COMP-09): QUANDO o usuário salvar a edição de um compromisso com evento no futuro, o sistema DEVE deixá-lo ativo e agendar os lembretes (sujeito a LEMB-10), esteja ele encerrado ou desligado à mão — como já acontece hoje ao salvar.
- **RF-011** (COMP-10): QUANDO o usuário religar, antes do horário do evento, um compromisso desligado à mão, o sistema DEVE agendar os lembretes ainda futuros e tocar imediatamente "HORA DE SAIR" no estilo escolhido, SE o horário de saída já passou, a saída não foi confirmada e o horário do evento ainda não chegou e a chegada não for considerada ao religar (LEMB-09). Etapas e avisos de atraso cujo horário já passou NÃO DEVEM tocar — mesma regra do RF-003 e do reinício (003).

## 7. Requisitos não funcionais

- **RNF-001** (tempo de resposta): o lembrete dos RF-003, RF-006 e RF-011 DEVE aparecer em até 5 s depois de sair do formulário, de ele deixar de estar visível ou de religar o compromisso.
- **RNF-002** (consistência): nenhum compromisso sem repetição encerrado aparece ativo — ao abrir a lista ou, com a lista já aberta, em até 1 min depois do horário do evento.

## 8. Entidades e conceitos-chave

- **Formulário de edição**: a tela que abre ao tocar em editar um compromisso existente. A pessoa **sai** dele ao salvar com sucesso ou ao voltar/fechar o formulário sem salvar; os demais casos em que ele deixa de estar visível seguem o RF-006.
- **Lembrete retido**: lembrete do compromisso em edição cujo horário chegou com o formulário aberto e visível.
- **Compromisso encerrado**: compromisso sem dias de repetição cujo horário do evento já passou (RF-007). Aparece desligado como um compromisso desligado à mão, mas, ao contrário dele, mantém o aviso de atraso e a detecção de chegada que ainda estejam no prazo.
- **Período de chegada**: definido na spec 002 (do início do preparo até 2 h depois do evento).

## 9. Casos de borda e erros

- Vários lembretes retidos → ao sair sem salvar, toca no máximo o "HORA DE SAIR" (CA-1.4), para não despejar notificações de uma vez (P5).
- "Você está atrasado!" retido → é descartado ao sair, como as etapas; se a saída não foi confirmada e o evento não começou, o "HORA DE SAIR" toca no lugar (CA-1.12). Depois do evento, um atraso que passou durante a edição não toca (aceito, mesma regra da 003).
- Editar durante o preparo → o salvar é recusado (COMP-07, sem mudança); ao sair, o lembrete retido toca (CA-1.6). Todo lembrete retido cai no preparo ou depois dele; por isso, com lembrete retido, só dá para salvar se o novo início do preparo ficar no futuro — adiando o evento, encurtando ou removendo etapas ou reduzindo o tempo de segurança (CA-1.3).
- Qualquer recusa ao salvar (campos obrigatórios, "Rota não encontrada.", "Erro ao salvar.", falta de permissão de alarme) conta como não salvar: ao sair, vale o RF-003.
- Chegada detectada durante a edição → "Você chegou! 🏁" aparece, os lembretes são cancelados pela LEMB-09 e, ao sair, nenhum lembrete retido toca (RF-005, CA-1.11).
- Lembrete já aberto quando a edição começa (ex.: "HORA DE SAIR" exibido às 9:15 e edição aberta às 9:16) → continua aberto durante a edição; a retenção vale só para o que tocaria depois. Ao salvar, ele é fechado (RF-002); ao sair sem salvar, continua aberto.
- Compromisso desligado manualmente antes do evento → depois do evento continua desligado e religá-lo é bloqueado (RF-008).
- Evento editado para o passado → já recusado ao salvar (COMP-07).

## 10. Critérios de sucesso

- **CS-001**: zero etapas, "HORA DE SAIR" ou "Você está atrasado!" do próprio compromisso tocando enquanto o formulário de edição dele está aberto e visível (RF-001).
- **CS-002**: zero lembretes perdidos em silêncio por causa da edição antes do evento: ao salvar, os retidos são substituídos pelo novo agendamento; ao sair sem salvar, o "HORA DE SAIR" toca em até 5 s se a saída não foi confirmada.
- **CS-003**: zero compromissos sem repetição encerrados aparecendo ativos, e zero lembretes antigos disparados ao tentar religar um compromisso sem repetição depois do evento; ao religar antes do evento, no máximo um lembrete passado toca.

## 11. Premissas e dependências

- **Premissa**: ao liberar a retenção ou religar, tocar só o "HORA DE SAIR" basta; etapas e atrasos passados perderam o sentido (mesma regra da 003).
- **Premissa**: o encerramento não precisa de aviso ao usuário; basta aparecer na lista.
- **Premissa**: editar um compromisso encerrado sem mudar a data continua recusado com "Impossível! Atraso de N min." (COMP-07, sem mudança), mesmo que N seja grande (ex.: 1440).
- **Dependência**: spec 001 (lembretes e estilos), spec 002 (período de chegada) e COMP-07/09/10/14.

## 12. Questões em aberto

Nenhuma.

## 13. Esclarecimentos

### Sessão 2026-10-30

- **P1 (RF-007, RNF-002, LEMB-11, COMP-14):** quando o compromisso termina? → **No horário do evento, sem cortar o que está no prazo.** O switch desliga e o card esmaece às 10:00 (também com a lista aberta), mas o atraso ainda futuro e a detecção de chegada até +2 h continuam valendo, inclusive após reiniciar. "Evento já passou" e "encerrado" passam a ser o mesmo momento; o "Excluir após concluir" segue apagando no horário do evento, como hoje. → RF-007, RNF-002, §8, CA-2.1, CA-2.7, CA-2.8.
- **P2 (RF-006):** e se o formulário sair da tela sem a pessoa sair dele? → **Qualquer caso libera** (tela apagada/bloqueada, tela inicial, outro app, app encerrado, reinício): o retido mais recente toca na hora, os seguintes no horário normal; ao voltar ao formulário, a retenção recomeça para o que vier depois. → RF-006, CA-1.9, CA-1.10.
- **P3 (RF-005):** a detecção de chegada continua durante a edição? → **Continua normal**: "Você chegou! 🏁" aparece e cancela os lembretes pendentes e retidos; ao sair, nada toca. → RF-005, §9, CA-1.11.
- **P4 (RF-003):** qual retido toca ao liberar? → **O mais recente, do jeito que ele é; depois do horário do evento, só o atraso** (etapas e "HORA DE SAIR" retidos não tocam), como na 003. → RF-003, CA-1.12, CA-1.13.
- **P5 (escopo):** religar antes do evento um compromisso desligado à mão entra? → **Entra, com a regra da 003**: toca só o lembrete passado mais recente e agenda o resto. → §2, RF-011, CA-2.9.
- **P4 revista (após a revisão final):** a "regra da 003" citada em P4 e P5 não era exata. Decisão do Bruno: **igual à 003** — ao liberar a retenção ou religar antes do evento, só "HORA DE SAIR" toca (se a saída passou, não foi confirmada e o evento não começou); etapas e atrasos passados são descartados. Substitui a P4. → RF-003, RF-011, §2, §9, §11, CS-002, CA-1.4, CA-1.10, CA-1.12, CA-1.13, CA-2.11.
- **Aprovações (Bruno):** texto do RF-008 aprovado; "Impossível! Atraso de N min." mantido para compromisso encerrado editado sem mudar a data (§11); ao salvar, o "HORA DE SAIR" antigo ainda aberto é fechado (RF-002, §9).

## 14. Delta nas capacidades (constituição §8)

| Capacidade | Mudança |
|---|---|
| LEMB-12 (nova) | Lembretes durante a edição: RF-001 a RF-006; critérios `.a`–`.o` |
| COMP-09 | Acrescenta a retenção dos lembretes enquanto o formulário está aberto (remete a LEMB-12) e explicita que salvar com evento futuro deixa o compromisso ativo (RF-010); critério `.a` |
| COMP-10 | Resolve e remove a observação sobre religar compromisso passado: RF-007 a RF-009 e RF-011 (religar antes do evento); critérios `.b`–`.j` |
| LEMB-11 | Compromisso encerrado mantém atraso futuro e chegada no reagendamento (RF-007, CA-2.7) |
| LEMB-09 | Conforme o RF-005: chegada durante a edição |
| COMP-14 | Critério `.a` (sem mudança de comportamento) |

## Checklist de qualidade

- [x] Sem detalhes de implementação (linguagens, frameworks, APIs, tabelas)
- [x] Requisitos testáveis e sem ambiguidade
- [x] Critérios de sucesso mensuráveis
- [x] Histórias priorizadas e independentes
- [x] Casos de borda identificados
- [x] Escopo delimitado por não-objetivos
- [x] Nenhum [PRECISA ESCLARECER] restante
