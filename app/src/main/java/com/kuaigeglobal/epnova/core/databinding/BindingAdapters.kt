package com.kuaigeglobal.epnova.core.databinding

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.drawable.AnimationDrawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.text.Html
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.method.ScrollingMovementMethod
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AnimationUtils
import android.view.animation.LinearInterpolator
import android.webkit.WebSettings
import android.webkit.WebView
import android.widget.EditText
import android.widget.ImageView
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.ViewAnimator
import android.widget.ViewFlipper
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.FloatRange
import androidx.annotation.RawRes
import androidx.appcompat.widget.Toolbar
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.animation.addListener
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.core.widget.NestedScrollView
import androidx.databinding.BindingAdapter
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.viewpager2.widget.ViewPager2
import com.airbnb.lottie.LottieAnimationView
import com.blankj.utilcode.util.BarUtils
import com.blankj.utilcode.util.ColorUtils
import com.blankj.utilcode.util.KeyboardUtils
import com.blankj.utilcode.util.ScreenUtils
import com.bumptech.glide.GenericTransitionOptions
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.drake.spannable.replaceSpan
import com.drake.spannable.span.CenterImageSpan
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.kuaigeglobal.epnova.App
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.common.LoadState
import com.kuaigeglobal.epnova.common.NumberAnimator
import com.kuaigeglobal.epnova.common.OssHelp
import com.kuaigeglobal.epnova.common.ext.fromHtml
import com.kuaigeglobal.epnova.common.ext.getBitmapDrawableCopyOrNull
import com.kuaigeglobal.epnova.common.ext.loadAnimatedWebp
import com.kuaigeglobal.epnova.common.ext.setClipPadding
import com.kuaigeglobal.epnova.common.ext.setColoredText
import com.kuaigeglobal.epnova.common.ext.shake
import com.kuaigeglobal.epnova.common.ext.topActivity
import com.kuaigeglobal.epnova.common.ext.updateLayoutParams
import com.kuaigeglobal.epnova.common.utils.BlurBackgroundOptionsBuilder
import com.kuaigeglobal.epnova.common.utils.ImageColorExtractionUtils
import com.kuaigeglobal.epnova.common.utils.Utils
import com.kuaigeglobal.epnova.common.utils.setBlurBackground
import com.kuaigeglobal.epnova.core.network.RetrofitClient
import com.kuaigeglobal.epnova.widget.CounterInputLayout
import com.kuaigeglobal.epnova.widget.CoverPlayerView
import com.kuaigeglobal.epnova.widget.DecimalInputFilter
import com.kuaigeglobal.epnova.widget.LoadStateViewHolder
import com.kuaigeglobal.epnova.widget.MarqueeTextView
import com.kuaigeglobal.epnova.widget.RecyclerEditText
import com.kuaigeglobal.epnova.widget.TagGridLayout
import com.kuaigeglobal.epnova.widget.TagLayout
import com.kuaigeglobal.epnova.widget.VerticalDashedLineView
import com.kuaigeglobal.epnova.widget.ViewFlipperWithListener
import com.kuaigeglobal.epnova.widget.expandtextview.ExpandableTextView
import com.kuaigeglobal.epnova.widget.htmlview.HtmlStyleParser
import com.kuaigeglobal.epnova.widget.htmlview.HtmlStyleParser.ImgAlignmentEnum
import com.kuaige.imageloader.GlideApp
import com.kuaige.imageloader.GlideImageLoader
import com.kuaige.imageloader.transform.BlurTransformation
import com.kuaige.videoedit.resource.UrlResource
import com.nhsd.basic.Session
import com.nhsd.basic.ext.dp
import com.nhsd.basic.ext.getActivity
import com.nhsd.basic.ext.getResColor
import com.nhsd.basic.ext.getResString
import com.nhsd.basic.ext.inRange
import com.nhsd.basic.ext.launchNoCrash
import com.nhsd.basic.ext.setOnSingleClickListener
import com.nhsd.basic.manager.FileManager
import com.nhsd.basic.utils.Logger
import com.nhsd.crop.util.ViewUtils
import com.nhsd.crop.view.UCropView
import com.scwang.smart.refresh.layout.SmartRefreshLayout
import com.shizhefei.view.coolrefreshview.CoolRefreshView
import com.shizhefei.view.coolrefreshview.PullHeader
import com.shizhefei.view.coolrefreshview.SimpleOnPullListener
import com.zhpan.bannerview.BannerViewPager
import com.zhpan.bannerview.BaseBannerAdapter
import com.zhpan.bannerview.constants.IndicatorGravity
import com.zhpan.indicator.enums.IndicatorSlideMode
import eightbitlab.com.blurview.BlurTarget
import eightbitlab.com.blurview.BlurView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import java.io.File
import kotlin.math.absoluteValue
import kotlin.math.min


@BindingAdapter("textColorRes", requireAll = false)
fun TextView.setTextColorRes(@ColorRes resId: Int) {
    setTextColor(App.instance.getColorStateList(resId))
}

@BindingAdapter("backgroundRes", requireAll = false)
fun View.setBackgroundRes(@ColorRes resId: Int) {
    setBackgroundResource(resId)
}

@BindingAdapter("backgroundDrawable", requireAll = false)
fun View.setBackgroundDrawable(drawable: Drawable?) {
    background = drawable
}

@SuppressLint("ResourceType")
@BindingAdapter("android:src", requireAll = false)
fun setUri(imageView: ImageView, @RawRes resId: Int) {
    imageView.setImageResource(resId)
}

@BindingAdapter("setImage", requireAll = false)
fun setImage(imageView: ImageView, resId: Int) {
    if (resId == 0) {
        imageView.setImageDrawable(null)
    } else {
        imageView.setImageResource(resId)
    }
}

@BindingAdapter("rotateAngle", requireAll = false)
fun rotateAngle(view: View, angle: Float) {
    view.rotation = angle
}

@BindingAdapter("setLayoutWidth", requireAll = false)
fun setLayoutWidth(view: View, width: Int) {
    if (width > 0) {
        view.layoutParams.width = width
    }
}

@BindingAdapter("setMaxWidth", requireAll = false)
fun setMaxWidth(view: TextView, width: Int) {
    if (width > 0) {
        view.maxWidth = width
    }
}

@BindingAdapter("setMinWidth", requireAll = false)
fun setMinWidth(view: TextView, width: Int) {
    if (width > 0) {
        view.minWidth = width
    }
}

@BindingAdapter("setLayoutWidthDp", requireAll = false)
fun setLayoutWidthDp(view: View, width: Int) {
    view.layoutParams.width = width.dp
}

@BindingAdapter("setLayoutHeightDp", requireAll = false)
fun setLayoutHeightDp(view: View, height: Int) {
    view.layoutParams.height = height.dp
}

@SuppressLint("ResourceType")
@BindingAdapter("android:background", requireAll = false)
fun backgroundColor(view: View, color: Int) {
    view.setBackgroundColor(color)
}

@BindingAdapter("android:drawableStart")
fun drawableStart(tv: TextView, @DrawableRes resId: Int) {
    tv.setCompoundDrawablesWithIntrinsicBounds(resId, 0, 0, 0)
}

@BindingAdapter("android:drawableEnd")
fun drawableEnd(tv: TextView, @DrawableRes resId: Int) {
    tv.setCompoundDrawablesWithIntrinsicBounds(0, 0, resId, 0)
}

