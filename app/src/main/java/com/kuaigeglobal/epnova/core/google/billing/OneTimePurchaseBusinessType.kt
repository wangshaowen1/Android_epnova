package com.kuaigeglobal.epnova.core.google.billing

import androidx.annotation.Keep

/** 一次性商品所属业务。恢复时必须依赖该值选择原购买使用的验单接口。 */
@Keep
enum class OneTimePurchaseBusinessType {
    COINS,
    CREDITS,
}
