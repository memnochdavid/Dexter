package com.david.pokedex_api.ui.screen.camara

import android.graphics.Bitmap
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import com.david.pokedex_api.R
import com.david.pokedex_api.api.gemini.GeminiRepository
import com.david.pokedex_api.api.gemini.PokemonIdentification
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

// --- Colores Pokedex ---
private val PokedexRed = Color(0xFFCC2222)
private val PokedexRedDark = Color(0xFFA01818)
private val PokedexRedShadow = Color(0xFF7A1010)
private val PokedexBezel = Color(0xFFF0EFEA)
private val PokedexBezelShadow = Color(0xFFD8D6CE)
private val PokedexLensBlue = Color(0xFF2266CC)
private val PokedexLensBlueBright = Color(0xFF55AAFF)
private val PokedexLensRim = Color(0xFFC0C8D0)
private val PokedexLedRed = Color(0xFFFF2222)
private val PokedexLedYellow = Color(0xFFDDCC00)
private val PokedexLedGreen = Color(0xFF22CC44)
private val PokedexGreenLcd = Color(0xFF3A5030)
private val PokedexGreenLcdBright = Color(0xFF6B8B5A)
private val PokedexBlack = Color(0xFF1A1A1A)

private sealed class ScanState {
    data object Idle : ScanState()
    data object Scanning : ScanState()
    data class Result(val identification: PokemonIdentification) : ScanState()
    data class NotFound(val message: String = "No identificado") : ScanState()
    data class Error(val message: String) : ScanState()
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraIdentifyScreen(
    onNavigateToDetails: (String) -> Unit,
    onNavigateBack: () -> Unit
) {
    val cameraPermission = rememberPermissionState(android.Manifest.permission.CAMERA)

    if (cameraPermission.status.isGranted) {
        CameraContent(
            onNavigateToDetails = onNavigateToDetails,
            onNavigateBack = onNavigateBack
        )
    } else {
        PermissionRequest(
            onRequestPermission = { cameraPermission.launchPermissionRequest() },
            onNavigateBack = onNavigateBack
        )
    }
}

@Composable
private fun PermissionRequest(
    onRequestPermission: () -> Unit,
    onNavigateBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PokedexRed)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        IconButton(
            onClick = onNavigateBack,
            modifier = Modifier.padding(8.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Volver",
                tint = Color.White
            )
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                painter = painterResource(id = R.drawable.pokeball_icon),
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = Color.White.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Se necesita acceso a la camara\npara identificar Pokemon",
                color = Color.White,
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )
            Spacer(modifier = Modifier.height(24.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .background(PokedexRedDark)
                    .clickable { onRequestPermission() }
                    .padding(horizontal = 32.dp, vertical = 14.dp)
            ) {
                Text(
                    text = "Conceder permiso",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun CameraContent(
    onNavigateToDetails: (String) -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val geminiRepository = remember { GeminiRepository() }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    var scanState by remember { mutableStateOf<ScanState>(ScanState.Idle) }
    val imageCapture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PokedexRed)
    ) {
        // ====== CABECERA: lente azul + LEDs ======
        PokedexHeader(onNavigateBack = onNavigateBack, scanState = scanState)

        // ====== PANTALLA: bisel blanco + camara ======
        PokedexScreen(
            imageCapture = imageCapture,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 16.dp)
        )

        // ====== CONTROLES: botones + sub-pantalla LCD ======
        PokedexControls(
            scanState = scanState,
            imageCapture = imageCapture,
            cameraExecutor = cameraExecutor,
            geminiRepository = geminiRepository,
            scope = scope,
            haptic = haptic,
            onScanStateChanged = { scanState = it },
            onNavigateToDetails = onNavigateToDetails,
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp, top = 8.dp)
        )
    }
}

// ============================================================
// CABECERA — Lente azul, LEDs, boton volver
// ============================================================

@Composable
private fun PokedexHeader(
    onNavigateBack: () -> Unit,
    scanState: ScanState
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Lente azul grande
            Box(contentAlignment = Alignment.Center) {
                // Aro exterior metalico
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(PokedexLensRim)
                        .border(2.dp, PokedexRedShadow, CircleShape)
                )
                // Lente interior con brillo
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    PokedexLensBlueBright,
                                    PokedexLensBlue,
                                    PokedexLensBlue.copy(alpha = 0.8f)
                                ),
                                center = Offset(30f, 25f),
                                radius = 60f
                            )
                        )
                        .border(2.dp, PokedexLensBlue.copy(alpha = 0.5f), CircleShape)
                )
                // Reflejo
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .offset(x = (-5).dp, y = (-5).dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.4f))
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // 3 LEDs indicadores
            val ledScanningTransition = rememberInfiniteTransition(label = "led_scan")
            val ledPulse by ledScanningTransition.animateFloat(
                initialValue = 0.4f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(tween(500), RepeatMode.Reverse),
                label = "led_pulse"
            )
            val isScanning = scanState is ScanState.Scanning

            LedIndicator(
                color = PokedexLedRed,
                size = 12.dp,
                lit = scanState is ScanState.Error || scanState is ScanState.NotFound,
                pulse = false,
                alpha = 1f
            )
            Spacer(modifier = Modifier.width(6.dp))
            LedIndicator(
                color = PokedexLedYellow,
                size = 12.dp,
                lit = isScanning,
                pulse = isScanning,
                alpha = if (isScanning) ledPulse else 1f
            )
            Spacer(modifier = Modifier.width(6.dp))
            LedIndicator(
                color = PokedexLedGreen,
                size = 12.dp,
                lit = scanState is ScanState.Result,
                pulse = false,
                alpha = 1f
            )

            Spacer(modifier = Modifier.weight(1f))

            // Boton volver
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Volver",
                    tint = Color.White.copy(alpha = 0.9f)
                )
            }
        }
    }

    // Linea decorativa de separacion (simula el escalon de la carcasa)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(4.dp)
            .background(PokedexRedDark)
    )
}

