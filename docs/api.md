# Referência da API

Base URL: `/api/v1` (mesma origem da SPA, via proxy — ver
[ADR-0005](adr/0005-multi-tenant-e-autenticacao.md)).

**Autenticação por sessão** (cookie `JSESSIONID` httpOnly). Toda rota fora de
`/auth/login|register|csrf|config` e do fluxo OAuth exige login e devolve
`401` se não houver sessão. Requisições que alteram estado (POST/PUT/DELETE)
exigem o header CSRF `X-XSRF-TOKEN` com o valor do cookie `XSRF-TOKEN` (o
cliente axios já faz isso automaticamente; obtenha o cookie com
`GET /auth/csrf` antes do primeiro POST).

Tudo é escopado pela **organização ativa** do usuário: um recurso de outra
organização devolve `404`. Respostas de erro seguem o formato:

```json
{ "status": 409, "error": "Conflict", "message": "...", "timestamp": "..." }
```

## Autenticação

| Método | Rota | Descrição |
|---|---|---|
| `GET` | `/auth/csrf` | Emite o cookie `XSRF-TOKEN` (204). Chame antes do primeiro POST |
| `GET` | `/auth/config` | `{ googleHabilitado }` — se o login Google está ativo |
| `POST` | `/auth/register` | Cria conta local — `{ nome, email, senha }` (senha ≥ 8) |
| `POST` | `/auth/login` | Login local — `{ email, senha }`; abre sessão e retorna a sessão |
| `POST` | `/auth/logout` | Encerra a sessão (204) |
| `GET` | `/auth/me` | Sessão atual (usuário, organizações, organização ativa) |
| — | `/oauth2/authorization/google` | Início do login Google (navegação de página, não XHR) |

`401` em `/auth/login` = credenciais inválidas.

## Organizações (workspaces) e equipe

| Método | Rota | Descrição |
|---|---|---|
| `GET` | `/organizacoes` | Organizações das quais o usuário é membro (com o papel dele) |
| `POST` | `/organizacoes` | Cria organização — `{ nome, documento? }`; o criador vira OWNER e ela fica ativa |
| `POST` | `/organizacoes/{uuid}/ativar` | Troca o workspace ativo |
| `GET` | `/organizacoes/{uuid}/membros` | Lista a equipe (qualquer membro) |
| `POST` | `/organizacoes/{uuid}/membros` | Adiciona membro — `{ email, papel? }` (OWNER/ADMIN; a pessoa precisa já ter conta) |
| `DELETE` | `/organizacoes/{uuid}/membros/{membroUuid}` | Remove membro (OWNER/ADMIN; não remove o OWNER) |

Papéis: `OWNER`, `ADMIN`, `MEMBRO`. `403` quando falta permissão.

---

## Projetos

| Método | Rota | Descrição |
|---|---|---|
| `GET` | `/projetos?page=0&size=20` | Lista projetos da organização ativa, paginado |
| `GET` | `/projetos/compartilhados` | Projetos compartilhados diretamente comigo (ADR-0006), com `podeEditar` |
| `GET` | `/projetos/{uuid}` | Detalhe de um projeto (inclui `podeEditar`) |
| `POST` | `/projetos` | Cria projeto na organização ativa — `{ nome, cliente }` |
| `PUT` | `/projetos/{uuid}` | Atualiza projeto (exige edição) |
| `DELETE` | `/projetos/{uuid}` | Remove projeto (exige edição; cascata: chapas, peças, planos, colaboradores) |

`podeEditar` = o usuário pode editar (Membro da organização ou colaborador
EDITOR) ou só visualizar (colaborador VISUALIZADOR). Mutar sem edição = `403`.

## Compartilhamento de projeto (ADR-0006)

Gestão exige acesso de **edição** ao projeto. `email` vazio no convite gera um
**link reutilizável**; preenchido gera um **convite de uso único** para o e-mail.

| Método | Rota | Descrição |
|---|---|---|
| `GET` | `/projetos/{projetoUuid}/colaboradores` | Pessoas com acesso direto |
| `DELETE` | `/projetos/{projetoUuid}/colaboradores/{uuid}` | Remove um colaborador |
| `GET` | `/projetos/{projetoUuid}/convites` | Convites/links ativos (com `urlConvite`) |
| `POST` | `/projetos/{projetoUuid}/convites` | Cria convite/link — `{ email?, papel }` (`EDITOR`/`VISUALIZADOR`) |
| `DELETE` | `/projetos/{projetoUuid}/convites/{uuid}` | Revoga um convite/link |
| `GET` | `/convites/{token}` | **Público** — detalhes do convite ("Fulano te convidou...") |
| `POST` | `/convites/{token}/aceitar` | Aceita (exige login); retorna `{ projetoUuid }` |

