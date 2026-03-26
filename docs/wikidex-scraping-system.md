# Sistema de scraping WikiDex - Fallback para descripciones en español

## Problema

La PokeAPI v2 (`pokemon-species/{name}`) es la unica fuente de `flavor_text_entries`, pero tiene carencias importantes en español:

| Juegos | Español en PokeAPI |
|--------|--------------------|
| Gen I-V (Red → White 2) | No - solo ingles |
| Gen VI-VIII (X → Shield) | Si |
| Brilliant Diamond / Shining Pearl | No existen en la API |
| Legends: Arceus | Solo ingles (y solo para algunos Pokemon) |
| Scarlet / Violet | Solo ingles, solo Pokemon de Gen IX |
| Legends: Z-A | No existe en la API |

**WikiDex** (wikidex.net) tiene descripciones en español para **todos** los juegos, incluyendo los que faltan en la PokeAPI.

## Investigacion previa

### Endpoint correcto
Se verifico que `pokemon-species/{name}` es el unico endpoint de PokeAPI v2 que devuelve flavor text. No hay v1 alternativa util (esta deprecada y tiene menos datos).

### Datos fuente de PokeAPI
El CSV fuente en GitHub (`PokeAPI/pokeapi/data/v2/csv/pokemon_species_flavor_text.csv`) solo llega hasta `version_id 34` (Shield). Las versiones 35-48 (DLC Sw/Sh, BD/SP, Legends Arceus, Scarlet/Violet, DLC S/V, Legends Z-A) no tienen datos o son parciales.

### Viabilidad de WikiDex
- **Anubis anti-scraping**: Solo protege `api.php` y `Special:Export`. Las paginas wiki normales se sirven sin restriccion con un User-Agent de navegador.
- **robots.txt**: Bloquea `ClaudeBot`, `GPTBot` y `/api/`. Las paginas wiki normales no estan bloqueadas para User-Agents genericos.
- **Repo GitHub** (`ciencia/WikiDex`): Solo scripts JS de frontend, no expone datos ni API.
- **API MediaWiki**: Bloqueada por Anubis, no viable.
- **Scraping directo con Jsoup**: Funciona. Verificado con `curl` y User-Agent de Firefox.

## Estructura HTML de WikiDex

### URL
```
https://www.wikidex.net/wiki/{NombrePokemonEnEspanol}
```
Ejemplo: `https://www.wikidex.net/wiki/Bulbasaur` (Bulbasaur se llama igual en español)

### Tabla de descripciones
```html
<table class="pokedex radius10 tfx">
  <tr>
    <th>Gen.</th>
    <th>Icono</th>
    <th>Edicion</th>
    <th>Descripcion de la Pokedex</th>
  </tr>
  <tr>
    <!-- Fila normal: 3 o 4 celdas (segun rowspan de generacion) -->
    <!-- Edicion = penultima celda, Descripcion = ultima celda -->
    <th>...</th>           <!-- generacion (con rowspan, no siempre presente) -->
    <th>...</th>           <!-- icono del juego -->
    <th>Esmeralda</th>     <!-- nombre de edicion con <a> tags -->
    <td>Descripcion...</td>
  </tr>
</table>
```

### Casos especiales del HTML

**Ediciones compartidas** (Rojo/Azul, LGP/LGE): Una sola fila con multiples `<a>` tags en la celda de edicion:
```html
<th>
  <div style="display:flex;">
    <div><a>Rojo</a></div>
    <div><a>Azul</a></div>
  </div>
</th>
```
Ambas ediciones comparten la misma descripcion.

**Prefijo "Pokemon"**: Algunas ediciones aparecen como "Pokemon X", "Pokemon Y". Hay que normalizar quitando el prefijo.

**Entradas vacias**: Filas con texto "No hay entrada de {Pokemon} en la Pokedex de {Juego}" o "{Pokemon} no aparece en {Juego}". Deben filtrarse.

**Edicion "Purpura"**: WikiDex usa "Purpura" para Violet (no "Violeta"). El mapeo debe contemplar esto.

## Arquitectura propuesta

### Prioridad de fallback
```
Para cada version de juego:
  1. PokeAPI español     → si existe, usar
  2. WikiDex español     → si existe en cache Room, usar; si no, scrapear y cachear
  3. PokeAPI ingles      → fallback final
```

### Componentes

```
api/wikidex/
  WikiDexFetcher.kt      ← Descarga HTML via Jsoup (generico, cualquier pagina)
  WikiDexParser.kt        ← Interface + FlavorTextParser + WikiDexGameMapper
  WikiDexRepository.kt    ← Orquestador: cache Room → fetch → parse → guardar

api/db/
  WikiDexCacheEntity.kt   ← Entidad Room generica (clave compuesta)
```

