package com.kuaigeglobal.epnova.core.network.apis.response

/**
 * {
 *         "workId": "作品 ID",
 *         "workTitle": "作品标题",
 *         "workCoverUrl": "作品封面图 URL",
 *         "playCount": "展示用作品播放量（目录头部「播放量」文案）",
 *         "totalEpisodeCnt": "作品总集数",
 *         "segmentTabs": [
 *             {
 *                 "segmentIndex": "段序号，从 1 开始，与请求参数 segmentIndex 对应",
 *                 "segmentLabel": "分段展示文案，如「1-30」「31-60」",
 *                 "startSortNo": "该段起始集序号（sort_no 下界，含）",
 *                 "endSortNo": "该段结束集序号（sort_no 上界，含）"
 *             }
 *         ]
 *     }
 */
data class EpisodeDirectoryData(
    val workId: String? = null,
    val workTitle: String? = null,
    val workCoverUrl: String? = null,
    val playCount: Int? = null,
    val segmentTabs: List<SegmentTab>? = null,
    val totalEpisodeCnt: Int? = null,
)

data class SegmentTab(
    val segmentIndex: Int? = null,
    val segmentLabel: String? = null,
    val startSortNo: Int? = null,
    val endSortNo: Int? = null,
)