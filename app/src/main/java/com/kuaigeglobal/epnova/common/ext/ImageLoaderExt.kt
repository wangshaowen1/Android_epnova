package com.kuaigeglobal.epnova.common.ext

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.view.ViewGroup
import android.widget.ImageView
import androidx.annotation.WorkerThread
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.BitmapImageViewTarget
import com.bumptech.glide.request.transition.Transition
import com.kuaigeglobal.epnova.App
import com.kuaige.imageloader.GlideApp
import com.nhsd.basic.ext.launchNoCrash

/**
 * 获取图片bitmap
 */
fun Context.getImageBitmap(imageUrl: String?, size: Int = 128, callback: (imageBitmap: Bitmap?) -> Unit) {
    val holder = ImageView(this)
    holder.layoutParams = ViewGroup.LayoutParams(size, size)
    GlideApp.with(holder)
        .asBitmap()
        .load(imageUrl)
        .into(object : BitmapImageViewTarget(holder) {
            override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                super.onResourceReady(resource, transition)
                callback.invoke(resource)
            }

            override fun onLoadFailed(errorDrawable: Drawable?) {
                super.onLoadFailed(errorDrawable)
                callback.invoke(null)
            }
        })
}

/**
 * 链接转bitmap
 */
@WorkerThread
fun String?.toBitmap(): Bitmap? {
    if (this.isNullOrEmpty()) return null
    return Glide.with(App.instance)
        .asBitmap()
        .load(this)
        .submit()
        .get()
}


fun String?.preload() {
    ioScope.launchNoCrash { toBitmap() }
}


/**
 * 获取图片bitmap
 */
fun getImageBitmap(imageUrl: String?, size: Int = 128, callback: (imageBitmap: Bitmap?) -> Unit) {
    val holder = ImageView(App.instance)
    holder.layoutParams = ViewGroup.LayoutParams(size, size)
    GlideApp.with(holder)
        .asBitmap()
        .load(imageUrl)
        .into(object : BitmapImageViewTarget(holder) {
            override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                super.onResourceReady(resource, transition)
                callback.invoke(resource)
            }

            override fun onLoadFailed(errorDrawable: Drawable?) {
                super.onLoadFailed(errorDrawable)
                callback.invoke(null)
            }
        })
}