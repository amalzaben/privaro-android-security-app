package com.example.privaro.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.example.privaro.data.log.LogRepository
import com.example.privaro.data.model.UsageLog
import com.example.privaro.detection.SensitiveContentDetector
import com.example.privaro.detection.SensitiveContentType
import com.example.privaro.ui.overlay.OverlayManager

class PrivaroAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "PRIVARO_DEBUG"

        @Volatile
        var instance: PrivaroAccessibilityService? = null
            private set

        fun isServiceRunning(): Boolean = instance != null
    }

    private lateinit var sensitiveContentDetector: SensitiveContentDetector
    private lateinit var overlayManager: OverlayManager
    private lateinit var logRepository: LogRepository

    private var isProcessingEvent = false
    private var lastProcessedEventTime = 0L
    private val eventDebounceMs = 100L

    // Cooldown after user clicks "Continue" - don't prompt again for this duration
    private var continueClickedTime = 0L
    private var continueClickedPackage: String? = null
    private val continueCooldownMs = 60_000L  // 1 minute cooldown

    init {
        Log.e(TAG, "★★★ PrivaroAccessibilityService INIT BLOCK ★★★")
    }

    override fun onCreate() {
        super.onCreate()
        Log.e(TAG, "★★★ onCreate() CALLED ★★★")
        instance = this
        sensitiveContentDetector = SensitiveContentDetector()
        overlayManager = OverlayManager(this)
        logRepository = LogRepository.getInstance(this)
        Log.e(TAG, "★★★ Privaro Accessibility Service CREATED ★★★")
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.e(TAG, "★★★ onServiceConnected() CALLED ★★★")

        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED or
                    AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED or
                    AccessibilityEvent.TYPE_ANNOUNCEMENT or
                    AccessibilityEvent.TYPE_VIEW_FOCUSED or
                    AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED

            feedbackType = AccessibilityServiceInfo.FEEDBACK_SPOKEN or
                    AccessibilityServiceInfo.FEEDBACK_HAPTIC

            flags = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS or
                    AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                    AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS

            notificationTimeout = 100
        }

        serviceInfo = info
        Log.e(TAG, "★★★ Privaro Accessibility Service CONNECTED - Ready to receive events! ★★★")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || isProcessingEvent) return

        // Ignore events from our own app
        if (event.packageName == packageName) return

        val currentTime = System.currentTimeMillis()

        // Check if we're in cooldown period for this package (user clicked "Continue")
        if (event.packageName == continueClickedPackage &&
            currentTime - continueClickedTime < continueCooldownMs) {
            return  // Skip - user already chose to continue
        }

        if (currentTime - lastProcessedEventTime < eventDebounceMs) return

        // Track current package for cooldown purposes
        currentEventPackage = event.packageName?.toString()

        when (event.eventType) {
            AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED,
            AccessibilityEvent.TYPE_VIEW_FOCUSED -> {
                Log.e(TAG, "→ Handling FOCUS event")
                handleFocusEvent(event)
            }
            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED -> {
                Log.e(TAG, "→ Handling TEXT_CHANGED event")
                handleTextChangedEvent(event)
            }
            AccessibilityEvent.TYPE_ANNOUNCEMENT -> {
                Log.e(TAG, "→ Handling ANNOUNCEMENT event")
                handleAnnouncementEvent(event)
            }
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                // Don't log this one - too noisy
                handleWindowContentChanged(event)
            }
        }
    }

    private fun handleFocusEvent(event: AccessibilityEvent) {
        val source = event.source
        if (source == null) {
            Log.e(TAG, "Focus event source is NULL")
            return
        }

        try {
            val className = source.className?.toString() ?: ""
            val isEditText = className.contains("EditText") || className.contains("edit", ignoreCase = true)
            val hintText = source.hintText?.toString()?.lowercase() ?: ""
            val contentDesc = source.contentDescription?.toString()?.lowercase() ?: ""
            val viewId = source.viewIdResourceName ?: ""

            Log.e(TAG, "★ FOCUS: class=$className, isPassword=${source.isPassword}, hint=$hintText, desc=$contentDesc, viewId=$viewId")

            // Check native password field
            if (source.isPassword) {
                Log.e(TAG, "★★★ NATIVE PASSWORD FIELD DETECTED ★★★")
                triggerSecurityPrompt(SensitiveContentType.PASSWORD, "Password field")
                return
            }

            // Check for WebView/web-based password fields by hint or description
            val passwordKeywords = listOf("password", "passwort", "contraseña", "mot de passe", "senha", "пароль")
            val isPasswordByHint = passwordKeywords.any { keyword ->
                hintText.contains(keyword) || contentDesc.contains(keyword) || viewId.contains(keyword, ignoreCase = true)
            }

            if (isEditText && isPasswordByHint) {
                Log.e(TAG, "★★★ WEBVIEW PASSWORD FIELD DETECTED (by hint/desc) ★★★")
                triggerSecurityPrompt(SensitiveContentType.PASSWORD, "Password field")
                return
            }

            val text = getNodeText(source)
            checkForSensitiveContent(text, source)
        } finally {
            source.recycle()
        }
    }

    private fun handleTextChangedEvent(event: AccessibilityEvent) {
        val source = event.source ?: return

        try {
            if (source.isPassword) {
                return
            }

            val text = event.text?.joinToString(" ") ?: return
            checkForSensitiveContent(text, source)
        } finally {
            source.recycle()
        }
    }

    private fun handleAnnouncementEvent(event: AccessibilityEvent) {
        val text = event.text?.joinToString(" ") ?: return
        checkForSensitiveContent(text, null)
    }

    private fun handleWindowContentChanged(event: AccessibilityEvent) {
        val source = event.source ?: return

        try {
            if (source.isPassword) {
                Log.d(TAG, "Password field content changed")
                triggerSecurityPrompt(SensitiveContentType.PASSWORD, "Password")
                return
            }

            val text = getNodeText(source)
            checkForSensitiveContent(text, source)
        } finally {
            source.recycle()
        }
    }

    private fun getNodeText(node: AccessibilityNodeInfo): String {
        val textParts = mutableListOf<String>()

        node.text?.let { textParts.add(it.toString()) }
        node.contentDescription?.let { textParts.add(it.toString()) }
        node.hintText?.let { textParts.add(it.toString()) }

        return textParts.joinToString(" ")
    }

    private fun checkForSensitiveContent(text: String, node: AccessibilityNodeInfo?) {
        if (text.isBlank()) return

        val detectionResult = sensitiveContentDetector.detect(text)

        if (detectionResult.isSensitive) {
            Log.d(TAG, "Sensitive content detected: ${detectionResult.type}")
            triggerSecurityPrompt(detectionResult.type, detectionResult.description)
        }
    }

    private var currentEventPackage: String? = null

    private fun triggerSecurityPrompt(type: SensitiveContentType, description: String) {
        if (isProcessingEvent) return

        isProcessingEvent = true
        lastProcessedEventTime = System.currentTimeMillis()

        Log.d(TAG, "Triggering security prompt for: $type - $description")

        val packageAtPrompt = currentEventPackage

        overlayManager.showSecurityPrompt(
            contentType = type,
            description = description,
            onContinue = {
                Log.d(TAG, "User chose to continue - setting cooldown for $packageAtPrompt")
                // Log the action
                logRepository.logSensitiveContentDetected(
                    contentType = type.name,
                    action = UsageLog.ACTION_CONTINUED,
                    packageName = packageAtPrompt
                )
                // Set cooldown so we don't prompt again for this app
                continueClickedTime = System.currentTimeMillis()
                continueClickedPackage = packageAtPrompt
                isProcessingEvent = false
            },
            onCheckSurroundings = {
                Log.d(TAG, "User chose to check surroundings")
                overlayManager.showScanningOverlay(
                    onScanComplete = { peopleDetected, frontCount, backCount ->
                        // Log the scan result
                        logRepository.logScanPerformed(
                            peopleDetected = peopleDetected,
                            action = if (peopleDetected == 0) "safe" else "people_detected"
                        )

                        overlayManager.showScanResult(
                            peopleDetected = peopleDetected,
                            frontCount = frontCount,
                            backCount = backCount,
                            onContinue = {
                                Log.d(TAG, "User chose to continue after scan - setting cooldown")
                                continueClickedTime = System.currentTimeMillis()
                                continueClickedPackage = packageAtPrompt
                                isProcessingEvent = false
                            },
                            onCancel = {
                                isProcessingEvent = false
                            },
                            onScanAgain = {
                                // Re-scan surroundings
                                overlayManager.showScanningOverlay { newPeopleDetected, newFrontCount, newBackCount ->
                                    // Log the re-scan result
                                    logRepository.logScanPerformed(
                                        peopleDetected = newPeopleDetected,
                                        action = if (newPeopleDetected == 0) "safe" else "people_detected"
                                    )
                                    overlayManager.showScanResult(
                                        peopleDetected = newPeopleDetected,
                                        frontCount = newFrontCount,
                                        backCount = newBackCount,
                                        onContinue = {
                                            continueClickedTime = System.currentTimeMillis()
                                            continueClickedPackage = packageAtPrompt
                                            isProcessingEvent = false
                                        },
                                        onCancel = {
                                            isProcessingEvent = false
                                        }
                                    )
                                }
                            }
                        )
                    }
                )
            },
            onCancel = {
                Log.d(TAG, "User cancelled")
                // Log the cancellation
                logRepository.logSensitiveContentDetected(
                    contentType = type.name,
                    action = UsageLog.ACTION_CANCELLED,
                    packageName = packageAtPrompt
                )
                isProcessingEvent = false
                performGlobalAction(GLOBAL_ACTION_BACK)
            }
        )
    }

    override fun onInterrupt() {
        Log.d(TAG, "Privaro Accessibility Service interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        overlayManager.dismissAll()
        Log.d(TAG, "Privaro Accessibility Service destroyed")
    }
}
