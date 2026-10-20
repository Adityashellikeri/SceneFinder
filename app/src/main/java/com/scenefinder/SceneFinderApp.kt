package com.scenefinder

import android.app.Application
import android.content.ComponentCallbacks2
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.scenefinder.data.repository.MediaEnrichmentRepository
import com.scenefinder.service.SessionNotificationManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class SceneFinderApp : Application(), ImageLoaderFactory {

    @Inject
    lateinit var notificationManager: SessionNotificationManager

    @Inject
    lateinit var enrichmentRepository: MediaEnrichmentRepository

    private var cachedImageLoader: ImageLoader? = null

    override fun onCreate() {
        super.onCreate()
        notificationManager.start()
    }

    override fun newImageLoader(): ImageLoader {
        return cachedImageLoader ?: ImageLoader.Builder(this)
            .dispatcher(kotlinx.coroutines.Dispatchers.IO.limitedParallelism(6))
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.22)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(200L * 1024 * 1024)
                    .build()
            }
            .crossfade(140)
            .allowHardware(true)
            .allowRgb565(true)
            .respectCacheHeaders(false)
            .build().also {
                cachedImageLoader = it
            }
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        try {
            when {
                level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL ||
                level >= ComponentCallbacks2.TRIM_MEMORY_COMPLETE ||
                level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW -> {
                    cachedImageLoader?.memoryCache?.clear()
                    enrichmentRepository.clearMemoryCache()
                }
                level == ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN -> {
                    cachedImageLoader?.memoryCache?.clear()
                    enrichmentRepository.trimMemoryCache(30)
                }
                level >= ComponentCallbacks2.TRIM_MEMORY_BACKGROUND -> {
                    cachedImageLoader?.memoryCache?.clear()
                    enrichmentRepository.clearMemoryCache()
                }
            }
        } catch (_: Exception) {}
    }

    override fun onLowMemory() {
        super.onLowMemory()
        try {
            cachedImageLoader?.memoryCache?.clear()
            enrichmentRepository.clearMemoryCache()
        } catch (_: Exception) {}
    }
}
