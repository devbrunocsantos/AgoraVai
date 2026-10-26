# Spec — Detecção de chegada

- **Data:** 2026-10-26
- **Autor:** Claude (a pedido de Bruno C. Santos)
- **Status:** Rascunho
- **Capacidades afetadas:** `capabilities/lembretes.md`
- **DTs relacionados:** DT-01
- **Depende de:** `2026-10-26-lembrete-de-saida` (usa o agendador para cancelar lembretes)

## 1. Problema
O app promete reconhecer a chegada ao destino, mas a detecção nunca é ativada.
Ativá-la como está criaria problemas novos: a geofence vale para sempre e
dispara a qualquer hora. Se o destino for um lugar que o usuário frequenta (o
trabalho, por exemplo), ele receberia "Você chegou!" dias antes do compromisso,
e a chegada real não seria detectada. Além disso, chegar cedo não impediria o
"HORA DE SAIR" de tocar depois.

## 2. Resultado esperado
Quando o usuário chega ao destino no período do compromisso, o app reconhece a
chegada, comemora e silencia os lembretes que perderam o sentido. Fora desse
período, estar no local não conta.

## 3. Delta de requisitos

### ALTERADOS

#### LEMB-09 · Detecção de chegada
- **Antes:** ONDE o destino tem coordenadas (COMP-08) e a permissão de localização em segundo plano foi concedida, QUANDO o usuário entrar num raio de 200 m do destino, o sistema DEVE gravar `confirmouChegada = true`, notificar "Você chegou! 🏁" com "Destino alcançado: <título>" e remover a geofence.
- **Depois:** ONDE o destino tem coordenadas (COMP-08) e a permissão de localização em segundo plano foi concedida, QUANDO o usuário entrar num raio de 200 m do destino **entre o início do preparo e 2 horas depois do horário do evento**, o sistema DEVE:
  1. gravar `confirmouChegada = true` e `confirmouSaida = true`;
  2. cancelar os lembretes desse compromisso que ainda não dispararam (etapas, saída e atraso);
  3. notificar "Você chegou! 🏁" com "Destino alcançado: <título>";
  4. parar de monitorar o destino.

  Entradas no raio fora desse período DEVEM ser ignoradas. O monitoramento DEVE terminar sozinho 2 horas depois do horário do evento.
- **Critérios de aceite:**
  - **a.** Dado um compromisso às 10:00 com preparo começando às 8:30, quando entro no raio às 9:40, então recebo "Você chegou! 🏁" e o compromisso fica com chegada confirmada.
  - **b.** Dado que cheguei às 9:10 e a saída era às 9:15, quando dá 9:15, então "HORA DE SAIR" não toca, e às 9:25 "Você está atrasado!" também não.
  - **c.** Dado o mesmo compromisso, quando entro no raio no dia anterior, então nada acontece e a chegada no dia certo continua sendo detectada.
  - **d.** Dado o app fechado (processo encerrado), quando entro no raio dentro do período, então a chegada é detectada da mesma forma.
  - **e.** Dado um destino sem coordenadas, quando salvo, então o compromisso é salvo normalmente, sem detecção de chegada (COMP-08).

## 4. Fora de escopo
- Re-registrar a geofence depois de reiniciar o aparelho (mudança `reagendar-apos-reiniciar`).
- Caso de borda: se o usuário já estiver dentro do raio quando o período começar e não sair dele, a chegada não é detectada (o Android só avisa na entrada). Fica para o futuro, com uso de "permanência" (`DWELL`).

## 5. Constituição
**P2**: o lembrete é coerente com o que aconteceu, sem "Hora de sair" para quem já chegou. **P5**: celebra a chegada e evita ruído.

## 6. Perguntas em aberto
- [ ] **Q3 —** o período "do início do preparo até 2 h depois do evento" está bom? (2 h cobre quem chega atrasado.)
- [ ] **Q4 —** chegar deve mesmo cancelar os lembretes pendentes e marcar a saída? Recomendo que sim.
