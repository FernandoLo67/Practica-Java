# 🏨 Hotel Sistema

Sistema de Administración Hotelera desarrollado en **Java 11** con **Swing**, **Maven** y **MySQL**.

Proyecto universitario construido módulo por módulo con arquitectura por capas (Modelo - DAO - Vista).

---

## 📋 Requisitos previos

Antes de ejecutar el proyecto asegúrate de tener instalado:

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

Verifica que se crearon las 6 tablas bajo el schema `hotel_sistema`:

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

Abre el archivo `src/main/java/com/hotel/util/ConexionDB.java` y edita estas líneas con tus datos de MySQL:

```java
private static final String DB_USER     = "root";      // tu usuario MySQL
private static final String DB_PASSWORD = "";          // tu contraseña MySQL
```

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

---

## 🏗️ Estructura del proyecto

```
src/main/java/com/hotel/
├── Main.java                        ← Punto de entrada
├── modelo/                          ← Clases POJO (entidades)
│   ├── Usuario.java
│   ├── Cliente.java
│   ├── Habitacion.java
│   ├── TipoHabitacion.java
│   ├── Reservacion.java
│   └── Factura.java
├── dao/                             ← Acceso a datos (JDBC)
│   ├── UsuarioDAO.java              ← Interfaz
│   └── impl/
│       └── UsuarioDAOImpl.java      ← Implementación
├── vista/                           ← Formularios Swing
│   ├── LoginForm.java
│   └── MenuPrincipal.java
└── util/                            ← Utilidades
    ├── ConexionDB.java              ← Conexión MySQL (Singleton)
    └── Validaciones.java            ← Validaciones de formulario
```

---

## 📦 Dependencias Maven

```xml
<dependency>
    <groupId>mysql</groupId>
    <artifactId>mysql-connector-java</artifactId>
    <version>8.0.33</version>
</dependency>
```

---

## 🗺️ Módulos del sistema

| # | Módulo | Estado |
|---|--------|--------|
| 1 | Login + Base de datos | ✅ Completado |
| 2 | Gestión de Clientes | 🔧 En desarrollo |
| 3 | Gestión de Habitaciones | 🔧 En desarrollo |
| 4 | Reservaciones | 🔧 En desarrollo |
| 5 | Check-In / Check-Out | 🔧 En desarrollo |
| 6 | Facturación | 🔧 En desarrollo |
| 7 | Reportes | 🔧 En desarrollo |

---

Desarrollado con Java 11 + NetBeans 16 + MySQL 8
