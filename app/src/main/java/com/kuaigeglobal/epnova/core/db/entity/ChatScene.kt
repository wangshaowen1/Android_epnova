package com.kuaigeglobal.epnova.core.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.kuaigeglobal.epnova.core.network.apis.entity.JumpMessageInfo
import com.kuaigeglobal.epnova.core.network.apis.entity.TemplateQuestion
import com.kuaigeglobal.epnova.common.Constants
import com.kuaigeglobal.epnova.core.db.convert.JumpMessageListTypeConverter
import com.kuaigeglobal.epnova.core.db.convert.QuestionListTypeConverter

@Entity(tableName = "ChatScene")
@TypeConverters(value = [QuestionListTypeConverter::class, JumpMessageListTypeConverter::class])
class ChatScene {
    constructor(sceneId: String, avatar: String?, nickname:String?,welcomeMessage: String?,tipsMessages: List<TemplateQuestion>?=null,jumpMessages:List<JumpMessageInfo?>?=null) {
        this.sceneId = sceneId
        this.messagesSceneId=sceneId
        this.avatar = avatar
        this.nickname=nickname
        this.welcomeMessage = welcomeMessage
        this.tipsMessages=tipsMessages
        this.jumpMessages=jumpMessages
    }
    @PrimaryKey
    var sceneId: String = Constants.Scene.AI_REPLY
    var messagesSceneId:String
    var avatar: String? = null
    var nickname:String?=null
    var welcomeMessage: String? = null
    var tipsMessages:List<TemplateQuestion>?=null
    var jumpMessages:List<JumpMessageInfo?>?=null
}