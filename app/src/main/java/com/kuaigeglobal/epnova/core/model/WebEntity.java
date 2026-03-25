package com.kuaigeglobal.epnova.core.model;

import android.os.Parcel;
import android.os.Parcelable;

public class WebEntity implements Parcelable {
    //{
    //shareType: 分享类型（1为网页分享）
    //title: 网页分享标题
    //icon: 网页分享图标（分享好友，朋友圈 图标）
    //content: 网页分享描述
    //path: 网页分享链接（分享好友，朋友圈 链接）,
    //timelineTitle: 朋友圈标题,
    //copyPath: 复制链接文本
    //}
    int shareType = 0;//  1：网页    2：小程序     3：图片  5：海报分享
    String icon;
    String title;// '网页标题',
    String content;
    String path;//网页分享链接（分享好友，朋友圈 链接）
    String timelineTitle;
    String copyPath;

    protected WebEntity(Parcel in) {
        shareType = in.readInt();
        icon = in.readString();
        title = in.readString();
        content = in.readString();
        path = in.readString();
        timelineTitle = in.readString();
        copyPath = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(shareType);
        dest.writeString(icon);
        dest.writeString(title);
        dest.writeString(content);
        dest.writeString(path);
        dest.writeString(timelineTitle);
        dest.writeString(copyPath);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<WebEntity> CREATOR = new Creator<WebEntity>() {
        @Override
        public WebEntity createFromParcel(Parcel in) {
            return new WebEntity(in);
        }

        @Override
        public WebEntity[] newArray(int size) {
            return new WebEntity[size];
        }
    };

    public int getShareType() {
        return shareType;
    }

    public void setShareType(int shareType) {
        this.shareType = shareType;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getTimelineTitle() {
        return timelineTitle;
    }

    public void setTimelineTitle(String timelineTitle) {
        this.timelineTitle = timelineTitle;
    }

    public String getCopyPath() {
        return copyPath;
    }

    public void setCopyPath(String copyPath) {
        this.copyPath = copyPath;
    }
}