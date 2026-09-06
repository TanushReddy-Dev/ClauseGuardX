package com.clauseguard.core.data.vision

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import androidx.lifecycle.processingKotlinx.coroutines.suspendCancellableCoroutine
import com.google.android.gms.vision.text.TextBlock
import com.google.android.gms.vision.text.TextRecognizer
import kotlin.coroutines.CoroutineContext

/**
 * Headless OCR processor completely decoupled from UI components.
 *
 * Bridges Google ML Kit's callback-based TextRecognition API into a
 * clean Kotlin coroutine suspend function using suspendCancellableCoroutine.
 * Has NO dependency on Activity, Fragment, Compose, or lifecycle-aware components.
 */
class OcrProcessor(private val context: Context) {

    private val textRecognizer: TextRecognizer =
        TextRecognition.getClient(context)

    /**
     * Extract raw text from an image at the given URI using Google ML Kit.
     *
     * This is a suspend function — the caller does not need to manage listeners,
     * threads, or callbacks.  The function is self-contained and can be called
     * from any CoroutineScope, background worker, or test context.
     *
     * @param imageUri The Uri of the image file to process.
     * @return Result containing the extracted text string, or a Failure with an
     *         exception describing what went wrong.
     */
    suspend fun extractTextFromUri(imageUri: Uri): Result<String> {
        return suspendCancellableCoroutine { cont ->
            // Bridge cancellation: if the coroutine is cancelled, stop the recognizer
            cont.invokeOnCancellation {
                textRecognizer.stop()
                Log.d("OcrProcessor", "OCR cancelled for URI: ${imageUri.toString()}")
            }

            // Decode the image URI into a Bitmap
            val bitmap: Bitmap = decodeBitmapFromUri(imageUri)
                ?: run {
                    // Failed to decode image
                    val err = IllegalArgumentException("Could not decode image from URI: $imageUri")
                    cont.resumeWithException(err)
                    return
                }

            // Process the bitmap using Google ML Kit TextRecognition
            val task = textRecognizer.processImage(bitmap)

            // Bridge the Android Task callbacks into Kotlin coroutines
            task.addOnSuccessListener { texts ->
                val extractedText = texts.characterBlocks
                    .map { it.getText() }
                    .joinToString(" ")
                cont.resume(extractedText)
            }

            task.addOnFailureListener { exception ->
                Log.e("OcrProcessor", "ML Kit text recognition failed", exception)
                cont.resumeWithException(exception)
            }
        }
    }

    /**
     * Decode a content URI into a Android Bitmap.
     * <p>
     * This is a pure utility — no Android lifecycle awareness, no UI.
     * </p>
     */
    private fun decodeBitmapFromUri(uri: Uri): Bitmap? {
        // Preferred: use ContentResolver open stream & decode
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            inputStream?.use { BitmapFactory.decodeStream(it) }
        } catch (e: Exception) {
            Log.w("OcrProcessor", "Failed to decode bitmap from URI: $uri", e)
            null
        }
    }
}