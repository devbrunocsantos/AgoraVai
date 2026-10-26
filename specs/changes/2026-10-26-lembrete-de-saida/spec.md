# Spec — Lembrete de saída confiável

- **Data:** 2026-10-26
- **Autor:** Claude (a pedido de Bruno C. Santos)
- **Status:** Rascunho
- **Capacidades afetadas:** `capabilities/lembretes.md`, `capabilities/compromissos.md`
- **DTs relacionados:** DT-02, DT-06, DT-16, DT-17, DT-19

## 1. Problema
O lembrete mais importante do app, "HORA DE SAIR", chega como "Lembrete / Hora do
compromisso" e sem o botão "Já Saí". Na tela cheia, confirmar a saída não fecha
a notificação. No Android 14, se o sistema bloquear a tela cheia, a notificação
não abre nada ao ser tocada, então o usuário não consegue confirmar e recebe
"Você está atrasado!" mesmo tendo saído. Sem permissão de alarme exato, os
lembretes deixam de ser agendados sem aviso.

## 2. Resultado esperado
O lembrete de saída sempre mostra o título do compromisso, a mensagem certa e
uma forma de confirmar a saída. Confirmar fecha o lembrete e cancela o aviso de
atraso. Se o app não puder garantir o horário, o usuário fica sabendo na hora
de salvar.

## 3. Delta de requisitos

### ALTERADOS

#### LEMB-02 · Lembrete "Hora de sair"
- **Antes / Depois:** texto sem mudança. O status passa de ⚠️ para ✅.
- **Critérios de aceite novos:**
  - **a.** Dado o compromisso "Dentista" com os itens [Chaves], quando chega o horário de saída, então o lembrete mostra "HORA DE SAIR: Dentista" e "Saia agora! Não esqueça seus itens.".
  - **b.** Dado um compromisso sem itens, quando chega o horário de saída, então a mensagem é "Saia agora para chegar a tempo!".

#### LEMB-03 · Estilo PADRAO
- **Antes / Depois:** texto sem mudança. O status passa de ⚠️ para ✅.
- **Critérios de aceite novos:**
  - **a.** Dado o tipo `PADRAO`, quando o lembrete de saída aparece, então ele tem a ação "Já Saí" e o texto expandido termina com "📌 Não esqueça: Chaves".
  - **b.** Quando toco em "Já Saí", então a notificação some e o aviso de atraso (LEMB-08) não aparece.

#### LEMB-04 · Estilo ALARME
- **Antes:** ONDE o tipo é `ALARME`, o sistema DEVE abrir uma tela cheia que acende a tela e aparece sobre o bloqueio, lista os itens e tem o botão único "JÁ ESTOU SAINDO".
- **Depois:** ONDE o tipo é `ALARME`, `INSISTENTE` ou `DESAFIO`, o sistema DEVE abrir uma tela cheia que acende a tela e aparece sobre o bloqueio, lista os itens e mostra o mecanismo de confirmação do tipo (LEMB-04 a LEMB-06). SE o sistema não permitir abrir a tela cheia sozinho, ENTÃO tocar na notificação DEVE abrir essa mesma tela.
- **Critérios de aceite novos:**
  - **a.** Dado o tipo `ALARME` com a tela bloqueada, quando chega o horário de saída, então a tela acende e mostra "JÁ ESTOU SAINDO".
  - **b.** Dado que o sistema exibiu só a notificação (sem tela cheia), quando toco nela, então a tela do lembrete abre.

#### LEMB-07 · Confirmar saída
- **Antes / Depois:** texto sem mudança. O status passa de ⚠️ para ✅.
- **Critérios de aceite novos:**
  - **b.** Dada a tela cheia aberta, quando confirmo a saída, então a tela fecha e a notificação do lembrete some da bandeja.
  - **c.** Dada a saída confirmada, quando se passam 10 min do horário de saída, então "Você está atrasado!" não aparece.

#### LEMB-10 · Precisão e Doze
- **Antes:** … SE o app não puder agendar alarmes exatos (Android 12+), ENTÃO o sistema DEVE avisar o usuário e oferecer o atalho para a permissão.
- **Depois:** Os alarmes DEVEM ser exatos e disparar mesmo com o aparelho em Doze. SE o app não puder agendar alarmes exatos, ENTÃO, ao salvar ou reativar um compromisso, o sistema DEVE exibir "Permita “Alarmes e lembretes” para o Agora Vai avisar na hora certa." e NÃO DEVE salvar nem reativar. Ao salvar, a mensagem DEVE oferecer a ação "Abrir configurações", que leva à tela da permissão.
- **Critérios de aceite novos:**
  - **a.** Dada a permissão de alarmes exatos negada, quando salvo um compromisso válido, então vejo a mensagem com "Abrir configurações" e nada é gravado.
  - **b.** Dada a permissão negada, quando religo um compromisso desativado, então vejo a mensagem e o switch continua desligado.

#### COMP-10 e COMP-11 · Desativar e excluir
- **Antes / Depois:** texto sem mudança. COMP-11.a passa de ⚠️ para ✅.
- **Critérios de aceite novos:**
  - **COMP-10.a.** Dado um compromisso desativado antes do horário de saída, quando passa o horário de saída + 10 min, então nenhum lembrete de saída nem aviso de atraso aparece.
  - **COMP-11.b.** Dado um compromisso excluído, quando passa o horário de saída + 10 min, então nenhum aviso de atraso aparece.

## 4. Fora de escopo
- Reagendar após reiniciar ou atualizar o app (mudança `reagendar-apos-reiniciar`). Alarmes **já agendados** nos aparelhos continuam com o defeito até serem reagendados. Aquela mudança faz isso automaticamente quando o app é atualizado.
- Repetição semanal (COMP-13), que depende de decisão de produto.
- Mostrar a permissão de tela cheia do Android 14 no diagnóstico (SIS-05) fica como follow-up.

## 5. Constituição
Corrige violações de **P2** (um lembrete nunca falha em silêncio) e **P6**
(contratos explícitos). Introduz os primeiros testes unitários (**P7**).

## 6. Perguntas em aberto
- [ ] **Q1 — sem alarme exato: bloquear ou salvar mesmo assim?** Recomendo **bloquear**, como está acima: o app nunca aceita um compromisso que não consegue lembrar na hora certa. A alternativa é salvar com um alarme aproximado (pode atrasar alguns minutos) e mostrar um aviso.
- [ ] **Q2 —** o texto "Permita “Alarmes e lembretes” para o Agora Vai avisar na hora certa." está bom?
