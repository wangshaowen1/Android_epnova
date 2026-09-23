package com.kuaigeglobal.epnova.common.ext

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.text.SpannableString
import android.view.WindowManager
import androidx.annotation.DrawableRes
import androidx.fragment.app.FragmentActivity
import com.kuaigeglobal.epnova.widget.CommonDialog

fun FragmentActivity.showCommonDialog(title: String, confirm: () -> Unit) {
    showCommonDialog(null, title, { confirm.invoke() }, null)
}

fun FragmentActivity.showCommonDialogWithTip(tip: String, confirm: () -> Unit) {
    showCommonDialog(tip, null, { confirm.invoke() }, null)
}

fun FragmentActivity.showCommonDialog(
    tip: String,
    confirm: (CommonDialog) -> Unit,
    cancel: (CommonDialog) -> Unit
) {
    showCommonDialog(tip, null, confirm, cancel)
}

fun Context.showCommonDialog(tip: String? = null, title: String? = null, confirm: () -> Unit) {
    showCommonDialog(tip, title, null, false, confirm)
}

fun Context.showCommonDialog(
    tip: String? = null,
    title: String? = null,
    confirmText: String?,
    confirm: () -> Unit
) {
    showCommonDialog(tip, title, confirmText, false, confirm)
}

fun Context.showCommonDialog(
    tip: String? = null,
    title: String? = null,
    confirm: (CommonDialog) -> Unit,
    cancel: ((CommonDialog) -> Unit)? = null
) {
    showCommonDialog(tip, title, false, confirm, cancel)
}


fun Context.showCommonDialog(
    tip: String,
    title: String? = null,
    system: Boolean,
    confirm: () -> Unit
) {
    showCommonDialog(tip, title, null, system, confirm)
}


fun Context.showCommonDialog(
    tip: String? = null,
    title: String? = null,
    confirmText: String? = null,
    system: Boolean,
    confirm: () -> Unit
) {
    val commonDialog = CommonDialog(this, tip)
    title?.let {
        commonDialog.setTitle(it)
    }

    commonDialog.onCancelButtonClickListener = { dialog -> dialog?.dismiss() }
    commonDialog.onPositiveButtonClickListener = { dialog ->
        confirm.invoke()
        dialog?.dismiss()
    }
    confirmText?.let { commonDialog.setPositiveButtonText(it) }
    if (system) {
        commonDialog.window?.setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY);
    }
    commonDialog.show()
}


/**
 * 单确认弹框
 * @receiver Context
 * @param title String
 * @param positiveButtonText String
 * @param confirm Function0<Unit>
 */
fun Context.showConfirmDialog(
    title: String,
    positiveButtonText: String?=null,
    confirm: ((Dialog) -> Unit)? = null
) {
    showConfirmDialog(null, title, positiveButtonText, confirm = confirm)
}

/**
 * 单确认弹框
 * @receiver Context
 * @param title String
 * @param positiveButtonText String
 * @param confirm Function0<Unit>
 */
fun Context.showConfirmDialog(
    tips: String?,
    title: String?,
    positiveButtonText: String?=null,
    gravity: Int? = null,
    backEnable: Boolean = true,
    confirm: ((Dialog) -> Unit)? = null
) {
    val dialog = CommonDialog(this)
    title?.let { dialog.setTitle(title) }
    tips?.let { dialog.setContent(tips) }
    dialog.setConfirmStyle(true)
    dialog.setBackEnable(backEnable)
    dialog.setCancelable(backEnable)
    positiveButtonText?.let { dialog.setPositiveButtonText(positiveButtonText) }
    dialog.onPositiveButtonClickListener = confirm
    gravity?.let { dialog.setContentGravity(it) }
    dialog.show()
}


fun Context.showCommonDialog(
    tip: String,
    title: String? = null,
    confirmText: String?,
    confirm: (CommonDialog) -> Unit,
    cancel: ((CommonDialog) -> Unit)?
) {
    showCommonDialog(tip, title, confirmText, false, confirm, cancel)
}

fun Context.showCommonDialog(
    tip: String? = null,
    title: String? = null,
    confirmText: String?,
    cancelText: String?,
    confirm: (CommonDialog) -> Unit,
) {
    showCommonDialog(tip, title, confirmText, cancelText, false, confirm, null)
}

fun Context.showCommonDialog(
    tip: CharSequence? = null,
    title: String? = null,
    confirmText: String?,
    cancelText: String?,
    confirm: (CommonDialog) -> Unit,
    cancel: ((CommonDialog) -> Unit)?
) {
    showCommonDialog(tip, title, confirmText, cancelText, false, confirm, cancel)
}

