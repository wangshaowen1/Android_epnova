package com.kuaigeglobal.epnova.core.network.apis.entity;


import com.kuaigeglobal.epnova.core.User;

public class CookieUserInfo {
    private String userToken;
    private String version;
    /**
     * 语言编码 zh_CN(简体中文-中国)、en_US(英语-美国)
     */
    private String lang;
    public CookieUserInfo(String userToken, String version) {
        this.userToken = userToken;
        this.version = version;
    }

    public CookieUserInfo(String userToken, String version, String lang) {
        this.userToken = userToken;
        this.version = version;
        this.lang = lang;
    }

    public CookieUserInfo() {
        userToken = User.get().getId();
    }

    public String getUserToken() {
        return userToken;
    }

    public void setUserToken(String userToken) {
        this.userToken = userToken;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getLang() {
        return lang;
    }

    public void setLang(String lang) {
        this.lang = lang;
    }
}
