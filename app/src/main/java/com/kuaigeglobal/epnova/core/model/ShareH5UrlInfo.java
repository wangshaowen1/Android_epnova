package com.kuaigeglobal.epnova.core.model;

import android.os.Parcel;
import android.os.Parcelable;

/*分享话题*/
public class ShareH5UrlInfo implements Parcelable {
    private String title;
    private String desc;
    private String thumb;
    private String url;

    private boolean deletable;
    private boolean editVisible;
    private boolean evaluateSettingVisible;
    private boolean complaintEnable;
    private boolean miQuanShareEnable;
    private TopStatus topStatus = TopStatus.None;

    //分享的数据的id（新增）：userShareDataItemType为1时传剧本id
    private String id;
    //即sharebody的userShareDataItemType 分享的数据的类型（新增2-10）： 1-分享平台剧本，2-圈子，3-组局，4-动态，5-剧评，6-玩家名片，7-邀请玩家，8-榜单，9-剧本集，10-剧本墙
    private int shareDataType;
    //即sharebody的userShareLaunchPosition："用户分享发起的位置 1-五折活动界面 2-剧本数据海报界面（不用了，使用类型4） 3-助力活动界面 4-剧本详情界面 5-圈子详情界面，6-组局详情界面，7-动态详情界面，8-剧评详情界面，9-玩家名片界面，10-邀请玩家界面，11-剧本榜单界面，12-剧本集界面 13-剧本墙",
    private int shareLocation;
    private  boolean inReview;
    private String reviewHint;
    public enum TopStatus {
        None, Top, CancelTop
    }

    public ShareH5UrlInfo(String title, String desc, String thumb, String url) {
        this(title, desc, thumb, url, false, false, TopStatus.None);
    }

    public ShareH5UrlInfo(String title, String desc, String thumb, String url, boolean deletable, boolean complaintEnable) {
        this(title, desc, thumb, url, deletable, complaintEnable, TopStatus.None);
    }

    public ShareH5UrlInfo(String title, String desc, String thumb, String url, boolean deletable, boolean complaintEnable, TopStatus topStatus) {
        this(title, desc, thumb, url, false, deletable, complaintEnable, topStatus);
    }

    public ShareH5UrlInfo(String title, String desc, String thumb, String url, boolean editVisible, boolean deletable, boolean complaintEnable, TopStatus topStatus) {
        this.title = title;
        this.desc = desc;
        this.thumb = thumb;
        this.url = url;
        this.deletable = deletable;
        this.editVisible = editVisible;
        this.complaintEnable = complaintEnable;
        this.topStatus = topStatus;
    }


    protected ShareH5UrlInfo(Parcel in) {
        title = in.readString();
        desc = in.readString();
        thumb = in.readString();
        url = in.readString();
        deletable = in.readByte() != 0;
        editVisible = in.readByte() != 0;
        evaluateSettingVisible = in.readByte() != 0;
        complaintEnable = in.readByte() != 0;
        miQuanShareEnable = in.readByte() != 0;
        id = in.readString();
        shareDataType = in.readInt();
        shareLocation = in.readInt();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(title);
        dest.writeString(desc);
        dest.writeString(thumb);
        dest.writeString(url);
        dest.writeByte((byte) (deletable ? 1 : 0));
        dest.writeByte((byte) (editVisible ? 1 : 0));
        dest.writeByte((byte) (evaluateSettingVisible ? 1 : 0));
        dest.writeByte((byte) (complaintEnable ? 1 : 0));
        dest.writeByte((byte) (miQuanShareEnable ? 1 : 0));
        dest.writeString(id);
        dest.writeInt(shareDataType);
        dest.writeInt(shareLocation);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<ShareH5UrlInfo> CREATOR = new Creator<ShareH5UrlInfo>() {
        @Override
        public ShareH5UrlInfo createFromParcel(Parcel in) {
            return new ShareH5UrlInfo(in);
        }

        @Override
        public ShareH5UrlInfo[] newArray(int size) {
            return new ShareH5UrlInfo[size];
        }
    };

    public boolean isMiQuanShareEnable() {
        return miQuanShareEnable;
    }

    public void setMiQuanShareEnable(boolean miQuanShareEnable) {
        this.miQuanShareEnable = miQuanShareEnable;
    }

    public int getShareLocation() {
        return shareLocation;
    }

    public void setShareLocation(int shareLocation) {
        this.shareLocation = shareLocation;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getShareDataType() {
        return shareDataType;
    }

    public void setShareDataType(int shareDataType) {
        this.shareDataType = shareDataType;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public String getThumb() {
        return thumb;
    }

    public void setThumb(String thumb) {
        this.thumb = thumb;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public boolean isDeletable() {
        return deletable;
    }

    public void setDeletable(boolean deletable) {
        this.deletable = deletable;
    }

    public boolean isComplaintEnable() {
        return complaintEnable;
    }

    public void setComplaintEnable(boolean complaintEnable) {
        this.complaintEnable = complaintEnable;
    }

    public TopStatus getTopStatus() {
        return topStatus;
    }

    public void setTopStatus(TopStatus topStatus) {
        this.topStatus = topStatus;
    }

    public boolean isEditVisible() {
        return editVisible;
    }

    public void setEditVisible(boolean editVisible) {
        this.editVisible = editVisible;
    }

    public boolean isEvaluateSettingVisible() {
        return evaluateSettingVisible;
    }

    public void setEvaluateSettingVisible(boolean evaluateSettingVisible) {
        this.evaluateSettingVisible = evaluateSettingVisible;
    }

    public boolean isInReview() {
        return inReview;
    }

    public void setInReview(boolean inReview) {
        this.inReview = inReview;
    }

    public String getReviewHint() {
        return reviewHint;
    }

    public void setReviewHint(String reviewHint) {
        this.reviewHint = reviewHint;
    }
}
