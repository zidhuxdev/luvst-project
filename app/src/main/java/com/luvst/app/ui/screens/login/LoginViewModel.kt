package com.luvst.app.ui.screens.login

import android.content.Context
import androidx.lifecycle.ViewModel
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.luvst.app.data.local.UserPreferences
import com.luvst.app.data.remote.ApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userPreferences: UserPreferences,
    private val apiService: ApiService
) : ViewModel() {
    
    suspend fun signInWithGoogle(account: GoogleSignInAccount?): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                if (account == null) return@withContext false
                
                val idToken = account.idToken
                val email = account.email
                val name = account.displayName
                val photoUrl = account.photoUrl?.toString()
                
                // Send to backend
                val response = apiService.googleSignIn(
                    GoogleSignInRequest(
                        idToken = idToken ?: "",
                        email = email ?: "",
                        name = name ?: "",
                        photoUrl = photoUrl
                    )
                )
                
                if (response.isSuccessful) {
                    val body = response.body()
                    body?.let {
                        // Save user data locally
                        userPreferences.saveUser(
                            userId = it.userId,
                            email = it.email,
                            name = it.name,
                            photoUrl = it.photoUrl,
                            token = it.token,
                            username = it.username
                        )
                    }
                    true
                } else {
                    false
                }
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }
}

data class GoogleSignInRequest(
    val idToken: String,
    val email: String,
    val name: String,
    val photoUrl: String? = null
)

data class GoogleSignInResponse(
    val userId: String,
    val email: String,
    val name: String,
    val photoUrl: String? = null,
    val token: String,
    val username: String? = null,
    val isNewUser: Boolean = false
)
