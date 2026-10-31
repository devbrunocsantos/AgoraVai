---
spec: docs/sdd/specs/005-lembretes-na-edicao-e-fim-do-evento/spec.md
pesquisa: docs/sdd/specs/005-lembretes-na-edicao-e-fim-do-evento/pesquisa.md
data: 2026-10-30
commit_base: e45fbfd600eed39a6e5907217635f51dc67ffc09
status: em-implementacao  # rascunho | aprovado | em-implementacao | concluido
---

# Plano de implementação: Lembretes durante a edição e compromisso encerrado

> Caminhos de código relativos a `app/src/main/java/com/brunocsantos/agoravai/`; testes em `app/src/test/java/com/brunocsantos/agoravai/notifications/`.

## Visão geral

Duas mudanças independentes, sobre a mesma base de regras puras:

1. **Retenção no disparo (História 1).** Nenhum alarme é cancelado durante a edição. Um `object EdicaoEmAndamento` guarda qual compromisso está em edição e se o formulário está visível. `AlarmReceiver` (etapas e saída) e `CHECK_LATE` (atraso) consultam esse estado antes de notificar e, se retido, apenas registram que houve retenção. Ao sair sem salvar ou quando o formulário deixa de estar visível, a regra da 003 (`planoDeReagendamento(reinicio = true)`) decide se o "HORA DE SAIR" toca — só se houve retenção.
2. **Encerrado derivado (História 2).** "Encerrado" é calculado (`diasRepeticao` vazio e `agora >= dataHoraEvento`), sem mudar o banco. A lista mostra o switch desligado/esmaecido e se atualiza a cada 30 s; ligar um encerrado é bloqueado antes da checagem de permissão; religar antes do evento passa pela regra da 003.

## Estado atual

- `notifications/AlarmReceiver.kt:30-49` — mostra o que vier nos extras, sem consultar estado.
- `services/NotificationActionReceiver.kt:78-99` — `CHECK_LATE` relê o banco e notifica se `!confirmouSaida`.
- `notifications/NotificationScheduler.kt:43-80` — alarme de etapa sem `EXTRA_COMPROMISSO_ID`.
- `notifications/HorariosLembrete.kt:81-119` — `planoDeReagendamento` (regra da 003), hoje usado só por `Reagendador.planoDe` (privado, `notifications/Reagendador.kt:151-169`).
- `MainActivity.kt:254` — `showAddCompromissoScreen` em `remember` (rotação fecha o formulário); fechamento em `:267-278` (BackHandler) e `:346-349` (ícone); `:113-116` observa `onStart/onStop`.
- `ui/compromissos/CompromissoViewModel.kt:222-234` — `toggleCompromisso` agenda tudo sem filtrar passado; `:141-220` salvar.
- `ui/compromissos/CompromissosScreen.kt:112-127` — switch e esmaecimento só por `isEnabled`.
- Banco v10 com `fallbackToDestructiveMigration` (`data/AppDatabase.kt:36`) — este plano não mexe no schema.

## Estado desejado

- Com o formulário de edição de A aberto e visível, nenhum lembrete de A aparece; ao sair sem salvar (ou o formulário sair da tela), toca no máximo o "HORA DE SAIR", nas condições do RF-003, e só se algo foi retido.
- Ao salvar, o "HORA DE SAIR" antigo (bandeja e tela cheia) fecha e vale o novo agendamento.
- Compromisso sem repetição aparece desligado/esmaecido a partir do horário do evento (em até 30 s com a lista aberta), sem perder atraso e chegada; ligá-lo mostra "Esse compromisso já passou. Edite a data para ativá-lo de novo.".
- Religar antes do evento toca no máximo o "HORA DE SAIR".
- `./gradlew testDebugUnitTest assembleDebug` verde, com os testes novos citando os CAs, e roteiros manuais registrados neste plano.

## Fora de escopo

- Repetição semanal (COMP-13/DT-04): compromissos com `diasRepeticao` não são encerrados nem bloqueados.
- Mudar COMP-07 ("Impossível! Atraso de N min.") ou o "Excluir após concluir" (COMP-14), inclusive o fato de `deleteExpiredCompromissos` não cancelar alarmes.
- Migração do Room / DT-03.
- Corrigir os literais `"EXTRA_COMPROMISSO_ID"` existentes em `NotificationActionReceiver.kt:26` e `ReminderActivity.kt:57,80` (DT-02) — o código novo usa as constantes, sem piorar.
- Fechar notificações de etapa ou de atraso já exibidas ao salvar.
- Unificar `cancelar(context, id, 10)` (salvar) e `cancelar(id, etapas.size)` (toggle).

## Decisões de design

