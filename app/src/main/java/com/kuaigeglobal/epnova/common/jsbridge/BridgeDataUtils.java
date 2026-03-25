package com.kuaigeglobal.epnova.common.jsbridge;

import com.google.gson.Gson;

public class BridgeDataUtils {
    private static Gson gson = new Gson();

    public static <T> T parseJson(String data, Class<T> clazz) {
        return gson.fromJson(data, clazz);
    }

    public static String toJson(Object object) {
        return gson.toJson(object);
    }
}
