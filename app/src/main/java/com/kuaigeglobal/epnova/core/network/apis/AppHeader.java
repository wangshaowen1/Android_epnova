package com.kuaigeglobal.epnova.core.network.apis;

import android.text.TextUtils;
import android.util.Base64;

import com.blankj.utilcode.util.ConvertUtils;
import com.github.gzuliyujiang.oaid.DeviceID;
import com.kuaigeglobal.epnova.App;
import com.kuaigeglobal.epnova.common.Gaid;
import com.kuaigeglobal.epnova.common.utils.EncryptUtils;
import com.kuaigeglobal.epnova.core.Config;
import com.kuaigeglobal.epnova.core.manager.AdStrategyManager;
import com.nhsd.basic.Session;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

public class AppHeader {
    public static final String DEFAULT_CHANNEL_ID = "1";
    private int emulatorStatus;
    //APP版本
    private String appVersion;
    // 手机类型 0 iphone 1 安卓
    private String mobileType;
    // 手机唯一编号
    private String mobileCode;
    //安卓系统版本
    private String mobileSys;
    //品牌
    private String mobileCtSys;
    //渠道ID
    private String channelId;
    //渠道
    private String channelCode;
    //手机型号
    private String mobileMode;
    //androidId
    private String macAddress;
    //oaid
    private String oaidCode;
    private String mobileDeviceId;
    //AppsFlyer 投放渠道标记
    private String regChannelCode;
    //bd_did
    private String bdDid;
    /**
     * openinstall 广告追踪用到的参数
     */
    private String opid;
    /**
     * 马甲包类型
     */
    private int appType = 0;
    /**
     * 设备指纹
     */
    private String deviceFingerprint;
    private String riskKey;
    /**
     * 商店区域
     */
    private String storeCountryCode;

    /**
     * 获取手机系统地区的，注册国家使用的
     */
    private String systemCountryCode;

    public AppHeader() {
        super();
        this.appVersion = Session.getInstance().getVersionName();
        //String deviceId = Config.get() != null ? Config.get().getDeviceToken():null;
        if (App.getInstance().hasAgree()) {
            String androidId = Session.getInstance().getAndroidId();
            this.mobileCode = EncryptUtils.md5(androidId, Session.getInstance().getMac());
            this.mobileMode = Session.getInstance().getModel();
            this.mobileCtSys = Session.getInstance().getDeviceBrand();
            this.mobileSys = Session.getInstance().getOsVersion();
            this.opid = Config.get().getOpid();
            this.regChannelCode = Config.get().getRegChannelCode();
            this.storeCountryCode = Config.get().getStoreCountryCode();
            this.systemCountryCode = Locale.getDefault().getCountry();


            String mac = Session.getInstance().getMac();
            String gaid = Gaid.safeGetGaid();
            String mobileDeviceId = DeviceID.getClientId();
            String imei = Session.getInstance().getImei();
            String riskKey = getRandomLowerCaseLetter() + androidId
                    + "&" + (TextUtils.isEmpty(gaid) ? "@@" : gaid)
                    + "&" + (TextUtils.isEmpty(mac) ? "@@" : mac)
                    + "&" + (TextUtils.isEmpty(mobileDeviceId) ? "@@" : mobileDeviceId)
                    + "&" + (TextUtils.isEmpty(bdDid) ? "@@" : bdDid)
                    + "&" + (TextUtils.isEmpty(imei) ? "@@" : imei);
            byte[] keyByte = Base64.encode(riskKey.getBytes(StandardCharsets.UTF_8), Base64.NO_WRAP);
            String keyString = ConvertUtils.bytes2String(keyByte);
            this.riskKey = getRandomLowerCaseLetter() + keyString;
        }
        this.mobileType = String.valueOf(1);
        this.channelId = AdStrategyManager.getDefaultObjectId();//取本地objectId，默认1
        this.channelCode = Session.getInstance().getChannel();
    }

    public static char getRandomLowerCaseLetter() {
        // 生成 a-z 之间的随机字母
        return (char) ThreadLocalRandom.current().nextInt('a', 'z' + 1);
    }

    public String getChannelId() {
        return channelId;
    }

    public void setChannelId(String channelId) {
        this.channelId = channelId;
    }

    public String getAppVersion() {
        return appVersion;
    }

    public void setAppVersion(String version) {
        this.appVersion = version;
    }

    public String getMobileCode() {
        return mobileCode;
    }

    public void setMobileCode(String mobileCode) {
        this.mobileCode = mobileCode;
    }

    public String getMobileType() {
        return mobileType;
    }

    public void setMobileType(String mobileType) {
        this.mobileType = mobileType;
    }

    public String getMobileMode() {
        return mobileMode;
    }

    public void setMobileMode(String mobileMode) {
        this.mobileMode = mobileMode;
    }

    public String getMobileSys() {
        return mobileSys;
    }

    public void setMobileSys(String mobileSys) {
        this.mobileSys = mobileSys;
    }

    public String getMobileCtSys() {
        return mobileCtSys;
    }

    public void setMobileCtSys(String mobileCtSys) {
        this.mobileCtSys = mobileCtSys;
    }

    public String getChannelCode() {
        return channelCode;
    }

    public void setChannelCode(String channelCode) {
        this.channelCode = channelCode;
    }

    public String getMacAddress() {
        return macAddress;
    }

    public void setMacAddress(String macAddress) {
        this.macAddress = macAddress;
    }


    public String getMobileDeviceId() {
        return mobileDeviceId;
    }

    public int getEmulatorStatus() {
        return emulatorStatus;
    }

    public void setEmulatorStatus(int emulatorStatus) {
        this.emulatorStatus = emulatorStatus;
    }

    public String getOaidCode() {
        return oaidCode;
    }

    public void setOaidCode(String oaidCode) {
        this.oaidCode = oaidCode;
    }

    public void setMobileDeviceId(String mobileDeviceId) {
        this.mobileDeviceId = mobileDeviceId;
    }

    public String getBdDid() {
        return bdDid;
    }

    public String getDeviceFingerprint() {
        return deviceFingerprint;
    }
}