| Decisão | Alternativas consideradas | Motivo da escolha |
|---|---|---|
| D1 — Reter **no disparo** (`EdicaoEmAndamento` consultado por `AlarmReceiver` e `CHECK_LATE`) | Cancelar alarmes ao abrir e reagendar ao sair; híbrido | Seguro na falha (P2): se o processo morrer, o estado zera e os alarmes tocam. Cancelar deixaria lembretes perdidos até o próximo boot após um crash. Aprovado pelo Bruno (2026-10-30). |
| D2 — "Encerrado" **derivado** (`estaEncerrado`), sem coluna | Coluna + `Migration`; gravar `isEnabled=false` | Sem risco a dados (P3); `isEnabled` segue `true`, então atraso, geofence e Reagendador continuam funcionando (CA-2.7/2.8). `isEnabled=false` quebraria RF-007. Aprovado pelo Bruno. |
| D3 — Religar via `planoDeReagendamento(reinicio = true)` + `reagendar`, **respeitando** `confirmouSaida/Chegada` e **sem ler a localização** | Manter `agendar`; zerar confirmações; ler localização como o Reagendador | Toca em até 5 s (RNF-001); com chegada já registrada, nada é agendado; quem já está no destino tem a chegada detectada logo depois pela geofence (`INITIAL_TRIGGER_ENTER`). Aprovado pelo Bruno. |
| Liberar só **se houve retenção** | Liberar sempre que a saída passou | Evita repetir a notificação já na bandeja (P5, §9 "continua aberto"). Aprovado pelo Bruno. |
| Estado "formulário aberto" vai para o ViewModel (`StateFlow`) | `rememberSaveable` | O VM (escopo de Activity) sobrevive à rotação; após morte do processo o formulário fecha junto com o `EdicaoEmAndamento`, sem restaurar um formulário vazio. Efeito colateral aceito: a criação também passa a sobreviver à rotação. |
| Liberação entrega a saída com `EXTRA_SEM_RETENCAO = true` | `sendBroadcast` comum | Evita corrida: se o formulário voltar a ficar visível antes da entrega do broadcast, a saída liberada não é retida de novo. |
| Sair durante um salvamento em andamento: contador `sessaoFormulario` checado antes das gravações (desvio da implementação, 2026-10-30) | Cancelar o `Job` do salvamento | Cancelar no meio poderia deixar o compromisso gravado com os alarmes já cancelados e sem o novo agendamento (P2). Com a sessão, o salvamento iniciado em outra abertura do formulário termina sem gravar. |
| Lista atualiza com um fluxo `agora` de 30 s (`WhileSubscribed`) | Alarme no horário do evento | Mais simples; atende RNF-002 (≤ 1 min); só roda com a lista na tela. |
| `onStop` com `isChangingConfigurations` não libera | Liberar sempre no `onStop` | Rotação não conta como sair (RF-006, CA-1.15). |
| Liberação roda num escopo do processo (`CoroutineScope(SupervisorJob() + Dispatchers.IO)`), não no `viewModelScope` | `viewModelScope` | A tela cheia de outro compromisso abre com `NEW_TASK | CLEAR_TASK` na mesma task (`AlarmReceiver.kt:61`, sem `taskAffinity` em `AndroidManifest.xml:83-90`) e destrói a `MainActivity` logo após o `onStop`; o VM é limpo e cancelaria a liberação. Padrão de `NotificationActionReceiver.kt:47`. |
| RF-002 fecha só a **notificação** do "HORA DE SAIR" antigo (`cancel(idAlarmeSaida(id))`); não há código para fechar a `ReminderActivity` | Evento para a `ReminderActivity` fechar | Pela configuração atual, com a tela cheia aberta a `MainActivity` não existe (CLEAR_TASK) e abrir o app traz de volta a própria tela cheia: salvar com ela aberta é inalcançável. O que resta do lembrete antigo ao salvar é a notificação (fixa no tipo ALARME). P8: sem estado novo para um caso inalcançável. |

## Conformidade com a constituição

- [x] P1 A spec vem primeiro — spec `pesquisada`; este plano aguarda aprovação; a Fase 5 atualiza `capacidades/`.
- [x] P2 Lembrete nunca falha em silêncio — retenção sem cancelar alarmes; falha do processo libera tudo; liberação ao sair/onStop/reinício.
- [x] P3 Dados preservados — sem mudança de schema.
- [x] P4 Local-first — nenhuma chamada de rede nova.
- [x] P5 Desenho para quem luta contra o tempo — texto novo aprovado na spec (RF-008); no máximo uma notificação ao liberar.
- [x] P6 Contratos explícitos — `EXTRA_COMPROMISSO_ID` passa a ir no alarme de etapa e `EXTRA_SEM_RETENCAO` é novo; ambos como constantes em `AlarmReceiver.kt` e documentados em `arquitetura.md` §5.2 (Fase 5). Request codes e IDs de notificação não mudam.
- [x] P7 Cada CA verificável — regras puras com testes JUnit que citam os CAs; o resto em roteiros manuais neste plano.
- [x] P8 Simplicidade — sem dependências; `object` de estado no padrão de `services/AppForegroundState.kt`.
- [x] P9 Consistência — nomes em português, `StateFlow` no VM, comentários com IDs.

---

## Fase 1: Regras puras

### Objetivo
Concentrar em código puro e testado todas as decisões novas, sem mudar comportamento. As fases seguintes só ligam essas funções ao Android.

### Mudanças

#### 1. Regras novas
**Arquivo**: `notifications/HorariosLembrete.kt` (junto de `planoDeReagendamento`, mesmo estilo de KDoc e comentários com ID)

```kotlin
// LEMB-12 (RF-001, RF-004, RF-006): o lembrete é retido só se for do compromisso em edição
// e o formulário estiver visível. Criação (idEmEdicao = null) nunca retém.
fun deveReter(compromissoId: Long, idEmEdicao: Long?, formularioVisivel: Boolean): Boolean =
    idEmEdicao != null && compromissoId == idEmEdicao && formularioVisivel

// LEMB-12 (RF-003, RF-006): ao liberar, toca o "HORA DE SAIR" só se algo foi retido e a
// regra da 003 considerar a saída perdida (saída passou, não confirmada, evento não começou).
fun deveTocarSaidaAoLiberar(houveRetencao: Boolean, plano: Reagendamento): Boolean =
    houveRetencao && plano.saidaPerdida

// COMP-10 (RF-007, RF-009): sem dias de repetição e com o horário do evento alcançado.
fun estaEncerrado(diasRepeticao: String, dataHoraEvento: Long, agora: Long): Boolean =
    diasRepeticao.isBlank() && agora >= dataHoraEvento

enum class DecisaoAoLigar { BLOQUEAR_PASSADO, BLOQUEAR_PERMISSAO, LIGAR }

// COMP-10 (RF-008): o bloqueio de compromisso passado vem antes da exigência de alarme exato (LEMB-10)
fun decisaoAoLigar(encerrado: Boolean, podeAgendarExato: Boolean): DecisaoAoLigar = when {
    encerrado -> DecisaoAoLigar.BLOQUEAR_PASSADO
    !podeAgendarExato -> DecisaoAoLigar.BLOQUEAR_PERMISSAO
    else -> DecisaoAoLigar.LIGAR
}
```

