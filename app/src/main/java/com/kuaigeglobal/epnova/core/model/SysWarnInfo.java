package com.kuaigeglobal.epnova.core.model;

public class SysWarnInfo {

    private long msgId;
    private String msgContent;

    public SysWarnInfo(long msgId, String msgContent) {
        this.msgId = msgId;
        this.msgContent = msgContent;
    }

    public long getMsgId() {
        return msgId;
    }

    public void setMsgId(long msgId) {
        this.msgId = msgId;
    }

    public String getMsgContent() {
        return msgContent;
    }

    public void setMsgContent(String msgContent) {
        this.msgContent = msgContent;
    }
}
