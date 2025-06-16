package com.david.pokedex_api.ui.composables

import androidx.annotation.RawRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.*
// Asegúrate de tener esta importación si no la tienes,
// reemplazando com.david.pokedex_api con tu package name real para acceder a R
// import com.david.pokedex_api.R


/**
 * Un Composable para mostrar una animación Lottie desde un recurso raw.
 *
 * @param modifier Modificador a aplicar a este Composable.
 * @param rawResId El ID del recurso raw (ej. R.raw.your_animation).
 * @param iterations El número de veces que la animación debe repetirse.
 *                   Usa [LottieConstants.IterateForever] para repetición infinita.
 * @param speed La velocidad de reproducción de la animación. 1.0f es la velocidad normal.
 * @param size El tamaño deseado para la animación Lottie. Si es null,
 *             el LottieAnimation ocupará el espacio disponible o su tamaño intrínseco.
 * @param contentAlignment Alineación del Lottie dentro de su Box contenedor,
 *                         útil si se especifica un tamaño para el Box y el Lottie es más pequeño.
 */
@Composable
fun Lottie(
    modifier: Modifier = Modifier,
    @RawRes rawResId: Int,
    iterations: Int = LottieConstants.IterateForever,
    speed: Float = 1.0f,
    size: Dp? = null, // Tamaño opcional para el Lottie
    contentAlignment: Alignment = Alignment.Center,
    onClick: (() -> Unit)? = null // <--- NUEVO PARÁMETRO onClick
) {
    val compositionResult: LottieCompositionResult = rememberLottieComposition(
        spec = LottieCompositionSpec.RawRes(rawResId)
    )
    val progress by animateLottieCompositionAsState(
        composition = compositionResult.value, // Accede al valor de LottieCompositionResult
        iterations = iterations,
        speed = speed,
        isPlaying = true, // Asegura que la animación comience a reproducirse
        restartOnPlay = false // Puedes ajustarlo si necesitas reiniciar en cada recomposición (generalmente false)
    )


    var clickableModifier = modifier
    if (onClick != null) {
        clickableModifier = clickableModifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onClick,
            role = Role.Button // Semántica apropiada si actúa como un botón
        )
    }

    val boxModifier = if (size != null) {
        clickableModifier.size(size)
    } else {
        clickableModifier
    }

    Box(modifier = clickableModifier, contentAlignment = contentAlignment) {
        if (compositionResult.isSuccess) {
            LottieAnimation(
                composition = compositionResult.value,
                progress = { progress },
                applyOpacityToLayers = true,
                // El modificador aquí es para el LottieAnimation interno,
                // que podría ser útil si no se especifica un tamaño para el Box.
                // Si el Box tiene un tamaño, LottieAnimation se ajustará a él.
                modifier = Modifier.matchParentSize() // Para que el Lottie llene el Box si el Box tiene tamaño
            )
        } else if (compositionResult.isLoading) {
            // Opcional: Mostrar un placeholder o un loader mientras carga la composición
            // Por ejemplo: CircularProgressIndicator()
            // O simplemente no mostrar nada hasta que esté listo
        } else if (compositionResult.isFailure) {
            // Opcional: Mostrar un indicador de error si la composición falla al cargar
            // Por ejemplo: Text("Failed to load animation")
            // Log.e("LottieAnimationView", "Failed to load Lottie composition: ${compositionResult.error}")
        }
    }
}

// Preview (necesitarás un archivo lottie de ejemplo en res/raw/ para que funcione)
// Por ejemplo, si tienes un archivo llamado "loader.json" en res/raw/
/*
@Preview(showBackground = true)
@Composable
fun LottieAnimationViewPreview() {
    // Reemplaza con tu R real si el preview está en otro módulo
    // y asegúrate de que R.raw.loader existe.
    // Si R no se resuelve, asegúrate que el package de R está importado correctamente.
    // Es posible que necesites limpiar y reconstruir el proyecto.
    val sampleRawResId = 0 // Reemplaza con R.raw.tu_animacion_de_ejemplo si la tienes
    if (sampleRawResId != 0) {
         LottieAnimationView(
            rawResId = sampleRawResId, // Ejemplo: R.raw.loader
            size = 100.dp,
            iterations = LottieConstants.IterateForever
        )
    } else {
        Text("Add a sample Lottie to res/raw for preview")
    }
}
*/