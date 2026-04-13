package com.example.privaro.ui.screens.auth

import android.app.Activity
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.privaro.ui.components.auth.AuthHeader
import com.example.privaro.ui.components.auth.AuthTab
import com.example.privaro.ui.components.auth.AuthTabBar
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.launch

private const val TAG = "AuthScreen"

@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit,
    webClientId: String,
    viewModel: AuthViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Navigate to main screen when authenticated
    LaunchedEffect(uiState.isAuthenticated) {
        if (uiState.isAuthenticated) {
            onAuthSuccess()
        }
    }

    // Google Sign-In setup
    val credentialManager = remember { CredentialManager.create(context) }

    val googleSignIn: () -> Unit = {
        viewModel.onGoogleSignInStarted()
        scope.launch {
            try {
                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(webClientId)
                    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                // CredentialManager requires an Activity context, not just Context
                val activity = context as Activity
                val result = credentialManager.getCredential(
                    request = request,
                    context = activity
                )

                val credential = result.credential
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val idToken = googleIdTokenCredential.idToken

                // Send the ID token to our backend instead of Firebase
                viewModel.signInWithGoogle(idToken)
            } catch (e: GetCredentialException) {
                Log.e(TAG, "Google Sign-In failed: ${e.type} - ${e.message}", e)
                // Show error to user
                viewModel.onGoogleSignInError("Google Sign-In failed: ${e.message ?: "Unknown error"}")
            } catch (e: Exception) {
                Log.e(TAG, "Google Sign-In error: ${e.message}", e)
                viewModel.onGoogleSignInError("Sign-In error: ${e.message ?: "Unknown error"}")
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // Header
        AuthHeader()

        // Content card overlapping the header
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .offset(y = 220.dp),
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
            color = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(modifier = Modifier.height(24.dp))

                // Tab bar
                AuthTabBar(
                    selectedTab = uiState.selectedTab,
                    onTabSelected = viewModel::onTabSelected
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Content based on selected tab
                when (uiState.selectedTab) {
                    AuthTab.SIGN_IN -> SignInContent(
                        uiState = uiState,
                        onEmailChange = viewModel::onEmailChange,
                        onPasswordChange = viewModel::onPasswordChange,
                        onSignIn = viewModel::signIn,
                        onGoogleSignIn = googleSignIn,
                        onForgotPassword = viewModel::sendPasswordReset
                    )
                    AuthTab.SIGN_UP -> SignUpContent(
                        uiState = uiState,
                        onEmailChange = viewModel::onEmailChange,
                        onPasswordChange = viewModel::onPasswordChange,
                        onTermsAgreedChange = viewModel::onTermsAgreedChange,
                        onSignUp = viewModel::signUp,
                        onGoogleSignUp = googleSignIn
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}
