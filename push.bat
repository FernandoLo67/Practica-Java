@echo off
cd /d "C:\Users\COMPUBYTES\Desktop\Netbeans\mavenproject2"
git add .
git commit -m "Batch 4 — Clientes mejorado, Tipos de Habitacion, precio especial y Email

Mejoras modulo Clientes:
- ClientesPanel v3: KPI cards (total, activos, nuevos este mes)
- Columnas Activo y Fecha Registro en la tabla de clientes
- Toggle Solo activos / Mostrar todos para filtrar rapidamente
- Boton Historial: ClienteHistorialDialog con reservaciones y facturas del cliente
- ClienteFormDialog: checkbox activo/inactivo en modo edicion + BitacoraService
- ClienteDAOImpl: contarActivos(), contarNuevosEsteMes(), cambiarActivo()
- ClientesPanel: BitacoraService en guardar() y eliminar()
- SQL: ALTER TABLE clientes ADD COLUMN IF NOT EXISTS activo BOOLEAN

Tipos de Habitacion y precio especial por habitacion:
- TipoHabitacionDAO interfaz + TipoHabitacionDAOImpl CRUD completo
- TiposHabitacionPanel: tabla de tipos + formulario TipoHabitacionFormDialog
- Habitacion.java: campo precioEspecial (Double nullable)
- Habitacion.getPrecioNoche(): usa precioEspecial si esta seteado, sino tipo.precioBase
- HabitacionDAOImpl: precio_especial en SQL_BASE, SQL_GUARDAR, SQL_ACTUALIZAR y mapear()
- HabitacionDAOImpl: getPrecioMin(), getPrecioMax(), getPrecioProm() con COALESCE
- HabitacionFormDialog: campo editable precio especial por habitacion
- HabitacionesPanel: 3 tarjetas de precio (minimo, promedio, maximo)
- MenuPrincipal: boton Tipos de Habitacion en sidebar + case tipos en navegarA()
- SQL: ALTER TABLE habitaciones ADD COLUMN IF NOT EXISTS precio_especial DECIMAL(10,2)

Email de confirmacion de reservacion:
- pom.xml: angus-mail 2.0.3 (Jakarta Mail implementation)
- email.properties: config SMTP Gmail (starttls, puerto 587, placeholder seguro)
- EmailService.java: envio asincrono en Thread separado, no bloquea UI
- Plantilla HTML con detalles de habitacion, fechas, noches y total con IVA
- Auto-detecta credenciales placeholder y deshabilita silenciosamente
- ReservacionFormDialog: EmailService en nueva reservacion + BitacoraService CREAR/EDITAR

Batch 3 - JUnit5, JFreeChart, Excel export y Bitacora:
- PasswordUtilTest, ValidacionesTest, ReservacionTest (JUnit 5 / 5.10.1)
- DashboardPanel: graficas de barras y pie con JFreeChart 1.5.4
- ExcelExporter: JTable a .xlsx con Apache POI 5.2.5
- Bitacora: modelo, DAO, panel ADMIN, integracion Login/CheckIn/Facturas

Batch 2 - Funcionalidades y calidad:
- DashboardPanel: KPIs async con SwingWorker, agenda del dia
- ReportesPanel: PDF con Apache PDFBox
- SLF4J en todos los DAOs, simplelogger.properties
- ClientesPanel refactor completo con Tema + UIHelper + TableRowSorter

Batch 1 - Seguridad y arquitectura:
- HikariCP, BCrypt, Tema.java, UIHelper.java, UsuariosPanel
- MenuPrincipal: permisos por rol, badge de rol, acceso denegado"
git push
pause
