package com.example.jellyfinnew.di

import android.content.Context
import com.example.jellyfinnew.data.JellyfinRepository
import com.example.jellyfinnew.data.UserPreferencesManager

object ServiceLocator {
    @Volatile
    private var repository: JellyfinRepository? = null
    
    @Volatile
    private var userPreferencesManager: UserPreferencesManager? = null
    
    fun provideRepository(context: Context): JellyfinRepository {
        return repository ?: synchronized(this) {
            repository ?: JellyfinRepository(context).also { repository = it }
        }
    }
    
    fun provideUserPreferencesManager(context: Context): UserPreferencesManager {
        return userPreferencesManager ?: synchronized(this) {
            userPreferencesManager ?: UserPreferencesManager(context).also { userPreferencesManager = it }
        }
    }
}
