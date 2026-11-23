-- Secuencia completa para inicializar o reconstruir NextEventux.

-- En el arranque normal solo se ejecuta cuando la base PUBLIC no tiene tablas.

DROP TABLE IF EXISTS Presupuesto_marketing CASCADE;
DROP TABLE IF EXISTS Segmento_cliente CASCADE;
DROP TABLE IF EXISTS Busqueda_evento CASCADE;
DROP TABLE IF EXISTS Resena_Organizador CASCADE;
DROP TABLE IF EXISTS Solicitud_Contratacion_Organizador CASCADE;
DROP TABLE IF EXISTS Entrega_galeria CASCADE;
DROP TABLE IF EXISTS Fotografia_evento CASCADE;
DROP TABLE IF EXISTS Finalizacion_evento CASCADE;
DROP TABLE IF EXISTS Notificacion_interna CASCADE;
DROP TABLE IF EXISTS Necesidad_Servicio_Evento CASCADE;
DROP TABLE IF EXISTS Pregunta_Servicio CASCADE;
DROP TABLE IF EXISTS Imagen_Servicio CASCADE;
DROP TABLE IF EXISTS Plantilla_evento CASCADE;
DROP TABLE IF EXISTS Solicitud_publicacion CASCADE;
DROP TABLE IF EXISTS Disponibilidad_Servicio CASCADE;
DROP TABLE IF EXISTS Puntos_fidelidad CASCADE;
DROP TABLE IF EXISTS Envio_Campana CASCADE;
DROP TABLE IF EXISTS Campana CASCADE;
DROP TABLE IF EXISTS Segmentacion CASCADE;
DROP TABLE IF EXISTS Sancion CASCADE;
DROP TABLE IF EXISTS Respuesta_resena CASCADE;
DROP TABLE IF EXISTS Mensaje CASCADE;
DROP TABLE IF EXISTS Conversacion CASCADE;
DROP TABLE IF EXISTS Resena CASCADE;
DROP TABLE IF EXISTS Movimiento_financiero CASCADE;
DROP TABLE IF EXISTS Contratacion CASCADE;
DROP TABLE IF EXISTS Propuesta CASCADE;
DROP TABLE IF EXISTS Solicitud_Ejecucion CASCADE;
DROP TABLE IF EXISTS Paquete_servicio CASCADE;
DROP TABLE IF EXISTS Servicio CASCADE;
DROP TABLE IF EXISTS Reserva_regalo CASCADE;
DROP TABLE IF EXISTS Regalo CASCADE;
DROP TABLE IF EXISTS Necesidad_Especial CASCADE;
DROP TABLE IF EXISTS Acompanante CASCADE;
DROP TABLE IF EXISTS Invitado CASCADE;
DROP TABLE IF EXISTS Invitacion CASCADE;
DROP TABLE IF EXISTS Planificacion CASCADE;
DROP TABLE IF EXISTS Evento CASCADE;
DROP TABLE IF EXISTS Proveedor CASCADE;
DROP TABLE IF EXISTS Organizador CASCADE;
DROP TABLE IF EXISTS Lugar CASCADE;
DROP TABLE IF EXISTS Usuario_rol CASCADE;
DROP TABLE IF EXISTS Rol_permiso CASCADE;
DROP TABLE IF EXISTS Usuario CASCADE;
DROP TABLE IF EXISTS Rol CASCADE;
DROP TABLE IF EXISTS Permiso CASCADE;

DROP TABLE IF EXISTS Registro_demo_catalogo CASCADE;
DROP TABLE IF EXISTS Esquema_inicializado CASCADE;
DROP TABLE IF EXISTS Esquema_version CASCADE;