@Composable
private fun LedIndicator(
    color: Color,
    size: Dp,
    lit: Boolean,
    pulse: Boolean,
    alpha: Float
) {
    val displayAlpha = if (lit) alpha else 0.25f
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(color.copy(alpha = displayAlpha))
            .border(1.dp, PokedexRedShadow, CircleShape)
    )
}

// ============================================================
// PANTALLA — Bisel blanco con esquina recortada + camara
// ============================================================

@Composable
private fun PokedexScreen(
    imageCapture: ImageCapture,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Bisel blanco exterior
    Box(
        modifier = modifier
            .padding(top = 8.dp)
            .clip(PokedexScreenShape)
            .background(PokedexBezel)
            .border(2.dp, PokedexBezelShadow, PokedexScreenShape)
            .padding(3.dp)
    ) {
        Column {
            // Dos puntos rojos sobre la pantalla
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 48.dp, top = 6.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(PokedexLedRed.copy(alpha = 0.7f))
                )
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(PokedexLedRed.copy(alpha = 0.7f))
                )
            }

            // Pantalla de camara (area negra/preview)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 6.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(PokedexBlack)
            ) {
                AndroidView(
                    factory = { ctx ->
                        PreviewView(ctx).also { previewView ->
                            previewView.scaleType = PreviewView.ScaleType.FILL_CENTER
                            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                            cameraProviderFuture.addListener({
                                val cameraProvider = cameraProviderFuture.get()
                                val preview = Preview.Builder().build().also {
                                    it.surfaceProvider = previewView.surfaceProvider
                                }
                                try {
                                    cameraProvider.unbindAll()
                                    cameraProvider.bindToLifecycle(
                                        lifecycleOwner,
                                        CameraSelector.DEFAULT_BACK_CAMERA,
                                        preview,
                                        imageCapture
                                    )
                                } catch (e: Exception) {
                                    Log.e("CameraIdentify", "Error binding camera", e)
                                }
                            }, ContextCompat.getMainExecutor(ctx))
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Lineas de altavoz bajo la pantalla
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(4) {
                    Box(
                        modifier = Modifier
                            .width(20.dp)
                            .height(2.dp)
                            .background(PokedexRedDark.copy(alpha = 0.4f))
                    )
                    if (it < 3) Spacer(modifier = Modifier.width(3.dp))
                }
            }
        }
    }
}

/** Forma del bisel: rectangulo redondeado con esquina superior izquierda cortada en angulo */
private val PokedexScreenShape = GenericShape { size, _ ->
    val cornerCut = size.width * 0.15f  // tamaño del corte diagonal
    val r = 12f                         // radio de esquinas normales

    moveTo(cornerCut, 0f)
    // Borde superior
    lineTo(size.width - r, 0f)
    // Esquina superior derecha (redondeada)
    quadraticTo(size.width, 0f, size.width, r)
    // Borde derecho
    lineTo(size.width, size.height - r)
    // Esquina inferior derecha
    quadraticTo(size.width, size.height, size.width - r, size.height)
    // Borde inferior
    lineTo(r, size.height)
    // Esquina inferior izquierda
    quadraticTo(0f, size.height, 0f, size.height - r)
    // Borde izquierdo
    lineTo(0f, cornerCut)
    // Corte diagonal (esquina superior izquierda)
    close()
}

// ============================================================
// CONTROLES — Boton scan, sub-pantalla LCD, resultado
// ============================================================

