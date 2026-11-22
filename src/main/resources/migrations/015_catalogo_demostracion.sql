CREATE TABLE IF NOT EXISTS Registro_demo_catalogo (
    tipo VARCHAR(20) NOT NULL,
    clave VARCHAR(100) NOT NULL,
    id_registro INT NOT NULL,
    PRIMARY KEY (tipo, clave),
    UNIQUE (tipo, id_registro)
);
