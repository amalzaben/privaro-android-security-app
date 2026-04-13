package com.example.privaro.ui.screens.auth

import android.app.Application
import android.util.Patterns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.privaro.data.auth.AuthRepository
import com.example.privaro.data.auth.AuthUser
import com.example.privaro.data.log.LogRepository
import com.example.privaro.ui.components.auth.AuthTab
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthUiState(
    val selectedTab: AuthTab = AuthTab.SIGN_UP,
    val email: String = "",
    val password: String = "",
    val agreedToTerms: Boolean = false,
    val isLoading: Boolean = false,
    val isGoogleLoading: Boolean = false,
    val emailError: String? = null,
    val passwordError: String? = null,
    val termsError: String? = null,
    val generalError: String? = null,
    val isAuthenticated: Boolean = false,
    val currentUser: AuthUser? = null
)

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val authRepository: AuthRepository = AuthRepository.getInstance(application)
    private val logRepository: LogRepository = LogRepository.getInstance(application)

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        // Check initial auth state
        _uiState.update { it.copy(
            isAuthenticated = authRepository.isAuthenticated,
            currentUser = authRepository.currentUser
        )}

        // Listen to auth state changes
        viewModelScope.launch {
            authRepository.authStateFlow().collect { user ->
                _uiState.update { it.copy(
                    isAuthenticated = user != null,
                    currentUser = user
                )}
            }
        }
    }

    fun onTabSelected(tab: AuthTab) {
        _uiState.update { it.copy(
            selectedTab = tab,
            emailError = null,
            passwordError = null,
            termsError = null,
            generalError = null
        )}
    }

    fun onEmailChange(email: String) {
        _uiState.update { it.copy(
            email = email,
            emailError = null,
            generalError = null
        )}
    }

    fun onPasswordChange(password: String) {
        _uiState.update { it.copy(
            password = password,
            passwordError = null,
            generalError = null
        )}
    }

    fun onTermsAgreedChange(agreed: Boolean) {
        _uiState.update { it.copy(
            agreedToTerms = agreed,
            termsError = null
        )}
    }

    fun signIn() {
        val state = _uiState.value
        if (!validateSignInFields(state)) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, generalError = null) }

            val result = authRepository.signInWithEmail(state.email, state.password)
            result.fold(
                onSuccess = {
                    logRepository.logSignIn()
                    _uiState.update { it.copy(isLoading = false) }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(
                        isLoading = false,
                        generalError = e.message ?: "Sign in failed"
                    )}
                }
            )
        }
    }

    fun signUp() {
        val state = _uiState.value
        if (!validateSignUpFields(state)) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, generalError = null) }

            val result = authRepository.signUpWithEmail(state.email, state.password)
            result.fold(
                onSuccess = {
                    logRepository.logSignUp()
                    _uiState.update { it.copy(isLoading = false) }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(
                        isLoading = false,
                        generalError = e.message ?: "Sign up failed"
                    )}
                }
            )
        }
    }

    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isGoogleLoading = true, generalError = null) }

            val result = authRepository.signInWithGoogle(idToken)
            result.fold(
                onSuccess = {
                    logRepository.logSignIn()
                    _uiState.update { it.copy(isGoogleLoading = false) }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(
                        isGoogleLoading = false,
                        generalError = e.message ?: "Google sign in failed"
                    )}
                }
            )
        }
    }

    fun onGoogleSignInStarted() {
        _uiState.update { it.copy(isGoogleLoading = true, generalError = null) }
    }

    fun onGoogleSignInError(errorMessage: String) {
        _uiState.update { it.copy(
            isGoogleLoading = false,
            generalError = errorMessage
        )}
    }

    fun sendPasswordReset() {
        val email = _uiState.value.email
        if (email.isBlank()) {
            _uiState.update { it.copy(emailError = "Enter your email to reset password") }
            return
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _uiState.update { it.copy(emailError = "Please enter a valid email") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = authRepository.sendPasswordResetEmail(email)
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(
                        isLoading = false,
                        generalError = "Password reset email sent"
                    )}
                },
                onFailure = { e ->
                    _uiState.update { it.copy(
                        isLoading = false,
                        generalError = e.message ?: "Failed to send reset email"
                    )}
                }
            )
        }
    }

    fun signOut() {
        authRepository.signOut()
    }

    fun clearError() {
        _uiState.update { it.copy(generalError = null) }
    }

    private fun validateSignInFields(state: AuthUiState): Boolean {
        var isValid = true

        if (state.email.isBlank()) {
            _uiState.update { it.copy(emailError = "Email is required") }
            isValid = false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(state.email).matches()) {
            _uiState.update { it.copy(emailError = "Please enter a valid email") }
            isValid = false
        }

        if (state.password.isBlank()) {
            _uiState.update { it.copy(passwordError = "Password is required") }
            isValid = false
        }

        return isValid
    }

    private fun validateSignUpFields(state: AuthUiState): Boolean {
        var isValid = validateSignInFields(state)

        if (state.password.length < 6) {
            _uiState.update { it.copy(passwordError = "Password must be at least 6 characters") }
            isValid = false
        }

        if (!state.agreedToTerms) {
            _uiState.update { it.copy(termsError = "You must agree to the terms & policy") }
            isValid = false
        }

        return isValid
    }
}
