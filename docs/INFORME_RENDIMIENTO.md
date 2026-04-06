# Informe de rendimiento en arranque (móvil)

## Estado actual

La app presenta stuttering/jank en el arranque en dispositivos móviles. Se han identificado 6 puntos de mejora ordenados por impacto.

---

## 1. Carga lazy por generación — `IMPLEMENTADO`

**Problema:** Al arrancar se lanzaba `fetchPokemonForGeneration()` para las 9 generaciones a la vez, generando 500+ peticiones API concurrentes.

**Solución aplicada:**
- `ListaPokemon.kt`: Eliminado el `LaunchedEffect(generations)` que cargaba todo de golpe. Sustituido por un `LaunchedEffect(currentPage)` que solo carga la generación visible + adyacentes (±1 página del `HorizontalPager`).
- `ListaPokemon.kt`: Añadido un `LaunchedEffect(isSearching)` que dispara la carga de las generaciones faltantes en segundo plano cuando el usuario activa búsqueda o filtros.
- `PokemonViewModel.kt`: Añadida función `ensureAllGenerationsLoaded()` que solo carga las generaciones que no estén ya en caché.

**Resultado:**
- Arranque: de ~1000+ peticiones a ~300 (Gen 1 + Gen 2)
- Swipe entre generaciones: la siguiente ya está precargada
- Búsqueda: los resultados van apareciendo conforme se cargan las generaciones restantes
- Segunda apertura: instantánea gracias a Room (ya existente)

---

## 2. Reducir concurrencia del dispatcher de red — `PENDIENTE`

**Problema:** `RetrofitClient.kt:26-36` configura `maxRequests = 100` y `maxRequestsPerHost = 50`. En móvil con 3G/4G esto causa agotamiento de sockets, TCP window stalls y timeouts en cascada.

**Solución propuesta:**
- Reducir a `maxRequests = 20`, `maxRequestsPerHost = 10`
- Implementar retry con backoff exponencial

**Impacto esperado:** Menos fallos de red, menos memoria consumida por conexiones abiertas.

---

## 3. Consolidar StateFlow/LiveData — `PENDIENTE`

**Problema:** `PokemonViewModel.kt:200-279` declara 22+ objetos `LiveData`/`StateFlow`. Al arrancar, `_generations` y `_pokemonByGenerationCache` se actualizan repetidamente, cada actualización dispara recomposiciones del árbol de UI completo.

**Solución propuesta:**
- Agrupar estados relacionados en un único `data class` con un solo `StateFlow`
- Usar `derivedStateOf` donde sea posible para evitar recomposiciones innecesarias

**Impacto esperado:** Menos recomposiciones en cascada, arranque más fluido.

---

## 4. Simplificar animaciones de tarjetas — `PENDIENTE`

**Problema:** `PokemonCard.kt:62-212` — Cada tarjeta tiene múltiples `animateFloatAsState` + gradientes radiales con 5 color stops que se recalculan por frame. Con 60-150 tarjetas visibles se produce overdraw masivo en la GPU.

Detalle:
- `animateFloatAsState` para scale, contentScale, pokeballAlpha (por cada tarjeta)
- Shimmer con `Brush.radialGradient` de 5 color stops
- `ColorFilter.tint` con `BlendMode.SrcAtop` por frame
- Las animaciones se evalúan incluso para tarjetas fuera de pantalla

**Solución propuesta:**
- Desactivar animaciones para tarjetas no visibles
- Simplificar gradientes radiales del shimmer (menos color stops)
- Considerar eliminar shimmer en dispositivos de gama baja

**Impacto esperado:** Reducción significativa de carga GPU y overdraw.

---

## 5. Eliminar assets raw / usar solo red — `PENDIENTE`

**Problema:** `/res/raw/` contiene 2.930 archivos WebP (2.4 GB). Esto ralentiza la primera instalación/extracción del APK y genera presión de memoria y almacenamiento.

**Solución propuesta:**
- Eliminar los assets empaquetados
- Usar exclusivamente URLs del CDN de Pokémon (ya implementado para las tarjetas vía Coil)
- Coil ya tiene cache de imágenes configurado

**Impacto esperado:** APK mucho más ligero, instalación más rápida, menos presión de almacenamiento.

---

## 6. Precomputar strings y colores — `PENDIENTE`

**Problema:** `PokemonCard.kt:218` ejecuta `adaptaNombre(transformPokemonNameToResourceName(...))` en cada recomposición de cada tarjeta. Igualmente, `getPokemonTypeColorClear()` y `getPokemonTypeGradientColors()` se llaman por tarjeta en cada composición.

**Solución propuesta:**
- Precomputar el nombre formateado al crear el `PokemonSummary`, no en el render
- Cachear colores/gradientes por tipo en un `Map` estático

**Impacto esperado:** Menos allocations y ciclos CPU por frame, mejora acumulativa con muchas tarjetas.
