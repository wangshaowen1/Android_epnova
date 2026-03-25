package com.kuaigeglobal.epnova.core.network.apis.response

data class PictureBookGenerateStatusData(
    /**
     * [枚举]绘本生成(合成)状态 [0=待生成 1=生成中 2=生成成功 3=生成失败]
     */
    val pictureBookGenStatus: Int? = null,
    //绘本id
    val pictureBookId: String? = null
)