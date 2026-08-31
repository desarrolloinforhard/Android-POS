# ADR-004 - WorkManager y reintentos locales

- Estado: `piloto`
- Alcance: scheduling local Android sin integración HTTP

Se incorpora `core:work` como adapter aislado. El trabajo es único y utiliza
`ExistingWorkPolicy.KEEP`, restricción `NetworkType.CONNECTED` y backoff
exponencial inicial de 30 segundos. Estar conectado no concede capacidad
offline ni autorización de negocio.

El worker no crea repositorios, clientes HTTP ni endpoints. Sólo puede operar
con un `CommandWorkDelegate` inyectado mediante `CommandSyncWorkerFactory`. El
módulo no está conectado a `app`; por lo tanto no agenda ni ejecuta trabajo en
runtime.

Antes de cualquier worker, `CommandDispatchPlanner` recupera `SENDING` como
`UNCERTAIN`. Si existe al menos un comando incierto, el plan contiene sólo
reconciliaciones y bloquea el envío de comandos `QUEUED`.

WorkManager 2.10.5 se usa como versión `piloto` compatible con Kotlin 1.9.24 y
`compileSdk 35`. La versión estable 2.11.2 fue evaluada y rechazada para este
incremento porque su metadata Kotlin 2.1 no es compatible con el compilador
actual. No se habilitó `-Xskip-metadata-version-check`. Una actualización exige
una migración explícita y verificada del toolchain.

Permanecen `pendiente de confirmar` el límite de intentos, jitter, ventanas,
política de batería, respuesta a revocación y resultados de reconciliación.
Nada de este ADR habilita integración real, venta, pago o fiscalidad.

## Evidencia

- Compilación completa y lint: correctos.
- Pruebas instrumentadas en X-3566 con Android 11: 2/2 correctas.
- `COMPLETE`, `RETRY` y `BLOCKED` se traducen a resultados WorkManager sin
  conocimiento de transporte.
- Dos schedules consecutivos con `KEEP` conservan un único trabajo pendiente.
- El APK temporal se retiró y los ajustes del verificador de instalación se
  restauraron al estado previo.
