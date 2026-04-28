package com.luvst.app.ui.screens.luvst

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.luvst.app.data.local.UserPreferences
import com.luvst.app.data.remote.ApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import id.zelory.compressor.Compressor
import id.zelory.compressor.constraint.format
import id.zelory.compressor.constraint.quality
import id.zelory.compressor.constraint.resolution
import id.zelory.compressor.constraint.size
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

@HiltViewModel
class LuvstViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userPreferences: UserPreferences,
    private val apiService: ApiService
) : ViewModel() {
    
    private val _uiState = mutableStateOf(LuvstUiState())
    val uiState: State<LuvstUiState> = _uiState
    
    init {
        loadSharedMedia()
    }
    
    private fun loadSharedMedia() {
        viewModelScope.launch {
            try {
                val user = userPreferences.getUser() ?: return@launch
                val response = apiService.getSharedMedia(user.userId)
                if (response.isSuccessful) {
                    response.body()?.let { mediaList ->
                        _uiState.value = _uiState.value.copy(
                            sharedMedia = mediaList,
                            totalPoints = mediaList.sumOf { it.pointsEarned }
                        )
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    fun uploadMedia(uri: Uri) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(uploading = true, uploadProgress = 0f)
                
                val user = userPreferences.getUser() ?: return@launch
                
                // Step 1: Compress media on device
                _uiState.value = _uiState.value.copy(compressionProgress = 0.3f)
                
                val compressedFile = compressMedia(uri)
                
                _uiState.value = _uiState.value.copy(
                    compressionProgress = 1f,
                    uploadProgress = 0.5f
                )
                
                // Step 2: Get compression info
                val originalFile = getFileFromUri(uri)
                val compressionInfo = calculateCompressionInfo(originalFile, compressedFile)
                _uiState.value = _uiState.value.copy(lastCompressionInfo = compressionInfo)
                
                // Step 3: Upload to server
                val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
                val requestFile = compressedFile.asRequestBody(mimeType.toMediaTypeOrNull())
                val body = MultipartBody.Part.createFormData(
                    "media", 
                    compressedFile.name, 
                    requestFile
                )
                
                val metadata = """
                    {
                        "senderId": "${user.userId}",
                        "type": "${if (mimeType.startsWith("video")) "video" else "image"}",
                    }
                """.trimIndent().toRequestBody("application/json".toMediaTypeOrNull())
                
                val response = apiService.uploadMedia(body, metadata)
                
                if (response.isSuccessful) {
                    _uiState.value = _uiState.value.copy(uploadProgress = 1f)
                    // Refresh media list
                    loadSharedMedia()
                }
                
                // Cleanup
                compressedFile.delete()
                
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _uiState.value = _uiState.value.copy(
                    uploading = false,
                    uploadProgress = 0f,
                    compressionProgress = 0f
                )
            }
        }
    }
    
    private suspend fun compressMedia(uri: Uri): File = withContext(Dispatchers.IO) {
        val mimeType = context.contentResolver.getType(uri)
        val isVideo = mimeType?.startsWith("video") == true
        
        if (isVideo) {
            compressVideo(uri)
        } else {
            compressImage(uri)
        }
    }
    
    private suspend fun compressImage(uri: Uri): File = withContext(Dispatchers.IO) {
        val inputStream = context.contentResolver.openInputStream(uri)
        val tempFile = File(context.cacheDir, "temp_image_${System.currentTimeMillis()}.jpg")
        
        inputStream?.use { input ->
            FileOutputStream(tempFile).use { output ->
                input.copyTo(output)
            }
        }
        
        // Use Compressor library with aggressive settings
        Compressor.compress(context, tempFile) {
            resolution(1280, 1280) // Max 1280px on longest side
            quality(75) // 75% quality (60-85% range)
            format(Bitmap.CompressFormat.JPEG)
            size(500_000) // Max 500KB
        }
    }
    
    private suspend fun compressVideo(uri: Uri): File = withContext(Dispatchers.IO) {
        val inputStream = context.contentResolver.openInputStream(uri)
        val tempFile = File(context.cacheDir, "temp_video_${System.currentTimeMillis()}.mp4")
        
        inputStream?.use { input ->
            FileOutputStream(tempFile).use { output ->
                input.copyTo(output)
            }
        }
        
        // For video, we rely on server-side compression
        // But we limit file size to 50MB for upload
        val maxSize = 50 * 1024 * 1024 // 50MB
        if (tempFile.length() > maxSize) {
            throw Exception("Video file too large. Please select a video under 50MB.")
        }
        
        tempFile
    }
    
    private fun getFileFromUri(uri: Uri): File? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val tempFile = File(context.cacheDir, "original_${System.currentTimeMillis()}.tmp")
            inputStream?.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            }
            tempFile
        } catch (e: Exception) {
            null
        }
    }
    
    private fun calculateCompressionInfo(
        original: File?, 
        compressed: File
    ): CompressionInfo? {
        if (original == null) return null
        
        val originalSize = original.length()
        val compressedSize = compressed.length()
        
        // Get dimensions
        val originalBitmap = BitmapFactory.decodeFile(original.absolutePath)
        val compressedBitmap = BitmapFactory.decodeFile(compressed.absolutePath)
        
        val savedPercentage = ((originalSize - compressedSize) * 100 / originalSize).toInt()
        
        val info = CompressionInfo(
            originalSize = originalSize,
            compressedSize = compressedSize,
            originalDimensions = Pair(originalBitmap?.width ?: 0, originalBitmap?.height ?: 0),
            compressedDimensions = Pair(compressedBitmap?.width ?: 0, compressedBitmap?.height ?: 0),
            format = "JPEG",
            quality = 75,
            savedPercentage = savedPercentage.coerceAtLeast(0)
        )
        
        original.delete()
        originalBitmap?.recycle()
        compressedBitmap?.recycle()
        
        return info
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
                    loadSharedMedia()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

data class LuvstUiState(
    val sharedMedia: List<SharedMedia> = emptyList(),
    val totalPoints: Int = 0,
    val uploading: Boolean = false,
    val uploadProgress: Float = 0f,
    val compressionProgress: Float = 0f,
    val lastCompressionInfo: CompressionInfo? = null
)

data class RateMediaRequest(
    val raterId: String,
    val rating: Int
)
