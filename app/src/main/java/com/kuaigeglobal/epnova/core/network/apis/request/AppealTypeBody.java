package com.kuaigeglobal.epnova.core.network.apis.request;


import com.kuaigeglobal.epnova.core.User;

public class AppealTypeBody {
    //申诉类型 401-用户账户被拉黑 405-用户设备被拉黑
    public static final int TYPE_ACCOUNT = 401;
    public static final int TYPE_DEVICE = 405;

    private String userToken;
    private int appealType;

    public AppealTypeBody(int appealType) {
        this.userToken = User.get().getId();
        this.appealType = appealType;
    }

    public String getUserToken() {
        return userToken;
    }

    public void setUserToken(String userToken) {
        this.userToken = userToken;
    }

    public int getAppealType() {
        return appealType;
    }

    public void setAppealType(int appealType) {
        this.appealType = appealType;
    }
}
