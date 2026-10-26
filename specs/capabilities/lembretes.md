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

- **a.** Dadas a saída às 9:00 e as etapas [Banho 15, Café 10], então "Banho" dispara às 8:35 e "Café" às 8:50.

### LEMB-02 · Lembrete "Hora de sair" — ⚠️ DT-02
No horário de saída, o sistema DEVE disparar o lembrete com título
"HORA DE SAIR: <título>" e mensagem "Saia agora! Não esqueça seus itens." (se
houver itens) ou "Saia agora para chegar a tempo!". O estilo segue o tipo
escolhido (LEMB-03 a LEMB-06).

- Hoje: o lembrete dispara no horário certo, mas aparece como "Lembrete" / "Hora do compromisso", porque as chaves dos extras divergem.

### LEMB-03 · Estilo PADRAO — ⚠️ DT-02
ONDE o tipo é `PADRAO`, o sistema DEVE mostrar uma notificação de alta prioridade
cujo texto expandido termine com "📌 Não esqueça: <itens separados por vírgula>"
e que tenha a ação "Já Saí".

- Hoje: os itens aparecem, mas a ação "Já Saí" não.

### LEMB-04 · Estilo ALARME — ✅
ONDE o tipo é `ALARME`, o sistema DEVE abrir uma tela cheia que acende a tela e
aparece sobre o bloqueio, lista os itens e tem o botão único "JÁ ESTOU SAINDO".

### LEMB-05 · Estilo INSISTENTE — ✅
ONDE o tipo é `INSISTENTE`, a tela cheia DEVE exigir que o usuário segure o
botão "SEGURE" por 3 segundos contínuos. Soltar antes zera o progresso.

### LEMB-06 · Estilo DESAFIO — ✅
ONDE o tipo é `DESAFIO`, a tela cheia DEVE manter "CONFIRMAR SAÍDA" desabilitado
até o usuário digitar "JÁ FIZ" (sem diferenciar maiúsculas e ignorando espaços nas pontas).

### LEMB-07 · Confirmar saída — ⚠️ DT-16
QUANDO o usuário confirma a saída (ação "Já Saí" ou botão da tela cheia), o
sistema DEVE gravar `confirmouSaida = true`, fechar o lembrete de saída e
impedir o aviso de atraso (LEMB-08).

- **a.** ✅ `confirmouSaida` é gravado.
- **b.** ⚠️ A notificação de tela cheia é `ongoing` e não é cancelada ao confirmar.

### LEMB-08 · Aviso de atraso — ✅
QUANDO se passarem 10 min do horário de saída, SE a saída não tiver sido
confirmada, ENTÃO o sistema DEVE notificar "Você está atrasado! 🏃" com
"Ainda dá tempo! Saia agora para: <título>".

### LEMB-09 · Detecção de chegada — ⚠️ DT-01
ONDE o destino tem coordenadas (COMP-08) e a permissão de localização em segundo
plano foi concedida, QUANDO o usuário entrar num raio de 200 m do destino, o
sistema DEVE gravar `confirmouChegada = true`, notificar "Você chegou! 🏁" com
"Destino alcançado: <título>" e remover a geofence.

- Hoje: a geofence nunca é registrada porque `CompromissoGeofenceManager.initialize()` não é chamado.

### LEMB-10 · Precisão e Doze — ✅ / ⚠️ DT-17
Os alarmes DEVEM ser exatos e disparar mesmo com o aparelho em Doze
(`setExactAndAllowWhileIdle`, `RTC_WAKEUP`).
SE o app não puder agendar alarmes exatos (Android 12+), ENTÃO o sistema DEVE
avisar o usuário e oferecer o atalho para a permissão.

- Hoje: os alarmes de etapa deixam de ser agendados em silêncio. O alarme de saída lança uma exceção, que aparece como "Erro ao salvar." depois de o compromisso já ter sido gravado.

### LEMB-11 · Sobreviver a reinicialização — ⬜ DT-05
QUANDO o aparelho reiniciar ou a hora do sistema mudar, o sistema DEVE
reagendar alarmes e geofences de todos os compromissos ativos e futuros.
