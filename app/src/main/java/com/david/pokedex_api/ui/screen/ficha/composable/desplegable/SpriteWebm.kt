package com.david.pokedex_api.ui.screen.ficha.composable.desplegable

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.exoplayer.ExoPlayer
import org.json.JSONArray
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer
import org.videolan.libvlc.util.VLCVideoLayout
import java.io.InputStream
import java.nio.charset.Charset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.ui.PlayerView

// Data class para representar cada objeto en tu JSON
data class PokemonSpriteInfo(
    val nombreJson: String, // El nombre original del JSON "Archivo:Pikachu.webm"
    val resourceName: String // El nombre del archivo en res/raw sin extensión "pikachu_home"
)
private const val TAG = "SimpleWebmPlayer"
private const val TAG_EXOPLAYER = "ExoPlayerComposable"

private const val TAG_LOADER = "PokemonSpriteLoader"

fun loadPokemonSprites(context: Context): List<PokemonSpriteInfo> {
    val sprites = mutableListOf<PokemonSpriteInfo>()
    try {
        val inputStream: InputStream = context.resources.openRawResource(
            context.resources.getIdentifier(
                "sprites_pokemon_home", // Nombre de tu archivo JSON sin la extensión
                "raw",
                context.packageName
            )
        )
        val size: Int = inputStream.available()
        val buffer = ByteArray(size)
        inputStream.read(buffer)
        inputStream.close()
        val jsonString = String(buffer, Charset.defaultCharset())
        val jsonArray = JSONArray(jsonString)

        for (i in 0 until jsonArray.length()) {
            val jsonObject = jsonArray.getJSONObject(i)
            val nombreOriginalJson = jsonObject.getString("nombre") // ej: "Archivo:Pikachu_HOME.webm"

            // Extraer el nombre base para el recurso raw
            // Asumimos que los archivos en res/raw son como "pikachu_home.webm", "bulbasaur_home.webm", etc.
            // Y que el JSON tiene nombres como "Archivo:Pikachu_HOME.webm"
            val resourceName = nombreOriginalJson
                .substringAfter("Archivo:") // "Pikachu_HOME.webm"
                .substringBeforeLast(".webm") // "Pikachu_HOME"
                .lowercase() // "pikachu_home" (convención para nombres de recursos raw)

            sprites.add(
                PokemonSpriteInfo(
                    nombreJson = nombreOriginalJson,
                    resourceName = resourceName
                )
            )
        }
    } catch (e: Exception) {
        Log.e(TAG_LOADER, "Error loading Pokemon sprites from JSON", e)
    }
    return sprites
}

// Función para encontrar la URL por el nombre del Pokémon (simplificada)
// Deberás ajustar la lógica de `normalizePokemonName` según cómo estén los nombres en tu JSON
fun findPokemonResourceName(pokemonName: String, sprites: List<PokemonSpriteInfo>): String? {
    val normalizedQueryName = pokemonName.lowercase() // Nombre del Pokémon que buscamos, ej "pikachu"

    // La idea es que resourceName en PokemonSpriteInfo ya está normalizado, ej. "pikachu_home"
    // Buscamos un resourceName que comience con el nombre del Pokémon normalizado.
    val match = sprites.find { spriteInfo ->
        // Ejemplo: spriteInfo.resourceName podría ser "pikachu_home"
        // normalizedQueryName podría ser "pikachu"
        // Queremos asegurar que buscamos "pikachu_home" (o similar) y no variantes que puedan
        // estar en el nombre del JSON original pero que no queremos para el sprite HOME base.
        spriteInfo.resourceName.startsWith(normalizedQueryName) &&
                spriteInfo.resourceName.endsWith("_home") && // Asegura que es el sprite "HOME"
                !spriteInfo.resourceName.contains("hembra") && // Excluir variantes si es necesario
                !spriteInfo.resourceName.contains("alola") &&
                !spriteInfo.resourceName.contains("galar") &&
                !spriteInfo.resourceName.contains("hisui")
        // Puedes añadir más lógica de exclusión si tus nombres de recurso raw son más complejos.
    }

    if (match != null) {
        Log.d(TAG_LOADER, "Found resource for $pokemonName: ${match.resourceName}")
    } else {
        Log.w(TAG_LOADER, "No matching resource found for $pokemonName")
    }
    return match?.resourceName
}