@BindingAdapter(
    "loadUri",
    "placeHolder",
    "noHolderIfDrawableNotNull",
    "drawableAsHolder",
    requireAll = false
)
fun loadUri(
    imageView: ImageView,
    path: String?,
    placeHolder: Drawable? = null,
    noHolderIfDrawableNotNull: Boolean = false,
    drawableAsHolder: Boolean = false
) {
    var holder = placeHolder
    if (noHolderIfDrawableNotNull) {
        if (imageView.drawable != null) {
            holder = null
        }
    } else if (drawableAsHolder) {
        holder = imageView.getBitmapDrawableCopyOrNull()
    }
    GlideImageLoader.with(imageView).display(path, holder)
}

@BindingAdapter("loadAnimatedWebp", "placeHolder", requireAll = false)
fun loadAnimatedWebp(imageView: ImageView, url: String?, placeHolder: Drawable? = null) {
    imageView.loadAnimatedWebp(url, placeHolder)
}

//缩略图预加载
@BindingAdapter("imageThumbnailPreload", requireAll = false)
fun imageThumbnailPreload(imageView: ImageView, url: String?) {
    Glide.with(imageView.context)
        .load(url)
        .thumbnail(0.1f) // 先加载原图10%质量的缩略图
        .into(imageView)
}

@BindingAdapter("imageResource", requireAll = false)
fun imageResource(imageView: ImageView, imageResource: Int) {
    imageView.setImageResource(imageResource)
}

interface OnImageLoadedListener {
    fun onImageLoaded(success: Boolean)
}

@BindingAdapter(
    "loadThumbnailUri",
    "placeHolder",
    "noHolderIfDrawableNotNull",
    "drawableAsHolder",
    "isBlur",
    "listener",
    "fitHeightDp",
    requireAll = false
)
fun loadThumbnailUri(
    imageView: ImageView,
    path: String?,
    placeHolder: Drawable? = null,
    noHolderIfDrawableNotNull: Boolean = false,
    drawableAsHolder: Boolean = false,
    isBlur: Boolean? = false,
    listener: OnImageLoadedListener? = null,
    fitHeightDp: Int = 0
) {
    var holder = placeHolder
    if (noHolderIfDrawableNotNull) {
        if (imageView.drawable != null) {
            holder = null
        }
    } else if (drawableAsHolder) {
        holder = imageView.getBitmapDrawableCopyOrNull()
    }
    GlideImageLoader.with(imageView).thumbnail()
        .blur(if (isBlur == true) GlideImageLoader.NATIVE_BLUR else GlideImageLoader.NO_BLUR)
        .load(path).placeholder(holder)
        .addListener(object : RequestListener<Drawable?> {

            override fun onLoadFailed(
                e: GlideException?,
                model: Any?,
                target: Target<Drawable?>,
                isFirstResource: Boolean
            ): Boolean {
                listener?.onImageLoaded(false)
                return false
            }

            override fun onResourceReady(
                resource: Drawable,
                model: Any,
                target: Target<Drawable?>,
                dataSource: DataSource,
                isFirstResource: Boolean
            ): Boolean {
                if (resource != null && fitHeightDp > 0) {
                    imageView.updateLayoutParams<ViewGroup.LayoutParams> {
                        it.height = fitHeightDp.dp
                        it.width =
                            (1f * resource.intrinsicWidth / resource.intrinsicHeight * it.height).toInt()
                    }
                }
                listener?.onImageLoaded(true)
                return false
            }
        }).into(imageView)
}

@BindingAdapter(
    "loadVideoFirstFrame",
    "placeHolder",
    requireAll = false
)
fun loadVideoFirstFrame(
    imageView: ImageView,
    videoUrl: String?,
    placeHolder: Drawable? = null
) {
    if (videoUrl.isNullOrEmpty()) return
    MainScope().launchNoCrash {
        runCatching {
            val videoFile = withContext(Dispatchers.IO) { UrlResource(videoUrl, RetrofitClient.httpClient).download { } }
            val cover = Utils.getVideoCover(videoFile.absolutePath)
            GlideImageLoader.with(imageView).thumbnail()
                .load(cover)
                .placeholder(placeHolder)
                .into(imageView)
        }
    }
}

@BindingAdapter("loadGifUrl", "placeHolder", requireAll = false)
fun loadGifUrl(imageView: ImageView, path: String?, placeHolder: Drawable? = null) {
    if (path?.endsWith(".gif") == true) {
        GlideApp.with(imageView).asGif().load(path).placeholder(placeHolder).skipMemoryCache(true)
            .into(imageView)
    } else {
        GlideImageLoader.with(imageView).display(path, placeHolder)
    }
}

@BindingAdapter("setAvatar", requireAll = false)
fun setAvatar(imageView: ImageView, path: String?) {
    path?.let {
        GlideImageLoader.with(imageView).thumbnail().display(path)
    }
}


@BindingAdapter("loadBitmap", requireAll = false)
fun loadBitmap(imageView: ImageView, bitmap: Bitmap?) {
    bitmap?.let {
        GlideImageLoader.with(imageView).display(bitmap)
    }
}


@BindingAdapter("loadHQThumbnailUri", "placeHolder", "listener", requireAll = false)
fun loadHQThumbnailUri(
    imageView: ImageView, path: String?, placeHolder: Drawable? = null,
    listener: OnImageLoadedListener? = null
) {
    GlideImageLoader.with(imageView).hdThumbnail().load(path).placeholder(placeHolder)
        .addListener(object : RequestListener<Drawable?> {

            override fun onLoadFailed(
                e: GlideException?,
                model: Any?,
                target: Target<Drawable?>,
                isFirstResource: Boolean
            ): Boolean {
                listener?.onImageLoaded(false)
                return false
            }

            override fun onResourceReady(
                resource: Drawable,
                model: Any,
                target: Target<Drawable?>,
                dataSource: DataSource,
                isFirstResource: Boolean
            ): Boolean {
                listener?.onImageLoaded(true)
                return false
            }
        }).into(imageView)
}


@BindingAdapter("srcBitmap", "withFadeAnim", requireAll = false)
fun srcBitmap(imageView: ImageView, bitmap: Bitmap?, withFadeAnim: Boolean?) {
    imageView.setImageBitmap(bitmap)
}

@BindingAdapter(value = ["onRefreshListener"], requireAll = false)
fun onRefreshListener(swipeRefreshLayout: SwipeRefreshLayout, listener: (() -> Unit)?) {
    swipeRefreshLayout.setOnRefreshListener {
        listener?.invoke()
    }
}

@BindingAdapter(
    value = ["setAdapter", "setBannerUrls", "setCurrentItem", "indicatorTopMargin"],
    requireAll = false
)
fun <T> setAdapter(
    pager: BannerViewPager<T>,
    adapter: BaseBannerAdapter<T>?,
    urls: List<T>?,
    currentItem: Int?,
    indicatorTopMargin: Int = 0
) {
    pager.setIndicatorGravity(IndicatorGravity.CENTER)
        .setIndicatorSliderWidth(8.dp, 12.dp).setIndicatorHeight(3.dp)
        .setIndicatorMargin(
            0,
            indicatorTopMargin.dp,
            0,
            (ScreenUtils.getScreenHeight() - ViewUtils.getNavigationBarHeight(topActivity ?: App.instance) - BarUtils.getStatusBarHeight() - 25.dp)
        )
        .setIndicatorSliderColor(R.color.color_50_ffffff.getResColor(), R.color.white.getResColor())
        .setIndicatorSlideMode(IndicatorSlideMode.SCALE)
    pager.setOffScreenPageLimit(1)
    pager.setScrollDuration(750)
    pager.setInterval(3000)

    adapter?.let { pager.adapter = it as BaseBannerAdapter<T>? }
    if (pager.adapter != null) {
        pager.create(urls)
    }
    pager.setCurrentItem(currentItem ?: 0, false)
}

