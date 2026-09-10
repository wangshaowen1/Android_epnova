package com.kuaigeglobal.epnova.widget

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.AttributeSet
import android.view.View
import android.widget.CompoundButton
import androidx.appcompat.widget.AppCompatRadioButton
import com.kuaigeglobal.epnova.R

/**
 *
 * 类说明: 1.点击带动画效果的RadioButton
 *        2.修复Android 4.4 radioButton设置 android:button="@null" 无效
 *
 * qtip:RadioGroup中设置gravity 为bottom
 */
class FixedAnimatedRadioButton : AppCompatRadioButton, ValueAnimator.AnimatorUpdateListener {
    private var checkedDrawable: Drawable? = null

    /**
     * 缩放比
     */
    private var animatedScaleRate = 1f

    /**
     * 是否打开了动画
     */
    private var animationOpen = true

    /**
     * 默认图标drawable的Rect位置
     */
    private var mDefaultDrawableBounds: Array<Rect?>? = null
    private var appliedDrawableBounds: Array<Rect?>? = null
    private var animatedDrawables: Array<Drawable?>? = null
    private lateinit var valueAnimator: ValueAnimator

    constructor(context: Context?) : super(context) {
        init(context, null)
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        init(context, attrs)
    }

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        init(context, attrs)
    }


    private fun init(context: Context?, attrs: AttributeSet?) {
        val typedArray =
            context?.obtainStyledAttributes(attrs, R.styleable.FixedAnimatedRadioButton)
        typedArray?.apply {
            //默认打开缩放动画
            animationOpen = getBoolean(R.styleable.FixedAnimatedRadioButton_animation_open, true)
            checkedDrawable = getDrawable(R.styleable.FixedAnimatedRadioButton_checkedDrawable)
            recycle()
        }
        valueAnimator = ValueAnimator.ofFloat(1f, 0.75f, 0.95f, 0.85f, 0.95f, 1f).apply {
            duration = 1000
            addUpdateListener(this@FixedAnimatedRadioButton)
        }
    }

    override fun setButtonDrawable(buttonDrawable: Drawable?) {
        //修复低版本(android4.4)设置按钮为null时显示默认按钮
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT) {
            try {
                val clazz = CompoundButton::class.java
                val field = clazz.getDeclaredField("mButtonDrawable")
                field.isAccessible = true
                field.set(this, null)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            super.setButtonDrawable(buttonDrawable)
        }
    }

    private fun startAnimator() {
        resetAnimation()
        val drawables = compoundDrawables
        if (drawables.all { it == null }) return
        animatedDrawables = drawables
        mDefaultDrawableBounds = Array(4) { index -> drawables[index]?.let { Rect(it.bounds) } }
        appliedDrawableBounds = Array(4) { index -> drawables[index]?.let { Rect(it.bounds) } }
        valueAnimator.start()
    }

    override fun setCompoundDrawables(left: Drawable?, top: Drawable?, right: Drawable?, bottom: Drawable?) {
        // 在 TextView 缓存图标占位前复原仍被复用的图标，而不是等下一帧才清理。
        resetAnimation()
        super.setCompoundDrawables(left, top, right, bottom)
    }

    override fun setCompoundDrawablesRelative(start: Drawable?, top: Drawable?, end: Drawable?, bottom: Drawable?) {
        resetAnimation()
        super.setCompoundDrawablesRelative(start, top, end, bottom)
    }


    override fun setChecked(checked: Boolean) {
        val wasChecked = isChecked
        super.setChecked(checked)
        // CompoundButton 构造期间也会调用此方法，此时动画尚未初始化。
        if (!::valueAnimator.isInitialized) return
        if (!isChecked) {
            resetAnimation()
        } else if (!wasChecked && animationOpen && isAttachedToWindow && isShown && width > 0 && height > 0) {
            startAnimator()
        }
    }


    override fun onAnimationUpdate(animation: ValueAnimator) {
        val drawables = animatedDrawables ?: return
        val bounds = mDefaultDrawableBounds ?: return
        val current = compoundDrawables
        // 运行中更换图标时结束旧动画，避免把旧尺寸施加到新图标。
        if (drawables.indices.any { current[it] !== drawables[it] }) {
            resetAnimation()
            return
        }
        animatedScaleRate = animation.animatedValue as Float
        for (index in drawables.indices) {
            val drawable = drawables[index] ?: continue
            val original = bounds[index] ?: continue
            val dx = (original.width() * (1 - animatedScaleRate)).toInt() / 2
            val dy = (original.height() * (1 - animatedScaleRate)).toInt() / 2
            val applied = appliedDrawableBounds?.get(index) ?: continue
            applied.set(original.left + dx, original.top + dy, original.right - dx, original.bottom - dy)
            drawable.bounds = applied
        }
        // 图标占位保持不变，只重绘；避免每帧设置 compound drawable 触发重新布局。
        invalidate()
    }

    private fun resetAnimation() {
        if (!::valueAnimator.isInitialized) return
        valueAnimator.cancel()
        animatedDrawables?.forEachIndexed { index, drawable ->
            // 调用方可能已为同一个 Drawable 设置新尺寸，仅复原动画自身写入的 bounds。
            if (drawable != null && drawable.bounds == appliedDrawableBounds?.get(index)) {
                mDefaultDrawableBounds?.get(index)?.let { drawable.bounds = it }
            }
        }
        animatedDrawables = null
        mDefaultDrawableBounds = null
        appliedDrawableBounds = null
        animatedScaleRate = 1f
        invalidate()
    }

    override fun onDetachedFromWindow() {
        resetAnimation()
        super.onDetachedFromWindow()
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        if (!isShown) resetAnimation()
    }

    override fun onWindowVisibilityChanged(visibility: Int) {
        super.onWindowVisibilityChanged(visibility)
        if (visibility != VISIBLE) resetAnimation()
    }
}
