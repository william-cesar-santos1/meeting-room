-- ============================================================
-- Meeting Room — Script de Inicialização
-- ============================================================

-- ------------------------------------------------------------
-- Tabelas
-- ------------------------------------------------------------

CREATE TABLE IF NOT EXISTS rooms (
    id       BIGSERIAL    PRIMARY KEY,
    name     VARCHAR(255) NOT NULL UNIQUE,
    capacity INT          NOT NULL CHECK (capacity > 0)
);

CREATE TABLE IF NOT EXISTS reservations (
    id         BIGSERIAL    PRIMARY KEY,
    room_id    BIGINT       NOT NULL REFERENCES rooms(id),
    guest_name VARCHAR(255) NOT NULL,
    start_at   TIMESTAMP    NOT NULL,
    end_at     TIMESTAMP    NOT NULL,
    CONSTRAINT chk_reservation_period CHECK (end_at > start_at)
);

-- ------------------------------------------------------------
-- Índices
-- ------------------------------------------------------------

CREATE INDEX IF NOT EXISTS idx_reservations_room_id ON reservations (room_id);
CREATE INDEX IF NOT EXISTS idx_reservations_start_at ON reservations (start_at);
CREATE INDEX IF NOT EXISTS idx_reservations_end_at   ON reservations (end_at);

-- ------------------------------------------------------------
-- Salas
-- ------------------------------------------------------------

INSERT INTO rooms (name, capacity) VALUES
    ('Sala Apolo',       8),
    ('Sala Artemis',    12),
    ('Sala Atena',      20),
    ('Sala Hermes',      6),
    ('Sala Zeus',       30),
    ('Sala Poseidon',   10),
    ('Sala Hera',        4),
    ('Sala Ares',       16),
    ('Sala Hefesto',     8),
    ('Sala Afrodite',   14);

-- ------------------------------------------------------------
-- Reservas (passadas + futuras para cobrir os dois cenários da view)
-- ------------------------------------------------------------

-- Sala 1 — Apolo
INSERT INTO reservations (room_id, guest_name, start_at, end_at) VALUES
    (1, 'Alice Souza',     NOW() - INTERVAL '10 day' + INTERVAL '9 hour',  NOW() - INTERVAL '10 day' + INTERVAL '11 hour'),
    (1, 'Bruno Lima',      NOW() - INTERVAL '8 day'  + INTERVAL '14 hour', NOW() - INTERVAL '8 day'  + INTERVAL '16 hour'),
    (1, 'Carla Mendes',    NOW() - INTERVAL '6 day'  + INTERVAL '9 hour',  NOW() - INTERVAL '6 day'  + INTERVAL '12 hour'),
    (1, 'Diego Ferreira',  NOW() - INTERVAL '4 day'  + INTERVAL '10 hour', NOW() - INTERVAL '4 day'  + INTERVAL '11 hour'),
    (1, 'Eva Rocha',       NOW() - INTERVAL '2 day'  + INTERVAL '8 hour',  NOW() - INTERVAL '2 day'  + INTERVAL '10 hour'),
    (1, 'Fábio Gomes',     NOW() - INTERVAL '1 day'  + INTERVAL '13 hour', NOW() - INTERVAL '1 day'  + INTERVAL '15 hour'),
    (1, 'Gia Alves',       NOW() + INTERVAL '1 day'  + INTERVAL '9 hour',  NOW() + INTERVAL '1 day'  + INTERVAL '11 hour'),
    (1, 'Hugo Nunes',      NOW() + INTERVAL '3 day'  + INTERVAL '10 hour', NOW() + INTERVAL '3 day'  + INTERVAL '13 hour'),
    (1, 'Íris Costa',      NOW() + INTERVAL '5 day'  + INTERVAL '14 hour', NOW() + INTERVAL '5 day'  + INTERVAL '16 hour'),
    (1, 'João Pereira',    NOW() + INTERVAL '7 day'  + INTERVAL '9 hour',  NOW() + INTERVAL '7 day'  + INTERVAL '11 hour');

