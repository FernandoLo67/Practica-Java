# Napkin Runbook — Hotel Sistema (Java 11 Swing / Maven / MySQL)

## Curation Rules
- Re-prioritize on every read.
- Keep recurring, high-value notes only.
- Max 10 items per category.
- Each item includes date + "Do instead".

---

## Execution & Validation (Highest Priority)

1. **[2026-05-16] No javac/mvn in Cowork sandbox**
   Do instead: Build via NetBeans IDE on user machine. Sandbox only for file read/write/grep. Never attempt `mvn compile` here.

2. **[2026-05-16] `getNoches()` returns `long`, not `int`**
   Do instead: Cast explicitly — `int noches = (int) res.getNoches();` wherever used in PDF/print context.

3. **[2026-05-16] Static final Color fields break dark mode**
   Do instead: Declare panel colors as `private final Color COLOR_X = Tema.COLOR_X;` (instance-level, non-static). Static fields read Tema once at class-load; instance fields re-read on each panel construction.

4. **[2026-05-16] Dark mode theme order matters**
   Do instead: Call `Tema.setModoOscuro()` + `aplicarTema()` BEFORE `initComponents()` in MenuPrincipal constructor. Reading prefs after init = wrong colors.

5. **[2026-05-18] HabitacionDAOImpl.SQL_ACTUALIZAR includes imagen_url at param 7, id at param 8**
   Do instead: Any future edit to SQL_ACTUALIZAR — keep param order: numero(1), piso(2), id_tipo(3), estado(4), descripcion(5), precio_especial(6), imagen_url(7), id(8).

---

## Architecture & Patterns

1. **[2026-05-16] DAO layer structure**
   Do instead: Interface in `com.hotel.dao`, impl in `com.hotel.dao.impl`. Always use `ConexionDB.getConexion()` inside try-with-resources. No connection field on DAO class.

2. **[2026-05-16] N+1 avoided via JOIN**
   Do instead: All DAOs use a `SQL_BASE` with `INNER JOIN tipo_habitacion` / client / user. Never lazy-load in loops.

3. **[2026-05-16] Tema.java is the single color source**
   Do instead: All panels read colors from `Tema.*` fields. Never hardcode palette values in panels.

4. **[2026-05-16] Panel color fields — instance not static**
   Do instead: `private final Color X = Tema.X;` (no `static`). Dark-mode toggle calls `navegarA(moduloActivo)` which creates new panel instance → re-reads updated Tema.

5. **[2026-05-16] MenuPrincipal.navegarA() always creates new panel**
   Do instead: Every `case` in `navegarA()` instantiates a fresh panel. Don't cache panel references. Dark mode rebuild works because of this.

6. **[2026-05-16] PDF generation uses PDFBox 2.0.30**
   Do instead: `PDType1Font` for fonts (no TTF needed), `PDPageContentStream`, `PDRectangle.A4`. `generarPDF(Factura, File)` pattern reused in both FacturasPanel and temp-file email flow.

7. **[2026-05-18] ReportesPanel inner classes: GraficoBarras, GraficoLineas, BarraProgreso**
   Do instead: All three are `static` inner classes with `paintComponent`. GraficoLineas takes `double[]` values (not `int[]`). Add new chart types as static inner classes.

---

## Shell & Command Reliability

