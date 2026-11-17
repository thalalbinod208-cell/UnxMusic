package com.maxrave.simpmusic

import android.annotation.SuppressLint
import android.app.Application
import android.database.CursorWindow
import android.os.Build
import android.util.Log
import androidx.work.Configuration
import androidx.work.WorkManager
import cat.ereza.customactivityoncrash.config.CaocConfig
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.annotation.ExperimentalCoilApi
import coil3.compose.preferEndFirstIntrinsicSize
import coil3.disk.DiskCache
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.CachePolicy
import coil3.request.crossfade
import com.maxrave.common.AppIdentity
import com.maxrave.data.di.loader.loadAllModules
import com.maxrave.domain.manager.DataStoreManager
import com.maxrave.domain.repository.LibrarySyncPhoneRepository
import com.maxrave.domain.data.entities.BugTicketEntity
import com.maxrave.domain.repository.SelfHealRepository
import com.maxrave.domain.selfheal.ErrorFingerprintGenerator
import com.maxrave.domain.selfheal.SelfHealEventBus
import com.maxrave.logger.Logger
import com.maxrave.simpmusic.di.viewModelModule
import com.maxrave.simpmusic.service.backup.AutoBackupScheduler
import com.maxrave.simpmusic.service.sync.LibrarySyncScheduler
import com.maxrave.simpmusic.utils.NetworkFirstInterceptor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import multiplatform.network.cmptoast.AppContext
import okhttp3.OkHttpClient
import okio.FileSystem
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.component.inject
import org.koin.core.context.loadKoinModules
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import org.simpmusic.crashlytics.configCrashlytics
import org.simpmusic.discord.configDiscord
import org.simpmusic.lastfm.configLastfm
import java.io.File
import java.lang.reflect.Field
import kotlin.time.Clock