fun Context.showCommonDialog(
    tip: String? = null,
    title: String? = null,
    system: Boolean,
    confirm: (CommonDialog) -> Unit,
    cancel: ((CommonDialog) -> Unit)?
) {
    showCommonDialog(tip, title, null, system, confirm, cancel)
}

fun Context.showCommonDialog(
    tip: String? = null,
    title: String? = null,
    confirmText: String?,
    system: Boolean,
    confirm: (CommonDialog) -> Unit,
    cancel: ((CommonDialog) -> Unit)?,
) {
    showCommonDialog(tip, title, confirmText, null, system, confirm, cancel)
}

fun Context.showCommonDialog(
    tip: CharSequence? = null,
    title: String? = null,
    confirmText: String?,
    cancelText: String?,
    system: Boolean,
    confirm: ((CommonDialog) -> Unit)?,
    cancel: ((CommonDialog) -> Unit)?,
) {
    val commonDialog = CommonDialog(this, tip)
    title?.let {
        commonDialog.setTitle(it)
    }

    cancel?.let {
        commonDialog.onCancelButtonClickListener = { dialog ->
            it.invoke(dialog as CommonDialog)
            dialog.dismiss()
        }
    }
    commonDialog.onPositiveButtonClickListener = { dialog ->
        confirm?.invoke(dialog as CommonDialog)
        dialog.dismiss()
    }
    cancelText?.let {
        commonDialog.setCancelButtonText(cancelText)
    }
    confirmText?.let { commonDialog.setPositiveButtonText(it) }
    if (system) {
        commonDialog.window?.setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY);
    }
    commonDialog.show()
}

// 对话框扩展
fun Context.showConfirmPrompt(
    tip: Int? = null,
    title: Int? = null,
    confirmText: Int? = null,
    cancelText: Int? = null,
    gravity: Int? = null,
    confirmStyle: Boolean? = null,
    cancelable: Boolean? = null,
    system: Boolean? = null,
    @DrawableRes confirmButtonBackgroundResId: Int? = null,
    cancel: ((CommonDialog) -> Unit)? = null,
    confirm: ((CommonDialog) -> Unit)? = null
) {
    showConfirmPrompt(
        tip?.let { getString(it) },
        tipSpannable = null,
        title?.let { getString(it) },
        confirmText?.let { getString(it) },
        cancelText?.let { getString(it) },
        gravity,
        confirmStyle,
        cancelable,
        system,
        confirmButtonBackgroundResId,
        cancel,
        confirm
    )
}

/**
 * 通用弹窗，省一堆的构造函数
 */
fun Context.showConfirmPrompt(
    tip: CharSequence? = null,
    tipSpannable: SpannableString? = null,
    title: String? = null,
    confirmText: String? = null,
    cancelText: String? = null,
    gravity: Int? = null,
    confirmStyle: Boolean? = null,
    cancelable: Boolean? = null,
    system: Boolean? = null,
    @DrawableRes confirmButtonBackgroundResId: Int? = null,
    cancel: ((CommonDialog) -> Unit)? = null,
    confirm: ((CommonDialog) -> Unit)? = null
) {
    val activity = when (this) {
        is Activity -> this
        is ContextWrapper -> (this.baseContext as? Activity)
        else -> null
    }

    if (activity == null || activity.isFinishing || activity.isDestroyed) return

    val commonDialog = CommonDialog(this, tip)
    title?.let {
        commonDialog.setTitle(it)
    }
    tipSpannable?.let {
        commonDialog.setContent(it)
    }
    cancel?.let {
        commonDialog.onCancelButtonClickListener = { dialog ->
            it.invoke(dialog as CommonDialog)
            dialog.dismiss()
        }
    }
    commonDialog.onPositiveButtonClickListener = { dialog ->
        confirm?.invoke(dialog as CommonDialog)
        dialog.dismiss()
    }
    gravity?.let { commonDialog.setContentGravity(it) }
    confirmStyle?.let {
        commonDialog.setConfirmStyle(it)
    }
    cancelText?.let {
        commonDialog.setCancelButtonText(it)
    }
    cancelable?.let {
        commonDialog.setBackEnable(it)
        commonDialog.setCancelable(it)
    }
    confirmText?.let { commonDialog.setPositiveButtonText(it) }
    confirmButtonBackgroundResId?.let { commonDialog.setConfirmButtonBackgroundResource(it) }
    if (system == true) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            commonDialog.window?.setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY)
        }
    }
    try {
        commonDialog.show()
    } catch (e: WindowManager.BadTokenException) {
        e.printStackTrace()
    }
}