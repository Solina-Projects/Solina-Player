package app.solina.player.di

import android.content.Context
import androidx.annotation.OptIn
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.work.WorkManager
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import app.solina.player.BuildConfig
import app.solina.player.SolinaPlayerApplication
import app.solina.player.data.database.AlbumArtThemeDao
import app.solina.player.data.database.EngagementDao
import app.solina.player.data.database.FavoritesDao
import app.solina.player.data.database.LyricsDao
import app.solina.player.data.database.LocalPlaylistDao
import app.solina.player.data.database.ListenBrainzDao
import app.solina.player.data.database.AudioBookmarkDao
import app.solina.player.data.database.MIGRATION_1_2
import app.solina.player.data.database.MIGRATION_2_3
import app.solina.player.data.database.MIGRATION_3_4
import app.solina.player.data.database.MIGRATION_4_5
import app.solina.player.data.database.MIGRATION_5_6
import app.solina.player.data.database.MusicDao
import app.solina.player.data.database.OfflineTrackDao
import app.solina.player.data.database.SolinaPlayerDatabase
import app.solina.player.data.database.SearchHistoryDao
import app.solina.player.data.database.TransitionDao
import app.solina.player.data.preferences.UserPreferencesRepository
import app.solina.player.data.preferences.PlaylistPreferencesRepository
import app.solina.player.data.preferences.dataStore
import app.solina.player.data.media.SongMetadataEditor
import app.solina.player.data.network.deezer.DeezerApiService
import app.solina.player.data.network.lyrics.LrcLibApiService
import app.solina.player.data.repository.ArtistImageRepository
import app.solina.player.data.repository.AudioBookmarkRepository
import app.solina.player.data.repository.AudioBookmarkRepositoryImpl
import app.solina.player.data.repository.LyricsRepository
import app.solina.player.data.repository.LyricsRepositoryImpl
import app.solina.player.data.repository.MediaStoreSongRepository
import app.solina.player.data.repository.MusicRepository
import app.solina.player.data.repository.MusicRepositoryImpl
import app.solina.player.data.repository.SongRepository
import app.solina.player.data.repository.TransitionRepository
import app.solina.player.data.repository.TransitionRepositoryImpl
import app.solina.player.data.repository.FolderTreeBuilder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import javax.inject.Qualifier
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Singleton
    @Provides
    fun provideApplication(@ApplicationContext app: Context): SolinaPlayerApplication {
        return app as SolinaPlayerApplication
    }

    @Singleton
    @Provides
    fun provideGson(): com.google.gson.Gson {
        return com.google.gson.Gson()
    }

    @OptIn(UnstableApi::class)
    @Singleton
    @Provides
    fun provideSessionToken(@ApplicationContext context: Context): androidx.media3.session.SessionToken {
        return androidx.media3.session.SessionToken(
            context,
            android.content.ComponentName(context, app.solina.player.data.service.MusicService::class.java)
        )
    }

    @Provides
    @Singleton
    fun providePreferencesDataStore(
        @ApplicationContext context: Context
    ): DataStore<Preferences> = context.dataStore

    @Singleton
    @Provides
    fun provideJson(): Json {
        return Json {
            isLenient = true
            ignoreUnknownKeys = true
            coerceInputValues = true
        }
    }

    @Singleton
    @Provides
    @AppScope
    fun provideAppCoroutineScope(dispatchers: DispatcherProvider): CoroutineScope {
        return CoroutineScope(SupervisorJob() + dispatchers.io)
    }

    @Singleton
    @Provides
    fun provideWorkManager(@ApplicationContext context: Context): WorkManager {
        return WorkManager.getInstance(context)
    }

    @Singleton
    @Provides
    fun provideSolinaPlayerDatabase(@ApplicationContext context: Context): SolinaPlayerDatabase {
        val builder = Room.databaseBuilder(
            context.applicationContext,
            SolinaPlayerDatabase::class.java,
            "solina_database"
        )
            .addCallback(SolinaPlayerDatabase.createRuntimeArtifactsCallback())
            .addMigrations(
                MIGRATION_1_2,
                MIGRATION_2_3,
                MIGRATION_3_4,
                MIGRATION_4_5,
                MIGRATION_5_6,
            )
            .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)

        if (BuildConfig.DEBUG) {
            builder.fallbackToDestructiveMigration(dropAllTables = true)
        }

        return builder.build()
    }

    @Singleton
    @Provides
    fun provideAlbumArtThemeDao(database: SolinaPlayerDatabase): AlbumArtThemeDao {
        return database.albumArtThemeDao()
    }

    @Singleton
    @Provides
    fun provideSearchHistoryDao(database: SolinaPlayerDatabase): SearchHistoryDao {
        return database.searchHistoryDao()
    }

    @Singleton
    @Provides
    fun provideMusicDao(database: SolinaPlayerDatabase): MusicDao {
        return database.musicDao()
    }

    @Singleton
    @Provides
    fun provideTransitionDao(database: SolinaPlayerDatabase): TransitionDao {
        return database.transitionDao()
    }

    @Singleton
    @Provides
    fun provideEngagementDao(database: SolinaPlayerDatabase): EngagementDao {
        return database.engagementDao()
    }

    @Singleton
    @Provides
    fun provideFavoritesDao(database: SolinaPlayerDatabase): FavoritesDao {
        return database.favoritesDao()
    }

    @Singleton
    @Provides
    fun provideLyricsDao(database: SolinaPlayerDatabase): LyricsDao {
        return database.lyricsDao()
    }

    @Singleton
    @Provides
    fun provideLocalPlaylistDao(database: SolinaPlayerDatabase): LocalPlaylistDao {
        return database.localPlaylistDao()
    }

    @Singleton
    @Provides
    fun provideNavidromeDao(database: SolinaPlayerDatabase): app.solina.player.data.database.NavidromeDao {
        return database.navidromeDao()
    }
    
    @Singleton
    @Provides
    fun provideJellyfinDao(database: SolinaPlayerDatabase): app.solina.player.data.database.JellyfinDao {
        return database.jellyfinDao()
    }

    @Singleton
    @Provides
    fun provideListenBrainzDao(database: SolinaPlayerDatabase): ListenBrainzDao {
        return database.listenBrainzDao()
    }

    @Singleton
    @Provides
    fun provideAudioBookmarkDao(database: SolinaPlayerDatabase): AudioBookmarkDao {
        return database.audioBookmarkDao()
    }

    @Singleton
    @Provides
    fun provideOfflineTrackDao(database: SolinaPlayerDatabase): OfflineTrackDao {
        return database.offlineTrackDao()
    }

    @Provides
    @Singleton
    fun provideImageLoader(
        @ApplicationContext context: Context,
        okHttpClient: OkHttpClient
    ): ImageLoader {
        return ImageLoader.Builder(context)
            .okHttpClient(okHttpClient)
            .dispatcher(Dispatchers.Default)
            .allowHardware(true)
            .memoryCache {
                MemoryCache.Builder(context)
                    .maxSizeBytes(40 * 1024 * 1024)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve("image_cache"))
                    .maxSizeBytes(100L * 1024 * 1024)
                    .build()
            }
            .respectCacheHeaders(false)
            .build()
    }

    @Provides
    @Singleton
    fun provideLyricsRepository(
        @ApplicationContext context: Context,
        lrcLibApiService: LrcLibApiService,
        lyricsDao: LyricsDao,
        okHttpClient: OkHttpClient,
        userPreferencesRepository: UserPreferencesRepository
    ): LyricsRepository {
        return LyricsRepositoryImpl(
            context = context,
            lrcLibApiService = lrcLibApiService,
            lyricsDao = lyricsDao,
            okHttpClient = okHttpClient,
            userPreferencesRepository = userPreferencesRepository
        )
    }

    @Provides
    @Singleton
    fun provideSongRepository(
        @ApplicationContext context: Context,
        mediaStoreObserver: app.solina.player.data.observer.MediaStoreObserver,
        favoritesDao: FavoritesDao,
        userPreferencesRepository: UserPreferencesRepository,
        musicDao: MusicDao
    ): SongRepository {
        return MediaStoreSongRepository(
            context = context,
            mediaStoreObserver = mediaStoreObserver,
            favoritesDao = favoritesDao,
            userPreferencesRepository = userPreferencesRepository,
            musicDao = musicDao
        )
    }

    @Provides
    @Singleton
    fun provideVideoRepository(
        @ApplicationContext context: Context,
        mediaStoreObserver: app.solina.player.data.observer.MediaStoreObserver
    ): app.solina.player.data.repository.VideoRepository {
        return app.solina.player.data.repository.MediaStoreVideoRepository(
            context = context,
            mediaStoreObserver = mediaStoreObserver
        )
    }

    @Provides
    @Singleton
    fun provideFolderTreeBuilder(): FolderTreeBuilder {
        return FolderTreeBuilder()
    }

    @Provides
    @Singleton
    fun provideMusicRepository(
        @ApplicationContext context: Context,
        userPreferencesRepository: UserPreferencesRepository,
        playlistPreferencesRepository: PlaylistPreferencesRepository,
        searchHistoryDao: SearchHistoryDao,
        musicDao: MusicDao,
        lyricsRepository: LyricsRepository,
        songRepository: SongRepository,
        favoritesDao: FavoritesDao,
        artistImageRepository: ArtistImageRepository,
        folderTreeBuilder: FolderTreeBuilder
    ): MusicRepository {
        return MusicRepositoryImpl(
            context = context,
            userPreferencesRepository = userPreferencesRepository,
            playlistPreferencesRepository = playlistPreferencesRepository,
            searchHistoryDao = searchHistoryDao,
            musicDao = musicDao,
            lyricsRepository = lyricsRepository,
            songRepository = songRepository,
            favoritesDao = favoritesDao,
            artistImageRepository = artistImageRepository,
            folderTreeBuilder = folderTreeBuilder
        )

    }

    @Provides
    @Singleton
    fun provideTransitionRepository(
        transitionRepositoryImpl: TransitionRepositoryImpl
    ): TransitionRepository {
        return transitionRepositoryImpl
    }

    @Provides
    @Singleton
    fun provideAudioBookmarkRepository(
        audioBookmarkRepositoryImpl: AudioBookmarkRepositoryImpl
    ): AudioBookmarkRepository {
        return audioBookmarkRepositoryImpl
    }

    @Singleton
    @Provides
    fun provideSongMetadataEditor(
        @ApplicationContext context: Context,
        musicDao: MusicDao,
        userPreferencesRepository: UserPreferencesRepository
    ): SongMetadataEditor {
        return SongMetadataEditor(context, musicDao, userPreferencesRepository)
    }

    /**
     * Provides a singleton OkHttpClient instance with logging and a User-Agent interceptor.
     * Retry logic with backoff is handled in coroutine-based callers.
     */
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.HEADERS
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
            redactHeader("Authorization")
            redactHeader("Proxy-Authorization")
            redactHeader("Cookie")
            redactHeader("Set-Cookie")
            redactHeader("X-Emby-Token")
            redactHeader("X-Emby-Authorization")
            redactHeader("X-MediaBrowser-Token")
        }
        
        val connectionPool = okhttp3.ConnectionPool(
            maxIdleConnections = 5,
            keepAliveDuration = 30,
            timeUnit = java.util.concurrent.TimeUnit.SECONDS
        )
        
        return OkHttpClient.Builder()
            .connectionPool(connectionPool)
            .connectTimeout(8, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(8, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(8, java.util.concurrent.TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .addInterceptor { chain ->
                val originalRequest = chain.request()
                val requestWithUserAgent = originalRequest.newBuilder()
                    .header("User-Agent", "SolinaPlayer/1.0 (Android; Music Player)")
                    .build()
                chain.proceed(requestWithUserAgent)
            }
            .addInterceptor(loggingInterceptor)
            .build()
    }

    /**
     * Provides an OkHttpClient instance with timeouts for lyrics searches.
     * Includes DNS resolver, modern TLS, connection pool, and connection retry.
     */
    @Provides
    @Singleton
    @FastOkHttpClient
    fun provideFastOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.HEADERS
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
            redactHeader("Authorization")
            redactHeader("Proxy-Authorization")
            redactHeader("Cookie")
            redactHeader("Set-Cookie")
            redactHeader("X-Emby-Token")
            redactHeader("X-Emby-Authorization")
            redactHeader("X-MediaBrowser-Token")
        }
        
        val connectionPool = okhttp3.ConnectionPool(
            maxIdleConnections = 5,
            keepAliveDuration = 30,
            timeUnit = java.util.concurrent.TimeUnit.SECONDS
        )
        
        val dns = okhttp3.Dns { hostname ->
            try {
                okhttp3.Dns.SYSTEM.lookup(hostname)
            } catch (e: Exception) {
                java.net.InetAddress.getAllByName(hostname).toList()
            }
        }

        return OkHttpClient.Builder()
            .dns(dns)
            .connectionPool(connectionPool)
            .protocols(listOf(okhttp3.Protocol.HTTP_1_1))
            .connectionSpecs(listOf(
                okhttp3.ConnectionSpec.MODERN_TLS,
                okhttp3.ConnectionSpec.COMPATIBLE_TLS
            ))
            .connectTimeout(8, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(8, java.util.concurrent.TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .addInterceptor { chain ->
                val originalRequest = chain.request()
                val requestWithHeaders = originalRequest.newBuilder()
                    .header("User-Agent", "SolinaPlayer/1.0 (Android; Music Player)")
                    .header("Accept", "application/json")
                    .build()
                chain.proceed(requestWithHeaders)
            }
            .addInterceptor(loggingInterceptor)
            .build()
    }

    /**
     * Provides a singleton Retrofit instance for the LRCLIB API.
     */
    @Provides
    @Singleton
    fun provideRetrofit(@FastOkHttpClient okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://lrclib.net/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    /**
     * Provides a singleton instance of the LRCLIB API service.
     */
    @Provides
    @Singleton
    fun provideLrcLibApiService(retrofit: Retrofit): LrcLibApiService {
        return retrofit.create(LrcLibApiService::class.java)
    }

    /**
     * Provides a Retrofit instance for the Deezer API.
     */
    @Provides
    @Singleton
    @DeezerRetrofit
    fun provideDeezerRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://api.deezer.com/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    /**
     * Provides the Deezer API service.
     */
    @Provides
    @Singleton
    fun provideDeezerApiService(@DeezerRetrofit retrofit: Retrofit): DeezerApiService {
        return retrofit.create(DeezerApiService::class.java)
    }

    /**
     * Provides the artist image repository.
     */
    @Provides
    @Singleton
    fun provideArtistImageRepository(
        deezerApiService: DeezerApiService,
        musicDao: MusicDao,
        userPreferencesRepository: UserPreferencesRepository,
        @ApplicationContext context: Context
    ): ArtistImageRepository {
        return ArtistImageRepository(deezerApiService, musicDao, userPreferencesRepository, context)
    }
}
