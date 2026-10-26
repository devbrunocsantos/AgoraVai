# Capacidade: Automações do Pomodoro (`AUTO`)

Sugerir o início de uma sessão de foco num horário e/ou ao chegar num local.

**Código principal:** `ui/pomodoro/PomodoroViewModel.kt`,
`AutomationSettingsScreen.kt`, `HistoryScreen.kt`, `services/PomodoroScheduler.kt`,
`workers/PomodoroWorker.kt`, `services/geofence/GeofenceManager.kt`,
`GeofenceBroadcastReceiver.kt`, `data/AutomationSetting.kt`.

---

### AUTO-01 · Recurso premium — ⚠️ DT-09
SE o usuário não for assinante, ENTÃO tocar em "+" no Pomodoro DEVE abrir o
diálogo "Desbloqueie o Agendamento" com as opções "Assinar Agora" e "Depois".

- Hoje: o premium é forçado como ativo (billing em modo de teste).

### AUTO-02 · Criar e editar — ✅
O formulário DEVE ter título (padrão "Novo Pomodoro"), meta de sessões de foco
(padrão = ciclos até a pausa longa), gatilho de horário (switch + horário),
gatilho de local (switch + local) e repetição (switch + dias da semana).
O botão "Salvar" DEVE ficar habilitado somente quando:
título não vazio **E** meta inteira > 0 **E** ao menos um gatilho ligado **E**
(repetição desligada **OU** ≥ 1 dia) **E** (local desligado **OU** local escolhido).

### AUTO-03 · Escolher local — ✅
O sistema DEVE permitir escolher o local pelo Places Autocomplete (restrito ao
Brasil) ou pela localização atual (endereço por geocodificação reversa).

### AUTO-04 · Gatilho de horário — ✅
ONDE o gatilho de horário está ligado, o sistema DEVE avaliar a automação
diariamente por volta de HH:MM. O WorkManager é periódico de 24 h e pode
atrasar alguns minutos.

### AUTO-05 · Gatilho de local — ✅
ONDE o gatilho de local está ligado, o sistema DEVE monitorar um raio de 100 m
em volta do local e registrar se o usuário está dentro ou fora. QUANDO o usuário
entra no raio, o sistema DEVE avaliar a automação.

### AUTO-06 · Condições para sugerir — ✅
Ao avaliar uma automação, o sistema DEVE sugerir a sessão somente se:
a automação estiver ativa **E** (repetição desligada **OU** hoje estiver nos dias)
**E** (gatilho de local desligado **OU** usuário dentro do raio).

- Observação: a entrada no raio avalia a automação em qualquer horário, mesmo com o gatilho de horário ligado. Os gatilhos funcionam como "OU" para disparar, e o local funciona como condição "E" do horário.

### AUTO-07 · Sugestão — ✅
QUANDO as condições forem satisfeitas, o sistema DEVE notificar "Hora de Focar!"
com "Pronto para iniciar sua sessão de '<título>'?" e as ações "Iniciar" e
"Ignorar". "Iniciar" DEVE começar o Pomodoro com a meta da automação.
"Ignorar" ou dispensar a notificação DEVE apenas fechá-la.

### AUTO-08 · Automação de uso único — ✅
ONDE a repetição está desligada, depois de sugerir a sessão o sistema DEVE
arquivar a automação no histórico e remover a geofence dela.

### AUTO-09 · Ativar e desativar — ✅ / ⚠️ DT-07
O switch na lista DEVE ativar ou desativar a automação, reagendando ou
cancelando os gatilhos dela.

- ⚠️ Reagendar cancela **todo** o trabalho do WorkManager do app, não só o das automações.

### AUTO-10 · Excluir — ✅
QUANDO o usuário toca em "Excluir Pomodoro" na edição, o sistema DEVE apagar a
automação e cancelar os gatilhos dela.

### AUTO-11 · Histórico — ✅
O histórico DEVE listar as automações arquivadas e permitir "Reativar" (volta a
ficar ativa e reagenda), "Excluir" e "Limpar Histórico", este com confirmação
("Esta ação não pode ser desfeita."). ENQUANTO estiver vazio, DEVE mostrar
"Seu histórico está vazio.".

### AUTO-12 · Lista — ✅
Cada automação na lista DEVE mostrar título, "<n> ciclos de foco", "Às HH:MM"
(se houver horário), "Em: <endereço>" (se houver local) e
"Repete: Diariamente | <dias>" (se houver repetição).
