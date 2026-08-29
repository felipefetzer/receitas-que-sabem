-- Primeira migration. Serve para provar que o Flyway corre e consegue
-- escrever na base de dados. As tabelas do domínio chegam na Fase 1.

CREATE TABLE app_info (
    id         BIGSERIAL PRIMARY KEY,
    name       TEXT        NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

INSERT INTO app_info (name) VALUES ('Receitas que Sabem');
