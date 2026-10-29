# Roadmap: Agora Vai

> Atualizado em: 2026-10-29

## Visão
Um app em que o usuário confia de olhos fechados para sair na hora: nenhum
lembrete falha em silêncio (P2) e nenhum dado se perde (P3). Próximo marco:
**M1 — lembretes confiáveis**, concluindo e validando as correções em andamento.

## Marcos
| Marco | Objetivo | Specs | Status |
|---|---|---|---|
| M1 — Lembretes confiáveis | Lembrete de saída, chegada e reinicialização funcionando em aparelho real | 001, 002, 003, + DT-21 | em andamento |
| M2 — Pomodoro confiável | Meta da sessão, progresso e agendamento por automação corretos | 004 | não iniciado |
| M3 — Pronto para publicar | Dados preservados entre versões, billing decidido, chave protegida, build limpo | DT-03, DT-09, DT-10, DT-13 | não iniciado |

> Marcos e prioridades abaixo são **proposta** do PM (2026-10-29) — aguardando confirmação do Bruno.

## Backlog
| ID | Item | Prioridade | Valor | Esforço | Depende de | Status |
|---|---|---|---|---|---|---|
| 001 | Lembrete de saída confiável (DT-02, 06, 16, 17, 19) | Must | alto | M | — | implementada — faltam A4, R4 |
| 002 | Detecção de chegada (DT-01, DT-20) | Must | alto | M | 001 | implementada — B5/B6 ✅; faltam R4, R5 |
| 003 | Reagendar após reiniciar ou atualizar (DT-05) | Must | alto | M | 001, 002 | rascunho (pesquisa/plano prévios) |
| — | Endereço inexistente aceito com 0 min de percurso (DT-21, COMP-05) | Must | alto | P | — | ideia |
| — | Migrações reais do Room (DT-03, P3) | Must | alto | M | — | ideia — **antes de qualquer mudança de schema** |
| 004 | Pomodoro: meta da sessão e progresso (DT-07, 08, 18) | Should | médio | M | — | rascunho (pesquisa/plano prévios) |
| — | Reagendar compromissos recorrentes (DT-04, COMP-13) | Should | médio | M | 003 | ideia — requer decisão de produto |
| — | Atualizar AGP 8.4.1 → 8.13.2 | Should | baixo | P | — | feito (`f4be55c`) |
| — | Toolchain Java: JDK 22 avisa que source/target 8 está obsoleto (`jvmTarget = 1.8`) | Could | baixo | P | — | ideia |
| — | Canal de notificação criado no `Application` (follow-up da 002) | Should | médio | P | — | ideia |
| — | Permissão de tela cheia (Android 14) no diagnóstico (SIS-05) | Could | médio | P | 001 | ideia |
| — | Decidir billing (DT-09) | Could | — | P | — | ideia — decisão de produto |
| — | Proteger a chave da Directions API e usar trânsito (DT-10) | Could | médio | M | — | ideia |
| — | Higiene de build e UI (DT-11, DT-12, DT-13, DT-15) | Could | baixo | P | — | ideia |

## Em andamento
- **001** — validação — rodar A4 (Android 14) e R4 (Android 12/12L) e então `/validar 001`.
- **002** — validação — B5 e B6 ✅; faltam R4 e R5 (ver risco DT-21) e então `/validar 002`.

## Decisões
| Data | Decisão | Motivo | Impacto |
|---|---|---|---|
| 2026-10-26 | Sem alarme exato, bloquear salvar/reativar (001, Q1) | O app nunca aceita compromisso que não consegue lembrar na hora | RF-005 da 001 |
| 2026-10-28 | Revisão 2 da 002: monitorar a chegada só a partir do início do preparo | Teste em aparelho: quem já estava no destino não tinha a chegada detectada | Fase 5 da 002; a 003 herda o comportamento |
| 2026-10-29 | Migrar a convenção `specs/` para `docs/sdd/` (padrão SDD+RPI) | Pedido do Bruno: um fluxo só, com as skills `/especificar` … `/validar` | Comandos `/spec`, `/research`, `/plan`, `/implement` removidos |
| 2026-10-29 | Manter `capacidades/` e `arquitetura.md` como transitórios até concluir 001–004 | Os testes citam IDs de capacidade e o registro de DTs ainda é usado | Constituição §8; decidir o destino depois |
| 2026-10-29 | Atualizar o AGP para 8.13.2 após verificação (opção A) | Build limpo e testes passando | Commit separado `f4be55c` |
| 2026-10-29 | Reescrever as 4 specs no template novo; `implement.md` mesclado no `plano.md` | Padronizar os artefatos | CAs citam o ID antigo (`CA-1.4 (LEMB-09.f)`) |

## Riscos
| Risco | Probabilidade | Impacto | Mitigação | Dono | Status |
|---|---|---|---|---|---|
| Testes manuais exigem aparelhos específicos (Android 12/12L para R4, Android 14 para A4) | alta | médio | Usar emulador AVD da versão certa | Bruno | aberto |
| Trabalho em andamento acima do ideal: 2 specs à espera de validação, e a 003 depende delas | média | médio | Validar 001/002 antes de começar a 003 | PM | aberto |
| DT-21: endereço errado gera lembrete tarde demais, sem aviso (P2) | média | alto | Spec própria logo após M1 | PM | aberto |
| DT-03: qualquer mudança de schema apaga os dados do usuário | média | alto | Spec de migrações antes de qualquer mudança no `AppDatabase` | PM | aberto |
| AGP 8.13.2 sem commit e sem verificação pode quebrar o build | média | médio | Build limpo + testes 10/10 verificados; commit `f4be55c` | Bruno | fechado |

## Próximos passos
1. Rodar no aparelho A4 e R4 (001) e R4 e R5 (002) → `/validar 001` e `/validar 002`.
2. `/esclarecer 003` (Q5 e o critério de chegada após reiniciar) → reconferir a pesquisa → `/planejar 003`.
3. Especificar DT-21.
