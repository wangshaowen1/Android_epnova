package com.kuaigeglobal.epnova.feature;


import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Canvas;
import android.graphics.Color;
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
import com.alexvasilkov.gestures.views.GestureImageView;
import com.bumptech.glide.Glide;
import com.kuaigeglobal.epnova.R;
import com.kuaigeglobal.epnova.common.OssHelp;
import com.kuaigeglobal.epnova.common.utils.GlideHelper;
import com.kuaigeglobal.epnova.common.utils.Utils;
import com.kuaigeglobal.epnova.core.Global;
import com.kuaigeglobal.epnova.core.ext.DataUtils;
import com.kuaigeglobal.epnova.core.model.PhotoItem;
import com.kuaigeglobal.epnova.widget.HackyViewPager;
import com.kuaigeglobal.epnova.widget.PagerIndicator;
import com.kuaigeglobal.epnova.widget.actionsheet.ActionSheetHelper;
import com.gyf.immersionbar.ImmersionBar;
import com.nhsd.basic.BaseActivity;
import com.nhsd.basic.utils.T;
import com.nhsd.crop.util.ViewUtils;

import java.util.ArrayList;
import java.util.List;


public class PhotoPagerActivity extends BaseActivity {
    public static final String EXTRA_VIEW_POSITIONS = "extra_view_positions";
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
    private HackyViewPager mViewPager;

    private PhotoPagerAdapter adapter;
    private PagerIndicator photoIndicator;