#### 2. Plano compartilhado
**Arquivo**: `notifications/Reagendador.kt`
**Mudança**: `planoDe(c, agora, reinicio)` (`:151-169`) deixa de ser `private` (vira `fun` pública do `object`), para o ViewModel usar a mesma conversão entidade → plano. Sem mudança de lógica.

#### 3. Testes
**Arquivos novos**: `EdicaoLembretesTest.kt` (História 1) e `EncerramentoTest.kt` (História 2), no padrão de `ReagendamentoTest.kt:13-37` (helper `em(dia, h, m)` em UTC, fixture `plano(...)` com parâmetros padrão, nome entre crases começando pelo ID sem ponto, comentário `// CA-x.y (CAP-nn.l): cenário` acima). Casos na tabela "Estratégia de testes".

### Critérios de sucesso

#### Verificação automatizada
- [x] `./gradlew testDebugUnitTest assembleDebug` verde, sem falha em nenhum teste existente (linha de base: 30/30 em 2026-10-30) ✅ 55/55
- [x] `EdicaoLembretesTest` cobre CA-1.1, 1.2, 1.4, 1.5, 1.6, 1.8, 1.9, 1.10, 1.11, 1.12, 1.13
- [x] `EncerramentoTest` cobre CA-2.1, 2.2, 2.3, 2.4, 2.7, 2.8, 2.9, 2.10, 2.11 e a regressão da LEMB-10 (evento futuro sem permissão → `BLOQUEAR_PERMISSAO`)

#### Verificação manual
- Nenhuma (sem mudança de comportamento).

> ⏸ Commit da fase e seguir.

---

## Fase 2: Retenção durante a edição (P1)

### Objetivo
Entregar a História 1 (exceto o fechamento do lembrete antigo ao salvar, Fase 3).

### Mudanças

#### 1. Estado da edição
**Arquivo novo**: `services/EdicaoEmAndamento.kt` (padrão de `services/AppForegroundState.kt:9-16`; receivers e UI rodam na main thread, mas os métodos ficam `@Synchronized` por segurança e cada leitura+mudança é atômica)

```kotlin
/** LEMB-12: compromisso em edição e se o formulário está visível. Só em memória: se o
 *  processo morrer, tudo volta a tocar normalmente (P2). */
object EdicaoEmAndamento {
    private var idEmEdicao: Long? = null
    private var visivel = false
    private var houveRetencao = false

    @Synchronized fun iniciar(compromissoId: Long) { idEmEdicao = compromissoId; visivel = true; houveRetencao = false }
    @Synchronized fun tornarVisivel() { if (idEmEdicao != null) visivel = true }

    /** Chamado pelos receivers: devolve true e registra a retenção se o lembrete deve esperar. */
    @Synchronized fun reter(compromissoId: Long): Boolean =
        deveReter(compromissoId, idEmEdicao, visivel).also { if (it) houveRetencao = true }

    /** Formulário deixou de estar visível: para de reter e devolve (id, houve retenção). */
    @Synchronized fun suspender(): Pair<Long?, Boolean> {
        visivel = false
        return (idEmEdicao to houveRetencao).also { houveRetencao = false }
    }

    /** Fim da edição (salvou ou saiu): limpa tudo e devolve (id, houve retenção). */
    @Synchronized fun encerrar(): Pair<Long?, Boolean> = suspender().also { idEmEdicao = null }
}
```

#### 2. Contratos
**Arquivo**: `notifications/AlarmReceiver.kt` (constantes `:13-24`)
**Mudança**: nova `const val EXTRA_SEM_RETENCAO = "EXTRA_SEM_RETENCAO"`.

**Arquivo**: `notifications/NotificationScheduler.kt` (`scheduleAlarm`, `:43-80`)
**Mudança**: novo parâmetro `compromissoId: Long = -1L`; quando `!= -1L`, `putExtra(EXTRA_COMPROMISSO_ID, compromissoId)` no `Intent` do alarme. Request code inalterado (extras não entram na identidade do `PendingIntent`, então `cancelAlarm` continua acertando alarmes antigos).

**Arquivo**: `notifications/CompromissoAlarmScheduler.kt` (`agendarEtapa`, `:92-101`)
**Mudança**: passar `compromissoId` para `scheduleAlarm`.

**Arquivo**: `notifications/CompromissoAlarmScheduler.kt` (`dispararSaidaAgora`, `:126-129`)
**Mudança**: novo parâmetro `semRetencao: Boolean = false`; quando `true`, `putExtra(EXTRA_SEM_RETENCAO, true)` no intent de `intentSaida` antes do `sendBroadcast`. O chamador atual (`Reagendador.kt:96-98`) não muda.

#### 3. Receivers consultam a retenção
**Arquivo**: `notifications/AlarmReceiver.kt` (`onReceive`, `:30-49`)
**Mudança**: logo após ler `compromissoId`:

