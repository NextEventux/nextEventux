CREATE TABLE Permiso (
    id_permiso INT PRIMARY KEY AUTO_INCREMENT,
    nombre VARCHAR(100) NOT NULL,
    descripcion VARCHAR(255)
);

CREATE TABLE Rol (
    id_rol INT PRIMARY KEY AUTO_INCREMENT,
    nombre VARCHAR(100) NOT NULL,
    descripcion VARCHAR(255),
    estado VARCHAR(30) NOT NULL
);

CREATE TABLE Usuario (
    id_usuario INT PRIMARY KEY AUTO_INCREMENT,
    nombre VARCHAR(100) NOT NULL,
    usuario VARCHAR(100) NOT NULL,
    contraseña VARCHAR(255) NOT NULL,
    estado VARCHAR(30) NOT NULL,
    fecha_registro TIMESTAMP NOT NULL,
    acepta_marketing BOOLEAN NOT NULL,
    tipo_usuario VARCHAR(50) NOT NULL
);

CREATE TABLE Rol_permiso (
    id_rol_permiso INT PRIMARY KEY AUTO_INCREMENT,
    id_rol INT NOT NULL,
    id_permiso INT NOT NULL,
    FOREIGN KEY (id_rol) REFERENCES Rol(id_rol),
    FOREIGN KEY (id_permiso) REFERENCES Permiso(id_permiso)
);

CREATE TABLE Usuario_rol (
    id_usuario_rol INT PRIMARY KEY AUTO_INCREMENT,
    id_usuario INT NOT NULL,
    id_rol INT NOT NULL,
    FOREIGN KEY (id_usuario) REFERENCES Usuario(id_usuario),
    FOREIGN KEY (id_rol) REFERENCES Rol(id_rol)
);

CREATE TABLE Lugar (
    id_lugar INT PRIMARY KEY AUTO_INCREMENT,
    nombre VARCHAR(100) NOT NULL,
    direccion VARCHAR(255) NOT NULL,
    capacidad INT NOT NULL,
    descripcion VARCHAR(255),
    estado VARCHAR(30) NOT NULL
);

CREATE TABLE Organizador (
    id_organizador INT PRIMARY KEY AUTO_INCREMENT,
    id_usuario INT NOT NULL,
    empresa VARCHAR(150),
    telefono VARCHAR(30),
    estado VARCHAR(30) NOT NULL,
    FOREIGN KEY (id_usuario) REFERENCES Usuario(id_usuario)
);

CREATE TABLE Proveedor (
    id_proveedor INT PRIMARY KEY AUTO_INCREMENT,
    id_usuario INT NOT NULL,
    nombre VARCHAR(100) NOT NULL,
    empresa VARCHAR(150),
    correo VARCHAR(150),
    telefono VARCHAR(30),
    direccion VARCHAR(255),
    estado VARCHAR(30) NOT NULL,
    FOREIGN KEY (id_usuario) REFERENCES Usuario(id_usuario)
);

CREATE TABLE Evento (
    id_evento INT PRIMARY KEY AUTO_INCREMENT,
    id_organizador INT NOT NULL,
    nombre VARCHAR(150) NOT NULL,
    descripcion VARCHAR(255),
    fecha_inicio TIMESTAMP NOT NULL,
    fecha_fin TIMESTAMP NOT NULL,
    ubicacion VARCHAR(255),
    estado VARCHAR(30) NOT NULL,
    fecha_creacion TIMESTAMP NOT NULL,
    FOREIGN KEY (id_organizador) REFERENCES Organizador(id_organizador)
);

CREATE TABLE Planificacion (
    id_planificacion INT PRIMARY KEY AUTO_INCREMENT,
    id_evento INT NOT NULL,
    version INT NOT NULL,
    descripcion VARCHAR(255),
    estado VARCHAR(30) NOT NULL,
    FOREIGN KEY (id_evento) REFERENCES Evento(id_evento)
);

