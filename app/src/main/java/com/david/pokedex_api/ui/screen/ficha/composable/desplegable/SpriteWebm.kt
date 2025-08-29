package com.david.pokedex_api.ui.screen.ficha.composable.desplegable

import android.net.Uri
import android.util.Log
import android.view.TextureView
import androidx.annotation.OptIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
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
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween

                ){
                    Text(
                        text = pokemonDisplayName,
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier
                            .padding(bottom = 16.dp)
                            .weight(0.8f)
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(0.2f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close"
                        )
                    }
                }
                // Usar ExoPlayerComposable
                ExoPlayerSimple(
                    pokemonInputName = pokemonResourceName, // Pasar el nombre del recurso
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f) // Asumiendo que los sprites son cuadrados
                        .clip(RoundedCornerShape(12.dp)) // Clip opcional
//                        .background(Color.Black) // Fondo negro detrás del video transparente
                )

            }
        }
    }
}




@OptIn(UnstableApi::class)
@Composable
fun ExoPlayerSimple(
    pokemonInputName: String, // Nombre como "blastoise-mega"
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val resourceName = remember(pokemonInputName) {

        transformPokemonNameToResourceName(pokemonInputName) // Ej: transforma "blastoise-mega" a "mega_blastoise"

    }

    Log.d("ExoPlayerSimple", "Input: '$pokemonInputName', Transformed resource name: '$resourceName'")

    val resourceId = remember(resourceName) { // `remember` ahora depende del `resourceName` transformado
        context.resources.getIdentifier(resourceName, "raw", context.packageName)
    }

    if (resourceId == 0) {
        Text("Video resource '$resourceName.webm' not found for $pokemonInputName", color = Color.Red, modifier = modifier)
        Log.e("ExoPlayerSimple", "Resource ID is 0 for raw file: $resourceName.webm (from input $pokemonInputName)")
        return
    }

    val videoUri = remember(resourceId) {
        Uri.parse("android.resource://${context.packageName}/$resourceId")
    }

    val exoPlayer = remember(videoUri) { // `remember` el player basado en el URI, para que se recree si cambia
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(videoUri))
            repeatMode = Player.REPEAT_MODE_ONE
            playWhenReady = true
            prepare()
            Log.d("ExoPlayerSimple", "ExoPlayer prepared for URI: $videoUri")
        }
    }

    DisposableEffect(exoPlayer) { // El efecto debe depender de exoPlayer para la limpieza correcta
        onDispose {
            Log.d("ExoPlayerSimple", "Releasing ExoPlayer instance for $resourceName")
            exoPlayer.release()
        }
    }

    // --- Control del ciclo de vida (OPCIONAL PERO RECOMENDADO) ---
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, exoPlayer) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> exoPlayer.pause()
                Lifecycle.Event.ON_RESUME -> exoPlayer.play()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    // --- Fin Control del ciclo de vida ---


    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                player = exoPlayer
                useController = false // No mostrar controles

                // --- INICIO DE AJUSTES DE TRANSPARENCIA ---
                // Para que el fondo de Compose se vea a través de las partes transparentes del WebM
//                setBackgroundColor(android.graphics.Color.BLACK)

                val surfaceView = this.videoSurfaceView
                if (surfaceView is android.view.SurfaceView) {
                    Log.d("ExoPlayerSimple", "Configuring SurfaceView for transparency.")
                    surfaceView.setZOrderOnTop(true)
                    surfaceView.holder.setFormat(android.graphics.PixelFormat.TRANSLUCENT)
                } else if (surfaceView is TextureView) {
                    Log.d("ExoPlayerSimple", "Video surface is TextureView. Setting Opaque to false.")
                    surfaceView.isOpaque = false
                }
                // --- FIN DE AJUSTES DE TRANSPARENCIA ---

                // Originalmente tenías esto, si el objetivo NO es transparencia sino un fondo negro para el video:
//                 setShutterBackgroundColor(android.graphics.Color.BLACK) // Color mientras el video carga
//                 setBackgroundColor(android.graphics.Color.BLACK) // Fondo del PlayerView
            }
        },
        modifier = modifier
    )
}

fun transformPokemonNameToResourceName(pokemonInputName: String): String {
    var inputLower = pokemonInputName.lowercase()
    var partes = listOf<String>()
    if(inputLower.contains("-")){
        partes = inputLower.split("-")
    }
    if(partes.isEmpty()){
        if(inputLower.contains("código")) return "codigo_cero"//caso muy particular
        return inputLower
    }
    else{
        if(partes.size == 2 && partes[1].equals("mega")){
            return "mega_" + partes[0]
        }
        else if(partes.size == 3 && partes[1].equals("mega")){
            return "mega_" + partes[0] + "_" + partes[2]
        }
        //regionales o nombres compuestos
        else {
            when(partes[1]){
                "alola" -> {return partes[0] + "_de_alola"}
                "galar" -> {return partes[0] + "_de_galar"}
                "hisui" -> {return partes[0] + "_de_hisui"}
                "paldea" -> {
                    if(partes[0].equals("tauros")){
                        when(partes[2]){
                            "blaze" -> {return partes[0] + "_de_paldea_ardiente"}
                            "aqua" -> {return partes[0] + "_de_paldea_acuatica"}
                            "combat" -> {return partes[0] + "_de_paldea_combatiente"}
                        }
                    }
                    else{
                        return partes[0] + "_de_paldea"
                    }
                }
                "f" -> {return partes[0] + "_hembra"}
                "m" -> {return partes[0] + "_macho"}
                "shield" -> {return partes[0] + "_escudo"}//falta el filo
                "z" -> {return partes[0] + "_z"}
                else -> {
                    //casos aun más particulares
                    inputLower = partes[0] + "_" + partes[1]
                    inputLower
                }
            }
        }
    }
    return inputLower
}

/*
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
*/