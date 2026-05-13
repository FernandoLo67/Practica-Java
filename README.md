# 🏨 Hotel Sistema

Sistema de Administración Hotelera desarrollado en **Java 11** con **Swing**, **Maven** y **MySQL**.

Arquitectura por capas (Modelo → DAO → Vista), conexión pooling con HikariCP, autenticación BCrypt, exportación a PDF/Excel, gráficas con JFreeChart, bitácora de auditoría y envío de correos con Jakarta Mail.

---

## 📋 Requisitos previos

| Herramienta | Versión recomendada | Descarga |
|-------------|-------------------|---------|
| Java JDK | 11 o superior | [adoptium.net](https://adoptium.net) |
| NetBeans | 16 o superior | [netbeans.apache.org](https://netbeans.apache.org) |
| MySQL Server | 8.0 o superior | [dev.mysql.com](https://dev.mysql.com/downloads/installer/) |
| MySQL Workbench | 8.0 o superior | Incluido en MySQL Installer |

---

## 🗄️ Paso 1 — Crear la base de datos

1. Abre **MySQL Workbench** y conéctate a tu servidor local
2. Ve a **File → Open SQL Script...**
3. Selecciona el archivo: `sql/hotel_sistema.sql`
4. Ejecuta el script con `Ctrl + Shift + Enter`

Se crearán las tablas bajo el schema `hotel_sistema`:

```
hotel_sistema
├── clientes          ← Huéspedes (campo activo para baja lógica)
├── habitaciones      ← Habitaciones físicas (campo precio_especial)
├── tipo_habitacion   ← Categorías con precio base y capacidad
├── reservaciones     ← Vincula cliente + habitación + fechas
├── facturas          ← Cobros generados por cada reservación
├── usuarios          ← Cuentas del sistema (ADMIN / RECEPCIONISTA)
└── bitacora          ← Registro de auditoría de todas las acciones
```

> Si ya tenías la BD creada sin las columnas `activo` o `precio_especial`, ejecuta también:
> ```sql
> ALTER TABLE clientes ADD COLUMN activo BOOLEAN NOT NULL DEFAULT TRUE;
> ALTER TABLE habitaciones ADD COLUMN precio_especial DECIMAL(10,2) DEFAULT NULL;
> ```

---

## ⚙️ Paso 2 — Configurar la conexión

Crea el archivo `src/main/resources/database.properties` con tus credenciales:

```properties
db.host=localhost
db.port=3306
db.name=hotel_sistema
db.user=root
db.password=TU_CONTRASEÑA_AQUI

# Pool HikariCP
pool.maximumPoolSize=10
pool.minimumIdle=2
pool.connectionTimeout=30000
pool.idleTimeout=600000
pool.maxLifetime=1800000
```

> ⚠️ Este archivo está en `.gitignore` — nunca se sube al repositorio.

---

## 📧 Paso 3 — Configurar email (opcional)

Para activar el envío de correos de confirmación de reservación, edita `src/main/resources/email.properties`:

```properties
mail.smtp.host=smtp.gmail.com
mail.smtp.port=587
mail.usuario=TU_CORREO@gmail.com
mail.password=TU_CONTRASEÑA_DE_APP   # Contraseña de aplicación Gmail (16 chars)
mail.from.nombre=Hotel Vista
mail.from.email=TU_CORREO@gmail.com
```

> Si no configuras el archivo, el sistema funciona igual — los envíos se deshabilitan automáticamente sin mostrar errores.

---

## 🚀 Paso 4 — Ejecutar el proyecto

1. Abre el proyecto en **NetBeans**: `File → Open Project` → selecciona la carpeta `mavenproject2`
2. Clic derecho → **Clean and Build** (descarga dependencias Maven)
3. Clic en **Run** (▶) o presiona `F6`

---

## 🔑 Usuarios de prueba

| Usuario | Contraseña | Rol |
|---------|-----------|-----|
| `admin` | `admin123` | Administrador |
| `maria` | `maria123` | Recepcionista |
| `carlos` | `carlos123` | Recepcionista |

> Las contraseñas se almacenan como hash **BCrypt**. El sistema migra automáticamente contraseñas en texto plano al primer inicio de sesión.

---

## 🗺️ Módulos del sistema

| # | Módulo | Descripción |
|---|--------|-------------|
| 1 | **Login** | Autenticación BCrypt, roles ADMIN/RECEPCIONISTA, bitácora de intentos |
| 2 | **Dashboard** | KPIs en tiempo real (SwingWorker async), agenda del día, gráfica de barras por mes, gráfica de pie por estado de habitaciones |
| 3 | **Clientes** | CRUD completo, KPI cards (total/activos/nuevos este mes), toggle activos/todos, historial de reservaciones por cliente, columna activo y fecha registro |
| 4 | **Tipos de Habitación** | CRUD de categorías (Simple/Doble/Suite…), precio base y capacidad editables |
| 5 | **Habitaciones** | Gestión de habitaciones físicas, precio especial por habitación, KPI cards de precio (mín/prom/máx), filtro por estado |
| 6 | **Reservaciones** | Formulario con máscara de fecha, cálculo automático de noches y total, verificación de disponibilidad, email de confirmación al crear |
| 7 | **Check-In / Check-Out** | Flujo completo, genera factura automáticamente, bitácora de eventos |
| 8 | **Facturación** | Listado de facturas, cambio de estado, detalle completo |
| 9 | **Reportes** | KPIs + reservaciones + top clientes exportados a PDF con PDFBox |
| 10 | **Usuarios** | CRUD de cuentas del sistema (ADMIN only) |
| 11 | **Bitácora** | Registro de todas las acciones con filtro por texto y módulo (ADMIN only) |

---

## 🏗️ Estructura del proyecto

```
src/main/
├── java/com/hotel/
│   ├── Main.java
│   ├── modelo/
│   │   ├── Usuario.java
│   │   ├── Cliente.java
│   │   ├── Habitacion.java          ← campo precioEspecial (nullable)
│   │   ├── TipoHabitacion.java
│   │   ├── Reservacion.java
│   │   ├── Factura.java
│   │   └── Bitacora.java            ← constantes de acciones y módulos
│   ├── dao/
│   │   ├── ClienteDAO.java
│   │   ├── HabitacionDAO.java
│   │   ├── TipoHabitacionDAO.java
│   │   ├── ReservacionDAO.java
│   │   ├── FacturaDAO.java
│   │   ├── UsuarioDAO.java
│   │   ├── BitacoraDAO.java
│   │   └── impl/
│   │       ├── ClienteDAOImpl.java      ← contarActivos, contarNuevosEsteMes
│   │       ├── HabitacionDAOImpl.java   ← precio_especial, getPrecioMin/Max/Prom
│   │       ├── TipoHabitacionDAOImpl.java
│   │       ├── ReservacionDAOImpl.java
│   │       ├── FacturaDAOImpl.java
│   │       ├── UsuarioDAOImpl.java      ← BCrypt + migración automática
│   │       └── BitacoraDAOImpl.java     ← insert en hilo separado
│   ├── vista/
│   │   ├── LoginForm.java
│   │   ├── MenuPrincipal.java
│   │   ├── DashboardPanel.java          ← JFreeChart, SwingWorker
│   │   ├── ClientesPanel.java           ← KPIs, toggle activos, historial
│   │   ├── ClienteFormDialog.java
│   │   ├── ClienteHistorialDialog.java  ← reservaciones + facturas del cliente
│   │   ├── HabitacionesPanel.java       ← KPIs de precio
│   │   ├── HabitacionFormDialog.java    ← campo precio especial
│   │   ├── TiposHabitacionPanel.java    ← CRUD tipos
│   │   ├── TipoHabitacionFormDialog.java
│   │   ├── ReservacionesPanel.java
│   │   ├── ReservacionFormDialog.java   ← email al crear, bitácora
│   │   ├── CheckInOutPanel.java
│   │   ├── FacturasPanel.java
│   │   ├── ReportesPanel.java
│   │   ├── UsuariosPanel.java
│   │   ├── UsuarioFormDialog.java
│   │   └── BitacoraPanel.java           ← tabla ADMIN con filtros
│   └── util/
│       ├── ConexionDB.java              ← HikariCP, database.properties
│       ├── PasswordUtil.java            ← BCrypt con retrocompatibilidad
│       ├── EmailService.java            ← Jakarta Mail, plantilla HTML
│       ├── BitacoraService.java         ← log estático desde cualquier clase
│       ├── SesionActual.java            ← usuario autenticado (singleton)
│       ├── ExcelExporter.java           ← JTable → .xlsx con Apache POI
│       ├── Tema.java                    ← colores y fuentes centralizados
│       ├── UIHelper.java                ← fábrica de componentes Swing
│       └── Validaciones.java
└── resources/
    ├── database.properties              ← credenciales BD (NO commitear)
    ├── email.properties                 ← credenciales SMTP (NO commitear)
    └── simplelogger.properties          ← nivel de log SLF4J
```

---

## 📦 Dependencias Maven

| Librería | Versión | Uso |
|----------|---------|-----|
| mysql-connector-java | 8.0.33 | Driver JDBC MySQL |
| HikariCP | 5.1.0 | Pool de conexiones |
| jbcrypt | 0.4 | Hash de contraseñas |
| slf4j-simple | 2.0.9 | Logging |
| pdfbox | 2.0.30 | Exportar reportes a PDF |
| jfreechart | 1.5.4 | Gráficas en Dashboard |
| poi-ooxml | 5.2.5 | Exportar tablas a Excel |
| angus-mail | 2.0.3 | Envío de correos SMTP |
| junit-jupiter | 5.10.1 | Pruebas unitarias |

---

## 🔐 Seguridad

- Contraseñas hasheadas con **BCrypt** (costo 12) — comparación en Java, no en SQL
- Migración automática de contraseñas en texto plano al primer login exitoso
- Credenciales de BD y SMTP en archivos externos excluidos del repositorio (`.gitignore`)
- Permisos por rol: módulos Usuarios y Bitácora solo accesibles para ADMIN
- Pool de conexiones HikariCP con timeout y reconexión automática
- Bitácora de auditoría: login exitoso/fallido, CRUD de clientes/habitaciones/reservaciones, Check-In/Out, cambios de factura

---

## 🧪 Pruebas unitarias

```
src/test/java/com/hotel/
├── util/PasswordUtilTest.java    ← hash BCrypt, verificación, retrocompatibilidad
├── util/ValidacionesTest.java    ← email, documento, texto, capitalizar
└── modelo/ReservacionTest.java   ← cálculo de noches, totales, estados
```

Ejecutar: `mvn test`

---

## 📁 Scripts SQL

| Archivo | Descripción |
|---------|-------------|
| `sql/hotel_sistema.sql` | Crea toda la BD: tablas, índices, vistas, datos iniciales |
| `sql/insertar_clientes.sql` | 26 clientes de prueba (guatemaltecos, internacionales, centroamericanos) |

---

Desarrollado con ☕ Java 11 + NetBeans 16 + MySQL 8 + Maven
