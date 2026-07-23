# Changelog

## [0.5.0] — Espaço pessoal + compartilhamento de projeto

Menos fricção e compartilhamento estilo Canva/Figma (ADR-0006):

- **Espaço pessoal automático:** no cadastro (local ou Google) cria-se uma
  organização pessoal em silêncio — o usuário cai direto em "criar projeto",
  sem tela de "criar empresa". Organização de verdade (com equipe) vira opcional
- **Compartilhamento direto de projeto**, sem organização: `ColaboradorProjeto`
  (EDITOR/VISUALIZADOR) + `ConviteProjeto`. `email` no convite = convite pessoal
  de uso único; sem e-mail = **link reutilizável** (vários usam até revogar).
  Tela pública `/convite/:token` ("Fulano te convidou para o projeto X")
- **"Compartilhados comigo"** na lista de projetos (à parte do workspace ativo)
- **Autorização por acesso** (`ProjetoService.nivelAcesso`): Membro da
  organização OU colaborador direto; VISUALIZADOR abre o projeto/plano em modo
  somente-leitura (edição bloqueada no backend e escondida na UI)
- Painel "Compartilhar" no projeto (gerar link, convidar por e-mail, listar
  pessoas com acesso, revogar)
- DDL + migração `0004_espaco_pessoal_e_compartilhamento.sql`
- Correção de design vs. a proposta original: link reutilizável não é
  "consumido" no primeiro aceite (só o convite por e-mail é de uso único)

## [0.4.0] — Contas, organizações (multi-tenant) e equipe

Preparação para deploy (ADR-0005):

- **Autenticação:** login por e-mail/senha (BCrypt) e por Google (OAuth2/OIDC,
  opcional), por sessão com cookie httpOnly + CSRF por cookie/header. Sem JWT
- **Multi-tenant:** `Usuario` global, `Organizacao` como tenant, `Membro` com
  papéis OWNER/ADMIN/MEMBRO. Todo `Projeto` pertence a uma organização; um
  usuário alterna entre organizações por um seletor de workspace
- **Escopo de acesso** aplicado num único ponto (`ProjetoService` pela
  organização ativa), protegendo peças/chapas/planos transitivamente — recurso
  de outra organização devolve 404
- **Backend:** entidades/repos de identidade, `SecurityConfig`,
  `OrganizacaoContexto`, `AuthController` e `OrganizacaoController` (criar
  organização, trocar ativa, gerenciar equipe); DDL + migração
  `0003_multi_tenant_auth.sql` + seed com conta demo (`demo@cutflow.app`)
- **Frontend:** telas de login/cadastro (+ botão Google condicional),
  onboarding de organização, seletor de workspace, tela de equipe, rotas
  protegidas; `axios` com cookies + CSRF; proxy do Vite para mesma origem
- **Docs:** ADR-0005, `docs/deploy.md` (recomendação de hospedagem), api.md e
  setup atualizados
- Migração para bancos existentes: `db/migrations/0003_multi_tenant_auth.sql`

## [0.3.0] — Correção veio/liso + plano em tempo real

Correções a partir do teste de uso real (feedback de 16/07/2026):

- **Correção de domínio (bug):** peça com veio e peça lisa da mesma espessura
  eram encaixadas na mesma chapa. O acabamento (liso/com veio) já vem de
  fábrica na chapa; agora `Chapa` é identificada por espessura **e**
  acabamento, o auto-provisionamento é por combinação e o plano agrupa por
  combinação — veio e liso nunca dividem chapa (ADR-0004)
- **Correção (bug):** após gerar um plano, remover peça falhava por FK
  (`posicionamentos.peca_id`) e o erro era engolido pelo frontend — o
  projeto ficava "travado" no primeiro plano. Planos agora são descartados
  em toda mutação de peça/chapa (dado derivado) + cascades de segurança no
  banco + erros de remoção exibidos na tela
- **Plano em tempo real:** o plano é regenerado automaticamente (debounce de
  600ms) a cada peça/chapa criada, editada ou removida; o botão "Gerar plano
  de corte" passa a ser um "forçar geração"
- **Exclusão de chapa** (`DELETE /chapas/{uuid}`), permitida quando não
  restam peças da combinação (409 explicativo caso contrário)
- **Edição e duplicação de peça** na tela do projeto (o PUT já existia na
  API; agora tem UI)
- Visualização e PDF mostram o acabamento de cada chapa ("15mm · com veio");
  resumo ao vivo inclui o total de chapas necessárias
- Testes novos: veio/liso nunca compartilham chapa; mutação de peça descarta
  planos antes de deletar
- Migração para bancos existentes:
  `db/migrations/0002_chapa_tipo_acabamento_e_cascades.sql`

## [0.2.0] — Chapa sem estoque e auto-provisionada (ADR-0003)

- `Chapa` perde `quantidadeDisponivel`; algoritmo abre quantas chapas forem
  necessárias, sem teto de negócio
- Chapa auto-provisionada por espessura ao salvar peça; sem criação manual
  (listagem com edição de largura/altura/kerf/margem)
- Resumo ao vivo de peças (total, liso/veio, por espessura) no frontend

## [0.1.0] — MVP

- Cadastro de Projeto, Chapa (por espessura) e Peça
- Algoritmo Guillotine de otimização (`OtimizadorDePlano` / `GuillotineOtimizadorDePlano`), isolado e testado (`GuillotineOtimizadorDePlanoTest`)
- Orquestração por espessura no `PlanoDeCorteService`, com numeração global de chapas e etiquetas
- Registro de sobras (`Sobra`) ao final do encaixe de cada chapa
- Exportação em PDF (`PlanoDeCortePdfService`, OpenPDF)
- Frontend PWA (React + Vite + Tailwind): lista de projetos, workspace de projeto com formulários de chapa/peça, visualização Canvas do plano gerado, exportação de PDF
- Schema PostgreSQL versionado (`db/cutflow_ddl.sql`) com projeto de demonstração (`db/seed.sql`)
- Ambiente containerizado com Docker Compose
