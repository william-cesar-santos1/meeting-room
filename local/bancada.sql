-- ============================================================
-- Sistema Bancário — Script de Inicialização
-- Banco: banco_db
-- ============================================================
-- Senhas: todos os usuários têm a senha  ->  senha123
-- ============================================================
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- ------------------------------------------------------------
-- Tabelas
-- ------------------------------------------------------------

CREATE TABLE IF NOT EXISTS cliente (
    id    BIGSERIAL    PRIMARY KEY,
    nome  VARCHAR(255) NOT NULL,
    cpf   VARCHAR(14)  NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    senha VARCHAR(255) NOT NULL,
    role  VARCHAR(20)  NOT NULL DEFAULT 'CLIENTE'
    );

CREATE TABLE IF NOT EXISTS conta (
    id         BIGSERIAL      PRIMARY KEY,
    numero     VARCHAR(20)    NOT NULL UNIQUE,
    tipo       VARCHAR(20)    NOT NULL CHECK (tipo IN ('CORRENTE', 'POUPANCA', 'ELETRONICA')),
    cliente_id BIGINT         NOT NULL REFERENCES cliente(id)
    );

CREATE TABLE IF NOT EXISTS transacao (
    id               BIGSERIAL      PRIMARY KEY,
    tipo             VARCHAR(25)    NOT NULL CHECK (tipo IN ('DEPOSITO', 'SAQUE', 'TRANSFERENCIA')),
    valor            DECIMAL(15, 2) NOT NULL,
    data_hora        TIMESTAMP      NOT NULL DEFAULT NOW(),
    conta_origem_id  BIGINT         REFERENCES conta(id),
    conta_destino_id BIGINT         REFERENCES conta(id)
    );

CREATE OR REPLACE VIEW view_saldo AS
SELECT
    c.id,
    c.numero,
    c.tipo,
    COALESCE(SUM(
                     CASE
                         WHEN t.tipo = 'DEPOSITO'      AND t.conta_destino_id = c.id THEN  t.valor
                         WHEN t.tipo = 'SAQUE'         AND t.conta_origem_id  = c.id THEN  t.valor   -- já negativo
                         WHEN t.tipo = 'TRANSFERENCIA' AND t.conta_destino_id = c.id THEN  t.valor   -- crédito
                         WHEN t.tipo = 'TRANSFERENCIA' AND t.conta_origem_id  = c.id THEN -t.valor   -- débito
                         ELSE 0
                         END
             ), 0) AS saldo
FROM conta c
         LEFT JOIN transacao t
                   ON c.id = t.conta_origem_id
                       OR c.id = t.conta_destino_id
GROUP BY c.id, c.numero, c.tipo
ORDER BY c.id;

-- ------------------------------------------------------------
-- Índices
-- ------------------------------------------------------------

CREATE INDEX IF NOT EXISTS idx_conta_cliente    ON conta     (cliente_id);
CREATE INDEX IF NOT EXISTS idx_trans_origem     ON transacao (conta_origem_id);
CREATE INDEX IF NOT EXISTS idx_trans_destino    ON transacao (conta_destino_id);
CREATE INDEX IF NOT EXISTS idx_trans_data_hora  ON transacao (data_hora);
CREATE INDEX IF NOT EXISTS idx_trans_tipo       ON transacao (tipo);

-- ------------------------------------------------------------
-- Dados de exemplo
-- ------------------------------------------------------------

INSERT INTO cliente (nome, cpf, email, senha, role)
VALUES
    ('Alice Silva', '000.000.000-01', 'alice.silva@bancada.com.br', crypt('senha123', gen_salt('bf', 10)), 'GERENTE'),
    ('Bob Santos', '000.000.000-02', 'bob.santos@bancada.com.br', crypt('senha123', gen_salt('bf', 10)), 'GERENTE'),
    ('Carlos Oliveira', '000.000.000-03', 'carlos.oliveira@bancada.com.br', crypt('senha123', gen_salt('bf', 10)), 'CLIENTE'),
    ('Diana Souza', '000.000.000-04', 'diana.souza@bancada.com.br', crypt('senha123', gen_salt('bf', 10)), 'CLIENTE'),
    ('Eduardo Ferreira', '000.000.000-05', 'eduardo.ferreira@bancada.com.br', crypt('senha123', gen_salt('bf', 10)), 'CLIENTE'),
    ('Fernanda Lima', '000.000.000-06', 'fernanda.lima@bancada.com.br', crypt('senha123', gen_salt('bf', 10)), 'CLIENTE'),
    ('Gabriel Gomes', '000.000.000-07', 'gabriel.gomes@bancada.com.br', crypt('senha123', gen_salt('bf', 10)), 'CLIENTE'),
    ('Helena Costa', '000.000.000-08', 'helena.costa@bancada.com.br', crypt('senha123', gen_salt('bf', 10)), 'CLIENTE'),
    ('Igor Ribeiro', '000.000.000-09', 'igor.ribeiro@bancada.com.br', crypt('senha123', gen_salt('bf', 10)), 'CLIENTE'),
    ('Juliana Martins', '000.000.000-10', 'juliana.martins@bancada.com.br', crypt('senha123', gen_salt('bf', 10)), 'CLIENTE');

