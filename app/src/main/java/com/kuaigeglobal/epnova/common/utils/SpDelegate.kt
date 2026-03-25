package com.kuaigeglobal.epnova.common.utils

import android.text.TextUtils
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.kuaigeglobal.epnova.common.SP
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * 基础类型 SP 委托
 * 修复：支持 default 为 null 的情况（默认为 String 类型）
 */
class SpDelegate<T>(
    private val key: String,
    private val default: T,
    private val spProvider: () -> SP
) : ReadWriteProperty<Any, T> {

    @Suppress("UNCHECKED_CAST")
    override fun getValue(thisRef: Any, property: KProperty<*>): T {
        val sp = spProvider()

        // 1. 处理 default 为 null 的情况，通常对应 String? 类型
        if (default == null) {
            return sp.getString(key, null) as T
        }

        // 2. 根据 default 的实际类型调用对应方法
        return when (default) {
            is String -> sp.getString(key, default)
            is Int -> sp.getInt(key, default)
            is Boolean -> sp.getBoolean(key, default)
            is Float -> sp.getFloat(key, default)
            is Long -> sp.getLong(key, default)
            else -> throw IllegalArgumentException("Unsupported type: ${default!!::class.java.simpleName}")
        } as T
    }

    override fun setValue(thisRef: Any, property: KProperty<*>, value: T) {
        val sp = spProvider()

        // 1. 处理 value 为 null 的情况
        if (value == null) {
            // SP 中通常只有 String 支持存 null (或移除)，这里统一调用 putString(null)
            // 具体的 SP 实现应该处理 null 值（通常是 remove）
            sp.putString(key, null)
            return
        }

        // 2. 根据 value 的实际类型调用对应方法
        when (value) {
            is String -> sp.putString(key, value)
            is Int -> sp.putInt(key, value)
            is Boolean -> sp.putBoolean(key, value)
            is Float -> sp.putFloat(key, value)
            is Long -> sp.putLong(key, value)
            else -> throw IllegalArgumentException("Unsupported type: ${value!!::class.java.simpleName}")
        }
    }
}

/**
 * 对象类型 SP 委托 (自动 Gson 序列化)
 */
class SpObjectDelegate<T>(
    private val key: String,
    private val clazz: Class<T>,
    private val spProvider: () -> SP
) : ReadWriteProperty<Any, T?> {

    override fun getValue(thisRef: Any, property: KProperty<*>): T? {
        val json = spProvider().getString(key, "")
        if (TextUtils.isEmpty(json)) return null
        return try {
            Gson().fromJson(json, clazz)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override fun setValue(thisRef: Any, property: KProperty<*>, value: T?) {
        val sp = spProvider()
        if (value == null) {
            sp.putString(key, "")
        } else {
            try {
                sp.putString(key, Gson().toJson(value))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

/**
 * List 类型 SP 委托 (自动 Gson 序列化)
 */
class SpListDelegate<T>(
    private val key: String,
    private val spProvider: () -> SP
) : ReadWriteProperty<Any, List<T>> {

    override fun getValue(thisRef: Any, property: KProperty<*>): List<T> {
        val json = spProvider().getString(key, "")
        if (TextUtils.isEmpty(json)) return emptyList()
        return try {
            val type = object : TypeToken<List<T>>() {}.type
            Gson().fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    override fun setValue(thisRef: Any, property: KProperty<*>, value: List<T>) {
        try {
            spProvider().putString(key, Gson().toJson(value))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}