package com.kuaigeglobal.epnova.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Pair;

import com.blankj.utilcode.util.ScreenUtils;
import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.kuaigeglobal.epnova.R;
import com.kuaigeglobal.epnova.feature.PhotoPagerActivity;
import com.kuaige.imageloader.GlideImageLoader;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 少量图片的只读网格，所有数据和配置接口在主线程调用。
 * 默认保留单图原有尺寸/裁剪规则、多图固定方格；自适应模式需要显式配置。
 * 行列限制只影响缩略图数量，预览保留全部图片。
 * 子项由控件管理，不支持外部增删子 View 或单独修改子项尺寸/显隐。
 */
public class GridImageLayout extends ViewGroup {
    public static final int SIZE_ORIGINAL = 0;
    public static final int SIZE_AUTO_WIDTH = 1;
    public static final int SIZE_FIT_INSIDE = 2;
    private final ArrayList<String> images = new ArrayList<>();
    private final ArrayList<ImageCell> cells = new ArrayList<>();
    private int columnCount;
    private int maxRowCount;
    private int cellSize;
    private int singleColumnSize;
    private int doubleColumnSize;
    private int multiColumnSize;
    private int sizeMode;
    private int horizontalSpacing;
    private int verticalSpacing;
    private int singleMaxWidth;
    private int singleMaxHeight;
    private int singleWidth;
    private int singleHeight;
    private int singleMinWidth;
    private int singleMinHeight;
    private int imageWidth;
    private int imageHeight;
    private final int maxDecodeEdge;
    private boolean mirrorInRtl;
    private final int fallbackCellSize;
    private final int fallbackSingleSize;
    private final int placeholderRes;
    private final float cornerRadius;
    private Geometry geometry = Geometry.EMPTY;
    private OnImageClickListener onImageClickListener;
    private boolean previewEnabled = true;
    private boolean defaultClickEnabled = true;

    public GridImageLayout(Context context) {
        this(context, null);
    }

    public GridImageLayout(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public GridImageLayout(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        fallbackCellSize = dp(93);
        fallbackSingleSize = dp(180);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.GridImageLayout, defStyleAttr, 0);
        try {
            columnCount = Math.max(1, a.getInt(R.styleable.GridImageLayout_gridImageColumnCount, 3));
            maxRowCount = Math.max(0, a.getInt(R.styleable.GridImageLayout_gridImageMaxRowCount, 3));
            cellSize = dimension(a, R.styleable.GridImageLayout_gridImageCellSize, fallbackCellSize);
            singleColumnSize = optionalDimension(a, R.styleable.GridImageLayout_gridImageSingleColumnSize);
            doubleColumnSize = optionalDimension(a, R.styleable.GridImageLayout_gridImageDoubleColumnSize);
            multiColumnSize = optionalDimension(a, R.styleable.GridImageLayout_gridImageMultiColumnSize);
            sizeMode = normalizeSizeMode(a.getInt(R.styleable.GridImageLayout_gridImageSizeMode, SIZE_ORIGINAL));
            horizontalSpacing = dimension(a, R.styleable.GridImageLayout_gridImageHorizontalSpacing, dp(6));
            verticalSpacing = dimension(a, R.styleable.GridImageLayout_gridImageVerticalSpacing, dp(6));
            singleMaxWidth = dimension(a, R.styleable.GridImageLayout_gridImageSingleMaxWidth, 0);
            singleMaxHeight = dimension(a, R.styleable.GridImageLayout_gridImageSingleMaxHeight, 0);
            singleWidth = dimension(a, R.styleable.GridImageLayout_gridImageSingleWidth, 0);
            singleHeight = dimension(a, R.styleable.GridImageLayout_gridImageSingleHeight, 0);
            singleMinWidth = dimension(a, R.styleable.GridImageLayout_gridImageSingleMinWidth, fallbackSingleSize);
            singleMinHeight = dimension(a, R.styleable.GridImageLayout_gridImageSingleMinHeight, fallbackSingleSize);
            maxDecodeEdge = Math.max(0, a.getInt(R.styleable.GridImageLayout_gridImageMaxDecodeEdge, 0));
            mirrorInRtl = a.getBoolean(R.styleable.GridImageLayout_gridImageMirrorInRtl, false);
            previewEnabled = a.getBoolean(R.styleable.GridImageLayout_gridImagePreviewEnabled, true);
            placeholderRes = a.getResourceId(R.styleable.GridImageLayout_gridImagePlaceholder, R.drawable.img_default_cover_1_1);
            cornerRadius = Math.max(0, a.getDimension(R.styleable.GridImageLayout_gridImageCornerRadius, 0));
        } finally {
            a.recycle();
        }
    }

