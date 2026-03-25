package com.kuaigeglobal.epnova.common.jsbridge;

import android.app.Activity;
import android.graphics.Bitmap;
import android.net.http.SslError;
import android.os.Build;
import android.webkit.JavascriptInterface;
import android.webkit.SslErrorHandler;
import android.webkit.ValueCallback;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.nhsd.basic.utils.Logger;
import com.kuaigeglobal.epnova.R;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

/**
 * Created with IntelliJ IDEA.
 * implements Serializable in case of javascript interface will be removed in obfuscated code.
 * <p>
 * User: jack_fang
 * Date: 13-8-15
 * Time: 下午6:08
 */
public class WebViewJavascriptBridge implements Serializable {
    private static final String TAG = "SimpleBrowserActivity";
    WebView mWebView;
    Activity mContext;
    WVJBHandler _messageHandler;
    Map<String, WVJBHandler> _messageHandlers;
    Map<String, WVJBResponseCallback> _responseCallbacks;
    long _uniqueId;
    WebViewClient mCustomWebClient;

    public WebViewJavascriptBridge(Activity context, WebView webview, WVJBHandler handler) {
        this.mContext = context;
        this.mWebView = webview;
        this._messageHandler = handler;
        _messageHandlers = new HashMap<String, WVJBHandler>();
        _responseCallbacks = new HashMap<String, WVJBResponseCallback>();
        _uniqueId = 0;
        WebSettings webSettings = mWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        mWebView.addJavascriptInterface(this, "_WebViewJavascriptBridge");
        mWebView.setWebViewClient(new MyWebViewClient());
    }


    private void loadWebViewJavascriptBridgeJs(WebView webView) {
        InputStream is = mContext.getResources().openRawResource(R.raw.webviewjavascriptbridge);
        String script = convertStreamToString(is);
        String javascript = new String("javascript:" + script);
        int currentapiVersion = Build.VERSION.SDK_INT;

        // Version specific
        if (currentapiVersion < Build.VERSION_CODES.KITKAT) {
            webView.loadUrl(javascript);
        } else {
            webView.evaluateJavascript(javascript, new ValueCallback<String>() {
                @Override
                public void onReceiveValue(String value) {
                }
            });
        }

    }

    public static String convertStreamToString(InputStream is) {
        String s = "";
        try {
            Scanner scanner = new Scanner(is, "UTF-8").useDelimiter("\\A");
            if (scanner.hasNext()) s = scanner.next();
            is.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return s;
    }

    private class MyWebViewClient extends WebViewClient {


        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            if (mCustomWebClient != null)
                return mCustomWebClient.shouldOverrideUrlLoading(view, url);
            return super.shouldOverrideUrlLoading(view, url);
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            if (mCustomWebClient != null)
                mCustomWebClient.onPageStarted(view, url, favicon);
        }

        @Override
        public void onReceivedSslError(WebView view, SslErrorHandler handler,
                                       SslError error) {
            if (mCustomWebClient != null)
                mCustomWebClient.onReceivedSslError(view, handler, error);
        }


        public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
            if (mCustomWebClient != null)
                mCustomWebClient.onReceivedError(view,errorCode,description,failingUrl);
        }


        @Override
        public void onPageFinished(WebView webView, String url) {
            loadWebViewJavascriptBridgeJs(webView);
            if (mCustomWebClient != null)
                mCustomWebClient.onPageFinished(webView, url);
        }

    }


    public void setCustomWebViewClient(WebViewClient webViewClient) {
        mCustomWebClient = webViewClient;
    }

    public interface WVJBHandler {
        public void handle(String data, WVJBResponseCallback jsCallback);
    }

    public interface WVJBResponseCallback {
        public void callback(String data);
    }

    public void registerHandler(String handlerName, WVJBHandler handler) {
        _messageHandlers.put(handlerName, handler);
    }

    private class CallbackJs implements WVJBResponseCallback {
        private final String callbackIdJs;

        public CallbackJs(String callbackIdJs) {
            this.callbackIdJs = callbackIdJs;
        }

        @Override
        public void callback(String data) {
            _callbackJs(callbackIdJs, data);
        }
    }


    private void _callbackJs(String callbackIdJs, String data) {
        Map<String, String> message = new HashMap<String, String>();
        message.put("responseId", callbackIdJs);
        message.put("responseData", data);
        _dispatchMessage(message);
    }

    //    @JavascriptInterface
