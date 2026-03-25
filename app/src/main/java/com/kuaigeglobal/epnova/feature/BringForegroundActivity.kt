package com.kuaigeglobal.epnova.feature

import android.os.Bundle
import com.nhsd.basic.BaseActivity

/**
 * 将应用前台显示
 */
class BringForegroundActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        finish()
    }
}