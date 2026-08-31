# ADR-003 - Room, cifrado y migraciones

- Estado: `piloto`
- Alcance: persistencia estructurada local Android

Room se adopta para un spike con datos exclusivamente sintéticos, esquemas
exportados y migraciones explícitas forward-only. Se prohíben migraciones
destructivas.

Android Keystore no cifra automáticamente Room. Hasta aprobar y validar un
proveedor de cifrado no se persistirán tokens, bootstrap, contraseñas, claves,
payloads comerciales ni otros secretos.

El dominio seguirá independiente de Room y la apertura de base quedará detrás
de una fábrica compatible con un futuro proveedor de cifrado. Las entidades
locales no son contratos API ni tablas comerciales.

Permanecen pendientes de confirmar el proveedor de cifrado, rotación de clave,
retención, límites, downgrade, volumen de catálogo, ventana de rollback y los
mappings derivados de PS-1/PS-3/PS-6.

El spike se considerará cerrado cuando existan una base sintética, una migración
V1→V2 probada, esquemas exportados, una prueba transaccional y ninguna ruta HTTP
o secreto persistido.

## Evidencia del spike

- Módulo aislado `core:persistence` y base Room V2: `implementado`.
- Esquema V2 exportado y APK de pruebas instrumentadas: `implementado`.
- Migración V1→V2 y rollback transaccional: `implementado` en pruebas.
- Compilación completa, pruebas JVM y lint: comprobados.
- Ejecución Room en X-3566 con Android 11: `implementado`; 2/2 pruebas correctas.
  Package Manager requirió instalación ADB no streaming y el paquete temporal
  se retiró al finalizar.
- Base V3 con metadata de snapshots exclusivamente sintética, publicación
  atómica y rollback local: `implementado`; no representa catálogo ni Pricing
  productivos. Las 5 pruebas V3 pasaron en X-3566 con Android 11.
- Incremento V4: outbox durable sin payload comercial; sólo UUID local,
  `Idempotency-Key` y estado. La misma clave no puede mutar y `uncertain` debe
  sobrevivir el cierre y reapertura de la base: `implementado`. Las 8 pruebas
  instrumentadas V4 pasaron en X-3566 con Android 11. Para destrabar el
  verificador externo del firmware se desactivó sólo durante la instalación
  ADB y sus ajustes se restauraron al estado previo; el APK temporal se retiró.
