package com.example.privaro

import android.Manifest
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.privaro.data.auth.AuthRepository
import com.example.privaro.data.log.LogRepository
import com.example.privaro.data.model.UsageLog
import com.example.privaro.ui.overlay.OverlayManager
import com.example.privaro.ui.screens.auth.AuthScreen
import com.example.privaro.ui.screens.home.HomeScreen
import com.example.privaro.ui.screens.logs.LogsScreen
import com.example.privaro.ui.screens.permissions.PermissionsScreen
import com.example.privaro.ui.theme.PrivaroTeal
import com.example.privaro.ui.theme.PrivaroTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* Permission results handled by UI state */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PrivaroTheme {
                PrivaroApp(
                    onRequestCameraPermission = {
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                )
            }
        }
    }
}

enum class Screen { Home, Permissions, Logs }

@Composable
fun PrivaroApp(onRequestCameraPermission: () -> Unit) {
    val context = LocalContext.current
    val webClientId = context.getString(R.string.default_web_client_id)

    //  (Onboarding)
    var showOnboarding by remember { mutableStateOf(!hasCompletedOnboarding(context)) }

    val authRepository = remember { AuthRepository.getInstance(context) }
    val currentUser by authRepository.authStateFlow().collectAsState(initial = authRepository.currentUser)
    val isAuthenticated = currentUser != null

    when {
        showOnboarding -> {
            OnboardingScreen(onComplete = {
                setOnboardingComplete(context)
                showOnboarding = false
            })
        }
        !isAuthenticated -> {
            AuthScreen(onAuthSuccess = {}, webClientId = webClientId)
        }
        else -> {
            MainNavigation(
                userName = currentUser?.displayName ?: currentUser?.email?.substringBefore("@") ?: "User",
                onRequestCameraPermission = onRequestCameraPermission
            )
        }
    }
}

@Composable
fun MainNavigation(userName: String, onRequestCameraPermission: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()
    var currentScreen by remember { mutableStateOf(Screen.Home) }

    val authRepository = remember { AuthRepository.getInstance(context) }
    val logRepository = remember { LogRepository.getInstance(context) }
    var lastScanResult by remember { mutableStateOf<UsageLog?>(logRepository.getLastScanLog()) }
    var scanLogs by remember { mutableStateOf(logRepository.getScanLogs()) }
    val overlayManager = remember { OverlayManager(context) }

    // تحديث البيانات عند فتح التطبيق أو العودة إليه
    LaunchedEffect(currentScreen) {
        logRepository.fetchLogsFromBackend()
        lastScanResult = logRepository.getLastScanLog()
        scanLogs = logRepository.getScanLogs()
    }

    // الانتقال السلس بين الشاشات
    AnimatedContent(targetState = currentScreen, label = "navigation_fade") { screen ->
        when (screen) {
            Screen.Home -> HomeScreen(
                userName = userName,
                lastScanResult = lastScanResult,
                onStartScan = {
                    overlayManager.startManualScan { peopleDetected ->
                        logRepository.logScanPerformed(peopleDetected, if (peopleDetected == 0) "safe" else "people_detected")
                        lastScanResult = logRepository.getLastScanLog()
                    }
                },
                onNavigateToPermissions = { currentScreen = Screen.Permissions },
                onNavigateToLogs = { currentScreen = Screen.Logs },
                onLogout = {
                    logRepository.clearLogs()
                    authRepository.signOut()
                }
            )
            Screen.Permissions -> PermissionsScreen(
                onBack = { currentScreen = Screen.Home },
                onRequestCameraPermission = onRequestCameraPermission
            )
            Screen.Logs -> LogsScreen(
                logs = scanLogs,
                onBack = { currentScreen = Screen.Home }
            )
        }
    }
}

@Composable
fun OnboardingScreen(onComplete: () -> Unit) {
    var currentPage by remember { mutableIntStateOf(0) }
    val pages = listOf(
        OnboardingPage(
            R.drawable.img,
            "Welcome to Privaro",
            "",
            "Your privacy assistant. We help keep your information safe.",
            "Get Started"
        ),

        OnboardingPage(
            R.drawable.unbord2,
            "Privacy Protection",
            "",
            "We check if someone is nearby before TalkBack speaks.",
            "Continue"
        ),

        OnboardingPage(
            R.drawable.img_1,
            "You Are in Control",
            "",
            "See what happened and change settings anytime.",
            "Finish Setup"
        )
            )

                Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color.White, Color(0xFFF9F9F9))))
    ) {
        // زر الرجوع - مصمم ليكون أول هدف للمكفوفين عند الحاجة للعودة
        if (currentPage > 0) {
            IconButton(
                onClick = { currentPage-- },
                modifier = Modifier
                    .padding(top = 48.dp, start = 12.dp)
                    .semantics { contentDescription = "Back to previous page" }
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = PrivaroTeal)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .padding(top = 100.dp, bottom = 50.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // منطقة المحتوى الأساسية - دمج النصوص للمكفوفين
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clearAndSetSemantics {
                        contentDescription = "${pages[currentPage].title} ${pages[currentPage].highlightWord}. ${pages[currentPage].description}. Page ${currentPage + 1} of 3"
                    },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // منطقة عرض الصورة بحيث تعبي الدائرة بالكامل
                Box(contentAlignment = Alignment.Center) {
                    Surface(
                        modifier = Modifier.size(200.dp), // حجم الدائرة
                        shape = CircleShape,
                        color = Color.White,
                        shadowElevation = 4.dp,
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEEEEEE))
                    ) {
                        Image(
                            painter = painterResource(id = pages[currentPage].imageRes),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(), // بتخلي الصورة تعبي كل مساحة الـ Surface
                            contentScale = ContentScale.Crop // أهم خاصية: بتخلي الصورة تعبي الدائرة تماماً
                        )
                    }
                }

                Spacer(modifier = Modifier.height(48.dp))

                Text(
                    text = buildAnnotatedString {
                        append(pages[currentPage].title + " ")
                        withStyle(SpanStyle(color = PrivaroTeal, fontWeight = FontWeight.Bold)) {
                            append(pages[currentPage].highlightWord)
                        }
                    },
                    fontSize = 30.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                    color = Color(0xFF2D3142)
                )

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = pages[currentPage].description,
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center,
                    color = Color(0xFF6B7280),
                    lineHeight = 26.sp
                )
            }

            // القسم السفلي: المؤشرات والزر
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(
                    Modifier.semantics { contentDescription = "Step ${currentPage + 1} of 3" },
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    repeat(pages.size) { index ->
                        Box(
                            modifier = Modifier
                                .height(8.dp)
                                .width(if (index == currentPage) 28.dp else 8.dp)
                                .clip(CircleShape)
                                .background(if (index == currentPage) PrivaroTeal else Color(0xFFE5E7EB))
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = { if (currentPage < pages.lastIndex) currentPage++ else onComplete() },
                    modifier = Modifier.fillMaxWidth().height(60.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrivaroTeal),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                ) {
                    Text(pages[currentPage].buttonText, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// كلاس تخزين بيانات الصفحات
data class OnboardingPage(
    val imageRes: Int, val title: String, val highlightWord: String,
    val description: String, val buttonText: String
)

private fun hasCompletedOnboarding(context: Context): Boolean =
    context.getSharedPreferences("privaro_prefs", Context.MODE_PRIVATE).getBoolean("onboarding_complete", false)

private fun setOnboardingComplete(context: Context) =
    context.getSharedPreferences("privaro_prefs", Context.MODE_PRIVATE).edit().putBoolean("onboarding_complete", true).apply()