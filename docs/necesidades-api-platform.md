# Necesidades pendientes de API Platform

- Estado: `objetivo`
- Consumidor futuro: Android POS autoservicio
- Registro oficial: `J:\Proyectos\context\inforhard-pos-android\integraciones\api-platform\solicitudes-api.md`

Este resumen no propone rutas ni payloads HTTP.

| ID | Necesidad mínima | Condición esperada |
|---|---|---|
| API-REQ-001 | Bootstrap y enrolamiento de terminal | Identidad, revocación, rotación, anti-replay y contexto derivado |
| API-REQ-002 | Catálogo local | Snapshot/delta versionado y caché controlada |
| API-REQ-003 | Precios efectivos | Moneda, vigencia, versión y lectura autenticada |
| API-REQ-004 | Promociones | Tipos diferenciados y precedencia aprobada |
| API-REQ-005 | Reconciliación | Consulta segura usando idempotencia estable |
| API-REQ-006 | Pricing conceptual | Producto/barcode, umbral, grupos y explicación de reglas |
| API-REQ-007 | Cantidad elegible | Confirmar cantidad total por producto y recálculo determinista |
| API-REQ-008 | Ciclo de publicación | Cursor, tombstones, atomicidad, reconciliación y rollback |

Se espera que PS-1 defina el contrato conceptual, PS-6 publique contratos por
Gateway/HTTPS y PS-7 otorgue autorización explícita para integrar Android.
Hasta entonces todos los adapters permanecen como interfaces o fakes.
