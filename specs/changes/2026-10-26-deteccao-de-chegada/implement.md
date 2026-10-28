# Implement — Detecção de chegada

- **Spec:** [`spec.md`](spec.md) · **Plano:** [`plan.md`](plan.md)
- **Branch:** `ccr-80e3a1a2-ocxhin`
- **Início / fim:** 2026-10-27 / — (código concluído em 2026-10-27; faltam os roteiros manuais)

## Progresso por fase

| Fase | Status | Commit | Verificação |
|---|---|---|---|
| 1 — Application | ✅ feito | ver `git log` ("chegada fase 1") | build ✅ · testes ✅ · `AgoraVaiApplication` presente no manifest mesclado ✅ · `dumpsys location` ⏳ (aparelho) |
| 2 — Regra do período | ✅ feito | ver `git log` ("chegada fase 2") | build ✅ · testes 9/9 ✅ |
| 3 — Registro e receiver | ✅ feito | ver `git log` ("chegada fase 3") | build ✅ · testes 9/9 ✅ · manual R1–R4 ⏳ (aparelho) |
| 4 — Coordenadas não vazam (DT-20) | ✅ feito | ver `git log` ("chegada fase 4") | build ✅ · testes 9/9 ✅ · manual R5 ⏳ (aparelho) |

## Desvios do plano
- **Aprovação:** a Fase 4 (DT-20, critério COMP-08.a) foi acrescentada ao plano na aprovação, conforme proposto ao fim da mudança `lembrete-de-saida`.
- **Fase 3:** além de cancelar os alarmes pendentes, a chegada fecha a notificação "HORA DE SAIR" se ela já estiver na bandeja. A chegada grava `confirmouSaida`, e LEMB-07 diz que confirmar a saída fecha o lembrete. Sem isso, o alarme de tela cheia (que só sai ao confirmar) ficaria preso na bandeja de quem já chegou.
- **Fase 3:** `addGeofenceChegada` também deixa de registrar geofence para compromisso desativado (proteção; os chamadores já não faziam isso).

## Critérios de aceite

| Critério | Resultado | Evidência |
|---|---|---|
| LEMB-09.a / .c (regra do período) | ✅ | `JanelaChegadaTest` (dentro, antes do preparo, dia anterior, limites 8:30 e 12:00/12:01) |
| LEMB-09.a | ⏳ não verificado: o teste B2 foi feito já dentro do raio (limitação fora de escopo) | `handleCompromissoChegada` grava a chegada e notifica dentro do período |
| LEMB-09.b | ⏳ não verificado (idem B2) | `confirmarSaida` + `CompromissoAlarmScheduler.cancelar` na chegada |
| LEMB-09.c | 🟨 parcial (aparelho real, 2026-10-28: teste B1) | Entrada antes do período foi ignorada ✅; a detecção posterior não foi verificada (ver B2) |
| LEMB-09.d | ⏳ não verificado (B3 feito já dentro do raio, sem fechar à força) | `goAsync()` + managers inicializados no `AgoraVaiApplication` |
| LEMB-09.e | ⏳ código inalterado, falta roteiro R4 | `addGeofenceChegada` continua ignorando coordenadas `0.0` |
| COMP-08.a | ⏳ inconclusivo (B4): a Directions API aceitou o endereço inexistente com 0 min (DT-21) | `tempLatDestino/tempLngDestino` agora são locais a cada salvamento, iniciando em `0.0` |

## Saída das verificações
```
$ ./gradlew testDebugUnitTest assembleDebug
BUILD SUCCESSFUL
HorariosLembreteTest: tests=4 failures=0
JanelaChegadaTest: tests=5 failures=0
```

## Incorporação nas specs vivas
- [x] Delta incorporado em `specs/capabilities/lembretes.md` (LEMB-09), `compromissos.md` (COMP-08) e `automacoes.md` (AUTO-08), com status 🧪 até os roteiros passarem
- [x] `specs/architecture.md` atualizado (DT-01 e DT-20 🧪, §3.2, §5.3.1, histórico 1.4)
- [ ] `spec.md` marcada como **Concluída**: depois dos roteiros R1–R5

## Resultado dos testes em aparelho (aparelho real, 2026-10-28)
O usuário não tinha como simular localização no celular, então criou os compromissos **com destino no local onde já estava**:
- **B1 ✅:** entrada antes do período ignorada.
- **B2 ❌ / B3 ❌:** "HORA DE SAIR" tocou e "Você chegou!" não apareceu. **Causa:** ao salvar, a geofence é registrada com `INITIAL_TRIGGER_ENTER` e dispara a entrada na hora, mas fora do período (o salvamento exige que o preparo ainda não tenha começado), então é ignorada. Como o usuário não sai do raio, o Android não gera nova entrada. É a limitação listada como fora de escopo na spec. O comportamento bate com o código, mas o teste mostrou que o caso é real → **proposta de Revisão 2 na spec**.
- **B4 ⚠️:** endereço inexistente foi aceito com 0 min de percurso. Novo **DT-21** (correspondência aproximada da Directions API + arredondamento para baixo).

## Pendências / follow-ups
- Geofences de chegada **não sobrevivem a reiniciar o aparelho**: é a mudança `reagendar-apos-reiniciar`, que já usa `addGeofenceChegada(compromisso, etapas)`.
- O `NotificationScheduler.createNotificationChannel` continua só na `MainActivity`. Se um receiver notificar antes de o app ter sido aberto uma vez após a instalação, o canal não existe. Raro, mas vale mover para o `AgoraVaiApplication` numa mudança futura.