    private int dp(int value) {
        return Math.max(1, Math.round(value * getResources().getDisplayMetrics().density));
    }

    private static int dimension(TypedArray a, int index, int defaultValue) {
        return Geometry.size(a.getDimensionPixelOffset(index, defaultValue));
    }

    private static int optionalDimension(TypedArray a, int index) {
        return a.hasValue(index) ? dimension(a, index, 0) : -1;
    }

    private static int normalizeSizeMode(int mode) {
        return mode == SIZE_AUTO_WIDTH || mode == SIZE_FIT_INSIDE ? mode : SIZE_ORIGINAL;
    }

    /** 完整替换；null/空列表清空，过滤 null 和空白地址，保留顺序及重复图片。 */
    public void setImages(@Nullable List<String> urls) {
        replaceImages(urls, 0, 0);
    }

    /** 提供原图尺寸供原有尺寸规则计算；显式固定尺寸优先。非正宽高由加载结果推断。 */
    public void setImage(@Nullable String url, int sourceWidth, int sourceHeight) {
        boolean known = sourceWidth > 0 && sourceHeight > 0;
        replaceImages(Collections.singletonList(url), known ? sourceWidth : 0, known ? sourceHeight : 0);
    }

    public void clearImages() {
        setImages(null);
    }

    @NonNull
    public ArrayList<String> getImages() {
        return new ArrayList<>(images);
    }

    public int getImageCount() {
        return images.size();
    }

    private void replaceImages(@Nullable List<String> urls, int sourceWidth, int sourceHeight) {
        ArrayList<String> next = new ArrayList<>();
        if (urls != null) {
            for (String url : urls) {
                if (url != null && !url.trim().isEmpty()) next.add(url);
            }
        }
        if (next.size() != 1) { sourceWidth = 0; sourceHeight = 0; }
        boolean sameImages = images.equals(next);
        if (sameImages && imageWidth == sourceWidth && imageHeight == sourceHeight) {
            retryFailedImages();
            return;
        }
        imageWidth = sourceWidth;
        imageHeight = sourceHeight;
        if (!sameImages) {
            images.clear();
            images.addAll(next);
            reconcileCells();
        } else {
            requestLayout();
        }
    }

    public int getColumnCount() {
        return columnCount;
    }

    /** 小于 1 按 1 列处理。 */
    public void setColumnCount(int count) {
        count = Math.max(1, count);
        if (columnCount == count) return;
        columnCount = count;
        reconcileCells();
    }

    public int getMaxRowCount() {
        return maxRowCount;
    }

    /** 0 隐藏所有缩略图但保留数据，增加上限后重新展示。 */
    public void setMaxRowCount(int count) {
        count = Math.max(0, count);
        if (maxRowCount == count) return;
        maxRowCount = count;
        reconcileCells();
    }

    public int getCellSize() {
        return cellSize;
    }

    /** 多图公共边长，单位 px；各列数未单独配置时使用。不会隐式切换尺寸模式。 */
    public void setCellSize(int sizePx) {
        sizePx = Geometry.size(sizePx);
        if (cellSize == sizePx) return;
        cellSize = sizePx;
        requestLayout();
    }

