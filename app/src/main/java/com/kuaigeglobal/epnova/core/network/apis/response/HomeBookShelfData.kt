package com.kuaigeglobal.epnova.core.network.apis.response

import com.kuaigeglobal.epnova.core.network.apis.entity.WorkVOInfo


/**
 *  {
 *                 "bookshelfId": "书架配置主键，对应 b_app_bookshelf_config.id",
 *                 "bookshelfName": "书架区块标题（如「上新」「最热」等运营配置名称）",
 *                 "bookshelfType": "书架类型：1=横滑书架，2=瀑布流；与配置表 bookshelf_type 一致",
 *                 "sortNo": "区块排序号，数值越小越靠前（与配置表 sort_no 一致）",
 *                 "horizontalWorks": [
 *                     {
 *                         "workId": "作品 ID",
 *                         "workTitle": "作品标题（展示用文案）",
 *                         "workCoverUrl": "作品封面图 URL",
 *                         "playCount": "展示用播放量（虚拟量与真实量合并或择优策略由服务端实现决定）",
 *                         "totalEpisodeCnt": "总集数",
 *                         "workRatio": "视频画幅比例，如 16:9（横屏）、9:16（竖屏）"
 *                     }
 *                 ]
 *             }
 *         ]
 *     }
 */
data class HomeBookShelfData(
    val sections: List<Section>? = null
)

data class Section(
    val bookshelfId: String? = null,
    val bookshelfName: String? = null,
    val bookshelfType: Int? = null,
    val horizontalWorks: List<WorkVOInfo>? = null,
    val sortNo: Int? = null
) {
    fun isHorizontal(): Boolean {
        return bookshelfType == 1
    }
}