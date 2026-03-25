package com.kuaigeglobal.epnova.common.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.os.Environment
import androidx.core.graphics.createBitmap
import com.nhsd.basic.utils.Logger
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * *****************************************
 * 项目名称: KidsTales
 * @Author: SZYMN
 * @Introduce:可以将任意尺寸的图片转换为16:9和9:16两种比例，空白部分用白色填充，并保存到本地。
 * @Data: 2025/9/15/周一  15:15
 * @Version: 1.0
 * *****************************************
 */
object ImageRatioConverter {
    private const val TAG = "ImageRatioConverter"

    //16:9
    const val RATIO_16_9 = "sixteenCompareNine"

    //9:16
    const val RATIO_9_16 = "nineCompareSixteen"


    // 转换图片为指定比例并保存
    fun convertImageToRatios(context: Context, originalPath: String?): HashMap<String, String>? {
        if (originalPath.isNullOrEmpty()) return null
        try {
            // 读取原始图片
            val originalBitmap = BitmapFactory.decodeFile(originalPath)

            // 创建16:9比例的图片
            val landscapeBitmap = createRatioBitmap(originalBitmap, 1920, 1080)

            // 创建9:16比例的图片
            val portraitBitmap = createRatioBitmap(originalBitmap, 1080, 1920)

            // 保存图片
            val landscapePath = saveBitmap(context = context, landscapeBitmap, RATIO_16_9)
            val portraitPath = saveBitmap(context = context, portraitBitmap, RATIO_9_16)

            // 回收Bitmap内存
            originalBitmap.recycle()
            landscapeBitmap.recycle()
            portraitBitmap.recycle()

            return hashMapOf(RATIO_9_16 to portraitPath, RATIO_16_9 to landscapePath)
        } catch (e: Exception) {
            Logger.i("合成图片失败：${e.printStackTrace()}")
            return null
        }
    }

    // 创建指定比例的Bitmap，空白部分用白色填充
    private fun createRatioBitmap(
        originalBitmap: Bitmap,
        widthCompare: Int,
        heightCompare: Int
    ): Bitmap {
        if (widthCompare <= 0 || heightCompare <= 0) return originalBitmap
        val originalWidth = originalBitmap.width.toFloat()
        val originalHeight = originalBitmap.height.toFloat()

        val isWidth = if (originalWidth < widthCompare) {
            if (originalHeight < heightCompare) {
                if ((widthCompare / originalWidth * originalHeight) > heightCompare) {
                    false
                } else {
                    true
                }
            } else {
                false
            }
        } else {
            if (originalHeight > heightCompare) {
                if ((widthCompare / originalWidth * originalHeight) > heightCompare) {
                    false
                } else {
                    true
                }
            } else {
                true
            }
        }

        Logger.i(TAG, "isWidth：${isWidth}")
        Logger.i(TAG, "widthCompare：${widthCompare} heightCompare:${heightCompare}")
        Logger.i(TAG, "originalWidth：${originalWidth} originalHeight:${originalHeight}")

        // 创建目标Bitmap（白色背景）
        val resultBitmap = createBitmap(widthCompare, heightCompare)

        val canvas = Canvas(resultBitmap)
        canvas.drawColor(Color.WHITE)

        //计算缩放比例
        var scaleX = 1.0f
        var scaleY = 1.0f
        //判断图片是否需要缩放，如果需要就对比进行缩放
        if (originalHeight == heightCompare.toFloat() && originalWidth == widthCompare.toFloat()) {
            //不需要处理
            scaleX = 1.0f
            scaleY = 1.0f
        } else if (isWidth) {
            scaleX = widthCompare / originalWidth
            scaleY = scaleX
        } else {
            scaleY = heightCompare / originalHeight
            scaleX = scaleY
        }
        val matrix = Matrix()
        matrix.setScale(scaleX, scaleY)

        Logger.i(TAG, "originalWidth：${originalWidth*scaleX} originalHeight:${originalHeight*scaleY}")

        // 使用Bitmap.createBitmap进行高质量缩放
        val newOriginalBitmap = Bitmap.createBitmap(
            originalBitmap,
            0, 0,
            originalBitmap.width,
            originalBitmap.height,
            matrix,
            true
        )

        val paint = Paint()
        // 3. 计算前景图在底图中心显示的起始坐标
        val centerX = (resultBitmap.width - newOriginalBitmap.width) / 2f
        val centerY = (resultBitmap.height - newOriginalBitmap.height) / 2f


        // 如果你需要前景图保持原大小，使用这个：
        canvas.drawBitmap(newOriginalBitmap, centerX, centerY, paint)
        return resultBitmap
    }

    // 保存Bitmap到本地
    fun saveBitmap(context: Context, bitmap: Bitmap?, prefix: String): String {
        if (bitmap == null) return ""
        var storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        if (storageDir == null) {
              storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        }
        if (!storageDir.exists()) {
            storageDir.mkdirs()
        }
        val timestamp = System.currentTimeMillis()
        val fileName = "${prefix}_${timestamp}.jpg"
        val file = File(storageDir, fileName)

        try {
            val fos = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos)
            fos.flush()
            fos.close()
            return file.absolutePath
        } catch (e: IOException) {
            e.printStackTrace()
            return ""
        }
    }
}