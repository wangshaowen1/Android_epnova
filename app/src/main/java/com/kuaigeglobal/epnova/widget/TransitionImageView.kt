package com.kuaigeglobal.epnova.widget

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.TransitionDrawable
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.kuaigeglobal.epnova.R

class TransitionImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    // 控件状态枚举
    enum class PlayState {
        IDLE,       // 空闲
        PLAYING,    // 播放中
        PAUSED,     // 暂停
        STOPPED     // 停止
    }

    // 播放状态监听器
    interface OnPlayStateListener {
        fun onPlayStart()
        fun onPlayPause()
        fun onPlayResume()
        fun onPlayStop()
        fun onImageChanged(position: Int, total: Int)
        fun onPlayCompleted()
        fun onSeekAnimationStart(fromPosition: Int, toPosition: Int) // 新增：跳转动画开始
        fun onSeekAnimationEnd(toPosition: Int) // 新增：跳转动画结束
    }

    // 封装加载的图片数据
    private data class LoadedImage(
        val drawable: Drawable,
        val url: String
    )

    // 配置参数（现在从XML属性初始化）
    private var staticDisplayTime: Long = 3000      // 静态显示时间（毫秒）
    private var transitionDuration: Int = 1000      // 过渡动画时间（毫秒）
    private var placeholderResId: Int = 0           // 占位图资源ID
    private var errorResId: Int = 0                 // 错误图资源ID
    private var autoPlay: Boolean = false            // 是否自动播放
    private var loop: Boolean = true                // 是否循环播放

    // 成员变量
    private val imageView: ImageView
    private val handler = Handler(Looper.getMainLooper())

    private var imageUrls: List<String> = emptyList()
    private val loadedImages = mutableListOf<LoadedImage?>()
    private val loadTargets = mutableMapOf<Int, CustomTarget<Drawable>>()
    private var requestManager: RequestManager? = null
    private var loadGeneration = 0
    private var loadNextRunnable: Runnable? = null
    private var currentIndex = 0
    private var currentState = PlayState.IDLE
    private var isPreloading = false
    private var playStateListener: OnPlayStateListener? = null
    private var isReleased = false
    private var isSeeking = false // 是否正在执行跳转动画
    private var maxCachedImages = 0
    private var windowOrder: List<Int> = emptyList()
    private var windowReady: (() -> Unit)? = null

    /**
     * 0 保持整组预加载；至少 3 开启有界缓存（当前图、目标图及预取图）。
     * 有界模式 seek 未缓存位置时异步加载，完成后沿用原有跳转回调。
     * 限制的是本控件持有的资源数，不修改 Glide 全局缓存。
     */
    fun setMaxCachedImages(count: Int) {
        require(count == 0 || count >= 3) { "Cache size must be 0 or at least 3" }
        if (isReleased || maxCachedImages == count) return
        cancelSeek()
        maxCachedImages = count
        releaseLoadedImages()
        preloadAllImages()
    }

    // 播放控制Runnable
    private val transitionRunnable = Runnable { performTransition() }

    init {
        // 初始化ImageView
        imageView = AppCompatImageView(context).apply {
            layoutParams = LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT
            )
            scaleType = ImageView.ScaleType.FIT_CENTER
            adjustViewBounds = true
        }
        addView(imageView)

        // 从XML属性初始化配置
        initAttributes(context, attrs)
    }

    /**
     * 从XML属性初始化配置
     */
    private fun initAttributes(context: Context, attrs: AttributeSet?) {
        attrs?.let { attributeSet ->
            val typedArray = context.obtainStyledAttributes(
                attributeSet,
                R.styleable.TransitionImageView,
                0,
                0
            )

            try {
                staticDisplayTime = typedArray.getInt(
                    R.styleable.TransitionImageView_staticDisplayTime,
                    3000
                ).toLong().coerceAtLeast(1L)

                transitionDuration = typedArray.getInt(
                    R.styleable.TransitionImageView_transitionDuration,
                    1000
                ).coerceAtLeast(0)

                placeholderResId = typedArray.getResourceId(
                    R.styleable.TransitionImageView_placeholder,
                    0
                )

                errorResId = typedArray.getResourceId(
                    R.styleable.TransitionImageView_errorImage,
                    0
                )

                autoPlay = typedArray.getBoolean(
                    R.styleable.TransitionImageView_autoPlay,
                    false
                )

                loop = typedArray.getBoolean(
                    R.styleable.TransitionImageView_loop,
                    true
                )
            } finally {
                typedArray.recycle()
            }
        }
    }

    /**
     * 设置图片URL列表
     */
    fun setImageUrls(urls: List<String?>) {
        if (isReleased) return

        val newUrls = urls.filterNotNull()
        if (newUrls == imageUrls) return

        val generation = loadGeneration
        stop()
        if (isReleased || generation != loadGeneration) return
        releaseLoadedImages()
        imageUrls = newUrls
        currentIndex = 0
        currentState = PlayState.IDLE
        isSeeking = false

        if (imageUrls.isNotEmpty()) {
            preloadAllImages()
        } else {
            imageView.setImageDrawable(null)
        }
    }

    /**
     * 设置图片URL数组（可变参数）
     */
    fun setImageUrls(vararg urls: String) {
        setImageUrls(urls.toList())
    }

    /**
     * 设置播放状态监听器
     */
    fun setOnPlayStateListener(listener: OnPlayStateListener) {
        this.playStateListener = listener
    }

    /**
     * 开始播放
     */
    fun play() {
        if (currentState == PlayState.PLAYING || isReleased || imageUrls.isEmpty()) return
        val previousState = currentState
        if (previousState == PlayState.STOPPED) {
            currentIndex = 0
            imageView.setImageDrawable(loadedImages.getOrNull(0)?.drawable)
        }
        currentState = PlayState.PLAYING
        val generation = loadGeneration
        if (previousState == PlayState.PAUSED) {
            playStateListener?.onPlayResume()
        } else {
            playStateListener?.onPlayStart()
        }
        if (generation != loadGeneration || isReleased || currentState != PlayState.PLAYING) return
        if (maxCachedImages > 0) {
            prepareWindow(currentIndex) { finishPreloading() }
            return
        }
        if (!isPreloading && loadedImages.getOrNull(currentIndex) != null) {
            playStateListener?.onImageChanged(currentIndex, imageUrls.size)
        }
        scheduleNextTransition()
    }

    /**
     * 暂停播放
     */
    fun pause() {
        if (currentState != PlayState.PLAYING) return

        handler.removeCallbacks(transitionRunnable)
        currentState = PlayState.PAUSED
        playStateListener?.onPlayPause()
    }

    /**
     * 停止播放
     */
    fun stop() {
        handler.removeCallbacks(transitionRunnable)
        windowReady = null
        cancelSeek()
        if (currentState == PlayState.STOPPED || isReleased) return
        currentState = PlayState.STOPPED
        playStateListener?.onPlayStop()
    }

    /**
     * 释放资源
     */
    fun release() {
        if (isReleased) return

        isReleased = true
        handler.removeCallbacksAndMessages(null)
        isSeeking = false
        currentState = PlayState.STOPPED
        imageUrls = emptyList()

        imageView.setImageDrawable(null)
        releaseLoadedImages()
        playStateListener = null
    }

    /**
     * 释放已加载的图片资源
     */
    private fun releaseLoadedImages() {
        // 先失效旧回调并解除显示引用，再交还 Glide 资源，避免绘制已回收的 Bitmap。
        loadGeneration++
        isPreloading = false
        windowReady = null
        windowOrder = emptyList()
        handler.removeCallbacks(windowTransitionEnd)
        loadNextRunnable?.let { handler.removeCallbacks(it) }
        loadNextRunnable = null
        imageView.setImageDrawable(null)
        loadedImages.clear()
        val targets = loadTargets.values.toList()
        loadTargets.clear()
        targets.forEach { requestManager?.clear(it) }
    }

    /**
     * 跳转到指定位置 - 支持动画
     * @param position 目标位置
     * @param animate 是否使用动画（默认true）
     */
    fun seekTo(position: Int, animate: Boolean = true) {
        if (position < 0 || position >= imageUrls.size || isReleased) return
        if (position == currentIndex) {
            if (maxCachedImages > 0 && isPreloading) {
                prepareWindow(currentIndex) { scheduleNextTransition() }
            }
            return
        }
        if (maxCachedImages > 0 && (isPreloading || loadedImages.getOrNull(position) == null)) {
            prepareWindow(position) { seekTo(position, animate) }
            return
        }
        val targetImage = loadedImages.getOrNull(position) ?: return
        val currentImage = loadedImages.getOrNull(currentIndex)
        if (!isDrawableValid(targetImage.drawable)) {
            reloadInvalidImages()
            return
        }
        val fromPosition = currentIndex
        cancelSeek()
        handler.removeCallbacks(transitionRunnable)
        if (animate && transitionDuration > 0 && currentImage != null && isDrawableValid(currentImage.drawable)) {
            val transitionDrawable = TransitionDrawable(arrayOf(currentImage.drawable, targetImage.drawable))
            imageView.setImageDrawable(transitionDrawable)
            currentIndex = position
            isSeeking = true
            transitionDrawable.startTransition(transitionDuration)
            handler.postDelayed(seekCompleteRunnable, transitionDuration.toLong())
            playStateListener?.onSeekAnimationStart(fromPosition, position)
        } else {
            performDirectSeek(position)
        }
    }

    /**
     * 直接跳转（无动画）
     */
    private fun performDirectSeek(position: Int) {
        val image = loadedImages.getOrNull(position) ?: return
        imageView.setImageDrawable(image.drawable)
        currentIndex = position
        val generation = loadGeneration
        playStateListener?.onImageChanged(currentIndex, imageUrls.size)
        if (generation == loadGeneration && currentIndex == position) scheduleAfterImageChange()
    }

    /**
     * 跳转完成Runnable
     */
    private val seekCompleteRunnable = Runnable {
        if (isSeeking && !isReleased) {
            isSeeking = false
            val generation = loadGeneration
            val position = currentIndex
            imageView.setImageDrawable(loadedImages.getOrNull(position)?.drawable)
            playStateListener?.onSeekAnimationEnd(position)
            if (generation == loadGeneration && position == currentIndex && !isSeeking && !isReleased) {
                playStateListener?.onImageChanged(position, imageUrls.size)
                if (generation == loadGeneration && currentIndex == position) scheduleAfterImageChange()
            }
        }
    }

    private fun cancelSeek() {
        handler.removeCallbacks(seekCompleteRunnable)
        handler.removeCallbacks(windowTransitionEnd)
        if (isSeeking) {
            isSeeking = false
            imageView.setImageDrawable(loadedImages.getOrNull(currentIndex)?.drawable)
        }
        if (maxCachedImages > 0) imageView.setImageDrawable(loadedImages.getOrNull(currentIndex)?.drawable)
    }

    private val windowTransitionEnd = Runnable {
        if (!isReleased && !isSeeking) {
            imageView.setImageDrawable(loadedImages.getOrNull(currentIndex)?.drawable)
            scheduleAfterImageChange()
        }
    }

    private fun scheduleAfterImageChange() {
        if (isReleased || isSeeking || isPreloading) return
        if (maxCachedImages > 0) prepareWindow(currentIndex) { scheduleNextTransition() }
        else scheduleNextTransition()
    }

    private fun prepareWindow(center: Int, ready: () -> Unit) {
        if (isReleased || !isAttachedToWindow || imageUrls.isEmpty()) return
        handler.removeCallbacks(transitionRunnable)
        cancelSeek()
        loadNextRunnable?.let { handler.removeCallbacks(it) }
        loadNextRunnable = null
        // 失效旧加载和旧 seek 意图；淘汰前先解除过渡 Drawable 的引用。
        val generation = ++loadGeneration
        if (loadedImages.size != imageUrls.size) {
            loadedImages.clear()
            repeat(imageUrls.size) { loadedImages.add(null) }
        }
        val keep = linkedSetOf(center, currentIndex)
        val limit = minOf(maxCachedImages, imageUrls.size)
        val forwardCount = if (loop) imageUrls.size - 1 else imageUrls.size - center - 1
        for (offset in 1..forwardCount) {
            if (keep.size >= limit) break
            val next = ((center.toLong() + offset) % imageUrls.size).toInt()
            keep.add(next)
        }
        val removed = loadTargets.keys.filter { it !in keep || loadedImages[it] == null }
        removed.forEach { index ->
            val target = loadTargets.remove(index)
            loadedImages[index] = null
            target?.let { requestManager?.clear(it) }
        }
        requestManager = Glide.with(context)
        windowOrder = keep.toList()
        windowReady = ready
        isPreloading = true
        loadWindowNext(generation)
    }

    private fun loadWindowNext(generation: Int) {
        if (generation != loadGeneration || isReleased || !isAttachedToWindow) return
        val next = windowOrder.firstOrNull { loadedImages.getOrNull(it) == null }
        if (next != null) {
            loadImagesSafely(next, generation)
        } else {
            isPreloading = false
            val ready = windowReady
            windowReady = null
            if (ready != null) ready() else scheduleNextTransition()
        }
    }

    private fun finishPreloading() {
        isPreloading = false
        if (autoPlay && currentState == PlayState.IDLE) {
            play()
        } else if (currentState == PlayState.PLAYING) {
            playStateListener?.onImageChanged(currentIndex, imageUrls.size)
            scheduleNextTransition()
        }
    }

    /**
     * 获取当前播放状态
     */
    fun getCurrentState(): PlayState = currentState

    /**
     * 获取当前播放位置
     */
    fun getCurrentPosition(): Int = currentIndex

    /**
     * 获取总图片数量
     */
    fun getTotalCount(): Int = imageUrls.size

    /**
     * 是否正在执行跳转动画
     */
    fun isSeeking(): Boolean = isSeeking

    /**
     * 动态更新配置的方法
     */
    fun setStaticDisplayTime(time: Long) {
        staticDisplayTime = time.coerceAtLeast(1L)
    }

    fun setTransitionDuration(duration: Int) {
        transitionDuration = duration.coerceAtLeast(0)
    }

    fun setAutoPlay(auto: Boolean) {
        autoPlay = auto
    }

    fun setLoop(shouldLoop: Boolean) {
        loop = shouldLoop
    }

    /**
     * 预加载所有图片
     */
    private fun preloadAllImages() {
        if (isPreloading || isReleased || imageUrls.isEmpty() || !isAttachedToWindow) return
        if (maxCachedImages > 0) {
            prepareWindow(currentIndex) { finishPreloading() }
            return
        }
        isPreloading = true
        loadedImages.clear()
        // 用原始位置保存结果，某张加载失败时也不能让后续图片的索引前移。
        repeat(imageUrls.size) { loadedImages.add(null) }
        requestManager = Glide.with(context)
        loadImagesSafely(0, loadGeneration)
    }

    /**
     * 安全加载图片序列
     */
    private fun loadImagesSafely(startIndex: Int, generation: Int) {
        if (generation != loadGeneration || isReleased || !isAttachedToWindow) return
        if (startIndex >= imageUrls.size) {
            finishPreloading()
            return
        }

        val url = imageUrls[startIndex]
        // wrap_content 首次可能测得 0；用屏幕尺寸兜底，避免等待图片与等待测量互相阻塞。
        val targetWidth = if (width > 0) width else resources.displayMetrics.widthPixels.coerceAtLeast(1)
        val targetHeight = if (height > 0) height else resources.displayMetrics.heightPixels.coerceAtLeast(1)
        val target = object : CustomTarget<Drawable>(targetWidth, targetHeight) {
            override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable>?) {
                onImageLoaded(startIndex, url, resource, generation)
            }

            override fun onLoadFailed(errorDrawable: Drawable?) {
                onImageLoaded(startIndex, url, errorDrawable ?: ColorDrawable(Color.TRANSPARENT), generation)
            }

            override fun onLoadCleared(placeholder: Drawable?) {
                // 窗口移动后保留的已加载 target 仍然有效，清理通知应按 target 身份判断。
                if (isReleased || loadTargets[startIndex] !== this) return
                // Glide 清理时解除所有显示引用，过渡 Drawable 也可能持有这张图。
                imageView.setImageDrawable(null)
                loadedImages[startIndex] = null
                handler.removeCallbacks(transitionRunnable)
                cancelSeek()
            }
        }
        loadTargets[startIndex] = target
        requestManager?.asDrawable()
            ?.load(url)
            ?.apply {
                if (placeholderResId != 0) placeholder(placeholderResId)
                if (errorResId != 0) error(errorResId)
                dontTransform()
            }
            ?.into(target)
    }

    private fun onImageLoaded(index: Int, url: String, drawable: Drawable, generation: Int) {
        if (generation != loadGeneration || isReleased) return
        loadedImages[index] = LoadedImage(drawable, url)
        if (index == currentIndex) {
            imageView.setImageDrawable(drawable)
        }
        // Glide 命中内存缓存时可能同步回调，通过队列续载避免深递归及回调内清理请求。
        val next = Runnable {
            if (generation == loadGeneration && !isReleased) {
                loadNextRunnable = null
                if (maxCachedImages > 0) loadWindowNext(generation)
                else loadImagesSafely(index + 1, generation)
            }
        }
        loadNextRunnable?.let { handler.removeCallbacks(it) }
        loadNextRunnable = next
        handler.post(next)
    }

    /**
     * 检查Drawable是否有效
     */
    private fun isDrawableValid(drawable: Drawable): Boolean {
        return if (drawable is BitmapDrawable) {
            val bitmap = drawable.bitmap
            !bitmap.isRecycled
        } else {
            true
        }
    }

    /**
     * 开始过渡循环
     */
    private fun scheduleNextTransition(delay: Long = staticDisplayTime) {
        handler.removeCallbacks(transitionRunnable)
        if (isReleased || !isAttachedToWindow || isPreloading || isSeeking || currentState != PlayState.PLAYING) return
        if (loadedImages.isEmpty() || (loop && loadedImages.size == 1)) return
        handler.postDelayed(transitionRunnable, delay)
    }

    /**
     * 执行图片过渡
     */
    private fun performTransition() {
        if (loadedImages.isEmpty() || isReleased || isSeeking || isPreloading ||
            !isAttachedToWindow || currentState != PlayState.PLAYING
        ) return

        val nextIndex = if (loop) {
            (currentIndex + 1) % loadedImages.size
        } else {
            currentIndex + 1
        }

        if (!loop && nextIndex >= loadedImages.size) {
            currentState = PlayState.STOPPED
            playStateListener?.onPlayCompleted()
            return
        }

        val currentImage = loadedImages.getOrNull(currentIndex)
        val nextImage = loadedImages.getOrNull(nextIndex)

        if (currentImage == null || nextImage == null ||
            !isDrawableValid(currentImage.drawable) || !isDrawableValid(nextImage.drawable)
        ) {
            reloadInvalidImages()
            return
        }

        val transitionDrawable = TransitionDrawable(
            arrayOf(
                currentImage.drawable,
                nextImage.drawable
            )
        )

        imageView.setImageDrawable(transitionDrawable)
        transitionDrawable.startTransition(transitionDuration)

        currentIndex = nextIndex
        val generation = loadGeneration
        playStateListener?.onImageChanged(currentIndex, imageUrls.size)
        if (generation != loadGeneration || currentIndex != nextIndex || isReleased) return

        val totalDisplayTime = staticDisplayTime.coerceAtMost(Long.MAX_VALUE - transitionDuration) + transitionDuration
        if (maxCachedImages > 0) {
            handler.removeCallbacks(windowTransitionEnd)
            handler.postDelayed(windowTransitionEnd, transitionDuration.toLong())
        } else {
            scheduleNextTransition(totalDisplayTime)
        }

        if (loop && nextIndex == 0 && currentState == PlayState.PLAYING && !isSeeking) {
            playStateListener?.onPlayCompleted()
        }
    }

    /**
     * 重新加载无效的图片
     */
    private fun reloadInvalidImages() {
        handler.removeCallbacks(transitionRunnable)
        cancelSeek()
        releaseLoadedImages()
        preloadAllImages()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (loadedImages.isEmpty()) preloadAllImages()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (!isReleased) preloadAllImages()
    }

    override fun onDetachedFromWindow() {
        // 暂时移除 View 仍允许重新挂载；只有显式 release() 才永久结束使用。
        handler.removeCallbacksAndMessages(null)
        cancelSeek()
        releaseLoadedImages()
        super.onDetachedFromWindow()
    }
}
