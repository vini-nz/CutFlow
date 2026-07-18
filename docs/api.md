# Referência da API

Base URL: `http://localhost:8080/api/v1`

Sem autenticação no MVP (ver [ADR-0001](adr/0001-sem-autenticacao-no-mvp.md)).
Todas as respostas de erro seguem o formato:

```json
{ "status": 409, "error": "Conflict", "message": "...", "timestamp": "..." }
```

---

## Projetos

| Método | Rota | Descrição |
|---|---|---|
| `GET` | `/projetos?page=0&size=20` | Lista projetos, paginado, mais recentes primeiro |
| `GET` | `/projetos/{uuid}` | Detalhe de um projeto |
| `POST` | `/projetos` | Cria projeto — `{ nome, cliente }` |
| `PUT` | `/projetos/{uuid}` | Atualiza projeto |
| `DELETE` | `/projetos/{uuid}` | Remove projeto (cascata: chapas, peças e planos junto) |

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
