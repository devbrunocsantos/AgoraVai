# Constituição do Projeto: Agora Vai

> Versão: 1.0.0 · Ratificada em: 2026-10-26 · Última emenda: 2026-10-29
>
> Este documento define as regras não negociáveis do projeto. Specs, planos e
> implementações DEVEM respeitá-lo. Conflitos com a constituição devem ser
> resolvidos emendando-a explicitamente — nunca ignorando-a.

## 1. Propósito do projeto

App Android de pontualidade e foco para quem luta contra o tempo (inclusive
pessoas com TDAH): lembretes de saída para compromissos, calculados pela rota,
e timer Pomodoro com automações por horário e local.

## 2. Princípios

Alguns princípios ainda não são cumpridos pelo código atual. Nesses casos a
violação conhecida aparece com o `DT-xx` correspondente em
[`arquitetura.md`](arquitetura.md) §9. Mudanças novas NÃO DEVEM piorar essas
violações. Para violar um princípio é preciso registrar uma exceção justificada
no `plano.md` e aprová-la explicitamente.

### P1. A spec vem primeiro
Nenhum comportamento observável DEVE mudar sem uma spec aprovada em
`docs/sdd/specs/NNN-<slug>/`. Enquanto existirem as specs vivas
(`docs/sdd/capacidades/`, ver §8), ao concluir a mudança elas DEVEM descrever
o novo comportamento.
**Por quê:** a spec é a fonte da verdade; código e revisão existem para cumpri-la.
**Como verificar:** todo PR/commit funcional aponta para uma spec com status ≥ `planejada`.

### P2. Um lembrete nunca falha em silêncio
Lembretes e alarmes DEVEM disparar com o app fechado, em Doze e após reiniciar
o aparelho. Se uma permissão necessária faltar, o usuário DEVE ser **avisado**;
não basta registrar no log.
**Por quê:** o usuário confia no app para sair na hora.
**Como verificar:** todo caminho de agendamento trata a falta de permissão com aviso visível.
*Violações conhecidas:* DT-01, DT-02, DT-05, DT-17, DT-21.

### P3. Os dados do usuário são preservados
Nenhuma versão publicada DEVE apagar dados do usuário. Toda mudança de schema
do Room DEVE trazer uma `Migration` explícita, com o schema exportado e testado.
**Por quê:** perder compromissos e histórico destrói a confiança no app.
**Como verificar:** mudança na versão do `AppDatabase` vem com `Migration` + teste.
*Violação conhecida:* DT-03 (`fallbackToDestructiveMigration`).

### P4. Local-first e privacidade
Os dados DEVEM ficar no dispositivo. Nada sai do aparelho além do estritamente
necessário para as APIs do Google Maps (endereços de origem e destino).
Nenhuma telemetria ou serviço novo sem spec própria.
**Como verificar:** nenhuma chamada de rede nova fora de `network/` sem spec.

### P5. Desenho para quem luta contra o tempo
O app DEVE:
- exigir o mínimo de decisões e toques para agir;
- usar textos curtos, diretos e encorajadores, em pt-BR, sem culpar o usuário;
- deixar o próximo passo sempre óbvio ("Saia agora", "Já Saí");
- usar a insistência de forma proporcional e controlada pelo usuário (tipos PADRAO → DESAFIO).
**Como verificar:** textos visíveis novos aparecem entre aspas na spec e são aprovados.

### P6. Contratos explícitos
Chaves de extras de `Intent`, request codes de `PendingIntent`, IDs de
notificação, IDs de geofence e chaves de SharedPreferences são contratos. Eles
DEVEM ficar definidos em um único lugar, como constantes, e documentados em
`arquitetura.md` §5.2. NÃO DEVE haver string literal duplicada.
**Como verificar:** busca por literais `"EXTRA_` fora das constantes.
*Violação conhecida:* DT-02.

### P7. Cada critério de aceitação é verificável
Todo critério de aceitação DEVE ter um teste automatizado que cita seu ID.
Quando o teste não é viável (ex.: tela de bloqueio), DEVE haver um roteiro
manual escrito no `plano.md`. Regras de cálculo, como horários de saída e a
máquina de estados do Pomodoro, DEVEM ficar em código puro, testável sem Android.
**Como verificar:** a estratégia de testes do plano cobre todos os CAs.
*Violação conhecida:* DT-14.

### P8. Simplicidade
Usar as APIs do Android e as bibliotecas já presentes. Uma dependência nova,
um padrão novo (DI, Navigation, repositórios) ou uma refatoração ampla só
entra com justificativa no plano e registro de ADR em `arquitetura.md` §10.

