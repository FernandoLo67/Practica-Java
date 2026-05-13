@echo off
cd /d "C:\Users\COMPUBYTES\Desktop\Netbeans\mavenproject2"
git add .
git commit -m "Sistema completo: Dashboard, PDF, SLF4J, Tema.java y todos los modulos terminados

Batch 1 - Seguridad y arquitectura:
- ConexionDB: pool HikariCP (max 10), credenciales en database.properties (excluido de git)
- UsuarioDAOImpl: autenticacion BCrypt en Java, migracion automatica de texto plano
- PasswordUtil.java: wrapper BCrypt con retrocompatibilidad
- HotelException.java: excepcion personalizada con codigos de error
- Tema.java: constantes centralizadas de colores y fuentes
- UIHelper.java: fabrica de componentes Swing reutilizables
- UsuariosPanel + UsuarioFormDialog: modulo CRUD de usuarios (ADMIN only)
- MenuPrincipal: seccion Administracion, badge de rol, pantalla acceso denegado
- ReservacionFormDialog: MaskFormatter en campos de fecha (dd/mm/aaaa)
- Main: shutdown hook HikariCP

Batch 2 - Funcionalidades y calidad:
- DashboardPanel: KPIs en tiempo real con SwingWorker (async), agenda del dia
- ReportesPanel: exportar a PDF con Apache PDFBox (KPIs, reservas, top clientes)
- SLF4J en todos los DAOs: reemplaza System.err.println con log.error/debug
- simplelogger.properties: configuracion de nivel de log
- ClientesPanel: refactor completo con Tema.java y UIHelper, filtro TableRowSorter
- HabitacionesPanel, ReservacionesPanel, CheckInOutPanel, FacturasPanel: colores delegados a Tema
- README.md: reescritura completa con estructura actual, dependencias y configuracion"
git push
pause
