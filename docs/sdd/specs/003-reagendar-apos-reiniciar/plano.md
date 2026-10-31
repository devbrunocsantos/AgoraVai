---
spec: docs/sdd/specs/003-reagendar-apos-reiniciar/spec.md
pesquisa: docs/sdd/specs/003-reagendar-apos-reiniciar/pesquisa.md
data: 2026-10-29
commit_base: 0016aa3
status: concluido  # rascunho | aprovado | em-implementacao | concluido
---

# Plano de implementação: Reagendar após reiniciar ou atualizar

Caminhos de código relativos a `app/src/main/java/com/brunocsantos/agoravai/`; testes em `app/src/test/java/com/brunocsantos/agoravai/`.

## Visão geral

Um `BootReceiver` (`BOOT_COMPLETED` e `MY_PACKAGE_REPLACED`) chama um `object Reagendador`
que, para cada compromisso ativo, aplica uma **regra pura** (`planoDeReagendamento`) que
decide quais lembretes ainda devem existir — descartando etapas e atrasos passados e,
só no reinício, disparando na hora a saída perdida antes do evento — e os agenda por um
novo `CompromissoAlarmScheduler.reagendar`. Quando há saída perdida com destino
localizado, uma leitura única da localização decide se a chegada tem precedência
(reutilizando a lógica do receiver de geofence). Sem alarme exato, o usuário recebe
"Seus lembretes estão pausados". As geofences das automações ativas são re-registradas.

## Estado atual

Ver `pesquisa.md`. Em resumo:
- `AndroidManifest.xml:5-21,49-59` — sem `RECEIVE_BOOT_COMPLETED` e sem receiver com `intent-filter`.
- `notifications/CompromissoAlarmScheduler.kt:37-66` — `agendar` agenda tudo, inclusive horários passados (disparam na hora); `agendarExato` sem permissão só loga (`:147-155`).
- `notifications/AlarmReceiver.kt:30-49` — mostra a saída sem consultar o banco.
- `services/geofence/GeofenceBroadcastReceiver.kt:55-108` — a lógica de "registrar a chegada" está inline no handler do ENTER.
- `data/AppDao.kt:31-33` — única listagem de compromissos é `Flow`; `getActiveAutomationSettings()` existe sem uso (`:85`).
- `MainActivity.kt:120` — único ponto que cria o canal de notificação.

## Estado desejado

Após `adb reboot` ou `adb install -r`, sem abrir o app: `dumpsys alarm` lista só os
alarmes futuros dos compromissos ativos; a saída perdida antes do evento toca em até
1 min após o desbloqueio, a menos que o usuário esteja no destino (aí "Você chegou!");
automações por local voltam a disparar; sem alarme exato, aparece o aviso. Verificado
por `ReagendamentoTest` e pelos roteiros M1–M10.

## Fora de escopo

- Mudar o agendamento de **salvar** e **religar** (`CompromissoViewModel.kt:143-236`): continua usando `agendar` sem filtro (religar no meio do período mantém o comportamento atual).
- `AlarmReceiver` consultar o banco antes de mostrar a saída.
- Aviso quando a localização em segundo plano ou as notificações estiverem negadas (spec §3).
- Repetição semanal (COMP-13 / DT-04) — inclusive a lógica `proximaOcorrencia` do stash `stash@{0}`, que não será aplicado.
- Gatilho de horário das automações (WorkManager já sobrevive ao reboot).

## Decisões de design

