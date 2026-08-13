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

## Seguridad local implementada

- Adapter Android Keystore con clave EC privada no exportable.
- Preferencia por StrongBox en Android 9 o superior y fallback explícito al
  proveedor Android Keystore cuando StrongBox no está disponible.
- Firma local `SHA256withECDSA`; el formato PoP remoto continúa pendiente de
  contrato y no está implementado.
- `Idempotency-Key` local con 128 bits completos codificados en hexadecimal.

No se incluyen ventas, precios, pagos, fiscalidad ni tablas comerciales.
