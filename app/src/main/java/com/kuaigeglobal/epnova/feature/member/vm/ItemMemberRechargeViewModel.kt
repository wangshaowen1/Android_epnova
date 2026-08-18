package com.kuaigeglobal.epnova.feature.member.vm

import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.base.item.AppItemViewModel
import com.kuaigeglobal.epnova.core.network.apis.response.MemberPlanInfo
import com.nhsd.basic.ext.getResString

/**
 * 会员订阅档位
 *
 * @createtime 2026年08月17日 10:39
 * @author TooWhiteT
 */

class ItemMemberRechargeViewModel(data: MemberPlanInfo) : AppItemViewModel<MemberPlanInfo>(data) {

    val isSelected = ObservableBoolean(false)
    val showDiscount = ObservableBoolean(false)
    val priceText = ObservableField("")
    val originalPriceText = ObservableField("")

    fun discountText(): String {
        val percent = data.get()?.discountPercent ?: return ""
        return R.string.member_discount_percent.getResString(percent)
    }
}