# Capacidade: Lembretes de compromisso (`LEMB`)

Fazer o usuário começar a se arrumar, sair e chegar na hora. Os horários vêm
das definições em [`compromissos.md`](compromissos.md).

**Código principal:** `CompromissoViewModel.agendarAlarmes`,
`notifications/NotificationScheduler.kt`, `AlarmReceiver.kt`,
`ui/ReminderActivity.kt`, `services/NotificationActionReceiver.kt`,
`services/geofence/CompromissoGeofenceManager.kt`, `GeofenceBroadcastReceiver.kt`.

```
início etapa 1 … início etapa N     SAÍDA          SAÍDA + 10 min     ENTRADA NO RAIO
     LEMB-01         LEMB-01       LEMB-02..07        LEMB-08            LEMB-09
```

---

### LEMB-01 · Alarme de cada etapa — ✅
ENQUANTO o compromisso estiver ativo, o sistema DEVE disparar, no horário de
início de cada etapa, uma notificação "Próximo Preparativo: <descrição>" com o
texto "Faltam <duração> min.". As etapas são encadeadas de trás para frente a
partir do horário de saída, na ordem definida pelo usuário.

- **a.** Dadas a saída às 9:00 e as etapas [Banho 15, Café 10], então "Banho" dispara às 8:35 e "Café" às 8:50. *(teste: `HorariosLembreteTest`)*

### LEMB-02 · Lembrete "Hora de sair" — ✅
No horário de saída, o sistema DEVE disparar o lembrete com título
"HORA DE SAIR: <título>" e mensagem "Saia agora! Não esqueça seus itens." (se
houver itens) ou "Saia agora para chegar a tempo!". O estilo segue o tipo
escolhido (LEMB-03 a LEMB-06).

- **a.** Dado o compromisso "Dentista" com os itens [Chaves], quando chega o horário de saída, então o lembrete mostra "HORA DE SAIR: Dentista" e "Saia agora! Não esqueça seus itens.".
- **b.** Dado um compromisso sem itens, quando chega o horário de saída, então a mensagem é "Saia agora para chegar a tempo!".
- ✅ Corrigido em [`lembrete-de-saida`](../changes/2026-10-26-lembrete-de-saida/); verificado em aparelho real em 2026-10-28 (testes A1, A2).

### LEMB-03 · Estilo PADRAO — ✅
ONDE o tipo é `PADRAO`, o sistema DEVE mostrar uma notificação de alta prioridade
cujo texto expandido termine com "📌 Não esqueça: <itens separados por vírgula>"
e que tenha a ação "Já Saí".

- **a.** Dado o tipo `PADRAO`, quando o lembrete de saída aparece, então ele tem a ação "Já Saí" e o texto expandido termina com "📌 Não esqueça: Chaves".
- **b.** Quando toco em "Já Saí", então a notificação some e o aviso de atraso (LEMB-08) não aparece.
- ✅ Corrigido em [`lembrete-de-saida`](../changes/2026-10-26-lembrete-de-saida/); verificado em aparelho real em 2026-10-28 (teste A1).

### LEMB-04 · Estilos de tela cheia (ALARME) — 🧪
ONDE o tipo é `ALARME`, `INSISTENTE` ou `DESAFIO`, o sistema DEVE abrir uma tela
cheia que acende a tela e aparece sobre o bloqueio, lista os itens e mostra o
mecanismo de confirmação do tipo. ONDE o tipo é `ALARME`, esse mecanismo é o
botão único "JÁ ESTOU SAINDO". SE o sistema não permitir abrir a tela cheia
sozinho, ENTÃO tocar na notificação DEVE abrir essa mesma tela. A notificação
DEVE ficar na bandeja até a saída ser confirmada.

- **a.** Dado o tipo `ALARME` com a tela bloqueada, quando chega o horário de saída, então a tela acende e mostra "JÁ ESTOU SAINDO".
- **b.** Dado que o sistema exibiu só a notificação (sem tela cheia), quando toco nela, então a tela do lembrete abre.
- 🧪 Corrigido em [`lembrete-de-saida`](../changes/2026-10-26-lembrete-de-saida/). **a** verificado em aparelho real em 2026-10-28 (teste A3); **b** aguarda o teste A4 (tela cheia negada).

### LEMB-05 · Estilo INSISTENTE — ✅
ONDE o tipo é `INSISTENTE`, a tela cheia DEVE exigir que o usuário segure o
botão "SEGURE" por 3 segundos contínuos. Soltar antes zera o progresso.

### LEMB-06 · Estilo DESAFIO — ✅
ONDE o tipo é `DESAFIO`, a tela cheia DEVE manter "CONFIRMAR SAÍDA" desabilitado
até o usuário digitar "JÁ FIZ" (sem diferenciar maiúsculas e ignorando espaços nas pontas).

