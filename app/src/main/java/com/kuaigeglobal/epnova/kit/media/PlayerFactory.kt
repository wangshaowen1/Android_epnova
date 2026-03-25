package com.kuaigeglobal.epnova.kit.media

import androidx.annotation.Keep
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.CacheDataSource.Factory
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.exoplayer.upstream.DefaultAllocator
import com.nhsd.basic.manager.FileManager
import com.kuaigeglobal.epnova.App
import java.io.File

@OptIn(UnstableApi::class)
@Keep
class PlayerFactory {


    companion object {
        //Minimum Video you want to buffer while Playing
        private const val MIN_BUFFER_DURATION = 1000// 最小缓冲时间，

        //Max Video you want to buffer during PlayBack
        private const val MAX_BUFFER_DURATION = 5000 // 最大缓冲时间

        //Min Video you want to buffer before start Playing it
        private const val MIN_PLAYBACK_START_BUFFER = 500// 最小播放缓冲时间，只有缓冲到达这个时间后才是可播放状态

        //Min video You want to buffer when user resumes video
        private const val MIN_PLAYBACK_RESUME_BUFFER = 1000// 当缓冲用完，再次缓冲的时间
        val loadControl
            get() = DefaultLoadControl.Builder()
            .setAllocator(DefaultAllocator(true, 16)) // 设置分配器
            .setBufferDurationsMs(
                MIN_BUFFER_DURATION, // 最小缓冲时间（毫秒）
                MAX_BUFFER_DURATION, // 最大缓冲时间（毫秒）
                MIN_PLAYBACK_START_BUFFER, // 为播放缓冲的时间（毫秒）
                MIN_PLAYBACK_RESUME_BUFFER  // 重新缓冲后为播放缓冲的时间（毫秒）
            )
            .setTargetBufferBytes(C.LENGTH_UNSET) // 可选：设置目标缓冲大小
            .setPrioritizeTimeOverSizeThresholds(true) // 可选：是否优先考虑时间而不是大小阈值
            .build()

        // 设置缓存目录
        val cacheDir by lazy { File(FileManager.getInstance().mediaFolder, "media3_cache") }

        // 创建缓存，指定缓存大小
        val simpleCache: Cache by lazy {
            SimpleCache(cacheDir, LeastRecentlyUsedCacheEvictor(4096 * 1024 * 1024L))
        }// 100 MB

        // 创建默认的数据源工厂
        val dataSourceFactory: DataSource.Factory by lazy { DefaultHttpDataSource.Factory() }

        // 创建带有缓存的缓存数据源工厂
        val cacheDataSourceFactory: Factory by lazy {
            Factory().apply {
                setCache(simpleCache)
                    .setUpstreamDataSourceFactory(dataSourceFactory)
                    .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
            }
        }

        val mediaSourceFactory: ProgressiveMediaSource.Factory by lazy {
            ProgressiveMediaSource.Factory(cacheDataSourceFactory)
        }
        /**
         * Creates an ExoPlayer instance with optional caching and automatic audio focus handling.
         *
         * @param withCache If true, the player will be configured to use a media source factory that supports caching.
         * @param autoAudioFocus If true, the player will automatically handle audio focus changes. This includes setting
         *                       appropriate audio attributes to allow audio interruptions from other apps.
         * @return An instance of ExoPlayer configured according to the provided parameters.
         */
        fun createPlayer(withCache: Boolean = true, autoAudioFocus:Boolean=false): ExoPlayer {
            return ExoPlayer.Builder(App.instance)
                //支持软解
                .setRenderersFactory(DefaultRenderersFactory(App.instance).setEnableDecoderFallback(true))

                .setLoadControl(loadControl).also {
                    if(autoAudioFocus){
                         it.setAudioAttributes(AudioAttributes.Builder()
                        .setUsage(C.USAGE_MEDIA)
                        .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                        .build(),true)
                    }else{
                        it.setAudioAttributes( AudioAttributes.Builder().build(),false)
                    }
                if (withCache) {
                    it.setMediaSourceFactory(mediaSourceFactory)
                }

            }.build()
        }
    }


}