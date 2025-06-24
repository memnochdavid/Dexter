package com.david.pokedex_api.camera.composable

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.YuvImage
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import java.io.ByteArrayOutputStream
import android.graphics.Matrix
import androidx.compose.animation.core.copy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.camera.core.Preview as CameraXPreview // O el alias que prefieras, ej: CameraPreview
import java.util.concurrent.ExecutorService
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

// (Añade las funciones para convertir ImageProxy a Bitmap)



fun takePhoto(
    context: Context, // Contexto, si es necesario para algo (ej. archivos temporales)
    imageCapture: ImageCapture,
    executor: ExecutorService, // <--- PARÁMETRO AÑADIDO/MODIFICADO
    onImageCaptured: (Bitmap) -> Unit,
    onError: (ImageCaptureException) -> Unit
) {
    imageCapture.takePicture(
        executor, // <--- USA EL EXECUTOR PROPORCIONADO
        object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(image: ImageProxy) {
                // Convertir ImageProxy a Bitmap
                val buffer = image.planes[0].buffer
                val bytes = ByteArray(buffer.remaining())
                buffer.get(bytes)
                var bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

                // Corregir la rotación del bitmap si es necesario
                // La rotación se obtiene de image.imageInfo.rotationDegrees
                val rotationDegrees = image.imageInfo.rotationDegrees
                if (rotationDegrees != 0) {
                    val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
                    bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                }

                onImageCaptured(bitmap)
                image.close() // ¡Muy importante cerrar ImageProxy!
            }

            override fun onError(exception: ImageCaptureException) {
                onError(exception)
            }
        }
    )
}




@Composable
fun SimpleCameraView(
    executor: ExecutorService, // Ya no es necesario crear uno nuevo aquí, se recibe como parámetro
    onImageCaptureReady: (ImageCapture) -> Unit,
    onError: (Exception) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    // No necesitamos imageCaptureInstance aquí si lo manejamos en el callback onImageCaptureReady

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            // Crear la instancia de ImageCapture aquí
            val newImageCapture = ImageCapture.Builder()
                // Puedes añadir configuraciones aquí si es necesario, ej:
                // .setTargetRotation(previewView.display.rotation)
                // .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()

            // Invocar el callback en cuanto ImageCapture está construido.
            onImageCaptureReady(newImageCapture)

            try {
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()

                    // Usar el alias para la Preview de CameraX
                    val preview = CameraXPreview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    // Seleccionar la cámara trasera por defecto
                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                    // Desvincular todos los casos de uso anteriores para evitar conflictos
                    cameraProvider.unbindAll()

                    // Vincular los casos de uso (preview e imageCapture) al ciclo de vida
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        newImageCapture // Usamos la instancia creada
                    )
                }, ContextCompat.getMainExecutor(ctx)) // Usar el executor principal para UI
            } catch (e: Exception) {
                Log.e("SimpleCameraView", "Error al inicializar la cámara: ${e.message}", e)
                onError(e) // Propagar el error
            }
            previewView // Devuelve la PreviewView para ser mostrada
        }
        // No es necesario un update block aquí si la configuración no cambia dinámicamente
        // después de la creación inicial de esta manera.
    )
}