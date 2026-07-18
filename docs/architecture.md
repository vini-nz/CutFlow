# Decisões de arquitetura

Este documento reúne as escolhas técnicas do backend que valem a pena
explicar — o "porquê", não só o "o quê". A documentação completa de domínio,
levantamento de requisitos e a entrevista com o marceneiro-piloto ficam em
[`PlanoDeCorte_Documentacao_Fundamentos.md`](../PlanoDeCorte_Documentacao_Fundamentos.md);
aqui ficam apenas as decisões de implementação.

## Sem autenticação no MVP — decisão deliberada, não esquecimento

Diferente do FlowOps (multi-tenant, várias empresas, RBAC), o CutFlow nasce
para **um marceneiro-piloto específico**. Nenhuma pergunta da entrevista
levantou necessidade de múltiplos usuários, perfis de acesso ou login no
fluxo real dele. Adicionar JWT/RBAC agora seria complexidade sem requisito
real por trás — o oposto do que a entrevista pediu (doc, seção 0: "resolver
muito bem o problema de pequenas e médias marcenarias, com uma interface
simples"). Ver [`docs/adr/0001-sem-autenticacao-no-mvp.md`](adr/0001-sem-autenticacao-no-mvp.md).

Isso também elimina toda a complexidade multi-tenant do FlowOps (filtro por
`company_id` em toda query, `@PreAuthorize`, JWT stateless): o CutFlow do
MVP serve **um** conjunto de projetos, sem isolamento entre usuários. Se o
produto evoluir para SaaS multi-cliente (doc, roadmap V3), essa é a primeira
peça a ser adicionada — o modelo de dados já está pronto para ganhar uma
tabela `usuarios`/`contas` sem redesenho, seguindo o mesmo padrão de UUID
público que o FlowOps usa.

## Timestamps controlados pelo banco, não pelo Java

As colunas `created_at`/`updated_at` são mapeadas como
`insertable = false, updatable = false` nas entidades JPA. O valor real vem
do `DEFAULT now()` e do trigger `set_updated_at()` do PostgreSQL, definidos
no schema (`db/cutflow_ddl.sql`) — mesmo padrão do FlowOps, pelo mesmo
motivo: evita divergência entre o horário do banco e o do servidor de
aplicação.

## UUID gerado no Java, IDs internos nunca expostos na API

Mesmo padrão do FlowOps: o `id` sequencial (`BIGSERIAL`) existe só para
joins e índices internos; toda resposta da API usa `uuid`. O `@PrePersist`
gera o UUID antes do insert para que o objeto já tenha o valor certo em
memória sem precisar de `refresh()`.

## `ddl-auto: validate`, nunca `update` ou `create`

O schema é responsabilidade exclusiva do `cutflow_ddl.sql`. O Hibernate só
confirma que o mapeamento das entidades bate com as tabelas existentes — se
uma entidade for alterada sem atualizar o DDL, a aplicação falha ao iniciar
em vez de alterar o banco silenciosamente.

## Uma Chapa por espessura, por projeto

Confirmado na entrevista: o marceneiro-piloto usa sempre a mesma medida de
chapa (274x184cm) e só varia a espessura (6/15/18/25mm). A constraint
`uq_chapas_projeto_espessura` trava essa regra no banco, e
`ChapaService.create`/`update` antecipa o erro com uma mensagem legível em
vez de esperar o 409 genérico do `DataIntegrityViolationException`.

Isso também define como o algoritmo é orquestrado: `PlanoDeCorteService`
agrupa as peças do projeto por `espessuraMm` e roda o `OtimizadorDePlano`
uma vez por grupo, casando cada grupo com a `Chapa` de mesma espessura —
nunca mistura peças de espessuras diferentes numa mesma chapa física, regra
de negócio real confirmada na entrevista, não só geométrica.

## Algoritmo isolado atrás de uma interface (`OtimizadorDePlano`)

`GuillotineOtimizadorDePlano` não conhece JPA, `Chapa` ou `Peca` — só os
tipos próprios do pacote `optimizer` (`ParametrosChapa`, `PecaParaEmpacotar`,
`ResultadoOtimizacao`). Isso permite:
- testar o algoritmo com `GuillotineOtimizadorDePlanoTest` sem subir
  contexto Spring nem banco;
- trocar a heurística por MaxRects/Skyline no futuro (doc, seção 6.2) sem
  tocar em service, controller ou persistência — só trocar a implementação
  do bean `OtimizadorDePlano`.

A numeração global de etiquetas (`numeroEtiqueta`) e de chapas
(`numeroChapa`) quando um projeto usa mais de uma espessura é
responsabilidade do `PlanoDeCorteService`, não do algoritmo: cada chamada ao
otimizador recomeça a numeração em 1 para aquele grupo, e o service aplica
um offset ao persistir, para que "peça #12" e "chapa 3" sejam únicos no
plano inteiro, não só dentro do grupo de espessura.

## Guillotine, não MaxRects, na V1

Reproduz o corte real da esquadrejadeira manual do marceneiro-piloto: cortes
sempre retos, de ponta a ponta (doc, seção 6). MaxRects tem melhor
aproveitamento geométrico teórico, mas pode gerar um plano geometricamente
melhor e fisicamente impossível de cortar numa esquadrejadeira manual — o
problema que a doc chama de "a melhor geometricamente vs. a melhor para
produção" (seção "Ordem dos cortes"). Fica candidato a V2 **apenas se** o
aproveitamento do Guillotine não bastar na prática com o piloto.

## Kerf e margem de borda são parâmetros da Chapa, não do algoritmo

`kerfMm` (padrão 4mm) e `margemBordaMm` (padrão 6mm) são campos da entidade
`Chapa`, ajustáveis por cadastro — confirmado na entrevista que esses
valores podem variar com a prática de corte do próprio marceneiro, não são
uma constante do sistema.

## Sobra registrada, reaproveitamento automático fora do MVP

O algoritmo devolve todo retângulo livre remanescente ao final do
empacotamento de cada chapa (sem filtro de tamanho mínimo — confirmado na
entrevista que sobra pequena é reaproveitada, ex: frente de gaveta) e o
`PlanoDeCorteService` persiste cada um como `Sobra`. O campo `sentidoVeio`
existe na entidade para permitir, no futuro, casar uma sobra com uma peça
nova respeitando o veio — mas fica sempre `null` no MVP: o próprio
marceneiro relatou que reaproveitamento entre projetos é raro na prática
dele, então implementar o casamento automático agora seria complexidade sem
uso real por trás. Decidir quais sobras usar em qual peça é, por ora, uma
decisão humana feita olhando o PDF/visualização.

## Cada geração de plano cria um registro novo — sem versionamento

Chamar `POST /plano-de-corte` de novo (ex: depois de editar uma peça) cria
um novo `PlanoDeCorte`, sem apagar nem "atualizar" o anterior. Não existe
comparação entre planos nem histórico de versões no MVP — a UI sempre
mostra o mais recente (`findFirstByProjetoIdOrderByGeradoEmDesc`). Ver
[`docs/adr/0002-plano-sem-versionamento.md`](adr/0002-plano-sem-versionamento.md)
para o raciocínio completo e quando isso deve mudar.

## Geração de PDF: OpenPDF, mesmo padrão do FlowOps

`PlanoDeCortePdfService` monta o documento com a API de baixo nível do
OpenPDF (`Document`/`PdfPTable`), no mesmo padrão do `BudgetPdfService` do
FlowOps e pelo mesmo motivo: sem justificativa de complexidade para um
motor HTML→PDF num documento de layout simples. Licença OpenPDF
(LGPL/MPL) escolhida por ser compatível com uso comercial sem custo,
diferente do iText 5+/7 (AGPL).

## Exclusão de Projeto é hard delete com cascade

Diferente do `Client`/`WorkOrder` do FlowOps (soft delete, porque um
cliente pode ter WorkOrders históricas que não devem sumir), aqui um
Projeto é uma unidade de trabalho fechada: excluir o projeto e perder
chapas/peças/planos associados é o comportamento esperado, não um risco de
perda de histórico de negócio. `ON DELETE CASCADE` no schema resolve isso
sem lógica adicional no Service.
