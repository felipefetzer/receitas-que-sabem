-- ============================================================
-- Utilizadores
-- ============================================================
CREATE TABLE users (
    id            BIGSERIAL PRIMARY KEY,
    username      VARCHAR(50)  NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT users_username_uk UNIQUE (username)
);

-- ============================================================
-- Receitas e o seu agregado
-- ============================================================
CREATE TABLE recipes (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    name       VARCHAR(120) NOT NULL,
    notes      TEXT,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    -- R3: o nome é único por utilizador, não globalmente
    CONSTRAINT recipes_user_name_uk UNIQUE (user_id, name)
);

CREATE TABLE ingredients (
    id        BIGSERIAL PRIMARY KEY,
    recipe_id BIGINT       NOT NULL REFERENCES recipes (id) ON DELETE CASCADE,
    position  INT          NOT NULL,
    quantity  VARCHAR(50),
    unit      VARCHAR(50),
    item      VARCHAR(150) NOT NULL
);
CREATE INDEX ingredients_recipe_ix ON ingredients (recipe_id);

CREATE TABLE utensils (
    id        BIGSERIAL PRIMARY KEY,
    recipe_id BIGINT       NOT NULL REFERENCES recipes (id) ON DELETE CASCADE,
    name      VARCHAR(150) NOT NULL
);
CREATE INDEX utensils_recipe_ix ON utensils (recipe_id);

CREATE TABLE preparation_sections (
    id        BIGSERIAL PRIMARY KEY,
    recipe_id BIGINT       NOT NULL REFERENCES recipes (id) ON DELETE CASCADE,
    position  INT          NOT NULL,
    title     VARCHAR(120) NOT NULL
);
CREATE INDEX preparation_sections_recipe_ix ON preparation_sections (recipe_id);

CREATE TABLE preparation_steps (
    id               BIGSERIAL PRIMARY KEY,
    section_id       BIGINT NOT NULL REFERENCES preparation_sections (id) ON DELETE CASCADE,
    position         INT    NOT NULL,
    instruction      TEXT   NOT NULL,
    duration_minutes INT
);
CREATE INDEX preparation_steps_section_ix ON preparation_steps (section_id);

-- ============================================================
-- Ativações (receitas que estou a fazer) e estado da despensa
-- ============================================================
CREATE TABLE activations (
    id           BIGSERIAL PRIMARY KEY,
    user_id      BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    recipe_id    BIGINT      NOT NULL REFERENCES recipes (id) ON DELETE CASCADE,
    activated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT activations_user_recipe_uk UNIQUE (user_id, recipe_id)
);

-- O estado "tenho / não tenho" pertence à ativação, não à receita:
-- dois utilizadores podem ativar a mesma receita e ter despensas diferentes.
CREATE TABLE activation_ingredients (
    id            BIGSERIAL PRIMARY KEY,
    activation_id BIGINT  NOT NULL REFERENCES activations (id) ON DELETE CASCADE,
    ingredient_id BIGINT  NOT NULL REFERENCES ingredients (id) ON DELETE CASCADE,
    available     BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT activation_ingredients_uk UNIQUE (activation_id, ingredient_id)
);

-- ============================================================
-- Spring Session: sessões guardadas na base de dados, para
-- sobreviverem aos reinícios do serviço no Render.
-- ============================================================
CREATE TABLE SPRING_SESSION (
    PRIMARY_ID            CHAR(36) NOT NULL,
    SESSION_ID            CHAR(36) NOT NULL,
    CREATION_TIME         BIGINT   NOT NULL,
    LAST_ACCESS_TIME      BIGINT   NOT NULL,
    MAX_INACTIVE_INTERVAL INT      NOT NULL,
    EXPIRY_TIME           BIGINT   NOT NULL,
    PRINCIPAL_NAME        VARCHAR(100),
    CONSTRAINT SPRING_SESSION_PK PRIMARY KEY (PRIMARY_ID)
);
CREATE UNIQUE INDEX SPRING_SESSION_IX1 ON SPRING_SESSION (SESSION_ID);
CREATE INDEX SPRING_SESSION_IX2 ON SPRING_SESSION (EXPIRY_TIME);
CREATE INDEX SPRING_SESSION_IX3 ON SPRING_SESSION (PRINCIPAL_NAME, EXPIRY_TIME);

CREATE TABLE SPRING_SESSION_ATTRIBUTES (
    SESSION_PRIMARY_ID CHAR(36)     NOT NULL,
    ATTRIBUTE_NAME     VARCHAR(200) NOT NULL,
    ATTRIBUTE_BYTES    BYTEA        NOT NULL,
    CONSTRAINT SPRING_SESSION_ATTRIBUTES_PK PRIMARY KEY (SESSION_PRIMARY_ID, ATTRIBUTE_NAME),
    CONSTRAINT SPRING_SESSION_ATTRIBUTES_FK FOREIGN KEY (SESSION_PRIMARY_ID)
        REFERENCES SPRING_SESSION (PRIMARY_ID) ON DELETE CASCADE
);
