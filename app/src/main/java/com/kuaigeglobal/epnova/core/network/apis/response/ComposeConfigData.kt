package com.kuaigeglobal.epnova.core.network.apis.response

import android.os.Parcelable
import com.nhsd.basic.utils.T
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.core.network.apis.response.ComposeConfigFunction.Companion.GEN_MUSIC_BASE
import com.kuaigeglobal.epnova.core.network.apis.response.ComposeConfigFunction.Companion.MUSIC_TAGS
import com.kuaigeglobal.epnova.core.User
import com.nhsd.basic.ext.showToast
import kotlinx.parcelize.Parcelize

/**
 * {
 *     "code": "返回状态码200成功",
 *     "msg": "返回描述信息",
 *     "data": {
 *         "items": [{
 *             "musicGenConfigId": "音乐生成配置表id，实际返回String类型",
 *             "musicCommodityInfo": {
 *                 "musicCommodityInfoId": "AI音乐商品配置信息表id，实际返回String类型",
 *                 "musicCommodityType": "商品类型 1-AI写歌_两首 2-AI写歌_单首",
 *                 "musicCommodityDiamondNum": "商品需要的钻石数量(单位:个)"
 *             },
 *             "aiApiPlatform": "ai-api平台 1=天工AI 2=duomi-suno-ai 3=open-ai 4=kuaige-open-ai",
 *             "openApiType": "open-ai的API的类型 open-ai（1-suno 2-多米 3-天工 4-aceData） kuaige-open-ai（10001-豆包）",
 *             "onceGenMusicNum": "单次生成歌曲数量",
 *             "functions": [{
 *                 "functionCode": "功能编码<br/>GEN_MUSIC_BASE<br/>PURE_MUSIC<br/>MUSIC_TAGS<br/>REFERENCES_MUSIC<br/>PRE_PLAY<br/>WAV<br/>VOCALS<br/>",
 *                 "functionName": "功能名称<br/>生成音乐基础功能<br/>纯音乐<br/>风格<br/>参考音乐<br/>试听<br/>wav格式<br/>人声音色<br/>",
 *                 "supportStatus": "支持状态 0-不支持 1-支持，默认支持",
 *                 "enableStatus": "启用状态 0-禁用 1-启用，默认启用"
 *             }]
 *         }]
 *     }
 * }
 */
@Parcelize
data class ComposeConfigData(
    val musicGenerateWay: Int? = null,//音乐生成方式 1=灵感写歌 2=自定义写歌 3=照片留声机活动(图片写歌) 4=场景写歌 5=出道神曲6-音乐心情-灵感模式 7-音乐心情-自定义模式 8-灵感写歌-新用户 9-视频换脸_送他歌 10-视频换脸_灵感写歌15-老用户_歌曲爆改16-老用户_灵感库100-圣诞专属藏头歌场景生成音乐101-年度总结活动102-25年新年活动_拜年红包103-25年新年活动_讨红包200-创意写歌
    val configs: List<ComposeConfigItem>?//音乐生成配置
) : Parcelable {

    /**
     *@Deprecated 这个逻辑由后台执行，逻辑暂时保留
     */
    fun getFirstSongDefaultConfig(): ComposeConfigItem? {
        // 定义模型优先级映射（1:大师 > 2:经典 > 3:新锐）
        val priorityOrder = listOf("1", "2", "3")
        return configs
            ?.filter { it.enableStatus == 1 } // 只保留启用的配置
            ?.sortedWith(compareBy<ComposeConfigItem> {
                // 第一优先级：默认选中状态（1优先）
                if (it.defaultStatus == 1) 0 else 1
            }.thenBy {
                // 第二优先级：模型类型优先级（1>2>3）
                priorityOrder.indexOf(it.musicGenConfigId).takeIf { index -> index != -1 }
                    ?: Int.MAX_VALUE
            })
            ?.firstOrNull() ?: getDefaultConfig()
    }

    fun getDefaultConfig(): ComposeConfigItem? {
        val lastConfig = getLastConfig()
        if (lastConfig != null && lastConfig.isAiEnable()) return lastConfig
        return configs?.find { item -> item.isDefaultAi() }
    }

    fun getLastConfig(): ComposeConfigItem? {
        return configs?.find { item -> item.isLastConfig() }
    }
}

