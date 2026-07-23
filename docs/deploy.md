# Publicação em produção — guia passo a passo

Este guia assume que você nunca hospedou nada antes. Ao final, o CutFlow
estará no ar, com HTTPS, num link fixo, **sem pagar nada por mês** — só o
domínio é opcional (existe uma alternativa 100% grátis, explicada abaixo).

Tempo estimado: 45–60 minutos na primeira vez, feito uma única vez.

## Como funciona (resumo antes de começar)

O CutFlow roda em 3 partes (Postgres, backend Java, frontend), empacotadas em
containers Docker que já testamos localmente. Vamos colocá-las para rodar
**de graça, para sempre** — não é um teste grátis por 30 dias — numa
máquina virtual da Oracle Cloud ("Always Free"), com um endereço público
(domínio) e certificado HTTPS automático.

Por que Oracle e não Render/Railway (que também têm "grátis")? Porque nos
dois o **banco de dados grátis expira ou o serviço dorme** — inviável para
guardar projetos reais de um cliente. A VM da Oracle não expira, não dorme e
não tem pegadinha: é gratuita enquanto você não passar dos limites (bem
folgados para o nosso caso).

---

## Parte 1 — Criar a máquina virtual gratuita (Oracle Cloud)

### 1.1. Criar a conta

1. Acesse **https://www.oracle.com/cloud/free/** e clique em "Start for free".
2. Preencha e-mail, país e crie uma senha.
3. A Oracle pede um **cartão de crédito para verificar sua identidade** —
   isso é normal e **não gera cobrança** enquanto você usar só os recursos
   "Always Free" (os que vamos usar). Nenhuma assinatura é iniciada.
4. Escolha uma região próxima de você (ex.: `Brazil East (São Paulo)` ou
   `Brazil Southeast (Vinhedo)`, se disponíveis) — depois de criada, a
   região **não muda mais**, então escolha com calma.
5. Confirme o e-mail e finalize o cadastro.

### 1.2. Criar a VM (a "máquina" onde o CutFlow vai rodar)

1. No menu (☰, canto superior esquerdo) → **Compute → Instances → Create
   instance**.
2. Nome: `cutflow-server` (ou o que preferir).
3. Em **Image and shape**, clique em "Edit":
   - Imagem: **Canonical Ubuntu** (24.04 ou mais recente).
   - Shape: clique em "Change shape" → aba **Ampere** → escolha
     `VM.Standard.A1.Flex` → configure **2 OCPU / 12 GB de memória**
     (o teto do plano gratuito) → confirme. Certifique-se de que diz
     **"Always Free-eligible"** na tela.
