package com.example.privaro.ui.screens.auth

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext // نحتاجه للوصول للـ SharedPreferences
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.privaro.R
import com.example.privaro.ui.components.auth.AuthButton
import com.example.privaro.ui.components.auth.AuthTextField
import com.example.privaro.ui.components.auth.GoogleSignInButton
import com.example.privaro.ui.theme.PrivaroTeal

@Composable
fun SignInContent(
    uiState: AuthUiState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSignIn: () -> Unit,
    onGoogleSignIn: () -> Unit,
    onForgotPassword: () -> Unit,
    modifier: Modifier = Modifier
) {
    // الحصول على Context للوصول للتخزين المحلي
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp)
    ) {
        // Title
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.auth_welcome_back),
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = " Privaro",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = PrivaroTeal
            )
        }

        Text(
            text = stringResource(R.string.auth_enter_credentials),
            fontSize = 13.sp,
            color = androidx.compose.ui.graphics.Color.Gray,
            modifier = Modifier.padding(top = 4.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Email field
        AuthTextField(
            value = uiState.email,
            onValueChange = onEmailChange,
            label = stringResource(R.string.auth_email),
            keyboardType = KeyboardType.Email,
            imeAction = ImeAction.Next,
            isError = uiState.emailError != null,
            errorMessage = uiState.emailError
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Password field
        AuthTextField(
            value = uiState.password,
            onValueChange = onPasswordChange,
            label = stringResource(R.string.auth_password),
            isPassword = true,
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Done,
            onImeAction = {
                // تخزين البيانات عند ضغط Done من الكيبورد
                saveToLocal(context, uiState.email, uiState.password)
                onSignIn()
            },
            isError = uiState.passwordError != null,
            errorMessage = uiState.passwordError
        )

        // Forgot Password link
        Text(
            text = stringResource(R.string.auth_forgot_password),
            fontSize = 13.sp,
            color = PrivaroTeal,
            modifier = Modifier
                .padding(top = 8.dp)
                .clickable(onClick = onForgotPassword)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Sign In button
        AuthButton(
            text = stringResource(R.string.auth_sign_in),
            onClick = {
                // تخزين البيانات محلياً عند الضغط على الزر
                saveToLocal(context, uiState.email, uiState.password)
                onSignIn()
            },
            isLoading = uiState.isLoading
        )

        // Or divider
        OrDivider()

        // Google Sign In button
        GoogleSignInButton(
            text = stringResource(R.string.auth_sign_in_google),
            onClick = onGoogleSignIn,
            isLoading = uiState.isGoogleLoading
        )

        // Error message
        if (uiState.generalError != null) {
            Text(
                text = uiState.generalError,
                color = if (uiState.generalError.contains("sent", ignoreCase = true))
                    PrivaroTeal else MaterialTheme.colorScheme.error,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 16.dp)
            )
        }
    }
}

// دالة الحفظ (Simple SharedPreferences)
private fun saveToLocal(context: Context, email: String, pass: String) {
    val sharedPref = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
    with(sharedPref.edit()) {
        putString("saved_email", email)
        putString("saved_password", pass)
        apply() // حفظ في الخلفية
    }
}

@Composable
private fun OrDivider() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = androidx.compose.ui.graphics.Color.LightGray
        )
        Text(
            text = stringResource(R.string.auth_or),
            fontSize = 14.sp,
            color = androidx.compose.ui.graphics.Color.Gray,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = androidx.compose.ui.graphics.Color.LightGray
        )
    }
}