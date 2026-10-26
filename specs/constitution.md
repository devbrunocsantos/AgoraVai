# Constituição · Agora Vai

Princípios que valem para **toda** mudança. O `plan.md` de cada mudança tem um
checklist destes itens. Para violar um princípio é preciso registrar uma
exceção justificada no plano e aprová-la explicitamente.

Alguns princípios ainda não são cumpridos pelo código atual. Nesses casos a
violação conhecida aparece abaixo com o `DT-xx` correspondente em
[`architecture.md`](architecture.md). Mudanças novas não podem piorar essas
violações.

---

### P1 — A spec vem primeiro
Nenhum comportamento observável muda sem uma spec aprovada em
`specs/changes/`. Ao concluir a mudança, `specs/capabilities/` descreve o novo
comportamento.

### P2 — Um lembrete nunca falha em silêncio
O usuário confia no app para sair na hora. Lembretes e alarmes devem disparar
com o app fechado, em Doze e após reiniciar o aparelho. Se uma permissão
necessária faltar, o usuário precisa ser **avisado**. Não basta registrar no log.
*Violações conhecidas:* DT-01, DT-02, DT-05, DT-17.

### P3 — Os dados do usuário são preservados
Nenhuma versão publicada pode apagar dados do usuário. Toda mudança de schema
do Room traz uma `Migration` explícita, com o schema exportado e testado.
*Violação conhecida:* DT-03 (`fallbackToDestructiveMigration`).

### P4 — Local-first e privacidade
Os dados ficam no dispositivo. Nada sai do aparelho além do estritamente
necessário para as APIs do Google Maps (endereços de origem e destino).
Nenhuma telemetria ou serviço novo sem spec própria.

### P5 — Desenho para quem luta contra o tempo
O público inclui pessoas com TDAH, então o app deve:
- exigir o mínimo de decisões e toques para agir;
- usar textos curtos, diretos e encorajadores, em pt-BR, sem culpar o usuário;
- deixar o próximo passo sempre óbvio ("Saia agora", "Já Saí");
- usar a insistência de forma proporcional e controlada pelo usuário (tipos PADRAO → DESAFIO).

### P6 — Contratos explícitos
Chaves de extras de `Intent`, request codes de `PendingIntent`, IDs de
notificação, IDs de geofence e chaves de SharedPreferences são contratos.
Eles ficam definidos em um único lugar, como constantes, e documentados em
`architecture.md` §5.2. Não se usa string literal duplicada.
*Violação conhecida:* DT-02.

### P7 — Cada critério de aceite é verificável
Todo critério de aceite tem um teste automatizado que cita seu ID. Quando o
teste não é viável (ex.: tela de bloqueio), tem um roteiro manual escrito no
`plan.md`. Regras de cálculo, como horários de saída e a máquina de estados do
Pomodoro, ficam em código puro, testável sem Android.
*Violação conhecida:* DT-14.

### P8 — Simplicidade
Usar as APIs do Android e as bibliotecas já presentes. Uma dependência nova,
um padrão novo (DI, Navigation, repositórios) ou uma refatoração ampla só
entra com justificativa no plano e registro de ADR em `architecture.md` §10.

### P9 — Consistência com o código existente
Nomes de domínio em português (`Compromisso`, `Etapa`), `StateFlow` nos
ViewModels, `object` para singletons de infraestrutura e Material 3.
Melhorias de estilo vêm em mudanças próprias, não misturadas com features.

---

| Versão | Data | Mudança |
|---|---|---|
| 1.0 | 2026-10-26 | Versão inicial |