CREATE TABLE Invitacion (
    id_invitacion INT PRIMARY KEY AUTO_INCREMENT,
    id_evento INT NOT NULL,
    id_lugar INT NOT NULL,
    fecha_envio TIMESTAMP NOT NULL,
    mensaje VARCHAR(500),
    estado_respuesta VARCHAR(30),
    FOREIGN KEY (id_evento) REFERENCES Evento(id_evento),
    FOREIGN KEY (id_lugar) REFERENCES Lugar(id_lugar)
);

CREATE TABLE Invitado (
    id_invitado INT PRIMARY KEY AUTO_INCREMENT,
    id_invitacion INT NOT NULL,
    nombre VARCHAR(100) NOT NULL,
    telefono VARCHAR(30),
    direccion VARCHAR(255),
    estado VARCHAR(30),
    correo VARCHAR(150),
    FOREIGN KEY (id_invitacion) REFERENCES Invitacion(id_invitacion)
);

CREATE TABLE Acompanante (
    id_acompanante INT PRIMARY KEY AUTO_INCREMENT,
    id_invitado INT NOT NULL,
    nombre VARCHAR(100) NOT NULL,
    telefono VARCHAR(30),
    tipo VARCHAR(50),
    FOREIGN KEY (id_invitado) REFERENCES Invitado(id_invitado)
);

CREATE TABLE Necesidad_Especial (
    id_necesidad INT PRIMARY KEY AUTO_INCREMENT,
    id_invitado INT NOT NULL,
    descripcion VARCHAR(255),
    estado VARCHAR(30),
    FOREIGN KEY (id_invitado) REFERENCES Invitado(id_invitado)
);

CREATE TABLE Regalo (
    id_regalo INT PRIMARY KEY AUTO_INCREMENT,
    id_invitacion INT NOT NULL,
    nombre VARCHAR(150) NOT NULL,
    descripcion VARCHAR(255),
    estado VARCHAR(30),
    precio_estimado DECIMAL(12,2),
    FOREIGN KEY (id_invitacion) REFERENCES Invitacion(id_invitacion)
);

CREATE TABLE Reserva_regalo (
    id_reserva INT PRIMARY KEY AUTO_INCREMENT,
    id_evento INT NOT NULL,
    id_invitado INT NOT NULL,
    id_regalo INT NOT NULL,
    fecha_reserva TIMESTAMP NOT NULL,
    estado VARCHAR(30),
    FOREIGN KEY (id_evento) REFERENCES Evento(id_evento),
    FOREIGN KEY (id_invitado) REFERENCES Invitado(id_invitado),
    FOREIGN KEY (id_regalo) REFERENCES Regalo(id_regalo)
);

CREATE TABLE Servicio (
    id_servicio INT PRIMARY KEY AUTO_INCREMENT,
    id_proveedor INT NOT NULL,
    nombre VARCHAR(150) NOT NULL,
    descripcion VARCHAR(255),
    precio DECIMAL(12,2),
    categoria VARCHAR(100),
    estado VARCHAR(30),
    FOREIGN KEY (id_proveedor) REFERENCES Proveedor(id_proveedor)
);

CREATE TABLE Paquete_servicio (
    id_paquete INT PRIMARY KEY AUTO_INCREMENT,
    id_proveedor INT NOT NULL,
    nombre VARCHAR(150) NOT NULL,
    descripcion VARCHAR(255),
    precio DECIMAL(12,2),
    categoria VARCHAR(100),
    estado VARCHAR(30),
    FOREIGN KEY (id_proveedor) REFERENCES Proveedor(id_proveedor)
);

CREATE TABLE Solicitud_Ejecucion (
    id_solicitud INT PRIMARY KEY AUTO_INCREMENT,
    id_proveedor INT NOT NULL,
    id_evento INT NOT NULL,
    fecha_solicitud TIMESTAMP NOT NULL,
    estado VARCHAR(30),
    FOREIGN KEY (id_proveedor) REFERENCES Proveedor(id_proveedor),
    FOREIGN KEY (id_evento) REFERENCES Evento(id_evento)
);

