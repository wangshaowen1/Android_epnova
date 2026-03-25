package com.kuaigeglobal.epnova.common.jsbridge.data;

import java.util.List;

public class TakePhotoData {

    /**
     * maxCount : 1
     * sourceType : ["album"]
     * method : articleContentImg
     */

    private int maxCount;
    private String method;
    private List<String> sourceType;

    public int getMaxCount() {
        return maxCount;
    }

    public void setMaxCount(int maxCount) {
        this.maxCount = maxCount;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public List<String> getSourceType() {
        return sourceType;
    }

    public void setSourceType(List<String> sourceType) {
        this.sourceType = sourceType;
    }
}
