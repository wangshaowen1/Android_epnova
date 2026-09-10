package com.kuaigeglobal.epnova.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.widget.FrameLayout
import androidx.core.view.doOnLayout
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kuaigeglobal.epnova.widget.testing.WidgetTestActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** 使用内存 PNG 和真实窗口，不依赖网络、图片服务器或私有状态反射。 */
@RunWith(AndroidJUnit4::class)
class TransitionImageViewRegressionTest {
    @Test
    fun windowedCache_seekFromImageCallback_keepsNewTarget() = withWindowedView { scenario, view ->
        val arrived = CountDownLatch(1)
        val positions = mutableListOf<Int>()
        scenario.onActivity {
            view.pause()
            view.setOnPlayStateListener(object : Listener() {
                override fun onImageChanged(position: Int, total: Int) {
                    positions.add(position)
                    if (position == 1) view.seekTo(7, false)
                    if (position == 7) arrived.countDown()
                }
            })
            view.seekTo(1, false)
        }
        await(arrived)
        scenario.onActivity {
            assertEquals(listOf(1, 7), positions)
            assertCenterColor(view, Color.YELLOW)
        }
    }

    @Test
    fun windowedCache_consecutiveUncachedSeeks_onlyShowsLatestTarget() = withWindowedView { scenario, view ->
        val arrived = CountDownLatch(1)
        val positions = mutableListOf<Int>()
        scenario.onActivity {
            view.pause()
            view.setOnPlayStateListener(object : Listener() {
                override fun onImageChanged(position: Int, total: Int) {
                    positions.add(position)
                    if (position == 7) arrived.countDown()
                }
            })
            view.seekTo(4, false)
            view.seekTo(7, false)
        }
        await(arrived)
        scenario.onActivity {
            assertEquals(listOf(7), positions)
            assertEquals(7, view.getCurrentPosition())
            assertCenterColor(view, Color.YELLOW)
        }
    }

    @Test
    fun windowedCache_releaseCancelsPendingSeek() = withWindowedView { scenario, view ->
        val drained = CountDownLatch(1)
        var changes = 0
        scenario.onActivity {
            view.setOnPlayStateListener(object : Listener() {
                override fun onImageChanged(position: Int, total: Int) { changes++ }
            })
            view.seekTo(6)
            view.release()
            Handler(Looper.getMainLooper()).postDelayed({ drained.countDown() }, 200)
        }
        await(drained)
        scenario.onActivity {
            assertEquals(0, changes)
            assertEquals(0, view.getTotalCount())
        }
    }

    @Test
    fun windowedCache_playbackMovesBeyondInitialWindow() = withWindowedView { scenario, view ->
        val advanced = CountDownLatch(1)
        scenario.onActivity {
            view.pause()
            view.setLoop(false)
            view.setStaticDisplayTime(20)
            view.setTransitionDuration(0)
            view.setOnPlayStateListener(object : Listener() {
                override fun onImageChanged(position: Int, total: Int) {
                    if (position == 5) {
                        view.pause()
                        advanced.countDown()
                    }
                }
            })
            view.play()
        }
        await(advanced)
        scenario.onActivity { assertEquals(5, view.getCurrentPosition()) }
    }

    private fun withWindowedView(block: (ActivityScenario<WidgetTestActivity>, TransitionImageView) -> Unit) {
        ActivityScenario.launch(WidgetTestActivity::class.java).use { scenario ->
            lateinit var view: TransitionImageView
            val loaded = CountDownLatch(2)
            scenario.onActivity { activity ->
                view = TransitionImageView(activity)
                view.setMaxCachedImages(3)
                activity.container.addView(view, FrameLayout.LayoutParams(120, 120))
                view.doOnLayout { loaded.countDown() }
                view.setStaticDisplayTime(60_000)
                view.setOnPlayStateListener(object : Listener() {
                    override fun onImageChanged(position: Int, total: Int) { loaded.countDown() }
                })
                view.setImageUrls((0..7).map { imageUrl(if (it == 7) Color.YELLOW else Color.RED + it) })
                view.play()
            }
            await(loaded)
            try { block(scenario, view) } finally { scenario.onActivity { view.release() } }
        }
    }

    @Test
    fun seekingCurrentPosition_keepsAutomaticPlayback() = withLoadedView { scenario, view ->
        val advanced = CountDownLatch(1)
        scenario.onActivity {
            view.pause()
            view.setStaticDisplayTime(30)
            view.setTransitionDuration(0)
            view.setOnPlayStateListener(object : Listener() {
                override fun onImageChanged(position: Int, total: Int) {
                    if (position == 1) {
                        view.pause()
                        advanced.countDown()
                    }
                }
            })
            view.play()
            view.seekTo(view.getCurrentPosition())
        }
        await(advanced)
    }

    @Test
    fun consecutiveSeeks_onlyCompleteLatestAnimation() = withLoadedView { scenario, view ->
        val completed = CountDownLatch(1)
        val positions = mutableListOf<Int>()
        scenario.onActivity {
            view.setTransitionDuration(80)
            view.setOnPlayStateListener(object : Listener() {
                override fun onSeekAnimationEnd(toPosition: Int) {
                    positions.add(toPosition)
                    if (toPosition == 2) completed.countDown()
                }
            })
            view.seekTo(1)
            view.seekTo(2)
        }
        await(completed)
        scenario.onActivity {
            assertEquals(listOf(2), positions)
            assertEquals(2, view.getCurrentPosition())
            assertFalse(view.isSeeking())
        }
    }