-- Sala 2 — Artemis
INSERT INTO reservations (room_id, guest_name, start_at, end_at) VALUES
    (2, 'Karen Silva',     NOW() - INTERVAL '9 day'  + INTERVAL '9 hour',  NOW() - INTERVAL '9 day'  + INTERVAL '12 hour'),
    (2, 'Lucas Martins',   NOW() - INTERVAL '7 day'  + INTERVAL '10 hour', NOW() - INTERVAL '7 day'  + INTERVAL '12 hour'),
    (2, 'Mariana Vieira',  NOW() - INTERVAL '5 day'  + INTERVAL '13 hour', NOW() - INTERVAL '5 day'  + INTERVAL '15 hour'),
    (2, 'Nathan Carvalho', NOW() - INTERVAL '3 day'  + INTERVAL '9 hour',  NOW() - INTERVAL '3 day'  + INTERVAL '11 hour'),
    (2, 'Olívia Santos',   NOW() - INTERVAL '1 day'  + INTERVAL '10 hour', NOW() - INTERVAL '1 day'  + INTERVAL '12 hour'),
    (2, 'Paulo Ribeiro',   NOW() - INTERVAL '12 hour',                     NOW() - INTERVAL '10 hour'),
    (2, 'Queila Andrade',  NOW() + INTERVAL '1 day'  + INTERVAL '9 hour',  NOW() + INTERVAL '1 day'  + INTERVAL '11 hour'),
    (2, 'Rafael Teixeira', NOW() + INTERVAL '2 day'  + INTERVAL '14 hour', NOW() + INTERVAL '2 day'  + INTERVAL '17 hour'),
    (2, 'Sara Oliveira',   NOW() + INTERVAL '4 day'  + INTERVAL '10 hour', NOW() + INTERVAL '4 day'  + INTERVAL '12 hour'),
    (2, 'Thiago Castro',   NOW() + INTERVAL '6 day'  + INTERVAL '9 hour',  NOW() + INTERVAL '6 day'  + INTERVAL '11 hour');

-- Sala 3 — Atena
INSERT INTO reservations (room_id, guest_name, start_at, end_at) VALUES
    (3, 'Ursula Freitas',  NOW() - INTERVAL '10 day' + INTERVAL '8 hour',  NOW() - INTERVAL '10 day' + INTERVAL '10 hour'),
    (3, 'Victor Moreira',  NOW() - INTERVAL '8 day'  + INTERVAL '9 hour',  NOW() - INTERVAL '8 day'  + INTERVAL '13 hour'),
    (3, 'Wanda Pinto',     NOW() - INTERVAL '6 day'  + INTERVAL '14 hour', NOW() - INTERVAL '6 day'  + INTERVAL '16 hour'),
    (3, 'Xavier Lopes',    NOW() - INTERVAL '4 day'  + INTERVAL '10 hour', NOW() - INTERVAL '4 day'  + INTERVAL '12 hour'),
    (3, 'Yasmin Correia',  NOW() - INTERVAL '2 day'  + INTERVAL '9 hour',  NOW() - INTERVAL '2 day'  + INTERVAL '11 hour'),
    (3, 'Zara Figueiredo', NOW() - INTERVAL '1 day'  + INTERVAL '13 hour', NOW() - INTERVAL '1 day'  + INTERVAL '15 hour'),
    (3, 'André Dias',      NOW() + INTERVAL '1 day'  + INTERVAL '9 hour',  NOW() + INTERVAL '1 day'  + INTERVAL '12 hour'),
    (3, 'Beatriz Neves',   NOW() + INTERVAL '3 day'  + INTERVAL '10 hour', NOW() + INTERVAL '3 day'  + INTERVAL '14 hour'),
    (3, 'Caio Sousa',      NOW() + INTERVAL '5 day'  + INTERVAL '9 hour',  NOW() + INTERVAL '5 day'  + INTERVAL '11 hour'),
    (3, 'Débora Azevedo',  NOW() + INTERVAL '7 day'  + INTERVAL '14 hour', NOW() + INTERVAL '7 day'  + INTERVAL '16 hour');

