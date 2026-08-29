# CLAUDE.md — Receitas que Sabem

> O nome é um trocadilho: receitas que *sabem* (conhecimento) e comida que *sabe bem* (sabor).

Contexto do projeto para assistentes de IA e para o próprio autor.
Se algo neste ficheiro contradisser o código, o ficheiro está desatualizado: corrigir aqui.

---

## 1. O que é

Aplicação pessoal/comunitária de receitas. Cada utilizador regista as suas receitas,
consulta as dos outros, "ativa" as que vai cozinhar e daí sai automaticamente uma
lista de compras com os ingredientes que lhe faltam.

Objetivo secundário: servir de projeto de aprendizagem e portefólio.
Prioridade de utilização: **telemóvel** (a app é usada na cozinha).

---

## 2. Stack

### Backend (`/backend`)
- Java 21 (LTS)
- Spring Boot 3.x
- Spring Web (REST), Spring Security, Spring Data JPA, Spring Session JDBC
- Bean Validation (`jakarta.validation`)
- PostgreSQL
- Flyway (migrations versionadas — o schema **nunca** é gerado por `ddl-auto`)
- springdoc-openapi (documentação e contrato da API)
- Maven

### Frontend (`/frontend`)
- React + TypeScript
- Vite (build e dev server)
- React Router (navegação)
- TanStack Query (estado do servidor, cache, revalidação)
- `vite-plugin-pwa` (manifest + service worker)
- `@dnd-kit` (reordenação por arrastar de ingredientes e passos)
- CSS simples ou Tailwind — a decidir na primeira UI

### Infraestrutura
- Um repositório, duas pastas, **dois deploys independentes**
- **Backend:** Render, serviço web gratuito, via Dockerfile
- **Base de dados:** Neon (Postgres serverless), plano gratuito — **não** o Postgres do Render,
  que expira 30 dias após a criação e é apagado
- **Frontend:** Cloudflare Pages (build estático)
- `docker-compose.yml` na raiz para levantar Postgres + backend localmente

Limitações aceites conscientemente: o serviço do Render adormece após 15 minutos sem tráfego
e demora até um minuto a acordar; o Neon suspende a computação quando inativa. A primeira
utilização do dia é lenta. O plano gratuito do Neon dá 0,5 GB, partilhados com as fotos —
daí a compressão agressiva no cliente.

---

## 3. Arquitetura

**Isto não é uma arquitetura de microserviços — é um monólito modular com SPA.**
Um backend, uma base de dados, um frontend que o consome. A separação em serviços
independentes com bases de dados próprias não traz nada a este domínio e custa
muito em complexidade operacional. O que o autor pretendia (API + cliente separados)
é exatamente o que está descrito abaixo.

```
recipes-app/
├── backend/          # API Spring Boot
├── frontend/         # SPA React + Vite
├── docker-compose.yml
└── CLAUDE.md
```

### Organização do backend
Pacotes **por funcionalidade**, não por camada técnica:

```
com.<autor>.recipes
├── auth/
├── user/
├── recipe/
├── activation/       # receitas ativas + estado dos ingredientes
├── shoppinglist/
├── rating/
└── common/           # exceções, config, tratamento de erros
```

Cada pacote tem o seu `Controller`, `Service`, `Repository`, entidades e DTOs.
Entidades JPA **nunca** atravessam a fronteira do controller — a resposta é sempre DTO.

---

## 4. Modelo de dados

```
users
  id, username (UNIQUE), password_hash, created_at

recipes
  id, user_id → users, name, notes (TEXT), created_at, updated_at
  UNIQUE (user_id, name)

ingredients
  id, recipe_id → recipes, position, quantity (TEXT), unit (TEXT), item (TEXT)
  UNIQUE (recipe_id, position)

utensils
  id, recipe_id → recipes, name

preparation_sections
  id, recipe_id → recipes, position, title
  UNIQUE (recipe_id, position)

preparation_steps
  id, section_id → preparation_sections, position, instruction, duration_minutes (NULLABLE)
  UNIQUE (section_id, position)

recipe_photos
  id, recipe_id → recipes, position (1 ou 2), content_type, data (BYTEA)
  UNIQUE (recipe_id, position)

ratings
  id, recipe_id → recipes, user_id → users, category (FLAVOR|DIFFICULTY|PRICE), score (1..5)
  UNIQUE (recipe_id, user_id, category)

active_recipes
  id, user_id → users, recipe_id → recipes, activated_at
  UNIQUE (user_id, recipe_id)

active_recipe_ingredients
  id, active_recipe_id → active_recipes, ingredient_id → ingredients, available (BOOLEAN)
  UNIQUE (active_recipe_id, ingredient_id)
```

### Decisão central: a lista de compras não é uma tabela

A lista de compras é **derivada por query**, não armazenada:

