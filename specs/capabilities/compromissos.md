# Capacidade: Compromissos (`COMP`)

Planejar um compromisso: quando é, de onde o usuário sai, para onde vai, o que
precisa fazer antes e o que não pode esquecer. Os lembretes gerados a partir
daqui estão em [`lembretes.md`](lembretes.md).

**Código principal:** `ui/compromissos/CompromissoViewModel.kt`,
`AddCompromissoScreen.kt`, `CompromissosScreen.kt`, `AddressAutocompleteField.kt`,
`data/Compromisso.kt`, `Etapa.kt`, `ItemLembrete.kt`.

**Definições**
- **Deslocamento:** minutos de viagem retornados pela Directions API (`duration.value / 60`, arredondado para baixo).
- **Horário de saída:** `dataHoraEvento − deslocamento − tempoDeSeguranca`.
- **Início do preparo:** `horário de saída − Σ duração das etapas`.

---

### COMP-01 · Novo compromisso com valores padrão — ✅
QUANDO o usuário toca no botão "+" da lista, o sistema DEVE abrir o formulário com:
data/hora = agora + 1 hora com minutos zerados; tempo de segurança = padrão
global (SIS-04); uma etapa vazia; itens "Chaves 🔑", "Carteira 💳", "Documentos 📄";
tipo de lembrete `PADRAO`.

- **a.** Dado que são 14:37, quando abro um novo compromisso, então a hora sugerida é 15:00 de hoje.
- **b.** Dado o tempo de segurança padrão de 20 min, quando abro um novo compromisso, então o campo mostra "20".

### COMP-02 · Campos obrigatórios — ✅
SE título, origem ou destino estiverem em branco ao salvar, ENTÃO o sistema DEVE
exibir "Preencha todos os campos." e não salvar.

- **a.** Dado o destino vazio, quando toco "Salvar", então vejo "Preencha todos os campos." e nada é gravado.

### COMP-03 · Etapas (preparativos) — ✅
O sistema DEVE permitir adicionar, remover (mantendo no mínimo uma linha),
reordenar (subir/descer) e editar etapas com descrição e duração em minutos.
SE uma etapa tiver descrição e duração ausente, não numérica ou ≤ 0, ENTÃO o
sistema DEVE marcar o campo de duração com erro e não salvar.
Etapas com descrição em branco DEVEM ser descartadas ao salvar.

- **a.** Dada a etapa "Banho" com duração vazia, quando salvo, então o campo "Min" fica em erro e nada é gravado.
- **b.** Dada uma etapa com descrição vazia e duração "10", quando salvo, então ela não é gravada.

### COMP-04 · Itens a não esquecer — ✅
O sistema DEVE permitir adicionar, editar e remover itens. Itens com nome em
branco DEVEM ser descartados ao salvar.

### COMP-05 · Cálculo de rota — ✅
QUANDO o usuário salva um compromisso válido, o sistema DEVE consultar a
Directions API com origem e destino e gravar o deslocamento em minutos.
SE a API não retornar rota, ENTÃO o sistema DEVE exibir "Rota não encontrada." e não salvar.
SE ocorrer exceção (ex.: sem internet), ENTÃO o sistema DEVE exibir "Erro ao salvar.".

- **a.** Dada uma rota de 1.830 s, quando salvo, então `tempoDeslocamentoMinutos = 30`.
- **b.** Dado um destino inexistente, quando salvo, então vejo "Rota não encontrada.".

### COMP-06 · Tempo de segurança — ✅
O campo de tempo de segurança DEVE aceitar só dígitos. SE estiver vazio ao
salvar, ENTÃO o sistema DEVE usar o padrão global.

### COMP-07 · Bloqueio de compromisso inviável — ✅
SE o início do preparo for anterior ao momento atual, ENTÃO o sistema DEVE exibir
"Impossível! Atraso de N min." (N = minutos inteiros de atraso) e não salvar.

- **a.** Dado um evento às 10:00, 30 min de deslocamento, 15 de segurança e 20 de etapas, quando salvo às 9:00, então vejo "Impossível! Atraso de 5 min.". *(teste do cálculo: `HorariosLembreteTest`)*

