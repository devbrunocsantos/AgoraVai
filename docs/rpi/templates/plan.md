# Plan — <título da tarefa>

- **Data:** AAAA-MM-DD
- **Research:** [`research.md`](research.md)
- **IDs do SDD:** RF-xx, DT-xx
- **Status:** Rascunho | Aprovado em AAAA-MM-DD por <nome>

## 1. Objetivo
<Uma ou duas frases.>

## 2. Critérios de aceite
- [ ] <comportamento observável e testável>
- [ ] ...

## 3. Fora de escopo
- ...

## 4. Abordagem
<Solução escolhida e por quê.>

**Alternativas descartadas**

| Alternativa | Por que não |
|---|---|
| ... | ... |

## 5. Passos

### Fase 1 — <nome>
- **Arquivos:** `...`
- **Mudanças:** ...
- **Verificação:** `./gradlew assembleDebug` · `./gradlew test` · <roteiro manual>

### Fase 2 — <nome>
- **Arquivos:** `...`
- **Mudanças:** ...
- **Verificação:** ...

## 6. Impacto
- **Banco de dados:** <mudança de schema? nova versão? migração `Migration(x, y)`?>
- **Manifest / permissões:** ...
- **Alarmes / geofences já agendados em aparelhos existentes:** <precisam ser recriados?>
- **UI / textos:** ...

## 7. Riscos e rollback
| Risco | Mitigação |
|---|---|
| ... | ... |

**Rollback:** <como desfazer com segurança>

## 8. Atualizações no SDD
- [ ] §<seção> — <o que muda>
- [ ] Marcar DT-xx como resolvido
- [ ] Nova linha no histórico de versões
