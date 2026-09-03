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

## Corrección de foco HID — 2026-09-03

- Estado: `piloto`.
- Usuario reporta agua agregada en primera lectura y asistencia en la tercera.
- Se sustituye onKeyDown por dispatchKeyEvent y AndroidHidKeyRouter, antes de
  Compose. Consume DOWN/UP del scanner y conserva Enter aislado para navegación.
- Unitarias, lint y compilación debug/test completados. Suite UI en X-3566:
  4/4, incluyendo tres secuencias con asistencia enfocada, una línea de agua,
  cantidad 3 y total fixture ARS 375,00; Enter aislado conserva asistencia.
- Validación física posterior confirmada por el usuario con capturas sucesivas:
  2 aguas / ARS 300,00 y 3 aguas / ARS 375,00, una línea y sin abrir asistencia.
  Caso reportado comprobado en este terminal y fixture; otros lectores y
  escenarios pendientes de confirmar. APK debug actualizado sin cambiar seguridad.
- Detalle oficial: contexto Android, relevamientos/correccion-foco-scanner-2026-09-03.md.

## Validación del carrito y aviso de eliminación — 2026-09-03

- Estado: `piloto`.
- Capturas del usuario confirman la secuencia 4 aguas / ARS 500,00,
  3 / ARS 375,00 y 2 / ARS 300,00 con precios exclusivamente sintéticos.
- Eliminar deja el carrito vacío / ARS 0,00; nuevo escaneo agrega una unidad
  / ARS 150,00, sin recuperar la cantidad anterior.
- Se observó un aviso obsoleto de producto agregado después de eliminar.
- Corrección local: al quitar el último artículo, incluso con decremento a cero,
  mostrar "Carrito vacío"; si quedan artículos, identificar el producto eliminado.
- Tres pruebas de regresión cubren eliminación y nuevo escaneo, decremento final,
  eliminación parcial y producto inexistente.
- Comprobación física del aviso actualizado: confirmada por el usuario con
  capturas de 1 agua / ARS 150,00 y posterior eliminación: "Carrito vacío",
  total ARS 0,00, sin aviso anterior de producto agregado.
- Validación automatizada: 8/8 pruebas del controlador, lint y compilación debug
  correctos. APK instalado en el X-3566 sin modificar su seguridad.
- Sin cambios de flujo arquitectónico, reglas de precios, API ni infraestructura.

## Descarte de lecturas fuera del carrito — 2026-09-03

- Estado: `piloto`.
- Reporte físico del usuario: escanear en asistencia activa "Volver al carrito".
- Causa en código: el router devolvía false al estar scanEnabled deshabilitado,
  permitiendo que las teclas del lector llegaran al botón enfocado.
- Corrección: capturar DOWN/UP en todas las pantallas; entregar resultados
  únicamente para secuencias iniciadas y mantenidas dentro del carrito.
  Fuera del carrito se descartan, incluido Enter; no se acumulan para después.
- Se preserva Enter aislado de navegación. No se cambia el timeout ni se agregan
  endpoints, reglas comerciales, SDK de hardware o configuración de seguridad.
- Pruebas: unitarias de app y lint correctos; compilación debug/test correcta;
  7/7 pruebas UI en X-3566 Android 11 (18,433 s), con foco de teclado explícito.
  Nuevos casos: asistencia conserva 2 aguas / ARS 300,00 y próximo scan da
  3 / ARS 375,00; bienvenida no inicia por lectura; cancelación no confirma
  ni modifica el carrito por lectura. Son eventos sintéticos instrumentados.
- APK debug actualizado. El usuario confirmó que la prueba física posterior
  en asistencia ahora funciona; no aportó una nueva captura de esta repetición.
  Validación física en bienvenida y cancelación: pendiente de confirmar;
  esos escenarios cuentan con las pruebas instrumentadas descritas arriba.
  La ronda anterior de asistencia sólo escaneaba después de volver al carrito
  y no cubría el nuevo incidente.
- Draw.io actualizado en código y contexto para representar el descarte.
