package com.luvst.app.ui.screens.home

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.luvst.app.data.local.UserPreferences
import com.luvst.app.data.remote.ApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val userPreferences: UserPreferences,
    private val apiService: ApiService
) : ViewModel() {
    
    private val _uiState = mutableStateOf<HomeUiState>(HomeUiState.Loading)
    val uiState: State<HomeUiState> = _uiState
    
    init {
        loadUserData()
    }
    
    private fun loadUserData() {
        viewModelScope.launch {
            try {
                val user = userPreferences.getUser()
                if (user != null) {
                    // Fetch partner info from server
                    val response = apiService.getPartnerInfo(user.userId)
                    if (response.isSuccessful) {
                        val partner = response.body()
                        if (partner != null) {
                            _uiState.value = HomeUiState.HasPartner(
                                partnerName = partner.name,
                                partnerPhotoUrl = partner.photoUrl,
                                relationshipStartDate = partner.relationshipStartDate
                            )
                        } else {
                            _uiState.value = HomeUiState.NoPartner()
                        }
                    } else {
                        _uiState.value = HomeUiState.NoPartner()
                    }
                } else {
                    _uiState.value = HomeUiState.NoPartner()
                }
            } catch (e: Exception) {
                _uiState.value = HomeUiState.NoPartner()
            }
        }
    }
    
    fun searchUser(username: String) {
        viewModelScope.launch {
            try {
                val response = apiService.searchUser(username)
                val currentState = _uiState.value as? HomeUiState.NoPartner ?: return@launch
                
                if (response.isSuccessful) {
                    val user = response.body()
                    if (user != null) {
                        _uiState.value = currentState.copy(
                            searchResult = UserSearchResult.Found(
                                username = user.username,
                                name = user.name,
                                photoUrl = user.photoUrl
                            )
                        )
                    } else {
                        _uiState.value = currentState.copy(
                            searchResult = UserSearchResult.NotFound
                        )
                    }
                } else {
                    _uiState.value = currentState.copy(
                        searchResult = UserSearchResult.Error("Search failed")
                    )
                }
            } catch (e: Exception) {
                val currentState = _uiState.value as? HomeUiState.NoPartner
                currentState?.let {
                    _uiState.value = it.copy(
                        searchResult = UserSearchResult.Error(e.message ?: "Unknown error")
                    )
                }
            }
        }
    }
    
    fun sendConnectionRequest(username: String) {
        viewModelScope.launch {
            try {
                val user = userPreferences.getUser() ?: return@launch
                val response = apiService.sendConnectionRequest(
                    SendConnectionRequest(
                        fromUserId = user.userId,
                        toUsername = username,
                        relationshipStartDate = System.currentTimeMillis()
                    )
                )
                
                if (response.isSuccessful) {
                    val currentState = _uiState.value as? HomeUiState.NoPartner
                    currentState?.let {
                        _uiState.value = it.copy(
                            pendingRequest = PendingConnectionRequest(toUsername = username)
                        )
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    fun setRelationshipStartDate(date: Long) {
        // Update the relationship start date
        viewModelScope.launch {
            // Call API to update
        }
    }
    
    fun acceptConnectionRequest(requestId: String) {
        viewModelScope.launch {
            try {
                val response = apiService.acceptConnectionRequest(requestId)
                if (response.isSuccessful) {
                    loadUserData()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

sealed class HomeUiState {
    object Loading : HomeUiState()
    data class NoPartner(
        val searchResult: UserSearchResult? = null,
        val pendingRequest: PendingConnectionRequest? = null
    ) : HomeUiState()
    data class HasPartner(
        val partnerName: String,
        val partnerPhotoUrl: String?,
        val relationshipStartDate: Long
    ) : HomeUiState()
    data class WaitingForAcceptance(
        val partnerName: String
    ) : HomeUiState()
}

sealed class UserSearchResult {
    data class Found(
        val username: String,
        val name: String,
        val photoUrl: String?
    ) : UserSearchResult()
    object NotFound : UserSearchResult()
    data class Error(val message: String) : UserSearchResult()
}

data class PendingConnectionRequest(
    val toUsername: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class SendConnectionRequest(
    val fromUserId: String,
    val toUsername: String,
    val relationshipStartDate: Long
)
