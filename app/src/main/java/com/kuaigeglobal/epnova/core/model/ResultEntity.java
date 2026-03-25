package com.kuaigeglobal.epnova.core.model;

public class ResultEntity {

    public static final int SUCCESS = 200;
    public static final int FAILED = 501;
    private int code;
    private String data;

    public ResultEntity(int code) {
        this.code = code;
        this.data="";
    }

    public ResultEntity(int code, String data) {
        this.code = code;
        this.data = data;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }
}
