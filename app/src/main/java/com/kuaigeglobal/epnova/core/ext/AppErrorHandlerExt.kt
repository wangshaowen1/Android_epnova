@file:JvmName("ErrorUtils")
package com.kuaigeglobal.epnova.core.ext

import android.app.Dialog
import android.content.DialogInterface
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.blankj.utilcode.util.NetworkUtils
import com.kuaigeglobal.epnova.App
import com.nhsd.basic.BaseActivity
import com.nhsd.basic.ext.isActive
import com.nhsd.basic.ext.showToast
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.core.Events
import com.kuaigeglobal.epnova.core.model.SysWarnInfo
import com.kuaigeglobal.epnova.core.network.apis.ApiException
import com.kuaigeglobal.epnova.core.network.apis.request.AppealTypeBody
import com.kuaigeglobal.epnova.router.AppNavigator
import com.kuaigeglobal.epnova.widget.CommonDialog
import com.kuaigeglobal.epnova.widget.WarningDialog
import com.nhsd.basic.bus.FlowEventBus

var setBlackDialog: CommonDialog? = null
var warningDialog: WarningDialog? = null
fun showError(context: FragmentActivity, ex: Throwable?) {
    if (ex is ApiException) {
        val apiException = ex
        when (apiException.getErrorCode()) {
            ApiException.USER_ADD_TO_BLACK_LIST -> {
                //showReLoginDialog(context, ex);
                FlowEventBus.postSync(Events.UserChangedEvent(Events.UserChangedEvent.TOKEN_EXPIRED))
                //                    if (context != null && context.getPackageName() != null) {
//                        starReLogin(context);
//                    }
                parseMessage(ex)?.showToast()
            }

            ApiException.LOGGED_OUT_USER, ApiException.AUTHORIZATION_ERROR -> FlowEventBus.postSync(Events.UserChangedEvent(Events.UserChangedEvent.TOKEN_EXPIRED))
            ApiException.USER_ACCOUNT_SET_BLACK -> {
                showSetBlackDialog(
                    context,
                    apiException.message,
                    true,
                    AppealTypeBody.TYPE_ACCOUNT
                )
                showSetBlackDialog(
                    context,
                    apiException.message,
                    true,
                    AppealTypeBody.TYPE_DEVICE
                )
            }

            ApiException.DEVICE_SET_BLACK -> showSetBlackDialog(
                context,
                apiException.message,
                true,
                AppealTypeBody.TYPE_DEVICE
            )

            ApiException.USER_ACCOUNT_SET_BLACK_APPEAL, ApiException.DEVICE_SET_BLACK_APPEAL -> showSetBlackDialog(
                context,
                apiException.message,
                false,
                0
            )

            else ->  parseMessage(ex)?.showToast()
        }
    } else {
        parseMessage(ex)?.showToast()
    }
}

private fun showSetBlackDialog(
    context: FragmentActivity,
    reason: String?,
    isAppeal: Boolean,
    appealType: Int
) {
    if (setBlackDialog == null) {
        setBlackDialog = CommonDialog(context)
        setBlackDialog?.setTitle(context.getString(R.string.account_banned_violation))
        setBlackDialog?.setConfirmStyle(true)
        setBlackDialog?.setCanceledOnTouchOutside(false)
        setBlackDialog?.setCancelable(false)
        setBlackDialog?.setPositiveButtonTextColor(
            context.getResources().getColor(R.color.textColorDark)
        )
        setBlackDialog?.setPositiveButtonText(if (isAppeal) context.getString(R.string.action_appeal_now) else context.getString(R.string.status_appeal_reviewing))
        //            setBlackDialog.setPositiveButtonEnable(isAppeal);
        val lifecycleObserver: LifecycleEventObserver = object : LifecycleEventObserver {


            override fun onStateChanged(
                source: LifecycleOwner,
                event: Lifecycle.Event
            ) {
                if (source.lifecycle.currentState == Lifecycle.State.DESTROYED) {
                    if (setBlackDialog != null && setBlackDialog?.isShowing() == true) {
                        setBlackDialog?.dismiss()
                    }
                    source.lifecycle.removeObserver(this)
                }
            }
        }
        context.lifecycle.addObserver(lifecycleObserver)
    }
    if (setBlackDialog?.isShowing==false) {
        //setBlackDialog.setContent("你的账号、设备已被永久封禁。\n封号原因：" + reason);
        setBlackDialog?.setContent(reason)
        setBlackDialog?.show()
        setBlackDialog?.onPositiveButtonClickListener = { dialog: Dialog? ->
            //                    if (isAppeal) {
//                        BlackAppealActivity.Companion.start(context, appealType);
//                    } else {
//                        AppealingStatusActivity.Companion.start(context);
//                    }
            dialog!!.dismiss()
            null
        }

        setBlackDialog?.setOnDismissListener { setBlackDialog = null }
    }
}

fun showWarnDialog(context: BaseActivity?, warningData: SysWarnInfo) {
    if (warningDialog == null) {
        warningDialog = WarningDialog(context, warningData.getMsgContent())
        warningDialog?.setCanceledOnTouchOutside(false)
        warningDialog?.setCancelable(false)

        val lifecycleObserver: LifecycleEventObserver = object : LifecycleEventObserver {
            override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                if (source.lifecycle.currentState == Lifecycle.State.DESTROYED) {
                    if (warningDialog != null && warningDialog?.isShowing == true) {
                        warningDialog?.dismiss()
                    }
                    source.lifecycle.removeObserver(this)
                }
            }
        }
        context?.lifecycle?.addObserver(lifecycleObserver)
    }
    if (context != null && context.isActive && warningDialog?.isShowing ==false) {
        warningDialog?.show()
        warningDialog?.setOnClickListener {

        }
        warningDialog?.setOnDismissListener { warningDialog = null }
    }
}
fun parseMessage(e: Throwable?): String? {
    return if (NetworkUtils.isConnected()) {
        if (e is ApiException) {
            e.message
        } else {
            App.instance.getString(R.string.request_failed_tips)
        }
    } else {
        App.instance.getString(R.string.no_network_tips)
    }
}