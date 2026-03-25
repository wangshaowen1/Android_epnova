package com.kuaigeglobal.epnova.common

import com.google.gson.Gson
import java.lang.reflect.Type

/**
 * 缓存工具类 (Kotlin Object 版)
 * 逻辑说明：
 * - cache(): 仅写入内存缓存 (mCache)
 * - persistedCache(): 写入内存缓存 + SP 持久化
 * - getXxx(): 优先读内存，没有读 SP 并回填内存
 */
object Cache {
    // 内存缓存 (保持与 Java 版一致的 HashMap)
    private val mCache = HashMap<Any?, Any?>()

    // 延迟初始化 SP 和 Gson
    private val sp: SP by lazy {
        SP("CACHE_DATA")
    }

    private val gson: Gson by lazy { Gson() }

    /**
     * 兼容 Java 的静态获取方法
     * Java 调用: Cache.get().xxx()
     */
    @JvmStatic
    fun get(): Cache {
        return this
    }

    // =================================================================================
    // 基础方法：仅操作内存缓存
    // =================================================================================

    /**
     * 仅写入内存缓存，不持久化
     */
    fun cache(key: String?, value: Any?) {
        mCache[key] = value
    }

    // =================================================================================
    // 持久化方法：内存 + SP
    // =================================================================================

    fun persistedCache(key: String, value: String?) {
        cache(key, value)
        if (value != null) {
            sp.putString(key, value)
        } else {
            sp.remove(key)
        }
    }

    fun persistedCache(key: String, value: Int) {
        cache(key, value)
        sp.putInt(key, value)
    }

    fun persistedCache(key: String, value: Long) {
        cache(key, value)
        sp.putLong(key, value)
    }

    fun persistedCache(key: String, value: Boolean) {
        cache(key, value)
        sp.putBoolean(key, value)
    }

    /**
     * 缓存对象（序列化为 Json 存储）
     */
    fun persistedCache(key: String, value: Any?) {
        if (value == null) {
            remove(key)
            return
        }
        cache(key, value)
        sp.putString(key, gson.toJson(value))
    }

    // =================================================================================
    // 读取方法：内存 -> SP -> 回填内存
    // =================================================================================

    fun getInt(key: String): Int {
        val value = mCache[key]
        if (value is Int) {
            return value
        }
        // 内存中没有，从 SP 读取
        val spValue = sp.getInt(key, 0)
        // 回填到内存
        cache(key, spValue)
        return spValue
    }

    fun getLong(key: String): Long {
        val value = mCache[key]
        if (value is Long) {
            return value
        }
        val spValue = sp.getLong(key, 0L)
        cache(key, spValue)
        return spValue
    }

    fun getBoolean(key: String, defaultValue: Boolean = false): Boolean {
        val value = mCache[key]
        if (value is Boolean) {
            return value
        }
        val spValue = sp.getBoolean(key, defaultValue)
        cache(key, spValue)
        return spValue
    }

    fun getString(key: String?): String? {
        val value = mCache[key]
        if (value is String) {
            return value
        }
        val spValue = sp.getString(key, null)
        if (spValue != null) {
            cache(key, spValue)
        }
        return spValue
    }

    fun <T> getObject(key: String, type: Type): T? {
        // 1. 尝试从内存获取
        val value = mCache[key]
        if (value != null) {
            try {
                @Suppress("UNCHECKED_CAST")
                return value as T
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // 2. 尝试从 SP 获取 JSON 并反序列化
        val json = sp.getString(key, null)
        if (json != null) {
            try {
                val obj = gson.fromJson<T>(json, type)
                if (obj != null) {
                    // 3. 回填到内存缓存
                    cache(key, obj)
                }
                return obj
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return null
    }

    // =================================================================================
    // 移除与清理
    // =================================================================================

    /**
     * 移除指定 Key 的缓存（内存 + SP）
     */
    fun remove(key: String?) {
        mCache.remove(key)
        sp.remove(key)
    }

    /**
     * 清空所有缓存
     */
    fun clear() {
        mCache.clear()
        sp.clear()
    }
}