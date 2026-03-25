package com.kuaigeglobal.epnova.common.jsbridge.data;

public class WXAccountData {
    private String msg;
    private String wxUnionId;
    private String wxOpenId;

    public WXAccountData(String msg, String wxUnionId, String wxOpenId) {
        this.msg = msg;
        this.wxUnionId = wxUnionId;
        this.wxOpenId = wxOpenId;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public String getWxUnionId() {
        return wxUnionId;
    }

    public void setWxUnionId(String wxUnionId) {
        this.wxUnionId = wxUnionId;
    }

    public String getWxOpenId() {
        return wxOpenId;
    }

    public void setWxOpenId(String wxOpenId) {
        this.wxOpenId = wxOpenId;
    }
}
