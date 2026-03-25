package com.kuaigeglobal.epnova.core.model;

public class PushMessageInfo {
    public final static int NORMAL = 1;
    public final static int WARNING = 2;
    //推送消息类型 1-普通消息（默认为1） 2-警告弹出消息
    private String pushMsgContent;
    private int pushMsgType;
    private long notifyId;

    private String pushMsgJumpUri;

    public String getPushMsgJumpUri() {
        return pushMsgJumpUri;
    }

    public void setPushMsgJumpUri(String pushMsgJumpUri) {
        this.pushMsgJumpUri = pushMsgJumpUri;
    }

    public String getPushMsgContent() {
        return pushMsgContent;
    }

    public void setPushMsgContent(String pushMsgContent) {
        this.pushMsgContent = pushMsgContent;
    }

    public int getPushMsgType() {
        return pushMsgType;
    }

    public void setPushMsgType(int pushMsgType) {
        this.pushMsgType = pushMsgType;
    }

    public long getMsgId() {
        return notifyId;
    }

    public void setMsgId(long msgId) {
        this.notifyId = msgId;
    }

    @Override
    public String toString() {
        return "PushMessageInfo{" +
                "pushMsgContent='" + pushMsgContent + '\'' +
                ", pushMsgType=" + pushMsgType +
                ", msgId=" + notifyId +
                ", pushMsgJumpUri='" + pushMsgJumpUri + '\'' +
                '}';
    }
}