CREATE TABLE Propuesta (
    id_propuesta INT PRIMARY KEY AUTO_INCREMENT,
    id_solicitud INT NOT NULL,
    valor DECIMAL(12,2),
    descripcion VARCHAR(255),
    estado VARCHAR(30),
    FOREIGN KEY (id_solicitud) REFERENCES Solicitud_Ejecucion(id_solicitud)
);

CREATE TABLE Contratacion (
    id_contratacion INT PRIMARY KEY AUTO_INCREMENT,
    id_propuesta INT NOT NULL,
    id_evento INT NOT NULL,
    fecha_contratacion TIMESTAMP NOT NULL,
    estado VARCHAR(30),
    condiciones VARCHAR(500),
    valor_acordado DECIMAL(12,2),
    FOREIGN KEY (id_propuesta) REFERENCES Propuesta(id_propuesta),
    FOREIGN KEY (id_evento) REFERENCES Evento(id_evento)
);

CREATE TABLE Movimiento_financiero (
    id_movimiento INT PRIMARY KEY AUTO_INCREMENT,
    id_propuesta INT NOT NULL,
    id_evento INT NOT NULL,
    id_contratacion INT NOT NULL,
    valor DECIMAL(12,2),
    fecha TIMESTAMP NOT NULL,
    descripcion VARCHAR(255),
    tipo VARCHAR(50),
    estado VARCHAR(30),
    FOREIGN KEY (id_propuesta) REFERENCES Propuesta(id_propuesta),
    FOREIGN KEY (id_evento) REFERENCES Evento(id_evento),
    FOREIGN KEY (id_contratacion) REFERENCES Contratacion(id_contratacion)
);

CREATE TABLE Resena (
    id_resena INT PRIMARY KEY AUTO_INCREMENT,
    id_contratacion INT NOT NULL,
    calificacion INT,
    comentario VARCHAR(500),
    fecha TIMESTAMP NOT NULL,
    FOREIGN KEY (id_contratacion) REFERENCES Contratacion(id_contratacion)
);

CREATE TABLE Conversacion (
    id_conversacion INT PRIMARY KEY AUTO_INCREMENT,
    id_solicitud INT NOT NULL,
    fecha_creacion TIMESTAMP NOT NULL,
    estado VARCHAR(30),
    FOREIGN KEY (id_solicitud) REFERENCES Solicitud_Ejecucion(id_solicitud)
);

CREATE TABLE Mensaje (
    id_mensaje INT PRIMARY KEY AUTO_INCREMENT,
    id_conversacion INT NOT NULL,
    id_usuario_remitente INT NOT NULL,
    contenido VARCHAR(1000) NOT NULL,
    fecha_hora TIMESTAMP NOT NULL,
    FOREIGN KEY (id_conversacion) REFERENCES Conversacion(id_conversacion),
    FOREIGN KEY (id_usuario_remitente) REFERENCES Usuario(id_usuario)
);

CREATE TABLE Respuesta_resena (
    id_respuesta INT PRIMARY KEY AUTO_INCREMENT,
    id_resena INT NOT NULL,
    id_proveedor INT NOT NULL,
    comentario VARCHAR(500),
    fecha TIMESTAMP NOT NULL,
    FOREIGN KEY (id_resena) REFERENCES Resena(id_resena),
    FOREIGN KEY (id_proveedor) REFERENCES Proveedor(id_proveedor)
);

CREATE TABLE Sancion (
    id_sancion INT PRIMARY KEY AUTO_INCREMENT,
    id_evento INT NOT NULL,
    motivo VARCHAR(255),
    valor DECIMAL(12,2),
    estado VARCHAR(30),
    fecha TIMESTAMP NOT NULL,
    FOREIGN KEY (id_evento) REFERENCES Evento(id_evento)
);

CREATE TABLE Segmentacion (
    id_segmento INT PRIMARY KEY AUTO_INCREMENT,
    id_usuario INT NOT NULL,
    nombre VARCHAR(150) NOT NULL,
    criterios VARCHAR(500),
    descripcion VARCHAR(255),
    estados VARCHAR(30),
    FOREIGN KEY (id_usuario) REFERENCES Usuario(id_usuario)
);

