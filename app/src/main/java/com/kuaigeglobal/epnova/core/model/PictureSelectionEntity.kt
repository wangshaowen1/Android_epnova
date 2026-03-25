package com.kuaigeglobal.epnova.core.model

/**
 * *****************************************
 * 项目名称: KidsTales
 * @Author: SZYMN
 * @Introduce:选择图片
 * @Data: 2024/7/19 星期五 20:05
 * @Version: 1.0
 * *****************************************
 */
data class PictureSelectionEntity(
    //图片地址
    val path: String? = null,
    //是否是图片 true:图片 false:选择图片
    val isImage: Boolean = true,
    //说明
    var description: String? = null,
)