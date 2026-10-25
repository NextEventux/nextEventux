## Objetivo

Explique brevemente qué problema resuelve este Pull Request y por qué es necesario.

## Issue relacionado

Indique `Closes #número` si el PR resuelve un Issue existente
que debe cerrarse al fusionar.

Use `Refs #número` si solo está relacionado.

Si no existe un Issue, escriba `No aplica`.

## Módulo y caso de uso

- Módulo:
- Caso de uso: CU-__ / No aplica

## Tipo de cambio

- [ ] Nueva funcionalidad
- [ ] Corrección de error
- [ ] Pruebas
- [ ] Refactorización
- [ ] Documentación
- [ ] Configuración o dependencias

## Cambios realizados

<!-- Enumere solo los cambios relevantes para revisar este PR. -->

## Validación y pruebas

Indique las pruebas realizadas, cómo se ejecutaron y sus resultados.

- Pruebas automatizadas:
- Pruebas manuales o de integración:
- Comprobaciones adicionales:
- Pruebas no ejecutadas y motivo:

Incluya únicamente las comprobaciones que correspondan al cambio.
No basta con escribir "funciona".

## Evidencia

Indique la evidencia disponible para comprobar el cambio.

Puede incluir:
- Resultados de pruebas automatizadas.
- Pasos para reproducir y verificar el comportamiento.
- Capturas de pantalla, cuando aporten información útil.
- Registros de ejecución o pruebas de integración.

No es obligatorio adjuntar capturas en todos los PR.
La evidencia debe ser suficiente para revisar el cambio.

## Riesgos, impactos y decisiones

- Base de datos H2: No aplica / Describir
- Interfaz JavaFX: No aplica / Describir
- SRS o caso de uso: No aplica / Describir
- Otros módulos: No aplica / Describir
- Riesgos o trabajo pendiente: Ninguno / Describir

## Rama de destino

La rama de destino es `main`, conforme a `CONTRIBUTING.md`.

## Lista de verificación del autor

- [ ] El PR atiende un único objetivo principal.
- [ ] Los cambios siguen las convenciones acordadas en `CONTRIBUTING.md`.
- [ ] No incluí datos H2 locales, secretos, credenciales ni archivos generados.
- [ ] Ejecuté las pruebas aplicables y documenté sus resultados o expliqué las que no pude ejecutar.
- [ ] Incluí evidencia suficiente para revisar el cambio.
- [ ] Verifiqué que los cambios no afecten negativamente las funcionalidades relacionadas.

### Comprobaciones según corresponda

- [ ] Si existe un Issue relacionado, revisé sus criterios de aceptación.
- [ ] Si modifiqué reglas de negocio, agregué o actualicé las pruebas correspondientes.
- [ ] Si el cambio afecta la documentación técnica, la actualicé.
- [ ] Si modifiqué componentes compartidos o afecté otros módulos, identifiqué el impacto y avisé a sus responsables.
- [ ] Verifiqué que el estado y las horas faltantes de la tarea reflejen su avance real en `Control_de_Alcance_NextEventux.xlsx`.

**Nota:** Las comprobaciones condicionales pueden indicarse como «No aplica» cuando no correspondan. No es necesario crear un Issue, modificar documentación o agregar pruebas nuevas si el cambio no lo requiere.

## Revisión

Este PR requiere la aprobación de **un compañero distinto del autor**. Todas las conversaciones de revisión deben resolverse o quedar justificadas antes de fusionar.

### Para la persona revisora

- [ ] El cambio es comprensible y cumple su objetivo.
- [ ] El alcance corresponde a la tarea y, si existe, al Issue relacionado.
- [ ] Las pruebas y evidencias son suficientes para el tipo de cambio.
- [ ] Se revisaron los posibles efectos sobre otros módulos o documentos.
- [ ] Las observaciones de la revisión quedaron resueltas o justificadas.

La aprobación debe realizarla un integrante distinto del autor,
antes de integrar el PR a `main`.
