package com.kuaigeglobal.epnova.core.network.apis;


public class ApiException extends RuntimeException {

    //token失效
    public static final int AUTHORIZATION_ERROR = 1001;
    //已注销用户
    public static final int LOGGED_OUT_USER = 112;

    //已拉黑
    public static final int USER_ADD_TO_BLACK_LIST = 113;
    /*
   * 401 -- 用户拉黑，未提出申诉
402 -- 用户拉黑，已提出申诉
405 -- 设备拉黑，未提出申诉
406 -- 设备拉黑，已提出申诉
407- 用户拉黑，固定时长拉黑用户拉黑，固定时长拉黑*/
    public static final int USER_ACCOUNT_SET_BLACK = 501;
    public static final int USER_ACCOUNT_SET_BLACK_APPEAL = 502;
    public static final int DEVICE_SET_BLACK = 505;
    public static final int DEVICE_SET_BLACK_APPEAL = 506;
    public static final int DEVICE_SET_BLACK_DURING = 507;


    /**
     * 406 -- 图片不合法
     */
    public static final int IMAGE_ILLEGAL = 406;
    /**
     * 407 -- 文字不合法
     */
    public static final int TEXT_ILLEGAL = 407;

    /**
     * 音乐生成配置已变更错误码
     */
    public static final int COMPOSE_CONFIG_CHANGED = 300002;
    /**
     * 作品加热个数超出范围
     */
    public static final int MUSIC_BOOST_OUT_OF_RANGE = 50001;

    public static final int INSUFFICIENT_BALANCE = 400029;
    /**
     * 对应ApiResponse中的code
     */
    public final int errorCode;
    /**
     * 对应ApiResponse中的errorCode(部分接口这个值=code，因为没设置过来，看清楚再用)
     */
    public final int businessErrorCode;

    public ApiException(int code, String message) {
        super(message);
        this.errorCode = code;
        this.businessErrorCode = code;
    }

    public ApiException(int code, int errorCode, String message) {
        super(message);
        this.errorCode = code;
        this.businessErrorCode = errorCode;
    }

    public int getErrorCode() {
        return errorCode;
    }

    public int getBusinessErrorCode() {
        return businessErrorCode;
    }
}

