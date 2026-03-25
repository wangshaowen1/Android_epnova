package com.kuaigeglobal.epnova.core.db.convert

import androidx.room.TypeConverter
import com.kuaigeglobal.epnova.core.network.apis.entity.TemplateQuestion
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class QuestionListTypeConverter {

    @TypeConverter
    fun stringToList(s: String?): List<TemplateQuestion>? =
            Gson().fromJson(s, object : TypeToken<List<TemplateQuestion>>() {}.type)

    @TypeConverter
    fun listToString(list: List<TemplateQuestion>?): String? = Gson().toJson(list)

}






