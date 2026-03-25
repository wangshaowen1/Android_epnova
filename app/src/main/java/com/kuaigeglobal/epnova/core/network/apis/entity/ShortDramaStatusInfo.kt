package com.kuaigeglobal.epnova.core.network.apis.entity

/**
 * 查询剧本状态信息
 */
data class ShortDramaStatusInfo(
    /**
     * 短剧id，接受Long和String类型
     */
    val dramasId: String,
    /**
     * 剧集id，String类型
     */
    val dramasSeriesId: String?,
    /**
     * [枚举]作品状态 [0=预生成中(剧本角色和分镜在生成中) 1=画面生成中 2=画面全部生成失败 3=画面部分生成失败 4=待选择角色 5=画面生成成功(未合成短剧) 6=已合成短剧 7=预生成成功(剧本角色和分镜在生成成功)]
     */
    val workStatus: Int?,
    /**
     * 发布状态 0-未发布 1-已发布
     */
    val publishStatus: Int,
    /**
     * [枚举]上架状态 [0=未上架 1=已上架]
     */
    val launchStatus: Int,
)
