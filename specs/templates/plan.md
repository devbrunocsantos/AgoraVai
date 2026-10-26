# Plan — <título da mudança>

- **Data:** AAAA-MM-DD
- **Spec:** [`spec.md`](spec.md) · **Research:** [`research.md`](research.md)
- **Status:** Rascunho | Aprovado em AAAA-MM-DD por <nome>

## 1. Objetivo
<Uma ou duas frases, ligadas à spec.>

## 2. Matriz de verificação
Cada critério de aceite do delta precisa de uma verificação (constituição P7).

| Critério | Tipo | Verificação |
|---|---|---|
| PREFIXO-NN.a | Unitário | `NomeDoTeste` em `app/src/test/...` (cita `PREFIXO-NN.a`) |
| PREFIXO-NN.b | Instrumentado / Manual | <roteiro passo a passo> |

## 3. Abordagem
<Solução escolhida e por quê.>

| Alternativa descartada | Por que não |
|---|---|
| … | … |

## 4. Checklist da constituição

| Princípio | Atende? | Nota |
|---|---|---|
| P1 Spec primeiro | ☐ | spec aprovada |
| P2 Lembrete não falha em silêncio | ☐ | |
| P3 Dados preservados (migração Room?) | ☐ | |
| P4 Local-first | ☐ | |
| P5 Desenho para TDAH (textos, fricção) | ☐ | |
| P6 Contratos explícitos | ☐ | |
| P7 Critérios verificáveis | ☐ | ver matriz §2 |
| P8 Simplicidade (deps/padrões novos → ADR) | ☐ | |
| P9 Consistência com o código | ☐ | |

**Exceções:** <nenhuma | princípio, motivo, por quanto tempo>

## 5. Passos

### Fase 1 — <nome>
- **Critérios cobertos:** PREFIXO-NN.a
- **Arquivos:** `…`
- **Mudanças:** …
- **Verificação:** `./gradlew assembleDebug` · `./gradlew test` · <roteiro>

### Fase 2 — <nome>
- **Critérios cobertos:** …
- **Arquivos:** `…`
- **Mudanças:** …
- **Verificação:** …

## 6. Impacto
- **Room:** <schema muda? versão N → N+1, `Migration(N, N+1)`, schema exportado>
- **Manifest / permissões:** …
- **Alarmes / geofences já agendados em aparelhos existentes:** …
- **Textos visíveis:** <exatamente como na spec>

## 7. Riscos e rollback
| Risco | Mitigação |
|---|---|
| … | … |

**Rollback:** …

## 8. Atualização das specs ao final
- [ ] `capabilities/<arquivo>.md`: incorporar o delta e atualizar os status
- [ ] `architecture.md`: DTs resolvidos, ADR (se houver), histórico