CREATE TABLE Campana (
    id_campana INT PRIMARY KEY AUTO_INCREMENT,
    id_proveedor INT NOT NULL,
    nombre VARCHAR(150) NOT NULL,
    descripcion VARCHAR(255),
    fecha_inicio TIMESTAMP,
    fecha_fin TIMESTAMP,
    FOREIGN KEY (id_proveedor) REFERENCES Proveedor(id_proveedor)
);

CREATE TABLE Envio_Campana (
    id_envio INT PRIMARY KEY AUTO_INCREMENT,
    id_campana INT NOT NULL,
    id_usuario INT NOT NULL,
    fecha_solicitud TIMESTAMP NOT NULL,
    estado VARCHAR(30),
    FOREIGN KEY (id_campana) REFERENCES Campana(id_campana),
    FOREIGN KEY (id_usuario) REFERENCES Usuario(id_usuario)
);

CREATE TABLE Puntos_fidelidad (
    id_puntos INT PRIMARY KEY AUTO_INCREMENT,
    id_usuario INT NOT NULL,
    cantidad INT NOT NULL,
    fecha_actualizacion TIMESTAMP NOT NULL,
    FOREIGN KEY (id_usuario) REFERENCES Usuario(id_usuario)
);

-- =========================================================
-- DATOS DE PRUEBA
-- =========================================================

-- ---------------------------------------------------------
-- PERMISOS
-- ---------------------------------------------------------

INSERT INTO Permiso (id_permiso, nombre, descripcion) VALUES
(1, 'CREAR_EVENTO', 'Permite crear eventos'),
(2, 'GESTIONAR_SERVICIOS', 'Permite gestionar servicios'),
(3, 'GESTIONAR_INVITACIONES', 'Permite gestionar invitaciones'),
(4, 'APROBAR_PROVEEDORES', 'Permite aprobar proveedores');


-- ---------------------------------------------------------
-- ROLES
-- ---------------------------------------------------------

INSERT INTO Rol (id_rol, nombre, descripcion, estado) VALUES
(1, 'ADMINISTRADOR', 'Administrador general del sistema', 'ACTIVO'),
(2, 'ORGANIZADOR', 'Usuario encargado de gestionar eventos', 'ACTIVO'),
(3, 'PROVEEDOR', 'Usuario que ofrece servicios para eventos', 'ACTIVO'),
(4, 'CLIENTE', 'Usuario que solicita y participa en la gestión de eventos', 'ACTIVO');


-- ---------------------------------------------------------
-- USUARIOS
-- ---------------------------------------------------------

INSERT INTO Usuario
(id_usuario, nombre, usuario, contraseña, estado, fecha_registro, acepta_marketing, tipo_usuario)
VALUES
(1, 'Laura Gómez', 'laura.gomez', 'clave123', 'ACTIVO', TIMESTAMP '2026-10-01 09:00:00', TRUE, 'ORGANIZADOR'),
(2, 'Carlos Rodríguez', 'carlos.rodriguez', 'clave456', 'ACTIVO', TIMESTAMP '2026-10-02 10:30:00', TRUE, 'ORGANIZADOR'),
(3, 'Decoraciones Arcoíris', 'decoraciones.arcoiris', 'clave789', 'ACTIVO', TIMESTAMP '2026-10-03 11:00:00', TRUE, 'PROVEEDOR'),
(4, 'Sabores & Eventos', 'sabores.eventos', 'clave321', 'ACTIVO', TIMESTAMP '2026-10-03 14:00:00', FALSE, 'PROVEEDOR'),
(5, 'Ana Martínez', 'ana.martinez', 'clave654', 'ACTIVO', TIMESTAMP '2026-10-04 08:30:00', TRUE, 'CLIENTE'),
(6, 'Diego Pérez', 'diego.perez', 'clave987', 'ACTIVO', TIMESTAMP '2026-10-05 16:00:00', FALSE, 'CLIENTE');


-- ---------------------------------------------------------
-- ROLES Y PERMISOS
-- ---------------------------------------------------------

INSERT INTO Rol_permiso
(id_rol_permiso, id_rol, id_permiso)
VALUES
(1, 1, 1),
(2, 1, 2),
(3, 1, 3),
(4, 1, 4),
(5, 2, 1),
(6, 2, 3);


