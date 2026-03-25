package com.kuaigeglobal.epnova.widget

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.animation.TranslateAnimation
import androidx.core.widget.NestedScrollView
import kotlin.math.abs

/********************
 * Created by ：  lj
 * Date：  2019\4\1 0001.
 * Introduction：   弹跳滚动视图ScrollView
 */
class BounceNestedScrollView(context: Context, attrs: AttributeSet?) :
    NestedScrollView(context, attrs) {
    // 孩子View
    private var inner: View? = null

    // 点击时y坐标
    private var yCoordinate = 0f

    // 矩形(这里只是个形式，只是用于判断是否需要动画.)
    private val normal = Rect()

    // 是否开始计算
    private var isCount = false
    private var lastX = 0f
    private var lastY = 0f
    private var currentX = 0f
    private var currentY = 0f
    private var distanceX = 0f
    private var distanceY = 0f

    //判断上下滑动的flag
    private var upDownSlide = false

    /***
     * 根据 XML 生成视图工作完成.该函数在生成视图的最后调用，在所有子视图添加完之后. 即使子类覆盖了 onFinishInflate
     * 方法，也应该调用父类的方法，使该方法得以执行.
     */
    @SuppressLint("MissingSuperCall")
    override fun onFinishInflate() {
        if (childCount > 0) {
            inner = getChildAt(0)
        }
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        currentX = ev.x
        currentY = ev.y
        when (ev.action) {
            MotionEvent.ACTION_DOWN -> {
            }
            MotionEvent.ACTION_MOVE -> {
                distanceX = currentX - lastX
                distanceY = currentY - lastY
                if (abs(distanceX) < abs(distanceY) && abs(distanceY) > 12) {
                    upDownSlide = true
                }
            }
            MotionEvent.ACTION_UP -> {
            }
            else -> {
            }
        }
        lastX = currentX
        lastY = currentY
        if (upDownSlide && inner != null) {
            commOnTouchEvent(ev)
        }
        return super.dispatchTouchEvent(ev)
    }

    /***
     * 触摸事件
     * @param ev
     */
    private fun commOnTouchEvent(ev: MotionEvent) {
        when (ev.action) {
            MotionEvent.ACTION_DOWN -> {
            }
            MotionEvent.ACTION_UP -> {
                // 手指松开.
                if (isNeedAnimation) {
                    animation()
                    isCount = false
                }
                clear()
            }
            MotionEvent.ACTION_MOVE -> {
                // 按下时的y坐标
                val preY = yCoordinate
                // 时时y坐标
                val nowY = ev.y
                // 滑动距离
                var deltaY = (preY - nowY).toInt()
                if (!isCount) {
                    // 在这里要归0.
                    deltaY = 0
                }
                yCoordinate = nowY
                // 当滚动到最上或者最下时就不会再滚动，这时移动布局
                if (isNeedMove) {
                    // 初始化头部矩形
                    if (normal.isEmpty) {
                        // 保存正常的布局位置
                        normal[inner!!.left, inner!!.top, inner!!.right] = inner!!.bottom
                    }
                    // 移动布局
                    inner!!.layout(
                        inner!!.left,
                        inner!!.top - deltaY / 2,
                        inner!!.right,
                        inner!!.bottom - deltaY / 2
                    )
                }
                isCount = true
            }
            else -> {
            }
        }
    }

    /***
     * 回缩动画
     */
    fun animation() {
        // 开启移动动画
        val ta = TranslateAnimation(0F, 0F, inner!!.top.toFloat(), normal.top.toFloat())
        ta.duration = 200
        inner!!.startAnimation(ta)
        // 设置回到正常的布局位置
        inner!!.layout(normal.left, normal.top, normal.right, normal.bottom)
        normal.setEmpty()
    }

    // 是否需要开启动画
    private val isNeedAnimation: Boolean
        get() = !normal.isEmpty// 0是顶部，后面那个是底部

    /***
     * 是否需要移动布局 inner.getMeasuredHeight():获取的是控件的总高度
     * getHeight()：获取的是屏幕的高度
     * @return
     */
    private val isNeedMove: Boolean
        get() {
            val offset = inner!!.measuredHeight - height
            val scrollY = scrollY
            // 0是顶部，后面那个是底部
            return scrollY == 0 || scrollY == offset
        }

    private fun clear() {
        lastX = 0f
        lastY = 0f
        distanceX = 0f
        distanceY = 0f
        upDownSlide = false
    }
}