package com.kuaigeglobal.epnova.core.network.apis.entity

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 *  {
 *                     "configCode": "配置 code剧集：ep_15=15集以内 ep_30=15~30集 ep_custom=自定义视频比例：ratio_16_9=16:9 ratio_9_16=9:16视频风格：style_cn_anime=国风动漫 style_kr_manhwa=韩漫风 style_realistic_anime=写实动漫 style_jp_anime=日漫风style_hand_drawn=手绘风 style_semi_real=二次元写实风 style_gu_feng=古风 style_anime=动漫风 style_us_modern=现代美式风",
 *                     "configValue": "配置值",
 *                     "extraConfig": "扩展配置（视频风格图片url）",
 *                     "sortNo": "排序号"
 *                 }
 */
@Parcelize
data class VideoConfigInfo(
    //配置名称
    val configName: String? = null,
    val configCode: String? = null,
    //配置值
    val configValue: String? = null,
    //扩展配置（视频风格图片url）
    val extraConfig: String? = null,
    //排序号
    val sortNo: Int? = null
) : Parcelable {
    fun isCustomInput(): Boolean {
        return configCode == MODE_CUSTOM
    }

    fun isSingleModel(): Boolean {
        return configCode == MODE_SINGLE
    }

    companion object {
        const val MODE_SINGLE = "RANGE_1"
        const val MODE_CUSTOM = "RANGE_CUSTOM"
    }
}

/**
 * "styleCode": "视频风格 Code",
 *                     "styleName": "风格名称",
 *                     "styleCoverUrl": "风格封面",
 *                     "stylePrompt": "风格提示词",
 *                     "sortNo": "排序号"
 */
@Parcelize
data class VideoStyleConfigInfo(
    val sortNo: Int? = null,
    val styleCode: String? = null,
    val styleCoverUrl: String? = null,
    val styleName: String? = null,
    val stylePrompt: String? = null
) : Parcelable

//配置类型(2-故事集数,3-视频比例,4-视频风格)
@Parcelize
data class VideoConfigData(
    val totalEpisodeCntItemVoList: List<VideoConfigInfo>?,
    val ratioItemVoList: List<VideoConfigInfo>?,
    val styleItemVOList: List<VideoStyleConfigInfo>?,
    val novelRecommendEpisodeGroupList: List<NovelRecommendEpisodeConfig>?
) : Parcelable

@Parcelize
data class NovelRecommendEpisodeConfig(
    val extraConfig: String? = null,//extra_config 值，标识字数区间，如：NOVEL_RANGE_10_12=10-12万字, NOVEL_RANGE_12_20=12-20万字, NOVEL_RANGE_20_30=20-30万字NOVEL_RANGE_30_40=30-40万字, NOVEL_RANGE_40_50=40-50万字, NOVEL_RANGE_50_60=50-60万字
    val minCount: Int? = null,//区间最小字数（字），如 100000
    val maxCount: Int? = null,//区间最大字数（字），如 120000
    val itemList: List<VideoConfigInfo>?,//该区间下的推荐集数选项
) : Parcelable