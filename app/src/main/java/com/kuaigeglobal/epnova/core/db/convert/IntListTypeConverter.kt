package com.kuaigeglobal.epnova.core.db.convert

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class IntListTypeConverter {

    @TypeConverter
    fun stringToList(s: String?): List<Int>? =
            Gson().fromJson(s, object : TypeToken<List<Int>>() {}.type)

    @TypeConverter
    fun listToString(list: List<Int>?): String? = Gson().toJson(list)

}






