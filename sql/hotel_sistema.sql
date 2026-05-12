-- ============================================================
-- SISTEMA DE ADMINISTRACIÓN HOTELERA
-- Script de creación de base de datos
-- Base de datos: hotel_sistema
-- Motor:         MySQL 8.0+
-- Autor:         Fernando
-- Versión:       1.0
-- ============================================================

-- Crear y seleccionar la base de datos
CREATE DATABASE IF NOT EXISTS hotel_sistema
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE hotel_sistema;

-- ============================================================
-- TABLA: tipo_habitacion
-- Clasifica las habitaciones (Simple, Doble, Suite, etc.)
-- ============================================================
CREATE TABLE IF NOT EXISTS tipo_habitacion (
    id            INT           PRIMARY KEY AUTO_INCREMENT,
    nombre        VARCHAR(50)   NOT NULL,
    descripcion   TEXT,
    precio_base   DECIMAL(10,2) NOT NULL COMMENT 'Precio por noche en USD',
    capacidad     INT           NOT NULL DEFAULT 1 COMMENT 'Número de personas'
);

-- ============================================================
-- TABLA: habitaciones
-- Representa cada habitación física del hotel
-- ============================================================
CREATE TABLE IF NOT EXISTS habitaciones (
    id            INT           PRIMARY KEY AUTO_INCREMENT,
    numero        VARCHAR(10)   NOT NULL UNIQUE COMMENT 'Ej: 101, 202, SUITE-1',
    piso          INT           NOT NULL,
    id_tipo       INT           NOT NULL,
    estado        ENUM('DISPONIBLE','OCUPADA','MANTENIMIENTO','RESERVADA')
                                NOT NULL DEFAULT 'DISPONIBLE',
    descripcion   TEXT,
    imagen_url    VARCHAR(255),
    CONSTRAINT fk_hab_tipo FOREIGN KEY (id_tipo)
        REFERENCES tipo_habitacion(id) ON UPDATE CASCADE
);

