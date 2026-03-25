package com.kuaigeglobal.epnova.feature.download.material

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.core.graphics.createBitmap
import com.blankj.utilcode.util.ImageUtils
import com.kuaigeglobal.epnova.common.ext.ioScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ViewSourceMaterialItem(
    private val view: View,
    private val defaultWidth: Int = 720 // 默认测量宽度
) : SourceMaterialItem() {

    override val filename: String = "view_${System.nanoTime()}.png"
    private var viewWidth = 0
    private var viewHeight = 0
    override val width: Int
        get() = viewWidth
    override val height: Int
        get() = viewHeight

    override fun prepare(listener: Listener) {
        super.prepare(listener)
        ioScope.launch {
            if (file.exists()) {
                onProgress(100)
                onSuccess()
                return@launch
            }
            try {
                // 必须在主线程进行 View 的测量和绘制
                val bitmap = withContext(Dispatchers.Main) {
                    if (view.width == 0 || view.height == 0) {
                        // 如果 View 尚未布局，进行手动测量
                        // MeasureSpec.EXACTLY 用于固定宽度，UNSPECIFIED 用于高度自适应
                        view.measure(
                            View.MeasureSpec.makeMeasureSpec(defaultWidth, View.MeasureSpec.EXACTLY),
                            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
                        )
                        view.layout(0, 0, view.measuredWidth, view.measuredHeight)
                    }

                    // 创建 Bitmap，确保宽高有效
                    val w = if (view.width > 0) view.width else 1
                    val h = if (view.height > 0) view.height else 1

                    val bmp = createBitmap(w, h, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(bmp)
                    view.draw(canvas)
                    bmp
                }

                viewWidth = bitmap.width
                viewHeight = bitmap.height
                val res = ImageUtils.save(bitmap, file, Bitmap.CompressFormat.PNG, true)
                if (res) {
                    onProgress(100)
                    onSuccess()
                } else {
                    onFail(kotlin.Exception("View转存文件失败 ${file.name}"))
                }
            } catch (e: Throwable) {
                onFail(kotlin.Exception("View转存文件失败 ${file.name}", e))
            }
        }
    }

    override fun cancel() {}
}