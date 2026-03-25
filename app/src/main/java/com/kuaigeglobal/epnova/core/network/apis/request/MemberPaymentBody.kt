package com.kuaigeglobal.epnova.core.network.apis.request

/**
 * *****************************************
 * 项目名称: KidsTales
 * @Author: SZYMN
 * @Introduce:会员支付
 * @Data: 2025/9/15/周一  17:55
 * @Version: 1.0
 * *****************************************
 */
data class MemberPaymentBody(
    //会员配置id，接受Long和String类型
   val configId: String?,
): UserTokenBody()