    private int startPosition;
    private View contentView;
    private ArrayList<PhotoItem> photoItems;
    private ArrayList<PhotoItem> checkedPhotoItems;
    private int firstViewPosition;
    private int backgroundColor;
    private List<ViewPosition> viewPositions;
    private boolean enter = false;
    private GestureImageView currentImage;
    private int totalCount;
    private FrameLayout footerLayout;
    private CheckBox photoCheckedStatus;
    private boolean checkable;

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

    }


    protected int getContentViewId() {
        return R.layout.activity_photos_pager;
    }

    protected void initView() {
        contentView = findViewById(R.id.contentView);
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
            contentView.setBackgroundColor(Color.BLACK);
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
        ArrayList<String> photoUris = intent.getStringArrayListExtra(EXTRA_PHOTO_URIS);
        startPosition = intent.getIntExtra(EXTRA_CLICK_POSITION, 0);
        firstViewPosition = intent.getIntExtra(EXTRA_FIRST_VIEW_POSITION, 0);
        totalCount = intent.getIntExtra(EXTRA_TOTAL_COUNT, 0);
        backgroundColor = intent.getIntExtra(EXTRA_BACKGROUND_COLOR, 0);
        ArrayList<String> positions = intent.getStringArrayListExtra(EXTRA_VIEW_POSITIONS);
        if (positions != null) {
            viewPositions = new ArrayList<>();
            for (String position : positions) {
                viewPositions.add(ViewPosition.unpack(position));
            }
        }
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
        if (viewPositions == null || viewPositions.size() == 0) {
            overridePendingTransition(com.nhsd.basic.R.anim.fade_in, com.nhsd.basic.R.anim.fade_out);
            contentView.setBackgroundColor(Color.BLACK);
        } else {
            overridePendingTransition(0, 0);
            photoIndicator.setAlpha(0);
            footerLayout.setAlpha(0);
            photoCheckedStatus.setAlpha(0);
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

    @Override
    public void onBackPressed() {
        if (currentImage != null) {
            exist(currentImage);
        } else {
            super.onBackPressed();
        }
    }

    private void exist(final GestureImageView image) {
        if (isPositionCorrect(image) && !image.getPositionAnimator().isLeaving()) {
            image.getPositionAnimator().exit(true);
            image.getPositionAnimator().addPositionUpdateListener(new ViewPositionAnimator.PositionUpdateListener() {
                @Override
                public void onPositionUpdate(float position, boolean isLeaving) {
                    String value = Integer.toHexString((int) (255 * position));
                    photoIndicator.setAlpha(getIndicatorAlpha(position));
                    footerLayout.setAlpha(getIndicatorAlpha(position));
                    photoCheckedStatus.setAlpha(getIndicatorAlpha(position));
                    if (value.length() == 1) {
                        value = "0" + value;
                    }
                    contentView.setBackgroundColor(Color.parseColor("#" + value + "000000"));
                    float max = 255 * position;
                    if (max < 100) {
                        contentView.setAlpha(max / 100);
                    }
                    if (position == 0) {
                        image.getPositionAnimator().removePositionUpdateListener(this);
                    }
                }
            });

        }
    }

    private float getIndicatorAlpha(float position) {
        return position * position * position;
    }

    private static boolean isPositionCorrect(GestureImageView image) {
        boolean isPositionCorrect = image.getPositionAnimator().getPosition() > 0 && image.getPositionAnimator().getPosition() <= 1;
        return isPositionCorrect;
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
                    .setFillViewport(isFillViewport).setMinZoom(1).setMaxZoom(3).setDoubleTapZoom(2);
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
            if (!holder.gesturesDisabled) {
                holder.image.getController().getSettings().disableGestures();
                holder.gesturesDisabled = true;
            }
            holder.image.getController().getSettings().setMaxZoom(5f);
//            holder.progress.animate().setStartDelay(PROGRESS_DELAY).alpha(1f);
            if (photoItems.size() > position) {
                // Loading image
                GlideHelper.loadFlickrFull(OssHelp.asOssWebp(photoItems.get(position).getUri()), photoItems.get(position).getThumbUri(), holder.image,
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
                            }

                            @Override
                            public void onFailed() {
//                                holder.progress.animate().alpha(0f);
                            }
                        });
            }
            if (!enter && startPosition == position) {
                holder.image.getPositionAnimator().addPositionUpdateListener(new ViewPositionAnimator.PositionUpdateListener() {
                    @Override
                    public void onPositionUpdate(float position, boolean isLeaving) {
                        enter = true;
                        String value = Integer.toHexString((int) (255 * position));
                        photoIndicator.setAlpha(getIndicatorAlpha(position));
                        footerLayout.setAlpha(getIndicatorAlpha(position));
                        photoCheckedStatus.setAlpha(getIndicatorAlpha(position));
                        if (value.length() == 1) {
                            value = "0" + value;
                        }
                        contentView.setBackgroundColor(Color.parseColor("#" + value + "000000"));
                        if (position == 1) {
                            holder.image.getPositionAnimator().removePositionUpdateListener(this);
                        }
                    }
                });

            }

            boolean animate = (position == startPosition);
            int currentViewPositionIndex = position - firstViewPosition;
            if (viewPositions != null && currentViewPositionIndex >= 0 && currentViewPositionIndex < viewPositions.size()) {
                holder.image.getPositionAnimator().enter(viewPositions.get(currentViewPositionIndex), animate);
            } else {
                holder.image.getPositionAnimator().enter(animate);
            }
        }

        @Override
        public void onRecycleViewHolder(@NonNull ViewHolder holder) {
            super.onRecycleViewHolder(holder);

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
            //            final View progress;
            boolean gesturesDisabled;

            ViewHolder(ViewGroup parent) {
                super(ViewUtils.inflate(parent, R.layout.item_photo_full));
                image = itemView.findViewById(R.id.photo_full_image);
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
        ArrayList<String> viewPositions = new ArrayList<>();
        if (visibleViews != null) {
            for (int i = 0; i < visibleViews.size(); i++) {
                View v = visibleViews.get(i);
                viewPositions.add(ViewPosition.from(v).pack());
                if (v == clickViewAndPhotoPosition.first) {
                    intent.putExtra(EXTRA_FIRST_VIEW_POSITION, Math.max(clickViewAndPhotoPosition.second - i, 0));
                }
            }
        }
        intent.putParcelableArrayListExtra(EXTRA_PHOTOS, photos);
        intent.putExtra(EXTRA_VIEW_POSITIONS, viewPositions);
        intent.putExtra(EXTRA_CLICK_POSITION, clickViewAndPhotoPosition.second);
        intent.putExtra(EXTRA_TOTAL_COUNT, totalCount);
        intent.putExtra(EXTRA_BACKGROUND_COLOR, backgroundColor);
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
