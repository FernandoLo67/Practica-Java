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

3. **[2026-05-16] ReservacionFormDialog.preseleccionarHabitacion() — call before setVisible**
   Do instead: Call after `new ReservacionFormDialog(...)` but before `.setVisible(true)`. Combos must be populated first (done in constructor via `cargarCombos()`).

4. **[2026-05-16] FacturasPanel.btnEmail — disable on load, enable on selection**
   Do instead: In `cargarFacturas()` call `btnEmail.setEnabled(false)`. In selection listener `setEnabled(hay)`. Same pattern as btnImprimir/btnAnular.

5. **[2026-05-16] CalendarioPanel occupancy map**
   Do instead: `Map<Integer, Map<Integer, Reservacion>> ocupacion` — habId → day-of-month → reservación. Mark every day from checkin to checkout-1 (exclusive end).

---

## User Directives

1. **[2026-05-16] Caveman mode active for this project**
   Do instead: Respond in caveman English (drop articles/filler, short synonyms, fragments OK). Technical terms stay exact. Code blocks unchanged.

2. **[2026-05-16] Commit messages requested compact**
   Do instead: One-line subject + short body with file list and actions. No verbose postmortem style.

3. **[2026-05-16] All 4 high-impact features implemented**
   Do instead: CalendarioPanel ✓, DisponibilidadDialog ✓, EmailService.enviarFacturaPDF ✓, ReportesPanel.exportarPDF ✓. Don't re-implement these.