```sql
SELECT ... FROM active_recipe_ingredients ari
JOIN active_recipes ar ON ...
WHERE ar.user_id = :userId AND ari.available = false
```

Consequência: desativar uma receita apaga a linha em `active_recipes`, o `ON DELETE CASCADE`
leva os `active_recipe_ingredients`, e os itens desaparecem da lista sozinhos. Não existe
código de sincronização a manter, logo não existe estado inconsistente possível.

O estado "tenho / não tenho" pertence à **ativação**, não à receita — porque o utilizador A
pode ativar uma receita do utilizador B, e cada um tem a sua despensa.

`position` é um inteiro guardado explicitamente. A ordem **nunca** depende da ordem
de inserção nem do `id`.

---

## 5. Regras de negócio

Cada regra abaixo deve ter teste correspondente.

**Utilizadores**
- R1. `username` é único em toda a aplicação.
- R2. Password guardada com BCrypt. Nunca em texto simples, nunca em logs.

**Receitas**
- R3. O nome da receita é único **por utilizador** (dois utilizadores podem ter "Bolo de Cenoura").
- R4. O autor é sempre o utilizador autenticado. Nunca aceitar `userId` vindo do cliente.
- R5. Só o autor pode editar ou apagar a sua receita.
- R6. Todos os utilizadores autenticados podem **ler** todas as receitas.
- R7. Máximo de 2 fotos por receita.

**Tempos de preparação**
- R8. O tempo de uma secção é a soma dos `duration_minutes` dos seus passos.
- R9. ≥ 60 minutos apresenta-se como "1h30", não "90 minutos".
- R10. Se **algum** passo da secção não tiver tempo, prefixar com "aproximadamente".
- R11. Se **nenhum** passo tiver tempo, não mostrar tempo nenhum.
- R12. Formatação de tempo é responsabilidade do **frontend**. A API devolve minutos (inteiro)
  e um booleano `partial`.

**Ativação**
- R13. Máximo de **5** receitas ativas por utilizador. A 6ª tentativa devolve `409 Conflict`.
- R14. Ao ativar, criar uma linha em `active_recipe_ingredients` por ingrediente,
  com `available = false` (assume-se que falta tudo até o utilizador dizer o contrário).
- R15. Desativar apaga a ativação e, em cascata, o estado dos ingredientes.
- R16. Qualquer utilizador pode ativar qualquer receita, incluindo as suas.

**Lista de compras**
- R17. Contém os ingredientes com `available = false` das receitas ativas do utilizador.
- R18. Agrupada por receita, mantendo a ordem original dos ingredientes.
  Itens iguais em receitas diferentes **não** são fundidos — quantidades em texto livre
  não são somáveis de forma fiável ("1 colher" + "200g").
- R19. Marcar a checkbox na lista de compras equivale a `available = true` na respetiva ativação.
- R20. Exportação para clipboard: texto simples, gerado no cliente.

**Avaliações**
- R21. Um voto por utilizador, por receita, por categoria. Votar de novo substitui o anterior.
- R22. O autor **pode** avaliar a própria receita.
- R23. A listagem mostra a média por categoria e o número de votos.

---

## 6. API

Prefixo `/api`. JSON. Nomes de recursos no plural, em inglês.

```
POST   /api/auth/register            {username, password}
POST   /api/auth/login               {username, password}
POST   /api/auth/logout
GET    /api/auth/me

GET    /api/recipes?scope=all|mine   → lista resumida (nome, autor, médias, foto de capa)
GET    /api/recipes/{id}             → receita completa
POST   /api/recipes
PUT    /api/recipes/{id}
DELETE /api/recipes/{id}

POST   /api/recipes/{id}/photos      multipart
DELETE /api/recipes/{id}/photos/{photoId}
GET    /api/recipes/{id}/photos/{photoId}   → binário, com Cache-Control

PUT    /api/recipes/{id}/ratings     {flavor?, difficulty?, price?}

GET    /api/activations
POST   /api/activations              {recipeId}
DELETE /api/activations/{id}
PATCH  /api/activations/{id}/ingredients/{ingredientId}   {available}

GET    /api/shopping-list
```

**Convenções**
- Receita é criada e atualizada como **agregado completo** (ingredientes, secções, passos
  e utensílios no mesmo payload). Não há endpoints individuais para sub-recursos —
  o formulário de edição é sempre um ecrã inteiro.
- Reordenar = enviar o agregado com os novos `position`.
- Erros seguem `ProblemDetail` (RFC 7807) via `@RestControllerAdvice`.
- `401` não autenticado, `403` sem permissão, `404` inexistente, `409` regra de negócio violada
  (nome duplicado, 6ª receita ativa), `422` validação.

---

## 7. Frontend

