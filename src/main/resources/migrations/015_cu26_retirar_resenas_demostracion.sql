
-- CU-026: retirar las reseñas de organizadores creadas como demostración.
--
-- Ninguna funcionalidad del sistema registra reseñas de organizadores; las
-- únicas filas de Resena_Organizador provienen de la migración 011 y de
-- versiones de seed.sql. Esta migración borra solo esas filas, identificadas
-- por su firma completa: organizador (id y nombre de usuario), cliente,
-- calificación y comentario exactos, más la fecha fija de la 011 o el nombre
-- de usuario del cliente del seed. Cualquier fila que difiera en un campo se
-- conserva. No se modifican usuarios, organizadores ni solicitudes, y la
-- migración 011 no se edita porque ya fue aplicada.

-- Insertadas por la migración 011 (fecha literal fija).
DELETE FROM Resena_Organizador r
WHERE r.id_organizador = 1 AND r.id_cliente = 5 AND r.calificacion = 5
  AND r.comentario = 'Buena organizacion y comunicacion durante el evento.'
  AND r.fecha = TIMESTAMP '2026-10-10 10:00:00'
  AND EXISTS (SELECT 1 FROM Organizador o JOIN Usuario u ON u.id_usuario = o.id_usuario
              WHERE o.id_organizador = r.id_organizador AND u.usuario = 'laura.gomez');

DELETE FROM Resena_Organizador r
WHERE r.id_organizador = 2 AND r.id_cliente = 6 AND r.calificacion = 4
  AND r.comentario = 'La celebracion estuvo bien organizada.'
  AND r.fecha = TIMESTAMP '2026-10-11 11:00:00'
  AND EXISTS (SELECT 1 FROM Organizador o JOIN Usuario u ON u.id_usuario = o.id_usuario
              WHERE o.id_organizador = r.id_organizador AND u.usuario = 'carlos.rodriguez');

-- Insertadas por seed.sql (la fecha la asignó la base al cargar los ejemplos).
DELETE FROM Resena_Organizador r
WHERE r.id_organizador = 1 AND r.id_cliente = 5 AND r.calificacion = 5
  AND r.comentario = 'Buena organizacion y comunicacion durante el evento.'
  AND EXISTS (SELECT 1 FROM Organizador o JOIN Usuario u ON u.id_usuario = o.id_usuario
              WHERE o.id_organizador = r.id_organizador AND u.usuario = 'laura.gomez')
  AND EXISTS (SELECT 1 FROM Usuario c
              WHERE c.id_usuario = r.id_cliente AND c.usuario = 'ana.martinez');

DELETE FROM Resena_Organizador r
WHERE r.id_organizador = 2 AND r.id_cliente = 6 AND r.calificacion = 4
  AND r.comentario = 'La celebracion estuvo bien organizada.'
  AND EXISTS (SELECT 1 FROM Organizador o JOIN Usuario u ON u.id_usuario = o.id_usuario
              WHERE o.id_organizador = r.id_organizador AND u.usuario = 'carlos.rodriguez')
  AND EXISTS (SELECT 1 FROM Usuario c
              WHERE c.id_usuario = r.id_cliente AND c.usuario = 'diego.perez');

DELETE FROM Resena_Organizador r
WHERE r.id_organizador = 3 AND r.id_cliente = 5 AND r.calificacion = 5
  AND r.comentario = 'Excelente atencion y organizacion.'
  AND EXISTS (SELECT 1 FROM Organizador o JOIN Usuario u ON u.id_usuario = o.id_usuario
              WHERE o.id_organizador = r.id_organizador AND u.usuario = 'valentina')
  AND EXISTS (SELECT 1 FROM Usuario c
              WHERE c.id_usuario = r.id_cliente AND c.usuario = 'ana.martinez');

DELETE FROM Resena_Organizador r
WHERE r.id_organizador = 4 AND r.id_cliente = 6 AND r.calificacion = 4
  AND r.comentario = 'Buen servicio para eventos empresariales.'
  AND EXISTS (SELECT 1 FROM Organizador o JOIN Usuario u ON u.id_usuario = o.id_usuario
              WHERE o.id_organizador = r.id_organizador AND u.usuario = 'santiago')
  AND EXISTS (SELECT 1 FROM Usuario c
              WHERE c.id_usuario = r.id_cliente AND c.usuario = 'diego.perez');

DELETE FROM Resena_Organizador r
WHERE r.id_organizador = 5 AND r.id_cliente = 5 AND r.calificacion = 5
  AND r.comentario = 'Una celebracion muy bien organizada.'
  AND EXISTS (SELECT 1 FROM Organizador o JOIN Usuario u ON u.id_usuario = o.id_usuario
              WHERE o.id_organizador = r.id_organizador AND u.usuario = 'juliana')
  AND EXISTS (SELECT 1 FROM Usuario c
              WHERE c.id_usuario = r.id_cliente AND c.usuario = 'ana.martinez');

DELETE FROM Resena_Organizador r
WHERE r.id_organizador = 3 AND r.id_cliente = 7 AND r.calificacion = 5
  AND r.comentario = 'Una celebracion muy bien organizada.'
  AND EXISTS (SELECT 1 FROM Organizador o JOIN Usuario u ON u.id_usuario = o.id_usuario
              WHERE o.id_organizador = r.id_organizador AND u.usuario = 'valentina')
  AND EXISTS (SELECT 1 FROM Usuario c
              WHERE c.id_usuario = r.id_cliente AND c.usuario = 'paula.rios');