    /** 分别设置一列、两列、多列的边长，负值表示使用公共尺寸。 */
    public void setColumnImageSizes(int singlePx, int doublePx, int multiPx) {
        singleColumnSize = singlePx < 0 ? -1 : Geometry.size(singlePx);
        doubleColumnSize = doublePx < 0 ? -1 : Geometry.size(doublePx);
        multiColumnSize = multiPx < 0 ? -1 : Geometry.size(multiPx);
        requestLayout();
    }

    private int configuredCellSize() {
        int override = columnCount == 1 ? singleColumnSize
                : columnCount == 2 || cells.size() == 2 ? doubleColumnSize : multiColumnSize;
        return override >= 0 ? override : cellSize;
    }

    public int getSizeMode() {
        return sizeMode;
    }

    /** original 保留原规则；autoWidth 均分宽度；fitInside 保留配置上限并按父容器收缩。 */
    public void setSizeMode(int mode) {
        mode = normalizeSizeMode(mode);
        if (sizeMode == mode) return;
        sizeMode = mode;
        requestLayout();
    }

    public void setMirrorInRtl(boolean enabled) {
        mirrorInRtl = enabled;
        requestLayout();
    }

    /** 间距单位 px；original/autoWidth 保持配置，仅 fitInside 允许收缩。 */
    public void setSpacing(int horizontalPx, int verticalPx) {
        horizontalPx = Geometry.size(horizontalPx);
        verticalPx = Geometry.size(verticalPx);
        if (horizontalSpacing == horizontalPx && verticalSpacing == verticalPx) return;
        horizontalSpacing = horizontalPx;
        verticalSpacing = verticalPx;
        requestLayout();
    }

    /** 单图固定尺寸，单位 px，两边均大于 0 时生效；original 沿用旧规则，fitInside 作为上限，autoWidth 忽略。 */
    public void setSingleImageSize(int widthPx, int heightPx) {
        singleWidth = Geometry.size(widthPx);
        singleHeight = Geometry.size(heightPx);
        requestLayout();
    }

    /** 保留旧规则中的占位/缩放最小尺寸；与最大尺寸冲突时最大尺寸优先。 */
    public void setSingleImageMinSize(int widthPx, int heightPx) {
        singleMinWidth = Geometry.size(widthPx);
        singleMinHeight = Geometry.size(heightPx);
        requestLayout();
    }

    /** 单图最大尺寸，单位 px；0 表示不额外设置该边上限。 */
    public void setSingleImageMaxSize(int widthPx, int heightPx) {
        widthPx = Geometry.size(widthPx);
        heightPx = Geometry.size(heightPx);
        if (singleMaxWidth == widthPx && singleMaxHeight == heightPx) return;
        singleMaxWidth = widthPx;
        singleMaxHeight = heightPx;
        requestLayout();
    }

    public interface OnImageClickListener {
        void onImageClick(View view, int position, @NonNull List<String> images);
    }

    /** 自定义回调优先；沿用旧行为，null 关闭点击。回调收到独立数据快照。 */
    public void setOnImageClickListener(@Nullable OnImageClickListener listener) {
        onImageClickListener = listener;
        defaultClickEnabled = false;
        updateClickability();
    }

    public void resetOnImageClickListener() {
        onImageClickListener = null;
        defaultClickEnabled = true;
        updateClickability();
    }

    /** 可从自定义回调主动打开默认预览；不受默认点击开关影响。 */
    public void previewImage(int position) {
        if (position < 0 || position >= images.size()) return;
        View source = position < cells.size() ? cells.get(position) : null;
        previewImage(source, position);
    }

    /** 允许调用方指定预览转场来源；null 表示无来源 View。 */
    public void previewImage(@Nullable View source, int position) {
        if (position < 0 || position >= images.size()) return;
        PhotoPagerActivity.launch(getContext(), getChildViews(), getImages(), new Pair<>(source, position));
    }

    public List<View> getChildViews() {
        return new ArrayList<View>(cells);
    }

    /** 控制默认预览，自定义回调不受影响。 */
    public void setPreviewEnabled(boolean enabled) {
        previewEnabled = enabled;
        updateClickability();
    }

    private void updateClickability() {
        for (ImageCell cell : cells) {
            cell.setClickable(onImageClickListener != null || (defaultClickEnabled && previewEnabled));
            cell.setFocusable(cell.isClickable());
        }
    }

