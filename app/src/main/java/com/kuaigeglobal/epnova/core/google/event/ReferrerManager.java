package com.kuaigeglobal.epnova.core.google.event;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import com.android.installreferrer.api.InstallReferrerClient;
import com.android.installreferrer.api.InstallReferrerStateListener;
import com.android.installreferrer.api.ReferrerDetails;
import com.nhsd.basic.utils.Logger;
import com.kuaigeglobal.epnova.core.Config;

import java.net.URLEncoder;

public class ReferrerManager {
    private InstallReferrerClient referrerClient;
    private int retryCount = 0;
    private static final int MAX_RETRY = 3; // 最大重试次数
    private boolean isRetrying = false;

    private Handler handler = new Handler(Looper.getMainLooper());

    public void init(Context context) {
        if (TextUtils.isEmpty(Config.get().getReferrer())) {
            connect(context);
        }else {
            destroy();
        }
    }

    private void connect(Context context) {
        try {
            if (referrerClient != null) {
                referrerClient.endConnection();
            }
            referrerClient = InstallReferrerClient.newBuilder(context).build();
            referrerClient.startConnection(new InstallReferrerStateListener() {
                @Override
                public void onInstallReferrerSetupFinished(int responseCode) {
                    isRetrying = false;
                    switch (responseCode) {
                        case InstallReferrerClient.InstallReferrerResponse.OK:
                            // 连接成功，可以安全获取数据
                            retryCount = 0; // 重置重试计数
                            getReferrer();
                            break;
                        case InstallReferrerClient.InstallReferrerResponse.FEATURE_NOT_SUPPORTED:
                            Logger.w("Referrer", "InstallReferrer not supported on this device");
                            destroy();
                            break;
                        case InstallReferrerClient.InstallReferrerResponse.SERVICE_UNAVAILABLE:
                            Logger.w("Referrer", "InstallReferrer service unavailable, retrying...");
                            retry(context);
                            break;
                    }
                }

                @Override
                public void onInstallReferrerServiceDisconnected() {
                    Logger.w("Referrer", "Service disconnected, retrying...");
                    retry(context);
                }
            });
        } catch (Exception e) {
            retry(context);
            e.printStackTrace();
        }
    }

    private void retry(Context context) {
        if (isRetrying || retryCount >= MAX_RETRY) {
            Logger.e("Referrer", "Max retry reached or already retrying, giving up.");
            destroy();
            return;
        }
        isRetrying = true;
        retryCount++;
        long delay = (long) Math.pow(2, retryCount - 1) * 1000L; // 1s, 2s, 4s
        handler.postDelayed(() -> connect(context), delay);
    }

    private void getReferrer() {
        try {
            ReferrerDetails response = referrerClient.getInstallReferrer();
            String referrerUrl = response.getInstallReferrer();
            long clickTime = response.getReferrerClickTimestampSeconds();
            long installTime = response.getInstallBeginTimestampSeconds();
            boolean instantExperience = response.getGooglePlayInstantParam();
            String installVersion = response.getInstallVersion();
            long installBeginTimestampServerSeconds = response.getInstallBeginTimestampServerSeconds();
            long referrerClickTimestampServerSeconds = response.getReferrerClickTimestampServerSeconds();

            if (!TextUtils.isEmpty(referrerUrl)) {
                Config.get().storeReferrer(URLEncoder.encode(referrerUrl, "UTF-8"));
            }

            Logger.d("Referrer", "instantExperience=" + instantExperience);
            Logger.d("Referrer", "installVersion=" + installVersion);
            Logger.d("Referrer", "referrerUrl=" + referrerUrl);
            Logger.d("Referrer", "clickTime=" + clickTime);
            Logger.d("Referrer", "installTime=" + installTime);
            Logger.d("Referrer", "installBeginTimestampServerSeconds=" + installBeginTimestampServerSeconds);
            Logger.d("Referrer", "referrerClickTimestampServerSeconds=" + referrerClickTimestampServerSeconds);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            destroy();
        }
    }

    public void destroy() {
        if (referrerClient != null) {
            referrerClient.endConnection();
            referrerClient = null;
        }
        handler.removeCallbacksAndMessages(null);
        handler = null;
    }
}
