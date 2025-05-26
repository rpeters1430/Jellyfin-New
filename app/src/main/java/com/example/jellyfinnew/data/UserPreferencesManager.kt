package com.example.jellyfinnew.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Extension to create DataStore instance
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

data class LoginCredentials(
    val serverUrl: String = "",
    val username: String = "",
    val password: String = ""
)

class UserPreferencesManager(private val context: Context) {
    
    companion object {
        private val SERVER_URL_KEY = stringPreferencesKey("server_url")
        private val USERNAME_KEY = stringPreferencesKey("username")
        private val PASSWORD_KEY = stringPreferencesKey("password")
    }
    
    val loginCredentials: Flow<LoginCredentials> = context.dataStore.data.map { preferences ->
        LoginCredentials(
            serverUrl = preferences[SERVER_URL_KEY] ?: "",
            username = preferences[USERNAME_KEY] ?: "",
            password = preferences[PASSWORD_KEY] ?: ""
        )
    }
    
    suspend fun saveLoginCredentials(serverUrl: String, username: String, password: String) {
        context.dataStore.edit { preferences ->
            preferences[SERVER_URL_KEY] = serverUrl
            preferences[USERNAME_KEY] = username
            preferences[PASSWORD_KEY] = password
        }
    }
    
    suspend fun clearLoginCredentials() {
        context.dataStore.edit { preferences ->
            preferences.remove(SERVER_URL_KEY)
            preferences.remove(USERNAME_KEY)
            preferences.remove(PASSWORD_KEY)
        }
    }
}