-- Sala 4 — Hermes
INSERT INTO reservations (room_id, guest_name, start_at, end_at) VALUES
    (4, 'Elias Cunha',     NOW() - INTERVAL '9 day'  + INTERVAL '10 hour', NOW() - INTERVAL '9 day'  + INTERVAL '11 hour'),
    (4, 'Flávia Ramos',    NOW() - INTERVAL '7 day'  + INTERVAL '9 hour',  NOW() - INTERVAL '7 day'  + INTERVAL '12 hour'),
    (4, 'Gustavo Borges',  NOW() - INTERVAL '5 day'  + INTERVAL '13 hour', NOW() - INTERVAL '5 day'  + INTERVAL '14 hour'),
    (4, 'Helena Melo',     NOW() - INTERVAL '3 day'  + INTERVAL '10 hour', NOW() - INTERVAL '3 day'  + INTERVAL '12 hour'),
    (4, 'Ivan Barros',     NOW() - INTERVAL '1 day'  + INTERVAL '9 hour',  NOW() - INTERVAL '1 day'  + INTERVAL '11 hour'),
    (4, 'Júlia Macedo',    NOW() - INTERVAL '6 hour',                      NOW() - INTERVAL '4 hour'),
    (4, 'Kléber Rezende',  NOW() + INTERVAL '1 day'  + INTERVAL '10 hour', NOW() + INTERVAL '1 day'  + INTERVAL '12 hour'),
    (4, 'Laura Campos',    NOW() + INTERVAL '2 day'  + INTERVAL '9 hour',  NOW() + INTERVAL '2 day'  + INTERVAL '11 hour'),
    (4, 'Márcio Duarte',   NOW() + INTERVAL '4 day'  + INTERVAL '14 hour', NOW() + INTERVAL '4 day'  + INTERVAL '16 hour'),
    (4, 'Natália Peixoto', NOW() + INTERVAL '6 day'  + INTERVAL '10 hour', NOW() + INTERVAL '6 day'  + INTERVAL '13 hour');

-- Sala 5 — Zeus
INSERT INTO reservations (room_id, guest_name, start_at, end_at) VALUES
    (5, 'Osvaldo Queiroz', NOW() - INTERVAL '10 day' + INTERVAL '9 hour',  NOW() - INTERVAL '10 day' + INTERVAL '12 hour'),
    (5, 'Patrícia Sá',     NOW() - INTERVAL '8 day'  + INTERVAL '10 hour', NOW() - INTERVAL '8 day'  + INTERVAL '13 hour'),
    (5, 'Rui Cavalcante',  NOW() - INTERVAL '6 day'  + INTERVAL '9 hour',  NOW() - INTERVAL '6 day'  + INTERVAL '11 hour'),
    (5, 'Simone Teles',    NOW() - INTERVAL '4 day'  + INTERVAL '14 hour', NOW() - INTERVAL '4 day'  + INTERVAL '17 hour'),
    (5, 'Tarcísio Viana',  NOW() - INTERVAL '2 day'  + INTERVAL '10 hour', NOW() - INTERVAL '2 day'  + INTERVAL '12 hour'),
    (5, 'Ubirajara Maia',  NOW() - INTERVAL '1 day'  + INTERVAL '9 hour',  NOW() - INTERVAL '1 day'  + INTERVAL '11 hour'),
    (5, 'Vanessa Farias',  NOW() + INTERVAL '1 day'  + INTERVAL '9 hour',  NOW() + INTERVAL '1 day'  + INTERVAL '12 hour'),
    (5, 'Wellington Lago', NOW() + INTERVAL '3 day'  + INTERVAL '10 hour', NOW() + INTERVAL '3 day'  + INTERVAL '13 hour'),
    (5, 'Ximena Paiva',    NOW() + INTERVAL '5 day'  + INTERVAL '14 hour', NOW() + INTERVAL '5 day'  + INTERVAL '16 hour'),
    (5, 'Yara Valente',    NOW() + INTERVAL '7 day'  + INTERVAL '9 hour',  NOW() + INTERVAL '7 day'  + INTERVAL '11 hour');