-- ============================================================
-- TABLA: usuarios
-- Usuarios del sistema (administradores y recepcionistas)
-- ============================================================
CREATE TABLE IF NOT EXISTS usuarios (
    id              INT           PRIMARY KEY AUTO_INCREMENT,
    nombre          VARCHAR(100)  NOT NULL,
    usuario         VARCHAR(50)   NOT NULL UNIQUE,
    password        VARCHAR(255)  NOT NULL COMMENT 'En producción usar hash BCrypt',
    rol             ENUM('ADMIN','RECEPCIONISTA') NOT NULL DEFAULT 'RECEPCIONISTA',
    activo          BOOLEAN       NOT NULL DEFAULT TRUE,
    fecha_creacion  TIMESTAMP     DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- TABLA: clientes
-- Huéspedes registrados en el hotel
-- ============================================================
CREATE TABLE IF NOT EXISTS clientes (
    id               INT           PRIMARY KEY AUTO_INCREMENT,
    nombre           VARCHAR(100)  NOT NULL,
    apellido         VARCHAR(100)  NOT NULL,
    tipo_documento   ENUM('DPI','PASAPORTE','CEDULA') DEFAULT 'DPI',
    documento        VARCHAR(20)   NOT NULL UNIQUE,
    telefono         VARCHAR(20),
    email            VARCHAR(100),
    direccion        TEXT,
    nacionalidad     VARCHAR(60),
    fecha_registro   TIMESTAMP     DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- TABLA: reservaciones
-- Vincula clientes con habitaciones en un período de fechas
-- ============================================================
CREATE TABLE IF NOT EXISTS reservaciones (
    id               INT     PRIMARY KEY AUTO_INCREMENT,
    id_cliente       INT     NOT NULL,
    id_habitacion    INT     NOT NULL,
    fecha_checkin    DATE    NOT NULL,
    fecha_checkout   DATE    NOT NULL,
    estado           ENUM('PENDIENTE','CONFIRMADA','CHECKIN','CHECKOUT','CANCELADA')
                             NOT NULL DEFAULT 'PENDIENTE',
    observaciones    TEXT,
    id_usuario       INT     NOT NULL COMMENT 'Usuario que creó la reserva',
    fecha_reserva    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_res_cliente    FOREIGN KEY (id_cliente)    REFERENCES clientes(id),
    CONSTRAINT fk_res_habitacion FOREIGN KEY (id_habitacion) REFERENCES habitaciones(id),
    CONSTRAINT fk_res_usuario    FOREIGN KEY (id_usuario)    REFERENCES usuarios(id),
    CONSTRAINT chk_fechas CHECK (fecha_checkout > fecha_checkin)
);

-- ============================================================
-- TABLA: facturas
-- Registro de cobros generados por cada reservación
-- ============================================================
CREATE TABLE IF NOT EXISTS facturas (
    id               INT            PRIMARY KEY AUTO_INCREMENT,
    id_reservacion   INT            NOT NULL UNIQUE,
    fecha_emision    TIMESTAMP      DEFAULT CURRENT_TIMESTAMP,
    subtotal         DECIMAL(10,2)  NOT NULL,
    impuesto         DECIMAL(10,2)  NOT NULL COMMENT '18% IGV/IVA',
    total            DECIMAL(10,2)  NOT NULL,
    estado           ENUM('PENDIENTE','PAGADA','ANULADA') DEFAULT 'PENDIENTE',
    metodo_pago      ENUM('EFECTIVO','TARJETA','TRANSFERENCIA') DEFAULT 'EFECTIVO',
    observaciones    TEXT,
    CONSTRAINT fk_fact_reservacion FOREIGN KEY (id_reservacion)
        REFERENCES reservaciones(id)
);

-- ============================================================
-- ÍNDICES para mejorar rendimiento en búsquedas frecuentes
-- ============================================================
CREATE INDEX idx_reservaciones_fechas    ON reservaciones(fecha_checkin, fecha_checkout);
CREATE INDEX idx_reservaciones_estado    ON reservaciones(estado);
CREATE INDEX idx_habitaciones_estado     ON habitaciones(estado);
CREATE INDEX idx_clientes_documento      ON clientes(documento);
CREATE INDEX idx_facturas_estado         ON facturas(estado);

-- ============================================================
-- DATOS INICIALES: Tipos de habitación
-- ============================================================
INSERT INTO tipo_habitacion (nombre, descripcion, precio_base, capacidad) VALUES
('Simple',    'Habitación individual con cama una plaza, TV y baño privado',          50.00, 1),
('Doble',     'Habitación con cama matrimonial, TV, minibar y baño privado',          80.00, 2),
('Triple',    'Habitación amplia con 3 camas individuales, TV y baño privado',       100.00, 3),
('Suite',     'Suite de lujo con sala, jacuzzi, vista panorámica y minibar',         200.00, 2),
('Familiar',  'Amplia habitación familiar con 2 camas dobles y sala de estar',       150.00, 4);

-- ============================================================
-- DATOS INICIALES: Habitaciones del hotel (20 habitaciones)
-- ============================================================
INSERT INTO habitaciones (numero, piso, id_tipo, estado, descripcion) VALUES
-- Piso 1: Habitaciones simples
('101', 1, 1, 'DISPONIBLE', 'Vista al jardín'),
('102', 1, 1, 'DISPONIBLE', 'Vista al jardín'),
('103', 1, 1, 'DISPONIBLE', 'Vista al estacionamiento'),
('104', 1, 2, 'DISPONIBLE', 'Vista al jardín'),
-- Piso 2: Habitaciones dobles
('201', 2, 2, 'DISPONIBLE', 'Vista a la piscina'),
('202', 2, 2, 'DISPONIBLE', 'Vista a la piscina'),
('203', 2, 2, 'DISPONIBLE', 'Vista a la ciudad'),
('204', 2, 3, 'DISPONIBLE', 'Vista a la ciudad'),
-- Piso 3: Habitaciones triples y familiares
('301', 3, 3, 'DISPONIBLE', 'Vista panorámica'),
('302', 3, 3, 'DISPONIBLE', 'Vista panorámica'),
('303', 3, 5, 'DISPONIBLE', 'Suite familiar con terraza'),
('304', 3, 5, 'DISPONIBLE', 'Suite familiar con terraza'),
-- Piso 4: Suites
('401', 4, 4, 'DISPONIBLE', 'Suite premium con jacuzzi y vista al mar'),
('402', 4, 4, 'DISPONIBLE', 'Suite premium con sala de reuniones'),
('403', 4, 4, 'DISPONIBLE', 'Suite presidencial - la mejor vista'),
-- Piso 5: Mixtas
('501', 5, 1, 'DISPONIBLE', 'Habitación económica'),
('502', 5, 2, 'MANTENIMIENTO', 'En remodelación'),
('503', 5, 3, 'DISPONIBLE', 'Vista a la montaña'),
('504', 5, 4, 'DISPONIBLE', 'Suite con terraza privada'),
('505', 5, 5, 'DISPONIBLE', 'Habitación familiar deluxe');

-- ============================================================
-- DATOS INICIALES: Usuarios del sistema
-- IMPORTANTE: En producción, usar contraseñas hasheadas (BCrypt)
-- ============================================================
INSERT INTO usuarios (nombre, usuario, password, rol) VALUES
('Administrador General',  'admin',  'admin123',  'ADMIN'),
('María Recepción',        'maria',  'maria123',  'RECEPCIONISTA'),
('Carlos Recepción',       'carlos', 'carlos123', 'RECEPCIONISTA');

-- ============================================================
-- DATOS DE PRUEBA: Clientes de ejemplo
-- ============================================================
INSERT INTO clientes (nombre, apellido, tipo_documento, documento, telefono, email, nacionalidad) VALUES
('Juan Carlos',  'García López',     'DPI',       '12345678', '999-111-222', 'juan@email.com',   'Peruana'),
('María Elena',  'Torres Vega',      'DPI',       '87654321', '999-333-444', 'maria@email.com',  'Peruana'),
('John',         'Smith',            'PASAPORTE', 'US123456', '999-555-666', 'john@email.com',   'Americana'),
('Ana',          'Martínez Ruiz',    'CEDULA',    'COL98765', '999-777-888', 'ana@email.com',    'Colombiana'),
('Pedro',        'Sánchez Castro',   'DPI',       '11223344', '999-999-000', 'pedro@email.com',  'Peruana');

-- ============================================================
-- VISTA útil: Disponibilidad de habitaciones
-- ============================================================
CREATE OR REPLACE VIEW v_habitaciones_disponibles AS
SELECT
    h.id,
    h.numero,
    h.piso,
    t.nombre        AS tipo,
    t.precio_base   AS precio_noche,
    t.capacidad,
    h.descripcion,
    h.estado
FROM habitaciones h
INNER JOIN tipo_habitacion t ON h.id_tipo = t.id
WHERE h.estado = 'DISPONIBLE'
ORDER BY h.piso, h.numero;

-- ============================================================
-- VISTA útil: Reservaciones activas con detalle
-- ============================================================
CREATE OR REPLACE VIEW v_reservaciones_activas AS
SELECT
    r.id                                        AS id_reservacion,
    CONCAT(c.nombre, ' ', c.apellido)           AS cliente,
    c.documento,
    h.numero                                    AS habitacion,
    t.nombre                                    AS tipo_habitacion,
    t.precio_base                               AS precio_noche,
    r.fecha_checkin,
    r.fecha_checkout,
    DATEDIFF(r.fecha_checkout, r.fecha_checkin) AS noches,
    r.estado,
    r.observaciones,
    u.nombre                                    AS atendido_por
FROM reservaciones r
INNER JOIN clientes    c ON r.id_cliente    = c.id
INNER JOIN habitaciones h ON r.id_habitacion = h.id
INNER JOIN tipo_habitacion t ON h.id_tipo   = t.id
INNER JOIN usuarios    u ON r.id_usuario    = u.id
WHERE r.estado IN ('PENDIENTE','CONFIRMADA','CHECKIN')
ORDER BY r.fecha_checkin;

-- ============================================================
-- FIN DEL SCRIPT
-- Ejecutar con: mysql -u root -p < hotel_sistema.sql
-- ============================================================
SELECT 'Base de datos hotel_sistema creada exitosamente ✓' AS resultado;
