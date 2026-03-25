package com.kuaigeglobal.epnova.common;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;
import android.util.Pair;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;

/**
 * SeesionManager for FansFramework
 *
 * @author andrew.wang
 */
public class SharedPreferenceManager implements Observer {

    // public static final String PRE_LOCAL_GUIDE_VERSION =
    // "pre.local.guide.version";
    // public static final String PRE_REMIND_VERSION = "pre.remind.version";
    // public static final String PRE_REMIND_SCAN_CODE = "pre.remind.scan_code";
    // public static final String PRE_REMIND_HOSPITAL = "pre.remind.hosptial";
    // public static final String PRE_REMIND_VACCINE = "pre.remind.vaccine";
    // public static final String PRE_REMIND_SETTING_TIME =
    // "pre.remind.setting.time";
    //
    // public static final String PRE_GOV_VACCINE_DES = "pre.gov.vaccine_desc";
    // version name
    private static SharedPreferenceManager mInstance;

    private SharedPreferences mPreference;
    private Context mContext;
    private LinkedList<Pair<String, Object>> mUpdateQueue = new LinkedList<Pair<String, Object>>();

    private HashMap<String, Object> cacheMap = new HashMap<String, Object>();

    private Thread mCurrentUpdateThread;

    private SharedPreferenceManager(Context context) {
        synchronized (this) {
            mContext = context;
            if (mPreference == null) {
                mPreference = PreferenceManager.getDefaultSharedPreferences(mContext);
            }
        }
    }

    public static SharedPreferenceManager get(Context context) {
        if (mInstance == null) {
            mInstance = new SharedPreferenceManager(context);
        }
        return mInstance;
    }



    /**
     * Release all resources
     */
    public void close() {
        mPreference = null;
        mInstance = null;
    }

