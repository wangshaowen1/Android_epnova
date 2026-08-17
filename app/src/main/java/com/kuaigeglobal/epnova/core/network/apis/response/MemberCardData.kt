package com.kuaigeglobal.epnova.core.network.apis.response

import com.kuaigeglobal.epnova.R
import com.nhsd.basic.ext.getResString

/**
 * 会员中心卡片信息。
 */
data class MemberCardData(
    /** 是否展示会员中心卡片。 */
    val display: Boolean? = null,
    /** 卡片场景：NON_MEMBER-非会员，MEMBER-会员。 */
    val cardScene: String? = null,
    /** 当前场景对应的卡片标题。 */
    val cardTitle: String? = null,
    /** 当前场景对应的卡片说明。 */
    val cardDescription: String? = null,
    /** 当前用户是否享有会员身份。 */
    val isMember: Boolean? = null,
    /** 会员状态：NONE、ACTIVE、GRACE_PERIOD、ON_HOLD、EXPIRED、REVOKED。 */
    val membershipStatus: String? = null,
    /** 当前订阅是否开启自动续费。 */
    val autoRenewEnabled: Boolean? = null,
    /** 当前生效套餐编码。 */
    val planCode: String? = null,
    /** 当前生效套餐展示名称。 */
    val planName: String? = null,
    /** 每个成功计费周期发放的会员积分数。 */
    val periodPointNum: Long? = null,
    /** 当前会员周期到期时间，毫秒时间戳。 */
    val expiresAt: Long? = null,
    /** 下周期待切换的套餐编码。 */
    val pendingPlanCode: String? = null,
    /** 待切换套餐预计生效时间，毫秒时间戳。 */
    val pendingEffectiveTime: Long? = null,
    /** 已按当前语言和请求时区渲染的状态文案。 */
    val statusText: String? = null,
    /** 点击动作：PURCHASE、SWITCH、NONE。 */
    val action: String? = null,
    /** 卡片是否允许点击。 */
    val clickable: Boolean? = null,
    /** 默认套餐优惠角标百分比。 */
    val discountPercent: Int? = null,
) {

    fun hasMember(): Boolean = isMember != null && isMember

    fun memberTitle(): String = cardTitle ?: R.string.member_creator_plan.getResString()

    fun memberDesc(): String = cardDescription ?: ""

    fun show(): Boolean = display != null && display
}
