package com.kuaigeglobal.epnova.common.ext

import android.animation.Animator
import android.animation.ArgbEvaluator
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.children
import com.blankj.utilcode.util.ActivityUtils
import com.kuaigeglobal.epnova.R
import com.nhsd.basic.ext.dp


fun View.shake() {
    (getTag(R.id.shakeAnim) as? Animator)?.cancel()
    val dp5 = 5.dp.toFloat()
    ObjectAnimator.ofFloat(this, "translationX", 0f, -dp5, 0f, dp5, 0f)
        .apply {
            duration = 300
            repeatCount = 1
            repeatMode = ValueAnimator.RESTART
            start()
        }.let {
            setTag(R.id.shakeAnim, it)
        }
}

fun <LayoutParams : ViewGroup.LayoutParams> View.updateLayoutParams(transform: (LayoutParams) -> Unit) {
    (layoutParams as? LayoutParams)?.let {
        transform(it)
        layoutParams = it
    }
}

// EditText扩展
fun EditText.setProhibitGetFocus(isProhibit: Boolean) {
    if (isProhibit) {
        isCursorVisible = false
        isFocusable = false
        isFocusableInTouchMode = false
        setOnClickListener(null)
    } else {
        isFocusable = true
        isFocusableInTouchMode = true
        requestFocus()
        isCursorVisible = true
    }
}

fun EditText.trimText(): String = text.toString().trim()


// ========================================================================================
// View 动画扩展
// ========================================================================================

fun View.animateScaleEnter(duration: Long = 300, listener: Animator.AnimatorListener? = null) {
    val p1 = PropertyValuesHolder.ofFloat("alpha", 0f, 1f)
    val p2 = PropertyValuesHolder.ofFloat("scaleX", 0f, 1f)
    val p3 = PropertyValuesHolder.ofFloat("scaleY", 0f, 1f)
    val p4 = PropertyValuesHolder.ofObject("backgroundColor", ArgbEvaluator(), Color.TRANSPARENT, Color.BLACK)

    ObjectAnimator.ofPropertyValuesHolder(this, p1, p2, p3, p4).apply {
        this.duration = duration
        if (listener != null) addListener(listener)
        start()
    }
}

fun View.animateScaleExit(duration: Long = 300, listener: Animator.AnimatorListener? = null) {
    val p1 = PropertyValuesHolder.ofFloat("alpha", 1f, 0f)
    val p2 = PropertyValuesHolder.ofFloat("scaleX", 1f, 0f)
    val p3 = PropertyValuesHolder.ofFloat("scaleY", 1f, 0f)
    val p4 = PropertyValuesHolder.ofObject("backgroundColor", ArgbEvaluator(), Color.BLACK, Color.TRANSPARENT)

    ObjectAnimator.ofPropertyValuesHolder(this, p1, p2, p3, p4).apply {
        this.duration = duration
        if (listener != null) addListener(listener)
        start()
    }
}


fun ViewGroup.getChildByType(vararg types: Class<out View>): View? {
    // 1. 先在当前层级找，找不到再深入递归（广度优先思想，性能更好）
    for (child in children) {
        if (types.any { it.isInstance(child) }) {
            return child
        }
    }
    // 2. 当前层级没找到，再去子层级找
    for (child in children) {
        if (child is ViewGroup) {
            val result = child.getChildByType(*types)
            if (result != null) return result
        }
    }
    return null
}

/**
 * 极简、稳健的键盘焦点自动清理方案
 */
fun View.applySmartFocusManager() {
    // 1. 让根布局成为“焦点避风港”
//    this.isFocusable = true
//    this.isFocusableInTouchMode = true

    // 2. 点击空白处：通过 findFocus() 找到内部的输入框并清理
    this.setOnClickListener {
        // 注意：这里调用的是 View 内部的 findFocus() 方法
        val focusedChild = this.findFocus()
        if (focusedChild is EditText) {
            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            imm?.hideSoftInputFromWindow(focusedChild.windowToken, 0)
            focusedChild.clearFocus()
        }
    }

    // 3. 监听键盘收起动作（修复“能输入不能删除”Bug的核心）
    var isKeyboardOpen = false

    ViewCompat.setOnApplyWindowInsetsListener(this) { v, insets ->
        val isKeyboardVisible = insets.isVisible(WindowInsetsCompat.Type.ime())

        // 只有键盘从“打开”变为“关闭”时才触发
        if (isKeyboardOpen && !isKeyboardVisible) {
            // 同样使用 findFocus() 向下寻找当前持有焦点的 View
            val focusedChild = v.findFocus()
            if (focusedChild is EditText) {
                // 使用 post 确保在下一帧布局稳定后执行，避开输入法冲突
                v.post {
                    if (v.isAttachedToWindow) {
                        focusedChild.clearFocus()
                    }
                }
            }
        }

        isKeyboardOpen = isKeyboardVisible
        // 必须原样返回 insets，否则会导致子 View 无法正常处理内边距
        insets
    }
}

fun View.getActivity(): Activity = ActivityUtils.getActivityByContext(context) ?: ActivityUtils.getTopActivity()