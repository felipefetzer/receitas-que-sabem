# CLAUDE.md — Receitas que Sabem

> O nome é um trocadilho: receitas que *sabem* (conhecimento) e comida que *sabe bem* (sabor).

Contexto do projeto para assistentes de IA e para o próprio autor.
Se algo aqui contradisser o código, este ficheiro está desatualizado: corrigir aqui.

**Estado:** em produção, com login, receitas, ativação e lista de compras a funcionar.
Fotos, avaliações e PWA ainda não estão implementados (ver secção 11).

---

## 1. O que é

Aplicação de receitas. Cada utilizador regista as suas, consulta as dos outros, "ativa"
as que vai cozinhar, e daí sai automaticamente uma lista de compras com o que lhe falta.

Prioridade de utilização: **telemóvel** — a app é usada na cozinha, muitas vezes com as
mãos ocupadas. O autor está a aprender: explicar decisões é parte do trabalho, não um extra.

---

## 2. Stack

### Backend (`/backend`)
- Java 21, Spring Boot 3.5.4, Maven
- Spring Web, Spring Security, Spring Data JPA, **Spring Session JDBC**
- Bean Validation
- PostgreSQL (Neon em produção, contentor local em desenvolvimento)
- Flyway — o schema **nunca** é gerado por `ddl-auto` (que está em `validate`)

### Frontend (`/frontend`)
- React + TypeScript, Vite
- React Router
- CSS simples num único `src/styles.css`, sem framework
- Sem TanStack Query: o estado do servidor é gerido com `useState` + `useEffect`.
  Se o número de ecrãs crescer, reavaliar.

### Infraestrutura
- Um repositório, duas pastas, **dois deploys independentes**
- **Backend:** Render, serviço web gratuito, via Dockerfile. Adormece após 15 min
  sem tráfego e demora até um minuto a acordar.
- **Base de dados:** Neon (Postgres serverless), plano gratuito, 0,5 GB.
  **Não** o Postgres do Render, que é apagado 30 dias após a criação.
- **Frontend:** Cloudflare, publicado como **Worker** com assets estáticos
  (`frontend/wrangler.jsonc`), não como Pages.
- `docker-compose.yml` na raiz levanta Postgres + backend localmente.

---

## 3. Arquitetura

**Monólito modular com SPA — não microserviços.** Um backend, uma base de dados,
um frontend que a consome. A separação API/cliente é a que interessa aqui; dividir
o backend em serviços autónomos só acrescentaria complexidade operacional.

```
receitas-que-sabem/
├── backend/          # API Spring Boot
├── frontend/         # SPA React + Vite
├── docker-compose.yml
└── CLAUDE.md
```

Pacotes **por funcionalidade**, não por camada:
`auth/`, `user/`, `recipe/`, `activation/`, `shoppinglist/`, `common/`.

Entidades JPA nunca atravessam a fronteira do controller — a resposta é sempre DTO.
Os DTOs de cada área vivem numa classe agregadora (`RecipeDtos`, `ActivationDtos`).

---

## 4. Modelo de dados

Migrations aplicadas: `V1__baseline.sql` (tabela `app_info`, usada pelo health check)
e `V2__auth_e_receitas.sql` (tudo o resto).

```
users                    id, username (UNIQUE), password_hash, created_at
recipes                  id, user_id, name, notes, created_at, updated_at
                         UNIQUE (user_id, name)
ingredients              id, recipe_id, position, quantity, unit, item
utensils                 id, recipe_id, name
preparation_sections     id, recipe_id, position, title
preparation_steps        id, section_id, position, instruction, duration_minutes (NULL)
activations              id, user_id, recipe_id, activated_at
                         UNIQUE (user_id, recipe_id)
activation_ingredients   id, activation_id, ingredient_id, available
                         UNIQUE (activation_id, ingredient_id)
SPRING_SESSION           sessões do Spring Session (criadas na V2, não pelo Boot)
SPRING_SESSION_ATTRIBUTES
```

### Decisão central: a lista de compras não é uma tabela

É **derivada por query**: os `activation_ingredients` com `available = false` das
ativações do utilizador. Desativar uma receita apaga a ativação, o `ON DELETE CASCADE`
leva o estado dos ingredientes, e os itens desaparecem da lista sozinhos. Não há código
de sincronização, logo não há estado inconsistente possível.

O estado "tenho / não tenho" pertence à **ativação**, não à receita — o utilizador A
pode ativar uma receita do utilizador B, e cada um tem a sua despensa.

`position` é um inteiro guardado explicitamente. A ordem nunca depende do `id`
nem da ordem de inserção.

