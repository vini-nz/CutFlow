# ADR-0001: Sem autenticação no MVP

## Status
Aceita

## Contexto
O CutFlow nasce a partir de uma entrevista com um marceneiro-piloto
específico, para resolver o fluxo dele: medir peças, gerar o plano de
corte, descobrir quantas chapas comprar, orçar. Nenhuma pergunta da
entrevista (roteiro completo na documentação de fundamentos, seção 3)
levantou necessidade de múltiplos usuários, perfis de acesso distintos ou
controle de quem pode ver o quê. O FlowOps, por comparação, é multi-tenant
por desenho (várias empresas, várias contas) desde o primeiro requisito.

## Decisão
O MVP do CutFlow não implementa login, JWT, sessões ou RBAC. Toda rota da
API é acessível sem autenticação. `CorsConfigurer` restringe apenas a
origem (`cutflow.cors.allowed-origins`) que pode chamar a API.

## Consequências
- Menos código e menos superfície de erro no MVP: sem `SecurityConfig`,
  `JwtService`, filtro de autenticação, nem `@PreAuthorize` para manter.
- O modelo de dados **não** tem `usuario_id`/`conta_id` em nenhuma tabela —
  todo projeto criado é visível para quem tiver acesso à URL da aplicação.
  Aceitável para uso pessoal/interno com o marceneiro-piloto; **não**
  aceitável se a aplicação for exposta publicamente na internet sem uma
  camada de acesso na frente (ex: VPN, autenticação básica no proxy).
- Se o produto evoluir para atender mais de um marceneiro (doc, roadmap
  V3 — "avaliação de módulo do FlowOps ou produto comercial
  independente"), essa é a primeira peça a ser adicionada. O padrão de UUID
  público já usado em todas as entidades facilita a migração: basta
  introduzir uma tabela `contas`/`usuarios` e adicionar a FK equivalente a
  `company_id` do FlowOps nas tabelas existentes, sem precisar redesenhar
  identificadores já expostos na API.

## Alternativas consideradas
- **Copiar o JWT/RBAC do FlowOps de antemão** — descartada: adicionaria
  complexidade (login, gestão de token, matriz de permissões) sem nenhum
  requisito real por trás no momento, contrariando diretamente o objetivo
  do MVP validado na entrevista ("interface simples", doc seção 1.3).