@Composable
fun WebmImageDialog(
    pokemonResourceName: String?, // Nombre del recurso en res/raw (ej: "pikachu_home")
    pokemonDisplayName: String,  // Nombre para mostrar en el título (ej: "Pikachu")
    onDismiss: () -> Unit
) {
    if (pokemonResourceName == null) {
        // Decide cómo manejar esto: puedes no mostrar el diálogo,
        // mostrar un mensaje de error, o simplemente loguearlo.
        Log.e("WebmImageDialog", "pokemonResourceName is null for $pokemonDisplayName. Dialog will not be shown.")
        return // No se muestra el diálogo si no hay nombre de recurso
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false) // Permite que la Card controle el ancho
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth(0.9f) // O el ancho que prefieras
                .wrapContentHeight(),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White) // O el color de fondo de la Card
        ) {
            Column( // androidx.compose.foundation.layout.Column
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Sprite 3D de $pokemonDisplayName",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Usar ExoPlayerComposable
                ExoPlayerComposable(
                    resourceName = pokemonResourceName, // Pasar el nombre del recurso
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f) // Asumiendo que los sprites son cuadrados
                        .clip(RoundedCornerShape(12.dp)) // Clip opcional
                        .background(Color.Black) // Fondo negro detrás del video transparente
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(onClick = onDismiss) {
                    Text("Close")
                }
            }
        }
    }
}