### COMP-08 · Coordenadas do destino — ✅
QUANDO o usuário salva, o sistema DEVE tentar obter latitude/longitude do destino
pelo Geocoder. A falha NÃO DEVE impedir o salvamento: as coordenadas ficam
`0.0` e a detecção de chegada (LEMB-09) é desativada para o compromisso.

### COMP-09 · Edição — ✅
QUANDO o usuário toca em editar, o sistema DEVE abrir o formulário preenchido.
Ao salvar, o sistema DEVE substituir etapas e itens, zerar `confirmouSaida` e
`confirmouChegada`, cancelar alarmes e geofence antigos e agendar os novos.

### COMP-10 · Ativar e desativar — 🧪
QUANDO o usuário desliga o switch, o sistema DEVE cancelar os alarmes e a
geofence do compromisso e exibi-lo esmaecido. QUANDO religa, o sistema DEVE
reagendar alarmes e geofence (sujeito a LEMB-10).

- **a.** Dado um compromisso desativado antes do horário de saída, quando passa o horário de saída + 10 min, então nenhum lembrete de saída nem aviso de atraso aparece.
- 🧪 Cancelamento do aviso de atraso corrigido em [`lembrete-de-saida`](../changes/2026-10-26-lembrete-de-saida/); aguarda o roteiro manual R5.

> Observação: religar um compromisso cujo horário já passou agenda alarmes no
> passado, e o `AlarmManager` dispara na hora. Nenhum requisito cobre esse
> caso ainda (pergunta em aberto).

### COMP-11 · Exclusão — 🧪
QUANDO o usuário toca em excluir, o sistema DEVE pedir confirmação ("Tem certeza
de que deseja excluir o compromisso "<título>"? Todas as notificações agendadas
serão canceladas."). Ao confirmar, o sistema DEVE apagar o compromisso, suas
etapas e itens (cascata) e cancelar alarmes e geofence.

- **b.** Dado um compromisso excluído, quando passa o horário de saída + 10 min, então nenhum aviso de atraso aparece.
- 🧪 Cancelamento do aviso de atraso corrigido em [`lembrete-de-saida`](../changes/2026-10-26-lembrete-de-saida/); aguarda o roteiro manual R5.

### COMP-12 · Lista — ✅
O sistema DEVE listar os compromissos em ordem crescente de data/hora, com
título, "dd/MM/yyyy às HH:mm" e dias de repetição (se houver). Ao expandir, DEVE
mostrar tempo total de preparo, tempo de viagem estimado, etapas, itens e as
ações mapa, editar e excluir.
ENQUANTO não houver compromissos, o sistema DEVE mostrar "Tudo organizado por aqui! ✨".

### COMP-13 · Repetição semanal — ⚠️ DT-04
ONDE o usuário selecionou dias de repetição, o sistema DEVE, após cada
ocorrência, agendar os lembretes da próxima ocorrência num dia selecionado.

- Hoje: os dias são gravados e exibidos ("Seg, Qua"), mas os alarmes disparam só na data original.

### COMP-14 · Excluir após concluir — ✅
ONDE "Excluir após concluir" está marcado, o sistema DEVE apagar o compromisso
quando a data/hora do evento já tiver passado. A verificação roda ao abrir o app,
na criação do `CompromissoViewModel`.

### COMP-15 · Entrada de endereço — ✅
O sistema DEVE sugerir endereços (Places Autocomplete) a partir do 3º caractere
digitado. QUANDO o usuário toca em "localização atual", o sistema DEVE preencher
o campo com o endereço da posição atual, ou com "lat, lng" se não houver endereço.
SE não houver permissão de localização, ENTÃO o sistema DEVE exibir "Permissão negada.".

### COMP-16 · Abrir rota no mapa — ✅
QUANDO o usuário toca no ícone de mapa, o sistema DEVE abrir o Google Maps com o
destino. SE não for possível, ENTÃO o sistema DEVE exibir "Não foi possível abrir o mapa.".
