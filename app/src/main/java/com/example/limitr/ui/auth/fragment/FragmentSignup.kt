package com.example.limitr.ui.auth.fragment

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.net.ConnectivityManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.limitr.R
import com.example.limitr.resource.LimitrResource
import com.example.limitr.ui.auth.vm.AuthViewModel
import com.example.limitr.ui.theme.UiColor
import com.example.limitr.utils.Constants.IS_NEW_USER
import com.example.limitr.utils.ViewUtils.showToast
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthEmailException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class FragmentSignup : Fragment() {

    private val authViewModel: AuthViewModel by viewModels()
    private lateinit var googleSignInClient: GoogleSignInClient
    private var isLoading by mutableStateOf(false)

    override fun onStart() {
        super.onStart()
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            updateUI()
        } else if (!isOnline()) {
            showNoInternetDialog()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                SignupScreen(
                    isLoading = isLoading,
                    onGoogleClick = {
                        if (!isOnline()) {
                            showNoInternetDialog()
                        } else {
                            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                                .requestIdToken(getString(R.string.default_web_client_id))
                                .requestEmail()
                                .build()

                            googleSignInClient = GoogleSignIn.getClient(requireContext(), gso)
                            signInGoogle()
                        }
                    },
                )
            }
        }
    }

    @Composable
    private fun SignupScreen(
        isLoading: Boolean,
        onGoogleClick: () -> Unit,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White),
        ) {
            if (isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = UiColor,
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
                    .background(UiColor),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                ContextCompat.getDrawable(requireContext(), R.drawable.logo_color)?.let { logo ->
                    Image(
                        bitmap = logo.toBitmap().asImageBitmap(),
                        contentDescription = getString(R.string.app_name),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 32.dp),
                    )
                }
            }

            Text(
                text = getString(R.string.Continue),
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 30.dp)
                    .padding(top = 100.dp)
                    .background(UiColor)
                    .clickable { onGoogleClick() }
                    .padding(12.dp),
            )
        }
    }

    private fun observeAuthState() {
        authViewModel.authResponse.observe(viewLifecycleOwner) { response ->
            when (response) {
                is LimitrResource.Loading<*> -> {
                    isLoading = true
                }

                is LimitrResource.Success<*> -> {
                    IS_NEW_USER = response.result as Boolean
                    showToast(requireContext(), getString(R.string.welcome))
                    isLoading = false
                    updateUI()
                }

                is LimitrResource.Error -> {
                    when (response.error) {
                        is FirebaseAuthEmailException -> {
                            showToast(requireContext(), requireContext().getString(R.string.invalid_email))
                        }

                        is FirebaseNetworkException -> {
                            showToast(requireContext(), requireContext().getString(R.string.network_Error))
                        }

                        is FirebaseAuthInvalidCredentialsException -> {
                            showToast(requireContext(), requireContext().getString(R.string.invalid_credentials))
                        }

                        else -> {
                            showToast(requireContext(), response.error.message.toString())
                        }
                    }

                    isLoading = false
                }

                else -> {}
            }
        }

    }

    private fun updateUI() {
        val action = FragmentSignupDirections.actionFragmentSignupToFragmentHome()
        findNavController().navigate(action)
    }

    private fun showNoInternetDialog() {
        val dialog = Dialog(requireContext())
        dialog.setCancelable(false)
        dialog.setContentView(
            ComposeView(requireContext()).apply {
                setContent {
                    AlertDialog(
                        onDismissRequest = {},
                        title = { Text(text = getString(R.string.internet_required)) },
                        text = { Text(text = getString(R.string.turn_on_internet)) },
                        confirmButton = {
                            TextButton(onClick = { dialog.dismiss() }) {
                                Text(text = getString(R.string.ok))
                            }
                        },
                    )
                }
            },
        )
        dialog.show()
    }

    private fun signInGoogle() {
        val signInIntent = googleSignInClient.signInIntent
        launcher.launch(signInIntent)
    }


    private val launcher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                isLoading = true
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                handleResults(task)
            }
        }

    private fun isOnline(): Boolean {
        val connectivityManger =
            requireContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        connectivityManger.activeNetworkInfo.also {
            return it != null && it.isConnected
        }
    }

    private fun handleResults(task: Task<GoogleSignInAccount>) {
        if (task.isSuccessful) {
            val account: GoogleSignInAccount? = task.result
            Timber.d("Email = ${account.toString()}")
            if (account != null) {
                authViewModel.loginWithGoogle(account)
                observeAuthState()
            }
        } else {
            isLoading = false
            showToast(requireContext(), task.exception.toString())
        }
    }
}
