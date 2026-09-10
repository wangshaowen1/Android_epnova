package com.kuaigeglobal.epnova.feature.web

import android.view.View
import androidx.core.graphics.Insets
import androidx.core.view.OnApplyWindowInsetsListener
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsAnimationCompat
import androidx.core.view.WindowInsetsCompat

/** 浏览器自行避让导航栏和 IME，顶部仍由页面工具栏/H5 处理。 */
internal class BrowserWindowInsetsHandler(
    private val root: View,
    private val isVideoFullscreen: () -> Boolean,
) : WindowInsetsAnimationCompat.Callback(DISPATCH_MODE_CONTINUE_ON_SUBTREE),
    OnApplyWindowInsetsListener {

    private val initialPadding = Insets.of(
        root.paddingLeft, root.paddingTop, root.paddingRight, root.paddingBottom,
    )
    private val imeAnimations = mutableSetOf<WindowInsetsAnimationCompat>()
    private var latestInsets: WindowInsetsCompat? = null
    private var attached = false

    fun attach() {
        if (attached) return
        attached = true
        ViewCompat.setOnApplyWindowInsetsListener(root, this)
        ViewCompat.setWindowInsetsAnimationCallback(root, this)
        ViewCompat.requestApplyInsets(root)
    }

    override fun onApplyWindowInsets(view: View, insets: WindowInsetsCompat): WindowInsetsCompat {
        if (!attached) return insets
        latestInsets = insets
        // onPrepare 先于目标 Insets 分发；动画结束前由 onProgress 更新，避免先跳到终点。
        if (imeAnimations.isEmpty()) applyPadding(insets)
        return remainingInsets(insets)
    }

    override fun onPrepare(animation: WindowInsetsAnimationCompat) {
        if (attached && animation.affectsIme()) imeAnimations.add(animation)
    }

    override fun onProgress(
        insets: WindowInsetsCompat,
        runningAnimations: MutableList<WindowInsetsAnimationCompat>,
    ): WindowInsetsCompat {
        if (!attached) return insets
        if (runningAnimations.any { it.affectsIme() }) applyPadding(insets)
        return remainingInsets(insets)
    }

    override fun onEnd(animation: WindowInsetsAnimationCompat) {
        if (!attached || !imeAnimations.remove(animation)) return
        // 中断、旋转或多个 IME 动画交接后，以最后一次目标 Insets 收敛。
        if (imeAnimations.isEmpty()) refresh()
    }

    fun refresh() {
        if (!attached) return
        latestInsets?.let(::applyPadding)
        ViewCompat.requestApplyInsets(root)
    }

    fun release() {
        if (!attached) return
        attached = false
        ViewCompat.setWindowInsetsAnimationCallback(root, null)
        ViewCompat.setOnApplyWindowInsetsListener(root, null)
        imeAnimations.clear()
        latestInsets = null
        root.setPadding(
            initialPadding.left, initialPadding.top, initialPadding.right, initialPadding.bottom,
        )
    }

    private fun applyPadding(insets: WindowInsetsCompat) {
        val safeInsets = insets.getInsets(
            WindowInsetsCompat.Type.navigationBars() or WindowInsetsCompat.Type.displayCutout(),
        )
        val imeBottom = if (isVideoFullscreen()) 0 else insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
        // IME 已包含底部导航栏区域，取最大值而非相加；横屏输入同样处理。
        val left = initialPadding.left + safeInsets.left
        val right = initialPadding.right + safeInsets.right
        val bottom = initialPadding.bottom + maxOf(safeInsets.bottom, imeBottom)
        if (root.paddingLeft != left || root.paddingRight != right || root.paddingBottom != bottom) {
            root.setPadding(left, initialPadding.top, right, bottom)
        }
    }

    private fun remainingInsets(insets: WindowInsetsCompat): WindowInsetsCompat {
        val cutout = insets.getInsets(WindowInsetsCompat.Type.displayCutout())
        // 父容器已避让的区域归零后继续分发，避免 WebView 再缩一次或收键盘后残留空白。
        return WindowInsetsCompat.Builder(insets)
            .setInsets(WindowInsetsCompat.Type.navigationBars() or WindowInsetsCompat.Type.ime(), Insets.NONE)
            .setInsets(WindowInsetsCompat.Type.displayCutout(), Insets.of(0, cutout.top, 0, 0))
            .build()
    }

    private fun WindowInsetsAnimationCompat.affectsIme(): Boolean =
        typeMask and WindowInsetsCompat.Type.ime() != 0
}
