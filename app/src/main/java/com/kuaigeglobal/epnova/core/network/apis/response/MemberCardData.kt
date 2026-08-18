package com.kuaigeglobal.epnova.core.network.apis.response

import com.kuaigeglobal.epnova.R
import com.nhsd.basic.ext.getResString

/**
 * 会员中心卡片信息。
 */
data class MemberCardData(
    /** 是否展示会员中心卡片。 */
    val display: Int? = null,
    /** 当前场景对应的卡片标题。 */
    val cardTitle: String? = null,
    /** 当前场景对应的卡片说明。 */
    val cardDescription: String? = null,
    /** 点击动作：PURCHASE、SWITCH、NONE。 */
    val action: String? = null,
    /** 默认套餐优惠角标百分比。 */
    val discountPercent: Int? = null,
) {

    fun hasMember(): Boolean = action != null && action != MemberActionEnum.PURCHASE.name

    fun memberTitle(): String = cardTitle ?: R.string.member_creator_plan.getResString()

    fun memberDesc(): String = cardDescription ?: ""

    fun show(): Boolean = display != null && display == 1

    fun hasDiscount(): Boolean = discountPercent != null && discountPercent != 0

    fun discountStr(): String = if (hasDiscount()) R.string.member_discount_percent.getResString(discountPercent) else ""

    fun hasEnable(): Boolean = action != null && action != MemberActionEnum.NONE.name

    fun btnStr(): String = if (action != null && action == MemberActionEnum.SWITCH.name)
            R.string.member_upgrade.getResString()
        else
            R.string.member_subscribe_now.getResString()

    /** 是否月卡 */
    fun hasMonthly(): Boolean = action != null && action == MemberActionEnum.UNAVAILABLE.name
}

enum class MemberStatusEnum(code: Int) {

    NONE(0),

    // 具有会员身
    ACTIVE(1),
    // 具有会员身份
    GRACE_PERIOD(3),

    ON_HOLD(4),

    EXPIRED(5),

    REVOKED(6)
}

enum class MemberActionEnum {
    /** */
    NONE,
    /** 新购 */
    PURCHASE,
    /** 预约下周期切换 */
    SWITCH,
    /** 当前套餐 */
    CURRENT,
    /** 已预约的目标套餐 */
    PENDING_SWITCH,
    /** 不可操作 */
    UNAVAILABLE,
}
