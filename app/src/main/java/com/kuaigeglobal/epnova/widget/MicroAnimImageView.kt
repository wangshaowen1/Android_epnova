package com.kuaigeglobal.epnova.widget

import android.content.Context
import android.graphics.*
import android.os.SystemClock
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.view.animation.Interpolator
import android.view.animation.LinearInterpolator
import androidx.appcompat.widget.AppCompatImageView
import com.nhsd.basic.utils.Logger
import kotlin.math.abs
import kotlin.math.sqrt

/** * 完全自定义绘制的微动画ImageView * 支持设置初始放大系数，确保图片始终在initialDrawRect内显示 */
class MicroAnimImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr) {
    private var microAnimationConfig: MicroAnimationConfig? = null
    private var isAnimationRunning = false
    private var pendingStart: View.OnLayoutChangeListener? = null
    // 动画相关变量
    var currentProgress = 0f
    private var animStartTime = 0L
    private var animationDuration = 0L
    // 绘制相关
    private var drawPaint = Paint().apply {
        isAntiAlias = true
        isFilterBitmap = true
        isDither = true
    }
    // 初始绘制区域 - 全程保持不变，用于裁切
    private var initialDrawRect = RectF()
    private var bitmapRect = RectF()
    // 变换参数
    private var currentScale = 1.0f
    private var currentTranslateX = 0f
    private var currentTranslateY = 0f
    // 基础缩放 - 可配置的初始放大系数
    private var baseScale = 1.0f
    // 调试
    private val DEBUG = false
    private var frameCount = 0
    // 动画状态
    private var isLoopEnabled = false
    private var isAnimationCompleted = false
    // 动画起始和结束值
    private var startTranslateX = 0f
    private var startTranslateY = 0f
    private var endTranslateX = 0f
    private var endTranslateY = 0f
    private var startScale = 1.0f
    private var endScale = 1.0f
    // 是否已经计算过初始绘制区域
    private var isInitialRectCalculated = false
    // 裁切区域 - 现在设置为initialDrawRect
    private var clipRect = RectF()
    // 避免重复计算
    private var lastCalculatedSize = Pair(0, 0)
    private val boundsRect=Rect()

    init {
        scaleType = ScaleType.FIT_CENTER
    }

    /** * 设置微动画配置 */
    fun setMicroAnimationConfig(config: MicroAnimationConfig?) {
        this.microAnimationConfig = config
        if (DEBUG) Logger.d("MicroAnim_", "Config set: $config")
        stopMicroAnimation()

        // 关键修复：立即计算动画值
        if (config != null && width > 0 && height > 0) {
            if (!isInitialRectCalculated) {
                calculateInitialDrawRect()
            }
            calculateAnimationValues(config)
        }

        resetAnimation()
        invalidate()
    }

    /** * 设置初始放大系数 */
    fun setInitialScale(scale: Float) {
        baseScale = scale.coerceAtLeast(1.0f)
        if (DEBUG) Logger.d("MicroAnim", "Initial scale set to: $baseScale")
        isInitialRectCalculated = false

        // 关键修复：重新计算动画值
        if (width > 0 && height > 0) {
            calculateInitialDrawRect()
            microAnimationConfig?.let { calculateAnimationValues(it) }
        }

        invalidate()
    }

    /** * 获取当前初始放大系数 */
    fun getInitialScale(): Float = baseScale

