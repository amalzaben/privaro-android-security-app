package com.example.privaro.ui.overlay

import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.privaro.camera.SurroundingScanner
import com.example.privaro.detection.SensitiveContentType

private const val TAG = "PRIVARO_OVERLAY"

class OverlayManager(private val context: Context) {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val mainHandler = Handler(Looper.getMainLooper())

    private var currentOverlay: View? = null
    private var surroundingScanner: SurroundingScanner? = null

    private val overlayState = mutableStateOf<OverlayState>(OverlayState.Hidden)

    sealed class OverlayState {
        object Hidden : OverlayState()
        data class SecurityPrompt(
            val contentType: SensitiveContentType,
            val description: String
        ) : OverlayState()
        object Scanning : OverlayState()
        data class ScanResult(
            val peopleDetected: Int,
            val frontCount: Int,
            val backCount: Int
        ) : OverlayState()
    }

    fun canDrawOverlays(): Boolean {
        return Settings.canDrawOverlays(context)
    }

    fun requestOverlayPermission(): Intent {
        return Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            android.net.Uri.parse("package:${context.packageName}")
        )
    }

    fun showSecurityPrompt(
        contentType: SensitiveContentType,
        description: String,
        onContinue: () -> Unit,
        onCheckSurroundings: () -> Unit,
        onCancel: () -> Unit
    ) {
        Log.e(TAG, "★★★ showSecurityPrompt called ★★★")
        Log.e(TAG, "canDrawOverlays = ${canDrawOverlays()}")

        if (!canDrawOverlays()) {
            Log.e(TAG, "❌ Cannot draw overlays - permission not granted!")
            return
        }

        mainHandler.post {
            Log.e(TAG, "Inside mainHandler.post")
            dismissAll()
            vibrateWarning()

            showOverlay {
                SecurityPromptOverlay(
                    contentType = contentType,
                    description = description,
                    onContinue = {
                        dismissAll()
                        onContinue()
                    },
                    onCheckSurroundings = {
                        dismissAll()
                        onCheckSurroundings()
                    },
                    onCancel = {
                        dismissAll()
                        onCancel()
                    }
                )
            }
        }
    }

    fun showScanningOverlay(
        onScanComplete: (peopleDetected: Int, frontCount: Int, backCount: Int) -> Unit
    ) {
        if (!canDrawOverlays()) return

        mainHandler.post {
            dismissAll()

            showOverlay {
                ScanningOverlay()
            }

            // Initialize and start the surrounding scanner
            surroundingScanner = SurroundingScanner(context)
            surroundingScanner?.startScan { result ->
                mainHandler.post {
                    dismissAll()
                    onScanComplete(result.totalPeopleCount, result.frontPeopleCount, result.backPeopleCount)
                }
            }
        }
    }

    fun showScanResult(
        peopleDetected: Int,
        frontCount: Int,
        backCount: Int,
        onContinue: () -> Unit,
        onCancel: () -> Unit,
        onScanAgain: (() -> Unit)? = null
    ) {
        if (!canDrawOverlays()) return

        mainHandler.post {
            dismissAll()

            if (peopleDetected > 0) {
                vibrateAlert()
            } else {
                vibrateSuccess()
            }

            showOverlay {
                ScanResultOverlay(
                    peopleDetected = peopleDetected,
                    frontCount = frontCount,
                    backCount = backCount,
                    onContinue = {
                        dismissAll()
                        onContinue()
                    },
                    onCancel = {
                        dismissAll()
                        onCancel()
                    },
                    onScanAgain = if (onScanAgain != null) {
                        {
                            dismissAll()
                            onScanAgain()
                        }
                    } else null
                )
            }
        }
    }

    /**
     * Starts a manual surroundings scan from the home screen.
     * Shows scanning overlay, then results, and logs the scan.
     */
    fun startManualScan(
        onComplete: (peopleDetected: Int) -> Unit
    ) {
        if (!canDrawOverlays()) {
            Log.e(TAG, "Cannot start manual scan - overlay permission not granted")
            return
        }

        showScanningOverlay { peopleDetected, frontCount, backCount ->
            showScanResult(
                peopleDetected = peopleDetected,
                frontCount = frontCount,
                backCount = backCount,
                onContinue = {
                    onComplete(peopleDetected)
                },
                onCancel = {
                    onComplete(peopleDetected)
                },
                onScanAgain = {
                    // Recursively call startManualScan to re-scan
                    startManualScan(onComplete)
                }
            )
        }
    }

    private fun showOverlay(content: @Composable () -> Unit) {
        Log.e(TAG, "showOverlay() called")

        try {
            val lifecycleOwner = OverlayLifecycleOwner()
            Log.e(TAG, "LifecycleOwner created, current state: ${lifecycleOwner.lifecycle.currentState}")

            val composeView = ComposeView(context)

            // Set composition strategy FIRST
            composeView.setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
            )
            Log.e(TAG, "Composition strategy set")

            // Set view tree owners
            composeView.setViewTreeLifecycleOwner(lifecycleOwner)
            composeView.setViewTreeSavedStateRegistryOwner(lifecycleOwner)
            Log.e(TAG, "ViewTree owners set")

            val windowType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }
            Log.e(TAG, "Window type: $windowType (SDK: ${Build.VERSION.SDK_INT})")

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                windowType,
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.CENTER
            }
            Log.e(TAG, "WindowManager params created, about to addView")

            // ADD VIEW TO WINDOW FIRST
            windowManager.addView(composeView, params)
            currentOverlay = composeView
            Log.e(TAG, "View added to window")

            // NOW trigger lifecycle events (after view is attached)
            lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
            lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_START)
            lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
            Log.e(TAG, "Lifecycle RESUMED, state: ${lifecycleOwner.lifecycle.currentState}")

            // Set content AFTER view is attached and lifecycle is ready
            composeView.setContent {
                Log.e(TAG, "★★★ COMPOSE CONTENT BEING RENDERED ★★★")
                MaterialTheme(
                    colorScheme = darkColorScheme()
                ) {
                    content()
                }
            }
            Log.e(TAG, "★★★ setContent called - OVERLAY SHOULD BE VISIBLE ★★★")

            // Log view state
            composeView.post {
                Log.e(TAG, "Post: width=${composeView.width}, height=${composeView.height}, attached=${composeView.isAttachedToWindow}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌❌❌ FAILED TO SHOW OVERLAY: ${e.message}", e)
        }
    }

    fun dismissAll() {
        Log.e(TAG, "★ dismissAll() called, currentOverlay = $currentOverlay ★")
        dismissAllInternal()
    }

    // Internal dismiss that runs synchronously (for use when already on main thread)
    private fun dismissAllInternal() {
        currentOverlay?.let {
            try {
                windowManager.removeView(it)
                Log.e(TAG, "dismissAll - view removed")
            } catch (e: Exception) {
                Log.e(TAG, "dismissAll - error removing view: ${e.message}")
            }
        }
        currentOverlay = null

        surroundingScanner?.stopScan()
        surroundingScanner = null
    }

    private fun vibrateWarning() {
        vibrate(longArrayOf(0, 100, 100, 100))
    }

    private fun vibrateAlert() {
        vibrate(longArrayOf(0, 200, 100, 200, 100, 200))
    }

    private fun vibrateSuccess() {
        vibrate(longArrayOf(0, 50, 50, 50))
    }

    private fun vibrate(pattern: LongArray) {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(pattern, -1)
        }
    }
}

private class OverlayLifecycleOwner : LifecycleOwner, SavedStateRegistryOwner {
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry

    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    init {
        savedStateRegistryController.performAttach()
        savedStateRegistryController.performRestore(null)
    }

    fun handleLifecycleEvent(event: Lifecycle.Event) {
        lifecycleRegistry.handleLifecycleEvent(event)
    }
}
