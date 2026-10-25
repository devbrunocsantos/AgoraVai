# Agora Vai — guia para agentes

App Android (Kotlin + Jetpack Compose, MVVM, Room) de pontualidade e foco: lembretes de saída para compromissos calculados pela rota e timer Pomodoro com automações por horário/local.

## Documentação
- **`docs/SDD.md`** — especificação e design do sistema (requisitos, arquitetura, modelo de dados, convenção de IDs de alarmes, dívida técnica). Leia antes de qualquer mudança.
- **`docs/rpi/README.md`** — fluxo Research → Plan → Implement. Mudanças não triviais seguem esse fluxo via `/research`, `/plan` e `/implement`.

## Build
- Requer `local.properties` com `MAPS_API_KEY=...`.
- `./gradlew assembleDebug` · `./gradlew test` · `./gradlew connectedAndroidTest`

## Cuidados
- Não renomeie chaves de extras de Intent, request codes de PendingIntent nem IDs de notificação sem conferir todos os usos (SDD §5.2).
- O banco usa `fallbackToDestructiveMigration`: mudar o schema apaga os dados do usuário. Planeje migrações.
- Nomes de domínio são em português (`Compromisso`, `Etapa`, `ItemLembrete`); mantenha o padrão.