@Composable
private fun PokedexControls(
    scanState: ScanState,
    imageCapture: ImageCapture,
    cameraExecutor: java.util.concurrent.ExecutorService,
    geminiRepository: GeminiRepository,
    scope: kotlinx.coroutines.CoroutineScope,
    haptic: androidx.compose.ui.hapticfeedback.HapticFeedback,
    onScanStateChanged: (ScanState) -> Unit,
    onNavigateToDetails: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            // Columna izquierda: boton de escaneo + boton secundario
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(end = 12.dp)
            ) {
                // Boton scan principal (negro, redondo)
                val isScanning = scanState is ScanState.Scanning
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(
                            if (isScanning) PokedexBlack.copy(alpha = 0.5f)
                            else PokedexBlack
                        )
                        .border(3.dp, PokedexRedShadow, CircleShape)
                        .then(
                            if (!isScanning) Modifier.clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onScanStateChanged(ScanState.Scanning)
                                imageCapture.takePicture(
                                    cameraExecutor,
                                    object : ImageCapture.OnImageCapturedCallback() {
                                        override fun onCaptureSuccess(imageProxy: ImageProxy) {
                                            val bitmap = imageProxy.toBitmap()
                                            imageProxy.close()
                                            scope.launch {
                                                val result = geminiRepository.identifyPokemon(bitmap)
                                                onScanStateChanged(
                                                    if (result != null) ScanState.Result(result)
                                                    else ScanState.NotFound()
                                                )
                                            }
                                        }

                                        override fun onError(exception: androidx.camera.core.ImageCaptureException) {
                                            Log.e("CameraIdentify", "Capture error", exception)
                                            onScanStateChanged(ScanState.Error("Error de captura"))
                                        }
                                    }
                                )
                            } else Modifier
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.pokeball_icon),
                        contentDescription = "Escanear",
                        modifier = Modifier.size(28.dp),
                        tint = if (isScanning) Color.White.copy(alpha = 0.3f) else Color.White
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Boton rectangular pequeño (rojo oscuro, decorativo / reintentar)
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(16.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(PokedexRedDark)
                        .border(1.dp, PokedexRedShadow, RoundedCornerShape(4.dp))
                        .clickable {
                            if (scanState !is ScanState.Scanning) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onScanStateChanged(ScanState.Idle)
                            }
                        }
                )
            }

            // Sub-pantalla LCD verde (resultado)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(80.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(PokedexGreenLcd, PokedexGreenLcdBright.copy(alpha = 0.7f))
                        )
                    )
                    .border(2.dp, PokedexRedShadow, RoundedCornerShape(4.dp))
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                LcdContent(
                    scanState = scanState,
                    onViewDetails = { name ->
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onNavigateToDetails(name)
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Fila inferior: D-pad decorativo + boton azul
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // D-pad (decorativo / navegacion futura)
            DPad(modifier = Modifier.size(64.dp))

            // Boton azul rectangular
            Box(
                modifier = Modifier
                    .width(48.dp)
                    .height(20.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(PokedexLensBlue)
                    .border(1.dp, PokedexLensBlue.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
            )
        }
    }
}

@Composable
private fun LcdContent(
    scanState: ScanState,
    onViewDetails: (String) -> Unit
) {
    when (scanState) {
        is ScanState.Idle -> {
            Text(
                text = "LISTO PARA\nESCANEAR",
                color = Color(0xFF1A3010),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                textAlign = TextAlign.Center,
                lineHeight = 16.sp
            )
        }
        is ScanState.Scanning -> {
            val infiniteTransition = rememberInfiniteTransition(label = "lcd_dots")
            val dotCount by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 4f,
                animationSpec = infiniteRepeatable(tween(800), RepeatMode.Restart),
                label = "dots"
            )
            Text(
                text = "ANALIZANDO" + ".".repeat(dotCount.toInt()),
                color = Color(0xFF1A3010),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                textAlign = TextAlign.Center
            )
        }
        is ScanState.Result -> {
            val identification = scanState.identification
            val displayName = identification.name.uppercase().replace("-", " ")

            if (identification.confidence == "high") {
                LaunchedEffect(identification.name) {
                    delay(600)
                    onViewDetails(identification.name)
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { onViewDetails(identification.name) },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = displayName,
                    color = Color(0xFF1A3010),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
                Text(
                    text = when (identification.confidence) {
                        "high" -> ">> ABRIENDO FICHA..."
                        "medium" -> "TOCA PARA VER FICHA"
                        else -> "POSIBLE COINCIDENCIA"
                    },
                    color = Color(0xFF1A3010).copy(alpha = 0.7f),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    textAlign = TextAlign.Center
                )
            }
        }
        is ScanState.NotFound -> {
            Text(
                text = "NO IDENTIFICADO\nINTENTA DE NUEVO",
                color = Color(0xFF1A3010),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                textAlign = TextAlign.Center,
                lineHeight = 15.sp
            )
        }
        is ScanState.Error -> {
            Text(
                text = "ERROR\n${scanState.message.uppercase()}",
                color = Color(0xFF3A1010),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                textAlign = TextAlign.Center,
                lineHeight = 15.sp
            )
        }
    }
}

// ============================================================
// D-PAD decorativo
// ============================================================

@Composable
private fun DPad(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        // Cruz vertical
        Box(
            modifier = Modifier
                .width(20.dp)
                .height(56.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(PokedexBlack)
        )
        // Cruz horizontal
        Box(
            modifier = Modifier
                .width(56.dp)
                .height(20.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(PokedexBlack)
        )
        // Centro
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(PokedexBlack.copy(alpha = 0.8f))
        )
    }
}
