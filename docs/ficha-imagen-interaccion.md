# Ficha Pokemon - Rediseno interaccion imagen y descripciones

**Fecha:** 2026-03-28
**Archivos principales:** `FichaPokemon.kt`, `FichaDesplegables.kt`

---

## Cambios implementados

### 1. Boton shiny independiente [HECHO]
- Boton circular `"✦"` (40dp, CircleShape) identico al de LiveSprites.
- Apilado verticalmente sobre el boton pokeball (Column) en esquina inferior-izquierda.
- Fondo dorado `0xFFFFD700` cuando activo, texto color `0xFFB8860B`.

### 2. Z-index de botones sobre imagen [HECHO]
- Botones en Column con `zIndex(2f)`, imagen en `zIndex(1f)`.

### 3. Expansion fluida de imagen al pulsar [HECHO]
- Pesos animados con `animateFloatAsState`: imagen (0.35 → 1.0), secciones inferiores (0.65 → 0.0).
- Opacidad inferior se desvanece con `graphicsLayer { alpha }`.
- Duracion: 450ms expansion, 350ms opacidad. Sin saltos abruptos.

### 4. Seccion Descripcion: dos columnas con caratulas [HECHO]
- **Columna izquierda (32%):** LazyColumn con caratulas de juego (`game_[nombre].ext`).
  - Cada item muestra la imagen de caratula + nombre del juego debajo.
  - Juego seleccionado tiene borde `colorAccent` y fondo resaltado.
  - Scroll vertical independiente.
- **Columna derecha (68%):** titulo del juego + descripcion en card.
- Funcion `getGameCoverResId()` mapea version API → drawable resource.
- Estado `selectedVersion` con `rememberSaveable`.

---

## Archivos afectados

| Archivo | Cambio |
|---------|--------|
| `FichaPokemon.kt` — `ComponenteImagen` | Boton shiny vertical, z-index, click expand |
| `FichaPokemon.kt` — `PokemonDetailsView` | Pesos animados, opacidad secciones inferiores |
| `FichaDesplegables.kt` — `SectionPage.DESC` | Layout dos columnas, caratulas, getGameCoverResId |
| `res/drawable/game_*.ext` | Caratulas renombradas (guiones → underscores) |