```kotlin
// LEMB-12 (RF-001): lembrete do compromisso em edição espera enquanto o formulário está visível
val semRetencao = intent.getBooleanExtra(EXTRA_SEM_RETENCAO, false)
if (compromissoId != -1L && !semRetencao && EdicaoEmAndamento.reter(compromissoId)) {
    Log.d(TAG, "Lembrete retido durante a edição: compromisso $compromissoId")
    return
}
```

Como a tela cheia só abre pela notificação, retê-la também cobre CA-1.7.

**Arquivo**: `services/NotificationActionReceiver.kt` (`CHECK_LATE`, `:78-99`)
**Mudança**: antes de abrir a corrotina, `if (EdicaoEmAndamento.reter(compromissoId)) return` (com comentário `// LEMB-12 (RF-001)`). O atraso retido não é reagendado: pela regra da 003, atraso passado é descartado ao liberar.

#### 4. Formulário no ViewModel e liberação
**Arquivo**: `ui/compromissos/CompromissoViewModel.kt`

- Novo estado `private val _formularioAberto = MutableStateFlow(false)`, exposto como `formularioAberto: StateFlow<Boolean>`.
- `fun abrirCriacao()` → `resetAddCompromissoState()`; `_formularioAberto.value = true`.
- `fun abrirEdicao(c: CompromissoComEtapas)` → `iniciarEdicao(c)`; `EdicaoEmAndamento.iniciar(c.compromisso.id)`; `_formularioAberto.value = true`.
- `fun fecharSemSalvar()` → cancela o `Job` de salvamento em andamento (ver abaixo); `_formularioAberto.value = false`; `resetAddCompromissoState()`; `val (id, houve) = EdicaoEmAndamento.encerrar()`; `liberar(id, houve)`.
- `fun formularioSaiuDaTela()` (chamado no `onStop` real) → se `_formularioAberto.value`: `val (id, houve) = EdicaoEmAndamento.suspender()`; `liberar(id, houve)`.
- `onCleared()` → `EdicaoEmAndamento.encerrar()` (o VM só é limpo com a Activity destruída; a liberação já aconteceu no `onStop`).
- `salvarCompromisso` guarda o `Job` do `viewModelScope.launch` (`private var salvamento: Job?`), para `fecharSemSalvar` cancelá-lo: sair durante a chamada de rota não pode terminar gravando e reagendando depois da liberação.
- `fun formularioVoltouATela()` (chamado no `onStart`) → se `_formularioAberto.value`: `EdicaoEmAndamento.tornarVisivel()`.
- No `salvarCompromisso`, o `onSuccess` passa a ser precedido por `EdicaoEmAndamento.encerrar()` (retidos descartados, RF-002) e `_formularioAberto.value = false`. Recusas não mexem no estado (a edição continua).
- Liberação (`private fun liberar(id: Long?, houveRetencao: Boolean)`):

```kotlin
// LEMB-12 (RF-003, RF-006): regra da 003 — no máximo o "HORA DE SAIR", e só se algo foi retido
// A guarda antecipada só evita a leitura do banco; a regra testada é deveTocarSaidaAoLiberar
if (id == null || !houveRetencao) return
// Escopo do processo: a MainActivity pode ser destruída logo após o onStop (tela cheia de outro
// compromisso abre com CLEAR_TASK), e o viewModelScope cancelaria a liberação
escopoLiberacao.launch {
    try {
        val c = dao.getCompromissoComEtapasById(id) ?: return@launch
        val plano = Reagendador.planoDe(c, System.currentTimeMillis(), reinicio = true)
        if (deveTocarSaidaAoLiberar(houveRetencao, plano)) {
            CompromissoAlarmScheduler.dispararSaidaAgora(applicationContext, c.compromisso, c.itensLembrete.map { it.nomeItem }, semRetencao = true)
        }
    } catch (e: CancellationException) { throw e } catch (e: Exception) { Log.e("CompromissoVM", "Erro ao liberar lembretes", e) }
}
```

Com `private val escopoLiberacao = CoroutineScope(SupervisorJob() + Dispatchers.IO)` top-level privado no arquivo, no padrão de `services/NotificationActionReceiver.kt:47`. Usa `applicationContext` e `dao`, que não dependem da Activity.

#### 5. MainActivity
**Arquivo**: `MainActivity.kt`

- Remover `var showAddCompromissoScreen by remember` (`:254`); usar `val showAddCompromissoScreen by compromissoViewModel.formularioAberto.collectAsStateWithLifecycle()`.
- `onNavigateToAdd` (`:402-405`) → `compromissoViewModel.abrirCriacao()`; `onNavigateToEdit` (`:406-409`) → `compromissoViewModel.abrirEdicao(compromisso)`.
- `BackHandler` (`:267-278`) e `onBackClick` (`:346-349`) → `compromissoViewModel.fecharSemSalvar()`.
- `onSaveClick` (`:341-345`) → `compromissoViewModel.salvarCompromisso(context, apiKey) {}` (o fechamento passa para o VM, item 4).
- Observador do ciclo de vida (`:113-116`): manter `AppForegroundState` e acrescentar, em `onStart`, `compromissoViewModel.formularioVoltouATela()` e, em `onStop`, `if (!isChangingConfigurations) compromissoViewModel.formularioSaiuDaTela()`.

### Critérios de sucesso

#### Verificação automatizada
- [x] `./gradlew testDebugUnitTest assembleDebug` verde ✅ 55/55
- [x] `grep -rn "\"EXTRA_SEM_RETENCAO\"" app/src/main/java` aparece só na constante ✅

