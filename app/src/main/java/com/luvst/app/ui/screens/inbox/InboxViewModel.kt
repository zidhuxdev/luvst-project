package com.luvst.app.ui.screens.inbox

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.luvst.app.data.local.UserPreferences
import com.luvst.app.data.remote.ApiService
import com.luvst.app.ui.screens.luvst.RateMediaRequest
import com.luvst.app.ui.screens.luvst.SharedMedia
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InboxViewModel @Inject constructor(
    private val userPreferences: UserPreferences,
    private val apiService: ApiService
) : ViewModel() {
    
    private val _uiState = mutableStateOf(InboxUiState())
    val uiState: State<InboxUiState> = _uiState
    
    init {
        loadReceivedMedia()
    }
    
    private fun loadReceivedMedia() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                
                val user = userPreferences.getUser() ?: run {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    return@launch
                }
                
                val response = apiService.getReceivedMedia(user.userId)
                
                if (response.isSuccessful) {
                    val mediaList = response.body() ?: emptyList()
                    _uiState.value = _uiState.value.copy(
                        receivedMedia = mediaList,
                        isLoading = false
                    )
                } else {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }
    
    fun rateMedia(mediaId: String, rating: Int) {
        viewModelScope.launch {
            try {
                val user = userPreferences.getUser() ?: return@launch
                
                val response = apiService.rateMedia(
                    mediaId = mediaId,
                    request = RateMediaRequest(
                        raterId = user.userId,
                        rating = rating
                    )
                )
                
                if (response.isSuccessful) {
                    // Refresh list
                    loadReceivedMedia()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    fun refresh() {
        loadReceivedMedia()
    }
}

data class InboxUiState(
    val receivedMedia: List<SharedMedia> = emptyList(),
    val isLoading: Boolean = false
)