### Rotas
```
/login                  login e registo
/                       menu principal
/recipes                listagem + filtro (todas / minhas)
/recipes/new            formulário de criação
/recipes/:id            leitura; muda de modo se estiver ativa
/recipes/:id/edit       formulário de edição
/shopping-list          lista de compras
/about                  explicação do funcionamento
```

### Notas de UI
- O menu principal tem o botão de sair num canto.
- No ecrã de leitura, se a receita estiver ativa, cada ingrediente ganha um toggle
  ligado/desligado. (Os requisitos dizem "radio button", mas semanticamente é um
  toggle/checkbox — são estados independentes por ingrediente.)
- Alvos de toque com **mínimo 44px**: a app é usada com as mãos sujas, ao telemóvel.
- Fotos são redimensionadas e comprimidas **no cliente** antes do upload
  (máx. 1200px no lado maior, JPEG com qualidade ~0.8).
- O ecrã "Sobre" explica a composição da receita, como criar/ver e o que é uma receita ativa.

### PWA
- Instalável (manifest com ícones e `display: standalone`).
- Service worker faz cache do shell da aplicação e das receitas já visitadas,
  para funcionarem sem rede.
- Escritas offline **não** são suportadas na v1.

---

## 8. Convenções de código

**Java**
- Nomes em inglês. Comentários e mensagens de commit em português.
- DTOs como `record`. Entidades JPA como classes.
- Construtor para injeção de dependências. Sem `@Autowired` em campos.
- `@Transactional` no service, nunca no controller.
- Testes: JUnit 5 + Testcontainers (Postgres real, não H2 — evita divergências de dialeto).
- Sem `FetchType.EAGER`. Coleções carregadas com `JOIN FETCH` ou `@EntityGraph`.

**TypeScript**
- Componentes funcionais. Sem `any`.
- Tipos da API **gerados a partir do OpenAPI** — nunca escritos à mão.
- Chamadas à API isoladas em `src/api/`. Componentes não usam `fetch` diretamente.
- Estado do servidor pertence ao TanStack Query. `useState` só para estado local de UI.

**Geral**
- Nada de segredos no repositório. Configuração por variáveis de ambiente.
- Cada migration Flyway é imutável depois de aplicada. Correções fazem-se com nova migration.

---

## 9. Decisões tomadas (e o que foi descartado)

| Decisão | Escolha | Porquê |
|---|---|---|
| Arquitetura | Monólito modular + SPA | Microserviços não trazem benefício e multiplicam a complexidade de deploy |
| Repositório | Único, duas pastas | Histórico e documentação num só sítio |
| Deploy | Dois independentes | Frontend estático em CDN, não adormece, atualiza em segundos |
| Autenticação | Cookie de sessão HttpOnly + Spring Session JDBC | Cookie inacessível a JavaScript; sessões sobrevivem a reinícios do serviço |
| CSRF | Ativo, `CookieCsrfTokenRepository` | Obrigatório quando se usa cookies com `SameSite=None` |
| JWT em localStorage | **Descartado** | Vulnerável a XSS e sem revogação; não simplifica nada aqui |
| Fotos | `BYTEA` no Postgres | Evita um serviço externo; o disco dos hosts gratuitos é efémero. Reavaliar se passar de ~200MB |
| Lista de compras | Derivada por query | Impossível ficar dessincronizada |
| Frontend | React + TS + Vite | Padrão de mercado; Angular descartado por curva de aprendizagem |
| Schema | Flyway | `ddl-auto` não é aceitável fora de brincadeiras |

### Cross-origin
Frontend e backend em domínios diferentes exigem:
- CORS no backend com `allowCredentials(true)` e origem explícita (nunca `*`)
- Cookie de sessão com `SameSite=None; Secure` (obriga a HTTPS nas duas pontas)
- `credentials: 'include'` em todas as chamadas do frontend

Se isto se revelar demasiado atrito, a alternativa é servir o frontend a partir do
mesmo domínio via proxy da plataforma de CDN.

---

## 10. Pontos em aberto

- [ ] Tailwind ou CSS simples
- [ ] Pesquisa por nome de receita na listagem — provável, mas fora da v1
- [ ] Limite de tamanho por foto a aplicar no servidor

---

## 11. Fases

**Fase 0 — Esqueleto no ar.** Projetos criados, Postgres ligado, um endpoint de saúde,
um ecrã que o consome, os dois deploys a funcionar. Objetivo: eliminar cedo o risco
de infraestrutura, antes de existir funcionalidade.

**Fase 1 — Autenticação.** Registo, login, logout, sessão persistente, rotas protegidas.

**Fase 2 — Receitas.** CRUD completo com o agregado, listagem com filtro, ecrã de leitura.

**Fase 3 — Ativação e lista de compras.** Limite de 5, estado dos ingredientes,
lista derivada, cópia para clipboard.

**Fase 4 — Fotos e avaliações.**

**Fase 5 — PWA.** Manifest, service worker, cache offline, afinação para telemóvel.
