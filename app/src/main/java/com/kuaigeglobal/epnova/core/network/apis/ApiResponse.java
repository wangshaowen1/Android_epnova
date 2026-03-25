package com.kuaigeglobal.epnova.core.network.apis;


public class ApiResponse<T> {

    public static final int RESPONSE_CODE_SUCCESS = 200;
    /**
     * 算力不足
     */
    public static final int RESPONSE_CODE_POWER_NOT_ENOUGH = 300005;

    private int code;
    private String msg;
    private T data;
    private int errorCode;
    private String errorMsg;
    private String respId;

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }


    public boolean isSuccess() {
        return code == RESPONSE_CODE_SUCCESS;
    }


    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public int getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(int errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorMsg() {
        return errorMsg;
    }

    public void setErrorMsg(String errorMsg) {
        this.errorMsg = errorMsg;
    }

    public String getRespId() {
        return respId;
    }

    public void setRespId(String respId) {
        this.respId = respId;
    }

    public ApiException getApiException() {
        return new ApiException(code, msg);
    }
}
