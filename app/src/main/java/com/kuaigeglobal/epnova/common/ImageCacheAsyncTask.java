package com.kuaigeglobal.epnova.common;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Environment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.Target;
import com.kuaigeglobal.epnova.common.utils.FileUtils;
import com.kuaigeglobal.epnova.common.utils.Utils;

import java.io.File;

public class ImageCacheAsyncTask extends AsyncTask<String, Void, File> {

    private final Context context;

    public ImageCacheAsyncTask(Context context) {
        this.context = context;
    }

    @Override
    protected File doInBackground(String... params) {
        String imgUrl = params[0];
        try {
            File file = Glide.with(context)
                    .load(imgUrl)
                    .downloadOnly(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL)
                    .get();

            //获取到下载得到的图片，进行本地保存
            File pictureFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
            String fileName = System.currentTimeMillis() + ".jpg";
            File destFile = new File(pictureFolder, fileName);
            FileUtils.copy(file, destFile);
            file.delete();
            return destFile;
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }

    }

    @Override
    protected void onPostExecute(File result) {
        if (result == null) {
            return;
        }
        Utils.notifySystem(result);
    }

}