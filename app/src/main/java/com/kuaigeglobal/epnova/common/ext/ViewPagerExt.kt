package com.kuaigeglobal.epnova.common.ext

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import android.view.animation.Interpolator
import android.widget.OverScroller
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.CompositePageTransformer
import androidx.viewpager2.widget.MarginPageTransformer
import androidx.viewpager2.widget.ViewPager2
import com.blankj.utilcode.util.SizeUtils
import com.kuaigeglobal.epnova.common.transform.ScaleInTransformer
import com.kuaigeglobal.epnova.widget.FadeInOutTransformer
import com.zhpan.bannerview.BannerViewPager

// ViewPager2扩展
fun ViewPager2.setGalleryEffect(
    leftItemWidth: Int,
    rightItemWidth: Int,
    pageMargin: Int
) {
    setGalleryEffect(leftItemWidth, rightItemWidth, pageMargin, .85f, 1f)
}

fun ViewPager2.setGalleryEffect(
    itemWidth: Int,
    pageMargin: Int,
    scale: Float = 0.85f,
    alpha: Float = 1f
) {
    setGalleryEffect(itemWidth, itemWidth, pageMargin, scale, alpha)
}

/**
 * 为banner添加画廊效果
 *
 * @param leftItemWidth  item左展示的宽度,单位dp
 * @param rightItemWidth item右展示的宽度,单位dp
 * @param pageMargin     页面间距,单位dp
 * @param scale          缩放[0-1],1代表不缩放
 * @param alpha          透明[0-1],1代表不透明
 */
fun ViewPager2.setGalleryEffect(
    leftItemWidth: Int,
    rightItemWidth: Int,
    pageMargin: Int,
    scale: Float = 0.85f,
    alpha: Float = 1f
) {
    var mCompositePageTransformer = CompositePageTransformer()
    if (pageMargin > 0) {
        mCompositePageTransformer.addTransformer(
            MarginPageTransformer(
                SizeUtils.dp2px(
                    pageMargin.toFloat()
                )
            )
        )
    }
    if (scale < 1 && scale > 0) {
        mCompositePageTransformer.addTransformer(ScaleInTransformer(scale))
    }
    if (scale < 1 && alpha > 0) {
        mCompositePageTransformer.addTransformer(FadeInOutTransformer(alpha))
    }
    val leftItemPadding =
        if (leftItemWidth > 0) SizeUtils.dp2px((leftItemWidth + pageMargin).toFloat()) else 0
    val rightItemPadding =
        if (rightItemWidth > 0) SizeUtils.dp2px((rightItemWidth + pageMargin).toFloat()) else 0

    val recyclerView = getChildAt(0) as RecyclerView
    if (orientation == ViewPager2.ORIENTATION_VERTICAL) {
        recyclerView.setPadding(
            paddingLeft,
            leftItemPadding,
            paddingRight,
            rightItemPadding
        )
    } else {
        recyclerView.setPadding(
            leftItemPadding,
            paddingTop,
            rightItemPadding,
            paddingBottom
        )
    }
    recyclerView.clipToPadding = false
    setPageTransformer(mCompositePageTransformer)
}

fun ViewPager2.setClipPadding(leftPadding: Int, rightPadding: Int) {
    offscreenPageLimit = 1
    val recyclerView = getChildAt(0) as RecyclerView
    recyclerView.setPadding(leftPadding, recyclerView.paddingTop, rightPadding, recyclerView.paddingBottom)
    recyclerView.clipToPadding = false
}

fun ViewPager2.removeTheRipples() {
    try {
        val mRecyclerView = this.javaClass.getDeclaredField("mRecyclerView")
        mRecyclerView.isAccessible = true
        val recyclerView = mRecyclerView.get(this) as RecyclerView
        recyclerView.overScrollMode = View.OVER_SCROLL_NEVER
    } catch (e: Exception) {
        e.printStackTrace()
    }
}


class CustomScroller(context: Context, interpolator: Interpolator? = null) :
    OverScroller(context, interpolator) {
    private var mDuration = 1000 // 默认滑动时间为1000毫秒

    fun setDuration(duration: Int) {
        mDuration = duration
    }

    override fun startScroll(startX: Int, startY: Int, dx: Int, dy: Int, duration: Int) {
        super.startScroll(startX, startY, dx, dy, mDuration)
    }

    override fun fling(
        startX: Int,
        startY: Int,
        velocityX: Int,
        velocityY: Int,
        minX: Int,
        maxX: Int,
        minY: Int,
        maxY: Int,
        overX: Int,
        overY: Int
    ) {
        super.fling(startX, startY, velocityX, velocityY, minX, maxX, minY, maxY, overX, overY)
        // 可以在这里调整 fling 动画的持续时间
    }
}

fun ViewPager2.setScrollDuration(duration: Int) {
    try {
        val viewPagerField = ViewPager2::class.java.getDeclaredField("mScroller")
        viewPagerField.isAccessible = true
        val customScroller = CustomScroller(context)
        customScroller.setDuration(duration)
        viewPagerField.set(this, customScroller)
    } catch (e: NoSuchFieldException) {
        e.printStackTrace()
    } catch (e: IllegalAccessException) {
        e.printStackTrace()
    }
}


@SuppressLint("SuspiciousIndentation")
fun BannerViewPager<*>.setScrollDuration(duration: Int) {
    try {
        val viewPager = BannerViewPager::class.java.getDeclaredField("mViewPager") as ViewPager2
        setScrollDuration(duration)
    } catch (e: NoSuchFieldException) {
        e.printStackTrace()
    } catch (e: IllegalAccessException) {
        e.printStackTrace()
    }
}