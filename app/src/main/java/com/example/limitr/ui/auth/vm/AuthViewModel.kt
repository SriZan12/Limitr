package com.example.limitr.ui.auth.vm

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.limitr.repository.AuthRepository
import com.example.limitr.resource.AuthState
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.GoogleAuthProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _authState =
        MutableLiveData<AuthState>(AuthState.Idle)  //The value of _authState is initialized to the Idle state.
    val authState: LiveData<AuthState> = _authState

    fun loginWithGoogle(account: GoogleSignInAccount) {
        viewModelScope.launch {
            try {
                authRepository.loginWithGoogle(account)
                _authState.value = AuthState.Success

            } catch (exception: FirebaseAuthException) {
                _authState.value = AuthState.AuthError(exception.localizedMessage)
            }

        }
    }
}