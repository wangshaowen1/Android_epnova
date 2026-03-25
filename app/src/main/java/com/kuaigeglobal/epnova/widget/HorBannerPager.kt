package com.kuaigeglobal.epnova.widget

import android.content.Context
import android.util.AttributeSet
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.zhpan.bannerview.BannerViewPager

class HorBannerPager<T> : BannerViewPager<T> {
    constructor(context: Context) : super(context) {
        isNestedScrollingEnabled = false
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        isNestedScrollingEnabled = false
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        isNestedScrollingEnabled = false
    }


    override fun setNestedScrollingEnabled(enabled: Boolean) {
        super.setNestedScrollingEnabled(enabled)
        viewPager?.isNestedScrollingEnabled = false
        recyclerView?.isNestedScrollingEnabled = false
    }

    private val viewPager: ViewPager2? by lazy {
        try {
            val field = BannerViewPager::class.java.getDeclaredField("mViewPager")
            field.isAccessible = true
            field[this] as ViewPager2
        } catch (e: NoSuchFieldException) {
            null
        } catch (e: IllegalAccessException) {
            null
        }
    }


    private val recyclerView: RecyclerView? by lazy {
        try {
            val field = ViewPager2::class.java.getDeclaredField("mRecyclerView")
            field.isAccessible = true
            field.get(viewPager) as RecyclerView
        } catch (e: NoSuchFieldException) {
            null
        } catch (e: IllegalAccessException) {
            null
        }
    }
}