-- Sala 6 — Poseidon
INSERT INTO reservations (room_id, guest_name, start_at, end_at) VALUES
    (6, 'Zeno Aragão',     NOW() - INTERVAL '9 day'  + INTERVAL '9 hour',  NOW() - INTERVAL '9 day'  + INTERVAL '11 hour'),
    (6, 'Amanda Esteves',  NOW() - INTERVAL '7 day'  + INTERVAL '13 hour', NOW() - INTERVAL '7 day'  + INTERVAL '15 hour'),
    (6, 'Bernardo Fonseca',NOW() - INTERVAL '5 day'  + INTERVAL '10 hour', NOW() - INTERVAL '5 day'  + INTERVAL '12 hour'),
    (6, 'Cecília Guedes',  NOW() - INTERVAL '3 day'  + INTERVAL '9 hour',  NOW() - INTERVAL '3 day'  + INTERVAL '11 hour'),
    (6, 'Danilo Henriques',NOW() - INTERVAL '1 day'  + INTERVAL '14 hour', NOW() - INTERVAL '1 day'  + INTERVAL '16 hour'),
    (6, 'Elaine Iglesias', NOW() - INTERVAL '8 hour',                      NOW() - INTERVAL '6 hour'),
    (6, 'Fausto Jardim',   NOW() + INTERVAL '1 day'  + INTERVAL '9 hour',  NOW() + INTERVAL '1 day'  + INTERVAL '11 hour'),
    (6, 'Giovana Krug',    NOW() + INTERVAL '2 day'  + INTERVAL '10 hour', NOW() + INTERVAL '2 day'  + INTERVAL '12 hour'),
    (6, 'Henrique Luz',    NOW() + INTERVAL '4 day'  + INTERVAL '9 hour',  NOW() + INTERVAL '4 day'  + INTERVAL '12 hour'),
    (6, 'Isabela Muniz',   NOW() + INTERVAL '6 day'  + INTERVAL '14 hour', NOW() + INTERVAL '6 day'  + INTERVAL '16 hour');

-- Sala 7 — Hera
INSERT INTO reservations (room_id, guest_name, start_at, end_at) VALUES
    (7, 'Jorge Nogueira',  NOW() - INTERVAL '10 day' + INTERVAL '10 hour', NOW() - INTERVAL '10 day' + INTERVAL '11 hour'),
    (7, 'Karina Oliveira', NOW() - INTERVAL '8 day'  + INTERVAL '9 hour',  NOW() - INTERVAL '8 day'  + INTERVAL '10 hour'),
    (7, 'Leandro Porto',   NOW() - INTERVAL '6 day'  + INTERVAL '13 hour', NOW() - INTERVAL '6 day'  + INTERVAL '14 hour'),
    (7, 'Mônica Queirós',  NOW() - INTERVAL '4 day'  + INTERVAL '10 hour', NOW() - INTERVAL '4 day'  + INTERVAL '11 hour'),
    (7, 'Nelson Ribas',    NOW() - INTERVAL '2 day'  + INTERVAL '9 hour',  NOW() - INTERVAL '2 day'  + INTERVAL '10 hour'),
    (7, 'Odalys Sena',     NOW() - INTERVAL '1 day'  + INTERVAL '14 hour', NOW() - INTERVAL '1 day'  + INTERVAL '15 hour'),
    (7, 'Pedro Tavares',   NOW() + INTERVAL '1 day'  + INTERVAL '10 hour', NOW() + INTERVAL '1 day'  + INTERVAL '11 hour'),
    (7, 'Quirina Urzua',   NOW() + INTERVAL '3 day'  + INTERVAL '9 hour',  NOW() + INTERVAL '3 day'  + INTERVAL '10 hour'),
    (7, 'Reinaldo Vaz',    NOW() + INTERVAL '5 day'  + INTERVAL '13 hour', NOW() + INTERVAL '5 day'  + INTERVAL '14 hour'),
    (7, 'Sueli Xavier',    NOW() + INTERVAL '7 day'  + INTERVAL '10 hour', NOW() + INTERVAL '7 day'  + INTERVAL '12 hour');

