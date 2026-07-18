# ADR-0002: Plano de corte sem versionamento no MVP

## Status
Aceita

## Contexto
Um projeto pode ter seu plano de corte gerado mais de uma vez — por
exemplo, depois que o marceneiro edita a lista de peças ou ajusta a
quantidade de chapas disponíveis. É preciso decidir o que acontece com o
plano anterior quando um novo é gerado.

## Decisão
Cada chamada a `POST /projetos/{uuid}/plano-de-corte` cria um novo registro
`PlanoDeCorte`, sem apagar nem modificar planos gerados anteriormente para
o mesmo projeto. `GET /projetos/{uuid}/plano-de-corte` e a exportação em
PDF sempre retornam o mais recente
(`findFirstByProjetoIdOrderByGeradoEmDesc`). Não existe endpoint para listar
o histórico de planos nem para comparar dois planos entre si.

## Consequências
- Simples de implementar e de entender: "gerar de novo" sempre reflete o
  estado atual de peças/chapas, sem o usuário precisar gerenciar versões.
- Planos antigos continuam no banco (não são apagados), então o dado não se
  perde — só não há UI nem API para acessá-los ainda.
- Se o marceneiro quiser comparar "plano A vs. plano B" (ideia levantada na
  avaliação inicial da ideia, mas não confirmada como prioridade na
  entrevista), isso é aditivo: expor um endpoint de listagem/histórico não
  exige mudança de schema, só uma nova query e uma nova rota.

## Alternativas consideradas
- **Sobrescrever o plano existente em vez de criar um novo** — descartada:
  perde o histórico completamente e não abre caminho para comparação
  futura sem migração de dados.
- **Implementar comparação de planos desde já** — descartada para o MVP:
  não foi confirmada como prioridade na entrevista (doc, seção "Fora de
  escopo do MVP"), e a meta de validação é rodar plano em cima de projetos
  reais o quanto antes, não construir funcionalidades especulativas.
