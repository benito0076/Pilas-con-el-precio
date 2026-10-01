# Pilas con el precio

App Android para **comparar precios de la canasta familiar en Colombia**: alimentos, aseo del hogar y cuidado personal, entre cadenas como Éxito, Jumbo, Carulla, Olímpica, D1, Ara y Makro.

## Qué hace (v0.1)

- **Buscar** productos por nombre o marca (sin distinguir tildes) y filtrar por categoría.
- **Detalle de producto**: precio en cada tienda, la más barata resaltada, diferencia frente a la mejor y **precio normalizado** (por 100 g, 100 ml o unidad) para comparar presentaciones distintas.
- **Mi lista de compras** (se guarda en el dispositivo): total por tienda, tiendas a las que les faltan productos, y cuánto se ahorra repartiendo la compra entre varias tiendas.

## Fuentes de precios

- **En vivo (v0.2):** al buscar 3+ letras se consulta en paralelo el catálogo público VTEX de **Éxito, Carulla, Jumbo y Olímpica** (`data/VtexSource.kt`). Los resultados se unen por código de barras (EAN) y se normaliza el contenido a g/ml/unidad. Si una tienda no responde, se indica y se siguen mostrando las demás; si ninguna responde, se muestran los datos de ejemplo.
- **Lectura de página web (v0.3):** D1, Makro y Alkosto no tienen API pública conocida; `data/HtmlSource.kt` descarga su página de resultados y extrae los productos de los datos estructurados incrustados (JSON-LD de schema.org o JSON de Next.js). Las URL de búsqueda están en `LiveSearch.default()` y son **supuestas**: hay que confirmarlas. Si una página no incluye esos datos (por ejemplo, si carga los productos con JavaScript después), esa tienda aparecerá como "no respondió" y habrá que escribir un extractor específico.
- **Pendiente:** Ara (falta la URL de su tienda virtual) y el DANE (SIPSA, datos.gov.co), que son precios mayoristas y no de góndola.
- Estas integraciones usan endpoints no oficiales de las tiendas: **no se han probado contra los servidores reales** (el entorno de desarrollo no tenía salida a esos dominios), pueden cambiar sin aviso y su uso puede estar sujeto a los términos de cada tienda. Para producción conviene un acuerdo con las cadenas o un backend propio con caché.

> ⚠️ **Los precios de demostración incluidos son ilustrativos**, generados de forma determinista en `data/SeedData.kt`; no son precios reales. La app lo indica en pantalla.

## Estructura

- `model/` modelos y formato de pesos (`$12.900`) y precio por unidad.
- `data/` `PriceRepository` (interfaz), datos de ejemplo, `BasketCalculator` y persistencia de la lista.
- `ui/` Jetpack Compose (Material 3) y navegación.

## Compilar

Requiere JDK 17 y Android SDK 35. `./gradlew assembleDebug` genera el APK; `./gradlew testDebugUnitTest` corre las pruebas. El workflow de GitHub Actions hace ambas cosas y publica el APK.

## Próximos pasos

1. Reemplazar `SeedPriceRepository` por una fuente real de precios (API propia, aportes de usuarios con foto de etiqueta o escaneo de código de barras, convenios con cadenas).
2. Selección de ciudad/barrio y tiendas cercanas (precios varían por ciudad).
3. Fecha de actualización de cada precio, alertas de bajada y historial.
4. Escaneo de código de barras (EAN) para buscar un producto.
