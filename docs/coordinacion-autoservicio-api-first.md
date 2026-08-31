# Coordinación Android, API Platform y Pricing

- Estado: `actual`
- Fecha de corte: `2026-08-21`
- Alcance: POS Android de autoservicio local, sin integración real
- Evolución contemplada: modo cajero

Esta es una instantánea informativa del contexto oficial Android. No define
rutas HTTP, payloads productivos ni contratos propiedad de API Platform.

## Estado comprobado

| Fase | Estado | Resultado |
|---|---|---|
| A0 Fundación segura | `implementado` | Base modular, Keystore, scanner, estados y fakes |
| A1 Autoservicio local | `piloto` | Catálogo fixture, carrito y navegación local |
| A2 Pricing reemplazable | `piloto` | Motor determinista y fixtures sintéticos |
| A3 Persistencia local | `objetivo` | Room, cifrado, migraciones y recuperación |
| A4 Contratos consumidores | `objetivo` | Fixtures compartidos y mappings versionados |
| A5 Integración controlada | `objetivo` | HTTPS y Gateway en laboratorio autorizado |
| A6 Operación comercial | `objetivo` | Venta, pagos y comprobantes futuros |

La integración real permanece `NO-GO`. SQL Anywhere continúa como fuente
oficial `actual`; PostgreSQL Pricing es una proyección `piloto`. Android nunca
accede directamente a ninguna base ni utiliza ODBC.

## Arquitectura Android

| Aspecto | Decisión vigente |
|---|---|
| UI | Jetpack Compose `piloto` |
| SDK | compile/target 35; minSdk 25 `piloto`, pendiente de hardware real |
| Módulos | `app`, `core:model`, `core:domain`, `core:network`, `core:security`, `core:hardware`, `core:sync` |
| Networking | Interfaces y fakes; sin cliente HTTP ni URL real |
| Persistencia | En memoria; Room continúa como candidato |
| Seguridad | Clave privada no exportable mediante Android Keystore |
| Offline | Deny-by-default; no concede operaciones comerciales |

## Puede avanzar sin endpoints

- Flujo kiosk lógico de bienvenida, escaneo, carrito, asistencia y cancelación.
- Catálogo de productos y códigos de barra mediante fixtures sintéticos.
- Acumulación de escaneos repetidos en una cantidad total por producto.
- Recálculo completo y determinista del carrito.
- Pricing reemplazable para precio base, umbrales y grupos con remanente.
- Estados visuales online, degradado, offline y reconectando sin conceder capacidades.
- Accesibilidad, pruebas unitarias y pruebas Compose.

## Debe permanecer como interfaz o mock

- Enrolamiento, bootstrap, autenticación y configuración efectiva.
- Catálogo snapshot/delta, tombstones, publicación atómica y rollback.
- Precio efectivo, promociones y precedencia entre reglas.
- Creación de venta, pagos, comprobantes y fiscalidad.
- Sincronización y reconciliación contra servicios reales.

## Matriz de coordinación

| Capacidad Android | Puede avanzar ahora | Requiere API | Contrato esperado | Estado | Bloqueo |
|---|---:|---:|---|---|---|
| Shell Compose | Sí | No | Ninguno | `piloto` | Kiosk real/MDM pendiente |
| Scanner HID | Sí | No | Ninguno | `implementado` | Hardware objetivo pendiente |
| Catálogo fixture | Sí | No | Ninguno | `implementado` | Sólo datos sintéticos |
| Carrito acumulativo | Sí | No | Ninguno | `implementado` | Venta real bloqueada |
| Pricing fake | Sí | No | Ninguno | `implementado` | Precedencia no definitiva |
| Navegación autoservicio | Sí | No | Ninguno | `objetivo` | Ninguno para fake |
| Conectividad visual | Sí | No | Capacidad offline futura | `objetivo` | No habilita comandos |
| Asistencia/cancelación | Sí | No | Ninguno | `objetivo` | Sin backend |
| Snapshot/delta real | Sólo interfaz | Sí | Versión, cursor, tombstones y rollback | `objetivo` | PS-1/PS-6/PS-7 |
| Pricing efectivo | Sólo interfaz | Sí | Quote, vigencia, versión y explicación | `objetivo` | PS-1/PS-6/PS-7 |
| Enrolamiento/Auth | Sólo interfaz | Sí | Identidad, PoP, TTL y revocación | `objetivo` | Contrato ausente |
| Venta | No | Sí | Sales/POS aprobado | `objetivo` | Dominio no aprobado |
| Pagos/fiscalidad | No | Sí | Contratos de sus dominios | `objetivo` | Bloqueado |
| Reconciliación real | Sólo interfaz | Sí | Consulta por idempotencia estable | `objetivo` | Contrato ausente |
| Room/migraciones | Spike local | Conceptual | Volumen, retención y snapshot | `objetivo` | Esquema final pendiente |

## Modelos internos candidatos

Los nombres `ProductSummary`, `BarcodeLookup`, `PriceQuote`,
`AppliedPriceRule`, `Cart`, `CartItem`, `ConnectivityState`,
`PendingOperation` y `AppConfiguration` son modelos internos preliminares. No
son contratos API aprobados.

`CartService` consolida primero la cantidad total por producto y solicita luego
un quote para el carrito completo. Escanear dos veces y establecer cantidad dos
deben ser equivalentes. También se recalcula todo al cambiar una cantidad, una
versión de Pricing, una promoción o al eliminar un artículo. El motor es puro y
no depende de la secuencia de captura.

## Próximo bloque Android

1. Navegación Compose de bienvenida a carrito, asistencia y cancelación.
2. Estado de pantalla que conecte scanner, catálogo, carrito y Pricing fake.
3. Pruebas Compose, accesibilidad y retorno seguro a inicio.

A5 no puede comenzar sin contratos versionados, Gateway/HTTPS y autorización
explícita para PS-7. A6 permanece bloqueada.
