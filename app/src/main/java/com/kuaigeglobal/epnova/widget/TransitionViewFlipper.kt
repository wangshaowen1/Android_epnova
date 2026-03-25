package com.kuaigeglobal.epnova.widget

import android.content.Context
import android.util.AttributeSet
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AccelerateInterpolator
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.DecelerateInterpolator
import android.view.animation.RotateAnimation
import android.view.animation.ScaleAnimation
import android.view.animation.TranslateAnimation
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ViewFlipper
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.kuaigeglobal.epnova.R
import com.nhsd.basic.utils.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

class TransitionViewFlipper @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    enum class TransitionType {
        FADE, SLIDE_LEFT, SLIDE_RIGHT, SLIDE_UP, SLIDE_DOWN,
        ZOOM_IN, ZOOM_OUT, ROTATE, CUBE, ACCORDION
    }

    interface OnPlayStateListener {
        fun onPlayStart()
        fun onPlayPause()
        fun onPlayStop()
        fun onImageChanged(position: Int, total: Int)
        fun onTransitionStart(transitionType: TransitionType)
        fun onSeekAnimationStart(fromPosition: Int, toPosition: Int, transitionType: TransitionType)
        fun onSeekAnimationEnd(toPosition: Int, transitionType: TransitionType)
        fun onPreloadStarted(position: Int) // 新增：预加载开始回调
        fun onPreloadCompleted(position: Int) // 新增：预加载完成回调
    }
    // 在类顶部添加状态枚举和队列
    private enum class AnimationState {
        IDLE, PLAYING, PAUSED, SEEKING, TRANSITIONING
    }

    private val animationStateManager = AnimationStateManager()
    private val pendingActions = mutableListOf<PendingAction>()

    // 动画状态管理器
    private class AnimationStateManager {
        private var currentState: AnimationState = AnimationState.IDLE

        @Synchronized
        fun setState(newState: AnimationState) {
            currentState = newState
        }

        @Synchronized
        fun getState(): AnimationState = currentState

        @Synchronized
        fun canExecuteSeek(): Boolean {
            return currentState != AnimationState.SEEKING && currentState != AnimationState.TRANSITIONING
        }

        @Synchronized
        fun canExecutePauseResume(): Boolean {
            return currentState != AnimationState.SEEKING
        }
    }
    // 微动画配置
    private var microAnimationConfig: MicroAnimationConfig = MicroAnimationConfig()

    private val viewFlipper: ViewFlipper

    // 配置参数（从XML属性初始化）
    private var displayTime: Long = 3000
    private var transitionDuration: Long = 1000
    private var transitionType: TransitionType = TransitionType.FADE
    private var autoPlay: Boolean = false
    private var loop: Boolean = true

    private var imageUrls: List<String> = emptyList()
    private var currentIndex = 0
    private var isPlaying = false
    private var playStateListener: OnPlayStateListener? = null
    private var isReleased = false
    private var isSeeking = false
    // 协程作用域和任务管理
    private val animationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val seekingTasks = mutableSetOf<Job>()
    // 预加载相关变量
    private var preloadEnabled: Boolean = true // 是否启用预加载
    private var preloadRange: Int = 1 // 预加载范围（前后各几张）
    private val preloadedViews = mutableMapOf<Int, ImageView>() // 预加载的视图缓存
    private val loadingPositions = mutableSetOf<Int>() // 正在加载的位置

    private val glide: RequestManager by lazy {
        Glide.with(context.applicationContext)
    }

    // 播放控制Runnable
    private val transitionRunnable = Runnable { performTransition() }

    // 跳转完成Runnable
    private val seekCompleteRunnable = Runnable {
        isSeeking = false
        playStateListener?.onSeekAnimationEnd(currentIndex, transitionType)
    }

    init {
        // 初始化ViewFlipper
        viewFlipper = ViewFlipper(context).apply {
            layoutParams = LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT
            )
        }
        addView(viewFlipper)

        // 从XML属性初始化配置
        initAttributes(context, attrs)

        // 设置自定义动画
        setCustomAnimations()
    }

    /**
     * 从XML属性初始化配置
     */
    private fun initAttributes(context: Context, attrs: AttributeSet?) {
        attrs?.let { attributeSet ->
            val typedArray = context.obtainStyledAttributes(
                attributeSet,
                R.styleable.TransitionViewFlipper,
                0,
                0
            )

            try {
                // 原有属性初始化...
                displayTime = typedArray.getInt(
                    R.styleable.TransitionViewFlipper_displayTime,
                    3000
                ).toLong()

                transitionDuration = typedArray.getInt(
                    R.styleable.TransitionViewFlipper_transitionDuration,
                    1000
                ).toLong()

                // ... 其他原有属性

                // 微动画属性初始化
                val microAnimEnabled = typedArray.getBoolean(
                    R.styleable.TransitionViewFlipper_microAnimEnabled,
                    false
                )

                val microAnimDirection = typedArray.getInt(
                    R.styleable.TransitionViewFlipper_microAnimDirection,
                    0
                )

                val microAnimDistance = typedArray.getFloat(
                    R.styleable.TransitionViewFlipper_microAnimDistance,
                    0.1f
                )

                val microAnimScale = typedArray.getFloat(
                    R.styleable.TransitionViewFlipper_microAnimScale,
                    1.1f
                )

                val microAnimDuration = typedArray.getInt(
                    R.styleable.TransitionViewFlipper_microAnimDuration,
                    3000
                ).toLong()

                // 新增：初始放大系数
                val microAnimInitialScale = typedArray.getFloat(
                    R.styleable.TransitionViewFlipper_microAnimInitialScale,
                    1.02f // 默认轻微放大
                )

                microAnimationConfig = MicroAnimationConfig(
                    enabled = microAnimEnabled,
                    translationDirection = when (microAnimDirection) {
                        0 -> MicroAnimationConfig.TranslationDirection.NONE
                        1 -> MicroAnimationConfig.TranslationDirection.LEFT
                        2 -> MicroAnimationConfig.TranslationDirection.RIGHT
                        3 -> MicroAnimationConfig.TranslationDirection.TOP
                        4 -> MicroAnimationConfig.TranslationDirection.BOTTOM
                        5 -> MicroAnimationConfig.TranslationDirection.TOP_LEFT
                        6 -> MicroAnimationConfig.TranslationDirection.TOP_RIGHT
                        7 -> MicroAnimationConfig.TranslationDirection.BOTTOM_LEFT
                        8 -> MicroAnimationConfig.TranslationDirection.BOTTOM_RIGHT
                        else -> MicroAnimationConfig.TranslationDirection.NONE
                    },
                    translationDistance = microAnimDistance,
                    scaleRatio = microAnimScale,
                    duration = microAnimDuration,
                    initialScale = microAnimInitialScale
                )

            } finally {
                typedArray.recycle()
            }
        }
    }

    // 定义操作类型
    private sealed interface PendingAction {
        fun execute(flipper: TransitionViewFlipper)
    }

    private class PauseAction : PendingAction {
        override fun execute(flipper: TransitionViewFlipper) {
            flipper.doPauseMicroAnimation()
        }
    }

    private class ResumeAction : PendingAction {
        override fun execute(flipper: TransitionViewFlipper) {
            flipper.doResumeMicroAnimation()
        }
    }

    /**
     * 暂停微动画 - 支持优先级管理
     */
    fun pauseMicroAnimation() {
        // 如果正在执行跳转，将暂停操作加入队列
        if (!animationStateManager.canExecutePauseResume()) {
            pendingActions.removeAll { it is PauseAction }
            pendingActions.add(PauseAction())
            Logger.d("MicroAnim", "Pause delayed - waiting for seek to complete")
            return
        }

        doPauseMicroAnimation()
    }

    /**
     * 恢复微动画 - 支持优先级管理
     */
    fun resumeMicroAnimation() {
        // 如果正在执行跳转，将恢复操作加入队列
        if (!animationStateManager.canExecutePauseResume()) {
            pendingActions.removeAll { it is ResumeAction }
            pendingActions.add(ResumeAction())
            Logger.d("MicroAnim", "Resume delayed - waiting for seek to complete")
            return
        }

        doResumeMicroAnimation()
    }

    /**
     * 实际执行暂停操作
     */
    private fun doPauseMicroAnimation() {
        // 取消所有跳转任务
        cancelAllSeekingTasks()

        for (i in 0 until viewFlipper.childCount) {
            val imageView = viewFlipper.getChildAt(i) as? MicroAnimImageView
            imageView?.pauseMicroAnimation()
        }
        animationStateManager.setState(AnimationState.PAUSED)
        Logger.d("MicroAnim", "All micro animations paused")
    }

    /**
     * 实际执行恢复操作
     */
    private fun doResumeMicroAnimation() {
        // 取消可能冲突的任务
        cancelAllSeekingTasks()

        for (i in 0 until viewFlipper.childCount) {
            val imageView = viewFlipper.getChildAt(i) as? MicroAnimImageView
            imageView?.resumeMicroAnimation()
        }
        animationStateManager.setState(if (isPlaying) AnimationState.PLAYING else AnimationState.IDLE)
        Logger.d("MicroAnim", "All micro animations resumed")
    }

    /**
     * 执行等待中的操作
     */
    private fun executePendingActions() {
        if (pendingActions.isNotEmpty()) {
            Logger.d("MicroAnim", "Executing ${pendingActions.size} pending actions")
            val actions = pendingActions.toList()
            pendingActions.clear()

            actions.forEach { action ->
                try {
                    when (action) {
                        is PauseAction -> action.execute(this)
                        is ResumeAction -> action.execute(this)
                    }
                } catch (e: Exception) {
                    Logger.e("MicroAnim", "Error executing pending action: ${e.message}")
                }
            }
        }
    }

    /**
     * 清空待执行操作
     */
    private fun clearPendingActions() {
        pendingActions.clear()
        Logger.d("MicroAnim", "All pending actions cleared")
    }
    /**
     * 启动跳转任务 - 自动管理任务生命周期
     */
    private fun launchSeekingTask(block: suspend CoroutineScope.() -> Unit): Job {
        // 取消之前的跳转任务
        cancelAllSeekingTasks()

        return animationScope.launch(Dispatchers.Main) {
            try {
                block()
            } catch (e: CancellationException) {
                // 任务被取消是正常情况，不需要处理
                Logger.d("MicroAnim", "Seeking task cancelled: ${e.message}")
            } catch (e: Exception) {
                // 处理其他异常
                Logger.e("MicroAnim", "Seeking task error")
            }
        }.also {job->
            seekingTasks.add(job)
            // 任务完成后自动清理
            job.invokeOnCompletion {
                seekingTasks.remove(job)
                Logger.d("MicroAnim", "Seeking task completed, remaining: ${seekingTasks.size}")
            }
        }
    }
    /**
     * 安全延迟 - 可取消的延迟
     */
    private suspend fun safeDelay(delayMillis: Long) {
        try {
            delay(delayMillis)
        } catch (e: CancellationException) {
            // 延迟期间被取消是正常的
            throw e
        }
    }
    /**
     * 取消所有跳转任务
     */
    private fun cancelAllSeekingTasks() {
        seekingTasks.forEach { it.cancel() }
        seekingTasks.clear()
        Logger.d("MicroAnim", "All seeking tasks cancelled")
    }
    /**
     * 设置自定义动画
     */
    private fun setCustomAnimations() {
        if (isReleased) return

        val (inAnim, outAnim) = createAnimations(transitionType)
        viewFlipper.inAnimation = inAnim
        viewFlipper.outAnimation = outAnim
    }

    /**
     * 根据动画类型创建动画
     */
    private fun setCustomAnimations(type: TransitionType) {
        if (isReleased) return

        val (inAnim, outAnim) = createAnimations(type)
        viewFlipper.inAnimation = inAnim
        viewFlipper.outAnimation = outAnim
    }

    /**
     * 创建各种过渡动画
     */
    private fun createAnimations(type: TransitionType): Pair<Animation, Animation> {
        return when (type) {
            TransitionType.FADE -> createFadeAnimations()
            TransitionType.SLIDE_LEFT -> createSlideAnimations(-1f, 0f, 0f, 0f)
            TransitionType.SLIDE_RIGHT -> createSlideAnimations(1f, 0f, 0f, 0f)
            TransitionType.SLIDE_UP -> createSlideAnimations(0f, -1f, 0f, 0f)
            TransitionType.SLIDE_DOWN -> createSlideAnimations(0f, 1f, 0f, 0f)
            TransitionType.ZOOM_IN -> createZoomAnimations(0.5f, 1f, true)
            TransitionType.ZOOM_OUT -> createZoomAnimations(1f, 0.5f, false)
            TransitionType.ROTATE -> createRotateAnimations()
            TransitionType.CUBE -> createCubeAnimations()
            TransitionType.ACCORDION -> createAccordionAnimations()
        }
    }

    // 各种动画创建方法保持不变...
    private fun createFadeAnimations(): Pair<Animation, Animation> {
        val fadeIn = AlphaAnimation(0.0f, 1.0f).apply {
            duration = transitionDuration
            interpolator = AccelerateDecelerateInterpolator()
        }

        val fadeOut = AlphaAnimation(1.0f, 0.0f).apply {
            duration = transitionDuration
            interpolator = AccelerateDecelerateInterpolator()
        }

        return Pair(fadeIn, fadeOut)
    }

    private fun createSlideAnimations(
        fromXDelta: Float, toXDelta: Float,
        fromYDelta: Float, toYDelta: Float
    ): Pair<Animation, Animation> {
        val inAnim = TranslateAnimation(
            Animation.RELATIVE_TO_PARENT, fromXDelta,
            Animation.RELATIVE_TO_PARENT, toXDelta,
            Animation.RELATIVE_TO_PARENT, fromYDelta,
            Animation.RELATIVE_TO_PARENT, toYDelta
        ).apply {
            duration = transitionDuration
            interpolator = AccelerateDecelerateInterpolator()
        }

        val outAnim = TranslateAnimation(
            Animation.RELATIVE_TO_PARENT, 0f,
            Animation.RELATIVE_TO_PARENT, -fromXDelta,
            Animation.RELATIVE_TO_PARENT, 0f,
            Animation.RELATIVE_TO_PARENT, -fromYDelta
        ).apply {
            duration = transitionDuration
            interpolator = AccelerateDecelerateInterpolator()
        }

        return Pair(inAnim, outAnim)
    }

    private fun createZoomAnimations(
        fromScale: Float, toScale: Float, zoomIn: Boolean
    ): Pair<Animation, Animation> {
        val pivotType = Animation.RELATIVE_TO_SELF
        val pivotValue = 0.5f

        val inAnim = ScaleAnimation(
            fromScale, toScale, fromScale, toScale,
            pivotType, pivotValue, pivotType, pivotValue
        ).apply {
            duration = transitionDuration
            interpolator = AccelerateDecelerateInterpolator()
        }

        val outAnim = AlphaAnimation(1.0f, 0.0f).apply {
            duration = transitionDuration / 2
            interpolator = AccelerateInterpolator()
        }

        return Pair(inAnim, outAnim)
    }

    private fun createRotateAnimations(): Pair<Animation, Animation> {
        val inAnim = RotateAnimation(
            -90f, 0f,
            Animation.RELATIVE_TO_SELF, 0.5f,
            Animation.RELATIVE_TO_SELF, 0.5f
        ).apply {
            duration = transitionDuration
            interpolator = DecelerateInterpolator()
        }

        val outAnim = AlphaAnimation(1.0f, 0.3f).apply {
            duration = transitionDuration
            interpolator = AccelerateInterpolator()
        }

        return Pair(inAnim, outAnim)
    }

    private fun createCubeAnimations(): Pair<Animation, Animation> {
        val inAnim = RotateAnimation(
            90f, 0f,
            Animation.RELATIVE_TO_SELF, 0.0f,
            Animation.RELATIVE_TO_SELF, 0.5f
        ).apply {
            duration = transitionDuration
            interpolator = DecelerateInterpolator()
        }

        val outAnim = RotateAnimation(
            0f, -90f,
            Animation.RELATIVE_TO_SELF, 1.0f,
            Animation.RELATIVE_TO_SELF, 0.5f
        ).apply {
            duration = transitionDuration
            interpolator = AccelerateInterpolator()
        }

        return Pair(inAnim, outAnim)
    }

    private fun createAccordionAnimations(): Pair<Animation, Animation> {
        val inAnim = ScaleAnimation(
            0.0f, 1.0f, 1.0f, 1.0f,
            Animation.RELATIVE_TO_SELF, 0.0f,
            Animation.RELATIVE_TO_SELF, 0.5f
        ).apply {
            duration = transitionDuration
            interpolator = DecelerateInterpolator()
        }

        val outAnim = ScaleAnimation(
            1.0f, 0.0f, 1.0f, 1.0f,
            Animation.RELATIVE_TO_SELF, 1.0f,
            Animation.RELATIVE_TO_SELF, 0.5f
        ).apply {
            duration = transitionDuration
            interpolator = AccelerateInterpolator()
        }

        return Pair(inAnim, outAnim)
    }

    /**
     * 设置图片URL列表 - 整合预加载功能
     */
    fun setImageUrls(urls: List<String?>) {
        if (isReleased) {
            isReleased = false
        }

        stop()

        // 清理之前的预加载资源
        cleanupPreloadedResources()

        imageUrls = urls.filterNotNull()
        viewFlipper.removeAllViews()
        currentIndex = 0

        // 创建并添加所有ImageView
        imageUrls.forEachIndexed { index, url ->
            val imageView = createImageView()
            viewFlipper.addView(imageView)

            // 如果是第一张图片，立即加载并显示
            if (index == 0) {
                loadImageSafely(imageView, url, index)
            }
        }

        // 预加载相邻的图片
        if (preloadEnabled && urls.isNotEmpty()) {
            preloadAdjacentImages(currentIndex)
        }

        if (urls.isNotEmpty() && autoPlay) {
            play()
        }
    }

    /**
     * 安全地加载图片 - 增强版，支持预加载回调
     */
    private fun loadImageSafely(imageView: ImageView, url: String, position: Int) {
        if (loadingPositions.contains(position)) {
            return // 避免重复加载
        }

        loadingPositions.add(position)
        playStateListener?.onPreloadStarted(position)

        try {
            Logger.i("Preloading loadImageSafely url=$url}")
            glide.load(url)
                .diskCacheStrategy(DiskCacheStrategy.ALL) // 缓存到磁盘和内存
                .into(imageView)
        } catch (e: Exception) {
            e.printStackTrace()
            loadingPositions.remove(position)
        } finally {
            // 注意：这里我们无法准确知道Glide何时完成加载
            // 在实际应用中，你可能需要使用Glide的Listener来获取准确的加载状态
            loadingPositions.remove(position)
            playStateListener?.onPreloadCompleted(position)
        }
    }

    /**
     * 预加载相邻的图片
     */
    private fun preloadAdjacentImages(centerPosition: Int) {
        Logger.i("Preloading call preloadAdjacentImages")

        if (!preloadEnabled || imageUrls.isEmpty()) return

        val startPos = (centerPosition - preloadRange).coerceAtLeast(0)
        val endPos = (centerPosition + preloadRange).coerceAtMost(imageUrls.size - 1)
        Logger.i("Preloading  startPos=${startPos} endPos= $endPos")

        for (position in startPos..endPos) {
            // 跳过当前显示的位置和已经在加载的位置
            if (position == centerPosition || loadingPositions.contains(position)) {
                continue
            }

            // 如果这个位置的视图还没有加载过，就预加载
            val imageView = viewFlipper.getChildAt(position) as? ImageView
            Logger.i("Preloading image at position $position")
            if (imageView != null) {
                val url = imageUrls[position]
                loadImageSafely(imageView, url, position)
            }
        }
    }

    /**
     * 创建ImageView - 修改为使用MicroAnimImageView，并设置初始放大系数
     */
    private fun createImageView(): MicroAnimImageView {
        return MicroAnimImageView(context).apply {
            layoutParams = LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT
            )
            scaleType = ImageView.ScaleType.FIT_CENTER
            adjustViewBounds = true
            // 设置微动画配置
            setMicroAnimationConfig(microAnimationConfig)
            // 设置初始放大系数
            setInitialScale(microAnimationConfig.initialScale)
        }
    }

    /**
     * 设置微动画配置 - 添加详细日志
     */
    fun setMicroAnimationConfig(config: MicroAnimationConfig) {
        Logger.d(
            "MicroAnim",
            "setMicroAnimationConfig called - old: ${this.microAnimationConfig.translationDirection}, new: ${config.translationDirection}"
        )

        this.microAnimationConfig = config

        // 更新所有已存在的ImageView
        for (i in 0 until viewFlipper.childCount) {
            val imageView = viewFlipper.getChildAt(i) as? MicroAnimImageView
            imageView?.setMicroAnimationConfig(config)
            imageView?.setInitialScale(config.initialScale)
        }
    }

    /**
     * 设置微动画配置并立即应用到当前视图 - 添加详细日志
     */
    fun setMicroAnimationConfigAndApply(config: MicroAnimationConfig) {
        Logger.d(
            "MicroAnim",
            "setMicroAnimationConfigAndApply called - direction: ${config.translationDirection}"
        )

        setMicroAnimationConfig(config)
        // 关键修复：确保所有视图都立即计算动画值
        for (i in 0 until viewFlipper.childCount) {
            val imageView = viewFlipper.getChildAt(i) as? MicroAnimImageView
            imageView?.setMicroAnimationConfig(config)
            imageView?.setInitialScale(config.initialScale)
        }
        // 立即重置当前视图到起始位置
        getCurrentImageView()?.resetToInitialState()
        // 如果正在播放，重新启动微动画
        if (isPlaying) {
            startMicroAnimation()
        }
    }

    /**
     * 设置初始放大系数
     */
    fun setInitialScale(scale: Float) {
        // 更新配置
        microAnimationConfig = microAnimationConfig.copy(initialScale = scale.coerceAtLeast(1.0f))

        // 应用到所有已存在的ImageView
        for (i in 0 until viewFlipper.childCount) {
            val imageView = viewFlipper.getChildAt(i) as? MicroAnimImageView
            imageView?.setInitialScale(scale)
        }
    }

    /**
     * 获取当前初始放大系数
     */
    fun getInitialScale(): Float = microAnimationConfig.initialScale

    /**
     * 开始微动画
     */
    /**
     * 开始微动画 - 支持指定时长
     */
    fun startMicroAnimation(customDuration: Long? = null) {
        val currentImageView = getCurrentImageView()
        currentImageView?.startMicroAnimation(customDuration)
    }


    /**
     * 动态更新微动画配置（不停止当前动画）
     */
    fun updateMicroAnimationConfig(
        enabled: Boolean? = null,
        direction: MicroAnimationConfig.TranslationDirection? = null,
        distance: Float? = null,
        scale: Float? = null,
        duration: Long? = null,
        initialScale: Float? = null // 新增：初始放大系数
    ) {
        val newConfig = microAnimationConfig.copy(
            enabled = enabled ?: microAnimationConfig.enabled,
            translationDirection = direction ?: microAnimationConfig.translationDirection,
            translationDistance = distance ?: microAnimationConfig.translationDistance,
            scaleRatio = scale ?: microAnimationConfig.scaleRatio,
            duration = duration ?: microAnimationConfig.duration,
            initialScale = initialScale ?: microAnimationConfig.initialScale // 新增
        )
        setMicroAnimationConfigAndApply(newConfig)
    }

    /**
     * 停止微动画
     */
    fun stopMicroAnimation() {
        for (i in 0 until viewFlipper.childCount) {
            val imageView = viewFlipper.getChildAt(i) as? MicroAnimImageView
            imageView?.stopMicroAnimation()
        }
    }

    /**
     * 获取当前显示的ImageView
     */
    /**
     * 安全地获取当前ImageView
     */
    private fun getCurrentImageView(): MicroAnimImageView? {
        return try {
            if (viewFlipper.displayedChild < viewFlipper.childCount && viewFlipper.displayedChild >= 0) {
                viewFlipper.getChildAt(viewFlipper.displayedChild) as? MicroAnimImageView
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
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
        if (isPlaying || imageUrls.size <= 1 || isReleased) return

        isPlaying = true
        animationStateManager.setState(AnimationState.PLAYING)
        playStateListener?.onPlayStart()
        // 启动当前图片的微动画
        startMicroAnimation()
        scheduleNextTransition()
    }

    /**
     * 暂停播放
     */
    fun pause() {
        isPlaying = false
        animationStateManager.setState(AnimationState.PAUSED)
        viewFlipper.removeCallbacks(transitionRunnable)
        playStateListener?.onPlayPause()
    }

    /**
     * 停止播放
     */
    fun stop() {
        pause()
        currentIndex = 0
        if (viewFlipper.childCount > 0 && !isReleased) {
            viewFlipper.displayedChild = 0
        }
        animationStateManager.setState(AnimationState.IDLE)
        playStateListener?.onPlayStop()
    }

    /**
     * 释放资源
     */
    fun release() {
        if (isReleased) return

        isReleased = true
        stop()
        clearPendingActions()

        // 取消所有协程任务
        animationScope.cancel()
        seekingTasks.clear()

        viewFlipper.removeAllViews()
        cleanupPreloadedResources()
        stopMicroAnimation()

        // 清理所有回调
        viewFlipper.removeCallbacks(transitionRunnable)
        animationStateManager.setState(AnimationState.IDLE)
        Logger.d("MicroAnim", "ViewFlipper released with state management")
    }

    /**
     * 是否正在运行微动画
     */
    fun isMicroAnimationRunning(): Boolean {
        return getCurrentImageView()?.isMicroAnimationRunning() ?: false
    }

    /**
     * 获取当前微动画配置
     */
    fun getMicroAnimationConfig(): MicroAnimationConfig = microAnimationConfig

    /**
     * 清理预加载资源
     */
    private fun cleanupPreloadedResources() {
        preloadedViews.clear()
        loadingPositions.clear()
        // 清理Glide相关资源
//        glide.onDestroy()
    }

    /**
     * 设置过渡动画类型
     */
    fun setTransitionType(type: TransitionType) {
        if (isReleased) return

        transitionType = type
        setCustomAnimations()
    }

    /**
     * 设置显示时间
     */
    fun setDisplayTime(time: Long) {
        displayTime = time
    }

    /**
     * 设置过渡动画时长
     */
    fun setTransitionDuration(duration: Long) {
        transitionDuration = duration
        setCustomAnimations()
    }

    /**
     * 设置自动播放
     */
    fun setAutoPlay(enable: Boolean) {
        autoPlay = enable
    }

    /**
     * 设置循环播放
     */
    fun setLoop(enable: Boolean) {
        loop = enable
    }

    /**
     * 设置预加载配置
     */
    fun setPreloadConfig(enabled: Boolean, range: Int = 1) {
        preloadEnabled = enabled
        preloadRange = range.coerceAtLeast(0)
    }

    /**
     * 跳转到指定位置 - 使用默认动画和默认微动画方向
     */
    fun seekTo(position: Int) {
        seekTo(position, transitionType, null, null)
    }

    /**
     * 跳转到指定位置 - 支持指定动画类型
     */
    fun seekTo(position: Int, seekTransitionType: TransitionType) {
        seekTo(position, seekTransitionType, null, null)
    }

    /**
     * 跳转到指定位置 - 支持指定微动画时长
     */
    fun seekTo(position: Int, microAnimDuration: Long?) {
        seekTo(position, transitionType, microAnimDuration, null)
    }

    /**
     * 跳转到指定位置 - 支持指定微动画方向
     */
    fun seekTo(position: Int, microAnimDirection: MicroAnimationConfig.TranslationDirection) {
        seekTo(position, transitionType, null, microAnimDirection)
    }

    /**
     * 跳转到指定位置 - 支持指定动画类型和微动画方向
     */
    fun seekTo(
        position: Int,
        seekTransitionType: TransitionType,
        microAnimDirection: MicroAnimationConfig.TranslationDirection
    ) {
        seekTo(position, seekTransitionType, null, microAnimDirection)
    }

    /**
     * 跳转到指定位置 - 支持指定微动画时长和方向
     */
    fun seekTo(
        position: Int,
        microAnimDuration: Long?,
        microAnimDirection: MicroAnimationConfig.TranslationDirection
    ) {
        seekTo(position, transitionType, microAnimDuration, microAnimDirection)
    }

    /**
     * 跳转到指定位置 - 完整版：使用协程简化逻辑
     */
    fun seekTo(
        position: Int,
        seekTransitionType: TransitionType,
        microAnimDuration: Long?,
        microAnimDirection: MicroAnimationConfig.TranslationDirection?
    ) {
        if (position < 0 || position >= imageUrls.size || isReleased) return

        // 状态检查：如果正在执行高优先级操作，拒绝新的跳转
        if (!animationStateManager.canExecuteSeek()) {
            Logger.w("MicroAnim", "Seek rejected - current state doesn't allow seeking")
            return
        }

        val currentImageView = getCurrentImageView()
        val targetPosition = position
        val fromPosition = currentIndex

        if (targetPosition == fromPosition) {
            return
        }

        // 设置状态为跳转中
        animationStateManager.setState(AnimationState.SEEKING)

        // 使用协程管理整个跳转流程
        launchSeekingTask {
            try {
                // 步骤1: 准备阶段
                prepareSeekTransition(seekTransitionType, targetPosition, microAnimDirection)

                // 步骤2: 执行跳转并等待动画完成
                performSeekWithAnimation(targetPosition, seekTransitionType)

                // 步骤3: 完成后清理和启动微动画
                completeSeekTransition(microAnimDuration, microAnimDirection, currentImageView)
            } catch (e: CancellationException) {
                // 任务被取消是正常情况
                Logger.d("MicroAnim", "Seek task cancelled: ${e.message}")
            } catch (e: Exception) {
                Logger.e("MicroAnim", "Seek task error: ${e.message}")
            } finally {
                // 无论成功还是失败，都恢复状态
                animationStateManager.setState(if (isPlaying) AnimationState.PLAYING else AnimationState.IDLE)

                // 执行等待中的暂停/恢复操作
                executePendingActions()
            }
        }
    }

    /**
     * 准备跳转过渡
     */
    private suspend fun CoroutineScope.prepareSeekTransition(
        seekTransitionType: TransitionType,
        targetPosition: Int,
        microAnimDirection: MicroAnimationConfig.TranslationDirection?
    ) {
        // 取消之前的过渡任务
        viewFlipper.removeCallbacks(transitionRunnable)

        // 确保目标位置的图片已加载
        ensureImageLoaded(targetPosition)

        // 设置指定的动画类型
        setCustomAnimations(seekTransitionType)

        // 标记正在执行跳转
        isSeeking = true

        // 在切换之前就应用微动画配置
        val targetImageView = viewFlipper.getChildAt(targetPosition) as? MicroAnimImageView
        if (targetImageView != null && microAnimDirection != null) {
            val configToUse = microAnimationConfig.copy(translationDirection = microAnimDirection)
            targetImageView.setMicroAnimationConfig(configToUse)
            targetImageView.resetToInitialState()
        }

        // 通知动画开始
        playStateListener?.onSeekAnimationStart(currentIndex, targetPosition, seekTransitionType)
    }

    /**
     * 执行跳转并等待动画完成
     */
    private suspend fun CoroutineScope.performSeekWithAnimation(
        targetPosition: Int,
        seekTransitionType: TransitionType
    ) {
        // 执行跳转
        viewFlipper.displayedChild = targetPosition
        currentIndex = targetPosition

        // 预加载相邻图片
        if (preloadEnabled) {
            preloadAdjacentImages(targetPosition)
        }

        // 等待动画完成
        safeDelay(transitionDuration)

        // 动画完成后更新状态
        isSeeking = false
        playStateListener?.onSeekAnimationEnd(targetPosition, seekTransitionType)
        playStateListener?.onImageChanged(currentIndex, imageUrls.size)

        // 恢复默认动画类型
        setCustomAnimations(transitionType)
    }

    /**
     * 完成跳转过渡
     */
    private suspend fun CoroutineScope.completeSeekTransition(
        microAnimDuration: Long?,
        microAnimDirection: MicroAnimationConfig.TranslationDirection?,
        currentImageView: MicroAnimImageView?
    ) {
        // 短暂延迟后启动新图片的微动画
        safeDelay(50)

        val newImageView = getCurrentImageView()
        if (newImageView != null && !isReleased) {
            // 如果指定了微动画方向，确保配置已应用
            if (microAnimDirection != null) {
                val configToUse = microAnimationConfig.copy(translationDirection = microAnimDirection)
                newImageView.setMicroAnimationConfig(configToUse)
            }
            newImageView.startMicroAnimation(microAnimDuration ?: microAnimationConfig.duration)
        }

        // 延迟重置之前显示的图片
        safeDelay(50) // 总共延迟100ms，与之前保持一致

        currentImageView?.resetToInitialState()

        // 如果正在播放，安排下一次过渡
        if (isPlaying) {
            scheduleNextTransition()
        }
    }
    /**
     * 直接跳转到指定位置（无动画）- 使用协程简化逻辑
     */
    fun seekToImmediate(
        position: Int,
        microAnimDuration: Long? = null,
        microAnimDirection: MicroAnimationConfig.TranslationDirection? = null
    ) {
        if (position < 0 || position >= imageUrls.size || isReleased) return

        launchSeekingTask {
            performImmediateSeek(position, microAnimDuration, microAnimDirection)
        }
    }

    /**
     * 执行立即跳转
     */
    private suspend fun CoroutineScope.performImmediateSeek(
        position: Int,
        microAnimDuration: Long?,
        microAnimDirection: MicroAnimationConfig.TranslationDirection?
    ) {
        val currentImageView = getCurrentImageView()

        // 立即重置当前图片
        currentImageView?.resetToInitialState()

        // 取消之前的过渡任务
        viewFlipper.removeCallbacks(transitionRunnable)

        // 确保目标位置的图片已加载
        ensureImageLoaded(position)

        // 在切换之前就应用微动画配置
        val targetImageView = viewFlipper.getChildAt(position) as? MicroAnimImageView
        if (targetImageView != null) {
            val configToUse = if (microAnimDirection != null) {
                microAnimationConfig.copy(translationDirection = microAnimDirection)
            } else {
                microAnimationConfig
            }

            targetImageView.setMicroAnimationConfig(configToUse)
            targetImageView.resetToInitialState()
        }

        // 执行跳转
        currentIndex = position
        viewFlipper.displayedChild = position
        isSeeking = false

        // 预加载相邻图片
        if (preloadEnabled) {
            preloadAdjacentImages(position)
        }

        playStateListener?.onImageChanged(currentIndex, imageUrls.size)

        // 立即启动新图片的微动画
        val newImageView = getCurrentImageView()
        if (newImageView != null) {
            newImageView.startMicroAnimation(microAnimDuration ?: microAnimationConfig.duration)
        }

        if (isPlaying) {
            scheduleNextTransition()
        }
    }

    private fun performTransition() {
        if (!isPlaying || imageUrls.size <= 1 || isReleased || isSeeking) return

        launchSeekingTask {
            performTransitionInternal()
        }
    }

    private suspend fun CoroutineScope.performTransitionInternal() {
        playStateListener?.onTransitionStart(transitionType)

        val nextIndex = if (loop) {
            (currentIndex + 1) % imageUrls.size
        } else {
            currentIndex + 1
        }

        if (!loop && nextIndex >= imageUrls.size) {
            stop()
            return
        }

        val currentImageView = getCurrentImageView()

        // 确保下一张图片已加载
        ensureImageLoaded(nextIndex)

        // 在切换之前就应用微动画配置
        val nextImageView = viewFlipper.getChildAt(nextIndex) as? MicroAnimImageView
        nextImageView?.setMicroAnimationConfig(microAnimationConfig)
        nextImageView?.resetToInitialState()

        viewFlipper.showNext()
        currentIndex = nextIndex

        // 预加载相邻图片
        if (preloadEnabled) {
            preloadAdjacentImages(nextIndex)
        }

        playStateListener?.onImageChanged(currentIndex, imageUrls.size)

        // 立即启动新图片的微动画
        if (isPlaying && !isReleased) {
            getCurrentImageView()?.startMicroAnimation()
        }

        // 延迟重置之前显示的图片
        safeDelay(transitionDuration + 50)
        currentImageView?.resetToInitialState()

        scheduleNextTransition()
    }

    /**
     * 确保指定位置的图片已加载
     */
    private fun ensureImageLoaded(position: Int) {
        if (position < 0 || position >= imageUrls.size) return

        val imageView = viewFlipper.getChildAt(position) as? ImageView
        if (imageView != null && !loadingPositions.contains(position)) {
            val url = imageUrls[position]
            loadImageSafely(imageView, url, position)
        }
    }

    /**
     * 获取当前状态
     */
    fun isPlaying(): Boolean = isPlaying && !isReleased

    /**
     * 是否正在执行跳转动画
     */
    fun isSeeking(): Boolean = isSeeking

    fun getCurrentPosition(): Int = currentIndex
    fun getTotalCount(): Int = imageUrls.size

    private fun scheduleNextTransition() {
        if (!isPlaying || isReleased) return

        viewFlipper.postDelayed(transitionRunnable, displayTime)
    }



    /**
     * 是否正在运行或暂停微动画
     */
    fun isMicroAnimationActive(): Boolean {
        return getCurrentImageView()?.let {
            it.isMicroAnimationRunning() || it.isMicroAnimationPaused()
        } ?: false
    }

    /**
     * 获取当前微动画状态（增强版）
     */
    fun getMicroAnimationState(): MicroAnimationState {
        val currentImageView = getCurrentImageView()
        return MicroAnimationState(
            isRunning = currentImageView?.isMicroAnimationRunning() ?: false,
            isPaused = currentImageView?.isMicroAnimationPaused() ?: false,
            config = microAnimationConfig,
            currentPosition = currentIndex,
            progress = currentImageView?.let {
                if (it.isMicroAnimationRunning() || it.isMicroAnimationPaused()) {
                    it.currentProgress
                } else {
                    0f
                }
            } ?: 0f
        )
    }

    /**
     * 增强版微动画状态数据类
     */
    data class MicroAnimationState(
        val isRunning: Boolean,
        val isPaused: Boolean,
        val config: MicroAnimationConfig,
        val currentPosition: Int,
        val progress: Float
    )
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stop()
    }

    /**
     * 调试方法：检查当前状态
     */
    fun debugCheckMicroAnimation() {
        val currentView = getCurrentImageView()
        Logger.d(
            "MicroAnim",
            "=== Debug MicroAnimation ===" +
                    "\nViewFlipper - isPlaying: $isPlaying, currentIndex: $currentIndex" +
                    "\nImageView - isAnimRunning: ${currentView?.isMicroAnimationRunning()}" +
                    "\nView Size: ${width}x${height}" +
                    "\nConfig: $microAnimationConfig" +
                    "\nScaleType: ${currentView?.scaleType}" +
                    "\n==========================="
        )
    }

    /**
     * 调试方法：强制重绘
     */
    fun debugForceRedraw() {
        getCurrentImageView()?.invalidate()
        invalidate()
        Logger.d("MicroAnim", "Forced redraw")
    }

    /**
     * 调试方法：测试明显动画
     */
    fun debugTestObviousAnimation() {
        val testConfig = MicroAnimationConfig(
            enabled = true,
            translationDirection = MicroAnimationConfig.TranslationDirection.RIGHT,
            translationDistance = 0.5f, // 大幅移动
            scaleRatio = 1.5f,          // 大幅缩放
            duration = 2000L,
            initialScale = 1.05f        // 初始放大
        )
        setMicroAnimationConfig(testConfig)
        startMicroAnimation()
        Logger.d("MicroAnim", "Test animation started with obvious parameters")
    }

}

