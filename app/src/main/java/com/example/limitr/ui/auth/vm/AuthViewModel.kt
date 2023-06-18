package com.example.limitr.ui.auth.vm

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.limitr.resource.LimitrResource
import com.example.limitr.repository.AuthRepository
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.firebase.auth.FirebaseAuthException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _authResponse =
        MutableLiveData<LimitrResource>(LimitrResource.Loading("Loading"))  //The value of _authState is initialized to the Loading state.
    val authResponse: LiveData<LimitrResource> = _authResponse

    fun loginWithGoogle(account: GoogleSignInAccount) {
        viewModelScope.launch(Dispatchers.Main) {
            try {
                authRepository.loginWithGoogle(account)
                _authResponse.value = LimitrResource.Loading("Loading")
                _authResponse.value = LimitrResource.Success("Account Created")

            } catch (exception: FirebaseAuthException) {
                _authResponse.value = LimitrResource.Error(exception)
            }

        }
    }
}