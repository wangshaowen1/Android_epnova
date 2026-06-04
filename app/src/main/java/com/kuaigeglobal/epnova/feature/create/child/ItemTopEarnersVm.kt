package com.kuaigeglobal.epnova.feature.create.child

import com.kuaigeglobal.epnova.base.item.AppItemViewModel
import com.kuaigeglobal.epnova.common.ext.toUnitText
import com.kuaigeglobal.epnova.core.network.apis.entity.CreatorWorkInfo

/**
 * TopEarners 列表 item VM。
 *
 * 列表展示保持作品维度，所以这里绑定 CreatorWorkInfo，而不是播放器用的 CreatorPlayableEpisode。
 */
class ItemTopEarnersVm(data: CreatorWorkInfo?) : AppItemViewModel<CreatorWorkInfo>(data) {

    /**作品封面。*/
    fun workCoverUrl(): String? {
        return data.get()?.workCoverUrl
    }

    /**作品标题。*/
    fun workTitle(): String? {
        return data.get()?.workTitle
    }

    /**格式化播放量。*/
    fun getFormatPlaycount(): String? {
        return data.get()?.playCount?.toUnitText()
    }

    /**TopEarners 累计收益文本，后端已处理格式。*/
    fun getTotalEarningsText(): String {
        return data.get()?.totalEarnings.orEmpty()
    }
}
