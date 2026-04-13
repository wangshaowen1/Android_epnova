package com.kuaigeglobal.epnova.core

import android.util.Log
import com.kuaigeglobal.epnova.common.ext.ioScope
import com.kuaigeglobal.epnova.core.network.apis.ApiFactory
import com.kuaigeglobal.epnova.core.network.apis.request.UserTokenBody
import com.kuaigeglobal.epnova.core.network.apis.response.UserHomeData
import com.kuaigeglobal.epnova.kit.SignUtils
import com.nhsd.basic.ext.launchNoCrash
import com.nhsd.basic.ext.showToast
import kotlinx.coroutines.launch

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
    val continueWritingContent: MutableMap<String?, String?> by lazy{
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

    fun refreshBalance() {
        ioScope.launchNoCrash {
            val resp = ApiFactory.apiService.queryWalletInfo(UserTokenBody())
            if (resp.isSuccess) {
                resp.data?.pointNum?.let { User.get().storeComputingPower(it) }
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
    fun guestLogin(onSuccess: ((UserHomeData?) -> Unit)? = null,onError:((UserHomeData?)-> Unit)?)
    {

        ioScope.launch{

            try {

                Log.i("Hello","result001")
                val resp = ApiFactory.apiService.loginBygGuest()
                Log.i("Hello","result"+resp)
                response(resp,{
                    User.get().updateFrom(data)
                    SignUtils.signIn()
                    onSuccess?.invoke(data)
                },{
                    onError?.invoke(this?.data)
                })

            }catch (e: Exception)
            {
                onError?.invoke(null)
            }

        }
    }

}