@BindingAdapter("setIndicatorStyle", requireAll = false)
fun setIndicatorStyle(pager: BannerViewPager<*>, style: Int?) {
    style?.let { pager.setIndicatorStyle(style) }
}


@BindingAdapter(value = ["playItemAnimator"], requireAll = false)
fun setItemSelectedAnimator(view: View, playItemAnimator: Boolean) {
    if (playItemAnimator) {
        val scaleX = PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 0.75f, 0.95f, 0.85f, 0.95f, 1f)
        val scaleY = PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 0.75f, 0.95f, 0.85f, 0.95f, 1f)
        val scaleAnimator = ObjectAnimator.ofPropertyValuesHolder(view, scaleX, scaleY)
        scaleAnimator.setDuration(1000)
        scaleAnimator.start()
        view.tag = scaleAnimator
    } else {
        val animator = view.tag as? ObjectAnimator
        animator?.cancel()
    }
}

@BindingAdapter(value = ["setItemAnimator"], requireAll = false)
fun setItemAnimator(recyclerView: RecyclerView, itemAnimator: RecyclerView.ItemAnimator?) {
    itemAnimator?.let {
        recyclerView.itemAnimator = itemAnimator
    }
}

@BindingAdapter(value = ["setAutoPlay"], requireAll = false)
fun setAutoPlay(pager: ViewFlipper, autoPlay: Boolean) {
    if (autoPlay) {
        pager.startFlipping()
        pager.inAnimation =
            AnimationUtils.loadAnimation(pager.context, com.nhsd.basic.R.anim.fade_in_photo)
        pager.outAnimation =
            AnimationUtils.loadAnimation(pager.context, com.nhsd.basic.R.anim.fade_out_photo)
    } else {
        pager.stopFlipping()
        pager.inAnimation = null
        pager.outAnimation = null
    }
}

@BindingAdapter(value = ["addPagerListener"], requireAll = false)
fun addPagerListener(pager: ViewFlipperWithListener, callback: OnPageChangeListener?) {
    pager.setOnViewFlipperListener {
        callback?.onPageChange(it)
    }
}

@BindingAdapter("setLinearLayoutManager", requireAll = false)
fun setLayoutManager(recyclerView: RecyclerView, isVertical: Boolean) {
    if (isVertical) {
        recyclerView.layoutManager = object : LinearLayoutManager(App.instance) {
            override fun requestChildRectangleOnScreen(
                parent: RecyclerView,
                child: View,
                rect: Rect,
                immediate: Boolean,
                focusedChildVisible: Boolean
            ): Boolean {
                return false
            }
        }
    } else {
        recyclerView.layoutManager =
            object : LinearLayoutManager(App.instance, LinearLayoutManager.HORIZONTAL, false) {
                override fun requestChildRectangleOnScreen(
                    parent: RecyclerView,
                    child: View,
                    rect: Rect,
                    immediate: Boolean,
                    focusedChildVisible: Boolean
                ): Boolean {
                    return false
                }
            }
    }
}

@BindingAdapter("setGridLayoutManager", requireAll = false)
fun setGridLayoutManager(recyclerView: RecyclerView, spanCount: Int) {
    recyclerView.layoutManager = GridLayoutManager(App.instance, spanCount)
}

@BindingAdapter("setHorGridLayoutManager", requireAll = false)
fun setHorGridLayoutManager(recyclerView: RecyclerView, spanCount: Int) {
    recyclerView.layoutManager =
        GridLayoutManager(App.instance, spanCount, GridLayoutManager.HORIZONTAL, false)
}

@BindingAdapter("rvScrollToTop")
fun rvScrollToTop(recyclerView: RecyclerView, isToTop: Boolean) {
    if (isToTop) {
        recyclerView.smoothScrollToPosition(0)
    }
}

@BindingAdapter("scrollToPosition", "smooth", requireAll = false)
fun scrollToPosition(recyclerView: RecyclerView, position: Int, smooth: Boolean = true) {
    if (position < 0) return
    if (smooth) {
        recyclerView.smoothScrollToPosition(position)
    } else {
        recyclerView.scrollToPosition(position)
    }
}

@BindingAdapter("scrollToPage", "smooth", requireAll = false)
fun scrollToPage(viewPager: ViewPager2, position: Int, smooth: Boolean = true) {
    val nearPage = (viewPager.currentItem - position).absoluteValue == 1
    viewPager.setCurrentItem(position, if (nearPage) smooth else false)
}

@BindingAdapter(value = ["setLoadMoreListener"], requireAll = false)
fun setLoadMoreListener(recyclerView: RecyclerView, listener: (() -> Unit)?) {

    var isToTop = false

    recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
        override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
            super.onScrollStateChanged(recyclerView, newState)

            val lm = recyclerView.layoutManager
            if (lm is LinearLayoutManager) {
                val lastVisibleItemPosition = lm.findLastVisibleItemPosition()
                val totalItemCount = recyclerView.adapter?.itemCount ?: 0
                val visibleChildCount = recyclerView.childCount

                if (
                    isToTop &&
                    newState == RecyclerView.SCROLL_STATE_IDLE &&
                    totalItemCount != 0 &&
                    lastVisibleItemPosition == totalItemCount - 1 &&
                    visibleChildCount != 0
                ) {
                    listener?.invoke()
                }
            }
        }

        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)
            isToTop = dy > 0
        }
    })
}


//private fun <T, BA : BannerAdapter<T, out RecyclerView.RecommendPlaylistViewHolder>?> Banner<T, BA>.setOnBannerListener(listener: OnBannerListener<Any>) {
//
//}

@BindingAdapter("layoutHeight")
fun layoutHeight(view: View, px: Int) {
    val layoutParams = view.layoutParams
    layoutParams.height = px
    view.layoutParams = layoutParams
}


@BindingAdapter("layoutWidth")
fun layoutWidth(view: View, width: Int) {
    val layoutParams = view.layoutParams
    if (layoutParams.width != width) {
        layoutParams.width = width
        view.layoutParams = layoutParams
    }
}


@BindingAdapter("fabCustomSize")
fun fabCustomSize(fab: FloatingActionButton, dp: Int) {
    fab.customSize = dp
}


@BindingAdapter("etSetSelect")
fun etSetSelect(editText: EditText, position: Int) {
    editText.setSelection(position)
}

@BindingAdapter("singleClick", requireAll = false)
fun singleClick(view: View, clickListener: View.OnClickListener?) {
    Logger.i("clickListener", "clickListener${clickListener}")
    if (clickListener == null) {
        view.setOnClickListener(null)
        return
    }
    view.setOnSingleClickListener(clickListener)
}

@BindingAdapter("navigationClick", requireAll = false)
fun navigationClick(view: Toolbar, clickListener: View.OnClickListener) {
    view.setNavigationOnClickListener(clickListener)
}


@BindingAdapter("displayChild", requireAll = false)
fun displayChild(view: ViewAnimator, childIndex: Int) {
    Logger.i("album+++size display=${childIndex}")
    view.displayedChild = childIndex
}

@BindingAdapter(value = ["setHtmlText"], requireAll = false)
fun setHtmlText(view: TextView, htmlTxt: String?) {
    htmlTxt?.let { view.text = it.fromHtml() }
}

@BindingAdapter(value = ["setTextFromHtml"], requireAll = false)
fun setTextFromHtml(view: TextView, htmlTxt: String?) {
    htmlTxt?.let { view.text = Html.fromHtml(htmlTxt) }
}


@BindingAdapter(value = ["setTextHtmlSpannable"], requireAll = false)
fun setTextHtmlSpannable(view: TextView, htmlTxt: String?) {
    if (htmlTxt == null) return
    view.post {
        view.text =
            HtmlStyleParser.parseHtml(
                html = htmlTxt,
                targetTextView = view,
                imgAlignmentEnum = ImgAlignmentEnum.CENTER,
                context = view.context
            )
    }
}

