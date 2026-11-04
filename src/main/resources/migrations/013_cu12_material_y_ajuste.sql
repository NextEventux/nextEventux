-- CU-12: material de apoyo opcional de cada propuesta (ruta de un archivo
-- o enlace). Cada versión conserva el suyo y no se modifica después.
ALTER TABLE Propuesta
    ADD COLUMN IF NOT EXISTS material_apoyo VARCHAR(1000);
