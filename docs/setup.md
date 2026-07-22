# Guia de instalação e configuração

## Pré-requisitos

Você só precisa de **uma** destas duas opções:

### Opção A — Docker (recomendado)
- [Docker Desktop](https://www.docker.com/products/docker-desktop/) instalado
  (inclui o Docker Compose)

### Opção B — Ambiente local (sem Docker)
- Java 21 ([Adoptium Temurin](https://adoptium.net/))
- Maven 3.9+
- Node.js 20+ e npm
- PostgreSQL 16 instalado localmente

Este guia foca na Opção A. A Opção B está detalhada na seção
[Rodando sem Docker](#rodando-sem-docker-desenvolvimento-local).

---

## Passo a passo — Docker Compose

### 1. Entre na pasta do projeto

```bash
cd CutFlow
```

### 2. Crie o arquivo de variáveis de ambiente

```bash
cp .env.example .env
```

Os valores padrão já funcionam sem alterar nada para rodar localmente.

### 3. Suba os três serviços

```bash
docker compose up --build
```

Isso vai:
1. Criar o container do PostgreSQL 16 e rodar o schema (`cutflow_ddl.sql`)
   seguido do projeto de demonstração (`seed.sql` — o mesmo exemplo de
   armário de cozinha usado na entrevista com o marceneiro-piloto),
   automaticamente, só na primeira vez que o volume é criado
2. Buildar a imagem do backend (Maven + Spring Boot) e subir na porta `8080`
3. Buildar a imagem do frontend (Vite dev server) e subir na porta `5173`

### 4. Confirme que os três serviços estão de pé

```bash
docker compose ps
```

Espere ver `db`, `backend` e `frontend` com status `running` (ou `healthy`
no caso do `db` e do `backend`).

### 5. Acesse o sistema

Abra **http://localhost:5173** no navegador e entre com a conta de
demonstração (ADR-0005):

- **E-mail:** `demo@cutflow.app`
- **Senha:** `demo1234`

Ela já é dona da organização "Marcenaria Demo", onde o projeto de demonstração
("Armário Cozinha João") aparece na lista — abra-o e clique em "Gerar plano de
corte" para ver o algoritmo de ponta a ponta. Você também pode criar sua
própria conta em "Criar conta" e, no onboarding, criar sua marcenaria.

### 6. Para encerrar

```bash
docker compose down
```

Isso mantém o volume do banco intacto. Para apagar tudo e recomeçar do zero:

```bash
docker compose down -v
```

---

## Variáveis de ambiente

| Variável | Descrição | Padrão |
|---|---|---|
| `DB_NAME` / `DB_USER` / `DB_PASSWORD` | Credenciais do PostgreSQL | `cutflow` |
| `CORS_ALLOWED_ORIGINS` | Origem(ns) permitida(s) para o backend | `http://localhost:5173` |
| `FRONTEND_URL` | Para onde o backend redireciona após login Google | `http://localhost:5173` |
| `VITE_API_URL` | Base da API no frontend (relativa, mesma origem) | `/api/v1` |
| `VITE_PROXY_TARGET` | Alvo do proxy do Vite para o backend | `http://backend:8080` (compose) |
| `SESSION_COOKIE_SECURE` | Cookie de sessão só via HTTPS (`true` em prod) | `false` |
| `SESSION_COOKIE_SAMESITE` | Política SameSite do cookie de sessão | `lax` |
| `GOOGLE_CLIENT_ID` / `GOOGLE_CLIENT_SECRET` | Credenciais do login Google (opcional) | vazio |

Publicação em produção (HTTPS, mesma origem, hospedagem) em
[`deploy.md`](deploy.md).

---

## Rodando sem Docker (desenvolvimento local)

### Banco de dados

```bash
createdb cutflow
psql -d cutflow -f db/cutflow_ddl.sql
psql -d cutflow -f db/seed.sql
```

### Backend

```bash
cd backend

export DB_HOST=localhost
export DB_PORT=5432
export DB_NAME=cutflow
export DB_USER=cutflow
export DB_PASSWORD=cutflow

mvn spring-boot:run
```

A API sobe em `http://localhost:8080`.

### Frontend

```bash
cd frontend
npm install
npm run dev
```

O Vite sobe em `http://localhost:5173` e faz proxy de `/api`, `/oauth2` e do
callback do Google para `http://localhost:8080` (`VITE_PROXY_TARGET`), para que
o navegador veja SPA e API na mesma origem — necessário para os cookies de
sessão (ADR-0005).

---

## Solução de problemas

**Não consigo logar / cai de volta no login (sessão não "gruda")**
Login por sessão exige mesma origem. Em dev, acesse pela porta do frontend
(`http://localhost:5173`), não pela do backend, e deixe o proxy do Vite fazer o
resto. Em produção, sirva SPA e API atrás do mesmo domínio (ver
[`deploy.md`](deploy.md)).

**403 ao salvar/criar algo, mas o login funcionou (erro de CSRF)**
A SPA busca o cookie `XSRF-TOKEN` (`GET /auth/csrf`) antes do primeiro POST e o
axios reenvia no header `X-XSRF-TOKEN`. Se estiver chamando a API por fora da
SPA, replique esse par cookie/header.

**Botão "Entrar com Google" não aparece**
É proposital quando `GOOGLE_CLIENT_ID` está vazio — só o login local funciona.
Configure as credenciais (ver [`deploy.md`](deploy.md)) para habilitar.

**`backend` fica em loop de restart**
Veja os logs (`docker compose logs backend`). Na maioria das vezes é o
backend subindo antes do Postgres aceitar conexões — o `depends_on` com
`condition: service_healthy` já deveria evitar isso, mas se acontecer, rode
`docker compose up` novamente sem `--build`.

**Erro de CORS no console do navegador**
Confirme que `CORS_ALLOWED_ORIGINS` no `.env` bate exatamente com a URL que
você está usando no navegador (incluindo `http://` e a porta).

**O plano separa as peças em mais chapas do que eu esperava**
É proposital: o sistema nunca mistura espessuras diferentes nem acabamentos
diferentes (liso / com veio) numa mesma chapa física — o acabamento já vem
de fábrica na chapa (ver ADR-0004). Confira o acabamento cadastrado em cada
peça se o número de chapas parecer alto.

**"Ainda existem peças de Xmm (...) neste projeto" ao excluir chapa**
Uma chapa só pode ser excluída quando nenhuma peça daquela combinação
espessura+acabamento existe mais — senão ela seria recriada automaticamente
no próximo plano. Remova (ou mude a espessura/acabamento) das peças antes.

**Banco criado numa versão anterior**
Aplique as migrações em `db/migrations/` na ordem
(`0002_chapa_tipo_acabamento_e_cascades.sql`, depois
`0003_multi_tenant_auth.sql`) ou, em desenvolvimento, recrie o volume:
`docker compose down -v && docker compose up --build`.

**`docker compose logs` retorna "no configuration file provided: not found"**
O comando foi rodado fora da pasta do projeto — entre na pasta `CutFlow`
antes de rodar qualquer comando.
