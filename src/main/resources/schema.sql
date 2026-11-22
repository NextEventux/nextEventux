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

CREATE TABLE IF NOT EXISTS Registro_demo_catalogo (
    tipo VARCHAR(20) NOT NULL,
    clave VARCHAR(100) NOT NULL,
    id_registro INT NOT NULL,
    PRIMARY KEY (tipo, clave),
    UNIQUE (tipo, id_registro)
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