| Decisão | Alternativas consideradas | Motivo da escolha |
|---|---|---|
| Regra pura `planoDeReagendamento` + `CompromissoAlarmScheduler.reagendar`, usado só pelo `Reagendador` | Filtrar passados sempre em `agendar` | Filtrar sempre mudaria salvar/religar sem spec (P1); a regra pura é testável (P7) |
| Saída perdida só quando `reinicio = true` (`BOOT_COMPLETED`); na atualização, não | Tratar os dois iguais | Na atualização o aparelho não esteve desligado: a saída já tocou no horário; repeti-la seria ruído (RF-002a fala em "aparelho desligado") |
| Saída perdida não confirmada toca de novo no reinício, mesmo que já tenha tocado antes de desligar | Gravar que a saída já foi exibida | Decisão do Bruno (2026-10-29): conservador (P2), sem estado novo |
| `reagendar` chama `cancelar` antes de agendar o plano | Só agendar | Na atualização, alarmes antigos sobrevivem; cancelar evita sobras (ex.: etapa removida) e `FLAG_UPDATE_CURRENT` troca os extras (CA-2.1). Mesmos request codes (RNF-001) |
| Precedência da chegada: **uma** leitura de localização (`getCurrentLocation`, até 4 s) quando algum compromisso tem saída perdida com destino localizado no período; dentro → registra a chegada; fora → saída na hora; desconhecido → saída em agora + 45 s e ativação da chegada na hora | (A) só atrasar a saída 45 s; (D) `AlarmReceiver` consultar `confirmouChegada` | Decisão do Bruno (2026-10-29). Determinística na maioria dos casos; sem dependência nova (`play-services-location:21.3.0` e `kotlinx-coroutines-play-services:1.7.3` já em `app/build.gradle.kts:101-102`) |
| Extrair o registro da chegada para `RegistroChegada.registrar` (reutilizado pelo handler do ENTER e pelo `Reagendador`) | Duplicar o código | Uma fonte da regra de chegada (P9) |
| Raio da chegada como constante única `RAIO_CHEGADA_METROS = 200f` em `HorariosLembrete.kt`, usada pelo manager e pela classificação | Literal em dois lugares | P6 |
| Canal de notificação criado em `AgoraVaiApplication.onCreate` (sai da `MainActivity`) | Duplicar | O aviso sai de um receiver, possivelmente antes de o app ser aberto; a criação é idempotente |
| ID da notificação "lembretes pausados" `ID_NOTIFICACAO_LEMBRETES_PAUSADOS = Int.MAX_VALUE - 1` em `CompromissoAlarmScheduler.kt`, junto dos IDs de `:25-29`, documentado em §5.2 | Derivar de compromisso; constante no `Reagendador` | Notificação global, fora da faixa `id*100+x`; IDs ficam num lugar só (P6) |
| Sem alarme exato: aviso RF-004; a detecção de chegada é ativada **direto** (`addGeofenceChegada`, sem AlarmManager); a saída perdida (FORA/DESCONHECIDA) é entregue **na hora** por `sendBroadcast` ao `AlarmReceiver`; etapas, saída futura e atraso não são agendados | Não agendar nada (o aviso substitui tudo) | RF-003 não depende de alarme; a saída perdida é imediata e não precisa de alarme (P2). Limite: ativação direta antes do período não detecta quem já está no raio quando o período começa (o handler ignora entradas fora do período) |
| `MENSAGEM_PERMISSAO_ALARME` sai de `ui/compromissos/CompromissoViewModel.kt:45` e vai para `notifications/CompromissoAlarmScheduler.kt` (top-level) | Duplicar o texto; `notifications/` depender de `ui/` | Hoje `notifications/` não depende de `ui/`; texto único (P6) |
| RNF-002 medido com ~10 compromissos e extrapolado | Semente de debug com 50 | Decisão do Bruno (2026-10-29): sem código só de teste (P8) |

## Conformidade com a constituição

- [ ] P1 A spec vem primeiro — spec `pesquisada`; este plano aguarda aprovação
- [x] P2 Lembrete não falha em silêncio — foco da mudança: reagendamento sem abrir o app, saída perdida avisada, aviso sem permissão
- [x] P3 Dados preservados — só leitura + uma consulta nova, sem schema
- [x] P4 Local-first — a localização é lida e usada só no aparelho
- [x] P5 Desenho para TDAH — aviso curto que abre direto a permissão; nada de "HORA DE SAIR" para quem já chegou
- [x] P6 Contratos explícitos — mesmos request codes de §5.2; ID novo de notificação e raio como constantes documentadas
- [x] P7 Critérios verificáveis — regra pura e classificação com testes; roteiros M1–M10 para o resto
- [x] P8 Simplicidade — receiver padrão do Android; sem dependências novas
- [x] P9 Consistência — `object` em `notifications/`, padrão `goAsync` + IO, nomes em português

---

## Fase 1: Regras puras

### Objetivo
Toda a decisão "o que reagendar e quando" e "dentro/fora/desconhecido" em código puro, com testes, antes de tocar no Android.

### Mudanças

#### 1. Raio da chegada
**Arquivo**: `notifications/HorariosLembrete.kt`
**Mudança**: `const val RAIO_CHEGADA_METROS = 200f` (comentário citando LEMB-09).
**Arquivo**: `services/geofence/CompromissoGeofenceManager.kt:20`
**Mudança**: o raio passa a usar `RAIO_CHEGADA_METROS` (mesmo valor; sem mudança de comportamento).

#### 2. Plano de reagendamento
**Arquivo**: `notifications/HorariosLembrete.kt`
**Mudança**: tipos e função novos, no estilo das funções existentes (`Long` epoch ms, `agora` como parâmetro):

