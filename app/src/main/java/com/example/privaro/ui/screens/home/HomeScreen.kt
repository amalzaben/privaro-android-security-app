package com.example.privaro.ui.screens.home

import android.Manifest
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.privaro.data.model.UsageLog
import com.example.privaro.ui.theme.PrivaroTeal
import com.example.privaro.ui.theme.PrivaroTealDark
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun HomeScreen(
    userName: String,
    lastScanResult: UsageLog?,
    onStartScan: () -> Unit,
    onNavigateToPermissions: () -> Unit,
    onNavigateToLogs: () -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // التحقق من حالة الصلاحيات
    var hasCameraPermission by remember { mutableStateOf(checkCameraPermission(context)) }
    var hasOverlayPermission by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
    var isAccessibilityEnabled by remember { mutableStateOf(isAccessibilityServiceEnabled(context)) }

    val allPermissionsGranted = hasCameraPermission && hasOverlayPermission && isAccessibilityEnabled

    Scaffold(
        containerColor = Color(0xFFF2F6F7) // خلفية هادئة جداً
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // --- 1. الهيدر الفخم (تصميم انسيابي) ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .clip(RoundedCornerShape(bottomStart = 50.dp, bottomEnd = 50.dp))
                    .background(Brush.verticalGradient(listOf(PrivaroTeal, PrivaroTealDark)))
                    .padding(horizontal = 24.dp)
            ) {
                // زر تسجيل الخروج
                IconButton(
                    onClick = onLogout,
                    modifier = Modifier.align(Alignment.TopEnd).padding(top = 45.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ExitToApp, "Sign Out", tint = Color.White)
                }

                // عنوان التطبيق (كبير وواضح)
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "PRIVARO",
                        fontSize = 50.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        letterSpacing = 8.sp
                    )
                    Text(
                        text = "Your Privacy Protected",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.85f),
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )

                }
            }

            // --- 2. المحتوى الرئيسي (متداخل مع الهيدر) ---
            Column(
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .offset(y = (-50).dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // كرت الترحيب (الدائرة البيضاء النظيفة)
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(25.dp),
                    color = Color.White,
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier.size(65.dp),
                            shape = CircleShape,
                            color = Color.White,
                            border = BorderStroke(2.dp, PrivaroTeal.copy(0.2f)),
                            shadowElevation = 2.dp
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(userName.take(1).uppercase(), fontSize = 28.sp, fontWeight = FontWeight.Black, color = PrivaroTeal)
                            }
                        }
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text("Welcome, $userName", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                            Text("Ready to feel safe?", fontSize = 14.sp, color = Color.Gray)
                        }
                    }
                }

                // --- 3. زر المسح (إصلاح التشغيل وتصميم مبهر) ---
                Button(
                    onClick = { onStartScan() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(95.dp)
                        .shadow(15.dp, RoundedCornerShape(25.dp)),
                    shape = RoundedCornerShape(25.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrivaroTeal)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(" QUICK CHECK ", fontSize = 22.sp, fontWeight = FontWeight.Black)
                        Text("Make sure your surroundings are safe", fontSize = 12.sp)
                    }
                }

                // --- 4. أزرار الملاحة (تصميم زجاجي مبسط) ---
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    NavigationCard(Modifier.weight(1f), "📜 History", onNavigateToLogs)
                    NavigationCard(Modifier.weight(1f), "⚙️ Settings", onNavigateToPermissions)
                }

                // --- 5. كرت حالة الحماية (لغة غير تقنية) ---
                ProtectionStatusCard(allPermissionsGranted)

                // --- 6. حالة آخر فحص (Accessibility Optimized) ---
                LastScanBrief(lastScanResult)
            }
        }
    }
}

@Composable
fun NavigationCard(modifier: Modifier, label: String, onClick: () -> Unit) {
    Surface(
        modifier = modifier.height(75.dp).clickable { onClick() },
        shape = RoundedCornerShape(22.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFE0E0E0)),
        shadowElevation = 2.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(label, fontWeight = FontWeight.Bold, color = Color(0xFF444444))
        }
    }
}

@Composable
private fun ProtectionStatusCard(isActive: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) Color(0xFFE8F5E9) else Color(0xFFFFF3E0)
        )
    ) {
        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(if (isActive) "🛡️" else "🚨", fontSize = 24.sp)
            Spacer(Modifier.width(16.dp))
            Column {
                Text(
                    text = if (isActive) "You’re protected" else "Protection is off",
                    fontWeight = FontWeight.Bold,
                    color = if (isActive) Color(0xFF2E7D32) else Color(0xFFD32F2F)
                )

                Text(
                    text = if (isActive)
                        "We help protect your privacy when TalkBack speaks."
                    else
                        "Please check Settings and enable required permissions",
                    fontSize = 13.sp,
                    color = Color.Gray
                )


            }
        }
    }
}

@Composable
private fun LastScanBrief(lastScanResult: UsageLog?) {
    val isSafe = (lastScanResult?.peopleDetected ?: 0) == 0
    val time = lastScanResult?.let {
        DateTimeFormatter.ofPattern("hh:mm a").withZone(ZoneId.systemDefault()).format(it.timestamp)
    } ?: "No data"

    Card(
        modifier = Modifier.fillMaxWidth().semantics {
            contentDescription = "Last activity status: ${if (isSafe) "Safe" else "Warning"}. Performed at $time"
        },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(if (isSafe) PrivaroTeal else Color.Red))
            Spacer(Modifier.width(12.dp))
            Text(
                text = if (isSafe) "No scans yet" else "Someone nearby",
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.weight(1f))
            Text(time, fontSize = 12.sp, color = Color.Gray)
        }
    }
}

// دالات التحقق (يجب أن تكون موجودة ليعمل الكود)
private fun checkCameraPermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == android.content.pm.PackageManager.PERMISSION_GRANTED

private fun isAccessibilityServiceEnabled(context: Context): Boolean {
    val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
    return am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK).isNotEmpty()
}