@Composable
fun VlcPlayer(
    url: String,
    modifier: Modifier = Modifier // Este modifier es importante, vendrá de WebmImageDialog
) {
    val context = LocalContext.current
    val TAG = "VlcPlayerComposable"

    var libVLC by remember { mutableStateOf<LibVLC?>(null) }
    var vlcMediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    // 1. Inicializar LibVLC
    LaunchedEffect(context) {
        Log.d(TAG, "Attempting to initialize LibVLC.")
        try {
            libVLC = LibVLC(context, ArrayList<String>().apply {
                // add("--no-audio") // Descomenta si no necesitas audio
                add("--verbose=0") // 0 para menos logs, 1 o 2 para más detalle si depuras
            })
            Log.d(TAG, "LibVLC initialized successfully: $libVLC")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing LibVLC", e)
        }
    }

    // 2. Liberar recursos de LibVLC y MediaPlayer
    DisposableEffect(Unit) {
        onDispose {
            Log.d(TAG, "Disposing VlcPlayer: Releasing MediaPlayer and LibVLC.")
            vlcMediaPlayer?.apply {
                if (isPlaying) {
                    stop()
                }
                detachViews()
                release()
            }
            vlcMediaPlayer = null
            libVLC?.release()
            libVLC = null
            Log.d(TAG, "MediaPlayer and LibVLC released.")
        }
    }

    // 3. Manejar pausa/reanudación del ciclo de vida
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, vlcMediaPlayer) {
        val observer = LifecycleEventObserver { _, event ->
            val currentPlayer = vlcMediaPlayer
            if (currentPlayer == null) {
                return@LifecycleEventObserver
            }
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    if (currentPlayer.isPlaying) {
                        currentPlayer.pause()
                        Log.d(TAG, "Lifecycle ON_PAUSE: MediaPlayer paused.")
                    }
                }
                Lifecycle.Event.ON_RESUME -> {
                    if (!currentPlayer.isPlaying && currentPlayer.media != null) {
                        currentPlayer.play()
                        Log.d(TAG, "Lifecycle ON_RESUME: MediaPlayer playing.")
                    }
                }
                else -> { /* No-op */ }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            Log.d(TAG, "Removing lifecycle observer.")
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // 4. Crear y actualizar la vista de video
    AndroidView(
        factory = { ctx ->
            Log.d(TAG, "AndroidView factory: Creating VLCVideoLayout.")
            VLCVideoLayout(ctx).apply {
                // INTENTO DE CONFIGURAR TRANSPARENCIA:
                // El objetivo es que el SurfaceView dentro de VLCVideoLayout sea transparente
                // para que el fondo negro aplicado en el Modifier de Compose (en WebmImageDialog)
                // se muestre a través de las partes transparentes del video.

                var surfaceViewSuccessfullyConfigured = false
                if (this.childCount > 0) {
                    val surfaceViewCandidate = this.getChildAt(0) // VLCVideoLayout suele tener el SurfaceView como primer hijo
                    if (surfaceViewCandidate is android.view.SurfaceView) {
                        Log.d(TAG, "SurfaceView found, attempting to configure for transparency.")

                        // setZOrderOnTop(true) es crucial para que setFormat con transparencia funcione en un SurfaceView.
                        // ADVERTENCIA: Esto hace que el SurfaceView se dibuje encima de cualquier otro
                        // elemento de Compose en la misma elevación en la ventana.
                        // Para un diálogo donde el video es el contenido principal, podría estar bien.
                        surfaceViewCandidate.setZOrderOnTop(true)

                        // Establece el formato de píxeles del SurfaceHolder para soportar transparencia.
                        // PixelFormat.TRANSLUCENT es la opción estándar.
                        // PixelFormat.RGBA_8888 también soporta alfa y podría ser una alternativa a probar.
                        surfaceViewCandidate.holder.setFormat(android.graphics.PixelFormat.TRANSLUCENT)

                        // Adicionalmente, podrías intentar establecer el color de fondo del propio SurfaceView a transparente,
                        // aunque setFormat debería ser el principal habilitador de la transparencia.
                        // surfaceViewCandidate.setBackgroundColor(android.graphics.Color.TRANSPARENT)

                        surfaceViewSuccessfullyConfigured = true
                        Log.d(TAG, "SurfaceView configured for transparency (setZOrderOnTop(true), holder.setFormat(TRANSLUCENT)).")
                    } else {
                        Log.w(TAG, "VLCVideoLayout's first child is not a SurfaceView. Found: ${surfaceViewCandidate::class.java.name}")
                    }
                } else {
                    Log.w(TAG, "VLCVideoLayout has no children upon creation; cannot configure SurfaceView directly.")
                }

                // El fondo del VLCVideoLayout (que es un FrameLayout) también debe ser transparente.
                // Si el SurfaceView se configuró correctamente como transparente, esto asegura que el
                // FrameLayout contenedor no lo opaque.
                // Si el SurfaceView NO se pudo configurar como transparente, establecer esto a transparente
                // por sí solo NO hará que el contenido del video alfa se mezcle con el fondo de Compose;
                // simplemente hará que el FrameLayout de VLCVideoLayout sea transparente, pero el video
                // renderizado por el motor de VLC (con su fondo rosa) aún se dibujaría.
                setBackgroundColor(android.graphics.Color.TRANSPARENT)

                if (surfaceViewSuccessfullyConfigured) {
                    Log.d(TAG, "VLCVideoLayout background set to TRANSPARENT (as SurfaceView is expected to be transparent).")
                } else {
                    Log.d(TAG, "VLCVideoLayout background set to TRANSPARENT (SurfaceView transparency configuration might have failed or was not applicable).")
                }
            }
        },
        modifier = modifier, // Este modifier incluye el .background(Color.Black) de WebmImageDialog
        update = { vlcVideoLayout ->
            val currentLibVLC = libVLC
            if (currentLibVLC != null && vlcMediaPlayer == null) {
                Log.d(TAG, "AndroidView update: LibVLC available, MediaPlayer is null. Creating MediaPlayer for URL: $url")

                vlcMediaPlayer = MediaPlayer(currentLibVLC).apply {
                    attachViews(vlcVideoLayout, null, false, false)
                    Log.d(TAG, "MediaPlayer views attached to VLCVideoLayout.")

                    val media = Media(currentLibVLC, Uri.parse(url)).apply {
                        addOption(":input-repeat=65535")
                        Log.d(TAG, "Media option :input-repeat=65535 added.")

                        // Para WebM (VP8/VP9), la decodificación por software suele ser robusta.
                        // La aceleración por hardware puede ser quisquillosa, especialmente con transparencia.
                        setHWDecoderEnabled(false, false)
                        Log.d(TAG, "Media HW Decoder enabled: false (prefer software for potential alpha handling).")

                        // Opciones adicionales que podrían influir en el manejo del alfa (experimentales):
                        // addOption(":codec=libvpx") // o vp9, vp8 si ayuda a VLC a seleccionar el decodificador correcto
                        // addOption("--no-osd") // Deshabilita On Screen Display, por si interfiere
                        // addOption("--video-chroma=RGBA") // Esto es más para salida, pero podría tener algún efecto (raro)
                    }

                    setMedia(media)
                    media.release()
                    Log.d(TAG, "Media set on MediaPlayer and released.")

                    setEventListener { event ->
                        when (event.type) {
                            MediaPlayer.Event.Playing -> Log.d(TAG, "MediaPlayer Event: Playing")
                            MediaPlayer.Event.Paused -> Log.d(TAG, "MediaPlayer Event: Paused")
                            MediaPlayer.Event.Stopped -> Log.d(TAG, "MediaPlayer Event: Stopped")
                            MediaPlayer.Event.EndReached -> Log.d(TAG, "MediaPlayer Event: EndReached (should be handled by :input-repeat)")
                            MediaPlayer.Event.EncounteredError -> Log.e(TAG, "MediaPlayer Event: EncounteredError")
                            MediaPlayer.Event.Buffering -> {
                                // val buffering = event.buffering
                                // Log.d(TAG, "MediaPlayer Event: Buffering $buffering")
                            }
                            MediaPlayer.Event.Opening -> Log.d(TAG, "MediaPlayer Event: Opening")
                            // Puedes añadir más eventos para depuración si es necesario
                            // MediaPlayer.Event.TimeChanged -> { /* Log.v(TAG, "MediaPlayer Event: TimeChanged ${event.timeChanged}"); */ }
                            // MediaPlayer.Event.PositionChanged -> { /* Log.v(TAG, "MediaPlayer Event: PositionChanged ${event.positionChanged}"); */ }
                            // MediaPlayer.Event.Vout -> Log.d(TAG, "MediaPlayer Event: Vout ${event.voutCount}") // Video output count
                        }
                    }

                    play()
                    Log.d(TAG, "MediaPlayer.play() called.")
                }
            } else if (currentLibVLC == null && vlcMediaPlayer == null) {
                Log.w(TAG, "AndroidView update: LibVLC is null, cannot create MediaPlayer yet.")
            } else if (vlcMediaPlayer != null) {
                // MediaPlayer ya existe.
                Log.v(TAG, "AndroidView update: MediaPlayer already exists. Views should be attached. Current state: isPlaying=${vlcMediaPlayer?.isPlaying}")
                // Si la vlcVideoLayout necesitara ser re-adjuntada por alguna razón (poco común en este setup)
                // if (!vlcMediaPlayer!!.isPlaying && vlcVideoLayout.isAttachedToWindow) {
                //    vlcMediaPlayer?.attachViews(vlcVideoLayout, null, false, false)
                // }
            }
        }
    )
}