/**
 * 动画状态管理器 - 统一管理所有动画状态
 */
private class AnimationStateManager {
    private val lock = Any()
    private var currentState: AnimationState = AnimationState.IDLE
    private val pendingActions = mutableListOf<() -> Unit>()

    enum class AnimationState {
        IDLE, PLAYING, PAUSED, SEEKING, TRANSITIONING
    }

    fun setState(newState: AnimationState) {
        synchronized(lock) {
            currentState = newState
        }
    }

    fun getState(): AnimationState {
        synchronized(lock) {
            return currentState
        }
    }

    fun canStartAnimation(): Boolean {
        synchronized(lock) {
            return currentState != AnimationState.PAUSED && currentState != AnimationState.SEEKING
        }
    }

    fun executeIfAllowed(action: () -> Unit) {
        synchronized(lock) {
            if (canStartAnimation()) {
                action()
            } else {
                pendingActions.add(action)
            }
        }
    }

    fun executePendingActions() {
        synchronized(lock) {
            val actions = pendingActions.toList()
            pendingActions.clear()

            if (canStartAnimation()) {
                actions.forEach { it() }
            } else {
                pendingActions.addAll(actions)
            }
        }
    }

    fun clearPendingActions() {
        synchronized(lock) {
            pendingActions.clear()
        }
    }
}