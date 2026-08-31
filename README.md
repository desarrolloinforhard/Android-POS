# Android POS

Cliente Android POS de Inforhard en etapa de laboratorio local.

## Estado

- Integración con servicios reales: `NO-GO`.
- No se consumen rutas `/internal/lab`.
- No existen endpoints configurados.
- El transporte disponible es exclusivamente fake.
- `applicationId`: `com.inforhard.pos`.
- `minSdk 25`: decisión inicial sujeta a validación con hardware real.

## Módulos iniciales

- `app`: shell Compose.
- `core:model`: modelos locales sin contratos comerciales.
- `core:domain`: políticas y máquina de estados.
- `core:network`: interfaz de transporte y fake local.
- `core:security`: interfaz para identidad protegida por Keystore.
- `core:hardware`: contratos de hardware y compatibilidad inicial HID.
- `core:sync`: coordinación local de cola y reconciliación contra fakes.
- `core:persistence`: spike Room aislado con datos sintéticos y migraciones.
- `core:work`: adapter WorkManager aislado, sin conexión a la app ni transporte.

## Seguridad local implementada

- Adapter Android Keystore con clave EC privada no exportable.
- Preferencia por StrongBox en Android 9 o superior y fallback explícito al
  proveedor Android Keystore cuando StrongBox no está disponible.
- Firma local `SHA256withECDSA`; el formato PoP remoto continúa pendiente de
  contrato y no está implementado.
- `Idempotency-Key` local con 128 bits completos codificados en hexadecimal.
- Estado local separado para identidad del dispositivo y sesión del operador.
- Contexto de empresa, sucursal y terminal representado como derivado por
  servidor y no editable como autoridad local.
- Revocación conserva el conteo de comandos pendientes y bloquea la sesión.
- Fake de enrolamiento que responde `ContractUnavailable` sin definir rutas ni
  payloads.
- Scanner HID/teclado encapsulado detrás de una interfaz, con terminación por
  Enter, cancelación y límite defensivo. La compatibilidad definitiva debe
  validarse en los equipos reales.
- El shell Compose conecta la captura HID con catálogo y Pricing sintéticos,
  acumula cantidades, permite editar/eliminar líneas y navega entre bienvenida,
  carrito, asistencia y cancelación. No crea ventas ni realiza llamadas de red.
- La cola de dominio conserva `Idempotency-Key`, convierte timeout en
  `uncertain` y consulta mediante una interfaz de reconciliación antes de
  cualquier reenvío. Room V4 implementa persistencia durable de su metadata;
  un planificador puro bloquea envíos pendientes mientras exista incertidumbre.
- El adapter WorkManager `piloto` agenda trabajo único con red conectada,
  política `KEEP` y backoff exponencial de 30 segundos. Requiere un delegado y
  una `WorkerFactory` explícitos; no está conectado a `app`, no define rutas y
  no puede ejecutar transporte real por sí mismo. Sus 2 pruebas instrumentadas
  pasaron en el X-3566 con Android 11: resultados del delegado y ausencia de
  duplicación del trabajo pendiente.
- La política offline permite borradores puramente locales y niega toda
  operación remota sin capacidad vigente y verificada. Pagos, reembolsos,
  reversiones, liquidaciones, administración y fiscalidad permanecen siempre
  bloqueados offline, incluso si una capacidad sintética intentara listarlos.
- La evidencia diagnóstica admite identificadores técnicos seguros, intentos y
  mensajes acotados. Un sanitizador elimina credenciales, bearer, bootstrap,
  contraseñas, tokens y secretos antes de cualquier registro futuro.
- Modelos candidatos de catálogo, carrito y Pricing viven sólo en el dominio
  Android. Un catálogo fixture y un motor fake recalculan el carrito completo,
  acumulan scans por producto y rechazan precedencias ambiguas.
- El estado de conectividad se muestra como evidencia local y no concede
  capacidades. Asistencia y cancelación son flujos locales sin backend.
- Un timeout lógico de inactividad vuelve a bienvenida y elimina sólo el
  carrito en memoria. Una revisión de actividad evita que timers obsoletos
  cierren una sesión que tuvo interacción posterior.
- Las pruebas instrumentadas Compose cubren asistencia, cancelación y timeout;
  las dos pruebas fueron ejecutadas correctamente en un terminal `X-3566` con
  Android 11. La validación del inventario completo de hardware sigue pendiente.
- Room está incorporado sólo como spike `piloto`: entidad sintética, esquema
  exportado, migración V1→V2 y rollback transaccional. No se conecta todavía a
  la app ni permite persistir secretos o payloads comerciales.
- Las dos pruebas Room fueron ejecutadas correctamente en el X-3566: migración
  V1→V2 sin pérdida y rollback transaccional. Package Manager requirió instalar
  el APK en modo ADB no streaming; el paquete temporal se retiró al finalizar.
- Room V3 agrega sólo metadata sintética de snapshots y demuestra publicación
  atómica, rollback y preservación de la versión activa ante un rollback
  inválido. Las 5 pruebas instrumentadas V3 pasaron en el X-3566.
- Room V4 incorpora un adapter durable de `CommandRepository` sin payload:
  conserva UUID, clave idempotente y estado, y rechaza cualquier mutación de la
  `Idempotency-Key` para un comando existente. Al reiniciar el proceso, todo
  comando interrumpido en `SENDING` pasa atómicamente a `UNCERTAIN`; nunca se
  reencola ni se rechaza automáticamente. Las 9 pruebas instrumentadas V4
  pasaron en el X-3566 con Android 11.

No se incluyen ventas, precios, pagos, fiscalidad ni tablas comerciales.

## Documentación de coordinación

- [Estado y plan de autoservicio API-first](docs/coordinacion-autoservicio-api-first.md)
- [Necesidades pendientes de API Platform](docs/necesidades-api-platform.md)
- [Flujo de arquitectura Draw.io](docs/flujo-autoservicio-api-first.drawio)
- [Matriz de Pricing determinista](docs/matriz-pricing-determinista.md)
- [ADR-003: Room, cifrado y migraciones](docs/adr-003-room-cifrado-migraciones.md)
- [ADR-004: WorkManager y reintentos](docs/adr-004-workmanager-reintentos.md)
- [Flujo de persistencia local A3](docs/persistencia-local-a3.drawio)

Los documentos publicados son una instantánea informativa. El registro oficial
continúa en `J:\Proyectos\context\inforhard-pos-android`; ningún nombre interno,
modelo candidato o necesidad documentada constituye un contrato HTTP aprobado.
