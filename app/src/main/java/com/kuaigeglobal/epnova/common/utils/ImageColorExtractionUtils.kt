package com.kuaigeglobal.epnova.common.utils

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.widget.ImageView
import androidx.palette.graphics.Palette
import com.blankj.utilcode.util.ScreenUtils
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.request.transition.Transition
import com.kuaige.imageloader.GlideApp
import com.nhsd.basic.utils.Logger
import com.kuaigeglobal.epnova.R
import com.kuaige.imageloader.GlideImageLoader


/**
 * *****************************************
 * 项目名称: KuaigeAi
 * @Author: SZYMN
 * @Introduce:
 * @Data: 2024/7/11 星期四 15:41
 * @Version: 1.0
 * *****************************************
 */
object ImageColorExtractionUtils {
    private val coloredBackgroundCache = android.util.LruCache<String?, Bitmap?>(20)
    private val colorBackgroundCache = android.util.LruCache<String?, Int?>(20)

    /**
     *
     * @param activity Activity
     * @param coverImageUrl String? 图片地址
     * @param defaultResId Int 默认图片
     * @param width Int
     * @param height Int
     * @param onBitmap Function1<[@kotlin.ParameterName] Bitmap?, Unit>
     */
    fun getColorBackground(
        activity: Activity,
        coverImageUrl: String?,
        defaultResId: Int = R.drawable.ic_play_default_cover,
        width: Int = ScreenUtils.getAppScreenWidth(),
        height: Int = ScreenUtils.getAppScreenHeight(),
        onBitmap: ((bitmap: Bitmap?) -> Unit)
    ) {
        if (coverImageUrl.isNullOrEmpty()) {
            val bitmap = BitmapFactory.decodeResource(activity.resources, defaultResId) ?: return
            getBitmapBackground(
                bitmap,
                width,
                height
            ) { mBitmap ->
                onBitmap.invoke(mBitmap)
            }
            return
        }
        coloredBackgroundCache.get(coverImageUrl)?.let {
            onBitmap.invoke(it)
            return
        }
        GlideApp.with(activity).asBitmap()
            .load(coverImageUrl)
            .placeholder(defaultResId)
            .into(object : CustomTarget<Bitmap>() {
                override fun onResourceReady(
                    resource: Bitmap,
                    transition: Transition<in Bitmap>?
                ) {
                    if (coloredBackgroundCache.get(coverImageUrl) == null) {
                        getBitmapBackground(
                            resource,
                            width,
                            height
                        ) { bitmap ->
                            coloredBackgroundCache.put(coverImageUrl, bitmap)
                            onBitmap.invoke(bitmap)
                        }
                    }
                }

                override fun onLoadCleared(placeholder: Drawable?) {
                    Logger.i("onLoadCleared=$defaultResId")
                    //已清载,使用默认图片
                    val bitmap =
                        BitmapFactory.decodeResource(activity.resources, defaultResId) ?: return
                    getBitmapBackground(
                        bitmap,
                        width,
                        height
                    ) { mBitmap ->
                        onBitmap.invoke(mBitmap)
                    }
                }
            })
    }

