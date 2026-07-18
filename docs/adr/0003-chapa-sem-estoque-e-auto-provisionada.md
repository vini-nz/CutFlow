# ADR-0003: Chapa deixa de ter "quantidade disponível" e de exigir cadastro manual

## Status
Aceita

## Contexto
A versão inicial do MVP exigia que o marceneiro-piloto cadastrasse uma
`Chapa` (largura, altura, espessura, **quantidade disponível**, kerf,
margem) antes de poder gerar qualquer plano de corte. O campo
`quantidadeDisponivel` era usado pelo algoritmo (`GuillotineOtimizadorDePlano`)
como um teto rígido: se o encaixe precisasse de mais chapas do que o
número informado, o sistema recusava gerar o plano.

Ao mostrar essa primeira versão para ele, o feedback foi direto:
- Ele sempre usa a mesma medida de chapa — ter que reinformar isso é
  repetitivo.
- "Quantidade disponível" não faz sentido para o fluxo dele: ele não
  trabalha com estoque (confirmado também na entrevista original, doc
  seção 3.1/3.5). O propósito do plano de corte é justamente **descobrir**
  quantas chapas ele precisa comprar — pedir para ele adivinhar um teto
  antes inverte o valor central do produto (doc seção 0).
- Ele confirmou que gostaria de manter uma listagem de chapas (não
  eliminar o conceito por completo), com liberdade para editar
  largura/altura, kerf e margem quando quiser — mesmo que venham com
  valor padrão.

Essa é exatamente a inconsistência já registrada na documentação de
fundamentos (seção 11, item 2: "Chapa mistura catálogo e estoque") se
manifestando em uso real.

## Decisão
1. `Chapa` perde o campo `quantidadeDisponivel` (entidade, DTOs, DDL,
   algoritmo). O otimizador (`GuillotineOtimizadorDePlano`) não tem mais
   teto de negócio — ele abre quantas chapas forem necessárias para
   encaixar todas as peças. Existe apenas uma guarda defensiva
   (`MAX_CHAPAS_SEGURANCA = 500`) contra bug de loop infinito, nunca uma
   regra de produto.
2. `Chapa` não é mais criada manualmente pelo usuário. Ela é
   **auto-provisionada** com valores padrão (1840x2740mm, kerf 4mm, margem
   6mm) por `ChapaService.garantirChapaParaEspessura`, chamada por
   `PecaService` no momento em que a primeira peça de uma determinada
   espessura é salva. `PlanoDeCorteService` também chama esse método como
   rede de segurança.
3. A listagem de chapas continua visível na UI (uma por espessura em uso),
   mas sem formulário de criação — só um botão "Editar" por chapa, para
   quem quiser ajustar largura, altura, kerf ou margem manualmente.
   `ChapaController` perde os endpoints `POST` e `DELETE`; mantém `GET`
   (listagem) e `PUT` (edição).

## Consequências
- Fluxo principal do marceneiro fica: adicionar peças → ver o resumo ao
  vivo (contagem por tipo/espessura) → gerar plano → ver quantas chapas
  são necessárias. Nenhum cadastro prévio de chapa é exigido.
- Quem quiser um tamanho de chapa diferente do padrão, ou ajustar
  kerf/margem, ainda pode — via edição, não via um formulário obrigatório
  no caminho crítico.
- Banco de dados existente precisa de uma migração manual (não há
  Flyway/Liquibase neste projeto — DDL aplicado manualmente, ver
  `application.yml`): `ALTER TABLE chapas DROP COLUMN quantidade_disponivel;`
- Testes de unidade que verificavam o teto de chapas foram substituídos por
  um teste que confirma que o otimizador abre quantas chapas forem
  necessárias, sem lançar erro.

## Alternativas consideradas
- **Remover completamente o conceito de Chapa da UI** (proposta inicial
  antes desta conversa) — descartada a pedido do próprio marceneiro: ele
  quer a flexibilidade de ver e editar os parâmetros quando precisar,
  mesmo usando o padrão na maior parte do tempo.
- **Manter `quantidadeDisponivel` como informativo, sem função de teto** —
  descartada: o campo não tem nenhum significado real no fluxo dele (ele
  não guarda estoque), então mantê-lo como número exibido só geraria
  confusão sem utilidade.
