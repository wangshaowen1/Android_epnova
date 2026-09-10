package com.kuaigeglobal.epnova.widget

import android.text.InputFilter
import android.text.Spanned
import android.view.ContextThemeWrapper
import android.view.inputmethod.EditorInfo
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.widget.testing.WidgetTestActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class XEditTextRestrictionsTest {
    @Test
    fun flags_allCombinationsRestrictOnlySelectedKinds() = withActivity { activity ->
        val view = XEditText(activity)
        val inputs = listOf(
            "\uD83D\uDE00" to XEditText.RESTRICTION_NO_EMOJI,
            " " to XEditText.RESTRICTION_NO_SPACE,
            "\t" to XEditText.RESTRICTION_NO_SPACE,
            "\u3000" to XEditText.RESTRICTION_NO_SPACE,
            "\u00A0" to XEditText.RESTRICTION_NO_SPACE,
            "\n" to XEditText.RESTRICTION_NO_NEXT_LINE,
            "\r" to XEditText.RESTRICTION_NO_NEXT_LINE,
        )
        for (flags in 0..7) {
            view.inputRestrictions = flags
            assertEquals(flags, view.inputRestrictions)
            for ((text, flag) in inputs) {
                view.setText("ok")
                view.text!!.replace(0, 2, text)
                assertEquals("flags=$flags, flag=$flag", if (flags and flag != 0) "ok" else text, view.text.toString())
            }
            view.setText("中文Ab12")
            assertEquals("中文Ab12", view.text.toString())
        }
    }

    @Test
    fun xml_combinedFlagsKeepAndroidLengthLimit() = withActivity { activity ->
        val root = activity.layoutInflater.inflate(R.layout.widget_input_restrictions, activity.container, false)
        val view = root.findViewById<XEditText>(R.id.test_input_restrictions_combined)
        assertEquals(7, view.inputRestrictions)
        view.setText("abcdef")
        assertEquals("abcde", view.text.toString())
        view.text!!.replace(1, 3, "a b\n")
        assertEquals("abcde", view.text.toString())
        view.text!!.delete(0, 2)
        assertEquals("cde", view.text.toString())
    }

    @Test
    fun xml_styleAndExplicitOverrideAlsoWorkForSubclass() = withActivity { activity ->
        val root = activity.layoutInflater.inflate(R.layout.widget_input_restrictions, activity.container, false)
        val styled = root.findViewById<XEditText>(R.id.test_input_restrictions_style)
        assertEquals(3, styled.inputRestrictions)
        styled.setText("a\nb")
        assertEquals("a\nb", styled.text.toString())
        val overridden = root.findViewById<EditTextWithScrollView>(R.id.test_input_restrictions_override)
        assertEquals(XEditText.RESTRICTION_NO_NEXT_LINE, overridden.inputRestrictions)
        overridden.setText("a b")
        overridden.text!!.replace(0, 3, "a\nb")
        assertEquals("a b", overridden.text.toString())
    }

    @Test
    fun xml_noneOverridesStyleWithoutRemovingLengthLimit() = withActivity { activity ->
        val root = activity.layoutInflater.inflate(R.layout.widget_input_restrictions, activity.container, false)
        val view = root.findViewById<XEditText>(R.id.test_input_restrictions_none)
        assertEquals(XEditText.RESTRICTION_NONE, view.inputRestrictions)
        view.setText(" \uD83D\uDE00\n")
        assertEquals(" \uD83D\uDE00\n", view.text.toString())
        view.setText("abcdef")
        assertEquals("abcde", view.text.toString())
    }

    @Test
    fun constructors_readThemeDefaultAndHonorExplicitDefStyleAttr() = withActivity { activity ->
        val context = ContextThemeWrapper(activity, R.style.ThemeInputRestrictionsTest)
        val views = listOf(
            XEditText(context),
            XEditText(context, null),
            XEditText(context, null, androidx.appcompat.R.attr.editTextStyle),
        )
        for (view in views) {
            assertEquals(3, view.inputRestrictions)
            view.setText("abcdef")
            assertEquals("abcde", view.text.toString())
            view.text!!.replace(0, 1, " ")
            assertEquals("abcde", view.text.toString())
        }
        assertEquals(XEditText.RESTRICTION_NONE, XEditText(context, null, 0).inputRestrictions)
    }

    @Test
    fun filters_replacingExternalFiltersKeepsRestrictionsAndNoDuplicates() = withActivity { activity ->
        val view = XEditText(activity)
        val caps = InputFilter.AllCaps()
        view.inputRestrictions = XEditText.RESTRICTION_NO_SPACE
        repeat(3) {
            view.filters = arrayOf(caps, InputFilter.LengthFilter(5))
            view.inputRestrictions = XEditText.RESTRICTION_NO_SPACE
        }
        assertEquals(3, view.filters.size)
        assertSame(caps, view.filters[1])
        view.setText("abcdef")
        assertEquals("ABCDE", view.text.toString())
        view.text!!.replace(1, 3, "a b")
        assertEquals("ABCDE", view.text.toString())
        view.inputRestrictions = XEditText.RESTRICTION_NONE
        assertEquals(2, view.filters.size)
        view.setText("a b")
        assertEquals("A B", view.text.toString())
    }

    @Test
    fun filters_preserveCustomLengthFilterRules() = withActivity { activity ->
        val view = XEditText(activity)
        val custom = object : InputFilter.LengthFilter(5) {
            override fun filter(source: CharSequence, start: Int, end: Int, dest: Spanned, dstart: Int, dend: Int): CharSequence? {
                if ('#' in source.subSequence(start, end)) return dest.subSequence(dstart, dend)
                return super.filter(source, start, end, dest, dstart, dend)
            }
        }
        view.filters = arrayOf(custom)
        view.inputRestrictions = XEditText.RESTRICTION_NO_SPACE
        assertSame(custom, view.filters[1])
        view.setText("abc")
        view.text!!.replace(0, 1, "#")
        assertEquals("abc", view.text.toString())
        view.setText("abcdef")
        assertEquals("abcde", view.text.toString())
    }

    @Test
    fun filters_rejectedSelectionPreservesExistingOverLimitText() = withActivity { activity ->
        val view = XEditText(activity)
        view.setText("abcdef")
        view.filters = arrayOf(InputFilter.LengthFilter(2))
        view.inputRestrictions = XEditText.RESTRICTION_NO_SPACE
        assertEquals("abcdef", view.text.toString())
        view.text!!.replace(1, 3, " ")
        assertEquals("abcdef", view.text.toString())
        view.text!!.delete(0, 1)
        assertEquals("bcdef", view.text.toString())
    }

    @Test
    fun flags_sourceSliceAndDynamicLengthKeepRestrictions() = withActivity { activity ->
        val view = XEditText(activity)
        view.inputRestrictions = 7
        view.setMaxLength(8)
        view.text!!.replace(0, 0, "\uD83D\uDE00ok \n", 2, 4)
        assertEquals("ok", view.text.toString())
        view.setMaxLength(3)
        view.text!!.replace(0, 2, "\n")
        assertEquals("ok", view.text.toString())
        assertEquals(7, view.inputRestrictions)
    }

    @Test
    fun flags_canRemoveOnlyNewlineRestriction() = withActivity { activity ->
        val view = XEditText(activity)
        view.inputRestrictions = 7
        view.inputRestrictions = view.inputRestrictions and XEditText.RESTRICTION_NO_NEXT_LINE.inv()
        assertEquals(3, view.inputRestrictions)
        view.setText("a\nb")
        assertEquals("a\nb", view.text.toString())
        view.text!!.replace(0, 3, "a b")
        assertEquals("a\nb", view.text.toString())
    }

    @Test
    fun counter_xmlRestrictionsReachInnerInputAndCanBeChanged() = withActivity { activity ->
        val root = activity.layoutInflater.inflate(R.layout.widget_input_restrictions, activity.container, false)
        val view = root.findViewById<CounterInputLayout>(R.id.test_counter_input_restrictions)
        assertEquals(XEditText.RESTRICTION_NO_NEXT_LINE, view.inputRestrictions)
        view.setText("a b")
        view.editableText.replace(0, 3, "a\nb")
        assertEquals("a b", view.text)
        view.inputRestrictions = XEditText.RESTRICTION_NONE
        view.setText("a\nb")
        assertEquals("a\nb", view.text)
    }

    @Test
    fun flags_fullReplacementDoesNotChangeLengthLimit() = withActivity { activity ->
        val view = XEditText(activity)
        view.inputRestrictions = 7
        view.setMaxLength(5)
        view.inputRestrictions = XEditText.RESTRICTION_NO_EMOJI
        view.setText("a b\ncdef")
        assertEquals("a b\nc", view.text.toString())
        assertEquals(XEditText.RESTRICTION_NO_EMOJI, view.inputRestrictions)
        view.inputRestrictions = XEditText.RESTRICTION_NONE
        view.setText("\uD83D\uDE00")
        assertEquals("\uD83D\uDE00", view.text.toString())
    }

    @Test
    fun editorActions_restrictionsDoNotBlockImeActionCallback() = withActivity { activity ->
        val view = XEditText(activity)
        val forwarded = mutableListOf<Int>()
        view.setOnEditorActionListener { _, action, _ ->
            forwarded.add(action)
            true
        }
        view.inputRestrictions = 7
        view.onEditorAction(EditorInfo.IME_ACTION_DONE)
        assertEquals(listOf(EditorInfo.IME_ACTION_DONE), forwarded)
        view.inputRestrictions = XEditText.RESTRICTION_NONE
        view.onEditorAction(EditorInfo.IME_ACTION_SEND)
        assertEquals(listOf(EditorInfo.IME_ACTION_DONE, EditorInfo.IME_ACTION_SEND), forwarded)
    }

    private fun withActivity(block: (WidgetTestActivity) -> Unit) {
        ActivityScenario.launch(WidgetTestActivity::class.java).use { scenario -> scenario.onActivity { block(it) } }
    }
}