@Composable
fun ExoPlayerComposable(
    resourceName: String, // Nombre del archivo en res/raw SIN extensión, ej: "pikachu_home"
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var exoPlayer by remember { mutableStateOf<ExoPlayer?>(null) }
    // No necesitamos playerView como estado si solo se configura en factory y se usa en update

    // Construir el URI para el recurso raw
    val videoUri: Uri? = remember(resourceName, context) {
        val resourceId = context.resources.getIdentifier(resourceName, "raw", context.packageName)
        if (resourceId != 0) {
            Uri.parse("android.resource://${context.packageName}/$resourceId")
        } else {
            Log.e(TAG_EXOPLAYER, "Resource not found for name: $resourceName in package: ${context.packageName}")
            null
        }
    }

    // Inicialización y actualización de ExoPlayer
    LaunchedEffect(videoUri, context) { // Relanzar si el URI o el contexto cambian
        if (videoUri == null) {
            Log.e(TAG_EXOPLAYER, "Video URI is null for resourceName '$resourceName'. Cannot initialize player.")
            exoPlayer?.stop() // Detener si ya existía y el nuevo URI es nulo
            exoPlayer?.clearMediaItems()
            return@LaunchedEffect
        }

        val playerInstance = exoPlayer ?: ExoPlayer.Builder(context)
            .build()
            .also { exoPlayer = it; Log.d(TAG_EXOPLAYER, "New ExoPlayer instance created.") }

        val mediaItem = MediaItem.Builder()
            .setUri(videoUri)
            .setMimeType(MimeTypes.VIDEO_WEBM) // Especificar el MimeType puede ayudar
            .build()

        playerInstance.setMediaItem(mediaItem)
        playerInstance.repeatMode = Player.REPEAT_MODE_ONE
        playerInstance.playWhenReady = true
        playerInstance.prepare()
        Log.d(TAG_EXOPLAYER, "ExoPlayer prepared with URI: $videoUri")
    }

    // Manejo del ciclo de vida de ExoPlayer y liberación de recursos
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(exoPlayer) { // Observa cambios en exoPlayer para el cleanup
        val lifecycleObserver = LifecycleEventObserver { _, event ->
            val currentPlayer = exoPlayer ?: return@LifecycleEventObserver
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    Log.d(TAG_EXOPLAYER, "Lifecycle ON_RESUME: playing")
                    currentPlayer.play()
                }
                Lifecycle.Event.ON_PAUSE -> {
                    Log.d(TAG_EXOPLAYER, "Lifecycle ON_PAUSE: pausing")
                    currentPlayer.pause()
                }
                else -> {}
            }
        }

        lifecycleOwner.lifecycle.addObserver(lifecycleObserver)

        onDispose {
            Log.d(TAG_EXOPLAYER, "Disposing ExoPlayer instance.")
            lifecycleOwner.lifecycle.removeObserver(lifecycleObserver)
            exoPlayer?.release() // Libera el reproductor
            // exoPlayer = null // No es necesario setearlo a null aquí, el remember lo manejará
        }
    }

    // Configuración de la vista (PlayerView)
    if (videoUri != null) { // Solo muestra PlayerView si tenemos un URI válido
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    this.player = exoPlayer // Asigna la instancia de ExoPlayer al PlayerView
                    useController = false // No mostrar controles de reproducción
                    // Para que el fondo sea transparente y se vea el .background(Color.Black) de Compose:
                    setBackgroundColor(android.graphics.Color.TRANSPARENT) // Fondo del PlayerView
                    // La transparencia del SurfaceView subyacente de ExoPlayer suele ser buena por defecto
                    // para formatos con alfa (como VP8/VP9 en WebM), pero si no:
                    // this.videoSurfaceView?.holder?.setFormat(PixelFormat.TRANSLUCENT)
                    // (videoSurfaceView es a veces null aquí, puede necesitarse post-layout)
                }
            },
            update = { view ->
                // Actualiza el player en la vista si la instancia de ExoPlayer cambia
                // (útil si el LaunchedEffect recrea exoPlayer por cambio de contexto)
                if (view.player != exoPlayer) {
                    Log.d(TAG_EXOPLAYER, "AndroidView update: Player instance changed, updating view.")
                    view.player = exoPlayer
                }
            },
            modifier = modifier
        )
    } else {
        // Opcional: Muestra algo si el videoUri es nulo (ej. un Box con un icono de error o un texto)
        Log.w(TAG_EXOPLAYER, "videoUri is null, not rendering PlayerView for resourceName: $resourceName")
        // androidx.compose.foundation.layout.Box(modifier = modifier.background(Color.Gray)) { /* Placeholder */ }
    }
}

