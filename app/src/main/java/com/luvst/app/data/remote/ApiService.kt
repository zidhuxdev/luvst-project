package com.luvst.app.data.remote

import com.luvst.app.ui.screens.home.SendConnectionRequest
import com.luvst.app.ui.screens.login.GoogleSignInRequest
import com.luvst.app.ui.screens.login.GoogleSignInResponse
import com.luvst.app.ui.screens.luvst.CompressionInfo
import com.luvst.app.ui.screens.luvst.RateMediaRequest
import com.luvst.app.ui.screens.luvst.SharedMedia
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {
    
    // Auth
    @POST("api/auth/google")
    suspend fun googleSignIn(
        @Body request: GoogleSignInRequest
    ): Response<GoogleSignInResponse>
    
    // User
    @GET("api/users/search")
    suspend fun searchUser(
        @Query("username") username: String
    ): Response<UserSearchResponse>
    
    @GET("api/users/{userId}/partner")
    suspend fun getPartnerInfo(
        @Path("userId") userId: String
    ): Response<PartnerResponse>
    
    // Connections
    @POST("api/connections/request")
    suspend fun sendConnectionRequest(
        @Body request: SendConnectionRequest
    ): Response<ConnectionResponse>
    
    @POST("api/connections/{requestId}/accept")
    suspend fun acceptConnectionRequest(
        @Path("requestId") requestId: String
    ): Response<ConnectionResponse>
    
    // Media
    @Multipart
    @POST("api/media/upload")
    suspend fun uploadMedia(
        @Part file: MultipartBody.Part,
        @Part("metadata") metadata: RequestBody
    ): Response<UploadMediaResponse>
    
    @GET("api/users/{userId}/media")
    suspend fun getSharedMedia(
        @Path("userId") userId: String
    ): Response<List<SharedMedia>>
    
    @GET("api/users/{userId}/received-media")
    suspend fun getReceivedMedia(
        @Path("userId") userId: String
    ): Response<List<SharedMedia>>
    
    @POST("api/media/{mediaId}/rate")
    suspend fun rateMedia(
        @Path("mediaId") mediaId: String,
        @Body request: RateMediaRequest
    ): Response<RateMediaResponse>
    
    // Voice messages
    @Multipart
    @POST("api/voice-messages/upload")
    suspend fun uploadVoiceMessage(
        @Part file: MultipartBody.Part,
        @Part("senderId") senderId: RequestBody,
        @Part("receiverId") receiverId: RequestBody
    ): Response<VoiceMessageResponse>
    
    @GET("api/users/{userId}/voice-messages")
    suspend fun getVoiceMessages(
        @Path("userId") userId: String
    ): Response<List<VoiceMessageItem>>
}

data class UserSearchResponse(
    val userId: String,
    val username: String,
    val name: String,
    val photoUrl: String?
)

data class PartnerResponse(
    val userId: String,
    val name: String,
    val photoUrl: String?,
    val relationshipStartDate: Long
)

data class ConnectionResponse(
    val requestId: String,
    val status: String,
    val message: String
)

data class UploadMediaResponse(
    val mediaId: String,
    val url: String,
    val thumbnailUrl: String?,
    val points: Int,
    val compressionInfo: CompressionInfo?
)

data class RateMediaResponse(
    val mediaId: String,
    val rating: Int,
    val pointsAwarded: Int,
    val senderNewTotal: Int
)

data class VoiceMessageResponse(
    val messageId: String,
    val url: String,
    val duration: Int,
    val timestamp: Long
)

data class VoiceMessageItem(
    val messageId: String,
    val senderId: String,
    val senderName: String,
    val url: String,
    val duration: Int,
    val timestamp: Long,
    val isListened: Boolean
)