#### Verificação manual (emulador ou aparelho, tipo PADRAO salvo indicação; destino a ~1 km; anotar a saída S exibida)
- [ ] **A1 — CA-1.1, CA-1.2:** compromisso sem etapas, evento em +15 min. Em S−3 abrir a edição e manter a tela ligada. Em S nada aparece. Em S+2 voltar sem salvar → "HORA DE SAIR" em até 5 s; em S+10 "Você está atrasado!".
- [ ] **A2 — CA-1.6:** repetir A1, mas em S+1 tocar Salvar sem mudar nada → "Impossível! Atraso de 1 min."; voltar → "HORA DE SAIR" em até 5 s.
- [ ] **A3 — CA-1.12:** repetir A1 ficando no formulário até S+12 → ao voltar toca "HORA DE SAIR" e não "Você está atrasado!" (`adb shell dumpsys alarm | grep -A3 agoravai` sem alarme de atraso pendente).
- [ ] **A4 — CA-1.7:** A1 com tipo ALARME → em S a tela cheia não abre; ao voltar em S+2 ela abre em até 5 s.
- [ ] **A5 — CA-1.9, CA-1.10:** A1; em S+2 `adb shell input keyevent KEYCODE_SLEEP` → "HORA DE SAIR" em até 5 s. `KEYCODE_WAKEUP`, desbloquear e voltar ao formulário até S+11 → atraso não aparece; voltar em S+12 → "HORA DE SAIR" de novo, sem atraso.
- [ ] **A6 — CA-1.15:** A1; em S+1 girar a tela (`adb shell settings put system accelerometer_rotation 0` e `user_rotation 1`) → o formulário continua aberto e nada toca; `adb reboot` com o formulário aberto em S+2 → após o boot, "HORA DE SAIR"; atraso em S+10.
- [ ] **B1 — CA-1.4:** compromisso com etapa de 5 min (E = início da etapa). Abrir a edição em E−3, ficar até S+3, voltar → só "HORA DE SAIR" (a etapa não aparece em nenhum momento).
- [ ] **C1 — CA-1.5:** dois compromissos A e B com saídas próximas; editando A no horário da saída de B → "HORA DE SAIR" de B aparece. Repetir criando um compromisso novo. Repetir com B do tipo ALARME e o formulário de A aberto depois de S(A): a tela cheia de B abre e o "HORA DE SAIR" retido de A também aparece em até 5 s (a liberação sobrevive à destruição da `MainActivity`).
- [ ] **C1b — RF-006 (snackbar de permissão):** com `SCHEDULE_EXACT_ALARM deny`, tentar salvar → snackbar → "Abrir configurações" → o retido é liberado; restaurar a permissão (`appops ... default`), voltar ao formulário → um lembrete seguinte volta a ser retido.
- [ ] **C2 — CA-1.8:** A1 sem abrir a edição antes; em S o "HORA DE SAIR" aparece; em S+1 abrir a edição, tocar "Já Saí" na notificação, voltar em S+11 → nada toca.
- [ ] **C3 — CA-1.11:** emulador, A1 com destino conhecido; com o formulário aberto em S+3, `adb emu geo fix <lon> <lat>` do destino → "Você chegou! 🏁"; voltar → nada toca (nem em S+10).
- [ ] **C4 — CA-1.13:** compromisso com saída a 5 min do evento; formulário aberto de S−5 até evento+2 → ao voltar, nada; em S+10 (depois do evento) "Você está atrasado!".

> ⏸ Após a verificação automatizada, pausar para os roteiros manuais e commit.

---

## Fase 3: Salvar fecha o lembrete antigo

### Objetivo
RF-002: ao salvar, o "HORA DE SAIR" do agendamento antigo que ainda estiver na bandeja fecha. Com a configuração atual da `ReminderActivity` (CLEAR_TASK na mesma task), salvar com a tela cheia aberta é inalcançável (ver Decisões); no tipo ALARME o que resta é a notificação fixa, coberta aqui.

### Mudanças

#### 1. Salvar
**Arquivo**: `ui/compromissos/CompromissoViewModel.kt` (ramo de edição do salvar, `:200-207`)
**Mudança**: após `CompromissoAlarmScheduler.cancelar(...)`, cancelar a notificação `CompromissoAlarmScheduler.idAlarmeSaida(currentState.id)` via `NotificationManager` (como `services/geofence/RegistroChegada.kt:35`), com comentário `// LEMB-12 (RF-002)`.

### Critérios de sucesso

#### Verificação automatizada
- [ ] `./gradlew testDebugUnitTest assembleDebug` verde

#### Verificação manual
- [ ] **C5 — CA-1.14:** tipo PADRAO, deixar o "HORA DE SAIR" aparecer em S sem tocar "Já Saí"; em S+1 abrir a edição, adiar o evento 30 min e salvar → a notificação some e o novo "HORA DE SAIR" é agendado (`dumpsys alarm`).
- [ ] **C6 — CA-1.14 (ALARME):** tipo ALARME; em S a tela cheia abre; fechá-la com o voltar sem confirmar (a notificação fixa continua); abrir o app, editar, adiar e salvar → a notificação fixa some. Se abrir o app trouxer a tela cheia de volta, registrar aqui (caso inalcançável, ver Decisões).
- [ ] **C7 — CA-1.3:** A1 com edição aberta em S−3; em S+2 adiar o evento 15 min e salvar → nada toca no salvamento; o novo "HORA DE SAIR" toca em S+15.

> ⏸ Após a verificação automatizada, pausar para os roteiros manuais e commit.

---

