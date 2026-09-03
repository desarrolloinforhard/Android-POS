# Inventario de hardware X-3566

- Estado: `actual`
- Fecha: `2026-08-31`
- Método: inspección read-only mediante ADB
- Alcance: un terminal conectado; no representa todavía todo el parque

## Comprobado

| Capacidad | Evidencia |
|---|---|
| Terminal | Rockchip X-3566 |
| Sistema | Android 11, API 30, ABI principal arm64-v8a |
| Pantalla | 1366×768, 160 dpi, formato horizontal |
| Memoria | Aproximadamente 2 GB RAM |
| Almacenamiento | Partición de datos de 24 GB, con 17 GB disponibles durante la inspección |
| Entrada táctil | Controlador USB ILITEK presente |
| USB host | Característica Android y host USB presentes |
| Scanner | Newtologic 7810SP, USB HID, VID 060A/PID 0860 |
| Clasificación scanner | Android lo expone como teclado alfanumérico con teclas numéricas y Enter |
| Conectividad | Ethernet activa, no medida y validada |
| Impresión Android | Servicio de impresión integrado presente, sin trabajo activo |
| Cifrado del dispositivo | Cifrado por archivos activo |
| StrongBox | No disponible |
| Attest key dedicada | Característica no disponible |

El fallback ya implementado hacia Android Keystore es necesario en este equipo
porque StrongBox no está disponible.

## Bloqueos y pendientes de confirmar

- Dos lecturas físicas numéricas con terminación Enter fueron capturadas el
  2026-09-03 (detalle abajo). Layout alfanumérico, ráfagas rápidas, otras
  simbologías y acumulación end-to-end en la app: pendiente de confirmar.
- No había impresora USB conectada; protocolo, ancho, corte, cajón y encoding
  permanecen pendientes de confirmar.
- No se detectaron balanza ni dispositivo de pago.
- `minSdk 25` no queda aprobado: este equipo sólo demuestra ejecución en API 30.
- El firmware inspeccionado es `userdebug` y Verified Boot informa `orange`;
  una imagen de producción bloqueada, firmada y administrable queda pendiente
  de confirmar antes de cualquier operación real.
- Kiosk/Device Owner, MDM, arranque automático y recuperación ante corte de
  energía permanecen pendientes de confirmar.
- El inventario debe repetirse sobre cada modelo objetivo.

## Consecuencia Android

El adapter HID/teclado existente es compatible conceptualmente con la forma en
que Android clasifica el Newtologic 7810SP. Esto no sustituye la prueba end-to-end
de un barcode real dentro de la app.

No se habilitan endpoints, ventas, pagos, fiscalidad ni operación productiva.

## Captura física del scanner — 2026-09-03

- Estado: `actual`.
- Captura acotada de 45 segundos sobre el dispositivo Newtologic identificado
  en esta sesión como /dev/input/event3, sin inyectar eventos.
- Dos secuencias KEY_DOWN reconstruyen el mismo valor: `7798320512886`.
- El usuario confirmó que ese valor coincide con el número impreso.
- Cada secuencia finaliza con KEY_ENTER DOWN/UP.
- Desde el primer dígito hasta Enter DOWN: aproximadamente 72 y 84 ms.
- Separación entre comienzos: 15,10 segundos; no demuestra lectura en ráfaga.
- Evidencia limitada a eventos HID del sistema. No se comprobó traducción
  Android Unicode ni recepción/lookup/acumulación en el carrito.
- El valor capturado no pertenece al catálogo fixture de la app. No se agregó
  un producto ni se asignó un precio ficticio a ese código.
- La captura terminó y no dejó un lector de eventos activo.