    fun setColorBackground(
        coverView: ImageView,
        coverBgView: ImageView,
        coverImageUrl: String?,
        defaultResId: Int = R.drawable.ic_play_default_cover,
        colorBitmapRequestListener: RequestListener<Drawable>? = null
    ) {

        coloredBackgroundCache.get(coverImageUrl)?.let {
            Logger.i("use cache:$coverImageUrl")
            coverBgView.setImageBitmap(it)
        }
        GlideImageLoader.with(coverView).load(coverImageUrl).placeholder(defaultResId)
            .addListener(object : RequestListener<Drawable> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<Drawable>,
                    isFirstResource: Boolean
                ): Boolean {
                    colorBitmapRequestListener?.onLoadFailed(e, model, target, isFirstResource)
                    return false
                }

                override fun onResourceReady(
                    drawable: Drawable,
                    model: Any,
                    target: Target<Drawable>,
                    dataSource: DataSource,
                    isFirstResource: Boolean
                ): Boolean {
                    if (coloredBackgroundCache.get(coverImageUrl) == null) {
                        getBitmapBackground(
                            (drawable as BitmapDrawable).bitmap,
                            ScreenUtils.getAppScreenWidth(),
                            ScreenUtils.getAppScreenHeight()
                        ) {
                            Logger.i("save cache:$coverImageUrl")
                            coloredBackgroundCache.put(coverImageUrl, it)
                            coverBgView.setImageBitmap(it)
                            colorBitmapRequestListener?.onResourceReady(
                                BitmapDrawable(it),
                                model,
                                target,
                                dataSource,
                                isFirstResource
                            )
                        }
                    }
                    return false
                }
            }).into(coverView)
    }


    /**
     * 提取Bitmap中的颜色生成渐变效果的图片
     * @param bitmap Bitmap
     * @param onBitmap Function1<[@kotlin.ParameterName] Bitmap?, Unit>
     */
    fun getBitmapBackground(
        bitmap: Bitmap,
        width: Int,
        height: Int,
        onBitmap: ((bitmap: Bitmap?) -> Unit)
    ) {
        //关于资源就绪
        Palette.from(bitmap).generate(object : Palette.PaletteAsyncListener {
            override fun onGenerated(palette: Palette?) {
                palette ?: return
                // 获取到活跃的明亮的颜色
                val lightVibrantColor = palette.getDarkMutedColor(Color.DKGRAY)
                // 获取图片中一个最柔和的颜色
                val mutedColor = palette.getMutedColor(Color.DKGRAY)

                // 2. 创建一个新的Bitmap用于渐变效果
                val newBitmap = Bitmap.createBitmap(
                    bitmap.width,
                    bitmap.height,
                    Bitmap.Config.ARGB_8888
                )
                val canvas = Canvas(newBitmap)
                val paint = Paint()
                // 3. 使用LinearGradient创建渐变效果
                val gradient = LinearGradient(
                    0f,
                    0f, // 渐变起始点
                    0f,
                    height.toFloat(), // 渐变结束点
                    mutedColor,
                    lightVibrantColor, // 开始和结束颜色
                    Shader.TileMode.CLAMP // 渐变模式
                )
                paint.shader = gradient
                // 4. 绘制渐变效果
                canvas.drawRect(
                    0f,
                    0f,
                    width.toFloat(),
                    height.toFloat(),
                    paint
                )
                onBitmap.invoke(newBitmap)
            }
        })
    }


    /**
     *
     * @param activity Activity
     * @param coverImageUrl String? 图片地址
     * @param defaultResId Int 默认图片
     * @param onColor Function1<[@kotlin.ParameterName] Bitmap?, Unit>
     */
    fun getImgColor(
        activity: Activity,
        coverImageUrl: String?,
        defaultResId: Int = R.drawable.ic_play_default_cover,
        onColor: ((color: Int) -> Unit)
    ) {
        if (coverImageUrl.isNullOrEmpty()) {
            val bitmap = BitmapFactory.decodeResource(activity.resources, defaultResId) ?: return
            getColorBackground(
                bitmap,
            ) { color ->
                onColor.invoke(color)
            }
            return
        }
        colorBackgroundCache.get(coverImageUrl)?.let {
            onColor.invoke(it)
            return
        }
        GlideApp.with(activity).asBitmap()
            .load(coverImageUrl)
            .placeholder(defaultResId)
            .into(object : CustomTarget<Bitmap>() {
                override fun onResourceReady(
                    resource: Bitmap,
                    transition: Transition<in Bitmap>?
                ) {
                    if (colorBackgroundCache.get(coverImageUrl) == null) {
                        getColorBackground(
                            resource,
                        ) { color ->
                            colorBackgroundCache.put(coverImageUrl, color)
                            onColor.invoke(color)
                        }
                    }
                }

                override fun onLoadCleared(placeholder: Drawable?) {
                    Logger.i("onLoadCleared=$defaultResId")
                    //已清载,使用默认图片
                    val bitmap =
                        BitmapFactory.decodeResource(activity.resources, defaultResId) ?: return
                    getColorBackground(
                        bitmap,
                    ) { color ->
                        onColor.invoke(color)
                    }
                }
            })
    }

    /**
     * 提取Bitmap中的颜色生成渐变效果的图片
     * @param bitmap Bitmap
     * @param onColor Function1<[@kotlin.ParameterName] Int?, Unit>
     */
    fun getColorBackground(
        bitmap: Bitmap,
        onColor: ((color: Int) -> Unit)
    ) {
        //关于资源就绪
        Palette.from(bitmap).generate(object : Palette.PaletteAsyncListener {
            override fun onGenerated(palette: Palette?) {
                palette ?: return
                // 获取图片中一个最柔和的颜色
                val mutedColor = palette.getMutedColor(Color.DKGRAY)
                onColor.invoke(mutedColor)
            }
        })
    }
}