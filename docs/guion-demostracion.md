# Guion de demostración

## Actualización de los datos de demostración

Cada integrante utiliza su propia base H2 local.

- En una base vacía, NextEventux carga los ejemplos de `seed.sql`.
- En una base existente, los registros se conservan. No se ejecuta nuevamente todo `seed.sql`.
- Al iniciar, NextEventux agrega cuentas oficiales faltantes y sus roles y perfiles sin reemplazar contraseñas ni datos existentes.
- También agrega los cuatro eventos y quince servicios oficiales faltantes. Sus propietarios se resuelven desde las cuentas y perfiles actuales, no desde los IDs de `seed.sql`.
- El catálogo registra la procedencia de los ejemplos para que las siguientes sincronizaciones reconozcan los registros aunque se hayan editado. Nunca reemplaza sus precios, estados, fechas ni otros valores.
- Los registros existentes sin procedencia solo se adoptan si coinciden sus campos descriptivos de identidad y propietario. Un homónimo incompatible o una identidad ambigua detiene la sincronización sin crear registros parciales; si esos campos ya se editaron antes de la primera sincronización, se requiere resolver el conflicto manualmente.
- Esta etapa no sincroniza solicitudes, propuestas, contrataciones, invitaciones ni otras tablas dependientes.
- Las cuentas oficiales nuevas usan la contraseña inicial `1234`; las contraseñas de cuentas ya existentes se conservan.
- `014_cuentas_demostracion.sql` se conserva como migración histórica, pero no se aplica ni restablece contraseñas.
- Los datos locales no identificados como ejemplos oficiales no se comparten mediante `git pull`.
- Cuando se agreguen ejemplos oficiales para nuevos casos de uso, deberán incluirse actualizaciones versionadas y pruebas para bases existentes.

Después de actualizar el repositorio, cada integrante debe iniciar la aplicación para que se sincronicen las cuentas, eventos y servicios oficiales pendientes.


Los datos de ejemplo se cargan automáticamente cuando la base está vacía.
Si ya existe una base local, NextEventux conserva sus registros y aplica
las actualizaciones de demostración que estén implementadas y pendientes.
Esto no significa que todo cambio posterior en `seed.sql` se copie
automáticamente a las bases existentes.

## Cuentas

Todas las cuentas usan la clave `1234`.

| Actor | Usuarios |
| --- | --- |
| Organizador | `laura.gomez`, `carlos.rodriguez`, `valentina` |
| Proveedor | `decoraciones.arcoiris`, `sabores.eventos`, `flash` |
| Cliente | `ana.martinez`, `diego.perez`, `paula.rios` |
| Administrador | `camila`, `mateo`, `shirly` |

## Qué hay cargado para los proveedores

Cada proveedor tiene cinco servicios y al menos cinco solicitudes.

| Servicios (CU-11) | Dónde verlo |
| --- | --- |
| Publicados con horarios | `flash`: cobertura fotográfica, video y cabina de fotos |
| En borrador, sin horarios | `flash`: sesión de retratos. Sirve para probar «Revisar y publicar» |
| Retirados | `flash`: álbum impreso premium |
| Con preguntas pendientes y respondidas | cobertura fotográfica de `flash` |

| Solicitudes (CU-12) | Dónde verlo |
| --- | --- |
| Pendiente de responder | `decoraciones.arcoiris`: decoración temática de unicornios |
| En negociación con propuesta enviada | `sabores.eventos`: catering gourmet |
| Con ajuste pedido y nueva versión | `flash`: cobertura del cumpleaños (conversación) |
| Rechazada por el proveedor | `sabores.eventos`: catering básico |
| Propuesta rechazada por el organizador | `decoraciones.arcoiris`: arreglos de mesa |

| Contratación (CU-13) | Dónde verlo |
| --- | --- |
| Propuesta seleccionada, lista para contratar | `valentina`: cobertura fotográfica de 8 horas |
| Contratadas | `laura.gomez`: decoración y catering de la boda |

## Recorrido sugerido

1. **CU-11.** Entra como `flash`, abre «Mis servicios» y elige «Sesión de
   retratos». Pulsa «Revisar y publicar»: la pantalla avisa que faltan horarios.
   Agrega un horario y vuelve a revisar; ahora se puede publicar.
2. **CU-11, retiro.** Con `decoraciones.arcoiris`, intenta retirar «Decoración
   temática»: tiene una solicitud en curso y el aviso aparece en la pantalla.
3. **CU-12, proveedor.** Con `decoraciones.arcoiris`, abre «Solicitudes
   recibidas», elige la decoración temática y envía una propuesta con resumen.
4. **CU-12, organizador.** Con `valentina`, abre «Propuestas recibidas». Elige
   la mesa de postres y pulsa «Solicitar ajuste»; el ajuste queda en la
   conversación.
5. **CU-13.** Con `valentina`, elige la propuesta «Seleccionada para
   contratación» de la cobertura fotográfica y pulsa «Contratar servicio».
   La pantalla ya muestra la disponibilidad verificada y el presupuesto antes y
   después. Confirma la contratación.
6. **Navegación.** Con cada rol revisa que el menú lateral se mantiene igual en
   todas las pantallas (ver `navegacion-compartida.md`).

## Fechas

Los eventos nuevos son del 20 de febrero y el 13 de marzo de 2027. La
contratación exige que el evento no haya terminado, así que si la demostración
se hace después de esas fechas hay que mover los eventos 3 y 4 y los horarios
101 a 111 del archivo `seed.sql`.
