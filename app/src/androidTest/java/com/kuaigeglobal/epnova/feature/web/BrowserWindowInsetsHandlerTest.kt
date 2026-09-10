package com.kuaigeglobal.epnova.feature.web

import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import androidx.core.graphics.Insets
import androidx.core.view.WindowInsetsAnimationCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 30)
class BrowserWindowInsetsHandlerTest {
    @Test
    fun paddingDoesNotAccumulateOrCountNavigationBarTwice() = withRoot { root ->
        val handler = BrowserWindowInsetsHandler(root) { false }
        handler.attach()
        try {
            repeat(3) { handler.onApplyWindowInsets(root, insets(ime = 300)) }
            assertEquals(305, root.paddingBottom)
            val forwarded = handler.onApplyWindowInsets(root, insets(ime = 0, sideCutout = 40))
            assertEquals(42, root.paddingLeft)
            assertEquals(3, root.paddingTop)
            assertEquals(29, root.paddingBottom)
            assertEquals(Insets.NONE, forwarded.getInsets(WindowInsetsCompat.Type.ime()))
            assertEquals(Insets.NONE, forwarded.getInsets(WindowInsetsCompat.Type.navigationBars()))
            assertEquals(Insets.of(0, 20, 0, 0), forwarded.getInsets(WindowInsetsCompat.Type.displayCutout()))
            assertEquals(30, forwarded.getInsets(WindowInsetsCompat.Type.statusBars()).top)
            assertFalse(forwarded.isConsumed)
            handler.onApplyWindowInsets(root, insets(ime = 300))
            assertEquals(2, root.paddingLeft)
        } finally {
            handler.release()
        }
    }

    @Test
    fun imeAnimationDefersTargetAndFollowsProgressBeforeSettling() = withRoot { root ->
        val handler = BrowserWindowInsetsHandler(root) { false }
        val animation = imeAnimation()
        handler.attach()
        try {
            handler.onApplyWindowInsets(root, insets(ime = 0))
            handler.onPrepare(animation)
            handler.onApplyWindowInsets(root, insets(ime = 300))
            assertEquals(29, root.paddingBottom)
            val forwarded = handler.onProgress(insets(ime = 150), mutableListOf(animation))
            assertEquals(155, root.paddingBottom)
            assertEquals(Insets.NONE, forwarded.getInsets(WindowInsetsCompat.Type.ime()))
            handler.onEnd(animation)
            assertEquals(305, root.paddingBottom)
            handler.onApplyWindowInsets(root, insets(ime = 0))
            assertEquals(29, root.paddingBottom)
        } finally {
            handler.release()
        }
    }

    @Test
    fun overlappingAnimationsKeepTheLatestTarget() = withRoot { root ->
        val handler = BrowserWindowInsetsHandler(root) { false }
        val opening = imeAnimation()
        val closing = imeAnimation()
        handler.attach()
        try {
            handler.onApplyWindowInsets(root, insets(ime = 0))
            handler.onPrepare(opening)
            handler.onApplyWindowInsets(root, insets(ime = 300))
            handler.onProgress(insets(ime = 150), mutableListOf(opening))
            handler.onPrepare(closing)
            handler.onApplyWindowInsets(root, insets(ime = 0))
            handler.onEnd(opening)
            assertEquals(155, root.paddingBottom)
            handler.onProgress(insets(ime = 60), mutableListOf(closing))
            assertEquals(65, root.paddingBottom)
            handler.onEnd(closing)
            assertEquals(29, root.paddingBottom)
        } finally {
            handler.release()
        }
    }

    @Test
    fun fullscreenVideoIgnoresImeButKeepsSystemSafeArea() = withRoot { root ->
        var fullscreen = false
        val handler = BrowserWindowInsetsHandler(root) { fullscreen }
        handler.attach()
        try {
            handler.onApplyWindowInsets(root, insets(ime = 300, sideCutout = 40))
            assertEquals(305, root.paddingBottom)
            fullscreen = true
            handler.refresh()
            assertEquals(29, root.paddingBottom)
            assertEquals(42, root.paddingLeft)
            fullscreen = false
            handler.refresh()
            assertEquals(305, root.paddingBottom)
        } finally {
            handler.release()
        }
    }

    @Test
    fun releaseStopsPendingCallbacksAndAllowsCleanReattach() = withRoot { root ->
        val handler = BrowserWindowInsetsHandler(root) { false }
        val animation = imeAnimation()
        handler.attach()
        handler.onApplyWindowInsets(root, insets(ime = 300))
        handler.onPrepare(animation)
        handler.release()
        handler.onProgress(insets(ime = 150), mutableListOf(animation))
        handler.onEnd(animation)
        handler.onApplyWindowInsets(root, insets(ime = 300))
        assertEquals(5, root.paddingBottom)
        handler.attach()
        try {
            handler.onApplyWindowInsets(root, insets(ime = 0))
            assertEquals(29, root.paddingBottom)
        } finally {
            handler.release()
        }
    }

    private fun insets(ime: Int, sideCutout: Int = 0): WindowInsetsCompat =
        WindowInsetsCompat.Builder()
            .setInsets(WindowInsetsCompat.Type.statusBars(), Insets.of(0, 30, 0, 0))
            .setInsets(WindowInsetsCompat.Type.navigationBars(), Insets.of(0, 0, 0, 24))
            .setInsets(WindowInsetsCompat.Type.displayCutout(), Insets.of(sideCutout, 20, 0, 0))
            .setInsets(WindowInsetsCompat.Type.ime(), Insets.of(0, 0, 0, ime))
            .build()

    private fun imeAnimation() = WindowInsetsAnimationCompat(
        WindowInsetsCompat.Type.ime(), LinearInterpolator(), 250L,
    )

    private fun withRoot(block: (FrameLayout) -> Unit) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync {
            block(FrameLayout(instrumentation.targetContext).apply { setPadding(2, 3, 4, 5) })
        }
    }
}
