# Spec — Reagendar após reiniciar ou atualizar

- **Data:** 2026-10-26
- **Autor:** Claude (a pedido de Bruno C. Santos)
- **Status:** Rascunho
- **Capacidades afetadas:** `capabilities/lembretes.md`, `capabilities/automacoes.md`
- **DTs relacionados:** DT-05
- **Depende de:** `2026-10-26-lembrete-de-saida` e `2026-10-26-deteccao-de-chegada`

## 1. Problema
Quando o celular reinicia (inclusive por falta de bateria), o Android apaga
todos os alarmes e geofences do app. O usuário não recebe mais nenhum lembrete
dos compromissos já cadastrados e não tem como saber disso. Além disso, quem
atualizar o app continuaria com os lembretes defeituosos agendados pela versão
antiga.

## 2. Resultado esperado
Depois de reiniciar ou atualizar, sem o usuário abrir o app, todos os lembretes
futuros voltam a estar agendados. Se não for possível, o usuário é avisado.

## 3. Delta de requisitos

### ALTERADOS

#### LEMB-11 · Sobreviver a reinicialização
- **Antes:** QUANDO o aparelho reiniciar ou a hora do sistema mudar, o sistema DEVE reagendar alarmes e geofences de todos os compromissos ativos e futuros.
- **Depois:** QUANDO o aparelho terminar de reiniciar ou o app for atualizado, o sistema DEVE, sem precisar que o app seja aberto, reagendar os lembretes (etapas, saída e atraso) que ainda estão no futuro e a detecção de chegada (LEMB-09) cujo período não terminou, para todos os compromissos ativos. Lembretes cujo horário já passou NÃO DEVEM disparar. SE o app não puder agendar alarmes exatos, ENTÃO o sistema DEVE notificar "Seus lembretes estão pausados" com "Permita “Alarmes e lembretes” para o Agora Vai avisar na hora certa.", e tocar na notificação DEVE abrir o app.
- **Nota:** a mudança da hora do sistema saiu do requisito. Os horários são guardados como instantes absolutos e o `AlarmManager` (RTC) já acompanha a mudança, então não há o que fazer.
- **Critérios de aceite:**
  - **a.** Dado um compromisso amanhã às 10:00 com saída às 9:15, quando reinicio o aparelho hoje e não abro o app, então às 9:15 o lembrete de saída toca.
  - **b.** Dado um compromisso cujo alarme da primeira etapa era às 8:00, quando o aparelho reinicia às 8:10, então essa etapa não toca, mas as seguintes, a saída e o atraso continuam agendados.
  - **c.** Dado um compromisso desativado, quando o aparelho reinicia, então nada é agendado para ele.
  - **d.** Dada a versão anterior instalada com lembretes agendados, quando atualizo o app, então os lembretes passam a aparecer com título, mensagem e "Já Saí" (LEMB-02/03).
  - **e.** Dada a permissão de alarmes exatos negada, quando o aparelho reinicia, então recebo "Seus lembretes estão pausados".

#### AUTO-05 · Gatilho de local
- **Antes:** ONDE o gatilho de local está ligado, o sistema DEVE monitorar um raio de 100 m em volta do local e registrar se o usuário está dentro ou fora. QUANDO o usuário entra no raio, o sistema DEVE avaliar a automação.
- **Depois:** igual, acrescentando: o monitoramento DEVE continuar após reiniciar o aparelho ou atualizar o app, sem precisar abrir o app.
- **Critérios de aceite:**
  - **a.** Dada uma automação com local ativa, quando reinicio o aparelho e entro no raio sem abrir o app, então recebo "Hora de Focar!".

## 4. Fora de escopo
- O gatilho de horário das automações já sobrevive à reinicialização (o WorkManager se reagenda sozinho). Nada muda.
- Repetição semanal (COMP-13).

## 5. Constituição
Corrige a violação de **P2** apontada em DT-05.

## 6. Perguntas em aberto
- [ ] **Q5 —** o texto "Seus lembretes estão pausados" / "Permita “Alarmes e lembretes”…" está bom?
