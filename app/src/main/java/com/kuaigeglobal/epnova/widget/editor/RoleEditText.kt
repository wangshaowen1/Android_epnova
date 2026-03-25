package com.kuaigeglobal.epnova.widget.editor

import android.content.Context
import android.graphics.*
import android.text.*
import android.text.style.ReplacementSpan
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputConnectionWrapper
import androidx.appcompat.widget.AppCompatEditText
import com.kuaigeglobal.epnova.core.network.apis.response.Role
import kotlin.collections.find
import kotlin.collections.first
import kotlin.collections.isNotEmpty
import kotlin.jvm.java
import kotlin.ranges.coerceAtLeast
import kotlin.text.substring
import kotlin.to
import androidx.core.graphics.toColorInt

/** 可插入@角色的EditText */
class RoleEditText @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : AppCompatEditText(context, attrs) {

    private val roleList = mutableListOf<Role>()

    init {
        isFocusable = true
        isFocusableInTouchMode = true
        requestFocus()
    }

    /** 插入角色 */
    fun insertRole(role: Role) {
        val editable = text ?: return
        val start = selectionStart.coerceAtLeast(0)
        val roleText = "@${role.roleName}"
        editable.insert(start, roleText)
        editable.setSpan(
            RoleSpan(role),
            start,
            start + roleText.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        roleList.add(role)
        setSelection(start + roleText.length)
    }

    /** 上传格式转换：<role:@张三> + id 列表 */
    fun toServerFormat(): Pair<String, List<String>> {
        val spannable = text ?: return "" to emptyList()
        val sb = kotlin.text.StringBuilder()
        val ids = mutableListOf<String>()
        var i = 0
        while (i < spannable.length) {
            val spans = spannable.getSpans(i, i + 1, RoleSpan::class.java)
            if (spans.isNotEmpty()) {
                val span = spans.first()
                val start = spannable.getSpanStart(span)
                val end = spannable.getSpanEnd(span)
                val info = span.roleInfo
                sb.append("<role:@${info.roleName}>")
                ids.add(info.roleId ?: "")
                i = end
            } else {
                sb.append(spannable[i])
                i++
            }
        }
        return sb.toString() to ids
    }

    /** 从服务器格式还原 */
    fun fromServerFormat(textWithTags: String?, roles: List<Role>?) {
        if (textWithTags.isNullOrEmpty()) return
        val regex = Regex("<role:@(.*?)>")
        val spannable = SpannableStringBuilder()
        var lastIndex = 0

        for (match in regex.findAll(textWithTags)) {
            val startIndex = match.range.first
            if (startIndex > lastIndex) {
                spannable.append(textWithTags.substring(lastIndex, startIndex))
            }
            val name = match.groupValues[1]
            val role = roles?.find { it.roleName == name } ?: Role(name, "")
            val roleText = "@$name"
            val start = spannable.length
            spannable.append(roleText)
            spannable.setSpan(RoleSpan(role), start, spannable.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            roleList.add(role)
            lastIndex = match.range.last + 1
        }

        if (lastIndex < textWithTags.length) {
            spannable.append(textWithTags.substring(lastIndex))
        }

        setText(spannable)
        setSelection(spannable.length)
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        val result = super.onTouchEvent(event)
        requestFocus()
        return result
    }

    /**
     * 解决问题：onKeyDown(KEYCODE_DEL) 很多输入法不会走（比如 Gboard、华为输入法、讯飞输入法等），
     * 它们直接通过 InputConnection.deleteSurroundingText() 通知系统删除字符，
     * 而不会触发 onKeyDown()。
     *
     * ✅ 关键：包装 InputConnection 以拦截 deleteSurroundingText
     * Gboard、华为输入法、讯飞都支持
     */
    override fun onCreateInputConnection(outAttrs: EditorInfo): InputConnection {
        val ic = super.onCreateInputConnection(outAttrs)
        return object : InputConnectionWrapper(ic, true) {
            override fun deleteSurroundingText(beforeLength: Int, afterLength: Int): Boolean {
                val editable = text ?: return super.deleteSurroundingText(beforeLength, afterLength)
                val cursor = selectionStart
                val spans = editable.getSpans(0, editable.length, RoleSpan::class.java)
                for (span in spans) {
                    val start = editable.getSpanStart(span)
                    val end = editable.getSpanEnd(span)
                    if (cursor in (start + 1)..end) {
                        editable.delete(start, end)
                        roleList.remove(span.roleInfo)
                        return true // 拦截默认删除
                    }
                }
                return super.deleteSurroundingText(beforeLength, afterLength)
            }

            override fun sendKeyEvent(event: KeyEvent): Boolean {
                // 对某些输入法仍然处理物理删除键
                if (event.keyCode == KeyEvent.KEYCODE_DEL && event.action == KeyEvent.ACTION_DOWN) {
                    val editable = text ?: return super.sendKeyEvent(event)
                    val cursor = selectionStart
                    val spans = editable.getSpans(0, editable.length, RoleSpan::class.java)
                    for (span in spans) {
                        val start = editable.getSpanStart(span)
                        val end = editable.getSpanEnd(span)
                        if (cursor in (start + 1)..end) {
                            editable.delete(start, end)
                            roleList.remove(span.roleInfo)
                            return true
                        }
                    }
                }
                return super.sendKeyEvent(event)
            }
        }
    }
}

/** 高亮显示的Span */
class RoleSpan(val roleInfo: Role) : ReplacementSpan() {
    private val bgColor = "#E0F7FA".toColorInt()
    private val textColor = "#ff4b00".toColorInt()
    private val radius = 16f
    private val paddingX = 0
    private val paddingY = 0

    private var showBg = false

    private var cachedWidth = 0f
    private var cachedText: String? = null

    override fun getSize(paint: Paint, text: CharSequence, start: Int, end: Int, fm: Paint.FontMetricsInt?): Int {
        val txt = text.subSequence(start, end).toString()
        if (txt != cachedText) {
            cachedText = txt
            cachedWidth = paint.measureText(txt) + paddingX * 2
        }
        return cachedWidth.toInt()
    }

    override fun draw(
        canvas: Canvas,
        text: CharSequence,
        start: Int,
        end: Int,
        x: Float,
        top: Int,
        y: Int,
        bottom: Int,
        paint: Paint
    ) {
        val txt = text.subSequence(start, end).toString()
        val oldColor = paint.color
        if (showBg) {
            val rect = RectF(
                x,
                y + paint.ascent() - paddingY,
                x + cachedWidth,
                y + paint.descent() + paddingY
            )
            paint.color = bgColor
            canvas.drawRoundRect(rect, radius, radius, paint)
        }
        paint.color = textColor
        canvas.drawText(txt, x + paddingX, y.toFloat(), paint)
        paint.color = oldColor
    }
}
