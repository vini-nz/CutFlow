# Publicação em produção

Este guia cobre o que muda ao sair do `docker compose` local para um ambiente
real, e a recomendação de hospedagem.

## Princípio: frontend e backend na MESMA origem

O login é por **sessão** (cookie httpOnly). Para o cookie ser first-party e o
CSRF funcionar sem dor, a SPA e a API devem responder no **mesmo domínio**
(ex.: `https://app.suamarcenaria.com.br` serve a SPA e faz proxy de `/api`,
`/oauth2` e `/login/oauth2` para o backend). Um reverse proxy (Caddy, Nginx ou
Traefik) resolve isso. Evite frontend e backend em domínios diferentes — exige
`SameSite=None` + HTTPS e complica CSRF/CORS sem ganho real aqui.

## Checklist de segurança pré-deploy

- [ ] **HTTPS obrigatório** (a maioria dos hosts entrega de graça; com Caddy é
      automático). Depois, `SESSION_COOKIE_SECURE=true`.
- [ ] **Segredos só em variáveis de ambiente do host** — `DB_PASSWORD`,
      `GOOGLE_CLIENT_SECRET`. Nunca no `.env` commitado (já está no
      `.gitignore`).
- [ ] **Trocar a conta demo/migração** (`demo@cutflow.app` /
      `admin@cutflow.local`, senha `demo1234`): crie sua conta real e remova/
      altere as de exemplo.
- [ ] `CORS_ALLOWED_ORIGINS` e `FRONTEND_URL` = domínio real (não localhost).
- [ ] **Backup automático do Postgres** (ver o que o host oferece).
- [ ] Rodar a migração `db/migrations/0003_multi_tenant_auth.sql` se o banco já
      existir (senão o schema novo do DDL cobre uma base zerada).

## Variáveis de ambiente (produção)

| Variável | Exemplo |
|---|---|
| `DB_NAME`, `DB_USER`, `DB_PASSWORD` | credenciais do Postgres gerenciado |
| `CORS_ALLOWED_ORIGINS` | `https://app.suamarcenaria.com.br` |
| `FRONTEND_URL` | `https://app.suamarcenaria.com.br` |
| `SESSION_COOKIE_SECURE` | `true` |
| `SESSION_COOKIE_SAMESITE` | `lax` (mesma origem) |
| `GOOGLE_CLIENT_ID` / `GOOGLE_CLIENT_SECRET` | credenciais OAuth (opcional) |

### Login Google (opcional)

No [Google Cloud Console](https://console.cloud.google.com/), crie um OAuth 2.0
Client ID (Web application) e cadastre o **Authorized redirect URI**:

- Dev: `http://localhost:5173/login/oauth2/code/google`
- Prod: `https://app.suamarcenaria.com.br/login/oauth2/code/google`

## Recomendação de hospedagem

O CutFlow são três peças: **Postgres**, **backend Spring Boot** e **frontend**.
Duas rotas fazem sentido para o estágio atual (uma marcenaria, uso real):

### Opção A — VPS pequeno com o docker-compose (recomendada para custo/controle)

Um servidor pequeno (ex.: **Hetzner CX22 ~€4/mês**, DigitalOcean/Contabo
equivalentes) rodando o `docker-compose` que você **já tem funcionando**, com
**Caddy** na frente para HTTPS automático e para servir a SPA + proxy da API na
mesma origem.

- **Prós:** mais barato, sempre ligado (sem "cold start"), você já domina o
  docker-compose, tudo num lugar só.
- **Contras:** você administra o servidor (atualizações do SO, backup do
  Postgres — configure um `pg_dump` agendado ou snapshot do provedor).
- É o passo mais natural a partir de onde o projeto está hoje.

### Opção B — Plataforma gerenciada (recomendada para zero-ops)

**Railway** ou **Render**: conectam ao seu repositório GitHub (que já existe),
sobem backend + Postgres gerenciado + frontend, com HTTPS e deploy automático a
cada push.

- **Prós:** quase nada de administração; Postgres gerenciado com backup;
  bom para focar no produto.
- **Contras:** custo mensal cresce com os serviços; no plano gratuito o serviço
  "dorme" (cold start ruim para uso real) — para um cliente pagante, use um
  plano pago always-on.

**Sugestão prática:** comece pela **Opção A (VPS + Caddy)** — é a mais barata,
sempre ligada e aproveita o que já está pronto. Migre para a Opção B se
quiser parar de administrar servidor conforme crescer para mais marcenarias.

### Exemplo de Caddyfile (Opção A)

```
app.suamarcenaria.com.br {
    handle /api/*        { reverse_proxy backend:8080 }
    handle /oauth2/*     { reverse_proxy backend:8080 }
    handle /login/oauth2/* { reverse_proxy backend:8080 }
    handle               { reverse_proxy frontend:5173 }
}
```

Para produção "de verdade", troque o frontend do modo dev (`npm run dev`) por um
build estático (`npm run build`) servido pelo próprio Caddy — fica mais leve e
rápido. O dev server atual funciona, mas não é o ideal para produção.