    private boolean isPreferenceNull() {
        if (mPreference == null)
            return true;
        return false;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void update(Observable observable, Object data) {
        if (data instanceof Pair) {
            synchronized (mUpdateQueue) {
                if (data != null) {
                    Pair<String, Object> pair = (Pair<String, Object>) data;
                    mUpdateQueue.add(pair);
                }
            }
            writePreferenceSlowly();
        }
    }

    /*
     * Do Hibernation slowly
     */
    private void writePreferenceSlowly() {
        if (mCurrentUpdateThread != null) {
            if (mCurrentUpdateThread.isAlive()) {
                // the update thread is still running,
                // so no need to start a new one
                return;
            }
        }

        mCurrentUpdateThread = new Thread() {

            @Override
            public void run() {

                try {
                    // sleep 10secs to wait some concurrent task be
                    // inserted into the task queue
                    sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                writePreference();
            }

        };
        mCurrentUpdateThread.setPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
        try{
            mCurrentUpdateThread.start();
        }catch (Throwable e){
            //ignore
        }
    }

    /*
     * Do Hibernation immediately
     */
    public void writePreferenceQuickly() {

        mCurrentUpdateThread = new Thread() {

            @Override
            public void run() {
                writePreference();
            }

        };
        mCurrentUpdateThread.setPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
        mCurrentUpdateThread.start();
    }

    /**
     * Write session value back to preference
     */
    private void writePreference() {

        Editor editor = mPreference.edit();

        synchronized (mUpdateQueue) {
            while (!mUpdateQueue.isEmpty()) {
                try {


                    Pair<String, Object> updateItem = mUpdateQueue.remove();
                    final String key = updateItem.first;
                    Object value = updateItem.second;
                    // System.out.println(key+":"+value);
                    if (value == null) {
                        editor.putString(key, null);
                    } else if (value instanceof String) {
                        editor.putString(key, (String) value);
                    } else if (value instanceof Integer) {
                        editor.putInt(key, (Integer) value);
                    } else if (value instanceof Long) {
                        editor.putLong(key, (Long) value);
                    } else if (value instanceof Float) {
                        editor.putFloat(key, (Float) value);
                    } else if (value instanceof Boolean) {
                        editor.putBoolean(key, (Boolean) value);
                    }
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }
        editor.apply();
    }

    // public HashMap<String, Object> readPreference() {
    //
    // if (isPreferenceNull()) {
    // return null;
    // }
    //
    // HashMap<String, Object> data = new HashMap<String, Object>();
    // data.put(PRE_LOCAL_GUIDE_VERSION,
    // mPreference.getInt(PRE_LOCAL_GUIDE_VERSION, 0));
    // data.put(PRE_REMIND_VERSION, mPreference.getInt(PRE_REMIND_VERSION, 0));
    // data.put(PRE_GOV_VACCINE_DES, mPreference.getString(PRE_GOV_VACCINE_DES,
    // null));
    // data.put(PRE_REMIND_SCAN_CODE, mPreference.getInt(PRE_REMIND_SCAN_CODE,
    // -1));
    // data.put(PRE_REMIND_HOSPITAL, mPreference.getInt(PRE_REMIND_HOSPITAL,
    // -1));
    // data.put(PRE_REMIND_VACCINE, mPreference.getInt(PRE_REMIND_VACCINE, -1));
    // data.put(PRE_REMIND_SETTING_TIME,
    // mPreference.getLong(PRE_REMIND_SETTING_TIME, -1));
    // return data;
    // }

    public String get(String key, String defaultValue) {
        Object object = cacheMap.get(key);
        if (object != null && object instanceof String) {
            return object.toString();
        } else {
            return mPreference.getString(key, defaultValue);
        }
    }

    public void put(String key, String value) {
        // System.out.println("put string...");
        cacheMap.put(key, value);
        mUpdateQueue.add(new Pair<String, Object>(key, value));
        writePreferenceSlowly();
    }

    public void put(String key, int value) {
        cacheMap.put(key, value);
        mUpdateQueue.add(new Pair<String, Object>(key, value));
        writePreferenceSlowly();
    }

    public int get(String key, int defaultValue) {
        Object object = cacheMap.get(key);
        if (object != null && object instanceof Integer) {
            return ((Integer) object).intValue();
        } else {
            return mPreference.getInt(key, defaultValue);
        }
    }

    public void put(String key, float value) {
        cacheMap.put(key, value);
        mUpdateQueue.add(new Pair<String, Object>(key, value));
        writePreferenceSlowly();
    }

    public float get(String key, float defaultValue) {
        Object object = cacheMap.get(key);
        if (object != null && object instanceof Float) {
            return ((Float) object).floatValue();
        } else {
            return mPreference.getFloat(key, defaultValue);
        }
    }

    public void put(String key, long value) {
        cacheMap.put(key, value);
        mUpdateQueue.add(new Pair<String, Object>(key, value));
        writePreferenceSlowly();
    }

    public long get(String key, long defaultValue) {
        Object object = cacheMap.get(key);
        if (object != null && object instanceof Double) {
            return ((Long) object).longValue();
        } else {
            return mPreference.getLong(key, defaultValue);
        }
    }

    public void put(String key, boolean value) {
        cacheMap.put(key, value);
        mUpdateQueue.add(new Pair<String, Object>(key, value));
        writePreferenceSlowly();
    }

    public boolean get(String key, boolean defaultValue) {
        Object object = cacheMap.get(key);
        if (object != null && object instanceof Boolean) {
            return (Boolean) object;
        } else {
            return mPreference.getBoolean(key, defaultValue);
        }
    }

    public void remove(String key) {
        Object value = cacheMap.remove(key);
        mUpdateQueue.remove(new Pair<String, Object>(key, value));
        mPreference.edit().remove(key).commit();
    }

    /**
     * 根据 Key 前缀批量删除数据（修复版：同步清理内存缓存）
     * @param prefix Key 前缀 (例如 "USER_DATA_")
     */
    public void removeByPrefix(String prefix) {
        if (prefix == null) return;

        // 1. 清理内存缓存 (cacheMap) [关键修复点]
        // 必须先清内存，否则下次读取会直接命中脏数据
        if (cacheMap != null) {
            // 使用迭代器遍历，防止 ConcurrentModificationException
            java.util.Iterator<String> iterator = cacheMap.keySet().iterator();
            while (iterator.hasNext()) {
                String key = iterator.next();
                if (key != null && key.startsWith(prefix)) {
                    iterator.remove();
                }
            }
        }

        // 2. 清理待写入队列 (mUpdateQueue) [防御性修复]
        // 防止清理后，之前积压的异步写入任务又把旧数据写回磁盘
        if (mUpdateQueue != null) {
            java.util.Iterator<android.util.Pair<String, Object>> queueIter = mUpdateQueue.iterator();
            while (queueIter.hasNext()) {
                android.util.Pair<String, Object> pair = queueIter.next();
                if (pair.first != null && pair.first.startsWith(prefix)) {
                    queueIter.remove();
                }
            }
        }

        // 3. 清理磁盘文件 (SharedPreferences)
        if (mPreference != null) {
            Map<String, ?> map = mPreference.getAll();
            Editor editor = mPreference.edit();
            boolean hasChange = false;

            for (String key : map.keySet()) {
                if (key != null && key.startsWith(prefix)) {
                    editor.remove(key);
                    hasChange = true;
                }
            }
            // 只有真正有删除操作时才提交 IO
            if (hasChange) {
                editor.apply();
            }
        }
    }
    public void clear() {
        cacheMap.clear();
        mPreference.edit().clear().commit();
    }

    public Map<String, ?> getAll() {
        return mPreference.getAll();
    }
}
