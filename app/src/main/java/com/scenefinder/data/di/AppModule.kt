package com.scenefinder.data.di

import android.content.Context
import androidx.room.Room
import com.scenefinder.data.local.AppDatabase
import com.scenefinder.data.local.dao.EnrichedMediaDao
import com.scenefinder.data.remote.api.AniListApi
import com.scenefinder.data.remote.api.AniListOAuthApi
import com.scenefinder.data.remote.api.MangaDexApi
import com.scenefinder.data.remote.api.TmdbAccountApi
import com.scenefinder.data.remote.api.TmdbApi
import com.scenefinder.data.remote.api.TraceMoeApi
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDefaultDispatcher(): CoroutineDispatcher = Dispatchers.Default

    @Provides
    @Singleton
    fun provideMoshi(): Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        return OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "scenefinder.db"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    @Singleton
    fun provideEnrichedMediaDao(db: AppDatabase): EnrichedMediaDao {
        return db.enrichedMediaDao()
    }

    @Provides
    @Singleton
    fun provideAniListApi(okHttpClient: OkHttpClient, moshi: Moshi): AniListApi {
        return Retrofit.Builder()
            .baseUrl("https://graphql.anilist.co/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(AniListApi::class.java)
    }

    @Provides
    @Singleton
    fun provideTmdbApi(okHttpClient: OkHttpClient, moshi: Moshi): TmdbApi {
        return Retrofit.Builder()
            .baseUrl("https://api.themoviedb.org/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(TmdbApi::class.java)
    }

    @Provides
    @Singleton
    fun provideMangaDexApi(okHttpClient: OkHttpClient, moshi: Moshi): MangaDexApi {
        return Retrofit.Builder()
            .baseUrl("https://api.mangadex.org/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(MangaDexApi::class.java)
    }

    @Provides
    @Singleton
    fun provideTraceMoeApi(okHttpClient: OkHttpClient, moshi: Moshi): TraceMoeApi {
        return Retrofit.Builder()
            .baseUrl("https://api.trace.moe/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(TraceMoeApi::class.java)
    }

    @Provides
    @Singleton
    fun provideAniListOAuthApi(okHttpClient: OkHttpClient, moshi: Moshi): AniListOAuthApi {
        return Retrofit.Builder()
            .baseUrl("https://anilist.co/api/v2/oauth/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(AniListOAuthApi::class.java)
    }

    @Provides
    @Singleton
    fun provideTmdbAccountApi(okHttpClient: OkHttpClient, moshi: Moshi): TmdbAccountApi {
        return Retrofit.Builder()
            .baseUrl("https://api.themoviedb.org/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(TmdbAccountApi::class.java)
    }
}
