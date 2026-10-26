# Plan — Lembrete de saída confiável

- **Data:** 2026-10-26
- **Spec:** [`spec.md`](spec.md) · **Research:** [`research.md`](research.md)
- **Status:** Aprovado em 2026-10-26 por Bruno C. Santos

## 1. Objetivo
Fazer o lembrete de saída cumprir LEMB-02/03/04/07/10 e garantir que desativar
ou excluir cancela todos os alarmes (COMP-10/11). No caminho, tirar a lógica de
alarmes do ViewModel para um agendador reutilizável, que as mudanças de chegada
e de reinicialização também vão usar.

## 2. Matriz de verificação

| Critério | Tipo | Verificação |
|---|---|---|
| LEMB-01.a (regressão) | Unitário | `HorariosLembreteTest` — etapas encadeadas |
| LEMB-08 (horário +10 min) | Unitário | `HorariosLembreteTest` — `verificacaoAtraso` |
| COMP-07.a (regressão do cálculo) | Unitário | `HorariosLembreteTest` — `inicioPreparo` |
| LEMB-02.a / .b | Manual | Roteiro R1 (com e sem itens) |
| LEMB-03.a / .b | Manual | Roteiro R1, tipo PADRAO |
| LEMB-04.a | Manual | Roteiro R2 (tela bloqueada) |
| LEMB-04.b | Manual | Roteiro R3 (revogar a tela cheia no Android 14) |
| LEMB-07.b / .c | Manual | Roteiro R2 |
| LEMB-10.a / .b | Manual | Roteiro R4 (Android 12, revogar alarmes exatos) |
| COMP-10.a, COMP-11.b | Manual | Roteiro R5 + `adb shell dumpsys alarm` |

**Roteiro base:** origem "Av. Paulista, 1000, São Paulo", destino "Av. Paulista, 1500, São Paulo", segurança 1 min, etapa "Teste" de 1 min e evento daqui a ~8 min. A Directions vai devolver poucos minutos de trajeto.
- **R1:** tipo PADRAO, com itens. Conferir título, mensagem e itens, tocar "Já Saí", esperar 10 min e conferir que não há aviso de atraso. Repetir sem itens.
- **R2:** tipo ALARME e tela bloqueada. A tela acende; confirmar; a notificação some; nenhum aviso de atraso.
- **R3:** Android 14, `adb shell appops set com.brunocsantos.agoravai USE_FULL_SCREEN_INTENT deny`, tipo DESAFIO. Tocar na notificação abre a tela do lembrete.
- **R4:** Android 12, desligar "Alarmes e lembretes" para o app. Salvar mostra a mensagem com "Abrir configurações" e a lista não muda. Desativar um compromisso existente e tentar religar: aparece a mensagem e o switch fica desligado.
- **R5:** criar, depois desativar e excluir outro; conferir com `adb shell dumpsys alarm | grep agoravai` que não restam alarmes deles.

## 3. Abordagem
1. **Cálculo puro** em `notifications/HorariosLembrete.kt`. É o primeiro código testável sem Android (P7).
2. **`object CompromissoAlarmScheduler`** em `notifications/`, com `agendar(context, compromisso, etapas, itens)`, `cancelar(context, compromissoId, numEtapas)` e `podeAgendarExato(context)`. O conteúdo vem de `CompromissoViewModel.kt:240-316`, com estas correções:
   - extras via constantes `EXTRA_*` do `AlarmReceiver.kt` (DT-02);
   - **uma única fábrica** para o `PendingIntent` de `CHECK_LATE`, usada para agendar e cancelar (DT-06);
   - request codes idênticos aos de hoje (§5.2).
3. **`AlarmReceiver`**: `setContentIntent(fullScreenPendingIntent)` na notificação de tela cheia (DT-19) e o ID da notificação repassado à `ReminderActivity`.
4. **`ReminderActivity`**: repassa o ID da notificação no `CONFIRM_EXIT` (DT-16).
5. **`NotificationActionReceiver.CONFIRM_EXIT`**: cancela o alarme de atraso pela fábrica do item 2, no lugar de `id*100+98`.
6. **Permissão (LEMB-10)**: `salvarCompromisso` e `toggleCompromisso` checam `podeAgendarExato` **antes** de gravar qualquer coisa. Novo campo `pedirPermissaoAlarme: Boolean` em `AddCompromissoUiState`; o Snackbar ganha a ação "Abrir configurações" (`ACTION_REQUEST_SCHEDULE_EXACT_ALARM`). No toggle aparece um `Toast` e o compromisso não é gravado como ativo.

