package com.kuaigeglobal.epnova.feature.recharge

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.common.ext.showCommonDialog
import com.kuaigeglobal.epnova.feature.settings.feedback.FeedbackActivity

fun Context.showGooglePaymentExceptionDialog() {
    val activity = findActivity() ?: return
    if (activity.isFinishing || activity.isDestroyed) return

    activity.showCommonDialog(
        tip = activity.getString(R.string.google_payment_unsuccessful_content),
        title = activity.getString(R.string.google_payment_unsuccessful_title),
        confirmText = activity.getString(R.string.app_contact_customer_service),
        cancelText = activity.getString(R.string.close),
        confirm = { FeedbackActivity.start(activity) },
        cancel = null
    )
}

private tailrec fun Context.findActivity(): Activity? {
    return when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
}
