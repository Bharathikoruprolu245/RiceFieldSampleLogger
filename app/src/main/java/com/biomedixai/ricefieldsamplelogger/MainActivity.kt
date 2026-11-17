package com.biomedixai.ricefieldsamplelogger

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var credentialManager: CredentialManager

    private lateinit var signedOutContainer: LinearLayout
    private lateinit var signedInContainer: LinearLayout
    private lateinit var googleSignInButton: MaterialButton
    private lateinit var btnTestSignIn: MaterialButton
    private lateinit var openDashboardButton: MaterialButton
    private lateinit var signOutButton: MaterialButton
    private lateinit var userNameText: TextView
    private lateinit var userEmailText: TextView
    private lateinit var statusText: TextView
    private lateinit var progressBar: ProgressBar

    companion object {
        private const val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        auth = FirebaseAuth.getInstance()
        credentialManager = CredentialManager.create(this)

        signedOutContainer = findViewById(R.id.signedOutContainer)
        signedInContainer = findViewById(R.id.signedInContainer)
        googleSignInButton = findViewById(R.id.googleSignInButton)
        btnTestSignIn = findViewById(R.id.btnTestSignIn)
        openDashboardButton = findViewById(R.id.openDashboardButton)
        signOutButton = findViewById(R.id.signOutButton)
        userNameText = findViewById(R.id.userNameText)
        userEmailText = findViewById(R.id.userEmailText)
        statusText = findViewById(R.id.statusText)
        progressBar = findViewById(R.id.progressBar)

        googleSignInButton.setOnClickListener {
            performGoogleSignIn()
        }

        btnTestSignIn.setOnClickListener {
            performTestSignIn()
        }

        signOutButton.setOnClickListener {
            signOutUser()
        }

        openDashboardButton.setOnClickListener {
            openDashboard()
        }
    }

    override fun onStart() {
        super.onStart()
        updateUI(auth.currentUser)
    }

    private fun performGoogleSignIn() {
        showLoading(true)
        statusText.text = getString(R.string.status_please_sign_in)

        val webClientId = getString(R.string.default_web_client_id)
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(webClientId)
            .setAutoSelectEnabled(false)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        lifecycleScope.launch {
            try {
                val result = credentialManager.getCredential(
                    request = request,
                    context = this@MainActivity
                )
                handleSignInResult(result.credential)
            } catch (e: GetCredentialException) {
                showLoading(false)
                Log.e(TAG, "Credential Manager error: ${e.message}", e)
                statusText.text = "No credentials available. Use 'Guest / Test Sign-In'."
                Toast.makeText(this@MainActivity, "No Google accounts available on device. Use Guest / Test Sign-In.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun performTestSignIn() {
        showLoading(true)
        statusText.text = "Signing in as Guest Researcher..."

        auth.signInAnonymously()
            .addOnCompleteListener(this) { task ->
                showLoading(false)
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    updateUI(user)
                    Toast.makeText(this, "Signed in as Guest Researcher", Toast.LENGTH_SHORT).show()
                    openDashboard()
                } else {
                    statusText.text = "Guest sign-in failed: ${task.exception?.message}"
                    Toast.makeText(this, "Test sign-in failed.", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun handleSignInResult(credential: androidx.credentials.Credential) {
        if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            try {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val idToken = googleIdTokenCredential.idToken
                firebaseAuthWithGoogle(idToken)
            } catch (e: GoogleIdTokenParsingException) {
                showLoading(false)
                Log.e(TAG, "Received invalid Google ID Token", e)
                statusText.text = "Error parsing Google ID Token"
            }
        } else {
            showLoading(false)
            Log.e(TAG, "Unexpected type of credential received")
            statusText.text = "Unexpected credential type received"
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val authCredential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(authCredential)
            .addOnCompleteListener(this) { task ->
                showLoading(false)
                if (task.isSuccessful) {
                    Log.d(TAG, "firebaseAuthWithGoogle: success")
                    val user = auth.currentUser
                    updateUI(user)
                    Toast.makeText(this, "Signed in as ${user?.displayName ?: user?.email}", Toast.LENGTH_SHORT).show()
                } else {
                    Log.w(TAG, "firebaseAuthWithGoogle: failure", task.exception)
                    statusText.text = "Authentication failed: ${task.exception?.message}"
                    Toast.makeText(this, "Authentication failed.", Toast.LENGTH_SHORT).show()
                    updateUI(null)
                }
            }
    }

    private fun signOutUser() {
        auth.signOut()
        updateUI(null)
        statusText.text = getString(R.string.status_please_sign_in)
        Toast.makeText(this, "Signed out successfully", Toast.LENGTH_SHORT).show()
    }

    private fun updateUI(user: FirebaseUser?) {
        if (user != null) {
            signedOutContainer.visibility = View.GONE
            signedInContainer.visibility = View.VISIBLE
            val displayName = if (user.isAnonymous) "Guest Researcher" else (user.displayName ?: "Researcher")
            val displayEmail = if (user.isAnonymous) "guest@ricefield.ai" else (user.email ?: "N/A")
            userNameText.text = getString(R.string.welcome_user, displayName)
            userEmailText.text = getString(R.string.user_email, displayEmail)
            statusText.text = "Authenticated with Firebase Auth"
        } else {
            signedOutContainer.visibility = View.VISIBLE
            signedInContainer.visibility = View.GONE
            userNameText.text = ""
            userEmailText.text = ""
        }
    }

    private fun showLoading(isLoading: Boolean) {
        if (isLoading) {
            progressBar.visibility = View.VISIBLE
            googleSignInButton.isEnabled = false
            btnTestSignIn.isEnabled = false
        } else {
            progressBar.visibility = View.GONE
            googleSignInButton.isEnabled = true
            btnTestSignIn.isEnabled = true
        }
    }

    private fun openDashboard() {
        val intent = Intent(this, DashboardActivity::class.java)
        startActivity(intent)
    }
}
