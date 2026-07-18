# ADR-0004 — Acabamento identifica a chapa; plano de corte é descartável

- **Status:** aceita
- **Data:** 2026-07-17
- **Contexto:** correções após teste de uso real da versão apresentada ao
  marceneiro-piloto (feedback de 2026-07-16)

## Contexto

Dois problemas foram encontrados no teste de uso da versão anterior:

1. **Peça com veio e peça lisa eram encaixadas na mesma chapa.** A modelagem
   tratava `tipoAcabamento` como um atributo apenas da Peça (controlando só a
   rotação no encaixe), e o plano agrupava peças por espessura. Isso está
   errado no domínio: o acabamento (liso / com veio) **já vem de fábrica na
   chapa** — não é aplicado depois na peça. Uma peça com veio só pode sair de
   uma chapa com veio, e uma lisa de uma chapa lisa. Misturá-las num mesmo
   plano de chapa produz um plano fisicamente sem sentido.

2. **Depois de gerar um plano, o projeto "travava".** `posicionamentos`
   referencia `pecas` sem cascade, então remover uma peça que aparecia num
   plano antigo violava a FK; o frontend engolia o erro e o usuário ficava
   preso ao primeiro plano gerado, sem conseguir alterar peças para comparar
   resultados.

## Decisão

1. **`Chapa` passa a ser identificada por (projeto, espessura, acabamento).**
   - `chapas.tipo_acabamento` (`LISO` / `COM_VEIO`), com unique
     `uq_chapas_projeto_espessura_acabamento`.
   - O auto-provisionamento (ADR-0003) passa a ser por combinação:
     `ChapaService.garantirChapa(projetoId, projeto, espessura, acabamento)`.
   - O plano agrupa peças por (espessura, acabamento) e empacota cada grupo
     na chapa da mesma combinação. Grupos nunca se misturam.
   - O acabamento da chapa não é editável (identidade, como a espessura);
     mudar o acabamento de uma peça cria/usa a chapa da outra combinação.

2. **Plano de corte é dado derivado e descartável.** Toda mutação de peça
   (criar/editar/remover) ou de chapa (editar/excluir) **descarta os planos
   já gerados do projeto** (`PlanoDeCorteRepository.deleteByProjetoId`). O
   frontend regenera o plano automaticamente (debounce de 600ms) após cada
   mudança — o botão "Gerar plano de corte" vira um "forçar geração".
   - Reforço no banco: `posicionamentos.peca_id` e
     `chapas_utilizadas.chapa_id` ganham `ON DELETE CASCADE` como rede de
     segurança — um plano obsoleto nunca pode bloquear a exclusão de uma
     peça/chapa.

3. **Exclusão de chapa** é permitida apenas quando não restam peças da
   combinação (senão ela seria recriada em silêncio no próximo plano, o que
   desfaz a exclusão de forma confusa). O backend devolve 409 com mensagem
   explicando o que fazer.

## Consequências

- O bug de domínio morre na raiz: veio e liso nunca dividem chapa, e o
  número de chapas reportado passa a refletir a compra real (chapas com veio
  e lisas são compradas separadamente).
- Projetos com peças 15mm liso + 15mm veio passam a consumir no mínimo 2
  chapas — aproveitamento global pode cair em relação ao plano antigo, mas o
  plano antigo era irrealizável.
- Sem versionamento de plano (ADR-0002 continua): o plano persistido é
  sempre o retrato do estado atual do projeto; histórico/comparação fica
  para V2.
- Migração para bancos existentes:
  `db/migrations/0002_chapa_tipo_acabamento_e_cascades.sql` (ou
  `docker compose down -v` em desenvolvimento).

## Alternativas consideradas

- **Manter acabamento só na peça e separar grupos "implicitamente" no
  algoritmo** — resolveria o encaixe, mas deixaria a listagem de chapas
  mentindo (uma chapa 15mm "neutra" para dois materiais fisicamente
  diferentes) e o total de chapas sem correspondência com a compra.
- **Cascade sem invalidação em serviço** — planos antigos ficariam com
  "buracos" (peças removidas sumindo do desenho) e totais errados; pior que
  não ter plano.
- **Bloquear exclusão de peça enquanto houver plano** — inverte a
  prioridade: o plano existe para servir as peças, não o contrário.
