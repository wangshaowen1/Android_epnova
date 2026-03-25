package com.kuaigeglobal.epnova.core.db.entity

import kotlinx.serialization.Serializable

@Serializable
data class SSEMessage(val code: Int?, val content: String?) {

    fun isError(): Boolean {
        return code == TYPE_ERROR
    }

    fun isContent(): Boolean {
        return code == TYPE_CONTENT
    }

    fun isOver(): Boolean {
        return code == TYPE_FINISH
    }

    companion object {
        //内容编码1-正常信息 2-报错信息
        const val TYPE_CONTENT = 1
        const val TYPE_ERROR = 2
        const val TYPE_FINISH = -1


        const val TYPE_REASON = 1001
        const val TYPE_MSG_JSON = 1000
        const val TYPE_INVALID = 1002//终端自己定义的状态码  歌词无效
    }
}