```kotlin
// LEMB-11: o que reagendar depois de reiniciar ou atualizar
data class Reagendamento(
    val etapas: List<Pair<Int, Long>>, // (índice na ordem do usuário, horário) — só futuras
    val saida: Long?,                  // horário futuro, ou `agora` se for saída perdida
    val saidaPerdida: Boolean,
    val atraso: Long?,
    val ativacaoChegada: Long?
) { val vazio get() = etapas.isEmpty() && saida == null && atraso == null && ativacaoChegada == null }

fun planoDeReagendamento(
    horarios: HorariosLembrete, dataHoraEvento: Long, agora: Long,
    ativo: Boolean, confirmouSaida: Boolean, confirmouChegada: Boolean,
    temCoordenadas: Boolean, reinicio: Boolean
): Reagendamento
```

Regras (futuro = `> agora`):
- `!ativo || confirmouChegada` → tudo vazio (CA-1.3, CA-1.7).
- `confirmouSaida` → sem etapas, saída e atraso; a ativação da chegada segue a regra abaixo.
- Etapas: só `inicioEtapas[i] > agora` (CA-1.2, RF-002).
- Saída: `saida > agora` → `saida`; senão, `reinicio && dataHoraEvento > agora` → `agora` e `saidaPerdida = true` (RF-002a, CA-1.4); senão `null` (CA-1.5).
- Atraso: `verificacaoAtraso > agora` → mantém, mesmo depois do evento (RF-002); senão `null`.
- Ativação: `temCoordenadas && agora < fimDaJanelaDeChegada(evento)` → `max(inicioPreparo, agora)` (RF-003, CA-1.6); senão `null`.

#### 3. Classificação da posição
**Arquivo**: `notifications/HorariosLembrete.kt`
**Mudança**:

```kotlin
enum class PosicaoChegada { DENTRO, FORA, DESCONHECIDA }

// LEMB-11.i: sem leitura, ou leitura imprecisa demais, não dá para decidir
fun classificarPosicao(distanciaMetros: Float?, precisaoMetros: Float?): PosicaoChegada
```
`null` em qualquer um, ou `precisaoMetros > RAIO_CHEGADA_METROS` → `DESCONHECIDA`; `distanciaMetros <= RAIO_CHEGADA_METROS` → `DENTRO`; senão `FORA`.

#### 4. Testes
**Arquivo**: `notifications/ReagendamentoTest.kt` (novo), seguindo `JanelaChegadaTest.kt:10-30` (helper `em(dia, h, m)` em UTC). Os `HorariosLembrete` são montados **com os números literais de cada CA** (o tipo é um `data class`, não precisa passar por `calcularHorarios`): base evento dia 27 às 10:00, saída 9:15, atraso 9:25; CA-1.2 com etapas às 8:00 e 8:20; CA-1.6 com preparo às 8:30; o caso "atraso depois do evento" com saída 10:00 − 5 min (deslocamento 5, **segurança 0**) → atraso 10:05.

### Critérios de sucesso

#### Verificação automatizada
- [x] `./gradlew testDebugUnitTest assembleDebug` ✅ (30/30)
- [x] `ReagendamentoTest` com os casos (20/20):
  - `LEMB-11a reinicio na vespera reagenda etapas saida atraso e ativacao` (CA-1.1)
  - `LEMB-11b etapas passadas descartadas saida e atraso mantidos` (CA-1.2)
  - `LEMB-11b etapa exatamente em agora e descartada`
  - `LEMB-11b atraso exatamente em agora e descartado`
  - `LEMB-11c compromisso desativado nada e agendado` (CA-1.3)
  - `LEMB-11f saida perdida antes do evento dispara agora e atraso segue em 925` (CA-1.4)
  - `LEMB-11f saida exatamente em agora dispara imediata`
  - `LEMB-11f sem etapas saida perdida dispara saida e ativacao em agora`
  - `LEMB-11f saida confirmada nao agenda etapas saida nem atraso`
  - `LEMB-11f na atualizacao saida passada nao dispara` (`reinicio = false`)
  - `LEMB-11g evento ja passou nao dispara etapas saida nem atraso` (CA-1.5; afirma também `ativacaoChegada == agora`, ou seja, o plano não fica vazio)
  - `LEMB-11g evento exatamente agora nao dispara saida perdida`
  - `LEMB-11h reinicio antes do preparo agenda ativacao no inicio do preparo` (CA-1.6)
  - `LEMB-11i chegada confirmada nada e agendado` (CA-1.7, parte da regra)
  - `LEMB-11 sem coordenadas nao agenda ativacao mas agenda saida`
  - `LEMB-11 apos fim do periodo de chegada nao ativa chegada` (12:01) e `exatamente no fim nao ativa` (12:00)
  - `LEMB-11 atraso depois do evento ainda futuro e mantido` (trajeto 5 min, atraso 10:05, agora 10:01)
  - `RNF-001 regra e deterministica para a mesma entrada`
  - `LEMB-11i classificar posicao` — dentro (150 m, ±20), fora (500 m, ±20), desconhecida (sem leitura; ±300)
