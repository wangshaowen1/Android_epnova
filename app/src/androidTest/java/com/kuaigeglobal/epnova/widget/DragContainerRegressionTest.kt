package com.kuaigeglobal.epnova.widget

import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.widget.dragfooterview.DragContainer
import com.kuaigeglobal.epnova.widget.testing.WidgetTestActivity
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** 通过真实窗口布局检查拖动阈值、取消与离屏复位；未执行前不视为通过。 */
@RunWith(AndroidJUnit4::class)
class DragContainerRegressionTest {
    @Test
    fun cancelAfterThreshold_doesNotTrigger() = withContainer { _, view ->
        var triggers = 0
        view.setOnPullTriggerListener({ triggers++; true })
        pull(view)
        event(MotionEvent.ACTION_CANCEL, 0f) { view.onTouchEvent(it) }
        assertEquals(0, triggers)
        view.reset()
        assertReset(view)
    }

    @Test
    fun releaseAtThreshold_holdsUntilFinished() = withContainer { _, view ->
        var triggers = 0
        view.setOnPullTriggerListener({ triggers++; true }, holdOnTrigger = true)
        pull(view)
        event(MotionEvent.ACTION_UP, 0f) { view.onTouchEvent(it) }
        assertEquals(1, triggers)
        assertTrue(view.getChildAt(1).translationX < 0f)
        // 保持期间重复收到抬手不能再次触发。
        event(MotionEvent.ACTION_UP, 0f) { view.onTouchEvent(it) }
        assertEquals(1, triggers)
        view.finishTrigger()
        assertReset(view)
    }

    @Test
    fun detachWhileHolding_resetsWithoutRetriggering() = withContainer { activity, view ->
        var triggers = 0
        view.setOnPullTriggerListener({ triggers++; true }, holdOnTrigger = true)
        pull(view)
        event(MotionEvent.ACTION_UP, 0f) { view.onTouchEvent(it) }
        activity.container.removeView(view)
        assertReset(view)
        view.finishTrigger()
        assertEquals(1, triggers)
    }

    @Test
    fun disabledListener_doesNotIntercept() = withContainer { _, view ->
        view.setOnPullTriggerListener(null)
        event(MotionEvent.ACTION_DOWN, view.width - 1f) {
            assertFalse(view.onInterceptTouchEvent(it))
        }
        event(MotionEvent.ACTION_MOVE, 0f) {
            assertFalse(view.onInterceptTouchEvent(it))
        }
        assertReset(view)
    }

    private fun withContainer(block: (WidgetTestActivity, DragContainer) -> Unit) {
        ActivityScenario.launch(WidgetTestActivity::class.java).use { scenario ->
            lateinit var view: DragContainer
            scenario.onActivity { activity ->
                view = activity.layoutInflater.inflate(
                    R.layout.widget_regression_drag_container, activity.container, false,
                ) as DragContainer
                val list = view.getChildAt(1) as RecyclerView
                list.layoutManager = LinearLayoutManager(activity, RecyclerView.HORIZONTAL, false)
                list.adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
                    override fun getItemCount() = 1
                    override fun onCreateViewHolder(parent: ViewGroup, type: Int) =
                        object : RecyclerView.ViewHolder(TextView(parent.context).apply {
                            layoutParams = RecyclerView.LayoutParams(40, 40)
                        }) {}
                    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) = Unit
                }
                activity.container.addView(view)
            }
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            scenario.onActivity { activity ->
                assertTrue(view.width > 0)
                assertTrue(view.getChildAt(0).width > 0)
                block(activity, view)
            }
        }
    }

    private fun pull(view: DragContainer) {
        val start = view.width - 1f
        event(MotionEvent.ACTION_DOWN, start) { view.onInterceptTouchEvent(it) }
        val end = start - view.getChildAt(0).width * 2f
        event(MotionEvent.ACTION_MOVE, end) { assertTrue(view.onInterceptTouchEvent(it)) }
        assertTrue(view.getChildAt(1).translationX <= -view.getChildAt(0).width)
    }

    private fun assertReset(view: DragContainer) {
        assertEquals(0f, view.getChildAt(1).translationX, 0f)
        assertEquals(0f, view.getChildAt(0).alpha, 0f)
    }

    private fun event(action: Int, x: Float, block: (MotionEvent) -> Unit) {
        val now = SystemClock.uptimeMillis()
        val event = MotionEvent.obtain(now, now, action, x, 20f, 0)
        try { block(event) } finally { event.recycle() }
    }
}
