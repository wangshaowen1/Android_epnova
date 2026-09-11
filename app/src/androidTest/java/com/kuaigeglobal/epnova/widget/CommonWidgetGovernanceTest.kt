package com.kuaigeglobal.epnova.widget

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Shader
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Animatable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.StateListDrawable
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.widget.roundedimageview.RoundedDrawable
import com.kuaigeglobal.epnova.widget.roundedimageview.RoundedImageView
import com.kuaigeglobal.epnova.widget.testing.WidgetTestActivity
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class CommonWidgetGovernanceTest {
    private fun withView(block: (WidgetTestActivity, Context) -> Unit) {
        ActivityScenario.launch(WidgetTestActivity::class.java).use { scenario ->
            scenario.onActivity { block(it, ContextThemeWrapper(it, R.style.AppTheme)) }
        }
    }
    private fun layout(view: View, w: Int = 160, h: Int = 60) {
        view.measure(View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY))
        view.layout(0, 0, w, h)
    }
    private fun draw(view: View): Bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888).also { view.draw(Canvas(it)) }
    private fun sample(): Bitmap = Bitmap.createBitmap(20, 10, Bitmap.Config.ARGB_8888).apply {
        eraseColor(Color.RED)
        for (y in 0 until 10) for (x in 10 until 20) setPixel(x, y, Color.BLUE)
    }
    private fun paint(drawable: Drawable, w: Int = 80, h: Int = 60): Bitmap {
        drawable.setBounds(0, 0, w, h)
        return Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).also { drawable.draw(Canvas(it)) }
    }
    private fun assertPixelsEqual(a: Bitmap, b: Bitmap) {
        assertEquals(a.width, b.width); assertEquals(a.height, b.height)
        for (y in 0 until a.height) for (x in 0 until a.width) assertEquals("pixel $x,$y", a.getPixel(x, y), b.getPixel(x, y))
    }

    @Test fun ordinaryCropImageKeepsSourceAndScaleType() = withView { _, c ->
        val image = CropShapeImageView(c); val bitmap = sample()
        image.setImageBitmap(bitmap); layout(image); draw(image)
        assertSame(bitmap, (image.drawable as BitmapDrawable).bitmap)
        assertEquals(ImageView.ScaleType.CENTER_CROP, image.scaleType)
        assertFalse(bitmap.isRecycled)
    }
    private class LimitedCanvas(bitmap: Bitmap) : Canvas(bitmap) {
        override fun getMaximumBitmapWidth() = 8
        override fun getMaximumBitmapHeight() = 8
    }
    @Test fun cropRecoveryIsDeferredAndRetainsAspectRatio() {
        ActivityScenario.launch(WidgetTestActivity::class.java).use { scenario ->
            lateinit var image: CropShapeImageView
            val bitmap = sample()
            scenario.onActivity {
                image = CropShapeImageView(ContextThemeWrapper(it, R.style.AppTheme)); it.container.addView(image)
                image.setImageBitmap(bitmap); layout(image)
                image.draw(LimitedCanvas(Bitmap.createBitmap(160, 60, Bitmap.Config.ARGB_8888)))
                assertSame(bitmap, (image.drawable as BitmapDrawable).bitmap)
            }
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            scenario.onActivity {
                val result = (image.drawable as BitmapDrawable).bitmap
                assertEquals(8, result.width); assertEquals(4, result.height); assertFalse(bitmap.isRecycled)
            }
        }
    }
    @Test fun delayedCropRecoveryCannotReplaceNewImage() {
        ActivityScenario.launch(WidgetTestActivity::class.java).use { scenario ->
            lateinit var image: CropShapeImageView
            val replacement = ColorDrawable(Color.GREEN)
            scenario.onActivity {
                image = CropShapeImageView(ContextThemeWrapper(it, R.style.AppTheme)); it.container.addView(image)
                image.setImageBitmap(sample()); layout(image)
                image.draw(LimitedCanvas(Bitmap.createBitmap(160, 60, Bitmap.Config.ARGB_8888)))
                image.setImageDrawable(replacement)
            }
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            scenario.onActivity { assertSame(replacement, image.drawable) }
        }
    }
    @Test fun edgeThresholdBypassBalancesCanvasAndKeepsChildOpaque() = withView { _, c ->
        val root = LayoutInflater.from(c).inflate(R.layout.view_common_widget_regression, null)
        val edge = root.findViewById<EdgeTransparentView>(R.id.regressionEdge)
        val width = (120 * c.resources.displayMetrics.density).toInt()
        layout(edge, width, 60)
        val bitmap = Bitmap.createBitmap(width, 60, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap); val before = canvas.saveCount
        edge.draw(canvas)
        assertEquals(before, canvas.saveCount)
        assertEquals(255, Color.alpha(bitmap.getPixel(1, 30)))
    }
    @Test fun edgeZeroWidthAndRuntimeColorsTakeEffectImmediately() = withView { _, c ->
        val edge = EdgeTransparentView(c)
        edge.addView(View(c).apply { setBackgroundColor(Color.BLUE) }, FrameLayout.LayoutParams(-1, -1))
        layout(edge)
        edge.setDrawSize(0f)
        assertEquals(Color.BLUE, draw(edge).getPixel(1, 30))
        edge.setDrawSize(20f); edge.setGradientColors(intArrayOf(Color.TRANSPARENT, Color.TRANSPARENT))
        assertEquals(Color.BLUE, draw(edge).getPixel(1, 30))
        edge.setGradientColors(intArrayOf(Color.WHITE, Color.TRANSPARENT))
        assertTrue(Color.alpha(draw(edge).getPixel(1, 30)) < 100)
    }
    @Test fun edgeRejectsBadColorsBeforeChangingExistingGradient() = withView { _, c ->
        val edge = EdgeTransparentView(c)
        edge.addView(View(c).apply { setBackgroundColor(Color.RED) }, FrameLayout.LayoutParams(-1, -1)); layout(edge)
        val before = draw(edge)
        try { edge.setGradientColors(intArrayOf(Color.BLUE)); fail("must reject invalid gradient") } catch (_: IllegalArgumentException) { }
        assertPixelsEqual(before, draw(edge))
    }
    @Test fun marqueeConstructorsAndXmlTextDrawWithoutBitmapInitialization() = withView { _, c ->
        val direct = MarqueeTextView(c).apply { setTextColor(Color.BLACK); text = "Visible" }
        layout(direct)
        val root = LayoutInflater.from(c).inflate(R.layout.view_common_widget_regression, null)
        val xml = root.findViewById<MarqueeTextView>(R.id.regressionMarquee); layout(xml)
        for (image in listOf(draw(direct), draw(xml))) {
            assertTrue((0 until image.width).any { x -> (0 until image.height).any { y -> Color.alpha(image.getPixel(x, y)) > 0 } })
        }
    }
    @Test fun marqueeProgrammaticTextColorUpdatesWithoutChangingText() = withView { _, c ->
        val view = MarqueeTextView(c).apply { text = "MMMM"; setTextColor(Color.RED) }; layout(view)
        val red = draw(view); view.setTextColor(Color.BLUE); val blue = draw(view)
        val points = (0 until red.width).flatMap { x -> (0 until red.height).map { y -> x to y } }
        val p = points.first { Color.alpha(red.getPixel(it.first, it.second)) > 200 }
        assertTrue(Color.red(red.getPixel(p.first, p.second)) > Color.blue(red.getPixel(p.first, p.second)))
        assertTrue(Color.blue(blue.getPixel(p.first, p.second)) > Color.red(blue.getPixel(p.first, p.second)))
    }
    @Test fun marqueeRepeatedMeasurementKeepsTextAndHintRenderable() = withView { activity, c ->
        val view = MarqueeTextView(c).apply { text = "Visible text"; setTextColor(Color.BLACK); setHintTextColor(Color.BLACK) }
        activity.container.addView(view, FrameLayout.LayoutParams(160, 60))
        layout(view)
        val first = draw(view)
        // 强制走第二轮 onMeasure，覆盖 ScrollView/权重布局的重复测量路径。
        view.forceLayout(); layout(view)
        assertPixelsEqual(first, draw(view))
        view.hint = "Visible hint"; view.text = ""
        for (width in listOf(90, 160, 160)) {
            view.forceLayout(); layout(view, width, 60)
            val bitmap = draw(view)
            assertTrue((0 until bitmap.width).any { x -> (0 until bitmap.height).any { y -> Color.alpha(bitmap.getPixel(x, y)) > 0 } })
        }
    }
    @Test fun widgetShowcaseLaunchCompletesWindowLayout() {
        val intent = Intent(InstrumentationRegistry.getInstrumentation().targetContext, WidgetTestActivity::class.java)
            .putExtra("widget_showcase", true)
        ActivityScenario.launch<WidgetTestActivity>(intent).use { scenario ->
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            scenario.onActivity {
                assertEquals(1, it.container.childCount)
                assertTrue(it.container.isLaidOut)
                assertTrue(it.container.getChildAt(0).isShown)
            }
        }
    }
    private class Owner : LifecycleOwner {
        val registry = LifecycleRegistry(this)
        override val lifecycle: Lifecycle get() = registry
    }
    @Test fun marqueeLifecycleBindingIsIdempotentAndUnhooksOnDetach() = withView { activity, c ->
        val owner = Owner(); owner.registry.currentState = Lifecycle.State.RESUMED
        val view = MarqueeTextView(c); activity.container.addView(view); layout(view)
        view.bindToLifeCycle(owner.lifecycle); view.bindToLifeCycle(owner.lifecycle)
        assertEquals(1, owner.registry.observerCount)
        activity.container.removeView(view); assertEquals(0, owner.registry.observerCount)
        activity.container.addView(view); assertEquals(1, owner.registry.observerCount)
        view.bindToLifeCycle(null); assertEquals(0, owner.registry.observerCount)
    }
    @Test fun marqueeChangingLifecycleDoesNotRetainPreviousOwner() = withView { activity, c ->
        val a = Owner(); val b = Owner(); val view = MarqueeTextView(c)
        activity.container.addView(view); view.bindToLifeCycle(a.lifecycle); view.bindToLifeCycle(b.lifecycle)
        assertEquals(0, a.registry.observerCount); assertEquals(1, b.registry.observerCount)
        b.registry.currentState = Lifecycle.State.CREATED
        b.registry.currentState = Lifecycle.State.DESTROYED
        assertEquals(0, b.registry.observerCount)
    }
    @Test fun marqueePauseFreezesPixelsAndStartResumesFrames() {
        ActivityScenario.launch(WidgetTestActivity::class.java).use { scenario ->
            lateinit var view: MarqueeTextView
            lateinit var paused: Bitmap
            val started = CountDownLatch(3)
            scenario.onActivity {
                view = MarqueeTextView(ContextThemeWrapper(it, R.style.AppTheme)).apply {
                    text = "MMMM Long scrolling text to verify pause and resume 1234567890"
                    setTextColor(Color.BLACK); setMarqueeRepeatLimit(-1); setRepeatDelay(0)
                    addMarqueeTextViewScrollListener { running -> if (running) started.countDown() }
                }
                it.container.addView(view, FrameLayout.LayoutParams(80, 40)); layout(view, 80, 40)
                view.startScroll()
            }
            assertTrue("scroll frames must start", started.await(3, TimeUnit.SECONDS))
            val resumed = CountDownLatch(2)
            scenario.onActivity {
                view.pauseScroll(); paused = draw(view)
                view.addMarqueeTextViewScrollListener { running -> if (running) resumed.countDown() }
            }
            assertFalse("pause must remove scheduled frames", resumed.await(120, TimeUnit.MILLISECONDS))
            scenario.onActivity { assertPixelsEqual(paused, draw(view)); view.startScroll() }
            assertTrue("start must resume", resumed.await(3, TimeUnit.SECONDS))
        }
    }
    @Test fun marqueeFiniteRepeatStopsAtInitialPosition() {
        ActivityScenario.launch(WidgetTestActivity::class.java).use { scenario ->
            lateinit var view: MarqueeTextView
            lateinit var initial: Bitmap
            val stopped = CountDownLatch(1)
            var moved = false
            scenario.onActivity {
                view = MarqueeTextView(ContextThemeWrapper(it, R.style.AppTheme)).apply {
                    text = "Finite marquee repeat 1234567890"; setTextColor(Color.BLACK)
                    setRepeatDelay(0); setDuration(100); setMarqueeRepeatLimit(1)
                    addMarqueeTextViewScrollListener { running ->
                        if (running) moved = true else if (moved) stopped.countDown()
                    }
                }
                it.container.addView(view, FrameLayout.LayoutParams(80, 40)); layout(view, 80, 40)
                initial = draw(view); view.restartScroll()
            }
            assertTrue("finite repeat must stop", stopped.await(3, TimeUnit.SECONDS))
            scenario.onActivity { assertPixelsEqual(initial, draw(view)) }
        }
    }
    @Test fun loadingTextIsRestoredAfterNoTextLoading() = withView { _, c ->
        val holder = LoadStateViewHolder(c)
        holder.showLoadingNoText(); assertEquals(View.GONE, holder.binding.loadingText.visibility)
        holder.showEmpty(); holder.showLoading()
        assertEquals(View.VISIBLE, holder.binding.loadingText.visibility); assertEquals(LoadStateViewHolder.LOADING, holder.currentViewIndex)
    }
    @Test fun invalidStateReplacementPreservesExistingChildrenAndState() = withView { _, c ->
        val holder = LoadStateViewHolder(c); val parent = FrameLayout(c); val replacement = View(c); parent.addView(replacement)
        holder.showEmpty(); val old = holder.currentView
        try { holder.setEmptyView(replacement); fail("already attached") } catch (_: IllegalArgumentException) { }
        assertSame(old, holder.currentView); assertEquals(3, holder.childCount); assertSame(parent, replacement.parent)
    }
    @Test fun replacingDisplayedStateKeepsTheSameStateAndNewViewVisible() = withView { _, c ->
        val holder = LoadStateViewHolder(c); holder.showEmpty(); val replacement = TextView(c)
        holder.setEmptyView(replacement); holder.setEmptyView(replacement)
        assertSame(replacement, holder.currentView); assertEquals(View.VISIBLE, replacement.visibility); assertEquals(3, holder.childCount)
    }
    @Test fun contentCanBeInstalledUnattachedAndReplacedRepeatedly() = withView { _, c ->
        val holder = LoadStateViewHolder(c); val first = View(c); val second = View(c)
        holder.setContent(first); holder.showContent(); holder.setContent(second); holder.setContent(second)
        assertEquals(4, holder.childCount); assertSame(second, holder.content); assertSame(second, holder.currentView); assertNull(first.parent)
    }
    @Test fun contentWrappingPreservesParentIndexAndOriginalLayoutParams() = withView { _, c ->
        val parent = FrameLayout(c); parent.addView(View(c)); val content = View(c)
        val params = FrameLayout.LayoutParams(90, 70).apply { leftMargin = 7 }
        parent.addView(content, params)
        val holder = LoadStateViewHolder(c); holder.setContent(content)
        assertSame(holder, parent.getChildAt(1)); assertSame(params, holder.layoutParams); assertSame(content, holder.content)
    }
    @Test fun stateGravityPreservesIndependentPaddingAndMarginsAfterScrollableWrap() = withView { _, c ->
        val holder = LoadStateViewHolder(c); holder.makeScrollable()
        holder.binding.retryLayout.setPadding(3, 4, 5, 6)
        (holder.binding.retryLayout.layoutParams as FrameLayout.LayoutParams).leftMargin = 11
        holder.setGravityHorizontalTop(20)
        assertEquals(3, holder.binding.retryLayout.paddingLeft); assertEquals(5, holder.binding.retryLayout.paddingRight)
        assertEquals(6, holder.binding.retryLayout.paddingBottom); assertEquals(20, holder.binding.retryLayout.paddingTop)
        assertEquals(11, (holder.binding.retryLayout.layoutParams as FrameLayout.LayoutParams).leftMargin)
    }
    @Test fun scrollableWrappingRetainsStateAndRevealsEachInnerView() = withView { _, c ->
        val holder = LoadStateViewHolder(c); holder.setContent(View(c)); holder.showEmpty()
        holder.makeScrollable(); holder.makeScrollable()
        assertEquals(LoadStateViewHolder.NO_DATA, holder.currentViewIndex)
        for (state in 0..3) {
            holder.displayedChild = state
            val wrapper = holder.currentView as androidx.core.widget.NestedScrollView
            assertEquals(View.VISIBLE, wrapper.visibility)
            assertEquals(1, wrapper.childCount); assertEquals(View.VISIBLE, wrapper.getChildAt(0).visibility)
        }
    }
    @Test fun xmlNetworkErrorImageDoesNotReadRequestErrorImageAttribute() = withView { _, c ->
        val root = LayoutInflater.from(c).inflate(R.layout.view_common_widget_regression, null)
        val holder = root.findViewById<LoadStateViewHolder>(R.id.regressionState)
        val field = LoadStateViewHolder::class.java.getDeclaredField("networkAnomalySrc").apply { isAccessible = true }
        assertEquals(android.R.drawable.ic_delete, field.getInt(holder))
    }
    @Test fun roundedResizeAfterFirstDrawMatchesFreshDrawable() {
        val src = sample(); val reused = RoundedDrawable(src).setScaleType(ImageView.ScaleType.CENTER_CROP)
        paint(reused, 100, 100)
        assertPixelsEqual(paint(RoundedDrawable(src).setScaleType(ImageView.ScaleType.CENTER_CROP), 60, 90), paint(reused, 60, 90))
    }
    @Test fun roundedBorderChangeAfterFirstDrawMatchesFreshDrawable() {
        val src = sample(); val reused = RoundedDrawable(src); paint(reused)
        reused.setBorderWidth(8f)
        assertPixelsEqual(paint(RoundedDrawable(src).setBorderWidth(8f)), paint(reused))
    }
    @Test fun roundedScaleChangeAfterFirstDrawMatchesFreshDrawable() {
        val src = sample(); val reused = RoundedDrawable(src); paint(reused)
        for (scale in ImageView.ScaleType.values()) {
            reused.setScaleType(scale)
            assertPixelsEqual(paint(RoundedDrawable(src).setScaleType(scale)), paint(reused))
        }
    }
    @Test fun roundedSquareTopRightRemainsFilledWhenImageHasVerticalInset() {
        val drawable = RoundedDrawable(sample()).setCornerRadius(8f, 0f, 8f, 8f)
        val result = paint(drawable, 80, 60)
        assertEquals(Color.BLUE, result.getPixel(79, 10))
        assertEquals(0, Color.alpha(result.getPixel(0, 10)))
    }
    @Test fun roundedRejectsInvalidBorderBeforeChangingState() = withView { _, c ->
        val view = RoundedImageView(c).apply { setImageBitmap(sample()); setBorderWidth(4f) }
        layout(view); val before = draw(view)
        try { view.setBorderWidth(-1f); fail("must reject negative border") } catch (_: IllegalArgumentException) { }
        assertEquals(4f, view.borderWidth, 0f); assertPixelsEqual(before, draw(view))
        view.setImageBitmap(sample()); assertPixelsEqual(before, draw(view))
    }
    @Test fun snapshotConversionDoesNotMutateSourceBoundsOrSharedLayerChildren() {
        val child = ColorDrawable(Color.BLUE); child.setBounds(7, 8, 12, 13)
        RoundedDrawable.drawableToBitmap(child)
        assertEquals(android.graphics.Rect(7, 8, 12, 13), child.bounds)
        val layer = LayerDrawable(arrayOf(ColorDrawable(Color.RED), ColorDrawable(Color.BLUE)))
        val first = layer.getDrawable(0); val second = layer.getDrawable(1)
        val converted = RoundedDrawable.fromDrawable(layer) as LayerDrawable
        assertSame(first, layer.getDrawable(0)); assertSame(second, layer.getDrawable(1))
        assertNotSame(converted.getDrawable(0), converted.getDrawable(1))
    }
    @Test fun roundedLiveDrawableUpdatesStateWhileDefaultRetainsStaticSnapshot() = withView { _, c ->
        fun selector() = StateListDrawable().apply {
            addState(intArrayOf(android.R.attr.state_selected), ColorDrawable(Color.BLUE)); addState(intArrayOf(), ColorDrawable(Color.RED))
        }
        val live = RoundedImageView(c).apply { setPreserveDrawableState(true); setImageDrawable(selector()) }
        val legacy = RoundedImageView(c).apply { setImageDrawable(selector()) }
        layout(live); layout(legacy); live.isSelected = true; legacy.isSelected = true
        assertEquals(Color.BLUE, draw(live).getPixel(80, 30)); assertEquals(Color.RED, draw(legacy).getPixel(80, 30))
    }
    @Test fun roundedLiveModeRetainsTileModesAndCornerConfiguration() = withView { _, c ->
        val view = RoundedImageView(c).apply {
            setPreserveDrawableState(true); setImageDrawable(ColorDrawable(Color.BLUE)); setCornerRadius(12f)
            tileModeX = Shader.TileMode.REPEAT; tileModeY = Shader.TileMode.MIRROR
        }
        layout(view); val image = draw(view)
        assertEquals(Color.BLUE, image.getPixel(80, 30)); assertEquals(0, Color.alpha(image.getPixel(0, 0)))
        assertEquals(Shader.TileMode.REPEAT, view.tileModeX); assertEquals(Shader.TileMode.MIRROR, view.tileModeY)
    }
    private class AnimatedColor : ColorDrawable(Color.RED), Animatable {
        private var running = false
        override fun start() { running = true }
        override fun stop() { running = false }
        override fun isRunning() = running
    }
    @Test fun roundedLiveAnimationStopsOnDetachAndResumesOnReattach() = withView { activity, c ->
        val source = AnimatedColor()
        val view = RoundedImageView(c).apply { setPreserveDrawableState(true); setImageDrawable(source) }
        activity.container.addView(view); source.start()
        activity.container.removeView(view); assertFalse(source.isRunning)
        activity.container.addView(view); assertTrue(source.isRunning)
        view.setImageDrawable(ColorDrawable(Color.BLUE))
        assertFalse(source.isRunning); assertNull(source.callback)
    }
    @Test fun disablingRoundedLiveModeReleasesSourceCallbacksAndKeepsCurrentPixels() = withView { _, c ->
        val source = AnimatedColor()
        val view = RoundedImageView(c).apply { setPreserveDrawableState(true); setImageDrawable(source) }
        layout(view); val before = draw(view); source.start()
        view.setPreserveDrawableState(false)
        assertFalse(source.isRunning); assertNull(source.callback); assertPixelsEqual(before, draw(view))
        view.setPreserveDrawableState(true)
        assertNotNull(source.callback); assertPixelsEqual(before, draw(view))
    }
    @Test fun explicitStopWhileRoundedAnimationIsHiddenCancelsAutomaticResume() = withView { _, c ->
        val source = AnimatedColor()
        val view = RoundedImageView(c).apply { setPreserveDrawableState(true); setImageDrawable(source) }
        val drawable = view.drawable
        val animation = drawable as Animatable
        animation.start(); assertTrue(source.isRunning)
        drawable.setVisible(false, false); assertFalse(source.isRunning)
        animation.stop()
        drawable.setVisible(true, false); assertFalse(source.isRunning)
        animation.start(); assertTrue(source.isRunning)
        drawable.setVisible(false, false)
        drawable.setVisible(true, false); assertTrue(source.isRunning)
    }
    @Test fun windowNavInsetTracksChangesAndDoesNotConsumeInsets() = withView { _, c ->
        val view = NavBarView(c).apply { insetMode = NavBarView.INSET_WINDOW }
        for (bottom in listOf(24, 48, 0)) {
            val insets = WindowInsetsCompat.Builder().setInsets(WindowInsetsCompat.Type.navigationBars(), Insets.of(0, 0, 0, bottom)).build()
            val result = ViewCompat.dispatchApplyWindowInsets(view, insets)
            view.measure(View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.AT_MOST))
            assertEquals(bottom, view.measuredHeight); assertEquals(insets.getInsets(WindowInsetsCompat.Type.navigationBars()), result.getInsets(WindowInsetsCompat.Type.navigationBars()))
        }
    }
    @Test fun windowNavInsetRespectsParentExactHeightAndIgnoresKeyboardHeight() = withView { _, c ->
        val view = NavBarView(c).apply { insetMode = NavBarView.INSET_WINDOW }
        val insets = WindowInsetsCompat.Builder().setInsets(WindowInsetsCompat.Type.navigationBars(), Insets.of(0, 0, 0, 20))
            .setInsets(WindowInsetsCompat.Type.ime(), Insets.of(0, 0, 0, 300)).build()
        ViewCompat.dispatchApplyWindowInsets(view, insets)
        layout(view, 100, 7); assertEquals(7, view.measuredHeight); assertEquals(20, view.navBarHeight())
    }
}
