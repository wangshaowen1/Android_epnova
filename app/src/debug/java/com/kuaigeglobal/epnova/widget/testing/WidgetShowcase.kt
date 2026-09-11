package com.kuaigeglobal.epnova.widget.testing

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.drawable.AnimationDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.StateListDrawable
import android.view.ContextThemeWrapper
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.shape.ShapeAppearanceModel
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.widget.*
import com.kuaigeglobal.epnova.widget.roundedimageview.RoundedImageView

/** 本地生成样本，不依赖网络、账号和业务数据。仅 Debug 宿主按 intent extra 挂载。 */
internal fun WidgetTestActivity.showWidgetShowcase() {
    val context = ContextThemeWrapper(this, R.style.AppTheme)
    fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
    WindowCompat.setDecorFitsSystemWindows(window, false)
    val root = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        setBackgroundColor(Color.WHITE)
    }
    container.addView(root, FrameLayout.LayoutParams(-1, -1))
    ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
        val bars = insets.getInsets(WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.displayCutout())
        val sides = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
        view.setPadding(maxOf(bars.left, sides.left), bars.top, maxOf(bars.right, sides.right), 0)
        insets
    }
    val scroll = ScrollView(context)
    root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
    val body = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(8), dp(16), dp(16)) }
    scroll.addView(body)
    fun title(id: Int) = TextView(context).also {
        it.setText(id); it.setTextColor(Color.DKGRAY); it.textSize = 16f; it.setPadding(0, dp(14), 0, dp(6))
        body.addView(it, LinearLayout.LayoutParams(-1, -2))
    }
    fun button(id: Int, action: () -> Unit) {
        body.addView(Button(context).apply { setText(id); setOnClickListener { action() } }, LinearLayout.LayoutParams(-1, -2))
    }
    title(R.string.widget_audit_title)
    title(R.string.widget_audit_note)
    title(R.string.widget_audit_marquee)
    val marqueeHost = FrameLayout(context)
    val marquee = MarqueeTextView(context).apply {
        textSize = 18f; setTextColor(Color.BLACK); setMarqueeRepeatLimit(-1)
        setMarqueeText(getString(R.string.widget_audit_long_text))
    }
    body.addView(marqueeHost, LinearLayout.LayoutParams(-1, dp(64)))
    marqueeHost.addView(marquee, FrameLayout.LayoutParams(-1, -1))
    var longText = true
    button(R.string.widget_audit_toggle_text) {
        longText = !longText
        marquee.setMarqueeText(getString(if (longText) R.string.widget_audit_long_text else R.string.widget_audit_short_text))
    }
    button(R.string.widget_audit_toggle_direction) {
        marquee.layoutDirection = if (marquee.layoutDirection == View.LAYOUT_DIRECTION_RTL) View.LAYOUT_DIRECTION_LTR else View.LAYOUT_DIRECTION_RTL
    }
    var large = false
    button(R.string.widget_audit_toggle_size) {
        large = !large; marquee.textSize = if (large) 26f else 18f
        marquee.setTextColor(if (large) Color.BLUE else Color.BLACK)
    }
    var paused = false
    button(R.string.widget_audit_pause_resume) { paused = !paused; if (paused) marquee.pauseScroll() else marquee.startScroll() }
    button(R.string.widget_audit_reattach) {
        if (marquee.parent == null) marqueeHost.addView(marquee, FrameLayout.LayoutParams(-1, -1)) else marqueeHost.removeView(marquee)
    }

    val sample = Bitmap.createBitmap(240, 120, Bitmap.Config.ARGB_8888).also {
        val canvas = Canvas(it); val paint = Paint()
        paint.color = Color.rgb(255, 180, 40); canvas.drawRect(0f, 0f, 120f, 120f, paint)
        paint.color = Color.rgb(40, 120, 240); canvas.drawRect(120f, 0f, 240f, 120f, paint)
        paint.color = Color.WHITE; canvas.drawCircle(120f, 60f, 30f, paint)
    }
    title(R.string.widget_audit_crop)
    val crop = CropShapeImageView(context).apply {
        shapeAppearanceModel = ShapeAppearanceModel.builder().setAllCornerSizes(dp(18).toFloat()).build()
        setImageBitmap(sample)
    }
    body.addView(crop, LinearLayout.LayoutParams(dp(140), dp(100)))
    title(R.string.widget_audit_round)
    val rounded = RoundedImageView(context).apply { setImageBitmap(sample); setCornerRadius(dp(16).toFloat()); setBorderColor(Color.RED) }
    body.addView(rounded, LinearLayout.LayoutParams(dp(180), dp(120)))
    val modeText = title(R.string.widget_audit_scale)
    val scaleTypes = ImageView.ScaleType.values()
    var scaleIndex = scaleTypes.indexOf(ImageView.ScaleType.FIT_CENTER)
    button(R.string.widget_audit_scale) { scaleIndex = (scaleIndex + 1) % scaleTypes.size; rounded.scaleType = scaleTypes[scaleIndex]; modeText.text = scaleTypes[scaleIndex].name }
    button(R.string.widget_audit_border) { rounded.setBorderWidth(if (rounded.borderWidth == 0f) dp(8).toFloat() else 0f) }
    button(R.string.widget_audit_oval) { rounded.isOval = !rounded.isOval }
    var tile = 0
    button(R.string.widget_audit_tile) {
        tile = (tile + 1) % 3
        val modes = arrayOf(Shader.TileMode.CLAMP, Shader.TileMode.REPEAT, Shader.TileMode.MIRROR)
        rounded.tileModeX = modes[tile]; rounded.tileModeY = modes[tile]
    }
    val live = RoundedImageView(context).apply {
        setPreserveDrawableState(true); setCornerRadius(dp(12).toFloat())
        setImageDrawable(StateListDrawable().apply {
            addState(intArrayOf(android.R.attr.state_selected), ColorDrawable(Color.BLUE))
            addState(intArrayOf(), ColorDrawable(Color.RED))
        })
    }
    body.addView(live, LinearLayout.LayoutParams(dp(90), dp(60)))
    button(R.string.widget_audit_state) { live.isSelected = !live.isSelected }
    val animated = RoundedImageView(context).apply { setPreserveDrawableState(true); setOval(true) }
    val animation = AnimationDrawable().apply {
        addFrame(ColorDrawable(Color.RED), 700); addFrame(ColorDrawable(Color.BLUE), 700); isOneShot = false
    }
    animated.setImageDrawable(animation)
    body.addView(animated, LinearLayout.LayoutParams(dp(60), dp(60)))
    animation.start()

    title(R.string.widget_audit_edge)
    val edge = EdgeTransparentView(context)
    edge.addView(ImageView(context).apply { scaleType = ImageView.ScaleType.FIT_XY; setImageBitmap(sample) }, FrameLayout.LayoutParams(-1, -1))
    body.addView(edge, LinearLayout.LayoutParams(-1, dp(100)))
    var faded = true
    button(R.string.widget_audit_edge_width) { faded = !faded; edge.setDrawSize(if (faded) dp(20).toFloat() else 0f) }
    var strong = true
    button(R.string.widget_audit_edge_color) { strong = !strong; edge.setGradientColors(intArrayOf(if (strong) Color.WHITE else 0x66ffffff, Color.TRANSPARENT)) }

    title(R.string.widget_audit_load)
    val states = LoadStateViewHolder(context).apply {
        setContent(TextView(context).apply { setText(R.string.widget_audit_content); setTextColor(Color.BLUE); gravity = android.view.Gravity.CENTER })
        setRetryListener { showLoading() }
    }
    body.addView(states, LinearLayout.LayoutParams(-1, dp(250)))
    var state = 0
    button(R.string.widget_audit_load_next) {
        state = (state + 1) % 5
        when (state) { 0 -> states.showLoading(); 1 -> states.showLoadingNoText(); 2 -> states.showContent(); 3 -> states.showEmpty(); else -> states.showRetry(IllegalStateException()) }
    }
    val nav = NavBarView(context).apply { insetMode = NavBarView.INSET_WINDOW; setBackgroundColor(Color.GREEN) }
    button(R.string.widget_audit_nav) { nav.insetMode = if (nav.insetMode == NavBarView.INSET_WINDOW) NavBarView.INSET_LEGACY else NavBarView.INSET_WINDOW }
    root.addView(nav, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
    ViewCompat.requestApplyInsets(root)
}