- [x] `HorariosLembreteTest` e `JanelaChegadaTest` continuam verdes (o raio não mudou de valor)

> ⏸ Após a verificação automatizada desta fase, pausar para confirmação antes de seguir.

---

## Fase 2: Reagendar ao reiniciar ou atualizar

### Objetivo
Entregar o núcleo de P1: lembretes futuros (e a saída perdida, ainda sem precedência da chegada) voltam sem abrir o app.

### Mudanças

#### 1. Consulta
**Arquivo**: `data/AppDao.kt` (junto de `getCompromissoComEtapasById`, `:28-29`)
**Mudança**:
```kotlin
@Transaction
@Query("SELECT * FROM compromissos WHERE isEnabled = 1")
suspend fun getCompromissosAtivosComEtapas(): List<CompromissoComEtapas>
```
Sem mudança de schema nem de versão do banco.

#### 2. Agendador
**Arquivo**: `notifications/CompromissoAlarmScheduler.kt`
**Mudança**:
- Extrair o corpo do laço de etapas de `agendar` (`:47-55`) para `private fun agendarEtapa(context, compromissoId, etapas, indice, horario)` e usá-lo em `agendar` (mesmo request code `idAlarmeEtapa(id, etapas.size - 1 - indice)` e mesmos textos).
- Nova `fun reagendar(context, compromisso, etapas, itens, plano: Reagendamento)`: chama `cancelar(context, compromisso.id, etapas.size)`; depois agenda cada item não nulo do plano com os helpers existentes (`agendarEtapa`, `agendarSaida`, `agendarExato` + `pendingVerificacaoAtraso`, `agendarExato` + `pendingAtivacaoChegada`).
- Comentário no topo do objeto: `agendar` (salvar/religar) não filtra o passado; `reagendar` (boot/atualização) segue o plano (LEMB-11).

#### 3. Reagendador
**Arquivo**: `notifications/Reagendador.kt` (novo)
**Mudança**: `object Reagendador` com `suspend fun reagendarTudo(context: Context, reinicio: Boolean)`:
1. `Log.d("AgoraVai-Boot", "inicio reinicio=$reinicio")` e marca `System.currentTimeMillis()`.
2. `val dao = AppDatabase.getDatabase(context).appDao()`; `val agora = System.currentTimeMillis()`.
3. Para cada `CompromissoComEtapas` de `getCompromissosAtivosComEtapas()`: `calcularHorarios(...)`; `planoDeReagendamento(..., temCoordenadas = lat != 0.0 && lng != 0.0, reinicio)`; **sempre** `CompromissoAlarmScheduler.reagendar(context, c.compromisso, c.etapas, c.itensLembrete.map { it.nomeItem }, plano)` (padrão de chamada de `CompromissoViewModel.kt:233`) — com plano vazio, `reagendar` só cancela sobras (RNF-001).
4. `Log.d("AgoraVai-Boot", "fim n=<qtde> em <ms>ms")` (RNF-002).

#### 4. Receiver
**Arquivo**: `notifications/BootReceiver.kt` (novo), seguindo `NotificationActionReceiver.kt:60-75`:
```kotlin
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val reinicio = when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED -> true
            Intent.ACTION_MY_PACKAGE_REPLACED -> false
            else -> return
        }
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try { Reagendador.reagendarTudo(context, reinicio) }
            catch (e: Exception) { Log.e("AgoraVai-Boot", "Falha ao reagendar", e) }
            finally { pendingResult.finish() }
        }
    }
}
```
(O `catch` existe para que uma falha não derrube o processo durante o boot; o erro fica no log.)

#### 5. Manifest
**Arquivo**: `AndroidManifest.xml`
**Mudança**: `<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />` junto das demais (`:5-21`) e, junto dos receivers (`:49-59`):
```xml
<receiver android:name=".notifications.BootReceiver" android:exported="true">
    <intent-filter>
        <action android:name="android.intent.action.BOOT_COMPLETED" />
        <action android:name="android.intent.action.MY_PACKAGE_REPLACED" />
    </intent-filter>
</receiver>
```

### Critérios de sucesso

#### Verificação automatizada
- [x] `./gradlew testDebugUnitTest assembleDebug` ✅ (30/30)
- [x] Manifest mesclado (`app/build/intermediates/merged_manifests/debug/.../AndroidManifest.xml`) contém `RECEIVE_BOOT_COMPLETED` e o `BootReceiver` com as duas ações