### P9. Consistência com o código existente
Nomes de domínio em português (`Compromisso`, `Etapa`), `StateFlow` nos
ViewModels, `object` para singletons de infraestrutura e Material 3.
Melhorias de estilo vêm em mudanças próprias, não misturadas com features.

## 3. Stack e restrições técnicas

| Item | Escolha | Observações |
|---|---|---|
| Linguagem | Kotlin 1.9.23 | `jvmTarget = 1.8` |
| UI | Jetpack Compose (compilador 1.5.13), Material 3 | MVVM com `StateFlow` |
| Plataforma | Android, `minSdk 26`, `target/compileSdk 34` | AGP 8.4.1 (atualização para 8.13.2 pendente, fora de commit) |
| Banco de dados | Room 2.6.1 (KSP) | Ver P3 / DT-03 |
| Agendamento | `AlarmManager` exato (lembretes), WorkManager 2.9.0 (automações) | WorkManager não é exato: não usar para lembretes |
| Localização | Play Services Geofencing | |
| Rede | Retrofit 2.9.0 + Gson → Google Directions API | Chave em `local.properties` (`MAPS_API_KEY="..."`, com aspas) |
| Dependências proibidas | Novas sem ADR (P8) | Robolectric descartado por peso |

## 4. Padrões de código

- Estrutura: `app/src/main/java/com/brunocsantos/agoravai/` com `data/`, `ui/<área>/`, `notifications/`, `services/`, `workers/`, `network/`.
- Nomenclatura: domínio em português (P9).
- Tratamento de erros: falha de agendamento vira aviso visível ao usuário (P2), nunca só log.
- Testes: JUnit 4 em `app/src/test/...`; o nome ou comentário do teste cita o ID do critério que cobre (ex.: `LEMB-09.f`, `CA-2.6`).

## 5. Comandos de verificação

| Verificação | Comando |
|---|---|
| Instalar dependências | resolvido pelo Gradle (requer `local.properties` com `MAPS_API_KEY` e `sdk.dir`) |
| Testes unitários | `./gradlew testDebugUnitTest` |
| Testes instrumentados | `./gradlew connectedAndroidTest` (requer aparelho/emulador; não há testes hoje) |
| Lint | não configurado como portão (`./gradlew lintDebug` disponível) |
| Format (check) | não configurado |
| Typecheck | coberto pelo build (compilação Kotlin) |
| Build | `./gradlew assembleDebug` |

Verificação padrão de cada fase: `./gradlew testDebugUnitTest assembleDebug`.

## 6. Definição de pronto

Uma mudança só está pronta quando:
- [ ] Todos os comandos de verificação passam.
- [ ] Todos os critérios de aceitação da spec têm evidência (teste ou roteiro manual executado em aparelho e registrado no `plano.md`).
- [ ] A validação (`validacao.md`) foi gerada sem bloqueantes.
- [ ] Enquanto existirem: `capacidades/*.md` e `arquitetura.md` refletem o novo comportamento (status, DTs resolvidos, histórico).
- [ ] Um commit por fase do plano, com mensagem descritiva.

## 7. Governança

- Emendas são feitas com `/constituicao <mudança>` e registradas abaixo.
- Versionamento: MAJOR remove/redefine princípio; MINOR adiciona; PATCH esclarece.

## 8. Extensão transitória: specs vivas e dívida técnica

Herdado da convenção anterior do projeto, mantido **até a conclusão das specs
001–004** (decisão de 2026-10-29, ver roadmap):

- `docs/sdd/capacidades/*.md` descrevem o comportamento atual, com IDs estáveis
  (`COMP`, `LEMB`, `POMO`, `AUTO`, `SIS`), requisitos em EARS, critérios
  `ID.letra` e status ✅ ⚠️ 🧪 ⬜ 🗑️. Os testes existentes citam esses IDs.
- `docs/sdd/arquitetura.md` guarda o design técnico, os contratos (§5.2) e o
  registro de dívida técnica `DT-xx`.
- Specs novas citam, em cada critério, o ID da capacidade afetada
  (ex.: `CA-1.1 (LEMB-02.a)`), e a fase final do plano incorpora o delta.
- Ao fim desse período, uma emenda decide o destino desses artefatos
  (aposentar, migrando os DTs abertos para o roadmap, ou manter).

## Histórico de emendas

| Versão | Data | Mudança |
|---|---|---|
| 1.0.0 | 2026-10-26 | Ratificação inicial (como `specs/constitution.md` v1.0) |
| 1.0.0 | 2026-10-29 | Migrada para `docs/sdd/` no padrão SDD+RPI, sem mudança de princípios; adicionados stack, comandos de verificação, definição de pronto e §8 |
