# Regresiones de navegación local

- Fecha: 2026-09-03.
- Estado: `implementado` para pruebas JVM con fixtures sintéticos.
- Suite: PosShellControllerTest; :app:testDebugUnitTest correcto, 12/12.

## Cobertura agregada

- unknownBarcodePreservesExistingCartAndNextScanRecovers: un código desconocido
  conserva carrito, total y pantalla; el siguiente scan válido deja 2 aguas /
  ARS 300,00 y reemplaza el mensaje de error.
- cancellationCanBeDismissedThenConfirmedWithoutRestoringOldCart:
  Seguir comprando conserva el carrito; confirmar vuelve a bienvenida;
  Comenzar deja vacío / ARS 0,00 y el siguiente scan agrega 1 agua / ARS 150,00.
- repeatedScanResultsInAssistanceAreDiscardedNotDeferred: cinco resultados
  de lectura no cambian ningún campo del estado en asistencia; al volver
  conserva 1 agua / ARS 150,00 y el siguiente scan deja 2 / ARS 300,00.

## Límites

Se invoca el controlador directamente: no sustituye las pruebas HID/Compose,
no mide tiempos reales y no prueba persistencia tras reinicio.
No cambian reglas, UI, APK, contratos ni servicios. Integración real NO-GO.
El contexto oficial conserva la evidencia física y sus límites en
relevamientos/correccion-foco-scanner-2026-09-03.md.
