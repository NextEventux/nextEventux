-- CU-029: historial de suspensiones y reactivaciones de cuentas de usuario.
--
-- Se llama Historial_suspension_usuario para no chocar con las tablas
-- Suspension_usuario y Reactivacion_usuario que creo una rama anterior,
-- abandonada, en algunas bases locales. Esas tablas no se usan ni se tocan.
-- Por la misma razon los constraints llevan el prefijo hist_susp_
-- (en H2 sus nombres son unicos en todo el esquema).
--
-- Cada fila es una suspension. Al reactivar la cuenta se completan
-- id_administrador_reactivacion y fecha_reactivacion; la fila nunca se borra.
-- usuario_vigente solo tiene valor mientras la suspension no se ha cerrado,
-- de modo que la restriccion UNIQUE impide dos suspensiones vigentes del
-- mismo usuario (UNIQUE admite varios NULL).
CREATE TABLE IF NOT EXISTS Historial_suspension_usuario (
    id_suspension INT PRIMARY KEY AUTO_INCREMENT,
    id_usuario INT NOT NULL,
    id_administrador INT NOT NULL,
    motivo VARCHAR(255) NOT NULL,
    fecha_suspension TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    id_administrador_reactivacion INT,
    fecha_reactivacion TIMESTAMP,
    usuario_vigente INT GENERATED ALWAYS AS
        (CASE WHEN fecha_reactivacion IS NULL THEN id_usuario END),
    CONSTRAINT fk_hist_susp_usuario
        FOREIGN KEY (id_usuario) REFERENCES Usuario(id_usuario),
    CONSTRAINT fk_hist_susp_administrador
        FOREIGN KEY (id_administrador) REFERENCES Usuario(id_usuario),
    CONSTRAINT fk_hist_susp_admin_reactivacion
        FOREIGN KEY (id_administrador_reactivacion) REFERENCES Usuario(id_usuario),
    CONSTRAINT chk_hist_susp_motivo CHECK (TRIM(motivo) <> ''),
    CONSTRAINT chk_hist_susp_reactivacion CHECK (
        (id_administrador_reactivacion IS NULL AND fecha_reactivacion IS NULL)
        OR (id_administrador_reactivacion IS NOT NULL AND fecha_reactivacion IS NOT NULL)),
    CONSTRAINT uq_hist_susp_vigente UNIQUE (usuario_vigente)
);
