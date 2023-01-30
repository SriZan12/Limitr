package com.example.limitr.ui.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor() : ViewModel() {

    private val firebaseAuth: FirebaseAuth by lazy { FirebaseAuth.getInstance() } //This means that the value of _authState will not be calculated until it is first accessed.
    private val _authState =
        MutableLiveData<AuthState>(AuthState.Idle)  //The value of _authState is initialized to the Idle state.
    val authState: LiveData<AuthState> = _authState
    private val auth: String = "Auth"

    fun loginWithGoogle(account: GoogleSignInAccount) {
        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
        firebaseAuth.signInWithCredential(credential).addOnCompleteListener { it ->
            if (it.isSuccessful) {
                _authState.value = AuthState.Success
            } else {
                it.exception?.let {
                    _authState.value = AuthState.AuthError(it.localizedMessage)
                }
            }
        }
    }
}