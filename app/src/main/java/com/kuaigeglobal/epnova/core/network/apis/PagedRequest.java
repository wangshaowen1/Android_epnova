package com.kuaigeglobal.epnova.core.network.apis;


import com.kuaigeglobal.epnova.core.User;

public class PagedRequest {

    /**
     * uniqueId : 7210e8d9
     * pageNum : 1
     * pageSize : 3
     */

    private String userToken;
    private int pageNum;
    private int pageSize;


    public PagedRequest() {
        this.userToken = User.get().getId();
    }

    public PagedRequest(int pageNum, int pageSize) {
        this.userToken = User.get().getId();
        this.pageNum = pageNum;
        this.pageSize = pageSize;
    }

    public PagedRequest(String userToken, int pageNum, int pageSize) {
        this.userToken = userToken;
        this.pageNum = pageNum;
        this.pageSize = pageSize;
    }

    public String getUserToken() {
        return userToken;
    }

    public void setUserToken(String userToken) {
        this.userToken = userToken;
    }

    public int getPageNum() {
        return pageNum;
    }

    public void setPageNum(int pageNum) {
        this.pageNum = pageNum;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }
}
