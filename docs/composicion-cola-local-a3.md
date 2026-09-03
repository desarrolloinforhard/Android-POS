# Composición de cola local A3

- Estado: `piloto`.
- Fecha: 2026-09-03.
- Prueba instrumentada: SyntheticPilotDatabaseTest.reopenedQueueRequiresReconciliationBeforeSendingOtherCommands.

## Alcance

Compone RoomCommandRepository, CommandDispatchPlanner y CommandCoordinator
con transportes fake en el APK de pruebas, no en la app.
La dependencia adicional core:network es sólo androidTestImplementation.
No cambia esquema, datos comerciales ni código de producción.

## Escenario comprobado

1. Persistir metadata sintética SENDING y QUEUED con identidades distintas.
2. Reabrir la base: el plan convierte SENDING en UNCERTAIN y bloquea QUEUED.
3. Reconciliación fake incierta mantiene el bloqueo y ningún envío.
4. Reconciliación fake confirmada permite planificar sólo el segundo comando.
5. El transporte fake verifica SENDING persistido antes de confirmar.
6. Otra reapertura conserva RECONCILED y ACKNOWLEDGED, mismas identidades
   y claves idempotentes, sin comandos para reenviar.

## Evidencia y límites

- Compilación :core:persistence:assembleDebugAndroidTest correcta.
- :core:sync:testDebugUnitTest correcto (UP-TO-DATE, sin nueva ejecución).
- Primer intento de instalación normal rechazado: INSTALL_FAILED_VERIFICATION_FAILURE.
  Reintento solicitado por el usuario aceptado, sin desactivar verificadores.
- Suite ampliada ejecutada en X-3566: OK (10 tests), 1,997 s, incluida la
  composición de recuperación y reconciliación. Paquete auxiliar retirado después.
- No se desactivaron verificadores ni se cambió la app POS.
- Cierre/reapertura de base no equivale a muerte real de proceso o corte eléctrico.
- El orden lo aplica explícitamente la prueba usando el plan: no demuestra
  exclusión concurrente ni protección contra consumidores que ignoren el plan.
- WorkManager/delegado integrado no se prueba todavía.
- Sin tráfico HTTP, contratos nuevos ni autorización productiva.
