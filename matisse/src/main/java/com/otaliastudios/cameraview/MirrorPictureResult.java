package com.otaliastudios.cameraview;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.otaliastudios.cameraview.internal.WorkerHandler;

import java.io.ByteArrayOutputStream;
import java.io.File;

public class MirrorPictureResult extends PictureResult {

    public static MirrorPictureResult copy(@NonNull PictureResult result) {
        Stub stub = new Stub();
        stub.data = result.getData();
        stub.facing = result.getFacing();
        stub.size = result.getSize();
        stub.location = result.getLocation();
        stub.format = result.getFormat();
        stub.isSnapshot = result.isSnapshot();
        stub.rotation = result.getRotation();
        return new MirrorPictureResult(stub);
    }

    public MirrorPictureResult(@NonNull Stub builder) {
        super(builder);
    }

    @Override
    public void toFile(@NonNull File file, @NonNull FileCallback callback) {
        toBitmap(bitmap -> WorkerHandler.execute(() -> {
            try {
                Bitmap mirrorBitmap = createMirrorBitmap(bitmap);
                byte[] data = bitmap2Bytes(mirrorBitmap, Bitmap.CompressFormat.JPEG, 100);
                CameraUtils.writeToFile(data, file, callback);
                bitmap.recycle();
                mirrorBitmap.recycle();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }));
    }

    public Bitmap createMirrorBitmap(Bitmap originalBitmap) {
        Bitmap mirrorBitmap = Bitmap.createBitmap(originalBitmap.getWidth(), originalBitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(mirrorBitmap);
        Matrix matrix = new Matrix();
        matrix.setScale(-1, 1); // 水平翻转
        matrix.postTranslate(originalBitmap.getWidth(), 0); // 平移
        canvas.drawBitmap(originalBitmap, matrix, null);
        return mirrorBitmap;
    }

    /**
     * Bitmap to bytes.
     *
     * @param bitmap  The bitmap.
     * @param format  The format of bitmap.
     * @param quality The quality.
     * @return bytes
     */
    public byte[] bitmap2Bytes(@Nullable final Bitmap bitmap, @NonNull final Bitmap.CompressFormat format, int quality) {
        if (bitmap == null) return null;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(format, quality, baos);
        return baos.toByteArray();
    }

}