    private void reconcileCells() {
        int visibleCount = (int) Math.min(images.size(), (long) columnCount * maxRowCount);
        Map<String, ArrayDeque<ImageCell>> reusable = new HashMap<>();
        for (ImageCell cell : cells) {
            ArrayDeque<ImageCell> occurrences = reusable.get(cell.url);
            if (occurrences == null) {
                occurrences = new ArrayDeque<>();
                reusable.put(cell.url, occurrences);
            }
            occurrences.add(cell);
        }
        ArrayList<ImageCell> next = new ArrayList<>(visibleCount);
        for (int i = 0; i < visibleCount; i++) {
            String url = images.get(i);
            ArrayDeque<ImageCell> occurrences = reusable.get(url);
            next.add(occurrences == null || occurrences.isEmpty() ? new ImageCell(url) : occurrences.removeFirst());
        }
        cells.clear();
        cells.addAll(next);
        for (ArrayDeque<ImageCell> unused : reusable.values()) {
            for (ImageCell cell : unused) {
                cell.release();
                removeView(cell);
            }
        }
        for (int i = 0; i < cells.size(); i++) {
            ImageCell cell = cells.get(i);
            if (getChildAt(i) != cell) {
                if (indexOfChild(cell) >= 0) removeView(cell);
                addView(cell, i, new LayoutParams(0, 0));
            }
            cell.setContentDescription(getResources().getString(R.string.grid_image_description, i + 1, images.size()));
        }
        updateClickability();
        requestLayout();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int paddingWidth = Geometry.size((long) getPaddingLeft() + getPaddingRight());
        int paddingHeight = Geometry.size((long) getPaddingTop() + getPaddingBottom());
        int availableWidth = availableSize(widthMeasureSpec, paddingWidth);
        int availableHeight = availableSize(heightMeasureSpec, paddingHeight);
        if (cells.isEmpty()) {
            geometry = Geometry.EMPTY;
        } else if (sizeMode == SIZE_AUTO_WIDTH) {
            geometry = Geometry.autoWidthGrid(cells.size(), columnCount, horizontalSpacing,
                    verticalSpacing, availableWidth, configuredCellSize());
        } else if (images.size() == 1) {
            int expectedWidth = hasFixedSingleSize() ? singleWidth : imageWidth > 0 ? imageWidth : cells.get(0).sourceWidth;
            int expectedHeight = hasFixedSingleSize() ? singleHeight : imageHeight > 0 ? imageHeight : cells.get(0).sourceHeight;
            if (sizeMode == SIZE_FIT_INSIDE) {
                double ratio = expectedWidth > 0 && expectedHeight > 0 ? (double) expectedWidth / expectedHeight : 1;
                int maxWidth = singleMaxWidth;
                int maxHeight = singleMaxHeight;
                if (hasFixedSingleSize()) {
                    maxWidth = maxWidth > 0 ? Math.min(singleWidth, maxWidth) : singleWidth;
                    maxHeight = maxHeight > 0 ? Math.min(singleHeight, maxHeight) : singleHeight;
                }
                geometry = Geometry.fitInsideSingle(ratio, maxWidth, maxHeight,
                        availableWidth, availableHeight, fallbackSingleSize);
            } else if (expectedWidth <= 0 || expectedHeight <= 0) {
                geometry = measureSinglePlaceholder(widthMeasureSpec, heightMeasureSpec, paddingWidth, paddingHeight);
            } else {
                geometry = Geometry.originalSingle(expectedWidth, expectedHeight, singleMinWidth, singleMinHeight,
                        singleMaxWidth, singleMaxHeight, multiColumnSize >= 0 ? multiColumnSize : cellSize);
            }
        } else if (sizeMode == SIZE_FIT_INSIDE) {
            geometry = Geometry.fitInsideGrid(cells.size(), columnCount, configuredCellSize(),
                    horizontalSpacing, verticalSpacing, availableWidth, availableHeight);
        } else {
            geometry = Geometry.fixedGrid(cells.size(), columnCount, configuredCellSize(), horizontalSpacing, verticalSpacing);
        }
        for (ImageCell cell : cells) {
            boolean crop = images.size() > 1 || sizeMode == SIZE_AUTO_WIDTH || geometry.crop;
            cell.setScaleType(crop ? ImageView.ScaleType.CENTER_CROP : ImageView.ScaleType.FIT_CENTER);
            cell.measure(MeasureSpec.makeMeasureSpec(geometry.cellWidth, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(geometry.cellHeight, MeasureSpec.EXACTLY));
        }
        int width = Math.max(getSuggestedMinimumWidth(), Geometry.size((long) geometry.width + paddingWidth));
        int height = Math.max(getSuggestedMinimumHeight(), Geometry.size((long) geometry.height + paddingHeight));
        setMeasuredDimension(resolveSizeAndState(width, widthMeasureSpec, 0), resolveSizeAndState(height, heightMeasureSpec, 0));
    }

    private boolean hasFixedSingleSize() {
        return singleWidth > 0 && singleHeight > 0;
    }

    /** 未知尺寸时沿用旧 ImageView 的 WRAP_CONTENT 测量，保留占位图比例、最小值和父约束。 */
    private Geometry measureSinglePlaceholder(int widthSpec, int heightSpec, int paddingWidth, int paddingHeight) {
        ImageCell cell = cells.get(0);
        if (cell.requestedStrategy != ImageCell.REQUEST_NATURAL) cell.setImageResource(placeholderRes);
        cell.setScaleType(ImageView.ScaleType.FIT_CENTER);
        int maxWidth = singleMaxWidth > 0 ? singleMaxWidth : Integer.MAX_VALUE;
        int maxHeight = singleMaxHeight > 0 ? singleMaxHeight : Integer.MAX_VALUE;
        int minWidth = Math.min(singleMinWidth, maxWidth);
        int minHeight = Math.min(singleMinHeight, maxHeight);
        // 这些 setter 会 requestLayout，配置未变时避免在每轮测量中重复请求。
        if (!cell.getAdjustViewBounds()) cell.setAdjustViewBounds(true);
        if (cell.getMaxWidth() != maxWidth) cell.setMaxWidth(maxWidth);
        if (cell.getMaxHeight() != maxHeight) cell.setMaxHeight(maxHeight);
        if (cell.getMinimumWidth() != minWidth) cell.setMinimumWidth(minWidth);
        if (cell.getMinimumHeight() != minHeight) cell.setMinimumHeight(minHeight);
        cell.measure(getChildMeasureSpec(widthSpec, paddingWidth, LayoutParams.WRAP_CONTENT),
                getChildMeasureSpec(heightSpec, paddingHeight, LayoutParams.WRAP_CONTENT));
        return Geometry.placeholder(cell.getMeasuredWidth(), cell.getMeasuredHeight());
    }

    private static int availableSize(int spec, int padding) {
        return MeasureSpec.getMode(spec) == MeasureSpec.UNSPECIFIED ? Geometry.UNBOUNDED
                : Geometry.size((long) MeasureSpec.getSize(spec) - padding);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        boolean rtl = mirrorInRtl && getLayoutDirection() == LAYOUT_DIRECTION_RTL;
        for (int i = 0; i < cells.size(); i++) {
            ImageCell cell = cells.get(i);
            int x = Geometry.size((long) (i % geometry.columns) * (geometry.cellWidth + geometry.horizontalSpacing));
            int y = getPaddingTop() + Geometry.size((long) (i / geometry.columns) * (geometry.cellHeight + geometry.verticalSpacing));
            int left = rtl ? getWidth() - getPaddingRight() - x - geometry.cellWidth : getPaddingLeft() + x;
            cell.layout(left, y, left + geometry.cellWidth, y + geometry.cellHeight);
            cell.loadIfNeeded();
        }
    }

    @Override
    public void onRtlPropertiesChanged(int layoutDirection) {
        super.onRtlPropertiesChanged(layoutDirection);
        requestLayout();
    }

    /** 仅重试加载失败的可见图片，不重建子项或重复加载成功的图片。 */
    public void retryFailedImages() {
        boolean retry = false;
        for (ImageCell cell : cells) {
            if (cell.failed) {
                cell.requestWidth = 0;
                cell.requestHeight = 0;
                retry = true;
            }
        }
        if (retry) requestLayout();
    }

    private final class ImageCell extends ColorFilterImageView {
        private static final int REQUEST_NATURAL = 0;
        private static final int REQUEST_SINGLE_FIT = 1;
        private static final int REQUEST_SQUARE_CROP = 2;
        private static final int REQUEST_SINGLE_CROP = 3;
        final String url;
        int sourceWidth;
        int sourceHeight;
        int requestWidth;
        int requestHeight;
        int requestedStrategy = -1;
        int generation;
        boolean failed;

        ImageCell(String url) {
            super(GridImageLayout.this.getContext());
            this.url = url;
            setId(View.generateViewId());
            setCornerRadius(cornerRadius);
            setOnClickListener(v -> {
                int position = cells.indexOf(this);
                if (position < 0 || !isClickable()) return;
                ArrayList<String> snapshot = getImages();
                if (onImageClickListener != null) {
                    onImageClickListener.onImageClick(this, position, snapshot);
                } else if (defaultClickEnabled && previewEnabled) {
                    previewImage(position);
                }
            });
        }

        void loadIfNeeded() {
            int width = getWidth();
            int height = getHeight();
            boolean single = images.size() == 1;
            // 原有单图自动尺寸依赖加载后的尺寸，不能先按占位尺寸裁剪原图。
            boolean natural = single && sizeMode == SIZE_ORIGINAL && !hasFixedSingleSize() && imageWidth == 0;
            if (natural) {
                width = Math.max(1, ScreenUtils.getScreenHeight());
                height = width;
            }
            if (width <= 0 || height <= 0) return;
            int strategy = natural ? REQUEST_NATURAL : !single || sizeMode == SIZE_AUTO_WIDTH
                    ? REQUEST_SQUARE_CROP : geometry.crop ? REQUEST_SINGLE_CROP : REQUEST_SINGLE_FIT;
            boolean cropped = strategy == REQUEST_SQUARE_CROP || strategy == REQUEST_SINGLE_CROP;
            // 非方形固定单图的裁剪比例可能改变，不能复用旧比例的裁剪结果。
            boolean adequate = strategy == REQUEST_SINGLE_CROP ? requestWidth == width && requestHeight == height
                    : requestWidth >= width && requestHeight >= height;
            if (requestedStrategy == strategy && adequate) return;
            if (requestedStrategy != strategy || strategy == REQUEST_SINGLE_CROP) {
                requestWidth = 0;
                requestHeight = 0;
            }
            requestedStrategy = strategy;
            requestWidth = Math.max(requestWidth, width);
            requestHeight = Math.max(requestHeight, height);
            int token = ++generation;
            failed = false;
            // 原有加载不加统一清晰度上限；需要控制缩略图开销时通过 attr 显式设置。
            int decodeWidth = requestWidth;
            int decodeHeight = requestHeight;
            int longestEdge = Math.max(decodeWidth, decodeHeight);
            if (maxDecodeEdge > 0 && longestEdge > maxDecodeEdge) {
                // 两边同比例缩小，避免限制解码尺寸后改变 centerCrop 的裁剪比例。
                decodeWidth = Math.max(1, (int) ((long) decodeWidth * maxDecodeEdge / longestEdge));
                decodeHeight = Math.max(1, (int) ((long) decodeHeight * maxDecodeEdge / longestEdge));
            }
            RequestBuilder<Drawable> request = GlideImageLoader.with(this).load(url)
                    .placeholder(placeholderRes).error(placeholderRes)
                    .override(decodeWidth, decodeHeight);
            request = cropped ? request.centerCrop() : request.fitCenter();
            request.listener(new RequestListener<Drawable>() {
                        @Override
                        public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean first) {
                            if (token != generation || !cells.contains(ImageCell.this)) return true;
                            failed = true;
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource source, boolean first) {
                            if (token != generation || !cells.contains(ImageCell.this)) return true;
                            if (!cropped && resource.getIntrinsicWidth() > 0 && resource.getIntrinsicHeight() > 0) {
                                if (sourceWidth != resource.getIntrinsicWidth() || sourceHeight != resource.getIntrinsicHeight()) {
                                    sourceWidth = resource.getIntrinsicWidth();
                                    sourceHeight = resource.getIntrinsicHeight();
                                    if (images.size() == 1 && imageWidth == 0 && !hasFixedSingleSize()) GridImageLayout.this.requestLayout();
                                }
                            }
                            return false;
                        }
                    }).into(this);
        }

        void release() {
            generation++;
            Glide.with(this).clear(this);
            setOnClickListener(null);
            setImageDrawable(null);
        }
    }

