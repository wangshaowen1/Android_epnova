package com.kuaigeglobal.epnova.widget

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.text.InputFilter
import android.text.SpannableStringBuilder
import android.view.View
import android.widget.FrameLayout
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.kuaigeglobal.epnova.core.databinding.setInputDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/** 使用 Android 的真实文本替换和测量流程，避免只验证过滤器的返回字符串。 */
@RunWith(AndroidJUnit4::class)
class WidgetRegressionTest {
    private val context: Context get() = ApplicationProvider.getApplicationContext()

    @Test
    fun decimal_deleteLeadingZero_doesNotDuplicateDecimalPoint() {
        val text = decimalText("0.")
        text.delete(0, 1)
        assertEquals(".", text.toString())
        text.delete(0, 1)
        assertEquals("", text.toString())
    }

    @Test
    fun decimal_invalidSelectionReplacement_preservesSelection() {
        val text = decimalText("12.34")
        text.replace(0, 2, "abc")
        assertEquals("12.34", text.toString())
        text.delete(2, 3)
        assertEquals("12.34", text.toString())
        text.replace(0, 2, "5")
        assertEquals("5.34", text.toString())
    }

    @Test
    fun decimal_pasteRespectsSourceRange_andWholeReplacement() {
        val text = decimalText("")
        text.replace(0, 0, "x12.34y", 1, 6)
        assertEquals("12.34", text.toString())
        text.replace(0, text.length, ".")
        assertEquals("0.", text.toString())
        text.append("123")
        assertEquals("0.", text.toString())
    }

    @Test
    fun decimal_zeroDecimalDigits_rejectsPoint() {
        val text = SpannableStringBuilder("12").apply {
            filters = arrayOf(DecimalInputFilter(2, 0))
        }
        text.append(".")
        assertEquals("12", text.toString())
        text.clear()
        text.append(".")
        assertEquals("", text.toString())
    }

    @Test
    fun decimal_bindingKeepsExistingFilters_withoutDuplicates() = onMain {
        val view = RestrictedEditText(context)
        val lengthFilter = InputFilter.LengthFilter(4)
        view.filters = arrayOf(lengthFilter)
        setInputDecimal(view, true, 2, 2)
        setInputDecimal(view, true, 3, 2)
        assertEquals(2, view.filters.size)
        assertSame(lengthFilter, view.filters.first())
        setInputDecimal(view, false, 3, 2)
        assertEquals(1, view.filters.size)
        assertSame(lengthFilter, view.filters.single())
    }

    @Test
    fun restrictedInput_canDisableRestrictions_afterRepeatedConfiguration() = onMain {
        val view = RestrictedEditText(context)
        repeat(3) { view.createInputFilter(true, 10) }
        view.setText("A!")
        assertEquals("A", view.text.toString())
        view.createInputFilter(false, 10)
        view.setText("A!")
        assertEquals("A!", view.text.toString())
    }

    @Test
    fun maximumHeight_handlesDefaultLimitAndParentConstraints() = onMain {
        val view = MaxHeightScrollView(context)
        view.addView(View(context), FrameLayout.LayoutParams(100, 200))
        view.measure(exactly(100), atMost(400))
        assertEquals(200, view.measuredHeight)
        view.maxHeight = 80
        view.measure(exactly(100), atMost(400))
        assertEquals(80, view.measuredHeight)
        view.measure(exactly(100), atMost(50))
        assertEquals(50, view.measuredHeight)
        view.measure(exactly(100), exactly(120))
        assertEquals(120, view.measuredHeight)
    }

    @Test
    fun autoResize_respectsHeightLimit_andFixedHeight() = onMain {
        val view = AutoResizeImageView(context)
        view.setImageDrawable(object : ColorDrawable(Color.RED) {
            override fun getIntrinsicWidth() = 100
            override fun getIntrinsicHeight() = 200
        })
        view.measure(exactly(100), atMost(80))
        assertEquals(80, view.measuredHeight)
        view.measure(exactly(100), exactly(60))
        assertEquals(60, view.measuredHeight)
    }

    @Test
    fun countDown_startListenerMayCancel_andRepeatedStopIsIdempotent() = onMain {
        val view = CountDownTextView(context)
        var completions = 0
        view.setOnTimeListener(object : CountDownTextView.OnTimeListener {
            override fun onStartCountTime() = view.stopCountDown()
            override fun onTime(time: Int) = Unit
            override fun onComplete() { completions++ }
        })
        view.startCountDown(60, 1, false)
        assertFalse(view.isCountDowning)
        assertTrue(view.isEnabled)
        assertEquals(1, completions)
        view.stopCountDown()
        assertEquals(1, completions)
    }

    @Test
    fun countDown_withoutListener_restoresOriginalEnabledState() = onMain {
        val view = CountDownTextView(context)
        view.startCountDown(0, 1, false)
        assertFalse(view.isCountDowning)
        assertTrue(view.isEnabled)
        view.isEnabled = false
        view.startCountDown(0, 1, false)
        assertFalse(view.isEnabled)
    }

    @Test
    fun countDown_forwardTicksUseSeconds() {
        val tick = CountDownLatch(1)
        val elapsed = AtomicInteger(-1)
        lateinit var view: CountDownTextView
        onMain {
            view = CountDownTextView(context)
            view.setOnTimeListener(object : CountDownTextView.OnTimeListener {
                override fun onStartCountTime() = Unit
                override fun onTime(time: Int) {
                    elapsed.set(time)
                    view.stopCountDown()
                    tick.countDown()
                }
                override fun onComplete() = Unit
            })
            view.startCountDown(60, 1, true)
        }
        try {
            assertTrue(tick.await(2, TimeUnit.SECONDS))
            assertTrue(elapsed.get() in 0..2)
        } finally {
            onMain { view.stopCountDown() }
        }
    }

    @Test
    fun countDown_completionMayRestart_withoutReenablingActiveTimer() = onMain {
        val view = CountDownTextView(context)
        var completions = 0
        view.setOnTimeListener(object : CountDownTextView.OnTimeListener {
            override fun onStartCountTime() = Unit
            override fun onTime(time: Int) = Unit
            override fun onComplete() {
                completions++
                if (completions == 1) view.startCountDown(60, 1, false)
            }
        })
        try {
            view.startCountDown(0, 1, false)
            assertTrue(view.isCountDowning)
            assertFalse(view.isEnabled)
            assertEquals(1, completions)
        } finally {
            view.setOnTimeListener(null)
            view.stopCountDown()
        }
    }

    private fun decimalText(value: String) = SpannableStringBuilder(value).apply {
        filters = arrayOf(DecimalInputFilter(2, 2))
    }

    private fun exactly(size: Int) = View.MeasureSpec.makeMeasureSpec(size, View.MeasureSpec.EXACTLY)
    private fun atMost(size: Int) = View.MeasureSpec.makeMeasureSpec(size, View.MeasureSpec.AT_MOST)
    private fun onMain(block: () -> Unit) = InstrumentationRegistry.getInstrumentation().runOnMainSync(block)
}
