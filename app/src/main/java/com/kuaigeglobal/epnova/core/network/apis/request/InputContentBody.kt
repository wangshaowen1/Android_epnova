package com.kuaigeglobal.epnova.core.network.apis.request

data class InputContentBody(
    val genWay: Int? = null,//[枚举]生成方式 [1=灵感剧本 2=自定义剧本 3=续写故事 4=爆款制作同款]
    val inputContent: String? = null,
) : UserTokenBody()


data class PopularRecommendDramasIdBody(
    val popularRecommendDramasId: String? = null,//爆款推荐剧本ID
) : UserTokenBody()

/**
 * {
 *     "scriptContent": "剧本内容(流式输出的内容)",
 *     "genWay": "[枚举]生成方式 [1=灵感剧本 2=自定义剧本 3=续写故事 4=爆款制作同款]",
 *     "styleId": "短剧风格表id(b_short_dramas_style)",
 *     "videoRatio": "[枚举]视频比例 [1=9:16 2=16:9 3=3:4 4=4:3]",
 *     "userToken": "用户token，接受Long和String类型"
 * }
 */
data class DramaGenerateBody(
    val scriptContent: String? = null,
    val genWay: Int? = null,
    val styleId: String? = null,
    val videoRatio: Int? = null
) : UserTokenBody() {
    companion object {
        //[枚举]生成方式 [1=灵感剧本 2=自定义剧本 3=续写故事 4=爆款制作同款]
        const val GEN_WAY_INSPIRATION = 1
        const val GEN_WAY_CUSTOMIZED = 2
        const val GEN_WAY_CONTINUE_STORY = 3
        const val GEN_WAY_MAKE_SAME_STORY = 4
    }
}

/**
 * {
 *     "dramasId": "短剧id，接受Long和String类型",
 *     "sourceType": "[枚举]来源类型[1=剧集续写 2=爆款制作同款]",
 *     "sourceId": "来源id[来源类型=1时短剧剧集id 来源类型=2时爆款推荐剧本内容表id",
 *     "userToken": "用户token，接受Long和String类型"
 * }
 */
data class DramaGenerateSourceReportBody(
    val dramasId: String? = null,
    val sourceId: String? = null,
    val sourceType: Int? = null,
) : UserTokenBody()