-- ---------------------------------------------------------
-- USUARIOS Y ROLES
-- ---------------------------------------------------------

INSERT INTO Usuario_rol
(id_usuario_rol, id_usuario, id_rol)
VALUES
(1, 1, 2),
(2, 2, 2),
(3, 3, 3),
(4, 4, 3),
(5, 5, 4),
(6, 6, 4);


-- ---------------------------------------------------------
-- LUGARES
-- ---------------------------------------------------------

INSERT INTO Lugar
(id_lugar, nombre, direccion, capacidad, descripcion, estado)
VALUES
(1, 'Salón Primavera', 'Calle 100 #15-20', 150, 'Salón para celebraciones sociales', 'DISPONIBLE'),
(2, 'Jardín Los Pinos', 'Carrera 7 #120-15', 250, 'Espacio al aire libre para eventos', 'DISPONIBLE'),
(3, 'Salón Mirador', 'Calle 80 #10-45', 80, 'Salón para eventos pequeños', 'DISPONIBLE');


-- ---------------------------------------------------------
-- ORGANIZADORES
-- ---------------------------------------------------------

INSERT INTO Organizador
(id_organizador, id_usuario, empresa, telefono, estado)
VALUES
(1, 1, 'Eventos Laura', '3001234567', 'ACTIVO'),
(2, 2, 'Celebraciones CR', '3017654321', 'ACTIVO');


-- ---------------------------------------------------------
-- PROVEEDORES
-- ---------------------------------------------------------

INSERT INTO Proveedor
(id_proveedor, id_usuario, nombre, empresa, correo, telefono, direccion, estado)
VALUES
(1, 3, 'Mariana López', 'Decoraciones Arcoíris', 'contacto@arcoiris.com', '3101234567', 'Carrera 15 #80-20', 'APROBADO'),
(2, 4, 'Andrés Torres', 'Sabores & Eventos', 'contacto@saboreseventos.com', '3117654321', 'Calle 90 #20-30', 'APROBADO');


-- ---------------------------------------------------------
-- EVENTOS
-- ---------------------------------------------------------

INSERT INTO Evento
(id_evento, id_organizador, nombre, descripcion, fecha_inicio, fecha_fin, ubicacion, estado, fecha_creacion)
VALUES
(1, 1, 'Boda Laura y Daniel', 'Celebración de matrimonio', TIMESTAMP '2026-11-15 15:00:00', TIMESTAMP '2026-11-15 23:00:00', 'Salón Primavera', 'PLANEADO', TIMESTAMP '2026-10-06 09:00:00'),
(2, 2, 'Baby Shower Valentina', 'Celebración de bienvenida para bebé', TIMESTAMP '2026-12-05 14:00:00', TIMESTAMP '2026-12-05 19:00:00', 'Jardín Los Pinos', 'PLANEADO', TIMESTAMP '2026-10-07 10:00:00');


-- ---------------------------------------------------------
-- PLANIFICACIONES
-- ---------------------------------------------------------

INSERT INTO Planificacion
(id_planificacion, id_evento, version, descripcion, estado)
VALUES
(1, 1, 1, 'Primera propuesta de planificación de la boda', 'BORRADOR'),
(2, 1, 2, 'Segunda propuesta con ajustes de decoración', 'ACTIVA'),
(3, 2, 1, 'Planificación inicial del baby shower', 'ACTIVA');


-- ---------------------------------------------------------
-- INVITACIONES
-- ---------------------------------------------------------

INSERT INTO Invitacion
(id_invitacion, id_evento, id_lugar, fecha_envio, mensaje, estado_respuesta)
VALUES
(1, 1, 1, TIMESTAMP '2026-10-20 10:00:00', 'Te invitamos a celebrar con nosotros este día especial.', 'ENVIADA'),
(2, 1, 1, TIMESTAMP '2026-10-20 10:15:00', 'Esperamos contar contigo en nuestra boda.', 'ENVIADA'),
(3, 2, 2, TIMESTAMP '2026-11-01 09:00:00', 'Acompáñanos en el baby shower de Valentina.', 'ENVIADA');


