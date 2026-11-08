# Guion de demostración

## Datos de demostración y bases existentes

Cada integrante utiliza su propia base H2 local.

- En una base sin datos de negocio, la aplicación carga `seed.sql`.
- En una base existente no vuelve a cargar ese archivo. Al iniciar, sincroniza el conjunto oficial descrito abajo, sin restablecer las contraseñas de cuentas existentes.
- La sincronización no copia todos los datos del seed a las bases antiguas. Los registros no incluidos en los sincronizadores —por ejemplo, movimientos financieros, mensajes, invitaciones, invitados y regalos— aparecen automáticamente solo cuando se carga el seed en una base vacía.
- Las cuentas, perfiles y demás registros personales de cada integrante permanecen en su H2 local; Git no comparte los archivos de base de datos.
- No borres, reemplaces ni reinicies tu H2 para obtener los ejemplos oficiales. Si hay un conflicto de identidad o procedencia, la inicialización puede detenerse y requerir revisión manual; el sincronizador no resuelve toda inconsistencia automáticamente.

Al iniciar la aplicación se sincronizan:

- Las 12 cuentas oficiales y sus roles y perfiles que falten. En una H2 nueva, las cuentas del seed usan `1234`; una cuenta oficial que ya existía conserva su contraseña actual, por lo que `1234` no necesariamente funcionará para ella.
- Los cuatro eventos y quince servicios oficiales. Se resuelven sus propietarios e IDs desde los datos existentes; los datos personales no se reemplazan. Los conflictos incompatibles o ambiguos requieren revisión.
- Ocho necesidades, quince solicitudes, diez propuestas y dos contrataciones oficiales.
- El horario del 20 de febrero de 2027, de 14:00 a 23:59, para la cobertura fotográfica necesaria al contratar la propuesta seleccionada de los quince años de Sofía. No se sincronizan las otras disponibilidades del seed a H2 existentes. Si ya hay un horario exactamente igual, se conserva; si hay uno que se solapa, no se modifica ni se duplica y se muestra un aviso. Una procedencia inconsistente requiere revisión.

En una H2 nueva, `seed.sql` también incluye once horarios y datos de otros módulos, como movimientos financieros, conversaciones y mensajes, invitaciones, invitados y regalos. Esos conjuntos no se incorporan automáticamente a una H2 existente mediante esta sincronización.

## Cuentas

Las cuentas oficiales disponibles para demostración son:

| Actor | Usuarios |
| --- | --- |
| Organizador | `laura.gomez`, `carlos.rodriguez`, `valentina` |
| Proveedor | `decoraciones.arcoiris`, `sabores.eventos`, `flash` |
| Cliente | `ana.martinez`, `diego.perez`, `paula.rios` |
| Administrador | `camila`, `mateo`, `shirly` |

La clave inicial `1234` aplica a cuentas creadas por el seed o por el sincronizador. Si el usuario oficial ya existía en la H2, se conserva su clave.

## Ejemplos sincronizados para proveedores y solicitudes

Cada proveedor tiene cinco servicios oficiales. Sus estados y la cantidad de solicitudes varían:

| Servicios (CU-11) | Dónde verlo |
| --- | --- |
| Disponibles | `flash`: cobertura fotográfica, video y cabina de fotos |
| En borrador | `flash`: sesión de retratos. Sirve para probar «Revisar y publicar» |
| Retirado | `flash`: álbum impreso premium |
| Preguntas del servicio | El seed las contiene para una H2 nueva; no se sincronizan en H2 existentes |

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
   todas las pantallas (ver `navegacion-compartida.md`).

## Límites del escenario

La propuesta de fotografía corresponde al evento del 20 de febrero de 2027. La
contratación requiere que el evento siga vigente y que el horario cubra el evento;
la fecha oficial está definida tanto en el seed como en el sincronizador. Si deja
de servir para una demostración, no cambies solo `seed.sql`: cualquier ajuste debe
coordinarse con el equipo y sus pruebas.

Los ejemplos muestran los flujos que están implementados, pero no significan que
todos los módulos o casos de uso del proyecto estén terminados. Otras
funcionalidades pueden seguir en desarrollo por sus responsables.
