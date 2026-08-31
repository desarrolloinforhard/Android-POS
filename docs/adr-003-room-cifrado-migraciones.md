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
- Ejecución Room en X-3566: pendiente de confirmar; Package Manager no completó
  la instalación del APK de pruebas. No se reinició ni alteró el terminal.
