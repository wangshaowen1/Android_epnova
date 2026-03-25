package com.kuaigeglobal.epnova.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;


import androidx.annotation.Nullable;
import androidx.core.util.Pair;

import com.blankj.utilcode.util.ScreenUtils;
import com.blankj.utilcode.util.SizeUtils;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.kuaige.imageloader.GlideImageLoader;
import com.nhsd.basic.utils.Logger;
import com.kuaigeglobal.epnova.R;
import com.kuaigeglobal.epnova.common.utils.Utils;
import com.kuaigeglobal.epnova.feature.PhotoPagerActivity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

//
//                            _ooOoo_
//                           o8888888o
//                           88" . "88
//                           (| -_- |)
//                           O\  =  /O
//                        ____/`---'\____
//                      .'  \\|     |//  `.
//                     /  \\|||  :  |||//  \
//                    /  _||||| -:- |||||-  \
//                    |   | \\\  -  /// |   |
//                    | \_|  ''\---/''  |   |
//                    \  .-\__  `-`  ___/-. /
//                  ___`. .'  /--.--\  `. . __
//               ."" '<  `.___\_<|>_/___.'  >'"".
//              | | :  `- \`.;`\ _ /`;.`/ - ` : | |
//              \  \ `-.   \_ __\ /__ _/   .-` /  /
//         ======`-.____`-.___\_____/___.-`____.-'======
//                            `=---='
//        ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
//                      Buddha Bless, No Bug !
//grid
public class GridImageLayout extends ViewGroup {
    private static final int DEFAULT_SINGLE_COLUMN_IMAGE_MAX_SIZE = SizeUtils.dp2px(93);
    private static final int DEFAULT_DOUBLE_COLUMN_MAX_SIZE = SizeUtils.dp2px(93);
    private static final int DEFAULT_MULT_COLUMN_IMAGE_MAX_SIZE = SizeUtils.dp2px(93);
    private static final int DEFAULT_DIVIDER_SIZE = SizeUtils.dp2px(6);
    private static final int SINGLE_IMAGE_MIN_WIDTH = SizeUtils.dp2px(180);
    private static final int SINGLE_IMAGE_MIN_HEIGHT = SizeUtils.dp2px(180);
    private static final int DEFAULT_MAX_ROW_COUNT = 3;
    private static final int DEFAULT_MAX_COLUMN_COUNT = 3;
    private int singleImageMaxWidth = 0;
    private int singleImageMaxHeight = 0;
    private int singleImageMinWidth = SINGLE_IMAGE_MIN_WIDTH;
    private int singleImageMinHeight = SINGLE_IMAGE_MIN_HEIGHT;
    private int singleImageWidth = 0;//0 auto fit
    private int singleImageHeight = 0;//0 auto fit
    private int singleColumnImageSize = DEFAULT_SINGLE_COLUMN_IMAGE_MAX_SIZE;
    private int doubleColumnImageSize = DEFAULT_DOUBLE_COLUMN_MAX_SIZE;
    private int multColumnImageSize = DEFAULT_MULT_COLUMN_IMAGE_MAX_SIZE;
    private List<String> imageUris;
    private int mHorizontalDividerSize = DEFAULT_DIVIDER_SIZE;
    private int mVerticalDividerSize = DEFAULT_DIVIDER_SIZE;
    private int maxColumnCount = DEFAULT_MAX_COLUMN_COUNT;
    private int maxRowCount = DEFAULT_MAX_ROW_COUNT;
    private boolean imageSizeAutoFit;
    private float cornerRadius;
    private int placeHolderRes;

    public GridImageLayout(Context context) {
        super(context);
    }

