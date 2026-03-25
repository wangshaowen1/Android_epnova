package com.kuaigeglobal.epnova.widget.htmlview

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.AbsoluteSizeSpan
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.text.style.ImageSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.text.style.SubscriptSpan
import android.text.style.SuperscriptSpan
import android.text.style.UnderlineSpan
import android.widget.TextView
import com.nhsd.basic.utils.Logger
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.TextNode
import androidx.core.graphics.toColorInt
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import kotlin.text.toFloat

/**
 * *****************************************
 * 项目名称: KidsTales
 * @Author: SZYMN
 * @Introduce:HTML样式解析工具类,支持解析HTML中的样式属性并应用到TextView
 * @Data: 2025/8/22/周五  10:45
 * @Version: 1.0
 * *****************************************
 */
object HtmlStyleParser {

    private const val TAG = "HtmlStyleParser"

    enum class ImgAlignmentEnum {
        //基线对齐
        BASELINE,

        //居中对齐
        CENTER
    }


    // 存储样式应用信息的数据类
    private data class StyleApplication(
        val start: Int,
        val end: Int,
        val spans: List<Any>
    )

    /**
     * 解析HTML字符串并返回带有样式的Spanned文本
     * @param html HTML字符串
     * @param context 上下文对象（用于单位转换）
     * @return 带有样式的Spanned文本
     */
    fun parseHtml(
        html: String,
        targetTextView: TextView,
        imgAlignmentEnum: ImgAlignmentEnum = ImgAlignmentEnum.BASELINE,
        context: Context? = null
    ): Spanned {
        return try {
            val doc = Jsoup.parse(html)
            val builder = SpannableStringBuilder()
            val styleApplications = mutableListOf<StyleApplication>()

            processNode(
                targetTextView = targetTextView,
                builder = builder,
                styleApplications = styleApplications,
                imgAlignmentEnum = imgAlignmentEnum,
                element = doc.body(),
                context = context
            )
            // 按照从内到外的顺序应用样式（确保内层样式不会被外层覆盖）
            for (application in styleApplications.reversed()) {
                for (span in application.spans) {
                    builder.setSpan(
                        span,
                        application.start,
                        application.end,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
            }

            builder
        } catch (e: Exception) {
            Logger.e(TAG, "解析HTML时出错: ${e.message}")
            SpannableStringBuilder(html.replace(Regex("<[^>]*>"), ""))
        }
    }


    /**
     * 清除HTML标签，只保留纯文本
     * @param html HTML字符串
     * @return 纯文本
     */
    fun stripHtml(html: String): String {
        return Jsoup.parse(html).text()
    }

    /**
     * 处理HTML节点
     */
    private fun processNode(
        targetTextView: TextView,
        builder: SpannableStringBuilder,
        styleApplications: MutableList<StyleApplication>,
        imgAlignmentEnum: ImgAlignmentEnum = ImgAlignmentEnum.BASELINE,
        element: Element,
        context: Context?
    ) {
        for (node in element.childNodes()) {
            when (node) {
                is TextNode -> {
                    // 文本节点直接添加
                    builder.append(node.text())
                }

                is Element -> {
                    when (node.tagName()) {
                        "img" -> {
                            handleImageTag(
                                targetTextView = targetTextView,
                                builder = builder,
                                imgAlignmentEnum = imgAlignmentEnum,
                                element = node,
                                context = context
                            )
                        }

                        else -> {
                            // 元素节点递归处理
                            val start = builder.length
                            processNode(
                                targetTextView = targetTextView,
                                builder = builder,
                                styleApplications = styleApplications,
                                imgAlignmentEnum = imgAlignmentEnum,
                                element = node,
                                context = context
                            )
                            val end = builder.length
                            // 收集样式应用信息，稍后统一处理
                            collectStyles(
                                styleApplications = styleApplications,
                                start = start,
                                end = end,
                                element = node
                            )
                        }
                    }
                }
            }
        }
    }

    private fun handleImageTag(
        targetTextView: TextView,
        builder: SpannableStringBuilder,
        imgAlignmentEnum: ImgAlignmentEnum = ImgAlignmentEnum.BASELINE,
        element: Element,
        context: Context?
    ) {
        val src = element.attr("src")
        if (context == null) return
        if (!src.isNotEmpty()) return
        // 添加占位符
        val placeholder = " "
        val start = builder.length
        builder.append(placeholder)
        val end = builder.length
        Logger.i(TAG, "图片地址：$src")
        // 加载图片
        Glide.with(context)
            .load(src)
            .into(object : CustomTarget<Drawable>() {
                override fun onResourceReady(
                    resource: Drawable,
                    transition: Transition<in Drawable>?
                ) {
                    setImage(
                        targetTextView = targetTextView,
                        builder = builder,
                        drawable = resource,
                        imgAlignmentEnum = imgAlignmentEnum,
                        element = element,
                        start = start,
                        end = end,
                        context = context
                    )
                }

                override fun onLoadCleared(placeholder: Drawable?) {
                    // 加载被清除时不做任何操作
                }

                override fun onLoadFailed(errorDrawable: Drawable?) {
                    Logger.e(TAG, "加载图片失败")
                }
            })
    }

    private fun setImage(
        targetTextView: TextView,
        builder: SpannableStringBuilder,
        imgAlignmentEnum: ImgAlignmentEnum = ImgAlignmentEnum.BASELINE,
        drawable: Drawable?,
        element: Element,
        start: Int,
        end: Int,
        context: Context?
    ) {
        if (drawable == null) return
        // 处理图片尺寸
        val sizedDrawable = processImageSize(drawable, element, context)
        val finalDrawable = processImageStyleSize(sizedDrawable, element, context)

        // 创建 ImageSpan
        val imageSpan = when (imgAlignmentEnum) {
            ImgAlignmentEnum.CENTER -> CenterAlignImageSpan(finalDrawable)
            ImgAlignmentEnum.BASELINE -> ImageSpan(finalDrawable)
        }

        // 在主线程中更新 UI
        Handler(Looper.getMainLooper()).post {
            try {
                val currentText = builder.toString()
                if (start < currentText.length && end <= currentText.length) {
                    Logger.i(TAG, "设置图片Span: $start - $end")
                    builder.setSpan(
                        imageSpan,
                        start,
                        end,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )

                    // 刷新TextView - 使用 postInvalidate 而不是 invalidate
                    targetTextView.postInvalidate()
                    targetTextView.text = builder
                }
            } catch (e: Exception) {
                Logger.e(TAG, "设置图片Span时出错: ${e.message}")
            }
        }
    }


    /**
     * 应用样式到文本
     */
    private fun collectStyles(
        styleApplications: MutableList<StyleApplication>,
        start: Int,
        end: Int,
        element: Element,
    ) {
        if (start >= end) return

        val spans = mutableListOf<Any>()

        // 处理标签特定的样式
        when (element.tagName()) {
            "b", "strong" -> {
                spans.add(StyleSpan(android.graphics.Typeface.BOLD))
            }

            "i", "em" -> {
                spans.add(StyleSpan(android.graphics.Typeface.ITALIC))
            }

            "u" -> {
                spans.add(UnderlineSpan())
            }

            "strike", "s" -> {
                spans.add(StrikethroughSpan())
            }

            "sub" -> {
                spans.add(SubscriptSpan())
            }

            "sup" -> {
                spans.add(SuperscriptSpan())
            }
        }

        // 处理内联样式
        val style = element.attr("style")
        if (style.isNotEmpty()) {
            val styles = parseStyle(style)
            // 应用颜色
            styles["color"]?.let { colorStr ->
                try {
                    val color = parseColor(colorStr)
                    spans.add(ForegroundColorSpan(color))
                } catch (e: Exception) {
                    Logger.i(TAG, "分析颜色时出错: $colorStr  $e")
                }
            }

            // 应用背景色
            styles["background-color"]?.let { bgColorStr ->
                try {
                    val color = parseColor(bgColorStr)
                    spans.add(BackgroundColorSpan(color))
                } catch (e: Exception) {
                    Logger.e(TAG, "Error parsing background color: $bgColorStr  $e")
                }
            }

            // 应用字体大小
            styles["font-size"]?.let { fontSizeStr ->
                try {
                    when {
                        fontSizeStr.endsWith("em") -> {
                            // 相对大小
                            val size = fontSizeStr.removeSuffix("em").toFloat()
                            spans.add(RelativeSizeSpan(size))
                        }

                        fontSizeStr.endsWith("px") -> {
                            // 绝对大小（像素）
                            val size = fontSizeStr.removeSuffix("px").toInt()
                            spans.add(AbsoluteSizeSpan(size))
                        }

                        fontSizeStr.endsWith("sp") -> {
                            // 绝对大小（缩放像素）
                            val size = fontSizeStr.removeSuffix("sp").toInt()
                            spans.add(AbsoluteSizeSpan(size, true))
                        }
                    }
                } catch (e: Exception) {
                    Logger.e(TAG, "Error parsing font-size: $fontSizeStr $e")
                }
            }

            // 添加字体粗细支持
            styles["font-weight"]?.let { fontWeight ->
                when {
                    fontWeight == "bold" || fontWeight.toIntOrNull()?.let { it >= 600 } == true -> {
                        spans.add(StyleSpan(android.graphics.Typeface.BOLD))
                    }

                    fontWeight == "normal" || fontWeight.toIntOrNull()
                        ?.let { it < 600 } == true -> {
                        spans.add(StyleSpan(android.graphics.Typeface.NORMAL))
                    }
                }
            }


            // 添加文字装饰支持（下划线、删除线等）
            styles["text-decoration"]?.let { textDecoration ->
                when {
                    textDecoration.contains("underline") -> spans.add(UnderlineSpan())
                    textDecoration.contains("line-through") -> spans.add(StrikethroughSpan())
                    // 可以添加更多文字装饰支持
                }
            }

            // 添加字体样式支持（斜体）
            styles["font-style"]?.let { fontStyle ->
                if (fontStyle == "italic") {
                    spans.add(StyleSpan(android.graphics.Typeface.ITALIC))
                }
            }

            // 添加文字对齐支持（需要特殊处理，通常应用于整个TextView）
            styles["text-align"]?.let { textAlign ->
                // 注意：文字对齐通常应用于整个TextView而不是部分文本
                // 这里只是示例，实际应用中可能需要特殊处理
                Logger.e(TAG, "对齐: $textAlign (通常需要应用于整个TextView)")
            }

            // 添加行高支持
            styles["line-height"]?.let { lineHeight ->
                try {
                    if (lineHeight.endsWith("px")) {
                        val size = lineHeight.removeSuffix("px").toInt()
                        // 注意：Android中没有直接的行高Span，可能需要自定义
                        // 这里只是示例
                        Logger.e(TAG, "线条高度: $size px (需要自定义实现)")
                    } else if (lineHeight.endsWith("em")) {
                        val size = lineHeight.removeSuffix("em").toFloat()
                        Logger.e(TAG, "线条高度: $size em (需要自定义实现)")
                    }
                } catch (e: Exception) {
                    Logger.e(TAG, "Error parsing line-height: $lineHeight $e")
                }
            }


            // 添加文字阴影支持
            styles["text-shadow"]?.let { textShadow ->
                // 解析文字阴影，格式通常为: h-shadow v-shadow blur-radius color
                try {
                    val parts = textShadow.split(" ")
                    if (parts.size >= 3) {
                        val hShadow = parts[0].replace("px", "").toFloat()
                        val vShadow = parts[1].replace("px", "").toFloat()
                        val blurRadius = parts[2].replace("px", "").toFloat()
                        val color = if (parts.size >= 4) parseColor(parts[3]) else Color.BLACK

                        // 注意：Android中没有直接的文字阴影Span，可能需要自定义
                        Logger.e(
                            TAG,
                            "文本阴影: $hShadow $vShadow $blurRadius $color (需要自定义实现)"
                        )
                    }
                } catch (e: Exception) {
                    Logger.e(TAG, "解析文本阴影时出错: $textShadow $e")
                }
            }

            // 添加字母间距支持
            styles["letter-spacing"]?.let { letterSpacing ->
                try {
                    if (letterSpacing.endsWith("px")) {
                        val spacing = letterSpacing.removeSuffix("px").toFloat()
                        // 注意：Android中没有直接的字母间距Span，可能需要自定义
                        Logger.e(TAG, "字母间距: $spacing px (需要自定义实现)")
                    } else if (letterSpacing.endsWith("em")) {
                        val spacing = letterSpacing.removeSuffix("em").toFloat()
                        Logger.e(TAG, "字母间距: $spacing em (需要自定义实现)")
                    }
                } catch (e: Exception) {
                    Logger.e(TAG, "解析字母间距时出错: $letterSpacing $e")
                }
            }

            // 添加文字变形支持（大写、小写等）
            styles["text-transform"]?.let { textTransform ->
                when (textTransform) {
                    "uppercase" -> {
                        // 注意：Android中没有直接的文字变形Span，需要将文本转换为大写
                        // 这里只是示例，实际应用中需要在文本处理阶段实现
                        Logger.e(TAG, "文本转换：大写 (需要在文本处理阶段实现)")
                    }

                    "lowercase" -> {
                        Logger.e(TAG, "文本转换：小写 (需要在文本处理阶段实现)")
                    }

                    "capitalize" -> {
                        Logger.e(TAG, "文本转换：大写 (需要在文本处理阶段实现)")
                    }
                }
            }

            // 添加字体家族支持
            styles["font-family"]?.let { fontFamily ->
                // 注意：Android中使用TypefaceSpan设置字体家族
                spans.add(android.text.style.TypefaceSpan(fontFamily))
            }

            // 添加背景支持（扩展）
            styles["background"]?.let { background ->
                // 尝试解析背景颜色（简化处理，只解析颜色）
                try {
                    // 这里只是简单示例，实际背景可能包含图片、渐变等复杂值
                    if (background.startsWith("#") || background.startsWith("rgb")) {
                        val color = parseColor(background)
                        spans.add(BackgroundColorSpan(color))
                    }
                } catch (e: Exception) {
                    Logger.e(TAG, "Error parsing background: $background $e")
                }
            }


            // 添加透明度支持
            styles["opacity"]?.let { opacity ->
                try {
                    val alpha = (opacity.toFloat() * 255).toInt()
                    // 注意：Android中没有直接的透明度Span，可能需要结合其他Span实现
                    Logger.e(TAG, "Opacity: $opacity (alpha: $alpha) (需要自定义实现)")
                } catch (e: Exception) {
                    Logger.e(TAG, "Error parsing opacity: $opacity $e")
                }
            }

            // 添加垂直对齐支持（上标、下标）
            styles["vertical-align"]?.let { verticalAlign ->
                when (verticalAlign) {
                    "super" -> spans.add(SuperscriptSpan())
                    "sub" -> spans.add(SubscriptSpan())
                    // 可以添加更多垂直对齐支持
                }
            }
            // 这里可以添加更多样式支持...

            if (spans.isNotEmpty()) {
                styleApplications.add(StyleApplication(start, end, spans))
            }
        }
    }

    /**
     * 解析CSS样式字符串
     */
    private fun parseStyle(style: String): Map<String, String> {
        val styles = mutableMapOf<String, String>()
        val pairs = style.split(";")
        for (pair in pairs) {
            val keyValue = pair.split(":", limit = 2)
            if (keyValue.size == 2) {
                val key = keyValue[0].trim()
                val value = keyValue[1].trim()
                styles[key] = value
            }
        }
        return styles
    }

    /**
     * 解析颜色值
     */
    private fun parseColor(colorStr: String): Int {
        return when {
            colorStr.startsWith("rgba") -> {
                val values = colorStr.substringAfter('(')
                    .substringBefore(')')
                    .split(',')
                    .map { it.trim() }

                val r = (values[0].toFloat() * 255).toInt()
                val g = (values[1].toFloat() * 255).toInt()
                val b = (values[2].toFloat() * 255).toInt()
                val a = (values[3].toFloat() * 255).toInt()

                Color.argb(a, r, g, b)
            }

            colorStr.startsWith("rgb") -> {
                val values = colorStr.substringAfter('(')
                    .substringBefore(')')
                    .split(',')
                    .map { it.trim().toInt() }

                Color.rgb(values[0], values[1], values[2])
            }

            colorStr.startsWith("#") -> {
                colorStr.toColorInt()
            }

            else -> {
                // 尝试解析颜色名称
                try {
                    colorStr.toColorInt()
                } catch (e: Exception) {
                    // 如果无法解析，返回黑色
                    Color.BLACK
                }
            }
        }
    }


    /**
     * 解析图片尺寸属性
     * @param sizeStr 尺寸字符串，可以是数字、百分比或带单位的值
     * @param defaultSize 默认尺寸
     * @param context 上下文用于单位转换
     * @return 解析后的像素值
     */
    private fun parseSize(sizeStr: String, defaultSize: Int, context: Context?): Int {
        if (sizeStr.isEmpty()) return defaultSize

        return try {
            when {
                // 百分比尺寸 (如 "50%")
                sizeStr.endsWith("%") -> {
                    val percentage = sizeStr.removeSuffix("%").toFloat() / 100f
                    (defaultSize * percentage).toInt()
                }

                // 像素尺寸 (如 "100px" 或 "100")
                sizeStr.endsWith("px") -> sizeStr.removeSuffix("px").toInt()
                sizeStr.toIntOrNull() != null -> sizeStr.toInt()

                // DP尺寸 (如 "100dp")
                sizeStr.endsWith("dp") && context != null -> {
                    val dpValue = sizeStr.removeSuffix("dp").toFloat()
                    (dpValue * context.resources.displayMetrics.density).toInt()
                }

                // SP尺寸 (如 "100sp") - 通常不用于图片尺寸
                sizeStr.endsWith("sp") && context != null -> {
                    val spValue = sizeStr.removeSuffix("sp").toFloat()
                    (spValue * context.resources.displayMetrics.scaledDensity).toInt()
                }

                // 其他情况使用默认尺寸
                else -> defaultSize
            }
        } catch (e: Exception) {
            Logger.e(TAG, "解析尺寸时出错: $sizeStr $e")
            defaultSize
        }
    }

    /**
     * 处理图片尺寸
     * @param drawable 原始图片
     * @param element 图片元素
     * @param context 上下文用于单位转换
     * @return 调整尺寸后的图片
     */
    private fun processImageSize(
        drawable: Drawable,
        element: Element,
        context: Context?
    ): Drawable {
        val intrinsicWidth = drawable.intrinsicWidth
        val intrinsicHeight = drawable.intrinsicHeight

        // 获取宽度属性
        val widthAttr = element.attr("width")
        val heightAttr = element.attr("height")

        // 解析宽度和高度
        var targetWidth = parseSize(widthAttr, intrinsicWidth, context)
        var targetHeight = parseSize(heightAttr, intrinsicHeight, context)

        // 如果只指定了一个维度，保持宽高比
        if (widthAttr.isNotEmpty() && heightAttr.isEmpty() && intrinsicWidth > 0) {
            val aspectRatio = intrinsicHeight.toFloat() / intrinsicWidth.toFloat()
            targetHeight = (targetWidth * aspectRatio).toInt()
        } else if (heightAttr.isNotEmpty() && widthAttr.isEmpty() && intrinsicHeight > 0) {
            val aspectRatio = intrinsicWidth.toFloat() / intrinsicHeight.toFloat()
            targetWidth = (targetHeight * aspectRatio).toInt()
        }

        // 确保最小尺寸
        if (targetWidth <= 0) targetWidth = if (intrinsicWidth > 0) intrinsicWidth else 100
        if (targetHeight <= 0) targetHeight = if (intrinsicHeight > 0) intrinsicHeight else 100

        // 设置图片边界
        drawable.setBounds(0, 0, targetWidth, targetHeight)

        return drawable
    }

    /**
     * 处理图片样式中的尺寸
     * @param drawable 原始图片
     * @param element 图片元素
     * @param context 上下文用于单位转换
     * @return 调整尺寸后的图片
     */
    private fun processImageStyleSize(
        drawable: Drawable,
        element: Element,
        context: Context?
    ): Drawable {
        val intrinsicWidth = drawable.intrinsicWidth
        val intrinsicHeight = drawable.intrinsicHeight

        // 检查style属性中的尺寸
        val style = element.attr("style")
        if (style.isNotEmpty()) {
            val styles = parseStyle(style)

            var targetWidth = intrinsicWidth
            var targetHeight = intrinsicHeight

            // 处理style中的宽度和高度
            styles["width"]?.let { widthStr ->
                targetWidth = parseSize(widthStr, intrinsicWidth, context)
            }

            styles["height"]?.let { heightStr ->
                targetHeight = parseSize(heightStr, intrinsicHeight, context)
            }

            // 如果只指定了一个维度，保持宽高比
            if (styles.containsKey("width") && !styles.containsKey("height")) {
                val aspectRatio = intrinsicHeight.toFloat() / intrinsicWidth.toFloat()
                targetHeight = (targetWidth * aspectRatio).toInt()
            } else if (styles.containsKey("height") && !styles.containsKey("width")) {
                val aspectRatio = intrinsicWidth.toFloat() / intrinsicHeight.toFloat()
                targetWidth = (targetHeight * aspectRatio).toInt()
            }

            // 确保最小尺寸
            if (targetWidth <= 0) targetWidth = 100
            if (targetHeight <= 0) targetHeight = 100

            // 设置图片边界
            drawable.setBounds(0, 0, targetWidth, targetHeight)
        }

        return drawable
    }
}
