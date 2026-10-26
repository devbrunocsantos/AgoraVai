# Agora Vai — guia para agentes

App Android (Kotlin + Jetpack Compose, MVVM, Room) de pontualidade e foco: lembretes de saída para compromissos, calculados pela rota, e timer Pomodoro com automações por horário e local.

## Spec-Driven Development
Este projeto segue **Spec-Driven Development**: a spec é a fonte da verdade e vem antes do código.

- **`specs/README.md`** — o fluxo **Spec → Research → Plan → Implement** e como escrever requisitos (EARS, critérios de aceite, status).
- **`specs/constitution.md`** — princípios inegociáveis. Todo plano é checado contra eles.
- **`specs/capabilities/`** — specs vivas do comportamento atual (`COMP`, `LEMB`, `POMO`, `AUTO`, `SIS`). Leia as relevantes antes de qualquer mudança.
- **`specs/architecture.md`** — design técnico, modelo de dados, convenção de IDs (§5.2) e dívida técnica (`DT-xx`).
- **`specs/changes/`** — uma pasta por mudança, com `spec.md`, `research.md`, `plan.md` e `implement.md`.

Comandos: `/spec`, `/research`, `/plan`, `/implement`. Não pule os portões de aprovação entre as fases.
Nenhum comportamento observável muda sem spec aprovada. Ao concluir uma mudança, as specs vivas precisam refletir o novo comportamento.

## Build
- Requer `local.properties` com `MAPS_API_KEY=...`.
- `./gradlew assembleDebug` · `./gradlew test` · `./gradlew connectedAndroidTest`

## Cuidados
- Não renomeie chaves de extras de Intent, request codes de PendingIntent nem IDs de notificação sem conferir todos os usos (`architecture.md` §5.2).
- O banco usa `fallbackToDestructiveMigration`, então mudar o schema apaga os dados do usuário. Planeje migrações (constituição P3).
- Nomes de domínio são em português (`Compromisso`, `Etapa`, `ItemLembrete`). Mantenha o padrão.
- Testes citam o ID do critério de aceite que cobrem (ex.: `COMP-07.a`).
