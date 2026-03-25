package com.kuaigeglobal.epnova.core.network.apis.request

data class MessagesTypeBody(
    /**
     * 消息类型1=追剧消息2=喜欢消息3=系统消息
     */
    val messagesType: Int?
) : UserTokenBody() {


    companion object {
        /**
         * 追剧(短剧剧集)消息
         */
        const val TYPE_WATCHING = 1

        /**
         * 喜欢消息
         */
        const val TYPE_LIKED = 2

        /**
         * 系统消息
         */
        const val TYPE_SYSTEM_NOTICE = 3
    }

}