# Capacidade: Sistema, permissões e configurações (`SIS`)

**Código principal:** `MainActivity.kt`, `ui/permissions/PermissionsScreen.kt`,
`ui/settings/SettingsScreen.kt`, `utils/FraseMotivacionalManager.kt`,
`AndroidManifest.xml`.

---

### SIS-01 · Navegação principal — ✅
O sistema DEVE ter duas abas, "Compromissos" e "Pomodoro", e um botão de
Configurações no topo. O botão voltar DEVE fechar a tela secundária aberta antes
de sair do app. QUANDO o app é aberto por uma notificação do Pomodoro, DEVE
abrir na aba Pomodoro.

### SIS-02 · Frase do dia — ✅
O topo DEVE mostrar "Agora Vai..." e uma frase de complemento ("...dar certo.").
A frase DEVE mudar uma vez por dia e ser diferente da do dia anterior.

### SIS-03 · Permissões na primeira execução — ✅
Ao abrir, o sistema DEVE pedir as permissões que faltarem: notificações
(Android 13+), localização precisa e depois localização em segundo plano (com
explicação prévia: "Permitir o tempo todo"). SE a localização for negada, ENTÃO
DEVE exibir "Permissão de localização é necessária para as automações por local.".
Uma única vez, DEVE sugerir desativar a otimização de bateria ("Melhore a Confiabilidade").

### SIS-04 · Configurações — ✅
A tela de Configurações DEVE permitir editar as durações do Pomodoro (POMO-03)
e o "Tempo de Segurança Padrão (minutos)" dos compromissos (padrão 15).
"Salvar" grava e volta. Valores inválidos caem no padrão.

### SIS-05 · Diagnóstico de permissões — ✅
A tela "Verificar Permissões & Problemas" DEVE mostrar o estado de notificações,
localização, alarmes exatos e sobreposição a outros apps, com botão "Ativar"
para o que faltar. O estado DEVE ser reavaliado ao voltar das configurações do
sistema. A tela DEVE trazer orientações para usuários Xiaomi/Samsung e o atalho
"Abrir Configurações do Sistema".

### SIS-06 · Restaurar compras — ⚠️ DT-09
O botão "Restaurar Compras" DEVE consultar as compras do usuário na Play Store e
atualizar o status premium.

- Hoje: força premium = verdadeiro.

### SIS-07 · Privacidade dos dados — ✅
Os dados DEVEM ficar só no dispositivo e não entrar em backup (`allowBackup=false`).
A chave do Google Maps NÃO DEVE ser versionada: ela vem de `local.properties`.

### SIS-08 · Compatibilidade — ✅
O app DEVE funcionar do Android 8.0 (API 26) ao Android 14 (API 34), em pt-BR,
com tema claro e escuro.
