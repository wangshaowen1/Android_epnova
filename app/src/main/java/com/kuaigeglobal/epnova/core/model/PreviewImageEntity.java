package com.kuaigeglobal.epnova.core.model;

import java.util.List;

public class PreviewImageEntity {
    private List<String> urls;
    private int current;

    public List<String> getUrls() {
        return urls;
    }

    public void setUrls(List<String> urls) {
        this.urls = urls;
    }

    public int getCurrent() {
        return current;
    }

    public void setCurrent(int current) {
        this.current = current;
    }
}
