package com.kuaigeglobal.epnova.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;

import androidx.appcompat.widget.AppCompatImageView;

import android.util.AttributeSet;

import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.request.RequestListener;
import com.kuaigeglobal.epnova.R;
import com.kuaige.imageloader.GlideApp;
import com.kuaige.imageloader.GlideImageLoader;
import com.kuaige.imageloader.transform.CropCircleTransformation;

/**
 * @author lu
 */
public class RemoteImageView extends AppCompatImageView {

    private static final int NONE = 0;
    /**
     * 网络图片url
     */
    private Transformation<Bitmap> bitmapTransformation;
    private static final int CROP_CIRCLE = 1;
    private int placeHolder = NONE;
    private GlideImageLoader imageLoader;

    public RemoteImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs);
    }


    public RemoteImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public RemoteImageView(Context context) {
        super(context);
        init(context,null);
    }

    private void init(Context context, AttributeSet attrs) {
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.RemoteImageView);
        if (a != null) {
            if (a.hasValue(R.styleable.RemoteImageView_transformation)) {
                int transFormation = a.getInt(R.styleable.RemoteImageView_transformation, 0);
                switch (transFormation) {
                    case CROP_CIRCLE:
                        bitmapTransformation = new CropCircleTransformation(context);
                        break;
                    default:
                        break;
                }
                placeHolder = a.getResourceId(R.styleable.RemoteImageView_placeholder, NONE);
            }
            a.recycle();
        }
        imageLoader = GlideImageLoader.with(this);
    }



    public void setImageRes(final int imageRes) {
        imageLoader.display(imageRes, placeHolder, bitmapTransformation);
    }
    public void setImageUri(final String url) {
        imageLoader.display(url, placeHolder, bitmapTransformation);
    }

    public void setImageUri(int defResId, final String url) {
        imageLoader.display(url, defResId, bitmapTransformation);
    }

    public void setImageUri(int defResId, final String url, RequestListener<Drawable> listener) {
        imageLoader.load(url, defResId, bitmapTransformation).listener(listener).into(this);
    }

    public void setThumbUri(final String url) {
        imageLoader.thumbnail().display(url, placeHolder, bitmapTransformation);
    }

    public void setThumbUri(int defResId, final String url) {
        imageLoader.thumbnail().display(url, defResId, bitmapTransformation);
    }

    public void setHQThumbUri(final String url) {
        imageLoader.hdThumbnail().display(url, placeHolder, bitmapTransformation);
    }

    public void setHQThumbUri(int defResId, final String url) {
        imageLoader.hdThumbnail().display(url, defResId, bitmapTransformation);
    }

    public void setThumbUri(int defResId, final String url, RequestListener<Drawable> listener) {
        imageLoader.thumbnail().load(url, defResId, bitmapTransformation).listener(listener).into(this);
    }

    public void setHQThumbUri(int defResId, final String url, RequestListener<Drawable> listener) {
        imageLoader.hdThumbnail().load(url, defResId, bitmapTransformation).listener(listener).into(this);
    }

    public void setBitmapTransformation(Transformation<Bitmap> tf) {
        this.bitmapTransformation = tf;
    }

    public void clear() {
        GlideApp.with(getContext()).clear(this);
    }
}