---

## 5. Regras de negócio

Numeradas para servirem de checklist. **Ainda não existem testes automatizados** —
é a maior dívida técnica atual.

**Utilizadores**
- R1. `username` único em toda a aplicação.
- R2. Password com BCrypt. Nunca em texto simples, nunca em logs.

**Receitas**
- R3. Nome único **por utilizador** (dois utilizadores podem ter "Bolo de Cenoura").
- R4. O autor é sempre o utilizador autenticado (`CurrentUser`). Nunca aceitar `userId` do cliente.
- R5. Só o autor pode editar ou apagar.
- R6. Qualquer utilizador autenticado pode ler todas as receitas.

**Tempos**
- R8. O tempo de uma secção é a soma dos `duration_minutes` dos seus passos.
- R9. ≥ 60 minutos apresenta-se como "1h30", não "90 min".
- R10. Se algum passo da secção não tiver tempo, prefixar "aproximadamente".
- R11. Se nenhum passo tiver tempo, não mostrar tempo.
- R12. A formatação é do **frontend** (`src/lib/time.ts`); a API devolve
  `totalMinutes` e o booleano `partial`.

**Ativação**
- R13. Máximo de **5** ativas por utilizador (`ActivationService.MAX_ACTIVE`). A 6ª dá `409`.
- R14. Ao ativar, cria-se uma linha por ingrediente com `available = false`.
- R15. Desativar apaga a ativação e, em cascata, o estado dos ingredientes.
- R16. Qualquer utilizador pode ativar qualquer receita, incluindo as suas.

**Lista de compras**
- R17. Contém os ingredientes com `available = false` das ativações do utilizador.
- R18. Agrupada por receita, na ordem original dos ingredientes. Itens iguais em
  receitas diferentes **não** são fundidos — "1 colher" e "200g" não somam.
- R19. Riscar na lista equivale a `available = true` na ativação.
- R20. Exportação para clipboard: texto simples, gerado no cliente.

---

## 6. API

Prefixo `/api`. JSON. Recursos no plural, em inglês.

```
GET    /api/health                     público
GET    /api/auth/csrf                  público — devolve {token, headerName}
POST   /api/auth/register              público
POST   /api/auth/login                 público
POST   /api/auth/logout
GET    /api/auth/me

GET    /api/recipes?scope=all|mine
GET    /api/recipes/{id}
POST   /api/recipes
PUT    /api/recipes/{id}
DELETE /api/recipes/{id}

GET    /api/activations
POST   /api/activations                {recipeId}
DELETE /api/activations/{id}
PATCH  /api/activations/{id}/ingredients/{ingredientId}   {available}

GET    /api/shopping-list
```

**Convenções**
- A receita é criada e atualizada como **agregado completo**: ingredientes, secções,
  passos e utensílios no mesmo payload. Não há endpoints para sub-recursos.
- Reordenar = enviar o agregado com a nova ordem; o servidor reatribui `position`
  pelo índice do array.
- Ao atualizar, o agregado é reescrito por inteiro (`clear()` + `orphanRemoval`).
  Mais simples e mais seguro do que casar item a item o que mudou.
- Erros seguem `ProblemDetail` (RFC 7807). O frontend lê sempre o campo `detail`.
- `401` sem sessão, `403` sem permissão, `404` inexistente, `409` regra de negócio, `422` validação.

---

## 7. Autenticação, CORS e CSRF

A parte mais delicada do projeto. Frontend e backend estão em **domínios diferentes**,
o que muda tudo.

**Sessão.** Cookie `RECEITAS_SESSION`, HttpOnly, guardado no Postgres via Spring Session
JDBC — assim sobrevive aos reinícios do Render. Em produção tem de ser
`SameSite=None; Secure`, senão o browser não o envia entre domínios.

**Variáveis de ambiente no Render** (sem elas o login falha em silêncio):
```
COOKIE_SAME_SITE=None
COOKIE_SECURE=true
CORS_ALLOWED_ORIGINS=https://receitas-que-sabem.titisdesucesso.workers.dev,http://localhost:5173
DATABASE_URL=jdbc:postgresql://<pooler-host>/neondb?sslmode=require
DATABASE_USER / DATABASE_PASSWORD
```
Localmente os valores por omissão (`Lax`, `secure=false`) chegam, porque
`localhost:5173` e `localhost:8080` contam como o mesmo site.

