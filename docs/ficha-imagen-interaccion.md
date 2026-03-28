# Ficha Pokemon - Rediseno interaccion imagen estatica

**Fecha:** 2026-03-28
**Archivo principal:** `ui/screen/ficha/FichaPokemon.kt` — `ComponenteImagen`

---

## Contexto

Actualmente, pulsar la imagen oficial (artwork) del Pokemon alterna entre vista normal y shiny.
Se quiere reorganizar las interacciones de la imagen para separar funciones y anadir una vista expandida.

---

## Cambios planificados

### 1. Boton shiny independiente
- **Antes:** pulsar la imagen alterna normal/shiny.
- **Despues:** un boton circular con estrella `"✦"` (identico al de `LiveSprites`) junto al boton pokeball.
- Estilo: `40.dp`, `CircleShape`, fondo dorado `0xFFFFD700` cuando activo, texto `"✦"` color `0xFFB8860B`.
- Ubicacion: a la derecha del boton pokeball, en la esquina inferior-izquierda de la banda de color.

### 2. Z-index de botones sobre imagen
- **Problema:** la imagen estatica se superpone parcialmente a los botones (pokeball y nuevo shiny).
- **Solucion:** mover ambos botones fuera del `Card` de fondo y colocarlos en el `Box` padre con `zIndex` superior al de la imagen, manteniendo la misma posicion visual (bottom-start).

### 3. Expansion de imagen al pulsar
- **Nueva funcion al pulsar imagen:** la imagen y su contenedor se expanden para ocupar todo el espacio vertical disponible.
- Los `DetallesDesplegables` (seccion inferior) se pliegan con animacion fluida (`animateContentSize` / `AnimatedVisibility`).
- Al volver a pulsar, la imagen vuelve a su tamano original y los desplegables se despliegan de nuevo.
- Transiciones suaves tanto en expansion como en contraccion.

### 4. Estado `isExpanded`
- Nuevo estado `var isExpanded by remember { mutableStateOf(false) }` en `ComponenteImagen` o en `PokemonDetailsView`.
- La altura de la imagen pasa de `300.dp` fijo a `fillMaxSize` cuando expandida.
- Se comunica al padre (`PokemonDetailsView`) para controlar la visibilidad de `DetallesDesplegables` y `NombreNumAlturaPeso`.

---

## Flujo de implementacion

1. Crear estado `isExpanded` en `PokemonDetailsView` y pasarlo a `ComponenteImagen`.
2. En `ComponenteImagen`:
   - Quitar toggle shiny del `clickable` de la imagen.
   - Anadir boton shiny (estilo `LiveSprites`) junto a pokeball.
   - Reorganizar z-index: botones sobre imagen.
   - Imagen `clickable` ahora alterna `isExpanded`.
   - Altura animada: `300.dp` normal vs peso `1f` expandido.
3. En `PokemonDetailsView`:
   - `NombreNumAlturaPeso` y `DetallesDesplegables` envueltos en `AnimatedVisibility(!isExpanded)` con transicion suave.
   - La `Column` principal redistribuye espacio con `weight` animado.

---

## Archivos afectados

| Archivo | Cambio |
|---------|--------|
| `FichaPokemon.kt` — `ComponenteImagen` | Boton shiny, z-index, click expand |
| `FichaPokemon.kt` — `PokemonDetailsView` | Estado isExpanded, AnimatedVisibility desplegables |