1. **[2026-05-16] Workspace mount path**
   Do instead: User project at `/sessions/wizardly-exciting-knuth/mnt/Netbeans/mavenproject2/`. File tools use Windows path `C:\Users\COMPUBYTES\Desktop\Netbeans\mavenproject2\`. Both point to same dir.

2. **[2026-05-16] Maven wrapper absent**
   Do instead: No `mvnw`. No Maven in PATH. Can't compile in sandbox. Read files and write; user runs build in NetBeans.

---

## Domain Behavior Guardrails

1. **[2026-05-16] Email disabled gracefully when unconfigured**
   Do instead: `EmailService.habilitado` flag. If `email.properties` missing or has placeholder credentials, all send methods return early/false silently. Never throw on unconfigured email.

2. **[2026-05-16] Disponibilidad SQL excludes MANTENIMIENTO + overlap check**
   Do instead: `WHERE h.estado != 'MANTENIMIENTO' AND h.id NOT IN (SELECT ... WHERE estado NOT IN ('CANCELADA','CHECKOUT') AND NOT (checkout <= ? OR checkin >= ?))`. Param order: `(checkin, checkout)`.

3. **[2026-05-18] Auto-checkout usa dos UPDATE separados**
   Do instead: Primero UPDATE reservaciones SET estado='CHECKOUT', luego UPDATE habitaciones SET estado='DISPONIBLE' via JOIN con id_reservacion. Un solo UPDATE no alcanza ambas tablas en HikariCP con autocommit.

4. **[2026-05-18] MenuPrincipal timers — 3 activos, todos deben pararse en cerrarSesion()**
   Do instead: `timerCampana` (60 s), `timerAutoCheckout` (60 min), `timerRecordatorio` (12 h). Stop all three in `cerrarSesion()`. Recordatorio envía emails pre-checkin del día siguiente.

5. **[2026-05-18] EmailService — mismo paquete que HotelConfig: no import necesario**
   Do instead: `EmailService` está en `com.hotel.util`. No agregar `import com.hotel.util.HotelConfig` — compilará con "duplicate import" warning o error en algunos JDKs.

6. **[2026-05-16] ReservacionFormDialog.preseleccionarHabitacion() — call before setVisible**
   Do instead: Call after `new ReservacionFormDialog(...)` but before `.setVisible(true)`. Combos must be populated first (done in constructor via `cargarCombos()`).

7. **[2026-05-18] IVA configurable: HotelConfig.getIva() en Factura constructor**
   Do instead: `this.impuesto = subtotal * HotelConfig.getIva();` — nunca hardcodear `0.18`. DatosHotelPanel valida rango 0–1 antes de guardar.

---

## Tests de Integración

1. **[2026-05-18] Tests DAO requieren BD real**
   Do instead: Usar documentos/números de prueba únicos (DOC_CLI="RES-TEST-CLI-001", NUM_HAB="TST-99"). Limpiar con DELETE en @BeforeEach + @AfterEach. Nunca usar datos reales de producción.

2. **[2026-05-18] Fixtures de FK en integración: orden de creación/limpieza**
   Do instead: Crear cliente → tipo → habitación → usuario en @BeforeAll. Eliminar reservaciones primero (FK), luego habitación, luego cliente en @AfterAll.

3. **[2026-05-18] Test disponibilidad: adyacente no es solapamiento**
   Do instead: `checkout_existente = checkin_nuevo` es VÁLIDO (no se superpone). SQL usa `NOT (fecha_checkout <= ? OR fecha_checkin >= ?)`. Verificar este caso límite.

4. **[2026-05-18] BackupService: mysqldump PATH antes que rutas absolutas**
   Do instead: Intentar "mysqldump" en PATH primero. Si falla, recorrer RUTAS_WIN. Si ninguna funciona, mostrar mensaje claro al usuario.

---

## User Directives

1. **[2026-05-16] Caveman mode active for this project**
   Do instead: Respond in caveman English (drop articles/filler, short synonyms, fragments OK). Technical terms stay exact. Code blocks unchanged.

2. **[2026-05-16] Commit messages requested compact**
   Do instead: One-line subject + short body with file list and actions. No verbose postmortem style.

3. **[2026-05-18] All major features implemented as of 2026-05-18**
   Do instead: IVA configurable ✓, imagen habitación ✓, recordatorio pre-checkin ✓, historial cliente ✓, gráfica ingresos mensuales ✓, validación tel/email ✓, README+migrations ✓. Check before re-implementing.