class SimpMusicApplication :
    Application(),
    KoinComponent,
    SingletonImageLoader.Factory {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val dataStoreManager: DataStoreManager by inject()
    private lateinit var autoBackupScheduler: AutoBackupScheduler

    override fun onCreate() {
        super.onCreate()
        // First, so the rest of startup reaches the App log too (Settings → Developer option).
        Logger.enableFileLogging(File(filesDir, "logs").path)
        configCrashlytics(this, BuildKonfig.sentryDsn)
        configLastfm(BuildKonfig.lastfmApiKey, BuildKonfig.lastfmSecret)
        // Before Koin: DiscordRepository is created at start and reads which login this build uses.
        configDiscord(BuildKonfig.discordClientId)
        startKoin {
            androidLogger(level = Level.DEBUG)
            androidContext(this@SimpMusicApplication)
            loadAllModules(
                AppIdentity(
                    applicationId = BuildConfig.APPLICATION_ID,
                    versionName = BuildConfig.VERSION_NAME,
                    platform = "Android ${Build.VERSION.RELEASE}",
                    isDevBuild = BuildConfig.DEBUG,
                ),
            )
            loadKoinModules(viewModelModule)
        }
        val selfHealRepository: SelfHealRepository = get()
        applicationScope.launch(Dispatchers.IO) {
            runCatching {
                val pending = File(filesDir, "pending_unx_crash.txt")
                if (pending.exists()) {
                    val trace = pending.readText().take(48_000)
                    val now = Clock.System.now().toEpochMilliseconds()
                    val hash = ErrorFingerprintGenerator.generate("UNCAUGHT_CRASH", trace)
                    selfHealRepository.record(BugTicketEntity(
                        errorHash = hash,
                        source = "RUNTIME_CRASH",
                        title = trace.lineSequence().firstOrNull { it.isNotBlank() }?.take(180) ?: "Previous app crash",
                        category = "CRITICAL_CRASH",
                        severity = "CRITICAL",
                        stackTrace = trace,
                        deviceContext = "Android ${Build.VERSION.RELEASE}; ${Build.MANUFACTURER} ${Build.MODEL}; ${BuildConfig.VERSION_NAME}",
                        createdAt = now,
                        updatedAt = now,
                    ))
                    pending.delete()
                }
            }
        }
        SelfHealEventBus.onPlaybackFailure = { title, mediaId, code, message ->
            applicationScope.launch(Dispatchers.IO) {
                val now = Clock.System.now().toEpochMilliseconds()
                val diagnostic = "${message.take(4_000)}\ntrack=${mediaId.take(256)}"
                val hash = ErrorFingerprintGenerator.generate("PLAYBACK_ERROR", diagnostic, code.toString())
                runCatching {
                    selfHealRepository.record(BugTicketEntity(
                        errorHash = hash,
                        source = "PLAYBACK_ERROR",
                        title = title.take(180).ifBlank { "Playback error $code" },
                        category = if ("403" in message) "PLAYBACK_403" else "NETWORK_PERF",
                        severity = "HIGH",
                        stackTrace = diagnostic,
                        deviceContext = "Android ${Build.VERSION.RELEASE}; ${Build.MANUFACTURER} ${Build.MODEL}",
                        createdAt = now,
                        updatedAt = now,
                    ))
                }
            }
        }
        // provide custom configuration
        val workConfig =
            Configuration
                .Builder()
                .setMinimumLoggingLevel(Log.INFO)
                .build()

        // initialize WorkManager
        WorkManager.initialize(this, workConfig)

        // Initialize and start AutoBackupScheduler
        autoBackupScheduler = AutoBackupScheduler(this, dataStoreManager)
        applicationScope.launch {
            autoBackupScheduler.observeAndSchedule()
        }

        val librarySyncScheduler = LibrarySyncScheduler(this, get<LibrarySyncPhoneRepository>())
        applicationScope.launch {
            librarySyncScheduler.observeAndSchedule()
        }

        CaocConfig.Builder
            .create()
            .backgroundMode(CaocConfig.BACKGROUND_MODE_SILENT) // default: CaocConfig.BACKGROUND_MODE_SHOW_CUSTOM
            .enabled(true) // default: true
            .showErrorDetails(true) // default: true
            .showRestartButton(true) // default: true
            .errorDrawable(R.mipmap.ic_launcher_round)
            .logErrorOnRestart(false) // default: true
            .trackActivities(true) // default: false
            .minTimeBetweenCrashesMs(2000) // default: 3000 //default: bug image
            .restartActivity(MainActivity::class.java) // default: null (your app's launch activity)
            .apply()
        // Wrap the crash SDK's handler so its restart/report flow remains intact.
        Thread.setDefaultUncaughtExceptionHandler(UnxCrashHandler(this))

        @SuppressLint("DiscouragedPrivateApi")
        val field: Field = CursorWindow::class.java.getDeclaredField("sCursorWindowSize")
        field.isAccessible = true
        val expectSize = 100 * 1024 * 1024
        field.set(null, expectSize)

        AppContext.apply {
            set(applicationContext)
        }
    }

    override fun onTerminate() {
        super.onTerminate()

        Logger.w("Terminate", "Checking")
    }

    @OptIn(ExperimentalCoilApi::class)
    override fun newImageLoader(context: PlatformContext): ImageLoader {
        lateinit var imageLoader: ImageLoader
        imageLoader =
            ImageLoader
                .Builder(context)
                .components {
                    add(NetworkFirstInterceptor { imageLoader })
                    add(
                        OkHttpNetworkFetcherFactory(
                            callFactory = {
                                OkHttpClient()
                            },
                        ),
                    )
                }.diskCachePolicy(CachePolicy.ENABLED)
                .networkCachePolicy(CachePolicy.ENABLED)
                .diskCache(
                    DiskCache
                        .Builder()
                        .directory(FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "image_cache")
                        .maxSizeBytes(512L * 1024 * 1024)
                        .build(),
                ).crossfade(true)
                // CrossfadePainter otherwise sizes itself as max(placeholder, image) once and keeps it,
                // so a square holder over a wide banner leaves the banner zoomed after the fade.
                .preferEndFirstIntrinsicSize(true)
                .build()
        return imageLoader
    }
}
