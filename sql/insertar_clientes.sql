-- ============================================================
-- SCRIPT: insertar_clientes.sql
-- Inserta clientes de prueba en la tabla 'clientes'
-- Base de datos: hotel_sistema
-- Autor: Fernando
-- Versión: 1.0
--
-- USO:
--   mysql -u root -p hotel_sistema < insertar_clientes.sql
--   o pegar directamente en MySQL Workbench / DBeaver
-- ============================================================

USE hotel_sistema;

-- ============================================================
-- PASO 1: Asegurar que la columna activo exista
-- (no falla si ya existe gracias a IF NOT EXISTS)
-- ============================================================
ALTER TABLE clientes
    ADD COLUMN IF NOT EXISTS activo BOOLEAN NOT NULL DEFAULT TRUE;

-- ============================================================
-- CLIENTES GUATEMALTECOS (DPI)
-- ============================================================
INSERT INTO clientes (nombre, apellido, tipo_documento, documento, telefono, email, direccion, nacionalidad, activo)
VALUES
('Carlos Andres',   'Pérez Morales',    'DPI', '2891456701',  '5521-3344', 'carlos.perez@gmail.com',    'Zona 10, Ciudad de Guatemala',          'Guatemalteca', TRUE),
('María Fernanda',  'López Castillo',   'DPI', '1784523690',  '4432-8877', 'mfernanda.lopez@gmail.com', 'Zona 1, Quetzaltenango',                'Guatemalteca', TRUE),
('Roberto',         'Ajú Xocoy',        'DPI', '3301289475',  '3318-9900', 'roberto.aju@hotmail.com',   'Panajachel, Sololá',                    'Guatemalteca', TRUE),
('Ana Lucía',       'Girón Sandoval',   'DPI', '2056781234',  '5598-1122', 'ana.giron@outlook.com',     'Zona 15, Vista Hermosa, Guatemala',     'Guatemalteca', TRUE),
('Diego Alejandro', 'Mendoza Pérez',    'DPI', '1923456780',  '4401-5566', 'diego.mendoza@gmail.com',   'Antigua Guatemala, Sacatepéquez',       'Guatemalteca', TRUE),
('Sofía Isabel',    'Ramírez Chan',     'DPI', '2674512890',  '5532-7788', 'sofia.ramirez@gmail.com',   'Zona 4, Mixco, Guatemala',              'Guatemalteca', TRUE),
('Luis Fernando',   'Chávez Aquino',    'DPI', '1456789023',  '3356-4321', 'luis.chavez@yahoo.com',     'Villa Nueva, Guatemala',                'Guatemalteca', TRUE),
('Carmen Rosa',     'Tojil Mendoza',    'DPI', '3012456789',  '5511-0099', 'carmen.tojil@gmail.com',    'Huehuetenango, Huehuetenango',          'Guatemalteca', TRUE),

-- ============================================================
-- CLIENTES INTERNACIONALES (PASAPORTE)
-- ============================================================
('James',           'Anderson',         'PASAPORTE', 'US789012', '999-555-0011', 'james.anderson@gmail.com',  '45 Oak Street, New York, USA',          'Estadounidense', TRUE),
('Emma',            'Müller',           'PASAPORTE', 'DE234567', '999-555-0022', 'emma.mueller@gmx.de',       'Hauptstraße 12, Berlin, Alemania',      'Alemana',        TRUE),
('Hiroshi',         'Tanaka',           'PASAPORTE', 'JP456789', '999-555-0033', 'hiroshi.tanaka@yahoo.co.jp','3-5 Shibuya, Tokio, Japón',             'Japonesa',       TRUE),
('Valentina',       'Rossi',            'PASAPORTE', 'IT678901', '999-555-0044', 'v.rossi@libero.it',         'Via Roma 8, Milán, Italia',             'Italiana',       TRUE),
('Liam',            'Murphy',           'PASAPORTE', 'IE890123', '999-555-0055', 'liam.murphy@eircom.ie',     '22 Grafton Street, Dublín, Irlanda',    'Irlandesa',      TRUE),
('Sophie',          'Dubois',           'PASAPORTE', 'FR112233', '999-555-0066', 'sophie.dubois@orange.fr',   '18 Rue de Rivoli, París, Francia',      'Francesa',       TRUE),
('Chen',            'Wei',              'PASAPORTE', 'CN334455', '999-555-0077', 'chen.wei@163.com',          'Nanjing Road 200, Shanghái, China',     'China',          TRUE),
('Priya',           'Sharma',           'PASAPORTE', 'IN556677', '999-555-0088', 'priya.sharma@gmail.com',    'MG Road, Bangalore, India',             'India',          TRUE),

-- ============================================================
-- CLIENTES CENTROAMERICANOS (CÉDULA)
-- ============================================================
('Alejandro',       'Vargas Solano',    'CEDULA', 'CR445566', '8800-1122', 'alejandro.vargas@gmail.com', 'Barrio Escalante, San José, Costa Rica',  'Costarricense', TRUE),
('Laura Patricia',  'Núñez García',     'CEDULA', 'MX778899', '55-1234-5678','laura.nunez@hotmail.com', 'Colonia Roma, Ciudad de México',           'Mexicana',       TRUE),
('Óscar Mauricio',  'Herrera Blanco',   'CEDULA', 'SV223344', '7890-5566', 'oscar.herrera@gmail.com',   'Colonia Escalón, San Salvador, El Salvador','Salvadoreña',  TRUE),
('Claudia María',   'Reyes Montoya',    'CEDULA', 'CO889900', '310-444-7788','claudia.reyes@gmail.com', 'El Poblado, Medellín, Colombia',           'Colombiana',     TRUE),
('Sergio Antonio',  'Flores Zúñiga',    'CEDULA', 'HN112200', '9900-3344', 'sergio.flores@yahoo.com',   'Colonia Kennedy, Tegucigalpa, Honduras',  'Hondureña',      TRUE),
('Valeria',         'Moreno Salinas',   'CEDULA', 'NI334411', '8156-7788', 'valeria.moreno@gmail.com',  'Reparto San Juan, Managua, Nicaragua',    'Nicaragüense',   TRUE),

-- ============================================================
-- CLIENTES INACTIVOS (activo = FALSE) — para probar el filtro
-- ============================================================
('Roberto',         'Vásquez Leiva',    'DPI',      '4500012345', '5577-0011', 'roberto.vasquez@gmail.com',  'Zona 6, Guatemala',                    'Guatemalteca',   FALSE),
('Daniela',         'Ochoa Paredes',    'CEDULA',   'PE667788',   '987-654-321','daniela.ochoa@gmail.com',  'Miraflores, Lima, Perú',               'Peruana',        FALSE);

-- ============================================================
-- VERIFICACIÓN
-- ============================================================
SELECT
    COUNT(*)                          AS total_insertados,
    SUM(activo = TRUE)                AS activos,
    SUM(activo = FALSE)               AS inactivos,
    SUM(tipo_documento = 'DPI')       AS con_dpi,
    SUM(tipo_documento = 'PASAPORTE') AS con_pasaporte,
    SUM(tipo_documento = 'CEDULA')    AS con_cedula
FROM clientes;

SELECT id, CONCAT(nombre, ' ', apellido) AS nombre_completo,
       tipo_documento, documento, nacionalidad,
       IF(activo, 'Activo', 'Inactivo') AS estado
FROM clientes
ORDER BY id;
