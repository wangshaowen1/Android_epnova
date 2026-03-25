package com.kuaigeglobal.epnova.widget

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.NestedScrollView
import androidx.recyclerview.widget.RecyclerView
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.common.utils.FlingUtil

class RecyclerScrollView : NestedScrollView {

    private var recyclerView: RecyclerView? = null
    private var contentHeight = 0
    private var recycleViewId = 0

    constructor(cn: Context) : super(cn)

    constructor(cn: Context, attrs: AttributeSet) : super(cn, attrs) {
        initWithAttr(cn, attrs)
    }

    constructor(cn: Context, attrs: AttributeSet, defStyleAttr: Int) : super(cn, attrs, defStyleAttr) {
        initWithAttr(cn, attrs)
    }

    private fun initWithAttr(context: Context, attrs: AttributeSet?) {
        val a = context.obtainStyledAttributes(attrs, R.styleable.RecyclerScrollView)
        recycleViewId = a.getResourceId(R.styleable.RecyclerScrollView_recycler_view_id, 0)
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        recyclerView = findRecycleView()
    }

    private fun findRecycleView(): RecyclerView? {
        if (recycleViewId != 0) {
            recyclerView = findViewById(recycleViewId)
        } else {
            for (i in 0 until childCount) {
                var view = getChildAt(i)
                if (view is RecyclerView) {
                    return view
                }
            }
        }
        return null
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        if (recyclerView != null) {
            val params = recyclerView?.layoutParams as ViewGroup.LayoutParams
            params.height = measuredHeight
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        super.onLayout(changed, l, t, r, b)
        contentHeight = recyclerView?.top ?: 0
    }

    /*
    * 注意这里嵌套滑动的主导者是RecyclerView（所有嵌套滑动的主导者都是子view询问父view是否需要优先滚动）
    * 当NestedScrollView消耗不完的事件才交给RecyclerView来处理
    * */
    override fun onNestedPreScroll(target: View, dx: Int, dy: Int, consumed: IntArray, type: Int) {
        if (recyclerView?.visibility == View.VISIBLE) {
            if (scrollY < contentHeight) {
                if (scrollY + dy <= contentHeight) {//NestedScrollView能处理所有的滚动事件（内容区域已经还在显示），所以没有剩余的滑动给RecyclerView
                    scrollBy(0, dy)
                    consumed[1] = dy
                } else if (scrollY + dy > contentHeight) {//内容区域已经全部隐藏，所以剩余滑动事件交给RecyclerView
                    val scrollViewNeedScrollY = contentHeight - scrollY
                    scrollBy(0, scrollViewNeedScrollY)
                    consumed[1] = scrollViewNeedScrollY
                }
            }
        } else {
            super.onNestedPreScroll(target, dx, dy, consumed, type)
        }
    }


    /*
    * 注意这里的Fling不是属于传统意义的嵌套滑动的范畴，所以没有重写onNestedFling，这里的主导view是NestedScrollView
    * 当其没有消耗的事件才交给recyclerView来处理
    * */
    override fun fling(velocityY: Int) {
        if (recyclerView?.visibility == View.VISIBLE) {
            val dy = FlingUtil.getDistanceByVelocity(context, velocityY)
            if (scrollY < contentHeight) {
                if (scrollY + dy <= contentHeight) {
                    super.fling(velocityY)
                } else if (scrollY + dy > contentHeight) {
                    val scrollViewNeedScrollY = contentHeight - scrollY
                    //让NestedScrollView先处理所有的滚动事件
                    val scrollViewNeedVelocity = FlingUtil.getVelocityByDistance(context, scrollViewNeedScrollY.toDouble())
                    if (velocityY > 0) {
                        super.fling(scrollViewNeedVelocity)
                    } else {
                        super.fling(-scrollViewNeedVelocity)
                    }
                    //把剩余的滚动事件交给RecyclerView处理
                    val recyclerViewScrollY = dy - scrollViewNeedScrollY
                    val recyclerViewNeedVelocity = FlingUtil.getVelocityByDistance(context, recyclerViewScrollY)
                    if (velocityY > 0) {
                        recyclerView?.fling(0, recyclerViewNeedVelocity)
                    } else {
                        recyclerView?.fling(0, -recyclerViewNeedVelocity)
                    }
                }
            }
        } else {
            super.fling(velocityY)
        }
    }

}