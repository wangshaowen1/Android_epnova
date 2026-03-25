package com.kuaigeglobal.epnova.common.utils

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.widget.ImageView
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.kuaige.imageloader.GlideImageLoader

data class BlurBackgroundOptions(
    val coverView: ImageView,
    val coverBgView: ImageView,
    val coverImageUrl: String?,
    val defaultResId: Int,
    val useThumbnail: Boolean,
    val blurRadius: Int,
    val sampling: Int,
    val requestListener: RequestListener<Drawable>?
)

class BlurBackgroundOptionsBuilder(
    private val coverView: ImageView,
    private val coverBgView: ImageView,
    private val coverImageUrl: String?
) {
    private var defaultResId: Int = 0
    private var useThumbnail: Boolean = false
    private var blurRadius: Int = 25 // 默认模糊半径
    private var sampling: Int = 5
    private var requestListener: RequestListener<Drawable>? = null

    fun setDefaultResId(defaultResId: Int) = apply { this.defaultResId = defaultResId }
    fun setUseThumbnail(useThumbnail: Boolean) = apply { this.useThumbnail = useThumbnail }
    fun setBlurRadius(blurRadius: Int) = apply { this.blurRadius = blurRadius }
    fun setBlurSampling(sampling: Int) = apply { this.sampling = sampling }
    fun setRequestListener(requestListener: RequestListener<Drawable>) = apply { this.requestListener = requestListener }

    fun build() = BlurBackgroundOptions(
        coverView = coverView,
        coverBgView = coverBgView,
        coverImageUrl = coverImageUrl,
        defaultResId = defaultResId,
        useThumbnail = useThumbnail,
        blurRadius = blurRadius,
        sampling = sampling,
        requestListener = requestListener
    )
}

fun setBlurBackground(options: BlurBackgroundOptions) {
    val glide = if (options.useThumbnail) GlideImageLoader.with(options.coverView).thumbnail() else GlideImageLoader.with(options.coverView)
    glide.load(options.coverImageUrl)
        .placeholder(options.defaultResId)
        .addListener(object : RequestListener<Drawable> {
            override fun onLoadFailed(
                e: GlideException?,
                model: Any?,
                target: Target<Drawable>,
                isFirstResource: Boolean
            ): Boolean {
                options.requestListener?.onLoadFailed(e, model, target, isFirstResource)
                return false
            }

            override fun onResourceReady(
                resource: Drawable,
                model: Any,
                target: Target<Drawable>?,
                dataSource: DataSource,
                isFirstResource: Boolean
            ): Boolean {
                val originalBitmap = (resource as BitmapDrawable).bitmap
                val blurBitmap: Bitmap = ImageUtils.fastBlur( originalBitmap,  1f/options.sampling,options.blurRadius.toFloat())
                options.coverBgView.setImageBitmap(blurBitmap)
                options.requestListener?.onResourceReady(
                    BitmapDrawable(options.coverBgView.context.resources, blurBitmap),
                    model, target, dataSource, isFirstResource
                )
                return false
            }
        }).into(options.coverView)
}