## Fase 4: Compromisso encerrado e religar

### Objetivo
Entregar a História 2.

### Mudanças

#### 1. Texto
**Arquivo**: `notifications/CompromissoAlarmScheduler.kt` (junto de `MENSAGEM_PERMISSAO_ALARME`, `:14`)
**Mudança**: `const val MENSAGEM_COMPROMISSO_PASSADO = "Esse compromisso já passou. Edite a data para ativá-lo de novo."` (comentário `// COMP-10 (RF-008)`).

#### 2. Relógio da lista
**Arquivo**: `ui/compromissos/CompromissoViewModel.kt`
**Mudança**: `val agora: StateFlow<Long>` a partir de `flow { while (true) { emit(System.currentTimeMillis()); delay(30_000) } }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), System.currentTimeMillis())` (comentário `// COMP-10 (RNF-002)`).

#### 3. Lista
**Arquivo**: `ui/compromissos/CompromissosScreen.kt`
**Mudança**: coletar `viewModel.agora` com `collectAsStateWithLifecycle()` na tela e passar ao card. No card (`:104-127`), `val ligado = c.compromisso.isEnabled && !estaEncerrado(c.compromisso.diasRepeticao, c.compromisso.dataHoraEvento, agora)`; usar `ligado` no lugar de `isEnabled` na cor (`:112`), elevação (`:116`), título (`:122`) e `Switch(checked = ligado, ...)` (`:127`).

#### 4. Toggle
**Arquivo**: `ui/compromissos/CompromissoViewModel.kt` (`toggleCompromisso`, `:222-234`)

```kotlin
fun toggleCompromisso(c: CompromissoComEtapas, isEnabled: Boolean) {
    if (isEnabled) {
        val agora = System.currentTimeMillis()
        val encerrado = estaEncerrado(c.compromisso.diasRepeticao, c.compromisso.dataHoraEvento, agora)
        when (decisaoAoLigar(encerrado, CompromissoAlarmScheduler.podeAgendarExato(applicationContext))) {
            // COMP-10 (RF-008): antes da LEMB-10; nada é gravado nem agendado
            DecisaoAoLigar.BLOQUEAR_PASSADO -> { Toast.makeText(applicationContext, MENSAGEM_COMPROMISSO_PASSADO, Toast.LENGTH_LONG).show(); return }
            // LEMB-10: sem alarme exato, o compromisso não é reativado
            DecisaoAoLigar.BLOQUEAR_PERMISSAO -> { Toast.makeText(applicationContext, MENSAGEM_PERMISSAO_ALARME, Toast.LENGTH_LONG).show(); return }
            DecisaoAoLigar.LIGAR -> Unit
        }
    }
    viewModelScope.launch {
        val updated = c.compromisso.copy(isEnabled = isEnabled)
        dao.updateCompromisso(updated)
        if (isEnabled) {
            // COMP-10 (RF-011): regra da 003 — só o "HORA DE SAIR" passado toca; confirmações respeitadas
            val atual = c.copy(compromisso = updated)
            val plano = Reagendador.planoDe(atual, System.currentTimeMillis(), reinicio = true)
            CompromissoAlarmScheduler.reagendar(applicationContext, updated, c.etapas, c.itensLembrete.map { it.nomeItem }, plano)
        } else {
            CompromissoAlarmScheduler.cancelar(applicationContext, updated.id, c.etapas.size)
            CompromissoGeofenceManager.removeGeofence(updated.id)
        }
    }
}
```

A saída perdida sai como alarme exato em `agora` (o toggle já exige permissão), que o AlarmManager entrega na hora. A ativação da chegada vai para `max(inicioPreparo, agora)`.

### Critérios de sucesso

#### Verificação automatizada
- [ ] `./gradlew testDebugUnitTest assembleDebug` verde
- [ ] `grep -rn "Esse compromisso já passou" app/src/main/java` aparece só na constante

#### Verificação manual
- [ ] **D1 — CA-2.1:** compromisso sem repetição com evento em +3 min; abrir a lista às E−1 e esperar → até E+1 min o switch fica desligado e o card esmaecido. Fechar e reabrir o app em E+2 → continua assim.
- [ ] **D2 — CA-2.2, CA-2.7:** compromisso com saída a 5 min do evento, sem confirmar a saída; em E+2 ligar o switch → mensagem do RF-008, switch desligado, `dumpsys alarm` mantém o atraso de S+10; `adb reboot` em E+2 → o atraso toca em S+10.
- [ ] **D3 — CA-2.8:** emulador; mesmo compromisso, em E+30 `adb emu geo fix` no destino → "Você chegou! 🏁".
- [ ] **D4 — CA-2.10:** encerrado; `adb shell appops set com.brunocsantos.agoravai SCHEDULE_EXACT_ALARM deny`; ligar → mensagem do RF-008, não a de permissão. Restaurar com `default`.
- [ ] **D5 — CA-2.3, CA-2.6:** editar um encerrado para amanhã e salvar → switch ligado, alarmes agendados (`dumpsys alarm`). Repetir com um compromisso desligado à mão com evento amanhã.
- [ ] **D6 — CA-2.4:** compromisso com dias de repetição e evento passado → switch continua ligado; desligar e religar não é bloqueado.
- [ ] **D7 — CA-2.5:** encerrado com "Excluir após concluir" → fechar e reabrir o app → apagado.
- [ ] **D8 — CA-2.9:** compromisso com etapa (E) e saída (S); desligar antes de E; religar em S+5 → só "HORA DE SAIR" em até 5 s; etapa não aparece; atraso em S+10 (`dumpsys alarm`).
- [ ] **D9 — CA-2.11:** mesmo compromisso; desligar antes de E; religar em E+5 (antes de S) → nada toca na hora; "HORA DE SAIR" em S.