    /** 纯尺寸计算；原有模式保留配置尺寸，只有显式选择 fitInside 才收缩尺寸和间距。 */
    static final class Geometry {
        static final int UNBOUNDED = 0x00ffffff;
        static final Geometry EMPTY = new Geometry(1, 0, 0, 0, 0, 0);
        final int columns;
        final int rows;
        final int cellWidth;
        final int cellHeight;
        final int horizontalSpacing;
        final int verticalSpacing;
        final int width;
        final int height;
        final boolean crop;

        private Geometry(int columns, int rows, int cellWidth, int cellHeight, int horizontalSpacing, int verticalSpacing) {
            this(columns, rows, cellWidth, cellHeight, horizontalSpacing, verticalSpacing, false);
        }

        private Geometry(int columns, int rows, int cellWidth, int cellHeight,
                                  int horizontalSpacing, int verticalSpacing, boolean crop) {
            this.columns = columns;
            this.rows = rows;
            this.cellWidth = cellWidth;
            this.cellHeight = cellHeight;
            this.horizontalSpacing = horizontalSpacing;
            this.verticalSpacing = verticalSpacing;
            this.crop = crop;
            width = rows == 0 ? 0 : size((long) columns * cellWidth + (long) (columns - 1) * horizontalSpacing);
            height = size((long) rows * cellHeight + (long) Math.max(0, rows - 1) * verticalSpacing);
        }