#### Verificação manual
- [x] M1 — CA-1.1, CA-1.3 ✅ (2026-10-30)
- [x] M2 — CA-1.2, CA-1.4 (longe do destino) ✅ (2026-10-30)
- [x] M3 — CA-1.5 ✅ (2026-10-30)
- [x] M4 — CA-1.6 ✅ (2026-10-30)
- [ ] M6 — CA-2.1
- [ ] M9 — RNF-001
- [ ] M10 — RNF-002

> ⏸ Pausar para verificação manual.

---

## Fase 3: Chegada antes da saída perdida

### Objetivo
CA-1.7: no reinício com saída perdida, quem está no destino recebe "Você chegou!" e não "HORA DE SAIR".

### Mudanças

#### 1. Registro da chegada reutilizável
**Arquivo**: `services/geofence/RegistroChegada.kt` (novo)
**Mudança**: `object RegistroChegada { suspend fun registrar(context: Context, dados: CompromissoComEtapas) }` com o conteúdo hoje em `GeofenceBroadcastReceiver.kt:82-103` (log, `confirmarSaida` + `confirmarChegada`, `CompromissoAlarmScheduler.cancelar`, `cancel(idAlarmeSaida)`, notificação "Você chegou! 🏁" com ID `id*100+999`, `CompromissoGeofenceManager.removeGeofence`). Textos, IDs e ordem idênticos. `registrar` **não** refaz a checagem de período: quem chama garante (o handler checa em `:75-79`; no `Reagendador`, `saidaPerdida` com ativação implica preparo ≤ agora < evento).
**Arquivo**: `services/geofence/GeofenceBroadcastReceiver.kt`
**Mudança**: `handleCompromissoChegada` mantém as checagens (`:63-80`) e passa a chamar `RegistroChegada.registrar(context, dados)`.

#### 2. Leitura da localização
**Arquivo**: `notifications/Reagendador.kt`
**Mudança**: `private suspend fun lerLocalizacao(context: Context): Location?`:
- Permissão: segue só se `FINE && (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q || BACKGROUND)` (com `ContextCompat.checkSelfPermission`); abaixo do Android 10 não existe a permissão de segundo plano. Sem permissão → `null`.
- Leitura com cancelamento real no timeout:
```kotlin
val cts = CancellationTokenSource()
val local = withTimeoutOrNull(TIMEOUT_LOCALIZACAO_MS) {   // 4_000L
    LocationServices.getFusedLocationProviderClient(context)
        .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token).await()
}
if (local == null) cts.cancel()
```
- `try/catch (e: SecurityException)` → `null`.
- Imports: `com.google.android.gms.location.LocationServices`, `com.google.android.gms.location.Priority`, `com.google.android.gms.tasks.CancellationTokenSource`, `kotlinx.coroutines.tasks.await`, `kotlinx.coroutines.withTimeoutOrNull`.

#### 3. Precedência no reagendamento
**Arquivo**: `notifications/Reagendador.kt`
**Mudança**: em `reagendarTudo`, para compromissos cujo plano tem `saidaPerdida && ativacaoChegada != null` (há coordenadas e o período não terminou):
- a localização é lida **uma vez** (lazy) para todos;
- distância: `local.distanceTo(Location("destino").apply { latitude = c.compromisso.latitudeDestino; longitude = c.compromisso.longitudeDestino })`; precisão: `if (local.hasAccuracy()) local.accuracy else null` (sem precisão → `DESCONHECIDA`);
- `classificarPosicao(distancia, precisao)`:
  - `DENTRO` → `RegistroChegada.registrar(context, c)` e **não** reagenda esse compromisso;
  - `FORA` → `reagendar` com o plano como está (saída em `agora`);
  - `DESCONHECIDA` → `reagendar` com `plano.copy(saida = agora + 45_000)` (constante `ATRASO_SAIDA_SEM_LOCALIZACAO_MS = 45_000L` com comentário); a ativação em `agora` registra a geofence e, se o ENTER inicial chegar antes, o registro da chegada cancela a saída pendente.

### Critérios de sucesso

#### Verificação automatizada
- [x] `./gradlew testDebugUnitTest assembleDebug` ✅ (30/30)
- [x] Testes da Fase 1 seguem verdes (`classificarPosicao` cobre a decisão)

#### Verificação manual
- [x] M5 — CA-1.7 (no destino) ✅ (2026-10-30)
- [x] M2 repetido — CA-1.4 continua (fora do destino, a saída toca em ≤ 1 min) ✅ (2026-10-30)
- [x] B5 da spec 002 repetido — chegada pelo ENTER continua funcionando depois da extração do `RegistroChegada` ✅ (2026-10-30, coberto pelo M4)

> ⏸ Pausar para verificação manual.

---

## Fase 4: Aviso de lembretes pausados e automações por local

### Objetivo
RF-004 (CA-3.1, CA-3.2) e RF-005 (CA-4.1, CA-4.2).

