package com.khadka.limitr.repository

import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class AuthRepository @Inject constructor(
    private val firebaseAuth: FirebaseAuth, private val context: Context
) {

    suspend fun loginWithGoogle(account: GoogleSignInAccount): Boolean {
        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
        val authResult = firebaseAuth.signInWithCredential(credential).await()

        return authResult.additionalUserInfo?.isNewUser ?: false
    }
}