-- ---------------------------------------------------------
-- INVITADOS
-- ---------------------------------------------------------

INSERT INTO Invitado
(id_invitado, id_invitacion, nombre, telefono, direccion, estado, correo)
VALUES
(1, 1, 'Sofía Cortés', '3005551111', 'Calle 50 #10-20', 'CONFIRMADO', 'sofia@email.com'),
(2, 2, 'Juan García', '3005552222', 'Carrera 20 #30-40', 'PENDIENTE', 'juan@email.com'),
(3, 3, 'Camila Torres', '3005553333', 'Calle 70 #15-25', 'CONFIRMADO', 'camila@email.com'),
(4, 3, 'Natalia Ruiz', '3005554444', 'Carrera 12 #45-60', 'PENDIENTE', 'natalia@email.com');


-- ---------------------------------------------------------
-- ACOMPAÑANTES
-- ---------------------------------------------------------

INSERT INTO Acompanante
(id_acompanante, id_invitado, nombre, telefono, tipo)
VALUES
(1, 1, 'Daniel Cortés', '3006661111', 'PAREJA'),
(2, 3, 'Mateo Torres', '3006662222', 'PAREJA'),
(3, 4, 'Laura Ruiz', '3006663333', 'FAMILIAR');


-- ---------------------------------------------------------
-- NECESIDADES ESPECIALES
-- ---------------------------------------------------------

INSERT INTO Necesidad_Especial
(id_necesidad, id_invitado, descripcion, estado)
VALUES
(1, 3, 'Espacio accesible para silla de ruedas', 'REGISTRADA'),
(2, 4, 'Preferencia por menú sin frutos secos', 'REGISTRADA');


-- ---------------------------------------------------------
-- REGALOS
-- ---------------------------------------------------------

INSERT INTO Regalo
(id_regalo, id_invitacion, nombre, descripcion, estado, precio_estimado)
VALUES
(1, 3, 'Pañitos para bebé', 'Paquete de pañitos húmedos', 'DISPONIBLE', 25000.00),
(2, 3, 'Kit de biberones', 'Kit de biberones para recién nacido', 'DISPONIBLE', 80000.00),
(3, 3, 'Cobija para bebé', 'Cobija suave para recién nacido', 'DISPONIBLE', 60000.00),
(4, 3, 'Ropa de bebé', 'Conjunto de ropa para recién nacido', 'DISPONIBLE', 50000.00);


-- ---------------------------------------------------------
-- RESERVAS DE REGALOS
-- ---------------------------------------------------------

INSERT INTO Reserva_regalo
(id_reserva, id_evento, id_invitado, id_regalo, fecha_reserva, estado)
VALUES
(1, 2, 3, 1, TIMESTAMP '2026-11-10 12:00:00', 'RESERVADO'),
(2, 2, 4, 1, TIMESTAMP '2026-11-11 15:30:00', 'RESERVADO'),
(3, 2, 3, 2, TIMESTAMP '2026-11-12 09:00:00', 'RESERVADO');


-- ---------------------------------------------------------
-- SERVICIOS
-- ---------------------------------------------------------

INSERT INTO Servicio
(id_servicio, id_proveedor, nombre, descripcion, precio, categoria, estado)
VALUES
(1, 1, 'Decoración floral', 'Decoración floral para eventos sociales', 1200000.00, 'DECORACION', 'DISPONIBLE'),
(2, 1, 'Decoración temática', 'Decoración personalizada según temática', 900000.00, 'DECORACION', 'DISPONIBLE'),
(3, 2, 'Catering básico', 'Servicio de alimentación para eventos', 1800000.00, 'ALIMENTACION', 'DISPONIBLE'),
(4, 2, 'Mesa de postres', 'Postres variados para celebraciones', 650000.00, 'ALIMENTACION', 'DISPONIBLE');


-- ---------------------------------------------------------
-- PAQUETES DE SERVICIO
-- ---------------------------------------------------------

