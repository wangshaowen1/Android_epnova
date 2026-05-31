package com.kuaigeglobal.epnova.core

import com.kuaigeglobal.epnova.App
import com.kuaigeglobal.epnova.common.ext.ioScope
import com.kuaigeglobal.epnova.core.manager.OverseasFingerprintManager
import com.kuaigeglobal.epnova.core.network.apis.ApiException
import com.kuaigeglobal.epnova.core.network.apis.ApiFactory
import com.kuaigeglobal.epnova.core.network.apis.request.UserTokenBody
import com.kuaigeglobal.epnova.core.network.apis.response.UserHomeData
import com.nhsd.basic.ext.launchNoCrash

/**
 * 全部变量
 */
object Global {


    /**
     * 首次注册用户，需要弹出完善资料
     */
    var needCompleteProfile = false

    /**
     * 续写的内容，生命周期内保持，退出登录，退出app或者使用后清除
     */
    val continueWritingContent: MutableMap<String?, String?> by lazy {
        mutableMapOf()
    }

    /**
     * 灵感生成的内容，生命周期内保持，退出登录，退出app或者使用后清除
     */
    var inspirationContent: String? = null

    /**
     * 自己写故事内容，生命周期内保持，退出登录，退出app或者使用后清除
     */
    var customizedContent: String? = null

    fun refreshUserInfo() {
        ioScope.launchNoCrash {
            response(ApiFactory.apiService.getUserHomeInfo(UserTokenBody())) {
                User.get().updateFrom(data)
            }
        }
    }

    fun refreshCoinsAndPoints() {
        if (User.get().isVisitor) return
        ioScope.launchNoCrash {
            val resp = ApiFactory.apiService.queryCoinsAndPointsWalletInfo(UserTokenBody())
            if (resp.isSuccess) {
                resp.data?.let { User.get().storeCoinsAndPointsWallet(it) }
            }
        }
    }

    fun clearContinueInputContent(dramasSeriesId: String?) {
        continueWritingContent.remove(dramasSeriesId)
    }

    fun clearInputContent() {
        inspirationContent = null
        customizedContent = null
    }

    /**
     * 检测是否登录
     * 如果未登录 进行强制游客登录
     */
    fun guestLogin(onSuccess: ((UserHomeData?) -> Unit)? = null, onError: ((Throwable?) -> Unit)? = null) {
        ioScope.launchNoCrash({
            val fingerprint = OverseasFingerprintManager.getFingerprint(App.instance)
            response(ApiFactory.apiService.loginBygGuest(fingerprint), {
                onSuccess?.invoke(data)
            }, {
                onError?.invoke(ApiException(this?.code ?: -1, this?.msg))
            })
        }, onError = {
            onError?.invoke(it)
        })
    }
}