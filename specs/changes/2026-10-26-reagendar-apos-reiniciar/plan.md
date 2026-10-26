# Plan — Reagendar após reiniciar ou atualizar

- **Data:** 2026-10-26
- **Spec:** [`spec.md`](spec.md) · **Research:** [`research.md`](research.md)
- **Status:** Rascunho
- **Pré-requisitos:** `lembrete-de-saida` e `deteccao-de-chegada` implementadas

## 1. Objetivo
Cumprir LEMB-11 e AUTO-05 após reinicialização e atualização, sem abrir o app.

## 2. Matriz de verificação

| Critério | Tipo | Verificação |
|---|---|---|
| LEMB-11.b (filtrar o passado) | Unitário | `ReagendamentoTest`: horários passados são descartados, futuros mantidos |
| LEMB-11.c | Unitário | `ReagendamentoTest`: compromisso desativado não gera alarmes |
| LEMB-11.a | Manual | R1: criar compromisso, `adb reboot`, não abrir o app, `dumpsys alarm` lista os alarmes; esperar o lembrete |
| LEMB-11.d | Manual | R2: instalar a versão atual (1.5), criar compromisso, `adb install -r` a nova versão, conferir título e "Já Saí" |
| LEMB-11.e | Manual | R3: Android 12, revogar alarmes exatos, `adb reboot` → notificação "Seus lembretes estão pausados" |
| AUTO-05.a | Manual | R4: automação por local, `adb reboot`, mock de localização no local → "Hora de Focar!" |

## 3. Abordagem
1. **Regra pura** `filtrarFuturos(horarios, agora)` em `HorariosLembrete.kt`: quais alarmes ainda devem ser agendados.
2. **`object Reagendador`** em `notifications/`:
   - `reagendarTudo(context)`: compromissos ativos → `CompromissoAlarmScheduler.agendar` (só horários futuros) + geofence de chegada (se o período não terminou);
   - automações ativas com local → `GeofenceManager.addOrUpdateGeofence`.
   - Sem permissão de alarme exato → notificação de LEMB-11.e com `contentIntent` para a `MainActivity`.
3. **`BootReceiver`** (`exported=true`) para `BOOT_COMPLETED` e `MY_PACKAGE_REPLACED`, com `goAsync()` chamando `Reagendador.reagendarTudo`.
4. O `CompromissoAlarmScheduler.agendar` ganha o parâmetro `apenasFuturos`. O reagendador usa `true`. Salvar e religar mantêm o comportamento atual até a decisão de produto sobre COMP-10.

| Alternativa descartada | Por que não |
|---|---|
| Reagendar ao abrir o app | Não cumpre "sem abrir o app"; o usuário pode nunca abrir |
| Migrar os lembretes para o WorkManager (que sobrevive ao boot) | O WorkManager não é exato; violaria LEMB-10 |

## 4. Checklist da constituição

| Princípio | Atende? | Nota |
|---|---|---|
| P1 Spec primeiro | ☐ | aguarda aprovação |
| P2 Lembrete não falha em silêncio | ✅ | foco da mudança, inclusive o aviso sem permissão |
| P3 Dados preservados | ✅ | só leitura |
| P4 Local-first | ✅ | — |
| P5 Desenho para TDAH | ✅ | aviso curto e acionável |
| P6 Contratos explícitos | ✅ | reutiliza o agendador (mesmos códigos) |
| P7 Critérios verificáveis | ✅ | matriz §2 |
| P8 Simplicidade | ✅ | receiver padrão do Android, sem dependências |
| P9 Consistência | ✅ | — |

## 5. Passos

### Fase 1 — Regra e reagendador
- **Critérios:** LEMB-11.b/.c
- **Arquivos:** `notifications/HorariosLembrete.kt`, `notifications/Reagendador.kt` (novo), `notifications/CompromissoAlarmScheduler.kt`, `data/AppDao.kt` (consulta `suspend` de compromissos ativos com etapas e itens), testes
- **Verificação:** `./gradlew testDebugUnitTest assembleDebug`

### Fase 2 — Receiver
- **Critérios:** LEMB-11.a/.d, AUTO-05.a
- **Arquivos:** `notifications/BootReceiver.kt` (novo), `AndroidManifest.xml` (permissão + receiver)
- **Verificação:** build + R1, R2, R4

### Fase 3 — Aviso sem permissão
- **Critérios:** LEMB-11.e
- **Arquivos:** `notifications/Reagendador.kt`
- **Verificação:** build + R3

## 6. Impacto
- **Room:** uma consulta nova, sem schema.
- **Manifest:** `RECEIVE_BOOT_COMPLETED` + `<receiver .notifications.BootReceiver>`. A Play Store não exige justificativa para essa permissão.
- **Aparelhos existentes:** na atualização, todos os lembretes futuros são reagendados com os extras corrigidos.
- **Textos visíveis:** "Seus lembretes estão pausados", "Permita “Alarmes e lembretes” para o Agora Vai avisar na hora certa."

## 7. Riscos e rollback
| Risco | Mitigação |
|---|---|
| Reagendar dispara lembretes antigos | `apenasFuturos = true` + teste LEMB-11.b |
| O receiver estoura o tempo com muitos compromissos | `goAsync()`; operações são locais e rápidas |

**Rollback:** reverter os commits e remover o receiver do manifest.

## 8. Atualização das specs ao final
- [ ] `capabilities/lembretes.md`: LEMB-11 com o texto novo, status ✅
- [ ] `capabilities/automacoes.md`: AUTO-05 com o texto novo
- [ ] `architecture.md`: riscar DT-05; §3.1 diagrama com o `BootReceiver`; §7 permissão; histórico
