/*
 * Copyright 2017 Zhihu Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.nhsd.mattise.ui;

import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.READ_MEDIA_IMAGES;
import static android.Manifest.permission.READ_MEDIA_VIDEO;
import static android.Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.Manifest;
import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.TypedValue;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.gyf.immersionbar.ImmersionBar;
import com.nhsd.basic.manager.FileManager;
import com.nhsd.crop.Crop;
import com.nhsd.crop.util.ViewUtils;
import com.nhsd.mattise.internal.entity.Album;
import com.nhsd.mattise.internal.entity.Item;
import com.nhsd.mattise.internal.entity.SelectionSpec;
import com.nhsd.mattise.internal.model.AlbumCollection;
import com.nhsd.mattise.internal.model.SelectedItemCollection;
import com.nhsd.mattise.internal.ui.AlbumPreviewActivity;
import com.nhsd.mattise.internal.ui.BasePreviewActivity;
import com.nhsd.mattise.internal.ui.MediaSelectionFragment;
import com.nhsd.mattise.internal.ui.SelectedPreviewActivity;
import com.nhsd.mattise.internal.ui.adapter.AlbumMediaAdapter;
import com.nhsd.mattise.internal.ui.adapter.AlbumsAdapter;
import com.nhsd.mattise.internal.ui.livedata.AlbumViewModel;
import com.nhsd.mattise.internal.ui.widget.CheckRadioView;
import com.nhsd.mattise.internal.ui.widget.IncapableDialog;
import com.nhsd.mattise.internal.utils.MediaStoreCompat;
import com.nhsd.mattise.internal.utils.PathUtils;
import com.nhsd.mattise.internal.utils.PermissionUtils;
import com.nhsd.mattise.internal.utils.PhotoMetadataUtils;
import com.nhsd.mattise.internal.utils.SingleMediaScanner;
import com.nhsd.permisson.PermissionX;
import com.zhihu.matisse.R;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import kotlin.Pair;

/**
 * Main Activity to display albums and media content (images/videos) in each album
 * and also support media selecting operations.
 */
public class MatisseActivity extends AppCompatActivity implements
        AlbumCollection.AlbumCallbacks,
        MediaSelectionFragment.SelectionProvider, View.OnClickListener,
        AlbumMediaAdapter.CheckStateListener, AlbumMediaAdapter.OnMediaClickListener,
        AlbumMediaAdapter.OnPhotoCapture, AdapterView.OnItemClickListener {

    public static final String EXTRA_RESULT_SELECTION = "extra_result_selection";
    public static final String EXTRA_RESULT_SELECTION_PATH = "extra_result_selection_path";
    public static final String EXTRA_RESULT_SELECTION_ITEMS = "extra_result_selection_items";
    public static final String EXTRA_RESULT_ORIGINAL_ENABLE = "extra_result_original_enable";
    private static final int REQUEST_CODE_PREVIEW = 23;
    private static final int REQUEST_CODE_CAPTURE = 24;
    public static final String CHECK_STATE = "checkState";
    private final AlbumCollection mAlbumCollection = new AlbumCollection();
    private MediaStoreCompat mMediaStoreCompat;
    private SelectedItemCollection mSelectedCollection = new SelectedItemCollection(this);
    private SelectionSpec mSpec;

    //    private AlbumsSpinner mAlbumsSpinner;
    private AlbumsAdapter mAlbumsAdapter;
    private ViewGroup mBottomToolbar;
    private TextView mButtonPreview;
    private TextView mButtonApply;
    private View mContainer;
    private View mEmptyView;

    private LinearLayout mOriginalLayout;
    private CheckRadioView mOriginal;
    private boolean mOriginalEnable;
    private MediaSelectionFragment fragment;
    private AlbumViewModel viewModel;
    private TextView mAlbumTitle;
    private ImageView mAlbumTitleArrow;
    private FrameLayout mAlbumListLayout;
    private ListView mAlbumList;
    private View mAlbumTitleLayout;

    private final ActivityResultLauncher<Intent> appDetailsSetting = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                // 处理返回结果
                if (!PermissionUtils.isAndroid14UserSelectedPermissionsGranted(this)) {
                    viewModel.refresh();
                    updateSettingUI();
                }
            }
    );

    @SuppressLint("WrongViewCast")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        // programmatically set theme before super.onCreate()
        mSpec = SelectionSpec.getInstance();
        setTheme(mSpec.themeId);
        super.onCreate(savedInstanceState);
