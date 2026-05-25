# 🔍 QA Review — Hotel Sistema v1.1
**Fecha:** 2026-05-23  
**Revisado por:** QA Engineer Senior (análisis automatizado + manual)  
**Alcance:** 58 clases main · 6 archivos de test · ~5 500 líneas de código

---

## ÍNDICE
1. [CRÍTICO — 3 issues](#critico)
2. [ALTO — 6 issues](#alto)
3. [MEDIO — 9 issues](#medio)
4. [BAJO — 6 issues](#bajo)
5. [Arquitectura y SOLID](#arquitectura)
6. [Cobertura de pruebas](#tests)
7. [Escenarios de falla reales](#escenarios)
8. [Resumen y puntuación](#resumen)

---

## 1. CRÍTICO {#critico}

---

### C-01 · IVA hardcodeado en `Reservacion.java` — inconsistencia con `Factura`

**Archivo:** `src/main/java/com/hotel/modelo/Reservacion.java` · línea 107  
**Gravedad:** 🔴 CRÍTICO

**Descripción:**  
`getTotalConImpuesto()` siempre multiplica por `1.18` en lugar de leer `HotelConfig.getIva()`. Al mismo tiempo, `Factura.java` SÍ usa `HotelConfig.getIva()`. Esto crea una inconsistencia de datos real:

- El **email de confirmación** muestra un total calculado con 18 % fijo.
- El **Calendario** muestra totales con 18 % fijo.
- La **factura real** usa el IVA configurado (p.ej. 12 % en Guatemala).

Si el hotel configura IVA al 12 %, el email le dice al cliente que debe Q 118 y la factura le cobra Q 112. Es un error de dinero.

**Código actual:**
```java
// Reservacion.java - línea 107
public double getTotalConImpuesto() {
    return getTotalSinImpuesto() * 1.18;  // ❌ hardcoded
}
```

**Código corregido:**
```java
public double getTotalConImpuesto() {
    return getTotalSinImpuesto() * (1 + HotelConfig.getIva());  // ✅ configurable
}
```

También hay que actualizar el diálogo de checkout en `CheckInOutPanel.java` línea 327 que dice `"IVA (18%):"` hardcodeado como texto.

---

### C-02 · Falta de transacción en el proceso de Check-Out

**Archivo:** `src/main/java/com/hotel/vista/CheckInOutPanel.java` · líneas 309–314  
**Gravedad:** 🔴 CRÍTICO

**Descripción:**  
El checkout ejecuta 3 operaciones de BD independientes, cada una con su propia conexión del pool:

```java
// Líneas 309-314 — 3 conexiones separadas, sin transacción
reservacionDAO.cambiarEstado(id, Reservacion.ESTADO_CHECKOUT);   // conn 1
habitacionDAO.cambiarEstado(r.getHabitacion().getId(), "DISPONIBLE"); // conn 2
Factura factura = new Factura(r, metodo);
facturaDAO.guardar(factura);                                       // conn 3
```

**Escenario de falla real:**  
Si el servidor MySQL cae después del paso 1 pero antes del paso 3:
- La reservación queda en `CHECKOUT`
- La habitación queda `OCUPADA` (no se liberó)
- No existe factura

El hotel tiene una habitación fantasma ocupada que nadie puede reservar, y no hay factura del cliente.

**Solución — checkout atómico:**
```java
private void ejecutarCheckout(Reservacion r, String metodoPago) {
    try (Connection conn = ConexionDB.getConexion()) {
        conn.setAutoCommit(false);
        try {
            // 1. Cambiar estado reservación
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE reservaciones SET estado = ? WHERE id = ?")) {
                ps.setString(1, Reservacion.ESTADO_CHECKOUT);
                ps.setInt(2, r.getId());
                ps.executeUpdate();
            }
            // 2. Liberar habitación
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE habitaciones SET estado = 'DISPONIBLE' WHERE id = ?")) {
                ps.setInt(1, r.getHabitacion().getId());
                ps.executeUpdate();
            }
            // 3. Crear factura
            Factura factura = new Factura(r, metodoPago);
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO facturas (id_reservacion, subtotal, impuesto, total, estado, metodo_pago) " +
                    "VALUES (?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, r.getId());
                ps.setDouble(2, factura.getSubtotal());
                ps.setDouble(3, factura.getImpuesto());
                ps.setDouble(4, factura.getTotal());
                ps.setString(5, Factura.ESTADO_PAGADA);
                ps.setString(6, metodoPago);
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) factura.setId(keys.getInt(1));
                }
            }
            conn.commit();
            // ... mostrar confirmación
        } catch (SQLException ex) {
            conn.rollback();
            throw ex;
        }
    } catch (SQLException e) {
        log.error("Fallo checkout para reservación {}", r.getId(), e);
        mostrarError("No se completó el checkout: " + e.getMessage());
    }
}
```

---

### C-03 · `BackupService` expone la contraseña en argumentos del proceso

**Archivo:** `src/main/java/com/hotel/util/BackupService.java` · línea 102  
**Gravedad:** 🔴 CRÍTICO (seguridad)

**Descripción:**  
La contraseña de la base de datos se pasa como argumento de línea de comandos:

```java
if (!pass.isEmpty()) cmd.add("--password=" + pass);  // ❌ visible en ps aux / task manager
```

En Windows, cualquier usuario con acceso al Administrador de tareas puede ver los argumentos de procesos. En Linux/macOS es visible con `ps aux`. El código ya hace lo correcto al usar `MYSQL_PWD` como variable de entorno, pero luego también agrega el argumento redundante y peligroso.

**Solución:**
```java
// Eliminar completamente la línea:
// cmd.add("--password=" + pass);

// Mantener solo el env (ya existente):
pb.environment().put("MYSQL_PWD", pass);

// Agregar también esta opción para silenciar warnings:
// No se necesita --password= cuando MYSQL_PWD está seteado
```

---

## 2. ALTO {#alto}

---

### A-01 · `MenuPrincipal` usa `private static Color` — sidebar no refleja el tema al toggling

**Archivo:** `src/main/java/com/hotel/vista/MenuPrincipal.java` · líneas 58–62  
**Gravedad:** 🟠 ALTO

**Descripción:**  
Los colores del sidebar son campos `static`. Cuando el usuario cambia de tema oscuro a claro (o viceversa), `aplicarTema()` reasigna esas variables estáticas, pero los componentes `JPanel` ya construidos siguen con sus colores anteriores. El sidebar nunca se actualiza visualmente hasta reiniciar la app.

```java
// MenuPrincipal.java — problema
private static Color COLOR_SIDEBAR;       // ❌ static
private static Color COLOR_SIDEBAR_HOVER;
```

Todos los paneles de formulario fueron corregidos en la sesión anterior, pero `MenuPrincipal` quedó sin corregir porque no es un diálogo que se destruya y reconstruya — es permanente.

**Solución:**  
La solución correcta es que al cambiar el tema se llame a un método `refreshTema()` que reaplique los colores a todos los componentes del sidebar ya construidos, o que se navegue nuevamente al módulo activo para reconstruir el panel de contenido. Alternativamente, cambiar el tema podría requerir reiniciar la sesión (documentarlo claramente en la UI).

---

### A-02 · `log.error()` pierde el stack trace en todos los DAOs

**Archivos:** todos los `*DAOImpl.java` — 36+ ocurrencias  
**Gravedad:** 🟠 ALTO

**Descripción:**  
La forma correcta de loggear una excepción con SLF4J es pasar el objeto `Throwable` como último argumento. Al usar concatenación de string se pierde toda la traza de la pila:

```java
// ❌ Actual — solo imprime el mensaje, sin stack trace
log.error("Error en listarTodas(): " + e.getMessage());

// ✅ Correcto — incluye clase, método, línea y causa completa
log.error("Error en listarTodas()", e);
```

En producción, cuando hay un error de SQL raro (deadlock, constraint violation, timeout), los logs no dirán nada útil — solo "Error en guardar(): Duplicate entry '101' for key...". El stack trace completo es esencial para diagnosticar.

**Corrección masiva (bash):**
```bash
find src/main/java -name "*DAOImpl.java" -exec sed -i \
  's/log\.error("\(.*\): " + e\.getMessage());/log.error("\1", e);/g' {} \;
```

---

### A-03 · Hilos de bitácora sin pool — riesgo de thread exhaustion

**Archivo:** `src/main/java/com/hotel/dao/impl/BitacoraDAOImpl.java` · línea 50  
**Gravedad:** 🟠 ALTO

**Descripción:**  
Cada registro de bitácora crea un hilo nuevo del sistema:

```java
new Thread(() -> {
    // insertar en BD...
}, "bitacora-insert").start();
```

En un escenario de carga alta (muchos checkouts simultáneos, exportaciones, búsquedas), se pueden crear decenas de hilos en segundos. Cada hilo de Java consume ~512 KB de stack. Con 200 hilos simultáneos = 100 MB de memoria solo en threads de bitácora.

**Solución — usar ExecutorService:**
```java
// En BitacoraDAOImpl — campo de clase
private static final ExecutorService EXECUTOR =
    Executors.newFixedThreadPool(2, r -> {
        Thread t = new Thread(r, "bitacora-worker");
        t.setDaemon(true);
        return t;
    });

// En registrar():
EXECUTOR.submit(() -> {
    // lógica de inserción...
});
```

---

### A-04 · `useSSL=false` + `allowPublicKeyRetrieval=true` inseguros para producción

**Archivo:** `src/main/java/com/hotel/util/ConexionDB.java` · líneas 86–88  
**Gravedad:** 🟠 ALTO (seguridad)

**Descripción:**  
```java
"?useSSL=false"                      // ❌ tráfico BD sin cifrar
"&allowPublicKeyRetrieval=true"       // ❌ vulnerable a MITM en auth
```

Si la base de datos está en otro servidor (servidor compartido, VPS, nube), cualquiera en la red puede interceptar las credenciales y datos del hotel.

**Solución para producción:**
```java
// Cambiar a:
"?useSSL=true"
"&verifyServerCertificate=false"      // solo si es servidor local/desarrollo
// O mejor, configurar certificados SSL en MySQL
```

Para desarrollo local está aceptable, pero debe documentarse claramente que esto no es configuración de producción.

---

### A-05 · BCrypt no verifica hashes `$2y$` (formato moderno de PHP/OpenSSL)

**Archivo:** `src/main/java/com/hotel/util/PasswordUtil.java` · líneas 39–44  
**Gravedad:** 🟠 ALTO

**Descripción:**
```java
if (hashAlmacenado.startsWith("$2a$") || hashAlmacenado.startsWith("$2b$")) {
    return BCrypt.checkpw(passwordPlano, hashAlmacenado);
}
```

Si la base de datos alguna vez recibe hashes generados por otro sistema (PHP, Python, Ruby) que use el prefijo `$2y$` (estándar IEEE), el sistema los tratará como "texto plano antiguo" y los comparará directamente, fallando la autenticación o, peor, aceptando cualquier contraseña que literalmente sea igual al hash.

**Solución:**
```java
if (hashAlmacenado.startsWith("$2a$") 
    || hashAlmacenado.startsWith("$2b$")
    || hashAlmacenado.startsWith("$2y$")) {
    return BCrypt.checkpw(passwordPlano, hashAlmacenado);
}
```

---

### A-06 · `CheckInOutPanel`: texto "IVA (18%)" hardcodeado

**Archivo:** `src/main/java/com/hotel/vista/CheckInOutPanel.java` · línea 327  
**Gravedad:** 🟠 ALTO

**Descripción:**
```java
"  IVA (18%):    Q " + String.format("%.2f", factura.getImpuesto()) + "\n" +
```

El porcentaje en el texto no refleja el IVA real configurado. Si el hotel está en Guatemala con IVA al 12 %, el diálogo dirá "IVA (18%)" pero calculará 12 %.

**Solución:**
```java
String pctIva = String.format("%.0f%%", HotelConfig.getIva() * 100);
"  IVA (" + pctIva + "):   Q " + String.format("%.2f", factura.getImpuesto()) + "\n" +
```

---

## 3. MEDIO {#medio}

---

### M-01 · Ausencia de capa de Servicio — vista acopla directamente con DAOs

**Gravedad:** 🟡 MEDIO (arquitectura)

**Descripción:**  
Las clases de la capa `vista` instancian DAOs directamente:
```java
// CheckInOutPanel.java
private final ReservacionDAOImpl reservacionDAO = new ReservacionDAOImpl();
private final HabitacionDAOImpl  habitacionDAO  = new HabitacionDAOImpl();
private final FacturaDAOImpl     facturaDAO     = new FacturaDAOImpl();
```

Hay 29 instanciaciones de `*DAOImpl` directamente en la vista. Esto viola el principio de Inversión de Dependencias (SOLID-D). La vista conoce la implementación concreta, haciendo imposible reemplazar la BD sin tocar los formularios.

**Impacto:** La lógica de negocio (checkout = actualizar reservación + habitación + factura) está mezclada en el panel de UI, no en un `ServicioReservacion`. Esto hace el código muy difícil de testear.

**Solución ideal:** Crear `com.hotel.service.*` con `ReservacionService`, `CheckInService`, etc. que encapsulen la lógica de negocio y usen las interfaces DAO (no las implementaciones).

---

### M-02 · `HotelConfig.defaults()` no incluye `hotel.iva`

**Archivo:** `src/main/java/com/hotel/util/HotelConfig.java` · método `defaults()`  
**Gravedad:** 🟡 MEDIO

**Descripción:**  
El método `defaults()` que inicializa las propiedades base no incluye `hotel.iva`:

```java
private static Properties defaults() {
    Properties p = new Properties();
    p.setProperty("hotel.nombre",    DEF_NOMBRE);
    // ... otros campos
    // ❌ Falta: p.setProperty("hotel.iva", String.valueOf(DEF_IVA));
    return p;
}
```

Si el archivo `hotel.properties` se regenera a partir de `defaults()`, se pierde la configuración de IVA y vuelve al 18 % por defecto sin advertencia al usuario.

**Solución:**
```java
p.setProperty("hotel.iva", String.valueOf(DEF_IVA));
```

---

### M-03 · Plantillas de email HTML sin escapar datos del usuario

**Archivo:** `src/main/java/com/hotel/util/EmailService.java`  
**Gravedad:** 🟡 MEDIO (seguridad menor)

**Descripción:**  
Los datos del cliente se insertan directamente en el HTML del email:

```java
String nombreCliente = res.getCliente().getNombreCompleto();
// ...
"<strong>" + nombreCliente + "</strong>"  // ❌ sin escapar
```

Si el nombre del cliente contiene `<b>`, `&`, o `"`, el HTML del email se verá mal o puede romper el layout. Aunque el vector de ataque XSS en emails es limitado, es mala práctica.

**Solución:**
```java
private static String escapeHtml(String s) {
    if (s == null) return "";
    return s.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;");
}
// Usar: escapeHtml(nombreCliente)
```

---

### M-04 · Tests de integración requieren BD real sin marcadores

**Archivos:** `src/test/java/com/hotel/dao/*IntegrationTest.java`  
**Gravedad:** 🟡 MEDIO

**Descripción:**  
Los tres tests de integración (`ClienteDAOIntegrationTest`, `HabitacionDAOIntegrationTest`, `ReservacionDAOIntegrationTest`) intentan conectarse a MySQL. Si MySQL no está corriendo, el CI/CD falla con un error de conexión críptico en lugar de un mensaje claro.

Además, si se ejecutan en producción sin limpieza, insertan datos reales (`@Test` que crean clientes de prueba).

**Solución:**
```java
@Tag("integration")        // marcar como integration test
@EnabledIfSystemProperty(named = "runIntegrationTests", matches = "true")
class ClienteDAOIntegrationTest {
    // ...
}
```
Y en `pom.xml`:
```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-surefire-plugin</artifactId>
    <configuration>
        <excludedGroups>integration</excludedGroups>  <!-- excluir por defecto -->
    </configuration>
</plugin>
```

---

### M-05 · `Mavenproject2.java` — clase basura de NetBeans

**Archivo:** `src/main/java/com/mycompany/mavenproject2/Mavenproject2.java`  
**Gravedad:** 🟡 MEDIO

**Descripción:**  
Este archivo es el stub inicial generado por NetBeans al crear el proyecto y nunca fue eliminado:

```java
public class Mavenproject2 {
    public static void main(String[] args) {
        System.out.println("Hello World!");
    }
}
```

Está en un paquete diferente (`com.mycompany.mavenproject2`) al resto del proyecto (`com.hotel`). No hace nada útil, confunde a quien lea el código y aumenta el JAR innecesariamente.

**Solución:** Eliminar el archivo.

---

### M-06 · Falta `serialVersionUID` en `HotelException`

**Archivo:** `src/main/java/com/hotel/exception/HotelException.java`  
**Gravedad:** 🟡 MEDIO

**Descripción:**  
`HotelException extends RuntimeException` (que es `Serializable`) sin declarar `serialVersionUID`. La JVM genera uno automáticamente basado en la estructura de la clase. Si el compilador cambia el orden de los campos, el ID cambia y la deserialización de excepciones guardadas falla.

**Solución:**
```java
public class HotelException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    // ...
}
```

---

### M-07 · Dead code en `LoginForm` — dos métodos nunca usados

**Archivo:** `src/main/java/com/hotel/vista/LoginForm.java`  
**Gravedad:** 🟡 MEDIO

**Descripción:**  
Los métodos `crearEtiquetaCampo(String)` y `estilizarCampo(JTextField)` existen pero nunca son invocados (las etiquetas y campos se crean directamente inline en `crearPanelFormulario()`):

```java
private JLabel crearEtiquetaCampo(String texto) { ... }   // ❌ nunca llamado
private void estilizarCampo(JTextField campo) { ... }      // ❌ nunca llamado
```

**Solución:** Eliminar ambos métodos para reducir el ruido del código.

---

### M-08 · Contraseña de BD expuesta en `System.out.println`

**Archivo:** `src/main/java/com/hotel/dao/impl/UsuarioDAOImpl.java` · línea 97  
**Gravedad:** 🟡 MEDIO (seguridad)

**Descripción:**
```java
System.out.println("✓ Password del usuario '" + usuario + "' migrado a BCrypt automáticamente.");
```

Aunque no imprime la contraseña en sí, esta salida aparece en la consola del servidor en texto plano, revelando que el usuario acaba de autenticarse con éxito. En ambientes de producción, la consola puede ser capturada en logs que cualquier administrador puede leer.

**Solución:**
```java
log.info("Password del usuario '{}' migrado a BCrypt automáticamente.", usuario);
// Además, eliminar el ex.printStackTrace() en LoginForm línea 366:
log.error("Error de conexión durante login", ex);
```

---

### M-09 · `autoReconnect=true` está deprecated y puede ocultar errores reales

**Archivo:** `src/main/java/com/hotel/util/ConexionDB.java` · línea 91  
**Gravedad:** 🟡 MEDIO

**Descripción:**  
```java
"&autoReconnect=true"
```

MySQL Connector/J tiene esta opción deprecada desde la versión 8.x. La reconexión automática puede causar que la aplicación continúe usando una transacción en una nueva conexión, potencialmente perdiendo datos. HikariCP ya maneja la reconexión correctamente con `keepaliveTime` y `connectionTestQuery`.

**Solución:** Eliminar `&autoReconnect=true` de la URL y confiar en HikariCP para el manejo de conexiones.

---

## 4. BAJO {#bajo}

---

### B-01 · Contraseña mínima de 6 caracteres es insuficiente para producción

**Archivo:** `src/main/java/com/hotel/util/Validaciones.java` · línea referente a `validarPassword`  
**Gravedad:** 🔵 BAJO

**Descripción:**  
El sistema acepta contraseñas de 6 caracteres como "abc123", que son trivialmente crackeables. Para un sistema hotelero con datos de clientes, se recomienda al menos 8 caracteres con complejidad.

**Mejora sugerida:**
```java
public static String validarPassword(String password) {
    if (password == null || password.isEmpty()) return "La contraseña es obligatoria.";
    if (password.length() < 8) return "La contraseña debe tener al menos 8 caracteres.";
    if (!password.matches(".*[A-Z].*")) return "Debe contener al menos una mayúscula.";
    if (!password.matches(".*[0-9].*")) return "Debe contener al menos un número.";
    return null;
}
```

---

### B-02 · `SimpleDateFormat` en `EmailService` no es thread-safe

**Archivo:** `src/main/java/com/hotel/util/EmailService.java` · línea 19  
**Gravedad:** 🔵 BAJO

**Descripción:**
```java
private static final SimpleDateFormat FMT = new SimpleDateFormat("dd/MM/yyyy");
```

`SimpleDateFormat` no es thread-safe. Aunque en la práctica los métodos de EmailService se llaman desde hilos de email separados, si dos emails se envían simultáneamente podrían corromper el formato de fecha del otro.

**Solución:**
```java
// Reemplazar con DateTimeFormatter (Java 8+, inmutable y thread-safe)
private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
// Y en uso:
res.getFechaCheckin().toLocalDate().format(FMT)
```

---

### B-03 · `HotelConfig.guardar()` no es thread-safe

**Archivo:** `src/main/java/com/hotel/util/HotelConfig.java` · método `guardar()`  
**Gravedad:** 🔵 BAJO

**Descripción:**  
Si dos usuarios administradores guardan la configuración del hotel simultáneamente desde terminales diferentes (escenario remoto pero posible), el archivo `hotel.properties` puede quedar corrompido.

**Solución:**
```java
public static synchronized void guardar(Properties nuevos) throws IOException {
    // ... lógica actual
}
```

---

### B-04 · Falta `@Override` explícito en métodos del modelo

**Archivos:** varios modelos  
**Gravedad:** 🔵 BAJO

**Descripción:**  
Los métodos `toString()` en `Factura`, `Reservacion`, etc. no tienen `@Override`. Si en el futuro se cambia la firma por error, el compilador no lo detecta.

---

### B-05 · Versión de artefacto en pom.xml sigue siendo `1.0-SNAPSHOT`

**Archivo:** `pom.xml`  
**Gravedad:** 🔵 BAJO

**Descripción:**  
El proyecto está en versión `1.1` funcionalmente (con todas las mejoras completadas), pero el `pom.xml` sigue en `1.0-SNAPSHOT`. Esto causa confusión en el versionado del JAR generado.

**Solución:** Actualizar a `<version>1.1.0</version>` al hacer el release.

---

### B-06 · Falta README.md y script SQL de instalación

**Gravedad:** 🔵 BAJO

**Descripción:**  
Un proyecto sin documentación de instalación es imposible de reproducir en otro entorno. No hay instrucciones de cómo crear la BD, qué usuario MySQL necesita, cómo configurar los `.properties`, ni cómo ejecutar el JAR.

---

## 5. Arquitectura y SOLID {#arquitectura}

| Principio | Estado | Nota |
|-----------|--------|------|
| **S** — Single Responsibility | ⚠️ Parcial | Los paneles de UI mezclan lógica de negocio (checkout, facturación) con presentación |
| **O** — Open/Closed | ✅ Bien | Los DAOs implementan interfaces; agregar nueva BD requiere solo nueva impl |
| **L** — Liskov | ✅ Bien | Las implementaciones respetan los contratos de las interfaces |
| **I** — Interface Segregation | ✅ Bien | Interfaces DAO separadas por entidad |
| **D** — Dependency Inversion | ❌ Violado | Vista instancia `*DAOImpl` directamente (29 veces) en lugar de inyectar interfaces |

**Clean Code:**
- ✅ Nombres descriptivos en español consistentes
- ✅ Métodos cortos y con una responsabilidad (en DAOs)
- ✅ Constantes SQL extraídas al inicio de la clase
- ✅ Comentarios Javadoc en utilidades clave
- ⚠️ Algunos paneles de vista (ReportesPanel) superan las 500 líneas

---

## 6. Cobertura de pruebas {#tests}

| Capa | Clases | Tests | Cobertura estimada |
|------|--------|-------|--------------------|
| `modelo` | 7 | 1 | ~15 % |
| `dao.impl` | 7 | 3 (integración) | ~40 % (solo happy path) |
| `util` | 8 | 2 | ~25 % |
| `vista` | 20 | 0 | 0 % |
| **Total** | **58** | **6** | **~15 %** |

**Ausencias críticas:**
- No hay tests para `PasswordUtil.verificar()` con input nulo
- No hay tests para `Reservacion.getNoches()` con fechas invertidas (checkout < checkin → retorna negativo)
- No hay tests para `HotelConfig` (carga de IVA configurable)
- Cero tests de UI
- Los tests de integración no usan `@BeforeEach` para limpiar datos de prueba — pueden fallar si se corren dos veces

---

## 7. Escenarios de falla reales {#escenarios}

**Escenario 1 — Corte de luz durante checkout:**  
El sistema procesa los 3 pasos del checkout. Si la energía se va entre el paso 1 y 2, la habitación queda `OCUPADA` eternamente porque nadie hizo checkout real. El recepcionista no lo nota hasta que intenta asignar esa habitación a otro cliente.

**Escenario 2 — Hotel cambia IVA a 12 % para cumplir con SAT Guatemala:**  
El administrador actualiza el IVA en la configuración. Las facturas se generan correctamente al 12 %. Pero los emails de confirmación siguen mostrando el total con 18 %. El cliente llega al hotel y discute porque el email le dijo que debía Q 118 pero la factura cobra Q 112.

**Escenario 3 — 50 check-ins simultáneos en temporada alta:**  
El sistema crea 50 hilos de bitácora en ~2 segundos. Cada hilo espera una conexión del pool. Con `maximumPoolSize=10`, 40 hilos quedan bloqueados esperando. El pool tiene `connectionTimeout=30 000 ms`. Después de 30 segundos, 40 operaciones de bitácora fallan con timeout. Los registros de auditoría están incompletos.

**Escenario 4 — Mismo número de habitación en dos registros:**  
El campo `numero` en habitaciones tiene una validación en la vista pero si dos usuarios crean habitaciones simultáneamente con el mismo número, la BD permite el duplicado (depende del schema SQL — no se revisó la constraint `UNIQUE`). El sistema mostraría dos "Habitación 101".

**Escenario 5 — Email del cliente con carácter especial:**  
Un cliente se registra con nombre "Juan & María <VIP>". El email de confirmación tiene HTML malformado, el cliente puede recibir un email con formato roto o, dependiendo del cliente de correo, con los tags visibles.

---

## 8. Resumen General {#resumen}

### Puntuación: 7.2 / 10

| Categoría | Puntuación |
|-----------|------------|
| Seguridad | 6.5 / 10 |
| Arquitectura | 6.8 / 10 |
| Calidad de código | 8.0 / 10 |
| Manejo de errores | 6.5 / 10 |
| Cobertura de pruebas | 4.0 / 10 |
| UX / UI | 8.5 / 10 |
| Documentación interna | 8.0 / 10 |
| Rendimiento | 7.5 / 10 |

---

### Fortalezas del proyecto
- Arquitectura en capas clara (modelo / DAO / vista / util)
- BCrypt implementado correctamente para contraseñas
- HikariCP con configuración correcta del pool
- PreparedStatements en todos los DAOs (sin SQL injection)
- Tema oscuro/claro con sistema centralizado `Tema.java`
- Bitácora de auditoría asíncrona
- EmailService no bloquea la UI
- HotelConfig con IVA configurable (aunque inconsistente en uso)
- Código bien documentado con Javadoc

---

### Principales riesgos (ordenados por impacto)

1. **TRANSACCIÓN FALTANTE EN CHECKOUT** — puede corromper datos en producción
2. **IVA INCONSISTENTE** — genera discrepancias financieras entre email y factura
3. **CONTRASEÑA EN ARGUMENTOS CLI** — riesgo de seguridad en backups
4. **STACK TRACE PERDIDO EN LOGS** — imposible diagnosticar errores en producción
5. **SIN CAPA DE SERVICIO** — lógica de negocio en la UI, muy difícil de testear y mantener

---

### Prioridad de corrección

| Prioridad | Issue | Esfuerzo |
|-----------|-------|----------|
| 1 | C-02: Transacción en checkout | 2h |
| 2 | C-01: IVA hardcoded en Reservacion | 15min |
| 3 | C-03: Contraseña en args backup | 5min |
| 4 | A-02: Stack trace en logs | 10min (sed masivo) |
| 5 | A-06: Texto IVA en CheckOut dialog | 5min |
| 6 | A-03: ExecutorService en bitácora | 30min |
| 7 | M-02: hotel.iva en defaults() | 5min |
| 8 | M-05: Eliminar Mavenproject2.java | 1min |
| 9 | B-02: DateTimeFormatter en EmailService | 10min |
| 10 | A-05: BCrypt $2y$ support | 2min |

---

### Recomendaciones finales

1. **No está listo para producción** en el estado actual debido a C-01, C-02 y C-03. Con esas tres correcciones, el riesgo baja significativamente.

2. **Antes de lanzar:** agregar `UNIQUE (numero)` en la tabla `habitaciones` si no existe; agregar índices en `reservaciones(fecha_checkin, fecha_checkout, estado)` para acelerar consultas de disponibilidad.

3. **Para la siguiente versión:** crear la capa `com.hotel.service.*` para separar lógica de negocio de la UI. Empezar por `ReservacionService.realizarCheckout()`.

4. **Cobertura de tests:** agregar al menos tests unitarios para los casos borde de `Reservacion` (fechas inválidas, noches negativas) y `PasswordUtil` (null, vacío, $2y$).

5. **El proyecto tiene una base sólida** — el código es legible, bien estructurado y sigue buenas prácticas en la mayoría de los casos. Las correcciones críticas son puntuales y de bajo esfuerzo.