CREATE TABLE IF NOT EXISTS Permiso (
    id_permiso INT PRIMARY KEY AUTO_INCREMENT,
    nombre VARCHAR(100) NOT NULL,
    descripcion VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS Rol (
    id_rol INT PRIMARY KEY AUTO_INCREMENT,
    nombre VARCHAR(100) NOT NULL,
    descripcion VARCHAR(255),
    estado VARCHAR(30) NOT NULL
);

CREATE TABLE IF NOT EXISTS Usuario (
    id_usuario INT PRIMARY KEY AUTO_INCREMENT,
    nombre VARCHAR(100) NOT NULL,
    usuario VARCHAR(100) NOT NULL,
    contraseña VARCHAR(255) NOT NULL,
    estado VARCHAR(30) NOT NULL,
    fecha_registro TIMESTAMP NOT NULL,
    acepta_marketing BOOLEAN NOT NULL,
    tipo_usuario VARCHAR(50) NOT NULL
);

CREATE TABLE IF NOT EXISTS Rol_permiso (
    id_rol_permiso INT PRIMARY KEY AUTO_INCREMENT,
    id_rol INT NOT NULL,
    id_permiso INT NOT NULL,
    FOREIGN KEY (id_rol) REFERENCES Rol(id_rol),
    FOREIGN KEY (id_permiso) REFERENCES Permiso(id_permiso)
);

CREATE TABLE IF NOT EXISTS Usuario_rol (
    id_usuario_rol INT PRIMARY KEY AUTO_INCREMENT,
    id_usuario INT NOT NULL,
    id_rol INT NOT NULL,
    FOREIGN KEY (id_usuario) REFERENCES Usuario(id_usuario),
    FOREIGN KEY (id_rol) REFERENCES Rol(id_rol)
);

CREATE TABLE IF NOT EXISTS Lugar (
    id_lugar INT PRIMARY KEY AUTO_INCREMENT,
    nombre VARCHAR(100) NOT NULL,
    direccion VARCHAR(255) NOT NULL,
    capacidad INT NOT NULL,
    descripcion VARCHAR(255),
    estado VARCHAR(30) NOT NULL
);

CREATE TABLE IF NOT EXISTS Organizador (
    id_organizador INT PRIMARY KEY AUTO_INCREMENT,
    id_usuario INT NOT NULL,
    empresa VARCHAR(150),
    telefono VARCHAR(30),
    estado VARCHAR(30) NOT NULL,
    FOREIGN KEY (id_usuario) REFERENCES Usuario(id_usuario)
);

CREATE TABLE IF NOT EXISTS Proveedor (
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

CREATE TABLE IF NOT EXISTS Evento (
    id_evento INT PRIMARY KEY AUTO_INCREMENT,
    id_organizador INT NOT NULL,
    nombre VARCHAR(150) NOT NULL,
    tipo VARCHAR(100),
    presupuesto DECIMAL(12,2) CHECK (presupuesto >= 0),
    invitados_iniciales INT CHECK (invitados_iniciales >= 0),
    descripcion VARCHAR(255),
    fecha_inicio TIMESTAMP NOT NULL,
    fecha_fin TIMESTAMP NOT NULL,
    ubicacion VARCHAR(255),
    estado VARCHAR(30) NOT NULL,
    fecha_creacion TIMESTAMP NOT NULL,
    FOREIGN KEY (id_organizador) REFERENCES Organizador(id_organizador)
);

CREATE TABLE IF NOT EXISTS Planificacion (
    id_planificacion INT PRIMARY KEY AUTO_INCREMENT,
    id_evento INT NOT NULL,
    version INT NOT NULL,
    descripcion VARCHAR(255),
    estado VARCHAR(30) NOT NULL,
    FOREIGN KEY (id_evento) REFERENCES Evento(id_evento)
);

CREATE TABLE IF NOT EXISTS Invitacion (
    id_invitacion INT PRIMARY KEY AUTO_INCREMENT,
    id_evento INT NOT NULL,
    id_lugar INT NOT NULL,
    fecha_envio TIMESTAMP NOT NULL,
    mensaje VARCHAR(500),
    estado_respuesta VARCHAR(30),
    FOREIGN KEY (id_evento) REFERENCES Evento(id_evento),
    FOREIGN KEY (id_lugar) REFERENCES Lugar(id_lugar)
);

CREATE TABLE IF NOT EXISTS Invitado (
    id_invitado INT PRIMARY KEY AUTO_INCREMENT,
    id_invitacion INT NOT NULL,
    nombre VARCHAR(100) NOT NULL,
    telefono VARCHAR(30),
    direccion VARCHAR(255),
    estado VARCHAR(30),
    correo VARCHAR(150),
    FOREIGN KEY (id_invitacion) REFERENCES Invitacion(id_invitacion)
);

CREATE TABLE IF NOT EXISTS Acompanante (
    id_acompanante INT PRIMARY KEY AUTO_INCREMENT,
    id_invitado INT NOT NULL,
    nombre VARCHAR(100) NOT NULL,
    telefono VARCHAR(30),
    tipo VARCHAR(50),
    FOREIGN KEY (id_invitado) REFERENCES Invitado(id_invitado)
);

CREATE TABLE IF NOT EXISTS Necesidad_Especial (
    id_necesidad INT PRIMARY KEY AUTO_INCREMENT,
    id_invitado INT NOT NULL,
    descripcion VARCHAR(255),
    estado VARCHAR(30),
    FOREIGN KEY (id_invitado) REFERENCES Invitado(id_invitado)
);

CREATE TABLE IF NOT EXISTS Regalo (
    id_regalo INT PRIMARY KEY AUTO_INCREMENT,
    id_invitacion INT NOT NULL,
    nombre VARCHAR(150) NOT NULL,
    descripcion VARCHAR(255),
    estado VARCHAR(30),
    precio_estimado DECIMAL(12,2),
    FOREIGN KEY (id_invitacion) REFERENCES Invitacion(id_invitacion)
);

CREATE TABLE IF NOT EXISTS Reserva_regalo (
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

CREATE TABLE IF NOT EXISTS Servicio (
    id_servicio INT PRIMARY KEY AUTO_INCREMENT,
    id_proveedor INT NOT NULL,
    nombre VARCHAR(150) NOT NULL,
    descripcion VARCHAR(255),
    precio DECIMAL(12,2),
    categoria VARCHAR(100),
    estado VARCHAR(30),
    FOREIGN KEY (id_proveedor) REFERENCES Proveedor(id_proveedor)
);

CREATE TABLE IF NOT EXISTS Paquete_servicio (
    id_paquete INT PRIMARY KEY AUTO_INCREMENT,
    id_proveedor INT NOT NULL,
    nombre VARCHAR(150) NOT NULL,
    descripcion VARCHAR(255),
    precio DECIMAL(12,2),
    categoria VARCHAR(100),
    estado VARCHAR(30),
    FOREIGN KEY (id_proveedor) REFERENCES Proveedor(id_proveedor)
);

CREATE TABLE IF NOT EXISTS Solicitud_Ejecucion (
    id_solicitud INT PRIMARY KEY AUTO_INCREMENT,
    id_proveedor INT NOT NULL,
    id_evento INT NOT NULL,
    fecha_solicitud TIMESTAMP NOT NULL,
    estado VARCHAR(30),
    FOREIGN KEY (id_proveedor) REFERENCES Proveedor(id_proveedor),
    FOREIGN KEY (id_evento) REFERENCES Evento(id_evento)
);

CREATE TABLE IF NOT EXISTS Propuesta (
    id_propuesta INT PRIMARY KEY AUTO_INCREMENT,
    id_solicitud INT NOT NULL,
    valor DECIMAL(12,2),
    descripcion VARCHAR(255),
    estado VARCHAR(30),
    FOREIGN KEY (id_solicitud) REFERENCES Solicitud_Ejecucion(id_solicitud)
);

CREATE TABLE IF NOT EXISTS Contratacion (
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

CREATE TABLE IF NOT EXISTS Movimiento_financiero (
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

CREATE TABLE IF NOT EXISTS Resena (
    id_resena INT PRIMARY KEY AUTO_INCREMENT,
    id_contratacion INT NOT NULL,
    calificacion INT,
    comentario VARCHAR(500),
    fecha TIMESTAMP NOT NULL,
    FOREIGN KEY (id_contratacion) REFERENCES Contratacion(id_contratacion)
);

CREATE TABLE IF NOT EXISTS Conversacion (
    id_conversacion INT PRIMARY KEY AUTO_INCREMENT,
    id_solicitud INT NOT NULL,
    fecha_creacion TIMESTAMP NOT NULL,
    estado VARCHAR(30),
    FOREIGN KEY (id_solicitud) REFERENCES Solicitud_Ejecucion(id_solicitud)
);

CREATE TABLE IF NOT EXISTS Mensaje (
    id_mensaje INT PRIMARY KEY AUTO_INCREMENT,
    id_conversacion INT NOT NULL,
    id_usuario_remitente INT NOT NULL,
    contenido VARCHAR(1000) NOT NULL,
    fecha_hora TIMESTAMP NOT NULL,
    FOREIGN KEY (id_conversacion) REFERENCES Conversacion(id_conversacion),
    FOREIGN KEY (id_usuario_remitente) REFERENCES Usuario(id_usuario)
);

CREATE TABLE IF NOT EXISTS Respuesta_resena (
    id_respuesta INT PRIMARY KEY AUTO_INCREMENT,
    id_resena INT NOT NULL,
    id_proveedor INT NOT NULL,
    comentario VARCHAR(500),
    fecha TIMESTAMP NOT NULL,
    FOREIGN KEY (id_resena) REFERENCES Resena(id_resena),
    FOREIGN KEY (id_proveedor) REFERENCES Proveedor(id_proveedor)
);

CREATE TABLE IF NOT EXISTS Sancion (
    id_sancion INT PRIMARY KEY AUTO_INCREMENT,
    id_evento INT NOT NULL,
    motivo VARCHAR(255),
    valor DECIMAL(12,2),
    estado VARCHAR(30),
    fecha TIMESTAMP NOT NULL,
    FOREIGN KEY (id_evento) REFERENCES Evento(id_evento)
);

CREATE TABLE IF NOT EXISTS Segmentacion (
    id_segmento INT PRIMARY KEY AUTO_INCREMENT,
    id_usuario INT NOT NULL,
    nombre VARCHAR(150) NOT NULL,
    criterios VARCHAR(500),
    descripcion VARCHAR(255),
    estados VARCHAR(30),
    FOREIGN KEY (id_usuario) REFERENCES Usuario(id_usuario)
);

CREATE TABLE IF NOT EXISTS Campana (
    id_campana INT PRIMARY KEY AUTO_INCREMENT,
    id_proveedor INT,
    nombre VARCHAR(150) NOT NULL,
    descripcion VARCHAR(255),
    fecha_inicio TIMESTAMP,
    fecha_fin TIMESTAMP,
    FOREIGN KEY (id_proveedor) REFERENCES Proveedor(id_proveedor)
);

CREATE TABLE IF NOT EXISTS Envio_Campana (
    id_envio INT PRIMARY KEY AUTO_INCREMENT,
    id_campana INT NOT NULL,
    id_usuario INT NOT NULL,
    fecha_solicitud TIMESTAMP NOT NULL,
    estado VARCHAR(30),
    FOREIGN KEY (id_campana) REFERENCES Campana(id_campana),
    FOREIGN KEY (id_usuario) REFERENCES Usuario(id_usuario)
);

CREATE TABLE IF NOT EXISTS Puntos_fidelidad (
    id_puntos INT PRIMARY KEY AUTO_INCREMENT,
    id_usuario INT NOT NULL,
    cantidad INT NOT NULL,
    fecha_actualizacion TIMESTAMP NOT NULL,
    FOREIGN KEY (id_usuario) REFERENCES Usuario(id_usuario)
);

CREATE TABLE IF NOT EXISTS Disponibilidad_Servicio (
    id_disponibilidad INT PRIMARY KEY AUTO_INCREMENT,
    id_servicio INT NOT NULL,
    fecha DATE NOT NULL,
    hora_inicio TIME NOT NULL,
    hora_fin TIME NOT NULL,
    estado VARCHAR(30) DEFAULT 'DISPONIBLE',
    FOREIGN KEY (id_servicio) REFERENCES Servicio(id_servicio)
);

-- Columnas que complementan las tablas base.
ALTER TABLE Usuario ADD COLUMN IF NOT EXISTS pais VARCHAR(80);
ALTER TABLE Usuario ADD COLUMN IF NOT EXISTS ciudad VARCHAR(80);
ALTER TABLE Usuario ADD COLUMN IF NOT EXISTS fecha_nacimiento DATE;

ALTER TABLE Organizador ADD COLUMN IF NOT EXISTS ciudad VARCHAR(150);
ALTER TABLE Organizador ADD COLUMN IF NOT EXISTS especialidad VARCHAR(150);
ALTER TABLE Organizador ADD COLUMN IF NOT EXISTS portafolio VARCHAR(500);
ALTER TABLE Organizador ADD COLUMN IF NOT EXISTS tarifa_base DECIMAL(12,2);
ALTER TABLE Organizador ADD COLUMN IF NOT EXISTS estado_verificacion
    VARCHAR(30) NOT NULL DEFAULT 'PENDIENTE';

ALTER TABLE Evento ADD COLUMN IF NOT EXISTS id_lugar INT REFERENCES Lugar(id_lugar);
ALTER TABLE Evento ADD COLUMN IF NOT EXISTS id_cliente INT REFERENCES Usuario(id_usuario);

ALTER TABLE Servicio ADD COLUMN IF NOT EXISTS tipo VARCHAR(100);
ALTER TABLE Servicio ADD COLUMN IF NOT EXISTS cantidad_maxima_personas INT;
ALTER TABLE Servicio ADD COLUMN IF NOT EXISTS ciudad_zona VARCHAR(150);
ALTER TABLE Servicio ADD COLUMN IF NOT EXISTS anticipacion_minima INT DEFAULT 0;

ALTER TABLE Solicitud_Ejecucion ADD COLUMN IF NOT EXISTS id_servicio INT
    REFERENCES Servicio(id_servicio);
ALTER TABLE Solicitud_Ejecucion ADD COLUMN IF NOT EXISTS motivo_rechazo VARCHAR(500);
ALTER TABLE Solicitud_Ejecucion ADD COLUMN IF NOT EXISTS fecha_respuesta TIMESTAMP;
ALTER TABLE Solicitud_Ejecucion ADD COLUMN IF NOT EXISTS id_necesidad_servicio INT;
ALTER TABLE Solicitud_Ejecucion ADD COLUMN IF NOT EXISTS costo_comprometido DECIMAL(12,2)
    CHECK (costo_comprometido >= 0);

ALTER TABLE Propuesta ADD COLUMN IF NOT EXISTS numero_version INT DEFAULT 1;
ALTER TABLE Propuesta ADD COLUMN IF NOT EXISTS condiciones VARCHAR(1000);
ALTER TABLE Propuesta ADD COLUMN IF NOT EXISTS fecha_envio TIMESTAMP;
ALTER TABLE Propuesta ADD COLUMN IF NOT EXISTS vigente BOOLEAN DEFAULT TRUE;
ALTER TABLE Propuesta ADD COLUMN IF NOT EXISTS material_apoyo VARCHAR(1000);

ALTER TABLE Contratacion ALTER COLUMN condiciones SET DATA TYPE VARCHAR(1000);
ALTER TABLE Contratacion ADD COLUMN IF NOT EXISTS fecha_servicio DATE;
ALTER TABLE Contratacion ADD COLUMN IF NOT EXISTS hora_inicio TIME;
ALTER TABLE Contratacion ADD COLUMN IF NOT EXISTS hora_fin TIME;
ALTER TABLE Contratacion ADD COLUMN IF NOT EXISTS id_disponibilidad INT
    REFERENCES Disponibilidad_Servicio(id_disponibilidad);
ALTER TABLE Contratacion ADD COLUMN IF NOT EXISTS numero_version_propuesta INT;
ALTER TABLE Contratacion ADD COLUMN IF NOT EXISTS alcance_propuesta VARCHAR(255);
ALTER TABLE Contratacion ADD COLUMN IF NOT EXISTS id_necesidad_servicio INT;

ALTER TABLE Mensaje ADD COLUMN IF NOT EXISTS adjunto_url VARCHAR(1000);
ALTER TABLE Mensaje ADD COLUMN IF NOT EXISTS id_propuesta INT
    REFERENCES Propuesta(id_propuesta);

ALTER TABLE Campana ALTER COLUMN id_proveedor DROP NOT NULL;
ALTER TABLE Campana ADD COLUMN IF NOT EXISTS id_usuario INT
    REFERENCES Usuario(id_usuario);
ALTER TABLE Campana ADD COLUMN IF NOT EXISTS id_segmento INT
    REFERENCES Segmentacion(id_segmento);
ALTER TABLE Campana ADD COLUMN IF NOT EXISTS tipo_oferta VARCHAR(100);
ALTER TABLE Campana ADD COLUMN IF NOT EXISTS presupuesto_maximo DECIMAL(12,2);
ALTER TABLE Campana ADD COLUMN IF NOT EXISTS estado VARCHAR(20);
ALTER TABLE Campana ADD COLUMN IF NOT EXISTS visualizaciones BIGINT DEFAULT 0 NOT NULL;
ALTER TABLE Campana ADD COLUMN IF NOT EXISTS conversiones BIGINT DEFAULT 0 NOT NULL;

-- Tablas adicionales del esquema actual.
CREATE TABLE IF NOT EXISTS Solicitud_publicacion (
    id_solicitud INT PRIMARY KEY AUTO_INCREMENT,
    id_evento INT NOT NULL,
    estado VARCHAR(20) NOT NULL CHECK (estado IN ('PENDIENTE', 'APROBADA', 'RECHAZADA')),
    motivo VARCHAR(500),
    id_administrador INT,
    fecha_solicitud TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_revision TIMESTAMP,
    FOREIGN KEY (id_evento) REFERENCES Evento(id_evento),
    FOREIGN KEY (id_administrador) REFERENCES Usuario(id_usuario)
);

CREATE TABLE IF NOT EXISTS Plantilla_evento (
    id_plantilla INT PRIMARY KEY AUTO_INCREMENT,
    id_solicitud INT NOT NULL UNIQUE,
    nombre VARCHAR(150) NOT NULL,
    tipo VARCHAR(100),
    duracion_minutos BIGINT NOT NULL CHECK (duracion_minutos >= 0),
    FOREIGN KEY (id_solicitud) REFERENCES Solicitud_publicacion(id_solicitud)
);

CREATE TABLE IF NOT EXISTS Imagen_Servicio (
    id_imagen INT PRIMARY KEY AUTO_INCREMENT,
    id_servicio INT NOT NULL,
    ruta VARCHAR(500) NOT NULL,
    orden_imagen INT DEFAULT 0,
    FOREIGN KEY (id_servicio) REFERENCES Servicio(id_servicio)
);

CREATE TABLE IF NOT EXISTS Pregunta_Servicio (
    id_pregunta INT PRIMARY KEY AUTO_INCREMENT,
    id_servicio INT NOT NULL,
    id_organizador INT NOT NULL,
    pregunta VARCHAR(500) NOT NULL,
    respuesta VARCHAR(500),
    fecha_pregunta TIMESTAMP NOT NULL,
    fecha_respuesta TIMESTAMP,
    estado VARCHAR(30) DEFAULT 'PENDIENTE',
    FOREIGN KEY (id_servicio) REFERENCES Servicio(id_servicio)
);

CREATE TABLE IF NOT EXISTS Necesidad_Servicio_Evento (
    id_necesidad_servicio INT PRIMARY KEY AUTO_INCREMENT,
    id_evento INT NOT NULL,
    descripcion VARCHAR(500) NOT NULL,
    estado VARCHAR(30) DEFAULT 'ABIERTA',
    id_propuesta_seleccionada INT,
    FOREIGN KEY (id_evento) REFERENCES Evento(id_evento),
    FOREIGN KEY (id_propuesta_seleccionada) REFERENCES Propuesta(id_propuesta)
);

CREATE TABLE IF NOT EXISTS Notificacion_interna (
    id_notificacion INT PRIMARY KEY AUTO_INCREMENT,
    id_usuario INT,
    mensaje VARCHAR(500) NOT NULL,
    fecha TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    id_invitado INT,
    id_evento INT,
    id_usuario_destinatario INT,
    id_proveedor INT,
    FOREIGN KEY (id_usuario) REFERENCES Usuario(id_usuario),
    FOREIGN KEY (id_invitado) REFERENCES Invitado(id_invitado),
    FOREIGN KEY (id_evento) REFERENCES Evento(id_evento),
    FOREIGN KEY (id_usuario_destinatario) REFERENCES Usuario(id_usuario),
    FOREIGN KEY (id_proveedor) REFERENCES Proveedor(id_proveedor),
    CONSTRAINT destinatario_notificacion CHECK (
        (id_usuario IS NOT NULL AND id_invitado IS NULL)
        OR (id_usuario IS NULL AND id_invitado IS NOT NULL)
    )
);

CREATE TABLE IF NOT EXISTS Finalizacion_evento (
    id_evento INT PRIMARY KEY REFERENCES Evento(id_evento),
    fecha TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    enviada BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS Fotografia_evento (
    id_fotografia INT PRIMARY KEY AUTO_INCREMENT,
    id_evento INT NOT NULL REFERENCES Finalizacion_evento(id_evento),
    nombre VARCHAR(150) NOT NULL,
    contenido BLOB NOT NULL
);

CREATE TABLE IF NOT EXISTS Entrega_galeria (
    id_evento INT NOT NULL REFERENCES Finalizacion_evento(id_evento),
    id_invitado INT NOT NULL REFERENCES Invitado(id_invitado),
    PRIMARY KEY (id_evento, id_invitado)
);

CREATE TABLE IF NOT EXISTS Solicitud_Contratacion_Organizador (
    id_solicitud INT PRIMARY KEY AUTO_INCREMENT,
    id_cliente INT NOT NULL,
    id_organizador INT NOT NULL,
    tipo_evento VARCHAR(100) NOT NULL,
    ciudad VARCHAR(150) NOT NULL,
    fecha_aproximada DATE NOT NULL,
    presupuesto_estimado DECIMAL(12,2) NOT NULL CHECK (presupuesto_estimado >= 0),
    criterios_busqueda VARCHAR(500),
    estado VARCHAR(30) NOT NULL DEFAULT 'PENDIENTE',
    fecha_solicitud TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (id_cliente) REFERENCES Usuario(id_usuario),
    FOREIGN KEY (id_organizador) REFERENCES Organizador(id_organizador)
);

CREATE TABLE IF NOT EXISTS Resena_Organizador (
    id_resena_organizador INT PRIMARY KEY AUTO_INCREMENT,
    id_organizador INT NOT NULL,
    id_cliente INT NOT NULL,
    calificacion INT NOT NULL CHECK (calificacion BETWEEN 1 AND 5),
    comentario VARCHAR(500),
    fecha TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (id_organizador) REFERENCES Organizador(id_organizador),
    FOREIGN KEY (id_cliente) REFERENCES Usuario(id_usuario)
);

CREATE TABLE IF NOT EXISTS Busqueda_evento (
    id_busqueda INT PRIMARY KEY AUTO_INCREMENT,
    id_usuario INT NOT NULL,
    tipo VARCHAR(100) NOT NULL,
    fecha_busqueda TIMESTAMP NOT NULL,
    FOREIGN KEY (id_usuario) REFERENCES Usuario(id_usuario)
);

CREATE TABLE IF NOT EXISTS Segmento_cliente (
    id_segmento_cliente INT PRIMARY KEY AUTO_INCREMENT,
    id_segmento INT NOT NULL,
    id_usuario INT NOT NULL,
    FOREIGN KEY (id_segmento) REFERENCES Segmentacion(id_segmento),
    FOREIGN KEY (id_usuario) REFERENCES Usuario(id_usuario),
    CONSTRAINT uq_segmento_cliente UNIQUE (id_segmento, id_usuario)
);

CREATE TABLE IF NOT EXISTS Presupuesto_marketing (
    id_presupuesto INT PRIMARY KEY,
    disponible DECIMAL(12,2) NOT NULL CHECK (disponible >= 0)
);

-- Relaciones circulares o añadidas a tablas ya creadas.
ALTER TABLE Solicitud_Ejecucion
    ADD CONSTRAINT IF NOT EXISTS fk_solicitud_necesidad_servicio
    FOREIGN KEY (id_necesidad_servicio)
    REFERENCES Necesidad_Servicio_Evento(id_necesidad_servicio);

ALTER TABLE Contratacion
    ADD CONSTRAINT IF NOT EXISTS fk_contratacion_necesidad_servicio
    FOREIGN KEY (id_necesidad_servicio)
    REFERENCES Necesidad_Servicio_Evento(id_necesidad_servicio);

ALTER TABLE Pregunta_Servicio
    ADD CONSTRAINT IF NOT EXISTS fk_pregunta_organizador
    FOREIGN KEY (id_organizador) REFERENCES Organizador(id_organizador);

CREATE UNIQUE INDEX IF NOT EXISTS uq_disponibilidad_servicio
    ON Disponibilidad_Servicio(id_servicio, fecha, hora_inicio, hora_fin);
CREATE INDEX IF NOT EXISTS idx_imagen_servicio ON Imagen_Servicio(id_servicio);
CREATE INDEX IF NOT EXISTS idx_pregunta_servicio ON Pregunta_Servicio(id_servicio);
CREATE UNIQUE INDEX IF NOT EXISTS uq_organizador_usuario ON Organizador(id_usuario);
CREATE UNIQUE INDEX IF NOT EXISTS uq_propuesta_version
    ON Propuesta(id_solicitud, numero_version);
CREATE UNIQUE INDEX IF NOT EXISTS uq_contratacion_propuesta
    ON Contratacion(id_propuesta);
CREATE UNIQUE INDEX IF NOT EXISTS uq_contratacion_necesidad
    ON Contratacion(id_necesidad_servicio);
CREATE INDEX IF NOT EXISTS idx_contratacion_evento_estado
    ON Contratacion(id_evento, estado);
CREATE UNIQUE INDEX IF NOT EXISTS uq_conversacion_solicitud
    ON Conversacion(id_solicitud);
CREATE INDEX IF NOT EXISTS idx_cu26_cliente
    ON Solicitud_Contratacion_Organizador(id_cliente);
CREATE INDEX IF NOT EXISTS idx_cu26_organizador_estado
    ON Solicitud_Contratacion_Organizador(id_organizador, estado);
CREATE INDEX IF NOT EXISTS idx_cu26_resenas
    ON Resena_Organizador(id_organizador);


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

-- Tres cuentas por actor. Todas usan la clave 1234.
INSERT INTO Usuario
(id_usuario, nombre, usuario, contraseña, estado, fecha_registro, acepta_marketing, tipo_usuario)
VALUES
(1, 'Laura Gómez', 'laura.gomez', '1234', 'ACTIVO', TIMESTAMP '2026-10-01 09:00:00', TRUE, 'ORGANIZADOR'),
(2, 'Carlos Rodríguez', 'carlos.rodriguez', '1234', 'ACTIVO', TIMESTAMP '2026-10-02 10:30:00', TRUE, 'ORGANIZADOR'),
(8, 'Valentina Flores', 'valentina', '1234', 'ACTIVO', TIMESTAMP '2026-10-03 08:00:00', FALSE, 'ORGANIZADOR'),
(3, 'Decoraciones Arcoíris', 'decoraciones.arcoiris', '1234', 'ACTIVO', TIMESTAMP '2026-10-03 11:00:00', TRUE, 'PROVEEDOR'),
(4, 'Sabores & Eventos', 'sabores.eventos', '1234', 'ACTIVO', TIMESTAMP '2026-10-03 14:00:00', FALSE, 'PROVEEDOR'),
(13, 'Flash Studio Fotografía', 'flash', '1234', 'ACTIVO', TIMESTAMP '2026-10-04 09:00:00', FALSE, 'PROVEEDOR'),
(5, 'Ana Martínez', 'ana.martinez', '1234', 'ACTIVO', TIMESTAMP '2026-10-04 08:30:00', TRUE, 'CLIENTE'),
(6, 'Diego Pérez', 'diego.perez', '1234', 'ACTIVO', TIMESTAMP '2026-10-05 16:00:00', FALSE, 'CLIENTE'),
(7, 'Paula Ríos', 'paula.rios', '1234', 'ACTIVO', TIMESTAMP '2026-10-05 17:00:00', FALSE, 'CLIENTE'),
(14, 'Camila Reyes', 'camila', '1234', 'ACTIVO', TIMESTAMP '2026-10-06 09:00:00', FALSE, 'ADMINISTRADOR'),
(15, 'Mateo Castillo', 'mateo', '1234', 'ACTIVO', TIMESTAMP '2026-10-06 09:10:00', FALSE, 'ADMINISTRADOR'),
(16, 'Shirly Montenegro', 'shirly', '1234', 'ACTIVO', TIMESTAMP '2026-10-06 09:20:00', FALSE, 'ADMINISTRADOR');

-- Ubicacion y preferencias demo para la segmentacion de clientes.
UPDATE Usuario SET pais = 'Colombia', ciudad = 'Bogotá'
WHERE usuario = 'ana.martinez' AND tipo_usuario = 'CLIENTE';
UPDATE Usuario SET pais = 'Colombia', ciudad = 'Bogotá'
WHERE usuario = 'diego.perez' AND tipo_usuario = 'CLIENTE';
UPDATE Usuario SET pais = 'Colombia', ciudad = 'Medellín'
WHERE usuario = 'paula.rios' AND tipo_usuario = 'CLIENTE';

INSERT INTO Busqueda_evento (id_usuario, tipo, fecha_busqueda)
SELECT id_usuario, 'Boda', CURRENT_TIMESTAMP
FROM Usuario WHERE usuario IN ('ana.martinez', 'diego.perez')
    AND tipo_usuario = 'CLIENTE';
INSERT INTO Busqueda_evento (id_usuario, tipo, fecha_busqueda)
SELECT id_usuario, 'Quinceaños', CURRENT_TIMESTAMP
FROM Usuario WHERE usuario = 'paula.rios' AND tipo_usuario = 'CLIENTE';

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
(6, 6, 4),
(7, 7, 4),
(8, 8, 2),
(13, 13, 3),
(14, 14, 1),
(15, 15, 1),
(16, 16, 1);


-- ---------------------------------------------------------
-- LUGARES
-- ---------------------------------------------------------

INSERT INTO Lugar
(id_lugar, nombre, direccion, capacidad, descripcion, estado)
VALUES
(1, 'Salón Primavera', 'Calle 100 #15-20', 150, 'Salón para celebraciones sociales', 'DISPONIBLE'),
(2, 'Jardín Los Pinos', 'Carrera 7 #120-15', 250, 'Espacio al aire libre para eventos', 'DISPONIBLE'),
(3, 'Salón Mirador', 'Calle 80 #10-45', 80, 'Salón para eventos pequeños', 'DISPONIBLE'),
(4, 'Terraza Chapinero', 'Carrera 13 #60-30', 120, 'Terraza cubierta con vista a la ciudad', 'DISPONIBLE');


-- ---------------------------------------------------------
-- ORGANIZADORES
-- ---------------------------------------------------------

INSERT INTO Organizador
(id_organizador, id_usuario, empresa, telefono, estado,
 ciudad, especialidad, portafolio, tarifa_base, estado_verificacion)
VALUES
(1, 1, 'Eventos Laura', '3001234567', 'ACTIVO',
 'Bogota', 'Bodas y eventos sociales',
 'Organizacion de bodas y celebraciones sociales',
 1500000.00, 'VERIFICADO'),

(2, 2, 'Celebraciones CR', '3017654321', 'ACTIVO',
 'Bogota', 'Celebraciones familiares',
 'Organizacion de reuniones y celebraciones familiares',
 1000000.00, 'VERIFICADO'),

(3, 8, 'Momentos Valentina', '3005550101', 'ACTIVO',
 'Bogota', 'Eventos sociales',
 'Organizacion de eventos sociales y familiares',
 900000.00, 'VERIFICADO');

-- ---------------------------------------------------------
-- RESENAS DE ORGANIZADORES PARA CU-026
-- ---------------------------------------------------------

INSERT INTO Resena_Organizador
(id_organizador, id_cliente, calificacion, comentario)
VALUES
(1, 5, 5, 'Buena organizacion y comunicacion durante el evento.'),
(2, 6, 4, 'La celebracion estuvo bien organizada.'),
(3, 5, 5, 'Excelente atencion y organizacion.'),
(3, 7, 5, 'Una celebracion muy bien organizada.');

-- ---------------------------------------------------------
-- PROVEEDORES
-- ---------------------------------------------------------

INSERT INTO Proveedor
(id_proveedor, id_usuario, nombre, empresa, correo, telefono, direccion, estado)
VALUES
(1, 3, 'Mariana López', 'Decoraciones Arcoíris', 'contacto@arcoiris.com', '3101234567', 'Carrera 15 #80-20', 'APROBADO'),
(2, 4, 'Andrés Torres', 'Sabores & Eventos', 'contacto@saboreseventos.com', '3117654321', 'Calle 90 #20-30', 'APROBADO'),
(5, 13, 'Flash Studio Fotografía', 'Flash Studio Fotografía', 'flash@ejemplo.com', '3005550203', 'Bogotá', 'VERIFICADO');


-- ---------------------------------------------------------
-- EVENTOS
-- ---------------------------------------------------------

INSERT INTO Evento
(id_evento, id_organizador, nombre, descripcion, fecha_inicio, fecha_fin, ubicacion, estado, fecha_creacion, tipo, presupuesto, invitados_iniciales)
VALUES
(1, 1, 'Boda Laura y Daniel', 'Celebración de matrimonio', TIMESTAMP '2026-11-15 15:00:00', TIMESTAMP '2026-11-15 23:00:00', 'Salón Primavera', 'PLANEADO', TIMESTAMP '2026-10-06 09:00:00', 'Boda', 15000000.00, 120),
(2, 2, 'Baby Shower Valentina', 'Celebración de bienvenida para bebé', TIMESTAMP '2026-12-05 14:00:00', TIMESTAMP '2026-12-05 19:00:00', 'Jardín Los Pinos', 'PLANEADO', TIMESTAMP '2026-10-07 10:00:00', 'Baby shower', 5000000.00, 60),
(3, 3, 'Cumpleaños 15 de Sofía', 'Fiesta de quince años con temática de unicornios', TIMESTAMP '2027-02-20 17:00:00', TIMESTAMP '2027-02-20 23:00:00', 'Salón Mirador', 'PLANEADO', TIMESTAMP '2026-10-20 10:00:00', 'Cumpleaños', 12000000.00, 70),
(4, 3, 'Graduación de Medicina', 'Celebración de grado con cena y brindis', TIMESTAMP '2027-03-13 15:00:00', TIMESTAMP '2027-03-13 22:00:00', 'Terraza Chapinero', 'PLANEADO', TIMESTAMP '2026-10-22 11:00:00', 'Graduación', 9000000.00, 90);


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
(4, 3, 'Natalia Ruiz', '3005554444', 'Carrera 12 #45-60', 'PENDIENTE', 'natalia@email.com'),
(5, 1, 'Emilia Navarro', '3005550301', 'Bogotá', 'CONFIRMADO', 'emilia@ejemplo.com'),
(6, 2, 'Nicolás Vega', '3005550302', 'Bogotá', 'PENDIENTE', 'nicolas@ejemplo.com'),
(7, 3, 'Renata Morales', '3005550303', 'Bogotá', 'CONFIRMADO', 'renata@ejemplo.com');


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
(1, 3, 'Pañitos para bebé', 'Paquete de pañitos húmedos', 'RESERVADO', 25000.00),
(2, 3, 'Kit de biberones', 'Kit de biberones para recién nacido', 'RESERVADO', 80000.00),
(3, 3, 'Cobija para bebé', 'Cobija suave para recién nacido', 'RESERVADO', 60000.00),
(4, 3, 'Ropa de bebé', 'Conjunto de ropa para recién nacido', 'DISPONIBLE', 50000.00);


-- ---------------------------------------------------------
-- RESERVAS DE REGALOS
-- ---------------------------------------------------------

INSERT INTO Reserva_regalo
(id_reserva, id_evento, id_invitado, id_regalo, fecha_reserva, estado)
VALUES
(1, 2, 3, 1, TIMESTAMP '2026-11-10 12:00:00', 'RESERVADO'),
(2, 2, 4, 3, TIMESTAMP '2026-11-11 15:30:00', 'RESERVADO'),
(3, 2, 3, 2, TIMESTAMP '2026-11-12 09:00:00', 'RESERVADO');


-- ---------------------------------------------------------
-- SERVICIOS
-- ---------------------------------------------------------

-- Cada proveedor tiene cinco servicios en estados distintos (CU-11):
-- publicados con horarios, en borrador (sin horarios) y retirados.
INSERT INTO Servicio
(id_servicio, id_proveedor, nombre, descripcion, precio, categoria, estado,
 tipo, cantidad_maxima_personas, ciudad_zona, anticipacion_minima)
VALUES
(1, 1, 'Decoración floral', 'Decoración floral para eventos sociales', 1200000.00, 'DECORACION', 'DISPONIBLE', 'Decoración', 300, 'Bogotá', 7),
(2, 1, 'Decoración temática', 'Decoración personalizada según temática', 900000.00, 'DECORACION', 'DISPONIBLE', 'Decoración', 200, 'Bogotá', 10),
(3, 2, 'Catering básico', 'Servicio de alimentación para eventos', 1800000.00, 'ALIMENTACION', 'DISPONIBLE', 'Catering', 100, 'Bogotá', 7),
(4, 2, 'Mesa de postres', 'Postres variados para celebraciones', 650000.00, 'ALIMENTACION', 'DISPONIBLE', 'Postres', 150, 'Bogotá', 5),
(5, 5, 'Cobertura fotográfica completa', 'Fotógrafo profesional durante todo el evento, con galería digital', 1500000.00, 'FOTOGRAFIA', 'DISPONIBLE', 'Fotografía', 300, 'Bogotá', 7),
(6, 5, 'Video highlights del evento', 'Video resumen de cinco minutos con los mejores momentos', 950000.00, 'FOTOGRAFIA', 'DISPONIBLE', 'Video', 300, 'Bogotá', 10),
(7, 5, 'Sesión de retratos', 'Sesión de retratos familiares antes del evento', 400000.00, 'FOTOGRAFIA', 'BORRADOR', 'Fotografía', 20, 'Bogotá', 3),
(8, 5, 'Álbum impreso premium', 'Álbum de 40 páginas con las mejores fotos', 600000.00, 'FOTOGRAFIA', 'RETIRADO', 'Fotografía', 1, 'Bogotá', 15),
(9, 1, 'Arreglos de mesa', 'Centros de mesa con flores de temporada', 450000.00, 'DECORACION', 'DISPONIBLE', 'Decoración', 200, 'Bogotá', 5),
(10, 1, 'Arco de globos', 'Arco de globos para la entrada del salón', 380000.00, 'DECORACION', 'BORRADOR', 'Decoración', 300, 'Bogotá', 3),
(11, 1, 'Ambientación de salón', 'Ambientación completa con iluminación y cortinas', 1100000.00, 'DECORACION', 'DISPONIBLE', 'Decoración', 250, 'Bogotá', 14),
(12, 2, 'Catering gourmet', 'Menú de tres tiempos con meseros y montaje de buffet', 1900000.00, 'ALIMENTACION', 'DISPONIBLE', 'Catering', 150, 'Bogotá', 10),
(13, 2, 'Cóctel de bienvenida', 'Cóctel sin alcohol con estación de bebidas', 520000.00, 'ALIMENTACION', 'DISPONIBLE', 'Bebidas', 150, 'Bogotá', 5),
(14, 2, 'Estación de café', 'Café de origen y repostería para la sobremesa', 300000.00, 'ALIMENTACION', 'BORRADOR', 'Bebidas', 100, 'Bogotá', 3),
(15, 5, 'Cabina de fotos', 'Cabina instantánea con accesorios y fotos impresas', 700000.00, 'FOTOGRAFIA', 'DISPONIBLE', 'Fotografía', 150, 'Bogotá', 7);


-- ---------------------------------------------------------
-- HORARIOS DE LOS SERVICIOS (CU-11)
-- Los identificadores empiezan en 101 para no mezclarse con los horarios
-- que crean las pruebas y las contrataciones de las demostraciones.
-- ---------------------------------------------------------

INSERT INTO Disponibilidad_Servicio
(id_disponibilidad, id_servicio, fecha, hora_inicio, hora_fin, estado)
VALUES
(101, 5, DATE '2027-02-20', TIME '14:00:00', TIME '23:59:00', 'DISPONIBLE'),
(102, 5, DATE '2027-03-13', TIME '12:00:00', TIME '23:00:00', 'DISPONIBLE'),
(103, 6, DATE '2027-02-20', TIME '14:00:00', TIME '23:00:00', 'DISPONIBLE'),
(104, 6, DATE '2027-03-13', TIME '12:00:00', TIME '23:00:00', 'DISPONIBLE'),
(105, 9, DATE '2027-02-20', TIME '10:00:00', TIME '20:00:00', 'DISPONIBLE'),
(106, 11, DATE '2027-03-13', TIME '10:00:00', TIME '20:00:00', 'DISPONIBLE'),
(107, 12, DATE '2027-02-20', TIME '12:00:00', TIME '23:00:00', 'DISPONIBLE'),
(108, 13, DATE '2027-02-20', TIME '12:00:00', TIME '23:00:00', 'DISPONIBLE'),
(109, 13, DATE '2027-03-13', TIME '12:00:00', TIME '23:00:00', 'DISPONIBLE'),
(110, 4, DATE '2027-03-13', TIME '10:00:00', TIME '20:00:00', 'DISPONIBLE'),
(111, 15, DATE '2027-02-20', TIME '14:00:00', TIME '23:00:00', 'DISPONIBLE');


-- ---------------------------------------------------------
-- PREGUNTAS PÚBLICAS DE LOS SERVICIOS (CU-11)
-- ---------------------------------------------------------

INSERT INTO Pregunta_Servicio
(id_pregunta, id_servicio, id_organizador, pregunta, respuesta, fecha_pregunta, fecha_respuesta, estado)
VALUES
(1, 5, 3, '¿Entregan las fotos editadas o solo las originales?', 'Entregamos 300 fotos editadas en una galería privada.', TIMESTAMP '2026-10-25 10:00:00', TIMESTAMP '2026-10-25 15:00:00', 'RESPONDIDA'),
(2, 5, 1, '¿Cuántas horas de cobertura incluye?', NULL, TIMESTAMP '2026-11-02 09:00:00', NULL, 'PENDIENTE'),
(3, 12, 3, '¿El menú incluye opciones vegetarianas?', 'Sí, incluimos un plato vegetariano en cada tiempo.', TIMESTAMP '2026-10-26 11:00:00', TIMESTAMP '2026-10-26 16:00:00', 'RESPONDIDA'),
(4, 1, 2, '¿Pueden usar solo flores blancas?', NULL, TIMESTAMP '2026-11-02 14:30:00', NULL, 'PENDIENTE');


-- ---------------------------------------------------------
-- NECESIDADES DE SERVICIO DE LOS EVENTOS (CU-12)
-- Lo que cada organizador necesita contratar para su evento.
-- ---------------------------------------------------------

INSERT INTO Necesidad_Servicio_Evento
(id_necesidad_servicio, id_evento, descripcion, estado)
VALUES
(1, 3, 'Decoración temática de unicornios', 'ABIERTA'),
(2, 3, 'Catering para 70 personas', 'ABIERTA'),
(3, 3, 'Fotografía del evento', 'ABIERTA'),
(4, 4, 'Fotografía y video de la ceremonia', 'ABIERTA'),
(5, 4, 'Mesa de postres', 'ABIERTA'),
(6, 4, 'Decoración de mesas', 'ABIERTA'),
(7, 3, 'Cóctel de bienvenida', 'ABIERTA'),
(8, 2, 'Fotografía del baby shower', 'ABIERTA');


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

-- Solicitudes 1 a 3: las del evento 1 ya están contratadas y la 3 negocia.
-- Solicitudes 4 a 15: historia de CU-12 y CU-13 de la boda de Valentina.
INSERT INTO Solicitud_Ejecucion
(id_solicitud, id_proveedor, id_evento, fecha_solicitud, estado, id_servicio, id_necesidad_servicio, motivo_rechazo, fecha_respuesta)
VALUES
(1, 1, 1, TIMESTAMP '2026-10-08 10:00:00', 'CONTRATADA', 1, NULL, NULL, TIMESTAMP '2026-10-08 12:00:00'),
(2, 2, 1, TIMESTAMP '2026-10-08 11:30:00', 'CONTRATADA', 3, NULL, NULL, TIMESTAMP '2026-10-08 13:00:00'),
(3, 1, 2, TIMESTAMP '2026-10-09 09:00:00', 'EN_NEGOCIACION', 2, NULL, NULL, TIMESTAMP '2026-10-09 15:00:00'),
(4, 1, 3, TIMESTAMP '2026-11-01 09:00:00', 'PENDIENTE', 2, 1, NULL, NULL),
(5, 2, 3, TIMESTAMP '2026-11-01 09:30:00', 'EN_NEGOCIACION', 12, 2, NULL, TIMESTAMP '2026-11-01 14:00:00'),
(6, 5, 3, TIMESTAMP '2026-11-01 10:00:00', 'EN_NEGOCIACION', 5, 3, NULL, TIMESTAMP '2026-11-01 12:00:00'),
(7, 5, 4, TIMESTAMP '2026-11-01 10:30:00', 'EN_NEGOCIACION', 6, 4, NULL, TIMESTAMP '2026-11-01 16:00:00'),
(8, 2, 3, TIMESTAMP '2026-11-01 11:00:00', 'RECHAZADA', 3, 2, 'El catering básico atiende hasta 50 personas; te recomendamos el catering gourmet.', TIMESTAMP '2026-11-01 13:00:00'),
(9, 1, 4, TIMESTAMP '2026-11-01 11:30:00', 'PENDIENTE', 9, 6, NULL, NULL),
(10, 1, 3, TIMESTAMP '2026-11-01 12:00:00', 'PROPUESTA_RECHAZADA', 9, 1, NULL, TIMESTAMP '2026-11-01 17:00:00'),
(11, 2, 3, TIMESTAMP '2026-11-02 09:00:00', 'PENDIENTE', 13, 7, NULL, NULL),
(12, 5, 2, TIMESTAMP '2026-11-02 09:30:00', 'PENDIENTE', 5, 8, NULL, NULL),
(13, 5, 4, TIMESTAMP '2026-11-02 10:00:00', 'RECHAZADA', 5, 4, 'Ya tenemos otro evento ese día a esa hora.', TIMESTAMP '2026-11-02 12:00:00'),
(14, 5, 3, TIMESTAMP '2026-11-02 10:30:00', 'EN_NEGOCIACION', 6, 3, NULL, TIMESTAMP '2026-11-02 15:00:00'),
(15, 2, 4, TIMESTAMP '2026-11-02 11:00:00', 'EN_NEGOCIACION', 4, 5, NULL, TIMESTAMP '2026-11-02 16:00:00');


-- Cada solicitud en curso compromete el precio del servicio en el presupuesto
-- del evento (CU-01). Las solicitudes 1 y 2 ya se cuentan por su contratación.
UPDATE Solicitud_Ejecucion s
SET costo_comprometido = (SELECT v.precio FROM Servicio v WHERE v.id_servicio = s.id_servicio)
WHERE s.id_solicitud >= 3;


-- ---------------------------------------------------------
-- PROPUESTAS
-- La 6 es la versión 2 de la solicitud 6 y está seleccionada: la
-- organizadora puede contratarla (CU-13).
-- ---------------------------------------------------------

INSERT INTO Propuesta
(id_propuesta, id_solicitud, valor, descripcion, estado, numero_version, condiciones, fecha_envio, vigente, material_apoyo)
VALUES
(1, 1, 1800000.00, 'Decoración completa para la boda', 'CONTRATADA', 1, 'Montaje terminado tres horas antes del evento', TIMESTAMP '2026-10-08 12:00:00', TRUE, NULL),
(2, 2, 2200000.00, 'Servicio de catering y postres', 'CONTRATADA', 1, 'Servicio de alimentación para 120 personas', TIMESTAMP '2026-10-08 13:00:00', TRUE, NULL),
(3, 3, 900000.00, 'Decoración temática para baby shower', 'ENVIADA', 1, 'Anticipo del 50% para reservar la fecha', TIMESTAMP '2026-10-09 15:00:00', TRUE, NULL),
(4, 5, 1900000.00, 'Menú de tres tiempos para 70 personas con 4 meseros y montaje de buffet', 'ENVIADA', 1, 'Anticipo del 40% y saldo al finalizar el evento', TIMESTAMP '2026-11-01 14:00:00', TRUE, 'https://ejemplo.com/menu-gourmet.pdf'),
(5, 6, 1500000.00, 'Cobertura fotográfica de 10 horas con galería digital', 'ENVIADA', 1, 'Entrega de la galería en 15 días', TIMESTAMP '2026-11-01 12:00:00', FALSE, 'https://ejemplo.com/portafolio-fotografia.pdf'),
(6, 6, 1350000.00, 'Cobertura fotográfica de 8 horas con galería digital y 300 fotos editadas', 'SELECCIONADA', 2, 'Entrega de la galería en 15 días. Anticipo del 30%', TIMESTAMP '2026-11-02 10:00:00', TRUE, 'https://ejemplo.com/portafolio-fotografia.pdf'),
(7, 7, 950000.00, 'Video highlights de cinco minutos de la ceremonia', 'ENVIADA', 1, 'Entrega en 20 días en formato digital', TIMESTAMP '2026-11-01 16:00:00', TRUE, NULL),
(8, 10, 450000.00, 'Centros de mesa con flores de temporada', 'RECHAZADA', 1, 'Montaje dos horas antes del evento', TIMESTAMP '2026-11-01 17:00:00', FALSE, NULL),
(9, 14, 800000.00, 'Video highlights de tres minutos para redes sociales', 'ENVIADA', 1, 'Entrega en 10 días', TIMESTAMP '2026-11-02 15:00:00', TRUE, NULL),
(10, 15, 700000.00, 'Mesa de postres para 90 personas con seis variedades', 'ENVIADA', 1, 'Entrega y montaje incluidos', TIMESTAMP '2026-11-02 16:00:00', TRUE, NULL);

-- La necesidad de fotografía del cumpleaños ya tiene una propuesta seleccionada.
UPDATE Necesidad_Servicio_Evento
SET estado = 'SELECCIONADA', id_propuesta_seleccionada = 6
WHERE id_necesidad_servicio = 3;


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
(1, 1, 1, 1, 900000.00, TIMESTAMP '2026-10-11 09:00:00', 'Anticipo de decoración', 'EGRESO', 'REGISTRADO'),
(2, 1, 1, 1, 900000.00, TIMESTAMP '2026-11-15 23:30:00', 'Pago final de decoración', 'EGRESO', 'PENDIENTE'),
(3, 2, 1, 2, 1100000.00, TIMESTAMP '2026-10-12 10:00:00', 'Anticipo de catering', 'EGRESO', 'REGISTRADO'),
(4, 2, 1, 2, 1100000.00, TIMESTAMP '2026-11-15 23:30:00', 'Pago final de catering', 'EGRESO', 'PENDIENTE');


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
(2, 2, TIMESTAMP '2026-10-08 12:00:00', 'ACTIVA'),
(3, 3, TIMESTAMP '2026-10-09 09:30:00', 'ACTIVA'),
(4, 5, TIMESTAMP '2026-11-01 13:30:00', 'ACTIVA'),
(5, 6, TIMESTAMP '2026-11-01 11:30:00', 'ACTIVA');


-- ---------------------------------------------------------
-- MENSAJES
-- ---------------------------------------------------------

INSERT INTO Mensaje
(id_mensaje, id_conversacion, id_usuario_remitente, contenido, fecha_hora)
VALUES
(1, 1, 1, 'Hola, quisiera confirmar si pueden incluir flores blancas.', TIMESTAMP '2026-10-08 10:35:00'),
(2, 1, 3, 'Sí, podemos incluir flores blancas sin problema.', TIMESTAMP '2026-10-08 10:40:00'),
(3, 2, 1, '¿El servicio de catering incluye bebidas?', TIMESTAMP '2026-10-08 12:10:00'),
(4, 2, 4, 'Sí, incluye bebidas no alcohólicas.', TIMESTAMP '2026-10-08 12:15:00'),
(5, 3, 2, '¿Pueden decorar con temática de mariposas?', TIMESTAMP '2026-10-09 09:35:00'),
(6, 3, 3, 'Con gusto, enviamos la propuesta hoy mismo.', TIMESTAMP '2026-10-09 14:50:00'),
(7, 4, 8, '¿El menú incluye meseros y montaje del buffet?', TIMESTAMP '2026-11-01 13:40:00'),
(8, 4, 4, 'Sí, incluye cuatro meseros y el montaje completo.', TIMESTAMP '2026-11-01 13:55:00'),
(9, 5, 8, 'Nos interesa, pero el valor supera lo previsto. ¿Pueden ajustar la propuesta?', TIMESTAMP '2026-11-01 15:00:00'),
(10, 5, 13, 'Claro, enviamos una nueva versión con 8 horas de cobertura.', TIMESTAMP '2026-11-02 10:05:00');

-- El ajuste queda vinculado a la versión 1 de la propuesta de fotografía y
-- la respuesta lleva el portafolio como adjunto.
UPDATE Mensaje SET id_propuesta = 5 WHERE id_mensaje = 9;
UPDATE Mensaje SET adjunto_url = 'https://ejemplo.com/portafolio-fotografia.pdf' WHERE id_mensaje = 10;


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

INSERT INTO Presupuesto_marketing (id_presupuesto, disponible)
VALUES (1, 20000000.00);

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
(2, 6, 120, TIMESTAMP '2026-10-10 18:00:00'),
(3, 7, 80, TIMESTAMP '2026-10-10 18:00:00');

-- Continuar los identificadores automáticos después de los datos de ejemplo.
ALTER TABLE Permiso ALTER COLUMN id_permiso RESTART WITH 5;
ALTER TABLE Rol ALTER COLUMN id_rol RESTART WITH 5;
ALTER TABLE Usuario ALTER COLUMN id_usuario RESTART WITH 17;
ALTER TABLE Rol_permiso ALTER COLUMN id_rol_permiso RESTART WITH 7;
ALTER TABLE Usuario_rol ALTER COLUMN id_usuario_rol RESTART WITH 17;
ALTER TABLE Lugar ALTER COLUMN id_lugar RESTART WITH 5;
ALTER TABLE Organizador ALTER COLUMN id_organizador RESTART WITH 4;
ALTER TABLE Proveedor ALTER COLUMN id_proveedor RESTART WITH 6;
ALTER TABLE Evento ALTER COLUMN id_evento RESTART WITH 5;
ALTER TABLE Planificacion ALTER COLUMN id_planificacion RESTART WITH 4;
ALTER TABLE Invitacion ALTER COLUMN id_invitacion RESTART WITH 4;
ALTER TABLE Invitado ALTER COLUMN id_invitado RESTART WITH 8;
ALTER TABLE Acompanante ALTER COLUMN id_acompanante RESTART WITH 4;
ALTER TABLE Necesidad_Especial ALTER COLUMN id_necesidad RESTART WITH 3;
ALTER TABLE Regalo ALTER COLUMN id_regalo RESTART WITH 5;
ALTER TABLE Reserva_regalo ALTER COLUMN id_reserva RESTART WITH 4;
ALTER TABLE Servicio ALTER COLUMN id_servicio RESTART WITH 16;
ALTER TABLE Disponibilidad_Servicio ALTER COLUMN id_disponibilidad RESTART WITH 112;
ALTER TABLE Pregunta_Servicio ALTER COLUMN id_pregunta RESTART WITH 5;
ALTER TABLE Necesidad_Servicio_Evento ALTER COLUMN id_necesidad_servicio RESTART WITH 9;
ALTER TABLE Paquete_servicio ALTER COLUMN id_paquete RESTART WITH 3;
ALTER TABLE Solicitud_Ejecucion ALTER COLUMN id_solicitud RESTART WITH 16;
ALTER TABLE Propuesta ALTER COLUMN id_propuesta RESTART WITH 11;
ALTER TABLE Contratacion ALTER COLUMN id_contratacion RESTART WITH 3;
ALTER TABLE Movimiento_financiero ALTER COLUMN id_movimiento RESTART WITH 5;
ALTER TABLE Resena ALTER COLUMN id_resena RESTART WITH 3;
ALTER TABLE Conversacion ALTER COLUMN id_conversacion RESTART WITH 6;
ALTER TABLE Mensaje ALTER COLUMN id_mensaje RESTART WITH 11;
ALTER TABLE Respuesta_resena ALTER COLUMN id_respuesta RESTART WITH 3;
ALTER TABLE Sancion ALTER COLUMN id_sancion RESTART WITH 3;
ALTER TABLE Segmentacion ALTER COLUMN id_segmento RESTART WITH 4;
ALTER TABLE Campana ALTER COLUMN id_campana RESTART WITH 3;
ALTER TABLE Envio_Campana ALTER COLUMN id_envio RESTART WITH 5;
ALTER TABLE Puntos_fidelidad ALTER COLUMN id_puntos RESTART WITH 4;
