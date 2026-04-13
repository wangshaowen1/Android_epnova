package com.kuaigeglobal.epnova.feature.splash

import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.base.viewmodel.AppRepositoryViewModel
import com.kuaigeglobal.epnova.core.AppRepository
import com.kuaigeglobal.epnova.core.User
import com.kuaigeglobal.epnova.core.ext.launchWithLoading
import com.kuaigeglobal.epnova.core.network.apis.response.UserHomeData
import com.kuaigeglobal.epnova.core.response
import com.kuaigeglobal.epnova.kit.SignUtils
import com.nhsd.basic.ext.showToast

class SplashViewModel : AppRepositoryViewModel<AppRepository>()
{


    /**
     * 检测是否登录
     * 如果未登录 进行强制游客登录
     *
     * 无网络情况下 为了确保用户运行app期间 处于用户状态下 在未登录情况下 会强制检测网络
     * 用户登录情况下 无网络也可进app 确保用户体验
     */
    fun checkToGuestLogin(onLogin: ((UserHomeData?) -> Unit)? = null,onFail:(() -> Unit))
    {
        if (User.get().isVisitor) {

            launchWithLoading (false, block = {
                response(mRepo.api.loginBygGuest(), result = {
                    User.get().updateFrom(data)
                    SignUtils.signIn()
                    onLogin?.invoke(data)
                }, error = {
                    onFail()
                })

            }, onError = {
                onFail()
            })
        }
        else
        {
            onLogin?.invoke(null)
        }
    }

}