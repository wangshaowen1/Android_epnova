package com.kuaigeglobal.epnova.core.model;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

public class PhotoItem implements Parcelable {

    protected String id;
    protected String thumbUri;
    protected String uri;
    protected boolean aiChip;

    public PhotoItem() {
    }

    public PhotoItem(String thumbUri, String uri) {
        this.thumbUri = thumbUri;
        this.uri = uri;
    }

    public PhotoItem(String thumbUri, String uri,boolean aiChip) {
        this.thumbUri = thumbUri;
        this.uri = uri;
        this.aiChip = aiChip;
    }

    public PhotoItem(String id) {
        this.id = id;
    }

    protected PhotoItem(Parcel in) {
        id = in.readString();
        thumbUri = in.readString();
        uri = in.readString();
        aiChip = in.readByte() != 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(thumbUri);
        dest.writeString(uri);
        dest.writeByte((byte) (aiChip ? 1 : 0));
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<PhotoItem> CREATOR = new Creator<PhotoItem>() {
        @Override
        public PhotoItem createFromParcel(Parcel in) {
            return new PhotoItem(in);
        }

        @Override
        public PhotoItem[] newArray(int size) {
            return new PhotoItem[size];
        }
    };

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getThumbUri() {
        if(TextUtils.equals(uri,thumbUri)){
            return null;
        }
        return thumbUri;
    }

    public void setThumbUri(String thumbUri) {
        this.thumbUri = thumbUri;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public boolean isAiChip() {
        return aiChip;
    }

    public void setAiChip(boolean aiChip) {
        this.aiChip = aiChip;
    }
}