### WikiDexFetcher
Responsabilidad unica: descargar el HTML de una URL de WikiDex.

```kotlin
class WikiDexFetcher {
    suspend fun fetchDocument(url: String): Document?
    // - Jsoup.connect(url).userAgent("Mozilla/5.0 ...").timeout(10000).get()
    // - Ejecuta en Dispatchers.IO
    // - Si detecta Anubis (ausencia de contenido real), retorna null
    // - Generico: no sabe que datos se van a extraer
}
```

### WikiDexParser (interface generica)
Permite añadir nuevos extractores sin tocar el fetcher ni el repositorio.

```kotlin
interface WikiDexParser<T> {
    fun parse(doc: Document): T?
}
```

**FlavorTextParser**: Primera implementacion concreta.
- Busca `table.pokedex` (primera tabla con esa clase)
- Itera filas (`<tr>`), extrae penultima celda (edicion) y ultima celda (descripcion)
- Para ediciones compartidas: extrae todos los `<a>` tags de la celda de edicion
- Filtra descripciones que contienen "No hay entrada" o "no aparece en"
- Retorna `List<Pair<String, String>>` (edicion WikiDex, descripcion)

**WikiDexGameMapper**: Convierte nombres de edicion WikiDex a identificadores PokeAPI.
```kotlin
object WikiDexGameMapper {
    fun toApiVersionName(wikiDexEdition: String): String?
    // 1. Normaliza: trim, quita prefijo "Pokemon "
    // 2. Busca en mapa estatico (ver tabla completa abajo)
}
```

### WikiDexRepository
Punto de entrada para el ViewModel. Coordina cache y scraping.

```kotlin
class WikiDexRepository(private val dao: PokemonDao) {
    suspend fun getFlavorTexts(spanishName: String): Map<String, String>
    // 1. Consulta Room: wikidex_cache WHERE pokemonName=X AND dataType="flavor_text"
    // 2. Si hay cache → retorna Map<apiVersionName, descripcion>
    // 3. Si no hay cache:
    //    a. fetch wikidex.net/wiki/{spanishName}
    //    b. parse con FlavorTextParser
    //    c. mapear nombres con WikiDexGameMapper
    //    d. guardar en Room
    //    e. retornar resultado
    // 4. Si falla fetch/parse → retorna emptyMap() (fallback graceful)
}
```

### WikiDexCacheEntity
Entidad Room generica. La clave compuesta permite almacenar cualquier tipo de dato sin cambiar el esquema.

```kotlin
@Entity(tableName = "wikidex_cache", primaryKeys = ["pokemonName", "dataType", "dataKey"])
data class WikiDexCacheEntity(
    val pokemonName: String,   // Nombre español del Pokemon (usado en URL)
    val dataType: String,      // Tipo de dato: "flavor_text", "ability", "move_desc", etc.
    val dataKey: String,       // Clave contextual: version API, nombre de habilidad, etc.
    val value: String,         // Texto scrapeado
    val fetchedAtMillis: Long  // Timestamp para posible invalidacion futura
)
```

Ejemplos de uso futuro:
- Descripciones de habilidades: `dataType="ability"`, `dataKey="chlorophyll"`, `value="Sube Velocidad en sol"`
- Descripciones de movimientos: `dataType="move"`, `dataKey="solar-beam"`, `value="..."`

## Mapeo completo WikiDex → PokeAPI

| WikiDex | PokeAPI | Notas |
|---------|---------|-------|
| Rojo | red | Fila compartida con Azul |
| Azul | blue | Fila compartida con Rojo |
| Amarillo | yellow | |
| Oro | gold | |
| Plata | silver | |
| Cristal | crystal | |
| Rubi | ruby | |
| Zafiro | sapphire | |
| Esmeralda | emerald | |
| Rojo Fuego | firered | |
| Verde Hoja | leafgreen | |
| Diamante | diamond | |
| Perla | pearl | |
| Platino | platinum | |
| Oro HeartGold | heartgold | |
| Plata SoulSilver | soulsilver | |
| Negro | black | |
| Blanco | white | |
| Negro 2 | black-2 | |
| Blanco 2 | white-2 | |
| X | x | Puede aparecer como "Pokemon X" |
| Y | y | Puede aparecer como "Pokemon Y" |
| Rubi Omega | omega-ruby | |
| Zafiro Alfa | alpha-sapphire | |
| Sol | sun | |
| Luna | moon | |
| Ultrasol | ultra-sun | |
| Ultraluna | ultra-moon | |
| Let's Go, Pikachu! | lets-go-pikachu | Fila compartida con LGE |
| Let's Go, Eevee! | lets-go-eevee | Fila compartida con LGP |
| Espada | sword | |
| Escudo | shield | |
| Diamante Brillante | brilliant-diamond | No existe en PokeAPI |
| Perla Reluciente | shining-pearl | No existe en PokeAPI |
| Leyendas: Arceus | legends-arceus | Solo ingles en PokeAPI |
| Escarlata | scarlet | Solo ingles/parcial en PokeAPI |
| Purpura | violet | WikiDex usa "Purpura", no "Violeta" |
| Leyendas: Z-A | legends-za | No existe en PokeAPI |