-- Sala 8 — Ares
INSERT INTO reservations (room_id, guest_name, start_at, end_at) VALUES
    (8, 'Teodora Ybarra',  NOW() - INTERVAL '9 day'  + INTERVAL '9 hour',  NOW() - INTERVAL '9 day'  + INTERVAL '12 hour'),
    (8, 'Ulrico Zanetti',  NOW() - INTERVAL '7 day'  + INTERVAL '10 hour', NOW() - INTERVAL '7 day'  + INTERVAL '13 hour'),
    (8, 'Vera Alcântara',  NOW() - INTERVAL '5 day'  + INTERVAL '9 hour',  NOW() - INTERVAL '5 day'  + INTERVAL '11 hour'),
    (8, 'Walter Brito',    NOW() - INTERVAL '3 day'  + INTERVAL '14 hour', NOW() - INTERVAL '3 day'  + INTERVAL '16 hour'),
    (8, 'Xênia Câmara',    NOW() - INTERVAL '1 day'  + INTERVAL '10 hour', NOW() - INTERVAL '1 day'  + INTERVAL '12 hour'),
    (8, 'Yago Drumond',    NOW() - INTERVAL '5 hour',                      NOW() - INTERVAL '3 hour'),
    (8, 'Zilma Evangelista',NOW() + INTERVAL '1 day' + INTERVAL '9 hour',  NOW() + INTERVAL '1 day'  + INTERVAL '12 hour'),
    (8, 'Artur Falcão',    NOW() + INTERVAL '2 day'  + INTERVAL '10 hour', NOW() + INTERVAL '2 day'  + INTERVAL '14 hour'),
    (8, 'Brenda Galvão',   NOW() + INTERVAL '4 day'  + INTERVAL '9 hour',  NOW() + INTERVAL '4 day'  + INTERVAL '11 hour'),
    (8, 'César Hollanda',  NOW() + INTERVAL '6 day'  + INTERVAL '14 hour', NOW() + INTERVAL '6 day'  + INTERVAL '17 hour');

-- Sala 9 — Hefesto
INSERT INTO reservations (room_id, guest_name, start_at, end_at) VALUES
    (9, 'Diana Ivo',       NOW() - INTERVAL '10 day' + INTERVAL '9 hour',  NOW() - INTERVAL '10 day' + INTERVAL '11 hour'),
    (9, 'Eduardo Jatobá',  NOW() - INTERVAL '8 day'  + INTERVAL '13 hour', NOW() - INTERVAL '8 day'  + INTERVAL '15 hour'),
    (9, 'Fernanda Krause', NOW() - INTERVAL '6 day'  + INTERVAL '10 hour', NOW() - INTERVAL '6 day'  + INTERVAL '12 hour'),
    (9, 'Gabriel Leite',   NOW() - INTERVAL '4 day'  + INTERVAL '9 hour',  NOW() - INTERVAL '4 day'  + INTERVAL '11 hour'),
    (9, 'Helena Maciel',   NOW() - INTERVAL '2 day'  + INTERVAL '14 hour', NOW() - INTERVAL '2 day'  + INTERVAL '16 hour'),
    (9, 'Igor Novaes',     NOW() - INTERVAL '1 day'  + INTERVAL '10 hour', NOW() - INTERVAL '1 day'  + INTERVAL '12 hour'),
    (9, 'Joana Outeiro',   NOW() + INTERVAL '1 day'  + INTERVAL '9 hour',  NOW() + INTERVAL '1 day'  + INTERVAL '11 hour'),
    (9, 'Klaus Picanço',   NOW() + INTERVAL '3 day'  + INTERVAL '10 hour', NOW() + INTERVAL '3 day'  + INTERVAL '13 hour'),
    (9, 'Luana Querido',   NOW() + INTERVAL '5 day'  + INTERVAL '9 hour',  NOW() + INTERVAL '5 day'  + INTERVAL '11 hour'),
    (9, 'Marcos Rêgo',     NOW() + INTERVAL '7 day'  + INTERVAL '14 hour', NOW() + INTERVAL '7 day'  + INTERVAL '16 hour');