## Chapas

Uma chapa por combinação **espessura + acabamento**, por projeto
(ADR-0004). Não há criação manual: a chapa é auto-provisionada com valores
padrão (1840x2740mm, kerf 4mm, margem 6mm) quando a primeira peça da
combinação é salva (ADR-0003).

| Método | Rota | Descrição |
|---|---|---|
| `GET` | `/projetos/{projetoUuid}/chapas` | Lista chapas do projeto (espessura asc, LISO antes de COM_VEIO) |
| `PUT` | `/projetos/{projetoUuid}/chapas/{uuid}` | Edita largura/altura/kerf/margem — `{ larguraMm, alturaMm, espessuraMm, kerfMm?, margemBordaMm? }` (espessura e acabamento não mudam; descarta planos gerados) |
| `DELETE` | `/projetos/{projetoUuid}/chapas/{uuid}` | Exclui a chapa — `409` se ainda existirem peças da combinação |

`espessuraMm` aceita `6`, `15`, `18` ou `25` (constraint do banco).
`tipoAcabamento` (`LISO`/`COM_VEIO`) aparece na resposta e identifica a
chapa junto com a espessura.

## Peças

| Método | Rota | Descrição |
|---|---|---|
| `GET` | `/projetos/{projetoUuid}/pecas` | Lista peças do projeto |
| `POST` | `/projetos/{projetoUuid}/pecas` | Cria peça — `{ nome, alturaMm, larguraMm, espessuraMm, quantidade, tipoAcabamento }` |
| `PUT` | `/projetos/{projetoUuid}/pecas/{uuid}` | Atualiza peça |
| `DELETE` | `/projetos/{projetoUuid}/pecas/{uuid}` | Remove peça |

`tipoAcabamento`: `LISO` (pode rotacionar no encaixe) ou `COM_VEIO` (mantém
sempre a orientação largura x altura informada). O acabamento também define
de qual chapa a peça sai: peça com veio nunca entra em chapa lisa
(ADR-0004).

Criar, editar ou remover uma peça **descarta os planos de corte já gerados**
do projeto (o plano é dado derivado — o frontend regenera automaticamente).

## Plano de corte

| Método | Rota | Descrição |
|---|---|---|
| `POST` | `/projetos/{projetoUuid}/plano-de-corte` | Gera um novo plano — um grupo de chapas por combinação espessura+acabamento |
| `GET` | `/projetos/{projetoUuid}/plano-de-corte` | Retorna o plano mais recente (404 se nenhum plano válido existe) |
| `GET` | `/projetos/{projetoUuid}/plano-de-corte/pdf` | Exporta o plano mais recente em PDF (`application/pdf`) |

Erros possíveis ao gerar (`409 Conflict`):
- `"Projeto não tem peças cadastradas"`
- `"Peça \"Nome\" (LxAmm) não cabe na área útil da chapa (LxAmm)"`

### Exemplo de resposta — `GET /plano-de-corte`

```json
{
  "uuid": "...",
  "totalChapasUtilizadas": 2,
  "percentualAproveitamento": 84.30,
  "percentualDesperdicio": 15.70,
  "geradoEm": "2026-07-15T23:10:00Z",
  "chapas": [
    {
      "uuid": "...",
      "numeroChapa": 1,
      "larguraMm": 1840,
      "alturaMm": 2740,
      "espessuraMm": 15,
      "tipoAcabamento": "COM_VEIO",
      "areaDesperdicadaMm2": 612340,
      "percentualAproveitamento": 87.80,
      "posicionamentos": [
        {
          "pecaUuid": "...",
          "nomePeca": "Lateral",
          "numeroEtiqueta": 1,
          "xMm": 6, "yMm": 6,
          "larguraMm": 550, "alturaMm": 2200,
          "rotacionada": false
        }
      ],
      "sobras": [
        { "xMm": 562, "yMm": 6, "larguraMm": 1272, "alturaMm": 534 }
      ]
    }
  ]
}
```
