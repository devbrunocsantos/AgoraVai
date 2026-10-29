# Agora Vai — guia para agentes

App Android (Kotlin + Jetpack Compose, MVVM, Room) de pontualidade e foco: lembretes de saída para compromissos, calculados pela rota, e timer Pomodoro com automações por horário e local.

## Spec-Driven Development + RPI
Este projeto segue **Spec-Driven Development**: a spec é a fonte da verdade e vem antes do código. Tudo fica em `docs/sdd/`:

- **`docs/sdd/README.md`** — o fluxo e a estrutura dos artefatos.
- **`docs/sdd/constituicao.md`** — princípios inegociáveis, stack e comandos de verificação. Todo plano é checado contra eles.
- **`docs/sdd/roadmap.md`** — visão, marcos, backlog, decisões e riscos.
- **`docs/sdd/specs/NNN-<slug>/`** — uma pasta por funcionalidade/correção: `spec.md`, `pesquisa.md`, `plano.md`, `validacao.md`, `passagens/`.
- **`docs/sdd/capacidades/`** *(transitório)* — specs vivas do comportamento atual (`COMP`, `LEMB`, `POMO`, `AUTO`, `SIS`). Leia as relevantes antes de qualquer mudança.
- **`docs/sdd/arquitetura.md`** *(transitório)* — design técnico, modelo de dados, contratos (§5.2) e dívida técnica (`DT-xx`).

Fluxo: `/constituicao` → `/especificar` → `/esclarecer` → `/pesquisar` → `/planejar` → `/implementar` → `/validar`. Painel: `/status-sdd`. Contexto: `/passagem` e `/retomar`.
Não pule os portões de aprovação (revisão da pesquisa, aprovação do plano). Nenhum comportamento observável muda sem spec aprovada. Ao concluir uma spec, as capacidades precisam refletir o novo comportamento.

## Build
- Requer `local.properties` com `MAPS_API_KEY="..."` (com aspas) e `sdk.dir=...` fora do Android Studio.
- `./gradlew assembleDebug` · `./gradlew testDebugUnitTest` · `./gradlew connectedAndroidTest`

## Cuidados
- Não renomeie chaves de extras de Intent, request codes de PendingIntent nem IDs de notificação sem conferir todos os usos (`arquitetura.md` §5.2).
- O banco usa `fallbackToDestructiveMigration`, então mudar o schema apaga os dados do usuário. Planeje migrações (constituição P3).
- Nomes de domínio são em português (`Compromisso`, `Etapa`, `ItemLembrete`). Mantenha o padrão.
- Testes citam o ID do critério que cobrem (ex.: `COMP-07.a`, ou `CA-1.1` em specs novas).
