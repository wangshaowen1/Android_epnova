package com.kuaigeglobal.epnova.core.network.apis.request

/** 
 * @createtime 2026年08月20日 15:48
 * @author TooWhiteT
 */

data class InsufficientGuideBody(
    /**
     * 1 故事梗概
     * 2 生图
     * 3 生视频
     */
    val creationScene: Int
)