-- Sala 10 — Afrodite
INSERT INTO reservations (room_id, guest_name, start_at, end_at) VALUES
    (10, 'Nina Silveira',   NOW() - INTERVAL '9 day'  + INTERVAL '9 hour',  NOW() - INTERVAL '9 day'  + INTERVAL '12 hour'),
    (10, 'Otávio Torres',   NOW() - INTERVAL '7 day'  + INTERVAL '10 hour', NOW() - INTERVAL '7 day'  + INTERVAL '12 hour'),
    (10, 'Paula Ulhôa',     NOW() - INTERVAL '5 day'  + INTERVAL '13 hour', NOW() - INTERVAL '5 day'  + INTERVAL '15 hour'),
    (10, 'Quirino Vidal',   NOW() - INTERVAL '3 day'  + INTERVAL '9 hour',  NOW() - INTERVAL '3 day'  + INTERVAL '11 hour'),
    (10, 'Roberta Werneck', NOW() - INTERVAL '1 day'  + INTERVAL '10 hour', NOW() - INTERVAL '1 day'  + INTERVAL '13 hour'),
    (10, 'Silvio Xavier',   NOW() - INTERVAL '7 hour',                      NOW() - INTERVAL '5 hour'),
    (10, 'Tânia Yamada',    NOW() + INTERVAL '1 day'  + INTERVAL '9 hour',  NOW() + INTERVAL '1 day'  + INTERVAL '11 hour'),
    (10, 'Ulisses Zago',    NOW() + INTERVAL '2 day'  + INTERVAL '14 hour', NOW() + INTERVAL '2 day'  + INTERVAL '16 hour'),
    (10, 'Vânia Abreu',     NOW() + INTERVAL '4 day'  + INTERVAL '10 hour', NOW() + INTERVAL '4 day'  + INTERVAL '12 hour'),
    (10, 'William Braga',   NOW() + INTERVAL '6 day'  + INTERVAL '9 hour',  NOW() + INTERVAL '6 day'  + INTERVAL '11 hour');

-- ------------------------------------------------------------
-- View: ocupação por sala
--   horas_utilizadas  → reservas cujo end_at já passou (NOW())
--   horas_reservadas  → reservas cujo start_at ainda não chegou
-- ------------------------------------------------------------

CREATE OR REPLACE VIEW vw_room_occupancy AS
SELECT
    r.id                                                      AS room_id,
    r.name                                                    AS room_name,
    r.capacity,
    COUNT(res.id)                                             AS total_reservas,
    -- Horas já utilizadas (reservas passadas: end_at < NOW())
    COALESCE(
        SUM(
            EXTRACT(EPOCH FROM (res.end_at - res.start_at)) / 3600.0
        ) FILTER (WHERE res.end_at < NOW()),
    0)                                                        AS horas_utilizadas,
    -- Horas ainda reservadas (reservas futuras: start_at > NOW())
    COALESCE(
        SUM(
            EXTRACT(EPOCH FROM (res.end_at - res.start_at)) / 3600.0
        ) FILTER (WHERE res.start_at > NOW()),
    0)                                                        AS horas_reservadas
FROM rooms r
    LEFT JOIN reservations res ON res.room_id = r.id
GROUP BY r.id, r.name, r.capacity
ORDER BY r.id;

-- ------------------------------------------------------------
-- Verificações
-- ------------------------------------------------------------

SELECT COUNT(*) AS total_salas      FROM rooms;
SELECT COUNT(*) AS total_reservas   FROM reservations;
SELECT * FROM vw_room_occupancy;

