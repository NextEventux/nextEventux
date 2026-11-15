-- CU-22: Crear campaña de marketing.

-- Las campañas del CU-22 las crea un administrador para un segmento, sin proveedor.
-- Las campañas de proveedores ya existentes conservan su id_proveedor.
ALTER TABLE Campana ALTER COLUMN id_proveedor SET NULL;

ALTER TABLE Campana ADD COLUMN IF NOT EXISTS id_usuario INT;
ALTER TABLE Campana ADD COLUMN IF NOT EXISTS id_segmento INT;
ALTER TABLE Campana ADD COLUMN IF NOT EXISTS tipo_oferta VARCHAR(100);
ALTER TABLE Campana ADD COLUMN IF NOT EXISTS presupuesto_maximo DECIMAL(12,2);
ALTER TABLE Campana ADD COLUMN IF NOT EXISTS estado VARCHAR(20);
ALTER TABLE Campana ADD COLUMN IF NOT EXISTS visualizaciones BIGINT DEFAULT 0 NOT NULL;
ALTER TABLE Campana ADD COLUMN IF NOT EXISTS conversiones BIGINT DEFAULT 0 NOT NULL;

ALTER TABLE Campana ADD CONSTRAINT IF NOT EXISTS fk_campana_usuario
    FOREIGN KEY (id_usuario) REFERENCES Usuario(id_usuario);
ALTER TABLE Campana ADD CONSTRAINT IF NOT EXISTS fk_campana_segmento
    FOREIGN KEY (id_segmento) REFERENCES Segmentacion(id_segmento);

-- Presupuesto de marketing que aún no está comprometido en campañas (una sola fila, id 1).
-- La migración no inserta datos: sin fila, el disponible es 0 y la base sigue contando como vacía.
CREATE TABLE IF NOT EXISTS Presupuesto_marketing (
    id_presupuesto INT PRIMARY KEY,
    disponible DECIMAL(12,2) NOT NULL CHECK (disponible >= 0)
);