@BindingAdapter(value = ["setAnimDrawablePlay"], requireAll = false)
fun setAnimDrawablePlay(view: ImageView, play: Boolean?) {
    val animDrawable = view.drawable as? AnimationDrawable
    if (play == true) {
        animDrawable?.start()
    } else {
        animDrawable?.stop()
    }
}

@BindingAdapter("addItemDecoration")
fun addItemDecoration(recyclerView: RecyclerView, itemDecoration: RecyclerView.ItemDecoration?) {
    if (itemDecoration != null) {
        for (i in 0 until recyclerView.itemDecorationCount) {
            if (recyclerView.getItemDecorationAt(i) == itemDecoration) {
                return
            }
        }
        recyclerView.addItemDecoration(itemDecoration)
    }
}

@BindingAdapter("setItemDecoration")
fun setItemDecoration(recyclerView: RecyclerView, itemDecoration: RecyclerView.ItemDecoration?) {
    itemDecoration?.let {
        // 循环移除第一个装饰器，直到无剩余
        while (recyclerView.itemDecorationCount > 0) {
            recyclerView.removeItemDecorationAt(0)
        }
        recyclerView.addItemDecoration(it)
    }
}

@BindingAdapter("paddingHorizontal")
fun paddingHorizontal(v: View, value: Int) {
    v.setPadding(value, v.paddingTop, value, v.paddingBottom)
}

@BindingAdapter("paddingVertical")
fun paddingVertical(v: View, value: Int) {
    v.setPadding(v.paddingLeft, value, v.paddingRight, value)
}

@BindingAdapter("marginEnd")
fun marginEnd(v: View, value: Int) {
    var margin: MarginLayoutParams = v.layoutParams as MarginLayoutParams
    margin.marginEnd = value
}

@BindingAdapter("selected")
fun selected(v: View, value: Boolean) {
    v.isSelected = value
}

@BindingAdapter("autoFocus")
fun bindAutoFocus(editText: EditText, isFocused: Boolean) {
    if (isFocused) {
        editText.post {
            editText.requestFocus()
            // 把光标移动到末尾
            editText.setSelection(editText.text.length)
            // 弹出软键盘
            KeyboardUtils.showSoftInput(editText)
        }
    } else {
        editText.clearFocus()
        KeyboardUtils.hideSoftInput(editText)
    }
}

@BindingAdapter("marginBottomDp")
fun marginBottomDp(v: View, value: Int) {
    var layoutParams: MarginLayoutParams = v.layoutParams as MarginLayoutParams
    layoutParams.bottomMargin = value.dp

}

@BindingAdapter("marginLeftDp")
fun marginLeftDp(v: View, value: Float) {
    var layoutParams: MarginLayoutParams = v.layoutParams as MarginLayoutParams
    layoutParams.leftMargin = value.dp

}

@BindingAdapter("marginRightDp")
fun marginRightDp(v: View, value: Float) {
    var layoutParams: MarginLayoutParams = v.layoutParams as MarginLayoutParams
    layoutParams.rightMargin = value.dp

}

@BindingAdapter("marginBottomPx")
fun marginBottomPx(v: View, value: Int) {
    (v.layoutParams as? MarginLayoutParams)?.let {
        it.bottomMargin = value
        v.layoutParams = it
    }
}

@BindingAdapter("clipPaddingLeftDp", "clipPaddingRightDp", requireAll = false)
fun setClipPadding(view: ViewPager2, leftPadding: Int, rightPadding: Int) {
    view.setClipPadding(leftPadding.dp, rightPadding.dp)
}

@BindingAdapter(value = ["setEnabled"], requireAll = false)
fun setEnabled(view: View, isEnabled: Boolean) {
    view.isEnabled = isEnabled
}

@BindingAdapter(value = ["setClickable"], requireAll = false)
fun setClickable(view: View, isEnabled: Boolean) {
    view.isClickable = isEnabled
}

@BindingAdapter(value = ["setEnabled"], requireAll = false)
fun setEnabled(view: CounterInputLayout, isEnabled: Boolean) {
    view.isEnabled = isEnabled
    view.input.isEnabled = isEnabled
}

@BindingAdapter("setStrike")
fun setTrick(textView: TextView, antiAlias: Boolean) {
    if (antiAlias) {
        textView.paint.flags = Paint.STRIKE_THRU_TEXT_FLAG or Paint.ANTI_ALIAS_FLAG // 设置中划线并加清晰
    } else {
        textView.paint.flags = Paint.STRIKE_THRU_TEXT_FLAG; //中划线
    }
}

/**
 * 设置中划线
 * @param textView TextView 0:不设置中划线 1:设置中划线并加清晰 2:设置中划线
 * @param type Int
 */
@BindingAdapter("setStrikeType")
fun setStrikeType(textView: TextView, type: Int) {
    if (type == 0) {
        //不设置中划线
        textView.paint.flags = textView.paint.flags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
        return
    }
    if (type == 1) {
        // 设置中划线并加清晰
        textView.paint.flags = Paint.STRIKE_THRU_TEXT_FLAG or Paint.ANTI_ALIAS_FLAG
        return
    }
    //中划线
    textView.paint.flags = Paint.STRIKE_THRU_TEXT_FLAG
}


@BindingAdapter("marginTopDp")
fun marginTopDp(view: View, topMargin: Int) {
    val lp: MarginLayoutParams = view.layoutParams as MarginLayoutParams
    lp.topMargin = topMargin.dp

}

@BindingAdapter("marginTopPx")
fun marginTopPx(view: View, topMargin: Int) {
    (view.layoutParams as? MarginLayoutParams)?.let {
        it.topMargin = topMargin
        view.layoutParams = it
    }
}

@BindingAdapter("setTags")
fun setTags(tagLayout: TagLayout, tags: List<String?>?) {
    tagLayout.tags = tags
}

@BindingAdapter("setTags", "selectedTags", "setTagSelectMode", requireAll = false)
fun setTags(
    tagLayout: TagGridLayout,
    tags: List<String?>?,
    selected: List<String?>?,
    tagSelectMode: Int?
) {
    tagSelectMode?.let { tagLayout.setTagSelectMode(it) }
    tagLayout.tags = tags
    tagLayout.selectTags(selected)
}

interface OnSelectChangeListener {
    fun onSelectChange(index: Int, isSelected: Boolean)
}

@BindingAdapter("setOnSelectChangeListener")
fun setOnSelectChangeListener(tagLayout: TagGridLayout, listener: OnSelectChangeListener) {
    tagLayout.setOnSelectChangeListener { parent, child, index, isSelected ->
        listener.onSelectChange(index, isSelected)

    }

}

interface OnTagItemClickListener {
    fun onItemClick(index: Int, isChecked: Boolean): Boolean
}

@BindingAdapter("setOnItemClickListener")
fun setOnItemClickListener(tagLayout: TagGridLayout, listener: OnTagItemClickListener) {
    tagLayout.setOnItemClickListener { _, index ->
        listener.onItemClick(
            index,
            tagLayout.selectedTagPositions.contains(index)
        )
    }

}

@BindingAdapter("setHorizontalBias")
fun setHorizontalBias(view: View, bias: Float?) {
    bias ?: return
    view.updateLayoutParams<ConstraintLayout.LayoutParams> {
        it.horizontalBias = bias
    }
}

@BindingAdapter("setVerticalBias")
fun setVerticalBias(view: View, bias: Float?) {
    bias ?: return
    view.updateLayoutParams<ConstraintLayout.LayoutParams> {
        it.verticalBias = bias
    }
}

