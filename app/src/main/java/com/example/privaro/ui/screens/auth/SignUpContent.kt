package com.example.privaro.ui.screens.auth

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
fun SignUpContent(
    uiState: AuthUiState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onTermsAgreedChange: (Boolean) -> Unit,
    onSignUp: () -> Unit,
    onGoogleSignUp: () -> Unit,
    modifier: Modifier = Modifier
) {
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
                text = stringResource(R.string.auth_start_with),
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
            onImeAction = onSignUp,
            isError = uiState.passwordError != null,
            errorMessage = uiState.passwordError
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Terms checkbox
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = uiState.agreedToTerms,
                onCheckedChange = onTermsAgreedChange,
                colors = CheckboxDefaults.colors(
                    checkedColor = PrivaroTeal,
                    uncheckedColor = Color.Gray
                )
            )
            Text(
                text = stringResource(R.string.auth_agree_terms),
                fontSize = 14.sp,
                color = Color.DarkGray
            )
        }

        if (uiState.termsError != null) {
            Text(
                text = uiState.termsError,
                color = MaterialTheme.colorScheme.error,
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 12.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Sign Up button
        AuthButton(
            text = stringResource(R.string.auth_sign_up),
            onClick = onSignUp,
            isLoading = uiState.isLoading
        )

        // Or divider
        OrDivider()

        // Google Sign Up button
        GoogleSignInButton(
            text = stringResource(R.string.auth_sign_up_google),
            onClick = onGoogleSignUp,
            isLoading = uiState.isGoogleLoading
        )

        // Error message
        if (uiState.generalError != null) {
            Text(
                text = uiState.generalError,
                color = MaterialTheme.colorScheme.error,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 16.dp)
            )
        }
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
            color = Color.LightGray
        )
        Text(
            text = stringResource(R.string.auth_or),
            fontSize = 14.sp,
            color = Color.Gray,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = Color.LightGray
        )
    }
}
