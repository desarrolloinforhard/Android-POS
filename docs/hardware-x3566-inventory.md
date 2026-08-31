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

- No se capturó todavía un escaneo físico: sufijo Enter, layout, caracteres,
  repetición, ráfaga y códigos soportados permanecen pendientes de confirmar.
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

