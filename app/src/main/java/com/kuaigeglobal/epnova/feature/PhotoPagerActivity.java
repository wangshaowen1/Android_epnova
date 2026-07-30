package com.kuaigeglobal.epnova.feature;


import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Canvas;
import android.graphics.RectF;
import android.os.Build;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.GridView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.core.util.Pair;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.ViewPager;

import com.alexvasilkov.gestures.GestureController;
import com.alexvasilkov.gestures.Settings;
import com.alexvasilkov.gestures.animation.ViewPosition;
import com.alexvasilkov.gestures.animation.ViewPositionAnimator;
import com.alexvasilkov.gestures.commons.RecyclePagerAdapter;
import com.bumptech.glide.Glide;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.shape.RoundedCornerTreatment;
import com.google.android.material.shape.ShapeAppearanceModel;
import com.kuaigeglobal.epnova.R;
import com.kuaigeglobal.epnova.common.OssHelp;
import com.kuaigeglobal.epnova.common.utils.GlideHelper;
import com.kuaigeglobal.epnova.common.utils.Utils;
import com.kuaigeglobal.epnova.core.Global;
import com.kuaigeglobal.epnova.core.ext.DataUtils;
import com.kuaigeglobal.epnova.core.model.PhotoItem;
import com.kuaigeglobal.epnova.widget.GestureImageView;
import com.kuaigeglobal.epnova.widget.HackyViewPager;
import com.kuaigeglobal.epnova.widget.PagerIndicator;
import com.kuaigeglobal.epnova.widget.actionsheet.ActionSheetHelper;
import com.kuaigeglobal.epnova.widget.roundedimageview.Corner;
import com.kuaigeglobal.epnova.widget.roundedimageview.RoundedImageView;
import com.gyf.immersionbar.ImmersionBar;
import com.nhsd.basic.BaseActivity;
import com.nhsd.basic.utils.T;
import com.nhsd.crop.util.ViewUtils;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;


public class PhotoPagerActivity extends BaseActivity {

    private final OnBackPressedCallback backPressedCallback = new OnBackPressedCallback(true) {
        @Override
        public void handleOnBackPressed() {
            handleBackPressed();
        }
    };

    public static final String EXTRA_VIEW_POSITIONS = "extra_view_positions";
    public static final String EXTRA_VIEW_SHAPES = "extra_view_shapes";
    public static final String EXTRA_PHOTO_URIS = "photoUris";
    public static final String EXTRA_CLICK_POSITION = "position";
    public static final String EXTRA_PHOTOS = "photos";
    public static final String EXTRA_FIRST_VIEW_POSITION = "extra_first_view_position";
    public static final String EXTRA_TOTAL_COUNT = "totalCount";
    public static final String EXTRA_BACKGROUND_COLOR = "background";
    public static final String EXTRA_CHECKABLE = "checkable";
    public static final String EXTRA_MAX_CHECK_COUNT = "maxCheckCount";
    public static final String EXTRA_HIDE_STATUS_BAR = "hide_status_bar";
    public static final String EXTRA_RESULT = "result";
    public static final String EXTRA_AI_CHIP = "ai_chip";
    private HackyViewPager mViewPager;

    private PhotoPagerAdapter adapter;
    private PagerIndicator photoIndicator;

