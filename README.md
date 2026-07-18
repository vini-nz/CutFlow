# CutFlow

Gerador de plano de corte para marcenarias.

O CutFlow calcula, a partir das peças de um móvel e das chapas disponíveis,
a melhor forma de distribuir essas peças sobre as chapas para reduzir o
desperdício de material — o clássico *2D Cutting Stock Problem* aplicado ao
dia a dia de uma marcenaria pequena. Nasceu de uma entrevista com um
marceneiro-piloto real (roteiro completo e decisões de domínio em
[`PlanoDeCorte_Documentacao_Fundamentos.md`](PlanoDeCorte_Documentacao_Fundamentos.md)),
e foi pensado tanto para uso real com esse parceiro quanto como portfólio
técnico — mesmo padrão de engenharia usado no [FlowOps](../FlowOps).

---

## Funcionalidades

- Cadastro de projetos, chapas (por espessura) e peças
- Geração automática do plano de corte com heurística **Guillotine**
  (cortes retos de ponta a ponta, reproduzindo a esquadrejadeira manual),
  respeitando kerf, margem de borda, ordem de corte (peças mais compridas
  primeiro) e rotação condicional (peças "com veio" nunca giram)
- Visualização gráfica interativa das chapas (Canvas HTML5), peça por peça,
  numerada e colorida
- Registro de sobras aproveitáveis de cada chapa
- Relatório com nº de chapas necessárias, % de aproveitamento e % de
  desperdício
- Exportação do plano em PDF, para uso na oficina sem depender de internet
- Frontend como PWA responsivo (instalável, desktop e celular)

## Tecnologias

**Backend** — Java 21 · Spring Boot 3 · Spring Data JPA · PostgreSQL · OpenPDF

**Frontend** — React 18 · Vite · Tailwind CSS · Axios · Canvas HTML5

**Infraestrutura** — Docker · Docker Compose

---

## Executando o projeto

Pré-requisito: [Docker Desktop](https://www.docker.com/products/docker-desktop/)

```bash
cd CutFlow
cp .env.example .env
docker compose up --build
```

A aplicação fica disponível em:

| Serviço | URL |
|---|---|
| Frontend | http://localhost:5173 |
| Backend (API) | http://localhost:8080 |

Guia completo de instalação, variáveis de ambiente e solução de problemas em
[`docs/setup.md`](docs/setup.md).

---

## Estrutura

```
CutFlow/
├── backend/     Spring Boot (Java 21) — entities, algoritmo Guillotine, PDF
├── frontend/    React + Vite + Tailwind — PWA
├── db/          Schema PostgreSQL e projeto de demonstração
├── docs/        Documentação técnica
└── docker-compose.yml
```

## Documentação

| Documento | Conteúdo |
|---|---|
| [`PlanoDeCorte_Documentacao_Fundamentos.md`](PlanoDeCorte_Documentacao_Fundamentos.md) | Visão de produto, análise de mercado, entrevista com o marceneiro-piloto, modelagem de domínio, roadmap |
| [`docs/setup.md`](docs/setup.md) | Instalação detalhada, variáveis de ambiente, execução local sem Docker, solução de problemas |
| [`docs/architecture.md`](docs/architecture.md) | Decisões arquiteturais e o porquê de cada uma |
| [`docs/adr/`](docs/adr/) | Registros formais de decisões de arquitetura (ADRs) |
| [`docs/api.md`](docs/api.md) | Referência dos endpoints da API |
| [`CHANGELOG.md`](CHANGELOG.md) | Histórico de versões |

## Roadmap

- [x] MVP — cadastro de projeto/chapa/peça, algoritmo Guillotine, visualização, PDF
- [ ] Validação com o marceneiro-piloto em pelo menos 3 projetos reais
- [ ] V2 — fita de borda, estoque de sobras entre projetos, histórico/comparação de planos, importação CSV
- [ ] V3 — múltiplas alternativas de plano, etiquetas com QR Code, bloco financeiro/comercial

Detalhes e critérios de cada fase em
[`PlanoDeCorte_Documentacao_Fundamentos.md`](PlanoDeCorte_Documentacao_Fundamentos.md#8-roadmap-do-produto-e-definição-de-mvp).

---

## Objetivo

Mais do que uma ferramenta para um parceiro específico, o CutFlow busca
aplicar o mesmo rigor de descoberta de domínio, modelagem, arquitetura em
camadas e algoritmos aplicados usado no FlowOps — dessa vez com um
diferencial mensurável em dinheiro: cada plano gerado responde diretamente
"quantas chapas comprar", o dado que o marceneiro hoje calcula na mão antes
de fechar orçamento com o cliente.