## Cambios necesarios en el proyecto

### Nuevos archivos
| Archivo | Descripcion |
|---------|-------------|
| `api/wikidex/WikiDexFetcher.kt` | Descarga HTML de WikiDex con Jsoup |
| `api/wikidex/WikiDexParser.kt` | Interface generica + FlavorTextParser + WikiDexGameMapper |
| `api/wikidex/WikiDexRepository.kt` | Orquestador cache/fetch/parse |
| `api/db/WikiDexCacheEntity.kt` | Entidad Room generica |

### Archivos a modificar
| Archivo | Cambio |
|---------|--------|
| `app/build.gradle.kts` | Añadir `implementation("org.jsoup:jsoup:1.18.1")` |
| `app/proguard-rules.pro` | Añadir reglas keep para Jsoup |
| `api/db/DexterDatabase.kt` | Añadir entidad, bump version 3→4, MIGRATION_3_4 |
| `api/db/PokemonDao.kt` | Añadir queries getWikiDexCache, insertWikiDexCache |
| `DexterApplication.kt` | Exponer `appContext` en companion object |
| `api/viewModel/PokemonViewModel.kt` | Instanciar repositorio, nuevo LiveData, lanzar fetch |
| `ui/screen/ficha/FichaPokemon.kt` | Observar y pasar wikiDexFlavorTexts |
| `ui/screen/ficha/composable/FichaDesplegables.kt` | Merge 3 fuentes con prioridad ES→WikiDex→EN |

### Migracion Room 3→4
```sql
CREATE TABLE IF NOT EXISTS `wikidex_cache` (
    `pokemonName` TEXT NOT NULL,
    `dataType` TEXT NOT NULL,
    `dataKey` TEXT NOT NULL,
    `value` TEXT NOT NULL,
    `fetchedAtMillis` INTEGER NOT NULL,
    PRIMARY KEY(`pokemonName`, `dataType`, `dataKey`)
)
```

## Flujo de datos en la UI

```
PokemonDetailScreen
  ├─ LaunchedEffect(pokemonName)
  │   └─ viewModel.fetchPokemonDetailsByName(name, "es")
  │       ├─ Carga species de PokeAPI (paralelo)
  │       └─ Lanza coroutine WikiDex con nombre español (paralelo)
  │           └─ WikiDexRepository.getFlavorTexts(spanishName)
  │               ├─ Room cache hit → retorna datos inmediatamente
  │               └─ Room cache miss → fetch + parse + cache + retorna
  │
  ├─ observeAsState: pokemonSpecies, wikiDexFlavorTexts
  │
  └─ DetallesDesplegables(pokemonSpecies, wikiDexFlavorTexts, ...)
      └─ remember(pokemonSpecies, wikiDexFlavorTexts) {
            // Merge 3 fuentes:
            allVersions = pokeApiES.keys ∪ wikiDex.keys ∪ pokeApiEN.keys
            para cada version:
              texto = pokeApiES[v] ?: wikiDex[v] ?: pokeApiEN[v]
         }
```

La UI se renderiza primero con datos de PokeAPI (instantaneo si hay cache HTTP). Cuando WikiDex responde, el `remember` se recalcula y la UI se actualiza reactivamente con las versiones adicionales.

## Verificacion

1. **Compilacion**: `./gradlew assembleDebug` sin errores tras cada fase
2. **Migracion Room**: Instalar sobre version anterior, verificar que no crashea
3. **Test funcional**:
   - Bulbasaur: Gen I-V en español (WikiDex), Gen VI+ en español (PokeAPI), BD/SP + Escarlata + Purpura + Z-A (WikiDex)
   - Pikachu: Legends Arceus en español (WikiDex), Scarlet/Violet en español (WikiDex)
4. **Filtrado**: Verificar que "No hay entrada de..." no aparece
5. **Cache**: Segunda visita al mismo Pokemon no genera peticion HTTP a WikiDex
6. **Fallback graceful**: Desactivar red → la app funciona con datos de PokeAPI + cache Room existente

## Extensibilidad futura

Para añadir un nuevo tipo de dato desde WikiDex:

1. Crear nueva implementacion de `WikiDexParser<T>` (ej: `AbilityDescParser`)
2. Añadir metodo en `WikiDexRepository` que use el nuevo parser
3. Usar `dataType` distinto en `WikiDexCacheEntity` (ej: `"ability"`)
4. No requiere cambios en Room (mismo esquema generico)
