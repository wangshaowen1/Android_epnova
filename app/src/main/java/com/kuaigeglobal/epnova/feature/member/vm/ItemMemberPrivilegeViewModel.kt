package com.kuaigeglobal.epnova.feature.member.vm

import com.drake.spannable.replaceSpan
import com.drake.spannable.span.CenterImageSpan
import com.kuaigeglobal.epnova.App
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.base.item.AppItemViewModel
import com.kuaigeglobal.epnova.core.network.apis.response.MemberBenefitItemInfo
import com.kuaigeglobal.epnova.core.network.apis.response.MemberBenefitItemType
import com.nhsd.basic.ext.dp

/** 会员套餐权益项。 */
class ItemMemberPrivilegeViewModel(
    data: MemberBenefitItemInfo,
) : AppItemViewModel<MemberBenefitItemInfo>(data) {

    companion object {
        private const val POWER = "[power]"
    }

    private val powerSpan by lazy {
        CenterImageSpan(App.instance, R.drawable.icon_power).apply {
            setDrawableSize(12.dp, 12.dp)
        }
    }

    fun content(): CharSequence {
        val info = data.get() ?: return ""
        if (info.itemType != MemberBenefitItemType.POINT_BONUS.name) {
            return info.text.orEmpty()
        }

        val parts = mutableListOf<String>()
        info.textBefore?.trim()?.takeIf { it.isNotEmpty() }?.let(parts::add)
        // 添加算力标志位
        info.pointNum?.let {
            parts.add(it.toString())
            parts.add(POWER)
        }
        info.textAfter?.trim()?.takeIf { it.isNotEmpty() }?.let(parts::add)
        val content = parts.joinToString(" ")
        // 替换标志位为算力图标
        return if (content.contains(POWER)) {
            content.replaceSpan(POWER) { powerSpan }
        } else {
            content
        }
    }
}
