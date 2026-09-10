package com.kuaigeglobal.epnova.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.text.InputFilter
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.widget.progress.RingProgressView
import com.kuaigeglobal.epnova.widget.testing.WidgetTestActivity
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** 覆盖真实 View 事件、绘制和生命周期；仅静态检查不代表这些仪器用例已运行。 */
@RunWith(AndroidJUnit4::class)
class WidgetOptimizationRegressionTest {
    @Test
    fun autoScroll_hiddenViewStopsAndExplicitStopSurvivesShowing() = withWindow { scenario ->
        lateinit var view: AutoScrollRecyclerView
        var offset = 0
        scenario.onActivity { activity ->
            view = AutoScrollRecyclerView(activity, null)
            view.layoutManager = LinearLayoutManager(activity)
            view.adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
                override fun getItemCount() = 50
                override fun onCreateViewHolder(parent: ViewGroup, type: Int): RecyclerView.ViewHolder =
                    object : RecyclerView.ViewHolder(TextView(parent.context).apply {
                        layoutParams = RecyclerView.LayoutParams(100, 40)
                    }) {}
                override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
                    (holder.itemView as TextView).text = position.toString()
                }
            }
            activity.container.addView(view, FrameLayout.LayoutParams(100, 100))
            view.start()
        }
        drain(250)
        scenario.onActivity {
            offset = view.computeVerticalScrollOffset()
            assertTrue(offset > 0)
            view.visibility = View.INVISIBLE
        }
        drain(150)
        scenario.onActivity {
            assertEquals(offset, view.computeVerticalScrollOffset())
            view.stop()
            view.visibility = View.VISIBLE
        }
        drain(150)
        scenario.onActivity { assertEquals(offset, view.computeVerticalScrollOffset()) }
    }

    @Test
    fun autoScroll_detachPausesAndReattachResumesUnlessStopped() = withWindow { scenario ->
        lateinit var view: AutoScrollRecyclerView
        var offset = 0
        scenario.onActivity { activity ->
            view = AutoScrollRecyclerView(activity, null)
            view.layoutManager = LinearLayoutManager(activity)
            view.adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
                override fun getItemCount() = 50
                override fun onCreateViewHolder(parent: ViewGroup, type: Int): RecyclerView.ViewHolder =
                    object : RecyclerView.ViewHolder(TextView(parent.context).apply {
                        layoutParams = RecyclerView.LayoutParams(100, 40)
                    }) {}
                override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
                    (holder.itemView as TextView).text = position.toString()
                }
            }
            activity.container.addView(view, FrameLayout.LayoutParams(100, 100))
            view.start()
        }
        drain(250)
        scenario.onActivity { activity ->
            offset = view.computeVerticalScrollOffset()
            assertTrue(offset > 0)
            activity.container.removeView(view)
        }
        drain(150)
        scenario.onActivity { activity ->
            assertEquals(offset, view.computeVerticalScrollOffset())
            activity.container.addView(view)
        }
        drain(250)
        scenario.onActivity { activity ->
            assertTrue(view.computeVerticalScrollOffset() > offset)
            activity.container.removeView(view)
            view.stop()
            activity.container.addView(view)
        }
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        scenario.onActivity { offset = view.computeVerticalScrollOffset() }
        drain(150)
        scenario.onActivity { assertEquals(offset, view.computeVerticalScrollOffset()) }
    }

    @Test
    fun gradient_inflationPreservesOpaqueStateColorsAndRuntimeDisable() = withWindow { scenario ->
        scenario.onActivity { activity ->
            val root = activity.layoutInflater.inflate(R.layout.widget_regression_views, activity.container, false)
            val view = root.findViewById<GradientTextView>(R.id.test_gradient_text)
            val colors = view.textColors
            assertTrue(colors.isStateful)
            assertEquals(Color.RED, view.currentTextColor)
            view.layout(0, 0, 100, 100)
            view.setColorList(Color.GREEN)
            view.isEnabled = false
            assertEquals(Color.BLUE, view.currentTextColor)
            assertSame(colors, view.textColors)
            view.setGradientEnable(false)
            assertNull(view.paint.shader)
            assertEquals(Color.BLUE, view.currentTextColor)
            view.isEnabled = true
            assertEquals(Color.RED, view.currentTextColor)
        }
    }

    @Test
    fun codeInput_zeroLengthAndLargeFont_areSafe() = withWindow { scenario ->
        scenario.onActivity { activity ->
            val view = CodeInputView(activity).apply {
                layout(0, 0, 200, 100)
                setTextColor(Color.GREEN)
                textSize = 30f
                setText("1234")
            }
            val expectedSize = view.paint.textSize
            pixel(view, 100, 50)
            assertEquals(Color.GREEN, view.paint.color)
            assertEquals(expectedSize, view.paint.textSize, 0f)
            view.filters = arrayOf(InputFilter.LengthFilter(0))
            pixel(view, 100, 50)
            assertEquals(0, view.codeLength)
        }
    }

    @Test
    fun gradient_singleColorAndRuntimeDisable_areSafe() = withWindow { scenario ->
        scenario.onActivity { activity ->
            val view = GradientTextView(activity)
            view.layout(0, 0, 100, 100)
            view.setColorList(Color.RED)
            assertNotNull(view.paint.shader)
            view.setGradientEnable(false)
            assertNull(view.paint.shader)
            view.setGradientEnable(true)
            assertNotNull(view.paint.shader)
            view.setColorList(*intArrayOf())
            assertNull(view.paint.shader)
        }
    }

    @Test
    fun ring_nonzeroRangeAndEqualBounds_drawCorrectly() = withWindow { scenario ->
        scenario.onActivity { activity ->
            val view = RingProgressView(activity).apply {
                ringColor = Color.RED
                duration = 0
                startProgress = 20f
                endProgress = 100f
                progress = 20f
                layout(0, 0, 100, 100)
            }
            assertEquals(Color.TRANSPARENT, pixel(view, 96, 50))
            view.progress = 60f
            assertEquals(Color.RED, pixel(view, 96, 50))
            assertEquals(Color.TRANSPARENT, pixel(view, 4, 50))
            view.startProgress = 100f
            assertEquals(Color.TRANSPARENT, pixel(view, 96, 50))
        }
    }

    @Test
    fun gradientBorder_widthChange_rebuildsGeometry() = withWindow { scenario ->
        scenario.onActivity { activity ->
            val root = activity.layoutInflater.inflate(R.layout.widget_regression_views, activity.container, false)
            val view = root.findViewById<GradientBorderView>(R.id.test_gradient_border)
            view.layout(0, 0, 100, 100)
            view.borderWidth = 2f
            assertEquals(Color.BLUE, pixel(view, 5, 50))
            view.borderWidth = 12f
            assertEquals(Color.RED, pixel(view, 5, 50))
        }
    }

    @Test
    fun dragDisabled_preservesChildClickAndAccessibleContainerClick() = withWindow { scenario ->
        scenario.onActivity { activity ->
            val root = activity.layoutInflater.inflate(R.layout.widget_regression_views, activity.container, false)
            val view = root.findViewById<MoveFrameLayout>(R.id.test_drag_disabled)
            val child = root.findViewById<View>(R.id.test_drag_child)
            view.measure(exact(120), exact(60))
            view.layout(0, 0, 120, 60)
            var childClicks = 0
            child.setOnClickListener { childClicks++ }
            touch(view, MotionEvent.ACTION_DOWN, 30f, 30f)
            touch(view, MotionEvent.ACTION_UP, 30f, 30f)
            assertEquals(1, childClicks)
            var containerClicks = 0
            view.setOnMfClickListener { containerClicks++ }
            assertTrue(view.performClick())
            assertEquals(1, containerClicks)
        }
    }

    @Test
    fun dragCancel_doesNotClick() = withWindow { scenario ->
        scenario.onActivity { activity ->
            val view = MoveFrameLayout(activity)
            activity.container.addView(view, FrameLayout.LayoutParams(100, 100))
            var clicks = 0
            view.setOnMfClickListener { clicks++ }
            touch(view, MotionEvent.ACTION_DOWN, 20f, 20f)
            touch(view, MotionEvent.ACTION_CANCEL, 20f, 20f)
            assertEquals(0, clicks)
            assertFalse(view.isPressed)
        }
    }

    @Test
    fun pendingMicroAnimation_stopBeforeLayout_doesNotRestart() = withWindow { scenario ->
        lateinit var view: MicroAnimImageView
        scenario.onActivity { activity ->
            view = MicroAnimImageView(activity)
            view.setMicroAnimationConfig(MicroAnimationConfig(MicroAnimationConfig.TranslationDirection.LEFT))
            activity.container.addView(view, FrameLayout.LayoutParams(0, 0))
            view.startMicroAnimation()
            view.stopMicroAnimation()
            view.layoutParams = FrameLayout.LayoutParams(100, 100)
        }
        drain(150)
        scenario.onActivity { assertFalse(view.isMicroAnimationRunning()) }
    }

    @Test
    fun pressedScale_detachRestoresOriginalAndDisabledDoesNotAnimate() = withWindow { scenario ->
        lateinit var view: ScalableTextView
        scenario.onActivity { activity ->
            view = ScalableTextView(activity).apply {
                scaleX = 1.4f
                scaleY = 0.8f
            }
            activity.container.addView(view, FrameLayout.LayoutParams(100, 100))
            view.isPressed = true
        }
        drain(200)
        scenario.onActivity { activity ->
            assertEquals(1.4f * 0.95f, view.scaleX, 0.001f)
            activity.container.removeView(view)
            assertEquals(1.4f, view.scaleX, 0.001f)
            assertEquals(0.8f, view.scaleY, 0.001f)
            activity.container.addView(view)
            view.isEnabled = false
            view.isPressed = false
            view.isPressed = true
        }
        drain(200)
        scenario.onActivity { assertEquals(1.4f, view.scaleX, 0.001f) }
    }

    @Test
    fun avatars_rebindReusesChildrenAndConfigurationUpdatesImmediately() = withWindow { scenario ->
        scenario.onActivity { activity ->
            val view = MultiAvatarView<String>(activity)
            view.setAvatars(arrayOf<String?>(null, null, null))
            val first = view.getChildAt(0)
            val second = view.getChildAt(1)
            view.setAvatars(arrayOf<String?>(null, null, null))
            assertSame(first, view.getChildAt(0))
            view.setMaxCount(2)
            assertEquals(2, view.childCount)
            assertSame(first, view.getChildAt(0))
            assertSame(second, view.getChildAt(1))
            view.setNeedShowMore(true)
            assertSame(first, view.getChildAt(0))
            assertNotSame(second, view.getChildAt(1))
            view.setAvatars(emptyArray<String>())
            assertEquals(0, view.childCount)
        }
    }

    @Test
    fun expandCollapseExpand_oldCompletionCannotTruncateNewState() = withExpandable { scenario, view ->
        scenario.onActivity {
            val button = view.findViewById<View>(R.id.btnExpandRight)
            button.performClick()
            button.performClick()
            button.performClick()
        }
        drain(250)
        scenario.onActivity {
            assertTrue(view.isExpanded)
            assertEquals(Int.MAX_VALUE, view.findViewById<TextView>(R.id.tvContent).maxLines)
            assertEquals(FrameLayout.LayoutParams.WRAP_CONTENT, view.layoutParams.height)
        }
    }

    @Test
    fun replacingExpandedText_cancelsOldHeightAnimation() = withExpandable { scenario, view ->
        scenario.onActivity {
            view.findViewById<View>(R.id.btnExpandRight).performClick()
            view.text = "short"
        }
        drain(250)
        scenario.onActivity {
            assertFalse(view.isExpanded)
            assertEquals("short", view.text.toString())
            assertEquals(1, view.findViewById<TextView>(R.id.tvContent).maxLines)
            assertEquals(FrameLayout.LayoutParams.WRAP_CONTENT, view.layoutParams.height)
        }
    }

    private fun withExpandable(block: (ActivityScenario<WidgetTestActivity>, ExpandableTextView) -> Unit) =
        withWindow { scenario ->
            lateinit var view: ExpandableTextView
            scenario.onActivity { activity ->
                view = ExpandableTextView(activity).apply {
                    setExpandText("expand")
                    setCollapseText("collapse")
                    setAnimationDuration(80)
                    text = (1..20).joinToString("\n") { "line $it" }
                }
                activity.container.addView(view, FrameLayout.LayoutParams(200, FrameLayout.LayoutParams.WRAP_CONTENT))
            }
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            scenario.onActivity { assertTrue(view.isExpandable) }
            block(scenario, view)
        }

    private fun withWindow(block: (ActivityScenario<WidgetTestActivity>) -> Unit) {
        ActivityScenario.launch(WidgetTestActivity::class.java).use(block)
    }

    private fun drain(delay: Long) {
        val latch = CountDownLatch(1)
        Handler(Looper.getMainLooper()).postDelayed({ latch.countDown() }, delay)
        assertTrue(latch.await(5, TimeUnit.SECONDS))
    }

    private fun touch(view: View, action: Int, x: Float, y: Float) {
        val now = SystemClock.uptimeMillis()
        val event = MotionEvent.obtain(now, now, action, x, y, 0)
        try { view.dispatchTouchEvent(event) } finally { event.recycle() }
    }

    private fun exact(size: Int) = View.MeasureSpec.makeMeasureSpec(size, View.MeasureSpec.EXACTLY)

    private fun pixel(view: View, x: Int, y: Int): Int {
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        return try {
            view.draw(Canvas(bitmap))
            bitmap.getPixel(x, y)
        } finally { bitmap.recycle() }
    }
}
