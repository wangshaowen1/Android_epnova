package com.kuaigeglobal.epnova.widget

import android.content.Context
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
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
    private var loadedImages = mutableListOf<LoadedImage>()
    private var currentIndex = 0
    private var currentState = PlayState.IDLE
    private var isPreloading = false
    private var playStateListener: OnPlayStateListener? = null
    private var isReleased = false
    private var isSeeking = false // 是否正在执行跳转动画

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
                ).toLong()

                transitionDuration = typedArray.getInt(
                    R.styleable.TransitionImageView_transitionDuration,
                    1000
                )

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

        if (urls == imageUrls) return

        stop()
        releaseLoadedImages()
        imageUrls = urls.filterNotNull()
        loadedImages.clear()
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
        if (currentState == PlayState.PLAYING || isReleased) return

        when (currentState) {
            PlayState.IDLE -> {
                if (loadedImages.size >= 2) {
                    startTransitionCycle()
                } else {
                    currentState = PlayState.PLAYING
                    playStateListener?.onPlayStart()
                }
            }
            PlayState.PAUSED -> {
                currentState = PlayState.PLAYING
                playStateListener?.onPlayResume()
                handler.postDelayed(transitionRunnable, staticDisplayTime)
            }
            else -> {
                currentIndex = 0
                startTransitionCycle()
            }
        }
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
        currentState = PlayState.STOPPED
        playStateListener?.onPlayStop()
    }

    /**
     * 释放资源
     */
    fun release() {
        if (isReleased) return

        isReleased = true
        handler.removeCallbacks(transitionRunnable)
        currentState = PlayState.STOPPED
        imageUrls = emptyList()

        imageView.setImageDrawable(null)
        releaseLoadedImages()
    }

    /**
     * 释放已加载的图片资源
     */
    private fun releaseLoadedImages() {
        loadedImages.forEach { loadedImage ->
            if (loadedImage.drawable is BitmapDrawable) {
                val bitmapDrawable = loadedImage.drawable as BitmapDrawable
                val bitmap = bitmapDrawable.bitmap
                if (!bitmap.isRecycled) {
                    // 不主动回收，让GC处理
                }
            }
        }
        loadedImages.clear()
    }

    /**
     * 跳转到指定位置 - 支持动画
     * @param position 目标位置
     * @param animate 是否使用动画（默认true）
     */
    fun seekTo(position: Int, animate: Boolean = true) {
        if (position < 0 || position >= imageUrls.size || isReleased) return

        // 如果正在执行跳转动画，先停止
        if (isSeeking) {
            handler.removeCallbacks(seekCompleteRunnable)
            isSeeking = false
        }

        handler.removeCallbacks(transitionRunnable)

        val targetPosition = position
        val fromPosition = currentIndex

        if (targetPosition == fromPosition) {
            // 目标位置与当前位置相同，不执行任何操作
            return
        }

        if (animate && loadedImages.size > targetPosition && loadedImages.size > fromPosition) {
            // 使用动画跳转
            isSeeking = true

            // 通知动画开始
            playStateListener?.onSeekAnimationStart(fromPosition, targetPosition)

            val currentImage = loadedImages[fromPosition]
            val targetImage = loadedImages[targetPosition]

            if (isDrawableValid(currentImage.drawable) && isDrawableValid(targetImage.drawable)) {
                val transitionDrawable = android.graphics.drawable.TransitionDrawable(
                    arrayOf(
                        currentImage.drawable,
                        targetImage.drawable
                    )
                )

                imageView.setImageDrawable(transitionDrawable)
                transitionDrawable.startTransition(transitionDuration)

                // 更新当前位置
                currentIndex = targetPosition

                // 延迟执行动画完成回调
                handler.postDelayed({
                    isSeeking = false
                    playStateListener?.onSeekAnimationEnd(targetPosition)
                    playStateListener?.onImageChanged(currentIndex, imageUrls.size)

                    // 如果正在播放状态，继续自动播放
                    if (currentState == PlayState.PLAYING) {
                        handler.postDelayed(transitionRunnable, staticDisplayTime)
                    }
                }, transitionDuration.toLong())
            } else {
                // Drawable无效，不使用动画
                performDirectSeek(targetPosition)
            }
        } else {
            // 不使用动画，直接跳转
            performDirectSeek(targetPosition)
        }
    }

    /**
     * 直接跳转（无动画）
     */
    private fun performDirectSeek(position: Int) {
        if (loadedImages.size > position) {
            imageView.setImageDrawable(loadedImages[position].drawable)
            currentIndex = position
            playStateListener?.onImageChanged(currentIndex, imageUrls.size)

            // 如果正在播放状态，继续自动播放
            if (currentState == PlayState.PLAYING) {
                handler.postDelayed(transitionRunnable, staticDisplayTime)
            }
        }
    }

    /**
     * 跳转完成Runnable
     */
    private val seekCompleteRunnable = Runnable {
        isSeeking = false
        playStateListener?.onSeekAnimationEnd(currentIndex)
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
        staticDisplayTime = time
    }

    fun setTransitionDuration(duration: Int) {
        transitionDuration = duration
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
        if (isPreloading || isReleased) return

        isPreloading = true
        loadedImages.clear()
        loadImagesSafely(0)
    }

    /**
     * 安全加载图片序列
     */
    private fun loadImagesSafely(startIndex: Int) {
        if (startIndex >= imageUrls.size || isReleased) {
            isPreloading = false
            if (currentState == PlayState.PLAYING && loadedImages.size >= 2) {
                startTransitionCycle()
            } else if (autoPlay && loadedImages.size >= 2) {
                play()
            }
            return
        }

        val url = imageUrls[startIndex]

        try {
            Glide.with(context)
                .asDrawable()
                .load(url)
                .apply {
                    if (placeholderResId != 0) {
                        placeholder(placeholderResId)
                    }
                    if (errorResId != 0) {
                        error(errorResId)
                    }
                    dontTransform()
                }
                .into(object : CustomTarget<Drawable>() {
                    override fun onResourceReady(
                        resource: Drawable,
                        transition: Transition<in Drawable>?
                    ) {
                        if (isReleased) return

                        val safeDrawable = createSafeDrawable(resource)
                        loadedImages.add(LoadedImage(safeDrawable, url))

                        if (startIndex == 0) {
                            imageView.setImageDrawable(safeDrawable)
                        }

                        loadImagesSafely(startIndex + 1)
                    }

                    override fun onLoadCleared(placeholder: Drawable?) {
                        // 不做特殊处理
                    }

                    override fun onLoadFailed(errorDrawable: Drawable?) {
                        if (isReleased) return

                        val errorDrawable = if (errorResId != 0) {
                            ContextCompat.getDrawable(context, errorResId)
                        } else {
                            null
                        }
                        errorDrawable?.let {
                            loadedImages.add(LoadedImage(it, url))
                        }
                        loadImagesSafely(startIndex + 1)
                    }
                })
        } catch (e: Exception) {
            e.printStackTrace()
            loadImagesSafely(startIndex + 1)
        }
    }

    /**
     * 创建安全的Drawable
     */
    private fun createSafeDrawable(original: Drawable): Drawable {
        return if (original is BitmapDrawable) {
            val bitmap = original.bitmap
            val copiedBitmap = bitmap.copy(bitmap.config!!, true)
            BitmapDrawable(context.resources, copiedBitmap)
        } else {
            original
        }
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
    private fun startTransitionCycle() {
        if (loadedImages.size < 2 || isReleased) return

        handler.removeCallbacks(transitionRunnable)
        currentState = PlayState.PLAYING
        playStateListener?.onPlayStart()
        playStateListener?.onImageChanged(currentIndex, imageUrls.size)

        handler.postDelayed(transitionRunnable, staticDisplayTime)
    }

    /**
     * 执行图片过渡
     */
    private fun performTransition() {
        if (loadedImages.size < 2 || isReleased || isSeeking) return

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

        val currentImage = loadedImages[currentIndex]
        val nextImage = loadedImages[nextIndex]

        if (!isDrawableValid(currentImage.drawable) || !isDrawableValid(nextImage.drawable)) {
            reloadInvalidImages()
            return
        }

        val transitionDrawable = android.graphics.drawable.TransitionDrawable(
            arrayOf(
                currentImage.drawable,
                nextImage.drawable
            )
        )

        imageView.setImageDrawable(transitionDrawable)
        transitionDrawable.startTransition(transitionDuration)

        currentIndex = nextIndex
        playStateListener?.onImageChanged(currentIndex, imageUrls.size)

        val totalDisplayTime = staticDisplayTime + transitionDuration
        handler.postDelayed(transitionRunnable, totalDisplayTime)

        if (loop && nextIndex == 0) {
            playStateListener?.onPlayCompleted()
        }
    }

    /**
     * 重新加载无效的图片
     */
    private fun reloadInvalidImages() {
        val currentUrls = imageUrls.toList()
        setImageUrls(currentUrls)

        if (currentState == PlayState.PLAYING) {
            handler.postDelayed({
                if (loadedImages.size >= 2 && !isReleased) {
                    startTransitionCycle()
                }
            }, 1000)
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        release()
    }
}