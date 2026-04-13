package com.example.privaro.ui.overlay

import androidx.compose.foundation.BorderStroke // التأكد من وجود هذا الـ Import
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.privaro.ui.theme.PrivaroTeal

@Composable
fun ScanResultOverlay(
    peopleDetected: Int,
    frontCount: Int,
    backCount: Int,
    onContinue: () -> Unit,
    onCancel: () -> Unit,
    onScanAgain: (() -> Unit)? = null
) {
    val isSafe = peopleDetected == 0
    val cardBackground = Color(0xFF161622)
    val dangerRed = Color(0xFFFF4B4B)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f)),
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
            colors = CardDefaults.cardColors(containerColor = cardBackground)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 36.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // الأيقونة العلوية
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = if (isSafe)
                                    listOf(PrivaroTeal.copy(0.15f), Color.Transparent)
                                else
                                    listOf(dangerRed.copy(0.15f), Color.Transparent)
                            ),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = if (isSafe) "🛡️" else "⚠️", fontSize = 42.sp)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = if (isSafe) "All Clear" else "Security Alert",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isSafe) PrivaroTeal else dangerRed
                )

                Spacer(modifier = Modifier.height(12.dp))

                // وصف الحالة
                Text(
                    text = if (isSafe)
                        "Your surroundings are clear.\nSafe to proceed privately."
                    else
                        "Someone might see your screen or hear audio.\nTalkBack has been paused for your safety.",
                    fontSize = 15.sp,
                    color = Color.White.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )

                Spacer(modifier = Modifier.height(32.dp))

                // الأزرار المطلوبة فقط
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    if (isSafe) {
                        ActionButton("Continue", PrivaroTeal, Color.Black, onContinue)
                    } else {
                        // الكبسة الأولى: إعادة الفحص
                        if (onScanAgain != null) {
                            ActionButton("Check Surroundings Again", PrivaroTeal, Color.Black, onScanAgain)
                        }

                        // الكبسة الثانية: القراءة على أي حال (تم تصحيح BorderStroke هنا)
                        OutlinedButton(
                            onClick = onContinue,
                            modifier = Modifier.fillMaxWidth().height(58.dp),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, Color.White.copy(0.2f))
                        ) {
                            Text("Read Anyway", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        }

                        // زر إغلاق فرعي
                        TextButton(
                            onClick = onCancel,
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            Text("Close and Stop", color = Color.White.copy(0.4f), fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionButton(
    text: String,
    bgColor: Color,
    txtColor: Color,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(58.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = bgColor, contentColor = txtColor),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
    ) {
        Text(text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}