package com.kuaigeglobal.epnova.common.utils

import android.app.Activity


/**
 * *****************************************
 * 项目名称: 小元音乐圈
 * @Author: SZYMN
 * @Introduce: 微信分享的工具
 * @Data: 2024/6/25 星期二 15:17
 * @Version: 1.0
 * *****************************************
 */
object WXUtils {
    /**
     * 吊起微信客服
     */
    fun customerServiceChat(activity: Activity? = null, kfUrl: String? = null, corpId: String? = null) {
        if (activity == null) return
        if (kfUrl.isNullOrEmpty() || corpId.isNullOrEmpty()) return
//        val api = WXAPIFactory.createWXAPI(activity, BuildConfig.WX_APP_ID)
//        if (!api.isWXAppInstalled) {
//            val msg = "未检测到微信客户端，请先安装微信，若已安装请打开微信一次。"
//            T.showShort(msg)
//            return
//        }
//        // 判断当前版本是否支持拉起客服会话
//        if (api.wxAppSupportAPI >= Build.SUPPORT_OPEN_CUSTOMER_SERVICE_CHAT) {
//            val req = WXOpenCustomerServiceChat.Req()
//            // 企业ID
//            req.corpId = corpId
//            // 客服URL
//            req.url = kfUrl
//            api.sendReq(req)
//        }
    }
}