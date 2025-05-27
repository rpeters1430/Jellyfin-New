package com.example.jellyfinnew.di

import android.content.Context
import com.example.jellyfinnew.data.JellyfinRepository
import com.example.jellyfinnew.data.UserPreferencesManager
import com.example.jellyfinnew.data.repositories.ConnectionRepository
import com.example.jellyfinnew.data.repositories.MediaRepository
import com.example.jellyfinnew.data.repositories.TvShowsRepository
import com.example.jellyfinnew.data.repositories.StreamingRepository

object ServiceLocator {
    @Volatile
    private var repository: JellyfinRepository? = null
    
    @Volatile
    private var userPreferencesManager: UserPreferencesManager? = null
    
    @Volatile
    private var connectionRepository: ConnectionRepository? = null
    
    @Volatile
    private var mediaRepository: MediaRepository? = null
    
    @Volatile
    private var tvShowsRepository: TvShowsRepository? = null
    
    @Volatile
    private var streamingRepository: StreamingRepository? = null
    
    fun provideRepository(context: Context): JellyfinRepository {
        return repository ?: synchronized(this) {
            repository ?: JellyfinRepository(
                context.applicationContext,
                provideConnectionRepository(context),
                provideMediaRepository(),
                provideTvShowsRepository(),
                provideStreamingRepository()
            ).also { repository = it }
        }
    }
    
    fun provideConnectionRepository(context: Context): ConnectionRepository {
        return connectionRepository ?: synchronized(this) {
            connectionRepository ?: ConnectionRepository(context.applicationContext).also { connectionRepository = it }
        }
    }
    
    fun provideMediaRepository(): MediaRepository {
        return mediaRepository ?: synchronized(this) {
            mediaRepository ?: MediaRepository().also { mediaRepository = it }
        }
    }
    
    fun provideTvShowsRepository(): TvShowsRepository {
        return tvShowsRepository ?: synchronized(this) {
            tvShowsRepository ?: TvShowsRepository().also { tvShowsRepository = it }
        }
    }
    
    fun provideStreamingRepository(): StreamingRepository {
        return streamingRepository ?: synchronized(this) {
            streamingRepository ?: StreamingRepository().also { streamingRepository = it }
        }
    }
    
    fun provideUserPreferencesManager(context: Context): UserPreferencesManager {
        return userPreferencesManager ?: synchronized(this) {
            userPreferencesManager ?: UserPreferencesManager(context.applicationContext).also { userPreferencesManager = it }
        }
    }
}
