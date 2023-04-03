package com.example.bitcoinwallet.di

import android.content.Context
import com.example.bitcoinwallet.data.WalletRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideWalletRepository(
        @ApplicationContext context: Context
    ): WalletRepository {
        return WalletRepository(context = context)
    }

}