@BindingAdapter("setDimensionRatio")
fun setDimensionRatio(view: View, ratio: String?) {
    ratio ?: return
    view.updateLayoutParams<ConstraintLayout.LayoutParams> { it.dimensionRatio = ratio }
}

@BindingAdapter("totalItemCount", "lines")
fun setGridSpanCount(view: RecyclerView, totalItemCount: Int, lines: Int) {
    val spanCount = totalItemCount / lines + (if (totalItemCount % lines == 0) 0 else 1)
    view.layoutManager = GridLayoutManager(view.context, spanCount.coerceAtLeast(1))
}

@BindingAdapter("playOnce")
fun playOnce(view: LottieAnimationView, visible: Boolean) {
    if (visible) {
        view.visibility = View.VISIBLE
        view.progress = 0f
        view.removeAllAnimatorListeners()
        view.addAnimatorListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                view.visibility = View.GONE
            }

            override fun onAnimationCancel(animation: Animator) {
                super.onAnimationCancel(animation)
                view.visibility = View.GONE
            }
        })
        view.playAnimation()
    } else {
        view.cancelAnimation()
        view.visibility = View.GONE
    }
}

@BindingAdapter("setTextBold")
fun setTextBold(view: TextView, bold: Boolean) {
    view.typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
}

@BindingAdapter("setTextSizeSp")
fun setTextSizeSp(view: TextView, sp: Float) {
    view.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp)
}

@BindingAdapter("setTextSizeDp")
fun setTextSizeDp(view: TextView, dp: Float) {
    view.setTextSize(TypedValue.COMPLEX_UNIT_DIP, dp)
}

@BindingAdapter("setSelected")
fun setSelected(view: View, isSelected: Boolean) {
    view.isSelected = isSelected
}

@SuppressLint("SetJavaScriptEnabled")
@BindingAdapter("initUrl")
fun initUrl(webView: WebView, url: String?) {
    webView.settings.javaScriptEnabled = true
    webView.settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
    if (App.instance.hasAgree()) {
        val userAgent: String =
            webView.settings.userAgentString + " Networking Gang/" + Session.getInstance().versionName
        webView.settings.allowFileAccessFromFileURLs = true
        webView.settings.allowUniversalAccessFromFileURLs = true
        webView.settings.userAgentString = userAgent
        webView.settings.cacheMode = WebSettings.LOAD_DEFAULT //根据cache-control决定是否从网络上取数据。
        webView.settings.domStorageEnabled = true
        webView.settings.allowFileAccess = true
    }
    webView.loadUrl(url ?: "")
}

@BindingAdapter("showAndShake")
fun showAndShake(view: View, show: Long) {
    if (show > 0) {
        view.visibility = View.VISIBLE
        view.shake()
    } else {
        view.visibility = View.GONE
    }
}

@BindingAdapter("setLoadState")
fun setLoadState(view: LoadStateViewHolder, state: LoadState?) {
    when (state) {
        is LoadState.Error -> if (state.endOfPaginationReached) view.showRetry(state.error)
        is LoadState.NotLoading -> view.showContent()
        is LoadState.Loading -> view.showLoading()
        is LoadState.Empty -> view.showEmpty()
        null -> view.showLoading()
    }
}

@BindingAdapter("setTopPaddingDp")
fun setTopPadding(view: LoadStateViewHolder, topPaddingDp: Int) {
    view.setGravityHorizontalTop(topPaddingDp.dp)
}

@BindingAdapter("setPaddingBottomDp")
fun setPaddingBottomDp(view: View, padding: Int) {
    view.setPadding(view.paddingLeft, view.paddingTop, view.paddingRight, padding.dp)
}

@BindingAdapter("setPaddingBottomPx")
fun setPaddingBottomPx(view: View, padding: Int) {
    view.setPadding(view.paddingLeft, view.paddingTop, view.paddingRight, padding)
}

@BindingAdapter("onRetry")
fun onRetry(view: LoadStateViewHolder, onRetry: (() -> Unit)?) {
    view.setRetryListener {
        onRetry?.invoke()
    }
}

@BindingAdapter("setPullHeader")
fun setPullHeader(view: CoolRefreshView, header: PullHeader?) {
    header?.let {
        view.setPullHeader(header)
    }
}

@BindingAdapter("onRefresh")
fun onRefresh(view: SwipeRefreshLayout, onRefresh: (() -> Unit)?) {
    view.setOnRefreshListener {
        onRefresh?.invoke()
    }
}

@BindingAdapter("setRefreshColor")
fun setRefreshColor(view: SwipeRefreshLayout, color: Int) {
    view.setColorSchemeColors(color)
}

@BindingAdapter("setLoadState")
fun setLoadState(view: SwipeRefreshLayout, state: LoadState?) {
    Logger.i("set load state=$state")
    when (state) {
        is LoadState.Error -> view.isRefreshing = false
        is LoadState.NotLoading -> view.isRefreshing = false
        is LoadState.Loading -> {}
        LoadState.Empty -> view.isRefreshing = false
        null -> {}
    }
}

@BindingAdapter("onRefresh")
fun onRefresh(view: CoolRefreshView, onRefresh: (() -> Unit)?) {
    view.addOnPullListener(object : SimpleOnPullListener() {
        override fun onRefreshing(p0: CoolRefreshView?) {
            onRefresh?.invoke()
        }
    })
}

@BindingAdapter("setLoadState")
fun setLoadState(view: CoolRefreshView, state: LoadState?) {
    Logger.i("set load state=$state")
    when (state) {
        is LoadState.Error -> view.isRefreshing = false
        is LoadState.NotLoading -> view.isRefreshing = false
        is LoadState.Loading -> {}
        LoadState.Empty -> view.isRefreshing = false
        null -> {}
    }
}

@SuppressLint("ResourceType")
@BindingAdapter(
    "setSpanText",
    "spanText",
    "spanColor",
    "spanTextSizeSp",
    "spanBold",
    requireAll = false
)
fun setSpanText(
    view: TextView,
    content: String?,
    spanText: String?,
    spanTextSizeSp: Int?,
    spanColor: Int?,
    bold: Boolean?
) {
    view.setColoredText(
        content ?: "",
        spanText ?: "",
        spanTextSizeSp ?: 12,
        spanColor ?: R.color.color_ff4848.getResColor(),
        bold ?: false
    )
}

@BindingAdapter("setAutoFillScreen", "offsetOfDp", requireAll = false)
fun setAutoFillScreen(view: View, autoFill: Boolean, offsetOfDp: Int? = 0) {
    view.post {
        if (autoFill) {
            val location = IntArray(2)
            view.getLocationOnScreen(location)
            view.minimumHeight = 0.coerceAtLeast(
                ScreenUtils.getAppScreenHeight() - location[1] - (offsetOfDp?.dp
                    ?: 0) - ViewUtils.getNavigationBarHeight(view.context)
            )
        }
    }
}

/**
 * 设置LottieAnimationView动画开关
 * @param view LottieAnimationView
 * @param isAnimations Boolean
 */
@BindingAdapter("setLottieAnimationViewAnimations")
fun setLottieAnimationViewAnimations(view: LottieAnimationView, isAnimations: Boolean) {
    view.post {
        if (isAnimations) {
            //继续成头开始播放动画
            view.playAnimation()
        } else {
            //取消动画播放
            view.cancelAnimation()
        }
    }
}

/**
 * 设置LottieAnimationView动画开关
 * @param view LottieAnimationView
 * @param isAnimations Boolean
 */
