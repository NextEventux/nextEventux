-- Misma estructura de disponibilidad definida por CU11; no cambia sus registros.
CREATE TABLE IF NOT EXISTS Disponibilidad_Servicio (
    id_disponibilidad INT PRIMARY KEY AUTO_INCREMENT,
    id_servicio INT NOT NULL,
    fecha DATE NOT NULL,
    hora_inicio TIME NOT NULL,
    hora_fin TIME NOT NULL,
    estado VARCHAR(30) DEFAULT 'DISPONIBLE',
    FOREIGN KEY (id_servicio) REFERENCES Servicio(id_servicio)
);
ALTER TABLE Contratacion ADD COLUMN IF NOT EXISTS id_disponibilidad INT REFERENCES Disponibilidad_Servicio(id_disponibilidad);
ALTER TABLE Evento ADD COLUMN IF NOT EXISTS id_lugar INT REFERENCES Lugar(id_lugar);

-- Compatibilidad con bases locales que tengan un esquema anterior.
ALTER TABLE Notificacion_interna
    ADD COLUMN IF NOT EXISTS id_usuario INT
    REFERENCES Usuario(id_usuario);

-- El destinatario puede ser un invitado sin cuenta de usuario.
ALTER TABLE Notificacion_interna
    ALTER COLUMN id_usuario DROP NOT NULL;

-- Invitado no tiene cuenta Usuario en el modelo actual.
ALTER TABLE Notificacion_interna ADD COLUMN IF NOT EXISTS id_invitado INT REFERENCES Invitado(id_invitado);
ALTER TABLE Notificacion_interna ADD COLUMN IF NOT EXISTS id_evento INT REFERENCES Evento(id_evento);