INSERT INTO Paquete_servicio
(id_paquete, id_proveedor, nombre, descripcion, precio, categoria, estado)
VALUES
(1, 1, 'Paquete Boda Clásica', 'Decoración floral y decoración de mesas', 1800000.00, 'DECORACION', 'DISPONIBLE'),
(2, 2, 'Paquete Celebración', 'Catering y mesa de postres', 2200000.00, 'ALIMENTACION', 'DISPONIBLE');


-- ---------------------------------------------------------
-- SOLICITUDES DE EJECUCIÓN
-- ---------------------------------------------------------

INSERT INTO Solicitud_Ejecucion
(id_solicitud, id_proveedor, id_evento, fecha_solicitud, estado)
VALUES
(1, 1, 1, TIMESTAMP '2026-10-08 10:00:00', 'RESPONDIDA'),
(2, 2, 1, TIMESTAMP '2026-10-08 11:30:00', 'RESPONDIDA'),
(3, 1, 2, TIMESTAMP '2026-10-09 09:00:00', 'PENDIENTE');


-- ---------------------------------------------------------
-- PROPUESTAS
-- ---------------------------------------------------------

INSERT INTO Propuesta
(id_propuesta, id_solicitud, valor, descripcion, estado)
VALUES
(1, 1, 1800000.00, 'Decoración completa para la boda', 'ACEPTADA'),
(2, 2, 2200000.00, 'Servicio de catering y postres', 'ACEPTADA'),
(3, 3, 900000.00, 'Decoración temática para baby shower', 'PENDIENTE');


-- ---------------------------------------------------------
-- CONTRATACIONES
-- ---------------------------------------------------------

INSERT INTO Contratacion
(id_contratacion, id_propuesta, id_evento, fecha_contratacion, estado, condiciones, valor_acordado)
VALUES
(1, 1, 1, TIMESTAMP '2026-10-10 14:00:00', 'ACTIVA', 'Montaje terminado tres horas antes del evento', 1800000.00),
(2, 2, 1, TIMESTAMP '2026-10-10 15:00:00', 'ACTIVA', 'Servicio de alimentación para 120 personas', 2200000.00);


-- ---------------------------------------------------------
-- MOVIMIENTOS FINANCIEROS
-- ---------------------------------------------------------

INSERT INTO Movimiento_financiero
(id_movimiento, id_propuesta, id_evento, id_contratacion, valor, fecha, descripcion, tipo, estado)
VALUES
(1, 1, 1, 1, 900000.00, TIMESTAMP '2026-10-11 09:00:00', 'Anticipo de decoración', 'INGRESO', 'REGISTRADO'),
(2, 1, 1, 1, 900000.00, TIMESTAMP '2026-11-15 23:30:00', 'Pago final de decoración', 'INGRESO', 'PENDIENTE'),
(3, 2, 1, 2, 1100000.00, TIMESTAMP '2026-10-12 10:00:00', 'Anticipo de catering', 'INGRESO', 'REGISTRADO'),
(4, 2, 1, 2, 1100000.00, TIMESTAMP '2026-11-15 23:30:00', 'Pago final de catering', 'INGRESO', 'PENDIENTE');


-- ---------------------------------------------------------
-- RESEÑAS
-- ---------------------------------------------------------

INSERT INTO Resena
(id_resena, id_contratacion, calificacion, comentario, fecha)
VALUES
(1, 1, 5, 'La decoración quedó exactamente como la esperábamos.', TIMESTAMP '2026-11-16 10:00:00'),
(2, 2, 4, 'El servicio de catering fue muy bueno.', TIMESTAMP '2026-11-16 11:00:00');


-- ---------------------------------------------------------
-- CONVERSACIONES
-- ---------------------------------------------------------

INSERT INTO Conversacion
(id_conversacion, id_solicitud, fecha_creacion, estado)
VALUES
(1, 1, TIMESTAMP '2026-10-08 10:30:00', 'ACTIVA'),
(2, 2, TIMESTAMP '2026-10-08 12:00:00', 'ACTIVA');


-- ---------------------------------------------------------
-- MENSAJES
-- ---------------------------------------------------------

