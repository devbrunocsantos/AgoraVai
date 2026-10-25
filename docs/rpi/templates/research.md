# Research — <título da tarefa>

- **Data:** AAAA-MM-DD
- **Autor:** <nome / agente>
- **Pedido original:** <cole o pedido>
- **IDs do SDD envolvidos:** RF-xx, RNF-xx, DT-xx

> Fase de **fatos**. Não proponha soluções aqui — descreva o sistema como ele é.

## 1. Entendimento do pedido
<Reescreva o pedido com suas palavras. O que muda para o usuário?>

## 2. Arquivos e componentes relevantes

| Arquivo:linha | Componente | Papel nesta tarefa |
|---|---|---|
| `app/src/main/java/.../Arquivo.kt:123` | `Classe.funcao` | ... |

## 3. Fluxo atual (ponta a ponta)
1. <UI> ...
2. <ViewModel> ...
3. <DAO / Scheduler / Receiver> ...

## 4. Contratos que não podem quebrar
- Chaves de `Intent` extra: ...
- Request codes de `PendingIntent` / IDs de notificação: ...
- Tabelas/colunas Room e versão do banco: ...
- Chaves de SharedPreferences: ...
- Ações de broadcast (`NotificationActionReceiver.Actions`, `PomodoroService.Actions`): ...

## 5. Restrições de plataforma
<Versões de Android, permissões, Doze, comportamento de fabricantes (Xiaomi/Samsung), limites de API.>

## 6. Testes existentes
<Quais testes cobrem esta área? Como verificar manualmente hoje?>

## 7. Observações e possíveis bugs encontrados
<Fatos estranhos observados no caminho, mesmo fora do escopo.>

## 8. Perguntas em aberto
- [ ] ...