> ⏸ Após a verificação automatizada, pausar para os roteiros manuais e commit.

---

## Fase 5: Capacidades e arquitetura

### Objetivo
Constituição §8 / P1 / P6: os documentos vivos refletem o novo comportamento.

### Mudanças

#### 1. Capacidades
**Arquivo**: `docs/sdd/capacidades/lembretes.md`
- Nova **LEMB-12 · Lembretes durante a edição**, com RF-001 a RF-006 em EARS e critérios `.a`–`.o` (CA-1.1 a CA-1.15), status 🧪 até os roteiros passarem.
- LEMB-11: nota de que compromisso encerrado mantém atraso futuro e chegada no reagendamento (CA-2.7).
- LEMB-09: chegada durante a edição cancela os retidos (RF-005).

**Arquivo**: `docs/sdd/capacidades/compromissos.md`
- COMP-09: retenção durante a edição (remete a LEMB-12) e salvar com evento futuro deixa ativo (RF-010, `.a` = CA-2.6).
- COMP-10: remover a observação sobre religar passado (`:87`); RF-007, RF-008, RF-009, RF-011 com critérios `.b`–`.j`.
- COMP-14: critério `.a` (CA-2.5).

#### 2. Arquitetura
**Arquivo**: `docs/sdd/arquitetura.md`
- §5.2: `EXTRA_COMPROMISSO_ID` também no alarme de etapa; novo `EXTRA_SEM_RETENCAO` (saída liberada após a edição).
- Seção de componentes: `EdicaoEmAndamento` (estado em memória), "encerrado" derivado (`estaEncerrado`), relógio de 30 s da lista; nota de que a `ReminderActivity` abre com CLEAR_TASK na task do app.

#### 3. Spec e roadmap
- `spec.md`: `status: implementada`.
- `roadmap.md`: 005 implementada; próximo `/validar 005`.

### Critérios de sucesso

#### Verificação automatizada
- [ ] `./gradlew testDebugUnitTest assembleDebug` verde

#### Verificação manual
- [ ] Revisão dos diffs de `capacidades/` e `arquitetura.md` pelo Bruno

> ⏸ Commit da fase.

---

## Estratégia de testes

Unitários em `app/src/test/java/com/brunocsantos/agoravai/notifications/`. Manuais nos roteiros das fases (A1–A6, B1, C1–C7, D1–D9).

| Critério (spec) | Tipo | Teste | Fase |
|---|---|---|---|
| CA-1.1 | unitário + manual | `EdicaoLembretesTest`: `deveReter(id, id, true)` = true · A1 | 1, 2 |
| CA-1.2 | unitário + manual | plano(reinicio) às S+2: `saidaPerdida`, atraso S+10; `deveTocarSaidaAoLiberar(true, plano)` · A1 | 1, 2 |
| CA-1.3 | manual | C7 | 3 |
| CA-1.4 | unitário + manual | plano às S+5 com etapa passada: `etapas` vazio, `saidaPerdida` · B1 | 1, 2 |
| CA-1.5 | unitário + manual | `deveReter(outroId, id, true)` e `deveReter(id, null, true)` = false · C1 | 1, 2 |
| CA-1.6 | unitário + manual | mesmo plano do CA-1.2 · A2 | 1, 2 |
| CA-1.7 | manual | A4 | 2 |
| CA-1.8 | unitário + manual | plano com `confirmouSaida`: sem saída/atraso; `deveTocarSaidaAoLiberar` = false · C2 | 1, 2 |
| CA-1.9 | unitário + manual | `deveReter(id, id, false)` = false · A5 | 1, 2 |
| CA-1.10 | unitário + manual | plano às S+12: `atraso` null, `saidaPerdida` · A5 | 1, 2 |
| CA-1.11 | unitário + manual | plano com `confirmouChegada` vazio; `deveTocarSaidaAoLiberar` = false · C3 | 1, 2 |
| CA-1.12 | unitário + manual | plano às S+12 (igual 1.10) · A3 | 1, 2 |
| CA-1.13 | unitário + manual | evento 10:00, saída 9:55, agora 10:02: `saida` null, `atraso` 10:05; `deveTocarSaidaAoLiberar(true, plano)` = false · C4 | 1, 2 |
| CA-1.14 | manual | C5, C6 | 3 |
| CA-1.15 | manual | A6 | 2 |
| CA-2.1 | unitário + manual | `estaEncerrado`: evento−1 ms false, evento true, `""` e `" "` sem repetição · D1 | 1, 4 |
| CA-2.2 | unitário + manual | `decisaoAoLigar(true, true)` = BLOQUEAR_PASSADO · D2 | 1, 4 |
| CA-2.3 | unitário + manual | `estaEncerrado(amanhã)` false · D5 | 1, 4 |
| CA-2.4 | unitário + manual | `estaEncerrado(passado, "2,4")` false; `decisaoAoLigar(false, true)` = LIGAR · D6 | 1, 4 |
| CA-2.5 | manual | D7 | 4 |
| CA-2.6 | manual | D5 | 4 |
| CA-2.7 | unitário + manual | plano às 10:02 com `ativo = true`, evento 10:00: atraso 10:05, chegada, sem saída perdida · D2 | 1, 4 |
| CA-2.8 | unitário + manual | plano às 10:30: `ativacaoChegada` ≠ null; às 12:00: null · D3 | 1, 4 |
| CA-2.9 | unitário + manual | plano(reinicio) às 9:20 com etapa 9:00, saída 9:15: só saída perdida, atraso 9:25 · D8 | 1, 4 |
| CA-2.10 | unitário + manual | `decisaoAoLigar(true, false)` = BLOQUEAR_PASSADO · D4 | 1, 4 |
| CA-2.11 | unitário + manual | plano às 9:05: `etapas` vazio, `saida` 9:15, sem saída perdida · D9 | 1, 4 |
| RNF-001 | manual | cronometrar em A1, A2, A4, A5, D8 | 2, 4 |
| RNF-002 | manual | D1 | 4 |

