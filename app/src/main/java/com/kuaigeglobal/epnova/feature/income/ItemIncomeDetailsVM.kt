package com.kuaigeglobal.epnova.feature.income

import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.base.item.AppItemViewModel
import com.kuaigeglobal.epnova.core.network.apis.entity.IncomeDetailInfo
import com.nhsd.basic.ext.getResString
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs

/**
 * 收益明细列表 item 展示模型。
 */
class ItemIncomeDetailsVM(
    data: IncomeDetailInfo?
) : AppItemViewModel<IncomeDetailInfo>(data) {

    /**
     * 剧集标题，接口为空时展示空字符串。
     */
    fun workTitleText(): String = data.get()?.workTitle.orEmpty()

    /**
     * 播放量展示文案，格式：X,XXX views。
     */
    fun playCountText(): String {
        val count = data.get()?.playCount ?: 0
        val countText = NumberFormat.getIntegerInstance(Locale.US).format(count)
        return String.format(R.string.income_views_format.getResString(), countText)
    }

    /**
     * 收益金额展示文案，按分转美元并保留两位小数；缺少金额时兜底使用后端文案。
     */
    fun incomeAmountText(): String {
        val item = data.get() ?: return "+\$0.00"
        item.incomeAmountCent?.let { cent ->
            val symbol = if (cent >= 0) "+" else "-"
            return String.format(Locale.US, "%s\$%.2f", symbol, abs(cent) / 100.0)
        }

        return item.incomeAmountText?.takeIf { it.isNotBlank() } ?: "+\$0.00"
    }
}