    public GridImageLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        initWithAttr(context, attrs);
    }

    public GridImageLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initWithAttr(context, attrs);
    }

    private void initWithAttr(Context context, AttributeSet attrs) {

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.GridImageLayout);
        if (a != null) {
            singleImageWidth = (int) a.getDimension(R.styleable.GridImageLayout_singleImageWidth, 0);
            singleImageHeight = (int) a.getDimension(R.styleable.GridImageLayout_singleImageHeight, 0);
            singleImageMaxWidth = (int) a.getDimension(R.styleable.GridImageLayout_singleImageMaxWidth, 0);
            singleImageMaxHeight = (int) a.getDimension(R.styleable.GridImageLayout_singleImageMaxHeight, 0);
            singleColumnImageSize = (int) a.getDimension(R.styleable.GridImageLayout_singleColumnImageSize, DEFAULT_SINGLE_COLUMN_IMAGE_MAX_SIZE);
            doubleColumnImageSize = (int) a.getDimension(R.styleable.GridImageLayout_doubleColumnImageSize, DEFAULT_DOUBLE_COLUMN_MAX_SIZE);
            multColumnImageSize = (int) a.getDimension(R.styleable.GridImageLayout_multColumnImageSize, DEFAULT_MULT_COLUMN_IMAGE_MAX_SIZE);
            mHorizontalDividerSize = (int) a.getDimension(R.styleable.GridImageLayout_horizontalDividerSize, DEFAULT_DIVIDER_SIZE);
            mVerticalDividerSize = (int) a.getDimension(R.styleable.GridImageLayout_verticalDividerSize, DEFAULT_DIVIDER_SIZE);
            maxColumnCount = a.getInteger(R.styleable.GridImageLayout_maxColumnCount, DEFAULT_MAX_COLUMN_COUNT);
            singleImageMinWidth = (int) a.getDimension(R.styleable.GridImageLayout_singleImageMinWidth, SINGLE_IMAGE_MIN_WIDTH);
            singleImageMinHeight = (int) a.getDimension(R.styleable.GridImageLayout_singleImageMinHeight, SINGLE_IMAGE_MIN_HEIGHT);
            maxRowCount = a.getInteger(R.styleable.GridImageLayout_maxRowCount, DEFAULT_MAX_ROW_COUNT);
            imageSizeAutoFit = a.getBoolean(R.styleable.GridImageLayout_imageSizeAutoFit, false);
            placeHolderRes = a.getResourceId(R.styleable.GridImageLayout_placeHolder, R.drawable.img_default_cover_1_1);
//            a.getInteger(R.styleable.GridImageLayout_si)
            cornerRadius = a.getDimension(R.styleable.GridImageLayout_cornerRadius, 0);
            a.recycle();
        }
    }


    public void setImageUrlWithSize(String uri, int imageWidth, int imageHeight) {
        imageUris = new ArrayList<>();
        imageUris.add(uri);
        singleImageWidth = imageWidth;
        singleImageHeight = imageHeight;
        setupViews();
    }

    public void setMaxColumnCount(int maxColumnCount) {
        this.maxColumnCount = maxColumnCount;
        if (!Utils.isEmpty(imageUris)) {
            setupViews();
        }
    }

    public void setImageSizeAutoFit(boolean imageSizeAutoFit) {
        this.imageSizeAutoFit = imageSizeAutoFit;
        requestLayout();
    }

    public void setImageUris(String... imageUris) {
        setImageUris(Arrays.asList(imageUris));
    }

    public void setImageUris(List<String> images) {
        if (!Utils.isEmpty(images)) {
            this.imageUris = images;
            setupViews();
        }
    }

    private void setupViews() {
        int childCount = Math.min(maxColumnCount * maxRowCount, imageUris.size());
        if (childCount > 0) {
            if (getChildCount() == childCount) {
                if (childCount == 1) {
                    ColorFilterImageView child = (ColorFilterImageView) getChildAt(0);
                    if (imageSizeAutoFit) {
                        setExpectLayoutParams(child, 0, 0);
                    } else {
                        setExpectLayoutParams(child, singleImageWidth, singleImageHeight);
                    }
                    setImageUrl(child, imageUris.get(0));

                } else {
                    for (int i = 0; i < childCount; i++) {
                        ColorFilterImageView imageView = (ColorFilterImageView) getChildAt(i);
                        if (imageSizeAutoFit) {
                            imageView.getLayoutParams().width = LayoutParams.MATCH_PARENT;
                            imageView.getLayoutParams().height = LayoutParams.MATCH_PARENT;
                        } else {
                            int imageSize;
                            if (maxColumnCount == 1) {
                                imageSize = singleColumnImageSize;
                            } else if (maxColumnCount == 2 || childCount == 2) {
                                imageSize = doubleColumnImageSize;
                            } else {
                                imageSize = multColumnImageSize;
                            }
                            imageView.getLayoutParams().width = imageSize;
                            imageView.getLayoutParams().height = imageSize;

                        }
                        setImageUrl(imageView, imageUris.get(i));
                    }
                }
            } else {
                if (getChildCount() > 0) {
                    removeAllViews();
                }
                for (int i = 0; i < childCount; i++) {
                    ImageView imageView = createImageView(i);
                    setImageUrl(imageView, imageUris.get(i));
                    addView(imageView);
                }
            }
            requestLayout();
        }
    }

    private void setImageUrl(ImageView child, String url) {
        RequestBuilder<Drawable> request = GlideImageLoader.with(child).load(url).placeholder(placeHolderRes);
        if (child.getLayoutParams().width == LayoutParams.WRAP_CONTENT && child.getLayoutParams().height == MultRadioGroup.LayoutParams.WRAP_CONTENT) {
            request = request.listener(new RequestListener<Drawable>() {
                @Override
                public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                    return false;
                }

                @Override
                public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                    setExpectLayoutParams(child, resource.getIntrinsicWidth(), resource.getIntrinsicHeight());
                    return false;
                }
            });
            request.override(getOverrideSize(), getOverrideSize()).into(child);
        } else {
            request.into(child);

        }

    }

    private int getOverrideSize() {
        return ScreenUtils.getScreenHeight();
    }


    private ImageView createImageView(int position) {
        ColorFilterImageView imageView = new ColorFilterImageView(getContext());
        if (imageUris.size() == 1) {
            imageView.setAdjustViewBounds(true);
            if (imageSizeAutoFit) {
                setExpectLayoutParams(imageView, 0, 0);
            } else {
                setExpectLayoutParams(imageView, singleImageWidth, singleImageHeight);
            }
        } else {
            LayoutParams params = null;
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            if (imageSizeAutoFit) {
                params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
            } else {
                int imageSize;
                if (maxColumnCount == 1) {
                    imageSize = singleColumnImageSize;
                } else if (maxColumnCount == 2 || imageUris.size() == 2) {
                    imageSize = doubleColumnImageSize;
                } else {
                    imageSize = multColumnImageSize;
                }
                params = new LayoutParams(imageSize, imageSize);
            }
            imageView.setLayoutParams(params);
        }
        imageView.setId(10000 + position);
        imageView.setOnClickListener(v -> {
            if (onItemClickListener != null) {
                onItemClickListener.onItemClick(v, position);
            }
        });
        if (cornerRadius != 0) {
            imageView.setCornerRadius(cornerRadius);
        }

        return imageView;
    }

    public interface OnItemClickListener {
        void onItemClick(View v, int position);
    }

    private OnItemClickListener onItemClickListener = new OnItemClickListener() {
        @Override
        public void onItemClick(View v, int position) {
            performDefaultItemClick(v, position);
        }
    };

    public void performDefaultItemClick(View v, int position) {
        ArrayList<String> photoUris = new ArrayList<>();
        photoUris.addAll(imageUris);
        PhotoPagerActivity.launch(getContext(), getChildViews(), photoUris, new Pair<>(v, position));
    }

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }

    public List<View> getChildViews() {
        List<View> views = new ArrayList<>();
        for (int i = 0; i < getChildCount(); i++) {
            views.add(getChildAt(i));
        }
        return views;
    }

    private void setExpectLayoutParams(ImageView imageView, int expectW, int expectH) {
//        Logger.i("GridImageView", "expectW:" + expectH + ", exH:" + expectH + ", maxW:" + singleImageMaxHeight + ",maxW:" + singleImageMaxWidth);
        if (expectW == 0 || expectH == 0) {
            LayoutParams params = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
            imageView.setLayoutParams(params);
            if (singleImageMaxWidth > 0) {
                imageView.setMaxWidth(singleImageMaxWidth);
            }
            if (singleImageMaxHeight > 0) {
                imageView.setMaxHeight(singleImageMaxHeight);
            }
            imageView.setMinimumWidth(singleImageMinWidth);
            imageView.setMinimumHeight(singleImageMinHeight);
            imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
            imageView.setAdjustViewBounds(true);
        } else {
            if (singleImageMaxWidth > 0 || singleImageMaxHeight > 0) {
                imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
                int actualW;
                int actualH;
                float scale = ((float) expectH) / ((float) expectW);
                float maxHWScale = (float) singleImageMaxHeight / singleImageMaxWidth;
                if (expectW > singleImageMaxWidth || expectH > singleImageMaxHeight) {
                    if (scale > maxHWScale) {
                        actualH = singleImageMaxHeight;
                        actualW = (int) (actualH / scale);
                        //等比缩放不可以小于最小尺寸
                        if (actualW < singleImageMinWidth) {
                            actualW = singleImageMinWidth;
                            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                        }

                    } else {
                        actualW = singleImageMaxWidth;
                        actualH = (int) (actualW * scale);
                        //等比缩放不可以小于最小尺寸
                        if (actualH < singleImageMinHeight) {
                            actualH = singleImageMinHeight;
                            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                        }
                    }
                } else if (expectW < multColumnImageSize || expectH < multColumnImageSize) {
                    if (scale > maxHWScale) {
                        actualW = singleImageMinWidth;
                        actualH = (int) (actualW * scale);
                        //等比缩放不可以大于最大尺寸
                        if (actualH > singleImageMaxHeight) {
                            actualH = singleImageMaxHeight;
                            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                        }
                    } else {
                        actualH = singleImageMinHeight;
                        actualW = (int) (actualH / scale);
                        //等比缩放不可以大于最大尺寸
                        if (actualW > singleImageMaxWidth) {
                            actualW = singleImageMaxWidth;
                            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                        }
                    }
                } else {
                    actualW = expectW;
                    actualH = expectH;
                }
                imageView.setLayoutParams(new LayoutParams(actualW, actualH));
            } else {
                imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                imageView.setLayoutParams(new LayoutParams(expectW, expectH));
            }
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int childCount = getChildCount();
        int childState = 0;
        int totalDividerWidth = 0;
        int totalDividerHeight = 0;
        int totalChildWidth = 0;
        int totalChildHeight = 0;
        int maxWidth;
        int maxHeight;
        if (childCount > 0) {
            if (childCount <= maxColumnCount) {//列
                totalDividerWidth = mHorizontalDividerSize * (childCount - 1);
                totalDividerHeight = 0;
            } else {
                int actualRowCount = Math.min((childCount + maxColumnCount - 1) / maxColumnCount, maxColumnCount);
                totalDividerWidth = mHorizontalDividerSize * (maxRowCount - 1);
                totalDividerHeight = mVerticalDividerSize * (actualRowCount - 1);
            }

            if (imageSizeAutoFit) {
                int maxAvailableWidth = MeasureSpec.getSize(widthMeasureSpec) - totalDividerWidth;
                int childWidth = maxAvailableWidth / Math.min(childCount, maxColumnCount);
                //Logger.i("GridImageView", "auto fit with:" + childWidth);
                for (int i = 0; i < childCount; i++) {
                    final View child = getChildAt(i);
                    measureChild(child, MeasureSpec.makeMeasureSpec(childWidth, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(childWidth, MeasureSpec.EXACTLY));
                }
            } else {
                for (int i = 0; i < childCount; i++) {
                    final View child = getChildAt(i);
                    measureChild(child, widthMeasureSpec, heightMeasureSpec);
                }

            }
            int childWidth = childCount == 0 ? 0 : getChildAt(0).getMeasuredWidth();
            int childHeight = childCount == 0 ? 0 : getChildAt(0).getMeasuredHeight();
            if (childCount <= maxColumnCount) {
                totalChildWidth = childWidth * childCount;
                totalChildHeight = childHeight;
            } else {
                int actualRowCount = Math.min((childCount + maxColumnCount - 1) / maxColumnCount, maxColumnCount);
                totalChildWidth = childWidth * maxColumnCount;
                totalChildHeight = childHeight * actualRowCount;
            }
            maxWidth = totalDividerWidth + totalChildWidth;
            maxHeight = totalDividerHeight + totalChildHeight;
        } else {
            maxHeight = 0;
            maxWidth = 0;
        }
        // Account for padding too
        maxWidth += getPaddingLeft() + getPaddingRight();
        maxHeight += getPaddingTop() + getPaddingBottom();
        // Check against our minimum height and width
        maxHeight = Math.max(maxHeight, getSuggestedMinimumHeight());
        maxWidth = Math.max(maxWidth, getSuggestedMinimumWidth());

        // Check against our foreground's minimum height and width
        final Drawable drawable = getBackground();
        if (drawable != null) {
            maxHeight = Math.max(maxHeight, drawable.getMinimumHeight());
            maxWidth = Math.max(maxWidth, drawable.getMinimumWidth());
        }
        setMeasuredDimension(resolveSizeAndState(maxWidth, widthMeasureSpec, childState),
                resolveSizeAndState(maxHeight, heightMeasureSpec,
                        childState << MEASURED_HEIGHT_STATE_SHIFT));
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int count = getChildCount();
        if (count > 0) {
            int left = getPaddingLeft();
            int top = getPaddingTop();

            if (count <= maxColumnCount) {
                for (int i = 0; i < count; i++) {
                    final View child = getChildAt(i);
                    final int width = child.getMeasuredWidth();
                    final int height = child.getMeasuredHeight();
                    int childLeft = left + i * (width + mHorizontalDividerSize);
                    int childTop = top;
                    Logger.i("GridImageView", "childLeft:" + childLeft + ", width:" + width + ",ct:" + childTop + ",mw:" + getMeasuredWidth());
                    child.layout(childLeft, childTop, childLeft + width, childTop + height);
                }
            } else {
                for (int i = 0; i < count; i++) {
                    final View child = getChildAt(i);
                    final int width = child.getMeasuredWidth();
                    final int height = child.getMeasuredHeight();
                    int childLeft = left + (i % maxColumnCount) * (width + mHorizontalDividerSize);
                    int childTop = top + (i / maxColumnCount) * (height + mVerticalDividerSize);
                    child.layout(childLeft, childTop, childLeft + width, childTop + height);
                }

            }
        }
    }

}
