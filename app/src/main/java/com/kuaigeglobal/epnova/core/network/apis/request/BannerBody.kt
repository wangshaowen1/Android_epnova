package com.kuaigeglobal.epnova.core.network.apis.request

data class BannerBody(
    /**
     * 20260227启用新入参banner展示位置code（首页=banner_home，付费弹窗=banner_pay_popup，个人主页=banner_personal_home，商城页面=banner_mall_page，攻略咨询界面=banner_guide_page）
     */
    val bannerLocationCode: String?
) : UserTokenBody() {
    companion object {
        //消息页
        const val LOCATION_HOME = "banner_home"

        //付费弹窗
        const val LOCATION_PAY_POPUP = "banner_pay_popup"

        //个人主页
        const val LOCATION_PERSONAL_HOME = "banner_personal_home"

        //商城页面
        const val LOCATION_MALL_PAGE = "banner_mall_page"

        //攻略咨询界面
        const val LOCATION_GUIDE_PAGE = "banner_guide_page"
    }
}