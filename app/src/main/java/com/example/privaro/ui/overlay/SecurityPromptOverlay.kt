package com.example.privaro.ui.overlay

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.privaro.detection.SensitiveContentType
import com.example.privaro.ui.theme.PrivaroTeal

/**
 * UI Overlay only (does NOT auto-run by itself).
 */
@Composable
fun SecurityPromptOverlay(
    contentType: SensitiveContentType,
    description: String,
    onContinue: () -> Unit,
    onCheckSurroundings: () -> Unit,
    onCancel: () -> Unit
) {
    val cardBackground = Color(0xFF161622)
    val softWhite = Color(0xFFF0F0F0)

    val accessibilityText =
        "Security alert. ${getDescriptionForType(contentType)} " +
                "Someone might see your screen or hear the audio. " +
                "Choose Check Surroundings, Continue Anyway, or Close and Stop."

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f))
            .semantics {
                // Important: TalkBack reads this immediately (good for warnings)
                contentDescription = accessibilityText
                liveRegion = LiveRegionMode.Assertive
            },
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        listOf(Color.White.copy(0.1f), Color.Transparent)
                    ),
                    shape = RoundedCornerShape(32.dp)
                ),
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(containerColor = cardBackground),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 36.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Icon with glow
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(PrivaroTeal.copy(0.15f), Color.Transparent)
                            ),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = getIconForType(contentType),
                        fontSize = 42.sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Security Alert",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Main message
                Text(
                    text = buildString {
                        append(getDescriptionForType(contentType))
                        if (description.isNotBlank()) {
                            append("\n")
                            append(description)
                        }
                        append("\nSomeone might see your screen or hear the audio.\n")
                        append("TalkBack has been paused for your safety.")
                    },
                    fontSize = 15.sp,
                    color = softWhite.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )

                Spacer(modifier = Modifier.height(32.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Button(
                        onClick = onCheckSurroundings,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(58.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrivaroTeal,
                            contentColor = Color.Black
                        )
                    ) {
                        Text(
                            text = "Check Surroundings",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    OutlinedButton(
                        onClick = onContinue,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(58.dp),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color.White.copy(0.2f))
                    ) {
                        Text(
                            text = "Continue Anyway",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    TextButton(
                        onClick = onCancel,
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Text(
                            text = "Close and Stop",
                            color = Color.White.copy(0.4f),
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

/**
 * ✅ This is the "missing piece": a host that controls WHEN the overlay appears.
 * You can call requestPrompt(...) from anywhere to show it.
 */
class SecurityPromptController {
    val show: MutableState<Boolean> = mutableStateOf(false)
    val type: MutableState<SensitiveContentType> = mutableStateOf(SensitiveContentType.NONE)
    val description: MutableState<String> = mutableStateOf("")
    val onAllowedAction: MutableState<(() -> Unit)?> = mutableStateOf(null)

    fun requestPrompt(
        contentType: SensitiveContentType,
        desc: String = "",
        onAllowed: () -> Unit
    ) {
        type.value = contentType
        description.value = desc
        onAllowedAction.value = onAllowed
        show.value = true
    }

    fun dismiss() {
        show.value = false
    }
}

/**
 * Place this once near the root of your screen.
 */
@Composable
fun SecurityPromptHost(
    controller: SecurityPromptController,
    onCheckSurroundings: () -> Unit,
    onCancel: () -> Unit
) {
    if (controller.show.value) {
        SecurityPromptOverlay(
            contentType = controller.type.value,
            description = controller.description.value,
            onCheckSurroundings = {
                controller.dismiss()
                onCheckSurroundings()
            },
            onContinue = {
                val action = controller.onAllowedAction.value
                controller.dismiss()
                action?.invoke() // ✅ run the protected action (e.g., speak/reveal)
            },
            onCancel = {
                controller.dismiss()
                onCancel()
            }
        )
    }
}

/**
 * Example usage inside any screen.
 * Replace `doSensitiveRead()` with your real logic (TTS / show password / etc.)
 */
@Composable
fun ExampleScreen() {
    val controller = remember { SecurityPromptController() }

    Box(modifier = Modifier.fillMaxSize()) {

        // Your normal UI here...
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Text("Demo Screen", fontSize = 22.sp, fontWeight = FontWeight.Bold)

            Spacer(Modifier.height(12.dp))

            Button(
                onClick = {
                    // Pretend we detected OTP is about to be read aloud.
                    controller.requestPrompt(
                        contentType = SensitiveContentType.OTP,
                        desc = "A verification code is about to be read aloud.",
                        onAllowed = {
                            // This runs ONLY after user presses Continue Anyway
                            doSensitiveRead()
                        }
                    )
                }
            ) {
                Text("Trigger Security Prompt (OTP)")
            }
        }

        // ✅ Host draws overlay above everything when needed
        SecurityPromptHost(
            controller = controller,
            onCheckSurroundings = {
                // TODO: navigate to camera scan / start scan pipeline
                // e.g., navController.navigate("scan")
            },
            onCancel = {
                // TODO: cancel reading / close flow
            }
        )
    }
}

/**
 * Replace with your actual sensitive action:
 * - TextToSpeech.speak(...)
 * - Reveal password
 * - Read detected text, etc.
 */
private fun doSensitiveRead() {
    // Your code here
}

/** Helpers */
private fun getIconForType(type: SensitiveContentType): String {
    return when (type) {
        SensitiveContentType.PASSWORD -> "🔑"
        SensitiveContentType.OTP -> "🔢"
        SensitiveContentType.CARD_NUMBER -> "💳"
        SensitiveContentType.CVV -> "🛡️"
        SensitiveContentType.BANK_ACCOUNT -> "🏦"
        SensitiveContentType.PIN -> "⌨️"
        SensitiveContentType.NONE -> "⚠️"
    }
}

private fun getDescriptionForType(type: SensitiveContentType): String {
    val item = when (type) {
        SensitiveContentType.PASSWORD -> "Your password"
        SensitiveContentType.OTP -> "Your verification code"
        SensitiveContentType.CARD_NUMBER -> "Your card details"
        SensitiveContentType.CVV -> "Your security code"
        SensitiveContentType.BANK_ACCOUNT -> "Your bank info"
        SensitiveContentType.PIN -> "Your PIN"
        SensitiveContentType.NONE -> "Private information"
    }
    return "$item is about to be read aloud."
}
