package com.sharjeel.whatsappstatussaver.di

import android.content.Context
import androidx.room.Room
import com.sharjeel.whatsappstatussaver.data.local.StatusDatabase
import com.sharjeel.whatsappstatussaver.data.local.dao.SavedMediaDao
import com.sharjeel.whatsappstatussaver.data.repository.StorageRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideStatusDatabase(@ApplicationContext context: Context): StatusDatabase {
        return Room.databaseBuilder(
            context,
            StatusDatabase::class.java,
            "status_database"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    @Singleton
    fun provideSavedMediaDao(database: StatusDatabase): SavedMediaDao {
        return database.savedMediaDao()
    }

    @Provides
    @Singleton
    fun provideReminderDao(database: StatusDatabase): com.sharjeel.whatsappstatussaver.data.local.dao.ReminderDao {
        return database.reminderDao()
    }

    @Provides
    @Singleton
    fun provideStorageRepository(
        @ApplicationContext context: Context,
        savedMediaDao: SavedMediaDao,
        appSettings: com.sharjeel.whatsappstatussaver.data.local.datastore.AppSettings
    ): StorageRepository {
        return StorageRepository(context, savedMediaDao, appSettings)
    }
}