INSERT INTO conta (numero, tipo, cliente_id)
VALUES
    ('0001-1', 'CORRENTE', 3),
    ('0002-2', 'POUPANCA', 4),
    ('0003-3', 'ELETRONICA', 5),
    ('0004-1', 'CORRENTE', 6),
    ('0005-2', 'POUPANCA', 7),
    ('0006-3', 'ELETRONICA', 8),
    ('0007-1', 'CORRENTE', 9),
    ('0008-2', 'POUPANCA', 10);

INSERT INTO transacao (tipo, valor, data_hora, conta_origem_id, conta_destino_id)
VALUES
    -- Depósitos iniciais
    ('DEPOSITO', 500.00, NOW() - INTERVAL '10 day', NULL, 1),
    ('DEPOSITO', 800.00, NOW() - INTERVAL '9 day', NULL, 2),
    ('DEPOSITO', 450.00, NOW() - INTERVAL '8 day', NULL, 4),
    ('DEPOSITO', 900.00, NOW() - INTERVAL '7 day', NULL, 5),
    ('DEPOSITO', 700.00, NOW() - INTERVAL '6 day', NULL, 7),
    ('DEPOSITO', 300.00, NOW() - INTERVAL '5 day', NULL, 8),
    -- Saques iniciais
    ('SAQUE', -120.00, NOW() - INTERVAL '4 day', 1, NULL),
    ('SAQUE', -80.00, NOW() - INTERVAL '3 day', 2, NULL),
    ('SAQUE', -60.00, NOW() - INTERVAL '2 day', 4, NULL),
    ('SAQUE', -150.00, NOW() - INTERVAL '1 day', 5, NULL),
    -- Transferências (valor positivo, de conta_origem → conta_destino)
    ('TRANSFERENCIA', 200.00, NOW() - INTERVAL '20 hour', 1, 2),
    ('TRANSFERENCIA', 90.00, NOW() - INTERVAL '18 hour', 2, 3),
    ('TRANSFERENCIA', 110.00, NOW() - INTERVAL '16 hour', 3, 4),
    ('TRANSFERENCIA', 75.00, NOW() - INTERVAL '14 hour', 4, 6),
    ('TRANSFERENCIA', 50.00, NOW() - INTERVAL '12 hour', 6, 7),
    ('TRANSFERENCIA', 130.00, NOW() - INTERVAL '10 hour', 7, 8),
    ('TRANSFERENCIA', 33.00, NOW() - INTERVAL '1 minute', 1, 2),
    ('TRANSFERENCIA', 46.00, NOW() - INTERVAL '2 minute', 2, 3),
    ('TRANSFERENCIA', 59.00, NOW() - INTERVAL '3 minute', 3, 4),
    ('TRANSFERENCIA', 72.00, NOW() - INTERVAL '4 minute', 4, 5),
    ('TRANSFERENCIA', 85.00, NOW() - INTERVAL '5 minute', 5, 6),
    ('TRANSFERENCIA', 98.00, NOW() - INTERVAL '6 minute', 6, 7),
    ('TRANSFERENCIA', 111.00, NOW() - INTERVAL '7 minute', 7, 8),
    ('TRANSFERENCIA', 124.00, NOW() - INTERVAL '8 minute', 8, 1),
    ('TRANSFERENCIA', 137.00, NOW() - INTERVAL '9 minute', 1, 2),
    ('TRANSFERENCIA', 150.00, NOW() - INTERVAL '10 minute', 2, 3),
    ('TRANSFERENCIA', 163.00, NOW() - INTERVAL '11 minute', 3, 4),
    ('TRANSFERENCIA', 176.00, NOW() - INTERVAL '12 minute', 4, 5),
    ('TRANSFERENCIA', 189.00, NOW() - INTERVAL '13 minute', 5, 6),
    ('TRANSFERENCIA', 202.00, NOW() - INTERVAL '14 minute', 6, 7),
    ('TRANSFERENCIA', 215.00, NOW() - INTERVAL '15 minute', 7, 8),
    ('TRANSFERENCIA', 228.00, NOW() - INTERVAL '16 minute', 8, 1),
    ('TRANSFERENCIA', 241.00, NOW() - INTERVAL '17 minute', 1, 2),
    ('TRANSFERENCIA', 254.00, NOW() - INTERVAL '18 minute', 2, 3),
    ('TRANSFERENCIA', 267.00, NOW() - INTERVAL '19 minute', 3, 4),
    ('TRANSFERENCIA', 280.00, NOW() - INTERVAL '20 minute', 4, 5),
    ('TRANSFERENCIA', 293.00, NOW() - INTERVAL '21 minute', 5, 6),
    ('TRANSFERENCIA', 306.00, NOW() - INTERVAL '22 minute', 6, 7),
    ('TRANSFERENCIA', 319.00, NOW() - INTERVAL '23 minute', 7, 8),
    ('TRANSFERENCIA', 332.00, NOW() - INTERVAL '24 minute', 8, 1),
    ('TRANSFERENCIA', 345.00, NOW() - INTERVAL '25 minute', 1, 2),
    ('TRANSFERENCIA', 358.00, NOW() - INTERVAL '26 minute', 2, 3),
    ('TRANSFERENCIA', 21.00, NOW() - INTERVAL '27 minute', 3, 4),
    ('TRANSFERENCIA', 34.00, NOW() - INTERVAL '28 minute', 4, 5),
    ('TRANSFERENCIA', 47.00, NOW() - INTERVAL '29 minute', 5, 6),
    ('TRANSFERENCIA', 60.00, NOW() - INTERVAL '30 minute', 6, 7),
    ('TRANSFERENCIA', 73.00, NOW() - INTERVAL '31 minute', 7, 8),
    ('TRANSFERENCIA', 86.00, NOW() - INTERVAL '32 minute', 8, 1),
    ('TRANSFERENCIA', 99.00, NOW() - INTERVAL '33 minute', 1, 2),
    ('TRANSFERENCIA', 112.00, NOW() - INTERVAL '34 minute', 2, 3),
    ('TRANSFERENCIA', 125.00, NOW() - INTERVAL '35 minute', 3, 4),
    ('TRANSFERENCIA', 138.00, NOW() - INTERVAL '36 minute', 4, 5),
    ('TRANSFERENCIA', 151.00, NOW() - INTERVAL '37 minute', 5, 6),
    ('TRANSFERENCIA', 164.00, NOW() - INTERVAL '38 minute', 6, 7),
    ('TRANSFERENCIA', 177.00, NOW() - INTERVAL '39 minute', 7, 8),
    ('TRANSFERENCIA', 190.00, NOW() - INTERVAL '40 minute', 8, 1),
    ('TRANSFERENCIA', 203.00, NOW() - INTERVAL '41 minute', 1, 2),
    ('TRANSFERENCIA', 216.00, NOW() - INTERVAL '42 minute', 2, 3),
    ('TRANSFERENCIA', 229.00, NOW() - INTERVAL '43 minute', 3, 4),
    ('TRANSFERENCIA', 242.00, NOW() - INTERVAL '44 minute', 4, 5),
    ('TRANSFERENCIA', 255.00, NOW() - INTERVAL '45 minute', 5, 6),
    ('TRANSFERENCIA', 268.00, NOW() - INTERVAL '46 minute', 6, 7),
    ('TRANSFERENCIA', 281.00, NOW() - INTERVAL '47 minute', 7, 8),
    ('TRANSFERENCIA', 294.00, NOW() - INTERVAL '48 minute', 8, 1),
    ('TRANSFERENCIA', 307.00, NOW() - INTERVAL '49 minute', 1, 2),
    ('TRANSFERENCIA', 320.00, NOW() - INTERVAL '50 minute', 2, 3),
    ('TRANSFERENCIA', 333.00, NOW() - INTERVAL '51 minute', 3, 4),
    ('TRANSFERENCIA', 346.00, NOW() - INTERVAL '52 minute', 4, 5),
    ('TRANSFERENCIA', 359.00, NOW() - INTERVAL '53 minute', 5, 6),
    ('TRANSFERENCIA', 22.00, NOW() - INTERVAL '54 minute', 6, 7),
    ('TRANSFERENCIA', 35.00, NOW() - INTERVAL '55 minute', 7, 8),
    ('TRANSFERENCIA', 48.00, NOW() - INTERVAL '56 minute', 8, 1),
    ('TRANSFERENCIA', 61.00, NOW() - INTERVAL '57 minute', 1, 2),
    ('TRANSFERENCIA', 74.00, NOW() - INTERVAL '58 minute', 2, 3),
    ('TRANSFERENCIA', 87.00, NOW() - INTERVAL '59 minute', 3, 4),
    ('TRANSFERENCIA', 100.00, NOW() - INTERVAL '60 minute', 4, 5),
    ('TRANSFERENCIA', 113.00, NOW() - INTERVAL '61 minute', 5, 6),
    ('TRANSFERENCIA', 126.00, NOW() - INTERVAL '62 minute', 6, 7),
    ('TRANSFERENCIA', 139.00, NOW() - INTERVAL '63 minute', 7, 8),
    ('TRANSFERENCIA', 152.00, NOW() - INTERVAL '64 minute', 8, 1),
    ('TRANSFERENCIA', 165.00, NOW() - INTERVAL '65 minute', 1, 2),
    ('TRANSFERENCIA', 178.00, NOW() - INTERVAL '66 minute', 2, 3),
    ('TRANSFERENCIA', 191.00, NOW() - INTERVAL '67 minute', 3, 4),
    ('TRANSFERENCIA', 204.00, NOW() - INTERVAL '68 minute', 4, 5),
    ('TRANSFERENCIA', 217.00, NOW() - INTERVAL '69 minute', 5, 6),
    ('TRANSFERENCIA', 230.00, NOW() - INTERVAL '70 minute', 6, 7),
    ('TRANSFERENCIA', 243.00, NOW() - INTERVAL '71 minute', 7, 8),
    ('TRANSFERENCIA', 256.00, NOW() - INTERVAL '72 minute', 8, 1),
    ('TRANSFERENCIA', 269.00, NOW() - INTERVAL '73 minute', 1, 2),
    ('TRANSFERENCIA', 282.00, NOW() - INTERVAL '74 minute', 2, 3),
    ('TRANSFERENCIA', 295.00, NOW() - INTERVAL '75 minute', 3, 4),
    ('TRANSFERENCIA', 308.00, NOW() - INTERVAL '76 minute', 4, 5),
    ('TRANSFERENCIA', 321.00, NOW() - INTERVAL '77 minute', 5, 6),
    ('TRANSFERENCIA', 334.00, NOW() - INTERVAL '78 minute', 6, 7),
    ('TRANSFERENCIA', 347.00, NOW() - INTERVAL '79 minute', 7, 8),
    ('TRANSFERENCIA', 360.00, NOW() - INTERVAL '80 minute', 8, 1),
    ('TRANSFERENCIA', 23.00, NOW() - INTERVAL '81 minute', 1, 2),
    ('TRANSFERENCIA', 36.00, NOW() - INTERVAL '82 minute', 2, 3),
    ('TRANSFERENCIA', 49.00, NOW() - INTERVAL '83 minute', 3, 4),
    ('TRANSFERENCIA', 62.00, NOW() - INTERVAL '84 minute', 4, 5),
    ('TRANSFERENCIA', 75.00, NOW() - INTERVAL '85 minute', 5, 6),
    ('TRANSFERENCIA', 88.00, NOW() - INTERVAL '86 minute', 6, 7),
    ('TRANSFERENCIA', 101.00, NOW() - INTERVAL '87 minute', 7, 8),
    ('TRANSFERENCIA', 114.00, NOW() - INTERVAL '88 minute', 8, 1),
    ('TRANSFERENCIA', 127.00, NOW() - INTERVAL '89 minute', 1, 2),
    ('TRANSFERENCIA', 140.00, NOW() - INTERVAL '90 minute', 2, 3),
    ('TRANSFERENCIA', 153.00, NOW() - INTERVAL '91 minute', 3, 4),
    ('TRANSFERENCIA', 166.00, NOW() - INTERVAL '92 minute', 4, 5),
    ('TRANSFERENCIA', 179.00, NOW() - INTERVAL '93 minute', 5, 6),
    ('TRANSFERENCIA', 192.00, NOW() - INTERVAL '94 minute', 6, 7),
    ('TRANSFERENCIA', 205.00, NOW() - INTERVAL '95 minute', 7, 8),
    ('TRANSFERENCIA', 218.00, NOW() - INTERVAL '96 minute', 8, 1),
    ('TRANSFERENCIA', 231.00, NOW() - INTERVAL '97 minute', 1, 2),
    ('TRANSFERENCIA', 244.00, NOW() - INTERVAL '98 minute', 2, 3),
    ('TRANSFERENCIA', 257.00, NOW() - INTERVAL '99 minute', 3, 4),
    ('TRANSFERENCIA', 270.00, NOW() - INTERVAL '100 minute', 4, 5),
    ('TRANSFERENCIA', 283.00, NOW() - INTERVAL '101 minute', 5, 6),
    ('TRANSFERENCIA', 296.00, NOW() - INTERVAL '102 minute', 6, 7),
    ('TRANSFERENCIA', 309.00, NOW() - INTERVAL '103 minute', 7, 8),
    ('TRANSFERENCIA', 322.00, NOW() - INTERVAL '104 minute', 8, 1),
    ('TRANSFERENCIA', 335.00, NOW() - INTERVAL '105 minute', 1, 2),
    ('TRANSFERENCIA', 348.00, NOW() - INTERVAL '106 minute', 2, 3),
    ('TRANSFERENCIA', 361.00, NOW() - INTERVAL '107 minute', 3, 4),
    ('TRANSFERENCIA', 24.00, NOW() - INTERVAL '108 minute', 4, 5),
    ('TRANSFERENCIA', 37.00, NOW() - INTERVAL '109 minute', 5, 6),
    ('TRANSFERENCIA', 50.00, NOW() - INTERVAL '110 minute', 6, 7),
    ('TRANSFERENCIA', 63.00, NOW() - INTERVAL '111 minute', 7, 8),
    ('TRANSFERENCIA', 76.00, NOW() - INTERVAL '112 minute', 8, 1),
    ('TRANSFERENCIA', 89.00, NOW() - INTERVAL '113 minute', 1, 2),
    ('TRANSFERENCIA', 102.00, NOW() - INTERVAL '114 minute', 2, 3),
    ('TRANSFERENCIA', 115.00, NOW() - INTERVAL '115 minute', 3, 4),
    ('TRANSFERENCIA', 128.00, NOW() - INTERVAL '116 minute', 4, 5),
    ('TRANSFERENCIA', 141.00, NOW() - INTERVAL '117 minute', 5, 6),
    ('TRANSFERENCIA', 154.00, NOW() - INTERVAL '118 minute', 6, 7),
    ('TRANSFERENCIA', 167.00, NOW() - INTERVAL '119 minute', 7, 8),
    ('TRANSFERENCIA', 180.00, NOW() - INTERVAL '120 minute', 8, 1),
    ('TRANSFERENCIA', 193.00, NOW() - INTERVAL '121 minute', 1, 2),
    ('TRANSFERENCIA', 206.00, NOW() - INTERVAL '122 minute', 2, 3),
    ('TRANSFERENCIA', 219.00, NOW() - INTERVAL '123 minute', 3, 4),
    ('TRANSFERENCIA', 232.00, NOW() - INTERVAL '124 minute', 4, 5),
    ('TRANSFERENCIA', 245.00, NOW() - INTERVAL '125 minute', 5, 6),
    ('TRANSFERENCIA', 258.00, NOW() - INTERVAL '126 minute', 6, 7),
    ('TRANSFERENCIA', 271.00, NOW() - INTERVAL '127 minute', 7, 8),
    ('TRANSFERENCIA', 284.00, NOW() - INTERVAL '128 minute', 8, 1),
    ('TRANSFERENCIA', 297.00, NOW() - INTERVAL '129 minute', 1, 2),
    ('TRANSFERENCIA', 310.00, NOW() - INTERVAL '130 minute', 2, 3),
    ('TRANSFERENCIA', 323.00, NOW() - INTERVAL '131 minute', 3, 4),
    ('TRANSFERENCIA', 336.00, NOW() - INTERVAL '132 minute', 4, 5),
    ('TRANSFERENCIA', 349.00, NOW() - INTERVAL '133 minute', 5, 6),
    ('TRANSFERENCIA', 362.00, NOW() - INTERVAL '134 minute', 6, 7),
    ('TRANSFERENCIA', 25.00, NOW() - INTERVAL '135 minute', 7, 8),
    ('TRANSFERENCIA', 38.00, NOW() - INTERVAL '136 minute', 8, 1),
    ('TRANSFERENCIA', 51.00, NOW() - INTERVAL '137 minute', 1, 2),
    ('TRANSFERENCIA', 64.00, NOW() - INTERVAL '138 minute', 2, 3),
    ('TRANSFERENCIA', 77.00, NOW() - INTERVAL '139 minute', 3, 4),
    ('TRANSFERENCIA', 90.00, NOW() - INTERVAL '140 minute', 4, 5),
    ('TRANSFERENCIA', 103.00, NOW() - INTERVAL '141 minute', 5, 6),
    ('TRANSFERENCIA', 116.00, NOW() - INTERVAL '142 minute', 6, 7),
    ('TRANSFERENCIA', 129.00, NOW() - INTERVAL '143 minute', 7, 8),
    ('TRANSFERENCIA', 142.00, NOW() - INTERVAL '144 minute', 8, 1),
    ('TRANSFERENCIA', 155.00, NOW() - INTERVAL '145 minute', 1, 2),
    ('TRANSFERENCIA', 168.00, NOW() - INTERVAL '146 minute', 2, 3),
    ('TRANSFERENCIA', 181.00, NOW() - INTERVAL '147 minute', 3, 4),
    ('TRANSFERENCIA', 194.00, NOW() - INTERVAL '148 minute', 4, 5),
    ('TRANSFERENCIA', 207.00, NOW() - INTERVAL '149 minute', 5, 6),
    ('TRANSFERENCIA', 220.00, NOW() - INTERVAL '150 minute', 6, 7),
    ('TRANSFERENCIA', 233.00, NOW() - INTERVAL '151 minute', 7, 8),
    ('TRANSFERENCIA', 246.00, NOW() - INTERVAL '152 minute', 8, 1),
    ('TRANSFERENCIA', 259.00, NOW() - INTERVAL '153 minute', 1, 2),
    ('TRANSFERENCIA', 272.00, NOW() - INTERVAL '154 minute', 2, 3),
    ('TRANSFERENCIA', 285.00, NOW() - INTERVAL '155 minute', 3, 4),
    ('TRANSFERENCIA', 298.00, NOW() - INTERVAL '156 minute', 4, 5),
    ('TRANSFERENCIA', 311.00, NOW() - INTERVAL '157 minute', 5, 6),
    ('TRANSFERENCIA', 324.00, NOW() - INTERVAL '158 minute', 6, 7),
    ('TRANSFERENCIA', 337.00, NOW() - INTERVAL '159 minute', 7, 8),
    ('TRANSFERENCIA', 350.00, NOW() - INTERVAL '160 minute', 8, 1),
    ('TRANSFERENCIA', 363.00, NOW() - INTERVAL '161 minute', 1, 2),
    ('TRANSFERENCIA', 26.00, NOW() - INTERVAL '162 minute', 2, 3),
    ('TRANSFERENCIA', 39.00, NOW() - INTERVAL '163 minute', 3, 4),
    ('TRANSFERENCIA', 52.00, NOW() - INTERVAL '164 minute', 4, 5),
    ('TRANSFERENCIA', 65.00, NOW() - INTERVAL '165 minute', 5, 6),
    ('TRANSFERENCIA', 78.00, NOW() - INTERVAL '166 minute', 6, 7),
    ('TRANSFERENCIA', 91.00, NOW() - INTERVAL '167 minute', 7, 8),
    ('TRANSFERENCIA', 104.00, NOW() - INTERVAL '168 minute', 8, 1),
    ('TRANSFERENCIA', 117.00, NOW() - INTERVAL '169 minute', 1, 2),
    ('TRANSFERENCIA', 130.00, NOW() - INTERVAL '170 minute', 2, 3),
    ('TRANSFERENCIA', 143.00, NOW() - INTERVAL '171 minute', 3, 4),
    ('TRANSFERENCIA', 156.00, NOW() - INTERVAL '172 minute', 4, 5),
    ('TRANSFERENCIA', 169.00, NOW() - INTERVAL '173 minute', 5, 6),
    ('TRANSFERENCIA', 182.00, NOW() - INTERVAL '174 minute', 6, 7),
    ('TRANSFERENCIA', 195.00, NOW() - INTERVAL '175 minute', 7, 8),
    ('TRANSFERENCIA', 208.00, NOW() - INTERVAL '176 minute', 8, 1),
    ('TRANSFERENCIA', 221.00, NOW() - INTERVAL '177 minute', 1, 2),
    ('TRANSFERENCIA', 234.00, NOW() - INTERVAL '178 minute', 2, 3),
    ('TRANSFERENCIA', 247.00, NOW() - INTERVAL '179 minute', 3, 4),
    ('TRANSFERENCIA', 260.00, NOW() - INTERVAL '180 minute', 4, 5),
    ('TRANSFERENCIA', 273.00, NOW() - INTERVAL '181 minute', 5, 6),
    ('TRANSFERENCIA', 286.00, NOW() - INTERVAL '182 minute', 6, 7),
    ('TRANSFERENCIA', 299.00, NOW() - INTERVAL '183 minute', 7, 8),
    ('TRANSFERENCIA', 312.00, NOW() - INTERVAL '184 minute', 8, 1),
    ('TRANSFERENCIA', 325.00, NOW() - INTERVAL '185 minute', 1, 2),
    ('TRANSFERENCIA', 338.00, NOW() - INTERVAL '186 minute', 2, 3),
    ('TRANSFERENCIA', 351.00, NOW() - INTERVAL '187 minute', 3, 4),
    ('TRANSFERENCIA', 364.00, NOW() - INTERVAL '188 minute', 4, 5),
    ('TRANSFERENCIA', 27.00, NOW() - INTERVAL '189 minute', 5, 6),
    ('TRANSFERENCIA', 40.00, NOW() - INTERVAL '190 minute', 6, 7),
    ('TRANSFERENCIA', 53.00, NOW() - INTERVAL '191 minute', 7, 8),
    ('TRANSFERENCIA', 66.00, NOW() - INTERVAL '192 minute', 8, 1),
    ('TRANSFERENCIA', 79.00, NOW() - INTERVAL '193 minute', 1, 2),
    ('TRANSFERENCIA', 92.00, NOW() - INTERVAL '194 minute', 2, 3),
    ('TRANSFERENCIA', 105.00, NOW() - INTERVAL '195 minute', 3, 4),
    ('TRANSFERENCIA', 118.00, NOW() - INTERVAL '196 minute', 4, 5),
    ('TRANSFERENCIA', 131.00, NOW() - INTERVAL '197 minute', 5, 6),
    ('TRANSFERENCIA', 144.00, NOW() - INTERVAL '198 minute', 6, 7),
    ('TRANSFERENCIA', 157.00, NOW() - INTERVAL '199 minute', 7, 8),
    ('TRANSFERENCIA', 170.00, NOW() - INTERVAL '200 minute', 8, 1),
    -- Depósitos e saques adicionais
    ('DEPOSITO', 47.00, NOW() - INTERVAL '201 minute', NULL, 1),
    ('SAQUE', -64.00, NOW() - INTERVAL '202 minute', 2, NULL),
    ('DEPOSITO', 81.00, NOW() - INTERVAL '203 minute', NULL, 4),
    ('DEPOSITO', 115.00, NOW() - INTERVAL '205 minute', NULL, 7),
    ('SAQUE', -132.00, NOW() - INTERVAL '206 minute', 1, NULL),
    ('DEPOSITO', 149.00, NOW() - INTERVAL '207 minute', NULL, 2),
    ('SAQUE', -166.00, NOW() - INTERVAL '208 minute', 4, NULL),
    ('SAQUE', -200.00, NOW() - INTERVAL '210 minute', 7, NULL),
    ('DEPOSITO', 217.00, NOW() - INTERVAL '211 minute', NULL, 1),
    ('SAQUE', -234.00, NOW() - INTERVAL '212 minute', 2, NULL),
    ('DEPOSITO', 251.00, NOW() - INTERVAL '213 minute', NULL, 4),
    ('DEPOSITO', 285.00, NOW() - INTERVAL '215 minute', NULL, 7),
    ('SAQUE', -302.00, NOW() - INTERVAL '216 minute', 1, NULL),
    ('DEPOSITO', 319.00, NOW() - INTERVAL '217 minute', NULL, 2),
    ('SAQUE', -336.00, NOW() - INTERVAL '218 minute', 4, NULL),
    ('SAQUE', -370.00, NOW() - INTERVAL '220 minute', 7, NULL),
    ('DEPOSITO', 387.00, NOW() - INTERVAL '221 minute', NULL, 1),
    ('SAQUE', -404.00, NOW() - INTERVAL '222 minute', 2, NULL),
    ('DEPOSITO', 421.00, NOW() - INTERVAL '223 minute', NULL, 4),
    ('DEPOSITO', 455.00, NOW() - INTERVAL '225 minute', NULL, 7),
    ('SAQUE', -472.00, NOW() - INTERVAL '226 minute', 1, NULL),
    ('DEPOSITO', 489.00, NOW() - INTERVAL '227 minute', NULL, 2),
    ('SAQUE', -506.00, NOW() - INTERVAL '228 minute', 4, NULL),
    ('SAQUE', -40.00, NOW() - INTERVAL '230 minute', 7, NULL),
    ('DEPOSITO', 57.00, NOW() - INTERVAL '231 minute', NULL, 1),
    ('SAQUE', -74.00, NOW() - INTERVAL '232 minute', 2, NULL),
    ('DEPOSITO', 91.00, NOW() - INTERVAL '233 minute', NULL, 4),
    ('DEPOSITO', 125.00, NOW() - INTERVAL '235 minute', NULL, 7),
    ('SAQUE', -142.00, NOW() - INTERVAL '236 minute', 1, NULL),
    ('DEPOSITO', 159.00, NOW() - INTERVAL '237 minute', NULL, 2),
    ('SAQUE', -176.00, NOW() - INTERVAL '238 minute', 4, NULL),
    ('SAQUE', -210.00, NOW() - INTERVAL '240 minute', 7, NULL),
    ('DEPOSITO', 227.00, NOW() - INTERVAL '241 minute', NULL, 1),
    ('SAQUE', -244.00, NOW() - INTERVAL '242 minute', 2, NULL),
    ('DEPOSITO', 261.00, NOW() - INTERVAL '243 minute', NULL, 4),
    ('DEPOSITO', 295.00, NOW() - INTERVAL '245 minute', NULL, 7),
    ('SAQUE', -312.00, NOW() - INTERVAL '246 minute', 1, NULL),
    ('DEPOSITO', 329.00, NOW() - INTERVAL '247 minute', NULL, 2),
    ('SAQUE', -346.00, NOW() - INTERVAL '248 minute', 4, NULL),
    ('SAQUE', -380.00, NOW() - INTERVAL '250 minute', 7, NULL),
    ('DEPOSITO', 397.00, NOW() - INTERVAL '251 minute', NULL, 1),
    ('SAQUE', -414.00, NOW() - INTERVAL '252 minute', 2, NULL),
    ('DEPOSITO', 431.00, NOW() - INTERVAL '253 minute', NULL, 4),
    ('DEPOSITO', 465.00, NOW() - INTERVAL '255 minute', NULL, 7),
    ('SAQUE', -482.00, NOW() - INTERVAL '256 minute', 1, NULL),
    ('DEPOSITO', 499.00, NOW() - INTERVAL '257 minute', NULL, 2),
    ('SAQUE', -516.00, NOW() - INTERVAL '258 minute', 4, NULL),
    ('SAQUE', -50.00, NOW() - INTERVAL '260 minute', 7, NULL),
    ('DEPOSITO', 67.00, NOW() - INTERVAL '261 minute', NULL, 1),
    ('SAQUE', -84.00, NOW() - INTERVAL '262 minute', 2, NULL),
    ('DEPOSITO', 101.00, NOW() - INTERVAL '263 minute', NULL, 4),
    ('DEPOSITO', 135.00, NOW() - INTERVAL '265 minute', NULL, 7),
    ('SAQUE', -152.00, NOW() - INTERVAL '266 minute', 1, NULL),
    ('DEPOSITO', 169.00, NOW() - INTERVAL '267 minute', NULL, 2),
    ('SAQUE', -186.00, NOW() - INTERVAL '268 minute', 4, NULL),
    ('SAQUE', -220.00, NOW() - INTERVAL '270 minute', 7, NULL),
    ('DEPOSITO', 237.00, NOW() - INTERVAL '271 minute', NULL, 1),
    ('SAQUE', -254.00, NOW() - INTERVAL '272 minute', 2, NULL),
    ('DEPOSITO', 271.00, NOW() - INTERVAL '273 minute', NULL, 4),
    ('DEPOSITO', 305.00, NOW() - INTERVAL '275 minute', NULL, 7),
    ('SAQUE', -322.00, NOW() - INTERVAL '276 minute', 1, NULL),
    ('DEPOSITO', 339.00, NOW() - INTERVAL '277 minute', NULL, 2),
    ('SAQUE', -356.00, NOW() - INTERVAL '278 minute', 4, NULL),
    ('SAQUE', -390.00, NOW() - INTERVAL '280 minute', 7, NULL),
    ('DEPOSITO', 407.00, NOW() - INTERVAL '281 minute', NULL, 1),
    ('SAQUE', -424.00, NOW() - INTERVAL '282 minute', 2, NULL),
    ('DEPOSITO', 441.00, NOW() - INTERVAL '283 minute', NULL, 4),
    ('DEPOSITO', 475.00, NOW() - INTERVAL '285 minute', NULL, 7),
    ('SAQUE', -492.00, NOW() - INTERVAL '286 minute', 1, NULL),
    ('DEPOSITO', 509.00, NOW() - INTERVAL '287 minute', NULL, 2),
    ('SAQUE', -526.00, NOW() - INTERVAL '288 minute', 4, NULL),
    ('SAQUE', -60.00, NOW() - INTERVAL '290 minute', 7, NULL),
    ('DEPOSITO', 77.00, NOW() - INTERVAL '291 minute', NULL, 1),
    ('SAQUE', -94.00, NOW() - INTERVAL '292 minute', 2, NULL),
    ('DEPOSITO', 111.00, NOW() - INTERVAL '293 minute', NULL, 4),
    ('DEPOSITO', 145.00, NOW() - INTERVAL '295 minute', NULL, 7),
    ('SAQUE', -162.00, NOW() - INTERVAL '296 minute', 1, NULL),
    ('DEPOSITO', 179.00, NOW() - INTERVAL '297 minute', NULL, 2),
    ('SAQUE', -196.00, NOW() - INTERVAL '298 minute', 4, NULL),
    ('SAQUE', -230.00, NOW() - INTERVAL '300 minute', 7, NULL);

-- ------------------------------------------------------------
-- Verificações
-- ------------------------------------------------------------

SELECT COUNT(*) FROM cliente;
SELECT COUNT(*) FROM conta;
SELECT tipo, COUNT(*) FROM conta GROUP BY tipo ORDER BY tipo;
SELECT COUNT(*) FROM transacao;
SELECT tipo, COUNT(*) FROM transacao GROUP BY tipo ORDER BY tipo;

-- ------------------------------------------------------------
-- Consulta saldo das contas
-- ------------------------------------------------------------
SELECT * FROM view_saldo;