INSERT INTO Mensaje
(id_mensaje, id_conversacion, id_usuario_remitente, contenido, fecha_hora)
VALUES
(1, 1, 1, 'Hola, quisiera confirmar si pueden incluir flores blancas.', TIMESTAMP '2026-10-08 10:35:00'),
(2, 1, 3, 'Sí, podemos incluir flores blancas sin problema.', TIMESTAMP '2026-10-08 10:40:00'),
(3, 2, 1, '¿El servicio de catering incluye bebidas?', TIMESTAMP '2026-10-08 12:10:00'),
(4, 2, 4, 'Sí, incluye bebidas no alcohólicas.', TIMESTAMP '2026-10-08 12:15:00');


-- ---------------------------------------------------------
-- RESPUESTAS A RESEÑAS
-- ---------------------------------------------------------

INSERT INTO Respuesta_resena
(id_respuesta, id_resena, id_proveedor, comentario, fecha)
VALUES
(1, 1, 1, 'Muchas gracias por confiar en nuestro trabajo.', TIMESTAMP '2026-11-16 12:00:00'),
(2, 2, 2, 'Nos alegra saber que disfrutaron el servicio.', TIMESTAMP '2026-11-16 13:00:00');


-- ---------------------------------------------------------
-- SANCIONES
-- ---------------------------------------------------------

INSERT INTO Sancion
(id_sancion, id_evento, motivo, valor, estado, fecha)
VALUES
(1, 1, 'Incumplimiento de una condición del evento', 100000.00, 'PENDIENTE', TIMESTAMP '2026-11-16 14:00:00'),
(2, 2, 'Cancelación tardía de un servicio', 75000.00, 'PAGADA', TIMESTAMP '2026-11-16 15:00:00');


-- ---------------------------------------------------------
-- SEGMENTACIONES
-- ---------------------------------------------------------

INSERT INTO Segmentacion
(id_segmento, id_usuario, nombre, criterios, descripcion, estados)
VALUES
(1, 1, 'Clientes frecuentes', 'Más de 3 eventos registrados', 'Clientes con alta frecuencia de uso', 'ACTIVA'),
(2, 1, 'Eventos familiares', 'Tipo de evento familiar', 'Clientes interesados en eventos familiares', 'ACTIVA'),
(3, 2, 'Clientes nuevos', 'Primer evento registrado', 'Clientes que utilizan el servicio por primera vez', 'ACTIVA');


-- ---------------------------------------------------------
-- CAMPAÑAS
-- ---------------------------------------------------------

INSERT INTO Campana
(id_campana, id_proveedor, nombre, descripcion, fecha_inicio, fecha_fin)
VALUES
(1, 1, 'Decoración de fin de año', 'Promoción de servicios de decoración', TIMESTAMP '2026-11-01 00:00:00', TIMESTAMP '2026-12-31 23:59:59'),
(2, 2, 'Menús para celebraciones', 'Promoción de paquetes de alimentación', TIMESTAMP '2026-10-15 00:00:00', TIMESTAMP '2026-12-15 23:59:59');


-- ---------------------------------------------------------
-- ENVÍOS DE CAMPAÑA
-- ---------------------------------------------------------

INSERT INTO Envio_Campana
(id_envio, id_campana, id_usuario, fecha_solicitud, estado)
VALUES
(1, 1, 5, TIMESTAMP '2026-11-02 09:00:00', 'ENVIADO'),
(2, 1, 6, TIMESTAMP '2026-11-02 09:05:00', 'ENVIADO'),
(3, 2, 5, TIMESTAMP '2026-10-16 10:00:00', 'ENVIADO'),
(4, 2, 6, TIMESTAMP '2026-10-16 10:05:00', 'PENDIENTE');


-- ---------------------------------------------------------
-- PUNTOS DE FIDELIDAD
-- ---------------------------------------------------------

INSERT INTO Puntos_fidelidad
(id_puntos, id_usuario, cantidad, fecha_actualizacion)
VALUES
(1, 5, 350, TIMESTAMP '2026-10-10 18:00:00'),
(2, 6, 120, TIMESTAMP '2026-10-10 18:00:00');