        static int size(long value) {
            return (int) Math.max(0, Math.min(UNBOUNDED, value));
        }

        static Geometry placeholder(int width, int height) {
            return new Geometry(1, 1, size(width), size(height), 0, 0);
        }

        static Geometry fixedGrid(int count, int columnCount, int edge, int horizontalSpacing, int verticalSpacing) {
            if (count <= 0) return EMPTY;
            int columns = Math.min(count, Math.max(1, columnCount));
            return new Geometry(columns, (count - 1) / columns + 1, size(edge), size(edge),
                    size(horizontalSpacing), size(verticalSpacing));
        }

        /** 自动均分宽度，但不因高度约束缩图，也不擅自改变调用方配置的间距。 */
        static Geometry autoWidthGrid(int count, int columnCount, int horizontalSpacing,
                                               int verticalSpacing, int availableWidth, int fallbackSize) {
            if (count <= 0) return EMPTY;
            int columns = Math.min(count, Math.max(1, columnCount));
            int edge = availableWidth == UNBOUNDED ? fallbackSize
                    : size((long) availableWidth - (long) size(horizontalSpacing) * (columns - 1)) / columns;
            return fixedGrid(count, columnCount, edge, horizontalSpacing, verticalSpacing);
        }

        /**
         * 保留旧单图规则：无最大尺寸时使用提供/加载的尺寸；有最大尺寸时等比缩放，
         * 窄边不足最小值则以裁剪补足。小图放大的触发阈值沿用原多列图片尺寸。
         * 修正单边未设上限、最小/最大冲突，以及零最小值或极端比例导致图片消失的边界。
         */
        static Geometry originalSingle(int expectedWidth, int expectedHeight, int minWidth, int minHeight,
                                                int maxWidth, int maxHeight, int smallImageThreshold) {
            int limitW = maxWidth > 0 ? size(maxWidth) : UNBOUNDED;
            int limitH = maxHeight > 0 ? size(maxHeight) : UNBOUNDED;
            int minW = Math.min(size(minWidth), limitW);
            int minH = Math.min(size(minHeight), limitH);
            if (expectedWidth <= 0 || expectedHeight <= 0) {
                return new Geometry(1, 1, minW, minH, 0, 0);
            }
            if (maxWidth <= 0 && maxHeight <= 0) {
                return new Geometry(1, 1, size(expectedWidth), size(expectedHeight), 0, 0, true);
            }
            float ratio = (float) expectedHeight / expectedWidth;
            float limitRatio = (float) limitH / limitW;
            int width = expectedWidth;
            int height = expectedHeight;
            boolean crop = false;
            if (width > limitW || height > limitH) {
                if (ratio > limitRatio) {
                    height = limitH;
                    width = (int) (height / ratio);
                    if (width < minW) { width = minW; crop = true; }
                } else {
                    width = limitW;
                    height = (int) (width * ratio);
                    if (height < minH) { height = minH; crop = true; }
                }
            } else if (width < smallImageThreshold || height < smallImageThreshold) {
                if (ratio > limitRatio) {
                    width = minW > 0 ? minW : width;
                    height = (int) (width * ratio);
                    if (height > limitH) { height = limitH; crop = true; }
                } else {
                    height = minH > 0 ? minH : height;
                    width = (int) (height / ratio);
                    if (width > limitW) { width = limitW; crop = true; }
                }
            }
            return new Geometry(1, 1, Math.min(limitW, Math.max(1, size(width))),
                    Math.min(limitH, Math.max(1, size(height))), 0, 0, crop);
        }

