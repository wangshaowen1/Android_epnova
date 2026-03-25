package com.kuaigeglobal.epnova.core;

import com.google.gson.Gson;
import com.kuaigeglobal.epnova.common.Cache;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Objects;

/**
 * Created by lu on 2016/11/23.
 */

public abstract class EntityCache<T> {
    public static final int DEFAULT_EXPIRE_TIME=2*60 * 60 * 10000;
    private Gson gson = new Gson();
    private String key;
    private String timeKey;
    private Type type;
    private T cacheData;
    private long lastModifyTime = 0;
    private long expireTime;
    public EntityCache(String key){
        this(key,0);
    }
    public EntityCache(String key,long expireTime) {
        this.key = key;
        this.expireTime=expireTime;
        this.timeKey = key + "_time";
        Class clazz = getClass();
        if (clazz.getGenericSuperclass() instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) clazz.getGenericSuperclass();
            Type[] typeArguments = parameterizedType.getActualTypeArguments();
            if (typeArguments.length > 0) {
                type = typeArguments[0];

            }
        }
    }

    public void cache(T data) {
        if (!Objects.equals(cacheData, data)) {
            String json=gson.toJson(data);
            Cache.get().persistedCache(key,json );
            cacheData = data;
        }
        lastModifyTime = System.currentTimeMillis();
        Cache.get().persistedCache(timeKey, lastModifyTime);
    }


    public boolean isExpire() {
        if(expireTime==0){
            return true;
        }
        return System.currentTimeMillis() - lastModifyTime > expireTime;
    }

    public T getCache() {
        if (cacheData != null) {
            return cacheData;
        }
        String value = Cache.get().getString(key);
        if (value != null) {
            lastModifyTime=Cache.get().getLong(timeKey);
            T result = gson.fromJson(value, type);
            cacheData = result;
            return result;
        }
        return null;
    }


}
