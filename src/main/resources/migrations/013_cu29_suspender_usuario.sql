
-- CU-029: historial de suspensiones de cuentas de usuario.
-- Conserva la trazabilidad de quién suspendió, a quién y por qué.

CREATE TABLE IF NOT EXISTS Suspension_usuario (
                                                  id_suspension INT PRIMARY KEY AUTO_INCREMENT,
                                                  id_usuario INT NOT NULL,
                                                  id_administrador INT NOT NULL,
                                                  motivo VARCHAR(500) NOT NULL,
    fecha TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_suspension_usuario
    FOREIGN KEY (id_usuario)
    REFERENCES Usuario(id_usuario),

    CONSTRAINT fk_suspension_administrador
    FOREIGN KEY (id_administrador)
    REFERENCES Usuario(id_usuario),

    CONSTRAINT chk_motivo_suspension
    CHECK (TRIM(motivo) <> '')
    );
