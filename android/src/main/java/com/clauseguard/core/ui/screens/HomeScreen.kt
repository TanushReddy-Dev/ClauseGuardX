package com.clauseguard.core.ui.screens

import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clauseguard.core.presentation.contract.ContractIntent
import com.clauseguard.core.presentation.contract.ContractSideEffect
import com.clauseguard.core.presentation.viewmodel.ContractViewModel
import kotlinx.coroutines.flow.collectLatest
import java.io.ByteArrayOutputStream
import java.io.InputStream

/**
 * HomeScreen - The primary entry point for ClauseGuard.
 *
 * Implements an Apple-inspired frosted glass aesthetic with fluid
 * animations and delegates all business logic to the ContractViewModel
 * via MVI intents.
 */
@Composable
fun HomeScreen(
    viewModel: ContractViewModel,
    onNavigateToResults: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Observe one-off side effects (navigation, toasts)
    LaunchedEffect(viewModel.sideEffect) {
        viewModel.sideEffect.collectLatest { effect ->
            when (effect) {
                is ContractSideEffect.NavigateToResults -> onNavigateToResults(effect.contractId)
                is ContractSideEffect.ShowToast -> {
                    android.widget.Toast.makeText(context, effect.message, android.widget.Toast.LENGTH_SHORT).show()
                }
                is ContractSideEffect.TriggerHapticFeedback -> {
                    // Assuming a standard haptic utility is available, or use View.performHapticFeedback
                }
            }
        }
    }

    // 1. File Picker for PDF uploads
    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val inputStream: InputStream? = context.contentResolver.openInputStream(it)
                val bytes = inputStream?.readBytes()
                inputStream?.close()

                val filename = getFileName(context, it) ?: "upload.pdf"

                if (bytes != null) {
                    viewModel.handleIntent(ContractIntent.AnalyzeDocument(bytes, filename))
                } else {
                    viewModel.handleIntent(ContractIntent.DismissError) // Placeholder for ShowError
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // 2. Camera Capture for ML Kit OCR
    // TakePicturePreview returns a Bitmap. In a full implementation, you'd use TakePicture
    // to save to a high-res Uri, but for this headless MVP, we convert the Bitmap to a Uri.
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        bitmap?.let {
            // Convert Bitmap to a temporary Uri for the OcrProcessor
            // This is a simplified bridging step for the preview bitmap
            val path = MediaStore.Images.Media.insertImage(context.contentResolver, it, "Title", null)
            val uri = Uri.parse(path)
            viewModel.handleIntent(ContractIntent.CaptureFromOcr(uri))
        }
    }

    // Base UI Layout
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0C)) // Deep dark background
    ) {
        // Decorative background gradient orbs
        Box(
            modifier = Modifier
                .size(400.dp)
                .align(Alignment.TopStart)
                .offset(x = (-100).dp, y = (-100).dp)
                .blur(100.dp)
                .background(Color(0x334F46E5), CircleShape)
        )

        Box(
            modifier = Modifier
                .size(300.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 50.dp, y = 50.dp)
                .blur(100.dp)
                .background(Color(0x338B5CF6), CircleShape)
        )

        // Main Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Hero Section
            Text(
                text = "ClauseGuard",
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                letterSpacing = (-1).sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Extract, analyze, and protect your contracts with edge AI.",
                fontSize = 16.sp,
                color = Color(0xFFA1A1AA), // Zinc 400
                textAlign = TextAlign.Center,
                lineHeight = 24.sp,
                modifier = Modifier.padding(horizontal = 32.dp)
            )

            Spacer(modifier = Modifier.height(64.dp))

            // Action Buttons
            FrostedButton(
                text = "Scan Physical Contract",
                icon = Icons.Rounded.CameraAlt,
                onClick = { cameraLauncher.launch(null) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            FrostedButton(
                text = "Upload PDF or DOCX",
                icon = Icons.Rounded.Description,
                onClick = { pdfPickerLauncher.launch("application/pdf") } // Only PDF supported in MVP
            )
        }

        // Loading Overlay
        AnimatedVisibility(
            visible = uiState.isLoading,
            enter = fadeIn() + scaleIn(initialScale = 0.95f),
            exit = fadeOut() + scaleOut(targetScale = 0.95f),
            modifier = Modifier.fillMaxSize()
        ) {
            LoadingOverlay()
        }
    }
}

/**
 * Apple-inspired Frosted Glass Button
 */
@Composable
private fun FrostedButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }

    // Press animation scale
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "ButtonScale"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .scale(scale)
            .clickable {
                isPressed = true
                onClick()
                // Simple reset for the visual state
                // In a real app, use pointerInput to handle exact down/up events
            },
        shape = RoundedCornerShape(20.dp),
        color = Color(0x1AFFFFFF), // 10% white for frosted glass base
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = Color(0x33FFFFFF) // 20% white border
        )
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = text,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

/**
 * Sleek indeterminate loading animation (pulsing glass orb)
 */
@Composable
private fun LoadingOverlay() {
    // Rotation animation
    val infiniteTransition = rememberInfiniteTransition(label = "Loading")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Rotation"
    )

    // Pulse animation
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Pulse"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x99000000)) // 60% black scrim
            .clickable(enabled = false) {} // Block touches
            .blur(16.dp), // Blur the background behind the overlay
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Glowing Orb
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .scale(scale)
                    .rotate(rotation)
                    .drawBehind {
                        drawCircle(
                            brush = Brush.sweepGradient(
                                colors = listOf(
                                    Color(0xFF8B5CF6), // Purple
                                    Color(0x008B5CF6)
                                )
                            )
                        )
                    }
                    .border(
                        width = 2.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(Color(0xFF8B5CF6), Color(0xFF4F46E5))
                        ),
                        shape = CircleShape
                    )
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Analyzing contract...",
                color = Color.White,
                fontSize = 14.sp,
                letterSpacing = 1.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// Utility function to get filename from Uri
private fun getFileName(context: android.content.Context, uri: Uri): String? {
    var result: String? = null
    if (uri.scheme == "content") {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        try {
            if (cursor != null && cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (index != -1) result = cursor.getString(index)
            }
        } finally {
            cursor?.close()
        }
    }
    if (result == null) {
        result = uri.path
        val cut = result?.lastIndexOf('/')
        if (cut != -1 && cut != null) {
            result = result.substring(cut + 1)
        }
    }
    return result
}