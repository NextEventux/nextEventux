-- Completa la fecha de envío de las tres propuestas de demostración cargadas por
-- versiones antiguas de seed.sql, que las insertaban sin fecha_envio.
--
-- Sin este dato, SincronizarHistoriaDemo busca cada propuesta por solicitud,
-- versión y fecha de envío; no la encuentra, pero sí existe otra con la misma
-- solicitud y versión, y detiene el arranque con un conflicto de identidad.
--
-- Solo se rellena una fecha_envio NULL en filas que coinciden exactamente con
-- la firma original: id, solicitud, versión, valor y descripción de la propuesta,
-- más evento, proveedor, servicio y fecha de la solicitud asociada. No se
-- modifican estados, condiciones ni otras columnas, ni propuestas editadas o
-- con fecha ya asignada. No se borra ni se crea ningún registro.

UPDATE Propuesta p SET fecha_envio = TIMESTAMP '2026-10-08 12:00:00'
WHERE p.id_propuesta = 1 AND p.fecha_envio IS NULL
  AND p.id_solicitud = 1 AND p.numero_version = 1
  AND p.valor = 1800000.00
  AND p.descripcion = 'Decoración completa para la boda'
  AND EXISTS (SELECT 1 FROM Solicitud_Ejecucion s
              WHERE s.id_solicitud = p.id_solicitud AND s.id_evento = 1
                AND s.id_proveedor = 1 AND s.id_servicio = 1
                AND s.fecha_solicitud = TIMESTAMP '2026-10-08 10:00:00');

UPDATE Propuesta p SET fecha_envio = TIMESTAMP '2026-10-08 13:00:00'
WHERE p.id_propuesta = 2 AND p.fecha_envio IS NULL
  AND p.id_solicitud = 2 AND p.numero_version = 1
  AND p.valor = 2200000.00
  AND p.descripcion = 'Servicio de catering y postres'
  AND EXISTS (SELECT 1 FROM Solicitud_Ejecucion s
              WHERE s.id_solicitud = p.id_solicitud AND s.id_evento = 1
                AND s.id_proveedor = 2 AND s.id_servicio = 3
                AND s.fecha_solicitud = TIMESTAMP '2026-10-08 11:30:00');

UPDATE Propuesta p SET fecha_envio = TIMESTAMP '2026-10-09 15:00:00'
WHERE p.id_propuesta = 3 AND p.fecha_envio IS NULL
  AND p.id_solicitud = 3 AND p.numero_version = 1
  AND p.valor = 900000.00
  AND p.descripcion = 'Decoración temática para baby shower'
  AND EXISTS (SELECT 1 FROM Solicitud_Ejecucion s
              WHERE s.id_solicitud = p.id_solicitud AND s.id_evento = 2
                AND s.id_proveedor = 1 AND s.id_servicio = 2
                AND s.fecha_solicitud = TIMESTAMP '2026-10-09 09:00:00');
