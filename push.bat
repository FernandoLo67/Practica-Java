@echo off
cd /d "C:\Users\COMPUBYTES\Desktop\Netbeans\mavenproject2"
git add .
git commit -m "Mejoras de seguridad y arquitectura: HikariCP, BCrypt, permisos por rol y modulo de usuarios

- ConexionDB: reemplaza Singleton con pool HikariCP (max 10 conexiones, reconexion automatica)
- ConexionDB: credenciales leidas de database.properties (excluido de git)
- UsuarioDAOImpl: autenticacion BCrypt (verifica en Java, no en SQL)
- UsuarioDAOImpl: guarda passwords con hash BCrypt, migracion automatica de texto plano
- UsuarioDAO: nuevo metodo actualizarPassword(id, hash)
- Main: shutdown hook para cerrar pool HikariCP al salir
- Tema.java: constantes centralizadas de colores y fuentes
- UIHelper.java: fabrica de componentes Swing reutilizables
- PasswordUtil.java: wrapper BCrypt con retrocompatibilidad texto plano
- HotelException.java: excepcion personalizada con codigos de error
- UsuariosPanel.java: nuevo modulo de gestion de usuarios (ADMIN only)
- UsuarioFormDialog.java: dialogo crear/editar usuario con hash BCrypt automatico
- MenuPrincipal: seccion Administracion y boton Usuarios solo para ADMIN
- MenuPrincipal: badge de rol coloreado en el header
- MenuPrincipal: pantalla de acceso denegado para RECEPCIONISTA
- ReservacionFormDialog: MaskFormatter en campos de fecha (dd/mm/aaaa)"
git push
pause
