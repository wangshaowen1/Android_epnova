package com.kuaigeglobal.epnova.common.ext

import android.graphics.drawable.AnimatedImageDrawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.widget.ImageView
import androidx.annotation.DrawableRes
import androidx.core.graphics.drawable.toDrawable
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.request.transition.Transition
import com.kuaige.imageloader.GlideApp
import com.kuaige.imageloader.GlideImageLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

// ImageView加载扩展
fun ImageView.load(urlOrPath: String?, @DrawableRes placeHolder: Int? = null) {
    val load = GlideImageLoader.with(this).load(urlOrPath)
    placeHolder?.let { load.placeholder(placeHolder) }
    load.into(this)
}

fun ImageView.loadThumbnail(urlOrPath: String?, @DrawableRes placeHolder: Int? = null) {
    GlideImageLoader.with(this).thumbnail()
        .display(urlOrPath, placeHolder ?: 0)
}


fun ImageView.loadAnimatedWebp(
    url: String?,
    placeHolder: Drawable? = null,
    loadSuccess: (() -> Unit)? = null
) {
    if (url?.endsWith(".webp") == true && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        if (placeHolder != null) {
            this.setImageDrawable(placeHolder)
        }
        GlideApp.with(this)
            .asFile()
            .load(url)
            .into(object : CustomTarget<File>() {
                override fun onResourceReady(resource: File, transition: Transition<in File>?) {
                    // 创建 AnimatedImageDrawable
                    ioScope.launch {
                        val drawable = withContext(Dispatchers.IO) {
                            AnimatedImageDrawable.createFromPath(resource.absolutePath)
                        }
                        withContext(Dispatchers.Main) {
                            // 设置到 ImageView 并启动动画
                            setImageDrawable(drawable)
                            if (drawable is AnimatedImageDrawable) {
                                drawable.start()
                            }
                            loadSuccess?.invoke()
                        }
                    }
                }

                override fun onLoadCleared(placeholder: Drawable?) {
                    // 清理资源
                }
            })
    } else {
        GlideImageLoader.with(this).load(url).listener(object : RequestListener<Drawable> {
            override fun onLoadFailed(
                e: GlideException?,
                model: Any?,
                target: Target<Drawable>,
                isFirstResource: Boolean
            ): Boolean {
                return false
            }

            override fun onResourceReady(
                resource: Drawable,
                model: Any,
                target: Target<Drawable>?,
                dataSource: DataSource,
                isFirstResource: Boolean
            ): Boolean {
                loadSuccess?.invoke()
                return false
            }
        }).into(this)
    }
}
fun ImageView.getBitmapDrawableCopyOrNull(): BitmapDrawable? {
    val bitmap = (drawable as? BitmapDrawable)?.bitmap ?: return null
    return try {
        val bitmapCopy = bitmap.config?.let { bitmap.copy(it, true) }
        bitmapCopy?.toDrawable(resources)?.apply {
            bounds = drawable.bounds
        }
    } catch (_: Exception) {
        null
    }
}