    private int startPosition;
    private View contentView;
    private View transitionScrim;
    private ArrayList<PhotoItem> photoItems;
    private ArrayList<PhotoItem> checkedPhotoItems;
    private int firstViewPosition;
    private int backgroundColor;
    private List<ViewPosition> viewPositions;
    private List<String> viewShapes;
    private boolean enter = false;
    private GestureImageView currentImage;
    private int totalCount;
    private FrameLayout footerLayout;
    private CheckBox photoCheckedStatus;
    private boolean checkable;
    private boolean aiChip;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ImmersionBar.with(this).transparentNavigationBar().transparentStatusBar().init();
        boolean hideStatusBar = getIntent().getBooleanExtra(EXTRA_HIDE_STATUS_BAR, true);
        if (hideStatusBar) {
            //取消状态栏
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
//            bar.fullScreen(true);
        }
        if (Build.VERSION.SDK_INT != Build.VERSION_CODES.O) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
        setContentView(getContentViewId());
        initView();
        getOnBackPressedDispatcher().addCallback(this, backPressedCallback);

    }


    protected int getContentViewId() {
        return R.layout.activity_photos_pager;
    }

    protected void initView() {
        contentView = findViewById(R.id.contentView);
        transitionScrim = findViewById(R.id.transitionScrim);
        mViewPager = (HackyViewPager) findViewById(R.id.hackpager);
        photoIndicator = (PagerIndicator) findViewById(R.id.photoIndicator);
        footerLayout = (FrameLayout) findViewById(R.id.footerLayout);
        photoCheckedStatus = (CheckBox) findViewById(R.id.photoCheckedStatus);
        handleIntent(getIntent());
    }


    @Override
    public void finish() {

        if (checkable) {
            Intent extra = new Intent();
            ArrayList<String> checkedList = new ArrayList<>();
            for (PhotoItem item : checkedPhotoItems) {
                checkedList.add(item.getUri());
            }
            extra.putExtra(EXTRA_RESULT, checkedList);
            setResult(RESULT_OK, extra);
        }
        super.finish();
        if (currentImage == null) {
            transitionScrim.setAlpha(1f);
            overridePendingTransition(com.nhsd.basic.R.anim.fade_in, com.nhsd.basic.R.anim.fade_out);
        } else {
            overridePendingTransition(0, 0);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }

    @SuppressWarnings("deprecation")
    private void handleIntent(Intent intent) {
        enter = false;
        contentView.setAlpha(1f);
        ArrayList<String> photoUris = intent.getStringArrayListExtra(EXTRA_PHOTO_URIS);
        startPosition = intent.getIntExtra(EXTRA_CLICK_POSITION, 0);
        firstViewPosition = intent.getIntExtra(EXTRA_FIRST_VIEW_POSITION, 0);
        totalCount = intent.getIntExtra(EXTRA_TOTAL_COUNT, 0);
        backgroundColor = intent.getIntExtra(EXTRA_BACKGROUND_COLOR, 0);
        aiChip = intent.getBooleanExtra(EXTRA_AI_CHIP, false);
        ArrayList<String> positions = intent.getStringArrayListExtra(EXTRA_VIEW_POSITIONS);
        viewPositions = null;
        if (positions != null) {
            viewPositions = new ArrayList<>();
            for (String position : positions) {
                viewPositions.add(ViewPosition.unpack(position));
            }
        }
        viewShapes = intent.getStringArrayListExtra(EXTRA_VIEW_SHAPES);
        photoItems = intent.getParcelableArrayListExtra(EXTRA_PHOTOS);

        if (photoUris != null) {
            photoItems = new ArrayList<>();
            for (String photoUri : photoUris) {
                PhotoItem item = new PhotoItem();
                item.setId(photoUri);
                item.setUri(photoUri);
                photoItems.add(item);
            }
        }
        if (photoItems == null) {
            photoItems = new ArrayList<>();
        }
        if (aiChip) {
            for (PhotoItem item : photoItems) {
                item.setAiChip(aiChip);
            }
        }

        adapter = new PhotoPagerAdapter(mViewPager, photoItems);
        mViewPager.setAdapter(adapter);
        if (photoItems == null || photoItems.size() <= 1) {
            photoIndicator.setVisibility(View.GONE);
        } else {
            photoIndicator.setViewPager(mViewPager);
        }
        mViewPager.setCurrentItem(startPosition);
        mViewPager.setBackgroundColor(backgroundColor);
//        mViewPager.setPageTransformer(true, new DepthPageTransformer());
        if (!hasSourcePosition(startPosition)) {
            overridePendingTransition(com.nhsd.basic.R.anim.fade_in, com.nhsd.basic.R.anim.fade_out);
            transitionScrim.setAlpha(1f);
            photoIndicator.setAlpha(1f);
            footerLayout.setAlpha(1f);
            photoCheckedStatus.setAlpha(1f);
        } else {
            overridePendingTransition(0, 0);
            updateTransitionProgress(0f);
        }


        setCheckedStatus();

        if (checkable) {
            ViewUtils.paddingToNavigationBar(this, contentView);
        } else {
            ViewUtils.paddingToNavigationBar(this, (View) photoIndicator.getParent());
        }
    }

    private void setCheckedStatus() {
        checkable = getIntent().getBooleanExtra(EXTRA_CHECKABLE, false);
        final int maxCheckCount = getIntent().getIntExtra(EXTRA_MAX_CHECK_COUNT, photoItems.size());
        if (checkable) {
            footerLayout.setVisibility(View.VISIBLE);
            checkedPhotoItems = new ArrayList<>();
            checkedPhotoItems.addAll(photoItems);
            photoCheckedStatus.setChecked(true);
            photoCheckedStatus.setText(String.format(getString(com.nhsd.basic.R.string.current_checked), checkedPhotoItems.size(), maxCheckCount));
            photoCheckedStatus.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    PhotoItem item = photoItems.get(mViewPager.getCurrentItem());
                    if (checkedPhotoItems.contains(item)) {
                        checkedPhotoItems.remove(item);
                        photoCheckedStatus.setChecked(false);
                    } else {
                        checkedPhotoItems.add(item);
                        photoCheckedStatus.setChecked(true);
                    }
                    photoCheckedStatus.setText(String.format(getString(com.nhsd.basic.R.string.current_checked), checkedPhotoItems.size(), maxCheckCount));

                }
            });

            mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
                @Override
                public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

                }

                @Override
                public void onPageSelected(int position) {
                    PhotoItem item = photoItems.get(position);
                    if (checkedPhotoItems.contains(item)) {
                        photoCheckedStatus.setChecked(true);
                    } else {
                        photoCheckedStatus.setChecked(false);
                    }

                }

                @Override
                public void onPageScrollStateChanged(int state) {

                }
            });
        }
    }

    private void handleBackPressed() {
        if (currentImage != null && hasSourcePosition(mViewPager.getCurrentItem())) {
            exist(currentImage);
        } else {
            currentImage = null;
            exitPage();
        }
    }

    private void exitPage() {
        backPressedCallback.setEnabled(false);
        getOnBackPressedDispatcher().onBackPressed();
    }

    private void exist(final GestureImageView image) {
        if (!hasSourcePosition(mViewPager.getCurrentItem())) {
            currentImage = null;
            exitPage();
            return;
        }
        if (isPositionCorrect(image) && !image.getPositionAnimator().isLeaving()) {
            image.getPositionAnimator().addPositionUpdateListener(new ViewPositionAnimator.PositionUpdateListener() {
                @Override
                public void onPositionUpdate(float position, boolean isLeaving) {
                    updateTransitionProgress(position);
                    image.setTransitionShapeProgress(position);
                    if (position <= 0f) {
                        image.getPositionAnimator().removePositionUpdateListener(this);
                    }
                }
            });
            image.getPositionAnimator().exit(true);

        }
    }

    /** 只渐变背景遮罩与操作控件，缩放中的图片和根容器始终保持不透明。 */
    private void updateTransitionProgress(float position) {
        float progress = Math.max(0f, Math.min(1f, position));
        transitionScrim.setAlpha(progress);
        float controlsAlpha = getIndicatorAlpha(progress);
        photoIndicator.setAlpha(controlsAlpha);
        footerLayout.setAlpha(controlsAlpha);
        photoCheckedStatus.setAlpha(1f);
        contentView.setAlpha(1f);
    }

    /** 操作控件在放大后半程平滑出现，避免与主体图片争夺视觉焦点。 */
    private float getIndicatorAlpha(float position) {
        float progress = Math.max(0f, Math.min(1f, (position - 0.55f) / 0.45f));
        return progress * progress * (3f - 2f * progress);
    }

    private static boolean isPositionCorrect(GestureImageView image) {
        boolean isPositionCorrect = image.getPositionAnimator().getPosition() > 0 && image.getPositionAnimator().getPosition() <= 1;
        return isPositionCorrect;
    }

    /** 判断指定图片是否有可回收的来源缩略图位置，没有时退化为页面淡入淡出。 */
    private boolean hasSourcePosition(int position) {
        int sourcePosition = position - firstViewPosition;
        return viewPositions != null
                && sourcePosition >= 0
                && sourcePosition < viewPositions.size();
    }

    /** 将来源缩略图的形状配置到对应大图，无法识别时安全退化为矩形。 */
    private void configureTransitionShape(GestureImageView image, int position) {
        image.clearTransitionShape();
        int sourcePosition = position - firstViewPosition;
        if (viewShapes == null || sourcePosition < 0 || sourcePosition >= viewShapes.size()) return;
        String shape = viewShapes.get(sourcePosition);
        int separator = shape == null ? -1 : shape.indexOf(':');
        if (separator <= 0 || separator >= shape.length() - 1) return;
        try {
            int type = Integer.parseInt(shape.substring(0, separator));
            float radius = Float.parseFloat(shape.substring(separator + 1));
            if (type < GestureImageView.TRANSITION_SHAPE_RECT
                    || type > GestureImageView.TRANSITION_SHAPE_OVAL) return;
            image.setTransitionShape(type, radius);
        } catch (NumberFormatException ignored) {
            image.clearTransitionShape();
        }
    }


    protected class PhotoPagerAdapter extends RecyclePagerAdapter<PhotoPagerAdapter.ViewHolder> {


        private final int maxImageHeight;
        private final int maxImageWidth;
        private boolean isExitEnabled = false;
        private boolean isFillViewport = true;
        private ArrayList<PhotoItem> photoItems;
        private ViewPager viewPager;

        public PhotoPagerAdapter(ViewPager pager, ArrayList<PhotoItem> photoItems) {
            this.viewPager = pager;
            this.photoItems = photoItems;
            Canvas canvas = new Canvas();
            maxImageHeight = canvas.getMaximumBitmapHeight();
            maxImageWidth = canvas.getMaximumBitmapWidth();
        }

        @Override
        public int getCount() {
            return Math.max(totalCount, photoItems.size());
        }


        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup container) {
            final ViewHolder holder = new ViewHolder(container);

            holder.image.getController().getSettings()
                    .setExitEnabled(isExitEnabled).setFitMethod(Settings.Fit.HORIZONTAL)
                    .setFillViewport(isFillViewport).setMinZoom(1).setMaxZoom(3)
                    .setDoubleTapZoom(2).setAnimationsDuration(200L);
            holder.image.setMaxWidth(maxImageWidth);
            holder.image.setMaxHeight(maxImageHeight);
            holder.image.getPositionAnimator().addPositionUpdateListener(new ViewPositionAnimator.PositionUpdateListener() {
                @Override
                public void onPositionUpdate(float position, boolean isLeaving) {
//                    holder.progress.setVisibility(position == 1f ? View.VISIBLE : View.INVISIBLE);
                }
            });
            holder.image.getPositionAnimator().addPositionUpdateListener(new ViewPositionAnimator.PositionUpdateListener() {
                @Override
                public void onPositionUpdate(float position, boolean isLeaving) {

                    if (position == 0f && isLeaving) { // Exit finished
                        holder.image.getController().getSettings().disableBounds();
                        holder.image.getPositionAnimator().setState(0f, false, false);
                        // Finishing activity
                        finish();
                    }
                }
            });
            holder.image.getController().enableScrollInViewPager(viewPager);
            holder.image.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    showSaveImageDialog(holder.image);
                    return false;
                }
            });
            holder.image.getController().setOnGesturesListener(new GestureController.OnGestureListener() {
                @Override
                public void onDown(@NonNull MotionEvent event) {

                }

                @Override
                public void onUpOrCancel(@NonNull MotionEvent event) {

                }

                @Override
                public boolean onSingleTapUp(@NonNull MotionEvent event) {
                    return false;
                }

                @Override
                public boolean onSingleTapConfirmed(@NonNull MotionEvent event) {
                    exist(holder.image);
                    return false;
                }


                @Override
                public void onLongPress(@NonNull MotionEvent event) {

                }

                @Override
                public boolean onDoubleTap(@NonNull MotionEvent event) {
                    return false;
                }
            });

            return holder;
        }

        @Override
        public void setPrimaryItem(ViewGroup container, int position, Object object) {
            super.setPrimaryItem(container, position, object);
            ViewHolder holder = (ViewHolder) object;
            currentImage = holder.image;
        }


        @Override
        public void onBindViewHolder(@NonNull final ViewHolder holder, final int position) {
            holder.boundPosition = position;
            holder.entryStarted = false;
            if (holder.entryFallback != null) {
                holder.image.removeCallbacks(holder.entryFallback);
                holder.entryFallback = null;
            }
            if (!holder.gesturesDisabled) {
                holder.image.getController().getSettings().disableGestures();
                holder.gesturesDisabled = true;
            }
            holder.image.getController().getSettings().setMaxZoom(5f);
//            holder.progress.animate().setStartDelay(PROGRESS_DELAY).alpha(1f);
            if (photoItems.size() > position) {
                // Loading image
                PhotoItem photoItem = photoItems.get(position);
                holder.aiChip.setVisibility(photoItem.isAiChip() ? View.VISIBLE : View.GONE);
                GlideHelper.loadFlickrFull(OssHelp.asOssWebp(photoItem.getUri()), photoItem.getThumbUri(), holder.image,
                        new GlideHelper.ImageLoadingListener() {
                            @Override
                            public void onLoaded() {
//                                holder.progress.animate().cancel();
//                                holder.progress.setAlpha(0f);
                                // Re-enabling touch controls
                                if (holder.gesturesDisabled) {
                                    holder.image.getController().getSettings().enableGestures();
                                    holder.gesturesDisabled = false;
                                }
                                if (position == startPosition) {
                                    startPositionAnimation(holder, position);
                                }
                            }

                            @Override
                            public void onFailed() {
//                                holder.progress.animate().alpha(0f);
                                if (position == startPosition) {
                                    startPositionAnimation(holder, position);
                                }
                            }
                        });
            }
            if (position == startPosition && hasSourcePosition(position)) {
                holder.entryFallback = () -> startPositionAnimation(holder, position);
                holder.image.postDelayed(holder.entryFallback, 80L);
            } else {
                startPositionAnimation(holder, position);
            }
        }

        /** 在缩略资源可用或短暂兜底等待结束后，启动当前图片的位置过渡。 */
        private void startPositionAnimation(@NonNull final ViewHolder holder, final int position) {
            if (holder.boundPosition != position || holder.entryStarted) return;
            holder.entryStarted = true;
            if (holder.entryFallback != null) {
                holder.image.removeCallbacks(holder.entryFallback);
                holder.entryFallback = null;
            }

            boolean animate = position == startPosition && hasSourcePosition(position);
            configureTransitionShape(holder.image, position);
            holder.image.setTransitionShapeProgress(animate ? 0f : 1f);
            if (animate && !enter) {
                enter = true;
                holder.image.getPositionAnimator().addPositionUpdateListener(
                        new ViewPositionAnimator.PositionUpdateListener() {
                            @Override
                            public void onPositionUpdate(float progress, boolean isLeaving) {
                                updateTransitionProgress(progress);
                                holder.image.setTransitionShapeProgress(progress);
                                if (progress >= 1f) {
                                    holder.image.getPositionAnimator()
                                            .removePositionUpdateListener(this);
                                }
                            }
                        });
            }

            int currentViewPositionIndex = position - firstViewPosition;
            if (viewPositions != null
                    && currentViewPositionIndex >= 0
                    && currentViewPositionIndex < viewPositions.size()) {
                holder.image.getPositionAnimator().enter(
                        viewPositions.get(currentViewPositionIndex), animate);
            } else {
                holder.image.getPositionAnimator().enter(animate);
            }
        }

        @Override
        public void onRecycleViewHolder(@NonNull ViewHolder holder) {
            super.onRecycleViewHolder(holder);

            if (holder.entryFallback != null) {
                holder.image.removeCallbacks(holder.entryFallback);
                holder.entryFallback = null;
            }
            holder.image.clearTransitionShape();
            if (holder.gesturesDisabled) {
                holder.image.getController().getSettings().enableGestures();
                holder.gesturesDisabled = false;
            }

            Glide.with(PhotoPagerActivity.this).clear(holder.image);

//            holder.progress.animate().cancel();
//            holder.progress.setAlpha(0f);

            holder.image.setImageDrawable(null);
        }

        class ViewHolder extends RecyclePagerAdapter.ViewHolder {
            final GestureImageView image;
            final TextView aiChip;
            //            final View progress;
            boolean gesturesDisabled;
            boolean entryStarted;
            int boundPosition = -1;
            Runnable entryFallback;

            ViewHolder(ViewGroup parent) {
                super(ViewUtils.inflate(parent, R.layout.item_photo_full));
                image = itemView.findViewById(R.id.photo_full_image);
                aiChip = itemView.findViewById(R.id.aiChip);
//                image.setLayerType(View.LAYER_TYPE_SOFTWARE,null);
//                progress = ViewUtils.find(itemView, R.id.photo_full_progress);
            }
        }
    }

    private void showSaveImageDialog(GestureImageView image) {
        ActionSheetHelper.createDialog(new String[]{"Save photo"}, (v, position) -> {
            switch (position) {
                case 0:
                    Utils.saveToSystemAlbumWithWatermark(this, image.getDrawable(), exist -> {
                        if (exist) {
                            T.showShort(R.string.toast_save_success);
                        }
                    });
                    break;

            }
        }).show(getSupportFragmentManager());
    }


    public static void launchWithPhotos(Context context, List<View> visibleViews, ArrayList<PhotoItem> photos, Pair<View, Integer> clickViewAndPhotoPosition, int totalCount, int backgroundColor) {
        Intent intent = new Intent(context, PhotoPagerActivity.class);
        putViewPositions(intent, visibleViews, clickViewAndPhotoPosition.first,
                clickViewAndPhotoPosition.second);
        intent.putParcelableArrayListExtra(EXTRA_PHOTOS, photos);
        intent.putExtra(EXTRA_CLICK_POSITION, clickViewAndPhotoPosition.second);
        intent.putExtra(EXTRA_TOTAL_COUNT, totalCount);
        intent.putExtra(EXTRA_BACKGROUND_COLOR, backgroundColor);
        context.startActivity(intent);
    }

    /** 记录可见缩略图的位置与形状，供进入和退出转场复用。 */
    private static void putViewPositions(Intent intent, List<View> visibleViews,
                                         View clickView, int clickPosition) {
        ArrayList<String> viewPositions = new ArrayList<>();
        ArrayList<String> viewShapes = new ArrayList<>();
        if (visibleViews != null) {
            for (int i = 0; i < visibleViews.size(); i++) {
                View view = visibleViews.get(i);
                viewPositions.add(ViewPosition.from(view).pack());
                viewShapes.add(packViewShape(view));
                if (view == clickView) {
                    intent.putExtra(EXTRA_FIRST_VIEW_POSITION, Math.max(clickPosition - i, 0));
                }
            }
        }
        intent.putExtra(EXTRA_VIEW_POSITIONS, viewPositions);
        intent.putExtra(EXTRA_VIEW_SHAPES, viewShapes);
    }

    /** 读取项目两类圆角图片控件的真实形状，并序列化为转场参数。 */
    private static String packViewShape(View view) {
        int shape = GestureImageView.TRANSITION_SHAPE_RECT;
        float radius = 0f;
        if (view instanceof RoundedImageView) {
            RoundedImageView roundedView = (RoundedImageView) view;
            if (roundedView.isOval()) {
                shape = GestureImageView.TRANSITION_SHAPE_OVAL;
            } else {
                float topLeft = roundedView.getCornerRadius(Corner.TOP_LEFT);
                float topRight = roundedView.getCornerRadius(Corner.TOP_RIGHT);
                float bottomRight = roundedView.getCornerRadius(Corner.BOTTOM_RIGHT);
                float bottomLeft = roundedView.getCornerRadius(Corner.BOTTOM_LEFT);
                radius = getUniformCornerRadius(topLeft, topRight, bottomRight, bottomLeft);
                if (radius > 0f) shape = GestureImageView.TRANSITION_SHAPE_ROUNDED;
            }
        } else if (view instanceof ShapeableImageView) {
            ShapeableImageView shapeableView = (ShapeableImageView) view;
            ShapeAppearanceModel model = shapeableView.getShapeAppearanceModel();
            boolean roundedCorners = model.getTopLeftCorner() instanceof RoundedCornerTreatment
                    && model.getTopRightCorner() instanceof RoundedCornerTreatment
                    && model.getBottomRightCorner() instanceof RoundedCornerTreatment
                    && model.getBottomLeftCorner() instanceof RoundedCornerTreatment;
            if (roundedCorners && view.getWidth() > 0 && view.getHeight() > 0) {
                RectF bounds = new RectF(0f, 0f, view.getWidth(), view.getHeight());
                float topLeft = model.getTopLeftCornerSize().getCornerSize(bounds);
                float topRight = model.getTopRightCornerSize().getCornerSize(bounds);
                float bottomRight = model.getBottomRightCornerSize().getCornerSize(bounds);
                float bottomLeft = model.getBottomLeftCornerSize().getCornerSize(bounds);
                radius = getUniformCornerRadius(topLeft, topRight, bottomRight, bottomLeft);
                if (radius > 0f) {
                    float maxOvalRadius = Math.min(view.getWidth(), view.getHeight()) * 0.5f;
                    shape = Math.abs(radius - maxOvalRadius) <= 1f
                            ? GestureImageView.TRANSITION_SHAPE_OVAL
                            : GestureImageView.TRANSITION_SHAPE_ROUNDED;
                }
            }
        }
        return shape + ":" + radius;
    }

    /** 四角一致时返回统一圆角，非统一圆角退化为矩形以避免错误形变。 */
    private static float getUniformCornerRadius(float... radii) {
        if (radii.length == 0) return 0f;
        float min = radii[0];
        float max = radii[0];
        for (float radius : radii) {
            min = Math.min(min, radius);
            max = Math.max(max, radius);
        }
        return max - min <= 1f ? Math.max(0f, max) : 0f;
    }

    public static void launchWithAiChip(Context context, String photoUri) {
        ArrayList<String> uris = new ArrayList<>();
        uris.add(photoUri);
        ArrayList<@Nullable PhotoItem> photos = DataUtils.toPhotoItem(uris);
        Intent intent = new Intent(context, PhotoPagerActivity.class);
        ArrayList<String> viewPositions = new ArrayList<>();
        intent.putParcelableArrayListExtra(EXTRA_PHOTOS, photos);
//        intent.putExtra(EXTRA_VIEW_POSITIONS, 0);
//        intent.putExtra(EXTRA_CLICK_POSITION, 0);
//        intent.putExtra(EXTRA_TOTAL_COUNT, 1);
//        intent.putExtra(EXTRA_BACKGROUND_COLOR, 0);
        intent.putExtra(EXTRA_AI_CHIP, true);
        context.startActivity(intent);
    }

    public static void launchWithPhotos(Context context, List<View> visibleViews, ArrayList<PhotoItem> photos, Pair<View, Integer> clickViewAndPhotoPosition, int totalCount) {
        launchWithPhotos(context, visibleViews, photos, clickViewAndPhotoPosition, totalCount, 0);
    }

    public static void launchWithPhotos(Context context, ListView parent, ArrayList<PhotoItem> photos, int position, ImageGetterFromItemView getter) {
        Pair<List<View>, View> viewsAndTarget = getViewsAt(parent, position, getter);
        launchWithPhotos(context, viewsAndTarget.first, photos, new Pair<View, Integer>(viewsAndTarget.second, position), photos.size());
    }

    public static void launchWithPhotos(Context context, RecyclerView parent, ArrayList<PhotoItem> photos, int position, ImageGetterFromItemView getter, int totalCount) {
        Pair<List<View>, View> viewsAndTarget = getViewsAt(parent, position, getter);
        launchWithPhotos(context, viewsAndTarget.first, photos, new Pair<View, Integer>(viewsAndTarget.second, position), totalCount);
    }

    public static void launchWithPhotos(Context context, GridView parent, ArrayList<PhotoItem> photos, int position, ImageGetterFromItemView getter) {
        Pair<List<View>, View> viewsAndTarget = getViewsAt(parent, position, getter);
        launchWithPhotos(context, viewsAndTarget.first, photos, new Pair<View, Integer>(viewsAndTarget.second, position), photos.size());
    }

    public static void launchWithPhotos(Context context, GridLayout parent, ArrayList<PhotoItem> photos, int position, ImageGetterFromItemView getter) {
        List<View> childViews = new ArrayList<>();
        View clickedView = null;
        for (int i = 0; i < parent.getChildCount(); i++) {
            View v = getter.getImage(i, parent.getChildAt(i));
            if (v != null) {
                childViews.add(v);
                if (position == childViews.size() - 1) {
                    clickedView = v;
                }
            }
        }

        launchWithPhotos(context, childViews, photos, new Pair<View, Integer>(clickedView, position), photos.size());
    }

    public static void launchWithPhotos(Context context, GridLayout parent, ArrayList<PhotoItem> photos, int position, String userId) {

        launchWithPhotos(context, parent, photos, position, new ImageGetterFromItemView() {
            @Override
            public View getImage(int position, View itemView) {
                return itemView;
            }
        });
    }

    public static void launch(Context context, String photoUri) {
        ArrayList<String> uris = new ArrayList<>();
        uris.add(photoUri);
        launch(context, uris);
    }

    public static void launch(Context context, ArrayList<String> photoUris) {
        launch(context, photoUris, 0);
    }

    public static void launch(Context context, ArrayList<String> photoUris, int index) {
        launch(context, null, photoUris, new Pair<View, Integer>(null, index));
    }

    public static void launch(Context context, List<View> visibleViews, ArrayList<String> photoUris, Pair<View, Integer> viewAndPicPosition) {
        launchWithPhotos(context, visibleViews, DataUtils.toPhotoItem(photoUris), viewAndPicPosition, photoUris.size());
    }


    public interface ImageGetterFromItemView {
        public View getImage(int position, View itemView);
    }

    private static Pair<List<View>, View> getViewsAt(GridView parent, int position, ImageGetterFromItemView getter) {
        int first = parent.getFirstVisiblePosition();
        int last = parent.getLastVisiblePosition();
        ArrayList<View> views = new ArrayList<>();
        View clickView = null;
        for (int i = first; i <= last; i++) {
            View itemView = parent.getChildAt(i);
            if (itemView == null) {
                continue;
            }
            View v = getter.getImage(i, itemView);
            if (v != null) {
                views.add(v);
                if (first + views.size() - 1 == position) {//5
                    clickView = v;
                }
            }
        }
        return new Pair<List<View>, View>(views, clickView);

    }

    private static Pair<List<View>, View> getViewsAt(ListView parent, int position, ImageGetterFromItemView getter) {
        int first = parent.getFirstVisiblePosition();
        int last = parent.getLastVisiblePosition();
        ArrayList<View> views = new ArrayList<>();
        View clickView = null;
        for (int i = first; i <= last; i++) {
            View itemView = parent.getChildAt(i);
            if (itemView == null) {
                continue;
            }
            View v = getter.getImage(i, itemView);
            if (v != null) {
                views.add(v);
                if (first + views.size() - 1 == position) {//5
                    clickView = v;
                }
            }
        }
        return new Pair<List<View>, View>(views, clickView);

    }

    private static Pair<List<View>, View> getViewsAt(RecyclerView parent, int position, ImageGetterFromItemView getter) {
        LinearLayoutManager manager = (LinearLayoutManager) parent.getLayoutManager();
        int first = manager.findFirstVisibleItemPosition();
        int last = manager.findLastVisibleItemPosition();
        ArrayList<View> views = new ArrayList<>();
        View clickView = null;
        for (int i = first; i <= last; i++) {
            View itemView = parent.getLayoutManager().findViewByPosition(i);
            if (itemView == null) {
                continue;
            }
            View v = getter != null ? getter.getImage(i, itemView) : itemView;
            if (v != null) {
                views.add(v);
                if (first + views.size() - 1 == position) {
                    clickView = v;
                }
            }

        }
        return new Pair<List<View>, View>(views, clickView);

    }

    public static void launch(Context context, ListView parent, ArrayList<String> photoUris, int position, ImageGetterFromItemView getter) {
        Pair<List<View>, View> viewsAndTarget = getViewsAt(parent, position, getter);
        launch(context, viewsAndTarget.first, photoUris, new Pair<View, Integer>(viewsAndTarget.second, position));
    }

    public static void launch(Context context, GridView parent, ArrayList<String> photoUris, int position, ImageGetterFromItemView getter) {
        Pair<List<View>, View> viewsAndTarget = getViewsAt(parent, position, getter);
        launch(context, viewsAndTarget.first, photoUris, new Pair<View, Integer>(viewsAndTarget.second, position));
    }

    public static void launch(Context context, RecyclerView parent, ArrayList<String> photoUris, int position, ImageGetterFromItemView getter) {
        Pair<List<View>, View> viewsAndTarget = getViewsAt(parent, position, getter);
        launch(context, viewsAndTarget.first, photoUris, new Pair<View, Integer>(viewsAndTarget.second, position));
    }

    public static void launch(Context context, GridLayout parent, ArrayList<String> photoUris, int position, ImageGetterFromItemView getter) {
        List<View> childViews = new ArrayList<>();
        View clickedView = null;
        for (int i = 0; i < parent.getChildCount(); i++) {
            View v = getter.getImage(i, parent.getChildAt(i));
            if (v != null) {
                childViews.add(v);
                if (position == childViews.size() - 1) {
                    clickedView = childViews.get(position);
                }
            }
        }
        launch(context, childViews, photoUris, new Pair<View, Integer>(clickedView, position));
    }

    public static void launch(Context context, GridLayout parent, ArrayList<String> photoUris, int position) {
        launch(context, parent, photoUris, position, new ImageGetterFromItemView() {
            @Override
            public View getImage(int position, View itemView) {
                return itemView;
            }
        });
    }

    public static void launch(Context context, View v, ArrayList<String> photoUris, int position) {
        ArrayList<View> views = new ArrayList<>();
        views.add(v);
        launch(context, views, photoUris, new Pair<View, Integer>(v, position));
    }

    public static void launch(Context context, View v, String url) {

        ArrayList<String> urls = new ArrayList<>();
        urls.add(url);
        launch(context, v, urls, 0);
    }

    protected void setTranslucentStatus(boolean on) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            Window win = getWindow();
            WindowManager.LayoutParams winParams = win.getAttributes();
            final int bits = WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS;
            final int bitsNav = WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION;
            if (on) {
                winParams.flags |= bits;
                winParams.flags |= bitsNav;
            } else {
                winParams.flags &= ~bits;
                winParams.flags &= ~bitsNav;
            }

            win.setAttributes(winParams);
        }
    }


}
