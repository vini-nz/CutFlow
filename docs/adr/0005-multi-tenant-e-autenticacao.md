# ADR-0005 — Multi-tenant, autenticação e organização de acesso

- **Status:** aceita
- **Data:** 2026-07-20
- **Contexto:** preparação para hospedar o CutFlow para uso real, antes do deploy

## Contexto

Até aqui o CutFlow não tinha login (ADR-0001): qualquer pessoa com a URL via e
editava tudo. Isso foi correto para validar o produto, mas não pode ir para
produção. Além do login, o dono quer poder **expandir para várias marcenarias**
no futuro e **compartilhar projetos com a equipe**, com níveis de acesso.

## Decisão

### 1. Modelo multi-tenant: um login, várias organizações

- `Usuario` é **global** (não pertence a uma marcenaria). `Organizacao` é o
  **tenant**. O vínculo é `Membro (usuario, organizacao, papel)`.
- Todo `Projeto` pertence a uma `Organizacao`. Um usuário só enxerga projetos
  das organizações das quais é membro.
- O usuário escolhe a **organização ativa** (o "workspace") por um seletor no
  topo; a escolha fica na sessão. Isso permite a mesma pessoa participar de
  várias marcenarias e alternar entre elas — e abre caminho para o CutFlow
  virar SaaS para outras marcenarias sem redesenho.
- Optou-se pela abordagem "conta de usuário + organizações das quais é membro"
  (e não "login preso a um CNPJ"): é mais maleável, é o padrão de apps
  conhecidos, e cobre tanto o dono sozinho quanto equipe compartilhada.

### 2. Papéis (`PapelMembro`): OWNER, ADMIN, MEMBRO

- **OWNER**: dono, único por organização (quem a criou), não removível.
- **ADMIN**: gerencia equipe e projetos.
- **MEMBRO**: usa/edita projetos, sem gerenciar equipe.

### 3. Autenticação: sessão (sem JWT), login local + Google

- **Sessão** com cookie `JSESSIONID` httpOnly (Spring Security), não JWT.
  Para uma SPA de mesmo domínio é mais simples e mais seguro — não há token
  acessível a JavaScript (menos superfície a XSS). CSRF é mitigado por cookie
  `XSRF-TOKEN` legível + header `X-XSRF-TOKEN`.
- **Login local** (e-mail/senha, BCrypt) e **login Google** (OAuth2/OIDC).
  O Google é opcional em runtime: sem `GOOGLE_CLIENT_ID` o backend sobe e só o
  login local funciona. Um mesmo e-mail é uma única conta (Google vincula-se a
  um cadastro local pré-existente pelo e-mail).

### 4. Aplicação do escopo num único ponto

- Como `PecaService`, `ChapaService` e `PlanoDeCorteService` sempre resolvem o
  projeto por `ProjetoService.findOrThrow`, **escopar só o `ProjetoService`**
  pela organização ativa protege peças, chapas e planos transitivamente. Um
  `uuid` de outra organização devolve **404** (não confirma existência).
- O `OrganizacaoContexto` resolve, por requisição, o usuário logado e a
  organização ativa, **revalidando a pertinência** a cada chamada.

### 5. Mesma origem (cookies)

- Login por sessão exige que a SPA e a API sejam vistas como **mesma origem**.
  Em dev, o Vite faz proxy de `/api`, `/oauth2` e do callback do Google para o
  backend. Em produção, servir frontend e backend atrás do **mesmo domínio**
  (reverse proxy). Assim o cookie é first-party e `SameSite=Lax` basta.

## Consequências

- Reversão da ADR-0001 (sem autenticação) — agora explicitamente substituída.
- Migração: `db/migrations/0003_multi_tenant_auth.sql` cria as tabelas, uma
  organização/usuário "de migração" e vincula os projetos existentes a ela
  (senão `organizacao_id NOT NULL` quebraria). Em dev, `docker compose down -v`.
- Conta de demonstração no seed: `demo@cutflow.app` / `demo1234` (trocar em
  produção).

## Limitações conscientes (candidatas a melhoria)

- **Adicionar membro exige que a pessoa já tenha conta** no CutFlow (por
  e-mail). Convite por e-mail para quem ainda não tem conta fica para depois.
- **Papéis são por organização, não por projeto.** O pedido mencionou
  "níveis de acesso dentro do próprio projeto"; isso é uma evolução natural
  (ACL por projeto) sobre este modelo, sem redesenho.
- **Sem "esqueci minha senha"** nesta iteração (reset por e-mail depende de um
  provedor de e-mail configurado).

## Alternativas consideradas

- **JWT em vez de sessão** — mais comum para SPA+API cross-domain, mas guarda
  token em JS (risco a XSS) e exige refresh/expiração. Para uma SPA de mesmo
  domínio, sessão httpOnly é mais simples e segura.
- **Login preso a CNPJ da empresa** (usuario+senha+CNPJ) — rejeitado: menos
  maleável, não cobre uma pessoa em várias marcenarias, e prende a conta a um
  dado fiscal que é opcional.