        static Geometry fitInsideGrid(int count, int columnCount, int cellSize, int horizontalSpacing,
                                      int verticalSpacing, int availableWidth, int availableHeight) {
            if (count <= 0) return EMPTY;
            int columns = Math.min(count, Math.max(1, columnCount));
            int rows = (count - 1) / columns + 1;
            int width = size(availableWidth);
            int height = size(availableHeight);
            int horizontal = columns <= 1 ? 0 : Math.min(size(horizontalSpacing), width / (columns - 1));
            int vertical = rows <= 1 ? 0 : Math.min(size(verticalSpacing), height / (rows - 1));
            int maxCellWidth = (width - horizontal * (columns - 1)) / columns;
            int maxCellHeight = (height - vertical * (rows - 1)) / rows;
            int edge = Math.min(size(cellSize), Math.min(maxCellWidth, maxCellHeight));
            return new Geometry(columns, rows, edge, edge, horizontal, vertical);
        }

        static Geometry fitInsideSingle(double aspectRatio, int maxWidth, int maxHeight,
                                        int availableWidth, int availableHeight, int fallbackSize) {
            double ratio = aspectRatio > 0 && !Double.isInfinite(aspectRatio) && !Double.isNaN(aspectRatio) ? aspectRatio : 1;
            int widthLimit = Math.min(maxWidth > 0 ? size(maxWidth) : UNBOUNDED, size(availableWidth));
            int heightLimit = Math.min(maxHeight > 0 ? size(maxHeight) : UNBOUNDED, size(availableHeight));
            if (widthLimit == UNBOUNDED && heightLimit == UNBOUNDED) {
                widthLimit = size(fallbackSize);
                heightLimit = size(fallbackSize);
            }
            if (widthLimit == 0 || heightLimit == 0) return new Geometry(1, 1, 0, 0, 0, 0);
            double width = Math.min(widthLimit, heightLimit * ratio);
            int resultWidth = Math.min(widthLimit, Math.max(1, size(Math.round(width))));
            int resultHeight = Math.min(heightLimit, Math.max(1, size(Math.round(width / ratio))));
            return new Geometry(1, 1, resultWidth, resultHeight, 0, 0);
        }
    }
}
