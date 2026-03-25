package com.kuaigeglobal.epnova.core.db.convert

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.kuaigeglobal.epnova.core.db.entity.AIMessageContent

class AIMessageContentTypeConverter {

    @TypeConverter
    fun stringToAiResponse(s: String?): AIMessageContent? =
            Gson().fromJson(s, object : TypeToken<AIMessageContent>() {}.type)

    @TypeConverter
    fun listToString(response: AIMessageContent?): String? = Gson().toJson(response)

}






