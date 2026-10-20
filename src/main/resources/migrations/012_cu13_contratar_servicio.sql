-- CU-13: Contratar servicio.
-- La tabla Contratacion ya existe y pertenece a los servicios de proveedores
-- (la contratacion de organizadores de CU-26 usa Solicitud_Contratacion_Organizador).

-- Las condiciones de una propuesta admiten 1000 caracteres (CU-12),
-- pero Contratacion solo guardaba 500: se amplian para copiarlas completas.
ALTER TABLE Contratacion
    ALTER COLUMN condiciones SET DATA TYPE VARCHAR(1000);

-- Copia de la version exacta de la propuesta que se contrata.
-- El valor y las condiciones ya se guardan en valor_acordado y condiciones;
-- aqui se agrega la version y el alcance para que no dependan de la propuesta.
ALTER TABLE Contratacion
    ADD COLUMN IF NOT EXISTS numero_version_propuesta INT;

ALTER TABLE Contratacion
    ADD COLUMN IF NOT EXISTS alcance_propuesta VARCHAR(255);

-- Necesidad del evento que cubre la contratacion.
-- Se permite NULL para conservar las contrataciones anteriores a CU-13.
ALTER TABLE Contratacion
    ADD COLUMN IF NOT EXISTS id_necesidad_servicio INT;

ALTER TABLE Contratacion
    ADD CONSTRAINT IF NOT EXISTS fk_contratacion_necesidad_servicio
    FOREIGN KEY (id_necesidad_servicio)
    REFERENCES Necesidad_Servicio_Evento(id_necesidad_servicio);

-- Una propuesta solo puede contratarse una vez
CREATE UNIQUE INDEX IF NOT EXISTS uq_contratacion_propuesta
    ON Contratacion(id_propuesta);

-- Una necesidad solo puede cubrirse con una contratacion (E1 de CU-13).
-- Las contrataciones antiguas sin necesidad (NULL) no se ven afectadas.
CREATE UNIQUE INDEX IF NOT EXISTS uq_contratacion_necesidad
    ON Contratacion(id_necesidad_servicio);

CREATE INDEX IF NOT EXISTS idx_contratacion_evento_estado
    ON Contratacion(id_evento, estado);
