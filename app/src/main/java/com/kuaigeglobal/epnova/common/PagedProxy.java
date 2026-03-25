package com.kuaigeglobal.epnova.common;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import com.nhsd.basic.utils.Logger;

public class PagedProxy implements Parcelable {
    public static final int DEFAULT_PAGE_SIZE = 10;
    private int mDataCount = 0;
    private int mPageCount = 0;
    private boolean mReachEnd = false;
    //
    private int mCurrentPage = 0;
    private int mPageSize = DEFAULT_PAGE_SIZE;
    private boolean mPageCountSet = false;

    public PagedProxy() {

    }

    public PagedProxy(int pageSize) {
//        this.pag
        this.mPageSize = pageSize;
    }


    protected PagedProxy(Parcel in) {
        mDataCount = in.readInt();
        mPageCount = in.readInt();
        mReachEnd = in.readByte() != 0;
        mCurrentPage = in.readInt();
        mPageSize = in.readInt();
        mPageCountSet = in.readByte() != 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(mDataCount);
        dest.writeInt(mPageCount);
        dest.writeByte((byte) (mReachEnd ? 1 : 0));
        dest.writeInt(mCurrentPage);
        dest.writeInt(mPageSize);
        dest.writeByte((byte) (mPageCountSet ? 1 : 0));
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<PagedProxy> CREATOR = new Creator<PagedProxy>() {
        @Override
        public PagedProxy createFromParcel(Parcel in) {
            return new PagedProxy(in);
        }

        @Override
        public PagedProxy[] newArray(int size) {
            return new PagedProxy[size];
        }
    };

    public int reset() {
        this.setCurrentPage(1);
        this.setPageSize(mPageSize);
        this.mPageCountSet = false;
        this.mReachEnd = false;
        this.mDataCount = 0;
        this.mPageCount = 0;
        return mCurrentPage;
    }

    public final int getDataCount() {
        return this.mDataCount;
    }

    public final void setDataCount(int dataCount) {
        this.mDataCount = dataCount;
        this.mPageCount = (dataCount + mPageSize - 1) / mPageSize;
        this.mPageCountSet = true;
    }

    public int getPageCount() {
        return this.mPageCount;
    }

    public void setPageCount(int pageCount) {
        this.mPageCount = pageCount;
        this.mPageCountSet = true;
    }

    public boolean hasNextPage() {
        boolean hasNext = this.mReachEnd ? false : !this.mPageCountSet || mCurrentPage < this.mPageCount;
        return hasNext;
    }

    public void setReachEnd(boolean reachEnd) {
        this.mReachEnd = reachEnd;
    }

    public void setPageSize(int pageSize) {
        this.mPageSize = pageSize;
    }

    public int getPageSize() {
        return mPageSize;
    }

    public void setCurrentPage(int currentPage) {
        if (currentPage < 0) {
            throw new IllegalArgumentException();
        } else {
            this.mCurrentPage = currentPage;
        }
    }

    public boolean isFirstPage() {
        return mCurrentPage == 1;
    }

    public int getCurrentPage() {
        return mCurrentPage;
    }

    public int toNextPage() {
        return ++mCurrentPage;
    }

    public int toPrePage() {
        return --mCurrentPage;
    }

    public boolean isPageCountSet() {
        return mPageCountSet;
    }

}
