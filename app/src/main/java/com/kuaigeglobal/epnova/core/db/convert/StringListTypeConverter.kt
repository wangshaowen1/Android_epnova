package com.kuaigeglobal.epnova.core.db.convert

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class StringListTypeConverter {

    @TypeConverter
    fun stringToList(s: String?): List<String>? =
            Gson().fromJson(s, object : TypeToken<List<String>>() {}.type)

    @TypeConverter
    fun listToString(list: List<String>?): String? = Gson().toJson(list)

}






