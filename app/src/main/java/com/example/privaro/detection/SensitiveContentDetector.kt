package com.example.privaro.detection

enum class SensitiveContentType {
    PASSWORD,
    OTP,
    CARD_NUMBER,
    CVV,
    BANK_ACCOUNT,
    PIN,
    NONE
}

enum class SensitivityLevel {
    HIGH,    // All sensitive data types
    MEDIUM,  // Passwords and OTPs only
    LOW      // Passwords only
}

data class DetectionResult(
    val isSensitive: Boolean,
    val type: SensitiveContentType,
    val description: String,
    val matchedText: String = ""
)

class SensitiveContentDetector(
    private var sensitivityLevel: SensitivityLevel = SensitivityLevel.HIGH
) {
    companion object {
        // OTP patterns - 4 to 8 digit codes
        private val OTP_PATTERNS = listOf(
            Regex("""\b(?:OTP|otp|code|Code|CODE|verification|Verification)\s*[:\-]?\s*(\d{4,8})\b"""),
            Regex("""\b(\d{4,6})\s*(?:is your|is the)?\s*(?:OTP|otp|code|verification)\b"""),
            Regex("""\b(?:one[- ]?time|One[- ]?Time)\s*(?:password|code|pin)\s*[:\-]?\s*(\d{4,8})\b""", RegexOption.IGNORE_CASE),
            Regex("""\b(?:verify|confirm)\s*(?:with|using)?\s*[:\-]?\s*(\d{4,6})\b""", RegexOption.IGNORE_CASE)
        )

        // Card number patterns (13-19 digits, may have spaces or dashes)
        private val CARD_NUMBER_PATTERNS = listOf(
            Regex("""\b(\d{4}[\s\-]?\d{4}[\s\-]?\d{4}[\s\-]?\d{4})\b"""),
            Regex("""\b(\d{4}[\s\-]?\d{6}[\s\-]?\d{5})\b"""),  // Amex format
            Regex("""\b(\d{13,19})\b""")
        )

        // CVV patterns (3-4 digits)
        private val CVV_PATTERNS = listOf(
            Regex("""\b(?:CVV|cvv|CVC|cvc|CVV2|cvv2|security code|Security Code)\s*[:\-]?\s*(\d{3,4})\b"""),
            Regex("""\b(\d{3,4})\s*(?:CVV|cvv|CVC|cvc)\b""")
        )

        // PIN patterns
        private val PIN_PATTERNS = listOf(
            Regex("""\b(?:PIN|pin|Pin)\s*[:\-]?\s*(\d{4,6})\b"""),
            Regex("""\b(\d{4,6})\s*(?:is your|is the)?\s*(?:PIN|pin)\b""")
        )

        // Bank account patterns
        private val BANK_ACCOUNT_PATTERNS = listOf(
            Regex("""\b(?:account|Account|ACCOUNT)\s*(?:number|no|#)?\s*[:\-]?\s*(\d{8,17})\b"""),
            Regex("""\b(?:routing|Routing)\s*(?:number|no|#)?\s*[:\-]?\s*(\d{9})\b"""),
            Regex("""\b(?:IBAN|iban)\s*[:\-]?\s*([A-Z]{2}\d{2}[A-Z0-9]{11,30})\b""")
        )

        // Keywords that suggest sensitive context
        private val SENSITIVE_KEYWORDS = listOf(
            "password", "passwd", "secret", "credential",
            "otp", "one-time", "verification code", "verify",
            "card number", "credit card", "debit card", "cvv", "cvc",
            "pin", "bank account", "account number", "routing",
            "social security", "ssn", "tax id"
        )
    }

    fun setSensitivityLevel(level: SensitivityLevel) {
        sensitivityLevel = level
    }

    fun detect(text: String): DetectionResult {
        if (text.isBlank()) {
            return DetectionResult(false, SensitiveContentType.NONE, "")
        }

        val lowerText = text.lowercase()

        // Check for password-related keywords (all sensitivity levels)
        if (containsPasswordKeywords(lowerText)) {
            return DetectionResult(
                isSensitive = true,
                type = SensitiveContentType.PASSWORD,
                description = "Password or credential detected"
            )
        }

        if (sensitivityLevel == SensitivityLevel.LOW) {
            return DetectionResult(false, SensitiveContentType.NONE, "")
        }

        // Check for OTP (medium and high sensitivity)
        detectOTP(text)?.let { return it }

        // Check for PIN
        detectPIN(text)?.let { return it }

        if (sensitivityLevel == SensitivityLevel.MEDIUM) {
            return DetectionResult(false, SensitiveContentType.NONE, "")
        }

        // Check for card numbers (high sensitivity only)
        detectCardNumber(text)?.let { return it }

        // Check for CVV
        detectCVV(text)?.let { return it }

        // Check for bank account numbers
        detectBankAccount(text)?.let { return it }

        return DetectionResult(false, SensitiveContentType.NONE, "")
    }

    private fun containsPasswordKeywords(text: String): Boolean {
        val passwordKeywords = listOf(
            "password", "passwd", "passcode", "secret key",
            "credential", "login", "sign in", "authenticate"
        )
        return passwordKeywords.any { keyword ->
            text.contains(keyword) && (
                text.contains(":") ||
                text.contains("is") ||
                text.contains("enter") ||
                text.contains("type") ||
                text.contains("your")
            )
        }
    }

    private fun detectOTP(text: String): DetectionResult? {
        for (pattern in OTP_PATTERNS) {
            pattern.find(text)?.let { match ->
                return DetectionResult(
                    isSensitive = true,
                    type = SensitiveContentType.OTP,
                    description = "One-time password detected",
                    matchedText = match.groupValues.getOrElse(1) { match.value }
                )
            }
        }
        return null
    }

    private fun detectCardNumber(text: String): DetectionResult? {
        for (pattern in CARD_NUMBER_PATTERNS) {
            pattern.find(text)?.let { match ->
                val digits = match.value.replace(Regex("""[\s\-]"""), "")
                if (isValidCardNumber(digits)) {
                    return DetectionResult(
                        isSensitive = true,
                        type = SensitiveContentType.CARD_NUMBER,
                        description = "Card number detected",
                        matchedText = maskCardNumber(match.value)
                    )
                }
            }
        }
        return null
    }

    private fun detectCVV(text: String): DetectionResult? {
        for (pattern in CVV_PATTERNS) {
            pattern.find(text)?.let { match ->
                return DetectionResult(
                    isSensitive = true,
                    type = SensitiveContentType.CVV,
                    description = "CVV/Security code detected",
                    matchedText = "***"
                )
            }
        }
        return null
    }

    private fun detectPIN(text: String): DetectionResult? {
        for (pattern in PIN_PATTERNS) {
            pattern.find(text)?.let { match ->
                return DetectionResult(
                    isSensitive = true,
                    type = SensitiveContentType.PIN,
                    description = "PIN detected",
                    matchedText = "****"
                )
            }
        }
        return null
    }

    private fun detectBankAccount(text: String): DetectionResult? {
        for (pattern in BANK_ACCOUNT_PATTERNS) {
            pattern.find(text)?.let { match ->
                return DetectionResult(
                    isSensitive = true,
                    type = SensitiveContentType.BANK_ACCOUNT,
                    description = "Bank account number detected",
                    matchedText = maskAccountNumber(match.groupValues.getOrElse(1) { match.value })
                )
            }
        }
        return null
    }

    private fun isValidCardNumber(digits: String): Boolean {
        if (digits.length !in 13..19) return false
        if (!digits.all { it.isDigit() }) return false

        // Luhn algorithm validation
        var sum = 0
        var alternate = false

        for (i in digits.length - 1 downTo 0) {
            var digit = digits[i].digitToInt()

            if (alternate) {
                digit *= 2
                if (digit > 9) {
                    digit -= 9
                }
            }

            sum += digit
            alternate = !alternate
        }

        return sum % 10 == 0
    }

    private fun maskCardNumber(number: String): String {
        val digits = number.replace(Regex("""[\s\-]"""), "")
        if (digits.length < 8) return "****"
        return "**** **** **** ${digits.takeLast(4)}"
    }

    private fun maskAccountNumber(number: String): String {
        if (number.length < 4) return "****"
        return "****${number.takeLast(4)}"
    }
}
