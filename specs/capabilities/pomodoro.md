# Capacidade: Pomodoro (`POMO`)

Timer de foco com pausas, rodando em segundo plano.

**Código principal:** `services/PomodoroService.kt`, `PomodoroTimerState.kt`,
`FocusSessionState.kt`, `PomodoroCompletionState.kt`,
`ui/pomodoro/PomodoroScreen.kt`, `PomodoroViewModel.kt`, `utils/SettingsManager.kt`.

**Estados:** fase `PARADO | FOCO | PAUSA_CURTA | PAUSA_LONGA` e timer
`PARADO | RODANDO | PAUSADO`.

---

### POMO-01 · Controles do timer — ✅
O sistema DEVE oferecer Iniciar/Retomar, Pausar e Resetar. QUANDO o usuário
inicia a partir de `PARADO`, o sistema DEVE entrar em `FOCO` com a duração de
foco configurada e o ciclo zerado. Retomar continua do tempo restante. Resetar
volta a `PARADO`.

### POMO-02 · Execução em segundo plano — ✅
ENQUANTO o timer estiver rodando ou pausado, o sistema DEVE manter uma
notificação persistente, atualizada a cada segundo, com:
- título: "Hora de Focar! 💪 (<ciclo>/<meta>)", "Pausa Curta! ☕" ou "Pausa Longa! ✨";
- texto: "Tempo restante: MM:SS" ou "Pausado em MM:SS";
- ações: "Pausar" (rodando) ou "Retomar" (pausado), e "Resetar".

Tocar na notificação DEVE abrir o app na aba Pomodoro.

### POMO-03 · Durações configuráveis — ✅
O sistema DEVE usar as durações das Configurações. Os padrões são foco 25,
pausa curta 5, pausa longa 15, 4 ciclos até a pausa longa e meta 8. Valor
inválido ao salvar DEVE cair no padrão. QUANDO as configurações são salvas
com o timer parado, o tempo exibido DEVE passar a ser a nova duração de foco.

### POMO-04 · Transições de fase — ✅
QUANDO uma fase de foco termina, o sistema DEVE tocar o som de notificação,
incrementar o ciclo e ir para `PAUSA_LONGA` se `ciclo % ciclosAtePausaLonga == 0`,
ou para `PAUSA_CURTA` caso contrário. QUANDO uma pausa termina, DEVE voltar a `FOCO`.
ENQUANTO o app estiver em segundo plano, cada transição DEVE gerar uma notificação:
"Seu tempo de foco acabou. Hora de uma pausa curta!", "Ótimo trabalho! Hora da
sua merecida pausa longa." ou "A pausa terminou. Hora de voltar a focar!".

- **a.** Dado 4 ciclos até a pausa longa, quando o 4º foco termina, então a fase vira `PAUSA_LONGA`.

### POMO-05 · Conclusão da sessão — ⚠️ DT-18
QUANDO o número de focos concluídos atingir a meta da sessão, o sistema DEVE
encerrar o timer e:
- com o app visível: mostrar o diálogo "Sessão Concluída!";
- com o app em segundo plano: notificar "Sessão Concluída!" / "Parabéns! Você completou todos os ciclos.".

A meta da sessão DEVE ser a da automação que a iniciou (AUTO-07) ou, no início
manual, a meta diária. Ela DEVE se manter durante toda a sessão.

- **a.** ⚠️ Pausar e retomar uma sessão iniciada por automação troca a meta pela meta diária.

### POMO-06 · Progresso diário — ⚠️ DT-08
O sistema DEVE gravar cada foco concluído e mostrar quantos focos foram
concluídos hoje em relação à meta diária.

- **a.** ⚠️ Focos concluídos enquanto a interface não está carregada não são gravados.
- Observação: registros com mais de 24 h são apagados ao abrir o Pomodoro, então não existe histórico de longo prazo.

### POMO-07 · Layout adaptável — ✅
O sistema DEVE mostrar a tela do Pomodoro em layout próprio para retrato e
para paisagem, com o timer circular e a lista de automações.
