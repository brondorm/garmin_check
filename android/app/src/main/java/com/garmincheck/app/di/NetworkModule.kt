package com.garmincheck.app.di

import com.garmincheck.app.data.garmin.GarminConnectClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideGarminConnectClient(): GarminConnectClient {
        return GarminConnectClient()
    }
}
