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