| Alternativa descartada | Por que não |
|---|---|
| Só trocar as strings no VM | Corrige DT-02, mas deixa a lógica presa no VM: o boot receiver e a chegada não conseguiriam reagendar nem cancelar |
| Unificar as chaves num novo `object IntentExtras` com valores novos | Mudaria os valores lidos por receptores e quebraria alarmes já agendados; dá para fazer depois, numa mudança própria |
| Robolectric para testar extras e notificações | Dependência pesada (P8); os roteiros manuais bastam para esta mudança |

## 4. Checklist da constituição

| Princípio | Atende? | Nota |
|---|---|---|
| P1 Spec primeiro | ✅ | spec aprovada em 2026-10-26 |
| P2 Lembrete não falha em silêncio | ✅ | foco da mudança |
| P3 Dados preservados | ✅ | sem mudança de schema |
| P4 Local-first | ✅ | — |
| P5 Desenho para TDAH | ✅ | mensagem curta com ação direta |
| P6 Contratos explícitos | ✅ parcial | o VM passa a usar as constantes; os literais da `ReminderActivity`/`NotificationActionReceiver` seguem iguais (unificação fica para depois) |
| P7 Critérios verificáveis | ✅ | matriz §2; primeiros testes do projeto |
| P8 Simplicidade | ✅ | só `junit:junit:4.13.2` (já previsto no `libs.versions.toml`) |
| P9 Consistência | ✅ | `object` em `notifications/`, nomes em português |

**Exceções:** nenhuma.

## 5. Passos

### Fase 1 — Testes e cálculo puro
- **Critérios cobertos:** LEMB-01.a, LEMB-08, COMP-07.a
- **Arquivos:** `app/build.gradle.kts` (`testImplementation("junit:junit:4.13.2")`), `notifications/HorariosLembrete.kt` (novo), `app/src/test/.../notifications/HorariosLembreteTest.kt` (novo); remover o `ExampleUnitTest.kt` vazio.
- **Mudanças:** `calcularHorarios(dataHoraEvento, deslocamentoMin, segurancaMin, duracoesEtapasMin): HorariosLembrete(inicioEtapas, saida, verificacaoAtraso, inicioPreparo)`. O VM passa a usá-la em `salvarCompromisso` (COMP-07).
- **Verificação:** `./gradlew testDebugUnitTest assembleDebug`

### Fase 2 — Agendador e correção dos extras
- **Critérios cobertos:** LEMB-02, LEMB-03, LEMB-07, COMP-10.a, COMP-11.b
- **Arquivos:** `notifications/CompromissoAlarmScheduler.kt` (novo), `ui/compromissos/CompromissoViewModel.kt`, `services/NotificationActionReceiver.kt`
- **Mudanças:** itens 2 e 5 da abordagem. O VM chama o agendador e as funções privadas de alarme saem dele.
- **Verificação:** build + testes + R1, R5

### Fase 3 — Tela cheia
- **Critérios cobertos:** LEMB-04.a/.b, LEMB-07.b
- **Arquivos:** `notifications/AlarmReceiver.kt`, `ui/ReminderActivity.kt`
- **Mudanças:** itens 3 e 4 da abordagem.
- **Verificação:** build + R2, R3

### Fase 4 — Permissão de alarme exato
- **Critérios cobertos:** LEMB-10.a/.b
- **Arquivos:** `CompromissoViewModel.kt`, `AddCompromissoScreen.kt`, `CompromissosScreen.kt`
- **Mudanças:** item 6 da abordagem.
- **Verificação:** build + R4

## 6. Impacto
- **Room:** nenhum.
- **Manifest:** nenhum.
- **Alarmes já agendados em aparelhos:** seguem com os extras antigos até serem reagendados. A mudança `reagendar-apos-reiniciar` resolve isso automaticamente na atualização do app.
- **Textos visíveis:** "Permita “Alarmes e lembretes” para o Agora Vai avisar na hora certa.", "Abrir configurações".

## 7. Riscos e rollback
| Risco | Mitigação |
|---|---|
| Erro de request code no agendador quebra o cancelamento | Códigos copiados de §5.2 + R5 com `dumpsys alarm` |
| `SecurityException` em algum caminho não coberto | `podeAgendarExato` checado antes de qualquer gravação; o agendador também protege a chamada |

**Rollback:** reverter os commits da mudança (sem schema, sem migração).

## 8. Atualização das specs ao final
- [ ] `capabilities/lembretes.md`: LEMB-02/03/04/07/10 com o texto novo e status ✅
- [ ] `capabilities/compromissos.md`: COMP-10.a, COMP-11 → ✅
- [ ] `architecture.md`: riscar DT-02, DT-06, DT-16, DT-17, DT-19; §3.2 com o novo agendador; histórico
