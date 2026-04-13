package com.example.privaro.ui.screens.permissions

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import android.accessibilityservice.AccessibilityServiceInfo
import android.view.accessibility.AccessibilityManager
import com.example.privaro.service.PrivaroAccessibilityService
import com.example.privaro.ui.theme.PrivaroTeal
import com.example.privaro.ui.theme.PrivaroTealDark

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionsScreen(
    onBack: () -> Unit,
    onRequestCameraPermission: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // متابعة حالة الصلاحيات
    var cameraStatus by remember { mutableStateOf(checkCamera(context)) }
    var overlayStatus by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
    var accessibilityStatus by remember { mutableStateOf(checkAccessibility(context)) }

    // تحديث الحالة تلقائياً عند العودة للتطبيق
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                cameraStatus = checkCamera(context)
                overlayStatus = Settings.canDrawOverlays(context)
                accessibilityStatus = checkAccessibility(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val isAllReady = cameraStatus && overlayStatus && accessibilityStatus

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Permissions", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PrivaroTeal,
                    titleContentColor = Color.White
                )
            )
        }
    ) { space ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(space)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // كرت الحالة العلوى
            StatusCard(isActive = isAllReady)

            // إذن الكاميرا
            PermissionItem(
                title = "Camera Access",
                info = "Required to scan surroundings for nearby people",
                icon = "\uD83D\uDCF7",
                isActive = cameraStatus,
                onClick = {
                    if (cameraStatus) openSettings(context) else onRequestCameraPermission()
                }
            )

            // إذن العرض فوق التطبيقات
            PermissionItem(
                title = "Overlay Permission",
                info = "Required to show security prompts over other apps",
                icon = "\uD83D\uDCDD",
                isActive = overlayStatus,
                onClick = {
                    val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
                    context.startActivity(intent)
                }
            )

            // إذن خدمة الوصول
            PermissionItem(
                title = "Accessibility Service",
                info = "Required to detect sensitive information being read",
                icon = "\u267F",
                isActive = accessibilityStatus,
                onClick = {
                    context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                }
            )

            // توضيح خطوات تفعيل خدمة الوصول إذا لم تكن مفعلة
            if (!accessibilityStatus) {
                AccessibilityHelpBox()
            }
        }
    }
}

@Composable
private fun StatusCard(isActive: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = if (isActive) PrivaroTeal else Color(0xFF5D4037))
    ) {
        Row(modifier = Modifier.padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(64.dp).background(Color.White.copy(0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(if (isActive) "\uD83D\uDEE1\uFE0F" else "\u26A0\uFE0F", fontSize = 32.sp)
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(if (isActive) "Protection Active" else "Setup Required", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text(if (isActive) "Everything is working correctly" else "Allow permissions to activate", fontSize = 14.sp, color = Color.White.copy(0.8f))
            }
        }
    }
}

@Composable
private fun PermissionItem(
    title: String,
    info: String,
    icon: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(48.dp).background(if (isActive) PrivaroTeal.copy(0.2f) else Color(0xFFFF9800).copy(0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(icon, fontSize = 24.sp)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    text = if (isActive) "Permission Active" else info,
                    fontSize = 13.sp,
                    color = if (isActive) PrivaroTealDark else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (isActive) FontWeight.Medium else FontWeight.Normal
                )
            }
            Button(
                onClick = onClick,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isActive) Color.Gray.copy(0.2f) else MaterialTheme.colorScheme.primary,
                    contentColor = if (isActive) Color.Black else Color.White
                )
            ) {
                Text(if (isActive) "Turn Off" else "Allow", fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun AccessibilityHelpBox() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE0F7F7))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Steps to enable Accessibility Service:",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = PrivaroTealDark
            )
            Spacer(modifier = Modifier.height(12.dp))
            val steps = listOf(
                "1. Tap 'Allow' on the Accessibility card above.",
                "2. Find 'Privaro Privacy Guard' in the downloaded apps list.",
                "3. Switch the toggle to 'On'.",
                "4. Tap 'Allow' in the system popup to confirm."
            )
            steps.forEach { step ->
                Text(
                    text = step,
                    fontSize = 14.sp,
                    color = PrivaroTealDark.copy(alpha = 0.8f),
                    lineHeight = 22.sp,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }
        }
    }
}

// دالات التحقق
private fun checkCamera(context: Context) = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == 0

private fun checkAccessibility(context: Context): Boolean {
    val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
    return am.getEnabledAccessibilityServiceList(-1).any {
        it.resolveInfo.serviceInfo.packageName == context.packageName
    }
}

private fun openSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", context.packageName, null)
    }
    context.startActivity(intent)
}