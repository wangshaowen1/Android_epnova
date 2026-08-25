package com.kuaigeglobal.epnova.core.google.billing

import com.android.billingclient.api.Purchase

/**
 * Google Play 会员订阅的原始支付结果。
 *
 * 该类型只描述 Play Billing 已经明确返回的商店事实，不代表服务端会员权益已经生效：
 * - [Purchased] 后续仍必须携带购买凭证调用会员订阅 confirm；
 * - [Pending] 只能等待 Google 状态更新，不能 confirm、sync 或重复发起购买；
 * - [Uncertain] 必须先查询当前 SUBS，再根据查询结果决定 sync 或刷新服务端页面状态；
 * - 只有服务端 confirm/sync 成功后，业务层才能把整笔会员订阅流程视为完成。
 *
 * 会员订阅使用独立结果类型，目的是从类型层面阻止它进入普通积分的一次性商品
 * [PurchaseResult] 流程，避免误调用旧验单/消费接口。
 */
sealed class MemberBillingResult {

    /** Google 已返回 PURCHASED；这里仍不是“会员权益已到账”。 */
    data class Purchased(val purchase: Purchase) : MemberBillingResult()

    /** Google 已返回 PENDING；必须保留购买上下文并等待后续状态变化。 */
    data class Pending(val purchase: Purchase) : MemberBillingResult()

    /** 用户明确关闭或取消了 Google 支付页，本次商店交易未成交。 */
    object Cancelled : MemberBillingResult()

    /**
     * 商店结果无法证明交易成功或失败。
     *
     * 例如 ITEM_ALREADY_OWNED、网络/服务异常、回调为 ERROR，或者 OK 回调中没有找到本次
     * 商品。调用方不能直接重试购买，也不能直接释放 prepare，必须先查询当前 SUBS。
     */
    data class Uncertain(val responseCode: Int) : MemberBillingResult()

    /** 明确未成功进入商店交易流程的失败结果。 */
    sealed class Failed : MemberBillingResult() {

        /** 调用参数不完整或商品类型不是 SUBS，未调用 Google 支付。 */
        object InvalidArguments : Failed()

        /** 当前已有一次性购买或会员订阅流程未收敛，为避免回调串单而拒绝启动。 */
        object Busy : Failed()

        /** Google 明确拒绝启动，或返回了可判定为本次失败的响应码。 */
        data class Billing(val responseCode: Int) : Failed()
    }
}
