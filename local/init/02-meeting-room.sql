-- ============================================================
-- Aula 3 — Segurança JWT
-- Evoluções de schema em relação ao script 01-meeting-room.sql
-- ============================================================

-- ------------------------------------------------------------
-- 1. Tabela de usuários
-- Criada ANTES das alterações em reservations pois será
-- referenciada como chave estrangeira.
--
-- A coluna password armazena o hash Argon2id — nunca texto puro.
-- Parâmetros (recomendação OWASP 2024): m=65536, t=3, p=1
-- ------------------------------------------------------------

CREATE TABLE IF NOT EXISTS users (
    id       BIGSERIAL    PRIMARY KEY,
    name     VARCHAR(255) NOT NULL,
    username VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role     VARCHAR(20)  NOT NULL CHECK (role IN ('USER', 'ADMIN'))
);

-- Senhas originais para demo: admin→admin123 | joao/maria→user123
INSERT INTO users (name, username, password, role) VALUES
    ('Administrador',
     'admin',
     '$argon2id$v=19$m=65536,t=3,p=1$AV1Ilq7Gs1bRSJOHl34vqg$/q/Mxm2q8EvTtGVKnRajnhvA/nDlkNc4Egm9cQ52JfQ',
     'ADMIN'),

    ('Joao Silva',
     'joao',
     '$argon2id$v=19$m=65536,t=3,p=1$iylR4wPK1FyRXF1rGouUTg$VIG5lorwBM0DW0Nb1cTbywvZSNCZ1VyaUmlb9VLU4YI',
     'USER'),

    ('Maria Santos',
     'maria',
     '$argon2id$v=19$m=65536,t=3,p=1$7hbIXl+Vz4x+lrn06RYNfg$8AWmhcqpRARA7nYSjTtyhJy/YptCpr6CmtVcRKqA3tc',
     'USER');

-- ------------------------------------------------------------
-- 2. Coluna created_by em reservations → FK para users
--
-- Substitui o VARCHAR anterior por BIGINT com chave estrangeira.
-- Metade das reservas vinculadas a joao (id=2), metade a maria (id=3).
-- ------------------------------------------------------------

-- Adiciona como NULLABLE primeiro para popular os dados existentes
ALTER TABLE reservations ADD COLUMN created_by BIGINT;

-- Reservas com id ímpar → joao (id=2)
UPDATE reservations SET created_by = 2 WHERE id % 2 = 1;

-- Reservas com id par → maria (id=3)
UPDATE reservations SET created_by = 3 WHERE id % 2 = 0;

-- Com todos os registros populados, torna NOT NULL
ALTER TABLE reservations ALTER COLUMN created_by SET NOT NULL;

-- Chave estrangeira para users
ALTER TABLE reservations
    ADD CONSTRAINT fk_reservations_created_by
    FOREIGN KEY (created_by) REFERENCES users(id);

-- Índice para consultas por usuário
CREATE INDEX IF NOT EXISTS idx_reservations_created_by ON reservations (created_by);

-- ------------------------------------------------------------
-- Verificações
-- ------------------------------------------------------------

SELECT id, username, role, LEFT(password, 30) || '...' AS password_hash FROM users;

SELECT r.id, r.guest_name, u.username AS created_by
FROM reservations r
JOIN users u ON u.id = r.created_by
ORDER BY r.id
LIMIT 10;
