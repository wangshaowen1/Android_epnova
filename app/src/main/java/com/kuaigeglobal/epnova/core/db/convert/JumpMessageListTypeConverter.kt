package com.kuaigeglobal.epnova.core.db.convert

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.kuaigeglobal.epnova.core.network.apis.entity.JumpMessageInfo

class JumpMessageListTypeConverter {

    @TypeConverter
    fun stringToList(s: String?): List<JumpMessageInfo>? =
            Gson().fromJson(s, object : TypeToken<List<JumpMessageInfo>>() {}.type)

    @TypeConverter
    fun listToString(list: List<JumpMessageInfo>?): String? = Gson().toJson(list)

}






