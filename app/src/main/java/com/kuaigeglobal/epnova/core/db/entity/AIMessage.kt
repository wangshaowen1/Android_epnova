package com.kuaigeglobal.epnova.core.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.kuaigeglobal.epnova.core.db.convert.AIMessageContentTypeConverter
import com.kuaigeglobal.epnova.core.db.convert.StringListTypeConverter
import com.google.gson.annotations.Expose

@Entity(tableName = "AIMessage")
@TypeConverters(value = [StringListTypeConverter::class, AIMessageContentTypeConverter::class])
class AIMessage {

    companion object {

        const val INTERVAL: Int = 3 * 60 * 1000
        const val SOURCE_USER = 1
        const val SOURCE_SYSTEM = 2

        const val CONTENT_TYPE_PROMPT = 1
        const val CONTENT_TYPE_LYRIC = 2
        const val CONTENT_TYPE_MUSIC = 3
        const val CONTENT_TYPE_REMIX_CUSTOM = 4
        const val CONTENT_TYPE_REMIX = 5
    }

    @PrimaryKey
    var id: String = ""
    var ownerId: String? = null//数据库字段，多用户登录使用
    var chatSessionId: String? = null
    var questionId: String? = null//父消息id
    var createTime: Long? = null
    var messageUser: Int = 0//消息用户 (1=用户 2=系统)
    var messageStatus: Int = -1//消息状态  (0=待生成 1=生成中 2=生成成功 3=生成失败 4=发送失败)
    var lyricStatus: Int = 1//歌词状态 0 无效歌词  1有效歌词
    var messageTemplateType: Int =
        0//消息内容模板类型 [1=问题 2=回答 3=歌曲卡片 4=歌曲爆改歌名(musicTitles) 5=爆改创意歌曲(musicRemixTitleList)]
    var messageContent: AIMessageContent? = null//回答
    var chatStatus: Int = 0 //会话状态 1-正常 2-结束

    @Expose(serialize = false, deserialize = false)
    var showSendTime: Boolean = false

    fun isInvalidLyric(): Boolean {
        return lyricStatus == 0
    }

    fun isReply(): Boolean {
        return messageUser == SOURCE_SYSTEM
    }

    fun isOutMessage(): Boolean {
        return messageUser == SOURCE_USER
    }

    fun isMusic(): Boolean {
        return messageTemplateType == CONTENT_TYPE_MUSIC
    }

    fun isEnd(): Boolean {
        return chatStatus == 2
    }

    fun isResponding(): Boolean {
        return messageContent?.chatAnswerList?.any { it.status == LyricAnswer.STATUS_GENERATING || it.status == LyricAnswer.STATUS_LOADING } == true
    }

    fun isMusicGenerateFinish(): Boolean {
        return messageContent?.musics?.any { it.status == 1 || it.status == 2 } == true
    }

    fun getParentMusicId(): String? {
        val parentMusicId = messageContent?.musics?.let {
            var parentMusicId: String? = null
            if (it.isNotEmpty()) {
                parentMusicId = it[0].parentMusicId
            }
            parentMusicId
        }
        return parentMusicId
    }

    fun getAnswerId(): String? {
        val answerId = messageContent?.chatAnswerList?.let {
            var answerId: String? = null
            if (it.isNotEmpty()) {
                answerId = it[it.lastIndex].answerId
            }
            answerId
        }
        return answerId
    }

    fun getAnswerCount(): Int {
        return messageContent?.chatAnswerList?.size ?: 0
    }

    /**
     * 用户自己发的文本
     */
    fun getContent(): String? {
        val content = messageContent?.content
        return content
    }


    fun setShowSendTimeByPrevious(anchor: AIMessage?) {
        showSendTime = if (anchor == null) {
            true
        } else {
            (createTime ?: 0L) - (anchor.createTime ?: 0L) > INTERVAL
        }
    }

    fun setAnswerStatus(state: Int?) {
        messageContent?.chatAnswerList?.lastOrNull()?.status = state
    }

    fun addAnswerStatus(state: Int?) {
        if (messageContent == null) {
            messageContent = AIMessageContent()
        }
        if (messageContent!!.chatAnswerList == null) {
            messageContent?.chatAnswerList = arrayListOf<LyricAnswer>()
        }
        messageContent?.chatAnswerList?.add(LyricAnswer(status = state))
    }

    override fun toString(): String {
        return "AIMessage(id='$id', messageStatus=$messageStatus, messageContent=$messageContent)"
    }

    constructor()

    /**
     * 拉取ai回复时创建的本地loading空对象
     */
    constructor(questionId: String, answerId: String, state: Int) {
        this.questionId = questionId
        createTime = System.currentTimeMillis()
        messageTemplateType = CONTENT_TYPE_LYRIC
        messageContent = AIMessageContent(chatAnswerList = arrayListOf(LyricAnswer(answerId = answerId, status = state)))
    }
}

data class AIMessageContent(
    var status: Int = 0,//状态 客户端自己添加的字段，只用于本地处理
    var chatAnswerList: ArrayList<LyricAnswer>? = null,
    val musicTitles: List<String>? = null,//爆改推荐歌曲名
    val musicRemixTitleList: List<MusicTitle>? = null,//爆改创意歌曲
    val musics: ArrayList<SimpleTrack>? = null,//歌曲集合
    val title: String? = null,//标题
    val content: String? = null,//自己的文本消息
) {

    fun getMusicTitleCount(): Int {
        return musicRemixTitleList?.takeIf { it.isNotEmpty() }?.size
            ?: musicTitles?.size
            ?: 0
    }

    fun getMusicTitle(index: Int): String? {
        return musicRemixTitleList?.takeIf { it.isNotEmpty() }?.getOrNull(index)?.musicTitle
            ?: musicTitles?.getOrNull(index)
    }

    override fun toString(): String {
        return "AIMessageContent(chatAnswerList=$chatAnswerList, title=$title, content=$content)"
    }
}

data class MusicTitle(val id: String?, val musicTitle: String?)

/**
 * @param status 回答状态 0-生成中 1-生成成功 2-生成失败
 */
data class LyricAnswer(val answerId: String? = null, var reasoningContent: String? = null, var content: String? = null, var status: Int?) {
    companion object {
        const val STATUS_GENERATING = 0
        const val STATUS_FINISH = 1
        const val STATUS_ERROR = 2
        const val STATUS_LOADING = 3
        const val STATUS_LOADING_AGAIN = 4
    }

    override fun toString(): String {
        return "LyricAnswer(answerId=$answerId, reasoningContent=$reasoningContent, content=$content, status=$status)"
    }

}

//status 歌曲创作状态 0-创作中 1-创作成功完成 2-创作失败 3=初步生成完成 4=排队中
data class SimpleTrack(
    val trackId: String?,
    val parentMusicId: String?,
    val trackTitle: String?,
    val musicLyric: String?,
    val coverUrl: String?,
    val status: Int?
) {

    override fun toString(): String {
        return "SimpleTrack(trackId=$trackId, parentMusicId=$parentMusicId, trackTitle=$trackTitle, coverUrl=$coverUrl, status=$status)"
    }
}