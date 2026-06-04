package com.kuaigeglobal.epnova.core.db.convert

import androidx.room.TypeConverter
import com.google.common.reflect.TypeToken
import com.google.gson.Gson
import com.kuaigeglobal.epnova.core.network.apis.entity.CreatorEpisodeInfo
import com.kuaigeglobal.epnova.core.network.apis.entity.CreatorPlatformInfo

/**
 * Creator 作品缓存专用转换器。
 *
 * 与旧 WorkInfo/EpisodeInfo 转换器分开，避免新旧模型的泛型 List 转换在 Room 中互相抢占。
 */
class CreatorConverters {

    @TypeConverter
    fun fromStringList(value: List<String>?): String {
        return Gson().toJson(value)
    }

    @TypeConverter
    fun toStringList(value: String?): List<String> {
        val listType = object : TypeToken<List<String>>() {}.type
        return Gson().fromJson(value, listType) ?: emptyList()
    }

    @TypeConverter
    fun fromCreatorEpisodeInfoList(value: List<CreatorEpisodeInfo>?): String? {
        return value?.let { Gson().toJson(it) }
    }

    @TypeConverter
    fun toCreatorEpisodeInfoList(value: String?): List<CreatorEpisodeInfo>? {
        return value?.let {
            val listType = object : TypeToken<List<CreatorEpisodeInfo>>() {}.type
            Gson().fromJson(value, listType)
        }
    }

    @TypeConverter
    fun fromCreatorPlatformInfoList(value: List<CreatorPlatformInfo>?): String? {
        return value?.let { Gson().toJson(it) }
    }

    @TypeConverter
    fun toCreatorPlatformInfoList(value: String?): List<CreatorPlatformInfo>? {
        return value?.let {
            val listType = object : TypeToken<List<CreatorPlatformInfo>>() {}.type
            Gson().fromJson(value, listType)
        }
    }
}
