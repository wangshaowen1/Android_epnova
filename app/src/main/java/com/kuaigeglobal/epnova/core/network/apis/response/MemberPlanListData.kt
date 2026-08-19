package com.kuaigeglobal.epnova.core.network.apis.response

import com.kuaigeglobal.epnova.R
import com.nhsd.basic.ext.getResString

/**
 * 会员套餐页配置。
 */
data class MemberPlanListData(
    /** 页面模式：PURCHASE、SWITCH。 */
    val pageMode: String? = null,
    /** 当前平台套餐列表，服务端返回顺序即展示顺序。 */
    val plans: List<MemberPlanInfo>? = null,
)

/**
 * 会员订阅套餐。
 */
data class MemberPlanInfo(
    /** 不可变套餐版本编码。 */
    val planVersionCode: String? = null,
    /** 当前语言的套餐展示名称。 */
    val planName: String? = null,
    /** 计费周期单位：WEEK、MONTH。 */
    val periodUnit: String? = null,
    /** 每个成功计费周期实际发放的积分数量。 */
    val periodPointNum: Long? = null,
    /** Google Subscription Product ID。 */
    val productId: String? = null,
    /** Google Base Plan ID。 */
    val basePlanId: String? = null,
    /** Google 优惠标识，无独立优惠时为空。 */
    val offerId: String? = null,
    /** 套餐展示顺序。 */
    val displayOrder: Int? = null,
    /** 是否为默认选中套餐：0-否，1-是。 */
    val defaultSelected: Int? = null,
    /** 优惠角标百分比。 */
    val discountPercent: Int? = null,
    /** 首期优惠说明模板。 */
    val introBottomTextTemplate: String? = null,
    /** 标准订阅说明模板。 */
    val standardBottomTextTemplate: String? = null,
    /** 下周期套餐切换说明模板。 */
    val switchBottomTextTemplate: String? = null,
    /** 有序权益展示项。 */
    val benefitItems: List<MemberBenefitItemInfo>? = null,
    /** 当前用户可执行动作。 */
    val action: String? = null,
) {

    fun hasCurrent(): Boolean = action != null && action == MemberActionEnum.CURRENT.name

    fun planNameStr(): String = if (hasCurrent()) { R.string.member_your_current_plan.getResString() } else { (planName ?: "") }

    fun currentInfo(): String = R.string.member_current_unit.getResString((periodPointNum ?: 0), (periodUnit?.lowercase() ?: "unit"))
}

/**
 * 套餐权益项。
 */
data class MemberBenefitItemInfo(
    /** 展示类型：TEXT、POINT_BONUS。 */
    val itemType: String? = null,
    /** 普通权益文案。 */
    val text: String? = null,
    /** 积分数量前文案。 */
    val textBefore: String? = null,
    /** 营销展示积分数量。 */
    val pointNum: Long? = null,
    /** 积分数量后文案。 */
    val textAfter: String? = null,
)

enum class MemberBenefitItemType {
    TEXT,
    POINT_BONUS,
}

enum class MemberPlanMode {
    PURCHASE,
    SWITCH
}