@Parcelize
data class ComposeConfigItem(
    val aiApiPlatform: Int,
    val openApiModelName: String,//open-ai API的模型名称
    val openApiModelDesc: String,//open-ai API的模型描述
    val enableStatus: Int,//启用状态 0-禁用 1-启用，默认启用，至少要保留一个启用状态的配置
    val defaultStatus: Int,//默认选中状态 0-否 1-是，默认否，只能设置一个默认状态为是的配置
    val functions: List<ComposeConfigFunction>?,
    val musicCommodityInfos: List<MusicCommodityInfo>?,
    /**
     * 1=大师模型 2=经典模型 3=新锐模型
     */
    val musicGenConfigId: String,
    val tagsMultipleSelections: Int,//单个类型风格是否能多选 0-不能 1-能，默认能
    val onceGenMusicNum: Int,
    val wordsNumLimitMax: Int,//最大字数限制
    val wordsNumLimitMin: Int,//最小字数限制
    val openApiType: Int
) : Parcelable {

    companion object{
        const val AI_MASTER = "1"//SONO AI
    }

    fun getMusicCommodityInfo(): MusicCommodityInfo? {
        return musicCommodityInfos?.find { it.musicCommodityCurrencyUnit == 1 }
    }

    fun isAiEnable(): Boolean {
        return enableStatus == 1
    }

    fun isLastConfig(): Boolean {
        //todo 当前用户信息中没有lastComposeConfig字段，后面需要的时候需要改
        return true //User.get().lastComposeConfig == musicGenConfigId
    }

    fun isDefaultAi(): Boolean {
        return defaultStatus == 1
    }

    fun getCost(): Int {
        return getMusicCommodityInfo()?.musicCommodityDiamondNum ?: 0
    }

    private fun checkEnable(function: ComposeConfigFunction?): Boolean {
        return function == null || (function.supportStatus == 1 && function.enableStatus == 1)
    }

    private fun checkSupport(function: ComposeConfigFunction?): Boolean {
        return function == null || (function.supportStatus == 1)
    }

    /**
     * 生成音乐基础功能
     */
    fun isComposeEnable(withToast: Boolean = true): Boolean {
        val checkEnable = checkEnable(functions?.firstOrNull { it.functionCode == GEN_MUSIC_BASE })
        if (!checkEnable && withToast) {
            T.showShort(R.string.toast_service_updating)
        }
        return checkEnable
    }


    /**
     * 风格
     */
    fun isMusicTagsSupport(): Boolean {
        return checkSupport(functions?.firstOrNull { it.functionCode == MUSIC_TAGS })
    }



    /**
     * 风格每个分类下能选多个(目前只有豆包ai 10001单选)
     */
    fun isMusicTagsMultipleChoose(): Boolean {
        return tagsMultipleSelections == 1
    }
}

/**
 * 功能列表
 */
@Parcelize
data class ComposeConfigFunction(
    val enableStatus: Int,//控制功能使用，不可用时点击弹toast
    val functionCode: String,
    val functionName: String,
    val supportStatus: Int//控制功能显示，暂时不用处理
) : Parcelable {
    companion object {
        //功能编码<br/>GEN_MUSIC_BASE<br/>PURE_MUSIC<br/>MUSIC_TAGS<br/>REFERENCES_MUSIC<br/>PRE_PLAY<br/>WAV<br/>VOCALS<br/>
        //功能名称<br/>生成音乐基础功能<br/>纯音乐<br/>风格<br/>参考音乐<br/>试听<br/>wav格式<br/>人声音色<br/>
        /**
         * 生成音乐基础功能
         */
        const val GEN_MUSIC_BASE = "GEN_MUSIC_BASE"

        /**
         * 纯音乐
         */
        const val PURE_MUSIC = "PURE_MUSIC"

        /**
         * 风格
         */
        const val MUSIC_TAGS = "MUSIC_TAGS"

        /**
         * 参考音乐
         */
        const val REFERENCES_MUSIC = "REFERENCES_MUSIC"

        /**
         * 试听
         */
        const val PRE_PLAY = "PRE_PLAY"

        /**
         * wav格式
         */
        const val WAV = "WAV"

        /**
         * 人声音色
         */
        const val VOCALS = "VOCALS"
    }
}

/**
 * AI音乐商品配置信息
 */
@Parcelize
data class MusicCommodityInfo(
    val musicCommodityDiamondNum: Int?=null,//商品需要的货币数量(单位:个/厘)
    val musicCommodityInfoId: Long?=null,//AI音乐商品配置信息表id，实际返回String类型
    val musicCommodityType: Int?=null,//商品类型 1-AI写歌_两首(写歌积分)
    val musicCommodityCurrencyUnit: Int//商品货币单位 1-写歌积分
) : Parcelable