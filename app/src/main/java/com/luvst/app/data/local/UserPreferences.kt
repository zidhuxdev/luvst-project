package com.luvst.app.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

@Singleton
class UserPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val json = Json { ignoreUnknownKeys = true }
    
    companion object {
        val USER_DATA = stringPreferencesKey("user_data")
        val AUTH_TOKEN = stringPreferencesKey("auth_token")
    }
    
    suspend fun saveUser(
        userId: String,
        email: String,
        name: String,
        photoUrl: String?,
        token: String,
        username: String?
    ) {
        val userData = UserData(
            userId = userId,
            email = email,
            name = name,
            photoUrl = photoUrl,
            username = username
        )
        
        context.dataStore.edit { preferences ->
            preferences[USER_DATA] = json.encodeToString(userData)
            preferences[AUTH_TOKEN] = token
        }
    }
    
    suspend fun getUser(): UserData? {
        return context.dataStore.data.map { preferences ->
            preferences[USER_DATA]?.let { json.decodeFromString<UserData>(it) }
        }.first()
    }
    
    suspend fun getToken(): String? {
        return context.dataStore.data.map { preferences ->
            preferences[AUTH_TOKEN]
        }.first()
    }
    
    suspend fun clearUser() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
    
    suspend fun updateUsername(username: String) {
        context.dataStore.edit { preferences ->
            preferences[USER_DATA]?.let { existing ->
                val userData = json.decodeFromString<UserData>(existing)
                val updated = userData.copy(username = username)
                preferences[USER_DATA] = json.encodeToString(updated)
            }
        }
    }
}

@Serializable
data class UserData(
    val userId: String,
    val email: String,
    val name: String,
    val photoUrl: String? = null,
    val username: String? = null
)
