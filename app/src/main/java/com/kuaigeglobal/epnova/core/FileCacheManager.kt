package com.kuaigeglobal.epnova.core

import com.kuaige.videoedit.resource.Resource
import com.kuaige.videoedit.resource.UrlResource
import com.kuaigeglobal.epnova.core.network.RetrofitClient
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

/**
 * 文件缓存管理器
 * 优化：移除 Listener，使用 GlobalScope 执行下载任务（Fire-and-forget）
 */
object FileCacheManager {
    private val cache by lazy { mutableMapOf<String, Resource>() }

    /**
     * 获取缓存 Resource 对象
     */
    fun getCache(url: String): Resource = cache.getOrPut(url) { UrlResource(url, RetrofitClient.httpClient) }

    /**
     * 预加载/缓存文件
     */
    @OptIn(DelicateCoroutinesApi::class)
    fun cache(url: String) {
        // 作为全局单例缓存工具，使用 GlobalScope 是可以接受的，或者使用自定义的 Application Scope
        GlobalScope.launch {
            try {
                getCache(url).download()
            } catch (e: Exception) {
                // 预加载失败通常忽略，或者记录日志
                e.printStackTrace()
            }
        }
    }
}