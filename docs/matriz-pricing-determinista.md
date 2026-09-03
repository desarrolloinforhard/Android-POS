# Matriz de Pricing determinista

- Estado: `implementado` para fixtures sintéticos
- Alcance: dominio local Android; no constituye contrato API

| Escenario | Invariante comprobada |
|---|---|
| Tres escaneos vs. cantidad 3 | Misma línea, quote y versión |
| Producto A→B vs. B→A | Mismos ítems ordenados y mismo total |
| Umbral antes/al alcanzar | Precio base antes; precio umbral para todas las unidades al alcanzar |
| Grupos de 2, cantidades 1–5 | Sólo grupos completos promocionados; remanente a precio base |
| Promoción retirada | Recálculo completo con nueva versión y precio base |
| Umbral y grupo simultáneos | Resultado ambiguo, independiente del orden de reglas |
| Eliminación de producto | Recálculo de los ítems restantes |
| Cambio de versión Pricing | Quote completo conserva la nueva versión |

Los valores, nombres de reglas y productos son exclusivamente sintéticos. La
precedencia comercial entre listas, umbrales y promociones permanece pendiente
de confirmación por API Platform/Pricing.

## Regresión de carrito mixto en el controlador — 2026-09-03

- Estado: `implementado` para fixtures sintéticos.
- Prueba: PosShellControllerTest.mixedCartAppliesGroupsAndRemainderWithoutChangingWater.
- Reproduce 1 agua más 1, 2 y 3 cereales: totales ARS 470,00, 700,00
  y 1.020,00; subtotales de cereal ARS 320,00, 550,00 y 870,00.
- Comprueba dos líneas, cantidades, moneda ARS, ausencia de error de Pricing,
  permanencia en carrito y conservación del ítem completo de agua.
- Al eliminar cereal comprueba una sola línea de agua, ARS 150,00 y
  el aviso "Cereal eliminado".
- Validación: :app:testDebugUnitTest correcto; suite del controlador 9/9.
- Prueba JVM, no prueba nueva de UI ni de hardware. No cambian reglas ni APK.