//        overridePendingTransition(R.anim.push_bottom_in, 0);
        if (!mSpec.hasInited) {
            setResult(RESULT_CANCELED);
            finish();
            return;
        }
        setContentView(R.layout.activity_matisse);

        if (mSpec.needOrientationRestriction()) {
            setRequestedOrientation(mSpec.orientation);
        }

        if (mSpec.capture) {
            mMediaStoreCompat = new MediaStoreCompat(this);
            if (mSpec.captureStrategy == null)
                throw new RuntimeException("Don't forget to set CaptureStrategy.");
            mMediaStoreCompat.setCaptureStrategy(mSpec.captureStrategy);
        }

        findViewById(R.id.album_title_cancel).setOnClickListener(this);
        mAlbumTitle = findViewById(R.id.album_title);
        mAlbumTitleArrow = findViewById(R.id.album_title_arrow);
        mAlbumListLayout = findViewById(R.id.album_list_layout);
        mAlbumList = findViewById(R.id.album_list);
        mAlbumTitleLayout = findViewById(R.id.album_title_layout);
        mBottomToolbar = findViewById(R.id.bottom_toolbar);
        mButtonPreview = (TextView) findViewById(R.id.button_preview);
        mButtonApply = (TextView) findViewById(R.id.button_apply);
        mButtonPreview.setOnClickListener(this);
        mButtonApply.setOnClickListener(this);
        mAlbumTitleLayout.setOnClickListener(this);
        mAlbumListLayout.setOnClickListener(this);
        mContainer = findViewById(R.id.container);
        mEmptyView = findViewById(R.id.empty_view);
        mOriginalLayout = findViewById(R.id.originalLayout);
        mOriginal = findViewById(R.id.original);
        mOriginalLayout.setOnClickListener(this);
        findViewById(R.id.btn2Setting).setOnClickListener(v -> {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.fromParts("package", getPackageName(), null));
            appDetailsSetting.launch(intent);
        });
        updateSettingUI();
        mSelectedCollection.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            mOriginalEnable = savedInstanceState.getBoolean(CHECK_STATE);
        }
        updateBottomToolbar();
        mAlbumsAdapter = new AlbumsAdapter(this, null, false);
