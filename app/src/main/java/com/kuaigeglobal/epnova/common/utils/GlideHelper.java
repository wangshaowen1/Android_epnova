package com.kuaigeglobal.epnova.common.utils;

import android.graphics.drawable.Drawable;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.kuaige.imageloader.GlideApp;
import com.kuaige.imageloader.GlideRequest;


public class GlideHelper {


    private GlideHelper() {
    }


    public static void loadFlickrThumb(@NonNull final String photoUrl, String thumbnailUrl, @NonNull final ImageView image) {

        GlideRequest<Drawable> thumbnail = null;

        if (thumbnailUrl != null) {
            thumbnail = GlideApp.with(image.getContext())
                    .load(thumbnailUrl)
                    .diskCacheStrategy(DiskCacheStrategy.ALL);
        }
        GlideRequest<Drawable> glide = GlideApp.with(image.getContext())
                .load(photoUrl)
                .dontAnimate();
        if (thumbnail != null) {
            glide.thumbnail(thumbnail);
        }
        glide.into(image);
    }

    public static void loadFlickrFull(@NonNull final String photoUrl, String thumbnailUrl, @NonNull final ImageView image,
                                      @Nullable final ImageLoadingListener listener) {

        GlideRequest<Drawable> thumbnail = null;

        if (thumbnailUrl != null) {
            thumbnail = GlideApp.with(image.getContext())
                    .load(thumbnailUrl)
//                    .animate(ANIMATOR)
                    .diskCacheStrategy(DiskCacheStrategy.ALL);
        }
        GlideRequest<Drawable> glide = GlideApp.with(image.getContext())
                .load(photoUrl)
//                .dontAnimate()
                .placeholder(image.getDrawable());
        if (thumbnail != null) {
            glide.thumbnail(thumbnail);
        }
        glide.format(DecodeFormat.PREFER_RGB_565);
        glide.diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(new CustomTarget<Drawable>() {
                    @Override
                    public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                        image.setImageDrawable(resource);
                        if(listener!=null){
                            listener.onLoaded();
                        }
                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {
                        if(listener!=null){
                            listener.onFailed();
                        }
                    }
                });
    }

    public interface ImageLoadingListener {
        void onLoaded();

        void onFailed();
    }

}