@BindingAdapter("setCheckChangeListener")
fun setCheckChangeListener(
    view: RadioGroup,
    callback: ((v: ViewGroup, checkedId: Int) -> Unit)? = null
) {
    view.setOnCheckedChangeListener { v, checkedId -> callback?.invoke(v, checkedId) }
}


/**
 * 设置图片高斯模糊
 */
@BindingAdapter("gaussianAmbiguity")
fun gaussianAmbiguity(view: ImageView, path: String?) {
    GlideApp.with(view.context)
        .load(path) // 加载图片的Uri
        .apply(RequestOptions.bitmapTransform(BlurTransformation(view.context))) // 应用高斯模糊变换
        .dontAnimate()
        .fitCenter()
        .into(view) // 将图片设置到ImageView上
}


interface OnTagClickListener {
    fun onTagClick(tag: String)
}

@BindingAdapter("setOnTagClickListener")
fun setOnTagClickListener(
    view: TagLayout,
    onTagClickListener: OnTagClickListener
) {
    view.setOnItemClickListener { _, index ->
//        callback?.invoke(view.tags[index])
        onTagClickListener.onTagClick(view.tags[index])
        true
    }
}

@BindingAdapter("setOnTagClickListener")
fun setOnTagClickListener(
    view: TagGridLayout,
    onTagClickListener: OnTagClickListener
) {
    view.setOnItemClickListener { _, index ->
//        callback?.invoke(view.tags[index])
        onTagClickListener.onTagClick(view.tags[index])
        true
    }
}

val client by lazy { OkHttpClient() }


interface OnColorSetListener {
    public fun onColorSet(color: Int) {

    }
}

@BindingAdapter("setBackgroundColorUrl", "callback", requireAll = false)
fun setBackgroundColorUrl(view: View, url: String?, onColorSetListener: OnColorSetListener?) {
    if (url != null) {
        OssHelp.fetchImageColor(url) {
            if (it != null) {
                onColorSetListener?.onColorSet(it)
                view.setBackgroundColor(it)
            } else {
                view.getActivity()?.let {
                    ImageColorExtractionUtils.getColorBackground(it, url) { bg ->
                        view.setBackgroundDrawable(BitmapDrawable(bg))
                    }
                }
            }
        }
    } else {
        view.setBackgroundColor(Color.TRANSPARENT)
    }
}

@BindingAdapter("setWithPercent")
fun setWithPercent(view: View, percent: Float) {
    view.updateLayoutParams<ViewGroup.LayoutParams> {
        it.width = (ScreenUtils.getScreenWidth() * percent).toInt()
    }
}

@BindingAdapter("setConstrainWithPercent")
fun setConstrainWithPercent(view: View, percent: Float?) {
    percent ?: return
    view.updateLayoutParams<ConstraintLayout.LayoutParams> {
        it.matchConstraintPercentWidth = percent.inRange(0f, 1f)
    }
}

@BindingAdapter("setAutoPlay", requireAll = false)
fun setAutoPlay(pager: BannerViewPager<*>, autoPlay: Boolean) {
    pager.setAutoPlay(autoPlay)
    if (autoPlay) {
        pager.startLoop()
    } else {
        pager.stopLoop()
    }
}


interface OnPageChangeListener {
    public fun onPageChange(index: Int) {

    }
}

@BindingAdapter("addPagerListener")
fun addPagerListener(pager: BannerViewPager<*>, callback: OnPageChangeListener?) {
    pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            super.onPageSelected(position)
            callback?.onPageChange(position)

        }

    })
}


@BindingAdapter("setCropImageFile")
fun setCropImageFile(cropView: UCropView, file: File?) {
    if (file?.exists() != true) {
        cropView.cropImageView.setImageBitmap(null)
        return
    }
    cropView.cropImageView.setImageUri(
        Uri.fromFile(file),
        Uri.fromFile(File(FileManager.getInstance().imageFolder, "${System.nanoTime()}.png"))
    )
}

