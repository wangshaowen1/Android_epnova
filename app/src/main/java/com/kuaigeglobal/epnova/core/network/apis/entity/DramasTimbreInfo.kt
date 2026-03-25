package com.kuaigeglobal.epnova.core.network.apis.entity

data class DramasTimbreInfo(
    /**
     * 字幕音色id
     */
    val timbreId: String?,
    /**
     * 音色名称
     */
    val timbreName: String?,
    /**
     * 音色封面
     */
    val timbreCoverUrl: String?,
    /**
     * 音色样例音频URL
     */
    val modelAudioUrl: String?,
    /**
     * 音色价格，平台币（为0时是免费音色）
     */
    val timbrePrice: Int?,
    /**
     * 是否vip使用:0否,1是
     */
    val vipStatus: Int?,
    /**
     * 选中状态:0否,1是
     */
    val selectStatus: Int?,
    /**
     * 购买状态:0否,1是
     */
    val buyStatus: Int?,
    /**
     * 只有购买状态为1时，才有生成状态 0=待生成 1=生成中 2=生成成功 3=生成失败
     */
    val genStatus: Int?
)