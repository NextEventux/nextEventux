
-- CU-029: historial de reactivaciones de cuentas de usuario.
-- Conserva la trazabilidad de quién reactivó, a quién y cuándo.

CREATE TABLE IF NOT EXISTS Reactivacion_usuario (
                                                    id_reactivacion INT PRIMARY KEY AUTO_INCREMENT,
                                                    id_usuario INT NOT NULL,
                                                    id_administrador INT NOT NULL,
                                                    motivo VARCHAR(500),
    fecha TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_reactivacion_usuario
    FOREIGN KEY (id_usuario)
    REFERENCES Usuario(id_usuario),

    CONSTRAINT fk_reactivacion_administrador
    FOREIGN KEY (id_administrador)
    REFERENCES Usuario(id_usuario),

    CONSTRAINT chk_motivo_reactivacion
    CHECK (motivo IS NULL OR TRIM(motivo) <> '')
    );
