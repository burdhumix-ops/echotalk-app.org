package com.example.auth

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialException
import com.example.R
import com.example.data.storage.DriveManager
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * FirebaseAuthManager: Manages Firebase Authentication using Google Sign-In and Credential Manager.
 * Extracts the user's Google ID / OAuth token to configure DriveManager for private
 * Google Account Vault (appDataFolder) backup operations.
 */
class FirebaseAuthManager private constructor(private val context: Context) {

    companion object {
        const val TAG = "FirebaseAuthManager"

        @Volatile
        private var instance: FirebaseAuthManager? = null

        fun getInstance(context: Context): FirebaseAuthManager {
            return instance ?: synchronized(this) {
                instance ?: FirebaseAuthManager(context.applicationContext).also { instance = it }
            }
        }
    }

    private val firebaseAuth: FirebaseAuth? = try {
        FirebaseAuth.getInstance()
    } catch (e: Exception) {
        Log.w(TAG, "FirebaseAuth unavailable or running in local JVM test environment: ${e.message}")
        null
    }

    private val credentialManager: CredentialManager = try {
        CredentialManager.create(context)
    } catch (e: Exception) {
        Log.w(TAG, "CredentialManager creation fallback: ${e.message}")
        CredentialManager.create(context)
    }

    private val _currentUser = MutableStateFlow<FirebaseUser?>(firebaseAuth?.currentUser)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    private val _isSignedIn = MutableStateFlow(firebaseAuth?.currentUser != null)
    val isSignedIn: StateFlow<Boolean> = _isSignedIn.asStateFlow()

    private val _userEmail = MutableStateFlow<String?>(firebaseAuth?.currentUser?.email)
    val userEmail: StateFlow<String?> = _userEmail.asStateFlow()

    private val _userDisplayName = MutableStateFlow<String?>(firebaseAuth?.currentUser?.displayName)
    val userDisplayName: StateFlow<String?> = _userDisplayName.asStateFlow()

    private val _idToken = MutableStateFlow<String?>(null)
    val idToken: StateFlow<String?> = _idToken.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    // Single-tap prompt triggered upon Google Sign-In: "Restore chat history from Google Drive"
    private val _promptRestoreDialog = MutableStateFlow(false)
    val promptRestoreDialog: StateFlow<Boolean> = _promptRestoreDialog.asStateFlow()

    init {
        // Sync with existing Firebase user if present
        firebaseAuth?.addAuthStateListener { auth ->
            val user = auth.currentUser
            _currentUser.value = user
            _isSignedIn.value = user != null
            if (user != null) {
                _userEmail.value = user.email
                _userDisplayName.value = user.displayName
            }
        }
    }

    /**
     * Resolves the Web Client ID for Google Sign-In.
     */
    fun getServerClientId(): String {
        return try {
            context.getString(R.string.default_web_client_id)
        } catch (e: Exception) {
            "172751640665-client.apps.googleusercontent.com"
        }
    }

    /**
     * Sign in with Google using Jetpack Credential Manager and Firebase Auth.
     */
    suspend fun signInWithGoogle(
        activityContext: Context,
        onRestorePromptNeeded: ((Boolean) -> Unit)? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        _isLoading.value = true
        _authError.value = null

        val serverClientId = getServerClientId()

        try {
            // Build GetSignInWithGoogleOption
            val googleOption = try {
                GetSignInWithGoogleOption.Builder(serverClientId).build()
            } catch (e: Exception) {
                GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(serverClientId)
                    .setAutoSelectEnabled(false)
                    .build()
            }

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleOption)
                .build()

            val result: GetCredentialResponse = credentialManager.getCredential(
                request = request,
                context = activityContext
            )

            val credential = result.credential
            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val token = googleIdTokenCredential.idToken
                val email = googleIdTokenCredential.id
                val displayName = googleIdTokenCredential.displayName

                // 1. Pass token to DriveManager for private appDataFolder access
                DriveManager.getInstance(context).setAccessToken(token)

                // 2. Authenticate with Firebase Auth if available
                if (firebaseAuth != null) {
                    val firebaseCred = GoogleAuthProvider.getCredential(token, null)
                    try {
                        val authResult = firebaseAuth.signInWithCredential(firebaseCred)
                        // Await task or update flows
                        _currentUser.value = firebaseAuth.currentUser
                    } catch (e: Exception) {
                        Log.w(TAG, "FirebaseAuth signInWithCredential warning: ${e.message}")
                    }
                }

                _isSignedIn.value = true
                _userEmail.value = email
                _userDisplayName.value = displayName
                _idToken.value = token
                _promptRestoreDialog.value = true

                onRestorePromptNeeded?.invoke(true)
                Log.i(TAG, "Google Sign-In successful for $email, token registered with DriveManager.")
                Result.success(token)
            } else {
                val msg = "Unexpected credential type returned: ${credential.type}"
                Log.w(TAG, msg)
                _authError.value = msg
                Result.failure(IllegalStateException(msg))
            }
        } catch (e: GetCredentialException) {
            val msg = "CredentialManager authentication cancelled or failed: ${e.message}"
            Log.e(TAG, msg)
            _authError.value = msg
            Result.failure(e)
        } catch (e: Exception) {
            val msg = "Google Sign-In error: ${e.message}"
            Log.e(TAG, msg, e)
            _authError.value = msg
            Result.failure(e)
        } finally {
            _isLoading.value = false
        }
    }

    /**
     * Signs out from Firebase and clears DriveManager OAuth credentials.
     */
    fun signOut() {
        try {
            firebaseAuth?.signOut()
        } catch (e: Exception) {
            Log.w(TAG, "Firebase sign out warning: ${e.message}")
        }

        // Clear DriveManager tokens
        DriveManager.getInstance(context).setAccessToken(null)

        _currentUser.value = null
        _isSignedIn.value = false
        _userEmail.value = null
        _userDisplayName.value = null
        _idToken.value = null
        _promptRestoreDialog.value = false
        _authError.value = null
        Log.i(TAG, "Signed out, DriveManager credentials cleared.")
    }

    /**
     * Simulation method for automated tests, previews, or running when Play Services UI is unavailable.
     */
    fun simulateSignIn(
        userId: String = "google-user-1",
        email: String = "zavier.echotalk@gmail.com",
        displayName: String = "Zavier Sound",
        mockToken: String = "mock_google_id_token_authenticated_123"
    ) {
        _isSignedIn.value = true
        _userEmail.value = email
        _userDisplayName.value = displayName
        _idToken.value = mockToken
        _promptRestoreDialog.value = true
        _authError.value = null

        // Pass token to DriveManager
        DriveManager.getInstance(context).setAccessToken(mockToken)
        Log.i(TAG, "Simulated Google Sign-In complete: $email")
    }

    fun dismissRestorePrompt() {
        _promptRestoreDialog.value = false
    }
}
