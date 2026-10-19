package com.kuaigeglobal.epnova.core.network.apis.request

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * {
 *     "workNovelUrl": "小说地址",
 *     "novelCharCount": "小说字符数"
 * }
 */
@Parcelize
data class UploadStoryBody(
    val novelCharCount: Int? = null,
    val novelFileUrl: String? = null,
    val novelFileName: String? = null,//小说文件名
    val novelFileType: String? = null,//小说文件类型
    val workTitle: String? = null,
    val totalEpisodeCntCode: String? = null,
    val totalEpisodeCnt: String? = null,
    val workRatio: String? = null,
    val workStyleCode: String? = null,
) : Parcelable

/**
 *  {
 *         "workId": "作品ID",
 *         "genStatus": "[枚举]生成状态 [0=待生成 1=生成中 2=成功 3=失败]"
 *     }
 */
data class WorkTaskData(
    val genStatus: Int? = null,
    val workId: String? = null
)

/**
 * {
 *         "workId": "作品ID",
 *         "genStatus": "[枚举]生成状态 [0=待生成 1=生成中 2=成功 3=失败 4=算力不足，请充值 5=审核失败  6=无算力-生成草稿]
 *         "result": "生成结果（作品梗概内容）"
 *     }
 */
@Parcelize
data class PollSummaryData(
    val genStatus: Int? = null,
    val toastTextProgress: String? = null,//用户提示-进度文本;例如正在读取小说内容
    val toastTextTime: String? = null,//用户提示-剩余时间文本;例如12-15分钟
    val toastTextEstimatedSeconds: Int? = null,//用户提示-预估总秒数;例如30秒
    val novelCharCount: Int? = null,//小说字符数
    val novelFileName: String? = null,//小说文件名
    val toastTextAuditFailed: String? = null,//用户提示-审核失败;发现23处敏感词
    val result: RollNovelSummaryResult? = null,
    val workId: String? = null
) : Parcelable {
    fun isSuccess(): Boolean {
        return genStatus == 2
    }

    fun isFail(): Boolean {
        return genStatus == 3
    }

    fun isSensitive(): Boolean {
        return genStatus == 5
    }

    /**
     * 无算力，弹出充值弹窗
     */
    fun isNoPower(): Boolean {
        return genStatus == 6
    }
}

@Parcelize
data class RollNovelSummaryResult(
    var workDesc: String? = null,
    val workTitle: String? = null,
    val structureList: List<RollNovelStructure>? = null,
    // 故事简纲，按列表顺序展示
    var storyOutlineList: List<StoryOutlineInfo>? = null,
    val tags: List<String>? = null, // 只读展示的故事标签
) : Parcelable{
    fun getFirstStoryId(): String? {
        return structureList?.get(0)?.storyId
    }
}

@Parcelize
data class StoryOutlineInfo(
    val title: String? = null, // 简纲阶段标题
    var content: String? = null, // 对应阶段的故事内容
    val id: String? = null, // 对应阶段的故事内容
) : Parcelable

/**
 * {
 *             "workTitle": "作品名称",
 *             "workDesc": "作品简介",
 *             "structureList": [
 *                 {
 *                     "id": "主键ID",
 *                     "storyId": "剧集ID",
 *                     "storyTitle": "剧集标题",
 *                     "storySummary": "剧集梗概"
 *                 }
 *             ]
 *         }
 */
@Parcelize
data class RollNovelStructure(
    val id: String? = null,
    val storyId: String? = null,
    var storySummary: String? = null,
    val storyTitle: String? = null
) : Parcelable

data class PollScriptBody(
    val workId: String? = null,
    val storyId: String? = null,
    val workType: Int? = null//作品类型：1-灵感创作;2-小说改编;
)

data class PollScriptData(
    val workId: String? = null,
    val storyId: String? = null,
    val sortNo: Int? = null,//集数序号
    val genStatus: Int? = null,//[枚举]生成状态 [0=待生成 1=生成中 2=成功 3=失败 4=算力不足，请充值]
    val toastTextProgress: String? = null,//用户提示-进度文本;例如正在创建故事剧本
    val toastTextTime: String? = null,//用户提示-剩余时间文本;例如30-60秒
    val toastTextEstimatedSeconds: Int? = null,//用户提示-预估总秒数;例如30秒
    val storyContent: String? = null,
    //1.5.6版本加入
    var storyTilte : String? = null, //2026/10/16 新增，章节标题
    var storySummary : String? = null,  //2026/10/16新增。当前剧集最新剧情概览/剧本大纲，来自story_summary；无值返回空字符串。可编辑，区别于storyContent剧本正文。
    var storyNo : String? = null, //storyNo
){
    fun isSuccess(): Boolean {
        return genStatus == 2
    }

    fun isFail(): Boolean {
        return genStatus == 3
    }
}