@BindingAdapter("expandContent", "highlightContent", "onSpanClick", requireAll = false)
fun expandContent(
    textView: TextView,
    text: String?,
    highlightContent: String?,
    clickListener: View.OnClickListener?
) {
    if (highlightContent?.isNotEmpty() == true) {
        val color = Color.parseColor("#BDD3FF")
        val spanText = SpannableString(text)
        val startIndex: Int? = text?.indexOf(highlightContent)
        if (startIndex != null && startIndex != -1) {
            spanText.setSpan(
                ForegroundColorSpan(color), startIndex,
                startIndex + highlightContent.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            //设置icon可点击
            val clickableSpan = object : ClickableSpan() {
                override fun onClick(widget: View) {//点击事件
                    clickListener?.onClick(textView)
                }

                override fun updateDrawState(ds: TextPaint) {//不设置颜色和下划线
                }
            }
            spanText.setSpan(
                clickableSpan,
                startIndex,
                startIndex + highlightContent.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        textView.text = spanText
    } else {
        textView.text = text ?: "一起来听听我创作的歌曲吧"
    }
}


@BindingAdapter("expandContent", "highlightContent", "onSpanClick", requireAll = false)
fun expandContent(
    textView: ExpandableTextView,
    text: String?,
    highlightContent: String?,
    clickListener: View.OnClickListener?
) {

    if (highlightContent?.isNotEmpty() == true) {
        val color = Color.parseColor("#FF4B00")
        val spanText = SpannableString(text)
        val startIndex: Int? = text?.indexOf(highlightContent)
        if (startIndex != null && startIndex != -1) {
            spanText.setSpan(
                ForegroundColorSpan(color), startIndex,
                startIndex + highlightContent.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            //设置icon可点击
            val clickableSpan = object : ClickableSpan() {
                override fun onClick(widget: View) {//点击事件
                    clickListener?.onClick(textView)
                }

                override fun updateDrawState(ds: TextPaint) {//不设置颜色和下划线
                }
            }
            spanText.setSpan(
                clickableSpan,
                startIndex,
                startIndex + highlightContent.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        textView.text = spanText
    } else {
        textView.text = text ?: R.string.enter_content.getResString()
    }
}

fun isVideoUrl(url: String): Boolean {
    val videoExtensions = listOf("mp4", "mov", "avi", "mkv", "flv", "wmv")
    // 检查URL是否包含视频格式后缀
    val hasVideoExtension = videoExtensions.any { url.endsWith(it, ignoreCase = true) }
    return hasVideoExtension
}

@BindingAdapter("loadVideoUrl", "placeHolder", requireAll = false)
fun loadVideoCover(coverImageView: ImageView, videoUrl: String?, placeHolder: Drawable? = null) {
    GlideApp.with(coverImageView)
        .asBitmap()
        .load(videoUrl)
        .apply(
            RequestOptions.frameOf(0L)
                .diskCacheStrategy(DiskCacheStrategy.ALL) // 缓存首帧到磁盘
        )
        .placeholder(placeHolder)
        .transition(GenericTransitionOptions.with(com.nhsd.basic.R.anim.fade_in)) // 淡入动画
        .into(coverImageView)
}


@BindingAdapter("setBackgroundColorAlpha", "backgroundColorAlpha", requireAll = false)
fun setBackgroundColorAlpha(view: View, color: Int, alpha: Float) {
    view.setBackgroundColor(ColorUtils.setAlphaComponent(color, alpha))
}

@SuppressLint("ClickableViewAccessibility")
@BindingAdapter("setDrawableEndClickListener", requireAll = false)
fun setDrawableEndClickListener(view: TextView, listener: (() -> Unit)?) {
    if (listener == null) {
        view.setOnTouchListener(null)
        return
    }
    view.setOnTouchListener { v, event ->
        val drawableEnd = view.compoundDrawables[2]
        if (drawableEnd != null) {
            // 获取点击位置相对于TextView左上角的坐标
            val x = event.x

            // 获取drawable的边界
            val bounds = drawableEnd.getBounds()
            val drawableX = view.width - bounds.width()
            if (event.action == MotionEvent.ACTION_DOWN) {
                // 检查点击是否在drawable的范围内
                if (x > drawableX) {
                    // 点击事件处理
                    return@setOnTouchListener true // 事件已被处理，不再向下传递
                }
            } else if (event.action == MotionEvent.ACTION_UP) {
                // 检查点击是否在drawable的范围内
                if (x > drawableX) {
                    // 点击事件处理
                    listener?.invoke()
                }
            }
        }
        false // 事件未被处理，继续向下传递
    }
}

@BindingAdapter("setTextStartWithImage", "image", "imageWidth", "imageHeight", requireAll = true)
fun setTextStartWithImage(
    view: ExpandableTextView,
    text: String?,
    image: Drawable,
    imageWidth: Int,
    imageHeight: Int
) {
    "[start]  ${text ?: ""}".replaceSpan("[start]") {
        image.let { CenterImageSpan(it).setDrawableSize(imageWidth.dp, imageHeight.dp) }
    }.let { view.text = it }
}

@BindingAdapter("setRotateCenter", "pivotY", requireAll = false)
fun setRotateCenter(view: View, pivotX: Float, pivotY: Float) {
    view.post {
        view.pivotX = view.width * pivotX
        view.pivotY = view.height * pivotY
    }
}

@BindingAdapter(
    "setImageDrawableFitHeight",
    "fitHeightDp",
    requireAll = true
)
fun setImageDrawableFitHeight(
    imageView: ImageView,
    drawable: Drawable?,
    fitHeightDp: Int = 0
) {
    imageView.setImageDrawable(drawable)
    if (drawable == null) {
        //imageView.visibility = View.GONE
    } else {
        //imageView.visibility = View.VISIBLE
        imageView.updateLayoutParams<ViewGroup.LayoutParams> {
            it.height = fitHeightDp.dp
            it.width = (1f * drawable.intrinsicWidth / drawable.intrinsicHeight * it.height).toInt()
        }
    }
}

@BindingAdapter("setTextAndRequestLayout")
fun setTextAndRequestLayout(textView: TextView, text: CharSequence?) {
    textView.text = text
    textView.requestLayout()
}


@BindingAdapter("setLottieUrl")
fun setLottieUrl(view: LottieAnimationView, url: String?) {
    if (!url.isNullOrEmpty()) {
        view.setAnimationFromUrl(url)
    }
}

@BindingAdapter("setMinHeightMatchParent")
fun setMinHeightMatchParent(view: TextView, value: Boolean) {
    if (value) {
        val parent = view.parent as? View ?: return
        parent.post {
            view.minHeight = parent.height
        }
    }
}

@BindingAdapter("bindToLifeCycle")
fun bindToLifeCycle(view: MarqueeTextView, lifecycle: Lifecycle?) {
    lifecycle?.let { view.bindToLifeCycle(it) }
}

@BindingAdapter("changeText")
fun changeText(view: MarqueeTextView, text: CharSequence?) {
    view.setMarqueeText(text)
}

@BindingAdapter("startMarquee")
fun startMarquee(view: MarqueeTextView, start: Boolean) {
    if (start) {
        view.startScroll()
    } else {
        view.pauseScroll()
    }
}

/**
 * @param progress 0~1 0无位移 1向上位移控件高度
 */
@BindingAdapter("translationUpPercent")
fun translationUpPercent(view: View, progress: Float) {
    view.translationY = -view.height * progress
}


@BindingAdapter("srlFinishRefresh")
fun srlFinishRefresh(srl: SmartRefreshLayout, turnOffRefresh: Boolean = false) {
    if (turnOffRefresh) return
    //关闭下拉刷新状态
    srl.finishRefresh()
}

@BindingAdapter("vdViewColor")
fun bindVdvColor(view: VerticalDashedLineView, colorRes: Int) {
    view.setVdvColor(colorRes)
}

@SuppressLint("SetTextI18n")
@BindingAdapter("setIncreaseAnimatedValue", "setSuffix", requireAll = false)
fun setIncreaseAnimatedValue(textView: TextView, value: String?, suffix: String?) {
    value?.let {
        // 延迟一小段时间确保视图已布局
        textView.post {
            value.toIntOrNull()
                ?.let { it1 -> NumberAnimator.animateTextView(textView, it1, suffix ?: "") }
                ?: run {
                    textView.text = "0${suffix ?: ""}"
                }
        }
    } ?: run {
        textView.text = "0${suffix ?: ""}"
    }
}

@BindingAdapter(value = ["loadImageWithBlurBackground"], requireAll = false)
fun loadImageWithBlurBackground(parent: ViewGroup, imageUrl: String?) {
    val ivBlurBackground = parent.findViewById<ImageView>(R.id.iv_blur_background)
    val ivBlurSrc = parent.findViewById<ImageView>(R.id.iv_blur_src)
    ivBlurBackground.setImageBitmap(null)
    val options = BlurBackgroundOptionsBuilder(
        ivBlurSrc,
        ivBlurBackground,
        imageUrl
    )
        .setUseThumbnail(true)
        .setDefaultResId(R.drawable.img_default_cover_1_1)
        .setBlurRadius(8)
        .setBlurSampling(2)
        .build()
    setBlurBackground(options)
}

@BindingAdapter("animateVisibility")
fun View.animateVisibility(visible: Boolean) {
    if (visible && isVisible && alpha == 1f) return
    if (!visible && isGone) return

    animate().cancel()

    if (visible) {
        alpha = 0f
        visibility = View.VISIBLE
        animate()
            .alpha(1f)
            .setDuration(500)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()
    } else {
        animate()
            .alpha(0f)
            .setDuration(200)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction {
                visibility = View.GONE
                alpha = 1f
            }
            .start()
    }
}

/**
 * @param visible true 显示 false 隐藏
 */
@BindingAdapter("setVisible")
fun setVisible(view: View, visible: Boolean) {
    view.visibility = if (visible) View.VISIBLE else View.GONE
}

/**
 * 按宽度计算设置padding
 * @param screenWith true 按屏幕宽度计算 false 按控件宽度计算
 */
@BindingAdapter("setPaddingByWidth", "left", "top", "right", "bottom", requireAll = false)
fun setPaddingByWidth(view: View, screenWith: Boolean, left: Float = 0f, top: Float = 0f, right: Float = 0f, bottom: Float = 0f) {
    view.post {
        val width = if (screenWith) min(ScreenUtils.getScreenWidth(), ScreenUtils.getScreenHeight()) else view.width
        view.setPadding((width * left).toInt(), (width * top).toInt(), (width * right).toInt(), (width * bottom).toInt())
    }
}

/**
 * 按宽度计算最大高度
 * @param screenWith true 按屏幕宽度计算 false 按控件宽度计算
 */
@BindingAdapter("setConstraintMaxHeightByWidth", "hwRatio", requireAll = false)
fun setConstraintMaxHeightByWidth(view: View, screenWith: Boolean, hwRatio: Float = 0f) {
    if (screenWith) {
        val width = min(ScreenUtils.getScreenWidth(), ScreenUtils.getScreenHeight())
        view.updateLayoutParams<ConstraintLayout.LayoutParams> {
            it.matchConstraintMaxHeight = (width * hwRatio).toInt()
        }
    } else {
        view.post {
            val width = view.width
            view.updateLayoutParams<ConstraintLayout.LayoutParams> {
                it.matchConstraintMaxHeight = (width * hwRatio).toInt()
            }
        }
    }
}

@BindingAdapter("fade")
fun fade(view: View, _in: Boolean) {
    (view.getTag(R.id.viewAnimator) as? Animator)?.cancel()
    if (_in) {
        view.visibility = View.VISIBLE
        ObjectAnimator.ofFloat(view, "alpha", 0f, 1f).apply {
            duration = 250
            interpolator = LinearInterpolator()
            view.setTag(R.id.viewAnimator, this)
        }.start()
    } else {
        if (view.visibility != View.VISIBLE) return
        ObjectAnimator.ofFloat(view, "alpha", 1f, 0f).apply {
            addListener(onEnd = { view.visibility = View.GONE })
            duration = 250
            interpolator = LinearInterpolator()
            view.setTag(R.id.viewAnimator, this)
        }.start()
    }
}

@BindingAdapter("pop", "centerX", "centerY")
fun pop(view: View, _in: Boolean, @FloatRange(from = 0.0, to = 1.0) centerX: Float = 0.5f, @FloatRange(from = 0.0, to = 1.0) centerY: Float = 1f) {
    (view.getTag(R.id.viewAnimator) as? Animator)?.cancel()
    if (_in) {
        view.visibility = View.INVISIBLE
        view.post {
            view.pivotX = view.width * centerX
            view.pivotY = view.height * centerY
            val xAnimator = ObjectAnimator.ofFloat(view, "scaleX", 0f, 1f)
            val yAnimator = ObjectAnimator.ofFloat(view, "scaleY", 0f, 1f)
            view.visibility = View.VISIBLE
            AnimatorSet().apply {
                duration = 250
                interpolator = LinearInterpolator()
                playTogether(xAnimator, yAnimator)
                view.setTag(R.id.viewAnimator, this)
            }.start()
        }
    } else {
        if (view.visibility != View.VISIBLE) return
        val xAnimator = ObjectAnimator.ofFloat(view, "scaleX", 1f, 0f)
        val yAnimator = ObjectAnimator.ofFloat(view, "scaleY", 1f, 0f)
        AnimatorSet().apply {
            addListener(onEnd = { view.visibility = View.GONE })
            duration = 250
            interpolator = LinearInterpolator()
            playTogether(xAnimator, yAnimator)
            view.setTag(R.id.viewAnimator, this)
        }.start()
    }
}

@BindingAdapter("setInputDecimal", "maxIntegerDigits", "maxDecimalDigits")
fun setInputDecimal(editText: EditText, decimal: Boolean, maxIntegerDigits: Int, maxDecimalDigits: Int) {
    if (decimal) {
        editText.filters = arrayOf(DecimalInputFilter(maxIntegerDigits, maxDecimalDigits))
    } else {
        editText.filters = editText.filters.filterNot { it is DecimalInputFilter }.toTypedArray()
    }
}

@BindingAdapter("blurWith")
fun blurWith(blurView: BlurView, targetId: Int) {
    (blurView.parent as? View)?.findViewById<BlurTarget>(targetId)?.let {
        blurView.setupWith(it).setBlurRadius(5f) // 初始模糊半径（0-25）
    }
}

@BindingAdapter("foregroundLevel")
fun foregroundLevel(view: View, level: Int?) {
    if (level != null) {
        view.foreground?.level = level
    }
}


@BindingAdapter("setCoverUrl", "setCoverHeight", "placeHolder", requireAll = false)
fun setCoverUrl(
    view: CoverPlayerView,
    url: String?,
    coverHeight: Int?,
    placeHolder: Drawable? = null
) {
    coverHeight?.let {
        view.setCoverHeight(it)
    }
    val coverImageView = view.getCoverImage()
    if (coverImageView == null || url.isNullOrEmpty()) {
        // 如果封面或 URL 不存在，可选择清空 ImageView 或设置默认图
        coverImageView?.setImageDrawable(placeHolder)
        return
    }
    if (isVideoUrl(url)) {
        // 加载视频首帧
        GlideApp.with(coverImageView)
            .asBitmap()
            .load(url)
            .apply(
                RequestOptions.frameOf(0L)
                    .diskCacheStrategy(DiskCacheStrategy.ALL) // 缓存首帧到磁盘
            )
            .placeholder(placeHolder)
            .transition(GenericTransitionOptions.with(com.nhsd.basic.R.anim.fade_in)) // 淡入动画
            .into(coverImageView)
    } else {
        GlideImageLoader.with(coverImageView).hdThumbnail().display(url)
    }
}

@SuppressLint("ClickableViewAccessibility")
@BindingAdapter(value = ["setEditing", "content"], requireAll = true)
fun setEditing(view: RecyclerEditText, editing: Boolean, content: CharSequence?) {
    view.setText(content)
    //开启滚动
    view.movementMethod = ScrollingMovementMethod.getInstance()
    view.setOnTouchListener { v, event ->
        if (v.canScrollVertically(1) || v.canScrollVertically(-1)) {
            // 告诉父控件（及祖父控件等）：不要拦截这个触摸事件，让我自己处理
            v.parent.requestDisallowInterceptTouchEvent(true)
            // 当手指抬起或取消触摸时，恢复父控件的拦截权
            if (event.action == MotionEvent.ACTION_UP || event.action == MotionEvent.ACTION_CANCEL) {
                v.parent.requestDisallowInterceptTouchEvent(false)
            }
        }
        // 返回 false，表示不消费此事件，让 EditText 原本的触摸逻辑继续执行（比如光标移动、弹出键盘等）
        false
    }
    if (editing) {
        // 开启编辑模式
        view.isFocusable = true
        view.isFocusableInTouchMode = true
        KeyboardUtils.showSoftInput(view)
        view.setSelection(view.length())
    } else {
        // 禁用焦点
        view.isFocusable = false
        view.isFocusableInTouchMode = false
        view.clearFocus()
        KeyboardUtils.hideSoftInput(view)
    }
}

@BindingAdapter("rotateLoading")
fun setRotateLoading(view: ImageView, show: Boolean) {
    if (show) {
        view.visibility = View.VISIBLE
        // 检查当前是否有动画在运行，防止重复启动导致闪烁
        if (view.animation == null) {
            val rotateAnim = AnimationUtils.loadAnimation(view.context, R.anim.loading_2)
            view.startAnimation(rotateAnim)
        }
    } else {
        view.visibility = View.GONE
        view.clearAnimation()
    }
}

@BindingAdapter("isScrollable")
fun setScrollable(view: NestedScrollView, isScrollable: Boolean) {
    view.setOnTouchListener { _, _ ->
        // 如果 isScrollable 为 false，返回 true 消费事件，阻止滚动
        // 如果 isScrollable 为 true，返回 false 不处理事件，交给原生逻辑
        !isScrollable
    }
}

@BindingAdapter("isVisibleWithAnimation")
fun isVisibleWithAnimation(view: LottieAnimationView, isVisible: Boolean) {
    if (isVisible) {
        view.visibility = View.VISIBLE
        view.playAnimation()
    } else {
        view.visibility = View.GONE
        view.cancelAnimation() // 彻底停止，不占 CPU
    }
}

@BindingAdapter("titleText", "highlightKeyword", requireAll = false)
fun TextView.setTitleWithHighlight(text: String?, keyword: String?) {
    if (!keyword.isNullOrEmpty() && !text.isNullOrEmpty()) {
        val spanText = SpannableString(text)
        val startIndex = text.indexOf(keyword, ignoreCase = true)
        if (startIndex != -1) {
            spanText.setSpan(
                ForegroundColorSpan(Color.parseColor("#FF2E55")),
                startIndex,
                startIndex + keyword.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        this.text = spanText
    } else {
        this.text = text
    }
}
