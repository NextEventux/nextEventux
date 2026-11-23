# Guion de demostración

## Datos de demostración y bases existentes

Cada integrante utiliza su propia base H2 local.

- En una base sin tablas, `CargarDatosEjemplo` ejecuta `data.sql` para crear el esquema y cargar los datos de demostración.
- En una base existente con el esquema completo, el inicio normal conserva sus datos y no vuelve a ejecutar `data.sql`.
- Si existen tablas pero falta alguna de las esperadas, el inicio informa cuáles faltan y no modifica la base.
- Las cuentas, perfiles y demás registros personales de cada integrante permanecen en su H2 local; Git no comparte los archivos de base de datos.
- Para reconstruir una base de desarrollo, conecta un cliente H2 a una URL de desarrollo y ejecuta `data.sql` manualmente. El archivo elimina todas las tablas de esa base; no uses la ruta de tu H2 personal.

## Cuentas

Las cuentas oficiales disponibles para demostración son:

| Actor | Usuarios |
| --- | --- |
| Organizador | `laura.gomez`, `carlos.rodriguez`, `valentina` |
| Proveedor | `decoraciones.arcoiris`, `sabores.eventos`, `flash` |
| Cliente | `ana.martinez`, `diego.perez`, `paula.rios` |
| Administrador | `camila`, `mateo`, `shirly` |

La clave inicial `1234` aplica a las cuentas de demostración cargadas desde `data.sql`. El inicio normal no cambia las contraseñas de una base existente.

## Ejemplos incluidos en data.sql

Al cargar `data.sql` en una base nueva o reconstruida, cada proveedor tiene cinco servicios de demostración. Sus estados y la cantidad de solicitudes varían:

| Servicios (CU-11) | Dónde verlo |
| --- | --- |
| Disponibles | `flash`: cobertura fotográfica, video y cabina de fotos |
| En borrador | `flash`: sesión de retratos. Sirve para probar «Revisar y publicar» |
| Retirado | `flash`: álbum impreso premium |
| Preguntas del servicio | Se cargan desde `data.sql` en una base nueva o reconstruida |

| Solicitudes (CU-12) | Dónde verlo |
| --- | --- |
| Pendiente de responder | `decoraciones.arcoiris`: decoración temática para los quince de Sofía |
| En negociación con propuesta | `sabores.eventos`: catering gourmet |
| Propuesta seleccionada | `flash`: cobertura fotográfica para los quince de Sofía |
| Rechazada por el proveedor | `flash`: cobertura fotográfica para la graduación |
| Propuesta rechazada por el organizador | `decoraciones.arcoiris`: arreglos de mesa |

| Contratación (CU-13) | Dónde verlo |
| --- | --- |
| Propuesta seleccionada, lista para contratar | `valentina`: cobertura fotográfica de 8 horas |
| Contratadas | `laura.gomez`: decoración y catering de la boda |

## Recorrido sugerido

1. **CU-11.** Entra como `flash`, abre «Mis servicios» y elige «Sesión de
   retratos». Pulsa «Revisar y publicar»: si faltan horarios, agrega uno y vuelve
   a revisar el servicio.
2. **CU-11, retiro.** Con `decoraciones.arcoiris`, intenta retirar «Decoración
   temática»: tiene una solicitud en curso y el aviso aparece en la pantalla.
3. **CU-12, proveedor.** Con `decoraciones.arcoiris`, abre «Solicitudes
   recibidas», elige la decoración temática y envía una propuesta con resumen.
4. **CU-12, organizador.** Con `valentina`, revisa las propuestas recibidas y
   solicita un ajuste a una propuesta disponible.
5. **CU-13.** Con `valentina`, elige la propuesta «Seleccionada para
   contratación» de la cobertura fotográfica y pulsa «Contratar servicio».
   La pantalla ya muestra la disponibilidad verificada y el presupuesto antes y
   después. Confirma la contratación.
6. **Navegación.** Con cada rol revisa que el menú lateral se mantiene igual en
   todas las pantallas.

## Límites del escenario

La propuesta de fotografía corresponde al evento del 20 de febrero de 2027. La
contratación requiere que el evento siga vigente y que el horario cubra el evento;
la fecha oficial está definida en `data.sql`. Si deja de servir para una demostración,
coordina el ajuste con el equipo y sus pruebas.

Los ejemplos muestran los flujos que están implementados, pero no significan que
todos los módulos o casos de uso del proyecto estén terminados. Otras
funcionalidades pueden seguir en desarrollo por sus responsables.
