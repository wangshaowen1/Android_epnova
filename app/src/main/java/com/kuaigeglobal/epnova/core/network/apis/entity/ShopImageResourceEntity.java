package com.kuaigeglobal.epnova.core.network.apis.entity;

import android.os.Parcel;
import android.os.Parcelable;

public class ShopImageResourceEntity implements Parcelable {
    //{
    //            "dataType": "数据类型 1-图片 2-视频",
    //            "dataImgUrl": "数据图片路径",
    //            "dataVideoUrl": "数据视频路径"
    //        }

    private int dataType;
    private String dataImgUrl;
    private String dataVideoUrl;

    public ShopImageResourceEntity() {
    }

    public ShopImageResourceEntity(String dataImgUrl) {
        this.dataImgUrl = dataImgUrl;
    }

    protected ShopImageResourceEntity(Parcel in) {
        dataType = in.readInt();
        dataImgUrl = in.readString();
        dataVideoUrl = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(dataType);
        dest.writeString(dataImgUrl);
        dest.writeString(dataVideoUrl);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<ShopImageResourceEntity> CREATOR = new Creator<ShopImageResourceEntity>() {
        @Override
        public ShopImageResourceEntity createFromParcel(Parcel in) {
            return new ShopImageResourceEntity(in);
        }

        @Override
        public ShopImageResourceEntity[] newArray(int size) {
            return new ShopImageResourceEntity[size];
        }
    };

    public boolean isVideo() {
        return dataType == 2;
    }

    public int getDataType() {
        return dataType;
    }

    public void setDataType(int dataType) {
        this.dataType = dataType;
    }

    public String getDataImgUrl() {
        return dataImgUrl;
    }

    public void setDataImgUrl(String dataImgUrl) {
        this.dataImgUrl = dataImgUrl;
    }

    public String getDataVideoUrl() {
        return dataVideoUrl;
    }

    public void setDataVideoUrl(String dataVideoUrl) {
        this.dataVideoUrl = dataVideoUrl;
    }
}