### LEMB-07 · Confirmar saída — ✅
QUANDO o usuário confirma a saída (ação "Já Saí" ou botão da tela cheia), o
sistema DEVE gravar `confirmouSaida = true`, fechar o lembrete de saída e
impedir o aviso de atraso (LEMB-08).

- **a.** ✅ `confirmouSaida` é gravado.
- **b.** Dada a tela cheia aberta, quando confirmo a saída, então a tela fecha e a notificação do lembrete some da bandeja.
- **c.** Dada a saída confirmada, quando se passam 10 min do horário de saída, então "Você está atrasado!" não aparece.
- ✅ Corrigido em [`lembrete-de-saida`](../changes/2026-10-26-lembrete-de-saida/); verificado em aparelho real em 2026-10-28 (testes A1, A3).

### LEMB-08 · Aviso de atraso — ✅
QUANDO se passarem 10 min do horário de saída, SE a saída não tiver sido
confirmada, ENTÃO o sistema DEVE notificar "Você está atrasado! 🏃" com
"Ainda dá tempo! Saia agora para: <título>".

- O aviso é verificado 10 min após a saída *(teste: `HorariosLembreteTest`)*.

### LEMB-09 · Detecção de chegada — 🧪
ONDE o destino tem coordenadas (COMP-08) e a permissão de localização em segundo
plano foi concedida, QUANDO o usuário entrar num raio de 200 m do destino entre
o início do preparo e 2 horas depois do horário do evento, o sistema DEVE:
1. gravar `confirmouChegada = true` e `confirmouSaida = true`;
2. cancelar os lembretes desse compromisso que ainda não dispararam (etapas, saída e atraso) e fechar o lembrete de saída, se estiver aberto;
3. notificar "Você chegou! 🏁" com "Destino alcançado: <título>";
4. parar de monitorar o destino.

SE o usuário já estiver dentro do raio quando o período de chegada começar,
ENTÃO o sistema DEVE considerar a chegada nesse momento. O monitoramento DEVE
começar no início do período, e não antes. Entradas no raio fora desse período
DEVEM ser ignoradas. O monitoramento DEVE terminar sozinho 2 horas depois do
horário do evento.

- **a.** Dado um compromisso às 10:00 com preparo começando às 8:30, quando entro no raio às 9:40, então recebo "Você chegou! 🏁" e o compromisso fica com chegada confirmada. *(regra do período: `JanelaChegadaTest`)*
- **b.** Dado que cheguei às 9:10 e a saída era às 9:15, quando dá 9:15, então "HORA DE SAIR" não toca, e às 9:25 "Você está atrasado!" também não.
- **c.** Dado o mesmo compromisso, quando entro no raio no dia anterior, então nada acontece e a chegada no dia certo continua sendo detectada. *(regra do período: `JanelaChegadaTest`)*
- **d.** Dado o app fechado (processo encerrado), quando entro no raio dentro do período, então a chegada é detectada da mesma forma.
- **e.** Dado um destino sem coordenadas, quando salvo, então o compromisso é salvo normalmente, sem detecção de chegada (COMP-08).
- **f.** Dado que estou no destino quando salvo o compromisso e continuo lá, quando o preparo começa, então recebo "Você chegou! 🏁", e "HORA DE SAIR" e "Você está atrasado!" não tocam. *(início do monitoramento: `JanelaChegadaTest`)*
- 🧪 Implementado em [`deteccao-de-chegada`](../changes/2026-10-26-deteccao-de-chegada/) (Revisão 2 em 2026-10-28). Em aparelho: **c** (entrada antes do período ignorada) verificado em 2026-10-28; **a**, **b**, **d** e **f** aguardam os testes B5 e B6.

### LEMB-10 · Precisão e Doze — 🧪
Os alarmes DEVEM ser exatos e disparar mesmo com o aparelho em Doze
(`setExactAndAllowWhileIdle`, `RTC_WAKEUP`). SE o app não puder agendar alarmes
exatos, ENTÃO, ao salvar ou reativar um compromisso, o sistema DEVE exibir
"Permita “Alarmes e lembretes” para o Agora Vai avisar na hora certa." e NÃO
DEVE salvar nem reativar. Ao salvar, a mensagem DEVE oferecer a ação
"Abrir configurações", que leva à tela da permissão.

- **a.** Dada a permissão de alarmes exatos negada, quando salvo um compromisso válido, então vejo a mensagem com "Abrir configurações" e nada é gravado.
- **b.** Dada a permissão negada, quando religo um compromisso desativado, então vejo a mensagem e o switch continua desligado.
- 🧪 Corrigido em [`lembrete-de-saida`](../changes/2026-10-26-lembrete-de-saida/); aguarda o roteiro R4, que exige Android 12/12L (no 13+ a permissão é concedida automaticamente).

### LEMB-11 · Sobreviver a reinicialização — ⬜ DT-05
QUANDO o aparelho reiniciar ou a hora do sistema mudar, o sistema DEVE
reagendar alarmes e geofences de todos os compromissos ativos e futuros.