4. Em **Add SSH keys**, deixe a Oracle **gerar** o par de chaves e clique em
   "Save private key" — salve o arquivo (algo como `ssh-key-...key`) num
   lugar que você vai lembrar (ex.: `Documentos\chaves\`). É a "senha" que
   você vai usar para entrar na VM depois; sem ele, você perde o acesso.
5. Clique em **Create**.

**Se aparecer erro "Out of host capacity"**: é só falta de vaga temporária
para máquinas ARM gratuitas na sua região — muito comum, não é erro seu.
Tente de novo em alguns minutos, ou troque o "Availability domain" (AD-1,
AD-2, AD-3, se sua região tiver mais de um) e tente de novo. Costuma
funcionar em poucas tentativas.

### 1.3. Abrir as portas 80 e 443 (para o site ficar acessível)

Por padrão a Oracle só libera a porta 22 (acesso remoto). Precisamos abrir
80 (HTTP) e 443 (HTTPS):

1. Na página da instância criada, clique no link da **VCN** (a rede) →
   clique na **Security List** padrão (`Default Security List for ...`).
2. **Add Ingress Rules** → adicione uma regra:
   - Source CIDR: `0.0.0.0/0`
   - IP Protocol: TCP
   - Destination Port Range: `80`
3. Repita para a porta `443`.

### 1.4. Anotar o IP público

Na página da instância, copie o **Public IP Address** (algo como
`123.45.67.89`) — vamos precisar dele já já.

---

## Parte 2 — Domínio grátis (DuckDNS)

Você precisa de um endereço (`algumacoisa.com`) para o HTTPS automático
funcionar — não dá para emitir certificado só para um número de IP. A opção
100% grátis é o **DuckDNS**, que existe há anos e não tem custo nenhum:

1. Acesse **https://www.duckdns.org** e entre com sua conta Google ou GitHub.
2. No campo "subdomain", crie algo como `cutflow-suamarcenaria` → clique
   **add domain**. Você terá `cutflow-suamarcenaria.duckdns.org`.
3. No campo ao lado do domínio criado, cole o **IP público** da VM (Parte
   1.4) e clique em **update ip**.

Anote o domínio completo — é o que vamos usar no `.env` como `DOMAIN` e nas
URLs. (Se no futuro preferir um domínio "de verdade", tipo
`.com.br` (~R$ 40/ano), o processo é o mesmo: só aponta o DNS dele para o
mesmo IP em vez de usar o DuckDNS.)

---

## Parte 3 — Conectar na VM e instalar o Docker

### 3.1. Conectar por SSH

No **PowerShell** do seu computador (Windows já vem com `ssh` embutido):

```bash
ssh -i "C:\caminho\para\ssh-key-....key" ubuntu@SEU_IP_PUBLICO
```

Na primeira conexão ele pergunta se confia no servidor — digite `yes`.

> Se aparecer erro de permissão da chave, rode antes (uma vez):
> `icacls "C:\caminho\para\ssh-key-....key" /inheritance:r /grant:r "%username%:R"`

### 3.2. Instalar Docker

Já dentro da VM (via SSH), copie e cole o bloco inteiro:

```bash
sudo apt update && sudo apt upgrade -y
curl -fsSL https://get.docker.com | sudo sh
sudo usermod -aG docker $USER
```

Depois, **saia e entre de novo** (`exit` e reconecte com o mesmo comando do
3.1) para o grupo `docker` valer.

Confirme que funcionou:

```bash
docker --version
docker compose version
```

---

## Parte 4 — Colocar o CutFlow na VM

### 4.1. Clonar o projeto

```bash
git clone https://github.com/SEU-USUARIO/CutFlow.git
cd CutFlow
```

(Se o repositório for privado, o `git clone` vai pedir usuário/senha — use
um [personal access token](https://github.com/settings/tokens) do GitHub no
lugar da senha.)

### 4.2. Configurar o `.env` de produção

```bash
cp .env.example .env
nano .env
```

Ajuste estas linhas (as outras podem ficar como estão):

```
DB_PASSWORD=escolha-uma-senha-forte-aqui
CORS_ALLOWED_ORIGINS=https://cutflow-suamarcenaria.duckdns.org
FRONTEND_URL=https://cutflow-suamarcenaria.duckdns.org
DOMAIN=cutflow-suamarcenaria.duckdns.org
SESSION_COOKIE_SECURE=true
```

Troque `cutflow-suamarcenaria.duckdns.org` pelo domínio que você criou na
Parte 2. Para salvar no `nano`: `Ctrl+O`, `Enter`, depois `Ctrl+X` para sair.

**Login com Google (opcional):** se quiser esse botão habilitado, veja
"Login Google" mais abaixo antes de continuar — senão deixe
`GOOGLE_CLIENT_ID`/`GOOGLE_CLIENT_SECRET` em branco e só o login por e-mail/
senha fica disponível (dá para habilitar o Google depois, a qualquer hora).

### 4.3. Subir o CutFlow

```bash
docker compose -f docker-compose.prod.yml up -d --build
```

Isso demora alguns minutos na primeira vez (baixa imagens, compila o
backend, gera o build do frontend). Acompanhe com:

```bash
docker compose -f docker-compose.prod.yml logs -f
```

(`Ctrl+C` só sai do acompanhamento — os containers continuam rodando.)

### 4.4. Acessar

Abra `https://cutflow-suamarcenaria.duckdns.org` no navegador. O Caddy emite
o certificado HTTPS sozinho na primeira visita (pode levar de 10 a 60
segundos na primeiríssima vez). Crie sua conta real e confirme que
cadastro, criar projeto e gerar plano de corte funcionam.

---

## Login Google em produção (opcional)

Se já usa o Google em desenvolvimento, é só adicionar mais um endereço
autorizado — não precisa criar credenciais novas:

1. [Google Cloud Console](https://console.cloud.google.com/) → **APIs e
   Serviços → Credenciais** → abra seu OAuth Client existente.
2. Em **Authorized redirect URIs**, adicione:
   `https://cutflow-suamarcenaria.duckdns.org/login/oauth2/code/google`
   (mantendo a URI de `localhost` também, se ainda usa em dev).
3. Salve. Coloque o mesmo `GOOGLE_CLIENT_ID`/`GOOGLE_CLIENT_SECRET` no `.env`
   da VM (Parte 4.2) e suba de novo:
   `docker compose -f docker-compose.prod.yml up -d --build`.

Se a tela de consentimento OAuth ainda estiver em modo "Testing", só
e-mails cadastrados em **Usuários de teste** conseguem entrar — adicione lá
os e-mails dos usuários reais, ou publique o app.

---

## Checklist de segurança antes de divulgar o link

- [ ] `DB_PASSWORD` forte (não deixe o valor de exemplo)
- [ ] `SESSION_COOKIE_SECURE=true` (já é o padrão no `docker-compose.prod.yml`)
- [ ] `CORS_ALLOWED_ORIGINS`/`FRONTEND_URL`/`DOMAIN` = seu domínio real, não
      `localhost`
- [ ] Testou cadastro, login, criar projeto e gerar plano no domínio público
- [ ] Backup configurado (Parte 5)
- [ ] `.env` **nunca** commitado (já protegido pelo `.gitignore`)

Note que `docker-compose.prod.yml` já cuida de dois pontos que não dá para
esquecer manualmente: a conta de demonstração (`demo@cutflow.app`) não é
criada em produção (o `seed.sql` só é usado em dev), e o Postgres não fica
acessível pela internet (só o Caddy é exposto).

---

## Parte 5 — Backup automático (gratuito)

Já existe um script pronto (`scripts/backup-db.sh`) que gera um dump
comprimido do banco. Para rodar todo dia às 3h da manhã sozinho:

```bash
crontab -e
```

Adicione a linha (ajuste o caminho se clonou em outro lugar):

```
0 3 * * * cd /home/ubuntu/CutFlow && ./scripts/backup-db.sh >> backups/backup.log 2>&1
```

Os arquivos ficam em `CutFlow/backups/`, e o script já apaga sozinho
backups com mais de 14 dias. Para restaurar um backup manualmente:

```bash
gunzip -c backups/cutflow_2026-08-01_030000.sql.gz | \
  docker compose -f docker-compose.prod.yml exec -T db psql -U cutflow -d cutflow
```

**Dica extra (opcional, também grátis):** baixe os backups periodicamente
para fora da VM (seu computador, Google Drive etc.) — se a VM tiver algum
problema, um backup que só existe *dentro* dela não ajuda muito.

---

## Monitoramento gratuito (opcional, mas recomendado)

Para saber se o site cair sem você precisar ficar checando:
[UptimeRobot](https://uptimerobot.com) (plano grátis) — cadastre a URL do
seu CutFlow e ele avisa por e-mail se parar de responder.

---

## Manutenção — o dia a dia depois do deploy

### Publicar uma atualização (depois de eu implementar algo novo)

Na VM, dentro da pasta `CutFlow`:

```bash
git pull
docker compose -f docker-compose.prod.yml up -d --build
```

Isso reconstrói só o que mudou; o site fica fora do ar por poucos segundos
(o tempo de trocar os containers), sem perder dados do banco.

Se a atualização incluir uma **migração de banco** (arquivo novo em
`db/migrations/`), rode-a antes do `up -d --build`:

```bash
docker compose -f docker-compose.prod.yml exec -T db \
  psql -U cutflow -d cutflow < db/migrations/NOME_DA_MIGRACAO.sql
```

### Ver o que está acontecendo (logs)

```bash
docker compose -f docker-compose.prod.yml logs -f backend   # só o backend
docker compose -f docker-compose.prod.yml logs -f           # tudo
```

### Reiniciar um serviço travado

```bash
docker compose -f docker-compose.prod.yml restart backend
```

### Se a VM inteira reiniciar (queda de energia, manutenção da Oracle etc.)

Não precisa fazer nada: os containers têm `restart: unless-stopped` e o
Docker sobe automaticamente com o sistema — o CutFlow volta sozinho.

### Certificado HTTPS vencendo

Também não precisa fazer nada — o Caddy renova sozinho, automaticamente,
antes de vencer.

### Trocar uma senha/segredo (`DB_PASSWORD`, credenciais do Google etc.)

Edite o `.env` (`nano .env`) e suba de novo:

```bash
docker compose -f docker-compose.prod.yml up -d
```

### Manter o sistema operacional da VM em dia

De vez em quando (ex.: 1x por mês):

```bash
sudo apt update && sudo apt upgrade -y
sudo reboot
```

### Quando pensar em migrar para uma opção paga

O plano Always Free da Oracle aguenta tranquilamente um cenário de "uma
marcenaria, uso real" por muito tempo. Vale migrar para algo gerenciado
(Render/Railway pagos, ou um Postgres gerenciado tipo Neon) quando: (a) você
não quiser mais administrar servidor/backup manualmente, ou (b) o projeto
crescer para várias marcenarias pagantes e precisar de mais capacidade,
suporte e SLA do que uma VM sozinha oferece.

---

## O que mais vale considerar antes de divulgar (não bloqueia o deploy)

Nenhum destes impede colocar no ar hoje — são melhorias de acabamento para
quando fizer sentido:

- **"Esqueci minha senha"**: hoje não existe (ver ADR-0005). Com poucos
  usuários, dá para resetar manualmente pelo banco se alguém esquecer; virar
  prioridade se a base de usuários crescer.
- **Termos de uso / política de privacidade**: como o sistema agora guarda
  contas e dados de clientes de terceiros, vale ter um texto simples,
  principalmente se for oferecer a outras marcenarias além da piloto.
- **Página de erro amigável**: se a API cair, o frontend hoje mostra uma
  mensagem genérica de erro em vez de uma tela mais explicativa.