### Mudanças

#### 1. Canal no Application
**Arquivo**: `AgoraVaiApplication.kt:13-14`
**Mudança**: chamar `NotificationScheduler.createNotificationChannel(this)` em `onCreate`.
**Arquivo**: `MainActivity.kt:120`
**Mudança**: remover a chamada (passa a ser do `Application`).

#### 2. Aviso
**Arquivo**: `notifications/Reagendador.kt`
**Mudança**:
- Em `CompromissoAlarmScheduler.kt`: `const val ID_NOTIFICACAO_LEMBRETES_PAUSADOS = Int.MAX_VALUE - 1` (junto dos IDs de `:25-29`) e `const val MENSAGEM_PERMISSAO_ALARME` movida de `CompromissoViewModel.kt:45` (mesmo texto). Adicionar `import com.brunocsantos.agoravai.notifications.MENSAGEM_PERMISSAO_ALARME` em `CompromissoViewModel.kt` e em `AddCompromissoScreen.kt` (`:66`, que hoje usa a constante sem import por estar no mesmo pacote).
- Em `CompromissoAlarmScheduler.kt`: extrair de `agendarSaida` (`:83-118`) a montagem do Intent para `private fun intentSaida(context, compromisso, itens): Intent` e criar `fun dispararSaidaAgora(context, compromisso, itens)` = `context.sendBroadcast(intentSaida(...))` (mesmos extras, sem alarme).
- Em `reagendarTudo`, quando `!CompromissoAlarmScheduler.podeAgendarExato(context)`:
  - a precedência da chegada (Fase 3) é aplicada normalmente;
  - para cada compromisso: `ativacaoChegada != null` → `CompromissoGeofenceManager.addGeofenceChegada(c.compromisso, c.etapas)` direto (sem AlarmManager); saída perdida em `FORA`/`DESCONHECIDA` → `dispararSaidaAgora`; etapas, saída futura e atraso **não** são agendados;
  - se, depois da precedência, **algum** plano tiver `etapas`, `saida` futura ou `atraso` não nulos (lembretes futuros que ficaram sem agendar) → `mostrarAvisoLembretesPausados(context)`. Plano só com ativação da chegada (ex.: CA-1.5) ou resolvido como `DENTRO` não gera aviso.
- `mostrarAvisoLembretesPausados`: `NotificationCompat.Builder(context, CHANNEL_ID)` com `setSmallIcon(R.drawable.ic_launcher_foreground)`, título "Seus lembretes estão pausados", texto `MENSAGEM_PERMISSAO_ALARME` (`CompromissoViewModel.kt:45` — mover a constante para `notifications/` se for preciso evitar dependência de `ui/`), `PRIORITY_HIGH`, `setAutoCancel(true)` e `contentIntent = PendingIntent.getActivity(context, ID_NOTIFICACAO_LEMBRETES_PAUSADOS, Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}")).addFlags(FLAG_ACTIVITY_NEW_TASK), FLAG_UPDATE_CURRENT or FLAG_IMMUTABLE)` (padrão de `AddCompromissoScreen.kt:70-71` e `AlarmReceiver.kt:71-86`). Só ocorre com API ≥ 31, pois abaixo disso `podeAgendarExato` é sempre `true`.

#### 3. Automações por local
**Arquivo**: `notifications/Reagendador.kt`
**Mudança**: ao fim de `reagendarTudo`, `dao.getActiveAutomationSettings().forEach { GeofenceManager.addOrUpdateGeofence(it) }` (`AppDao.kt:85`, `GeofenceManager.kt:36-47`; o manager já é inicializado no `Application`). Automações sem local caem no `remove`, sem efeito.

### Critérios de sucesso

#### Verificação automatizada
- [x] `./gradlew testDebugUnitTest assembleDebug` ✅ (30/30)

#### Verificação manual
- [ ] M7 — CA-3.1, CA-3.2 (Android 12/12L) + contraprova sem compromisso futuro + instalação nunca aberta + saída perdida sem permissão toca na hora
- [ ] M8 — CA-4.1, CA-4.2

> ⏸ Pausar para verificação manual.

---

## Fase 5: Atualizar capacidades e arquitetura

### Mudanças
- `docs/sdd/capacidades/lembretes.md`: LEMB-11 com o texto novo (RF-001 a RF-004) e critérios `.a`–`.j`, status 🧪 até os roteiros passarem; LEMB-09 com a frase da reativação durante o período (spec §14).
- `docs/sdd/capacidades/automacoes.md`: AUTO-05 com RF-005 e critérios `.a`, `.b`.
- `docs/sdd/arquitetura.md`: DT-05 🧪 com link para a 003; §3 com `BootReceiver`, `Reagendador`, `RegistroChegada`; §5.2 com `ID_NOTIFICACAO_LEMBRETES_PAUSADOS` e `RAIO_CHEGADA_METROS`; §7 com `RECEIVE_BOOT_COMPLETED`; canal criado no `Application`; histórico 1.8.
- `docs/sdd/roadmap.md`: status da 003.

