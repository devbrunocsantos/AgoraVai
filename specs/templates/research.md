# Research — <título da mudança>

- **Data:** AAAA-MM-DD
- **Autor:** <nome / agente>
- **Spec:** [`spec.md`](spec.md)
- **Requisitos do delta:** PREFIXO-NN, …

> Fase de **fatos**. Não proponha soluções aqui. Descreva o sistema como ele é,
> sempre com `caminho:linha`.

## 1. Requisitos × código hoje

| Requisito | Status na spec viva | Status real no código | Evidência (`arquivo:linha`) |
|---|---|---|---|
| PREFIXO-NN | ✅ / ⚠️ / ⬜ | confere / diverge | `app/src/main/java/.../X.kt:123` |

<Se algum status da spec viva estiver errado, registre aqui. Esse é um achado importante.>

## 2. Arquivos e componentes relevantes

| Arquivo:linha | Componente | Papel nesta mudança |
|---|---|---|
| `...` | `Classe.funcao` | … |

## 3. Fluxo atual (ponta a ponta)
1. <UI> …
2. <ViewModel> …
3. <DAO / Scheduler / Receiver / Service> …

## 4. Contratos que não podem quebrar (constituição P6)
- Chaves de extras de `Intent`: …
- Request codes de `PendingIntent` / IDs de notificação / IDs de geofence: …
- Tabelas, colunas e versão do Room: …
- Chaves de SharedPreferences: …
- Ações de broadcast/serviço: …

## 5. Restrições de plataforma
<Versões do Android, permissões, Doze, fabricantes, limites de API.>

## 6. Testes e verificação existentes
<Quais testes cobrem a área? Como verificar manualmente hoje?>

## 7. Outros achados
<Fatos estranhos fora do escopo. Candidatos a novos DT-xx.>

## 8. Perguntas em aberto
- [ ] …
