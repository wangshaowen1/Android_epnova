package com.nhsd.mattise.internal.ui;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.otaliastudios.cameraview.CameraListener;
import com.otaliastudios.cameraview.CameraView;
import com.otaliastudios.cameraview.FileCallback;
import com.otaliastudios.cameraview.MirrorPictureResult;
import com.otaliastudios.cameraview.PictureResult;
import com.otaliastudios.cameraview.controls.Facing;
import com.otaliastudios.cameraview.controls.Flash;
import com.zhihu.matisse.R;

import java.io.File;

public class CaptureActivity extends AppCompatActivity implements FileCallback, View.OnClickListener {

    private static final String SP_NAME = "matisse_inner_camera";
    private static final String KEY_FLASH = "flash_on";
    private static final String KEY_FRONT = "front_camera";

    public static final String KEY_EXTRA_OUTPUT = "front_camera";

    private CameraView camera;
    private ImageView btnFlash;
    private SharedPreferences sp;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_capture);
        String filePath = getIntent().getStringExtra(KEY_EXTRA_OUTPUT);
        File file = new File(filePath);
        getCamera().setLifecycleOwner(this);
        getCamera().addCameraListener(new CameraListener() {
            @Override
            public void onPictureTaken(@NonNull PictureResult result) {
                if (result.getFacing() == Facing.FRONT) {
                    MirrorPictureResult mirrorPictureResult = MirrorPictureResult.copy(result);
                    mirrorPictureResult.toFile(file, CaptureActivity.this);
                } else {
                    result.toFile(file, CaptureActivity.this);
                }
            }
        });
        applyFlash();
        applyCamera();
        getBtnFlash().setOnClickListener(this);
        findViewById(R.id.btnTakePhoto).setOnClickListener(this);
        findViewById(R.id.btnSwitch).setOnClickListener(this);
        findViewById(R.id.btnBack).setOnClickListener(this);
    }

    @Override
    public void onFileReady(@Nullable File file) {
        setResult(RESULT_OK);
        finish();
    }

    @Override
    public void onClick(View v) {
        if (R.id.btnFlash == v.getId()) {
            if (!isFrontCamera()) {
                setFlashOn(!isFlashOn());
            }
        } else if (R.id.btnTakePhoto == v.getId()) {
            getCamera().takePicture();
        } else if (R.id.btnSwitch == v.getId()) {
            switchCamera();
        } else if (R.id.btnBack == v.getId()) {
            finish();
        }
    }

    private CameraView getCamera() {
        if (camera == null) {
            camera = findViewById(R.id.camera);
        }
        return camera;
    }

    private ImageView getBtnFlash() {
        if (btnFlash == null) {
            btnFlash = findViewById(R.id.btnFlash);
        }
        return btnFlash;
    }

    private SharedPreferences getSp() {
        if (sp == null) {
            sp = getSharedPreferences(SP_NAME, MODE_PRIVATE);
        }
        return sp;
    }

    private boolean isFlashOn() {
        return getSp().getBoolean(KEY_FLASH, false);
    }

    private void setFlashOn(boolean flashOn) {
        getSp().edit().putBoolean(KEY_FLASH, flashOn).apply();
        applyFlash();
    }

    private void applyFlash() {
        boolean isFlashOn = isFlashOn();
        getCamera().setFlash(isFlashOn ? Flash.TORCH : Flash.OFF);
        getBtnFlash().setImageResource(isFlashOn ? R.drawable.baseline_flash_on_24 : R.drawable.baseline_flash_off_24);
    }

    private boolean isFrontCamera() {
        return getSp().getBoolean(KEY_FRONT, false);
    }

    private void switchCamera() {
        getSp().edit().putBoolean(KEY_FRONT, !isFrontCamera()).apply();
        applyCamera();
    }

    private void applyCamera() {
        boolean isFrontCamera = isFrontCamera();
        getCamera().setFacing(isFrontCamera ? Facing.FRONT : Facing.BACK);
        if (isFrontCamera) {
            setFlashOn(false);
        }
    }

}
