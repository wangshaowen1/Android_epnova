package com.kuaigeglobal.epnova.core.model;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

public class PhotoItem implements Parcelable {

    protected String id;
    protected String thumbUri;
    protected String uri;

    public PhotoItem(String thumbUri, String uri) {
        this.thumbUri = thumbUri;
        this.uri = uri;
    }

    public PhotoItem(String id) {
        this.id = id;
    }

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

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.id);
        dest.writeString(this.thumbUri);
        dest.writeString(this.uri);
    }

    public PhotoItem() {
    }

    protected PhotoItem(Parcel in) {
        this.id = in.readString();
        this.thumbUri = in.readString();
        this.uri = in.readString();
    }

    public static final Creator<PhotoItem> CREATOR = new Creator<PhotoItem>() {
        @Override
        public PhotoItem createFromParcel(Parcel source) {
            return new PhotoItem(source);
        }

        @Override
        public PhotoItem[] newArray(int size) {
            return new PhotoItem[size];
        }
    };
}
