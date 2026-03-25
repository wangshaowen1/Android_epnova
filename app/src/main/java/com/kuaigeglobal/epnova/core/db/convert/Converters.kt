package com.kuaigeglobal.epnova.core.db.convert

import androidx.room.TypeConverter
import com.google.common.reflect.TypeToken
import com.google.gson.Gson
import com.kuaigeglobal.epnova.core.network.apis.entity.EpisodeInfo

/**
 * *****************************************
 * 项目名称: KgQuan
 * @Author: SZYMN
 * @Introduce:
 * @Data: 2026/1/7/周三  11:23
 * @Version: 1.0
 * *****************************************
 */
class Converters {
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
    fun fromEpisodeInfoList(value: List<EpisodeInfo>?): String? {
        return value?.let {  Gson().toJson(it) }
    }

    @TypeConverter
    fun toEpisodeInfoList(value: String?): List<EpisodeInfo>? {
        return value?.let {
            val listType = object : TypeToken<List<EpisodeInfo>>() {}.type
            Gson().fromJson(value, listType)
        }
    }
}