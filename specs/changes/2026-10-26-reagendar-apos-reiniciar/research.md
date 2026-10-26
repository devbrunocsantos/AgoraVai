# Research — Reagendar após reiniciar ou atualizar

- **Data:** 2026-10-26
- **Autor:** Claude
- **Spec:** [`spec.md`](spec.md)
- **Requisitos do delta:** LEMB-11, AUTO-05

## 1. Requisitos × código hoje

| Requisito | Status na spec viva | Real | Evidência |
|---|---|---|---|
| LEMB-11 | ⬜ | confere | O `AndroidManifest.xml` não declara `RECEIVE_BOOT_COMPLETED` nem um receiver para `BOOT_COMPLETED`/`MY_PACKAGE_REPLACED`. |
| AUTO-05 | ✅ | **diverge após reboot** | As geofences das automações só são registradas em `PomodoroViewModel.updateSchedules` (`ui/pomodoro/PomodoroViewModel.kt:243-256`), ou seja, quando o usuário salva ou alterna uma automação. Depois de reiniciar, a automação por local fica inativa até a próxima edição. |
| AUTO-04 | ✅ | confere | O `PeriodicWorkRequest` (`services/PomodoroScheduler.kt:19-50`) é persistido pelo WorkManager, que se reagenda sozinho após o boot. |

## 2. Fatos relevantes
- **Alarmes:** o `AlarmManager` apaga todos os alarmes ao reiniciar. Alarmes RTC usam horário absoluto (epoch), e `dataHoraEvento` também (`data/Compromisso.kt`), então mudanças de fuso ou relógio não pedem ação.
- **Geofences:** o Play Services as apaga ao reiniciar.
- **Dados para reagendar:** `AppDao.getAllCompromissosComEtapas()` só existe como `Flow` (`data/AppDao.kt`); um reagendador pode usar `.first()` ou uma consulta `suspend` nova. As automações têm `getAllAutomationSettingsOnce()`.
- **Agendamento:** depois da mudança `lembrete-de-saida`, vai existir `CompromissoAlarmScheduler.agendar/cancelar/podeAgendarExato`. Depois da `deteccao-de-chegada`, `AgoraVaiApplication` inicializa os managers de geofence e a regra do período de chegada.
- **Request codes iguais e `FLAG_UPDATE_CURRENT`:** reagendar substitui o alarme existente em vez de duplicar, então reagendar tudo várias vezes é seguro.

## 3. Restrições de plataforma
- `BOOT_COMPLETED` exige `RECEIVE_BOOT_COMPLETED` e um receiver `exported="true"`. É um broadcast protegido, só o sistema envia.
- `MY_PACKAGE_REPLACED` só é entregue ao próprio app e não precisa de permissão.
- Um receiver tem ~10 s. Acesso ao Room exige `goAsync()` + corrotina em IO.
- Em Android com "Direct Boot", `BOOT_COMPLETED` chega depois do desbloqueio. Para lembretes de compromisso isso é aceitável (o app não é `directBootAware`).
- Alguns fabricantes (Xiaomi, por exemplo) só entregam `BOOT_COMPLETED` com "início automático" liberado. Já existe a orientação na `PermissionsScreen` (SIS-05).

## 4. Contratos
Os IDs e request codes de `architecture.md` §5.2 precisam ser os mesmos usados no agendamento original.

## 5. Testes existentes
Nenhum. Verificação possível com `adb reboot` e `adb shell dumpsys alarm | grep agoravai`, e para atualização com `adb install -r`.

## 6. Perguntas em aberto
- Ver Q5 na spec.
