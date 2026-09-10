package com.kuaigeglobal.epnova.widget.testing

import android.app.Activity
import android.os.Bundle
import android.widget.FrameLayout

/** 仅供控件仪器测试挂载真实窗口，不进入 release 构建。 */
class WidgetTestActivity : Activity() {
    lateinit var container: FrameLayout
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        container = FrameLayout(this)
        setContentView(container)
    }
}
