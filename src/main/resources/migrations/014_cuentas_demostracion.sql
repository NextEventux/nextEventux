-- Actualiza solamente las cuentas oficiales de demostración
-- Se comprueba el ID, el usuario y el actor para evitar modificar otra cuenta

UPDATE Usuario SET contraseña = '1234'
WHERE id_usuario = 1 AND usuario = 'laura.gomez' AND tipo_usuario = 'ORGANIZADOR';

UPDATE Usuario SET contraseña = '1234'
WHERE id_usuario = 2 AND usuario = 'carlos.rodriguez' AND tipo_usuario = 'ORGANIZADOR';

UPDATE Usuario SET contraseña = '1234'
WHERE id_usuario = 8 AND usuario = 'valentina' AND tipo_usuario = 'ORGANIZADOR';

UPDATE Usuario SET contraseña = '1234'
WHERE id_usuario = 3 AND usuario = 'decoraciones.arcoiris' AND tipo_usuario = 'PROVEEDOR';

UPDATE Usuario SET contraseña = '1234'
WHERE id_usuario = 4 AND usuario = 'sabores.eventos' AND tipo_usuario = 'PROVEEDOR';

UPDATE Usuario SET contraseña = '1234'
WHERE id_usuario = 13 AND usuario = 'flash' AND tipo_usuario = 'PROVEEDOR';

UPDATE Usuario SET contraseña = '1234'
WHERE id_usuario = 5 AND usuario = 'ana.martinez' AND tipo_usuario = 'CLIENTE';

UPDATE Usuario SET contraseña = '1234'
WHERE id_usuario = 6 AND usuario = 'diego.perez' AND tipo_usuario = 'CLIENTE';

UPDATE Usuario SET contraseña = '1234'
WHERE id_usuario = 7 AND usuario = 'paula.rios' AND tipo_usuario = 'CLIENTE';

UPDATE Usuario SET contraseña = '1234'
WHERE id_usuario = 14 AND usuario = 'camila' AND tipo_usuario = 'ADMINISTRADOR';

UPDATE Usuario SET contraseña = '1234'
WHERE id_usuario = 15 AND usuario = 'mateo' AND tipo_usuario = 'ADMINISTRADOR';

UPDATE Usuario SET contraseña = '1234'
WHERE id_usuario = 16 AND usuario = 'shirly' AND tipo_usuario = 'ADMINISTRADOR';
