package com.kuaigeglobal.epnova.core.network;

import android.util.LruCache;

import androidx.annotation.NonNull;

import com.nhsd.basic.BuildConfig;
import com.nhsd.basic.utils.Logger;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.WeakHashMap;

import okhttp3.Call;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;
import okio.BufferedSource;

public class DuplicatedRequestFilterInterceptor implements Interceptor {

    private static final long WAIT_TIME_OUT_MILLIS = 10000;
    static LruCache<String, CachedResponse> responseCache = new LruCache<>(30);
    static WeakHashMap<String, WeakReference<Call>> calls = new WeakHashMap<>();

    private RequestConfig config;
    static boolean debug = BuildConfig.DEBUG;

    public DuplicatedRequestFilterInterceptor(RequestConfig config) {
        this.config = config;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();

        if (config == null) {
            return chain.proceed(request);
        }
        if (!config.shouldFilter(request.url().toString())) {
            return chain.proceed(request);
        }

        String key = generateKey(request);

        return execute(chain, request, key);
    }

    private Response execute(Chain chain, Request request, String key) throws IOException {
        //从缓存的call和response中判断要不要等待

        if (!isExecuting(key)) {
            CachedResponse cachedResponse = responseCache.get(key);
            if(cachedResponse!=null) {
                logw("response 已缓存,是否过期?" + cachedResponse.isExpired());
            }else {
                logw("response 未缓存");
            }
            if (config.shouldCache(request.url().toString())&&cachedResponse != null && !cachedResponse.isExpired()) {
                return cachedResponse.getClonedResponse();
            } else {
                if (cachedResponse != null && cachedResponse.isExpired()) {
                    responseCache.remove(key);
                }
                //将response缓存起来
                return proceed(chain, request, key);
            }
        } else {
            try {
                synchronized (key) {
                    key.wait(WAIT_TIME_OUT_MILLIS);
                }
                CachedResponse cachedResponse = responseCache.get(key);
                return cachedResponse != null ? cachedResponse.getClonedResponse() : chain.proceed(request);
            } catch (InterruptedException e) {
                return chain.proceed(request);
            }
        }

    }

    @NonNull
    private Response proceed(Chain chain, Request request, String key) throws IOException {
        calls.put(key, new WeakReference<>(chain.call()));
        Response response = chain.proceed(request);
        if (response.isSuccessful() && response.body() != null) {
            ResponseBody responseBody = response.body();
            BufferedSource source = responseBody.source();
            source.request(responseBody.contentLength() > 0 ? responseBody.contentLength() : Integer.MAX_VALUE);
            Buffer buffer = source.buffer();
            Charset charset = StandardCharsets.UTF_8;
            MediaType contentType = responseBody.contentType();
            if (contentType != null) {
                charset = contentType.charset(StandardCharsets.UTF_8);
            }
            String bodyString = buffer.clone().readString(charset);

            ResponseBody cloneBody = ResponseBody.create(response.body().contentType(), bodyString);

            Response responseClone = new Response.Builder()
                    .code(response.code())
                    .protocol(response.protocol())
                    .message(response.message())
                    .body(cloneBody)
                    .headers(response.headers())
                    .header("cached", "true")
                    .request(request)
                    .build();
            responseCache.put(key, new CachedResponse(bodyString, responseClone, System.currentTimeMillis()));
            calls.remove(key);
        }
        synchronized (key) {
            key.notifyAll();
        }
        return response;
    }


    private boolean isExecuting(String key) {

        if (calls.containsKey(key)) {
            WeakReference<Call> callWeakReference = calls.get(key);
            if (callWeakReference == null) {
                logw("duplicated call not exist");
                return false;
            }
            Call call = callWeakReference.get();
            if (call == null || call.isCanceled()) {
                logw("duplicated call not exist or canceled" + call);
                return false;
            }
            logw("duplicated call is requesting:" + call);
            return true;
        }
        logw("no duplicated call in this time");
        return false;
    }

    private static void logw(String str) {
        if (debug) {
            Logger.w("http", str);
        }
    }

    /**
     * @param request
     * @return
     */
    private String generateKey(Request request) {

        return config.generateCacheKey(request);
    }

    class CachedResponse {
        String body;
        Response response;

        long cachedTime;

        public CachedResponse(String body, Response response, long cachedTime) {
            this.body = body;
            this.response = response;
            this.cachedTime = cachedTime;
        }

        public Response getClonedResponse() {
            ResponseBody cloneBody = ResponseBody.create(response.body().contentType(), body);
            Response responseClone = new Response.Builder()
                    .code(response.code())
                    .protocol(response.protocol())
                    .message(response.message())
                    .body(cloneBody)
                    .headers(response.headers())
                    .header("cached", "true")
                    .request(response.request())
                    .build();
            return responseClone;
        }

        public boolean isExpired() {
            return System.currentTimeMillis() - cachedTime > config.responseCacheTimeInMills();
        }
    }

    public interface RequestConfig {
        boolean shouldFilter(String url);

        boolean shouldCache(String url);

        String generateCacheKey(Request request);

        long responseCacheTimeInMills();
    }
}