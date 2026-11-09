-- Completa el tipo de los cuatro servicios de demostración cargados por versiones
-- antiguas de seed.sql, que no incluían la columna tipo (agregada en la 002).
--
-- Sin este dato, SincronizarCatalogoDemo no reconoce esas filas como el catálogo
-- oficial y detiene el arranque con un conflicto de identidad.
--
-- Solo se rellena un tipo NULL en filas que coinciden exactamente con la firma
-- del seed: id, proveedor (por nombre de usuario), nombre, descripción y
-- categoría. Cualquier fila que difiera en un campo, o que ya tenga tipo, se
-- conserva sin cambios. No se borra ni se crea ningún registro.

UPDATE Servicio s SET tipo = 'Decoración'
WHERE s.id_servicio = 1 AND s.tipo IS NULL
  AND s.nombre = 'Decoración floral'
  AND s.descripcion = 'Decoración floral para eventos sociales'
  AND s.categoria = 'DECORACION'
  AND EXISTS (SELECT 1 FROM Proveedor p JOIN Usuario u ON u.id_usuario = p.id_usuario
              WHERE p.id_proveedor = s.id_proveedor AND u.usuario = 'decoraciones.arcoiris');

UPDATE Servicio s SET tipo = 'Decoración'
WHERE s.id_servicio = 2 AND s.tipo IS NULL
  AND s.nombre = 'Decoración temática'
  AND s.descripcion = 'Decoración personalizada según temática'
  AND s.categoria = 'DECORACION'
  AND EXISTS (SELECT 1 FROM Proveedor p JOIN Usuario u ON u.id_usuario = p.id_usuario
              WHERE p.id_proveedor = s.id_proveedor AND u.usuario = 'decoraciones.arcoiris');

UPDATE Servicio s SET tipo = 'Catering'
WHERE s.id_servicio = 3 AND s.tipo IS NULL
  AND s.nombre = 'Catering básico'
  AND s.descripcion = 'Servicio de alimentación para eventos'
  AND s.categoria = 'ALIMENTACION'
  AND EXISTS (SELECT 1 FROM Proveedor p JOIN Usuario u ON u.id_usuario = p.id_usuario
              WHERE p.id_proveedor = s.id_proveedor AND u.usuario = 'sabores.eventos');

UPDATE Servicio s SET tipo = 'Postres'
WHERE s.id_servicio = 4 AND s.tipo IS NULL
  AND s.nombre = 'Mesa de postres'
  AND s.descripcion = 'Postres variados para celebraciones'
  AND s.categoria = 'ALIMENTACION'
  AND EXISTS (SELECT 1 FROM Proveedor p JOIN Usuario u ON u.id_usuario = p.id_usuario
              WHERE p.id_proveedor = s.id_proveedor AND u.usuario = 'sabores.eventos');
