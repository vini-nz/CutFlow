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

Abra **http://localhost:5173** no navegador. O projeto de demonstração
("Armário Cozinha João") já aparece na lista — abra-o e clique em "Gerar
plano de corte" para ver o algoritmo funcionando de ponta a ponta.

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
| `CORS_ALLOWED_ORIGINS` | Origem permitida para chamadas ao backend | `http://localhost:5173` |
| `VITE_API_URL` | URL base da API usada pelo frontend | `http://localhost:8080/api/v1` |

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

O Vite sobe em `http://localhost:5173` e já aponta para
`http://localhost:8080/api/v1` por padrão.

---

## Solução de problemas

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
Aplique `db/migrations/0002_chapa_tipo_acabamento_e_cascades.sql` ou, em
desenvolvimento, recrie o volume: `docker compose down -v && docker compose
up --build`.

**`docker compose logs` retorna "no configuration file provided: not found"**
O comando foi rodado fora da pasta do projeto — entre na pasta `CutFlow`
antes de rodar qualquer comando.