    @Test
    fun releaseDuringSeek_suppressesLateCallbacks() = withLoadedView { scenario, view ->
        val drained = CountDownLatch(1)
        var completions = 0
        scenario.onActivity {
            view.setTransitionDuration(40)
            view.setOnPlayStateListener(object : Listener() {
                override fun onSeekAnimationEnd(toPosition: Int) { completions++ }
            })
            view.seekTo(1)
            view.release()
            Handler(Looper.getMainLooper()).postDelayed({ drained.countDown() }, 120)
        }
        await(drained)
        scenario.onActivity {
            assertEquals(0, completions)
            assertEquals(0, view.getTotalCount())
            assertEquals(TransitionImageView.PlayState.STOPPED, view.getCurrentState())
            assertFalse(view.isSeeking())
        }
    }

    @Test
    fun detachAndReattach_restoresPlaybackAndImage() = withLoadedView { scenario, view ->
        val restored = CountDownLatch(1)
        scenario.onActivity { activity ->
            view.setOnPlayStateListener(object : Listener() {
                override fun onImageChanged(position: Int, total: Int) { restored.countDown() }
            })
            activity.container.removeView(view)
            activity.container.addView(view)
        }
        await(restored)
        scenario.onActivity {
            assertEquals(3, view.getTotalCount())
            assertEquals(TransitionImageView.PlayState.PLAYING, view.getCurrentState())
            assertCenterColor(view, Color.RED)
        }
    }

    @Test
    fun replacingImageBatch_doesNotDisplayOldImage() = withLoadedView { scenario, view ->
        val replaced = CountDownLatch(1)
        val green = imageUrl(Color.GREEN)
        scenario.onActivity {
            view.setOnPlayStateListener(object : Listener() {
                override fun onImageChanged(position: Int, total: Int) {
                    if (total == 1) replaced.countDown()
                }
            })
            view.setImageUrls(listOf(imageUrl(Color.BLUE), imageUrl(Color.RED)))
            view.setImageUrls(listOf(green))
            view.play()
        }
        await(replaced)
        scenario.onActivity {
            assertEquals(1, view.getTotalCount())
            assertCenterColor(view, Color.GREEN)
        }
    }

    @Test
    fun failedImage_preservesFollowingImageIndex() = withLoadedView { scenario, view ->
        val replaced = CountDownLatch(1)
        scenario.onActivity {
            view.setOnPlayStateListener(object : Listener() {
                override fun onImageChanged(position: Int, total: Int) { replaced.countDown() }
            })
            view.setImageUrls(listOf(imageUrl(Color.RED), "data:image/png;base64,invalid", imageUrl(Color.BLUE)))
            view.play()
        }
        await(replaced)
        scenario.onActivity {
            view.seekTo(2, false)
            assertEquals(2, view.getCurrentPosition())
            assertCenterColor(view, Color.BLUE)
        }
    }

    private fun withLoadedView(block: (ActivityScenario<WidgetTestActivity>, TransitionImageView) -> Unit) {
        ActivityScenario.launch(WidgetTestActivity::class.java).use { scenario ->
            lateinit var view: TransitionImageView
            val loaded = CountDownLatch(2)
            scenario.onActivity { activity ->
                view = TransitionImageView(activity)
                activity.container.addView(view, FrameLayout.LayoutParams(120, 120))
                view.doOnLayout { loaded.countDown() }
                view.setStaticDisplayTime(60_000)
                view.setOnPlayStateListener(object : Listener() {
                    override fun onImageChanged(position: Int, total: Int) { loaded.countDown() }
                })
                view.setImageUrls(listOf(imageUrl(Color.RED), imageUrl(Color.GREEN), imageUrl(Color.BLUE)))
                view.play()
            }
            await(loaded)
            try {
                block(scenario, view)
            } finally {
                scenario.onActivity { view.release() }
            }
        }
    }

    private fun imageUrl(color: Int): String {
        val bitmap = Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888)
        return try {
            bitmap.eraseColor(color)
            val output = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
            "data:image/png;base64," + Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)
        } finally {
            bitmap.recycle()
        }
    }

    private fun assertCenterColor(view: TransitionImageView, color: Int) {
        assertTrue(view.width > 0 && view.height > 0)
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        try {
            view.draw(Canvas(bitmap))
            assertEquals(color, bitmap.getPixel(view.width / 2, view.height / 2))
        } finally {
            bitmap.recycle()
        }
    }

    private fun await(latch: CountDownLatch) {
        assertTrue("控件未在超时前发出预期回调", latch.await(5, TimeUnit.SECONDS))
    }

    private open class Listener : TransitionImageView.OnPlayStateListener {
        override fun onPlayStart() = Unit
        override fun onPlayPause() = Unit
        override fun onPlayResume() = Unit
        override fun onPlayStop() = Unit
        override fun onImageChanged(position: Int, total: Int) = Unit
        override fun onPlayCompleted() = Unit
        override fun onSeekAnimationStart(fromPosition: Int, toPosition: Int) = Unit
        override fun onSeekAnimationEnd(toPosition: Int) = Unit
    }
}
