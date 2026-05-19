-- ============================================================
-- MIGRACIONES INCREMENTALES — Hotel Sistema
-- Ejecutar solo si actualizas desde una versión anterior.
-- Cada bloque usa IF NOT EXISTS / IGNORE para ser idempotente.
-- Motor: MySQL 8.0+
-- ============================================================

USE hotel_sistema;

-- ------------------------------------------------------------
-- v1.0 → v1.1: columna activo en clientes
-- ------------------------------------------------------------
SET @col = (SELECT COUNT(*) FROM information_schema.columns
            WHERE table_schema = 'hotel_sistema'
              AND table_name   = 'clientes'
              AND column_name  = 'activo');
SET @sql = IF(@col = 0,
    'ALTER TABLE clientes ADD COLUMN activo BOOLEAN NOT NULL DEFAULT TRUE AFTER nacionalidad',
    'SELECT ''activo ya existe en clientes'' AS info');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ------------------------------------------------------------
-- v1.0 → v1.1: columna precio_especial en habitaciones
-- ------------------------------------------------------------
SET @col = (SELECT COUNT(*) FROM information_schema.columns
            WHERE table_schema = 'hotel_sistema'
              AND table_name   = 'habitaciones'
              AND column_name  = 'precio_especial');
SET @sql = IF(@col = 0,
    'ALTER TABLE habitaciones ADD COLUMN precio_especial DECIMAL(10,2) DEFAULT NULL AFTER estado',
    'SELECT ''precio_especial ya existe'' AS info');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ------------------------------------------------------------
-- v1.1 → v1.2: columna imagen_url en habitaciones
-- ------------------------------------------------------------
SET @col = (SELECT COUNT(*) FROM information_schema.columns
            WHERE table_schema = 'hotel_sistema'
              AND table_name   = 'habitaciones'
              AND column_name  = 'imagen_url');
SET @sql = IF(@col = 0,
    'ALTER TABLE habitaciones ADD COLUMN imagen_url VARCHAR(500) DEFAULT NULL AFTER descripcion',
    'SELECT ''imagen_url ya existe'' AS info');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ------------------------------------------------------------
-- Verificación final
-- ------------------------------------------------------------
SELECT
    table_name,
    GROUP_CONCAT(column_name ORDER BY ordinal_position SEPARATOR ', ') AS columnas
FROM information_schema.columns
WHERE table_schema = 'hotel_sistema'
  AND table_name IN ('clientes','habitaciones')
GROUP BY table_name;
