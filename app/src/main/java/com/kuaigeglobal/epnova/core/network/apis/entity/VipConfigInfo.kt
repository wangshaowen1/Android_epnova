package com.kuaigeglobal.epnova.core.network.apis.entity

/**
 * *****************************************
 * 项目名称: KidsTales
 * @Author: SZYMN
 * @Introduce:会员配置
 * @Data: 2025/4/23  11:13
 * @Version: 1.0
 * *****************************************
 */
data class VipConfigInfo(
    //会员配置列表
    val items: List<MembershipConfigurationInfo>?,
    //会员权益说明
    val vipPrivilegesArray: List<String>?,
) {
    data class MembershipConfigurationInfo(
        //会员配置ID
        val vipConfigId: String?,
        //会员名称
        val vipName: String?,
        //会员原价
        val saleVipAmount: Long?,
        //会员金额
        val vipAmount: Long?,
        //折扣 如：立减40
        val vipDesc: String?,
        //会员附加图标（右上角）
        val vipAdditionalIcon: String?,
        //默认选中状态 0-不选中 1-选中
        val defaultSelectStatus: Int?,
        //文本介绍，富文本 <span>共<span style="color:#F31D1D;font-size:1.3em;">24</span>个月，已优惠<span style="color:#F31D1D;font-size:1.3em;">1374</span>元</span>
        val vipRichTextDesc: String? = null,
        //是否突出0:不突出 1:突出
        val isZoom: Int? = 0,
        /** vip类型：1-初级会员 2-中级会员 3-高级会员*/
        val vipType: Int? = null,
    ) {
        //默认选中
        fun isSelect(): Boolean {
            return defaultSelectStatus == 1
        }
    }
}