//    public void callHandler(String method,String data,String responseData){
//        Log.e("test","callHandler  1:"+method+",2:"+",3:"+responseData);
//    }
    @JavascriptInterface
    public void _handleMessageFromJs(final String data, String responseId,
                                     String responseData, String callbackId, String handlerName) {

        Logger.e(TAG,"callHandler  method:"+handlerName+",data:"+data);
        if (null != responseId) {
            WVJBResponseCallback responseCallback = _responseCallbacks.get(responseId);
            responseCallback.callback(responseData);
            _responseCallbacks.remove(responseId);
        } else {
            WVJBResponseCallback responseCallback = null;
            if (null != callbackId) {
                responseCallback = new CallbackJs(callbackId);
            }
            final WVJBHandler handler;
            if (null != handlerName) {
                handler = _messageHandlers.get(handlerName);
                if (null == handler) {
                    return;
                }
            } else {
                handler = _messageHandler;
            }
            try {
                final WVJBResponseCallback finalResponseCallback = responseCallback;
                mContext.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        handler.handle(data, finalResponseCallback);
                    }
                });
            } catch (Exception exception) {
                Logger.e(TAG, "WebViewJavascriptBridge: WARNING: java handler threw. " + exception.getMessage());
            }
        }
    }

    public void send(String data) {
        send(data, null);
    }

    public void send(String data, WVJBResponseCallback responseCallback) {
        _sendData(data, responseCallback, null);
    }

    private void _sendData(String data, WVJBResponseCallback responseCallback, String handlerName) {

        Logger.e(TAG,"sendData  method:"+handlerName+",data="+data);
        Map<String, String> message = new HashMap<String, String>();
        message.put("data", data);
        if (null != responseCallback) {
            String callbackId = "java_cb_" + (++_uniqueId);
            _responseCallbacks.put(callbackId, responseCallback);
            message.put("callbackId", callbackId);
        }
        if (null != handlerName) {
            message.put("handlerName", handlerName);
        }
        _dispatchMessage(message);
    }

    private void _dispatchMessage(Map<String, String> message) {
        String messageJSON = BridgeDataUtils.toJson(message);
        //L.d(TAG, "sending:" + messageJSON);
        final String javascriptCommand =
                String.format("javascript:WebViewJavascriptBridge._handleMessageFromJava('%s');", doubleEscapeString(messageJSON));
        mContext.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mWebView.loadUrl(javascriptCommand);
            }
        });
    }


    public void callHandler(String handlerName) {
        callHandler(handlerName, null, null);
    }

    public void callHandler(String handlerName, String data) {
        callHandler(handlerName, data,null);
    }

    public void callHandler(String handlerName, String data, WVJBResponseCallback responseCallback) {
        _sendData(data, responseCallback, handlerName);
    }

    /*
     * you must escape the char \ and  char ", or you will not recevie a correct json object in
     * your javascript which will cause a exception in chrome.
     *
     * please check this and you will know why.
     * http://stackoverflow.com/questions/5569794/escape-nsstring-for-javascript-input
     * http://www.json.org/
     */
    private String doubleEscapeString(String javascript) {
        String result;
        result = javascript.replace("\\", "\\\\");
        result = result.replace("\"", "\\\"");
        result = result.replace("\'", "\\\'");
        result = result.replace("\n", "\\n");
        result = result.replace("\r", "\\r");
        result = result.replace("\f", "\\f");
        return result;
    }

}
