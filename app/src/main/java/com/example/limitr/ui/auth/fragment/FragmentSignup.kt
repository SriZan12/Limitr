package com.example.limitr.ui.auth.fragment

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.limitr.R
import com.example.limitr.databinding.SignupLayoutBinding
import com.example.limitr.resource.AuthState
import com.example.limitr.ui.auth.vm.AuthViewModel
import com.example.limitr.utils.ViewUtils.showToast
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class FragmentSignup : Fragment(R.layout.signup_layout) {

    private lateinit var fragmentSignupBinding: SignupLayoutBinding
    private val authViewModel: AuthViewModel by viewModels()
    private lateinit var googleSignInClient: GoogleSignInClient

    override fun onStart() {
        super.onStart()
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            updateUI()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        fragmentSignupBinding =
            DataBindingUtil.inflate(inflater, R.layout.signup_layout, container, false)
        return fragmentSignupBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fragmentSignupBinding.googleLogin.setOnClickListener {
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build()

            googleSignInClient = GoogleSignIn.getClient(requireContext(), gso)
            signInGoogle()
        }
    }

    private fun observeAuthState() {
        authViewModel.authState.observe(viewLifecycleOwner) { authState ->
            when (authState) {
                is AuthState.Idle -> {
                }

                is AuthState.Success -> {
                    showToast(requireContext(), "Account Created")
                    fragmentSignupBinding.progressBar.visibility = View.GONE
                    updateUI()
                }

                is AuthState.AuthError -> {
                    authState.message?.let { showToast(requireContext(), it) }
                    fragmentSignupBinding.progressBar.visibility = View.GONE
                }

                else -> {}
            }
        }

    }

    private fun updateUI() {
        val action =
            FragmentSignupDirections.actionFragmentSignupToFragmentHome()
        findNavController().navigate(action)
    }

    private fun signInGoogle() {
        val signInIntent = googleSignInClient.signInIntent
        launcher.launch(signInIntent)
    }


    private val launcher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {

                fragmentSignupBinding.progressBar.visibility = View.VISIBLE
                fragmentSignupBinding.progressBar.progress
//
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                handleResults(task)
            }
        }

    private fun handleResults(task: Task<GoogleSignInAccount>) {
        if (task.isSuccessful) {
            val account: GoogleSignInAccount? =
                task.result // Checking if the account is created or not
            Timber.d("Email = ${account.toString()}")
            if (account != null) {
                authViewModel.loginWithGoogle(account)
                observeAuthState()
            }
        } else {
            showToast(requireContext(), task.exception.toString())
        }
    }
}