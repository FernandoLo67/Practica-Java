# 🏨 Hotel Sistema

Sistema de Administración Hotelera desarrollado en **Java 11** con **Swing**, **Maven** y **MySQL**.

Proyecto universitario con arquitectura por capas (Modelo → DAO → Vista), conexión pooling con HikariCP, autenticación BCrypt y exportación a PDF.

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

Se crearán las 6 tablas bajo el schema `hotel_sistema`:

```
hotel_sistema
├── clientes
├── facturas
├── habitaciones
├── reservaciones
├── tipo_habitacion
└── usuarios
```

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

## 🚀 Paso 3 — Ejecutar el proyecto

1. Abre el proyecto en **NetBeans**: `File → Open Project` → selecciona la carpeta `mavenproject2`
2. Clic derecho en el proyecto → **Clean and Build** (descarga las dependencias Maven)
3. Clic en el botón **Run** (▶) o presiona `F6`

---

## 🔑 Usuarios de prueba

| Usuario | Contraseña | Rol |
|---------|-----------|-----|
| `admin` | `admin123` | Administrador |
| `maria` | `maria123` | Recepcionista |
| `carlos` | `carlos123` | Recepcionista |

> Las contraseñas se almacenan como hash **BCrypt**. El sistema migra automáticamente contraseñas en texto plano al primer inicio de sesión.

---

## 🏗️ Estructura del proyecto

```
src/main/
├── java/com/hotel/
│   ├── Main.java                        ← Punto de entrada + shutdown hook HikariCP
│   ├── modelo/                          ← Clases POJO (entidades)
│   │   ├── Usuario.java
│   │   ├── Cliente.java
│   │   ├── Habitacion.java
│   │   ├── TipoHabitacion.java
│   │   ├── Reservacion.java
│   │   └── Factura.java
│   ├── dao/                             ← Interfaces DAO
│   │   ├── UsuarioDAO.java
│   │   ├── ClienteDAO.java
│   │   ├── HabitacionDAO.java
│   │   ├── ReservacionDAO.java
│   │   └── FacturaDAO.java
│   │   └── impl/                        ← Implementaciones JDBC + SLF4J
│   │       ├── UsuarioDAOImpl.java
│   │       ├── ClienteDAOImpl.java
│   │       ├── HabitacionDAOImpl.java
│   │       ├── ReservacionDAOImpl.java
│   │       └── FacturaDAOImpl.java
│   ├── vista/                           ← Formularios y paneles Swing
│   │   ├── LoginForm.java
│   │   ├── MenuPrincipal.java
│   │   ├── DashboardPanel.java          ← KPIs en tiempo real (SwingWorker)
│   │   ├── ClientesPanel.java
│   │   ├── HabitacionesPanel.java
│   │   ├── ReservacionesPanel.java
│   │   ├── ReservacionFormDialog.java   ← MaskFormatter en campos de fecha
│   │   ├── CheckInOutPanel.java
│   │   ├── FacturasPanel.java
│   │   ├── ReportesPanel.java           ← Exportación a PDF con PDFBox
│   │   ├── UsuariosPanel.java           ← Gestión de usuarios (ADMIN only)
│   │   └── UsuarioFormDialog.java
│   ├── util/                            ← Utilidades
│   │   ├── ConexionDB.java              ← Pool HikariCP, lee database.properties
│   │   ├── PasswordUtil.java            ← BCrypt con retrocompatibilidad
│   │   ├── Tema.java                    ← Colores y fuentes centralizados
│   │   ├── UIHelper.java                ← Fábrica de componentes Swing
│   │   └── Validaciones.java
│   └── exception/
│       └── HotelException.java          ← Excepción personalizada con códigos
└── resources/
    ├── database.properties              ← Credenciales (NO commitear)
    └── simplelogger.properties          ← Configuración SLF4J
```

---

## 📦 Dependencias Maven

```xml
<!-- MySQL JDBC -->
<dependency>
    <groupId>mysql</groupId>
    <artifactId>mysql-connector-java</artifactId>
    <version>8.0.33</version>
</dependency>

<!-- HikariCP — Connection pooling -->
<dependency>
    <groupId>com.zaxxer</groupId>
    <artifactId>HikariCP</artifactId>
    <version>5.1.0</version>
</dependency>

<!-- jBCrypt — Hash de contraseñas -->
<dependency>
    <groupId>org.mindrot</groupId>
    <artifactId>jbcrypt</artifactId>
    <version>0.4</version>
</dependency>

<!-- SLF4J Simple — Logging -->
<dependency>
    <groupId>org.slf4j</groupId>
    <artifactId>slf4j-simple</artifactId>
    <version>2.0.9</version>
</dependency>

<!-- Apache PDFBox — Exportar reportes a PDF -->
<dependency>
    <groupId>org.apache.pdfbox</groupId>
    <artifactId>pdfbox</artifactId>
    <version>2.0.30</version>
</dependency>
```

---

## 🗺️ Módulos del sistema

| # | Módulo | Descripción | Estado |
|---|--------|-------------|--------|
| 1 | Login | Autenticación BCrypt, roles ADMIN/RECEPCIONISTA | ✅ Completado |
| 2 | Dashboard | KPIs en tiempo real cargados con SwingWorker | ✅ Completado |
| 3 | Clientes | CRUD completo con búsqueda por TableRowSorter | ✅ Completado |
| 4 | Habitaciones | Gestión con estados y tipos de habitación | ✅ Completado |
| 5 | Reservaciones | Formulario con máscara de fecha, cálculo automático | ✅ Completado |
| 6 | Check-In / Check-Out | Flujo completo, genera factura automática | ✅ Completado |
| 7 | Facturación | Listado, cambio de estado, detalle de factura | ✅ Completado |
| 8 | Reportes | Estadísticas + exportar a PDF con PDFBox | ✅ Completado |
| 9 | Usuarios | CRUD de usuarios, cambio de contraseña (ADMIN only) | ✅ Completado |

---

## 🔐 Seguridad

- Contraseñas hasheadas con **BCrypt** (costo 12) — comparación en Java, no en SQL
- Migración automática de contraseñas en texto plano al primer login exitoso
- Credenciales de base de datos en archivo externo excluido del repositorio
- Permisos por rol: el módulo de Usuarios solo es accesible para ADMIN
- Pool de conexiones HikariCP con timeout y reconexión automática

---

Desarrollado con ☕ Java 11 + NetBeans 16 + MySQL 8 + Maven