### Critérios de sucesso
#### Verificação automatizada
- [x] `./gradlew testDebugUnitTest assembleDebug` (regressão final) ✅ (30/30)

---

## Estratégia de testes

| Critério (spec) | Tipo | Teste | Fase |
|---|---|---|---|
| CA-1.1 | unitário + manual | `LEMB-11a…` / M1 | 1, 2 |
| CA-1.2 | unitário + manual | `LEMB-11b…` / M2 | 1, 2 |
| CA-1.3 | unitário + manual | `LEMB-11c…` / M1 | 1, 2 |
| CA-1.4 | unitário + manual | `LEMB-11f…` / M2 | 1, 2, 3 |
| CA-1.5 | unitário + manual | `LEMB-11g…` / M3 | 1, 2 |
| CA-1.6 | unitário + manual | `LEMB-11h…` / M4 | 1, 2 |
| CA-1.7 | unitário (parcial) + manual | `LEMB-11i…`, `classificarPosicao` / M5 | 1, 3 |
| CA-2.1 | manual | M6 | 2 |
| CA-3.1, CA-3.2 | manual | M7 | 4 |
| CA-4.1, CA-4.2 | manual | M8 | 4 |
| RNF-001 | unitário + manual | `RNF-001…` / M9 | 1, 2 |
| RNF-002 | manual | M10 | 2 |

**Roteiros manuais** (horários relativos; desligar com `adb reboot -p` ou fechar o emulador; conferir com `adb shell dumpsys alarm | grep -A3 agoravai`; após religar, **desbloquear e não abrir o app**):
- **M1 (CA-1.1, 1.3):** criar A (ativo, evento em +1 h) e B (desativado). `adb reboot`. Esperado: `dumpsys` lista os alarmes de A (etapas, saída, atraso, ativação) e nenhum de B; a saída de A toca no horário.
- **M2 (CA-1.2, 1.4):** longe do destino, compromisso com etapa em +3 min e saída em +6 min. Desligar em +2 min, religar em +8 min. Esperado: a etapa não toca; "HORA DE SAIR" em ≤ 1 min após o desbloqueio; o atraso toca em +16 min.
- **M3 (CA-1.5):** como M2, religando depois do horário do evento. Esperado: nenhum lembrete toca.
- **M4 (CA-1.6):** preparo em +10 min; `adb reboot` antes do preparo; ir até o destino durante o período. Esperado: "Você chegou! 🏁"; lembretes pendentes não tocam.
- **M5 (CA-1.7):** no destino (como o B5 da 002, sem simular localização), preparo em +3 min e saída em +6 min. Desligar antes do preparo, religar depois da saída, não sair do lugar. Esperado: "Você chegou!" em ≤ 1 min; "HORA DE SAIR" e atraso não aparecem. *Limite conhecido (spec CA-1.7 e §9):* sem leitura de localização e com ENTER inicial mais lento que 45 s, "HORA DE SAIR" pode aparecer e ser fechado quando a chegada for detectada — registrar se ocorrer.
- **M6 (CA-2.1):** instalar o APK atual sem esta mudança, criar compromisso PADRAO com saída em +10 min; `adb install -r app-debug.apk` da versão nova sem abrir o app. Esperado: o lembrete aparece com título, mensagem e "Já Saí".
- **M7 (CA-3.1, 3.2):** emulador **API 31/32** (no API 33+ o `USE_EXACT_ALARM` do manifest concede a permissão e o cenário não é reproduzível); compromisso futuro; `adb shell appops set com.brunocsantos.agoravai SCHEDULE_EXACT_ALARM deny`; `adb reboot` (3.1) e `adb install -r` (3.2). Esperado: "Seus lembretes estão pausados" + texto do RF-004; tocar abre "Alarmes e lembretes" do Agora Vai. Contraprova: sem compromisso futuro, sem aviso. Repetir numa instalação nova nunca aberta (canal no `Application`).
- **M8 (CA-4.1, 4.2):** automação com local num ponto a ~300 m; `adb reboot` (4.1) / `adb install -r` (4.2); caminhar até o raio sem abrir o app. Esperado: "Hora de Focar!".
- **M9 (RNF-001):** `adb install -r` três vezes (um `am broadcast` de `MY_PACKAGE_REPLACED` pelo shell é bloqueado por ser broadcast protegido). Esperado: `dumpsys alarm | grep -c agoravai` igual nas três; cada lembrete toca uma vez; com compromisso no período, uma única "Você chegou!".
- **M10 (RNF-002):** ~10 compromissos ativos; `adb logcat -v time -s AgoraVai-Boot` após `adb reboot` e desbloqueio. Registrar o tempo do "fim" e extrapolar linearmente para 50; meta ≤ 10 s.