//        mAlbumsSpinner = new AlbumsSpinner(this);
//        mAlbumsSpinner.setOnItemSelectedListener(this);
//        mAlbumsSpinner.setSelectedTextView((TextView) findViewById(R.id.selected_album));
//        mAlbumsSpinner.setPopupAnchorView(findViewById(R.id.toolbar));
//        mAlbumsSpinner.setAdapter(mAlbumsAdapter);
        mAlbumList.setAdapter(mAlbumsAdapter);
        mAlbumList.setOnItemClickListener(this);
        mAlbumCollection.onCreate(this, this);
        mAlbumCollection.onRestoreInstanceState(savedInstanceState);
        mAlbumCollection.loadAlbums();

        viewModel = new ViewModelProvider(this).get(AlbumViewModel.class);

        ImmersionBar.with(this).statusBarDarkFont(false).init();
        ViewUtils.paddingToStatusBar(this,findViewById(R.id.root));
    }

    private void updateSettingUI() {
        findViewById(R.id.llUserSelected).setVisibility(PermissionUtils.isAndroid14UserSelectedPermissionsGranted(this) ? VISIBLE : GONE);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mSelectedCollection.onSaveInstanceState(outState);
        mAlbumCollection.onSaveInstanceState(outState);
        outState.putBoolean("checkState", mOriginalEnable);
    }


    private void showSpinner() {
        mAlbumTitleArrow.setRotation(0);
        mAlbumTitleArrow.animate().rotation(180).setDuration(200).start();
        mAlbumListLayout.setVisibility(VISIBLE);
        Animation animation = AnimationUtils.loadAnimation(this, R.anim.gd_grow_from_top);
        mAlbumList.startAnimation(animation);
        int max = (int) animation.getDuration();
        ValueAnimator anInt = ValueAnimator.ofInt(0, max);
        anInt.addUpdateListener(animation1 -> {
            int value = (int) animation1.getAnimatedValue();
            float ratio = value / (float) max;
            ArgbEvaluator bgEvaluator = new ArgbEvaluator();
            int bgColor = (int) bgEvaluator.evaluate(ratio, Color.TRANSPARENT, Color.parseColor("#7F000000"));
            mAlbumListLayout.setBackgroundColor(bgColor);
        });
        anInt.start();
    }

    private void closeSpinner() {
        mAlbumTitleArrow.setRotation(180);
        mAlbumTitleArrow.animate().rotation(0).setDuration(200).start();
        Animation animation = AnimationUtils.loadAnimation(this, R.anim.gd_shrink_from_bottom);
        animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                mAlbumListLayout.setVisibility(GONE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        mAlbumList.startAnimation(animation);
        int max = (int) animation.getDuration();
        ValueAnimator anInt = ValueAnimator.ofInt(0, max);
        anInt.addUpdateListener(animation12 -> {
            int value = (int) animation12.getAnimatedValue();
            float ratio = value / (float) max;
            ArgbEvaluator bgEvaluator = new ArgbEvaluator();
            int bgColor = (int) bgEvaluator.evaluate(ratio, Color.parseColor("#7F000000"), Color.TRANSPARENT);
            mAlbumListLayout.setBackgroundColor(bgColor);
        });
        anInt.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mAlbumCollection.onDestroy();
//        mSpec.onCheckedListener = null;
//        mSpec.onSelectedListener = null;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        setResult(Activity.RESULT_CANCELED);
        super.onBackPressed();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK)
            return;

        if (requestCode == REQUEST_CODE_PREVIEW) {
            Bundle resultBundle = data.getBundleExtra(BasePreviewActivity.EXTRA_RESULT_BUNDLE);
            ArrayList<Item> selected = resultBundle.getParcelableArrayList(SelectedItemCollection.STATE_SELECTION);
            mOriginalEnable = data.getBooleanExtra(BasePreviewActivity.EXTRA_RESULT_ORIGINAL_ENABLE, false);
            int collectionType = resultBundle.getInt(SelectedItemCollection.STATE_COLLECTION_TYPE,
                    SelectedItemCollection.COLLECTION_UNDEFINED);
            if (data.getBooleanExtra(BasePreviewActivity.EXTRA_RESULT_APPLY, false)) {
                Intent result = new Intent();
                ArrayList<Uri> selectedUris = new ArrayList<>();
                ArrayList<String> selectedPaths = new ArrayList<>();
                if (selected != null) {
                    for (Item item : selected) {
                        selectedUris.add(item.getContentUri());
                        selectedPaths.add(PathUtils.getPath(this, item.getContentUri()));
                    }
                }
                result.putParcelableArrayListExtra(EXTRA_RESULT_SELECTION, selectedUris);
                result.putStringArrayListExtra(EXTRA_RESULT_SELECTION_PATH, selectedPaths);
                result.putParcelableArrayListExtra(EXTRA_RESULT_SELECTION_ITEMS, selected);
                result.putExtra(EXTRA_RESULT_ORIGINAL_ENABLE, mOriginalEnable);
                setResult(RESULT_OK, result);
                finish();
            } else {
                mSelectedCollection.overwrite(selected, collectionType);
                Fragment mediaSelectionFragment = getSupportFragmentManager().findFragmentByTag(
                        MediaSelectionFragment.class.getSimpleName());
                if (mediaSelectionFragment instanceof MediaSelectionFragment) {
                    ((MediaSelectionFragment) mediaSelectionFragment).refreshMediaGrid();
                }
                updateBottomToolbar();
            }
        } else if (requestCode == REQUEST_CODE_CAPTURE) {
            // Just pass the data back to previous calling Activity.
            Uri contentUri = mMediaStoreCompat.getCurrentPhotoUri();
            String path = mMediaStoreCompat.getCurrentPhotoPath();
            ArrayList<Uri> selected = new ArrayList<>();
            selected.add(contentUri);
            ArrayList<String> selectedPath = new ArrayList<>();
            selectedPath.add(path);
            if (mSpec.crop) {
                startCrop(this, contentUri, Uri.fromFile(newTempImageFile()), getColorPrimary(this), mAlbumTitle.getTextColors().getDefaultColor(), mSpec.cropWHRatio, mSpec.cropWHMax);
            } else {
                Intent result = new Intent();
                result.putParcelableArrayListExtra(EXTRA_RESULT_SELECTION, selected);
                result.putStringArrayListExtra(EXTRA_RESULT_SELECTION_PATH, selectedPath);
                result.putParcelableArrayListExtra(EXTRA_RESULT_SELECTION_ITEMS, selected);
                setResult(RESULT_OK, result);
                finish();
            }
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP)
                MatisseActivity.this.revokeUriPermission(contentUri,
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);

            new SingleMediaScanner(this.getApplicationContext(), path, new SingleMediaScanner.ScanListener() {
                @Override
                public void onScanFinish() {

                }
            });
        } else if (requestCode == Crop.REQUEST_CROP) {
            Uri cropped = data.getParcelableExtra(Crop.EXTRA_OUTPUT_URI);
            Intent result = new Intent();
            ArrayList<Uri> selectedUris = new ArrayList<>();
            ArrayList<String> selectedPaths = new ArrayList<>();
            selectedUris.add(cropped);
            selectedPaths.add(PathUtils.getPath(this, cropped));
            result.putParcelableArrayListExtra(EXTRA_RESULT_SELECTION, selectedUris);
            result.putStringArrayListExtra(EXTRA_RESULT_SELECTION_PATH, selectedPaths);
            result.putExtra(EXTRA_RESULT_ORIGINAL_ENABLE, mOriginalEnable);
            setResult(RESULT_OK, result);
            finish();
        }
    }

    private void updateBottomToolbar() {
        if (mSpec.crop) {
            mBottomToolbar.setVisibility(GONE);
        } else {
            mBottomToolbar.setVisibility(VISIBLE);
            int selectedCount = mSelectedCollection.count();
            if (selectedCount == 0) {
                mButtonPreview.setEnabled(false);
                mButtonApply.setEnabled(false);
                mButtonApply.setText(getString(R.string.button_apply_default));
            } else if (selectedCount == 1 && mSpec.singleSelectionModeEnabled()) {
                mButtonPreview.setEnabled(true);
                mButtonApply.setText(R.string.button_apply_default);
                mButtonApply.setEnabled(true);
            } else {
                mButtonPreview.setEnabled(true);
                mButtonApply.setEnabled(true);
                mButtonApply.setText(getString(R.string.button_apply, selectedCount));
            }


            if (mSpec.originalable) {
//            mOriginalLayout.setVisibility(View.VISIBLE);
                updateOriginalState();
            } else {
//            mOriginalLayout.setVisibility(View.INVISIBLE);
            }

        }
    }


    private void updateOriginalState() {
        mOriginal.setChecked(mOriginalEnable);
        if (countOverMaxSize() > 0) {

            if (mOriginalEnable) {
                IncapableDialog incapableDialog = IncapableDialog.newInstance("",
                        getString(R.string.error_over_original_size, mSpec.originalMaxSize));
                incapableDialog.show(getSupportFragmentManager(),
                        IncapableDialog.class.getName());

                mOriginal.setChecked(false);
                mOriginalEnable = false;
            }
        }
    }


    private int countOverMaxSize() {
        int count = 0;
        int selectedCount = mSelectedCollection.count();
        for (int i = 0; i < selectedCount; i++) {
            Item item = mSelectedCollection.asList().get(i);

            if (item.isImage()) {
                float size = PhotoMetadataUtils.getSizeInMB(item.size);
                if (size > mSpec.originalMaxSize) {
                    count++;
                }
            }
        }
        return count;
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.button_preview) {
            Intent intent = new Intent(this, SelectedPreviewActivity.class);
            intent.putExtra(BasePreviewActivity.EXTRA_DEFAULT_BUNDLE, mSelectedCollection.getDataWithBundle());
            intent.putExtra(BasePreviewActivity.EXTRA_RESULT_ORIGINAL_ENABLE, mOriginalEnable);
            startActivityForResult(intent, REQUEST_CODE_PREVIEW);
        } else if (v.getId() == R.id.button_apply) {
            Intent result = new Intent();
            ArrayList<Uri> selectedUris = (ArrayList<Uri>) mSelectedCollection.asListOfUri();
            result.putParcelableArrayListExtra(EXTRA_RESULT_SELECTION, selectedUris);
            ArrayList<String> selectedPaths = (ArrayList<String>) mSelectedCollection.asListOfString();
            result.putStringArrayListExtra(EXTRA_RESULT_SELECTION_PATH, selectedPaths);
            result.putParcelableArrayListExtra(EXTRA_RESULT_SELECTION_ITEMS, new ArrayList<>(mSelectedCollection.asList()));
            result.putExtra(EXTRA_RESULT_ORIGINAL_ENABLE, mOriginalEnable);
            setResult(RESULT_OK, result);
            finish();
        } else if (v.getId() == R.id.originalLayout) {
            int count = countOverMaxSize();
            if (count > 0) {
                IncapableDialog incapableDialog = IncapableDialog.newInstance("",
                        getString(R.string.error_over_original_count, count, mSpec.originalMaxSize));
                incapableDialog.show(getSupportFragmentManager(),
                        IncapableDialog.class.getName());
                return;
            }

            mOriginalEnable = !mOriginalEnable;
            mOriginal.setChecked(mOriginalEnable);

            if (mSpec.onCheckedListener != null) {
                mSpec.onCheckedListener.onCheck(mOriginalEnable);
            }
        } else if (v.getId() == R.id.album_title_layout) {
            if (mAlbumListLayout.getVisibility() == GONE) {
                showSpinner();
            } else {
                closeSpinner();
            }
        } else if (v.getId() == R.id.album_list_layout) {
            closeSpinner();
        } else if (v.getId() == R.id.album_title_cancel) {
            onBackPressed();
        }
    }


    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        mAlbumCollection.setStateCurrentSelection(position);
        mAlbumsAdapter.getCursor().moveToPosition(position);
        Album album = Album.valueOf(mAlbumsAdapter.getCursor());
        mAlbumsAdapter.moveToPosition(position);
        if (album.isAll() && SelectionSpec.getInstance().capture) {
            album.addCaptureCount();
        }
        onAlbumSelected(album);
    }

    @Override
    public void onAlbumLoad(final Cursor cursor) {
        mAlbumsAdapter.swapCursor(cursor);
        // select default album.
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {

            @Override
            public void run() {
                cursor.moveToPosition(mAlbumCollection.getCurrentSelection());
//                mAlbumsSpinner.setSelection(MatisseActivity.this,
//                        mAlbumCollection.getCurrentSelection());
                Album album = Album.valueOf(cursor);
                if (album.isAll() && SelectionSpec.getInstance().capture) {
                    album.addCaptureCount();
                }
                onAlbumSelected(album);
            }
        });
    }

    @Override
    public void onAlbumReset() {
        mAlbumsAdapter.swapCursor(null);
    }

    private void onAlbumSelected(Album album) {
        if (mAlbumListLayout.getVisibility() == VISIBLE) {
            closeSpinner();
        }
        if (album.isAll() && album.isEmpty()) {
            mContainer.setVisibility(GONE);
            mEmptyView.setVisibility(VISIBLE);
        } else {
            mContainer.setVisibility(VISIBLE);
            mEmptyView.setVisibility(GONE);
            if (fragment == null) {
                fragment = MediaSelectionFragment.newInstance(album);
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.container, fragment, MediaSelectionFragment.class.getSimpleName())
                        .commitAllowingStateLoss();
            } else {
                viewModel.getAlbumData().postValue(album);
            }
        }
    }

    @Override
    public void onUpdate() {
        // notify bottom toolbar that check state changed.
        updateBottomToolbar();

        if (mSpec.onSelectedListener != null) {
            mSpec.onSelectedListener.onSelected(mSelectedCollection.asList(),
                    mSelectedCollection.asListOfUri(), mSelectedCollection.asListOfString());
        }
    }


    private File newTempImageFile() {
        String PATTERN = "yyyyMMddHHmmss";
        String timeStamp = new SimpleDateFormat(PATTERN, Locale.CHINA).format(new Date());
        return new File(FileManager.getInstance().getImageFolder(), timeStamp + ".jpg");
    }

    public static int getColorPrimary(Context context) {
        TypedValue typedValue = new TypedValue();
        context.getTheme().resolveAttribute(android.R.attr.colorPrimary, typedValue, true);
        return typedValue.data;
    }

    @Override
    public void onMediaClick(Album album, Item item, int adapterPosition) {
        if (mSpec.crop) {
            startCrop(this, item.uri, Uri.fromFile(newTempImageFile()), getColorPrimary(this), mAlbumTitle.getTextColors().getDefaultColor(), mSpec.cropWHRatio, mSpec.cropWHMax);
        } else {
            Intent intent = new Intent(this, AlbumPreviewActivity.class);
            intent.putExtra(AlbumPreviewActivity.EXTRA_ALBUM, album);
            intent.putExtra(AlbumPreviewActivity.EXTRA_ITEM, item);
            intent.putExtra(BasePreviewActivity.EXTRA_DEFAULT_BUNDLE, mSelectedCollection.getDataWithBundle());
            intent.putExtra(BasePreviewActivity.EXTRA_RESULT_ORIGINAL_ENABLE, mOriginalEnable);
            startActivityForResult(intent, REQUEST_CODE_PREVIEW);
        }
    }

    private static final int MAX_SIZE = 2400;

    public static void startCrop(Activity activity, Uri source, @NonNull Uri destination, int statusBarColor, int widgetColor, @NonNull Pair<Float, Float> cropWHRatio, Pair<Integer, Integer> cropWHMax) {
        int maxW, maxH;
        if (cropWHMax != null) {
            maxW = cropWHMax.getFirst();
            maxH = cropWHMax.getSecond();
        } else {
            maxW = maxH = MAX_SIZE;
        }
        Crop crop = Crop.of(source, destination)
                .withAspectRatio(cropWHRatio.getFirst(), cropWHRatio.getSecond())
                .withMaxResultSize(maxW, maxH);

        Crop.Options options = new Crop.Options();
        options.setToolbarColor(statusBarColor);
        options.setStatusBarColor(statusBarColor);
        options.setToolbarWidgetColor(widgetColor);
//        options.setActiveControlsWidgetColor(widgetColor);
        options.setActiveWidgetColor(widgetColor);
        crop.withOptions(options);
        crop.start(activity);
    }

    @Override
    public SelectedItemCollection provideSelectedItemCollection() {
        return mSelectedCollection;
    }

    @Override
    public void capture() {
        String[] permissions;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions = new String[]{Manifest.permission.CAMERA};
        } else {
            permissions = new String[]{Manifest.permission.CAMERA, READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE};
        }
        PermissionX.with(this).permissions(permissions)
                .onRequestRationale(getResources().getString(R.string.camera_permission_description)).request(result -> {
                    if (result.isAllGranted()) {
                        if (mMediaStoreCompat != null) {
                            if (mSpec.useEmbeddedCamera) {
                                mMediaStoreCompat.capture(this, REQUEST_CODE_CAPTURE);
                            } else {
                                mMediaStoreCompat.dispatchCaptureIntent(this, REQUEST_CODE_CAPTURE);
                            }
                        }
                    }
                });


    }

    @Override
    public void expandAuthorizedMedia() {
        String[] permissions;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            permissions = new String[]{READ_MEDIA_VISUAL_USER_SELECTED, READ_MEDIA_IMAGES, READ_MEDIA_VIDEO};
            PermissionX.with(this).permissions(permissions)
                    .onRequestRationale(getResources().getString(R.string.camera_permission_description)).request(result -> {
                        if (ContextCompat.checkSelfPermission(this, READ_MEDIA_VISUAL_USER_SELECTED) == PERMISSION_GRANTED) {
                            viewModel.refresh();
                        }
                        updateSettingUI();
                    });
        }
    }

    @Override
    public void finish() {
        super.finish();
//        overridePendingTransition(R.anim.abc_slide_in_bottom,
//                R.anim.slide_out_top);
        overridePendingTransition(R.anim.no_anim,
                com.nhsd.basic.R.anim.push_bottom_out);
    }
}
