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

Uma chapa por espessura, por projeto (409 se já existir uma da mesma
espessura no projeto).

| Método | Rota | Descrição |
|---|---|---|
| `GET` | `/projetos/{projetoUuid}/chapas` | Lista chapas do projeto |
| `POST` | `/projetos/{projetoUuid}/chapas` | Cria chapa — `{ larguraMm, alturaMm, espessuraMm, quantidadeDisponivel, kerfMm?, margemBordaMm? }` |
| `PUT` | `/projetos/{projetoUuid}/chapas/{uuid}` | Atualiza chapa |
| `DELETE` | `/projetos/{projetoUuid}/chapas/{uuid}` | Remove chapa |

`espessuraMm` aceita `6`, `15`, `18` ou `25` (constraint do banco).
`kerfMm` padrão `4`, `margemBordaMm` padrão `6` se omitidos.

## Peças

| Método | Rota | Descrição |
|---|---|---|
| `GET` | `/projetos/{projetoUuid}/pecas` | Lista peças do projeto |
| `POST` | `/projetos/{projetoUuid}/pecas` | Cria peça — `{ nome, alturaMm, larguraMm, espessuraMm, quantidade, tipoAcabamento }` |
| `PUT` | `/projetos/{projetoUuid}/pecas/{uuid}` | Atualiza peça |
| `DELETE` | `/projetos/{projetoUuid}/pecas/{uuid}` | Remove peça |

`tipoAcabamento`: `LISO` (pode rotacionar no encaixe) ou `COM_VEIO` (mantém
sempre a orientação largura x altura informada).

## Plano de corte

| Método | Rota | Descrição |
|---|---|---|
| `POST` | `/projetos/{projetoUuid}/plano-de-corte` | Gera um novo plano (cria registro, não sobrescreve o anterior) |
| `GET` | `/projetos/{projetoUuid}/plano-de-corte` | Retorna o plano mais recente |
| `GET` | `/projetos/{projetoUuid}/plano-de-corte/pdf` | Exporta o plano mais recente em PDF (`application/pdf`) |

Erros possíveis ao gerar (`409 Conflict`):
- `"Projeto não tem peças cadastradas"`
- `"Não há chapa de Xmm cadastrada, mas o projeto tem peças dessa espessura"`
- `"Peça \"Nome\" (LxAmm) não cabe na área útil da chapa (LxAmm)"`
- `"Chapas insuficientes: seriam necessárias pelo menos N chapas, mas há apenas M disponíveis."`

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
