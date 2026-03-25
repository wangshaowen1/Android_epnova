package com.kuaigeglobal.epnova.core.network.apis.request

data class SceneCostBody(
    val sceneType: Int?,//场景类型:1=绘本生成 2=音色复刻 3=绘本图片重绘 4=生成绘本配音
    val bizData: String? = null,//业务数据，可不传，绘本生成时传故事内容，其他传id
    val pictureBookTemplateId: String? = null,//能力包内的模板id，可不传 能力包的绘本生成时必传
    val timbreType: Int? = null//音色类型 1-系统 2-用户音色
) : UserTokenBody() {
    companion object {
        const val SCENE_TYPE_GENERATE_PICTURE_BOOK = 1
        const val SCENE_TYPE_VOICE_CLONE = 2
        const val SCENE_TYPE_PICTURE_BOOK_IMAGE_REGENERATE = 3
        const val SCENE_TYPE_PICTURE_BOOK_VOICE_GENERATE = 4
    }
}