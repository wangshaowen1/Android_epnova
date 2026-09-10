package com.kuaigeglobal.epnova.widget

import android.animation.ValueAnimator
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.os.SystemClock
import android.text.InputFilter
import android.text.SpannableStringBuilder
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.RadioButton
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.widget.testing.WidgetTestActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CommonWidgetRegressionTest {
    @Test
    fun input_reconfigurationKeepsOtherFiltersAndRestrictions() = withActivity { activity ->
        val view = XEditText(activity)
        val caps = InputFilter.AllCaps()
        view.filters = arrayOf(caps, InputFilter.LengthFilter(2))
        repeat(3) {
            view.inputRestrictions = XEditText.RESTRICTION_NO_EMOJI or XEditText.RESTRICTION_NO_SPACE or XEditText.RESTRICTION_NO_NEXT_LINE
            view.setMaxLength(8)
        }
        assertEquals(3, view.filters.size)
        assertTrue(view.filters.any { it === caps })
        view.setText("abcd")
        assertEquals("ABCD", view.text.toString())
        view.text!!.replace(1, 3, "a b")
        assertEquals("ABCD", view.text.toString())
        view.inputRestrictions = XEditText.RESTRICTION_NONE
        assertEquals(2, view.filters.size)
        view.setText("a b")
        assertEquals("A B", view.text.toString())
    }

    @Test
    fun input_restrictionsRespectSourceRangeAndCanBeDisabled() = withActivity { activity ->
        val view = XEditText(activity)
        view.inputRestrictions = XEditText.RESTRICTION_NO_EMOJI or XEditText.RESTRICTION_NO_SPACE or XEditText.RESTRICTION_NO_NEXT_LINE
        view.setMaxLength(20)
        view.text!!.replace(0, 0, "\uD83D\uDE00ok", 2, 4)
        assertEquals("ok", view.text.toString())
        view.inputRestrictions = XEditText.RESTRICTION_NO_NEXT_LINE
        view.text!!.replace(0, 2, "a\nb")
        assertEquals("ok", view.text.toString())
        view.text!!.delete(0, 2)
        assertEquals("", view.text.toString())
        view.inputRestrictions = XEditText.RESTRICTION_NONE
        view.text!!.append("a\nb")
        assertEquals("a\nb", view.text.toString())
    }

    @Test
    fun input_lengthLimitDoesNotSplitSurrogateInSourceSlice() = withActivity { activity ->
        val view = XEditText(activity)
        val text = SpannableStringBuilder().apply { filters = arrayOf(view.LengthFilter(2)) }
        text.replace(0, 0, "xA\uD83D\uDE00z", 1, 5)
        assertEquals("A", text.toString())
        text.clear()
        text.filters = arrayOf(view.LengthFilter(1))
        text.replace(0, 0, "x\uD83D\uDE00z", 1, 4)
        assertEquals("", text.toString())
    }

    @Test
    fun input_deletingExistingOverLimitTextDoesNotReportOverflow() = withActivity { activity ->
        val view = XEditText(activity)
        view.setText("abcdef")
        var callbacks = 0
        view.setOnTextOutOfBondsListener { callbacks++ }
        view.setMaxLength(2)
        view.text!!.delete(0, 1)
        assertEquals("bcdef", view.text.toString())
        assertEquals(0, callbacks)
    }

    @Test
    fun input_rejectedSelectionInExistingOverLimitTextKeepsOriginal() = withActivity { activity ->
        val view = XEditText(activity)
        view.setText("abcdef")
        view.inputRestrictions = XEditText.RESTRICTION_NO_EMOJI or XEditText.RESTRICTION_NO_SPACE or XEditText.RESTRICTION_NO_NEXT_LINE
        view.setMaxLength(2)
        var callbacks = 0
        view.setOnTextOutOfBondsListener { callbacks++ }
        view.text!!.replace(1, 3, " ")
        assertEquals("abcdef", view.text.toString())
        assertEquals(0, callbacks)
    }

    @Test
    fun counter_changingLimitKeepsActualCountAndExistingText() = withActivity { activity ->
        val view = CounterInputLayout(activity)
        view.setText("hello")
        view.setMaxLength(3)
        assertEquals("hello", view.text)
        assertEquals("5", view.findViewById<TextView>(R.id.inputLength).text.toString())
        assertEquals("/3", view.findViewById<TextView>(R.id.inputLengthHint).text.toString())
        view.editableText.delete(0, 3)
        assertEquals("2", view.findViewById<TextView>(R.id.inputLength).text.toString())
    }

    @Test
    fun radio_precheckedNestedChildrenGetIdsAndSingleSelection() = withActivity { activity ->
        val group = MultRadioGroup(activity)
        val nested = FrameLayout(activity)
        val first = RadioButton(activity).apply { isChecked = true }
        val second = RadioButton(activity).apply { isChecked = true }
        nested.addView(first)
        nested.addView(second)
        var callbacks = 0
        group.setOnCheckedChangeListener { _, id ->
            callbacks++
            assertNotEquals(View.NO_ID, id)
            assertSame(second, group.checkedRadioButton)
            assertFalse(first.isChecked)
            assertTrue(second.isChecked)
        }
        group.addView(nested)
        assertNotEquals(View.NO_ID, first.id)
        assertNotEquals(View.NO_ID, second.id)
        assertEquals(second.id, group.checkedRadioButtonId)
        assertEquals(1, callbacks)
    }

    @Test
    fun radio_uncheckingAndRemovingSelectionClearGroupState() = withActivity { activity ->
        val group = MultRadioGroup(activity)
        val button = RadioButton(activity)
        group.addView(button)
        val changes = mutableListOf<Int>()
        group.setOnCheckedChangeListener { _, id -> changes.add(id) }
        group.check(button.id)
        button.isChecked = false
        assertEquals(View.NO_ID, group.checkedRadioButtonId)
        group.clearCheck()
        group.check(button.id)
        group.removeView(button)
        assertNull(group.checkedRadioButton)
        assertEquals(listOf(button.id, View.NO_ID, button.id, View.NO_ID), changes)
        button.isChecked = false
        assertEquals(4, changes.size)
    }

    @Test
    fun radio_invalidIdPreservesSelectionAndSilentCheckDoesNotNotify() = withActivity { activity ->
        val group = MultRadioGroup(activity)
        val button = RadioButton(activity)
        val other = View(activity).apply { id = View.generateViewId() }
        group.addView(button)
        group.addView(other)
        var callbacks = 0
        group.setOnCheckedChangeListener { _, _ -> callbacks++ }
        group.setCheckWithoutNotif(button.id)
        group.check(other.id)
        group.setCheckWithoutNotif(View.generateViewId())
        assertEquals(button.id, group.checkedRadioButtonId)
        assertTrue(button.isChecked)
        assertEquals(0, callbacks)
    }

    @Test
    fun radio_callbackMaySelectAnotherButtonWithoutStaleState() = withActivity { activity ->
        val group = MultRadioGroup(activity)
        val first = RadioButton(activity)
        val second = RadioButton(activity)
        group.addView(first)
        group.addView(second)
        val changes = mutableListOf<Int>()
        group.setOnCheckedChangeListener { _, id ->
            changes.add(id)
            if (id == first.id) group.check(second.id)
        }
        first.isChecked = true
        assertEquals(listOf(first.id, second.id), changes)
        assertFalse(first.isChecked)
        assertTrue(second.isChecked)
        assertEquals(second.id, group.checkedRadioButtonId)
    }

    @Test
    fun radioAnimation_repeatedCheckDoesNotRestartOrRequestLayoutAndDetachRestoresBounds() = withActivity { activity ->
        val view = FixedAnimatedRadioButton(activity)
        val icon = ColorDrawable(Color.RED).apply { setBounds(0, 0, 40, 40) }
        view.setCompoundDrawables(null, icon, null, null)
        activity.container.addView(view, FrameLayout.LayoutParams(120, 120))
        measureAndLayout(view)
        view.isChecked = true
        measureAndLayout(view)
        val animator = animator(view)
        animator.currentPlayTime = 200
        assertTrue(icon.bounds.width() < 40)
        assertFalse(view.isLayoutRequested)
        val playTime = animator.currentPlayTime
        view.isChecked = true
        assertTrue(animator.currentPlayTime >= playTime)
        activity.container.removeView(view)
        assertFalse(animator.isStarted)
        assertEquals(Rect(0, 0, 40, 40), icon.bounds)
    }

    @Test
    fun radioAnimation_disabledInXmlDoesNotStart() = withActivity { activity ->
        val root = activity.layoutInflater.inflate(R.layout.widget_regression_views, activity.container, false)
        activity.container.addView(root)
        val view = root.findViewById<FixedAnimatedRadioButton>(R.id.test_radio_animation_disabled)
        measureAndLayout(view)
        view.isChecked = true
        assertFalse(animator(view).isStarted)
    }

    @Test
    fun radioAnimation_replacingDrawableKeepsNewBoundsAndVisibilityCancels() = withActivity { activity ->
        val view = FixedAnimatedRadioButton(activity)
        val old = ColorDrawable(Color.RED).apply { setBounds(0, 0, 40, 40) }
        val replacement = ColorDrawable(Color.BLUE).apply { setBounds(0, 0, 60, 30) }
        view.setCompoundDrawables(null, old, null, null)
        activity.container.addView(view, FrameLayout.LayoutParams(120, 120))
        measureAndLayout(view)
        view.isChecked = true
        animator(view).currentPlayTime = 200
        view.setCompoundDrawables(null, replacement, null, null)
        assertFalse(animator(view).isStarted)
        assertEquals(Rect(0, 0, 60, 30), replacement.bounds)
        assertEquals(Rect(0, 0, 40, 40), old.bounds)
        view.isChecked = false
        view.isChecked = true
        animator(view).currentPlayTime = 200
        view.visibility = View.GONE
        assertFalse(animator(view).isStarted)
        assertEquals(Rect(0, 0, 60, 30), replacement.bounds)
    }

    @Test
    fun radioAnimation_partialReplacementDoesNotCacheShrunkSiblingSize() = withActivity { activity ->
        val view = FixedAnimatedRadioButton(activity)
        val top = ColorDrawable(Color.RED).apply { setBounds(0, 0, 40, 40) }
        val right = ColorDrawable(Color.BLUE).apply { setBounds(0, 0, 30, 30) }
        view.setCompoundDrawables(null, top, right, null)
        val originalPadding = view.compoundPaddingRight
        activity.container.addView(view, FrameLayout.LayoutParams(120, 120))
        measureAndLayout(view)
        view.isChecked = true
        animator(view).currentPlayTime = 200
        val replacement = ColorDrawable(Color.GREEN).apply { setBounds(0, 0, 60, 30) }
        view.setCompoundDrawables(null, replacement, right, null)
        assertEquals(Rect(0, 0, 30, 30), right.bounds)
        assertEquals(originalPadding, view.compoundPaddingRight)
        assertFalse(animator(view).isStarted)
    }

    @Test
    fun radioAnimation_reusingResizedDrawablePreservesRequestedBounds() = withActivity { activity ->
        val view = FixedAnimatedRadioButton(activity)
        val icon = ColorDrawable(Color.RED).apply { setBounds(0, 0, 40, 40) }
        view.setCompoundDrawablesRelative(null, icon, null, null)
        activity.container.addView(view, FrameLayout.LayoutParams(120, 120))
        measureAndLayout(view)
        view.isChecked = true
        animator(view).currentPlayTime = 200
        icon.setBounds(0, 0, 60, 30)
        view.setCompoundDrawablesRelative(null, icon, null, null)
        assertEquals(Rect(0, 0, 60, 30), icon.bounds)
        assertFalse(animator(view).isStarted)
    }

    @Test
    fun scrollInput_boundaryDoesNotRewriteMotionEventAndCancelReleasesParent() = withActivity { activity ->
        var disallowed = false
        val parent = object : FrameLayout(activity) {
            override fun requestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {
                disallowed = disallowIntercept
                super.requestDisallowInterceptTouchEvent(disallowIntercept)
            }
        }
        val view = EditTextWithScrollView(activity).apply {
            setText((1..30).joinToString("\n") { "line $it" })
        }
        parent.addView(view, FrameLayout.LayoutParams(120, 120))
        activity.container.addView(parent)
        measureAndLayout(view)
        touch(view, MotionEvent.ACTION_DOWN, 50f)
        assertTrue(disallowed)
        view.scrollTo(0, 10)
        view.scrollTo(0, 0)
        touch(view, MotionEvent.ACTION_MOVE, 90f)
        assertFalse(disallowed)
        touch(view, MotionEvent.ACTION_CANCEL, 90f)
        assertFalse(disallowed)
    }

    @Test
    fun scrollInput_scrollWithoutParentDoesNotCrash() = withActivity { activity ->
        val view = EditTextWithScrollView(activity)
        view.setText("first\nsecond\nthird")
        measureAndLayout(view)
        view.scrollTo(0, 1)
        view.scrollTo(0, 0)
    }

    private fun withActivity(block: (WidgetTestActivity) -> Unit) {
        ActivityScenario.launch(WidgetTestActivity::class.java).use { scenario -> scenario.onActivity { block(it) } }
    }

    private fun animator(view: FixedAnimatedRadioButton): ValueAnimator =
        FixedAnimatedRadioButton::class.java.getDeclaredField("valueAnimator").apply { isAccessible = true }
            .get(view) as ValueAnimator

    private fun measureAndLayout(view: View) {
        val spec = View.MeasureSpec.makeMeasureSpec(120, View.MeasureSpec.EXACTLY)
        view.measure(spec, spec)
        view.layout(0, 0, 120, 120)
    }

    private fun touch(view: View, action: Int, y: Float) {
        val time = SystemClock.uptimeMillis()
        val event = MotionEvent.obtain(time, time, action, 50f, y, 0)
        try {
            view.dispatchTouchEvent(event)
            assertEquals(action, event.actionMasked)
        } finally {
            event.recycle()
        }
    }
}
