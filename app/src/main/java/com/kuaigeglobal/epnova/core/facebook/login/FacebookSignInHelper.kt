package com.kuaigeglobal.epnova.core.facebook.login

import android.util.Log
import androidx.activity.ComponentActivity
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.kuaigeglobal.epnova.core.network.apis.request.LoginByThirdBody

class FacebookSignInHelper(private val activity: ComponentActivity) {

    private val tag = "FacebookSignInHelper"

    private val callbackManager: CallbackManager = CallbackManager.Factory.create()

    private var pendingOnSuccess: ((LoginByThirdBody) -> Unit)? = null
    private var pendingOnFailure: ((String?) -> Unit)? = null

    init {
        LoginManager.getInstance().registerCallback(callbackManager,
            object : FacebookCallback<LoginResult> {
                override fun onSuccess(result: LoginResult) {
                    val token = result.accessToken.token
                    Log.d(tag, "onSuccess: token=$token")
                    pendingOnSuccess?.invoke(
                        LoginByThirdBody(LoginByThirdBody.LOGIN_TYPE_FACEBOOK, accessToken = token)
                    )
                    clearPending()
                }

                override fun onCancel() {
                    Log.d(tag, "onCancel")
                    clearPending()
                }

                override fun onError(error: FacebookException) {
                    Log.d(tag, "onError: $error")
                    pendingOnFailure?.invoke(error.message)
                    clearPending()
                }
            })
    }

    fun signIn(
        onSuccess: (body: LoginByThirdBody) -> Unit,
        onFailure: (msg: String?) -> Unit
    ) {
        pendingOnSuccess = onSuccess
        pendingOnFailure = onFailure
        LoginManager.getInstance().logInWithReadPermissions(
            activity,
            callbackManager,
            listOf("email", "public_profile")
        )
    }

    fun signOut() {
        LoginManager.getInstance().logOut()
    }

    private fun clearPending() {
        pendingOnSuccess = null
        pendingOnFailure = null
    }
}
