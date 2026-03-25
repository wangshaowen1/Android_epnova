package com.kuaigeglobal.epnova.widget;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.annotation.Nullable;

import com.blankj.utilcode.util.SizeUtils;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.shape.CornerFamily;
import com.google.android.material.shape.ShapeAppearanceModel;
import com.kuaigeglobal.epnova.R;
import com.kuaige.imageloader.GlideImageLoader;

import java.util.List;

public class MultiAvatarView<T> extends FrameLayout {

    private boolean isCircle;//圆或是false圆角
    private final float cornerRadius;
    private int maxCount;//最大是6
    private final float imageSize;
    //isCircle false时可以用
    private final float height;
    private final float width;
    private final boolean stack;//叠加模式
    private final float stackSpacing;//叠加区域宽度
    private final float divideSpacing;//非叠加模式的分隔宽度
    private boolean needShowMore = false;//是否显示...遮罩
    private final float strokeWidth;//边线宽度
    private final int strokeColor;//边线颜色
    private final boolean reverseShow;//true 第一个在最上面 false 最后一个在最上面

    public MultiAvatarView(Context context) {
        this(context, null, 0);
    }

    public MultiAvatarView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MultiAvatarView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.MultiAvatarView);
        maxCount = typedArray.getInt(R.styleable.MultiAvatarView_maxCount, 6);
        isCircle = typedArray.getBoolean(R.styleable.MultiAvatarView_circle, true);
        cornerRadius = typedArray.getDimension(R.styleable.MultiAvatarView_corner_radius, 0f);

        imageSize = typedArray.getDimension(R.styleable.MultiAvatarView_imageSize, SizeUtils.dp2px(30f));
        height = typedArray.getDimension(R.styleable.MultiAvatarView_mImageHeight, SizeUtils.dp2px(30f));
        width = typedArray.getDimension(R.styleable.MultiAvatarView_mImageWidth, SizeUtils.dp2px(30f));
        stack = typedArray.getBoolean(R.styleable.MultiAvatarView_stack, false);
        stackSpacing = typedArray.getDimension(R.styleable.MultiAvatarView_stackSpacing, SizeUtils.dp2px(5f));
        divideSpacing = typedArray.getDimension(R.styleable.MultiAvatarView_divideSpacing, SizeUtils.dp2px(6f));
        strokeWidth = typedArray.getDimension(R.styleable.MultiAvatarView_strokeWidth, 0);
        strokeColor = typedArray.getColor(R.styleable.MultiAvatarView_strokeColor, Color.WHITE);
        needShowMore = typedArray.getBoolean(R.styleable.MultiAvatarView_showMore, false);
        reverseShow = typedArray.getBoolean(R.styleable.MultiAvatarView_reverseShow, false);
        typedArray.recycle();
    }

    public void setCircle(boolean isCircle) {
        this.isCircle = isCircle;
    }

    public void setAvatars(String[] mAvatars) {
        if (getChildCount() > 0) {
            removeAllViews();
        }
        if (mAvatars == null) {
            return;
        }
        setImage(mAvatars);
    }

    private void setImage(String[] mAvatars) {
        if (getChildCount() > 0) {
            removeAllViews();
        }

        int count = Math.min(maxCount, mAvatars.length);
        if (reverseShow) {
            for (int i = count - 1; i >= 0; i--) {
                addImageView(i, count, mAvatars);
            }
        } else {
            for (int i = 0; i < count; i++) {
                addImageView(i, count, mAvatars);
            }
        }

    }

    private void addImageView(int i, int count, String[] mAvatars) {
        if (i == count - 1 && needShowMore && (mAvatars.length > maxCount)) {
            LayoutParams lp = getLp(i);
            FrameLayout frameLayout = new FrameLayout(getContext());
            frameLayout.setLayoutParams(lp);

            LayoutParams imageLp = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            ShapeableImageView iv = getRoundImageView();
            iv.setLayoutParams(imageLp);

            setBoard(iv);
            GlideImageLoader.with(iv).thumbnail().load(mAvatars[i], R.drawable.ic_default_avatar_big).centerCrop().dontAnimate().into(iv);
            frameLayout.addView(iv);

            //添加遮罩
            ImageView ivMask = new ImageView(getContext());
            ivMask.setLayoutParams(imageLp);
            if (isCircle) {
                ivMask.setImageResource(R.drawable.home_tx_more);
            } else {
                ivMask.setImageResource(R.drawable.home_tx_more_round);
            }
            if (strokeWidth > 0) {
                int padding = (int) strokeWidth;
                ivMask.setPadding(padding, padding, padding, padding);
            }
            frameLayout.addView(ivMask);

            addView(frameLayout);
        } else {
            ShapeableImageView iv = getRoundImageView();
            LayoutParams lp = getLp(i);
            setBoard(iv);
            GlideImageLoader.with(iv).thumbnail().load(mAvatars[i], R.drawable.ic_default_avatar_big).centerCrop().dontAnimate().into(iv);
            addView(iv, lp);
        }
    }

    private ShapeableImageView getRoundImageView() {
        ShapeableImageView iv = new ShapeableImageView(getContext());
        ShapeAppearanceModel shapeAppearanceModel;
        if (isCircle) {
            shapeAppearanceModel = ShapeAppearanceModel.builder().setAllCornerSizes(ShapeAppearanceModel.PILL).build();
        } else {
            shapeAppearanceModel = ShapeAppearanceModel.builder().setAllCorners(CornerFamily.ROUNDED, cornerRadius).build();
        }
        iv.setShapeAppearanceModel(shapeAppearanceModel);
        return iv;
    }

    private LayoutParams getLp(int index) {
        LayoutParams lp;
        float imageWidth;
        if (isCircle) {
            lp = new LayoutParams((int) imageSize, (int) imageSize);
            imageWidth = imageSize;
        } else {
            lp = new LayoutParams((int) width, (int) height);
            imageWidth = width;
        }
        if (stack) {
            lp.setMarginStart((int) (imageWidth - stackSpacing) * index);
        } else {
            lp.setMarginStart((int) (imageWidth + divideSpacing) * index);
        }
        return lp;
    }

    private void setBoard(ShapeableImageView iv) {
        if (strokeWidth > 0) {
            int padding = (int) strokeWidth;
            //加白色边框
            iv.setPadding(padding, padding, padding, padding);
            iv.setStrokeColor(ColorStateList.valueOf(strokeColor));
            iv.setStrokeWidth(padding);
        }
    }

    public void setAvatars(List<T> list) {
        setAvatars(list, null);
    }

    public void setAvatars(List<T> list, Convert<T> tConvert) {
        if (list == null) {
            if (getChildCount() > 0) {
                removeAllViews();
            }
            return;
        }

        setConvert(tConvert);
        String[] data = new String[list.size()];
        if (convert == null) {
            for (int i = 0; i < list.size(); i++) {
                if (list.get(i) != null) {
                    data[i] = list.get(i).toString();
                }
            }
        } else {
            for (int i = 0; i < list.size(); i++) {
                data[i] = convert.convert(list.get(i));
            }
        }
        setImage(data);
    }

    public void setMaxCount(int maxCount) {
        this.maxCount = maxCount;
    }

    public void setNeedShowMore(boolean needShowMore) {
        this.needShowMore = needShowMore;
    }

    public interface Convert<T> {
        String convert(T item);
    }

    private Convert<T> convert;

    public void setConvert(Convert<T> convert) {
        this.convert = convert;
    }

}
