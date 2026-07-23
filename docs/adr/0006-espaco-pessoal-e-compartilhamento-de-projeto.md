# ADR-0006 — Espaço pessoal automático e compartilhamento direto de projeto

- **Status:** aceita
- **Data:** 2026-07-22
- **Contexto:** feedback de uso da v0.4 (multi-tenant): a obrigatoriedade de
  criar uma organização antes do primeiro projeto era fricção, e faltava um
  jeito de compartilhar um projeto pontual sem montar uma organização.

## Contexto

A ADR-0005 tornou toda operação escopada por organização e o frontend passou a
**exigir** a criação de uma organização (nome, CNPJ) antes do primeiro projeto.
Isso contradiz o princípio de baixa fricção que guiou o produto: o MEI que
trabalha sozinho não quer "criar uma empresa" para si. Além disso, o único
compartilhamento existente era adicionar alguém como **membro da organização
inteira** — pesado demais para o caso "Joaquim chama o Carlos para ajudar num
bico e precisa que ele veja/edite só aquele plano de corte".

A referência mental é Figma/Canva: espaço pessoal por padrão, times quando
precisa, e compartilhamento por projeto (link ou convite) independente de time.

## Decisão

Três camadas, **aditivas** sobre a ADR-0005 (nada do que foi testado é
descartado):

### 1. Espaço pessoal automático (remove a fricção do onboarding)

No cadastro (local ou primeiro login Google), cria-se **automaticamente** uma
`Organizacao` marcada `pessoal = true`, com o próprio usuário como único
membro (OWNER) — sem nenhuma tela. O usuário cai direto em "criar projeto". É a
mesma tabela `Organizacao`/`Membro` já testada; a organização "de verdade"
(com equipe) continua opcional, criada manualmente por quem tem time. O
frontend esconde a gestão de equipe nos espaços pessoais.

### 2. Modelo de navegação: workspace switcher (mantido)

Confirma-se o modelo atual (estar "dentro" de um workspace por vez e ver os
projetos dele), e **não** um "package/pasta" com tudo junto — é o padrão
Figma/Canva/Slack, mantém a fronteira mental clara e evita retrabalho. Os
projetos de fora do workspace ativo aparecem numa seção à parte,
"Compartilhados comigo".

### 3. Compartilhamento direto de projeto (novo)

- `ColaboradorProjeto (projeto, usuario, papel)` dá acesso a **um** projeto,
  independente de organização. `PapelColaborador`: `EDITOR` (edita) ou
  `VISUALIZADOR` (só vê).
- `ConviteProjeto` cobre dois modos, distinguidos por `emailAlvo`:
  - **`emailAlvo = null` → link reutilizável** (estilo Canva): qualquer pessoa
    logada que abrir o link entra como colaborador; vale até ser revogado
    (não é "consumido" — vários podem usar o mesmo link);
  - **`emailAlvo` preenchido → convite direcionado**: só aquele e-mail aceita
    e o convite é de uso único (marca `aceito_em`).
  - Essa distinção **corrige uma incoerência** do plano original (que marcava
    todo convite como aceito no primeiro uso, o que quebraria o link estilo
    Canva).
- O `uuid` do convite é o token da URL (`/convite/{uuid}`). O GET de detalhes é
  público ("Fulano te convidou para o projeto X") — o aceite exige login.
  Sem servidor de e-mail, não há envio automático: a API devolve a URL pronta
  para o dono copiar e mandar por WhatsApp/e-mail.

### 4. Autorização num único ponto (estendido)

`ProjetoService.nivelAcesso` decide o acesso olhando **os dois caminhos**:
Membro da organização dona (EDIÇÃO) **ou** colaborador direto (EDITOR = edição,
VISUALIZADOR = leitura). `findOrThrow` libera quem pode ao menos ver (404 para
quem não pode); `exigirPodeEditar` é chamado por Peca/Chapa/Plano antes de
qualquer mutação — é o único lugar que distingue leitura de edição. Assim, um
colaborador VISUALIZADOR abre o projeto e o plano, mas não altera nada.

## Consequências

- O MEI solo nunca vê "criar organização"; a marcenaria com equipe usa
  organização de verdade; o compartilhamento pontual usa link/convite — os
  três casos que o feedback levantou.
- `ProjetoRepository.findByUuid` deixa de filtrar por organização (a
  autorização passou a ser em `nivelAcesso`, não na query).
- Migração para bancos existentes:
  `db/migrations/0004_espaco_pessoal_e_compartilhamento.sql` (ou
  `docker compose down -v` em desenvolvimento).

## Limitações conscientes (candidatas a melhoria)

- **Edição "simultânea" não é em tempo real colaborativo** (sem WebSocket/CRDT):
  duas pessoas editando veem as mudanças ao recarregar, não ao vivo. O plano se
  regenera a cada mutação (ADR-0004), então o resultado converge, mas não há
  cursor compartilhado como no Figma.
- **Login Google não preserva o `next`** do convite (o OAuth volta para a
  home); o convite por e-mail/senha preserva. Quem entrar por Google reabre o
  link do convite.
- **Sem e-mail automático**: o convite direcionado não notifica por e-mail; o
  dono copia a URL e envia.

## Alternativas consideradas

- **"Package/pasta" com todos os projetos numa lista** — rejeitado: embaralha
  a fronteira "de quem é / quem vê", contra o padrão de mercado.
- **Só link reutilizável (sem convite por e-mail)** — cobriria o Joaquim/Carlos,
  mas o usuário pediu explicitamente "convite no e-mail **ou** uma URL"; o
  modelo unificado (e-mail opcional) entrega os dois.
- **Compartilhar sempre virando membro da organização** — pesado: daria à
  pessoa acesso a tudo da organização, não só ao projeto.
