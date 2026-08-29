# Guia da Fase 0 — pôr o esqueleto no ar

Objetivo: uma página em produção que diz "API a responder" e mostra um registo lido
do Postgres. Sem funcionalidade nenhuma. É de propósito — queremos que os problemas
de infraestrutura apareçam agora, e não com meio sistema construído por cima.

---

## 1. Instalar o Maven

O único que falta na sua máquina:

```powershell
winget install Apache.Maven
```

Feche e reabra o terminal (o PATH outra vez) e confirme:

```powershell
mvn -v
```

## 2. Montar o repositório

Clone o repositório e copie para dentro dele o conteúdo do zip que lhe dei:

```powershell
git clone https://github.com/felipefetzer/receitas-que-sabem.git
cd receitas-que-sabem
```

Deve ficar assim:

```
receitas-que-sabem/
├── .gitignore
├── CLAUDE.md
├── GUIA-FASE-0.md
├── docker-compose.yml
├── backend/
└── frontend/
```

## 3. Gerar o projeto Vite

A pasta `frontend/` que lhe dei só tem os ficheiros que interessam. O andaime do Vite
gera-se com um comando, para não ficar preso a boilerplate desatualizado.

Na raiz do repositório:

```powershell
npm create vite@latest frontend-tmp -- --template react-ts
```

Depois copie o conteúdo de `frontend-tmp/` para `frontend/`, **sem substituir**
o `src/App.tsx` nem o `src/api/client.ts` que já lá estão. Apague a pasta `frontend-tmp`.

A seguir, dentro de `frontend/`:

```powershell
cd frontend
npm install
copy .env.example .env.local
cd ..
```

## 4. Correr tudo localmente

Na raiz, com o Docker Desktop aberto:

```powershell
docker compose up --build
```

Isto levanta o Postgres, espera que fique saudável, constrói a imagem do backend e
arranca-o. Da primeira vez demora — está a descarregar o Maven e as dependências todas.

Quando parar de imprimir linhas, teste num browser:
`http://localhost:8080/api/health`

Deve devolver JSON com `"database":"ok"` e `"app":"Receitas que Sabem"`.

Noutro terminal:

```powershell
cd frontend
npm run dev
```

Abra `http://localhost:5173`. Se vir o cartão verde, as três peças estão ligadas.

**Se aparecer erro de CORS na consola do browser:** é sinal de que a origem do
frontend não bate certo com a lista autorizada. Confirme que o Vite está mesmo
na porta 5173 e que o `CORS_ALLOWED_ORIGINS` no `docker-compose.yml` corresponde.

## 5. Primeiro commit

```powershell
git add .
git commit -m "Fase 0: esqueleto do backend e do frontend"
git push
```

Antes de fazer push, confirme que **não** existe nenhum `.env.local` na lista do
`git status`. O `.gitignore` já trata disso, mas vale a pena olhar.

---

## 6. Deploy do backend no Render

1. **New → Web Service**, ligue ao GitHub e escolha `receitas-que-sabem`
   (autorize apenas este repositório).
2. **Root Directory:** `backend`
3. **Runtime / Language:** Docker
4. **Instance Type:** Free
5. **Environment Variables** — as quatro:

| Chave | Valor |
|---|---|
| `DATABASE_URL` | `jdbc:postgresql://<pooler-host>/neondb?sslmode=require` |
| `DATABASE_USER` | `neondb_owner` |
| `DATABASE_PASSWORD` | a password nova que gerou na Neon |
| `CORS_ALLOWED_ORIGINS` | por agora `http://localhost:5173`; atualiza no passo 8 |

**Atenção ao formato do URL.** A Neon dá-lhe uma string que começa por
`postgresql://utilizador:password@host/base`. O Java **não** usa esse formato.
Tem de a converter: começa por `jdbc:postgresql://`, tira o utilizador e a password
de dentro do URL (vão nas variáveis próprias) e usa o **host do pooler**, aquele
que tem `-pooler` no nome.

Fica algo como:
`jdbc:postgresql://ep-xxxx-pooler.c-6.eu-central-1.aws.neon.tech/neondb?sslmode=require`

6. Deploy. A primeira construção demora vários minutos.
7. Teste `https://<o-seu-servico>.onrender.com/api/health`.

Se der erro de ligação à base de dados, o `sslmode=require` é o suspeito número um —
a Neon recusa ligações sem TLS.

## 7. Deploy do frontend no Cloudflare Pages

1. **Workers & Pages → Create → Pages → Connect to Git**, mesmo repositório.
2. **Root directory:** `frontend`
3. **Build command:** `npm run build`
4. **Build output directory:** `dist`
5. **Environment variable:** `VITE_API_URL` = `https://<o-seu-servico>.onrender.com`
   (sem barra no fim)

Note a diferença: no backend as variáveis são lidas quando o servidor arranca; no
frontend são **coladas dentro do JavaScript durante a construção**. Mudar o
`VITE_API_URL` obriga a reconstruir o site.

## 8. Fechar o círculo do CORS

Volte ao Render e mude `CORS_ALLOWED_ORIGINS` para o URL do Pages, por exemplo:

```
https://receitas-que-sabem.pages.dev,http://localhost:5173
```

Duas origens separadas por vírgula: a de produção e a sua máquina. O serviço reinicia
sozinho. Abra o site do Pages — deve ver o cartão verde.

Se a primeira tentativa falhar, espere um minuto e recarregue: o serviço gratuito do
Render estava provavelmente a acordar.

---

## Quando isto estiver feito

A Fase 0 está fechada quando:

- [ ] `docker compose up` levanta tudo em casa
- [ ] `npm run dev` mostra o cartão verde contra o backend local
- [ ] O site no Cloudflare Pages mostra o cartão verde contra o Render
- [ ] O repositório não contém nenhuma password

Seguem-se as tabelas de utilizadores, o Spring Security e os cookies de sessão.