Casos de borda (unitários): `agora == saida` conta como saída perdida; `decisaoAoLigar(false, false)` = BLOQUEAR_PERMISSAO (regressão LEMB-10); `deveTocarSaidaAoLiberar(false, planoComSaidaPerdida)` = false (liberar sem retenção não toca).

## Rastreabilidade

| Requisito | Fase(s) |
|---|---|
| RF-001 | 1, 2 |
| RF-002 | 2 (descarta retidos), 3 (fecha a notificação antiga) |
| RF-003 | 1, 2 |
| RF-004 | 1, 2 |
| RF-005 | 2 (sem mudança na chegada; plano vazio após chegada) |
| RF-006 | 1, 2 |
| RF-007 | 1, 4 |
| RF-008 | 1, 4 |
| RF-009 | 1, 4 |
| RF-010 | 4 (verificação; comportamento existente em `CompromissoViewModel.kt:180-196`) |
| RF-011 | 1, 4 |
| RNF-001 | 2, 4 |
| RNF-002 | 4 |
| Delta §14 | 5 |

## Riscos e mitigação

| Risco | Impacto | Mitigação |
|---|---|---|
| Crash do app em primeiro plano com um lembrete já retido | O retido se perde; os seguintes tocam | Janela pequena; aceito (decisão D1). Os futuros nunca são cancelados. |
| Processo morto logo depois do `onStop`, antes da leitura do banco na liberação | "HORA DE SAIR" retido não sai | Escopo de processo (não depende do VM); janela de milissegundos; aceito. |
| `onStop` não disparar com tela apagada em algum aparelho | Retido até voltar ao formulário | Roteiro A5 confirma; `onStop` é o contrato do Android para tela apagada. |
| Tela cheia de outro compromisso (B) abre com o formulário de A na tela | `onStop` libera A e a `MainActivity` é destruída (CLEAR_TASK); a edição de A é perdida | Coerente com RF-006; a liberação roda fora do VM (Decisões) e `onCleared` limpa o estado. Roteiro C1. |
| Snackbar de permissão abre Configurações | `onStop` libera | Coerente com RF-006. |
| Autocomplete de endereço (Places) em tela cheia por cima do formulário | `onStop` libera: o "HORA DE SAIR" retido pode tocar enquanto a pessoa escolhe o endereço | Coerente com RF-006 (o formulário saiu da tela); conferir no roteiro A1 variando com o autocomplete. |
| Alarme de etapa antigo (agendado antes da atualização) sem `EXTRA_COMPROMISSO_ID` | Etapa antiga não é retida | `MY_PACKAGE_REPLACED` → Reagendador regrava os alarmes com o extra novo (`BootReceiver.kt:19-23`). |
| Compromissos encerrados ficam `isEnabled = true` para sempre | Reagendador gera plano vazio para eles; custo desprezível | Aceito (D2). |

## Migração e rollback

Sem migração de dados. Cada fase é um commit; rollback = reverter o commit da fase. O extra novo nos alarmes é ignorado por versões anteriores.

## Referências

- Spec: `docs/sdd/specs/005-lembretes-na-edicao-e-fim-do-evento/spec.md`
- Pesquisa: `docs/sdd/specs/005-lembretes-na-edicao-e-fim-do-evento/pesquisa.md`
- Padrões seguidos: `notifications/HorariosLembrete.kt:63-119` (regra pura), `notifications/Reagendador.kt:45-109` (efeito), `services/AppForegroundState.kt:9-16` (estado), `services/NotificationActionReceiver.kt:47` (escopo de processo), `app/src/test/java/com/brunocsantos/agoravai/notifications/ReagendamentoTest.kt:13-37,86-104` (testes)

## Registro da implementação

<!-- Preenchido por /implementar -->

- **Fase 1 (2026-10-30):** regras puras em `notifications/HorariosLembrete.kt` (`deveReter`, `deveTocarSaidaAoLiberar`, `estaEncerrado`, `DecisaoAoLigar`/`decisaoAoLigar`); `Reagendador.planoDe` público. Testes `EdicaoLembretesTest` (13) e `EncerramentoTest` (12); `testDebugUnitTest assembleDebug` → 55/55.
- **Fase 2 (2026-10-30):** `services/EdicaoEmAndamento.kt`; `EXTRA_SEM_RETENCAO`; etapa leva `EXTRA_COMPROMISSO_ID`; `AlarmReceiver` e `CHECK_LATE` consultam a retenção; formulário no VM (`formularioAberto`, `abrirCriacao`, `abrirEdicao`, `fecharSemSalvar`, `formularioSaiuDaTela`, `formularioVoltouATela`, `onCleared`); liberação em `escopoLiberacao`; `MainActivity` usa o estado do VM e chama o VM no `onStart`/`onStop`. Desvio: contador `sessaoFormulario` em vez de cancelar o `Job` (ver Decisões). Revisão de código: a finalização do salvar e as mensagens de erro também conferem a sessão (um salvamento antigo não fecha nem mostra erro no formulário seguinte). 55/55.
