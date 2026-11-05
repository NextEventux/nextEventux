-- CU-12: material de apoyo opcional de cada propuesta (ruta de un archivo
-- o enlace). Cada versión conserva el suyo y no se modifica después.
ALTER TABLE Propuesta
    ADD COLUMN IF NOT EXISTS material_apoyo VARCHAR(1000);

-- CU-12 (A2): una solicitud de ajuste queda vinculada a la versión de la
-- propuesta sobre la que se pide el cambio. NULL para los mensajes normales.
ALTER TABLE Mensaje
    ADD COLUMN IF NOT EXISTS id_propuesta INT;

ALTER TABLE Mensaje
    ADD CONSTRAINT IF NOT EXISTS fk_mensaje_propuesta
    FOREIGN KEY (id_propuesta) REFERENCES Propuesta(id_propuesta);