    /** * 开始微动画 - 支持自适应时长，修复初始位置 */
    fun startMicroAnimation(customDuration: Long? = null) {
        stopMicroAnimation()
        val config = microAnimationConfig ?: return
        if (!config.enabled || config.translationDirection == MicroAnimationConfig.TranslationDirection.NONE) {
            if (DEBUG) Logger.d("MicroAnim", "Animation disabled or direction is NONE")
            return
        }
        if (width == 0 || height == 0) {
            if (DEBUG) Logger.d("MicroAnim", "View not measured, delaying...")
            // 等待一次有效布局；停止、重配或离屏都能撤销，不在消息队列中忙等。
            pendingStart = View.OnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
                if (width > 0 && height > 0) startMicroAnimation(customDuration)
            }.also { addOnLayoutChangeListener(it) }
            return
        }
        // 确保初始绘制区域已计算
        if (!isInitialRectCalculated) {
            calculateInitialDrawRect()
        }
        // 计算动画起始和结束值
        calculateAnimationValues(config)
        // 立即设置到起始位置，确保首帧正确
        currentTranslateX = startTranslateX
        currentTranslateY = startTranslateY
        currentScale = startScale
        currentProgress = 0f
        // 计算实际平移距离用于调整时长
        val actualTranslateX = abs(endTranslateX - startTranslateX)
        val actualTranslateY = abs(endTranslateY - startTranslateY)
        val actualTranslateDistance = sqrt(actualTranslateX * actualTranslateX + actualTranslateY * actualTranslateY)
        val baseDuration = customDuration ?: config.duration
        val minTranslationForFullDuration = initialDrawRect.height() * 0.02f
        animationDuration = if (actualTranslateDistance < minTranslationForFullDuration) {
            (baseDuration * (actualTranslateDistance / minTranslationForFullDuration)).toLong().coerceAtLeast(1000L)
        } else {
            baseDuration
        }
        isAnimationRunning = true
        animStartTime = SystemClock.uptimeMillis()
        frameCount = 0
        if (DEBUG) Logger.d("MicroAnim",
            "Animation started - " +
                    "startPosition: ($startTranslateX, $startTranslateY), " +
                    "actualDistance: ${"%.1f".format(actualTranslateDistance)}, " +
                    "duration: $animationDuration")
        // 立即重绘，显示起始位置
        invalidate()
        postOnAnimation(animationRunnable)
    }

    /** * 停止微动画 */
    fun stopMicroAnimation() {
        cancelPendingStart()
        isAnimationRunning = false
        isAnimationCompleted = false
        removeCallbacks(animationRunnable)
        resetAnimation()
        invalidate()
    }

    private fun cancelPendingStart() {
        pendingStart?.let { removeOnLayoutChangeListener(it) }
        pendingStart = null
    }

    fun resetToInitialState() {
        stopMicroAnimation()
        currentProgress = 0f
        val config = microAnimationConfig
        if (config != null && config.enabled && config.translationDirection != MicroAnimationConfig.TranslationDirection.NONE) {
            // 关键修复：确保重新计算动画值
            calculateAnimationValues(config)
            // 设置到起始位置
            currentTranslateX = startTranslateX
            currentTranslateY = startTranslateY
            currentScale = startScale
            if (DEBUG) Logger.d("MicroAnim", "Reset to initial state with direction ${config.translationDirection}: ($currentTranslateX, $currentTranslateY, $currentScale)")
            // 立即重绘，确保首帧显示正确位置
            invalidate()
        } else {
            // 无动画配置时，使用居中位置
            currentTranslateX = 0f
            currentTranslateY = 0f
            currentScale = baseScale
            if (DEBUG) Logger.d("MicroAnim", "Reset to center state: ($currentTranslateX, $currentTranslateY, $currentScale)")
            invalidate()
        }
        frameCount = 0
    }

    /** * 是否正在运行动画 */
    fun isMicroAnimationRunning(): Boolean = isAnimationRunning

    private fun resetAnimation() {
        currentProgress = 0f
        val config = microAnimationConfig
        if (config != null && config.enabled && config.translationDirection != MicroAnimationConfig.TranslationDirection.NONE) {
            // 确保动画值已计算 - 使用配置跟踪
            if (lastCalculatedConfig != config) {
                calculateAnimationValues(config)
            }
            // 设置当前状态为起始位置
            currentTranslateX = startTranslateX
            currentTranslateY = startTranslateY
            currentScale = startScale
        } else {
            // 无动画配置或禁用时，使用居中位置
            currentTranslateX = 0f
            currentTranslateY = 0f
            currentScale = baseScale
        }
        if (DEBUG) Logger.d("MicroAnim", "Reset animation: ($currentTranslateX, $currentTranslateY, $currentScale)")
        frameCount = 0
    }

    /** * 计算动画起始和结束值 - 修复版：确保在initialDrawRect内平移 */
    private fun calculateAnimationValues(config: MicroAnimationConfig) {
        // 计算图片实际可平移的最大距离
        val (maxAllowedTranslateX, maxAllowedTranslateY) = calculateMaxAllowedTranslation()
        // 计算期望的平移距离（基于配置的比例）
        val desiredTranslateX = initialDrawRect.width() * config.translationDistance
        val desiredTranslateY = initialDrawRect.height() * config.translationDistance
        // 计算缩放范围
        val scaleRange = if (config.scaleRatio > 1.0f) {
            config.scaleRatio - 1.0f
        } else {
            0f
        }

        // 根据方向设置起始和结束位置，从一侧移动到另一侧
        when (config.translationDirection) {
            MicroAnimationConfig.TranslationDirection.LEFT -> {
                // 从右边移动到左边
                startTranslateX = desiredTranslateX.coerceAtMost(maxAllowedTranslateX)
                startTranslateY = 0f
                endTranslateX = -desiredTranslateX.coerceAtMost(maxAllowedTranslateX)
                endTranslateY = 0f
                startScale = baseScale
                endScale = baseScale + scaleRange
            }
            MicroAnimationConfig.TranslationDirection.RIGHT -> {
                // 从左边移动到右边
                startTranslateX = -desiredTranslateX.coerceAtMost(maxAllowedTranslateX)
                startTranslateY = 0f
                endTranslateX = desiredTranslateX.coerceAtMost(maxAllowedTranslateX)
                endTranslateY = 0f
                startScale = baseScale
                endScale = baseScale + scaleRange
            }
            MicroAnimationConfig.TranslationDirection.TOP -> {
                // 从底部移动到顶部
                startTranslateX = 0f
                startTranslateY = desiredTranslateY.coerceAtMost(maxAllowedTranslateY)
                endTranslateX = 0f
                endTranslateY = -desiredTranslateY.coerceAtMost(maxAllowedTranslateY)
                startScale = baseScale
                endScale = baseScale + scaleRange
            }
            MicroAnimationConfig.TranslationDirection.BOTTOM -> {
                // 从顶部移动到底部
                startTranslateX = 0f
                startTranslateY = -desiredTranslateY.coerceAtMost(maxAllowedTranslateY)
                endTranslateX = 0f
                endTranslateY = desiredTranslateY.coerceAtMost(maxAllowedTranslateY)
                startScale = baseScale
                endScale = baseScale + scaleRange
            }
            MicroAnimationConfig.TranslationDirection.TOP_LEFT -> {
                // 从右下角移动到左上角
                startTranslateX = desiredTranslateX.coerceAtMost(maxAllowedTranslateX)
                startTranslateY = desiredTranslateY.coerceAtMost(maxAllowedTranslateY)
                endTranslateX = -desiredTranslateX.coerceAtMost(maxAllowedTranslateX)
                endTranslateY = -desiredTranslateY.coerceAtMost(maxAllowedTranslateY)
                startScale = baseScale
                endScale = baseScale + scaleRange
            }
            MicroAnimationConfig.TranslationDirection.TOP_RIGHT -> {
                // 从左下角移动到右上角
                startTranslateX = -desiredTranslateX.coerceAtMost(maxAllowedTranslateX)
                startTranslateY = desiredTranslateY.coerceAtMost(maxAllowedTranslateY)
                endTranslateX = desiredTranslateX.coerceAtMost(maxAllowedTranslateX)
                endTranslateY = -desiredTranslateY.coerceAtMost(maxAllowedTranslateY)
                startScale = baseScale
                endScale = baseScale + scaleRange
            }
            MicroAnimationConfig.TranslationDirection.BOTTOM_LEFT -> {
                // 从右上角移动到左下角
                startTranslateX = desiredTranslateX.coerceAtMost(maxAllowedTranslateX)
                startTranslateY = -desiredTranslateY.coerceAtMost(maxAllowedTranslateY)
                endTranslateX = -desiredTranslateX.coerceAtMost(maxAllowedTranslateX)
                endTranslateY = desiredTranslateY.coerceAtMost(maxAllowedTranslateY)
                startScale = baseScale
                endScale = baseScale + scaleRange
            }
            MicroAnimationConfig.TranslationDirection.BOTTOM_RIGHT -> {
                // 从左上角移动到右下角
                startTranslateX = -desiredTranslateX.coerceAtMost(maxAllowedTranslateX)
                startTranslateY = -desiredTranslateY.coerceAtMost(maxAllowedTranslateY)
                endTranslateX = desiredTranslateX.coerceAtMost(maxAllowedTranslateX)
                endTranslateY = desiredTranslateY.coerceAtMost(maxAllowedTranslateY)
                startScale = baseScale
                endScale = baseScale + scaleRange
            }
            else -> {
                // 无方向或NONE
                startTranslateX = 0f
                startTranslateY = 0f
                endTranslateX = 0f
                endTranslateY = 0f
                startScale = baseScale
                endScale = baseScale
            }
        }
        lastCalculatedConfig = config
        if (DEBUG) Logger.d("MicroAnim",
            "Animation values - " +
                    "start: ($startTranslateX, $startTranslateY, $startScale) -> " +
                    "end: ($endTranslateX, $endTranslateY, $endScale)")
    }

    /** * 计算最大允许平移距离 - 修复版：基于initialDrawRect尺寸计算 */
    private fun calculateMaxAllowedTranslation(): Pair<Float, Float> {
        // 关键修复：基于initialDrawRect的尺寸计算最大平移距离
        // 确保平移不会超出initialDrawRect区域
        // 计算缩放后的图片超出initialDrawRect的部分
        // 这部分就是我们可以平移的最大距离
        val drawable = drawable ?: return Pair(0f, 0f)
        val drawableWidth = drawable.intrinsicWidth
        val drawableHeight = drawable.intrinsicHeight
        if (drawableWidth <= 0 || drawableHeight <= 0) {
            return Pair(0f, 0f)
        }
        // 计算缩放后的图片实际尺寸
        val scaledWidth = initialDrawRect.width()
        val scaledHeight = initialDrawRect.height()
        // 计算图片超出initialDrawRect的部分
        // 由于图片已经填充了整个initialDrawRect，所以超出的部分为0
        // 但我们允许在initialDrawRect内进行微小的平移
        val maxTranslateX = scaledWidth * 0.05f // 允许在宽度方向平移5%
        val maxTranslateY = scaledHeight * 0.05f // 允许在高度方向平移5%
        if (DEBUG) Logger.d("MicroAnim",
            "Max translation - " +
                    "scaledSize: ($scaledWidth, $scaledHeight), " +
                    "initialDrawRect: $initialDrawRect, " +
                    "maxTranslate: ($maxTranslateX, $maxTranslateY)")
        return Pair(maxTranslateX, maxTranslateY)
    }

    /** * 计算初始图片绘制区域 - 修复版：确保图片完全填充initialDrawRect */
    private fun calculateInitialDrawRect() {
        // 避免重复计算
        if (isInitialRectCalculated && width == lastCalculatedSize.first && height == lastCalculatedSize.second) {
            return
        }
        val drawable = drawable ?: return
        val drawableWidth = drawable.intrinsicWidth
        val drawableHeight = drawable.intrinsicHeight
        val viewWidth = width
        val viewHeight = height
        if (drawableWidth <= 0 || drawableHeight <= 0 || viewWidth <= 0 || viewHeight <= 0) {
            initialDrawRect.set(0f, 0f, viewWidth.toFloat(), viewHeight.toFloat())
            clipRect.set(initialDrawRect)
            isInitialRectCalculated = true
            lastCalculatedSize = Pair(viewWidth, viewHeight)
            return
        }
        // 计算 FIT_CENTER 的显示区域
        val scaleX = viewWidth.toFloat() / drawableWidth
        val scaleY = viewHeight.toFloat() / drawableHeight
        val scale = scaleX.coerceAtMost(scaleY)
        // 使用配置的初始放大系数
        // 如果没有特别设置，使用默认的1.02f
        if (baseScale == 1.0f) {
            baseScale = 1.02f // 默认轻微放大
        }
        val finalScale = scale * baseScale
        val scaledWidth = drawableWidth * finalScale
        val scaledHeight = drawableHeight * finalScale
        val left = (viewWidth - scaledWidth) * 0.5f
        val top = (viewHeight - scaledHeight) * 0.5f
        // 设置initialDrawRect为图片实际显示区域
        initialDrawRect.set(left, top, left + scaledWidth, top + scaledHeight)
        bitmapRect.set(0f, 0f, drawableWidth.toFloat(), drawableHeight.toFloat())
        // 关键修复：设置裁切区域为initialDrawRect，确保图片不会超出这个区域
        clipRect.set(initialDrawRect)
        isInitialRectCalculated = true
        lastCalculatedSize = Pair(viewWidth, viewHeight)
        // 关键修复：计算完成后立即计算动画值
        val config = microAnimationConfig
        if (config != null && config.enabled && config.translationDirection != MicroAnimationConfig.TranslationDirection.NONE) {
            calculateAnimationValues(config)
        }
        if (DEBUG) Logger.d("MicroAnim",
            "InitialDrawRect - " +
                    "drawable: ${drawableWidth}x$drawableHeight, " +
                    "view: ${viewWidth}x$viewHeight, " +
                    "scale: $finalScale, " +
                    "rect: $initialDrawRect, " +
                    "baseScale: $baseScale")
    }
    override fun onDraw(canvas: Canvas) {
        val drawable = drawable ?: run {
            super.onDraw(canvas)
            return
        }
        if (width == 0 || height == 0) {
            super.onDraw(canvas)
            return
        }

        // 确保初始绘制区域已计算
        if (!isInitialRectCalculated) {
            calculateInitialDrawRect()
        }

        // 保存画布状态
        canvas.save()
        try {
            // 关键修复：先裁切到initialDrawRect，确保图片不会超出这个区域
            canvas.clipRect(clipRect)

            val centerX = initialDrawRect.centerX()
            val centerY = initialDrawRect.centerY()

            // 应用变换 - 简化逻辑
            applyCurrentTransformation(canvas, centerX, centerY)

            // 安全绘制
            val currentDrawable = drawable
            boundsRect.set(initialDrawRect.left.toInt(),
                initialDrawRect.top.toInt(),
                initialDrawRect.right.toInt(),
                initialDrawRect.bottom.toInt()
            )
            currentDrawable.bounds = boundsRect
            currentDrawable.draw(canvas)
        } catch (e: Exception) {
            Logger.e("MicroAnim", "Error in onDraw")
            // 出错时回退到默认绘制
            super.onDraw(canvas)
        } finally {
            canvas.restore()
        }

        // 调试信息
        if (DEBUG && (isAnimationRunning || isAnimationCompleted)) {
            drawDebugInfo(canvas)
        }
    }

    // 更好的判断方式：检查是否已经为当前配置计算过动画值
    private var lastCalculatedConfig: MicroAnimationConfig? = null

    private fun applyCurrentTransformation(canvas: Canvas, centerX: Float, centerY: Float) {
        val config = microAnimationConfig
        val shouldAnimate = isAnimationRunning && config?.enabled == true
        val shouldKeepEndState = !isAnimationRunning && isAnimationCompleted && config?.enabled == true
        val isPaused = isMicroAnimationPaused() && config?.enabled == true

        if (shouldAnimate || shouldKeepEndState || isPaused) {
            // 动画状态、结束状态或暂停状态：应用当前变换
            canvas.scale(currentScale, currentScale, centerX, centerY)
            canvas.translate(currentTranslateX, currentTranslateY)
            if (DEBUG && isPaused) {
                Log.d("MicroAnim", "Drawing paused frame - translate: ($currentTranslateX, $currentTranslateY), scale: $currentScale")
            }
        } else {
            // 非动画状态：确保使用正确的起始位置
            val effectiveConfig = microAnimationConfig
            if (effectiveConfig?.enabled == true &&
                effectiveConfig.translationDirection != MicroAnimationConfig.TranslationDirection.NONE) {
                // 关键修复：如果配置变化或未计算，重新计算
                if (lastCalculatedConfig != effectiveConfig) {
                    calculateAnimationValues(effectiveConfig)
                    lastCalculatedConfig = effectiveConfig
                }
                canvas.scale(startScale, startScale, centerX, centerY)
                canvas.translate(startTranslateX, startTranslateY)
            } else {
                // 无动画配置时，使用基础缩放和居中位置
                canvas.scale(baseScale, baseScale, centerX, centerY)
            }
        }
    }

    /** * 绘制调试信息 */
    private fun drawDebugInfo(canvas: Canvas) {
        if (!DEBUG) return
        canvas.save()
        try {
            // 在屏幕坐标中绘制
            val identityMatrix = Matrix()
            canvas.setMatrix(identityMatrix)
            // 绘制图片显示区域边框
            val borderPaint = Paint().apply {
                color = Color.RED
                style = Paint.Style.STROKE
                strokeWidth = 3f
                isAntiAlias = true
            }
            canvas.drawRect(initialDrawRect, borderPaint)
            // 绘制裁切区域边框（绿色）
            val clipPaint = Paint().apply {
                color = Color.GREEN
                style = Paint.Style.STROKE
                strokeWidth = 2f
                isAntiAlias = true
            }
            canvas.drawRect(clipRect, clipPaint)
            // 绘制动画信息文本
            val textPaint = Paint().apply {
                color = Color.RED
                textSize = 30f
                isAntiAlias = true
            }
            val config = microAnimationConfig
            if (config != null) {
                val state = when {
                    isAnimationRunning -> "RUNNING"
                    isAnimationCompleted -> "COMPLETED"
                    else -> "STOPPED"
                }
                val info = "State: $state\n" +
                        "Progress: ${"%.2f".format(currentProgress)}\n" +
                        "Scale: ${"%.2f".format(currentScale)}\n" +
                        "Translate: (${"%.1f".format(currentTranslateX)}, ${"%.1f".format(currentTranslateY)})\n" +
                        "Direction: ${config.translationDirection}\n" +
                        "InitialScale: ${"%.2f".format(baseScale)}"
                val lines = info.split("\n")
                var yPos = initialDrawRect.top - 150
                lines.forEach { line ->
                    canvas.drawText(line, initialDrawRect.left, yPos, textPaint)
                    yPos += 35
                }
            }
        } catch (e: Exception) {
            Logger.e("MicroAnim", "Error drawing debug info")
        } finally {
            canvas.restore()
        }
    }

    /** * 更新动画参数 */
    private fun updateAnimation(config: MicroAnimationConfig) {
        val interpolatedProgress = config.interpolator.getInterpolation(currentProgress)
        currentTranslateX = startTranslateX + (endTranslateX - startTranslateX) * interpolatedProgress
        currentTranslateY = startTranslateY + (endTranslateY - startTranslateY) * interpolatedProgress
        currentScale = startScale + (endScale - startScale) * interpolatedProgress
        frameCount++
        if (DEBUG && frameCount % 30 == 0) {
            Logger.d("MicroAnim",
                "Frame $frameCount - " +
                        "Progress: ${"%.3f".format(currentProgress)}, " +
                        "Scale: ${"%.3f".format(currentScale)}, " +
                        "Translate: (${"%.1f".format(currentTranslateX)}, ${"%.1f".format(currentTranslateY)}")
        }
        invalidate()
    }

    /** * 动画Runnable */
    private val animationRunnable = object : Runnable {
        override fun run() {
            if (!isAnimationRunning) return
            val currentTime = SystemClock.uptimeMillis()
            val elapsed = currentTime - animStartTime
            val effectiveDuration = animationDuration.coerceAtLeast(16L)
            if (elapsed >= effectiveDuration) {
                if (isLoopEnabled) {
                    animStartTime = currentTime
                    currentProgress = 0f
                    if (DEBUG) Logger.d("MicroAnim", "Animation looped")
                } else {
                    isAnimationRunning = false
                    isAnimationCompleted = true
                    removeCallbacks(this)
                    currentTranslateX = endTranslateX
                    currentTranslateY = endTranslateY
                    currentScale = endScale
                    currentProgress = 1.0f
                    if (DEBUG) Logger.d("MicroAnim", "Animation completed")
                    invalidate()
                    return
                }
            } else {
                currentProgress = elapsed.toFloat() / effectiveDuration
            }
            val config = microAnimationConfig ?: return
            updateAnimation(config)
            postOnAnimation(this)
        }
    }

    /** * 检查是否可以进行有意义的平移 */
    fun hasMeaningfulTranslation(): Boolean {
        if (!isInitialRectCalculated) {
            calculateInitialDrawRect()
        }
        val (maxTranslateX, maxTranslateY) = calculateMaxAllowedTranslation()
        val config = microAnimationConfig ?: return false
        if (!config.enabled || config.translationDirection == MicroAnimationConfig.TranslationDirection.NONE) {
            return false
        }
        // 如果最大平移距离很小，认为没有有意义的平移
        val minMeaningfulTranslation = initialDrawRect.height() * 0.01f // 1% 高度作为阈值
        return maxTranslateX > minMeaningfulTranslation || maxTranslateY > minMeaningfulTranslation
    }

    /** * 获取当前可平移状态信息（用于调试） */
    fun getTranslationInfo(): String {
        if (!isInitialRectCalculated) {
            calculateInitialDrawRect()
        }
        val (maxTranslateX, maxTranslateY) = calculateMaxAllowedTranslation()
        val config = microAnimationConfig
        return "MaxTranslation: (${"%.1f".format(maxTranslateX)}, ${"%.1f".format(maxTranslateY)}), " +
                "Config: ${config?.translationDirection}, " +
                "HasMeaningful: ${hasMeaningfulTranslation()}, " +
                "InitialScale: ${"%.2f".format(baseScale)}"
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (DEBUG) Logger.d("MicroAnim", "onSizeChanged: $w x $h")
        isInitialRectCalculated = false
        calculateInitialDrawRect()
        invalidate()
    }

    override fun setImageDrawable(drawable: android.graphics.drawable.Drawable?) {
        super.setImageDrawable(drawable)
        isInitialRectCalculated = false
        calculateInitialDrawRect()
        invalidate()
    }

    override fun setImageResource(resId: Int) {
        super.setImageResource(resId)
        isInitialRectCalculated = false
        calculateInitialDrawRect()
        invalidate()
    }

    override fun setImageBitmap(bm: Bitmap?) {
        super.setImageBitmap(bm)
        isInitialRectCalculated = false
        calculateInitialDrawRect()
        invalidate()
    }

    /** * 暂停微动画 - 停留在当前帧 */
    fun pauseMicroAnimation() {
        cancelPendingStart()
        if (!isAnimationRunning) return

        isAnimationRunning = false
        removeCallbacks(animationRunnable)

        // 计算并保存当前进度
        if (animStartTime > 0) {
            val elapsed = SystemClock.uptimeMillis() - animStartTime
            val effectiveDuration = animationDuration.coerceAtLeast(16L)
            currentProgress = (elapsed.toFloat() / effectiveDuration).coerceIn(0f, 1f)

            // 关键：确保当前变换参数基于当前进度
            val config = microAnimationConfig
            if (config != null) {
                val interpolatedProgress = config.interpolator.getInterpolation(currentProgress)
                currentTranslateX = startTranslateX + (endTranslateX - startTranslateX) * interpolatedProgress
                currentTranslateY = startTranslateY + (endTranslateY - startTranslateY) * interpolatedProgress
                currentScale = startScale + (endScale - startScale) * interpolatedProgress
            }
        }

        if (DEBUG) Log.d("MicroAnim",
            "Animation paused - progress: $currentProgress, " +
                    "transform: ($currentTranslateX, $currentTranslateY, $currentScale)")

        // 重要：重绘以显示当前帧
        invalidate()
    }

    /** * 恢复微动画 - 从暂停的位置继续 */
    fun resumeMicroAnimation() {
        if (isAnimationRunning || microAnimationConfig?.enabled != true) return

        val config = microAnimationConfig ?: return
        if (!config.enabled || config.translationDirection == MicroAnimationConfig.TranslationDirection.NONE) {
            return
        }

        // 确保初始绘制区域已计算
        if (!isInitialRectCalculated) {
            calculateInitialDrawRect()
        }

        // 重新计算剩余时间和起始时间
        val remainingDuration = (animationDuration * (1 - currentProgress)).toLong().coerceAtLeast(16L)

        isAnimationRunning = true
        animStartTime = SystemClock.uptimeMillis() - (animationDuration * currentProgress).toLong()

        if (DEBUG) Log.d("MicroAnim",
            "Animation resumed - " +
                    "progress: $currentProgress, " +
                    "remainingDuration: $remainingDuration, " +
                    "transform: ($currentTranslateX, $currentTranslateY, $currentScale)")

        // 立即重绘，然后继续动画
        invalidate()
        postOnAnimation(animationRunnable)
    }

    /** * 是否处于暂停状态 */
    fun isMicroAnimationPaused(): Boolean {
        return !isAnimationRunning && currentProgress > 0f && currentProgress < 1f
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopMicroAnimation()
    }

    /** * 获取调试信息 */
    fun getDebugInfo(): String {
        val state = when {
            isAnimationRunning -> "RUNNING"
            isAnimationCompleted -> "COMPLETED"
            else -> "STOPPED"
        }
        return "State: $state, " +
                "InitialDrawRect: $initialDrawRect, Progress: $currentProgress, " +
                "Scale: $currentScale, " +
                "Translate: ($currentTranslateX, $currentTranslateY), " +
                "InitialScale: ${"%.2f".format(baseScale)}"
    }
}

/** * 微动画配置类 - 增强版：支持初始放大系数 */
data class MicroAnimationConfig(
    val translationDirection: TranslationDirection = TranslationDirection.NONE,
    val translationDistance: Float = 0.3f,
    val scaleRatio: Float = 1.1f,
    var duration: Long = 4000L,
    val interpolator: Interpolator = LinearInterpolator(),
    val enabled: Boolean = true,
    var initialScale: Float = 1.02f // 新增：初始放大系数，默认1.02表示轻微放大
) {
    enum class TranslationDirection {
        NONE, LEFT, RIGHT, TOP, BOTTOM,
        TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT
    }

    fun copyWithDuration(newDuration: Long): MicroAnimationConfig {
        return this.copy(duration = newDuration)
    }

    fun copyWithInitialScale(newInitialScale: Float): MicroAnimationConfig {
        return this.copy(initialScale = newInitialScale)
    }
}