**CSRF — solução fora do habitual.** A receita comum é o JavaScript ler o token do
cookie `XSRF-TOKEN`, mas isso exige que frontend e backend partilhem domínio.
Aqui o token é obtido em `GET /api/auth/csrf` e enviado no cabeçalho; o cookie
continua a ir junto e o servidor compara os dois. O `client.ts` trata disto sozinho
e repete o pedido uma vez se o token tiver expirado.

**Notas de configuração**
- `CORS_ALLOWED_ORIGINS` é dividido por vírgulas e sofre `trim()`. Sem barra no fim —
  o browser compara as origens carácter a carácter.
- `VITE_API_URL` também leva `trim` de barras finais no cliente: uma barra a mais
  produz `//api/health`, que não corresponde ao mapeamento `/api/**` e devolve 403.
- O `HttpStatusEntryPoint` garante que um pedido não autenticado recebe `401`
  em vez da página de login em HTML do Spring.

---

## 8. Frontend

### Rotas
```
/login                  login e registo (alterna no mesmo ecrã)
/                       menu principal, com botão de sair
/recipes                listagem + filtro (todas / só as minhas)
/recipes/new            formulário de criação
/recipes/:id            leitura; muda de modo quando a receita está ativa
/recipes/:id/edit       o mesmo formulário, pré-preenchido
/shopping-list          lista de compras
/about                  explicação do funcionamento
```

Tudo o que não seja `/login` passa pelo componente `Protected`.

### Decisões de UI
- **Reordenação por setas ↑ ↓**, não por arrastar. Funciona melhor no telemóvel e
  não traz dependências. Trocar por drag-and-drop é uma melhoria em aberto.
- O que os requisitos chamam "radio button" está implementado como **toggle**:
  cada ingrediente é independente, não são opções mutuamente exclusivas.
- Na lista de compras, o item riscado **permanece visível** até sair do ecrã, para
  não desaparecer debaixo do dedo no supermercado.
- Alvos de toque com mínimo de 44px.
- Serifa no corpo do texto; paleta quente. É um caderno, não um painel de controlo.

---

## 9. Convenções de código

**Java**
- Nomes em inglês. Comentários e commits em português.
- DTOs como `record`, entidades como classes com construtor protegido para o JPA.
- Injeção por construtor. Sem `@Autowired` em campos.
- `@Transactional` no service, nunca no controller (exceto o `ShoppingListController`,
  que só lê e não tem service próprio).
- Sem `FetchType.EAGER`; usar `join fetch` onde é preciso.

**TypeScript**
- Componentes funcionais. Sem `any`.
- Todas as chamadas à API passam por `src/api/client.ts`. Nenhum componente usa `fetch`.
- Tipos da API escritos à mão no `client.ts` — **não** gerados a partir de OpenAPI
  (o springdoc não está instalado). Ao mudar um DTO no backend, atualizar o tipo aqui.

**Geral**
- Nada de segredos no repositório. `.env.local` ignorado; `.env.production` pode ir,
  porque só contém o URL público da API.
- Migrations Flyway são imutáveis depois de aplicadas. Correções fazem-se com uma nova.

---

## 10. Pontos em aberto

- [ ] **Configuração de build da Cloudflare está errada** — publica a pasta errada,
  por isso o deploy do frontend é manual (`npm run build && npx wrangler deploy`).
  Devia ser root `frontend`, build `npm run build`, deploy `npx wrangler deploy`.
- [ ] **Sem testes automatizados.** As regras da secção 5 são a lista natural por onde começar.
- [ ] Flyway avisa que não foi testado com PostgreSQL 18 (a Neon corre 18.6).
  Funciona; atualizar a dependência quando for conveniente.
- [ ] Sem paginação nem pesquisa na listagem de receitas.
- [ ] Sem `updated_at` a ser mostrado no interface.
- [ ] O ecrã de leitura faz dois pedidos quando a receita está ativa
  (`/recipes/{id}` e `/activations`). Dá para resolver devolvendo a disponibilidade
  no próprio detalhe.

---

## 11. Por construir

Por ordem provável, mas nada está decidido:

- **Fotos** — até 2 por receita, em `BYTEA` no Postgres, comprimidas no cliente
  (máx. 1200px, JPEG ~0.8) antes do upload multipart. O disco do Render é efémero,
  por isso guardar ficheiros nele não é opção; e os 0,5 GB da Neon são partilhados
  com o resto dos dados.
- **Avaliações** — sabor, dificuldade e preço, 5 estrelas, um voto por utilizador
  por categoria, com média e contagem na listagem. O autor pode avaliar a própria receita.
- **PWA** — manifest, service worker, cache das receitas visitadas para funcionar
  offline na cozinha. Escritas offline não estão previstas.
