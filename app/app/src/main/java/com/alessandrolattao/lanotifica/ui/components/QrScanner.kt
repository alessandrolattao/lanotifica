package com.alessandrolattao.lanotifica.ui.components

import android.util.Log
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

@Composable
fun QrScanner(onQrCodeScanned: (String) -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val onQrCodeScannedState by rememberUpdatedState(onQrCodeScanned)
    val executor = remember { Executors.newSingleThreadExecutor() }
    val cameraController = remember { LifecycleCameraController(context) }
    val scanner = remember {
        BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build()
        )
    }

    DisposableEffect(lifecycleOwner) {
        cameraController.setImageAnalysisAnalyzer(executor) { imageProxy ->
            val mediaImage = imageProxy.image
            if (mediaImage != null) {
                val image =
                    InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                scanner
                    .process(image)
                    .addOnSuccessListener { barcodes ->
                        barcodes.firstOrNull()?.rawValue?.let { value ->
                            onQrCodeScannedState(value)
                        }
                    }
                    .addOnFailureListener { e -> Log.e("QrScanner", "Barcode scanning failed", e) }
                    .addOnCompleteListener { imageProxy.close() }
            } else {
                imageProxy.close()
            }
        }
        cameraController.bindToLifecycle(lifecycleOwner)
        onDispose {
            cameraController.unbind()
            executor.shutdown()
        }
    }

    AndroidView(
        factory = { ctx -> PreviewView(ctx).apply { controller = cameraController } },
        modifier = modifier.fillMaxSize(),
    )
}
