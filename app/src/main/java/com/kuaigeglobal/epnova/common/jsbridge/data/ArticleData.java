package com.kuaigeglobal.epnova.common.jsbridge.data;

import android.os.Parcel;
import android.os.Parcelable;

public class ArticleData implements Parcelable {
    private String articleId;
    private String articleTitle;
    private String articleThumbnailsUrl;
    private String articleOverview;
    public String getArticleId() {
        return articleId;
    }

    public void setArticleId(String articleId) {
        this.articleId = articleId;
    }

    public String getArticleTitle() {
        return articleTitle;
    }

    public void setArticleTitle(String articleTitle) {
        this.articleTitle = articleTitle;
    }

    public String getArticleThumbnailsUrl() {
        return articleThumbnailsUrl;
    }

    public void setArticleThumbnailsUrl(String articleThumbnailsUrl) {
        this.articleThumbnailsUrl = articleThumbnailsUrl;
    }

    public String getArticleOverview() {
        return articleOverview;
    }

    public void setArticleOverview(String articleOverview) {
        this.articleOverview = articleOverview;
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.articleId);
        dest.writeString(this.articleTitle);
        dest.writeString(this.articleThumbnailsUrl);
        dest.writeString(this.articleOverview);
    }

    public ArticleData() {
    }

    protected ArticleData(Parcel in) {
        this.articleId = in.readString();
        this.articleTitle = in.readString();
        this.articleThumbnailsUrl = in.readString();
        this.articleOverview = in.readString();
    }

    public static final Creator<ArticleData> CREATOR = new Creator<ArticleData>() {
        @Override
        public ArticleData createFromParcel(Parcel source) {
            return new ArticleData(source);
        }

        @Override
        public ArticleData[] newArray(int size) {
            return new ArticleData[size];
        }
    };
}
