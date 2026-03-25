package com.kuaigeglobal.epnova.common.utils

import android.text.TextUtils
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.tencent.mmkv.MMKV
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * MMKV 基础类型委托
 */
class MmkvDelegate<T>(
    private val key: String,
    private val default: T,
    private val mmkvProvider: () -> MMKV = { MMKV.defaultMMKV() }
) : ReadWriteProperty<Any, T> {

    @Suppress("UNCHECKED_CAST")
    override fun getValue(thisRef: Any, property: KProperty<*>): T {
        val mmkv = mmkvProvider()
        return when (default) {
            is String -> mmkv.decodeString(key, default)
            is Int -> mmkv.decodeInt(key, default)
            is Boolean -> mmkv.decodeBool(key, default)
            is Float -> mmkv.decodeFloat(key, default)
            is Long -> mmkv.decodeLong(key, default)
            is Double -> mmkv.decodeDouble(key, default)
            null -> mmkv.decodeString(key, null)
            else -> throw IllegalArgumentException("Unsupported type")
        } as T
    }

    override fun setValue(thisRef: Any, property: KProperty<*>, value: T) {
        val mmkv = mmkvProvider()
        if (value == null) {
            mmkv.removeValueForKey(key)
            return
        }
        when (value) {
            is String -> mmkv.encode(key, value)
            is Int -> mmkv.encode(key, value)
            is Boolean -> mmkv.encode(key, value)
            is Float -> mmkv.encode(key, value)
            is Long -> mmkv.encode(key, value)
            is Double -> mmkv.encode(key, value)
            else -> throw IllegalArgumentException("Unsupported type")
        }
    }
}

/**
 * MMKV 对象类型委托 (Gson 序列化)
 */
class MmkvObjectDelegate<T>(
    private val key: String,
    private val clazz: Class<T>,
    private val mmkvProvider: () -> MMKV = { MMKV.defaultMMKV() }
) : ReadWriteProperty<Any, T?> {

    override fun getValue(thisRef: Any, property: KProperty<*>): T? {
        val json = mmkvProvider().decodeString(key, "")
        if (TextUtils.isEmpty(json)) return null
        return try {
            Gson().fromJson(json, clazz)
        } catch (e: Exception) {
            null
        }
    }

    override fun setValue(thisRef: Any, property: KProperty<*>, value: T?) {
        val mmkv = mmkvProvider()
        if (value == null) {
            mmkv.removeValueForKey(key)
        } else {
            mmkv.encode(key, Gson().toJson(value))
        }
    }
}

/**
 * MMKV List 类型委托 (Gson 序列化)
 * 需要传入 TypeToken 以解决泛型擦除问题
 */
class MmkvListDelegate<T>(
    private val key: String,
    private val typeToken: TypeToken<List<T>>,
    private val mmkvProvider: () -> MMKV = { MMKV.defaultMMKV() }
) : ReadWriteProperty<Any, List<T>> {

    override fun getValue(thisRef: Any, property: KProperty<*>): List<T> {
        val json = mmkvProvider().decodeString(key, "")
        if (TextUtils.isEmpty(json)) return emptyList()
        return try {
            Gson().fromJson(json, typeToken.type) ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    override fun setValue(thisRef: Any, property: KProperty<*>, value: List<T>) {
        try {
            mmkvProvider().encode(key, Gson().toJson(value))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}