## Rastreabilidade

| Requisito | Fase(s) |
|---|---|
| RF-001 | 1, 2 |
| RF-002 | 1, 2 |
| RF-002a | 1, 2, 3 |
| RF-003 | 1, 2, 3 |
| RF-004 | 4 |
| RF-005 | 4 |
| RNF-001 | 1, 2 |
| RNF-002 | 2 (log), 3 (leitura ≤ 4 s) |
| Spec §14 (LEMB-11, LEMB-09, AUTO-05) | 5 |

## Riscos e mitigação

| Risco | Impacto | Mitigação |
|---|---|---|
| Reagendar dispara lembretes antigos | Alto | Regra pura com testes de borda; `reagendar` separado de `agendar` |
| Leitura de localização lenta/nula após o boot | Médio | Timeout de 4 s; fallback de 45 s + geofence; limite registrado no M5 |
| Receiver estoura ~10 s | Médio | Uma leitura de localização no máximo; o resto é local; medição M10 |
| Extração do `RegistroChegada` altera a chegada pelo ENTER | Médio | Código movido sem mudança; B5 repetido na Fase 3 |
| Fabricantes que não entregam o boot sem "início automático" | Médio | Orientação já existe (SIS-05); fora do controle do app |
| Re-registrar geofence com o mesmo requestId duplicar eventos | Baixo | M9 verifica uma única "Você chegou!" |

## Migração e rollback

- Room: uma consulta nova, sem versão nova. Manifest: permissão + receiver.
- Na primeira atualização com esta versão, os lembretes futuros já agendados são reagendados com os extras atuais.
- **Rollback:** reverter os commits das fases (o receiver sai do manifest junto).

## Registro da implementação

- **Código:** 2026-10-29 — fases 1 `ac66a8c`, 2 `430acb1`, 3 `d82636a`, 4 `c19bb79`; correções da revisão de código `fix` seguinte; fase 5 (documentação) no commit de encerramento.
- **Saída das verificações:** `./gradlew testDebugUnitTest assembleDebug` → BUILD SUCCESSFUL; `HorariosLembreteTest` 4/4, `JanelaChegadaTest` 6/6, `ReagendamentoTest` 20/20.
- **Execução:** a pedido do Bruno (opção A), as fases 2–4 foram implementadas em sequência; todos os roteiros M1–M10 (e o B5 repetido) ficam para uma rodada única em aparelho, antes do `/validar 003`.
- **Revisão de código (`revisor-de-codigo`):** sem bloqueantes; contratos (P6) preservados. Corrigido: falha num compromisso ou na leitura de localização interrompia o reagendamento dos demais e das automações (agora isolado por compromisso; `lerLocalizacao` trata qualquer exceção e sempre cancela o token); leitura única do relógio; ordem de imports.
- **Pendências levantadas pela revisão (para decisão do Bruno):**
  - Sem alarme exato, a ativação direta da chegada antes do período não detecta quem já está no destino quando o preparo começa (limite já registrado em "Decisões"). Alternativa: agendar a ativação com alarme **inexato** (`setAndAllowWhileIdle`, que não exige permissão).
  - Incluir `android.intent.action.QUICKBOOT_POWERON` (alguns fabricantes antigos usam em vez de `BOOT_COMPLETED`).
  - O `Reagendador` (precedência e caminho sem permissão) só é verificável em aparelho (M5, M7): sem Robolectric (P8).
- **Testes em aparelho:** 2026-10-30 — M1–M5 aprovados (informado pelo Bruno); o B5 da 002 repetido foi dado como coberto pelo M4 (mesmo caminho do ENTER → `RegistroChegada`), por decisão do Bruno. Pendentes no emulador: M6–M10.
- **Desvios do plano:** nenhum além das correções acima (todas dentro do escopo).

## Referências

- Spec: `docs/sdd/specs/003-reagendar-apos-reiniciar/spec.md`
- Pesquisa: `docs/sdd/specs/003-reagendar-apos-reiniciar/pesquisa.md`
- Padrões: receiver `goAsync` em `services/NotificationActionReceiver.kt:60-75`; regra pura em `notifications/HorariosLembrete.kt:19-54`; testes em `notifications/JanelaChegadaTest.kt:10-57`; intent de permissão em `ui/compromissos/AddCompromissoScreen.kt:70-71`
- Roteiro de referência "no destino": `docs/sdd/specs/002-deteccao-de-chegada/plano.md` (B5)
